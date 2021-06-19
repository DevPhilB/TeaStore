package web.rest.server;

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

public class HttpServer {

    private final String httpVersion;
    private final int port;
    private static final Logger logger = LogManager.getLogger(HttpServer.class);

    public HttpServer(String httpVersion, int port) {
        this.httpVersion = httpVersion;
        this.port = port;
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 80;
        if(args.length > 1) {
            new HttpServer(args[1], port).run();
        } else {
            new HttpServer("HTTP/1.1", port).run();
        }
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
                        channelPipeline.addLast(new HttpWebServiceHandler());
                    }
                });

            ChannelFuture future = bootstrap.bind(port).sync();
            System.err.println(httpVersion + " web service is available on " +
                    "http://127.0.0.1:" + port + "/api/web");

            future.channel().closeFuture().sync();

        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
