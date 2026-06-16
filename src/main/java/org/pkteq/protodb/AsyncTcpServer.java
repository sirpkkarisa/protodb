package org.pkteq.protodb;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.pkteq.protodb.core.Store;
import org.pkteq.protodb.handler.RedisProtocolHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AsyncTcpServer {
    private static final int PORT = 5379;
    public static final Map<String, Object> STORE = new ConcurrentHashMap<>();

    static void main(String[] args) throws Exception {
        IoHandlerFactory nioIoHandler = NioIoHandler.newFactory();
        EventLoopGroup bossGroup = new MultiThreadIoEventLoopGroup(1,nioIoHandler);
        EventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(1,nioIoHandler);

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) {
                     ch.pipeline().addLast(new RedisProtocolHandler());
                 }
             })
             .option(ChannelOption.SO_BACKLOG, 128)
             .childOption(ChannelOption.SO_KEEPALIVE, true);

            System.out.println("Netty Server starting on port " + PORT + "...");
            ChannelFuture f = b.bind(PORT).sync();

            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
