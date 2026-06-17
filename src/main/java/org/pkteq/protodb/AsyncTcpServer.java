package org.pkteq.protodb;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.pkteq.protodb.core.Store;
import org.pkteq.protodb.handler.RedisProtocolHandler;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class AsyncTcpServer {
    private static final int PORT = 5379;
    public static final Map<String, Object> STORE = new ConcurrentHashMap<>();

    static void main() throws Exception {
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
                     ch.eventLoop().schedule(AsyncTcpServer::deleteExpiredKeys, 1000, TimeUnit.MILLISECONDS);
                     ch.pipeline().addLast(new RedisProtocolHandler());
                 }
             })
             .option(ChannelOption.SO_BACKLOG, 128)
             .childOption(ChannelOption.SO_KEEPALIVE, true);

            System.out.println("ProtoDB Server starting on port " + PORT + "...");
            ChannelFuture f = b.bind(PORT).sync();

            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
    public static void deleteExpiredKeys() {
        // Run until we have a low percentage of expired keys
        while (true) {
            double expiredRatio = expireSample();
            // If less than 25% of sampled keys were expired, we stop for this cycle
            if (expiredRatio < 0.25) break;
            
            // Safety break to prevent spinning too long if there are millions of keys
        }
    }

    public static double expireSample() {
        int sampleLimit = 20;
        int checkedCount = 0;
        int expiredCount = 0;

        if (STORE.isEmpty()) return 0;

        // Redis-style expiration sampling
        // We iterate over the entry set. In a real system, we'd pick random keys.
        // For this implementation, we'll use the iterator but limit the scan.
        Iterator<Map.Entry<String, Object>> iterator = STORE.entrySet().iterator();

        while (iterator.hasNext() && checkedCount < sampleLimit) {
            Map.Entry<String, Object> entry = iterator.next();
            checkedCount++;

            Store.Obj obj = (Store.Obj) entry.getValue();
            if (obj.expiresAt() != -1) {
                if (obj.expiresAt() <= System.currentTimeMillis()) {
                    iterator.remove(); // Use iterator.remove() for safe deletion
                    expiredCount++;
                }
            }
        }

        if (checkedCount == 0) return 0;
        return (double) expiredCount / checkedCount;
    }
}
