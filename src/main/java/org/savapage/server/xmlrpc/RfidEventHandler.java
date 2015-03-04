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
package org.savapage.server.xmlrpc;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.savapage.core.PerformanceLogger;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.community.MemberCard;
import org.savapage.core.concurrent.ReadWriteLockEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.DeviceDao;
import org.savapage.core.dao.helpers.DeviceTypeEnum;
import org.savapage.core.dao.helpers.ProxyPrintAuthModeEnum;
import org.savapage.core.jpa.Device;
import org.savapage.core.jpa.User;
import org.savapage.core.print.proxy.ProxyPrintAuthManager;
import org.savapage.core.print.proxy.ProxyPrintException;
import org.savapage.core.rfid.RfidEvent;
import org.savapage.core.rfid.RfidReaderManager;
import org.savapage.core.services.DeviceService;
import org.savapage.core.services.InboxService;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.ServiceEntryPoint;
import org.savapage.core.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Datraverse B.V.
 */
public class RfidEventHandler implements ServiceEntryPoint {

    /**
     * The key for the (required) return code.
     */
    private static final String KEY_RC = "rc";

    /**
     * The key for the (optional) message.
     */
    private static final String KEY_MESSAGE = "message";

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(RfidEventHandler.class);

    private static final AdminPublisher ADMIN_PUBLISHER = AdminPublisher
            .instance();

    private static final DeviceService DEVICE_SERVICE = ServiceContext
            .getServiceFactory().getDeviceService();

    private static final InboxService INBOX_SERVICE = ServiceContext
            .getServiceFactory().getInboxService();

    private static final ProxyPrintAuthManager PROXYPRINT_AUTHMANAGER =
            ProxyPrintAuthManager.instance();

    private static final UserService USER_SERVICE = ServiceContext
            .getServiceFactory().getUserService();

    /**
    *
    */
    private static final ProxyPrintService PROXY_PRINT_SERVICE = ServiceContext
            .getServiceFactory().getProxyPrintService();

    /**
     *
     */
    private static final Integer RC_ACCEPT = Integer.valueOf(0);
    private static final Integer RC_DENY = Integer.valueOf(1);
    private static final Integer RC_EXCEPTION = Integer.valueOf(99);

    /**
     * Handles a card swipe from a card reader {@link Device}.
     *
     * @param apiId
     *            The API id.
     * @param apiKey
     *            The API key.
     * @param cardNumber
     *            The RFID card number.
     * @return The map with the {@link #KEY_RC} and {@link #KEY_MESSAGE} value.
     */
    public final Map<String, Object> cardSwipe(final String apiId,
            final String apiKey, final String cardNumber) {

        final Date perfStartTime = PerformanceLogger.startTime();

        final Map<String, Object> map = new HashMap<String, Object>();

        Integer rc = RC_DENY;

        ServiceContext.open();

        final DaoContext daoContext = ServiceContext.getDaoContext();
        final DeviceDao deviceDao = daoContext.getDeviceDao();

        ReadWriteLockEnum.DATABASE_READONLY.setReadLock(true);

        try {
            /*
             * NOTE: when apiId/Key is invalid an exception is thrown.
             */
            MemberCard.instance().validateContent(apiId, apiKey);

            final String clientIpAddress = SpXmlRpcServlet.getClientIpAddress();

            /*
             * Find the card reader.
             */
            final Device cardReader =
                    deviceDao.findByHostDeviceType(clientIpAddress,
                            DeviceTypeEnum.CARD_READER);

            if (cardReader == null) {

                onCardReaderUnknown(map, clientIpAddress, cardNumber);

            } else if (cardReader.getDisabled()) {

                onCardReaderDisabled(map, clientIpAddress, cardNumber);

            } else {
                rc = onCardSwipe(map, clientIpAddress, cardNumber, cardReader);
            }

        } catch (ProxyPrintException ex) {

            rc = RC_DENY;

            ADMIN_PUBLISHER.publish(PubTopicEnum.PROXY_PRINT,
                    PubLevelEnum.WARN, ex.getMessage());

            map.put(KEY_MESSAGE, ex.getMessage());

            LOGGER.info(ex.getMessage());

        } catch (Exception ex) {

            rc = RC_EXCEPTION;

            map.put(KEY_MESSAGE, ex.getMessage());

            if (!ConfigManager.isShutdownInProgress()) {

                ADMIN_PUBLISHER.publish(PubTopicEnum.NFC, PubLevelEnum.ERROR,
                        ex.getMessage());
                LOGGER.error(ex.getMessage(), ex);
            }

        } finally {

            try {

                daoContext.rollback();

            } catch (Exception ex) {

                if (!ConfigManager.isShutdownInProgress()) {
                    LOGGER.error(ex.getMessage(), ex);
                }

            } finally {
                ServiceContext.close();
            }

            ReadWriteLockEnum.DATABASE_READONLY.setReadLock(false);

        }

        map.put(KEY_RC, rc);

        PerformanceLogger.log(this.getClass(), "cardSwipe", perfStartTime,
                cardNumber);

        return map;
    }

    /**
     * Handles unknown card reader.
     *
     * @param map
     *            The map with the {@link #KEY_RC} and {@link #KEY_MESSAGE}
     *            value.
     * @param clientIpAddress
     *            The client IP address.
     * @param cardNumber
     *            The card number.
     */
    private static void onCardReaderUnknown(final Map<String, Object> map,
            final String clientIpAddress, final String cardNumber) {

        final String msg =
                "Card " + cardNumber
                        + " swiped at unregistered network reader "
                        + clientIpAddress;

        ADMIN_PUBLISHER.publish(PubTopicEnum.NFC, PubLevelEnum.WARN, msg);

        LOGGER.warn(msg);

        map.put(KEY_MESSAGE, "Unknown Card Reader [" + clientIpAddress + "]");
    }

    /**
     * Handles disabled card reader.
     *
     * @param map
     *            The map with the {@link #KEY_RC} and {@link #KEY_MESSAGE}
     *            value.
     * @param clientIpAddress
     *            The client IP address.
     * @param cardNumber
     *            The card number.
     */
    private static void onCardReaderDisabled(final Map<String, Object> map,
            final String clientIpAddress, final String cardNumber) {

        final String msg =
                "Card " + cardNumber + " swiped at disabled network reader "
                        + clientIpAddress;

        ADMIN_PUBLISHER.publish(PubTopicEnum.NFC, PubLevelEnum.WARN, msg);

        LOGGER.warn(msg);

        map.put(KEY_MESSAGE, "Disabled Card Reader [" + clientIpAddress + "]");
    }

    /**
     * Checks if a direct authenticated print is pending of a single printer or
     * printer group related to a card reader.
     *
     * @param cardReader
     *            The card reader.
     * @return {@code true} if an authenticated print is pending.
     */
    private static boolean isDirectAuthReqPending(final Device cardReader) {

        final Set<String> printerNames =
                DEVICE_SERVICE.collectPrinterNames(cardReader);

        boolean isPending = false;

        final Iterator<String> iter = printerNames.iterator();

        while (iter.hasNext()) {

            final String printerName = iter.next();

            isPending =
                    PROXYPRINT_AUTHMANAGER.isAuthPendingForPrinter(printerName);

            if (isPending) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Authorisation request for printer ["
                            + printerName + "] pending ");
                }
                break;
            }
        }

        return isPending;
    }

    /**
     * Handles a card swipe on a card reader.
     *
     * @param map
     *            The map with the {@link #KEY_RC} and {@link #KEY_MESSAGE}
     *            value.
     * @param clientIpAddress
     *            The client IP address.
     * @param cardNumber
     *            The card number.
     * @param cardReader
     *            The card reader {@link Device}.
     * @return The return code for the XML-RPC request.
     * @throws ProxyPrintException
     *             When logical proxy print errors.
     * @throws InterruptedException
     *             When thread is interrupted.
     */
    private static Integer onCardSwipe(final Map<String, Object> map,
            final String clientIpAddress, final String cardNumber,
            final Device cardReader) throws ProxyPrintException,
            InterruptedException {

        String msg =
                "Card " + cardNumber + " swiped at network reader "
                        + clientIpAddress;

        /*
         * Check if Card Reader supports Fast|Hold Print.
         */
        final ProxyPrintAuthModeEnum authMode =
                DEVICE_SERVICE.getProxyPrintAuthMode(cardReader.getId());

        final boolean isFastProxyPrintSupported =
                authMode != null && (authMode.isFast());

        final boolean isHoldReleasePrintSupported =
                authMode != null && (authMode.isHoldRelease());

        final boolean doHoldFastProxyPrint;

        if (isFastProxyPrintSupported || isHoldReleasePrintSupported) {

            doHoldFastProxyPrint = !isDirectAuthReqPending(cardReader);

            if (doHoldFastProxyPrint) {
                msg += " (proxy print release)";
            }

        } else {
            doHoldFastProxyPrint = false;
        }

        ADMIN_PUBLISHER.publish(PubTopicEnum.NFC, PubLevelEnum.INFO, msg);

        LOGGER.debug(msg);

        /*
         * Find the user of the card.
         */
        final User user = USER_SERVICE.findUserByCardNumber(cardNumber);

        if (user == null) {
            throw new ProxyPrintException("No user found for card ["
                    + cardNumber + "]");
        }

        /*
         *
         */
        if (doHoldFastProxyPrint) {

            /*
             * Hold|Fast Proxy Print.
             */
            doHoldFastProxyPrint(map, clientIpAddress, cardNumber, cardReader,
                    user, isHoldReleasePrintSupported);

        } else {

            if (!PROXYPRINT_AUTHMANAGER.isAuthPendingForUser(user.getId())) {

                throw new ProxyPrintException(
                        "Pending print request for user [" + user.getUserId()
                                + "] of card [" + cardNumber
                                + "] not found or expired.");
            }

            /*
             * WebApp Proxy Print.
             */
            RfidReaderManager.reportEvent(clientIpAddress, new RfidEvent(
                    RfidEvent.EventEnum.CARD_SWIPE, cardNumber));

            final StringBuilder builder = new StringBuilder(96);
            builder.append("User [").append(user.getUserId())
                    .append("] authenticated pending print request.");

            map.put(KEY_MESSAGE, builder.toString());

        }

        return RC_ACCEPT;
    }

    /**
     * Checks if a Fast Print is always to be tried after a Hold Print try.
     * <p>
     * When the inbox is cleared after issuing a Hold Proxy Print, we can safely
     * Fast release new PrintIn jobs.
     * </p>
     *
     * @return {@code true} if a Fast Print is always to be tried.
     */
    private static boolean isAlwaysFastRelease() {

        final IConfigProp.Key key =
                IConfigProp.Key.WEBAPP_USER_PROXY_PRINT_CLEAR_INBOX;

        return ConfigManager.instance().isConfigValue(key);
    }

    /**
     *
     * @param map
     *            The map with the {@link #KEY_RC} and {@link #KEY_MESSAGE}
     *            value.
     * @param clientIpAddress
     *            The client IP address.
     * @param cardNumber
     *            The card number.
     * @param cardReader
     *            The card reader {@link Device}.
     * @param user
     *            The user of the card.
     * @param isHoldPrintSupported
     *            {@code true} if Hold Print is supported.
     * @throws ProxyPrintException
     *             When logical proxy print errors.
     */
    private static void doHoldFastProxyPrint(final Map<String, Object> map,
            final String clientIpAddress, final String cardNumber,
            final Device cardReader, final User user,
            final boolean isHoldPrintSupported) throws ProxyPrintException {

        int nPages = 0;

        final DaoContext daoContext = ServiceContext.getDaoContext();

        daoContext.beginTransaction();

        int nPagesHoldReleased = 0;

        /*
         * Step 1: try to Hold Print.
         */
        if (isHoldPrintSupported) {

            nPagesHoldReleased =
                    PROXY_PRINT_SERVICE
                            .proxyPrintOutbox(cardReader, cardNumber);

            nPages += nPagesHoldReleased;
        }

        final boolean isAlwaysFastRelease = isAlwaysFastRelease();

        /*
         * Step 2: try to Fast Print.
         */
        if (nPagesHoldReleased == 0 || isAlwaysFastRelease) {

            /*
             * We need a new transaction, when Hold Print was executed.
             */
            if (!daoContext.isTransactionActive()) {
                daoContext.beginTransaction();
            }

            final int nPagesFastPrinted =
                    PROXY_PRINT_SERVICE.proxyPrintInboxFast(cardReader,
                            cardNumber);

            if (nPagesFastPrinted == 0 && nPagesHoldReleased == 0) {
                throw new ProxyPrintException("User [" + user.getUserId()
                        + "] has no pages for proxy printing.");
            }

            nPages += nPagesFastPrinted;
        }

        if (nPagesHoldReleased > 0 && !isAlwaysFastRelease) {
            /*
             * Clear the inbox to prevent Fast Print at next card swipe.
             */
            INBOX_SERVICE.deleteAllPages(user.getUserId());
        }

        daoContext.commit();

        final StringBuilder builder = new StringBuilder(96);

        builder.append("User [").append(user.getUserId())
                .append("] released [").append(nPages)
                .append("] proxy printed page");

        if (nPages > 1) {
            builder.append("s");
        }

        builder.append(" to printer [")
                .append(cardReader.getPrinter().getPrinterName()).append("].");

        map.put(KEY_MESSAGE, builder.toString());
    }
}
