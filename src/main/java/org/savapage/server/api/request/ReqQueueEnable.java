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

import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.dao.IppQueueDao;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.jpa.IppQueue;
import org.savapage.core.jpa.User;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.AppLogHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqQueueEnable extends ApiRequestMixin {

    /**
     *
     * @author Rijk Ravestein
     *
     */
    private static class DtoReq extends AbstractDto {

        private String urlPath;
        private Boolean enabled;

        public String getUrlPath() {
            return urlPath;
        }

        @SuppressWarnings("unused")
        public void setUrlPath(String urlPath) {
            this.urlPath = urlPath;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        @SuppressWarnings("unused")
        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final DtoReq req = DtoReq.create(DtoReq.class, this.getParmValueDto());

        final IppQueueDao dao = ServiceContext.getDaoContext().getIppQueueDao();

        final IppQueue queue = dao.findByUrlPath(req.getUrlPath());

        if (queue == null) {
            this.setApiResult(ApiResultCodeEnum.ERROR,
                    "msg-queue-urlpath-not-found", req.getUrlPath());
            return;
        }

        final String msgKey;

        if (queue.getDisabled() == req.getEnabled()) {

            queue.setDisabled(!req.getEnabled());
            dao.update(queue);

            final String msgKeyPub;
            final PubLevelEnum pubLevel;

            if (req.getEnabled()) {
                msgKey = "msg-queue-enabled";
                msgKeyPub = "msg-queue-enabled-pub";
                pubLevel = PubLevelEnum.CLEAR;
            } else {
                msgKey = "msg-queue-disabled";
                msgKeyPub = "msg-queue-disabled-pub";
                pubLevel = PubLevelEnum.WARN;
            }

            final String msg = AppLogHelper.logInfo(this.getClass(), msgKeyPub,
                    req.getUrlPath(), requestingUser);

            AdminPublisher.instance().publish(PubTopicEnum.IPP, pubLevel, msg);

        } else if (req.getEnabled()) {
            msgKey = "msg-queue-enabled-already";
        } else {
            msgKey = "msg-queue-disabled-already";
        }

        this.setApiResult(ApiResultCodeEnum.OK, msgKey, req.getUrlPath());
    }
}
