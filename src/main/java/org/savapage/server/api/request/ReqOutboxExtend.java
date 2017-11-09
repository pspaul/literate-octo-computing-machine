/*
 * This file is part of the SavaPage project <http://savapage.org>.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.api.request;

import java.io.IOException;

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

        final int minutes = 10;

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
