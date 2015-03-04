/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
 * Author: Rijk Ravestein.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server;

import java.io.FileWriter;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Date;
import java.util.Properties;

import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import org.savapage.common.ConfigDefaults;
import org.savapage.core.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main class for the Web Server.
 *
 * @author Datraverse B.V.
 *
 */
public final class WebServer {

    /**
     * When <b>true</b>: connectors are selected that implement efficient NIO
     * buffers with a non-blocking threading model. <i>These connectors are best
     * used when there are a many connections that have idle periods.</i>
     * <p>
     * When <b>false</b>: connectors are selected that implement a traditional
     * blocking IO and threading model. Jetty uses Normal JRE sockets and
     * allocates a thread per connection. Jetty allocates large buffers to
     * active connections only. <i>You should use this Connector only if NIO is
     * not available.</i>
     * </p>
     * See Jetty <a href=
     * "https://wiki.eclipse.org/Jetty/Howto/Configure_Connectors#SocketConnector"
     * >documentation</a>.
     */
    private static final boolean USE_NONBLOCKING_NIO = true;

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(WebServer.class);

    /**
     *
     */
    static class ShutdownThread extends Thread {

        /**
         *
         */
        private final Server myServer;

        /**
         * The constructor.
         *
         * @param server
         *            The {@link Server}.
         */
        public ShutdownThread(final Server server) {
            super("ShutdownThread");
            myServer = server;
        }

        @Override
        public void run() {

            LOGGER.info("Shutting down SavaPage server...");

            try {

                myServer.stop();
                myServer.join();

                LOGGER.info("SavaPage server shutdown completed");

            } catch (Exception e) {

                LOGGER.warn(e.getMessage());

                System.exit(1);
            }
        }
    }

    /**
     *
     */
    private WebServer() {
    }

    /**
     *
     */
    private static class ConnectorConfig {

        private static final int MIN_THREADS = 20;

        private static final int MAX_THREADS_X64 = 8000;

        private static final int MAX_THREADS_I686 = 4000;

        private static final int MAX_IDLE_TIME_MSEC = 30000;

        /**
         *
         * @return
         */
        public static int getMinThreads() {
            return MIN_THREADS;
        }

        public static boolean isX64() {
            return System.getProperty("os.arch").equalsIgnoreCase("amd64");
        }

        /**
         *
         * @return
         */
        public static int getMaxThreads() {

            final int maxThreads;

            if (isX64()) {
                maxThreads = MAX_THREADS_X64;
            } else {
                maxThreads = MAX_THREADS_I686;
            }

            return maxThreads;
        }

        public static int getMaxIdleTimeMsec() {
            return MAX_IDLE_TIME_MSEC;
        }
    }

    /**
     * Sets the properties and {@link ThreadPool} for the
     * {@link AbstractConnector}.
     *
     * @param connector
     *            The {@link AbstractConnector}.
     * @param isSsl
     *            {@code true} when the connector is an SSL connector.
     */
    private static void setConnectorProps(final AbstractConnector connector,
            final boolean isSsl) {

        final QueuedThreadPool threadPool = new QueuedThreadPool();

        final String poolName;

        if (isSsl) {
            poolName = "https";
        } else {
            poolName = "http";
        }

        threadPool.setName(poolName);

        threadPool.setMinThreads(ConnectorConfig.getMinThreads());
        threadPool.setMaxThreads(ConnectorConfig.getMaxThreads());
        threadPool.setMaxIdleTimeMs(ConnectorConfig.getMaxIdleTimeMsec());

        connector.setMaxIdleTime(ConnectorConfig.getMaxIdleTimeMsec());
        connector.setThreadPool(threadPool);

        // connector.setAcceptorPriorityOffset(offset);
        // connector.setLowResourcesMaxIdleTime(maxIdleTime);
        // connector.setMaxBuffers(maxBuffers);
        // connector.setRequestBufferSize(requestBufferSize);
        // connector.setSoLingerTime(soLingerTime);

    }

    /**
     *
     * @param args
     *            The arguments.
     * @throws Exception
     *             When unexpected things happen.
     */
    public static void main(final String[] args) throws Exception {

        ConfigManager.setDefaultServerPort(ConfigDefaults.SERVER_PORT);
        ConfigManager.setDefaultServerSslPort(ConfigDefaults.SERVER_SSL_PORT);

        /*
         * The number of connector acceptors depending on available processors a
         * connector.
         */
        final int numberOfConnectorAcceptors =
                2 * Runtime.getRuntime().availableProcessors();

        /*
         * Passed as -Dserver.home to JVM
         */
        final String serverHome = System.getProperty("server.home");

        /*
         * Read the properties files for this server
         */
        final String pathServerProperties = serverHome + "/server.properties";

        final Properties propsServer = new Properties();
        propsServer.load(new java.io.FileInputStream(pathServerProperties));

        /*
         * Add a connector for regular port
         */
        final int serverPort =
                Integer.parseInt(propsServer.getProperty("server.port",
                        ConfigDefaults.SERVER_PORT));

        // final int timeout = (int) Duration.ONE_HOUR.getMilliseconds();

        final Server server = new Server();

        final AbstractConnector connector;

        if (USE_NONBLOCKING_NIO) {
            connector = new SelectChannelConnector();
        } else {
            connector = new SocketConnector();
        }

        connector.setPort(serverPort);
        connector.setAcceptors(numberOfConnectorAcceptors);
        setConnectorProps(connector, false);

        server.addConnector(connector);

        /*
         * Check if a keystore for a SSL certificate is available, and if so,
         * start a SSL connector on the configured SSL port. By default, the
         * quickstart comes with a Apache Wicket Quickstart Certificate that
         * expires about half way september 2021. Do not use this certificate
         * anywhere important as the passwords are available in the source.
         */

        // Resource keystore = Resource.newClassPathResource("/keystore");
        // if (keystore != null && keystore.exists()) {

        /*
         * SSL
         */
        final int serverPortSsl =
                Integer.parseInt(propsServer.getProperty("server.ssl.port",
                        ConfigDefaults.SERVER_SSL_PORT));

        connector.setConfidentialPort(serverPortSsl);

        final SslContextFactory factory = new SslContextFactory();

        if (propsServer.getProperty("server.ssl.keystore") == null) {

            InputStream istr;

            /**
             *
             */
            final Properties propsPw = new Properties();
            istr =
                    new java.io.FileInputStream(serverHome
                            + "/data/default-ssl-keystore.pw");
            propsPw.load(istr);
            final String pw = propsPw.getProperty("password");
            istr.close();

            /**
             *
             */
            istr =
                    new java.io.FileInputStream(serverHome
                            + "/data/default-ssl-keystore");
            final KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(istr, pw.toCharArray());
            istr.close();

            /**
             *
             */
            factory.setKeyStore(ks);
            factory.setKeyManagerPassword(pw);

        } else {

            final Resource keystore =
                    Resource.newResource(serverHome + "/"
                            + propsServer.getProperty("server.ssl.keystore"));

            factory.setKeyStoreResource(keystore);

            factory.setKeyStorePassword(propsServer
                    .getProperty("server.ssl.keystore-password"));

            factory.setKeyManagerPassword(propsServer
                    .getProperty("server.ssl.key-password"));
        }

        final AbstractConnector sslConnector;

        if (USE_NONBLOCKING_NIO) {
            sslConnector = new SslSelectChannelConnector(factory);
        } else {
            sslConnector = new SslSocketConnector(factory);
        }

        sslConnector.setPort(serverPortSsl);
        sslConnector.setAcceptors(numberOfConnectorAcceptors);

        setConnectorProps(sslConnector, true);

        server.addConnector(sslConnector);

        LOGGER.info("SSL access has been enabled on port " + serverPortSsl);

        /*
         *
         */
        final WebAppContext bb = new WebAppContext();
        bb.setServer(server);
        bb.setContextPath("/");

        final boolean fDevelopment =
                (System.getProperty("savapage.war.file") == null);

        String pathToWarFile = null;

        if (fDevelopment) {
            pathToWarFile = "src/main/webapp";
        } else {
            pathToWarFile =
                    serverHome + "/lib/"
                            + System.getProperty("savapage.war.file");
        }
        bb.setWar(pathToWarFile);

        server.setHandler(bb);

        final String serverStartedFile =
                serverHome + "/logs/" + "server.started.txt";

        int status = 0;

        FileWriter writer = null;

        try {

            final Date now = new Date();

            /*
             * Writing the time we started in a file. This file is monitored by
             * the install script to see when the server has started.
             */
            writer = new FileWriter(serverStartedFile);

            writer.write("#" + now.toString() + "\n");
            writer.write(String.valueOf(now.getTime()) + "\n");

            writer.flush();

            Runtime.getRuntime().addShutdownHook(new ShutdownThread(server));

            server.start();

        } catch (Exception e) {

            LOGGER.error(e.getMessage(), e);

            status = 1;

        } finally {

            if (writer != null) {
                writer.close();
            }
        }

        if (status == 0) {

            LOGGER.info("server [" + server.getState() + "]");

            if (fDevelopment) {
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
