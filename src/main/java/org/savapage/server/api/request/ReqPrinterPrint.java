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
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.codehaus.jackson.JsonProcessingException;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.DeviceDao;
import org.savapage.core.dao.enums.DeviceTypeEnum;
import org.savapage.core.dao.enums.PrintModeEnum;
import org.savapage.core.dao.enums.ProxyPrintAuthModeEnum;
import org.savapage.core.dao.helpers.JsonPrintDelegation;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.dto.PrintDelegationDto;
import org.savapage.core.imaging.EcoPrintPdfTaskPendingException;
import org.savapage.core.inbox.InboxInfoDto;
import org.savapage.core.inbox.RangeAtom;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.ipp.attribute.syntax.IppKeyword;
import org.savapage.core.ipp.client.IppConnectException;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.Device;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.User;
import org.savapage.core.print.proxy.ProxyPrintAuthManager;
import org.savapage.core.print.proxy.ProxyPrintException;
import org.savapage.core.print.proxy.ProxyPrintInboxReq;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.AccountTrxInfo;
import org.savapage.core.services.helpers.AccountTrxInfoSet;
import org.savapage.core.services.helpers.InboxSelectScopeEnum;
import org.savapage.core.services.helpers.PageScalingEnum;
import org.savapage.core.services.helpers.ProxyPrintCostParms;
import org.savapage.core.services.impl.InboxServiceImpl;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.core.util.DateUtil;
import org.savapage.ext.papercut.PaperCutServerProxy;
import org.savapage.ext.papercut.job.PaperCutPrintMonitorJob;
import org.savapage.server.SpSession;
import org.savapage.server.api.JsonApiDict;
import org.savapage.server.api.JsonApiServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

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
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ReqPrinterPrint.class);

    /**
     *
     * @author Rijk Ravestein
     *
     */
    @JsonInclude(Include.NON_NULL)
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
        private InboxSelectScopeEnum clearScope;
        private Boolean jobTicket;
        private Long jobTicketDate;
        private Integer jobTicketHrs;
        private Integer jobTicketMin;
        private String jobTicketRemark;
        private Map<String, String> options;
        private PrintDelegationDto delegation;

        @SuppressWarnings("unused")
        public String getUser() {
            return user;
        }

        @SuppressWarnings("unused")
        public void setUser(String user) {
            this.user = user;
        }

        public String getPrinter() {
            return printer;
        }

        @SuppressWarnings("unused")
        public void setPrinter(String printer) {
            this.printer = printer;
        }

        public String getReaderName() {
            return readerName;
        }

        @SuppressWarnings("unused")
        public void setReaderName(String readerName) {
            this.readerName = readerName;
        }

        public String getJobName() {
            return jobName;
        }

        @SuppressWarnings("unused")
        public void setJobName(String jobName) {
            this.jobName = jobName;
        }

        public Integer getJobIndex() {
            return jobIndex;
        }

        @SuppressWarnings("unused")
        public void setJobIndex(Integer jobIndex) {
            this.jobIndex = jobIndex;
        }

        public PageScalingEnum getPageScaling() {
            return pageScaling;
        }

        @SuppressWarnings("unused")
        public void setPageScaling(PageScalingEnum pageScaling) {
            this.pageScaling = pageScaling;
        }

        public Integer getCopies() {
            return copies;
        }

        @SuppressWarnings("unused")
        public void setCopies(Integer copies) {
            this.copies = copies;
        }

        public String getRanges() {
            return ranges;
        }

        @SuppressWarnings("unused")
        public void setRanges(String ranges) {
            this.ranges = ranges;
        }

        public Boolean getCollate() {
            return collate;
        }

        @SuppressWarnings("unused")
        public void setCollate(Boolean collate) {
            this.collate = collate;
        }

        public Boolean getRemoveGraphics() {
            return removeGraphics;
        }

        @SuppressWarnings("unused")
        public void setRemoveGraphics(Boolean removeGraphics) {
            this.removeGraphics = removeGraphics;
        }

        public Boolean getEcoprint() {
            return ecoprint;
        }

        @SuppressWarnings("unused")
        public void setEcoprint(Boolean ecoprint) {
            this.ecoprint = ecoprint;
        }

        public InboxSelectScopeEnum getClearScope() {
            return clearScope;
        }

        @SuppressWarnings("unused")
        public void setClearScope(InboxSelectScopeEnum clearScope) {
            this.clearScope = clearScope;
        }

        public Boolean getJobTicket() {
            return jobTicket;
        }

        @SuppressWarnings("unused")
        public void setJobTicket(Boolean jobTicket) {
            this.jobTicket = jobTicket;
        }

        public Long getJobTicketDate() {
            return jobTicketDate;
        }

        @SuppressWarnings("unused")
        public void setJobTicketDate(Long jobTicketDate) {
            this.jobTicketDate = jobTicketDate;
        }

        public Integer getJobTicketHrs() {
            return jobTicketHrs;
        }

        @SuppressWarnings("unused")
        public void setJobTicketHrs(Integer jobTicketHrs) {
            this.jobTicketHrs = jobTicketHrs;
        }

        public Integer getJobTicketMin() {
            return jobTicketMin;
        }

        @SuppressWarnings("unused")
        public void setJobTicketMin(Integer jobTicketMin) {
            this.jobTicketMin = jobTicketMin;
        }

        public String getJobTicketRemark() {
            return jobTicketRemark;
        }

        @SuppressWarnings("unused")
        public void setJobTicketRemark(String jobTicketRemark) {
            this.jobTicketRemark = jobTicketRemark;
        }

        public Map<String, String> getOptions() {
            return options;
        }

        @SuppressWarnings("unused")
        public void setOptions(Map<String, String> options) {
            this.options = options;
        }

        public PrintDelegationDto getDelegation() {
            return delegation;
        }

        @SuppressWarnings("unused")
        public void setDelegation(PrintDelegationDto delegation) {
            this.delegation = delegation;
        }

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws JsonProcessingException, IOException, ProxyPrintException,
            IppConnectException, ParseException {

        final DtoReq dtoReq = DtoReq.create(DtoReq.class, getParmValue("dto"));

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(DtoReq.prettyPrint(getParmValue("dto")));
        }

        if (dtoReq.getRemoveGraphics() != null && dtoReq.getRemoveGraphics()
                && dtoReq.getEcoprint() != null && dtoReq.getEcoprint()) {
            setApiResult(ApiResultCodeEnum.INFO,
                    "msg-select-single-pdf-filter");
            return;
        }

        final ConfigManager cm = ConfigManager.instance();

        /*
         * If/how to clear the inbox.
         */
        final InboxSelectScopeEnum clearScope;

        if (cm.isConfigValue(Key.WEBAPP_USER_PROXY_PRINT_CLEAR_INBOX_ENABLE)) {

            /*
             * Overrule scope with system setting.
             */
            clearScope = cm.getConfigEnum(InboxSelectScopeEnum.class,
                    Key.WEBAPP_USER_PROXY_PRINT_CLEAR_INBOX_SCOPE);

            if (clearScope == null) {
                throw new IllegalStateException("Invalid clear scope.");
            }

        } else if (dtoReq.getClearScope() == null) {
            clearScope = InboxSelectScopeEnum.NONE;
        } else {
            clearScope = dtoReq.getClearScope();
        }

        //
        final DeviceDao deviceDao =
                ServiceContext.getDaoContext().getDeviceDao();

        final Map<String, Object> data = new HashMap<String, Object>();

        final Printer printer;

        try {
            printer = PROXY_PRINT_SERVICE.getValidateProxyPrinterAccess(
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

                nPagesPrinted = InboxServiceImpl
                        .calcSelectedDocPages(rangeAtoms, nPagesTot);
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

        final boolean isJobTicket = BooleanUtils.isTrue(dtoReq.getJobTicket());

        /*
         * INVARIANT: when NOT a job ticket the total number of printed pages
         * MUST be within limits. When Job Ticketing is enabled, this option is
         * prompted to the user.
         */
        if (!isJobTicket) {

            final int totPages = dtoReq.getCopies().intValue() * nPagesPrinted;

            final Integer maxPages =
                    cm.getConfigInteger(Key.PROXY_PRINT_MAX_PAGES);

            if (maxPages != null && totPages > maxPages.intValue()) {

                if (StringUtils.isBlank(
                        cm.getConfigValue(Key.JOBTICKET_PROXY_PRINTER))) {

                    setApiResult(ApiResultCodeEnum.ERROR,
                            "msg-print-exceeds-pagelimit",
                            String.valueOf(totPages), String.valueOf(maxPages));
                } else {
                    setApiResult(ApiResultCodeEnum.WARN,
                            "msg-print-exceeds-jobticket-pagelimit",
                            String.valueOf(totPages), String.valueOf(maxPages));
                }
                return;
            }
        }

        /*
         * Inspect the user printer options.
         */
        final String optionSides =
                dtoReq.getOptions().get(IppDictJobTemplateAttr.ATTR_SIDES);

        final boolean isDuplexPrint = optionSides != null
                && !optionSides.equals(IppKeyword.SIDES_ONE_SIDED);

        /*
         * Vanilla jobs?
         */
        final boolean chunkVanillaJobs;
        final Integer iVanillaJob;

        if (dtoReq.getJobIndex().intValue() < 0) {
            iVanillaJob = null;
            chunkVanillaJobs = isDuplexPrint && printEntireInbox
                    && jobs.getJobs().size() > 1
                    && INBOX_SERVICE.isInboxVanilla(jobs);
        } else {
            iVanillaJob = dtoReq.getJobIndex();
            chunkVanillaJobs = true;
        }

        /*
         * Create the proxy print request, and chunk it.
         */
        final ProxyPrintInboxReq printReq = new ProxyPrintInboxReq(iVanillaJob);

        printReq.setCollate(dtoReq.getCollate());
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
        printReq.setClearScope(clearScope);

        //
        printReq.setConvertToGrayscale(printReq.isGrayscale()
                && PROXY_PRINT_SERVICE.isColorPrinter(dtoReq.getPrinter())
                && PRINTER_SERVICE.isClientSideMonochrome(printer));

        //
        final PrintDelegationDto delegationDto = dtoReq.getDelegation();

        final boolean isDelegatedPrint =
                delegationDto != null && (!delegationDto.getGroups().isEmpty()
                        || !delegationDto.getUsers().isEmpty());

        if (isDelegatedPrint) {

            if (!cm.isConfigValue(Key.PROXY_PRINT_DELEGATE_ENABLE)) {
                throw new SpException("Delegated Print is disabled.");
            }

            final JsonPrintDelegation jsonDelegation =
                    JsonPrintDelegation.create(dtoReq.getDelegation());

            final AccountTrxInfoSet infoSet = PRINT_DELEGATION_SERVICE
                    .createAccountTrxInfoSet(jsonDelegation);

            printReq.setNumberOfCopies(infoSet.getWeightTotal());

            if (printReq.getNumberOfCopies() > 1) {
                printReq.setCollate(true);
            }
            printReq.setAccountTrxInfoSet(infoSet);
        }

        PROXY_PRINT_SERVICE.chunkProxyPrintRequest(lockedUser, printReq,
                dtoReq.getPageScaling(), chunkVanillaJobs, iVanillaJob);

        /*
         * Non-secure Proxy Print, integrated with PaperCut?
         */
        final boolean isNonSecureProxyPrint = dtoReq.getReaderName() == null;
        final boolean isExtPaperCutPrint;

        if (isNonSecureProxyPrint && isDelegatedPrint) {
            /*
             * PaperCut integration enable + PaperCut Managed Printer AND
             * Delegated Print integration with PaperCut?
             */
            isExtPaperCutPrint = PAPERCUT_SERVICE
                    .isExtPaperCutPrint(printer.getPrinterName())
                    && cm.isConfigValue(
                            Key.PROXY_PRINT_DELEGATE_PAPERCUT_ENABLE);

        } else {
            isExtPaperCutPrint = false;
        }

        if (isExtPaperCutPrint) {

            final List<String> usersNotFound = checkPaperCutUsers(
                    requestingUser, printReq.getAccountTrxInfoSet());

            if (!usersNotFound.isEmpty()) {

                final StringBuilder users = new StringBuilder();

                for (final String id : usersNotFound) {
                    users.append(id).append(' ');
                }
                setApiResult(ApiResultCodeEnum.ERROR,
                        "msg-print-users-missing-in-papercut",
                        users.toString().trim());
                return;
            }
        }

        /*
         * Calculate the printing cost.
         */
        final String currencySymbol = SpSession.getAppCurrencySymbol();

        final BigDecimal cost;

        try {
            if (isExtPaperCutPrint) {
                /*
                 * No need to calculate the cost since it is taken from PaperCut
                 * after PaperCut reports that job is printed successfully.
                 */
                cost = BigDecimal.ZERO;
            } else {
                /*
                 * Set the common parameters for all print job chunks, and
                 * calculate the cost.
                 */
                final ProxyPrintCostParms costParms =
                        printReq.createProxyPrintCostParms();

                cost = ACCOUNTING_SERVICE.calcProxyPrintCost(
                        ServiceContext.getLocale(), currencySymbol, lockedUser,
                        printer, costParms, printReq.getJobChunkInfo());
            }

        } catch (ProxyPrintException e) {
            this.setApiResultText(ApiResultCodeEnum.WARN, e.getMessage());
            return;
        }

        printReq.setCost(cost);

        final String localizedCost = localizedPrinterCost(cost, null);

        /*
         * Job Ticket?
         */
        if (isJobTicket) {

            printReq.setComment(dtoReq.getJobTicketRemark());

            Date deliveryDate;

            if (dtoReq.getJobTicketDate() == null) {
                deliveryDate = new Date();
            } else {
                deliveryDate = new Date(dtoReq.getJobTicketDate().longValue());
            }

            int minutes = 0;

            if (dtoReq.getJobTicketHrs() != null) {
                minutes += dtoReq.getJobTicketHrs().intValue()
                        * DateUtil.MINUTES_IN_HOUR;
            }

            if (dtoReq.getJobTicketMin() != null) {
                minutes += dtoReq.getJobTicketMin().intValue();
            }

            deliveryDate = DateUtils.addMinutes(
                    DateUtils.truncate(deliveryDate, Calendar.DAY_OF_MONTH),
                    minutes);

            this.onPrintJobTicket(lockedUser, printReq, currencySymbol,
                    deliveryDate);

            return;
        }

        /*
         * Direct Proxy Print integrated with PaperCut?
         */
        if (isExtPaperCutPrint) {
            this.onExtPaperCutPrint(lockedUser, printReq, currencySymbol);
            return;
        }

        /*
         * Direct Proxy Print?
         */
        if (isNonSecureProxyPrint) {
            this.onDirectProxyPrint(lockedUser, printReq, currencySymbol);
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
            throw new SpException(
                    "Reader Device [" + dtoReq.getReaderName() + "] NOT found");
        }
        /*
         * INVARIANT: Device MUST be enabled.
         */
        if (device.getDisabled()) {
            throw new SpException(
                    "Device [" + dtoReq.getReaderName() + "] is disabled");
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
            this.onHoldPrint(lockedUser, printReq, currencySymbol);
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
            data.put("printAuthExpirySecs",
                    cm.getConfigInt(Key.PROXY_PRINT_DIRECT_EXPIRY_SECS));

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

        String cost = BigDecimalUtil.localize(value,
                ConfigManager.getPrinterCostDecimals(), this.getLocale(), true);

        if (StringUtils.isBlank(currencySymbol)) {
            return cost;
        }
        return currencySymbol + cost;
    }

    /**
     *
     * @param lockedUser
     *            The locked {@link User} instance, can be {@code null}.
     * @param printReq
     *            The print request.
     * @param currencySymbol
     *            The currency symbol.
     * @param clearAfterPrint
     *            {@code true} to clear inbox after printing.
     * @param deliveryDate
     *            The requested date of delivery.
     */
    private void onPrintJobTicket(final User lockedUser,
            final ProxyPrintInboxReq printReq, final String currencySymbol,
            final Date deliveryDate) {

        printReq.setPrintMode(PrintModeEnum.PUSH);

        try {
            JOBTICKET_SERVICE.proxyPrintInbox(lockedUser, printReq,
                    deliveryDate);
        } catch (EcoPrintPdfTaskPendingException e) {
            setApiResult(ApiResultCodeEnum.INFO, "msg-ecoprint-pending");
            return;
        }

        /*
         * Since the job is preserved in the outbox we clear the inbox.
         */
        printReq.setClearedObjects(
                PROXY_PRINT_SERVICE.clearInbox(lockedUser, printReq));

        //
        JsonApiServer.setApiResultMsg(this.getUserData(), printReq);

        ApiRequestHelper.addUserStats(this.getUserData(), lockedUser,
                this.getLocale(), currencySymbol);

    }

    /**
     *
     * @param lockedUser
     *            The locked {@link User} instance, can be {@code null}.
     * @param printReq
     *            The print request.
     * @param currencySymbol
     *            The currency symbol.
     */
    private void onHoldPrint(final User lockedUser,
            final ProxyPrintInboxReq printReq, final String currencySymbol) {

        printReq.setPrintMode(PrintModeEnum.HOLD);

        try {
            OUTBOX_SERVICE.proxyPrintInbox(lockedUser, printReq);
        } catch (EcoPrintPdfTaskPendingException e) {
            setApiResult(ApiResultCodeEnum.INFO, "msg-ecoprint-pending");
            return;
        }

        /*
         * Since the job is preserved in the outbox we clear the inbox.
         */
        printReq.setClearedObjects(
                PROXY_PRINT_SERVICE.clearInbox(lockedUser, printReq));

        //
        JsonApiServer.setApiResultMsg(this.getUserData(), printReq);
        ApiRequestHelper.addUserStats(this.getUserData(), lockedUser,
                this.getLocale(), currencySymbol);
    }

    /**
     *
     * @param lockedUser
     *            The locked {@link User} instance, can be {@code null}.
     * @param printReq
     *            The print request.
     * @param currencySymbol
     *            The currency symbol.
     * @throws IppConnectException
     */
    private void onDirectProxyPrint(final User lockedUser,
            final ProxyPrintInboxReq printReq, final String currencySymbol)
            throws IppConnectException {

        printReq.setPrintMode(PrintModeEnum.PUSH);

        try {
            PROXY_PRINT_SERVICE.proxyPrintInbox(lockedUser, printReq);
        } catch (EcoPrintPdfTaskPendingException e) {
            setApiResult(ApiResultCodeEnum.INFO, "msg-ecoprint-pending");
            return;
        }

        JsonApiServer.setApiResultMsg(this.getUserData(), printReq);

        if (printReq.getStatus() == ProxyPrintInboxReq.Status.PRINTED) {

            ApiRequestHelper.addUserStats(this.getUserData(), lockedUser,
                    this.getLocale(), currencySymbol);
        }
    }

    /**
     * Proxy Prints to a PaperCut managed printer.
     * <p>
     * The PaperCut status is monitored by {@link PaperCutPrintMonitorJob}.
     * </p>
     *
     * @param lockedUser
     *            The locked {@link User} instance, can be {@code null}.
     * @param printReq
     * @param currencySymbol
     * @throws IppConnectException
     */
    private void onExtPaperCutPrint(final User lockedUser,
            final ProxyPrintInboxReq printReq, final String currencySymbol)
            throws IppConnectException {

        PAPERCUT_SERVICE.prepareForExtPaperCut(printReq, null);

        try {
            PROXY_PRINT_SERVICE.proxyPrintInbox(lockedUser, printReq);

        } catch (EcoPrintPdfTaskPendingException e) {
            setApiResult(ApiResultCodeEnum.INFO, "msg-ecoprint-pending");
            return;
        }

        JsonApiServer.setApiResultMsg(this.getUserData(), printReq);

        if (printReq.getStatus() == ProxyPrintInboxReq.Status.PRINTED) {

            ApiRequestHelper.addUserStats(this.getUserData(), lockedUser,
                    this.getLocale(), currencySymbol);
        }
    }

    /**
     * Checks if users exist in PaperCut.
     *
     * @param requestingUser
     *            The ID of the requesting user.
     * @param infoSet
     *            The {@link AccountTrxInfoSet} containing the user account
     *            information.
     * @return A list with IDs of users not found.
     */
    private List<String> checkPaperCutUsers(final String requestingUser,
            final AccountTrxInfoSet infoSet) {

        final List<String> usersNotFound = new ArrayList<>();

        final PaperCutServerProxy serverProxy =
                PaperCutServerProxy.create(ConfigManager.instance(), false);

        if (PAPERCUT_SERVICE.findUser(serverProxy, requestingUser) == null) {
            usersNotFound.add(requestingUser);
        }

        for (final AccountTrxInfo info : infoSet.getAccountTrxInfoList()) {
            final AccountTypeEnum accountType = EnumUtils.getEnum(
                    AccountTypeEnum.class, info.getAccount().getAccountType());

            if (accountType == AccountTypeEnum.GROUP
                    || accountType == AccountTypeEnum.SHARED) {
                continue;
            }

            final String userId = info.getAccount().getNameLower();

            if (PAPERCUT_SERVICE.findUser(serverProxy, userId) == null) {
                usersNotFound.add(userId);
            }
        }

        return usersNotFound;
    }

}
