/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
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

        public static int getIdleTimeoutMsec() {
            return MAX_IDLE_TIME_MSEC;
        }
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
        final int serverPort = Integer.parseInt(propsServer.getProperty(
                "server.port", ConfigDefaults.SERVER_PORT));

        final int serverPortSsl = Integer.parseInt(propsServer.getProperty(
                "server.ssl.port", ConfigDefaults.SERVER_SSL_PORT));

        /*
         *
         */
        final QueuedThreadPool threadPool = new QueuedThreadPool();
        final String poolName = "jetty-threadpool";

        threadPool.setName(poolName);
        threadPool.setMinThreads(ConnectorConfig.getMinThreads());
        threadPool.setMaxThreads(ConnectorConfig.getMaxThreads());
        threadPool.setIdleTimeout(ConnectorConfig.getIdleTimeoutMsec());

        final Server server = new Server(threadPool);

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
         * The first server connector we create is the one for http, passing in
         * the http configuration we configured above so it can get things like
         * the output buffer size, etc. We also set the port and configure an
         * idle timeout.
         */
        final ServerConnector http = new ServerConnector(server,
                new HttpConnectionFactory(httpConfig));

        http.setPort(serverPort);
        http.setIdleTimeout(ConnectorConfig.getIdleTimeoutMsec());

        server.addConnector(http);

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

        final SslContextFactory sslContextFactory = new SslContextFactory();

        if (propsServer.getProperty("server.ssl.keystore") == null) {

            InputStream istr;

            /**
             *
             */
            final Properties propsPw = new Properties();
            istr = new java.io.FileInputStream(serverHome
                    + "/data/default-ssl-keystore.pw");
            propsPw.load(istr);
            final String pw = propsPw.getProperty("password");
            istr.close();

            /**
             *
             */
            istr = new java.io.FileInputStream(serverHome
                    + "/data/default-ssl-keystore");
            final KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(istr, pw.toCharArray());
            istr.close();

            /**
             *
             */
            sslContextFactory.setKeyStore(ks);
            sslContextFactory.setKeyManagerPassword(pw);

        } else {

            final Resource keystore = Resource.newResource(serverHome + "/"
                    + propsServer.getProperty("server.ssl.keystore"));

            sslContextFactory.setKeyStoreResource(keystore);

            sslContextFactory.setKeyStorePassword(propsServer
                    .getProperty("server.ssl.keystore-password"));

            sslContextFactory.setKeyManagerPassword(propsServer
                    .getProperty("server.ssl.key-password"));
        }

        /*
         * HTTPS Configuration
         *
         * A new HttpConfiguration object is needed for the next connector and
         * you can pass the old one as an argument to effectively clone the
         * contents. On this HttpConfiguration object we add a
         * SecureRequestCustomizer which is how a new connector is able to
         * resolve the https connection before handing control over to the Jetty
         * Server.
         */
        final HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
        httpsConfig.addCustomizer(new SecureRequestCustomizer());

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
        https.setIdleTimeout(ConnectorConfig.getIdleTimeoutMsec());

        server.addConnector(https);

        LOGGER.info("SSL access has been enabled on port " + serverPortSsl);

        /*
         * Set a handler
         */
        final WebAppContext webAppContext = new WebAppContext();

        webAppContext.setServer(server);
        webAppContext.setContextPath("/");

        boolean fDevelopment = (System.getProperty("savapage.war.file") == null);

        String pathToWarFile = null;

        if (fDevelopment) {
            pathToWarFile = "src/main/webapp";
        } else {
            pathToWarFile = serverHome + "/lib/"
                    + System.getProperty("savapage.war.file");
        }
        webAppContext.setWar(pathToWarFile);

        server.setHandler(webAppContext);

        /*
         *
         */
        final String serverStartedFile = serverHome + "/logs/"
                + "server.started.txt";

        int status = 0;

        FileWriter writer = null;

        try {
            /*
             * Writing the time we started in a file. This file is monitored by
             * the install script to see when the server has started.
             */
            writer = new FileWriter(serverStartedFile);

            final Date now = new Date();
            writer.write("#" + now.toString() + "\n");
            writer.write(String.valueOf(now.getTime()) + "\n");
            writer.flush();

            Runtime.getRuntime().addShutdownHook(new ShutdownThread(server));

            /*
             * Start the server
             */
            server.start();

            if (!fDevelopment) {
                server.join();
            }

        } catch (Exception e) {

            e.printStackTrace();
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
