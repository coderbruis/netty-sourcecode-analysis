package io.netty.channel.mytest.pipeline;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.AttributeKey;

/**
 * @author lhy
 * @date 2021/6/14
 */
public class ServerTest {

    /**
     * channelPipeline是高级过滤器的实现，netty将channel中的数据导入到了channelPipeline中，
     * 为了让用户能够对channel的数据进行操作。
     * 1. channelPipeline是一个双向链表的数据结构，链表节点是channelContext；
     * 2. channelPipeline能够让客户往channel写数据，或者从channel里读数据；
     *
     *
     */

    public static void main(String[] args) throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup  = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childOption(ChannelOption.TCP_NODELAY,true)
                    .childAttr(AttributeKey.newInstance("childAttr"), "childValue")
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new InBoundHandlerA());
                            ch.pipeline().addLast(new InBoundHandlerB());
                            ch.pipeline().addLast(new InBoundHandlerC());
                            ch.pipeline().addLast(new OutBoundHandlerA());
                            ch.pipeline().addLast(new OutBoundHandlerB());
                            ch.pipeline().addLast(new OutBoundHandlerC());
                        }
                    });

            ChannelFuture future = bootstrap.bind(8888).sync();

            future.channel().closeFuture().sync();
        } catch (Exception e) {
            throw e;
        } finally {

        }
    }

}
