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

import static org.jboss.aerogear.simplepush.protocol.impl.json.JsonUtil.toJson;
import static org.jboss.aerogear.simplepush.util.UUIDUtil.newUAID;
import static org.jboss.aerogear.simplepush.server.netty.performance.ArgsUtil.*;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.net.URI;
import java.util.concurrent.CountDownLatch;

import org.jboss.aerogear.simplepush.protocol.impl.HelloMessageImpl;

/**
 * Performs 'hello' handshakes with the SimplePush Server using the number of threads configured.
 */
public class HelloOperation extends Operation {

    private final int warmupOperations;
    private boolean warmupDone;

    public HelloOperation(final URI uri, final CountDownLatch startLatch, final CountDownLatch endLatch,
            final int warmupOperations) {
        super(uri, startLatch, endLatch);
        this.warmupOperations = warmupOperations;
    }

    public static void perform(final URI uri, final int threads, final int warmOperations) throws Exception {
        Runner.withUri(uri).threads(threads).helloOperation().warmupOperations(warmOperations).perform();
    }

    public static void main(final String args[]) throws Exception {
        perform(firstArg(args, DEFAULT_URL), secondArg(args, DEFAULT_OPERATIONS), thirdArg(args, DEFAULT_WARMUP_OPERATIONS));
    }

    @Override
    public void run() {
        if (warmupOperations > 0 && !warmupDone) {
            for (int i = 0; i < warmupOperations; i++) {
                performHello();
            }
            countDownEndLatch();
            warmupDone = true;
        } else {
            performHello();;
            countDownEndLatch();
        }
    }

    private void performHello() {
        NettyConnection con = null;
        try {
            con = rawWebSocketConnect();
            waitForAllClientThreads();
            sendHello(newUAID(), con).release();
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            NettyConnection.shutdown(con);
        }
    }

    public static TextWebSocketFrame helloFrame(final String uaid) {
        return new TextWebSocketFrame(toJson(new HelloMessageImpl(uaid)));
    }

}
