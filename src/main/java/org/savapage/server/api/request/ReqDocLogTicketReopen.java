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
import org.savapage.core.doc.store.DocStoreException;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.User;
import org.savapage.core.services.ServiceContext;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqDocLogTicketReopen extends ApiRequestMixin {

    /**
     * The request.
     */
    private static class DtoReq extends AbstractDto {

        private Long docLogId;

        public Long getDocLogId() {
            return docLogId;
        }

        @SuppressWarnings("unused")
        public void setDocLogId(Long docLogId) {
            this.docLogId = docLogId;
        }

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final DtoReq dtoReq =
                DtoReq.create(DtoReq.class, this.getParmValueDto());

        final DocLog docLog = ServiceContext.getDaoContext().getDocLogDao()
                .findById(dtoReq.getDocLogId());

        if (!isValid(docLog)) {
            return;
        }

        try {
            JOBTICKET_SERVICE.reopenTicketForExtraCopies(docLog);
            this.setApiResult(ApiResultCodeEnum.OK, "msg-ticket-reopen-ok");
        } catch (DocStoreException e) {
            this.setApiResultText(ApiResultCodeEnum.ERROR, e.getMessage());
        }
    }

    /**
     *
     * @param docLog
     *            The {@link DocLog} to reopen (can be {@code null}.
     * @return {@code true} when the reopen can be applied.
     */
    private boolean isValid(final DocLog docLog) {

        if (docLog == null) {
            this.setApiResultText(ApiResultCodeEnum.ERROR,
                    "No document found."); // TODO
            return false;
        }

        /*
         * INVARIANT: not refunded.
         */
        if (!docLog.getCost().equals(docLog.getCostOriginal())) {
            this.setApiResultText(ApiResultCodeEnum.ERROR, "Ticket refunded.");
            return false;
        }

        /*
         * INVARIANT: single account.
         */
        if (docLog.getTransactions().size() > 1) {
            this.setApiResultText(ApiResultCodeEnum.ERROR,
                    "No single account.");
            return false;
        }

        final String ticketNumber = docLog.getExternalId();

        if (StringUtils.isBlank(ticketNumber)) {
            this.setApiResultText(ApiResultCodeEnum.ERROR, "Not a job ticket.");
            return false;
        }

        /*
         * INVARIANT: not already reopened.
         */
        if (JOBTICKET_SERVICE.isTicketReopened(ticketNumber)) {
            this.setApiResultText(ApiResultCodeEnum.WARN,
                    "Ticket is already reopened.");
            return false;
        }

        return true;
    }

}
