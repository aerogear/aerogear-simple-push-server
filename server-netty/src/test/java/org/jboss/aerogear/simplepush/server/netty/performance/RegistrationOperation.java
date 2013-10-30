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
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.net.URI;
import java.util.concurrent.CountDownLatch;

import org.jboss.aerogear.simplepush.protocol.impl.RegisterMessageImpl;
import org.jboss.aerogear.simplepush.protocol.impl.json.JsonUtil;

/**
 * Performs a 'hello' handshake followed by multiple channel 'registration's.
 */
public class RegistrationOperation extends Operation {

    private final int operations;

    public RegistrationOperation(final URI uri, final CountDownLatch startLatch, final CountDownLatch endLatch, final int operations) {
        super(uri, startLatch, endLatch);
        this.operations = operations;
    }

    public static void perform(final URI uri, final int threads, int operations, final int warmupOperations) throws Exception {
        Runner.withUri(uri).threads(threads).operations(operations).warmupOperations(warmupOperations).registrationOperation().perform();
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
            sendRegistrations(con);
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            countDownEndLatch();
            NettyConnection.shutdown(con);
        }
    }

    private void sendRegistrations(final NettyConnection con) throws Exception {
        for (long i = 1; i <= operations; i++) {
            sendRegistration(randomUUID().toString(), con);
        }
    }

    public static TextWebSocketFrame registerFrame(final String channelId) {
        return new TextWebSocketFrame(JsonUtil.toJson(new RegisterMessageImpl(channelId)));
    }

}
