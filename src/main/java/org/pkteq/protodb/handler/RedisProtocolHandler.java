package org.pkteq.protodb.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.pkteq.protodb.core.Eval;
import org.pkteq.protodb.core.RedisCmd;
import org.pkteq.protodb.core.Resp;

import java.util.List;

public class RedisProtocolHandler extends ChannelInboundHandlerAdapter {
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