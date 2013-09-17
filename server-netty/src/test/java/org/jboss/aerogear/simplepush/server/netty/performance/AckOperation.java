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

import static java.util.UUID.randomUUID;
import static org.jboss.aerogear.simplepush.server.netty.performance.ArgsUtil.firstArg;
import static org.jboss.aerogear.simplepush.server.netty.performance.ArgsUtil.fourthArg;
import static org.jboss.aerogear.simplepush.server.netty.performance.ArgsUtil.secondArg;
import static org.jboss.aerogear.simplepush.server.netty.performance.ArgsUtil.thirdArg;
import static org.jboss.aerogear.simplepush.util.UUIDUtil.newUAID;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.jboss.aerogear.simplepush.protocol.RegisterResponse;
import org.jboss.aerogear.simplepush.protocol.Update;
import org.jboss.aerogear.simplepush.protocol.impl.AckMessageImpl;
import org.jboss.aerogear.simplepush.protocol.impl.RegisterResponseImpl;
import org.jboss.aerogear.simplepush.protocol.impl.UpdateImpl;
import org.jboss.aerogear.simplepush.protocol.impl.json.JsonUtil;

/**
 * Performs a 'hello', a channel 'registration', multiple 'notification's and 'ack's.
 */
public class AckOperation extends Operation {

    private final int operations;

    public AckOperation(final URI uri, final CountDownLatch startLatch, final CountDownLatch endLatch, final int operations) {
        super(uri, startLatch, endLatch);
        this.operations = operations;
    }

    public static void perform(final URI uri, final int threads, final int operations, final int warmupOperations) throws Exception {
        Runner.withUri(uri).threads(threads).operations(operations).warmupOperations(warmupOperations).ackOperation().perform();
    }

    public static void main(final String args[]) throws Exception {
        perform(firstArg(args, DEFAULT_URL), secondArg(args, DEFAULT_THREADS), thirdArg(args, DEFAULT_OPERATIONS), fourthArg(args, DEFAULT_WARMUP_OPERATIONS));
    }

    @Override
    public void run() {
        NettyConnection con = null;
        try {
            con = rawWebSocketConnect();
            sendHello(newUAID(), con).release();
            waitForAllClientThreads();
            final RegisterResponseImpl registerResponse = sendRegistration(randomUUID().toString(), con);
            final Set<Update> acks = sendNotifictions(registerResponse);
            sendAcks(acks, con);
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            countDownEndLatch();
            NettyConnection.shutdown(con);
        }
    }

    public static void sendAcks(final Set<Update> updates, final NettyConnection con) throws Exception {
        final ChannelFuture ackFuture = con.channel().writeAndFlush(ackFrame(updates));
        ackFuture.sync();
    }

    private Set<Update> sendNotifictions(final RegisterResponse registerResponse) throws Exception {
            final Set<Update> updates = new HashSet<Update>();
            for (long i = 1; i <= operations; i++) {
                final HttpURLConnection http = NotificationOperation.getHttpConnection(new URL(registerResponse.getPushEndpoint()));
                NotificationOperation.sendVersion(i, http);
                updates.add(new UpdateImpl(registerResponse.getChannelId(), i));
            }
            return updates;
    }

    public static TextWebSocketFrame ackFrame(final Set<Update> updates) {
        return new TextWebSocketFrame(JsonUtil.toJson(new AckMessageImpl(updates)));
    }

}
