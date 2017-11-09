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
import org.savapage.core.services.helpers.JobTicketExecParms;
import org.savapage.server.session.JobTicketSession;
import org.savapage.server.session.SpSession;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
        private String mediaSourceJobSheet;
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
         * @return The {@link IppDictJobTemplateAttr#ATTR_MEDIA_SOURCE} value
         *         for the Job Sheet print job. Is irrelevant ({@code null})
         *         when settlement.
         */
        public String getMediaSourceJobSheet() {
            return mediaSourceJobSheet;
        }

        /**
         * @param mediaSourceJobSheet
         */
        public void setMediaSourceJobSheet(String mediaSourceJobSheet) {
            this.mediaSourceJobSheet = mediaSourceJobSheet;
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

        /**
         * Creates parameter object.
         *
         * @param operator
         *            The operator.
         * @param printer
         *            The printer.
         * @return The parameter object.
         */
        @JsonIgnore
        public JobTicketExecParms createExecParms(final String operator,
                final Printer printer) {

            final JobTicketExecParms parms = new JobTicketExecParms();

            parms.setOperator(operator);
            parms.setPrinter(printer);
            parms.setIppMediaSource(this.getMediaSource());
            parms.setIppMediaSourceJobSheet(this.getMediaSourceJobSheet());
            parms.setIppOutputBin(this.getOutputBin());
            parms.setIppJogOffset(this.getJogOffset());
            parms.setFileName(this.getJobFileName());

            return parms;
        }
    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final DtoReq dtoReq = DtoReq.create(DtoReq.class, getParmValueDto());

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
            dtoReq.setMediaSourceJobSheet(IppKeyword.MEDIA_SOURCE_AUTO);
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

                final JobTicketExecParms parms =
                        dtoReq.createExecParms(requestingUser, printer);

                /*
                 * INVARIANT: When org.savapage-job-sheet is specified,
                 * output-bin can NOT be auto.
                 */
                if (parms.getIppMediaSourceJobSheet() != null
                        && parms.getIppOutputBin() != null) {
                    if (parms.getIppOutputBin()
                            .equals(IppKeyword.OUTPUT_BIN_AUTO)) {
                        this.setApiResult(ApiResultCodeEnum.WARN,
                                "msg-jobticket-option-mismatch",
                                PROXY_PRINT_SERVICE.localizePrinterOpt(
                                        getLocale(),
                                        IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_JOB_SHEETS),
                                PROXY_PRINT_SERVICE.localizePrinterOpt(
                                        getLocale(),
                                        IppDictJobTemplateAttr.ATTR_OUTPUT_BIN),
                                PROXY_PRINT_SERVICE.localizePrinterOptValue(
                                        getLocale(),
                                        IppDictJobTemplateAttr.ATTR_OUTPUT_BIN,
                                        IppKeyword.OUTPUT_BIN_AUTO));
                        return;
                    }
                }

                if (dtoReq.isRetry()) {
                    dto = JOBTICKET_SERVICE.retryTicketPrint(parms);
                } else {
                    dto = JOBTICKET_SERVICE.printTicket(parms);
                }

                if (dtoReq.getPrinterId() != null) {
                    saveToSession(dtoReq);
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
     * Saves printer options to server session.
     *
     * @param dto
     *            The request.
     */
    private void saveToSession(final DtoReq dto) {

        JobTicketSession session = SpSession.get().getJobTicketSession();

        if (session == null) {
            session = new JobTicketSession();
            SpSession.get().setJobTicketSession(session);
        }

        session.setJogOffsetOption(dto.getJogOffset());

        Map<Long, Map<JobTicketSession.PrinterOpt, String>> printerOpts =
                session.getRedirectPrinterOptions();

        if (printerOpts == null) {
            printerOpts = new HashMap<>();
        }

        final Map<JobTicketSession.PrinterOpt, String> opts = new HashMap<>();

        opts.put(JobTicketSession.PrinterOpt.MEDIA_SOURCE,
                dto.getMediaSource());
        opts.put(JobTicketSession.PrinterOpt.MEDIA_SOURCE_SHEET,
                dto.getMediaSourceJobSheet());
        opts.put(JobTicketSession.PrinterOpt.OUTPUT_BIN, dto.getOutputBin());

        //
        printerOpts.put(dto.getPrinterId(), opts);

        session.setRedirectPrinterOptions(printerOpts);
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
