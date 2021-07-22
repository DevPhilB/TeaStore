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
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http2.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.*;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * HTTP/2 client for inter-service communication
 * @author Philipp Backes
 */
public class Http2Client {

    private final String host;
    private final Integer port;
    private final Http2Headers header;
    private final Http2DataFrame body;
    private static final Logger LOG = LogManager.getLogger();

    public Http2Client(String host, Integer port, Http2Headers header, Http2DataFrame body) {
        this.host = host;
        this.port = port;
        this.header = header;
        this.body = body;
    }

    public void sendRequest(Http2ClientStreamFrameHandler handler) {
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            // Configure SSL
            SslContext sslCtx = SslContextBuilder.forClient()
                    .sslProvider(SslProvider.JDK)
                    .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                    // Need to be changed for production
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .applicationProtocolConfig(new ApplicationProtocolConfig(
                            ApplicationProtocolConfig.Protocol.ALPN,
                            ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                            ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                            ApplicationProtocolNames.HTTP_2))
                    .build();

            // Configure the client
            Bootstrap bootstrap = new Bootstrap()
                .group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.AUTO_CLOSE, true)
                .remoteAddress(host, port)
                .handler(new ChannelInitializer<Channel> () {
                @Override
                public void initChannel(Channel channel) {
                    channel.pipeline().addFirst(sslCtx.newHandler(channel.alloc()));
                    channel.pipeline().addLast(Http2FrameCodecBuilder.forClient()
                            .initialSettings(Http2Settings.defaultSettings())
                            .build());
                    channel.pipeline().addLast(new Http2MultiplexHandler(handler));
                    channel.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
                }
            });
            // Start the client
            Channel channel = bootstrap.connect().syncUninterruptibly().channel();
            LOG.info("Connected to [" + host + ':' + port + ']');
            // Prepare handler
            Http2StreamChannelBootstrap streamChannelBootstrap = new Http2StreamChannelBootstrap(channel);
            Http2StreamChannel streamChannel = streamChannelBootstrap.open().syncUninterruptibly().getNow();
            streamChannel.pipeline().addLast(handler);
            // Send HTTP/2 request
            if (body != null) {
                channel.write(header);
                channel.writeAndFlush(body);
            } else {
                channel.writeAndFlush(header);
            }
            // Wait until the connection is closed
            channel.close().sync();
        } catch(Exception e) {
            LOG.error(e.getMessage());
        } finally {
            workerGroup.shutdownGracefully();
        }
    }
}
