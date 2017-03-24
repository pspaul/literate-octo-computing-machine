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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.dto.RedirectPrinterDto;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.ipp.attribute.syntax.IppKeyword;
import org.savapage.core.ipp.client.IppConnectException;
import org.savapage.core.ipp.helpers.IppOptionMap;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.User;
import org.savapage.core.msg.UserMsgIndicator;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobDto;
import org.savapage.core.services.ServiceContext;

/**
 * Job Ticket Executor.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqJobTicketExec extends ApiRequestMixin {

    /**
     *
     * The request.
     *
     */
    private static class DtoReq extends AbstractDto {

        private String jobFileName;
        private boolean print;
        private boolean retry;
        private Long printerId;
        private String mediaSource;
        private String outputBin;
        private String jogOffset;

        /**
         *
         * @return
         */
        public String getJobFileName() {
            return jobFileName;
        }

        @SuppressWarnings("unused")
        public void setJobFileName(String jobFileName) {
            this.jobFileName = jobFileName;
        }

        /**
         * @return {@code true} when ticket job must be printed, {@code false}
         *         when just settled.
         */
        public boolean isPrint() {
            return print;
        }

        @SuppressWarnings("unused")
        public void setPrint(boolean print) {
            this.print = print;
        }

        public boolean isRetry() {
            return retry;
        }

        @SuppressWarnings("unused")
        public void setRetry(boolean retry) {
            this.retry = retry;
        }

        public Long getPrinterId() {
            return printerId;
        }

        @SuppressWarnings("unused")
        public void setPrinterId(Long printerId) {
            this.printerId = printerId;
        }

        /**
         * @return The {@link IppDictJobTemplateAttr#ATTR_MEDIA_SOURCE} value
         *         for the print job. Is irrelevant ({@code null}) when
         *         settlement.
         */
        public String getMediaSource() {
            return mediaSource;
        }

        @SuppressWarnings("unused")
        public void setMediaSource(String mediaSource) {
            this.mediaSource = mediaSource;
        }

        /**
         * @return The {@link IppDictJobTemplateAttr#ATTR_OUTPUT_BIN} value for
         *         the print job. Is irrelevant ({@code null}) when settlement.
         */
        public String getOutputBin() {
            return outputBin;
        }

        @SuppressWarnings("unused")
        public void setOutputBin(String outputBin) {
            this.outputBin = outputBin;
        }

        /**
         * @return The
         *         {@link IppDictJobTemplateAttr#ORG_SAVAPAGE_ATTR_FINISHINGS_JOG_OFFSET}
         *         value for the print job. Is irrelevant ({@code null}) when
         *         settlement.
         */
        public String getJogOffset() {
            return jogOffset;
        }

        @SuppressWarnings("unused")
        public void setJogOffset(String jogOffset) {
            this.jogOffset = jogOffset;
        }

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final DtoReq dtoReq = DtoReq.create(DtoReq.class, getParmValue("dto"));

        Long printerId = dtoReq.getPrinterId();

        if (printerId == null) {

            final boolean settlement = !dtoReq.isPrint();

            /*
             * INVARIANT: Settlement MUST have printer.
             */
            if (settlement) {
                this.setApiResultText(ApiResultCodeEnum.ERROR,
                        "No printer specified or available.");
                return;
            }

            /*
             * Find a compatible printer with media-source choice "auto".
             */
            final Map<String, String> filter = new HashMap<>();

            filter.put(IppDictJobTemplateAttr.ATTR_MEDIA_SOURCE,
                    IppKeyword.MEDIA_SOURCE_AUTO);

            final RedirectPrinterDto dto = JOBTICKET_SERVICE.getRedirectPrinter(
                    dtoReq.getJobFileName(), new IppOptionMap(filter),
                    getLocale());

            /*
             * INVARIANT: Compatible printer MUST be present.
             */
            if (dto == null) {
                this.setApiResultText(ApiResultCodeEnum.ERROR,
                        "No printer specified or available.");
                return;
            }

            printerId = dto.getId();

            // Ad-hoc assign the media-source.
            dtoReq.setMediaSource(IppKeyword.MEDIA_SOURCE_AUTO);
        }

        final Printer printer = ServiceContext.getDaoContext().getPrinterDao()
                .findById(printerId);

        if (printer == null) {
            this.setApiResultText(ApiResultCodeEnum.ERROR, "Printer not found");
            return;
        }

        try {

            final OutboxJobDto dto;

            if (dtoReq.isPrint()) {
                /*
                 * INVARIANT: media-source MUST be specified.
                 */
                if (StringUtils.isBlank(dtoReq.getMediaSource())) {
                    this.setApiResultText(ApiResultCodeEnum.ERROR,
                            "No media-source found");
                    return;
                }

                if (dtoReq.isRetry()) {

                    dto = JOBTICKET_SERVICE.retryTicketPrint(requestingUser,
                            printer, dtoReq.getMediaSource(),
                            dtoReq.getOutputBin(), dtoReq.getJogOffset(),
                            dtoReq.getJobFileName());

                } else {
                    dto = JOBTICKET_SERVICE.printTicket(requestingUser, printer,
                            dtoReq.getMediaSource(), dtoReq.getOutputBin(),
                            dtoReq.getJogOffset(), dtoReq.getJobFileName());
                }

            } else {
                dto = JOBTICKET_SERVICE.settleTicket(requestingUser, printer,
                        dtoReq.getJobFileName());

                if (dto != null) {
                    notifySettlement(dto);
                }
            }

            if (dto == null) {
                this.setApiResult(ApiResultCodeEnum.OK,
                        "msg-jobticket-print-none");
            } else {
                final String msgKey;
                if (dtoReq.isPrint()) {
                    msgKey = "msg-jobticket-print-ok";
                } else {
                    msgKey = "msg-jobticket-settled-ok";
                }
                this.setApiResult(ApiResultCodeEnum.OK, msgKey,
                        dto.getTicketNumber());
            }

        } catch (IppConnectException e) {
            this.setApiResultText(ApiResultCodeEnum.ERROR, e.getMessage());
        }
    }

    /**
     * Notifies the settlement to the requesting user.
     *
     * @param dto
     *            The {@link OutboxJobDto} of the settlement.
     * @throws IOException
     *             When error sending the notification.
     */
    private void notifySettlement(final OutboxJobDto dto) throws IOException {

        final UserDao userDao = ServiceContext.getDaoContext().getUserDao();

        final User user = userDao.findById(dto.getUserId());

        if (UserMsgIndicator.isSafePagesDirPresent(user.getUserId())) {

            final UserMsgIndicator.Msg msg;

            if (dto.isCopyJobTicket()) {
                msg = UserMsgIndicator.Msg.JOBTICKET_SETTLED_COPY;
            } else {
                msg = UserMsgIndicator.Msg.JOBTICKET_SETTLED_PRINT;
            }

            UserMsgIndicator.write(user.getUserId(),
                    ServiceContext.getTransactionDate(), msg, null);
        }
    }

}
