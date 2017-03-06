/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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

import org.savapage.core.dao.UserDao;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.ipp.IppJobStateEnum;
import org.savapage.core.jpa.User;
import org.savapage.core.msg.UserMsgIndicator;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobDto;
import org.savapage.core.services.ServiceContext;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqJobTicketPrintClose extends ApiRequestMixin {

    /**
     *
     * The request.
     *
     */
    private static class DtoReq extends AbstractDto {

        private String jobFileName;

        public String getJobFileName() {
            return jobFileName;
        }

        @SuppressWarnings("unused")
        public void setJobFileName(String jobFileName) {
            this.jobFileName = jobFileName;
        }

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final DtoReq dtoReq = DtoReq.create(DtoReq.class, getParmValue("dto"));

        final OutboxJobDto dto =
                JOBTICKET_SERVICE.closeTicketPrint(dtoReq.getJobFileName());

        final String msgKey;

        if (dto == null) {
            msgKey = "msg-outbox-jobticket-print-close-none";
        } else {
            msgKey = "msg-outbox-jobticket-print-close";
            notifyUser(dto.getUserId(), dto.getIppJobState());
        }

        this.setApiResult(ApiResultCodeEnum.OK, msgKey);
    }

    /**
     * @param userKey
     *            The user database key
     * @throws IOException
     *             When IO error.
     */
    private void notifyUser(final Long userKey, final IppJobStateEnum jobState)
            throws IOException {

        final UserDao userDao = ServiceContext.getDaoContext().getUserDao();

        final User user = userDao.findById(userKey);

        if (UserMsgIndicator.isSafePagesDirPresent(user.getUserId())) {

            final UserMsgIndicator.Msg userMsgInd;

            if (Boolean
                    .valueOf(jobState == IppJobStateEnum.IPP_JOB_COMPLETED)) {
                userMsgInd = UserMsgIndicator.Msg.JOBTICKET_SETTLED_PRINT;
            } else {
                userMsgInd = UserMsgIndicator.Msg.JOBTICKET_DENIED;
            }

            UserMsgIndicator.write(user.getUserId(),
                    ServiceContext.getTransactionDate(), userMsgInd, null);
        }
    }

}
