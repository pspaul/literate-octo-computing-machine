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
package org.savapage.server.ipp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.mapper.parameter.INamedParameters.NamedPair;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.util.resource.AbstractResourceStream;
import org.apache.wicket.util.resource.FileResourceStream;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.apache.wicket.util.time.Duration;
import org.savapage.core.SpException;
import org.savapage.core.SpInfo;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.helpers.ReservedIppQueueEnum;
import org.savapage.core.ipp.operation.AbstractIppOperation;
import org.savapage.core.ipp.operation.IppMessageMixin;
import org.savapage.core.ipp.operation.IppOperationId;
import org.savapage.core.jpa.IppQueue;
import org.savapage.core.jpa.User;
import org.savapage.core.services.QueueService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.ServiceEntryPoint;
import org.savapage.core.services.UserService;
import org.savapage.core.util.DateUtil;
import org.savapage.core.util.InetUtils;
import org.savapage.server.WebApp;
import org.savapage.server.webapp.WebAppUserPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SavaPage IPP Print Server handling all IPP requests.
 * <p>
 * IMPORTANT: This class is mounted to the {@link WebApp#MOUNT_PATH_PRINTERS}
 * context. in the {@link WebApp} class. Its subclass
 * {@link IppPrintServerHomePage} is the handler of the default web context.
 * </p>
 *
 * @author Datraverse B.V.
 */
public class IppPrintServer extends WebPage implements ServiceEntryPoint {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    private static final String CONTENT_TYPE_PPD = "application/vnd.cups-ppd";

    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(IppPrintServer.class);

    /**
     * IMPORTANT: PPD download (triggered by Add Printer in Linux) works, but
     * has SIDE EFFECTS which makes it unusable for now. Need to be researched
     * and is NOT implemented yet. See Mantis #160.
     */
    private static final boolean ALLOW_PPD_DOWNLOAD = false;

    /**
     *
     */
    private final static QueueService QUEUE_SERVICE = ServiceContext
            .getServiceFactory().getQueueService();

    /**
    *
    */
    private final static UserService USER_SERVICE = ServiceContext
            .getServiceFactory().getUserService();

    /**
     *
     */
    public static void init() {
        SpInfo.instance().log("IPP Print Server started.");
    }

    /**
     *
     */
    private static class IppResourceStream extends AbstractResourceStream {

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        private final InputStream istr;

        IppResourceStream(final InputStream istr) {
            this.istr = istr;
        }

        @Override
        public void close() throws IOException {
            istr.close();
        }

        @Override
        public InputStream getInputStream()
                throws ResourceStreamNotFoundException {
            return istr;
        }

    }

    /**
     * Our own handler to control the .... streaming ...
     *
     * @author rijk
     *
     */
    private static class SpStreamRequestHandler extends
            ResourceStreamRequestHandler {

        /**
         *
         * @param istr
         *            The {@link InputStream}.
         */
        public SpStreamRequestHandler(final InputStream istr) {
            super(new IppResourceStream(istr));
        }

    }

    /**
     *
     * @param parameters
     *            The {@link PageParameters}.
     */
    public IppPrintServer(final PageParameters parameters) {

        final RequestCycle requestCycle = getRequestCycle();

        final HttpServletRequest request =
                (HttpServletRequest) requestCycle.getRequest()
                        .getContainerRequest();

        if (LOGGER.isDebugEnabled()) {
            logDebug(request, parameters);
        }

        final HttpServletResponse response =
                (HttpServletResponse) requestCycle.getResponse()
                        .getContainerResponse();

        final String contentTypeReq = request.getContentType();

        /*
         * Request for a PPD file: do NOT deliver the file, since the effect
         * needs to be tested. For now, just respond with SC_NOT_IMPLEMENTED.
         */
        if (contentTypeReq == null
                && StringUtils.upperCase(request.getRequestURL().toString())
                        .endsWith(".PPD")) {
            if (ALLOW_PPD_DOWNLOAD) {
                handlePpdRequest(response);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
            }
            return;
        }

        /*
         * Redirect to /user page for content types other than IPP_CONTENT_TYPE.
         */
        if (contentTypeReq == null
                || !contentTypeReq
                        .equalsIgnoreCase(IppMessageMixin.CONTENT_TYPE_IPP)) {

            setResponsePage(WebAppUserPage.class);
            return;
        }

        /*
         * OK, we can handle the IPP request.
         */
        response.setContentType(IppMessageMixin.CONTENT_TYPE_IPP);
        response.setStatus(HttpServletResponse.SC_OK);

        ServiceContext.open();

        try {

            final String remoteAddr = request.getRemoteAddr();

            /*
             * Get the Queue from the URL.
             */
            final IppPrintServerUrlParms serverPageParms =
                    new IppPrintServerUrlParms(Url.parse(request
                            .getRequestURL().toString()));

            final String requestedQueueUrlPath = serverPageParms.getPrinter();

            /*
             * Reserved queue?
             */
            final ReservedIppQueueEnum reservedQueueEnum =
                    QUEUE_SERVICE.getReservedQueue(requestedQueueUrlPath);

            /*
             * Find queue object.
             */
            final IppQueue queue =
                    ServiceContext.getDaoContext().getIppQueueDao()
                            .findByUrlPath(requestedQueueUrlPath);

            /*
             * Does user have access to queue?
             */
            final boolean hasPrintAccessToQueue;

            if (reservedQueueEnum != null && !reservedQueueEnum.isDriverPrint()) {

                hasPrintAccessToQueue = false;

            } else if (queue == null || queue.getDeleted()) {

                hasPrintAccessToQueue = false;

            } else if (queue.getDisabled()) {

                hasPrintAccessToQueue = false;

            } else if (reservedQueueEnum != ReservedIppQueueEnum.IPP_PRINT_INTERNET
                    && InetUtils.isPublicAddress(remoteAddr)) {

                hasPrintAccessToQueue = false;

            } else {

                hasPrintAccessToQueue =
                        QUEUE_SERVICE.hasClientIpAccessToQueue(queue,
                                serverPageParms.getPrinter(), remoteAddr);
            }

            /*
             *
             */
            final String trustedIppClientUserId;

            if (!hasPrintAccessToQueue) {

                trustedIppClientUserId = null;

            } else if (reservedQueueEnum == ReservedIppQueueEnum.IPP_PRINT_INTERNET) {

                final User remoteInternetUser =
                        USER_SERVICE.findUserByNumberUuid(
                                serverPageParms.getUserNumber(),
                                serverPageParms.getUserUuid());

                if (remoteInternetUser == null) {
                    trustedIppClientUserId = null;
                } else {
                    trustedIppClientUserId = remoteInternetUser.getUserId();
                }

            } else {
                trustedIppClientUserId = WebApp.getAuthUserByIpAddr(remoteAddr);
            }

            /*
             * Handle the request.
             */
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();

            final IppOperationId ippOperationId =
                    AbstractIppOperation.handle(remoteAddr, queue,
                            requestedQueueUrlPath, request.getInputStream(),
                            bos, hasPrintAccessToQueue, trustedIppClientUserId);

            /*
             *
             */
            if (ippOperationId != null
                    && ippOperationId == IppOperationId.VALIDATE_JOB) {

                final String warnMsg;

                if (reservedQueueEnum != null
                        && !reservedQueueEnum.isDriverPrint()) {

                    warnMsg =
                            new StringBuilder()
                                    .append("Print to reserved queue [")
                                    .append(requestedQueueUrlPath)
                                    .append("] denied from ")
                                    .append(remoteAddr).toString();

                } else if (queue == null || queue.getDeleted()) {

                    warnMsg =
                            new StringBuilder()
                                    .append("Print to unknown queue [")
                                    .append(requestedQueueUrlPath)
                                    .append("] denied from ")
                                    .append(remoteAddr).toString();

                } else if (queue.getDisabled()) {

                    warnMsg =
                            new StringBuilder()
                                    .append("Print to disabled queue [")
                                    .append(requestedQueueUrlPath)
                                    .append("] denied from ")
                                    .append(remoteAddr).toString();

                } else {

                    warnMsg = null;

                }

                if (warnMsg != null) {
                    AdminPublisher.instance().publish(PubTopicEnum.IPP,
                            PubLevelEnum.WARN, warnMsg);
                }

            }

            /*
             * Trace logging ...
             */
            if (LOGGER.isTraceEnabled()) {
                logIppOutputTrace(bos);
            }

            /*
             * Finishing up.
             */
            final ResourceStreamRequestHandler handler =
                    new SpStreamRequestHandler(new ByteArrayInputStream(
                            bos.toByteArray()));
            handler.setContentDisposition(ContentDisposition.INLINE);
            requestCycle.scheduleRequestHandlerAfterCurrent(handler);

        } catch (Exception e) {

            AdminPublisher.instance().publish(PubTopicEnum.IPP,
                    PubLevelEnum.ERROR, e.getMessage());

            try {
                /*
                 * Prevent continuous messaging when IPP client keeps retrying
                 * with same result.
                 */
                Thread.sleep(5 * DateUtil.DURATION_MSEC_SECOND);

            } catch (InterruptedException e1) {
                // noop
            }

            /*
             * This will produce HTTP Error 500 Internal server.
             */
            throw new SpException(e.getMessage(), e);

            // response.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED); // ??

        } finally {

            ServiceContext.close();
        }

    }

    /**
     * Handles a request for the SAVAPAGE.ppd file: the PPD file is returned
     * INLINE.
     *
     * @param response
     *            The response.
     */
    private void handlePpdRequest(final HttpServletResponse response) {

        response.setContentType(CONTENT_TYPE_PPD);
        response.setStatus(HttpServletResponse.SC_OK);

        final File file = ConfigManager.getPpdFile();

        if (!file.exists()) {
            throw new SpException("PPD file [" + file.getAbsolutePath()
                    + "] does NOT exist");
        }

        final IResourceStream resourceStream = new FileResourceStream(file);
        final ResourceStreamRequestHandler handler =
                new ResourceStreamRequestHandler(resourceStream);

        handler.setContentDisposition(ContentDisposition.INLINE);
        handler.setCacheDuration(Duration.NONE);

        getRequestCycle().scheduleRequestHandlerAfterCurrent(handler);
    }

    /**
     * Debug Log the request and parameters.
     *
     * @param request
     * @param parameters
     */
    private static void logDebug(final HttpServletRequest request,
            final PageParameters parameters) {

        final StringBuilder log = new StringBuilder(256).append('\n');

        log.append("Request [").append(request.getRequestURL().toString())
                .append("] From [").append(request.getRemoteAddr())
                .append("] Bytes [").append(request.getContentLength())
                .append("]\n");

        final Enumeration<String> headerNames = request.getHeaderNames();

        while (headerNames.hasMoreElements()) {

            final String name = headerNames.nextElement();
            final Enumeration<String> nameHeader = request.getHeaders(name);

            log.append("Header [").append(name).append("]:");

            while (nameHeader.hasMoreElements()) {
                log.append(" [").append(nameHeader.nextElement()).append("]");
            }
            log.append('\n');
        }

        for (final NamedPair namedPair : parameters.getAllNamed()) {
            log.append("Parameter [").append(namedPair.getKey())
                    .append("] = [").append(namedPair.getValue()).append("]\n");
        }

        LOGGER.debug(log.toString());
    }

    /**
     * Writes a pretty printed byte trace of the raw output stream to the log
     * file.
     *
     * @param bos
     *            The output stream.
     */
    private static void logIppOutputTrace(final ByteArrayOutputStream bos) {

        final int width = 10;

        final StringBuilder msg = new StringBuilder(1024);

        int i = 0;
        for (byte b : bos.toByteArray()) {

            if (i % width == 0) {
                msg.append("\n");
            }
            msg.append(String.format("0x%02X ", b));
            i++;
        }
        LOGGER.trace(msg.toString());
    }

}
