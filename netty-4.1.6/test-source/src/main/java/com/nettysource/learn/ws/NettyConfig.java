package com.nettysource.learn.ws;

import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * Netty全局配置
 *
 * @author lhy
 * @date 2021/6/24
 */
public class NettyConfig {

    /**
     * 存储每一个客户端链接进来是产生的channel对象
     */
    public static ChannelGroup group  = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

}
