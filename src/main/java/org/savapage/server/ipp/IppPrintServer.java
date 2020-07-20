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

import org.apache.commons.lang3.BooleanUtils;
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
import org.savapage.core.dao.enums.ReservedIppQueueEnum;
import org.savapage.core.ipp.IppProcessingException;
import org.savapage.core.ipp.IppProcessingException.StateEnum;
import org.savapage.core.ipp.operation.AbstractIppOperation;
import org.savapage.core.ipp.operation.IppOperationContext;
import org.savapage.core.ipp.operation.IppOperationId;
import org.savapage.core.jpa.IppQueue;
import org.savapage.core.jpa.User;
import org.savapage.core.services.QueueService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.ServiceEntryPoint;
import org.savapage.core.services.UserService;
import org.savapage.core.util.InetUtils;
import org.savapage.server.WebApp;
import org.savapage.server.webapp.WebAppHelper;
import org.savapage.server.webapp.WebAppUser;
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
 * @author Rijk Ravestein
 *
 */
public class IppPrintServer extends WebPage implements ServiceEntryPoint {

    /** */
    private static final long serialVersionUID = 1L;

    /** */
    private static final String CONTENT_TYPE_PPD = "application/vnd.cups-ppd";

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(IppPrintServer.class);

    /** */
    private static final QueueService QUEUE_SERVICE =
            ServiceContext.getServiceFactory().getQueueService();

    /** */
    private static final UserService USER_SERVICE =
            ServiceContext.getServiceFactory().getUserService();

    /**
     * Milliseconds sleep after an exception.
     */
    private static final long MSEC_SLEEP_AFTER_EXCEPTION = 5000;

    /** */
    public static void init() {
        SpInfo.instance().log("IPP Print Server started.");
    }

    /**
     *
     */
    private static class IppResourceStream extends AbstractResourceStream {

        /** */
        private static final long serialVersionUID = 1L;

        /** */
        private final InputStream istr;

        /**
         *
         * @param istr
         *            Input stream.
         */
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
    private static class SpStreamRequestHandler
            extends ResourceStreamRequestHandler {

        /**
         * @param istr
         *            The {@link InputStream}.
         */
        SpStreamRequestHandler(final InputStream istr) {
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

        final HttpServletRequest request = (HttpServletRequest) requestCycle
                .getRequest().getContainerRequest();

        if (LOGGER.isDebugEnabled()) {
            logDebug(request, parameters);
        }

        final HttpServletResponse response = (HttpServletResponse) requestCycle
                .getResponse().getContainerResponse();

        final String contentTypeReq = request.getContentType();

        /*
         * Request for a PPD file. See Mantis #160, #650.
         */
        if (contentTypeReq == null
                && StringUtils.upperCase(request.getRequestURL().toString())
                        .endsWith(".PPD")) {
            handlePpdRequest(response);
            return;
        }

        /*
         * Redirect to /user page for content types other than IPP_CONTENT_TYPE.
         */
        if (contentTypeReq == null || !contentTypeReq
                .equalsIgnoreCase(IppOperationContext.CONTENT_TYPE_IPP)) {
            setResponsePage(WebAppUser.class);
            return;
        }

        /*
         * OK, we can handle the IPP request.
         */
        response.setContentType(IppOperationContext.CONTENT_TYPE_IPP);
        response.setStatus(HttpServletResponse.SC_OK);

        /*
         * NOTE: There is NO top level database transaction. Specialized methods
         * have their own database transaction.
         */
        ServiceContext.open();

        try {
            final String remoteAddr = WebAppHelper.getClientIP(request);

            /*
             * Get the Queue from the URL.
             */
            final IppPrintServerUrlParms serverPageParms =
                    new IppPrintServerUrlParms(
                            Url.parse(request.getRequestURL().toString()));

            final String requestedQueueUrlPath = serverPageParms.getPrinter();

            /*
             * Reserved queue?
             */
            final ReservedIppQueueEnum reservedQueueEnum =
                    QUEUE_SERVICE.getReservedQueue(requestedQueueUrlPath);

            /*
             * Find queue object.
             */
            final IppQueue queue = ServiceContext.getDaoContext()
                    .getIppQueueDao().findByUrlPath(requestedQueueUrlPath);

            /*
             * Access allowed?
             */
            if (reservedQueueEnum != null
                    && !reservedQueueEnum.isDriverPrint()) {

                throw new IppProcessingException(StateEnum.UNAVAILABLE,
                        String.format("Queue [%s] is not for driver print.",
                                reservedQueueEnum.getUiText()));

            } else if (queue == null || queue.getDeleted()) {

                throw new IppProcessingException(StateEnum.UNAVAILABLE,
                        "Queue does not exist.");

            } else if (reservedQueueEnum != ReservedIppQueueEnum.IPP_PRINT_INTERNET
                    && StringUtils.isBlank(queue.getIpAllowed())
                    && InetUtils.isPublicAddress(remoteAddr)) {

                throw new IppProcessingException(StateEnum.UNAVAILABLE,
                        String.format(
                                "Queue [%s] is not accessible"
                                        + " from the Internet.",
                                queue.getUrlPath()));
            } else {

                if (!QUEUE_SERVICE.hasClientIpAccessToQueue(queue,
                        serverPageParms.getPrinter(), remoteAddr)) {
                    throw new IppProcessingException(StateEnum.UNAVAILABLE,
                            String.format(
                                    "Queue [%s] is not allowed for IP address.",
                                    queue.getUrlPath()));
                }
            }

            /*
             * Authenticated User ID associated with Internet Print or remote IP
             * address.
             */
            final String authUser;
            final boolean isAuthUserIppRequester;

            if (reservedQueueEnum == ReservedIppQueueEnum.IPP_PRINT_INTERNET) {

                final User remoteInternetUser = USER_SERVICE
                        .findUserByNumberUuid(serverPageParms.getUserNumber(),
                                serverPageParms.getUserUuid());

                if (remoteInternetUser == null) {
                    throw new IppProcessingException(StateEnum.UNAVAILABLE,
                            "Print service not available for user/uuid.");
                }

                authUser = remoteInternetUser.getUserId();
                isAuthUserIppRequester = true;

            } else {

                final String authUserByIP =
                        WebApp.getAuthUserByIpAddr(remoteAddr);

                if (authUserByIP == null) {
                    authUser = ConfigManager.getTrustedUserByIP(remoteAddr);
                } else {
                    authUser = authUserByIP;
                }

                isAuthUserIppRequester =
                        BooleanUtils.isNotTrue(queue.getTrusted());
            }

            /*
             * Handle the request.
             */
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();

            final IppOperationContext ippOperationContext =
                    new IppOperationContext();

            ippOperationContext.setRemoteAddr(remoteAddr);
            ippOperationContext.setRequestedQueueUrlPath(requestedQueueUrlPath);
            ippOperationContext
                    .setIppRoutingListener(WebApp.get().getPluginManager());

            final IppOperationId ippOperationId = AbstractIppOperation.handle(
                    queue, request.getInputStream(), bos, authUser,
                    isAuthUserIppRequester, ippOperationContext);

            if (ippOperationId != null
                    && ippOperationId == IppOperationId.VALIDATE_JOB) {

                final String warnMsg;

                if (reservedQueueEnum != null
                        && !reservedQueueEnum.isDriverPrint()) {

                    warnMsg = new StringBuilder()
                            .append("Print to reserved queue [")
                            .append(requestedQueueUrlPath)
                            .append("] denied from ").append(remoteAddr)
                            .toString();

                } else if (queue == null || queue.getDeleted()) {

                    warnMsg = new StringBuilder()
                            .append("Print to unknown queue [")
                            .append(requestedQueueUrlPath)
                            .append("] denied from ").append(remoteAddr)
                            .toString();

                } else if (queue.getDisabled()) {

                    warnMsg = new StringBuilder()
                            .append("Print to disabled queue [")
                            .append(requestedQueueUrlPath)
                            .append("] denied from ").append(remoteAddr)
                            .toString();

                } else {

                    warnMsg = null;

                }

                if (warnMsg != null) {
                    AdminPublisher.instance().publish(PubTopicEnum.IPP,
                            PubLevelEnum.WARN, warnMsg);
                }

            }

            if (LOGGER.isTraceEnabled()) {
                logIppOutputTrace(bos);
            }

            // Finishing up.
            scheduleRequestHandlerAfterCurrent(requestCycle, bos.toByteArray());

        } catch (IOException | IppProcessingException e) {

            AdminPublisher.instance().publish(PubTopicEnum.IPP,
                    PubLevelEnum.ERROR, e.getMessage());

            try {
                /*
                 * Prevent continuous messaging when IPP client keeps retrying
                 * with same result.
                 */
                Thread.sleep(MSEC_SLEEP_AFTER_EXCEPTION);

            } catch (InterruptedException e1) {
                // noop
            }

            // Dummy byte for unavailable service.
            scheduleRequestHandlerAfterCurrent(requestCycle, new byte[1]);

            final int httpStatus;

            if (e instanceof IOException) {
                httpStatus = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            } else {
                final IppProcessingException procEx =
                        (IppProcessingException) e;

                switch (procEx.getProcessingState()) {
                case CONTENT_ERROR:
                    httpStatus = HttpServletResponse.SC_NOT_ACCEPTABLE;
                    break;
                case UNAUTHORIZED:
                    httpStatus = HttpServletResponse.SC_UNAUTHORIZED;
                    break;
                case UNAVAILABLE:
                    httpStatus = HttpServletResponse.SC_SERVICE_UNAVAILABLE;
                    break;
                case INTERNAL_ERROR:
                    // no break intended
                default:
                    httpStatus = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                    break;
                }
            }
            response.setStatus(httpStatus);

        } finally {

            ServiceContext.close();
        }
    }

    /**
     * Schedules the request handler to be executed after the current one.
     *
     * @param requestCycle
     *            The request cycle.
     * @param buf
     *            The buffer to handle.
     */
    private static void scheduleRequestHandlerAfterCurrent(
            final RequestCycle requestCycle, final byte[] buf) {

        final ResourceStreamRequestHandler handler =
                new SpStreamRequestHandler(new ByteArrayInputStream(buf));

        handler.setContentDisposition(ContentDisposition.INLINE);
        requestCycle.scheduleRequestHandlerAfterCurrent(handler);
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
            throw new SpException(
                    "PPD file [" + file.getAbsolutePath() + "] does NOT exist");
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
     *            HTTP request.
     * @param parameters
     *            Page parameters.
     */
    private static void logDebug(final HttpServletRequest request,
            final PageParameters parameters) {

        final StringBuilder log = new StringBuilder();

        log.append("\nRequest [").append(request.getRequestURL().toString())
                .append("] From [").append(WebAppHelper.getClientIP(request))
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
            log.append("Parameter [").append(namedPair.getKey()).append("] = [")
                    .append(namedPair.getValue()).append("]\n");
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
