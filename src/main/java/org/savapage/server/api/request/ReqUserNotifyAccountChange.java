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

import org.savapage.core.dao.UserDao;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.jpa.User;
import org.savapage.core.msg.UserMsgIndicator;
import org.savapage.core.services.ServiceContext;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqUserNotifyAccountChange extends ApiRequestMixin {

    /**
     *
     * The request.
     *
     */
    private static class DtoReq extends AbstractDto {

        @JsonProperty("key")
        private Long userKey;

        public Long getUserKey() {
            return userKey;
        }

        @SuppressWarnings("unused")
        public void setUserKey(Long userKey) {
            this.userKey = userKey;
        }

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final UserDao userDao = ServiceContext.getDaoContext().getUserDao();

        final DtoReq dtoReq = DtoReq.create(DtoReq.class, getParmValue("dto"));

        final User user = userDao.findById(dtoReq.getUserKey());

        if (UserMsgIndicator.isSafePagesDirPresent(user.getUserId())) {
            UserMsgIndicator.write(user.getUserId(),
                    ServiceContext.getTransactionDate(),
                    UserMsgIndicator.Msg.ACCOUNT_INFO, null);
        }

        this.setApiResultOk();
    }

}
