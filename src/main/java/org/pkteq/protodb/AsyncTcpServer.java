package org.pkteq.protodb;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.pkteq.protodb.core.Store;
import org.pkteq.protodb.handler.RedisProtocolHandler;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AsyncTcpServer {
    private static final int PORT = 5379;

    static void main() throws Exception {
        IoHandlerFactory nioIoHandler = NioIoHandler.newFactory();
        EventLoopGroup bossGroup = new MultiThreadIoEventLoopGroup(1, nioIoHandler);

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup)
             .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) {
                     ch.pipeline().addLast(new RedisProtocolHandler());
                 }
             })
             .option(ChannelOption.SO_BACKLOG, 128)
             .childOption(ChannelOption.SO_KEEPALIVE, true);

            // Schedule globally once. Runs every 100ms.
            bossGroup.scheduleWithFixedDelay(AsyncTcpServer::deleteExpiredKeys, 100, 100, TimeUnit.MILLISECONDS);

            System.out.println("ProtoDB Server starting on port " + PORT + "...");
            ChannelFuture f = b.bind(PORT).sync();

            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
        }
    }

    public static void deleteExpiredKeys() {
        long startTime = System.currentTimeMillis();
        long maxDurationMs = 1; // Don't block the event loop for more than 1ms

        while (System.currentTimeMillis() - startTime < maxDurationMs) {
            double expiredRatio = expireSample();
            if (expiredRatio < 0.25) break;
        }
    }

    public static double expireSample() {
        int sampleLimit = 20;
        int checkedCount = 0;
        int expiredCount = 0;

        if (Store.isEmpty()) return 0;

        Iterator<Map.Entry<String, Store.Obj>> iterator = Store.entryIterator();

        while (iterator.hasNext() && checkedCount < sampleLimit) {
            Map.Entry<String, Store.Obj> entry = iterator.next();
            checkedCount++;

            Store.Obj obj = entry.getValue();
            if (obj.isExpired()) {
                iterator.remove(); 
                expiredCount++;
            }
        }

        if (checkedCount == 0) return 0;
        return (double) expiredCount / checkedCount;
    }
}
