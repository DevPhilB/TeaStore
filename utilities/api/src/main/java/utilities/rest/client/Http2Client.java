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
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http2.*;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.codec.http.HttpRequest;

/**
 * HTTP/2 client for inter-service communication
 * @author Philipp Backes
 */
public class Http2Client {

    private final String host;
    private final Integer port;
    private final HttpRequest httpRequest;

    public Http2Client(String host, Integer port, HttpRequest httpRequest) {
        this.host = host;
        this.port = port;
        this.httpRequest = httpRequest;
    }

    public void sendRequest(Http2ClientStreamFrameHandler handler) {
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            // Configure SSL.
            final SslContext sslCtx;
            final SslProvider provider =
                    SslProvider.isAlpnSupported(SslProvider.OPENSSL)? SslProvider.OPENSSL : SslProvider.JDK;
            sslCtx = SslContextBuilder.forClient()
                    .sslProvider(provider)
                    .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                    // TODO: Need to be changed for production
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .applicationProtocolConfig(new ApplicationProtocolConfig(
                            Protocol.ALPN,
                            SelectorFailureBehavior.NO_ADVERTISE,
                            SelectedListenerFailureBehavior.ACCEPT,
                            ApplicationProtocolNames.HTTP_2,
                            ApplicationProtocolNames.HTTP_1_1))
                    .build();

            final Http2FrameCodec http2FrameCodec = Http2FrameCodecBuilder.forClient()
                    .initialSettings(Http2Settings.defaultSettings())
                    .build();

            Bootstrap bootstrap = new Bootstrap()
                .group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .remoteAddress(host, port)
                .handler(new ChannelInitializer<Channel> () {
                @Override
                public void initChannel(Channel channel) throws Exception {
                    channel.pipeline().addFirst(sslCtx.newHandler(channel.alloc()));
                    channel.pipeline().addLast(http2FrameCodec);
                    channel.pipeline().addLast(handler);
                }
            });

            // Start the client
            final Channel channel = bootstrap.connect().syncUninterruptibly().channel();
            System.out.println("Connected to [" + host + ':' + port + ']');
            // Send the HTTP/2 request
            channel.writeAndFlush(httpRequest);
            // Wait until the connection is closed
            channel.close().syncUninterruptibly();
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
        }
    }
}
