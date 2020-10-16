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

import org.cometd.bayeux.ChannelId;
import org.cometd.bayeux.server.Authorizer;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.savapage.core.cometd.AdminPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class AdminChannelAuthorizer implements Authorizer {

    /**
     *
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AdminChannelAuthorizer.class);

    @Override
    public Result authorize(final Operation operation, final ChannelId channel,
            final ServerSession session, final ServerMessage message) {

        /*
         * We DO allow shallow and deep wild channel, i.e. /admin/* and
         * /admin/**
         *
         * See : channel.isWild()
         */
        boolean isAdminChannel = new ChannelId(AdminPublisher.CHANNEL_PUBLISH)
                .isParentOf(channel);

        if (!isAdminChannel) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn(
                        "authorization for channel [" + channel + "] ignored");
            }
            return Result.ignore();
        }

        /*
         * Always grant authorization to local clients on the server side (?)
         */
        if (session.isLocalSession()) {
            /*
             * TODO: check what localSession exactly means. Server is the
             * originator of message?
             */
            // return Result.grant();
        }

        /*
         * Retrieve session attribute 'isAdmin' as set in handshake handler of
         * our BayeuxAuthenticator.
         */
        final Boolean isAdmin = (Boolean) session
                .getAttribute(BayeuxAuthenticator.SERVER_SESSION_ATTR_IS_ADMIN);

        if (isAdmin != null && isAdmin) {
            return Result.grant();
        }

        final String msg = "Only SavaPage admins can ";

        if (operation == Operation.CREATE) {
            return Result.deny(msg + "create admin channels");
        } else if (operation == Operation.PUBLISH) {
            return Result.deny(msg + "publish to admin channels");
        } else if (operation == Operation.SUBSCRIBE) {
            return Result.deny(msg + "subscribe to admin channels");
        }
        return Result.ignore();
    }

}
