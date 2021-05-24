package com.nettysource.learn.test;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;

/**
 * @author lhy
 * @date 2021/5/24
 */
public class NettyServerHandler extends ChannelInboundHandlerAdapter {
    private int count=0;
    //读取数据事件
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String byteBuf= (String) msg;
        System.out.println("客户端发来消息:"+byteBuf);
        ctx.writeAndFlush(Unpooled.copiedBuffer("同好"+System.getProperty("line.separator"), CharsetUtil.UTF_8));
    }
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Netty Server channelRegistered");
        ctx.fireChannelRegistered();
    }
    //读取数据完毕事件
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelReadComplete();
    }
    //异常发生回调
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}
