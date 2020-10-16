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

import java.util.Set;

import org.savapage.core.dto.AbstractDto;
import org.savapage.core.jpa.User;
import org.savapage.core.services.ServiceContext;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class ReqUserDelegateObjectsPreferred extends ApiRequestMixin {

    /**
     * The request.
     */
    protected static class DtoReq extends AbstractDto {

        /**
         * The preferred Database IDs to be added or removed.
         */
        private Set<Long> dbKeys;

        /**
         * If {@code true}, the preferred Group IDs are added. If {@code false},
         * they are removed.
         */
        private boolean add;

        public Set<Long> getDbKeys() {
            return dbKeys;
        }

        public void setDbKeys(Set<Long> dbKeys) {
            this.dbKeys = dbKeys;
        }

        public boolean isAdd() {
            return add;
        }

        @SuppressWarnings("unused")
        public void setAdd(boolean add) {
            this.add = add;
        }
    }

    /**
     * @return the request.
     */
    protected final DtoReq getDtoReq() {
        return DtoReq.create(DtoReq.class, this.getParmValueDto());
    }

    /**
     * Retrieves session user from database.
     *
     * @return The user.
     */
    protected final User getDbUser() {
        return ServiceContext.getDaoContext().getUserDao()
                .findById(this.getSessionUserDbKey());
    }
}
