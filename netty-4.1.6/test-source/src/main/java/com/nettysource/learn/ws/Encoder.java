package com.nettysource.learn.ws;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

/**
 * ---------------------
 *|   4    |  4  |  ?   |
 * ---------------------
 *| length | age | name |
 * ---------------------
 */



public class Encoder extends MessageToByteEncoder<TextWebSocketFrame> {
    @Override
    protected void encode(ChannelHandlerContext ctx, TextWebSocketFrame msg, ByteBuf out) throws Exception {
        byte[] bytes = msg.text().getBytes();
        out.writeBytes(bytes);
    }
    //    @Override
//    protected void encode(ChannelHandlerContext ctx, User user, ByteBuf out) throws Exception {
//
//        byte[] bytes = user.getName().getBytes();
//        out.writeInt(4 + bytes.length);
//        out.writeInt(user.getAge());
//        out.writeBytes(bytes);
//    }
}
