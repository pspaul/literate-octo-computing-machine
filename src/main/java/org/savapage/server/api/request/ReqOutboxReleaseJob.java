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

import org.savapage.core.dto.AbstractDto;
import org.savapage.core.jpa.User;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobDto;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqOutboxReleaseJob extends ApiRequestMixin {

    /**
     *
     * The request.
     *
     */
    private static class DtoReq extends AbstractDto {

        private String jobFileName;
        private Long userDbId;

        public String getJobFileName() {
            return jobFileName;
        }

        @SuppressWarnings("unused")
        public void setJobFileName(String jobFileName) {
            this.jobFileName = jobFileName;
        }

        public Long getUserDbId() {
            return userDbId;
        }

        @SuppressWarnings("unused")
        public void setUserDbId(Long userDbId) {
            this.userDbId = userDbId;
        }

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final DtoReq dtoReq =
                DtoReq.create(DtoReq.class, this.getParmValueDto());

        final User user = USER_DAO.findActiveUserById(dtoReq.getUserDbId());

        final OutboxJobDto dtoJob = OUTBOX_SERVICE
                .getOutboxJob(user.getUserId(), dtoReq.getJobFileName());

        if (dtoJob == null) {
            this.setApiResult(ApiResultCodeEnum.ERROR,
                    "msg-outbox-release-job-none");
        } else {
            try {
                PROXY_PRINT_SERVICE.proxyPrintOutbox(dtoReq.getUserDbId(),
                        dtoJob);
                this.setApiResult(ApiResultCodeEnum.OK,
                        "msg-outbox-release-job");
            } catch (Exception e) {
                this.setApiResultText(ApiResultCodeEnum.ERROR, e.getMessage());
            }
        }
    }

}
