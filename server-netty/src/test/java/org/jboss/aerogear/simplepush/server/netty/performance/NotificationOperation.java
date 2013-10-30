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

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.CountDownLatch;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.jboss.aerogear.simplepush.protocol.RegisterResponse;

/**
 * Performs a 'hello', followed by a channel 'registration' and then multiple 'notification's.
 */
public class NotificationOperation extends Operation {

    private final int operations;

    public NotificationOperation(final URI uri, final CountDownLatch startLatch, final CountDownLatch endLatch, final int operations) {
        super(uri, startLatch, endLatch);
        this.operations = operations;
    }

    public static void perform(final URI uri, final int threads, final int operations, final int warmupOperations) throws Exception {
        Runner.withUri(uri).threads(threads).operations(operations).warmupOperations(warmupOperations).notificationOperation().perform();
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
            final RegisterResponse registerResponse = sendRegistration(randomUUID().toString(), con);
            sendNotifications(registerResponse, con);
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            countDownEndLatch();
            NettyConnection.shutdown(con);
        }
    }

    private void sendNotifications(final RegisterResponse registerResponse, final NettyConnection con) throws Exception {
        for (long i = 1; i <= operations; i++) {
            sendVersion(i, getHttpConnection(new URL(registerResponse.getPushEndpoint())));
        }
    }

    public static HttpURLConnection getHttpConnection(final URL url) throws Exception {
        final HttpURLConnection http = (HttpURLConnection) url.openConnection();
        http.setDoOutput(true);
        http.setUseCaches(false);
        http.setRequestMethod("PUT");
        http.setRequestProperty("Content-Type", "application/json");
        if (http instanceof HttpsURLConnection) {
            setCustomTrustStore(http, "/openshift.truststore", "password");
        }
        return http;
    }

    public static void sendVersion(final long version, final HttpURLConnection http) throws Exception {
        byte[] bytes = ("version=" + version).getBytes("UTF-8");
        http.setRequestProperty("Content-Length", String.valueOf(bytes.length));
        http.setFixedLengthStreamingMode(bytes.length);
        OutputStream out = null;
        try {
            out = http.getOutputStream();
            out.write(bytes);
            out.flush();
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private static void setCustomTrustStore(final HttpURLConnection conn, final String trustStore, final String password) throws IOException {
        try {
            final X509TrustManager customTrustManager = getCustomTrustManager(getDefaultTrustManager(), getCustomTrustStore(trustStore, password));
            setTrustStoreForConnection((HttpsURLConnection) conn, customTrustManager);
        } catch (final Exception e) {
            throw new IOException(e);
        }
    }

    private static X509TrustManager getCustomTrustManager(final X509TrustManager defaultTrustManager, final KeyStore customTrustStore)
            throws NoSuchAlgorithmException, KeyStoreException {
        final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(customTrustStore);
        final X509TrustManager customTrustManager = (X509TrustManager) tmf.getTrustManagers()[0];
        return new DelegatingTrustManager(defaultTrustManager, customTrustManager);
    }

    private static X509TrustManager getDefaultTrustManager() throws NoSuchAlgorithmException, KeyStoreException {
        final TrustManagerFactory deftmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        deftmf.init((KeyStore)null);
        final TrustManager[] trustManagers = deftmf.getTrustManagers();
        for (TrustManager trustManager : trustManagers) {
            if (trustManager instanceof X509TrustManager) {
                return (X509TrustManager) trustManager;
            }
        }
        throw new RuntimeException("Could not find a default trustmanager");
    }

    private static KeyStore getCustomTrustStore(final String trustStore, final String password) throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException {
        final KeyStore customTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        customTrustStore.load(NotificationOperation.class.getResourceAsStream(trustStore), password.toCharArray());
        return customTrustStore;
    }

    private static void setTrustStoreForConnection(final HttpsURLConnection connection, final X509TrustManager trustManager)
        throws KeyManagementException, NoSuchAlgorithmException {
        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{trustManager}, null);
        connection.setSSLSocketFactory(sslContext.getSocketFactory());
    }

    private static class DelegatingTrustManager implements X509TrustManager {

        private final X509TrustManager delegate;
        private final X509TrustManager custom;

        public DelegatingTrustManager(final X509TrustManager delegate, final X509TrustManager custom) {
            this.delegate = delegate;
            this.custom = custom;
        }

        @Override
        public void checkServerTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
            try {
                custom.checkClientTrusted(chain, authType);
            } catch (final CertificateException e) {
                delegate.checkServerTrusted(chain, authType);
            }
        }

        @Override
        public void checkClientTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
            delegate.checkClientTrusted(chain, authType);
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return delegate.getAcceptedIssuers();
        }

    }


}
