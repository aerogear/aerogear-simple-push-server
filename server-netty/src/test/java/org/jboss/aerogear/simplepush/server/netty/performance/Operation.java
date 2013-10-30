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

import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.net.URI;
import java.util.concurrent.CountDownLatch;

import org.jboss.aerogear.simplepush.protocol.impl.RegisterResponseImpl;
import org.jboss.aerogear.simplepush.protocol.impl.json.JsonUtil;

/**
 * An Operation represents one or a set of operations defined in the
 * SimplePush specifiction.
 * </p>
 * An example of a single operation might be to send the handshake 'hello' message.
 * An example of a compound operation might be to send the 'register' message which requires
 * that the 'hello' message as already been sent.
 */
public abstract class Operation implements Runnable {

    public static final int DEFAULT_THREADS = 5;
    public static final int DEFAULT_OPERATIONS = 100;
    public static final int DEFAULT_WARMUP_OPERATIONS = 100;
    public static final String DEFAULT_URL = "ws://localhost:7777/simplepush/websocket";

    private final URI uri;
    private final CountDownLatch startLatch;
    private final CountDownLatch endLatch;

    /**
     * Sole-constructor.
     *
     * @param uri the URI of the SimplePush Server to use.
     * @param startLatch a {@link CountDownLatch} that enables the operation to wait until a chosen point.
     *                   For example, one might want to wait until all clients have made their connections
     *                   to the server before proceeding so that request are concurrently executed.
     * @param endLatch a {@link CountDownLatch} that enables the operation to signal that it has completed
     *                   a single operation by calling countDown.
     */
    public Operation(final URI uri, final CountDownLatch startLatch, final CountDownLatch endLatch) {
        this.uri = uri;
        this.startLatch = startLatch;
        this.endLatch = endLatch;
    }

    public URI uri() {
        return uri;
    }

    /**
     * Will wait until the startLatch has counted down, which is done when all
     * the client threads have been started. This enables all threads to be run
     * more or less at the same time.
     * @throws InterruptedException
     */
    protected void waitForAllClientThreads() throws InterruptedException {
        startLatch.await();
    }

    protected void countDownEndLatch() {
        endLatch.countDown();
    }

    /**
     * The run method should perform the actual operation against the SimplePush Server.
     */
    @Override
    public abstract void run();

    public NettyConnection rawWebSocketConnect() throws Exception {
        return NettyConnection.rawWebSocketConnect(uri);
    }

    public TextWebSocketFrame sendHello(final String uaid, final NettyConnection con) throws Exception {
        con.channel().writeAndFlush(HelloOperation.helloFrame(uaid)).sync();
        return con.handler().getTextFrame();
    }

    public RegisterResponseImpl sendRegistration(final String channelId, final NettyConnection con) throws Exception {
        final ChannelFuture registerFuture = con.channel().writeAndFlush(RegistrationOperation.registerFrame(channelId));
        registerFuture.sync();
        final TextWebSocketFrame textFrame = con.handler().getTextFrame();
        return JsonUtil.fromJson(textFrame.text(), RegisterResponseImpl.class);
    }

}
