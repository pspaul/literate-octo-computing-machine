/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.cometd;

import java.util.Map;

import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.server.DefaultSecurityPolicy;
import org.savapage.common.dto.CometdConnectDto;
import org.savapage.core.cometd.CometdClientMixin;
import org.savapage.server.auth.ClientAppUserAuthManager;
import org.savapage.server.auth.UserAuthToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Authenticator implementing the Bayeux security policy.
 *
 * <p>
 * The following overrides are implemented:
 * <ul>
 * <li>Allow handshakes from valid SavaPage users only.</li>
 * <li>Allows creation of channel only from clients that handshook and only if
 * the channel is not a meta channel (inherited from DefaultSecurityPolicy), and
 * only if client is administrator (or local server session).</li>
 * </ul>
 * <p>
 * The following defaults are inherited from DefaultSecurityPolicy:
 * <ul>
 * <li>allows subscription from clients that handshook, but not if the channel
 * is a meta channel</li>
 * <li>allows publish from clients that handshook to any channel that is not a
 * meta channel</li>
 * </ul>
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public final class BayeuxAuthenticator extends DefaultSecurityPolicy
        implements ServerSession.RemoveListener {

    /**
     * Name of ServerSession attribute for admin indicator: value object
     * {@code Boolean}.
     */
    public static final String SERVER_SESSION_ATTR_IS_ADMIN = "isAdmin";

    /**
     *
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(BayeuxAuthenticator.class);

    @Override
    public boolean canHandshake(final BayeuxServer server,
            final ServerSession session, final ServerMessage message) {

        /*
         * We allow handshake for any server-side local session, i.e. such as
         * those associated with services.
         */
        if (session.isLocalSession()) {
            LOGGER.debug("handshake from LocalSession accepted");
            return true;
        }

        /*
         * Extract the authentication information from the message sent by the
         * client.
         */
        final Map<String, Object> ext = message.getExt();
        if (ext == null) {
            LOGGER.warn("handshake without EXT_FIELD");
            return false;
        }

        @SuppressWarnings("unchecked")
        final Map<String, Object> authentication = ((Map<String, Object>) ext
                .get(CometdConnectDto.SERVER_MSG_ATTR_AUTH));

        if (authentication == null) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("handshake EXT_FIELD without "
                        + CometdConnectDto.SERVER_MSG_ATTR_AUTH);
            }
            return false;
        }
        /*
         * We verify the authentication information sent by the client, and
         * obtain back server-side authentication data that we can later
         * associate with the remote client
         */
        final Boolean isAdmin = checkAuthorization(authentication,
                message.getBayeuxContext().getRemoteAddress().getHostString());

        if (isAdmin == null) {
            return false;
        }

        /*
         * Authentication successful!
         */

        /*
         * Link authentication data to the session
         */
        session.setAttribute(SERVER_SESSION_ATTR_IS_ADMIN, isAdmin);

        /*
         * Be notified when the session disappears
         */
        session.addListener(this);

        return true;
    }

    @Override
    public boolean canCreate(final BayeuxServer server,
            final ServerSession session, final String channelId,
            final ServerMessage message) {

        /*
         * Enforces that only administrators and local sessions can CREATE
         * channels.
         */

        /*
         * We allow 'create' from any server-side local session, (such as those
         * associated with services).
         */
        boolean allow = session.isLocalSession();

        if (!allow) {
            /*
             * May be client is administrator?
             */
            Boolean isAdmin = (Boolean) session
                    .getAttribute(SERVER_SESSION_ATTR_IS_ADMIN);

            if (isAdmin != null) {
                allow = isAdmin;
            }
        }
        return allow && super.canCreate(server, session, channelId, message);
    }

    @Override
    public void removed(final ServerSession session, final boolean expired) {
        /*
         * Unlink authentication data from the remote client
         */
        session.removeAttribute(SERVER_SESSION_ATTR_IS_ADMIN);
    }

    /**
     * Verifies the authentication information sent by the client.
     *
     * @param auth
     *            The authentication object.
     * @return {@code null} if NOT authorized, {@code true} if authorized as
     *         admin, and {@code false} if authorized as User or Device.
     */
    private Boolean checkAuthorization(final Map<String, Object> auth,
            final String clientIpAddress) {

        Boolean isAdmin = null;

        final Object sharedTokenObj =
                auth.get(CometdConnectDto.SERVER_MSG_ATTR_SHARED_TOKEN);

        if (sharedTokenObj != null) {

            final String sharedToken = sharedTokenObj.toString();

            if (sharedToken.equals(CometdClientMixin.SHARED_USER_ADMIN_TOKEN)) {

                isAdmin = Boolean.TRUE;

            } else if (sharedToken
                    .equals(CometdClientMixin.SHARED_USER_TOKEN)) {

                final Object userTokenObj =
                        auth.get(CometdConnectDto.SERVER_MSG_ATTR_USER_TOKEN);

                if (userTokenObj == null) {

                    isAdmin = Boolean.FALSE;

                } else {

                    final String userToken = userTokenObj.toString();

                    final UserAuthToken userAuthToken = ClientAppUserAuthManager
                            .getIpAuthToken(clientIpAddress);

                    if (userAuthToken != null
                            && userAuthToken.getToken().equals(userToken)) {
                        isAdmin = Boolean.FALSE;
                    }

                }

            } else if (sharedToken
                    .equals(CometdClientMixin.SHARED_DEVICE_TOKEN)) {

                isAdmin = Boolean.FALSE;
            }
        }
        return isAdmin;
    }
}