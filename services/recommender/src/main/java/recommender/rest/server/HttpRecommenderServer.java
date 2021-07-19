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

import io.netty.channel.*;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.*;
import io.netty.handler.ssl.util.SelfSignedCertificate;
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

import java.util.ServiceLoader;

import static utilities.rest.api.API.DEFAULT_RECOMMENDER_PORT;
import static utilities.rest.api.API.RECOMMENDER_ENDPOINT;

/**
 * HTTP server for recommender service
 * @author Philipp Backes
 */
public class HttpRecommenderServer {

    private final String httpVersion;
    private final String scheme;
    private final String gatewayHost;
    private final Integer gatewayPort;
    private static final Logger LOG = LogManager.getLogger();

    public HttpRecommenderServer(String httpVersion, String gatewayHost, Integer gatewayPort) {
        this.httpVersion = httpVersion;
        this.scheme = httpVersion.equals("HTTP/1.1") ? "http://" : "https://";
        this.gatewayHost = gatewayHost;
        this.gatewayPort = gatewayPort;
        // Setup and start training
        TrainingSynchronizer.getInstance().setupHttpClient(
                this.httpVersion,
                this.scheme,
                this.gatewayHost,
                this.gatewayHost.isEmpty() ? 3030 : gatewayPort
        );
        TrainingSynchronizer.getInstance().retrieveDataAndRetrain();
    }

    public static void main(String[] args) throws Exception {
        String httpVersion = args.length > 1 ? args[0] != null ? args[0] : "HTTP/2" : "HTTP/2";
        String gatewayHost = args.length > 2 ? args[1] != null ? args[1] : "" : "";
        Integer gatewayPort = args.length > 3 ? args[2] != null ? Integer.parseInt(args[2]) : 80 : 80;
        new HttpRecommenderServer(
                httpVersion,
                gatewayHost,
                gatewayPort
        ).run();
    }

    private void bindAndSync(ServerBootstrap bootstrap) throws InterruptedException {
        Channel channel;
        String status = httpVersion + " recommender service is available on " + scheme;
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
                final SslContext sslCtx;
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
                        .sslProvider(SslProvider.JDK)
                        .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                        .applicationProtocolConfig(new ApplicationProtocolConfig(
                                ApplicationProtocolConfig.Protocol.ALPN,
                                ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                                ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                                ApplicationProtocolNames.HTTP_2))
                        .build();
                // Configure the server
                EventLoopGroup group = new NioEventLoopGroup();
                try {
                    ServerBootstrap bootstrap = new ServerBootstrap();
                    bootstrap.option(ChannelOption.SO_BACKLOG, 1024);
                    bootstrap.group(group)
                            .channel(NioServerSocketChannel.class)
                            .handler(new LoggingHandler(LogLevel.INFO))
                            .childHandler(new ChannelInitializer<SocketChannel>() {
                                @Override
                                protected void initChannel(SocketChannel ch) throws Exception {
                                    // Configure new handlers for the channel pipeline of new channels
                                    ChannelPipeline pipeline = ch.pipeline();
                                    pipeline.addLast(sslCtx.newHandler(ch.alloc()));
                                    pipeline.addLast(Http2FrameCodecBuilder.forServer().build());
                                    pipeline.addLast(new Http2RecommenderServiceHandler(gatewayHost, gatewayPort));
                                }
                            });
                    bindAndSync(bootstrap);
                } finally {
                    group.shutdownGracefully();
                }
                break;
            case "HTTP/3":
                // TODO
                LOG.info("TODO");
                break;
        }
    }
}
