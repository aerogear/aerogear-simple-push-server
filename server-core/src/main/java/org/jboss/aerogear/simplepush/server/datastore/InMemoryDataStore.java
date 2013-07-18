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
package org.jboss.aerogear.simplepush.server.datastore;

import static org.jboss.aerogear.simplepush.util.ArgumentUtil.checkNotNull;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jboss.aerogear.simplepush.protocol.Update;
import org.jboss.aerogear.simplepush.server.Channel;
import org.jboss.aerogear.simplepush.server.DefaultChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InMemoryDataStore implements DataStore {

    private final ConcurrentMap<String, Channel> channels = new ConcurrentHashMap<String, Channel>();
    private final ConcurrentMap<UUID, Set<Update>> notifiedChannels = new ConcurrentHashMap<UUID, Set<Update>>();
    private final Logger logger = LoggerFactory.getLogger(InMemoryDataStore.class);

    @Override
    public boolean saveChannel(final Channel ch) {
        checkNotNull(ch, "ch");
        final Channel previous = channels.putIfAbsent(ch.getChannelId(), new DefaultChannel(ch.getUAID(), ch.getChannelId(), ch.getPushEndpoint()));
        return previous == null;
    }

    @Override
    public boolean removeChannel(final String channelId) {
        checkNotNull(channelId, "channelId");
        final Channel channel = channels.remove(channelId);
        return channel != null;
    }

    @Override
    public Channel getChannel(final String channelId) {
        checkNotNull(channelId, "channelId");
        return channels.get(channelId);
    }

    @Override
    public void removeChannels(final UUID uaid) {
        checkNotNull(uaid, "uaid");
        for (Channel channel : channels.values()) {
            if (channel.getUAID().equals(uaid)) {
                channels.remove(channel.getChannelId());
                logger.info("Removing [" + channel.getChannelId() + "] for UserAgent [" + uaid + "]");
            }
        }
        notifiedChannels.remove(uaid);
    }

    @Override
    public void storeUpdates(final Set<Update> updates, final UUID uaid) {
        checkNotNull(uaid, "uaid");
        checkNotNull(updates, "updates");
        final Set<Update> newUpdates = Collections.newSetFromMap(new ConcurrentHashMap<Update, Boolean>());
        newUpdates.addAll(updates);
        while (true) {
            final Set<Update> currentUpdates = notifiedChannels.get(uaid);
            if (currentUpdates == null) {
                final Set<Update> previous = notifiedChannels.putIfAbsent(uaid, newUpdates);
                if (previous != null) {
                    newUpdates.addAll(previous);
                    if (notifiedChannels.replace(uaid, previous, newUpdates)) {
                        break;
                    }
                }
            } else {
                newUpdates.addAll(currentUpdates);
                if (notifiedChannels.replace(uaid, currentUpdates, newUpdates)) {
                    break;
                }
            }
        }
    }

    @Override
    public Set<Update> getUpdates(final UUID uaid) {
        checkNotNull(uaid, "uaid");
        final Set<Update> updates = notifiedChannels.get(uaid);
        if (updates == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(updates);
    }

    @Override
    public boolean removeUpdate(final Update update, final UUID uaid) {
        checkNotNull(update, "update");
        checkNotNull(uaid, "uaid");
        while (true) {
            final Set<Update> currentUpdates = notifiedChannels.get(uaid);
            if (currentUpdates == null || currentUpdates.isEmpty()) {
                return false;
            }
            final Set<Update> newUpdates = Collections.newSetFromMap(new ConcurrentHashMap<Update, Boolean>());
            newUpdates.addAll(currentUpdates);
            if (newUpdates.remove(update)) {
                if (notifiedChannels.replace(uaid, currentUpdates, newUpdates)) {
                    return true;
                }
            } else {
                return false;
            }
        }
    }

}
