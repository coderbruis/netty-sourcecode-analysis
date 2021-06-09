package io.netty.channel.mytest;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.AttributeKey;
import org.junit.Test;

/**
 * @author lhy
 * @date 2021/6/6
 */
public class ServerTest {


    /**
     * 启动Server端
     */
    @Test
    public void bootstrapServer() throws Exception {

        /**
         * NioEventLoopGroup其实就是一个线程组，存放NioEventLoop的一个Group
         * 每个NioEventLoop都喝一个Selector绑定？？？？？？？？？？？？？？？？？？
         *
         */
        NioEventLoopGroup boosGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            // ServerBootStrap父类是AbstractBootStrap
            // AbstractBootStrap主要存的是boosGroup，而ServerBootStrap自身存workerGroup
            bootstrap.group(boosGroup, workerGroup)
                    // 给ServerBootStrap的channel属性channelFactory赋值，赋值为ReflectiveChannelFactory，用于反射出NioServerSocketChannel服务端channel
                    .channel(NioServerSocketChannel.class)
                    // channel的属性
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childAttr(AttributeKey.newInstance("childAttr"), "childAttrValue")
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
//                            ch.pipeline().addLast()
                        ch.pipeline().addLast(new NettyServerHandler());
                        }
                    });

            ChannelFuture future = bootstrap.bind(8888).sync();
            future.channel().closeFuture().sync();
        } finally {
            boosGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
