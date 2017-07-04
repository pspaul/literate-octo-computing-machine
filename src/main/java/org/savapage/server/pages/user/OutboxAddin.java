/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
 * Author: Rijk Ravestein.
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
package org.savapage.server.pages.user;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.print.attribute.standard.MediaSizeName;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.dao.AccountDao;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.PrintOutDao;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dao.enums.AppLogLevelEnum;
import org.savapage.core.dao.enums.DaoEnumHelper;
import org.savapage.core.dao.enums.ExternalSupplierEnum;
import org.savapage.core.dao.enums.ExternalSupplierStatusEnum;
import org.savapage.core.i18n.PrintOutNounEnum;
import org.savapage.core.i18n.PrintOutVerbEnum;
import org.savapage.core.ipp.IppJobStateEnum;
import org.savapage.core.ipp.IppSyntaxException;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.ipp.client.IppConnectException;
import org.savapage.core.ipp.helpers.IppOptionMap;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.PrintOut;
import org.savapage.core.outbox.OutboxInfoDto;
import org.savapage.core.outbox.OutboxInfoDto.OutboxAccountTrxInfoSet;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobDto;
import org.savapage.core.print.proxy.JsonProxyPrinter;
import org.savapage.core.print.proxy.ProxyPrintInboxReq;
import org.savapage.core.services.JobTicketService;
import org.savapage.core.services.OutboxService;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserService;
import org.savapage.core.util.DateUtil;
import org.savapage.core.util.MediaUtils;
import org.savapage.server.SpSession;
import org.savapage.server.WebApp;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.pages.ExtSupplierStatusPanel;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.pages.MessageContent;
import org.savapage.server.webapp.WebAppTypeEnum;

/**
 * A page showing the HOLD or TICKET proxy print jobs for a user.
 * <p>
 * This page is retrieved from the JavaScript Web App.
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public class OutboxAddin extends AbstractUserPage {

    /**
     * .
     */
    private static final long serialVersionUID = 1L;

    /**
     * .
     */
    private static final AccountDao ACCOUNT_DAO =
            ServiceContext.getDaoContext().getAccountDao();

    /**
     * .
     */
    private static final OutboxService OUTBOX_SERVICE =
            ServiceContext.getServiceFactory().getOutboxService();

    /**
     * .
     */
    private static final ProxyPrintService PROXYPRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();

    /**
     * .
     */
    private static final JobTicketService JOBTICKET_SERVICE =
            ServiceContext.getServiceFactory().getJobTicketService();

    /**
     * .
     */
    private static final UserService USER_SERVICE =
            ServiceContext.getServiceFactory().getUserService();

    /**
     * .
     */
    private static final UserDao USER_DAO =
            ServiceContext.getDaoContext().getUserDao();

    /**
     * .
     */
    private static final PrintOutDao PRINTOUT_DAO =
            ServiceContext.getDaoContext().getPrintOutDao();

    private static final String WICKET_ID_BTN_EDIT_OUTBOX_JOBTICKET =
            "button-edit-outbox-jobticket";

    private static final String WICKET_ID_BTN_SETTINGS_OUTBOX_JOBTICKET =
            "button-settings-outbox-jobticket";

    private static final String WICKET_ID_BTN_CANCEL_OUTBOX_JOB =
            "button-cancel-outbox-job";

    private static final String WICKET_ID_BTN_CANCEL_OUTBOX_JOBTICKET =
            "button-cancel-outbox-jobticket";

    private static final String WICKET_ID_BTN_PREVIEW_OUTBOX_JOB =
            "button-preview-outbox-job";

    private static final String WICKET_ID_BTN_PREVIEW_OUTBOX_JOBTICKET =
            "button-preview-outbox-jobticket";

    private static final String WICKET_ID_BTN_JOBTICKET_SETTLE =
            "button-jobticket-settle";

    private static final String WICKET_ID_BTN_JOBTICKET_PRINT =
            "button-jobticket-print";

    private static final String WICKET_ID_BTN_JOBTICKET_PRINT_CANCEL =
            "button-jobticket-print-cancel";

    private static final String WICKET_ID_BTN_JOBTICKET_PRINT_CLOSE =
            "button-jobticket-print-close";

    private static final String WICKET_ID_BTN_JOBTICKET_PRINT_RETRY =
            "button-jobticket-print-retry";

    private static final String WICKET_ID_OWNER_USER_ID = "owner-user-id";
    private static final String WICKET_ID_JOB_STATE = "job-state";
    private static final String WICKET_ID_JOB_STATE_IND = "job-state-ind";

    /**
     * .
     */
    private class OutboxJobView extends PropertyListView<OutboxJobDto> {

        /**
         * .
         */
        private static final long serialVersionUID = 1L;

        private static final String CSS_CLASS_TICKET =
                "sp-outbox-item-type-ticket";

        private static final String CSS_CLASS_HOLD = "sp-outbox-item-type-hold";

        private final boolean isJobticketView;

        /**
         *
         * @param id
         * @param list
         * @param jobticketView
         */
        public OutboxJobView(final String id, final List<OutboxJobDto> list,
                final boolean jobticketView) {
            super(id, list);
            this.isJobticketView = jobticketView;
        }

        @Override
        protected void populateItem(ListItem<OutboxJobDto> item) {

            final MarkupHelper helper = new MarkupHelper(item);
            final OutboxJobDto job = item.getModelObject();
            final IppOptionMap optionMap = job.createIppOptionMap();
            final boolean isJobTicketItem = job.getUserId() != null;
            final boolean isCopyJobTicket = job.isCopyJobTicket();

            final PrintOut printOut;

            if (job.getPrintOutId() != null) {
                printOut = PRINTOUT_DAO.findById(job.getPrintOutId());
            } else {
                printOut = null;
            }

            Label labelWlk;

            //
            final JsonProxyPrinter cachedPrinter =
                    PROXYPRINT_SERVICE.getCachedPrinter(job.getPrinter());

            final String printerDisplayName;

            if (cachedPrinter == null) {
                printerDisplayName = "???";
            } else {
                printerDisplayName = StringUtils.defaultString(
                        cachedPrinter.getDbPrinter().getDisplayName(),
                        job.getPrinter());
            }

            /*
             * Fixed attributes.
             */
            item.add(new Label("printer", printerDisplayName));

            item.add(new Label("timeSubmit",
                    job.getLocaleInfo().getSubmitTime()));

            //
            final StringBuilder imgSrc = new StringBuilder();

            //
            String jobticketCopy = null;

            imgSrc.append(WebApp.PATH_IMAGES).append('/');
            if (isJobTicketItem) {
                if (isCopyJobTicket) {
                    imgSrc.append("copy-jobticket-128x128.png");
                    jobticketCopy = localized("jobticket-type-copy");
                } else {
                    imgSrc.append("printer-jobticket-32x32.png");
                }
            } else {
                imgSrc.append("device-card-reader-16x16.png");
            }

            helper.encloseLabel("jobticket-type-copy", jobticketCopy,
                    jobticketCopy != null);

            final Label labelImg = helper.addModifyLabelAttr("img-job",
                    MarkupHelper.ATTR_SRC, imgSrc.toString());

            if (isJobTicketItem) {
                MarkupHelper.modifyLabelAttr(labelImg, MarkupHelper.ATTR_CLASS,
                        CSS_CLASS_TICKET);
            } else {
                MarkupHelper.modifyLabelAttr(labelImg, MarkupHelper.ATTR_CLASS,
                        CSS_CLASS_HOLD);
            }

            //
            helper.encloseLabel("jobticket-remark", job.getComment(),
                    isJobTicketItem
                            && StringUtils.isNotBlank(job.getComment()));

            //
            helper.encloseLabel("jobticket-nr", job.getTicketNumber(),
                    job.getTicketNumber() != null);

            /*
             * Totals
             */
            StringBuilder totals = new StringBuilder();

            //
            String key = null;
            int total = job.getPages();
            int copies = job.getCopies();

            //
            totals.append(helper.localizedNumber(total));
            totals.append(" ")
                    .append(helper.localized(PrintOutNounEnum.PAGE, total > 1));

            // n-up
            if (optionMap.getNumberUp().intValue() > 1) {
                totals.append(", ")
                        .append(localized("n-up", optionMap.getNumberUp()));
            }

            //
            if (copies > 0) {
                totals.append(", ").append(copies).append(" ").append(
                        helper.localized(PrintOutNounEnum.COPY, copies > 1));
            }

            //
            total = job.getSheets();
            if (total > 0) {
                totals.append(" (").append(total)
                        .append(" ").append(helper
                                .localized(PrintOutNounEnum.SHEET, total > 1))
                        .append(")");
            }

            item.add(new Label("totals", totals.toString()));

            //
            final String sparklineData = String.format("%d,%d", job.getSheets(),
                    job.getPages() * job.getCopies() - job.getSheets());
            item.add(new Label("printout-pie", sparklineData));

            if (!getSessionWebAppType().equals(WebAppTypeEnum.JOBTICKETS)
                    || printOut != null) {
                helper.discloseLabel(WICKET_ID_BTN_EDIT_OUTBOX_JOBTICKET);
                helper.discloseLabel(WICKET_ID_BTN_SETTINGS_OUTBOX_JOBTICKET);
            } else {

                labelWlk = helper.encloseLabel(
                        WICKET_ID_BTN_EDIT_OUTBOX_JOBTICKET,
                        HtmlButtonEnum.EDIT.uiTextDottedSfx(getLocale()),
                        isJobTicketItem);

                if (isJobTicketItem) {
                    this.addJobIdAttr(labelWlk, job);
                }

                labelWlk = helper.encloseLabel(
                        WICKET_ID_BTN_SETTINGS_OUTBOX_JOBTICKET,
                        HtmlButtonEnum.SETTINGS.uiTextDottedSfx(getLocale()),
                        isJobTicketItem);

                if (isJobTicketItem) {
                    this.addJobIdAttr(labelWlk, job);
                }

            }

            final String encloseButtonIdRemove;
            final String encloseButtonIdPreview;

            if (isJobTicketItem) {

                helper.discloseLabel(WICKET_ID_BTN_CANCEL_OUTBOX_JOB);
                helper.discloseLabel(WICKET_ID_BTN_PREVIEW_OUTBOX_JOB);

                encloseButtonIdRemove = WICKET_ID_BTN_CANCEL_OUTBOX_JOBTICKET;

                if (isCopyJobTicket) {
                    helper.discloseLabel(
                            WICKET_ID_BTN_PREVIEW_OUTBOX_JOBTICKET);
                    encloseButtonIdPreview = null;
                } else {
                    encloseButtonIdPreview =
                            WICKET_ID_BTN_PREVIEW_OUTBOX_JOBTICKET;
                }
            } else {
                helper.discloseLabel(WICKET_ID_BTN_CANCEL_OUTBOX_JOBTICKET);
                helper.discloseLabel(WICKET_ID_BTN_PREVIEW_OUTBOX_JOBTICKET);
                encloseButtonIdRemove = WICKET_ID_BTN_CANCEL_OUTBOX_JOB;
                encloseButtonIdPreview = WICKET_ID_BTN_PREVIEW_OUTBOX_JOB;
            }

            if (printOut == null) {
                this.addJobIdAttr(
                        helper.encloseLabel(
                                encloseButtonIdRemove, HtmlButtonEnum.CANCEL
                                        .uiText(getLocale(), isJobticketView),
                                true),
                        job);
            } else {
                helper.discloseLabel(encloseButtonIdRemove);
            }

            if (job.isDrm()) {
                helper.discloseLabel(encloseButtonIdPreview);
            } else if (encloseButtonIdPreview != null) {
                this.addJobIdAttr(helper.encloseLabel(encloseButtonIdPreview,
                        HtmlButtonEnum.PREVIEW.uiText(getLocale()), true), job);
            }
            /*
             * Variable attributes.
             */
            final boolean isExpiryUndetermined =
                    job.getSubmitTime() == job.getExpiryTime();

            if (isExpiryUndetermined) {
                helper.discloseLabel("timeExpiry");
            } else {
                labelWlk = new Label("timeExpiry",
                        job.getLocaleInfo().getExpiryTime());

                final long now = System.currentTimeMillis();
                final String cssClass;

                if (job.getExpiryTime() < now) {
                    cssClass = MarkupHelper.CSS_TXT_ERROR;
                } else if (job.getExpiryTime()
                        - now < DateUtil.DURATION_MSEC_HOUR) {
                    cssClass = MarkupHelper.CSS_TXT_WARN;
                } else {
                    cssClass = MarkupHelper.CSS_TXT_VALID;
                }

                MarkupHelper.modifyLabelAttr(labelWlk, "class", cssClass);
                item.add(labelWlk);
            }

            final Map<String, String> mapVisible = new HashMap<>();

            for (final String attr : new String[] { "title", "papersize",
                    "letterhead", "duplex", "simplex", "color", "collate",
                    "grayscale", "accounts", "removeGraphics", "ecoPrint",
                    "extSupplier", "owner-user-name", "drm", "punch", "staple",
                    "fold", "booklet", "jobticket-media", "jobticket-copy",
                    "jobticket-finishing-ext", "jobticket-custom-ext",
                    "landscape", "portrait", "job-id", "job-completed-time",
                    "job-printer" }) {
                mapVisible.put(attr, null);
            }

            //
            mapVisible.put("title", job.getJobName());

            //
            final String mediaOption =
                    ProxyPrintInboxReq.getMediaOption(job.getOptionValues());

            if (mediaOption != null) {
                final MediaSizeName mediaSizeName =
                        MediaUtils.getMediaSizeFromInboxMedia(mediaOption);

                if (mediaSizeName == null) {
                    mapVisible.put("papersize", mediaOption);
                } else {
                    mapVisible.put("papersize",
                            MediaUtils.getUserFriendlyMediaName(mediaSizeName));
                }
            }

            if (ProxyPrintInboxReq.isDuplex(job.getOptionValues())) {
                mapVisible.put("duplex",
                        helper.localized(PrintOutNounEnum.DUPLEX));
            } else {
                mapVisible.put("simplex",
                        helper.localized(PrintOutNounEnum.SIMPLEX));
            }

            if (ProxyPrintInboxReq.isGrayscale(job.getOptionValues())) {
                mapVisible.put("grayscale",
                        helper.localized(PrintOutNounEnum.GRAYSCALE));
            } else {
                mapVisible.put("color",
                        helper.localized(PrintOutNounEnum.COLOR));
            }

            if (BooleanUtils.isTrue(job.getLandscape())) {
                mapVisible.put("landscape",
                        helper.localized(PrintOutNounEnum.LANDSCAPE));
            } else {
                mapVisible.put("portrait",
                        helper.localized(PrintOutNounEnum.PORTRAIT));
            }

            if (job.isCollate() && job.getCopies() > 1 && job.getPages() > 1) {
                mapVisible.put("collate",
                        helper.localized(PrintOutVerbEnum.COLLATE));
            }

            if (job.isRemoveGraphics()) {
                mapVisible.put("removeGraphics", localized("graphics-removed"));
            }
            if (job.isEcoPrint()) {
                mapVisible.put("ecoPrint", "Eco Print");
            }

            if (optionMap.hasFinishingPunch()) {
                mapVisible.put("punch",
                        helper.localized(PrintOutVerbEnum.PUNCH));
            }
            if (optionMap.hasFinishingStaple()) {
                mapVisible.put("staple",
                        helper.localized(PrintOutVerbEnum.STAPLE));
            }
            if (optionMap.hasFinishingFold()) {
                mapVisible.put("fold", helper.localized(PrintOutVerbEnum.FOLD));
            }
            if (optionMap.hasFinishingBooklet()) {
                mapVisible.put("booklet",
                        helper.localized(PrintOutNounEnum.BOOKLET));
            }

            mapVisible.put("jobticket-media",
                    PROXYPRINT_SERVICE.getJobTicketOptionsUiText(getLocale(),
                            IppDictJobTemplateAttr.JOBTICKET_ATTR_MEDIA,
                            optionMap));

            mapVisible.put("jobticket-copy",
                    PROXYPRINT_SERVICE.getJobTicketOptionsUiText(getLocale(),
                            IppDictJobTemplateAttr.JOBTICKET_ATTR_COPY,
                            optionMap));

            mapVisible.put("jobticket-finishings-ext",
                    PROXYPRINT_SERVICE.getJobTicketOptionsUiText(getLocale(),
                            IppDictJobTemplateAttr.JOBTICKET_ATTR_FINISHINGS_EXT,
                            optionMap));

            mapVisible.put("jobticket-custom-ext",
                    PROXYPRINT_SERVICE.getJobTicketOptionsExtHtml(getLocale(),
                            job.getOptionValues()));

            //
            if (job.isDrm()) {
                mapVisible.put("drm", "DRM");
            }

            // Cost
            final StringBuilder cost = new StringBuilder();
            cost.append(job.getLocaleInfo().getCost());

            //
            final OutboxAccountTrxInfoSet trxInfoSet =
                    job.getAccountTransactions();

            if (trxInfoSet != null) {

                final int nAccounts = trxInfoSet.getTransactions().size();

                if (nAccounts > 0) {

                    cost.append(" (");

                    final boolean showAccountSum;

                    if (nAccounts == 1) {

                        final Account account = ACCOUNT_DAO.findById(trxInfoSet
                                .getTransactions().get(0).getAccountId());

                        final AccountTypeEnum accountType = AccountTypeEnum
                                .valueOf(account.getAccountType());

                        showAccountSum = accountType != AccountTypeEnum.SHARED;

                        if (!showAccountSum) {
                            if (account.getParent() != null) {
                                cost.append(account.getParent().getName())
                                        .append("\\");
                            }
                            account.getName();
                            cost.append(account.getName());
                        }

                    } else {
                        showAccountSum = true;
                    }

                    if (showAccountSum) {
                        cost.append(nAccounts).append(" ")
                                .append(helper.localized(
                                        PrintOutNounEnum.ACCOUNT,
                                        nAccounts > 1));
                    }
                    cost.append(")");
                }

            }

            item.add(new Label("cost",
                    StringUtils.replace(cost.toString(), " ", "&nbsp;"))
                            .setEscapeModelStrings(false));

            // Job Ticket Supplier
            final String extSupplierImgUrl;

            if (job.getExternalSupplierInfo() != null) {
                final ExternalSupplierEnum supplier =
                        job.getExternalSupplierInfo().getSupplier();
                mapVisible.put("extSupplier", supplier.getUiText());
                extSupplierImgUrl = WebApp.getExtSupplierEnumImgUrl(supplier);
            } else {
                extSupplierImgUrl = null;
            }

            if (StringUtils.isBlank(extSupplierImgUrl)) {
                helper.discloseLabel("extSupplierImg");
            } else {
                helper.encloseLabel("extSupplierImg", "", true)
                        .add(new AttributeModifier("src", extSupplierImgUrl));
            }

            // External Print Management (by PaperCut)
            final ExtSupplierStatusPanel panelExtPrintStatus;
            final ExternalSupplierStatusEnum extPrintStatus;

            if (printOut == null) {
                panelExtPrintStatus = null;
                extPrintStatus = null;
            } else {
                final org.savapage.core.jpa.DocLog docLog =
                        printOut.getDocOut().getDocLog();

                final ExternalSupplierEnum supplier =
                        DaoEnumHelper.getExtSupplier(docLog);

                if (supplier == null) {
                    panelExtPrintStatus = null;
                    extPrintStatus = null;
                } else {
                    extPrintStatus = DaoEnumHelper.getExtSupplierStatus(docLog);

                    panelExtPrintStatus =
                            new ExtSupplierStatusPanel("extSupplierPanel");
                    panelExtPrintStatus.populate(supplier, extPrintStatus,
                            PROXYPRINT_SERVICE.getExtPrinterManager(
                                    printOut.getPrinter().getPrinterName()));
                }
            }

            if (panelExtPrintStatus == null) {
                helper.discloseLabel("extSupplierPanel");
            } else {
                item.add(panelExtPrintStatus);
            }

            //
            if (printOut == null) {

                helper.discloseLabel(WICKET_ID_JOB_STATE_IND);

            } else {

                final IppJobStateEnum jobState = IppJobStateEnum
                        .asEnum(printOut.getCupsJobState().intValue());

                final String cssClass = MarkupHelper.getCssTxtClass(jobState);

                //
                labelWlk = helper.encloseLabel(WICKET_ID_JOB_STATE,
                        jobState.uiText(getLocale()), true);
                MarkupHelper.appendLabelAttr(labelWlk, "class", cssClass);

                if (printOut.getCupsCompletedTime() != null) {
                    mapVisible.put("job-completed-time",
                            DateUtil.localizedShortTime(
                                    PROXYPRINT_SERVICE.getCupsDate(
                                            printOut.getCupsCompletedTime()),
                                    getLocale()));
                }

                //
                final String cssClassInd;
                final String jobStateInd;

                if (extPrintStatus == null) {
                    cssClassInd = cssClass;
                    jobStateInd = jobState.uiText(getLocale());
                } else {
                    jobStateInd = extPrintStatus.uiText(getLocale());
                    cssClassInd = MarkupHelper.getCssTxtClass(extPrintStatus);
                }

                labelWlk = helper.encloseLabel(WICKET_ID_JOB_STATE_IND,
                        jobStateInd, true);
                MarkupHelper.appendLabelAttr(labelWlk, "class", cssClassInd);

            }

            if (printOut != null && getSessionWebAppType()
                    .equals(WebAppTypeEnum.JOBTICKETS)) {

                final IppJobStateEnum jobState = IppJobStateEnum
                        .asEnum(printOut.getCupsJobState().intValue());

                if (jobState.isPresentOnQueue()) {
                    helper.discloseLabel(WICKET_ID_BTN_JOBTICKET_PRINT_CLOSE);
                } else {
                    this.addJobIdAttr(helper.encloseLabel(
                            WICKET_ID_BTN_JOBTICKET_PRINT_CLOSE,
                            HtmlButtonEnum.CLOSE.uiText(getLocale()), true),
                            job);
                }

                boolean enableRetryButton;

                if (jobState.equals(IppJobStateEnum.IPP_JOB_COMPLETED)) {
                    helper.discloseLabel(WICKET_ID_BTN_JOBTICKET_PRINT_CANCEL);

                    enableRetryButton =
                            extPrintStatus == ExternalSupplierStatusEnum.CANCELLED
                                    || extPrintStatus == ExternalSupplierStatusEnum.EXPIRED
                                    || extPrintStatus == ExternalSupplierStatusEnum.ERROR;

                } else if (jobState.isPresentOnQueue()) {
                    enableRetryButton = false;
                    this.addJobIdAttr(helper.encloseLabel(
                            WICKET_ID_BTN_JOBTICKET_PRINT_CANCEL,
                            HtmlButtonEnum.CANCEL.uiText(getLocale()), true),
                            job);
                } else {
                    helper.discloseLabel(WICKET_ID_BTN_JOBTICKET_PRINT_CANCEL);
                    enableRetryButton = true;
                }

                if (enableRetryButton) {
                    this.addJobIdAttr(helper.encloseLabel(
                            WICKET_ID_BTN_JOBTICKET_PRINT_RETRY,
                            HtmlButtonEnum.RETRY.uiText(getLocale()), true),
                            job);
                } else {
                    helper.discloseLabel(WICKET_ID_BTN_JOBTICKET_PRINT_RETRY);
                }

            } else {

                helper.discloseLabel(WICKET_ID_BTN_JOBTICKET_PRINT_CANCEL);
                helper.discloseLabel(WICKET_ID_BTN_JOBTICKET_PRINT_CLOSE);
                helper.discloseLabel(WICKET_ID_BTN_JOBTICKET_PRINT_RETRY);
            }

            final boolean readUser;

            if (printOut != null) {

                mapVisible.put("job-id", printOut.getCupsJobId().toString());
                mapVisible.put("job-printer",
                        printOut.getPrinter().getDisplayName());

                helper.discloseLabel(WICKET_ID_BTN_JOBTICKET_PRINT);
                helper.discloseLabel(WICKET_ID_BTN_JOBTICKET_SETTLE);

                readUser = true;

            } else if (this.isJobticketView && job.getUserId() != null) {

                if (isCopyJobTicket || optionMap.isJobTicketSettleOnly()) {
                    helper.discloseLabel(WICKET_ID_BTN_JOBTICKET_PRINT);
                } else {
                    this.addJobIdAttr(helper.encloseLabel(
                            WICKET_ID_BTN_JOBTICKET_PRINT,
                            HtmlButtonEnum.PRINT.uiTextDottedSfx(getLocale()),
                            true), job);
                }

                this.addJobIdAttr(
                        helper.encloseLabel(WICKET_ID_BTN_JOBTICKET_SETTLE,
                                HtmlButtonEnum.SETTLE
                                        .uiTextDottedSfx(getLocale()),
                                true),
                        job);

                readUser = true;

            } else {
                helper.discloseLabel(WICKET_ID_OWNER_USER_ID);
                helper.discloseLabel(WICKET_ID_BTN_JOBTICKET_PRINT);
                helper.discloseLabel(WICKET_ID_BTN_JOBTICKET_SETTLE);
                readUser = false;
            }

            if (readUser) {
                final org.savapage.core.jpa.User user =
                        USER_DAO.findById(job.getUserId());
                if (user == null) {
                    labelWlk = helper.encloseLabel(WICKET_ID_OWNER_USER_ID,
                            "*** USER NOT FOUND ***", true);
                    MarkupHelper.appendLabelAttr(labelWlk, "class",
                            MarkupHelper.CSS_TXT_WARN);
                } else {
                    helper.encloseLabel(WICKET_ID_OWNER_USER_ID,
                            user.getUserId(), true);
                    mapVisible.put("owner-user-name", user.getFullName());

                    final String email =
                            USER_SERVICE.getPrimaryEmailAddress(user);

                    if (StringUtils.isBlank(email)) {
                        helper.discloseLabel("owner-user-email");
                    } else {
                        labelWlk = helper.encloseLabel("owner-user-email",
                                email, true);
                        MarkupHelper.appendLabelAttr(labelWlk, "href",
                                String.format("mailto:%s", email));
                    }
                }
            }
            /*
             * Hide/Show
             */
            for (Map.Entry<String, String> entry : mapVisible.entrySet()) {

                if (entry.getValue() == null) {
                    entry.setValue("");
                }

                helper.encloseLabel(entry.getKey(), entry.getValue(),
                        StringUtils.isNotBlank(entry.getValue()));
            }
        }

        /**
         * Adds job identification and title as HTML attribute to Label.
         *
         * @param label
         *            The label
         * @param job
         *            The job.
         * @return The same label (for chaining).
         */
        private Label addJobIdAttr(final Label label, final OutboxJobDto job) {
            label.add(new AttributeModifier(MarkupHelper.ATTR_DATA_SAVAPAGE,
                    job.getFile()))
                    .add(new AttributeModifier(MarkupHelper.ATTR_TITLE,
                            label.getDefaultModelObjectAsString()));
            return label;
        }
    }

    /**
     * .
     */
    public OutboxAddin(final PageParameters parameters) {

        super(parameters);

        final IRequestParameters parms =
                getRequestCycle().getRequest().getPostParameters();

        final boolean isJobticketView =
                parms.getParameterValue("jobTickets").toBoolean();

        if (isJobticketView
                && getSessionWebAppType() != WebAppTypeEnum.JOBTICKETS
                && getSessionWebAppType() != WebAppTypeEnum.ADMIN) {
            setResponsePage(
                    new MessageContent(AppLogLevelEnum.ERROR, "Access denied"));
            return;
        }

        /*
         * We need the cache to get the alias of the printer later on.
         */
        try {
            PROXYPRINT_SERVICE.lazyInitPrinterCache();
        } catch (IppConnectException | IppSyntaxException e) {
            setResponsePage(
                    new MessageContent(AppLogLevelEnum.ERROR, e.getMessage()));
            return;
        }

        final SpSession session = SpSession.get();

        final OutboxInfoDto outboxInfo;

        if (isJobticketView) {

            outboxInfo = new OutboxInfoDto();

            /*
             * Job Tickets mix-in.
             */
            final Long userKey =
                    parms.getParameterValue("userKey").toOptionalLong();

            final boolean expiryAsc = BooleanUtils.isTrue(
                    parms.getParameterValue("expiryAsc").toOptionalBoolean());

            final List<OutboxJobDto> tickets;

            if (userKey == null) {
                tickets = JOBTICKET_SERVICE.getTickets();
            } else {
                tickets = JOBTICKET_SERVICE.getTickets(userKey);
            }

            Collections.sort(tickets, new Comparator<OutboxJobDto>() {
                @Override
                public int compare(final OutboxJobDto left,
                        final OutboxJobDto right) {
                    final int ret;
                    if (left.getExpiryTime() < right.getExpiryTime()) {
                        ret = -1;
                    } else if (left.getExpiryTime() > right.getExpiryTime()) {
                        ret = 1;
                    } else {
                        ret = 0;
                    }
                    if (expiryAsc) {
                        return ret;
                    }
                    return -ret;
                }
            });

            for (final OutboxJobDto dto : tickets) {
                outboxInfo.addJob(dto.getFile(), dto);
            }

        } else {
            final DaoContext daoContext = ServiceContext.getDaoContext();
            /*
             * Lock user while getting the OutboxInfo.
             */
            daoContext.beginTransaction();

            final org.savapage.core.jpa.User lockedUser =
                    daoContext.getUserDao().lock(session.getUser().getId());

            outboxInfo = OUTBOX_SERVICE.getOutboxJobTicketInfo(lockedUser,
                    ServiceContext.getTransactionDate());

            // unlock
            daoContext.rollback();
        }

        OUTBOX_SERVICE.applyLocaleInfo(outboxInfo, session.getLocale(),
                SpSession.getAppCurrencySymbol());

        final List<OutboxJobDto> entryList = new ArrayList<>();

        for (final Entry<String, OutboxJobDto> entry : outboxInfo.getJobs()
                .entrySet()) {
            entryList.add(entry.getValue());
        }

        //
        add(new OutboxJobView("job-entry", entryList, isJobticketView));
    }

}
