/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2020 Datraverse B.V. <info@datraverse.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.SecuredRedirectHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import org.savapage.common.ConfigDefaults;
import org.savapage.common.SystemPropertyEnum;
import org.savapage.core.SpException;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.SslCertInfo;
import org.savapage.core.ipp.operation.IppOperationContext;
import org.savapage.core.util.DeadlockedThreadsDetector;
import org.savapage.core.util.InetUtils;
import org.savapage.server.ext.papercut.ExtPaperCutSyncServlet;
import org.savapage.server.feed.AtomFeedServlet;
import org.savapage.server.restful.RestApplication;
import org.savapage.server.restful.services.RestSystemService;
import org.savapage.server.restful.services.RestTestService;
import org.savapage.server.xmlrpc.SpXmlRpcServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jetty Web Server.
 *
 * @author Rijk Ravestein
 *
 */
public final class WebServer {

    /**
     * Redirect all traffic to SSL, except
     * {@link IppOperationContext.CONTENT_TYPE_IPP},
     * {@link SpXmlRpcServlet.URL_PATTERN_BASE} and
     * {@link WebApp.PATH_IPP_PRINTER_ICONS}.
     */
    private static class MySecuredRedirectHandler
            extends SecuredRedirectHandler {

        @Override
        public void handle(final String target,
                final org.eclipse.jetty.server.Request baseRequest,
                final HttpServletRequest request,
                final HttpServletResponse response)
                throws IOException, ServletException {

            final String contentTypeReq = request.getContentType();

            /*
             * For now, take /xmlrpc as it is, do not redirect. Reason: C++
             * modules are not prepared for SSL yet.
             */
            if (request.getPathInfo()
                    .startsWith(SpXmlRpcServlet.URL_PATTERN_BASE)) {
                return;
            }

            /*
             * SavaPage Printer Icons used by IPP Clients.
             */
            if (request.getPathInfo()
                    .startsWith(WebApp.PATH_IPP_PRINTER_ICONS)) {
                return;
            }

            /*
             * Take IPP traffic as it is, do not redirect.
             */
            if (contentTypeReq != null && contentTypeReq
                    .equalsIgnoreCase(IppOperationContext.CONTENT_TYPE_IPP)) {
                return;
            }

            super.handle(target, baseRequest, request, response);
        }
    }

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(WebServer.class);

    /** */
    private static final String PROP_KEY_SERVER_PORT = "server.port";

    /** */
    private static final String PROP_KEY_SERVER_PORT_SSL = "server.ssl.port";

    /** */
    private static final String PROP_KEY_HTML_REDIRECT_SSL =
            "server.html.redirect.ssl";

    /** */
    private static final String PROP_KEY_SSL_KEYSTORE = "server.ssl.keystore";

    /** */
    private static final String PROP_KEY_SSL_KEYSTORE_PW =
            "server.ssl.keystore-password";

    /** */
    private static final String PROP_KEY_SSL_KEY_PW = "server.ssl.key-password";

    /** */
    private static final String PROP_KEY_WEBAPP_CUSTOM_I18N =
            "webapp.custom.i18n";

    /** */
    private static final String PROP_KEY_SERVER_THREADPOOL_QUEUE_CAPACITY =
            "server.threadpool.queue.capacity";
    /** */
    private static final String PROP_KEY_SERVER_THREADPOOL_MIN_THREADS =
            "server.threadpool.minthreads";
    /** */
    private static final String PROP_KEY_SERVER_THREADPOOL_MAX_THREADS =
            "server.threadpool.maxthreads";

    /** */
    private static final String PROP_KEY_SERVER_THREADPOOL_IDLE_TIMEOUT_MSEC =
            "server.threadpool.idle-timeout-msec";

    /** */
    private static final String PROP_KEY_SERVER_SESSION_SCAVENGE_INTERVAL_SEC =
            "server.session.scavenge.interval-sec";

    /** */
    private static final String SERVER_THREADPOOL_MIN_THREADS_DEFAULT = "20";

    /** */
    private static final String SERVER_THREADPOOL_MAX_THREADS_DEFAULT = "200";

    /** */
    private static final String SERVER_THREADPOOL_IDLE_TIMEOUT_MSEC_DEFAULT =
            "30000";

    /** */
    private static final String SERVER_THREADPOOL_QUEUE_CAPACITY_DEFAULT =
            "3000";

    /** */
    private static final String SERVER_SESSION_SCAVENGE_INTERVAL_SEC_DEFAULT =
            "600";

    /**
     * SavaPage branded session cookie to avoid session conflict with other
     * Jetty powered Web App instances on same host that use default session
     * cookie.
     * <p>
     * Also see {@code init-param: browserCookieName = SP_BAYEUX_BROWSER} of
     * {@code cometd} servlet in {@code web.xml}.
     * </p>
     */
    private static final String SERVER_SESSION_COOKIE = "SP_JSESSIONID";

    /** */
    private static boolean developerEnv;

    /** */
    private static int serverPort;

    /** */
    private static int serverPortSsl;

    /**
     * Number of acceptor threads.
     */
    private static int serverAcceptorThreads;

    /**
     * ThreadPool parameter information.
     *
     * <a href= "https://wiki.eclipse.org/Jetty/Howto/High_Load#Jetty_Tuning">
     * Jetty/Howto/High Load</a>
     */
    public static class ThreadPoolInfo {
        /**
         * Configure the number of threads according to the webapp. That is, how
         * many threads it needs in order to achieve the best performance.
         * Configure with mind to limiting memory usage maximum available.
         * Typically >50 and <500.
         */
        private static int maxThreads;

        /** */
        private static int minThreads;

        /**
         * <p>
         * It is very important to limit the task queue of Jetty. By default,
         * the queue is unbounded! As a result, if under high load in excess of
         * the processing power of the webapp, jetty will keep a lot of requests
         * on the queue. Even after the load has stopped, Jetty will appear to
         * have stopped responding to new requests as it still has lots of
         * requests on the queue to handle.
         * </p>
         *
         * <p>
         * For a high reliability system, it should reject the excess requests
         * immediately (fail fast) by using a queue with a bounded capability.
         * The capability (maximum queue length) should be calculated according
         * to the "no-response" time tolerable. For example, if the webapp can
         * handle 100 requests per second, and if you can allow it one minute to
         * recover from excessive high load, you can set the queue capability to
         * 60*100=6000. If it is set too low, it will reject requests too soon
         * and can't handle normal load spike.
         * </p>
         */
        private static int queueCapacity;

        /**
         * Maximum time a thread may be idle in ms.
         */
        private static int idleTimeoutMsec;

        /**
         * @return Max threads in the {@link QueuedThreadPool}.
         */
        public static int getMaxThreads() {
            return maxThreads;
        }

        /**
         * @return Min threads in the {@link QueuedThreadPool}.
         */
        public static int getMinThreads() {
            return minThreads;
        }

        /**
         * @return Queue Capacity of the {@link QueuedThreadPool}.
         */
        public static int getQueueCapacity() {
            return queueCapacity;
        }

        /**
         * @return Maximum time a thread may be idle in ms.
         */
        public static int getIdleTimeoutMsec() {
            return idleTimeoutMsec;
        }

        /**
         * @return Log message for Max threads in the {@link QueuedThreadPool}.
         */
        public static String logMaxThreads() {
            return String.format("%s [%s]",
                    PROP_KEY_SERVER_THREADPOOL_MAX_THREADS,
                    ThreadPoolInfo.maxThreads);
        }

        /**
         * @return Log message for Min threads in the {@link QueuedThreadPool}.
         */
        public static String logMinThreads() {
            return String.format("%s [%s]",
                    PROP_KEY_SERVER_THREADPOOL_MIN_THREADS,
                    ThreadPoolInfo.minThreads);
        }

        /**
         * @return Log message for Queue Capacity of the
         *         {@link QueuedThreadPool}.
         */
        public static String logQueueCapacity() {
            return String.format("%s [%s]",
                    PROP_KEY_SERVER_THREADPOOL_QUEUE_CAPACITY,
                    ThreadPoolInfo.queueCapacity);
        }

        /**
         * @return Log message for Maximum time a thread may be idle in ms.
         */
        public static String logIdleTimeoutMsec() {
            return String.format("%s [%s]",
                    PROP_KEY_SERVER_THREADPOOL_IDLE_TIMEOUT_MSEC,
                    ThreadPoolInfo.idleTimeoutMsec);
        }
    }

    /** */
    private static boolean serverSslRedirect;

    /** */
    private static boolean webAppCustomI18n;

    /** */
    private static int sessionScavengeInterval;

    /** */
    private WebServer() {
    }

    /**
     * @return {@code true} when custom Web App i18n is to be applied.
     */
    public static boolean isWebAppCustomI18n() {
        return webAppCustomI18n;
    }

    /**
     * @return The server port.
     */
    public static int getServerPort() {
        return serverPort;
    }

    /**
     * @return The server SSL port.
     */
    public static int getServerPortSsl() {
        return serverPortSsl;
    }

    /**
     * @return Log message with session scavenge in seconds.
     */
    public static String logSessionScavengeInterval() {
        return String.format("%s [%d]",
                PROP_KEY_SERVER_SESSION_SCAVENGE_INTERVAL_SEC,
                sessionScavengeInterval);
    }

    /**
     * @return Number of server acceptor threads.
     */
    public static int getServerAcceptorThreads() {
        return serverAcceptorThreads;
    }

    /**
     * @return {@code true} if server access is SSL only.
     */
    public static boolean isSSLOnly() {
        return serverPortSsl > 0 && serverPort == 0;
    }

    /**
     * @return {@code true} if non-SSL port is redirected to SSL port.
     */
    public static boolean isSSLRedirect() {
        return serverSslRedirect;
    }

    /**
     * @return {@code true} if server runs in development environment.
     */
    public static boolean isDeveloperEnv() {
        return developerEnv;
    }

    /**
     * Creates the {@link SslCertInfo}.
     *
     * @param ksLocation
     *            The keystore location.
     * @param ksPassword
     *            The keystore password.
     * @return The {@link SslCertInfo}, or {@code null}. when alias is not
     *         found.
     */
    private static SslCertInfo createSslCertInfo(final String ksLocation,
            final String ksPassword) {

        final File file = new File(ksLocation);

        SslCertInfo certInfo = null;

        try (FileInputStream is = new FileInputStream(file);) {

            final KeyStore keystore =
                    KeyStore.getInstance(KeyStore.getDefaultType());

            keystore.load(is, ksPassword.toCharArray());

            final Enumeration<String> aliases = keystore.aliases();

            /*
             * Get X509 cert and alias with most recent "not after".
             */
            long minNotAfter = Long.MAX_VALUE;
            java.security.cert.X509Certificate minCertX509 = null;
            String minAlias = null;
            int nAliases = 0;

            while (aliases.hasMoreElements()) {

                final String alias = aliases.nextElement();

                final java.security.cert.Certificate cert =
                        keystore.getCertificate(alias);

                if (cert instanceof java.security.cert.X509Certificate) {

                    java.security.cert.X509Certificate certX509 =
                            (java.security.cert.X509Certificate) cert;

                    final long notAfter = certX509.getNotAfter().getTime();
                    if (notAfter < minNotAfter) {
                        minCertX509 = certX509;
                        minAlias = alias;
                    }

                    nAliases++;
                }
            }

            if (minCertX509 != null) {

                final Date creationDate = keystore.getCreationDate(minAlias);
                final Date notAfter = minCertX509.getNotAfter();

                String subjectCN = null;

                final LdapName lnSubject =
                        new LdapName(minCertX509.getSubjectDN().getName());
                for (final Rdn rdn : lnSubject.getRdns()) {
                    if (rdn.getType().equalsIgnoreCase("CN")) {
                        subjectCN = rdn.getValue().toString();
                        break;
                    }
                }

                final LdapName ln =
                        new LdapName(minCertX509.getIssuerDN().getName());
                for (final Rdn rdn : ln.getRdns()) {
                    if (rdn.getType().equalsIgnoreCase("CN")) {
                        final String issuerCN = rdn.getValue().toString();
                        certInfo = new SslCertInfo(issuerCN, subjectCN,
                                creationDate, notAfter, nAliases == 1);
                        break;
                    }
                }
            }

        } catch (KeyStoreException | NoSuchAlgorithmException
                | CertificateException | IOException | InvalidNameException e) {
            LOGGER.error(e.getMessage(), e);
            throw new SpException(e.getMessage(), e);
        }

        return certInfo;
    }

    /**
     * @return {@code true} when Java 8 runtime.
     */
    private static boolean checkJava8() {
        try {
            // Pick a class that was introduced in Java 8.
            final String java8ClassCheck = "java.time.Duration";
            Class.forName(java8ClassCheck);
            return true;
        } catch (ClassNotFoundException e) {
            // no code intended.
        }

        final String msg =
                "\n+=================================================+"
                        + "\n| SavaPage NOT started: "
                        + "Java 8 MUST be installed. |"
                        + "\n+========================"
                        + "=========================+";
        System.err.println(new Date().toString() + " : " + msg);
        LOGGER.error(msg);
        return false;
    }

    private final static int PORT_OFFSET = 1024;

    /**
     * @return {@code true} when ports are valid.
     */
    private static boolean checkPorts() {

        if (getServerPort() > PORT_OFFSET && getServerPortSsl() > PORT_OFFSET) {
            return true;
        }

        final String msg =
                "\n+========================================================+"
                        + "\n| SavaPage NOT started: server ports MUST be GT "
                        + String.valueOf(PORT_OFFSET) + "     |"
                        + "\n+==============================="
                        + "=========================+";
        System.err.println(new Date().toString() + " : " + msg);
        LOGGER.error(msg);
        return false;
    }

    /**
     * Add RESTfull servlet.
     *
     * @param context
     *            Web App context.
     */
    private static void initRESTful(final WebAppContext context) {

        final ServletHolder jerseyServlet = context.addServlet(
                org.glassfish.jersey.servlet.ServletContainer.class,
                RestApplication.SERVLET_URL_PATTERN);

        jerseyServlet.setInitParameter("javax.ws.rs.Application",
                RestApplication.class.getCanonicalName());
    }

    /**
     * Initializing action when started in development environment.
     */
    private static void initDevelopmenEnv() {

        RestSystemService.test();
        RestTestService.test();

        boolean createDeadlockTest = false;
        if (createDeadlockTest) {
            DeadlockedThreadsDetector.createDeadlockTest();
        }
    }

    /**
     * Starts the Web Server.
     * <p>
     * References:
     * </p>
     * <ul>
     * <li>Jetty: <a href="See:
     * https://www.eclipse.org/jetty/documentation/current/using-annotations
     * .html">Working with Annotations</a></li>
     * </ul>
     *
     * @param args
     *            The arguments.
     * @throws Exception
     *             When unexpected things happen.
     */
    public static void main(final String[] args) throws Exception {

        ConfigManager.initJavaUtilLogging();

        if (!checkJava8()) {
            return;
        }

        ConfigManager.setDefaultServerPort(ConfigDefaults.SERVER_PORT);
        ConfigManager.setDefaultServerSslPort(ConfigDefaults.SERVER_SSL_PORT);

        /*
         * Passed as -Dserver.home to JVM
         */
        final Properties propsServer = ConfigManager.loadServerProperties();

        /*
         * Notify WebApp.
         */
        WebApp.setServerProps(propsServer);
        WebApp.loadWebProperties();

        /*
         * Server Ports.
         */
        serverPort = Integer.parseInt(propsServer
                .getProperty(PROP_KEY_SERVER_PORT, ConfigDefaults.SERVER_PORT));

        serverPortSsl = Integer.parseInt(propsServer.getProperty(
                PROP_KEY_SERVER_PORT_SSL, ConfigDefaults.SERVER_SSL_PORT));

        if (!checkPorts()) {
            return;
        }

        /*
         * Check if ports are in use.
         */
        boolean portsInUse = false;

        for (final int port : new int[] { serverPort, serverPortSsl }) {
            if (InetUtils.isPortInUse(port)) {
                portsInUse = true;
                System.err.println(String.format("Port [%d] is in use.", port));
            }
        }
        if (portsInUse) {
            System.err.println(String.format("%s not started.",
                    CommunityDictEnum.SAVAPAGE.getWord()));
            System.exit(-1);
            return;
        }

        serverSslRedirect = !isSSLOnly() && BooleanUtils.toBooleanDefaultIfNull(
                BooleanUtils.toBooleanObject(
                        propsServer.getProperty(PROP_KEY_HTML_REDIRECT_SSL)),
                false);

        webAppCustomI18n = BooleanUtils.toBooleanDefaultIfNull(
                BooleanUtils.toBooleanObject(
                        propsServer.getProperty(PROP_KEY_WEBAPP_CUSTOM_I18N)),
                false);

        sessionScavengeInterval = Integer.parseInt(propsServer.getProperty(
                PROP_KEY_SERVER_SESSION_SCAVENGE_INTERVAL_SEC,
                SERVER_SESSION_SCAVENGE_INTERVAL_SEC_DEFAULT));

        ThreadPoolInfo.queueCapacity = Integer.parseInt(propsServer.getProperty(
                PROP_KEY_SERVER_THREADPOOL_QUEUE_CAPACITY,
                SERVER_THREADPOOL_QUEUE_CAPACITY_DEFAULT));

        if (ThreadPoolInfo.queueCapacity <= 0) {
            System.err.println(String.format(
                    "%s not started: %s [%d] is invalid "
                            + "(capacity must be GT zero, "
                            + "and can't be unbounded).",
                    CommunityDictEnum.SAVAPAGE.getWord(),
                    PROP_KEY_SERVER_THREADPOOL_QUEUE_CAPACITY,
                    ThreadPoolInfo.queueCapacity));
            System.exit(-1);
            return;
        }

        ThreadPoolInfo.maxThreads = Integer.parseInt(
                propsServer.getProperty(PROP_KEY_SERVER_THREADPOOL_MAX_THREADS,
                        SERVER_THREADPOOL_MAX_THREADS_DEFAULT));

        ThreadPoolInfo.minThreads = Integer.parseInt(
                propsServer.getProperty(PROP_KEY_SERVER_THREADPOOL_MIN_THREADS,
                        SERVER_THREADPOOL_MIN_THREADS_DEFAULT));

        ThreadPoolInfo.idleTimeoutMsec = Integer.parseInt(propsServer
                .getProperty(PROP_KEY_SERVER_THREADPOOL_IDLE_TIMEOUT_MSEC,
                        SERVER_THREADPOOL_IDLE_TIMEOUT_MSEC_DEFAULT));

        final QueuedThreadPool threadPool;

        /*
         * https://wiki.eclipse.org/Jetty/Howto/High_Load#Jetty_Tuning
         *
         * The number of acceptors is calculated by Jetty based of number of
         * available CPU cores.
         */
        if (ThreadPoolInfo.queueCapacity < 0) {
            threadPool = new QueuedThreadPool(ThreadPoolInfo.maxThreads,
                    ThreadPoolInfo.minThreads, ThreadPoolInfo.idleTimeoutMsec);
        } else {
            threadPool = new QueuedThreadPool(ThreadPoolInfo.maxThreads,
                    ThreadPoolInfo.minThreads, ThreadPoolInfo.idleTimeoutMsec,
                    new ArrayBlockingQueue<>(ThreadPoolInfo.queueCapacity));
        }

        threadPool.setName("jetty-threadpool");

        final Server server = new Server(threadPool);
        // First thing to do.
        Runtime.getRuntime().addShutdownHook(new WebServerShutdownHook(server));

        /*
         * This is needed to enable the Jetty annotations.
         */
        org.eclipse.jetty.webapp.Configuration.ClassList classlist =
                org.eclipse.jetty.webapp.Configuration.ClassList
                        .setServerDefault(server);
        classlist.addBefore("org.eclipse.jetty.webapp.JettyWebXmlConfiguration",
                "org.eclipse.jetty.annotations.AnnotationConfiguration");

        /*
         * HttpConfiguration is a collection of configuration information
         * appropriate for http and https.
         *
         * The default scheme for http is <code>http</code> of course, as the
         * default for secured http is <code>https</code> but we show setting
         * the scheme to show it can be done.
         *
         * The port for secured communication is also set here.
         */
        final HttpConfiguration httpConfig = new HttpConfiguration();

        httpConfig.setSecureScheme("https");
        httpConfig.setSecurePort(serverPortSsl);

        /*
         * Customize Requests for Proxy Forwarding.
         */
        httpConfig.addCustomizer(new ForwardedRequestCustomizer());

        if (!isSSLOnly()) {
            /*
             * The server connector we create is the one for http, passing in
             * the http configuration we configured above so it can get things
             * like the output buffer size, etc. We also set the port and
             * configure an idle timeout.
             */
            final ServerConnector http = new ServerConnector(server,
                    new HttpConnectionFactory(httpConfig));

            http.setPort(serverPort);
            http.setIdleTimeout(ThreadPoolInfo.idleTimeoutMsec);

            server.addConnector(http);
        }

        /*
         * SSL Context Factory for HTTPS and SPDY.
         *
         * SSL requires a certificate so we configure a factory for ssl contents
         * with information pointing to what keystore the ssl connection needs
         * to know about.
         *
         * Much more configuration is available the ssl context, including
         * things like choosing the particular certificate out of a keystore to
         * be used.
         */

        final SslContextFactory sslContextFactory =
                new SslContextFactory.Server();

        // Mantis #562
        sslContextFactory.addExcludeCipherSuites(
                //
                // weak
                "TLS_RSA_WITH_RC4_128_MD5",
                // weak
                "TLS_RSA_WITH_RC4_128_SHA",
                // insecure
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
                // weak
                "TLS_ECDHE_RSA_WITH_RC4_128_SHA",
                // insecure
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
                // insecure
                "TLS_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
                // insecure
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
                // insecure
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256"
        //
        );

        final String serverHome = ConfigManager.getServerHome();

        final String ksLocation;
        final String ksPassword;

        if (propsServer.getProperty(PROP_KEY_SSL_KEYSTORE) == null) {

            InputStream istr;

            final Properties propsPw = new Properties();

            istr = new java.io.FileInputStream(
                    serverHome + "/data/default-ssl-keystore.pw");

            propsPw.load(istr);
            ksPassword = propsPw.getProperty("password");
            istr.close();

            ksLocation = serverHome + "/data/default-ssl-keystore";

            istr = new java.io.FileInputStream(ksLocation);
            final KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(istr, ksPassword.toCharArray());
            istr.close();

            sslContextFactory.setKeyStore(ks);
            sslContextFactory.setKeyStorePassword(ksPassword);

        } else {

            ksLocation = String.format("%s%c%s", serverHome, File.separatorChar,
                    propsServer.getProperty(PROP_KEY_SSL_KEYSTORE));

            ksPassword = propsServer.getProperty(PROP_KEY_SSL_KEYSTORE_PW);

            final Resource keystore = Resource.newResource(ksLocation);
            sslContextFactory.setKeyStoreResource(keystore);

            // Step 1: KeyStore password.
            sslContextFactory.setKeyStorePassword(ksPassword);

            // Step 2: KeyManager password.
            final String kmPassword =
                    propsServer.getProperty(PROP_KEY_SSL_KEY_PW);

            if (StringUtils.isNoneBlank(kmPassword)
                    && !StringUtils.equals(ksPassword, kmPassword)) {
                sslContextFactory.setKeyManagerPassword(kmPassword);
            }
        }

        ConfigManager.setSslCertInfo(createSslCertInfo(ksLocation, ksPassword));

        /*
         * HTTPS Configuration
         *
         * A new HttpConfiguration object is needed for the next connector and
         * you can pass the old one as an argument to effectively clone the
         * contents.
         *
         * On this HttpConfiguration object we add a SecureRequestCustomizer
         * which is how a new connector is able to resolve the https connection
         * before handing control over to the Jetty Server.
         */
        final HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
        httpsConfig.addCustomizer(new SecureRequestCustomizer());

        /*
         * Customize Requests for Proxy Forwarding.
         */
        httpsConfig.addCustomizer(new ForwardedRequestCustomizer());

        /*
         * HTTPS connector
         *
         * We create a second ServerConnector, passing in the http configuration
         * we just made along with the previously created ssl context factory.
         * Next we set the port and a longer idle timeout.
         */
        final ServerConnector https = new ServerConnector(server,
                new SslConnectionFactory(sslContextFactory,
                        HttpVersion.HTTP_1_1.asString()),
                new HttpConnectionFactory(httpsConfig));

        https.setPort(serverPortSsl);
        https.setIdleTimeout(ThreadPoolInfo.idleTimeoutMsec);

        serverAcceptorThreads = https.getAcceptors();

        server.addConnector(https);

        /*
         * Set a handler
         */
        final WebAppContext webAppContext = new WebAppContext();

        webAppContext.setServer(server);
        webAppContext.setContextPath("/");

        developerEnv = SystemPropertyEnum.SAVAPAGE_WAR_FILE.getValue() == null;

        String pathToWarFile = null;

        if (developerEnv) {
            pathToWarFile = "src/main/webapp";
        } else {
            pathToWarFile = serverHome + "/lib/"
                    + SystemPropertyEnum.SAVAPAGE_WAR_FILE.getValue();
        }

        webAppContext.setWar(pathToWarFile);

        /*
         * This is needed for scanning "discoverable" Jetty annotations. The
         * "/classes/.*" scan is needed when running in development (Eclipse).
         * The "/savapage-server-*.jar$" scan in needed for production.
         */
        webAppContext.setAttribute(
                "org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
                ".*/savapage-server-[^/]*\\.jar$|.*/classes/.*");

        /*
         * Redirect to SSL?
         */
        final Handler[] handlerArray;

        if (serverSslRedirect) {
            handlerArray = new Handler[] { new MySecuredRedirectHandler(),
                    webAppContext };
        } else {
            handlerArray = new Handler[] { webAppContext };
        }

        /*
         * Set cookies to HttpOnly.
         */
        webAppContext.getSessionHandler().getSessionCookieConfig()
                .setHttpOnly(true);

        // Override default session cookie.
        webAppContext.getSessionHandler()
                .setSessionCookie(SERVER_SESSION_COOKIE);

        /*
         * Set the handler(s).
         */
        final HandlerList handlerList = new HandlerList();
        handlerList.setHandlers(handlerArray);

        server.setHandler(handlerList);

        /*
         * BASIC Authentication for Atom Feed and PaperCut User Syn/Auth
         * Interface.
         */
        final LoginService basicAuthLoginService = new BasicAuthLoginService(
                new String[] { AtomFeedServlet.ROLE_ALLOWED,
                        ExtPaperCutSyncServlet.ROLE_ALLOWED });

        server.addBean(basicAuthLoginService);

        /*
         * See web.xml:
         * <login-config><auth-method>BASIC</auth-method></login-config>
         */
        webAppContext.getSecurityHandler()
                .setLoginService(basicAuthLoginService);

        // Add RESTfull servlet.
        initRESTful(webAppContext);

        //
        final String serverStartedFile =
                String.format("%s%clogs%cserver.started.txt", serverHome,
                        File.separatorChar, File.separatorChar);

        int status = 0;

        try (FileWriter writer = new FileWriter(serverStartedFile);) {
            /*
             * Writing the time we started in a file. This file is monitored by
             * the install script to see when the server has started.
             */

            final Date now = new Date();

            writer.write("#");
            writer.write(now.toString());
            writer.write("\n");
            writer.write(String.valueOf(now.getTime()));
            writer.write("\n");

            writer.flush();

            /*
             * Start the server: WebApp is initialized.
             */
            server.start();

            // ... after start() !
            server.getSessionIdManager().getSessionHouseKeeper()
                    .setIntervalSec(sessionScavengeInterval);

            if (WebApp.hasInitializeError()) {
                System.exit(1);
                return;
            }

            if (!developerEnv) {
                server.join();
            }

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            status = 1;
        }

        if (status == 0) {

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("server [" + server.getState() + "]");
            }

            if (developerEnv) {

                initDevelopmenEnv();

                System.out
                        .println(" \n+========================================"
                                + "====================================+"
                                + "\n| You're running in development mode. "
                                + "Click in this console and press ENTER. |"
                                + "\n| This will call System.exit() so the "
                                + "shutdown routine is executed.          |"
                                + "\n+====================================="
                                + "=======================================+"
                                + "\n");
                try {

                    System.in.read();
                    System.exit(0);

                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
    }
}
