/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
package org.savapage.server.pages;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.DocLogDao;
import org.savapage.core.dao.enums.PrintInDeniedReasonEnum;
import org.savapage.core.dao.enums.PrintModeEnum;
import org.savapage.core.dto.JobTicketTagDto;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.i18n.PrintOutAdjectiveEnum;
import org.savapage.core.i18n.PrintOutNounEnum;
import org.savapage.core.i18n.PrintOutVerbEnum;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.ipp.helpers.IppOptionMap;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.jpa.User;
import org.savapage.core.print.proxy.TicketJobSheetDto;
import org.savapage.core.services.AccountingService;
import org.savapage.core.services.JobTicketService;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.JobTicketSupplierData;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.core.util.CurrencyUtil;
import org.savapage.server.WebApp;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.helpers.SparklineHtml;
import org.savapage.server.session.SpSession;
import org.savapage.server.webapp.WebAppTypeEnum;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class DocLogItemPanel extends Panel {

    private static final AccountingService ACCOUNTING_SERVICE =
            ServiceContext.getServiceFactory().getAccountingService();

    private static final JobTicketService JOBTICKET_SERVICE =
            ServiceContext.getServiceFactory().getJobTicketService();

    private static final ProxyPrintService PROXYPRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();

    private final NumberFormat fmNumber =
            NumberFormat.getInstance(getSession().getLocale());

    private final DateFormat dfShortDateTime = DateFormat.getDateTimeInstance(
            DateFormat.SHORT, DateFormat.SHORT, getSession().getLocale());

    private final DateFormat dfShortTime = DateFormat
            .getTimeInstance(DateFormat.SHORT, getSession().getLocale());

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Number of decimals for decimal scaling.
     */
    final int scale;

    /**
     * Number of currency decimals to display.
     */
    private final int currencyDecimals;

    //
    private final boolean showDocLogCost;

    /**
     *
     * @param id
     * @param model
     */
    public DocLogItemPanel(String id, IModel<DocLogItem> model,
            final boolean showFinancialData) {

        super(id, model);

        this.scale = ConfigManager.getFinancialDecimalsInDatabase();
        this.currencyDecimals = ConfigManager.getUserBalanceDecimals();
        this.showDocLogCost = showFinancialData;
    }

    /**
     *
     * @param model
     */
    public void populate(final IModel<DocLogItem> model) {

        final DocLogItem obj = model.getObject();
        final Locale locale = getLocale();

        String cssClass = null;

        final Map<String, String> mapVisible = new HashMap<>();

        for (final String attr : new String[] { "user-name", "title",
                "log-comment", "printoutMode", "signature", "destination",
                "letterhead", "author", "subject", "keywords", "drm", "userpw",
                "ownerpw", "duplex", "simplex", "color", "grayscale",
                "papersize", "media-source", "output-bin", "jog-offset",
                "cost-currency", "cost", "job-id", "job-state",
                "job-completed-date", "print-in-denied-reason-hyphen",
                "print-in-denied-reason", "collate", "ecoPrint",
                "removeGraphics", "pageRotate180", "punch", "staple", "fold",
                "booklet", "jobticket-media", "jobticket-copy",
                "jobticket-finishing-ext", "jobticket-custom-ext",
                "landscape" }) {
            mapVisible.put(attr, null);
        }

        final MarkupHelper helper = new MarkupHelper(this);

        //
        final boolean isExtSupplier = obj.getExtSupplier() != null;

        if (isExtSupplier) {
            final ExtSupplierStatusPanel panel =
                    new ExtSupplierStatusPanel("extSupplierPanel");
            panel.populate(obj.getExtSupplier(), obj.getExtSupplierStatus(),
                    null);
            add(panel);
        } else {
            helper.discloseLabel("extSupplierPanel");
        }

        //
        String cssJobState = null;

        //
        if (StringUtils.isNotBlank(obj.getComment())) {
            mapVisible.put("log-comment", obj.getComment());
        }

        // Account Transactions
        final StringBuilder sbAccTrx = new StringBuilder();

        if (obj.getTransactions().isEmpty()) {
            helper.discloseLabel("account-trx");
            helper.discloseLabel("btn-account-trx-info");
        } else {
            final String currencySymbol = CurrencyUtil
                    .getCurrencySymbol(obj.getCurrencyCode(), getLocale());

            int totWeightDelegators = 0;
            int copiesDelegatorsImplicit = 0;

            for (final AccountTrx trx : obj.getTransactions()) {

                final Account account = trx.getAccount();

                final AccountTypeEnum accountType =
                        AccountTypeEnum.valueOf(account.getAccountType());

                if (accountType != AccountTypeEnum.SHARED
                        && accountType != AccountTypeEnum.GROUP) {

                    final boolean isRefund =
                            trx.getAmount().compareTo(BigDecimal.ZERO) == 1;
                    if (!isRefund) {
                        totWeightDelegators +=
                                trx.getTransactionWeight().intValue();
                    }
                    continue;
                }

                copiesDelegatorsImplicit +=
                        trx.getTransactionWeight().intValue();

                final Account accountParent = account.getParent();

                sbAccTrx.append(" &bull; ");

                if (accountParent != null) {
                    sbAccTrx.append(accountParent.getName()).append('\\');
                }

                sbAccTrx.append(account.getName());

                if (trx.getAmount().compareTo(BigDecimal.ZERO) != 0) {
                    sbAccTrx.append(" ").append(currencySymbol).append("&nbsp;")
                            .append(localizedDecimal(trx.getAmount()));
                }

                sbAccTrx.append("&nbsp;(").append(trx.getTransactionWeight())
                        .append(')');
            }

            final int copiesDelegatorsIndividual =
                    obj.getCopies() - copiesDelegatorsImplicit;

            if (copiesDelegatorsIndividual > 0 && obj.getCopies() > 1) {

                final BigDecimal amount = ACCOUNTING_SERVICE.calcWeightedAmount(
                        obj.getCost(), obj.getCopies(),
                        copiesDelegatorsIndividual, this.scale);

                if (amount.compareTo(BigDecimal.ZERO) != 0) {
                    sbAccTrx.append(" &bull; ").append(currencySymbol)
                            .append("&nbsp;")
                            .append(localizedDecimal(amount.negate()));
                    sbAccTrx.append("&nbsp;(")
                            .append(copiesDelegatorsIndividual).append(")");
                }
            }

            if (isExtSupplier && totWeightDelegators > 0) {
                sbAccTrx.append(" &bull; ");
                sbAccTrx.append(localized("delegators")).append(" (")
                        .append(totWeightDelegators).append(")");
            }

            // When no text accumulated, this must be a charge to personal
            // account only.
            if (sbAccTrx.length() == 0
                    && obj.getCost().compareTo(BigDecimal.ZERO) != 0) {
                sbAccTrx.append(" &bull; ")
                        .append(PrintOutAdjectiveEnum.PERSONAL.uiText(locale))
                        .append(" ").append(currencySymbol).append("&nbsp;")
                        .append(localizedDecimal(obj.getCost().negate()))
                        .append("&nbsp;(").append(obj.getCopies()).append(")");
            }

            add(new Label("account-trx", sbAccTrx.toString())
                    .setEscapeModelStrings(false));

            helper.encloseLabel("account-trx-refund",
                    NounEnum.REFUND.uiText(locale), obj.isRefunded());

            final WebAppTypeEnum webAppType = SpSession.get().getWebAppType();

            if (webAppType == WebAppTypeEnum.JOBTICKETS
                    || webAppType == WebAppTypeEnum.ADMIN
                    || webAppType == WebAppTypeEnum.USER) {

                Label labelBtn = helper.encloseLabel("btn-account-trx-info",
                        "&nbsp;", true);
                labelBtn.setEscapeModelStrings(false);

                MarkupHelper.modifyLabelAttr(labelBtn,
                        MarkupHelper.ATTR_DATA_SAVAPAGE,
                        obj.getDocLogId().toString());

                MarkupHelper.modifyLabelAttr(labelBtn, MarkupHelper.ATTR_TITLE,
                        NounEnum.TRANSACTION.uiText(getLocale(), true));

                if (webAppType == WebAppTypeEnum.JOBTICKETS && !obj.isRefunded()
                        && obj.getCost().compareTo(BigDecimal.ZERO) != 0) {

                    labelBtn = helper.encloseLabel("btn-account-trx-refund",
                            "&nbsp;", true);
                    labelBtn.setEscapeModelStrings(false);

                    MarkupHelper.modifyLabelAttr(labelBtn,
                            MarkupHelper.ATTR_DATA_SAVAPAGE,
                            obj.getDocLogId().toString());

                    MarkupHelper.modifyLabelAttr(labelBtn,
                            MarkupHelper.ATTR_TITLE,
                            HtmlButtonEnum.REFUND.uiText(getLocale(), true));

                } else {
                    helper.discloseLabel("btn-account-trx-refund");
                }

            } else {
                helper.discloseLabel("btn-account-trx-info");
            }
        }

        String pieData = null;
        String pieSliceColors = null;

        //
        if (obj.getDocType() == DocLogDao.Type.IN) {

            cssClass = MarkupHelper.CSS_PRINT_IN_QUEUE;

            if (obj.getPaperSize() != null) {
                mapVisible.put("papersize", obj.getPaperSize().toUpperCase());
            }

            if (obj.getDrmRestricted()) {
                mapVisible.put("drm", "DRM");
            }

            if (!obj.getPrintInPrinted()) {

                mapVisible.put("print-in-denied-reason-hyphen", "-");

                PrintInDeniedReasonEnum deniedReason =
                        obj.getPrintInDeniedReason();

                String key = "print-in-denied-reason-unknown";
                if (deniedReason != null
                        && deniedReason.equals(PrintInDeniedReasonEnum.DRM)) {
                    key = "print-in-denied-reason-drm";
                }

                mapVisible.put("print-in-denied-reason", localized(key));

            } else {
                pieData = String.valueOf(obj.getTotalPages());
                pieSliceColors =
                        SparklineHtml.arrayAttr(SparklineHtml.COLOR_QUEUE);
            }

        } else {

            /*
             * Not for now...
             */
            // mapVisible.put("signature", obj.getSignature());

            if (obj.getLetterhead()) {
                mapVisible.put("letterhead", "LH");
            }

            if (obj.getDocType() == DocLogDao.Type.PDF) {

                cssClass = MarkupHelper.CSS_PRINT_OUT_PDF;

                mapVisible.put("destination", obj.getDestination());

                mapVisible.put("author", obj.getAuthor());
                mapVisible.put("subject", obj.getSubject());
                mapVisible.put("keywords", obj.getKeywords());

                if (obj.getDrmRestricted()) {
                    mapVisible.put("drm", "DRM");
                }
                if (obj.getUserPw()) {
                    mapVisible.put("userpw", "U");
                }
                if (obj.getOwnerPw()) {
                    mapVisible.put("ownerpw", "O");
                }

                pieData = String.valueOf(obj.getTotalPages());
                pieSliceColors =
                        SparklineHtml.arrayAttr(SparklineHtml.COLOR_PDF);

            } else {

                final String ticketNumber;
                final String ticketOperator;
                final String ticketTag;

                if (obj.getPrintMode() == PrintModeEnum.TICKET
                        || obj.getPrintMode() == PrintModeEnum.TICKET_C
                        || obj.getPrintMode() == PrintModeEnum.TICKET_E) {

                    ticketNumber = obj.getExtId();

                    final JobTicketTagDto tag =
                            JOBTICKET_SERVICE.getTicketNumberTag(ticketNumber);

                    if (tag == null) {
                        ticketTag = null;
                    } else {
                        ticketTag = String.format("• %s", tag.getWord());
                    }

                    // Just in case.
                    if (obj.getExtData() == null) {
                        ticketOperator = null;
                    } else {

                        final JobTicketSupplierData extData =
                                JobTicketSupplierData
                                        .createFromData(obj.getExtData());
                        if (extData == null || extData.getOperator() == null) {
                            ticketOperator = null;
                        } else {
                            ticketOperator = String.format("(%s)",
                                    extData.getOperator());
                        }
                    }

                    this.addJobSheetImg(obj.getIppOptionMap(), helper);

                } else {
                    ticketNumber = null;
                    ticketOperator = null;
                    ticketTag = null;
                    helper.discloseLabel("img-job-sheet");
                }

                mapVisible.put(
                        "printoutMode", String
                                .format("%s %s %s %s",
                                        obj.getPrintMode().uiText(getLocale()),
                                        StringUtils.defaultString(ticketNumber),
                                        StringUtils
                                                .defaultString(ticketOperator),
                                        StringUtils.defaultString(ticketTag))
                                .trim());

                cssClass = MarkupHelper.CSS_PRINT_OUT_PRINTER;

                if (obj.getDuplex()) {
                    mapVisible.put("duplex",
                            helper.localized(PrintOutNounEnum.DUPLEX));
                } else {
                    mapVisible.put("simplex",
                            helper.localized(PrintOutNounEnum.SIMPLEX));
                }

                if (obj.getGrayscale()) {
                    mapVisible.put("grayscale",
                            helper.localized(PrintOutNounEnum.GRAYSCALE));
                } else {
                    mapVisible.put("color",
                            helper.localized(PrintOutNounEnum.COLOR));
                }

                if (obj.isPageRotate180()) {
                    mapVisible.put("pageRotate180",
                            PROXYPRINT_SERVICE.localizePrinterOpt(locale,
                                    IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_INT_PAGE_ROTATE180));
                }
                if (obj.isFinishingPunch()) {
                    mapVisible.put("punch",
                            uiIppKeywordValue(locale,
                                    IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_FINISHINGS_PUNCH,
                                    obj));
                }
                if (obj.isFinishingStaple()) {
                    mapVisible.put("staple",
                            uiIppKeywordValue(locale,
                                    IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_FINISHINGS_STAPLE,
                                    obj));
                }
                if (obj.isFinishingFold()) {
                    mapVisible.put("fold",
                            String.format("%s %s",
                                    helper.localized(PrintOutVerbEnum.FOLD),
                                    uiIppKeywordValue(locale,
                                            IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_FINISHINGS_FOLD,
                                            obj)));
                }
                if (obj.isFinishingBooklet()) {
                    mapVisible.put("booklet",
                            helper.localized(PrintOutNounEnum.BOOKLET));
                }

                mapVisible.put("papersize", obj.getPaperSize().toUpperCase());

                //
                final String mediaSource = obj.getIppOptionMap().getOptionValue(
                        IppDictJobTemplateAttr.ATTR_MEDIA_SOURCE);

                String ippKeywordWlk;

                if (mediaSource != null) {
                    mapVisible.put("media-source",
                            PROXYPRINT_SERVICE.localizePrinterOptValue(locale,
                                    IppDictJobTemplateAttr.ATTR_MEDIA_SOURCE,
                                    mediaSource));

                    ippKeywordWlk = IppDictJobTemplateAttr.ATTR_OUTPUT_BIN;

                    final String outputBin =
                            obj.getIppOptionMap().getOptionValue(ippKeywordWlk);

                    if (outputBin != null) {
                        mapVisible.put("output-bin",
                                PROXYPRINT_SERVICE.localizePrinterOptValue(
                                        locale, ippKeywordWlk, outputBin));

                        ippKeywordWlk =
                                IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_FINISHINGS_JOG_OFFSET;

                        final String jogOfset = obj.getIppOptionMap()
                                .getOptionValue(ippKeywordWlk);

                        if (jogOfset != null) {
                            mapVisible.put("jog-offset",
                                    PROXYPRINT_SERVICE.localizePrinterOptValue(
                                            locale, ippKeywordWlk, jogOfset));
                        }

                    }

                }

                //
                if (obj.getJobId() != null
                        && !obj.getJobId().equals(Integer.valueOf(0))) {
                    mapVisible.put("job-id", obj.getJobId().toString());
                }

                if (this.showDocLogCost
                        && obj.getCost().compareTo(BigDecimal.ZERO) != 0) {
                    mapVisible.put("cost-currency",
                            CurrencyUtil.getCurrencySymbol(
                                    obj.getCurrencyCode(),
                                    getSession().getLocale()));
                    mapVisible.put("cost", localizedDecimal(obj.getCost()));
                }

                cssJobState = MarkupHelper.getCssTxtClass(obj.getJobState());

                if (obj.getJobState() != null) {
                    mapVisible.put("job-state",
                            obj.getJobState().uiText(locale));
                    if (obj.getCompletedDate() != null) {
                        mapVisible.put("job-completed-date",
                                localizedShortTime(obj.getCompletedDate()));
                    }
                }

                pieData = SparklineHtml.valueString(
                        String.valueOf(obj.getTotalSheets()),
                        String.valueOf(obj.getTotalPages() * obj.getCopies()
                                - obj.getTotalSheets()));
                pieSliceColors = SparklineHtml.arrayAttr(
                        SparklineHtml.COLOR_PRINTER, SparklineHtml.COLOR_SHEET);
            }

        }

        if (pieData == null) {
            helper.discloseLabel("document-pie");
        } else {
            MarkupHelper.modifyLabelAttr(
                    helper.addModifyLabelAttr("document-pie", pieData,
                            SparklineHtml.ATTR_SLICE_COLORS, pieSliceColors),
                    MarkupHelper.ATTR_CLASS, SparklineHtml.CSS_CLASS_DOCLOG);
        }
        //
        Label labelWlk = new Label("header");
        labelWlk.add(new AttributeModifier("class", cssClass));

        add(labelWlk);

        //
        if (!obj.getUserId().equals(User.ERASED_USER_ID)) {
            mapVisible.put("user-name", obj.getUserId());
        }

        //
        add(new Label("dateCreated",
                localizedShortDateTime(obj.getCreatedDate())));

        //
        String title = null;
        if (ConfigManager.instance()
                .isConfigValue(Key.WEBAPP_DOCLOG_SHOW_DOC_TITLE)) {
            title = obj.getTitle();
        }
        mapVisible.put("title", title);

        /*
         * Totals
         */
        final StringBuilder totals = new StringBuilder();

        //
        int total = obj.getTotalPages();
        int copies = obj.getCopies();

        //
        totals.append(localizedNumber(total));
        totals.append(" ")
                .append(helper.localized(PrintOutNounEnum.PAGE, total > 1));

        // n-up
        if (obj.getNumberUp() != null && obj.getNumberUp().intValue() > 1) {
            totals.append(", ").append(localized("n-up", obj.getNumberUp()));
        }

        //
        if (copies > 1) {

            totals.append(", ").append(copies).append(" ")
                    .append(helper.localized(PrintOutNounEnum.COPY, true));
        }

        //
        total = obj.getTotalSheets();
        if (total > 0) {
            totals.append(" (").append(total).append(" ")
                    .append(helper.localized(PrintOutNounEnum.SHEET, total > 1))
                    .append(")");
        }

        //
        if (obj.getHumanReadableByteCount() != null) {
            totals.append(", ").append(obj.getHumanReadableByteCount());
        }

        add(new Label("totals", totals.toString()));

        //
        if (copies > 1 && obj.getCollateCopies() != null
                && obj.getCollateCopies()) {
            mapVisible.put("collate",
                    helper.localized(PrintOutVerbEnum.COLLATE));
        }

        if (obj.getEcoPrint() != null && obj.getEcoPrint()) {
            mapVisible.put("ecoPrint", "EcoPrint");
        }

        if (obj.getRemoveGraphics() != null && obj.getRemoveGraphics()) {
            mapVisible.put("removeGraphics", localized("graphics-removed"));
        }

        mapVisible.put("jobticket-media",
                PROXYPRINT_SERVICE.getJobTicketOptionsUiText(locale,
                        IppDictJobTemplateAttr.JOBTICKET_ATTR_MEDIA,
                        obj.getIppOptionMap()));

        mapVisible.put("jobticket-copy",
                PROXYPRINT_SERVICE.getJobTicketOptionsUiText(locale,
                        IppDictJobTemplateAttr.JOBTICKET_ATTR_COPY,
                        obj.getIppOptionMap()));

        mapVisible.put("jobticket-finishings-ext",
                PROXYPRINT_SERVICE.getJobTicketOptionsUiText(locale,
                        IppDictJobTemplateAttr.JOBTICKET_ATTR_FINISHINGS_EXT,
                        obj.getIppOptionMap()));

        mapVisible.put("jobticket-custom-ext", PROXYPRINT_SERVICE
                .getJobTicketOptionsExtHtml(getLocale(), obj.getIppOptions()));

        //
        if (obj.getIppOptionMap() != null
                && obj.getIppOptionMap().isLandscapeJob()) {
            mapVisible.put("landscape",
                    helper.localized(PrintOutNounEnum.LANDSCAPE));
        }

        /*
         * Hide/Show
         */
        for (final Map.Entry<String, String> entry : mapVisible.entrySet()) {

            if (entry.getValue() == null) {
                entry.setValue("");
            }

            String cssClassWlk = null;

            if (entry.getKey().equals("job-state")) {
                cssClassWlk = cssJobState;
            }

            addVisible(StringUtils.isNotBlank(entry.getValue()), entry.getKey(),
                    entry.getValue(), cssClassWlk);
        }
    }

    /**
     *
     * @param locale
     * @param ippKeyword
     * @param obj
     * @return
     */
    private String uiIppKeywordValue(final Locale locale,
            final String ippKeyword, final DocLogItem obj) {
        return PROXYPRINT_SERVICE.localizePrinterOptValue(locale, ippKeyword,
                obj.getIppOptionMap().getOptionValue(ippKeyword));
    }

    /**
     *
     * @param optMap
     * @param helper
     */
    private void addJobSheetImg(final IppOptionMap optMap,
            final MarkupHelper helper) {
        //
        final TicketJobSheetDto jobSheet =
                JOBTICKET_SERVICE.getTicketJobSheet(optMap);

        if (jobSheet != null && jobSheet.isEnabled()) {
            final StringBuilder imgSrc = new StringBuilder();
            imgSrc.append(WebApp.PATH_IMAGES).append('/');
            imgSrc.append("copy-jobticket-128x128.png");
            helper.addModifyLabelAttr("img-job-sheet", MarkupHelper.ATTR_SRC,
                    imgSrc.toString());
        } else {
            helper.discloseLabel("img-job-sheet");
        }
    }

    /**
     *
     * @param isVisible
     * @param id
     * @param val
     */
    private final void addVisible(final boolean isVisible, final String id,
            final String val, final String cssClass) {

        Label label = new Label(id, val) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return isVisible;
            }
        };

        if (isVisible && cssClass != null) {
            label.add(new AttributeModifier("class", cssClass));
        }

        add(label);
    }

    /**
     * Gets the localized string for a BigDecimal.
     *
     * @param value
     *            The {@link BigDecimal}.
     * @return The localized string.
     */
    protected final String localizedDecimal(final BigDecimal value) {
        return BigDecimalUtil.localizeUc(value, this.currencyDecimals,
                getSession().getLocale(), true);
    }

    /**
     * Gets as localized string of a Number. The locale of the current session
     * is used.
     *
     * @param number
     *            The number.
     * @return The localized string.
     */
    protected final String localizedNumber(final long number) {
        return fmNumber.format(number);
    }

    /**
     * Gets as localized short date/time string of a Date. The locale of the
     * current session is used.
     *
     * @param date
     *            The date.
     * @return The localized short date/time string.
     */
    protected final String localizedShortDateTime(final Date date) {
        return dfShortDateTime.format(date);
    }

    /**
     * Gets as localized short time string of a Date. The locale of the current
     * session is used.
     *
     * @param date
     *            The date.
     * @return The localized short time string.
     */
    protected final String localizedShortTime(final Date date) {
        return dfShortTime.format(date);
    }

    /**
     * Gives the localized string for a key.
     *
     * @param key
     *            The key from the XML resource file
     * @return The localized string.
     */
    protected final String localized(final String key) {
        return getLocalizer().getString(key, this);
    }

    /**
     * Localizes and format a string with placeholder arguments.
     *
     * @param key
     *            The key from the XML resource file
     * @param objects
     *            The values to fill the placeholders
     * @return The localized string.
     */
    protected final String localized(final String key,
            final Object... objects) {
        return MessageFormat.format(getLocalizer().getString(key, this),
                objects);
    }

}
