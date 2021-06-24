package web.rest.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpRequest;

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
