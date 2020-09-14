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
package org.savapage.server.pages.user;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.print.attribute.standard.MediaSizeName;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.WebAppTypeEnum;
import org.savapage.core.dao.AccountDao;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.PrintOutDao;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.AppLogLevelEnum;
import org.savapage.core.dao.enums.DaoEnumHelper;
import org.savapage.core.dao.enums.ExternalSupplierEnum;
import org.savapage.core.dao.enums.ExternalSupplierStatusEnum;
import org.savapage.core.doc.store.DocStoreBranchEnum;
import org.savapage.core.doc.store.DocStoreTypeEnum;
import org.savapage.core.i18n.AdjectiveEnum;
import org.savapage.core.i18n.AdverbEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.i18n.PrintOutAdjectiveEnum;
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
import org.savapage.core.outbox.OutboxInfoDto.OutboxAccountTrxInfo;
import org.savapage.core.outbox.OutboxInfoDto.OutboxAccountTrxInfoSet;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobDto;
import org.savapage.core.print.proxy.JsonProxyPrinter;
import org.savapage.core.print.proxy.ProxyPrintInboxReq;
import org.savapage.core.print.proxy.TicketJobSheetDto;
import org.savapage.core.services.AccessControlService;
import org.savapage.core.services.AccountingService;
import org.savapage.core.services.DocStoreService;
import org.savapage.core.services.JobTicketService;
import org.savapage.core.services.OutboxService;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserService;
import org.savapage.core.services.helpers.ProxyPrintCostDto;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.core.util.DateUtil;
import org.savapage.core.util.MediaUtils;
import org.savapage.server.WebApp;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.helpers.SparklineHtml;
import org.savapage.server.pages.ExtSupplierStatusPanel;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.pages.MessageContent;
import org.savapage.server.session.SpSession;

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

    /** */
    private static final long serialVersionUID = 1L;

    /** */
    private static final AccountDao ACCOUNT_DAO =
            ServiceContext.getDaoContext().getAccountDao();

    /** */
    private static final AccessControlService ACCESS_CONTROL_SERVICE =
            ServiceContext.getServiceFactory().getAccessControlService();

    /** */
    private static final AccountingService ACCOUNTING_SERVICE =
            ServiceContext.getServiceFactory().getAccountingService();

    /** */
    private static final DocStoreService DOC_STORE_SERVICE =
            ServiceContext.getServiceFactory().getDocStoreService();

    /** */
    private static final OutboxService OUTBOX_SERVICE =
            ServiceContext.getServiceFactory().getOutboxService();

    /** */
    private static final ProxyPrintService PROXYPRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();

    /** */
    private static final JobTicketService JOBTICKET_SERVICE =
            ServiceContext.getServiceFactory().getJobTicketService();

    /** */
    private static final UserService USER_SERVICE =
            ServiceContext.getServiceFactory().getUserService();

    /** */
    private static final UserDao USER_DAO =
            ServiceContext.getDaoContext().getUserDao();

    /** */
    private static final PrintOutDao PRINTOUT_DAO =
            ServiceContext.getDaoContext().getPrintOutDao();

    /** */
    private static final String WICKET_ID_BTN_EDIT_OUTBOX_JOBTICKET =
            "button-edit-outbox-jobticket";

    /** */
    private static final String WICKET_ID_BTN_SETTINGS_OUTBOX_JOBTICKET =
            "button-settings-outbox-jobticket";

    /** */
    private static final String WICKET_ID_BTN_CANCEL_OUTBOX_JOB =
            "button-cancel-outbox-job";

    /** */
    private static final String WICKET_ID_BTN_CANCEL_OUTBOX_JOBTICKET =
            "button-cancel-outbox-jobticket";

    /** */
    private static final String WICKET_ID_BTN_PREVIEW_OUTBOX_JOB =
            "button-preview-outbox-job";
    /** */
    private static final String WICKET_ID_BTN_PREVIEW_OUTBOX_JOBTICKET =
            "button-preview-outbox-jobticket";
    /** */
    private static final String WICKET_ID_BTN_JOBTICKET_SETTLE =
            "button-jobticket-settle";
    /** */
    private static final String WICKET_ID_BTN_ACCOUNT_TRX_INFO_JOB =
            "button-account-trx-info-job";
    /** */
    private static final String WICKET_ID_BTN_ACCOUNT_TRX_INFO_JOBTICKET =
            "button-account-trx-info-jobticket";
    /** */
    private static final String WICKET_ID_BTN_JOBTICKET_PRINT =
            "button-jobticket-print";
    /** */
    private static final String WICKET_ID_BTN_OUTBOX_PRINT_RELEASE =
            "button-outbox-print-release";

    /** */
    private static final String WICKET_ID_BTN_JOBTICKET_PRINT_CANCEL =
            "button-jobticket-print-cancel";
    /** */
    private static final String WICKET_ID_BTN_JOBTICKET_PRINT_CLOSE =
            "button-jobticket-print-close";
    /** */
    private static final String WICKET_ID_BTN_JOBTICKET_PRINT_RETRY =
            "button-jobticket-print-retry";
    /** */
    private static final String WICKET_ID_OWNER_USER_ID = "owner-user-id";
    /** */
    private static final String WICKET_ID_OWNER_USER_EMAIL = "owner-user-email";

    /** */
    private static final String WICKET_ID_JOB_STATE = "job-state";
    /** */
    private static final String WICKET_ID_JOB_STATE_IND = "job-state-ind";
    /** */
    private static final String WICKET_ID_JOB_STATE_DURATION =
            "job-state-duration";
    /** */
    private static final String WICKET_ID_JOB_STATE_DURATION_EXT =
            "job-state-duration-ext";

    /** */
    private static final String WICKET_ID_IMG_DOC_STORE = "img-docstore";

    /** */
    private static final String WICKET_ID_CHUNK_INDEX = "chunkIndex";
    /** */
    private static final String WICKET_ID_CHUNK_SIZE = "chunkSize";

    /** */
    private static final String CSS_CLASS_JOBTICKET_PRINT_COMPLETED =
            "sp-jobticket-print-completed";

    /**
     * Boolean.
     */
    private static final String PAGE_PARM_JOBTICKETS = "jobTickets";

    /**
     * String.
     */
    private static final String PAGE_PARM_JOBTICKET_ID = "jobTicketId";

    /**
     * Long.
     * <p>
     * Primary DB key of job ticket printer group.
     * </p>
     */
    private static final String PAGE_PARM_JOBTICKET_GROUP_ID =
            "jobTicketGroupId";

    /**
     * String.
     */
    private static final String PAGE_PARM_USERKEY = "userKey";

    /**
     * Boolean.
     */
    private static final String PAGE_PARM_EXPIRY_ASC = "expiryAsc";

    /**
     * Integer.
     */
    private static final String PAGE_PARM_MAX_ITEMS = "maxItems";

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
         * Number of decimals for decimal scaling.
         */
        final int scale;

        /**
         * Number of currency decimals to display.
         */
        private final int currencyDecimals;

        /** */
        private final MediaSizeName defaultMediaSize;

        /** */
        private final WebAppTypeEnum webappType;

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
            this.scale = ConfigManager.getFinancialDecimalsInDatabase();
            this.currencyDecimals = ConfigManager.getUserBalanceDecimals();
            this.defaultMediaSize = MediaUtils.getDefaultMediaSize();
            this.webappType = getSessionWebAppType();
        }

        @Override
        protected void populateItem(final ListItem<OutboxJobDto> item) {

            final MarkupHelper helper = new MarkupHelper(item);
            final OutboxJobDto job = item.getModelObject();
            final IppOptionMap optionMap = job.createIppOptionMap();
            final boolean isJobTicketItem = job.getUserId() != null;
            final boolean isCopyJobTicket = job.isCopyJobTicket();

            final Locale locale = SpSession.get().getLocale();

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
                    imgSrc.append("scanner-32x32.png");
                    jobticketCopy = localized("jobticket-type-copy");
                } else {
                    imgSrc.append("printer-26x26.png");
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
                final HtmlButtonEnum title;
                if (isCopyJobTicket) {
                    title = HtmlButtonEnum.COPY;
                } else {
                    title = HtmlButtonEnum.PRINT;
                }
                MarkupHelper.modifyLabelAttr(labelImg, MarkupHelper.ATTR_TITLE,
                        title.uiText(locale));

            } else {
                MarkupHelper.modifyLabelAttr(labelImg, MarkupHelper.ATTR_CLASS,
                        CSS_CLASS_HOLD);
            }

            this.addJobIdAttr(
                    helper.encloseLabel(WICKET_ID_BTN_OUTBOX_PRINT_RELEASE,
                            HtmlButtonEnum.PRINT.uiText(locale),
                            !isJobTicketItem && this.webappType
                                    .equals(WebAppTypeEnum.PRINTSITE)),
                    job);

            //
            final TicketJobSheetDto jobSheet;
            if (isJobTicketItem) {
                jobSheet = JOBTICKET_SERVICE
                        .getTicketJobSheet(job.createIppOptionMap());
            } else {
                jobSheet = null;
            }
            if (jobSheet != null && jobSheet.isEnabled()) {
                imgSrc.setLength(0);
                imgSrc.append(WebApp.PATH_IMAGES).append('/');
                imgSrc.append("copy-jobticket-128x128.png");

                MarkupHelper.modifyLabelAttr(
                        helper.addModifyLabelAttr("img-job-sheet",
                                MarkupHelper.ATTR_SRC, imgSrc.toString()),
                        MarkupHelper.ATTR_TITLE,
                        PROXYPRINT_SERVICE.localizePrinterOpt(locale,
                                IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_JOB_SHEETS));

            } else {
                helper.discloseLabel("img-job-sheet");
            }

            //
            helper.encloseLabel("jobticket-remark", job.getComment(),
                    isJobTicketItem
                            && StringUtils.isNotBlank(job.getComment()));

            //
            helper.encloseLabel("jobticket-nr", job.getTicketNumber(),
                    job.getTicketNumber() != null);

            if (job.getTicketNumber() != null) {
                final String tagWord = JOBTICKET_SERVICE
                        .getTicketNumberLabel(job.getTicketNumber());
                helper.encloseLabel("jobticket-tag",
                        StringUtils.defaultString(tagWord), tagWord != null);
            }

            /*
             * Totals
             */
            StringBuilder totals = new StringBuilder();

            //
            int total = job.getPages();
            int copies = job.getCopies();

            //
            totals.append(helper.localizedNumber(total));
            totals.append(" ")
                    .append(helper.localized(PrintOutNounEnum.PAGE, total > 1));

            // n-up
            if (optionMap != null && optionMap.getNumberUp().intValue() > 1) {
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
                totals.append(" (").append(total).append(" ").append(
                        helper.localized(PrintOutNounEnum.SHEET, total > 1))
                        .append(")");
            }

            item.add(new Label("totals", totals.toString()));

            //
            MarkupHelper
                    .modifyLabelAttr(
                            helper.addModifyLabelAttr("printout-pie",
                                    SparklineHtml.valueString(
                                            String.valueOf(job.getSheets()),
                                            String.valueOf(job.getPages()
                                                    * job.getCopies()
                                                    - job.getSheets())),
                                    SparklineHtml.ATTR_SLICE_COLORS,
                                    SparklineHtml.arrayAttr(
                                            SparklineHtml.COLOR_PRINTER,
                                            SparklineHtml.COLOR_SHEET)),
                            MarkupHelper.ATTR_CLASS,
                            SparklineHtml.CSS_CLASS_PRINTOUT);
            //
            if (!this.webappType.equals(WebAppTypeEnum.JOBTICKETS)
                    || printOut != null) {
                helper.discloseLabel(WICKET_ID_BTN_EDIT_OUTBOX_JOBTICKET);
                helper.discloseLabel(WICKET_ID_BTN_SETTINGS_OUTBOX_JOBTICKET);
            } else {
                labelWlk =
                        helper.encloseLabel(WICKET_ID_BTN_EDIT_OUTBOX_JOBTICKET,
                                HtmlButtonEnum.EDIT.uiTextDottedSfx(locale),
                                isJobTicketItem);

                if (isJobTicketItem) {
                    this.addJobIdAttr(labelWlk, job);
                }

                labelWlk = helper.encloseLabel(
                        WICKET_ID_BTN_SETTINGS_OUTBOX_JOBTICKET,
                        HtmlButtonEnum.SETTINGS.uiTextDottedSfx(locale),
                        isJobTicketItem);

                if (isJobTicketItem) {
                    this.addJobIdAttr(labelWlk, job);
                }
            }

            // AccounTrx preview
            final String encloseButtonIdTrx;
            if (isJobTicketItem) {
                helper.discloseLabel(WICKET_ID_BTN_ACCOUNT_TRX_INFO_JOB);
                encloseButtonIdTrx = WICKET_ID_BTN_ACCOUNT_TRX_INFO_JOBTICKET;
            } else {
                helper.discloseLabel(WICKET_ID_BTN_ACCOUNT_TRX_INFO_JOBTICKET);
                encloseButtonIdTrx = WICKET_ID_BTN_ACCOUNT_TRX_INFO_JOB;
            }
            this.addJobIdAttr(helper.encloseLabel(encloseButtonIdTrx,
                    String.format("%s%s",
                            NounEnum.TRANSACTION.uiText(locale, true),
                            HtmlButtonEnum.DOTTED_SUFFIX),
                    true), job);

            //
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
                this.addJobIdAttr(helper.encloseLabel(encloseButtonIdRemove,
                        HtmlButtonEnum.CANCEL.uiText(locale, isJobticketView),
                        true), job);
            } else {
                helper.discloseLabel(encloseButtonIdRemove);
            }

            if (job.isDrm()) {
                helper.discloseLabel(encloseButtonIdPreview);
            } else if (encloseButtonIdPreview != null) {
                this.addJobIdAttr(
                        helper.encloseLabel(encloseButtonIdPreview,
                                HtmlButtonEnum.PREVIEW.uiText(locale), true),
                        job);
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

                MarkupHelper.modifyLabelAttr(labelWlk, MarkupHelper.ATTR_CLASS,
                        cssClass);
                item.add(labelWlk);
            }

            final Map<String, String> mapVisible = new HashMap<>();

            for (final String attr : new String[] { "title", "pdf-papersize",
                    "pdf-papersize-ext", "papersize", "papersize-ext",
                    "letterhead", "duplex", "simplex", "color", "collate",
                    "grayscale", "accounts", "removeGraphics", "ecoPrint",
                    "extSupplier", "owner-user-name", "drm", "pageRotate180",
                    "punch", "staple", "fold", "booklet", "jobticket-tag-plain",
                    "jobticket-media", "jobticket-copy",
                    "jobticket-finishing-ext", "jobticket-custom-ext",
                    "landscape", "scaled", "portrait", "job-id",
                    "job-creation-time", "job-completed-time", "job-printer",
                    "msg-invalid" }) {
                mapVisible.put(attr, null);
            }
            if (!isJobTicketItem
                    && StringUtils.isNotBlank(job.getJobTicketTag())) {
                mapVisible.put("jobticket-tag-plain", job.getJobTicketTag());
            }

            //
            mapVisible.put("title", job.getJobName());

            if (job.getChunkIndex() == null || job.getChunkSize() == null
                    || job.getChunkSize().intValue() < 2) {
                helper.discloseLabel(WICKET_ID_CHUNK_SIZE);
            } else {
                helper.addLabel(WICKET_ID_CHUNK_INDEX,
                        job.getChunkIndex().toString());
                helper.addLabel(WICKET_ID_CHUNK_SIZE,
                        job.getChunkSize().toString());
            }

            //
            final MediaSizeName mediaSizeNamePDF;
            final String mediaPDF = job.getMedia();
            final String mediaPDFDisplay;

            if (mediaPDF == null) {
                mediaSizeNamePDF = null;
                mediaPDFDisplay = null;
            } else {
                mediaSizeNamePDF =
                        MediaUtils.getMediaSizeFromInboxMedia(mediaPDF);
                if (mediaSizeNamePDF == null) {
                    mediaPDFDisplay = mediaPDF;
                    mapVisible.put("pdf-papersize-ext", mediaPDF);
                } else {
                    final String mediaKey;
                    if (mediaSizeNamePDF.equals(this.defaultMediaSize)) {
                        mediaKey = "pdf-papersize";
                    } else {
                        mediaKey = "pdf-papersize-ext";
                    }
                    mediaPDFDisplay = MediaUtils
                            .getUserFriendlyMediaName(mediaSizeNamePDF);
                    mapVisible.put(mediaKey, mediaPDFDisplay);
                }
            }

            //
            final String mediaOption =
                    ProxyPrintInboxReq.getMediaOption(job.getOptionValues());

            final boolean isMediaOptionDisplay;

            if (mediaOption != null) {

                final MediaSizeName mediaSizeName =
                        MediaUtils.getMediaSizeFromInboxMedia(mediaOption);

                if (mediaSizeName == null) {
                    isMediaOptionDisplay =
                            !mediaOption.equalsIgnoreCase(mediaPDFDisplay);
                    if (isMediaOptionDisplay) {
                        mapVisible.put("papersize-ext", mediaOption);
                    }
                } else {
                    final String mediaKey;
                    if (mediaSizeName.equals(this.defaultMediaSize)) {
                        mediaKey = "papersize";
                    } else {
                        mediaKey = "papersize-ext";
                    }
                    final String mediaDisplay =
                            MediaUtils.getUserFriendlyMediaName(mediaSizeName);

                    isMediaOptionDisplay =
                            !mediaDisplay.equalsIgnoreCase(mediaPDFDisplay);

                    if (isMediaOptionDisplay) {
                        mapVisible.put(mediaKey, mediaDisplay);
                    }
                }
            } else {
                isMediaOptionDisplay = false;
            }

            if (isMediaOptionDisplay) {
                final String htmlSep;
                if (mediaPDF == null) {
                    htmlSep = " &bull; ";
                } else {
                    htmlSep = " &gt; ";
                }
                helper.addLabel("papersize-separator", htmlSep)
                        .setEscapeModelStrings(false);
            } else {
                helper.discloseLabel("papersize-separator");
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

            if (BooleanUtils.isTrue(job.getFitToPage())
                    || (optionMap != null && optionMap.hasPrintScaling())) {
                mapVisible.put("scaled", AdjectiveEnum.SCALED.uiText(locale));
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

            if (optionMap.hasPageRotate180()) {
                mapVisible.put("pageRotate180",
                        PROXYPRINT_SERVICE.localizePrinterOpt(locale,
                                IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_INT_PAGE_ROTATE180));
            }

            if (optionMap.hasFinishingPunch()) {
                mapVisible.put("punch", uiIppKeywordValue(locale,
                        IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_FINISHINGS_PUNCH,
                        optionMap));
            }
            if (optionMap.hasFinishingStaple()) {
                mapVisible.put("staple", uiIppKeywordValue(locale,
                        IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_FINISHINGS_STAPLE,
                        optionMap));
            }
            if (optionMap.hasFinishingFold()) {
                mapVisible.put("fold", String.format("%s %s",
                        helper.localized(PrintOutVerbEnum.FOLD),
                        uiIppKeywordValue(locale,
                                IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_FINISHINGS_FOLD,
                                optionMap)));
            }
            if (optionMap.hasFinishingBooklet()) {
                mapVisible.put("booklet",
                        helper.localized(PrintOutNounEnum.BOOKLET));
            }

            mapVisible.put("jobticket-media",
                    PROXYPRINT_SERVICE.getJobTicketOptionsUiText(locale,
                            IppDictJobTemplateAttr.JOBTICKET_ATTR_MEDIA,
                            optionMap));

            mapVisible.put("jobticket-copy",
                    PROXYPRINT_SERVICE.getJobTicketOptionsUiText(locale,
                            IppDictJobTemplateAttr.JOBTICKET_ATTR_COPY,
                            optionMap));

            mapVisible.put("jobticket-finishings-ext",
                    PROXYPRINT_SERVICE.getJobTicketOptionsUiText(locale,
                            IppDictJobTemplateAttr.JOBTICKET_ATTR_FINISHINGS_EXT,
                            optionMap));

            mapVisible.put("jobticket-custom-ext", PROXYPRINT_SERVICE
                    .getJobTicketOptionsExtHtml(locale, job.getOptionValues()));

            //
            if (job.isDrm()) {
                mapVisible.put("drm", "DRM");
            }

            // Cost
            final StringBuilder sbAccTrx = new StringBuilder();
            final int nAccountsMissing =
                    getCostHtml(helper, job, sbAccTrx, locale);

            item.add(new Label("cost", sbAccTrx.toString())
                    .setEscapeModelStrings(false));

            if (nAccountsMissing == 1) {
                mapVisible.put("msg-invalid", localized("msg-missing-account"));
            } else if (nAccountsMissing > 1) {
                mapVisible.put("msg-invalid",
                        localized("msg-missing-accounts", nAccountsMissing));
            }

            final boolean allAccountsArePresent = nAccountsMissing == 0;

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
            final ExternalSupplierEnum supplier;

            if (printOut == null) {
                panelExtPrintStatus = null;
                extPrintStatus = null;
                supplier = null;
            } else {
                final org.savapage.core.jpa.DocLog docLog =
                        printOut.getDocOut().getDocLog();

                supplier = DaoEnumHelper.getExtSupplier(docLog);

                if (supplier == null) {
                    panelExtPrintStatus = null;
                    extPrintStatus = null;
                } else {
                    extPrintStatus = DaoEnumHelper.getExtSupplierStatus(docLog);

                    panelExtPrintStatus =
                            new ExtSupplierStatusPanel("extSupplierPanel");
                }
            }

            //
            if (printOut == null) {

                helper.discloseLabel(WICKET_ID_JOB_STATE_DURATION);
                helper.discloseLabel(WICKET_ID_JOB_STATE_DURATION_EXT);

                if (allAccountsArePresent) {
                    helper.discloseLabel(WICKET_ID_JOB_STATE_IND);
                } else {
                    MarkupHelper
                            .appendLabelAttr(
                                    helper.encloseLabel(WICKET_ID_JOB_STATE_IND,
                                            AdjectiveEnum.INVALID
                                                    .uiText(locale),
                                            true),
                                    MarkupHelper.ATTR_CLASS,
                                    MarkupHelper.CSS_TXT_ERROR);
                }

            } else {

                final IppJobStateEnum jobState = IppJobStateEnum
                        .asEnum(printOut.getCupsJobState().intValue());

                final String cssClass = MarkupHelper.getCssTxtClass(jobState);

                //
                labelWlk = helper.encloseLabel(WICKET_ID_JOB_STATE,
                        jobState.uiText(locale), true);
                MarkupHelper.appendLabelAttr(labelWlk, MarkupHelper.ATTR_CLASS,
                        cssClass);

                final Date dateCreation = PROXYPRINT_SERVICE
                        .getCupsDate(printOut.getCupsCreationTime());

                final long msecStateDuration;
                final Long msecStateDurationExt;

                if (jobState.isFinished()
                        && printOut.getCupsCompletedTime() != null
                        && printOut.getCupsCompletedTime().intValue() != 0) {

                    final Date dateCompleted = PROXYPRINT_SERVICE
                            .getCupsDate(printOut.getCupsCompletedTime());

                    mapVisible.put("job-completed-time",
                            DateUtil.localizedShortTime(dateCompleted, locale));

                    msecStateDuration =
                            dateCompleted.getTime() - dateCreation.getTime();

                    if (jobState == IppJobStateEnum.IPP_JOB_COMPLETED
                            && extPrintStatus == ExternalSupplierStatusEnum.PENDING_EXT) {
                        msecStateDurationExt =
                                Long.valueOf(System.currentTimeMillis()
                                        - dateCompleted.getTime());
                    } else {
                        msecStateDurationExt = null;
                    }

                } else {
                    mapVisible.put("job-creation-since",
                            AdverbEnum.SINCE.uiText(locale).toLowerCase());
                    mapVisible.put("job-creation-time",
                            DateUtil.localizedShortTime(dateCreation, locale));
                    msecStateDuration =
                            System.currentTimeMillis() - dateCreation.getTime();
                    msecStateDurationExt = null;
                }

                //
                final String cssClassInd;
                final String cssClassIndCups;
                final String cssClassIndExt;
                final String jobStateInd;

                if (extPrintStatus == null) {
                    cssClassInd = cssClass;
                    cssClassIndCups = cssClass;
                    cssClassIndExt = null;
                    jobStateInd = jobState.uiText(locale);
                } else {
                    cssClassInd = MarkupHelper.getCssTxtClass(extPrintStatus);
                    cssClassIndExt = cssClassInd;
                    cssClassIndCups = cssClass;
                    jobStateInd = extPrintStatus.uiText(locale);
                }

                //
                labelWlk = helper.encloseLabel(WICKET_ID_JOB_STATE_IND,
                        jobStateInd, true);
                MarkupHelper.appendLabelAttr(labelWlk, MarkupHelper.ATTR_CLASS,
                        cssClassInd);

                if (msecStateDuration < DateUtil.DURATION_MSEC_SECOND) {
                    helper.discloseLabel(WICKET_ID_JOB_STATE_DURATION);
                } else {
                    labelWlk = helper.encloseLabel(WICKET_ID_JOB_STATE_DURATION,
                            formatDuration(msecStateDuration), true);
                    MarkupHelper.appendLabelAttr(labelWlk,
                            MarkupHelper.ATTR_CLASS, cssClassIndCups);
                }

                //
                final String extSupplierStatusText;

                if (msecStateDurationExt != null) {

                    extSupplierStatusText =
                            formatDuration(msecStateDurationExt.longValue());

                    /*
                     * No display for now...
                     */
                    labelWlk = helper.encloseLabel(
                            WICKET_ID_JOB_STATE_DURATION_EXT,
                            extSupplierStatusText, false);

                    // MarkupHelper.appendLabelAttr(labelWlk,
                    // MarkupHelper.ATTR_CLASS, cssClassIndExt);

                } else {
                    helper.discloseLabel(WICKET_ID_JOB_STATE_DURATION_EXT);
                    extSupplierStatusText = null;
                }

                // Check if external print status monitoring is applicable.
                if (panelExtPrintStatus != null) {
                    panelExtPrintStatus.populate(supplier, extPrintStatus,
                            extSupplierStatusText,
                            PROXYPRINT_SERVICE.getExtPrinterManager(
                                    printOut.getPrinter().getPrinterName()),
                            locale);
                }
            }

            if (panelExtPrintStatus == null) {
                helper.discloseLabel("extSupplierPanel");
            } else {
                item.add(panelExtPrintStatus);
            }

            if (printOut != null
                    && this.webappType.equals(WebAppTypeEnum.JOBTICKETS)) {

                final IppJobStateEnum jobState = IppJobStateEnum
                        .asEnum(printOut.getCupsJobState().intValue());

                if (jobState.isPresentOnQueue()) {
                    helper.discloseLabel(WICKET_ID_BTN_JOBTICKET_PRINT_CLOSE);
                } else {
                    final Label btnClose = this.addJobIdAttr(
                            helper.encloseLabel(
                                    WICKET_ID_BTN_JOBTICKET_PRINT_CLOSE,
                                    HtmlButtonEnum.CLOSE.uiText(locale), true),
                            job);

                    if ((extPrintStatus == null && jobState
                            .equals(IppJobStateEnum.IPP_JOB_COMPLETED))
                            || (extPrintStatus != null && extPrintStatus.equals(
                                    ExternalSupplierStatusEnum.COMPLETED))) {
                        MarkupHelper.appendLabelAttr(btnClose,
                                MarkupHelper.ATTR_CLASS,
                                CSS_CLASS_JOBTICKET_PRINT_COMPLETED);
                    }
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
                    this.addJobIdAttr(
                            helper.encloseLabel(
                                    WICKET_ID_BTN_JOBTICKET_PRINT_CANCEL,
                                    HtmlButtonEnum.CANCEL.uiText(locale), true),
                            job);
                } else {
                    helper.discloseLabel(WICKET_ID_BTN_JOBTICKET_PRINT_CANCEL);
                    enableRetryButton = true;
                }

                if (enableRetryButton) {
                    this.addJobIdAttr(
                            helper.encloseLabel(
                                    WICKET_ID_BTN_JOBTICKET_PRINT_RETRY,
                                    HtmlButtonEnum.RETRY.uiText(locale), true),
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

            // Assign default: set to false in exceptional situations, where
            // Ticket must not be processed.
            boolean enclosePrintSettle = allAccountsArePresent;

            if (printOut != null) {

                mapVisible.put("job-id", printOut.getCupsJobId().toString());
                mapVisible.put("job-printer",
                        printOut.getPrinter().getDisplayName());

                enclosePrintSettle = false;
                readUser = true;

            } else if (this.isJobticketView && job.getUserId() != null) {

                readUser = true;

            } else {
                helper.discloseLabel(WICKET_ID_OWNER_USER_ID);
                helper.discloseLabel(WICKET_ID_OWNER_USER_EMAIL);

                enclosePrintSettle = false;
                readUser = false;
            }

            final org.savapage.core.jpa.User user;

            if (readUser) {

                user = USER_DAO.findById(job.getUserId());

                if (user == null) {
                    labelWlk = helper.encloseLabel(WICKET_ID_OWNER_USER_ID,
                            "*** USER NOT FOUND ***", true);
                    MarkupHelper.appendLabelAttr(labelWlk,
                            MarkupHelper.ATTR_CLASS, MarkupHelper.CSS_TXT_WARN);

                    helper.discloseLabel(WICKET_ID_OWNER_USER_EMAIL);

                    enclosePrintSettle = false;

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
                        MarkupHelper.appendLabelAttr(labelWlk,
                                MarkupHelper.ATTR_HREF,
                                String.format("mailto:%s", email));
                    }
                }
            } else {
                user = null;
            }

            if (enclosePrintSettle) {
                if (isCopyJobTicket || optionMap.isJobTicketSettleOnly()) {
                    helper.discloseLabel(WICKET_ID_BTN_JOBTICKET_PRINT);
                } else {
                    final Label lbl = this.addJobIdAttr(
                            helper.encloseLabel(WICKET_ID_BTN_JOBTICKET_PRINT,
                                    HtmlButtonEnum.PRINT
                                            .uiTextDottedSfx(locale),
                                    true),
                            job);

                    if (job.getCopies() == 0) {
                        MarkupHelper.modifyLabelAttr(lbl,
                                MarkupHelper.ATTR_DISABLED,
                                MarkupHelper.ATTR_DISABLED);
                    }
                }

                final Label lbl =
                        this.addJobIdAttr(
                                helper.encloseLabel(
                                        WICKET_ID_BTN_JOBTICKET_SETTLE,
                                        HtmlButtonEnum.SETTLE
                                                .uiTextDottedSfx(locale),
                                        true),
                                job);

                if (job.getCopies() == 0) {
                    MarkupHelper.modifyLabelAttr(lbl,
                            MarkupHelper.ATTR_DISABLED,
                            MarkupHelper.ATTR_DISABLED);
                }

            } else {
                helper.discloseLabel(WICKET_ID_BTN_JOBTICKET_PRINT);
                helper.discloseLabel(WICKET_ID_BTN_JOBTICKET_SETTLE);
            }

            //
            if (BooleanUtils.isTrue(job.getArchive())) {

                imgSrc.setLength(0);
                imgSrc.append(WebApp.PATH_IMAGES).append('/');
                imgSrc.append("archive-16x16.png");
                MarkupHelper.modifyLabelAttr(
                        helper.addModifyLabelAttr(WICKET_ID_IMG_DOC_STORE,
                                MarkupHelper.ATTR_SRC, imgSrc.toString()),
                        MarkupHelper.ATTR_TITLE,
                        HtmlButtonEnum.ARCHIVE.uiText(locale));

            } else if (!isJobTicketItem && cachedPrinter != null
                    && !cachedPrinter.isJournalDisabled()
                    && DOC_STORE_SERVICE.isEnabled(DocStoreTypeEnum.JOURNAL,
                            DocStoreBranchEnum.OUT_PRINT)) {

                final org.savapage.core.jpa.User userWlk;
                if (user == null) {
                    // Do not use job.getUserId(), because == null.
                    userWlk = USER_DAO.findById(SpSession.get().getUserDbKey());
                } else {
                    userWlk = user;
                }

                if (ACCESS_CONTROL_SERVICE.hasAccess(userWlk,
                        ACLOidEnum.U_PRINT_JOURNAL)) {
                    imgSrc.setLength(0);
                    imgSrc.append(WebApp.PATH_IMAGES).append('/');
                    imgSrc.append("journal-16x16.png");
                    MarkupHelper.modifyLabelAttr(
                            helper.addModifyLabelAttr(WICKET_ID_IMG_DOC_STORE,
                                    MarkupHelper.ATTR_SRC, imgSrc.toString()),
                            MarkupHelper.ATTR_TITLE,
                            NounEnum.JOURNAL.uiText(locale));
                } else {
                    helper.discloseLabel(WICKET_ID_IMG_DOC_STORE);
                }

            } else {
                helper.discloseLabel(WICKET_ID_IMG_DOC_STORE);
            }

            //
            helper.encloseLabel("jobticket-reopened",
                    AdjectiveEnum.REOPENED.uiText(locale),
                    JOBTICKET_SERVICE.isReopenedTicket(job));

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
         *
         * @param locale
         * @param ippKeyword
         * @param map
         * @return
         */
        private String uiIppKeywordValue(final Locale locale,
                final String ippKeyword, final IppOptionMap map) {
            return PROXYPRINT_SERVICE.localizePrinterOptValue(locale,
                    ippKeyword, map.getOptionValue(ippKeyword));
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

        /**
         *
         * @param costTotal
         * @param costPerCopy
         * @param weight
         * @param weightunit
         * @param copies
         * @param currencySymbol
         * @param sbAccTrx
         * @param locale
         *            The locale.
         */
        private void appendAccountCost(final BigDecimal costTotal,
                final BigDecimal costPerCopy, final int weight,
                final int weightunit, final int copies,
                final String currencySymbol, final StringBuilder sbAccTrx,
                final Locale locale) {

            final BigDecimal weightedCost;

            if (copies == 0) {
                weightedCost = BigDecimal.ZERO;
            } else {
                weightedCost = ACCOUNTING_SERVICE.calcWeightedAmount(costTotal,
                        copies, weight, weightunit, this.scale);
            }
            if (weightedCost.compareTo(BigDecimal.ZERO) != 0) {
                sbAccTrx.append(" ").append(currencySymbol).append("&nbsp;")
                        .append(localizedDecimal(weightedCost.negate(),
                                locale));
            }

            final BigDecimal weightCopies;

            if (costPerCopy.compareTo(BigDecimal.ZERO) == 0) {
                weightCopies = BigDecimal.ZERO;
            } else {
                weightCopies = ACCOUNTING_SERVICE
                        .calcPrintedCopies(weightedCost, costPerCopy, 0);
            }

            sbAccTrx.append("&nbsp;(").append(weightCopies).append(')');
        }

        /**
         * Gets HTML with extra cost details.
         *
         * @param helper
         *            The helper.
         * @param job
         *            The job.
         * @param sbAccTrx
         *            Builder to append on.
         * @param locale
         *            The locale.
         * @return The number of missing accounts.
         */
        private int getCostHtml(final MarkupHelper helper,
                final OutboxJobDto job, final StringBuilder sbAccTrx,
                final Locale locale) {

            int missingAccounts = 0;

            final ProxyPrintCostDto costResult = job.getCostResult();
            final BigDecimal costTotal = costResult.getCostTotal();

            final OutboxAccountTrxInfoSet trxInfoSet =
                    job.getAccountTransactions();

            final String currencySymbol = SpSession.getAppCurrencySymbol();

            sbAccTrx.append(StringUtils.replace(job.getLocaleInfo().getCost(),
                    " ", "&nbsp;"));

            if (trxInfoSet == null) {
                if (costTotal.compareTo(BigDecimal.ZERO) != 0) {
                    sbAccTrx.append(
                            " &bull; <span style=\"white-space:nowrap\">")
                            .append(MarkupHelper.HTML_IMG_ACCOUNT_PERSONAL)
                            .append("&nbsp;")
                            .append(PrintOutAdjectiveEnum.PERSONAL
                                    .uiText(locale))
                            .append("</span> ").append(currencySymbol)
                            .append("&nbsp;")
                            .append(localizedDecimal(costTotal.negate(),
                                    locale))
                            .append("&nbsp;(").append(job.getCopies())
                            .append(")");
                }
                return missingAccounts;
            }

            final int copies = job.getCopies();

            final BigDecimal costPerCopy = ACCOUNTING_SERVICE
                    .calcCostPerPrintedCopy(job.getCostTotal(), copies);

            int copiesDelegatorsImplicit = 0;
            int missingCopies = 0;

            // Create lookup of group accounts with personal invoicing.
            final Set<String> setGroupPersonal = new HashSet<>();
            for (final OutboxAccountTrxInfo trxInfo : trxInfoSet
                    .getTransactions()) {
                if (trxInfo.getExtDetails() != null) {
                    setGroupPersonal.add(trxInfo.getExtDetails());
                }
            }

            for (final OutboxAccountTrxInfo trxInfo : trxInfoSet
                    .getTransactions()) {

                final int weight = trxInfo.getWeight();

                final int weightUnit;
                if (trxInfo.getWeightUnit() == null) {
                    weightUnit = 1;
                } else {
                    weightUnit = trxInfo.getWeightUnit().intValue();
                }

                final Account account =
                        ACCOUNT_DAO.findById(trxInfo.getAccountId());

                if (account == null) {

                    sbAccTrx.append(" &bull; <span class=\"")
                            .append(MarkupHelper.CSS_TXT_ERROR).append("\">");

                    if (StringUtils.isNotBlank(trxInfo.getExtDetails())) {
                        sbAccTrx.append(trxInfo.getExtDetails());
                    }

                    appendAccountCost(costTotal, costPerCopy, weight,
                            weightUnit, copies, currencySymbol, sbAccTrx,
                            locale);

                    sbAccTrx.append("</span>");

                    missingAccounts++;
                    missingCopies += weight;

                    continue;
                }

                final AccountTypeEnum accountType =
                        AccountTypeEnum.valueOf(account.getAccountType());

                if (accountType != AccountTypeEnum.SHARED
                        && accountType != AccountTypeEnum.GROUP) {
                    continue;
                }

                copiesDelegatorsImplicit += weight;

                final Account accountParent = account.getParent();

                sbAccTrx.append(" &bull; <span style=\"white-space:nowrap\">");

                if (accountType == AccountTypeEnum.SHARED) {
                    sbAccTrx.append(MarkupHelper.HTML_IMG_ACCOUNT_SHARED);
                } else if (accountType == AccountTypeEnum.GROUP) {
                    sbAccTrx.append(MarkupHelper.HTML_IMG_ACCOUNT_GROUP);
                    if (setGroupPersonal.contains(account.getName())) {
                        sbAccTrx.append(MarkupHelper.HTML_IMG_ACCOUNT_PERSONAL);
                    }
                }
                sbAccTrx.append("&nbsp;");

                if (accountParent != null) {
                    sbAccTrx.append(accountParent.getName()).append("\\");
                }

                sbAccTrx.append(account.getName()).append("</span>");

                appendAccountCost(costTotal, costPerCopy, weight, weightUnit,
                        copies, currencySymbol, sbAccTrx, locale);
            }

            final int copiesDelegatorsIndividual =
                    copies - copiesDelegatorsImplicit - missingCopies;

            if (copiesDelegatorsIndividual > 0) {
                sbAccTrx.append(" &bull; <span style=\"white-space:nowrap\">")
                        .append(MarkupHelper.HTML_IMG_ACCOUNT_PERSONAL)
                        .append("&nbsp;");
                sbAccTrx.append(currencySymbol).append("&nbsp;")
                        .append(localizedDecimal(ACCOUNTING_SERVICE
                                .calcWeightedAmount(costTotal, copies, 1,
                                        copiesDelegatorsIndividual, this.scale)
                                .negate(), locale));
                sbAccTrx.append("&nbsp;(").append(copiesDelegatorsIndividual)
                        .append(")</span>");
            }

            return missingAccounts;
        }

        /**
         * Gets the localized string for a BigDecimal.
         *
         * @param value
         *            The {@link BigDecimal}.
         * @param locale
         *            The locale.
         * @return The localized string.
         */
        protected final String localizedDecimal(final BigDecimal value,
                final Locale locale) {
            try {
                return BigDecimalUtil.localize(value, this.currencyDecimals,
                        locale, true);
            } catch (ParseException e) {
                throw new SpException(e);
            }
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
                parms.getParameterValue(PAGE_PARM_JOBTICKETS).toBoolean();

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

        final OutboxInfoDto outboxInfo = new OutboxInfoDto();

        final Integer maxItems;
        final List<OutboxJobDto> tickets;

        if (isJobticketView) {

            maxItems = parms.getParameterValue(PAGE_PARM_MAX_ITEMS)
                    .toOptionalInteger();

            /*
             * Job Tickets mix-in.
             */
            final JobTicketService.JobTicketFilter filter =
                    new JobTicketService.JobTicketFilter();

            filter.setUserId(parms.getParameterValue(PAGE_PARM_USERKEY)
                    .toOptionalLong());
            filter.setSearchTicketId(
                    parms.getParameterValue(PAGE_PARM_JOBTICKET_ID)
                            .toOptionalString());
            filter.setPrinterGroupID(
                    parms.getParameterValue(PAGE_PARM_JOBTICKET_GROUP_ID)
                            .toOptionalLong());

            tickets = JOBTICKET_SERVICE.getTickets(filter);

        } else {

            maxItems = null;

            final DaoContext daoContext = ServiceContext.getDaoContext();

            /*
             * Lock user while getting the OutboxInfo.
             */
            final Long userKey;

            if (this.getWebAppTypeEnum(
                    parameters) == WebAppTypeEnum.PRINTSITE) {
                userKey = parms.getParameterValue(PAGE_PARM_USERKEY)
                        .toOptionalLong();
            } else {
                userKey = session.getUserDbKey();
            }

            daoContext.beginTransaction();

            final org.savapage.core.jpa.User lockedUser =
                    USER_SERVICE.lockUser(userKey);

            final OutboxInfoDto outboxInfoTmp =
                    OUTBOX_SERVICE.getOutboxJobTicketInfo(lockedUser,
                            ServiceContext.getTransactionDate());
            // unlock
            daoContext.rollback();

            tickets = new ArrayList<>();
            tickets.addAll(outboxInfoTmp.getJobs().values());
        }

        final boolean expiryAsc = BooleanUtils.isTrue(parms
                .getParameterValue(PAGE_PARM_EXPIRY_ASC).toOptionalBoolean());

        final Comparator<OutboxJobDto> comparator =
                new Comparator<OutboxJobDto>() {

                    private int compareLong(final long timeLeft,
                            final long timeRight) {

                        if (timeLeft < timeRight) {
                            return -1;
                        } else if (timeLeft > timeRight) {
                            return 1;
                        } else {
                            return 0;
                        }
                    }

                    @Override
                    public int compare(final OutboxJobDto left,
                            final OutboxJobDto right) {

                        final int cmpExpiry = compareLong(left.getExpiryTime(),
                                right.getExpiryTime());

                        // Sort #1
                        if (cmpExpiry != 0) {
                            if (expiryAsc) {
                                return cmpExpiry;
                            }
                            return -cmpExpiry;
                        }

                        // Sort #2
                        final int cmpSubmit = compareLong(left.getSubmitTime(),
                                right.getSubmitTime());

                        if (cmpSubmit != 0) {
                            return cmpSubmit;
                        }

                        // Sort #3

                        final int cmpUser;

                        if (left.getUserId() == null
                                || right.getUserId() == null) {
                            // User Web App: user is not specified.
                            cmpUser = 0;
                        } else {
                            cmpUser = compareLong(left.getUserId().longValue(),
                                    right.getUserId().longValue());
                        }

                        if (cmpUser != 0) {
                            return cmpUser;
                        }

                        // Sort #4 (optional)
                        if (left.getChunkIndex() == null
                                || right.getChunkIndex() == null) {
                            return cmpUser;
                        }

                        return left.getChunkIndex()
                                .compareTo(right.getChunkIndex());
                    }
                };

        Collections.sort(tickets, comparator);

        int iItems = 0;

        final Iterator<OutboxJobDto> iterTickets = tickets.iterator();
        OutboxJobDto dtoLast = null;

        // Add to limit.
        while (iterTickets.hasNext()) {
            dtoLast = iterTickets.next();
            outboxInfo.addJob(dtoLast.getFile(), dtoLast);
            if (maxItems != null && maxItems.intValue() > 0
                    && ++iItems >= maxItems.intValue()) {
                break;
            }
        }

        // Add extra to preserve user/submit-time unit of work.
        if (dtoLast != null) {
            while (iterTickets.hasNext()) {
                final OutboxJobDto dto = iterTickets.next();
                if (!dto.getUserId().equals(dtoLast.getUserId())
                        || dto.getExpiryTime() != dtoLast.getExpiryTime()
                        || dto.getSubmitTime() != dtoLast.getSubmitTime()) {
                    break;
                }
                outboxInfo.addJob(dto.getFile(), dto);
            }
        }

        //
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

    /**
     *
     * @param msecStateDuration
     *            Duration in milliseconds.
     * @return The formatted duration.
     */
    private static String formatDuration(final long msecStateDuration) {

        final String durationFormat;

        if (msecStateDuration < DateUtil.DURATION_MSEC_HOUR) {
            durationFormat = "m:ss";
        } else {
            durationFormat = "H:mm:ss";
        }
        return DurationFormatUtils.formatDuration(msecStateDuration,
                durationFormat, true);
    }
}
