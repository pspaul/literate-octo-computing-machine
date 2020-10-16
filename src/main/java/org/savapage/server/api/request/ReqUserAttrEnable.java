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

/**
 * Sets User Attribute boolean.
 *
 * @author Rijk Ravestein
 *
 */
public abstract class ReqUserAttrEnable extends ApiRequestMixin {

    /**
     *
     * The request.
     *
     */
    private static class DtoReq extends AbstractDto {

        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        @SuppressWarnings("unused")
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

    }

    /**
     *
     * @return {@link UserAttrEnum} boolean.
     */
    protected abstract UserAttrEnum getUserAttrEnum();

    @Override
    protected final void onRequest(final String requestingUser,
            final User lockedUser) throws IOException {

        final DtoReq dtoReq =
                DtoReq.create(DtoReq.class, this.getParmValueDto());

        final User jpaUser;
        if (lockedUser == null) {
            jpaUser = USER_DAO.findById(this.getSessionUserDbKey());
        } else {
            jpaUser = lockedUser;
        }

        USER_SERVICE.setUserAttrValue(jpaUser, this.getUserAttrEnum(),
                dtoReq.isEnabled());

        this.setApiResultOk();
    }

}
