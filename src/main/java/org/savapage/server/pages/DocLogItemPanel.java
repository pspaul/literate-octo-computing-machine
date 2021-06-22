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
package org.savapage.server.pages;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.config.WebAppTypeEnum;
import org.savapage.core.dao.DocLogDao;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.ACLPermissionEnum;
import org.savapage.core.dao.enums.PrintInDeniedReasonEnum;
import org.savapage.core.dao.enums.PrintModeEnum;
import org.savapage.core.dao.enums.ReservedIppQueueEnum;
import org.savapage.core.doc.DocContent;
import org.savapage.core.i18n.AdjectiveEnum;
import org.savapage.core.i18n.JobTicketNounEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.i18n.PrepositionEnum;
import org.savapage.core.i18n.PrintOutAdjectiveEnum;
import org.savapage.core.i18n.PrintOutNounEnum;
import org.savapage.core.i18n.PrintOutVerbEnum;
import org.savapage.core.ipp.IppJobStateEnum;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.ipp.helpers.IppOptionMap;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.jpa.PrintOut;
import org.savapage.core.print.proxy.TicketJobSheetDto;
import org.savapage.core.services.AccountingService;
import org.savapage.core.services.JobTicketService;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.MailPrintData;
import org.savapage.core.services.helpers.PrintSupplierData;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.core.util.CurrencyUtil;
import org.savapage.server.WebApp;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.helpers.SparklineHtml;
import org.savapage.server.session.SpSession;

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

    /** */
    private static final long serialVersionUID = 1L;

    /** */
    private static final String WID_BTN_GROUP = "btn-group";
    /** */
    private static final String WID_BTN_ACCOUNT_TRX_INFO =
            "btn-account-trx-info";
    /** */
    private static final String WID_BTN_ACCOUNT_TRX_REFUND =
            "btn-account-trx-refund";
    /** */
    private static final String WID_BTN_PRINT_OUT_REVERSE =
            "btn-printout-reverse";

    /** */
    private static final String WID_BTN_ARCHIVE_DOWNLOAD =
            "btn-doclog-docstore-archive-download";
    /** */
    private static final String WID_BTN_JOURNAL_DOWNLOAD =
            "btn-doclog-docstore-journal-download";

    /** */
    private static final String WID_BTN_DOCLOG_STORE_DELETE =
            "btn-doclog-store-delete";

    /** */
    private static final String WID_BTN_INBOX_PRINTIN_RESTORE_ADD =
            "btn-inbox-printin-restore-add";
    /** */
    private static final String WID_BTN_INBOX_PRINTIN_RESTORE_REPLACE =
            "btn-inbox-printin-restore-replace";

    /** */
    private static final String WID_BTN_TICKET_REOPEN = "btn-ticket-reopen";
    /** */
    private static final String WID_IMG_ARCHIVE = "img-archive";
    /** */
    private static final String WID_IMG_JOURNAL = "img-journal";
    /** */
    private static final String WID_IMG_COPY_JOB = "img-copy-job";
    /** */
    private static final String WID_IMG_JOB_SHEET = "img-job-sheet";

    /**
     * Number of currency decimals to display.
     */
    private final int currencyDecimals;

    /**
     * If {@code true}, financial data is shown.
     */
    private final boolean showDocLogCost;

    /**
     * If {@code true}, ticket reopen button is enabled.
     */
    private final boolean showTicketReopen;

    /**
     * If {@code true}, session user has permission to edit accounts:
     * {@link ACLOidEnum#A_ACCOUNTS}.
     */
    private final boolean isAccountsEditor;

    /**
     * User Web App Privilege bitmask for {@link ACLOidEnum#U_QUEUE_JOURNAL}. If
     * {@code null}, all privileges are granted.
     */
    private final Integer userQueueJournalPrivilege;

    /** */
    private static final String[] WICKET_IDS = new String[] { "prompt-user",
            "user-name", "title", "log-comment", "printoutMode",
            "prompt-signature", "signature", "prompt-origin", "origin",
            "prompt-destination", "prompt-email-ticket", "email-ticket",
            "prompt-email-ticket-completed", "email-ticket-completed",
            "prompt-email-ticket-failed", "email-ticket-failed",
            "prompt-email-ticket-pending", "email-ticket-pending",
            "destination", "letterhead", "author", "subject", "keywords",
            "print-in-label", "pdfpgp", "userpw", "ownerpw", "duplex",
            "simplex", "color", "grayscale", "papersize", "media-source",
            "output-bin", "jog-offset", "cost-currency", "cost", "job-id",
            "job-state", "job-completed-date", "print-in-denied-reason-hyphen",
            "print-in-denied-reason", "collate", "ecoPrint", "removeGraphics",
            "pageRotate180", "punch", "staple", "fold", "booklet",
            "jobcopy-options", "jobticket-tag-plain", "jobticket-media",
            "jobticket-copy", "jobticket-finishing-ext", "jobticket-custom-ext",
            "landscape", "scaled" };

    /**
     *
     * @param id
     *            Wicket ID.
     * @param model
     *            The model object.
     * @param showFinancialData
     *            If {@code true}, financial data is shown.
     * @param isTicketReopenEnabled
     *            If {@code true}, ticket reopen button is enabled.
     * @param accountsEditor
     *            If {@code true}, session user has permission to edit accounts:
     *            {@link ACLOidEnum#A_ACCOUNTS}.
     * @param userQueueJournalPriv
     *            User Web App Privilege bitmask for
     *            {@link ACLOidEnum#U_QUEUE_JOURNAL}. If {@code null}, all
     *            privileges are granted.
     */
    public DocLogItemPanel(final String id, final IModel<DocLogItem> model,
            final boolean showFinancialData,
            final boolean isTicketReopenEnabled, final boolean accountsEditor,
            final Integer userQueueJournalPriv) {

        super(id, model);

        this.currencyDecimals = ConfigManager.getUserBalanceDecimals();
        this.showDocLogCost = showFinancialData;
        this.showTicketReopen = isTicketReopenEnabled;
        this.isAccountsEditor = accountsEditor;
        this.userQueueJournalPrivilege = userQueueJournalPriv;
    }

    /**
     *
     * @param mapVisible
     * @param helper
     * @param obj
     * @param extData
     * @param locale
     */
    private void populateMailPrintTicket(final Map<String, String> mapVisible,
            final MarkupHelper helper, final DocLogItem obj,
            final MailPrintData extData, final Locale locale) {

        mapVisible.put("prompt-origin",
                PrepositionEnum.FROM_LOCATION.uiText(getLocale()));
        mapVisible.put("origin", extData.getFromAddress());

        if (StringUtils.isNotBlank(obj.getExtId())) {
            mapVisible.put("prompt-email-ticket",
                    JobTicketNounEnum.TICKET.uiText(getLocale()));
            mapVisible.put("email-ticket", obj.getExtId());
        }

        if (obj.getPrintOutOfDocIn() != null) {

            final StringBuilder completed = new StringBuilder();
            final StringBuilder failed = new StringBuilder();
            final StringBuilder pending = new StringBuilder();

            for (final PrintOut prt : obj.getPrintOutOfDocIn()) {
                final IppJobStateEnum state =
                        IppJobStateEnum.asEnum(prt.getCupsJobState());
                final StringBuilder wlk;
                if (state.isFailure()) {
                    wlk = failed;
                } else if (state.isFinished()) {
                    wlk = completed;
                } else {
                    wlk = pending;
                }
                wlk.append("#").append(prt.getCupsJobId());
                final BigDecimal cost =
                        prt.getDocOut().getDocLog().getCostOriginal();
                if (cost != null) {
                    wlk.append(" (");
                    try {
                        wlk.append(BigDecimalUtil.localize(cost, 2, locale,
                                false, true));
                    } catch (ParseException e) {
                        wlk.append("??");
                    }
                    wlk.append(")");
                }
                wlk.append(" ");
            }
            if (completed.length() > 0) {
                mapVisible.put("prompt-email-ticket-completed",
                        IppJobStateEnum.IPP_JOB_COMPLETED.uiText(getLocale()));
                mapVisible.put("email-ticket-completed",
                        completed.toString().trim());
            }
            if (failed.length() > 0) {
                mapVisible.put("prompt-email-ticket-failed",
                        NounEnum.ERROR.uiText(getLocale()));
                mapVisible.put("email-ticket-failed", failed.toString().trim());
            }
            if (pending.length() > 0) {
                mapVisible.put("prompt-email-ticket-pending",
                        IppJobStateEnum.IPP_JOB_PENDING.uiText(getLocale()));
                mapVisible.put("email-ticket-pending",
                        pending.toString().trim());
            }
        }
    }

    /**
     *
     * @param mapVisible
     * @param helper
     *            HTML helper
     * @param obj
     *            Item.
     * @param locale
     *            Locale.
     */
    private void populateOrigin(final Map<String, String> mapVisible,
            final MarkupHelper helper, final DocLogItem obj,
            final Locale locale) {

        if (obj.getPrintInReservedQueue() == ReservedIppQueueEnum.MAILPRINT) {
            final MailPrintData extData =
                    MailPrintData.createFromData(obj.getExtData());
            if (extData != null && extData.getFromAddress() != null) {
                this.populateMailPrintTicket(mapVisible, helper, obj, extData,
                        locale);
            }
        } else if (StringUtils.isNotBlank(obj.getDocInOriginatorIp())) {
            mapVisible.put("prompt-origin",
                    NounEnum.CLIENT.uiText(getLocale()));
            mapVisible.put("origin", obj.getDocInOriginatorIp());
        }

    }

    /**
     *
     * @param helper
     *            HTML helper
     * @param obj
     *            Item.
     * @param locale
     *            Locale.
     */
    private void populateExtSupplier(final MarkupHelper helper,
            final DocLogItem obj, final Locale locale) {

        if (obj.isExtSupplierPresent()) {
            final ExtSupplierStatusPanel panel =
                    new ExtSupplierStatusPanel("extSupplierPanel");
            panel.populate(obj.getExtSupplier(), obj.getExtSupplierStatus(),
                    null, obj.getExtPrintManager(), locale);
            add(panel);
        } else {
            helper.discloseLabel("extSupplierPanel");
        }
    }

    /**
     *
     * @param helper
     *            HTML helper
     * @param obj
     *            Item.
     * @param locale
     *            Locale.
     */
    private void populateTotals(final MarkupHelper helper, final DocLogItem obj,
            final Locale locale) {

        int total = obj.getTotalPages();
        int copies = obj.getCopies();

        final StringBuilder totals = new StringBuilder();

        totals.append(localizedNumber(total));
        totals.append(" ")
                .append(helper.localized(PrintOutNounEnum.PAGE, total > 1));

        if (obj.getNumberUp() != null && obj.getNumberUp().intValue() > 1) {
            totals.append(" &bull; ").append(PrintOutNounEnum.N_UP
                    .uiText(locale, obj.getNumberUp().toString()));
        }

        if (copies > 1) {
            totals.append(" &bull; ").append(copies).append(" ")
                    .append(helper.localized(PrintOutNounEnum.COPY, true));
        }

        total = obj.getTotalSheets();
        if (total > 0) {
            totals.append(" (").append(total).append(" ")
                    .append(helper.localized(PrintOutNounEnum.SHEET, total > 1))
                    .append(")");
        }

        if (obj.getHumanReadableByteCount() != null) {
            totals.append(" &bull; ").append(obj.getHumanReadableByteCount());
        }

        final Label labelWlk = new Label("totals", totals.toString());
        labelWlk.setEscapeModelStrings(false);
        add(labelWlk);
    }

    /**
     * Populate accounts transactions.
     *
     * @param webAppType
     * @param mapVisible
     * @param helper
     *            HTML helper
     * @param obj
     *            Item.
     * @param locale
     *            Locale.
     * @return number of buttons added.
     */
    private int populateAccountTrx(final WebAppTypeEnum webAppType,
            final Map<String, String> mapVisible, final MarkupHelper helper,
            final DocLogItem obj, final Locale locale) {

        final StringBuilder sbAccTrx = new StringBuilder();

        int countButtons = 0;

        if (obj.getTransactions().isEmpty()) {
            helper.discloseLabel("account-trx");
            helper.discloseLabel(WID_BTN_ACCOUNT_TRX_INFO);
            return countButtons;
        }
        final String currencySymbol = CurrencyUtil
                .getCurrencySymbol(obj.getCurrencyCode(), getLocale());

        BigDecimal totCopiesDelegators = BigDecimal.ZERO;
        BigDecimal totCopiesPersonal = BigDecimal.ZERO;

        BigDecimal totCopiesDelegatorsRefund = BigDecimal.ZERO;
        BigDecimal totCopiesPersonalRefund = BigDecimal.ZERO;

        BigDecimal amountCopiesDelegators = BigDecimal.ZERO;
        BigDecimal amountCopiesPersonal = BigDecimal.ZERO;

        BigDecimal amountCopiesDelegatorsRefund = BigDecimal.ZERO;
        BigDecimal amountCopiesPersonalRefund = BigDecimal.ZERO;

        final BigDecimal costPerCopy =
                ACCOUNTING_SERVICE.calcCostPerPrintedCopy(
                        obj.getCostOriginal().negate(), obj.getCopies());

        // Create lookup of group accounts with personal invoicing.
        final Set<String> groupsPersonal = new HashSet<>();
        final Set<String> groupsPersonalRefund = new HashSet<>();
        for (final AccountTrx trx : obj.getTransactions()) {
            if (trx.getExtDetails() != null) {
                final boolean isRefund =
                        trx.getAmount().compareTo(BigDecimal.ZERO) == 1;
                if (isRefund) {
                    groupsPersonalRefund.add(trx.getExtDetails());
                } else {
                    groupsPersonal.add(trx.getExtDetails());
                }
            }
        }
        //
        for (final AccountTrx trx : obj.getTransactions()) {

            final Account account = trx.getAccount();

            final boolean isRefund =
                    trx.getAmount().compareTo(BigDecimal.ZERO) == 1;

            final BigDecimal trxCopies;

            if (costPerCopy.compareTo(BigDecimal.ZERO) != 0) {
                trxCopies = ACCOUNTING_SERVICE
                        .calcPrintedCopies(trx.getAmount(), costPerCopy, 2);
            } else {
                trxCopies = BigDecimal.valueOf(trx.getTransactionWeight());
            }

            final AccountTypeEnum accountType =
                    AccountTypeEnum.valueOf(account.getAccountType());

            if (accountType != AccountTypeEnum.SHARED
                    && accountType != AccountTypeEnum.GROUP) {

                if (trx.getAccount().getName()
                        .equalsIgnoreCase(obj.getUserId())) {

                    if (isRefund) {
                        totCopiesPersonalRefund =
                                totCopiesPersonalRefund.add(trxCopies);
                        amountCopiesPersonalRefund =
                                amountCopiesPersonalRefund.add(trx.getAmount());
                    } else {
                        totCopiesPersonal = totCopiesPersonal.add(trxCopies);
                        amountCopiesPersonal =
                                amountCopiesPersonal.add(trx.getAmount());
                    }
                } else {
                    if (isRefund) {
                        totCopiesDelegatorsRefund =
                                totCopiesDelegatorsRefund.add(trxCopies);
                        amountCopiesDelegatorsRefund =
                                amountCopiesDelegatorsRefund
                                        .add(trx.getAmount());
                    } else {
                        totCopiesDelegators =
                                totCopiesDelegators.add(trxCopies);
                        amountCopiesDelegators =
                                amountCopiesDelegators.add(trx.getAmount());
                    }
                }
                continue;
            }

            final Account accountParent = account.getParent();

            sbAccTrx.append(" &bull; <span style=\"white-space:nowrap\">");

            if (accountType == AccountTypeEnum.SHARED) {
                sbAccTrx.append(MarkupHelper.HTML_IMG_ACCOUNT_SHARED);
            } else if (accountType == AccountTypeEnum.GROUP) {
                sbAccTrx.append(MarkupHelper.HTML_IMG_ACCOUNT_GROUP);
                if ((isRefund
                        && groupsPersonalRefund.contains(account.getName()))
                        || (!isRefund && groupsPersonal
                                .contains(account.getName()))) {
                    sbAccTrx.append(MarkupHelper.HTML_IMG_ACCOUNT_PERSONAL);
                }
            }
            sbAccTrx.append("&nbsp;");

            if (accountParent != null) {
                sbAccTrx.append(accountParent.getName()).append('\\');
            }

            sbAccTrx.append(account.getName()).append("</span>");

            if (trx.getAmount().compareTo(BigDecimal.ZERO) != 0) {

                sbAccTrx.append(" ").append(currencySymbol).append("&nbsp;")
                        .append(localizedDecimal(trx.getAmount()));

                sbAccTrx.append("&nbsp;(")
                        .append(trxCopies.setScale(0, RoundingMode.HALF_EVEN))
                        .append(')');
            }
        }

        if (totCopiesDelegators.compareTo(BigDecimal.ZERO) != 0) {
            sbAccTrx.append(" &bull; <span style=\"white-space:nowrap\">")
                    .append(MarkupHelper.HTML_IMG_ACCOUNT_PERSONAL)
                    .append("&nbsp;")
                    .append(NounEnum.DELEGATOR.uiText(locale, true))
                    .append("</span> ");
            if (amountCopiesDelegators.negate().compareTo(obj.getCost()) != 0) {
                sbAccTrx.append(currencySymbol).append("&nbsp;")
                        .append(localizedDecimal(amountCopiesDelegators))
                        .append("&nbsp;");
            }
            sbAccTrx.append("(");
            sbAccTrx.append(
                    totCopiesDelegators.setScale(0, RoundingMode.HALF_EVEN));
            sbAccTrx.append(")");
        }

        if (totCopiesDelegatorsRefund.compareTo(BigDecimal.ZERO) != 0) {
            sbAccTrx.append(" &bull; <span style=\"white-space:nowrap\">")
                    .append(MarkupHelper.HTML_IMG_ACCOUNT_PERSONAL)
                    .append("&nbsp;")
                    .append(NounEnum.DELEGATOR.uiText(locale, true))
                    .append("</span> ").append(currencySymbol).append("&nbsp;")
                    .append(localizedDecimal(amountCopiesDelegatorsRefund))
                    .append("&nbsp;(").append(totCopiesDelegatorsRefund
                            .setScale(0, RoundingMode.HALF_EVEN))
                    .append(")");
        }

        // When no text accumulated, this must be a charge to personal
        // account only.
        if (sbAccTrx.length() == 0 && !obj.isZeroCost()) {
            totCopiesPersonal = BigDecimal.valueOf(obj.getCopies());
        }

        if (totCopiesPersonal.compareTo(BigDecimal.ZERO) != 0) {
            sbAccTrx.append(" &bull; <span style=\"white-space:nowrap\">")
                    .append(MarkupHelper.HTML_IMG_ACCOUNT_PERSONAL)
                    .append("&nbsp;")
                    .append(PrintOutAdjectiveEnum.PERSONAL.uiText(locale))
                    .append("</span> ");
            if (amountCopiesPersonal.negate().compareTo(obj.getCost()) != 0) {
                sbAccTrx.append(currencySymbol).append("&nbsp;")
                        .append(localizedDecimal(amountCopiesPersonal))
                        .append("&nbsp;");
            }
            sbAccTrx.append("(");
            sbAccTrx.append(
                    totCopiesPersonal.setScale(0, RoundingMode.HALF_EVEN));
            sbAccTrx.append(")");
        }

        if (totCopiesPersonalRefund.compareTo(BigDecimal.ZERO) != 0) {
            sbAccTrx.append(" &bull; <span style=\"white-space:nowrap\">")
                    .append(MarkupHelper.HTML_IMG_ACCOUNT_PERSONAL)
                    .append("&nbsp;")
                    .append(PrintOutAdjectiveEnum.PERSONAL.uiText(locale))
                    .append("</span> ").append(currencySymbol).append("&nbsp;")
                    .append(localizedDecimal(amountCopiesPersonalRefund))
                    .append("&nbsp;(").append(totCopiesPersonalRefund
                            .setScale(0, RoundingMode.HALF_EVEN))
                    .append(")");
        }

        add(new Label("account-trx", sbAccTrx.toString())
                .setEscapeModelStrings(false));

        helper.encloseLabel("account-trx-refund",
                AdjectiveEnum.REFUNDED.uiText(locale),
                !obj.isZeroCostOriginal() && obj.isRefunded());

        if (webAppType == WebAppTypeEnum.JOBTICKETS
                || webAppType == WebAppTypeEnum.ADMIN
                || webAppType.isUserTypeOrVariant()) {

            countButtons++;

            Label labelBtn = helper.encloseLabel(WID_BTN_ACCOUNT_TRX_INFO,
                    "&nbsp;", true);
            labelBtn.setEscapeModelStrings(false);

            MarkupHelper.modifyLabelAttr(labelBtn,
                    MarkupHelper.ATTR_DATA_SAVAPAGE,
                    obj.getDocLogId().toString());

            MarkupHelper.modifyLabelAttr(labelBtn, MarkupHelper.ATTR_TITLE,
                    NounEnum.TRANSACTION.uiText(getLocale(), true));

        } else {
            helper.discloseLabel(WID_BTN_ACCOUNT_TRX_INFO);
        }

        return countButtons;
    }

    /**
     *
     * @param mapVisible
     * @param obj
     *            Item.
     */
    private void populatePrintInDeniedReason(
            final Map<String, String> mapVisible, final DocLogItem obj) {

        final PrintInDeniedReasonEnum deniedReason =
                obj.getPrintInDeniedReason();

        final String reason;

        if (deniedReason == null) {
            reason = localized("print-in-denied-reason-unknown");
        } else {
            switch (deniedReason) {
            case DRM:
                reason = localized("print-in-denied-reason-drm");
                break;
            case INVALID:
                mapVisible.put("print-in-label",
                        AdjectiveEnum.INVALID.uiText(getLocale()));
                reason = AdjectiveEnum.REJECTED.uiText(getLocale());
                break;
            default:
                throw new IllegalStateException(
                        "Unhandled ".concat(deniedReason.toString()));
            }
        }

        mapVisible.put("print-in-denied-reason", reason);
    }

    /**
     *
     * @param mapVisible
     * @param obj
     *            Item.
     */
    private void populateDocOutPDF(final Map<String, String> mapVisible,
            final DocLogItem obj) {

        mapVisible.put("destination", obj.getDestination());
        mapVisible.put("prompt-destination",
                NounEnum.DESTINATION.uiText(getLocale()));

        mapVisible.put("author", obj.getAuthor());
        mapVisible.put("subject", obj.getSubject());
        mapVisible.put("keywords", obj.getKeywords());

        if (obj.getDrmRestricted()) {
            mapVisible.put("print-in-label", "DRM");
        }
        if (obj.getUserPw()) {
            mapVisible.put("userpw", "U");
        }
        if (obj.getOwnerPw()) {
            mapVisible.put("ownerpw", "O");
        }

        if (obj.getMimeType().equals(DocContent.MIMETYPE_PDF_PGP)) {
            mapVisible.put("pdfpgp", "PGP");
        }
    }

    /**
     * @param helper
     *            HTML helper
     * @param mapVisible
     * @param obj
     *            Item.
     * @param locale
     *            Locale.
     * @return {@code true} if this item is a PrintOut of a Mail Print Ticket.
     */
    private boolean populatePrintOutMailPrintTicket(final MarkupHelper helper,
            final Map<String, String> mapVisible, final DocLogItem obj,
            final Locale locale) {

        final Map<String, MailPrintData> map = obj.getMailPrintInData();
        if (map == null || map.isEmpty()) {
            return false;
        }

        final StringBuilder from = new StringBuilder();
        final StringBuilder ticket = new StringBuilder();

        for (final Entry<String, MailPrintData> entry : map.entrySet()) {
            final MailPrintData data = entry.getValue();
            from.append(data.getFromAddress()).append(" ");
            ticket.append(entry.getKey()).append(" ");
        }

        mapVisible.put("prompt-origin",
                PrepositionEnum.FROM_LOCATION.uiText(getLocale()));
        mapVisible.put("origin", from.toString().trim());

        mapVisible.put("prompt-email-ticket",
                JobTicketNounEnum.TICKET.uiText(getLocale()));
        mapVisible.put("email-ticket", ticket.toString().trim());

        return true;
    }

    /**
     * @param helper
     *            HTML helper
     * @param mapVisible
     * @param obj
     *            Item.
     * @param locale
     *            Locale.
     */
    private void populateDocOutPrintTicket(final MarkupHelper helper,
            final Map<String, String> mapVisible, final DocLogItem obj,
            final Locale locale) {

        final String ticketNumber;
        final String ticketOperator;
        final String ticketLabel;

        if (obj.getPrintMode() == PrintModeEnum.TICKET
                || obj.getPrintMode() == PrintModeEnum.TICKET_C
                || obj.getPrintMode() == PrintModeEnum.TICKET_E) {

            ticketNumber = obj.getExtId();

            final String labelTmp =
                    JOBTICKET_SERVICE.getTicketNumberLabel(ticketNumber);

            if (labelTmp == null) {
                ticketLabel = null;
            } else {
                ticketLabel = String.format("• %s", labelTmp);
            }

            // Just in case.
            if (obj.getExtData() == null) {
                ticketOperator = null;
            } else {

                final PrintSupplierData extData =
                        PrintSupplierData.createFromData(obj.getExtData());
                if (extData == null || extData.getOperator() == null) {
                    ticketOperator = null;
                } else {
                    ticketOperator =
                            String.format("(%s)", extData.getOperator());
                }
            }

            this.populateJobSheetImg(obj.getIppOptionMap(), helper);

        } else {
            ticketNumber = null;
            ticketOperator = null;
            ticketLabel = null;

            if (this.populatePrintOutMailPrintTicket(helper, mapVisible, obj,
                    locale)) {
                this.populateJobSheetImg(obj.getIppOptionMap(), helper);
            } else {
                helper.discloseLabel(WID_IMG_JOB_SHEET);
            }

            mapVisible.put("jobticket-tag-plain", obj.getExtId());
        }

        mapVisible.put("printoutMode",
                String.format("%s %s %s %s",
                        obj.getPrintMode().uiText(getLocale()),
                        StringUtils.defaultString(ticketNumber),
                        StringUtils.defaultString(ticketOperator),
                        StringUtils.defaultString(ticketLabel)).trim());

        if (obj.getDuplex()) {
            mapVisible.put("duplex", helper.localized(PrintOutNounEnum.DUPLEX));
        } else {
            mapVisible.put("simplex",
                    helper.localized(PrintOutNounEnum.SIMPLEX));
        }

        if (obj.getGrayscale()) {
            mapVisible.put("grayscale",
                    helper.localized(PrintOutNounEnum.GRAYSCALE));
        } else {
            mapVisible.put("color", helper.localized(PrintOutNounEnum.COLOR));
        }

        if (obj.isPageRotate180()) {
            mapVisible.put("pageRotate180",
                    PROXYPRINT_SERVICE.localizePrinterOpt(locale,
                            IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_INT_PAGE_ROTATE180));
        }
        if (obj.isFinishingPunch()) {
            mapVisible.put("punch", uiIppKeywordValue(locale,
                    IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_FINISHINGS_PUNCH,
                    obj));
        }
        if (obj.isFinishingStaple()) {
            mapVisible.put("staple", uiIppKeywordValue(locale,
                    IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_FINISHINGS_STAPLE,
                    obj));
        }
        if (obj.isFinishingFold()) {
            mapVisible.put("fold", String.format("%s %s",
                    helper.localized(PrintOutVerbEnum.FOLD),
                    uiIppKeywordValue(locale,
                            IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_FINISHINGS_FOLD,
                            obj)));
        }
        if (obj.isFinishingBooklet()) {
            mapVisible.put("booklet",
                    helper.localized(PrintOutNounEnum.BOOKLET));
        }

        mapVisible.put("jobcopy-options", PROXYPRINT_SERVICE
                .getJobCopyOptionsHtml(getLocale(), obj.getIppOptions()));

        mapVisible.put("papersize", obj.getPaperSize().toUpperCase());

        final String mediaSource = obj.getIppOptionMap()
                .getOptionValue(IppDictJobTemplateAttr.ATTR_MEDIA_SOURCE);

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
                        PROXYPRINT_SERVICE.localizePrinterOptValue(locale,
                                ippKeywordWlk, outputBin));
                ippKeywordWlk =
                        IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_FINISHINGS_JOG_OFFSET;

                final String jogOfset =
                        obj.getIppOptionMap().getOptionValue(ippKeywordWlk);

                if (jogOfset != null) {
                    mapVisible.put("jog-offset",
                            PROXYPRINT_SERVICE.localizePrinterOptValue(locale,
                                    ippKeywordWlk, jogOfset));
                }
            }
        }

        if (obj.getJobId() != null
                && !obj.getJobId().equals(Integer.valueOf(0))) {
            mapVisible.put("job-id", obj.getJobId().toString());
        }

        if (this.showDocLogCost && !obj.isZeroCost()) {
            mapVisible.put("cost-currency", CurrencyUtil.getCurrencySymbol(
                    obj.getCurrencyCode(), getSession().getLocale()));
            mapVisible.put("cost", localizedDecimal(obj.getCost()));
        }

        if (obj.getJobState() != null) {
            mapVisible.put("job-state", obj.getJobState().uiText(locale));
            if (obj.getCompletedDate() != null) {
                mapVisible.put("job-completed-date",
                        localizedShortTime(obj.getCompletedDate()));
            }
        }
    }

    /**
     *
     * @param model
     */
    public void populate(final IModel<DocLogItem> model) {

        final DocLogItem obj = model.getObject();
        final Locale locale = getLocale();
        final WebAppTypeEnum webAppType = SpSession.get().getWebAppType();
        final MarkupHelper helper = new MarkupHelper(this);

        String cssClass = null;

        final Map<String, String> mapVisible = new HashMap<>();
        for (final String attr : WICKET_IDS) {
            mapVisible.put(attr, null);
        }

        this.populateExtSupplier(helper, obj, locale);

        if (StringUtils.isNotBlank(obj.getComment())) {
            mapVisible.put("log-comment", obj.getComment());
        }

        int countButtons = 0;

        countButtons += this.populateAccountTrx(webAppType, mapVisible, helper,
                obj, locale);
        countButtons += this.populateRefundReverse(webAppType, helper, obj);

        String pieData = null;
        String pieSliceColors = null;
        String cssJobState = null;

        if (obj.getDocType() == DocLogDao.Type.IN) {

            cssClass = MarkupHelper.CSS_PRINT_IN_QUEUE;

            if (obj.getPrintInPrinted() != null) {

                if (obj.getPaperSize() != null) {
                    mapVisible.put("papersize",
                            obj.getPaperSize().toUpperCase());
                }
                if (obj.getDrmRestricted()) {
                    mapVisible.put("print-in-label", "DRM");
                }
                if (obj.getPrintInPrinted()) {
                    pieData = String.valueOf(obj.getTotalPages());
                    pieSliceColors =
                            SparklineHtml.arrayAttr(SparklineHtml.COLOR_QUEUE);
                } else {
                    this.populatePrintInDeniedReason(mapVisible, obj);
                }
            }

        } else {
            // Not for now...
            // mapVisible.put("signature", obj.getSignature());
            if (obj.getLetterhead()) {
                mapVisible.put("letterhead", "LH");
            }
            if (obj.getDocType() == DocLogDao.Type.PDF) {

                cssClass = MarkupHelper.CSS_PRINT_OUT_PDF;

                this.populateDocOutPDF(mapVisible, obj);

                pieData = String.valueOf(obj.getTotalPages());
                pieSliceColors =
                        SparklineHtml.arrayAttr(SparklineHtml.COLOR_PDF);
            } else {

                cssClass = MarkupHelper.CSS_PRINT_OUT_PRINTER;
                cssJobState = MarkupHelper.getCssTxtClass(obj.getJobState());

                this.populateDocOutPrintTicket(helper, mapVisible, obj, locale);

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

        final Label labelWlk = new Label("header");
        labelWlk.add(new AttributeModifier("class", cssClass));
        add(labelWlk);

        mapVisible.put("prompt-user", NounEnum.USER.uiText(getLocale()));
        mapVisible.put("user-name", obj.getUserId());

        this.populateOrigin(mapVisible, helper, obj, locale);

        add(new Label("dateCreated",
                localizedShortDateTime(obj.getCreatedDate())));

        final boolean isReopenedTicketNumber =
                JOBTICKET_SERVICE.isReopenedTicketNumber(obj.getExtId());

        countButtons += this.populateDocStoreImg(webAppType, helper, obj);
        countButtons += this.populateTicketReopenBtn(helper, obj,
                isReopenedTicketNumber);

        helper.encloseLabel("jobticket-reopened",
                AdjectiveEnum.REOPENED.uiText(locale), isReopenedTicketNumber);

        this.populateCopyJobImg(helper, obj);

        String title = null;
        if (ConfigManager.instance()
                .isConfigValue(Key.WEBAPP_DOCLOG_SHOW_DOC_TITLE)) {
            title = obj.getTitle();
        }
        mapVisible.put("title", title);

        this.populateTotals(helper, obj, locale);

        if (obj.getCopies() > 1 && obj.getCollateCopies() != null
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

        if (obj.getIppOptionMap() != null
                && obj.getIppOptionMap().isLandscapeJob()) {
            mapVisible.put("landscape",
                    helper.localized(PrintOutNounEnum.LANDSCAPE));
        }

        if (obj.getIppOptionMap() != null
                && obj.getIppOptionMap().hasPrintScaling()) {
            mapVisible.put("scaled", AdjectiveEnum.SCALED.uiText(locale));
        }

        if (countButtons > 0) {
            helper.addTransparant(WID_BTN_GROUP);
        } else {
            helper.discloseLabel(WID_BTN_GROUP);
        }

        // Hide/Show
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
    private void populateJobSheetImg(final IppOptionMap optMap,
            final MarkupHelper helper) {
        //
        final TicketJobSheetDto jobSheet =
                JOBTICKET_SERVICE.getTicketJobSheet(optMap);

        if (jobSheet != null && jobSheet.isEnabled()) {
            final StringBuilder imgSrc = new StringBuilder();
            imgSrc.append(WebApp.PATH_IMAGES).append('/');
            imgSrc.append("copy-jobticket-128x128.png");
            final Label label = helper.addModifyLabelAttr(WID_IMG_JOB_SHEET,
                    MarkupHelper.ATTR_SRC, imgSrc.toString());
            MarkupHelper.modifyLabelAttr(label, MarkupHelper.ATTR_TITLE,
                    PrintOutNounEnum.JOB_SHEET.uiText(getLocale()));
        } else {
            helper.discloseLabel(WID_IMG_JOB_SHEET);
        }
    }

    /**
     * @param helper
     *            HTML helper
     * @param obj
     *            Item.
     */
    private void populateCopyJobImg(final MarkupHelper helper,
            final DocLogItem obj) {

        if (obj.getPrintMode() == PrintModeEnum.TICKET_C) {
            final StringBuilder imgSrc = new StringBuilder();
            imgSrc.append(WebApp.PATH_IMAGES).append('/');
            imgSrc.append("scanner-32x32.png");
            helper.addModifyLabelAttr(WID_IMG_COPY_JOB, MarkupHelper.ATTR_SRC,
                    imgSrc.toString());
        } else {
            helper.discloseLabel(WID_IMG_COPY_JOB);
        }
    }

    /**
     * @param aclPermission
     *            {@link ACLPermissionEnum}.
     * @return {@code true} if permission is granted
     */
    private boolean
            isUserQueuePrivilegeGranted(final ACLPermissionEnum aclPermission) {
        return this.userQueueJournalPrivilege == null
                || aclPermission.isPresent(this.userQueueJournalPrivilege);
    }

    /**
     * @param webAppType
     *            {@link WebAppTypeEnum}.
     * @param helper
     *            HTML helper
     * @param obj
     *            Item.
     * @return number of buttons added.
     */
    private int populateDocStoreImg(final WebAppTypeEnum webAppType,
            final MarkupHelper helper, final DocLogItem obj) {

        int countButtons = 0;

        if (obj.isPrintArchive() || obj.isPrintJournal()) {

            final StringBuilder imgSrc = new StringBuilder();
            imgSrc.setLength(0);
            imgSrc.append(WebApp.PATH_IMAGES).append('/');

            final String png;
            final String widImg;
            final String widBtn;
            final NounEnum nounTitle;

            if (obj.isPrintArchive()) {
                widImg = WID_IMG_ARCHIVE;
                widBtn = WID_BTN_ARCHIVE_DOWNLOAD;
                png = "archive-16x16.png";
                nounTitle = NounEnum.ARCHIVE;
                helper.discloseLabel(WID_IMG_JOURNAL);
                helper.discloseLabel(WID_BTN_JOURNAL_DOWNLOAD);
            } else {
                widImg = WID_IMG_JOURNAL;
                widBtn = WID_BTN_JOURNAL_DOWNLOAD;
                png = "journal-16x16.png";
                nounTitle = NounEnum.JOURNAL;
                helper.discloseLabel(WID_IMG_ARCHIVE);
                helper.discloseLabel(WID_BTN_ARCHIVE_DOWNLOAD);
            }
            // Image
            imgSrc.append(png);
            MarkupHelper.modifyLabelAttr(
                    helper.addModifyLabelAttr(widImg, MarkupHelper.ATTR_SRC,
                            imgSrc.toString()),
                    MarkupHelper.ATTR_TITLE, nounTitle.uiText(getLocale()));

            // Download
            if (webAppType.isUserTypeOrVariant() && obj.hasQueueJournal()
                    && !this.isUserQueuePrivilegeGranted(
                            ACLPermissionEnum.DOWNLOAD)) {
                helper.discloseLabel(widBtn);
            } else if (obj.getPrintMode() == PrintModeEnum.TICKET_C) {
                helper.discloseLabel(widBtn);
            } else {
                final Label labelBtn =
                        helper.encloseLabel(widBtn, "&nbsp;", true);
                labelBtn.setEscapeModelStrings(false);

                MarkupHelper.modifyLabelAttr(labelBtn,
                        MarkupHelper.ATTR_DATA_SAVAPAGE,
                        obj.getDocLogId().toString());

                MarkupHelper.modifyLabelAttr(labelBtn, MarkupHelper.ATTR_TITLE,
                        nounTitle.uiText(getLocale()));

                countButtons++;
            }

            // Add/Restore Queue Journal?
            if (webAppType.isUserTypeOrVariant() && obj.hasQueueJournal()
                    && this.isUserQueuePrivilegeGranted(
                            ACLPermissionEnum.SELECT)) {

                MarkupHelper.modifyLabelAttr(MarkupHelper.modifyLabelAttr(
                        helper.encloseLabel(WID_BTN_INBOX_PRINTIN_RESTORE_ADD,
                                "&nbsp;", true),
                        MarkupHelper.ATTR_DATA_SAVAPAGE,
                        obj.getDocLogId().toString()), MarkupHelper.ATTR_TITLE,
                        HtmlButtonEnum.ADD.uiText(getLocale()))
                        .setEscapeModelStrings(false);
                countButtons++;

                MarkupHelper
                        .modifyLabelAttr(MarkupHelper.modifyLabelAttr(
                                helper.encloseLabel(
                                        WID_BTN_INBOX_PRINTIN_RESTORE_REPLACE,
                                        "&nbsp;", true),
                                MarkupHelper.ATTR_DATA_SAVAPAGE,
                                obj.getDocLogId().toString()),
                                MarkupHelper.ATTR_TITLE,
                                HtmlButtonEnum.REPLACE.uiText(getLocale()))
                        .setEscapeModelStrings(false);
                countButtons++;

            } else {
                helper.discloseLabel(WID_BTN_INBOX_PRINTIN_RESTORE_ADD);
                helper.discloseLabel(WID_BTN_INBOX_PRINTIN_RESTORE_REPLACE);
            }

            // Delete Queue Journal?
            if (webAppType.isUserTypeOrVariant() && obj.hasQueueJournal()
                    && !this.isUserQueuePrivilegeGranted(
                            ACLPermissionEnum.DELETE)) {
                helper.discloseLabel(WID_BTN_DOCLOG_STORE_DELETE);
            } else if (obj.hasQueueJournal()) {
                MarkupHelper
                        .modifyLabelAttr(MarkupHelper.modifyLabelAttr(
                                helper.encloseLabel(WID_BTN_DOCLOG_STORE_DELETE,
                                        "&nbsp;", true),
                                MarkupHelper.ATTR_DATA_SAVAPAGE,
                                obj.getDocLogId().toString()),
                                MarkupHelper.ATTR_TITLE,
                                HtmlButtonEnum.DELETE.uiText(getLocale()))
                        .setEscapeModelStrings(false);
                countButtons++;
            } else {
                helper.discloseLabel(WID_BTN_DOCLOG_STORE_DELETE);
            }

        } else {
            helper.discloseLabel(WID_IMG_ARCHIVE);
            helper.discloseLabel(WID_IMG_JOURNAL);
            helper.discloseLabel(WID_BTN_ARCHIVE_DOWNLOAD);
            helper.discloseLabel(WID_BTN_JOURNAL_DOWNLOAD);
            helper.discloseLabel(WID_BTN_DOCLOG_STORE_DELETE);
            helper.discloseLabel(WID_BTN_INBOX_PRINTIN_RESTORE_ADD);
            helper.discloseLabel(WID_BTN_INBOX_PRINTIN_RESTORE_REPLACE);
        }
        return countButtons;
    }

    /**
     * @param webAppType
     *            Type of Web App.
     * @param helper
     *            HTML helper
     * @param obj
     *            Item.
     * @return number of buttons added.
     */
    private int populateRefundReverse(final WebAppTypeEnum webAppType,
            final MarkupHelper helper, final DocLogItem obj) {

        HtmlButtonEnum htmlButton = null;
        int countButtons = 0;

        final boolean extSupplierFailure =
                obj.isExtSupplierPresent() && obj.getExtSupplierStatus() != null
                        && obj.getExtSupplierStatus().isFailure();

        if (webAppType == WebAppTypeEnum.JOBTICKETS) {
            if (extSupplierFailure) {
                if (!obj.isRefunded()) {
                    htmlButton = HtmlButtonEnum.REVERSE;
                }
            } else {
                // Print is finished and not refunded.
                if (obj.getJobState().isFinished() && !obj.isRefunded()) {
                    if (obj.getJobState().isFailure()) {
                        // Reverse (refund) failed print.
                        htmlButton = HtmlButtonEnum.REVERSE;
                    } else if (!obj.isZeroCost()) {
                        // Refund completed print.
                        htmlButton = HtmlButtonEnum.REFUND;
                    }
                }
            }
        } else if (webAppType == WebAppTypeEnum.ADMIN && this.isAccountsEditor
                && obj.getDocType() == DocLogDao.Type.PRINT) {
            if (extSupplierFailure) {
                if (!obj.isRefunded()) {
                    htmlButton = HtmlButtonEnum.REVERSE;
                }
            } else if (obj.getJobState().isFailure() && !obj.isRefunded()) {
                htmlButton = HtmlButtonEnum.REVERSE;
            }
        }

        final Label label;

        if (htmlButton == HtmlButtonEnum.REFUND) {

            label = helper.encloseLabel(WID_BTN_ACCOUNT_TRX_REFUND, "&nbsp;",
                    true);

            helper.discloseLabel(WID_BTN_PRINT_OUT_REVERSE);

        } else if (htmlButton == HtmlButtonEnum.REVERSE) {

            label = helper.encloseLabel(WID_BTN_PRINT_OUT_REVERSE, "&nbsp;",
                    true);

            helper.discloseLabel(WID_BTN_ACCOUNT_TRX_REFUND);

        } else {
            label = null;
            helper.discloseLabel(WID_BTN_ACCOUNT_TRX_REFUND);
            helper.discloseLabel(WID_BTN_PRINT_OUT_REVERSE);
        }

        if (label != null) {

            label.setEscapeModelStrings(false);

            MarkupHelper.modifyLabelAttr(label, MarkupHelper.ATTR_DATA_SAVAPAGE,
                    obj.getDocLogId().toString());

            MarkupHelper.modifyLabelAttr(label, MarkupHelper.ATTR_TITLE,
                    htmlButton.uiText(getLocale(), true));

            // Disable button if job ticket that is present on ticket queue.
            if (obj.isJobTicket() && JOBTICKET_SERVICE
                    .isTicketNumberPresent(obj.getExtId())) {
                MarkupHelper.modifyLabelAttr(label, MarkupHelper.ATTR_DISABLED,
                        MarkupHelper.ATTR_DISABLED);
            }

            countButtons++;
        }

        if (obj.getDocType() == DocLogDao.Type.PRINT) {
            final boolean isCupsFailure = obj.getJobState().isFailure();
            final boolean isExtSupplierFailure =
                    obj.isExtSupplierPresent() && obj.isZeroCostOriginal()
                            && obj.getExtSupplierStatus().isFailure();

            helper.encloseLabel("printout-reversed",
                    AdjectiveEnum.REVERSED.uiText(getLocale()),
                    obj.getDocType() == DocLogDao.Type.PRINT
                            && (isCupsFailure || isExtSupplierFailure)
                            && obj.isRefunded());
        } else {
            helper.discloseLabel("printout-reversed");
        }

        return countButtons;
    }

    /**
     *
     * @param helper
     *            HTML helper
     * @param obj
     *            Item.
     * @param isReopenedTicketNumber
     *            If {@code true} ticket is reopened version
     * @return number of buttons added.
     */
    private int populateTicketReopenBtn(final MarkupHelper helper,
            final DocLogItem obj, final boolean isReopenedTicketNumber) {

        int countButtons = 0;

        if (obj.isPrintArchive() || obj.isPrintJournal()) {

            final String ticketNumber = obj.getExtId();

            if (this.showTicketReopen && obj.isJobTicket() && !obj.isRefunded()
                    && !isReopenedTicketNumber
                    && obj.getTransactions().size() <= 1) {

                final Label labelBtn = helper
                        .encloseLabel(WID_BTN_TICKET_REOPEN, "&nbsp;", true);
                labelBtn.setEscapeModelStrings(false);

                MarkupHelper.modifyLabelAttr(labelBtn,
                        MarkupHelper.ATTR_DATA_SAVAPAGE,
                        obj.getDocLogId().toString());

                MarkupHelper.modifyLabelAttr(labelBtn, MarkupHelper.ATTR_TITLE,
                        localized("tooltip-ticket-reopen"));

                if (JOBTICKET_SERVICE.isTicketReopened(ticketNumber)) {
                    MarkupHelper.modifyLabelAttr(labelBtn,
                            MarkupHelper.ATTR_DISABLED,
                            MarkupHelper.ATTR_DISABLED);
                }

                countButtons++;

            } else {
                helper.discloseLabel(WID_BTN_TICKET_REOPEN);
            }

        } else {
            helper.discloseLabel(WID_BTN_TICKET_REOPEN);
        }
        return countButtons;
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
