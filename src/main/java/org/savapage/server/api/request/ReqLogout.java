/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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

import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.config.WebAppTypeEnum;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.jpa.User;
import org.savapage.core.msg.UserMsgIndicator;
import org.savapage.core.services.ServiceContext;
import org.savapage.server.auth.ClientAppUserAuthManager;
import org.savapage.server.auth.WebAppUserAuthManager;
import org.savapage.server.session.SpSession;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Logs out by replacing the underlying (Web)Session, invalidating the current
 * one and creating a new one. Also sends the
 * {@link UserMsgIndicator.Msg#STOP_POLL_REQ} message.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqLogout extends ApiRequestMixin {

    /**
     * .
     */
    @JsonInclude(Include.NON_NULL)
    private static class DtoReq extends AbstractDto {

        private String authToken;

        public String getAuthToken() {
            return authToken;
        }

        @SuppressWarnings("unused")
        public void setAuthToken(String authToken) {
            this.authToken = authToken;
        }

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final DtoReq dtoReq = DtoReq.create(DtoReq.class, getParmValueDto());

        final WebAppTypeEnum webAppType = this.getSessionWebAppType();

        ClientAppUserAuthManager.removeUserAuthToken(this.getRemoteAddr());

        WebAppUserAuthManager.instance()
                .removeUserAuthToken(dtoReq.getAuthToken(), webAppType);

        ApiRequestHelper.stopReplaceSession(SpSession.get(), requestingUser,
                this.getRemoteAddr());

        if (webAppType == WebAppTypeEnum.USER && ConfigManager.instance()
                .isConfigValue(Key.WEBAPP_USER_LOGOUT_CLEAR_INBOX)) {
            this.clearUserInbox(requestingUser, lockedUser);
        }

        setApiResultOk();
    }

    /**
     * Clears the user's inbox.
     *
     * @param requestingUser
     *            The user if of the requesting user.
     * @param lockedUser
     *            The locked {@link User} instance: is {@code null} when use is
     *            <i>not</i> locked.
     */
    private void clearUserInbox(final String requestingUser,
            final User lockedUser) {

        if (INBOX_SERVICE.getInboxInfo(requestingUser).jobCount() == 0) {
            return;
        }

        final DaoContext daoCtx = ServiceContext.getDaoContext();

        final boolean applyTransaction = !daoCtx.isTransactionActive();

        if (applyTransaction) {
            daoCtx.beginTransaction();
        }
        try {
            if (lockedUser == null) {
                daoCtx.getUserDao().lockByUserId(requestingUser);
            }
            INBOX_SERVICE.deleteAllPages(requestingUser);
        } finally {
            if (applyTransaction) {
                daoCtx.rollback();
            }
        }
    }

}
