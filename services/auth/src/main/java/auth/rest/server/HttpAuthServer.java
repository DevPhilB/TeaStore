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
package auth.rest.server;

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

import static utilities.rest.api.API.DEFAULT_AUTH_PORT;
import static utilities.rest.api.API.AUTH_ENDPOINT;

/**
 * HTTP server for auth service
 * @author Philipp Backes
 */
public class HttpAuthServer {

    private final String httpVersion;
    private final String gatewayHost;
    private final Integer gatewayPort;
    private static final Logger LOG = LogManager.getLogger(HttpAuthServer.class);

    public HttpAuthServer(String httpVersion, String gatewayHost, Integer gatewayPort) {
        this.httpVersion = httpVersion;
        this.gatewayHost = gatewayHost;
        this.gatewayPort = gatewayPort;
    }

    public static void main(String[] args) throws Exception {
        String httpVersion = args.length > 0 ? args[0] != null ? args[0] : "HTTP/1.1" : "HTTP/1.1";
        String gatewayHost = args.length > 1 ? args[1] != null ? args[1] : "" : "";
        Integer gatewayPort = args.length > 2 ? args[2] != null ? Integer.parseInt(args[2]) : 80 : 80;
        new HttpAuthServer(
                httpVersion,
                gatewayHost,
                gatewayPort
        ).run();
    }

    private void bindAndSync(ServerBootstrap bootstrap) throws InterruptedException {
        Channel channel;
        String status = httpVersion + " auth service is available on " +
                (httpVersion.equals("HTTP/1.1") ? "http://" : "https://");;
        if (gatewayHost.isEmpty()) {
            channel = bootstrap.bind(DEFAULT_AUTH_PORT).sync().channel();
            status += "localhost:" + DEFAULT_AUTH_PORT + AUTH_ENDPOINT;
        } else {
            channel = bootstrap.bind(gatewayPort).sync().channel();
            status += "auth:" + gatewayPort + AUTH_ENDPOINT;
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
                                    channelPipeline.addLast(new Http1AuthServiceHandler(gatewayHost, gatewayPort));
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
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                SslContext sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
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
                                protected void initChannel(SocketChannel channel) {
                                    channel.pipeline().addLast(sslCtx.newHandler(channel.alloc()));
                                    channel.pipeline().addLast(Http2FrameCodecBuilder.forServer().build());
                                    channel.pipeline().addLast(new Http2AuthServiceHandler(gatewayHost, gatewayPort));
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
