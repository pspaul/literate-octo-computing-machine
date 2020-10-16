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
package org.savapage.server.api.request;

import java.io.IOException;

import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.jpa.User;
import org.savapage.server.session.SpSession;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqOutboxExtend extends ApiRequestMixin {

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final int minutes = ConfigManager.instance()
                .getConfigInt(Key.PROXY_PRINT_HOLD_EXTEND_MINS);

        final int jobCount = OUTBOX_SERVICE
                .extendOutboxExpiry(lockedUser.getUserId(), minutes);

        final String msgKey;

        if (jobCount == 0) {
            msgKey = "msg-outbox-extended-none";
        } else if (jobCount == 1) {
            msgKey = "msg-outbox-extended-one";
        } else {
            msgKey = "msg-outbox-extended-multiple";
        }

        this.setApiResult(ApiResultCodeEnum.OK, msgKey,
                String.valueOf(jobCount), String.valueOf(minutes));

        ApiRequestHelper.addUserStats(this.getUserData(), lockedUser,
                this.getLocale(), SpSession.getAppCurrencySymbol());

    }

}
