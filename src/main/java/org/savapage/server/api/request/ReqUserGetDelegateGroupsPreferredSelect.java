/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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

import org.apache.commons.lang3.BooleanUtils;
import org.savapage.core.dao.enums.UserAttrEnum;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.jpa.User;
import org.savapage.core.services.ServiceContext;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqUserGetDelegateGroupsPreferredSelect
        extends ApiRequestMixin {

    /**
     * The request.
     */
    private static class DtoRsp extends AbstractDto {

        /**
         * If {@code true}, user selected to search for preferred User Groups.
         */
        private boolean select;

        @SuppressWarnings("unused")
        public boolean isSelect() {
            return select;
        }

        public void setSelect(final boolean select) {
            this.select = select;
        }

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final User dbUser = ServiceContext.getDaoContext().getUserDao()
                .findById(this.getSessionUserDbKey());

        final String value = USER_SERVICE.getUserAttrValue(dbUser,
                UserAttrEnum.PROXY_PRINT_DELEGATE_GROUPS_PREFERRED_SELECT);

        final DtoRsp rsp = new DtoRsp();
        rsp.setSelect(BooleanUtils.toBoolean(value));
        this.setResponse(rsp);

        setApiResultOk();
    }
}
