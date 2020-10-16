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

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.jpa.User;
import org.savapage.core.msg.UserMsgIndicator;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobDto;
import org.savapage.core.services.ServiceContext;
import org.savapage.ext.notification.JobTicketCancelEvent;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqJobTicketCancel extends ApiRequestMixin {

    /**
     *
     * The request.
     *
     */
    private static class DtoReq extends AbstractDto {

        private String jobFileName;
        private String reason;

        public String getJobFileName() {
            return jobFileName;
        }

        @SuppressWarnings("unused")
        public void setJobFileName(String jobFileName) {
            this.jobFileName = jobFileName;
        }

        public String getReason() {
            return reason;
        }

        @SuppressWarnings("unused")
        public void setReason(String reason) {
            this.reason = reason;
        }

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final DtoReq dtoReq =
                DtoReq.create(DtoReq.class, this.getParmValueDto());

        final OutboxJobDto dto =
                JOBTICKET_SERVICE.cancelTicket(dtoReq.getJobFileName());

        final String msgKey;

        if (dto == null) {
            msgKey = "msg-outbox-cancelled-jobticket-none";
        } else {
            msgKey = "msg-outbox-cancelled-jobticket";

            final UserDao userDao = ServiceContext.getDaoContext().getUserDao();
            final User user = userDao.findById(dto.getUserId());

            if (user != null) {

                notifyUser(user);

                sendEmailNotification(requestingUser, dto,
                        StringUtils.defaultString(
                                StringUtils.trimToNull(dtoReq.getReason()),
                                "-"),
                        user);

                if (hasNotificationListener()) {

                    getNotificationListener().onJobTicketEvent(fillEvent(
                            new JobTicketCancelEvent(), requestingUser, user,
                            dto, dtoReq.getReason()));
                }
            }
        }
        this.setApiResult(ApiResultCodeEnum.OK, msgKey);
    }

    /**
     * Notifies a user in Web App.
     *
     * @param user
     *            The user.
     * @throws IOException
     *             When IO error.
     */
    private void notifyUser(final User user) throws IOException {
        if (UserMsgIndicator.isSafePagesDirPresent(user.getUserId())) {
            UserMsgIndicator.write(user.getUserId(),
                    ServiceContext.getTransactionDate(),
                    UserMsgIndicator.Msg.JOBTICKET_DENIED, null);
        }
    }

    /**
     * Notifies a user by email.
     *
     * @param operator
     *            The Ticket Operator.
     * @param dto
     *            The Ticket.
     * @param reason
     *            The reason for cancellation (can be {@code null}).
     * @param user
     *            The User
     * @return The email address or {@code null} when not send.
     */
    private String sendEmailNotification(final String operator,
            final OutboxJobDto dto, final String reason, final User user) {
        /*
         * INVARIANT: Notification must be enabled.
         */
        if (!ConfigManager.instance()
                .isConfigValue(Key.JOBTICKET_NOTIFY_EMAIL_CANCELED_ENABLE)) {
            return null;
        }

        return JOBTICKET_SERVICE.notifyTicketCanceledByEmail(dto, operator,
                user, reason, ConfigManager.getDefaultLocale());
    }

}
