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

import org.savapage.core.dao.enums.UserAttrEnum;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.jpa.User;
import org.savapage.core.services.ServiceContext;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqUserPasswordErase extends ApiRequestMixin {

    /**
     *
     * The request.
     *
     */
    private static class DtoReq extends AbstractDto {

        private Long userDbId;

        public Long getUserDbId() {
            return userDbId;
        }

        @SuppressWarnings("unused")
        public void setUserDbId(Long userDbId) {
            this.userDbId = userDbId;
        }

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final DtoReq dtoReq =
                DtoReq.create(DtoReq.class, this.getParmValueDto());

        final boolean isAuth;

        switch (getSessionWebAppType()) {
        case USER:
        case MAILTICKETS:
            isAuth = lockedUser.getId().equals(dtoReq.getUserDbId());
            break;
        case ADMIN:
        case PRINTSITE:
            isAuth = true;
            break;
        default:
            isAuth = false;
            break;
        }

        if (!isAuth) {
            setApiResultText(ApiResultCodeEnum.ERROR, "not authorized");
            return;
        }

        final User user = ServiceContext.getDaoContext().getUserDao()
                .findById(dtoReq.getUserDbId());

        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        USER_SERVICE.removeUserAttr(user, UserAttrEnum.INTERNAL_PASSWORD);

        setApiResult(ApiResultCodeEnum.OK, "msg-user-password-erased-ok");
    }
}
