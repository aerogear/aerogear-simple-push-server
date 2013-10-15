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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Runner "drives" performance tests but controlling warmup operations and executing the
 * main {@link Operation} configured using the {@link OperationFactory} supplied.
 */
public class Runner {
    private final URI uri;
    private final int threads;
    private final int operations;
    private final int warmupOperations;
    private final OperationFactory factory;

    public Runner(final Builder builder) {
        this.uri = builder.uri;
        this.threads = builder.threads;
        this.operations = builder.operations;
        this.warmupOperations = builder.warmupOperations;
        this.factory = builder.factory;
    }

    public void perform() throws InterruptedException {
        performWarmupOperations();
        performOperation();
    }

    private void performWarmupOperations() throws InterruptedException {
        if (warmupOperations > 0) {
            System.out.println("Starting warmup of " + warmupOperations + " operations");
            final CountDownLatch warmupStartLatch = new CountDownLatch(1);
            final CountDownLatch warmupEnd = new CountDownLatch(1);
            new Thread(factory.create(uri, warmupStartLatch, warmupEnd, warmupOperations)).start();
            warmupStartLatch.countDown();
            warmupEnd.await();
            System.out.println("Warmup done.");
        }
    }

    private void performOperation() throws InterruptedException {
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch endLatch = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            new Thread(factory.create(uri, startLatch, endLatch, operations)).start();
        }
        final long startTime = System.nanoTime();
        startLatch.countDown();
        endLatch.await();
        long elapsedTime = System.nanoTime() - startTime;
        long millis = TimeUnit.NANOSECONDS.toMillis(elapsedTime);
        if (operations != 0) {
            System.out.println(millis + " ms for " + operations * threads + " operations using " + threads + " threads");
            System.out.println(millis/operations + " ms (" + elapsedTime/operations + " ns) per operations");
        } else {
            System.out.println(millis + " ms for " + threads + " operations");
            System.out.println(millis/threads + " ms (" + elapsedTime/threads + " ns) per operations");
        }
    }

    public static Builder withUri(final URI uri) {
        return new Builder(uri);
    }

    public static class Builder {

        private final URI uri;
        private int threads;
        private int operations;
        private int warmupOperations;
        private OperationFactory factory;

        public Builder(final URI uri) {
            this.uri = uri;
        }

        public Builder threads(final int threads) {
            this.threads = threads;
            return this;
        }

        public Builder operations(final int operations) {
            this.operations = operations;
            return this;
        }

        public Builder warmupOperations(final int operations) {
            warmupOperations = operations;
            return this;
        }

        public Builder helloOperation() {
            this.factory = new OperationFactory() {
                @Override
                public Operation create(final URI uri, final CountDownLatch startLatch, final CountDownLatch endLatch, final int operations) {
                    return new HelloOperation(uri, startLatch, endLatch, operations);
                };
            };
            return this;
        }

        public Builder notificationOperation() {
            this.factory = new OperationFactory() {
                @Override
                public Operation create(final URI uri, final CountDownLatch startLatch, final CountDownLatch endLatch, final int operations) {
                    return new NotificationOperation(uri, startLatch, endLatch, operations);
                };
            };
            return this;
        }

        public Builder registrationOperation() {
            this.factory = new OperationFactory() {
                @Override
                public Operation create(URI uri, CountDownLatch startLatch, CountDownLatch endLatch, int operations) {
                    return new RegistrationOperation(uri, startLatch, endLatch, operations);
                }
            };
            return this;
        }

        public Builder unRegisterOperation() {
            this.factory = new OperationFactory() {
                @Override
                public Operation create(URI uri, CountDownLatch startLatch, CountDownLatch endLatch, int operations) {
                    return new UnregisterOperation(uri, startLatch, endLatch, operations);
                }
            };
            return this;
        }

        public Builder ackOperation() {
            this.factory = new OperationFactory() {
                @Override
                public Operation create(URI uri, CountDownLatch startLatch, CountDownLatch endLatch, int operations) {
                    return new AckOperation(uri, startLatch, endLatch, operations);
                }
            };
            return this;
        }

        public Builder operationFactory(final OperationFactory factory) {
            this.factory = factory;
            return this;
        }

        public Runner build() {
            return new Runner(this);
        }

        public void perform() throws Exception {
            new Runner(this).perform();
        }

    }

}
