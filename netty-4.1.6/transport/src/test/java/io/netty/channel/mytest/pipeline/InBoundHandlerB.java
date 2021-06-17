package io.netty.channel.mytest.pipeline;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @author
 */
public class InBoundHandlerB extends ChannelInboundHandlerAdapter {

    /**
     * TODO 这里ChannelHandlerContext#fireChannelRead 和 ChannelPipeline#fireChannelRead有什么区别？
     * 1.   对于ChannelHandlerContext#fireChannelRead，是从当前节点继续往下传播channelRead事件
     * 2.   对于ChannelPipeline#fireChannelRead，是从头结点开始往后传播事件
     *
     */

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("InBoundHandlerB: " + msg);
        ctx.fireChannelRead(msg);
    }

    /**
     * 通道触发时调用
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().pipeline().fireChannelRead("hello world!");
    }
}
