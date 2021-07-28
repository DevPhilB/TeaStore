/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package recommender.rest.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.*;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.incubator.codec.http3.Http3;
import io.netty.incubator.codec.http3.Http3ServerConnectionHandler;
import io.netty.incubator.codec.http3.Http3ServerPushStreamManager;
import io.netty.incubator.codec.quic.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import recommender.algorithm.TrainingSynchronizer;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import static utilities.rest.api.API.DEFAULT_RECOMMENDER_PORT;
import static utilities.rest.api.API.RECOMMENDER_ENDPOINT;

/**
 * HTTP server for recommender service
 * @author Philipp Backes
 */
public class HttpRecommenderServer {

    private final String httpVersion;
    private final String gatewayHost;
    private final Integer gatewayPort;
    private static final Logger LOG = LogManager.getLogger(HttpRecommenderServer.class);

    public HttpRecommenderServer(String httpVersion, String gatewayHost, Integer gatewayPort) {
        this.httpVersion = httpVersion;
        this.gatewayHost = gatewayHost;
        this.gatewayPort = gatewayPort;
        // Setup and start training
        TrainingSynchronizer.getInstance().setupHttpClient(
                httpVersion,
                gatewayHost,
                gatewayPort
        );
        TrainingSynchronizer.getInstance().retrieveDataAndRetrain();
    }

    public static void main(String[] args) throws Exception {
        String httpVersion = args.length > 0 ? args[0] != null ? args[0] : "HTTP/1.1" : "HTTP/1.1";
        String gatewayHost = args.length > 1 ? args[1] != null ? args[1] : "" : "";
        Integer gatewayPort = args.length > 2 ? args[2] != null ? Integer.parseInt(args[2]) : 80 : 80;
        new HttpRecommenderServer(
                httpVersion,
                gatewayHost,
                gatewayPort
        ).run();
    }

    private void bindAndSync(ServerBootstrap bootstrap) throws InterruptedException {
        Channel channel;
        String status = httpVersion + " recommender service is available on " +
                (httpVersion.equals("HTTP/1.1") ? "http://" : "https://");
        if (gatewayHost.isEmpty()) {
            channel = bootstrap.bind(DEFAULT_RECOMMENDER_PORT).sync().channel();
            status += "localhost:" + DEFAULT_RECOMMENDER_PORT + RECOMMENDER_ENDPOINT;
        } else {
            channel = bootstrap.bind(gatewayPort).sync().channel();
            status += "recommender:" + gatewayPort + RECOMMENDER_ENDPOINT;
        }
        LOG.info(status);
        channel.closeFuture().sync();
    }

    public void run() throws Exception {
        // Accept incoming connections
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        // Handle the traffic of the accepted connection
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        // Self signed certificate
        SelfSignedCertificate certificate = new SelfSignedCertificate();
        // Switch between HTTP versions
        switch (httpVersion) {
            case "HTTP/1.1":
                // Configure the server
                try {
                    ServerBootstrap bootstrap = new ServerBootstrap();
                    bootstrap.group(bossGroup, workerGroup)
                            // Instantiate new channels to accept incoming connections
                            .channel(NioServerSocketChannel.class)
                            // Instantiate new handler for logging
                            .handler(new LoggingHandler(LogLevel.INFO))
                            // Instantiate new handler for newly accepted channels
                            .childHandler(new ChannelInitializer<SocketChannel>() {
                                @Override
                                protected void initChannel(SocketChannel ch) throws Exception {
                                    // Configure new handlers for the channel pipeline of new channels
                                    ChannelPipeline channelPipeline = ch.pipeline();
                                    channelPipeline.addLast(new HttpRequestDecoder());
                                    channelPipeline.addLast(new HttpResponseEncoder());
                                    channelPipeline.addLast(new Http1RecommenderServiceHandler(gatewayHost, gatewayPort));
                                }
                            });
                    bindAndSync(bootstrap);
                } finally {
                    bossGroup.shutdownGracefully();
                    workerGroup.shutdownGracefully();
                }
                break;
            case "HTTP/2":
                // Configure SSL
                SslContext sslCtx = SslContextBuilder.forServer(certificate.certificate(), certificate.privateKey())
                        .sslProvider(SslProvider.JDK)
                        .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                        .applicationProtocolConfig(new ApplicationProtocolConfig(
                                ApplicationProtocolConfig.Protocol.ALPN,
                                ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                                ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                                ApplicationProtocolNames.HTTP_2))
                        .build();
                // Configure the server
                try {
                    ServerBootstrap bootstrap = new ServerBootstrap();
                    bootstrap.option(ChannelOption.SO_BACKLOG, 1024);
                    bootstrap.group(bossGroup)
                            .channel(NioServerSocketChannel.class)
                            .handler(new LoggingHandler(LogLevel.INFO))
                            .childHandler(new ChannelInitializer<SocketChannel>() {
                                @Override
                                protected void initChannel(SocketChannel channel) {
                                    channel.pipeline().addLast(sslCtx.newHandler(channel.alloc()));
                                    channel.pipeline().addLast(Http2FrameCodecBuilder.forServer().build());
                                    channel.pipeline().addLast(new Http2RecommenderServiceHandler(gatewayHost, gatewayPort));
                                }
                            });
                    bindAndSync(bootstrap);
                } finally {
                    bossGroup.shutdownGracefully();
                }
                break;
            case "HTTP/3":
                // Configure SSL
                QuicSslContext quicSslContext = QuicSslContextBuilder
                        .forServer(certificate.key(), null, certificate.cert())
                        .applicationProtocols(Http3.supportedApplicationProtocols()).build();
                // Configure codec
                ChannelHandler codec = Http3.newQuicServerCodecBuilder()
                        .sslContext(quicSslContext)
                        .maxIdleTimeout(5000, TimeUnit.MILLISECONDS)
                        .initialMaxData(10000000)
                        .initialMaxStreamDataBidirectionalLocal(1000000)
                        .initialMaxStreamDataBidirectionalRemote(1000000)
                        .initialMaxStreamsBidirectional(100)
                        .tokenHandler(InsecureQuicTokenHandler.INSTANCE)
                        .handler(new ChannelInitializer<QuicChannel>() {
                            @Override
                            protected void initChannel(QuicChannel quicChannel) {
                                // Called for each connection
                                Http3ServerPushStreamManager pushStreamManager =
                                        new Http3ServerPushStreamManager(quicChannel);
                                quicChannel.pipeline().addLast(new Http3ServerConnectionHandler(
                                        new ChannelInitializer<QuicStreamChannel>() {
                                            // Called for each request-stream
                                            @Override
                                            protected void initChannel(QuicStreamChannel streamChannel) {
                                                streamChannel.pipeline().addLast(
                                                        new Http3RecommenderServiceHandler(gatewayHost, gatewayPort)
                                                );
                                                streamChannel.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
                                            }
                                        },
                                        pushStreamManager.controlStreamListener(),
                                        null,
                                        null,
                                        false
                                ));
                                quicChannel.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
                            }
                        }).build();
                // Configure the server
                try {
                    Bootstrap bootstrap = new Bootstrap();
                    Channel channel;
                    String status = httpVersion + " recommender service is available on https://";
                    if (gatewayHost.isEmpty()) {
                        channel = bootstrap.group(bossGroup)
                                .channel(NioDatagramChannel.class)
                                .handler(codec)
                                .bind(new InetSocketAddress(DEFAULT_RECOMMENDER_PORT)).sync().channel();
                        status += "localhost:" + DEFAULT_RECOMMENDER_PORT + RECOMMENDER_ENDPOINT;
                    } else {
                        channel = bootstrap.group(bossGroup)
                                .channel(NioDatagramChannel.class)
                                .handler(codec)
                                .bind(new InetSocketAddress(gatewayPort)).sync().channel();
                        status += "recommender:" + gatewayPort + RECOMMENDER_ENDPOINT;
                    }
                    LOG.info(status);
                    channel.closeFuture().sync();
                } finally {
                    bossGroup.shutdownGracefully();
                }
                break;
        }
    }
}
