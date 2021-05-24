package com.nettysource.learn.test;


import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;

/**
 * @author lhy
 * @date 2021/5/24
 */
public class TestServer {
    public static void main(String[] args) throws InterruptedException {
        //就是一个死循环，不停地检测IO事件，处理IO事件，执行任务
        //创建一个线程组:接受客户端连接   主线程
        EventLoopGroup bossGroup=new NioEventLoopGroup(1);//cpu核心数*2
        //创建一个线程组:接受网络操作   工作线程
        EventLoopGroup workerGroup=new NioEventLoopGroup();  //cpu核心数*2
        //是服务端的一个启动辅助类，通过给他设置一系列参数来绑定端口启动服务
        ServerBootstrap serverBootstrap=new ServerBootstrap();
        // 我们需要两种类型的人干活，一个是老板，一个是工人，老板负责从外面接活，
        // 接到的活分配给工人干，放到这里，bossGroup的作用就是不断地accept到新的连接，将新的连接丢给workerGroup来处理
        serverBootstrap.group(bossGroup,workerGroup)
                //设置使用NioServerSocketChannel作为服务器通道的实现
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG,128) //设置线程队列中等待连接的个数
                .childOption(ChannelOption.SO_KEEPALIVE,true)//保持活动连接状态
                //表示一条新的连接进来之后，该怎么处理，也就是上面所说的，老板如何给工人配活
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                        nioSocketChannel.pipeline().addLast(new StringDecoder(),new NettyServerHandler());
                    }
                });
        System.out.println(".........server  init..........");
        // 这里就是真正的启动过程了，绑定9090端口，等待服务器启动完毕，才会进入下行代码
        ChannelFuture future = serverBootstrap.bind(9090).sync();
        System.out.println(".........server start..........");
        //等待服务端关闭socket
        future.channel().closeFuture().sync();
        // 关闭两组死循环
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
