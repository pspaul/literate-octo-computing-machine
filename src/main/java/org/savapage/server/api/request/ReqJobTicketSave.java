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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpException;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.dto.IppMediaSourceCostDto;
import org.savapage.core.ipp.IppMediaSizeEnum;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.ipp.helpers.IppOptionMap;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.User;
import org.savapage.core.msg.UserMsgIndicator;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobDto;
import org.savapage.core.pdf.PdfPrintCollector;
import org.savapage.core.print.proxy.JsonProxyPrinter;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.JobTicketWrapperDto;
import org.savapage.core.services.helpers.PrinterAttrLookup;
import org.savapage.core.services.helpers.ProxyPrintCostDto;
import org.savapage.core.services.helpers.ProxyPrintCostParms;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqJobTicketSave extends ApiRequestMixin {

    /**
     *
     * The request.
     *
     */
    private static class DtoReq extends AbstractDto {

        private String jobFileName;
        private int copies;
        private Boolean archive;
        private Map<String, String> ippOptions;

        public String getJobFileName() {
            return jobFileName;
        }

        @SuppressWarnings("unused")
        public void setJobFileName(String jobFileName) {
            this.jobFileName = jobFileName;
        }

        public int getCopies() {
            return copies;
        }

        @SuppressWarnings("unused")
        public void setCopies(int copies) {
            this.copies = copies;
        }

        public Boolean getArchive() {
            return archive;
        }

        public void setArchive(Boolean archive) {
            this.archive = archive;
        }

        public Map<String, String> getIppOptions() {
            return ippOptions;
        }

        @SuppressWarnings("unused")
        public void setIppOptions(Map<String, String> ippOptions) {
            this.ippOptions = ippOptions;
        }

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final DtoReq dtoReq =
                DtoReq.create(DtoReq.class, this.getParmValueDto());

        final OutboxJobDto dto =
                JOBTICKET_SERVICE.getTicket(dtoReq.getJobFileName());

        final String msgKey;

        if (dto == null) {

            msgKey = "msg-outbox-changed-jobticket-none";

        } else {

            final JobTicketWrapperDto wrapper = new JobTicketWrapperDto(dto);

            try {
                this.saveJobTicket(dtoReq, wrapper);
            } catch (IllegalStateException e) {
                this.setApiResultText(ApiResultCodeEnum.WARN, e.getMessage());
                return;
            }

            notifyUser(dto.getUserId());
            msgKey = "msg-outbox-changed-jobticket";
        }

        this.setApiResult(ApiResultCodeEnum.OK, msgKey);
    }

    /**
     * Saves the Job Ticket.
     *
     * @param dtoReq
     *            The changes.
     * @param wrapper
     *            The ticket wrapper.
     * @throws IllegalStateException
     *             When IPP option choice context is invalid.
     */
    private void saveJobTicket(final DtoReq dtoReq,
            final JobTicketWrapperDto wrapper) {

        final OutboxJobDto dto = wrapper.getTicket();

        if (dto.isSingleAccountPrint()) {
            dto.setSingleAccountPrintCopies(dtoReq.getCopies());
        }

        dto.setArchive(dtoReq.getArchive());

        /*
         * Collect options in temporary map for validation.
         */
        final Map<String, String> optionValuesTmp = new HashMap<>();

        for (final Entry<String, String> entry : dtoReq.getIppOptions()
                .entrySet()) {
            optionValuesTmp.put(entry.getKey(), entry.getValue());
        }

        //
        if (dto.getFillerPages() > 0) {

            final String kwSides = IppDictJobTemplateAttr.ATTR_SIDES;
            final String kwNup = IppDictJobTemplateAttr.ATTR_NUMBER_UP;

            for (final String kw : new String[] { kwSides, kwNup }) {

                if (!StringUtils.defaultString(optionValuesTmp.get(kw))
                        .equals(StringUtils.defaultString(
                                dto.getOptionValues().get(kw)))) {

                    throw new IllegalStateException(String.format(
                            "Document is prepared for \"%s\" or \"%s\", "
                                    + "so these options can not be changed.",
                            PROXY_PRINT_SERVICE.localizePrinterOpt(getLocale(),
                                    kwSides),
                            PROXY_PRINT_SERVICE.localizePrinterOpt(getLocale(),
                                    kwNup)));
                }
            }
        }

        /*
         * TODO: media from media-source
         */

        /*
         * Validate constraints.
         */
        final JsonProxyPrinter proxyPrinter =
                PROXY_PRINT_SERVICE.getCachedPrinter(dto.getPrinter());

        //
        String userMsg = PROXY_PRINT_SERVICE.validateContraintsMsg(proxyPrinter,
                optionValuesTmp, getLocale());

        if (userMsg == null) {
            userMsg = PROXY_PRINT_SERVICE.validateCustomCostRules(proxyPrinter,
                    optionValuesTmp, getLocale());
        }

        if (userMsg != null) {
            throw new IllegalStateException(userMsg);
        }

        /*
         * Now options are valid, finalize them in the dto.
         */
        dto.setOptionValues(optionValuesTmp);

        /*
         * Re-calculate the costs.
         */
        recalcCost(dto, proxyPrinter);

        /*
         * Update.
         */
        try {
            JOBTICKET_SERVICE.updateTicket(wrapper);
        } catch (IOException e) {
            throw new SpException(e.getMessage());
        }
    }

    /**
     * Recalculates the cost of an {@link OutboxJobDto}, based on its intrinsic
     * cost parameters.
     * <p>
     * Note: {@link OutboxJobDto#setCostResult(ProxyPrintCostDto)} is called to
     * update the cost.
     * </p>
     *
     * @param dto
     *            The {@link OutboxJobDto}.
     * @param proxyPrinter
     *            The {@link JsonProxyPrinter}.
     */
    private void recalcCost(final OutboxJobDto dto,
            final JsonProxyPrinter proxyPrinter) {

        final ProxyPrintCostParms costParms =
                new ProxyPrintCostParms(proxyPrinter);

        final IppOptionMap optionMap = new IppOptionMap(dto.getOptionValues());

        // Set parameters.
        costParms.setDuplex(optionMap.isDuplexJob());
        costParms.setEcoPrint(dto.isEcoPrint());
        costParms.setGrayscale(!optionMap.isColorJob());
        costParms.setPagesPerSide(optionMap.getNumberUp());

        costParms.setIppMediaOption(
                optionMap.getOptionValue(IppDictJobTemplateAttr.ATTR_MEDIA));
        costParms.importIppOptionValues(dto.getOptionValues());

        // Calculate custom cost metrics.
        costParms.calcCustomCost();

        //
        final Printer printer = ServiceContext.getDaoContext().getPrinterDao()
                .findByName(dto.getPrinter());

        // Retrieve the regular media source cost.
        final PrinterAttrLookup printerAttrLookup =
                new PrinterAttrLookup(printer);

        final IppMediaSizeEnum ippMediaSize =
                IppMediaSizeEnum.find(costParms.getIppMediaOption());

        final IppMediaSourceCostDto mediaSourceCost =
                printerAttrLookup.findAnyMediaSourceForMedia(ippMediaSize);

        costParms.setMediaSourceCost(mediaSourceCost);

        // Set number of sheets first ...
        dto.setSheets(PdfPrintCollector.calcNumberOfPrintedSheets(
                dto.getPages(), dto.getCopies(), costParms.isDuplex(),
                costParms.getPagesPerSide(), false, false, false));

        // .. and set ...
        costParms.setNumberOfCopies(dto.getCopies());
        costParms.setNumberOfPages(dto.getPages());
        costParms.setNumberOfSheets(dto.getSheets());

        // .. and finally calculate print cost.
        final ProxyPrintCostDto costResult =
                ACCOUNTING_SERVICE.calcProxyPrintCost(printer, costParms);

        dto.setCostResult(costResult);
    }

    /**
     * @param userKey
     *            The user database key
     * @throws IOException
     *             When IO error.
     */
    private void notifyUser(final Long userKey) throws IOException {

        final UserDao userDao = ServiceContext.getDaoContext().getUserDao();

        final User user = userDao.findById(userKey);

        if (UserMsgIndicator.isSafePagesDirPresent(user.getUserId())) {

            UserMsgIndicator.write(user.getUserId(),
                    ServiceContext.getTransactionDate(),
                    UserMsgIndicator.Msg.JOBTICKET_CHANGED, null);
        }
    }

}
