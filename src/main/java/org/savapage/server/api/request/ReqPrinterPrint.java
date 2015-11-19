/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonProcessingException;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.DeviceDao;
import org.savapage.core.dao.helpers.DeviceTypeEnum;
import org.savapage.core.dao.helpers.PrintModeEnum;
import org.savapage.core.dao.helpers.ProxyPrintAuthModeEnum;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.imaging.EcoPrintPdfTaskPendingException;
import org.savapage.core.inbox.InboxInfoDto;
import org.savapage.core.inbox.RangeAtom;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.ipp.attribute.syntax.IppKeyword;
import org.savapage.core.ipp.client.IppConnectException;
import org.savapage.core.jpa.Device;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.User;
import org.savapage.core.print.proxy.ProxyPrintAuthManager;
import org.savapage.core.print.proxy.ProxyPrintException;
import org.savapage.core.print.proxy.ProxyPrintInboxReq;
import org.savapage.core.services.AccountingService;
import org.savapage.core.services.DeviceService;
import org.savapage.core.services.InboxService;
import org.savapage.core.services.OutboxService;
import org.savapage.core.services.PrinterService;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.PageScalingEnum;
import org.savapage.core.services.helpers.ProxyPrintCostParms;
import org.savapage.core.services.impl.InboxServiceImpl;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.server.SpSession;
import org.savapage.server.api.JsonApiDict;
import org.savapage.server.api.JsonApiServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles a Proxy Print request.
 *
 * IMPORTANT: The printing transaction MUST be guarded by
 * {@link ConfigManager#readPrintOutLock()}. This is managed by the caller of
 * this request via {@link JsonApiDict#getPrintOutLockNeeded(String)}.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqPrinterPrint extends ApiRequestMixin {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(ReqPrinterPrint.class);

    /**
     * .
     */
    private static final AccountingService ACCOUNTING_SERVICE = ServiceContext
            .getServiceFactory().getAccountingService();

    /**
     * .
     */
    private static final DeviceService DEVICE_SERVICE = ServiceContext
            .getServiceFactory().getDeviceService();

    /**
     * .
     */
    private static final InboxService INBOX_SERVICE = ServiceContext
            .getServiceFactory().getInboxService();

    /**
     * .
     */
    private static final OutboxService OUTBOX_SERVICE = ServiceContext
            .getServiceFactory().getOutboxService();

    /**
     * .
     */
    private static final PrinterService PRINTER_SERVICE = ServiceContext
            .getServiceFactory().getPrinterService();

    /**
     * .
     */
    private static final ProxyPrintService PROXY_PRINT_SERVICE = ServiceContext
            .getServiceFactory().getProxyPrintService();

    /**
     *
     * @author Rijk Ravestein
     *
     */
    private static class DtoReq extends AbstractDto {

        private String user;
        private String printer;
        private String readerName;
        private String jobName;
        private Integer jobIndex;
        private PageScalingEnum pageScaling;
        private Integer copies;
        private String ranges;
        private Boolean collate;
        private Boolean removeGraphics;
        private Boolean ecoprint;
        private Boolean clear;
        private Map<String, String> options;

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public String getPrinter() {
            return printer;
        }

        public void setPrinter(String printer) {
            this.printer = printer;
        }

        public String getReaderName() {
            return readerName;
        }

        public void setReaderName(String readerName) {
            this.readerName = readerName;
        }

        public String getJobName() {
            return jobName;
        }

        public void setJobName(String jobName) {
            this.jobName = jobName;
        }

        public Integer getJobIndex() {
            return jobIndex;
        }

        public void setJobIndex(Integer jobIndex) {
            this.jobIndex = jobIndex;
        }

        public PageScalingEnum getPageScaling() {
            return pageScaling;
        }

        public void setPageScaling(PageScalingEnum pageScaling) {
            this.pageScaling = pageScaling;
        }

        public Integer getCopies() {
            return copies;
        }

        public void setCopies(Integer copies) {
            this.copies = copies;
        }

        public String getRanges() {
            return ranges;
        }

        public void setRanges(String ranges) {
            this.ranges = ranges;
        }

        public Boolean getCollate() {
            return collate;
        }

        public void setCollate(Boolean collate) {
            this.collate = collate;
        }

        public Boolean getRemoveGraphics() {
            return removeGraphics;
        }

        public void setRemoveGraphics(Boolean removeGraphics) {
            this.removeGraphics = removeGraphics;
        }

        public Boolean getEcoprint() {
            return ecoprint;
        }

        public void setEcoprint(Boolean ecoprint) {
            this.ecoprint = ecoprint;
        }

        public Boolean getClear() {
            return clear;
        }

        public void setClear(Boolean clear) {
            this.clear = clear;
        }

        public Map<String, String> getOptions() {
            return options;
        }

        public void setOptions(Map<String, String> options) {
            this.options = options;
        }

    }

    @Override
    protected void
            onRequest(final String requestingUser, final User lockedUser)
                    throws JsonProcessingException, IOException,
                    ProxyPrintException, IppConnectException, ParseException {

        final DtoReq dtoReq = DtoReq.create(DtoReq.class, getParmValue("dto"));

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(DtoReq.prettyPrint(getParmValue("dto")));
        }

        if (dtoReq.getRemoveGraphics() != null && dtoReq.getRemoveGraphics()
                && dtoReq.getEcoprint() != null && dtoReq.getEcoprint()) {
            setApiResult(ApiResultCodeEnum.INFO, "msg-select-single-pdf-filter");
            return;
        }

        final DeviceDao deviceDao =
                ServiceContext.getDaoContext().getDeviceDao();

        final Map<String, Object> data = new HashMap<String, Object>();

        final Printer printer;

        try {
            printer =
                    PROXY_PRINT_SERVICE.getValidateProxyPrinterAccess(
                            lockedUser, dtoReq.getPrinter(),
                            ServiceContext.getTransactionDate());

        } catch (ProxyPrintException e) {
            setApiResult(ApiResultCodeEnum.ERROR, e.getMessage());
            return;
        }

        final InboxInfoDto jobs =
                INBOX_SERVICE.getInboxInfo(lockedUser.getUserId());

        final int nPagesTot = INBOX_SERVICE.calcNumberOfPagesInJobs(jobs);
        int nPagesPrinted = nPagesTot;

        /*
         * Validate the ranges.
         */
        String ranges = dtoReq.getRanges().trim();

        final boolean printEntireInbox = ranges.isEmpty();

        if (!printEntireInbox) {
            /*
             * Remove inner spaces.
             */
            ranges = ranges.replace(" ", "");
            try {
                final List<RangeAtom> rangeAtoms =
                        INBOX_SERVICE.createSortedRangeArray(ranges);

                nPagesPrinted =
                        InboxServiceImpl.calcSelectedDocPages(rangeAtoms,
                                nPagesTot);
                /*
                 * This gives the SORTED ranges as string: CUPS cannot handle
                 * ranges like '7-8,5,2' but needs '2,5,7-8'
                 */
                ranges = RangeAtom.asText(rangeAtoms);

            } catch (Exception e) {
                setApiResult(ApiResultCodeEnum.ERROR,
                        "msg-clear-range-syntax-error", ranges);
                return;
            }
        }

        /*
         * INVARIANT: number of printed pages can NOT exceed total number of
         * pages.
         */
        if (nPagesPrinted > nPagesTot) {
            setApiResult(ApiResultCodeEnum.ERROR,
                    "msg-print-out-of-range-error", ranges,
                    String.valueOf(nPagesTot));
            return;
        }

        /*
         * Inspect the user printer options.
         */
        final String optionSides =
                dtoReq.getOptions().get(IppDictJobTemplateAttr.ATTR_SIDES);

        final boolean isDuplexPrint =
                optionSides != null
                        && !optionSides.equals(IppKeyword.SIDES_ONE_SIDED);

        /*
         * Create the proxy print request, and chunk it.
         */
        final ProxyPrintInboxReq printReq = new ProxyPrintInboxReq();

        printReq.setCollate(dtoReq.getCollate());
        printReq.setClearPages(dtoReq.getClear());
        printReq.setJobName(dtoReq.getJobName());
        printReq.setPageRanges(ranges);
        printReq.setNumberOfCopies(dtoReq.getCopies());
        printReq.setNumberOfPages(nPagesPrinted);
        printReq.setOptionValues(dtoReq.getOptions());
        printReq.setPrinterName(dtoReq.getPrinter());
        printReq.setRemoveGraphics(dtoReq.getRemoveGraphics());
        printReq.setEcoPrintShadow(dtoReq.getEcoprint());
        printReq.setLocale(this.getLocale());
        printReq.setIdUser(lockedUser.getId());

        /*
         * Vanilla jobs?
         */
        final boolean chunkVanillaJobs;
        final Integer iVanillaJob;

        if (dtoReq.getJobIndex().intValue() < 0) {
            iVanillaJob = null;
            chunkVanillaJobs =
                    isDuplexPrint && printEntireInbox
                            && jobs.getJobs().size() > 1
                            && INBOX_SERVICE.isInboxVanilla(jobs);
        } else {
            iVanillaJob = dtoReq.getJobIndex();
            chunkVanillaJobs = true;
        }

        PROXY_PRINT_SERVICE.chunkProxyPrintRequest(lockedUser, printReq,
                dtoReq.getPageScaling(), chunkVanillaJobs, iVanillaJob);

        /*
         * Calculate the printing cost.
         */
        final String currencySymbol = SpSession.getAppCurrencySymbol();

        final BigDecimal cost;

        try {

            final ProxyPrintCostParms costParms = new ProxyPrintCostParms();

            /*
             * Set the common parameters for all print job chunks, and calculate
             * the cost.
             */
            costParms.setDuplex(printReq.isDuplex());
            costParms.setEcoPrint(printReq.isEcoPrintShadow()
                    || printReq.isEcoPrint());
            costParms.setGrayscale(printReq.isGrayscale());
            costParms.setNumberOfCopies(printReq.getNumberOfCopies());
            costParms.setPagesPerSide(printReq.getNup());

            cost =
                    ACCOUNTING_SERVICE.calcProxyPrintCost(
                            ServiceContext.getLocale(), currencySymbol,
                            lockedUser, printer, costParms,
                            printReq.getJobChunkInfo());

        } catch (ProxyPrintException e) {
            this.setApiResultText(ApiResultCodeEnum.WARN, e.getMessage());
            return;
        }

        printReq.setCost(cost);

        final String localizedCost = localizedPrinterCost(cost, null);

        /*
         * Direct Proxy Print?
         */
        if (dtoReq.getReaderName() == null) {

            printReq.setPrintMode(PrintModeEnum.PUSH);

            try {
                PROXY_PRINT_SERVICE.proxyPrintInbox(lockedUser, printReq);
            } catch (EcoPrintPdfTaskPendingException e) {
                setApiResult(ApiResultCodeEnum.INFO, "msg-ecoprint-pending");
                return;
            }

            JsonApiServer.setApiResultMsg(this.getUserData(), printReq);

            if (printReq.getStatus() == ProxyPrintInboxReq.Status.PRINTED) {

                JsonApiServer.addUserStats(this.getUserData(), lockedUser,
                        this.getLocale(), currencySymbol);
            }

            return;
        }

        /*
         * Proxy Print Authentication is needed (secure printing).
         */
        final Device device = deviceDao.findByName(dtoReq.getReaderName());

        /*
         * INVARIANT: Device MUST exits.
         */
        if (device == null) {
            throw new SpException("Reader Device [" + dtoReq.getReaderName()
                    + "] NOT found");
        }
        /*
         * INVARIANT: Device MUST be enabled.
         */
        if (device.getDisabled()) {
            throw new SpException("Device [" + dtoReq.getReaderName()
                    + "] is disabled");
        }

        /*
         * INVARIANT: Device MUST be a reader.
         */
        if (!deviceDao.isCardReader(device)) {
            throw new SpException("Device [" + dtoReq.getReaderName()
                    + "] is NOT a Card Reader");
        }

        /*
         * INVARIANT: Reader MUST have Printer restriction.
         */
        if (!deviceDao.hasPrinterRestriction(device)) {
            throw new SpException("Reader [" + dtoReq.getReaderName()
                    + "] does not have associated Printer(s).");
        }

        /*
         * INVARIANT: Reader MUST have Printer restriction.
         */
        if (device.getPrinter() == null) {

            if (!PRINTER_SERVICE.checkDeviceSecurity(printer,
                    DeviceTypeEnum.CARD_READER, device)) {

                throw new SpException("Reader [" + dtoReq.getReaderName()
                        + "] does not have associated Printer(s).");
            }
        }

        /*
         * Accounting.
         */
        if (StringUtils.isNotBlank(localizedCost)) {
            data.put("formattedCost", localizedCost);

            if (StringUtils.isNotBlank(localizedCost)) {
                data.put("currencySymbol", currencySymbol);
            }
        }

        /*
         * Hold Print?
         */
        final ProxyPrintAuthModeEnum authModeEnum =
                DEVICE_SERVICE.getProxyPrintAuthMode(device.getId());

        if (authModeEnum.isHoldRelease()) {

            printReq.setPrintMode(PrintModeEnum.HOLD);

            try {
                OUTBOX_SERVICE.proxyPrintInbox(lockedUser, printReq);
            } catch (EcoPrintPdfTaskPendingException e) {
                setApiResult(ApiResultCodeEnum.INFO, "msg-ecoprint-pending");
                return;
            }

            JsonApiServer.setApiResultMsg(this.getUserData(), printReq);
            JsonApiServer.addUserStats(this.getUserData(), lockedUser,
                    this.getLocale(), currencySymbol);

            /*
             * Since the job is present in the outbox we can honor the clearIbox
             * request.
             */
            if (dtoReq.getClear()) {
                INBOX_SERVICE.deleteAllPages(lockedUser.getUserId());
            }

            return;
        }

        /*
         * User WebApp Authenticated Proxy Print.
         */
        printReq.setPrintMode(PrintModeEnum.AUTH);
        printReq.setStatus(ProxyPrintInboxReq.Status.NEEDS_AUTH);

        if (ProxyPrintAuthManager.submitRequest(dtoReq.getPrinter(),
                device.getHostname(), printReq)) {
            /*
             * Signal NEEDS_AUTH
             */
            data.put("requestStatus", printReq.getStatus().toString());
            data.put("printAuthExpirySecs", ConfigManager.instance()
                    .getConfigInt(Key.PROXY_PRINT_DIRECT_EXPIRY_SECS));

            setApiResultOk();

        } else {

            setApiResult(ApiResultCodeEnum.WARN, "msg-print-auth-pending");
        }

    }

    /**
     * Gets the localized string for a BigDecimal.
     *
     * @param decimal
     *            The {@link BigDecimal}.
     * @param currencySymbol
     *            {@code null} when not available.
     * @return The localized string.
     * @throws ParseException
     *             When decimal parsing fails.
     */
    private String localizedPrinterCost(final BigDecimal decimal,
            final String currencySymbol) throws ParseException {

        BigDecimal value = decimal;

        if (value == null) {
            value = BigDecimal.ZERO;
        }

        String cost =
                BigDecimalUtil.localize(value,
                        ConfigManager.getPrinterCostDecimals(),
                        this.getLocale(), true);

        if (StringUtils.isBlank(currencySymbol)) {
            return cost;
        }
        return currencySymbol + cost;
    }

}
