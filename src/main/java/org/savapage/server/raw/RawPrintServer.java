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
package org.savapage.server.raw;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.PerformanceLogger;
import org.savapage.core.SpException;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.concurrent.ReadWriteLockEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.IppQueueDao;
import org.savapage.core.dao.helpers.DocLogProtocolEnum;
import org.savapage.core.dao.helpers.ReservedIppQueueEnum;
import org.savapage.core.doc.DocContent;
import org.savapage.core.doc.DocContentTypeEnum;
import org.savapage.core.jpa.IppQueue;
import org.savapage.core.print.server.DocContentPrintProcessor;
import org.savapage.core.services.QueueService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.ServiceEntryPoint;
import org.savapage.core.users.AbstractUserSource;
import org.savapage.server.WebApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Raw Printer on default port 9100. The port can be set in the
 * server.properties file.
 *
 * <p>
 * This server is specially created for Windows Print Server, so it can be part
 * of an Active Directory Domain or a Workgroup, and shared among clients.
 * </p>
 * <p>
 * A test in a Windows Workgroup environment shows that the originating user is
 * indeed transferred via the shared printer to the PostScript header.
 * </p>
 */
public class RawPrintServer extends Thread implements ServiceEntryPoint {

    /**
     * .
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(RawPrintServer.class);

    /**
     * .
     */
    private static final int SERVER_SOCKET_SO_TIMEOUT = 2000;

    /**
     * .
     */
    private final static QueueService QUEUE_SERVICE = ServiceContext
            .getServiceFactory().getQueueService();

    private boolean myListeningFlag = true;
    private int myRequestCount = 0;
    private int myActiveRequestCount = 0;
    private int port = 9100;

    static private final int END_OF_PRINTJOB = 4;

    /**
     *
     */
    class ShutdownThread extends Thread {

        private final RawPrintServer myServer;

        /**
         *
         * @param server
         */
        public ShutdownThread(final RawPrintServer server) {
            super("RawPrintShutdownThread");
            this.myServer = server;
        }

        @Override
        public void run() {
            myServer.shutdown();
        }
    }

    /**
     *
     */
    class SocketServerThread extends Thread {

        private static final int READ_TIMEOUT_MSEC = 10000;

        private final java.net.Socket mySocket;
        private final RawPrintServer myServer;

        /**
         *
         * @param socket
         */
        public SocketServerThread(java.net.Socket socket,
                final RawPrintServer server) {
            super("SocketServerThread");
            mySocket = socket;
            myServer = server;
        }

        @Override
        public void run() {

            myRequestCount++;
            myActiveRequestCount++;

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("request #" + myRequestCount + " from ["
                        + mySocket.getInetAddress().getHostAddress() + "]");
            }

            try {

                mySocket.setSoTimeout(READ_TIMEOUT_MSEC);

                // ----------------------------------------------
                // Wait for conversation request from client
                // ----------------------------------------------
                myServer.readAndPrint(mySocket);

            } catch (Exception ex) {

                if (!ConfigManager.isShutdownInProgress()) {

                    final String hostAddress;
                    if (mySocket == null) {
                        hostAddress = "?";
                    } else {
                        hostAddress =
                                mySocket.getInetAddress().getHostAddress();
                    }

                    final String msg =
                            String.format("%s: %s (RAW print from %s)", ex
                                    .getClass().getSimpleName(), ex
                                    .getMessage(), hostAddress);

                    LOGGER.error(msg, ex);

                    AdminPublisher.instance().publish(PubTopicEnum.USER,
                            PubLevelEnum.ERROR, msg);
                }
            }

            try {
                // All data exchanged: close socket
                mySocket.close();
            } catch (IOException ex) {
                LOGGER.error(ex.getMessage());
            }

            // ------------------------------------------
            myActiveRequestCount--;
            LOGGER.debug("request #" + myRequestCount + " ended");
        }
    }

    /**
     *
     * @param iPort
     */
    public RawPrintServer(int iPort) {
        this.port = iPort;
    }

    /**
     *
     * @param istr
     * @return {@code null} when nothing to read.
     * @throws IOException
     */
    private static String readLine(final InputStream istr,
            final OutputStream ostr) throws IOException {

        String line = null;
        int iByte = istr.read();

        while ((-1 < iByte)) {

            ostr.write(iByte);

            if (line == null) {
                line = "";
            }

            if ((iByte == 10) || (iByte == 13) || (END_OF_PRINTJOB == iByte)) {
                // istr.read();
                break;
            }

            line += (char) iByte;

            iByte = istr.read();
        }
        return line;
    }

    /**
     * Reads and prints data from the socket.
     * <p>
     * Note: the number of copies can NOT be retrieved from the PostScript
     * header. See Mantis #492.
     * </p>
     *
     * @param socket
     *            The socket to read the print job data from.
     * @throws IOException
     *             When data brings NO PostScript file.
     */
    private void readAndPrint(final Socket socket) throws IOException {

        final Date perfStartTime = PerformanceLogger.startTime();

        // --------------------------------------------------------------------
        // Print from Windows Vista / 7
        //
        // %!PS-Adobe-3.0
        // %%Title: Test Page
        // %%Creator: PScript5.dll Version 5.2.2
        // %%CreationDate: 11/17/2014 19:3:48
        // %%For: Rijk Ravestein
        // %%BoundingBox: (atend)
        // %%Pages: (atend)
        // %%Orientation: Portrait
        // %%PageOrder: Special
        // %%DocumentNeededResources: (atend)
        // %%DocumentSuppliedResources: (atend)
        // %%DocumentData: Clean7Bit
        // %%TargetDevice: (SavaPage) (3010.000) 0
        // %%LanguageLevel: 3
        // %%EndComments
        //
        // %%BeginDefaults
        // %%PageBoundingBox: 0 0 595 842
        // %%ViewingOrientation: 1 0 0 1
        // %%EndDefaults
        //
        // %%BeginProlog
        //

        // --------------------------------------------------------------------
        // Print from OS X Mavericks
        //
        // %!PS-Adobe-3.0
        // %APL_DSC_Encoding: UTF8
        // %APLProducer: (Version 10.9.1 (Build 13B42) Quartz PS Context)
        // %%Title: (testprint)
        // %%Creator: (cgpdftops CUPS filter)
        // %%CreationDate: (Monday, January 06 2014 13:49:07 CET)
        // %%For: (rijk)
        //

        final InputStream istr = socket.getInputStream();
        LOGGER.debug("read print job...");

        String title = null;
        String userid = null;

        final String PFX_TITLE = "%%Title: ";
        final String PFX_USERID = "%%For: ";
        final String PFX_BEGIN_PROLOG = "%%BeginProlog";
        final String PFX_APL_PRODUCER = "%APLProducer: ";

        final String originatorIp = socket.getInetAddress().getHostAddress();

        final String authenticatedWebAppUser =
                WebApp.getAuthUserByIpAddr(originatorIp);

        /*
         * First line
         */
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();

        String strline = readLine(istr, bos);

        /*
         * Just a ping... when does this happen?
         * See Mantis #529
         */
        if (strline == null) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("no data from " + originatorIp);
            }
            return;
        }

        if (!strline.startsWith(DocContent.HEADER_PS)) {
            consumeWithoutProcessing(istr);
            throw new IOException("Raw Print job from [" + originatorIp
                    + "] is not PostScript. Header ["
                    + StringUtils.substring(strline, 0, 10) + "]");
        }

        final List<String> lines = new ArrayList<>();

        boolean parenthesesSyntax = false;

        while (strline != null) {

            lines.add(strline);

            if (strline.startsWith(PFX_APL_PRODUCER)) {
                parenthesesSyntax =
                        StringUtils.removeStart(strline, PFX_APL_PRODUCER)
                                .trim().startsWith("(");
            } else if (strline.startsWith(PFX_TITLE)) {
                title = StringUtils.removeStart(strline, PFX_TITLE);
                if (parenthesesSyntax) {
                    title = stripParentheses(title);
                }
            } else if (strline.startsWith(PFX_USERID)) {
                userid = StringUtils.removeStart(strline, PFX_USERID);
                if (parenthesesSyntax) {
                    userid = stripParentheses(userid);
                }
            } else if (strline.startsWith(PFX_BEGIN_PROLOG)) {
                break;
            }

            if (title != null && userid != null) {
                break;
            }

            strline = readLine(istr, bos);
        }

        if (title == null || userid == null) {

            consumeWithoutProcessing(istr);

            throw new IOException("Raw Print job from [" + originatorIp
                    + "] has no [" + PFX_TITLE + "] and/or [" + PFX_USERID
                    + "]");
        }

        // Mantis #503
        userid =
                AbstractUserSource.asDbUserId(userid,
                        ConfigManager.isLdapUserSync());

        if (LOGGER.isTraceEnabled()) {

            final int MAX_LINES = 30;
            int i;
            for (i = 0; i < lines.size() && i < MAX_LINES; i++) {
                LOGGER.trace(lines.get(i));
            }
            if (i > MAX_LINES) {
                LOGGER.trace("... " + Integer.valueOf(i - MAX_LINES)
                        + "more lines");
            }
        }

        ServiceContext.open();

        DocContentPrintProcessor processor = null;
        IppQueue queue = null;
        boolean isAuthorized = false;

        ReadWriteLockEnum.DATABASE_READONLY.setReadLock(true);

        try {

            final IppQueueDao queueDao =
                    ServiceContext.getDaoContext().getIppQueueDao();

            queue = queueDao.find(ReservedIppQueueEnum.RAW_PRINT);

            /*
             * Allowed to print?
             */
            final String uri = "RAW:" + this.port;

            final boolean clientIpAllowed =
                    QUEUE_SERVICE.hasClientIpAccessToQueue(queue, uri,
                            originatorIp);

            if (clientIpAllowed) {

                processor =
                        new DocContentPrintProcessor(queue, originatorIp,
                                title, authenticatedWebAppUser);

                processor.setReadAheadInputBytes(bos.toByteArray());

                processor.processRequestingUser(userid);

                isAuthorized = clientIpAllowed && processor.isAuthorized();

                if (isAuthorized) {
                    processor.process(istr, DocLogProtocolEnum.RAW, null,
                            DocContentTypeEnum.PS, null);
                }
            }

        } catch (Exception e) {

            if (processor != null) {
                processor.setDeferredException(e);
            } else {
                LOGGER.error(e.getMessage(), e);
            }

        } finally {
            ReadWriteLockEnum.DATABASE_READONLY.setReadLock(false);
            consumeWithoutProcessing(istr);
            ServiceContext.close();
        }

        processor.evaluateErrorState(isAuthorized);

        PerformanceLogger.log(this.getClass(), "readAndPrint", perfStartTime,
                userid);
    }

    /**
     *
     * @param content
     * @return
     */
    private String stripParentheses(String content) {
        return StringUtils.removeEnd(
                StringUtils.removeStart(content.trim(), "("), ")");
    }

    /**
     * Consumes the (rest of) the full input stream, without processing it, so
     * the client is fooled that everything is OK, even when things went wrong
     * :-)
     *
     * If we would NOT do this, the client will try again-and-again, flooding
     * the server with requests.
     *
     * @throws IOException
     *
     */
    private void consumeWithoutProcessing(InputStream istr) throws IOException {
        byte[] bytes = new byte[1024];
        while (istr.read(bytes) > -1) {
            // no code intended
        }
    }

    @Override
    public void run() {

        Runtime.getRuntime().addShutdownHook(new ShutdownThread(this));

        java.net.ServerSocket serverSocket = null;

        try {
            /*
             * from Javadoc: "The maximum queue length for incoming connection
             * indications (a request to connect) is set to <code>50</code>. If
             * a connection indication arrives when the queue is full, the
             * connection is refused."
             */
            serverSocket = new java.net.ServerSocket(this.port);

            serverSocket.setSoTimeout(SERVER_SOCKET_SO_TIMEOUT);

        } catch (Exception ex) {
            throw new SpException(ex);
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("listening on port " + this.port + " ...");
        }

        while (myListeningFlag) {
            try {
                java.net.Socket socket = serverSocket.accept();
                SocketServerThread st = new SocketServerThread(socket, this);
                st.start();
            } catch (java.net.SocketTimeoutException ex) {
                continue;
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
                break;
            }
        }

        try {
            serverSocket.close();
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     *
     * @return
     */
    public int getPort() {
        return port;
    }

    /**
     *
     */
    public void shutdown() {

        LOGGER.info("Shutting down Raw Print Server ...");

        myListeningFlag = false;
        /*
         * Waiting for active requests to finish.
         */
        while (myActiveRequestCount > 0) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                LOGGER.error(ex.getMessage(), ex);
                break;
            }
        }

        LOGGER.info("Raw Print Server shutdown completed.");
    }

}
