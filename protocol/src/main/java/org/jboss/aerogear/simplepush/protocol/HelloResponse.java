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
package org.jboss.aerogear.simplepush.protocol;


/**
 * Represents the handshake response message, 'hello' message type, in the
 * <a href="https://wiki.mozilla.org/WebAPI/SimplePush/Protocol">SimplePush specification protocol</a>.
 * This is sent as the response to a 'hello' message.
 *
 */
public interface HelloResponse extends MessageType {

    String UAID_FIELD = "uaid";

    /**
     * A globally unique identifier for a UserAgent created by the SimplePush Server.
     *
     * @return {@code String} a globally unique id for a UserAgent, or an empty String if the UserAgent has not
     * been assigned a uaid yet or wants to reset it, which will create a new one.
     */
    String getUAID();

}
