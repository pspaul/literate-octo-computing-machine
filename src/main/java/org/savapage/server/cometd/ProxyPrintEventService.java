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
package org.savapage.server.cometd;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.codehaus.jackson.map.ObjectMapper;
import org.cometd.bayeux.Promise;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.savapage.core.SpException;
import org.savapage.core.concurrent.ReadWriteLockEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.DeviceDao;
import org.savapage.core.jpa.Device;
import org.savapage.core.jpa.User;
import org.savapage.core.msg.JsonUserMsgNotification;
import org.savapage.core.print.proxy.ProxyPrintAuthManager;
import org.savapage.core.print.proxy.ProxyPrintInboxReq;
import org.savapage.core.rfid.RfidNumberFormat;
import org.savapage.core.services.DeviceService.DeviceAttrLookup;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserService;
import org.savapage.server.api.JsonApiServer;
import org.savapage.server.api.request.ApiRequestHelper;
import org.savapage.server.api.request.ReqPrinterPrint;
import org.savapage.server.webapp.WebAppHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens to and handles {@linkplain #CHANNEL_SUBSCRIPTION} where
 * clients publish service requests to be notified of <b>proxy print</b> events.
 * Events are published on {@linkplain #CHANNEL_PUBLISH}.
 *
 * @author Rijk Ravestein
 *
 */
public final class ProxyPrintEventService extends AbstractEventService {

    private static final String KEY_EVENT = "event";
    private static final String KEY_ERROR = "error";
    private static final String KEY_DATA = "data";

    private static final String EVENT_PRINTED = "printed";
    private static final String EVENT_ERROR = "error";

    /** */
    private static final ProxyPrintService PROXY_PRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();

    /** */
    private static final UserService USER_SERVICE =
            ServiceContext.getServiceFactory().getUserService();

    /**
     * The channel this <i>service</i> <strong>subscribes</strong> (listens) to.
     * <p>
     * This is the channel where the <i>client</i> <strong>publishes</strong>
     * to.
     * </p>
     * <p>
     * This is a <strong>Service</strong> channel (because its name name starts
     * with /service/ and is used from client to server communication), contrary
     * to <strong>Normal</strong> channels (whose name starts with any other
     * string, except '/meta/', and are used to broadcast messages between
     * clients).
     * </p>
     */
    private static final String CHANNEL_SUBSCRIPTION = "/service/proxyprint";

    /**
     * The channel this <i>service</i> <strong>publishes</strong> (writes
     * response) to.
     * <p>
     * This is the channel where the <i>client</i> <strong>subscribes</strong>
     * to.
     * </p>
     */
    private static final String CHANNEL_PUBLISH = "/proxyprint/event";

    /**
     * The name of the Java method (in this class) to call when messages are
     * received on this channel.
     */
    private static final String CHANNEL_MESSAGE_HANDLER =
            "monitorProxyPrintEvent";

    /**
     *
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ProxyPrintEventService.class);

    /**
     *
     * @param bayeux
     *            The bayeux instance
     * @param name
     *            The name of the service
     */
    public ProxyPrintEventService(final BayeuxServer bayeux,
            final String name) {
        super(bayeux, name);
        addService(CHANNEL_SUBSCRIPTION, CHANNEL_MESSAGE_HANDLER);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("ProxyPrintEventService added");
        }
    }

    /**
     * Monitors any event that should be notified to a user.
     *
     * @param remote
     * @param message
     */
    public void monitorProxyPrintEvent(final ServerSession remote,
            final ServerMessage message) {

        final Map<String, Object> input = message.getDataAsMap();

        final String printerName = input.get("printerName").toString();
        final String readerName = input.get("readerName").toString();
        final Long idUser = Long.parseLong(input.get("idUser").toString());
        final Locale locale = this.getLocale(input, "language", "country");

        final String clientIpAddress = WebAppHelper.getClientIP(message);

        Map<String, Object> eventData = null;

        ReadWriteLockEnum.DATABASE_READONLY.setReadLock(true);

        ServiceContext.open();
        ServiceContext.setLocale(locale);

        final DaoContext daoContext = ServiceContext.getDaoContext();

        try {

            final Device readerDevice = getReaderDevice(readerName);
            final String readerIpAddress = readerDevice.getHostname();

            final DeviceAttrLookup lookup = new DeviceAttrLookup(readerDevice);
            final RfidNumberFormat rfidNumberFormat =
                    ServiceContext.getServiceFactory().getDeviceService()
                            .createRfidNumberFormat(readerDevice, lookup);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("START Proxy Printer [" + printerName
                        + "] reader [" + readerIpAddress
                        + "] event monitoring for client [" + clientIpAddress
                        + "] user [" + idUser + "]");
            }
            /*
             * Mantis #328
             */
            remote.addListener(new ServerSession.RemovedListener() {
                @Override
                public void removed(final ServerSession session,
                        final ServerMessage message, final boolean timeout) {

                    if (!timeout) {
                        return;
                    }

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Listener removed (timeout) "
                                + "for Proxy Printer [" + printerName
                                + "] reader [" + readerIpAddress
                                + "] event for client [" + clientIpAddress
                                + "] user [" + idUser + "]");
                    }

                    try {
                        ProxyPrintAuthManager.cancelRequest(idUser,
                                printerName);

                    } catch (InterruptedException e) {

                        if (LOGGER.isInfoEnabled()) {
                            LOGGER.info("Listener removed (timeout) "
                                    + "for Proxy Printer [" + printerName
                                    + "] reader [" + readerIpAddress
                                    + "] event for client [" + clientIpAddress
                                    + "] user [" + idUser
                                    + "]. ProxyPrintAuthManager cancelRequest "
                                    + "failed because interrupted: "
                                    + e.getMessage());
                        }
                    }
                }
            });

            /*
             * Blocking till event (or timeout).
             */
            eventData = watchAuthEvent(idUser, printerName, readerIpAddress,
                    rfidNumberFormat);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("STOP Proxy Printer [" + printerName + "] reader ["
                        + readerIpAddress + "] event monitoring for client ["
                        + clientIpAddress + "] user [" + idUser + "]");
            }

        } catch (InterruptedException e) {

            eventData = new HashMap<String, Object>();
            eventData.put(KEY_EVENT, EVENT_ERROR);
            eventData.put(KEY_ERROR, e.getMessage());

        } catch (Exception e) {

            if (!ConfigManager.isShutdownInProgress()) {
                LOGGER.error(e.getMessage(), e);
            }
            eventData = new HashMap<String, Object>();
            eventData.put(KEY_EVENT, EVENT_ERROR);
            eventData.put(KEY_ERROR, e.getMessage());

        } finally {

            daoContext.rollback();
            ServiceContext.close();

            ReadWriteLockEnum.DATABASE_READONLY.setReadLock(false);

        }

        /*
         *
         */
        try {
            String jsonEvent = new ObjectMapper().writeValueAsString(eventData);

            /*
             * The JavaScript client subscribes to CHANNEL_PUBLISH like this:
             * $.cometd.subscribe('/user/event', function(message) {
             */
            remote.deliver(getServerSession(), CHANNEL_PUBLISH, jsonEvent,
                    Promise.noop());

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Delivered event [" + jsonEvent + "] for device ["
                        + clientIpAddress + "]");
            }

        } catch (Exception e) {

            if (!ConfigManager.isShutdownInProgress()) {
                LOGGER.error(e.getMessage());
            }
            throw new SpException(e);
        }

    }

    /**
     *
     * @param em
     * @param readerName
     * @return
     */
    private Device getReaderDevice(final String readerName) {

        final DeviceDao deviceDao =
                ServiceContext.getDaoContext().getDeviceDao();

        final Device device = deviceDao.findByName(readerName);

        /*
         * INVARIANT: Device MUST exits.
         */
        if (device == null) {
            throw new SpException("Device [" + readerName + "] NOT found");
        }
        /*
         * INVARIANT: Device MUST be enabled.
         */
        if (device.getDisabled()) {
            throw new SpException("Device [" + readerName + "] is disabled");
        }

        /*
         * INVARIANT: Device MUST be a reader.
         */
        if (!deviceDao.isCardReader(device)) {
            throw new SpException(
                    "Device [" + readerName + "] is NOT a Card Reader");
        }

        /*
         * INVARIANT: Reader MUST have Printer restriction.
         */
        if (!deviceDao.hasPrinterRestriction(device)) {
            throw new SpException("Reader [" + readerName
                    + "] does not have associated Printer(s).");
        }

        return device;
    }

    /**
     * Waits for a Proxy Print Job authentication event.
     *
     * @param idUser
     * @param printerName
     * @param readerIpAddress
     *            The IP-address of the Reader Device.
     * @param rfidNumberFormat
     *            The format of the RFID number.
     * @return When the max wait time has elapsed, or an event is encountered.
     * @throws Exception
     */
    private Map<String, Object> watchAuthEvent(final Long idUser,
            final String printerName, final String readerIpAddress,
            RfidNumberFormat rfidNumberFormat) throws Exception {

        final Map<String, Object> eventData = new HashMap<String, Object>();

        /*
         * Be pessimistic, and assume error.
         */
        eventData.put(KEY_EVENT, EVENT_ERROR);

        final long timeout = ProxyPrintAuthManager.getMaxRequestAgeSeconds();

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Waiting [" + timeout + "] seconds for card swipe...");
        }

        final ProxyPrintInboxReq request = ProxyPrintAuthManager.waitForAuth(
                idUser, printerName, readerIpAddress, rfidNumberFormat,
                ProxyPrintAuthManager.getMaxRequestAgeSeconds(),
                TimeUnit.SECONDS);

        if (request == null) {

            LOGGER.trace("Timed out.");

            JsonApiServer.setApiResultMsgError(eventData, "",
                    "No print request found. Please try again.");

        } else if (request.isExpired()) {

            LOGGER.trace("Print request expired.");

            request.setUserMsg(this.localize("proxyprint-auth-expired"));

            ReqPrinterPrint.setApiResultMsg(eventData, request);

        } else if (request.isAuthenticated()) {

            LOGGER.trace("Card swipe authentication: start transaction.");

            final DaoContext daoContext = ServiceContext.getDaoContext();

            boolean isCommitted = false;

            try {

                daoContext.beginTransaction();

                final User lockedUser =
                        USER_SERVICE.lockUser(request.getIdUser());

                if (lockedUser == null) {
                    throw new SpException("user [" + request.getIdUser()
                            + "] cannot be found");
                }

                LOGGER.trace("Send print job...");

                PROXY_PRINT_SERVICE.proxyPrintInbox(lockedUser, request);

                ReqPrinterPrint.setApiResultMsg(eventData, request);

                if (request.getStatus() == ProxyPrintInboxReq.Status.PRINTED) {

                    ApiRequestHelper.addUserStats(eventData, lockedUser,
                            ServiceContext.getLocale(),
                            ServiceContext.getAppCurrencySymbol());

                    /*
                     *
                     */
                    eventData.put(KEY_EVENT, EVENT_PRINTED);
                    JsonUserMsgNotification json =
                            new JsonUserMsgNotification();
                    json.setMsgTime(System.currentTimeMillis());
                    eventData.put(KEY_DATA, json);

                    /*
                     *
                     */
                    daoContext.commit();

                    isCommitted = true;

                    LOGGER.trace("Print job accepted: transaction committed.");

                } else {

                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Not printed. Status ["
                                + request.getStatus() + "]");
                    }
                }

            } finally {

                if (!isCommitted) {
                    daoContext.rollback();
                    LOGGER.trace("Transaction rolled back.");
                }
            }

        } else {

            LOGGER.trace("Authentication failed ");

            request.setUserMsg(this.localize("proxyprint-auth-failed"));

            ReqPrinterPrint.setApiResultMsg(eventData, request);
        }

        return eventData;
    }

}
