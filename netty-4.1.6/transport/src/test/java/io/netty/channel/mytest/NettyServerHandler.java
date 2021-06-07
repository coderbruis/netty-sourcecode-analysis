package io.netty.channel.mytest;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;

/**
 * @author lhy
 * @date 2021/6/7
 */
public class NettyServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("当前线程: " + Thread.currentThread().getName());
        // 用户自定义的普通任务
        ctx.channel()
                .eventLoop()
                .execute(new Runnable() {
                    /**
                     * NioEventLoop执行在执行runAllTasks的时候，就会执行此处的runnable
                     */
                    @Override
                    public void run() {
                        // 模拟一个耗时的操作
                        try {
                            Thread.sleep(5 * 1000);
                            ctx.writeAndFlush(Unpooled.copiedBuffer("hello, 客户端~(>^ω^<)喵2", CharsetUtil.UTF_8));
                            System.out.println("channel code=" + ctx.channel().hashCode());
                            System.out.println("线程：" + Thread.currentThread().getName());
                        } catch (Exception e) {
                            System.err.println("发生异常：" + e.getMessage());
                        }
                    }
                });
    }
}
