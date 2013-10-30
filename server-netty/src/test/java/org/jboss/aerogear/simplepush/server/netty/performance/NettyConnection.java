/**
 * JBoss, Home of Professional Open Source Copyright Red Hat, Inc., and individual contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.jboss.aerogear.simplepush.server.netty.performance;

import java.net.URI;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;

import org.jboss.aerogear.simplepush.server.netty.WebSocketClientHandler;

/**
 * A container for Netty connection related instances.
 */
public class NettyConnection {

    private final Channel channel;
    private final WebSocketClientHandler handler;
    private final EventLoopGroup group;

    public NettyConnection(final Channel channel, final WebSocketClientHandler handler, final EventLoopGroup group) {
        this.channel = channel;
        this.handler = handler;
        this.group = group;
    }

    public Channel channel() {
        return channel;
    }

    public WebSocketClientHandler handler() {
        return handler;
    }

    public EventLoopGroup group() {
        return group;
    }

    public static NettyConnection rawWebSocketConnect(final URI uri) throws Exception {
        final EventLoopGroup group = new NioEventLoopGroup();
        final Bootstrap b = new Bootstrap();
        final HttpHeaders customHeaders = new DefaultHttpHeaders();
        final WebSocketClientHandler handler = new WebSocketClientHandler(
                    WebSocketClientHandshakerFactory.newHandshaker(uri, WebSocketVersion.V13, null, false, customHeaders));
            b.group(group)
            .channel(NioSocketChannel.class)
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast("http-codec", new HttpClientCodec());
                    pipeline.addLast("aggregator", new HttpObjectAggregator(8192));
                    pipeline.addLast("ws-handler", handler);
                }
            });
        final Channel ch = b.connect(uri.getHost(), uri.getPort()).sync().channel();
        handler.handshakeFuture().sync();
        return new NettyConnection(ch, handler, group);
    }

    public static void sendClose(final NettyConnection con) {
        try {
            con.channel().writeAndFlush(new CloseWebSocketFrame());
            con.channel().closeFuture().sync();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public static void shutdown(final NettyConnection con) {
        if (con != null) {
            sendClose(con);
            con.group().shutdownGracefully();
        }
    }

}
