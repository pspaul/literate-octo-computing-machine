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

import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.i18n.SystemModeEnum;
import org.savapage.core.jpa.User;
import org.savapage.core.services.ServiceContext;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Changes the system mode.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqSystemModeChange extends ApiRequestMixin {

    /**
     * .
     */
    @JsonInclude(Include.NON_NULL)
    private static class DtoReq extends AbstractDto {

        private SystemModeEnum mode;

        public SystemModeEnum getMode() {
            return mode;
        }

        @SuppressWarnings("unused")
        public void setMode(SystemModeEnum mode) {
            this.mode = mode;
        }

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final DtoReq dtoReq = DtoReq.create(DtoReq.class, getParmValueDto());

        final String value;

        switch (dtoReq.getMode()) {
        case MAINTENANCE:
            value = IConfigProp.V_YES;
            break;

        case PRODUCTION:
            value = IConfigProp.V_NO;
            break;
        default:
            throw new SpException(dtoReq.getMode() + " not handled");
        }

        ConfigManager.instance().updateConfigKey(Key.SYS_MAINTENANCE, value,
                ServiceContext.getActor());

        setApiResultOk();
    }

}
