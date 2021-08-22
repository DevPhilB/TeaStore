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
package utilities.rest.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * HTTP/1.1 client for inter-service communication
 * @author Philipp Backes
 */
public class Http1Client {

    private final String host;
    private final Integer port;
    private final HttpRequest httpRequest;
    private static final Logger LOG = LogManager.getLogger(Http1Client.class);

    public Http1Client(String host, Integer port, HttpRequest httpRequest) {
        this.host = host;
        this.port = port;
        this.httpRequest = httpRequest;
    }

    public void sendRequest(Http1ClientHandler handler) {
        EventLoopGroup group = new NioEventLoopGroup(1);

        try {
            Bootstrap bootstrap = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel channel) throws Exception {
                    channel.pipeline().addFirst(new HttpClientCodec());
                    channel.pipeline().addLast(new HttpContentDecompressor());
                    channel.pipeline().addLast(handler);
                }
            });

            // Make the connection attempt
            Channel channel = bootstrap.connect(host, port).sync().channel();
            LOG.info("Connected to [" + host + ':' + port + ']');
            // Send the HTTP/1.1 request
            channel.writeAndFlush(httpRequest);
            // Wait until the connection is closed
            channel.closeFuture().sync();
        } catch(Exception e) {
            LOG.error(e.getMessage());
        } finally {
            group.shutdownGracefully();
        }
    }
}
