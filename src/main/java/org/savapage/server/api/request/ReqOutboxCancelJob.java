/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.api.request;

import java.io.IOException;

import org.apache.commons.lang3.BooleanUtils;
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

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final DtoReq dtoReq = DtoReq.create(DtoReq.class, getParmValue("dto"));

        final String msgKey;

        if (BooleanUtils.isTrue(dtoReq.getJobTicket())) {
            msgKey = cancelJobTicket(lockedUser, dtoReq);
        } else {
            msgKey = cancelOutboxJob(lockedUser, dtoReq);
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
     * @param lockedUser
     *            The user.
     * @param dtoReq
     *            The request.
     * @return The message key.
     */
    private String cancelJobTicket(final User lockedUser, final DtoReq dtoReq) {

        if (JOBTICKET_SERVICE.cancelTicket(lockedUser.getId(),
                dtoReq.getJobFileName()) == null) {
            return "msg-outbox-cancelled-jobticket-none";
        }
        return "msg-outbox-cancelled-jobticket";
    }

}
