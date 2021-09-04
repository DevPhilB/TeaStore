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
package web.rest.server;

import io.netty.channel.*;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

import static utilities.rest.api.API.DEFAULT_WEB_PORT;
import static utilities.rest.api.API.WEB_ENDPOINT;

/**
 * HTTP server for web service
 * @author Philipp Backes
 */
public class HttpWebServer {
    private final String httpVersion;
    private final String gatewayHost;
    private final Integer webPort;
    private final Integer persistencePort; // Only for HTTP/3
    private final Integer authPort; // Only for HTTP/3
    private final Integer imagePort; // Only for HTTP/3
    private final Integer recommenderPort; // Only for HTTP/3
    private static final Logger LOG = LogManager.getLogger(HttpWebServer.class);

    public HttpWebServer(
            String httpVersion,
            String gatewayHost,
            Integer webPort,
            Integer persistencePort,
            Integer authPort,
            Integer imagePort,
            Integer recommenderPort
    ) {
        this.httpVersion = httpVersion;
        this.gatewayHost = gatewayHost;
        this.webPort = webPort;
        this.persistencePort = persistencePort;
        this.authPort = authPort;
        this.imagePort = imagePort;
        this.recommenderPort = recommenderPort;
    }

    public static void main(String[] args) throws Exception {
        String httpVersion = args.length > 0 ? args[0] != null ? args[0] : "HTTP/1.1" : "HTTP/1.1";
        String gatewayHost = args.length > 1 ? args[1] != null ? args[1] : "" : "";
        Integer webPort = args.length > 2 ? args[2] != null ? Integer.parseInt(args[2]) : 80 : 80;
        Integer persistencePort = args.length > 3 ? args[3] != null ? Integer.parseInt(args[3]) : 80 : 80;
        Integer authPort = args.length > 4 ? args[4] != null ? Integer.parseInt(args[4]) : 80 : 80;
        Integer imagePort = args.length > 5 ? args[5] != null ? Integer.parseInt(args[5]) : 80 : 80;
        Integer recommenderPort = args.length > 6 ? args[6] != null ? Integer.parseInt(args[6]) : 80 : 80;
        new HttpWebServer(
                httpVersion,
                gatewayHost,
                webPort,
                persistencePort,
                authPort,
                imagePort,
                recommenderPort
        ).run();
    }

    private void bindAndSync(ServerBootstrap bootstrap) throws InterruptedException {
        Channel channel;
        String status = httpVersion + " web service is available on http://";
        if (gatewayHost.isEmpty()) {
            channel = bootstrap.bind(DEFAULT_WEB_PORT).sync().channel();
            status += "localhost:" + DEFAULT_WEB_PORT + WEB_ENDPOINT;
        } else {
            channel = bootstrap.bind(webPort).sync().channel();
            status += "web:" + webPort + WEB_ENDPOINT;
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
        // Configure the server
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    // Instantiate new channels to accept incoming connections
                    .channel(NioServerSocketChannel.class)
                    // Instantiate new handler for newly accepted channels
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            // Configure new handlers for the channel pipeline of new channels
                            ChannelPipeline channelPipeline = ch.pipeline();
                            channelPipeline.addLast(new HttpRequestDecoder());
                            channelPipeline.addLast(new HttpResponseEncoder());
                            channelPipeline.addLast(new HttpWebServiceHandler(
                                    httpVersion,
                                    gatewayHost,
                                    persistencePort,
                                    authPort,
                                    imagePort,
                                    recommenderPort
                            ));
                        }
                    });
            bindAndSync(bootstrap);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
