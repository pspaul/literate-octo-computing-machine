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

import org.savapage.core.dto.AbstractDto;
import org.savapage.core.dto.RedirectPrinterDto;
import org.savapage.core.ipp.client.IppConnectException;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.User;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobDto;
import org.savapage.core.services.ServiceContext;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqJobTicketPrint extends ApiRequestMixin {

    /**
     *
     * The request.
     *
     */
    private static class DtoReq extends AbstractDto {

        private String jobFileName;
        private Long printerId;

        public String getJobFileName() {
            return jobFileName;
        }

        @SuppressWarnings("unused")
        public void setJobFileName(String jobFileName) {
            this.jobFileName = jobFileName;
        }

        public Long getPrinterId() {
            return printerId;
        }

        @SuppressWarnings("unused")
        public void setPrinterId(Long printerId) {
            this.printerId = printerId;
        }

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final DtoReq dtoReq = DtoReq.create(DtoReq.class, getParmValue("dto"));

        Long printerId = dtoReq.getPrinterId();

        if (printerId == null) {

            final RedirectPrinterDto dto = JOBTICKET_SERVICE
                    .getRedirectPrinter(dtoReq.getJobFileName());

            if (dto != null) {
                printerId = dto.getId();
            }
        }

        if (printerId == null) {
            this.setApiResultText(ApiResultCodeEnum.ERROR,
                    "No printer specified or available.");
            return;
        }

        final Printer printer = ServiceContext.getDaoContext().getPrinterDao()
                .findById(printerId);

        if (printer == null) {
            this.setApiResultText(ApiResultCodeEnum.ERROR, "Printer not found");
            return;
        }

        try {

            final OutboxJobDto dto = JOBTICKET_SERVICE.printTicket(printer,
                    dtoReq.getJobFileName());

            final String msgKey;

            if (dto == null) {
                msgKey = "msg-jobticket-print-none";
            } else {
                msgKey = "msg-jobticket-print-ok";
            }

            this.setApiResult(ApiResultCodeEnum.OK, msgKey);

        } catch (IppConnectException e) {
            this.setApiResultText(ApiResultCodeEnum.ERROR, e.getMessage());
        }

    }

}
