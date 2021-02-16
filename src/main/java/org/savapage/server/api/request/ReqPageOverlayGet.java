/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Authors: Rijk Ravestein.
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

import org.savapage.core.dto.AbstractDto;
import org.savapage.core.imaging.ImageUrl;
import org.savapage.core.jpa.User;
import org.savapage.core.services.helpers.InboxPageImageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gets Canvas for SafaPage.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqPageOverlayGet extends ApiRequestMixin {

    /** The logger. */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ReqPageOverlayGet.class);

    /**
     * The request.
     */
    private static class DtoReq extends AbstractDto {

        private String imgUrl;

        public String getImgUrl() {
            return imgUrl;
        }

        @SuppressWarnings("unused")
        public void setImgUrl(String imgUrl) {
            this.imgUrl = imgUrl;
        }

    }

    /**
     * The response.
     */
    private static class DtoRsp extends AbstractDto {

        private String svg64;

        private String json64;

        @SuppressWarnings("unused")
        public String getSvg64() {
            return svg64;
        }

        public void setSvg64(String svg64) {
            this.svg64 = svg64;
        }

        @SuppressWarnings("unused")
        public String getJson64() {
            return json64;
        }

        public void setJson64(String json64) {
            this.json64 = json64;
        }

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final DtoReq req = DtoReq.create(DtoReq.class, this.getParmValueDto());

        final int iPage = ImageUrl.getPageOrdinalFromPath(req.getImgUrl());
        LOGGER.trace("Page {}", iPage);

        final InboxPageImageInfo imgPageInfo =
                INBOX_SERVICE.getPageImageInfo(requestingUser, iPage);

        final DtoRsp rsp = new DtoRsp();

        if (imgPageInfo != null) {
            if (imgPageInfo.getOverlayJSON64() != null) {
                rsp.setJson64(imgPageInfo.getOverlayJSON64());
            } else {
                rsp.setSvg64(imgPageInfo.getOverlaySVG64());
            }
        }

        this.setResponse(rsp);

        this.setApiResultOk();
    }

}
