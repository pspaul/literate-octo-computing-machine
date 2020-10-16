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

import org.apache.commons.lang3.BooleanUtils;
import org.savapage.core.config.WebAppTypeEnum;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.jpa.User;
import org.savapage.server.session.SpSession;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqOutboxCancelJob extends ApiRequestMixin {

    /**
     *
     * The request.
     *
     */
    private static class DtoReq extends AbstractDto {

        private String jobFileName;
        private Boolean jobTicket;
        private Long userDbId;

        public String getJobFileName() {
            return jobFileName;
        }

        @SuppressWarnings("unused")
        public void setJobFileName(String jobFileName) {
            this.jobFileName = jobFileName;
        }

        public Boolean getJobTicket() {
            return jobTicket;
        }

        @SuppressWarnings("unused")
        public void setJobTicket(Boolean jobTicket) {
            this.jobTicket = jobTicket;
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

        final Long userDbId;

        if (getSessionWebAppType() == WebAppTypeEnum.USER) {
            userDbId = lockedUser.getId();
        } else {
            userDbId = dtoReq.getUserDbId();
        }

        final String msgKey;

        if (BooleanUtils.isTrue(dtoReq.getJobTicket())) {

            msgKey = cancelJobTicket(userDbId, dtoReq);

        } else {

            final User user;

            if (userDbId.equals(lockedUser.getId())) {
                user = lockedUser;
            } else {
                user = USER_DAO.findActiveUserById(userDbId);
            }
            msgKey = cancelOutboxJob(user, dtoReq);
        }

        this.setApiResult(ApiResultCodeEnum.OK, msgKey);

        ApiRequestHelper.addUserStats(this.getUserData(), lockedUser,
                this.getLocale(), SpSession.getAppCurrencySymbol());
    }

    /**
     * Cancels a job in the user's outbox.
     *
     * @param lockedUser
     *            The user.
     * @param dtoReq
     *            The request.
     * @return The message key.
     */
    private String cancelOutboxJob(final User lockedUser, final DtoReq dtoReq) {

        if (OUTBOX_SERVICE.cancelOutboxJob(lockedUser.getUserId(),
                dtoReq.getJobFileName())) {
            return "msg-outbox-cancelled-job";
        }
        return "msg-outbox-cancelled-job-none";
    }

    /**
     * Cancels a Job Ticket from the user.
     *
     * @param userDbId
     *            The user database key of the ticket owner.
     * @param dtoReq
     *            The request.
     * @return The message key.
     */
    private String cancelJobTicket(final Long userDbId, final DtoReq dtoReq) {

        if (JOBTICKET_SERVICE.cancelTicket(userDbId,
                dtoReq.getJobFileName()) == null) {
            return "msg-outbox-cancelled-jobticket-none";
        }
        return "msg-outbox-cancelled-jobticket";
    }

}
