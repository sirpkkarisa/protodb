package org.pkteq.protodb;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import org.pkteq.protodb.core.RedisCmd;
import org.pkteq.protodb.core.Resp;
import org.pkteq.protodb.core.Eval;

import java.util.List;

public class NettySyncTcpServer {
    private static final int PORT = 5379;

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

    private static class RedisProtocolHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            ByteBuf in = (ByteBuf) msg;
            try {
                ByteBufInputStream inputStream = new ByteBufInputStream(in);
                List<?> response = Resp.decodeStream(inputStream);

                if (response != null && !response.isEmpty()) {
                    List<?> cmdList = (List<?>) response.getFirst();
                    String name = (String) cmdList.getFirst();
                    List<?> args = cmdList.subList(1, cmdList.size());
                    RedisCmd redisCmd = new RedisCmd(name, args);

//                    System.out.println("Netty Executing: " + redisCmd);
                    
                    String result = Eval.eval(redisCmd);
                    ctx.writeAndFlush(Unpooled.copiedBuffer(result.getBytes()));
                }
            } finally {
                in.release();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }
}
