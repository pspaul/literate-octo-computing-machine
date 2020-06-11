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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.imaging.ImageUrl;
import org.savapage.core.inbox.InboxInfoDto;
import org.savapage.core.jpa.User;
import org.savapage.core.services.helpers.InboxPageImageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sets Canvas for SafaPage.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqPageOverlaySet extends ApiRequestMixin {

    /** The logger. */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ReqPageOverlaySet.class);

    /**
     *
     * The request.
     *
     */
    private static class DtoReq extends AbstractDto {

        private String imgUrl;

        private String svg64;

        private String json64;

        public String getImgUrl() {
            return imgUrl;
        }

        @SuppressWarnings("unused")
        public void setImgUrl(String imgUrl) {
            this.imgUrl = imgUrl;
        }

        public String getSvg64() {
            return svg64;
        }

        @SuppressWarnings("unused")
        public void setSvg64(String svg64) {
            this.svg64 = svg64;
        }

        public String getJson64() {
            return json64;
        }

        @SuppressWarnings("unused")
        public void setJson64(String json64) {
            this.json64 = json64;
        }

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final DtoReq dtoReq =
                DtoReq.create(DtoReq.class, this.getParmValueDto());

        setPageOverlay(requestingUser,
                ImageUrl.getPageOrdinalFromPath(dtoReq.getImgUrl()),
                dtoReq.getSvg64(), dtoReq.getJson64());

        this.setApiResult(ApiResultCodeEnum.OK, "msg-apply-ok");
    }

    /**
     *
     * @param userId
     *            The unique user id.
     * @param iPage
     *            The zero-based page number of the accumulated SafePages.
     * @param svg64
     *            Base64 encoded SVG overlay.
     * @param json64
     *            Base64 encoded JSON representation of SVG overlay.
     */
    private static void setPageOverlay(final String userId, final int iPage,
            final String svg64, final String json64) {

        final InboxPageImageInfo imgPageInfo =
                INBOX_SERVICE.getPageImageInfo(userId, iPage);

        imgPageInfo.setOverlaySVG64(svg64);

        LOGGER.trace("SafePage: {} | {} | Page: {}", iPage,
                imgPageInfo.getFile(), imgPageInfo.getPageInFile());

        final InboxInfoDto info = INBOX_SERVICE.readInboxInfo(userId);
        final List<InboxInfoDto.InboxJob> jobs = info.getJobs();

        final boolean noSVG = StringUtils.isBlank(svg64);

        for (final InboxInfoDto.InboxJob job : jobs) {

            if (job.getFile().equals(imgPageInfo.getFile())) {

                Map<Integer, InboxInfoDto.PageOverlay> overlay =
                        job.getOverlay();

                if (overlay == null) {

                    if (noSVG) {
                        return;
                    }

                    overlay = new HashMap<>();
                    job.setOverlay(overlay);
                }

                final Integer key =
                        Integer.valueOf(imgPageInfo.getPageInFile());

                if (noSVG) {
                    overlay.remove(key);
                } else {
                    final InboxInfoDto.PageOverlay pageOverlay =
                            new InboxInfoDto.PageOverlay();
                    pageOverlay.setSvg64(svg64);
                    pageOverlay.setFabric64(json64);
                    overlay.put(key, pageOverlay);
                }

                // Prune
                if (overlay.isEmpty()) {
                    job.setOverlay(null);
                }

                break;
            }
        }
        INBOX_SERVICE.storeInboxInfo(userId, info);
    }

}
