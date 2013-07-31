/**
 * JBoss, Home of Professional Open Source
 * Copyright Red Hat, Inc., and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aerogear.simplepush.server.netty;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a UserAgent in the SimplePush Server.
 * @param <T>
 */
public class UserAgent<T> {

    private final String uaid;
    private final T transport;
    private final AtomicLong timestamp;

    /**
     * Sole constructor.
     *
     * @param uaid the unique identifier for this UserAgent.
     * @param transport the transport type that this UserAgent uses.
     * @param timestamp the time this UserAgent was created.
     */
    public UserAgent(final String uaid, final T transport, final long timestamp) {
        this.uaid = uaid;
        this.transport = transport;
        this.timestamp = new AtomicLong(timestamp);
    }

    public String uaid() {
        return uaid;
    }

    public T context() {
        return transport;
    }

    public long timestamp() {
        return timestamp.get();
    }

    public void timestamp(final long timestamp) {
        this.timestamp.set(timestamp);
    }

    @Override
    public String toString() {
        return "UserAgent[uaid=" + uaid + ", transport=" + transport + ", timestamp=" + timestamp() + "]";
    }

}
