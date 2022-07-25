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
import java.text.ParseException;

import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.jpa.User;
import org.savapage.core.util.BigDecimalUtil;

/**
 * Updates one or more configuration properties.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqConfigPropGet extends ApiRequestMixin {

    /**
     *
     * The request.
     *
     */
    private static class DtoReq extends AbstractDto {

        private String name;

        public String getName() {
            return name;
        }

        @SuppressWarnings("unused")
        public void setName(String name) {
            this.name = name;
        }

    }

    /**
     *
     * The response.
     *
     */
    private static class DtoRsp extends AbstractDto {

        private String name;
        private String value;
        private Boolean multiline;

        @SuppressWarnings("unused")
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @SuppressWarnings("unused")
        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @SuppressWarnings("unused")
        public Boolean getMultiline() {
            return multiline;
        }

        public void setMultiline(Boolean multiline) {
            this.multiline = multiline;
        }

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final DtoReq dtoReq =
                DtoReq.create(DtoReq.class, this.getParmValueDto());

        final ConfigManager cm = ConfigManager.instance();

        /*
         * INVARIANT: property MUST exist in cache.
         */
        final Key key = cm.getConfigKey(dtoReq.getName());

        if (key == null) {
            setApiResult(ApiResultCodeEnum.ERROR, "msg-config-prop-not-found",
                    dtoReq.getName());
            return;
        }

        /*
         * INVARIANT: property MUST exist in database.
         */
        String value = cm.readDbConfigKey(key);

        if (value == null) {
            setApiResult(ApiResultCodeEnum.ERROR, "msg-config-prop-not-found",
                    dtoReq.getName());
            return;
        }

        /*
         * If display of this value is Locale sensitive, we MUST revert to
         * locale format.
         */
        if (cm.isConfigBigDecimal(key)) {
            try {
                value = BigDecimalUtil.localize(BigDecimalUtil.valueOf(value),
                        getLocale(), true);
            } catch (ParseException e) {
                throw new IllegalStateException(e.getMessage());
            }
        }

        final DtoRsp dtoRsp = new DtoRsp();

        dtoRsp.setName(dtoReq.getName());
        dtoRsp.setValue(value);
        dtoRsp.setMultiline(Boolean.valueOf(cm.isConfigMultiline(key)));

        setResponse(dtoRsp);
        setApiResultOk();
    }

}
