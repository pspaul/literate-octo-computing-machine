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
package org.savapage.server.cometd;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.codehaus.jackson.map.ObjectMapper;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.DeviceDao;
import org.savapage.core.dao.helpers.DeviceTypeEnum;
import org.savapage.core.jpa.Device;
import org.savapage.core.rfid.RfidEvent;
import org.savapage.core.rfid.RfidNumberFormat;
import org.savapage.core.rfid.RfidReaderManager;
import org.savapage.core.services.DeviceService.DeviceAttrLookup;
import org.savapage.core.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens to and handles {@linkplain #CHANNEL_SUBSCRIPTION} where
 * clients publish service requests to be notified of <b>device</b> events like
 * Card swipe on an associated RFID Network Reader. Events are published on
 * {@linkplain #CHANNEL_PUBLISH}.
 *
 * @author Datraverse B.V.
 */
public final class DeviceEventService extends AbstractEventService {

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
    private static final String CHANNEL_SUBSCRIPTION = "/service/device";

    /**
     * The channel this <i>service</i> <strong>publishes</strong> (writes
     * response) to.
     * <p>
     * This is the channel where the <i>client</i> <strong>subscribes</strong>
     * to.
     * </p>
     */
    private static final String CHANNEL_PUBLISH = "/device/event";

    /**
     * The name of the Java method (in this class) to call when messages are
     * received on this channel.
     */
    private static final String CHANNEL_MESSAGE_HANDLER = "monitorDeviceEvent";

    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(DeviceEventService.class);

    /**
     *
     * @param bayeux
     *            The bayeux instance
     * @param name
     *            The name of the service
     */
    public DeviceEventService(final BayeuxServer bayeux, final String name) {
        super(bayeux, name);
        addService(CHANNEL_SUBSCRIPTION, CHANNEL_MESSAGE_HANDLER);
        LOGGER.trace("DEVICE EVENT SERVICE ADDED");
    }

    /**
     * Monitors any event that should be notified to a device.
     *
     * @param remote
     * @param message
     */
    public void monitorDeviceEvent(final ServerSession remote,
            final ServerMessage message) {

        final String clientIpAddress = getClientIpAddress();

        Map<String, Object> eventData = null;

        ServiceContext.open();

        final DaoContext daoContext = ServiceContext.getDaoContext();
        final DeviceDao deviceDao = daoContext.getDeviceDao();

        try {
            /*
             * Find the TERMINAL and linked CARD_READER device.
             */
            final Device userDevice =
                    deviceDao.findByHostDeviceType(clientIpAddress,
                            DeviceTypeEnum.TERMINAL);

            if (userDevice == null) {
                throw new SpException("No Terminal Device found for ["
                        + clientIpAddress + "]");
            }

            final Device readerDevice = userDevice.getCardReader();

            if (readerDevice == null) {
                throw new SpException(
                        "No Reader Device found for Terminal Device ["
                                + clientIpAddress + "]");
            }
            final String readerIpAddress = readerDevice.getHostname();

            if (!readerDevice.getDeviceType().equals(
                    DeviceTypeEnum.CARD_READER.toString())) {

                throw new SpException("Reader Device [" + "] for ["
                        + clientIpAddress + "] is not type "
                        + DeviceTypeEnum.CARD_READER);
            }

            final DeviceAttrLookup lookup = new DeviceAttrLookup(readerDevice);
            final RfidNumberFormat rfidNumberFormat =
                    ServiceContext.getServiceFactory().getDeviceService()
                            .createRfidNumberFormat(readerDevice, lookup);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("START reader [" + readerIpAddress
                        + "] event monitoring for device [" + clientIpAddress
                        + "]");
            }

            /*
             * Mantis #328
             */
            remote.addListener(new ServerSession.RemoveListener() {
                @Override
                public void removed(ServerSession session, boolean timeout) {

                    if (!timeout) {
                        return;
                    }

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Listener removed (timeout) "
                                + "for reader [" + readerIpAddress
                                + "] event monitoring for device ["
                                + clientIpAddress + "]");
                    }

                    try {

                        RfidReaderManager.reportEvent(readerIpAddress,
                                new RfidEvent(RfidEvent.EventEnum.VOID));

                    } catch (InterruptedException e) {
                        if (LOGGER.isInfoEnabled()) {
                            LOGGER.info("Listener removed (timeout) "
                                    + "for reader [" + readerIpAddress
                                    + "] event for device [" + clientIpAddress
                                    + "]. RfidReaderManager reportEvent(VOID) "
                                    + "failed because interrupted: "
                                    + e.getMessage());
                        }
                    }
                }
            });

            /*
             * Blocking till event (or timeout)
             */
            eventData =
                    watchCardReaderEvent(readerDevice.getHostname(),
                            rfidNumberFormat);

            if (eventData == null) {
                eventData = new HashMap<String, Object>();
                eventData.put("event", "");
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("STOP reader [" + readerIpAddress
                        + "] event monitoring for device [" + clientIpAddress
                        + "]");
            }

        } catch (InterruptedException e) {

            eventData = new HashMap<String, Object>();
            eventData.put("event", "");

        } catch (Exception e) {

            eventData = new HashMap<String, Object>();

            if (ConfigManager.isShutdownInProgress()) {

                eventData.put("event", "");

            } else {

                LOGGER.error(e.getMessage(), e);

                eventData.put("event", "error");
                eventData.put("error", e.getMessage());
            }

        } finally {

            daoContext.rollback();
            ServiceContext.close();
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
            remote.deliver(getServerSession(), CHANNEL_PUBLISH, jsonEvent);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Delivered event for device [" + clientIpAddress
                        + "]");
            }

        } catch (Exception e) {

            if (!ConfigManager.isShutdownInProgress()) {
                LOGGER.error(e.getMessage());
            }

            throw new SpException(e);
        }

    }

    /**
     * Waits for a reader device event.
     *
     * @param readerIpAddress
     *            The IP-address of the Reader Device.
     * @param rfidNumberFormat
     *            The format of the RFID number.
     * @return <code>null</code>when the max wait time has elapsed, or a object
     *         map with information about the event.
     * @throws InterruptedException
     */
    private Map<String, Object> watchCardReaderEvent(
            final String readerIpAddress, RfidNumberFormat rfidNumberFormat)
            throws InterruptedException {

        Map<String, Object> eventData = null;

        final RfidEvent event =
                RfidReaderManager.waitForEvent(readerIpAddress,
                        rfidNumberFormat, theMaxMonitorMsec,
                        TimeUnit.MILLISECONDS);

        if (event != null && event.getEvent() == RfidEvent.EventEnum.CARD_SWIPE) {
            eventData = new HashMap<String, Object>();
            eventData.put("event", "card-swipe");
            eventData.put("cardNumber", event.getCardNumber());
        }

        return eventData;
    }
}
