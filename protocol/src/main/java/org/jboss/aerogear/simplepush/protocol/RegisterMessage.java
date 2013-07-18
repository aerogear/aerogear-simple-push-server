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
 * Represents the Register message, 'register' message type, in the 
 * <a href="https://wiki.mozilla.org/WebAPI/SimplePush/Protocol">SimplePush specification protocol</a>
 * 
 * This message is sent from the UserAgent to the PushServer to register for notifications using the 
 * channelId. The channelId is create by the UserAgent.
 *
 */
public interface RegisterMessage extends MessageType {

    String CHANNEL_ID_FIELD = "channelID";

    /**
     * Returns the channelId that was sent from the UserAgent.
     * 
     * @return {@code String} the channelId.
     */
    String getChannelId();

}
