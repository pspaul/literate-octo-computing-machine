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
import java.net.URL;

import javax.naming.LimitExceededException;

import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.fonts.InternalFontFamilyEnum;
import org.savapage.core.jpa.User;
import org.savapage.core.services.ServiceContext;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqUrlPrint extends ApiRequestMixin {

    /**
     *
     * The request.
     *
     */
    private static class DtoReq extends AbstractDto {

        /** */
        private String url;

        /** */
        private InternalFontFamilyEnum fontEnum;

        public String getUrl() {
            return url;
        }

        @SuppressWarnings("unused")
        public void setUrl(String url) {
            this.url = url;
        }

        public InternalFontFamilyEnum getFontEnum() {
            return fontEnum;
        }

        @SuppressWarnings("unused")
        public void setFontEnum(InternalFontFamilyEnum fontEnum) {
            this.fontEnum = fontEnum;
        }

    }

    @Override
    protected void onRequest(final String requestingUser,
            final User lockedUser) {

        final DtoReq dtoReq =
                DtoReq.create(DtoReq.class, this.getParmValueDto());

        final User user = USER_DAO.findActiveUserByUserId(requestingUser);

        final int maxMB = ConfigManager.instance()
                .getConfigInt(IConfigProp.Key.WEB_PRINT_MAX_FILE_MB);
        try {
            if (ServiceContext.getServiceFactory().getDownloadService()
                    .download(new URL(dtoReq.getUrl()), this.getClientIP(),
                            user, dtoReq.getFontEnum(), maxMB)) {
                this.setApiResultOk();
            } else {
                this.setApiResult(ApiResultCodeEnum.WARN,
                        "msg-url-print-unknown-content-type",
                        dtoReq.getUrl().toString());
            }
        } catch (IOException e) {
            this.setApiResult(ApiResultCodeEnum.ERROR, "msg-url-print-error",
                    dtoReq.getUrl().toString(), e.getMessage());
        } catch (LimitExceededException e) {
            this.setApiResult(ApiResultCodeEnum.ERROR, "msg-url-print-error",
                    dtoReq.getUrl().toString(), e.getMessage());
        }
    }

}
