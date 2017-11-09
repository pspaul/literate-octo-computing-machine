/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
 * Authors: Rijk Ravestein.
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
package org.savapage.server.api.request;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.DeviceDao;
import org.savapage.core.dao.enums.DeviceTypeEnum;
import org.savapage.core.dto.AccountDisplayInfoDto;
import org.savapage.core.jpa.Device;
import org.savapage.core.jpa.User;
import org.savapage.core.msg.UserMsgIndicator;
import org.savapage.core.outbox.OutboxInfoDto;
import org.savapage.core.services.AccountingService;
import org.savapage.core.services.OutboxService;
import org.savapage.core.services.ServiceContext;
import org.savapage.server.WebApp;
import org.savapage.server.session.SpSession;
import org.savapage.server.webapp.WebAppTypeEnum;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ApiRequestHelper {

    /**
     * .
     */
    private static final AccountingService ACCOUNTING_SERVICE =
            ServiceContext.getServiceFactory().getAccountingService();

    /**
     * .
     */
    private static final OutboxService OUTBOX_SERVICE =
            ServiceContext.getServiceFactory().getOutboxService();

    /**
     * Prevents public instantiation.
     */
    private ApiRequestHelper() {
    }

    /**
     *
     * @return {@code true} when login via browser local storage auth token is
     *         enabled.
     */
    public static boolean isAuthTokenLoginEnabled() {
        return ConfigManager.instance()
                .isConfigValue(Key.WEB_LOGIN_AUTHTOKEN_ENABLE);
    }

    /**
     * Adds user statistics at the {@code stats} key of jsonData.
     * <p>
     * The User Statistics are used by the Client WebApp to update the Pie-chart
     * graphics.
     * </p>
     *
     * @param jsonData
     *            The JSON data to add the {@code stats} key to.
     * @param user
     *            The User to get the statistics from.
     * @param locale
     *            The locale.
     * @param currencySymbol
     *            The currency symbol.
     */
    public static void addUserStats(final Map<String, Object> jsonData,
            final User user, final Locale locale, final String currencySymbol) {

        final Map<String, Object> stats = new HashMap<>();

        stats.put("pagesPrintIn", user.getNumberOfPrintInPages());
        stats.put("pagesPrintOut", user.getNumberOfPrintOutPages());
        stats.put("pagesPdfOut", user.getNumberOfPdfOutPages());

        final AccountDisplayInfoDto dto = ACCOUNTING_SERVICE
                .getAccountDisplayInfo(user, locale, currencySymbol);

        stats.put("accountInfo", dto);

        final OutboxInfoDto outbox = OUTBOX_SERVICE.getOutboxJobTicketInfo(user,
                ServiceContext.getTransactionDate());

        OUTBOX_SERVICE.applyLocaleInfo(outbox, locale, currencySymbol);

        stats.put("outbox", outbox);

        jsonData.put("stats", stats);
    }

    /**
     * Interrupts all current User Web App long polls for this user.
     * <p>
     * If the user id is the reserved 'admin', the interrupt is NOT applied.
     * </p>
     *
     * @param userId
     *            The user id.
     * @param remoteAddr
     *            the IP address of the client that sent the request
     * @throws IOException
     *             When the interrupt message could not be written (to the
     *             message file).
     */
    public static void interruptPendingLongPolls(final String userId,
            final String remoteAddr) throws IOException {

        if (!ConfigManager.isInternalAdmin(userId)) {
            UserMsgIndicator.write(userId, new Date(),
                    UserMsgIndicator.Msg.STOP_POLL_REQ, remoteAddr);
        }
    }

    /**
     * Stops and replaces the underlying (Web)Session, invalidating the current
     * one and creating a new one.
     * <p>
     * NOTE: {@link #interruptPendingLongPolls(String, String)} is executed when
     * {@link WebAppTypeEnum#USER}.
     * </p>
     * <p>
     * NOTE: When replacing the session, the Wicket framework invalidate() calls
     * our {@link WebApp#sessionUnbound(String)} : this method publishes the
     * logout message.
     * </p>
     *
     * @param session
     *            The {@link SpSession}.
     * @param userId
     *            The user ID.
     * @param remoteAddr
     *            the IP address of the client that sent the request
     * @throws IOException
     *             When IO error.
     */
    public static void stopReplaceSession(final SpSession session,
            final String userId, final String remoteAddr) throws IOException {
        /*
         * Save the critical session attribute.
         */
        final WebAppTypeEnum savedWebAppType = session.getWebAppType();

        /*
         * IMPORTANT: Logout to remove the user and WebApp Type associated with
         * this session.
         */
        session.logout();

        /*
         * Replaces the underlying (Web)Session, invalidating the current one
         * and creating a new one. NOTE: data are copied from current session.
         */
        session.replaceSession();

        /*
         * Make sure that all User Web App long polls for this user are
         * interrupted.
         */
        if (userId != null && savedWebAppType == WebAppTypeEnum.USER) {
            interruptPendingLongPolls(userId, remoteAddr);
        }
    }

    /**
     * Gets the {@link Device.DeviceTypeEnum#TERMINAL} definition of the remote
     * client.
     *
     * @param remoteAddr
     *            the IP address of the client that sent the request
     *
     * @return {@code null} when no device definition is found.
     */
    public static Device getHostTerminal(final String remoteAddr) {

        final DeviceDao deviceDao =
                ServiceContext.getDaoContext().getDeviceDao();

        return deviceDao.findByHostDeviceType(remoteAddr,
                DeviceTypeEnum.TERMINAL);
    }

}
