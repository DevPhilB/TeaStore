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
package persistence.rest.server;

import io.netty.handler.codec.http.HttpVersion;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import static utilities.rest.api.API.DEFAULT_PERSISTENCE_PORT;
import static utilities.rest.api.API.PERSISTENCE_ENDPOINT;

/**
 * HTTP server for persistence service
 * @author Philipp Backes
 */
public class HttpPersistenceServer {

    private final HttpVersion httpVersion;
    private final String scheme;
    private final String gatewayHost;
    private final Integer gatewayPort;
    private static final Logger LOG = LogManager.getLogger(HttpPersistenceServer.class);

    public HttpPersistenceServer(
            HttpVersion httpVersion,
            String scheme,
            String gatewayHost,
            Integer gatewayPort
    ) {
        this.httpVersion = httpVersion;
        this.scheme = scheme;
        this.gatewayHost = gatewayHost;
        this.gatewayPort = gatewayPort;
    }

    public static void main(String[] args) throws Exception {
        String httpVersion = args.length > 1 ? args[0] != null ? args[0] : "HTTP/1.1" : "HTTP/1.1";
        String scheme = args.length > 2 ? args[1] != null ? args[1] : "http://" : "http://";
        String gatewayHost = args.length > 3 ? args[2] != null ? args[2] : "" : "";
        Integer gatewayPort = args.length > 4 ? args[3] != null ? Integer.parseInt(args[3]) : 80 : 80;
        new HttpPersistenceServer(
                httpVersion.equals("HTTP/1.1") ? HttpVersion.HTTP_1_1 : HttpVersion.HTTP_1_1,
                scheme,
                gatewayHost,
                gatewayPort
        ).run();
    }

    public void run() throws Exception {
        // Accept incoming connections
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        // Handle the traffic of the accepted connection
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            // Set up the server
            ServerBootstrap bootstrap = new ServerBootstrap();
            // Configure server
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
                        channelPipeline.addLast(
                                new HttpPersistenceServiceHandler(httpVersion, gatewayHost, gatewayPort)
                        );
                    }
                });
            //
            ChannelFuture future;
            String status = httpVersion + " persistence service is available on " + scheme;
            if(gatewayHost.isEmpty()) {
                future = bootstrap.bind(DEFAULT_PERSISTENCE_PORT).sync();
                status += "localhost:" + DEFAULT_PERSISTENCE_PORT + PERSISTENCE_ENDPOINT;
            } else {
                future = bootstrap.bind(gatewayPort).sync();
                status += "persistence:" + gatewayPort + PERSISTENCE_ENDPOINT;
            }
            LOG.info(status);
            System.err.println(status);
            future.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
