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
import java.util.Set;

import org.savapage.core.dto.AbstractDto;
import org.savapage.core.jpa.User;
import org.savapage.core.services.ServiceContext;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqUserDelegateGroupsPreferred extends ApiRequestMixin {

    /**
     * The request.
     */
    private static class DtoReq extends AbstractDto {

        /**
         * The preferred User Group IDs to be added or removed.
         */
        private Set<Long> groupIds;

        /**
         * If {@code true}, the preferred Group IDs are added. If {@code false},
         * they are removed.
         */
        private boolean add;

        public Set<Long> getGroupIds() {
            return groupIds;
        }

        @SuppressWarnings("unused")
        public void setGroupIds(Set<Long> groupIds) {
            this.groupIds = groupIds;
        }

        public boolean isAdd() {
            return add;
        }

        @SuppressWarnings("unused")
        public void setAdd(boolean add) {
            this.add = add;
        }

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final DtoReq dtoReq =
                DtoReq.create(DtoReq.class, this.getParmValueDto());

        final User dbUser = ServiceContext.getDaoContext().getUserDao()
                .findById(this.getSessionUser().getId());

        if (dtoReq.isAdd()) {
            USER_SERVICE.addPreferredDelegateGroups(dbUser,
                    dtoReq.getGroupIds());
        } else {
            USER_SERVICE.removePreferredDelegateGroups(dbUser,
                    dtoReq.getGroupIds());
        }

        setApiResultOk();
    }
}
