package web.rest.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class HttpClient {

    private final String httpVersion;
    private final String host;
    private final int port;

    public HttpClient(String httpVersion, String host, int port) {
        this.httpVersion = httpVersion;
        this.host = host;
        this.port = port;
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
                    channel.pipeline().addLast(handler);
                }
            });

            // Start the client
            ChannelFuture future = bootstrap.connect(host, port).sync();
            // Wait until the connection is closed
            future.channel().closeFuture().sync();
        } catch(InterruptedException e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        String host = "http:///api/web/isready";
        HttpClient client = new HttpClient("HTTP/1.1", host, 80);
        HttpClientHandler handler = new HttpClientHandler();
        client.sendRequest(handler);
    }
}
