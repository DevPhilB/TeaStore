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
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.incubator.codec.http3.*;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicSslContext;
import io.netty.incubator.codec.quic.QuicSslContextBuilder;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * HTTP/3 client for inter-service communication
 * @author Philipp Backes
 */
public class Http3Client {

    private final String host;
    private final Integer port;
    private final Http3HeadersFrame header;
    private final Http3DataFrame body;
    private static final Logger LOG = LogManager.getLogger(Http3Client.class);

    public Http3Client(String host, Integer port, Http3HeadersFrame header, Http3DataFrame body) {
        this.host = host;
        this.port = port;
        this.header = header;
        this.body = body;
    }

    public void sendRequest(Http3ClientStreamInboundHandler handler) {
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            // Configure QUIC SSL
            QuicSslContext context = QuicSslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .applicationProtocols(Http3.supportedApplicationProtocols()).build();
            ChannelHandler codec = Http3.newQuicClientCodecBuilder()
                    .sslContext(context)
                    .maxIdleTimeout(5000, TimeUnit.MILLISECONDS)
                    .initialMaxData(10000000)
                    .initialMaxStreamDataBidirectionalLocal(1000000)
                    .build();
            // Configure the client
            Bootstrap bootstrap = new Bootstrap();
            Channel channel = bootstrap.group(group)
                    .channel(NioDatagramChannel.class)
                    .handler(codec)
                    .bind(0).sync().channel();
            // Start the client
            QuicChannel quicChannel = QuicChannel.newBootstrap(channel)
                    .handler(new Http3ClientConnectionHandler())
                    .remoteAddress(new InetSocketAddress(host, port))
                    .connect()
                    .get();
            LOG.info("Connected to [" + host + ':' + port + ']');
            // Prepare request stream
            QuicStreamChannel streamChannel = Http3.newRequestStream(quicChannel, handler).sync().getNow();
            // Write header (and body
            if (body != null) {
                streamChannel.write(header).sync();
                streamChannel.writeAndFlush(body).addListener(QuicStreamChannel.SHUTDOWN_OUTPUT).sync();
            } else {
                streamChannel.writeAndFlush(header).addListener(QuicStreamChannel.SHUTDOWN_OUTPUT).sync();
            }
            // Wait for the stream and QUIC channel to be closed (after FIN)
            // Close the underlying datagram channel
            streamChannel.closeFuture().sync();
            // After we received the response lets also close the underlying QUIC and datagram channel
            quicChannel.close().sync();
            channel.close().sync();
        } catch(Exception e) {
            LOG.error(e.getMessage());
        } finally {
            group.shutdownGracefully();
        }
    }
}
