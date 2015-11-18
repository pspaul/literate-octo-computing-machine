/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.pages;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.DocLogDao;
import org.savapage.core.dao.helpers.PrintInDeniedReasonEnum;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.core.util.CurrencyUtil;

/**
 *
 * @author Datraverse B.V.
 */
public class DocLogItemPanel extends Panel {

    private final NumberFormat fmNumber = NumberFormat.getInstance(getSession()
            .getLocale());

    private final DateFormat dfShortDateTime = DateFormat.getDateTimeInstance(
            DateFormat.SHORT, DateFormat.SHORT, getSession().getLocale());

    private final DateFormat dfShortTime = DateFormat.getTimeInstance(
            DateFormat.SHORT, getSession().getLocale());

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Number of currency decimals to display.
     */
    private final int currencyDecimals;

    /**
     *
     * @param id
     * @param model
     */
    public DocLogItemPanel(String id, IModel<DocLogItem> model) {

        super(id, model);

        this.currencyDecimals = ConfigManager.getUserBalanceDecimals();
    }

    /**
     * Gets the locale for the currency.
     *
     * <p>
     * NOTE: We use the Locale from the request. Do NOT use the getLocale()
     * cause it will get you the locale from the session, which does NOT contain
     * the ISO 3166 country code needed for the {@link Currency#getSymbol()}
     * method.
     * </p>
     *
     * @throws IllegalArgumentException
     *             if the country of the given locale is not a supported ISO
     *             3166 country code.
     */
    private Locale getCurrencyLocale() {
        return getRequestCycle().getRequest().getLocale();
    }

    /**
     *
     * @param model
     */
    public void populate(IModel<DocLogItem> model) {

        final DocLogItem obj = model.getObject();

        String cssClass = null;

        final Map<String, String> mapVisible = new HashMap<>();

        mapVisible.put("title", null);
        mapVisible.put("log-comment", null);
        mapVisible.put("printin-pie", null);
        mapVisible.put("pdfout-pie", null);
        mapVisible.put("printout-pie", null);
        mapVisible.put("printoutMode", null);
        mapVisible.put("signature", null);
        mapVisible.put("destination", null);
        mapVisible.put("letterhead", null);
        mapVisible.put("author", null);
        mapVisible.put("subject", null);
        mapVisible.put("keywords", null);
        mapVisible.put("drm", null);
        mapVisible.put("userpw", null);
        mapVisible.put("ownerpw", null);
        mapVisible.put("duplex", null);
        mapVisible.put("singlex", null);
        mapVisible.put("color", null);
        mapVisible.put("grayscale", null);
        mapVisible.put("papersize", null);
        mapVisible.put("cost-currency", null);
        mapVisible.put("cost", null);
        mapVisible.put("account-trx", null);
        mapVisible.put("job-id", null);
        mapVisible.put("job-state", null);
        mapVisible.put("job-completed-date", null);
        mapVisible.put("print-in-denied-reason-hyphen", null);
        mapVisible.put("print-in-denied-reason", null);
        mapVisible.put("collateCopies", null);
        mapVisible.put("ecoPrint", null);
        mapVisible.put("removeGraphics", null);

        String cssJobState = null;

        //
        if (StringUtils.isNotBlank(obj.getComment())) {
            mapVisible.put("log-comment", obj.getComment());
        }

        // Account Transactions
        if (!obj.getTransactions().isEmpty()) {

            final StringBuilder builder = new StringBuilder();

            for (final AccountTrx trx : obj.getTransactions()) {

                final Account account = trx.getAccount();

                final AccountTypeEnum accountTYpe =
                        AccountTypeEnum.valueOf(account.getAccountType());

                if (accountTYpe != AccountTypeEnum.SHARED) {
                    continue;
                }

                final Account accountParent = account.getParent();

                builder.append(" • ");

                if (accountParent != null) {
                    builder.append(accountParent.getName()).append('\\');
                }

                builder.append(account.getName())
                        .append(" ")
                        .append(CurrencyUtil.getCurrencySymbol(
                                trx.getCurrencyCode(), getLocale()))
                        .append(" ").append(localizedDecimal(trx.getAmount()));

                builder.append(" (").append(trx.getTransactionWeight())
                        .append(')');

            }

            if (builder.length() > 0) {
                mapVisible.put("account-trx", builder.toString());
            }
        }

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
                mapVisible.put("printin-pie",
                        String.valueOf(obj.getTotalPages()));
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

                mapVisible.put("pdfout-pie",
                        String.valueOf(obj.getTotalPages()));

            } else {

                mapVisible.put("printoutMode", obj.getPrintMode().getUiText());

                cssClass = MarkupHelper.CSS_PRINT_OUT_PRINTER;

                if (obj.getDuplex()) {
                    mapVisible.put("duplex", localized("duplex"));
                } else {
                    mapVisible.put("singlex", localized("singlex"));
                }

                if (obj.getGrayscale()) {
                    mapVisible.put("grayscale", localized("grayscale"));
                } else {
                    mapVisible.put("color", localized("color"));
                }

                mapVisible.put("papersize", obj.getPaperSize().toUpperCase());
                mapVisible.put("job-id", obj.getJobId().toString());

                if (obj.getCost().compareTo(BigDecimal.ZERO) != 0) {
                    mapVisible.put("cost-currency", CurrencyUtil
                            .getCurrencySymbol(obj.getCurrencyCode(),
                                    getSession().getLocale()));
                    mapVisible.put("cost", localizedDecimal(obj.getCost()));
                }

                String sfx = null;
                switch (obj.getJobState()) {
                case IPP_JOB_ABORTED:
                    cssJobState = MarkupHelper.CSS_TXT_ERROR;
                    sfx = "aborted";
                    break;
                case IPP_JOB_CANCELED:
                    cssJobState = MarkupHelper.CSS_TXT_ERROR;
                    sfx = "canceled";
                    break;
                case IPP_JOB_COMPLETED:
                    cssJobState = MarkupHelper.CSS_TXT_VALID;
                    sfx = "completed";
                    break;
                case IPP_JOB_HELD:
                    cssJobState = MarkupHelper.CSS_TXT_WARN;
                    sfx = "held";
                    break;
                case IPP_JOB_PENDING:
                    cssJobState = MarkupHelper.CSS_TXT_WARN;
                    sfx = "pending";
                    break;
                case IPP_JOB_PROCESSING:
                    cssJobState = MarkupHelper.CSS_TXT_WARN;
                    sfx = "processing";
                    break;
                case IPP_JOB_STOPPED:
                    cssJobState = MarkupHelper.CSS_TXT_ERROR;
                    sfx = "stopped";
                    break;
                }

                if (sfx != null) {
                    mapVisible.put("job-state", localized("job-state-" + sfx));
                    if (obj.getCompletedDate() != null) {
                        mapVisible.put("job-completed-date",
                                localizedShortTime(obj.getCompletedDate()));
                    }
                }

                final String sparklineData =
                        String.format(
                                "%d,%d",
                                obj.getTotalSheets(),
                                obj.getTotalPages() * obj.getCopies()
                                        - obj.getTotalSheets());

                mapVisible.put("printout-pie", sparklineData);
            }
        }

        Label labelWlk = new Label("header");
        labelWlk.add(new AttributeModifier("class", cssClass));
        add(labelWlk);

        //
        add(new Label("user-name", obj.getUserId()));
        //
        add(new Label("dateCreated",
                localizedShortDateTime(obj.getCreatedDate())));

        //
        String title = null;
        if (ConfigManager.instance().isConfigValue(
                Key.WEBAPP_DOCLOG_SHOW_DOC_TITLE)) {
            title = obj.getTitle();
        }
        mapVisible.put("title", title);

        /*
         * Totals
         */
        final StringBuilder totals = new StringBuilder();

        //
        String key = null;

        int total = obj.getTotalPages();
        int copies = obj.getCopies();

        //
        totals.append(localizedNumber(total));
        key = (total == 1) ? "page" : "pages";
        totals.append(" ").append(localized(key));

        //
        if (copies > 1) {
            totals.append(", ").append(copies).append(" ");
            if (obj.getCollateCopies() != null) {
                if (obj.getCollateCopies()) {
                    key = "collated";
                } else {
                    key = "uncollated";
                }
                totals.append(localized(key)).append(" ");
            }

            totals.append(localized("copies"));
        }

        //
        total = obj.getTotalSheets();
        if (total > 0) {
            key = (total == 1) ? "sheet" : "sheets";
            totals.append(" (").append(total).append(" ")
                    .append(localized(key)).append(")");
        }

        //
        totals.append(", ").append(obj.getHumanReadableByteCount());

        add(new Label("totals", totals.toString()));

        if (obj.getEcoPrint() != null && obj.getEcoPrint()) {
            mapVisible.put("ecoPrint", "EcoPrint");
        }

        if (obj.getRemoveGraphics() != null && obj.getRemoveGraphics()) {
            mapVisible.put("removeGraphics", localized("graphics-removed"));
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

            addVisible(StringUtils.isNotBlank(entry.getValue()),
                    entry.getKey(), entry.getValue(), cssClassWlk);
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
     * @param vlaue
     *            The {@link BigDecimal}.
     * @return The localized string.
     */
    protected final String localizedDecimal(final BigDecimal value) {
        try {
            return BigDecimalUtil.localize(value, this.currencyDecimals,
                    getSession().getLocale(), true);
        } catch (ParseException e) {
            throw new SpException(e);
        }
    }

    /**
     * Gets as localized string of a Number. The locale of the current session
     * is used.
     *
     * @param number
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
     * Gets as localized short time string of a Date. The locale of the
     * current session is used.
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

}
