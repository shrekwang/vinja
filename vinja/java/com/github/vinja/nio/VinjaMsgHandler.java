package com.github.vinja.nio;

import com.github.vinja.util.JdeLogger;
import com.github.vinja.nio.task.CompileCtxTask;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.ByteBuf;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;


@Sharable 
public class VinjaMsgHandler extends ChannelInboundHandlerAdapter {

    private ExecutorService taskExecutor = Executors.newFixedThreadPool(10);

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf byteBuf = (ByteBuf) msg;
        String json = "{}";

        if (byteBuf.hasArray()) {
            json = new String(byteBuf.array());
        } else {
            int length = byteBuf.readableBytes();
            byte[] bytes = new byte[length];
            byteBuf.getBytes(byteBuf.readerIndex(), bytes);
            json = new String(bytes);
        }
        JdeLogger.getLogger("VinjaMsgHandler").info("get message:" + json);

        JSONObject jsonObj = JSONObject.parseObject(json);
        CompileCtxTask task = new CompileCtxTask(ctx, jsonObj);
        taskExecutor.submit(task);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        System.out.println("ctx read complete");
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        //cause.printStackTrace();
        //ctx.close();
    }
}
