package org.pkteq.protodb.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;

public class NettyTcpServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // Fires once when the connection is established
        ctx.writeAndFlush(Unpooled.copiedBuffer(
                "Welcome! Type a message. Type 'quit' to disconnect.\n",
                CharsetUtil.UTF_8));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf  = (ByteBuf) msg;
        String  line = buf.toString(CharsetUtil.UTF_8).stripTrailing();
        buf.release();  // always release incoming ByteBuf

        System.out.println("Received [" + ctx.channel().remoteAddress() + "]: " + line);

        if ("quit".equalsIgnoreCase(line)) {
            ctx.writeAndFlush(Unpooled.copiedBuffer("Goodbye!\n", CharsetUtil.UTF_8))
                    .addListener(ChannelFutureListener.CLOSE);  // close after write completes
            return;
        }

        // Echo back upper-cased
        String response = "Echo: " + line.toUpperCase() + "\n";
        ctx.writeAndFlush(Unpooled.copiedBuffer(response, CharsetUtil.UTF_8));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("Client disconnected: " + ctx.channel().remoteAddress());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.err.println("Error [" + ctx.channel().remoteAddress() + "]: " + cause.getMessage());
        ctx.close();
    }
}