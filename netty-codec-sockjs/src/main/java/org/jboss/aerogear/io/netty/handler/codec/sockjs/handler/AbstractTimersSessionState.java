/*
 * Copyright 2013 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.jboss.aerogear.io.netty.handler.codec.sockjs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.jboss.aerogear.io.netty.handler.codec.sockjs.SockJsSessionContext;
import org.jboss.aerogear.io.netty.handler.codec.sockjs.protocol.HeartbeatFrame;
import org.jboss.aerogear.io.netty.handler.codec.sockjs.util.ArgumentUtil;
import io.netty.util.concurrent.ScheduledFuture;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * Base class for SessionState implementations that require timers.
 *
 * This class provides a session timeout timer and a heartbeat timer
 * which are started when the onConnect method is called..
 */
abstract class AbstractTimersSessionState implements SessionState {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractTimersSessionState.class);

    private final ConcurrentMap<String, SockJsSession> sessions;
    private final SockJsSession session;
    private ScheduledFuture<?> heartbeatFuture;
    private ScheduledFuture<?> sessionTimer;

    protected AbstractTimersSessionState(final ConcurrentMap<String, SockJsSession> sessions,
                                         final SockJsSession session) {
        ArgumentUtil.checkNotNull(sessions, "sessions");
        this.sessions = sessions;
        this.session = session;
    }

    protected SockJsSession getSockJsSession() {
        return session;
    }

    @Override
    public State getState() {
        return session.getState();
    }

    @Override
    public void setState(final State state) {
        session.setState(state);
    }

    @Override
    public void onOpen(ChannelHandlerContext ctx) {
        session.setInuse(true);
        session.setOpenContext(ctx);
    }

    @Override
    public void onConnect(final ChannelHandlerContext ctx, final SockJsSessionContext sockJsSessionContext) {
        session.setConnectionContext(ctx);
        session.onOpen(sockJsSessionContext);
        startSessionTimer(ctx, session);
        startHeartbeatTimer(ctx, session);
    }

    private void startSessionTimer(final ChannelHandlerContext ctx, final SockJsSession session) {
        if (sessionTimer == null) {
            sessionTimer = ctx.executor().scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    final long now = TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
                    if (isInUse()) {
                        return;
                    }
                    if (session.timestamp() + session.config().sessionTimeout() < now) {
                        final SockJsSession removed = sessions.remove(session.sessionId());
                        session.connectionContext().close();
                        sessionTimer.cancel(true);
                        heartbeatFuture.cancel(true);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Removed {} from map[{}]", removed.sessionId(), sessions.size());
                        }
                    }
                }
            }, session.config().sessionTimeout(), session.config().sessionTimeout(), TimeUnit.MILLISECONDS);
        }
    }

    private void startHeartbeatTimer(final ChannelHandlerContext ctx, final SockJsSession session) {
        heartbeatFuture = ctx.executor().scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (ctx.channel().isActive() && ctx.channel().isRegistered()) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Sending heartbeat for {}", session);
                    }
                    ctx.channel().writeAndFlush(new HeartbeatFrame());
                }
            }
        },
        session.config().heartbeatInterval(),
        session.config().heartbeatInterval(),
        TimeUnit.MILLISECONDS);
    }

    @Override
    public void onClose() {
        session.onClose();
        session.setInuse(false);
    }

    @Override
    public void onMessage(String message) throws Exception {
        session.onMessage(message);
    }

    @Override
    public void setInuse() {
        session.setInuse(true);
    }

    @Override
    public void resetInuse() {
        session.setInuse(false);
    }

    @Override
    public void storeMessage(String message) {
        session.addMessage(message);
    }
}
