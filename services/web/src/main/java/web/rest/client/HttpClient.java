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
package web.rest.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpRequest;

/**
 * HTTP client for web service
 * @author Philipp Backes
 */
public class HttpClient {

    private final String host;
    private final Integer port;
    private final HttpRequest httpRequest;

    public HttpClient(String host, Integer port, HttpRequest httpRequest) {
        this.host = host;
        this.port = port;
        this.httpRequest = httpRequest;
    }

    public void sendRequest(HttpClientHandler handler) {
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap()
                .group(workerGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel channel) throws Exception {
                    channel.pipeline().addLast(new HttpClientCodec());
                    channel.pipeline().addLast(new HttpContentDecompressor());
                    channel.pipeline().addLast(handler);
                }
            });

            // Make the connection attempt
            Channel channel = bootstrap.connect(host, port).sync().channel();
            // Send the HTTP request
            channel.writeAndFlush(httpRequest);
            // Wait until the connection is closed
            channel.closeFuture().sync();
        } catch(InterruptedException e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
        }
    }
}
