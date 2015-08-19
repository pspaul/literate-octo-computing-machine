/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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
package org.savapage.server.pages.admin;

import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Date;

import javax.print.attribute.standard.MediaSizeName;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.savapage.core.SpException;
import org.savapage.core.circuitbreaker.CircuitStateEnum;
import org.savapage.core.config.CircuitBreakerEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.doc.OfficeToPdf;
import org.savapage.core.doc.XpsToPdf;
import org.savapage.core.jmx.JmxRemoteProperties;
import org.savapage.core.print.gcp.GcpPrinter;
import org.savapage.core.print.imap.ImapPrinter;
import org.savapage.core.print.smartschool.SmartSchoolPrinter;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.core.util.MediaUtils;
import org.savapage.server.pages.FontOptionsPanel;
import org.savapage.server.pages.MarkupHelper;

/**
 *
 * @author Datraverse B.V.
 */
public class Options extends AbstractAdminPage {

    private static final long serialVersionUID = 1L;

    @Override
    protected boolean needMembership() {
        return false;
    }

    /**
     *
     */
    public Options() {

        final MarkupHelper helper = new MarkupHelper(this);

        /*
         *
         */
        labelledRadio("auth-method", "-none", IConfigProp.Key.AUTH_METHOD,
                IConfigProp.AUTH_METHOD_V_NONE);
        labelledRadio("auth-method", "-unix", IConfigProp.Key.AUTH_METHOD,
                IConfigProp.AUTH_METHOD_V_UNIX);
        labelledRadio("auth-method", "-ldap", IConfigProp.Key.AUTH_METHOD,
                IConfigProp.AUTH_METHOD_V_LDAP);

        /*
         *
         */
        labelledRadio("ldap-schema-type", "-open",
                IConfigProp.Key.LDAP_SCHEMA_TYPE,
                IConfigProp.LDAP_TYPE_V_OPEN_LDAP);

        labelledRadio("ldap-schema-type", "-apple",
                IConfigProp.Key.LDAP_SCHEMA_TYPE, IConfigProp.LDAP_TYPE_V_APPLE);

        labelledRadio("ldap-schema-type", "-activ",
                IConfigProp.Key.LDAP_SCHEMA_TYPE, IConfigProp.LDAP_TYPE_V_ACTIV);

        labelledRadio("ldap-schema-type", "-edir",
                IConfigProp.Key.LDAP_SCHEMA_TYPE, IConfigProp.LDAP_TYPE_V_E_DIR);

        labelledInput("ldap-host", Key.AUTH_LDAP_HOST);
        labelledInput("ldap-port", Key.AUTH_LDAP_PORT);

        labelledCheckbox("ldap-use-ssl", Key.AUTH_LDAP_USE_SSL);

        labelledInput("ldap-basedn", Key.AUTH_LDAP_BASE_DN);
        labelledInput("ldap-admin-dn", Key.AUTH_LDAP_ADMIN_DN);
        labelledInput("ldap-admin-pw", Key.AUTH_LDAP_ADMIN_PASSWORD);

        labelledInput("ldap-user-id-number-field",
                Key.LDAP_SCHEMA_USER_ID_NUMBER_FIELD);
        labelledInput("ldap-user-card-number-field",
                Key.LDAP_SCHEMA_USER_CARD_NUMBER_FIELD);

        //
        labelledRadio("ldap-card-format", "-hex",
                IConfigProp.Key.LDAP_SCHEMA_USER_CARD_NUMBER_FORMAT,
                IConfigProp.CARD_NUMBER_FORMAT_V_HEX);
        labelledRadio("ldap-card-format", "-dec",
                IConfigProp.Key.LDAP_SCHEMA_USER_CARD_NUMBER_FORMAT,
                IConfigProp.CARD_NUMBER_FORMAT_V_DEC);

        //
        labelledRadio("ldap-card-format", "-lsb",
                IConfigProp.Key.LDAP_SCHEMA_USER_CARD_NUMBER_FIRST_BYTE,
                IConfigProp.CARD_NUMBER_FIRSTBYTE_V_LSB);
        labelledRadio("ldap-card-format", "-msb",
                IConfigProp.Key.LDAP_SCHEMA_USER_CARD_NUMBER_FIRST_BYTE,
                IConfigProp.CARD_NUMBER_FIRSTBYTE_V_MSB);
        /*
         *
         *
         */
        String sourceGroup =
                ConfigManager.instance().getConfigValue(Key.USER_SOURCE_GROUP)
                        .trim();
        if (sourceGroup.isEmpty()) {
            sourceGroup = getLocalizer().getString("all-users", this);
        }
        add(new Label("user-source-group", sourceGroup));

        labelledSelect("user-source-group-select", Key.USER_SOURCE_GROUP);

        /*
         *
         */
        labelledCheckbox("sync-user-update",
                IConfigProp.Key.USER_SOURCE_UPDATE_USER_DETAILS);
        labelledCheckbox("sync-schedule",
                IConfigProp.Key.SCHEDULE_AUTO_SYNC_USER);
        labelledCheckbox("sync-delete-users", "sync-delete-users", false);

        /*
         *
         */
        labelledCheckbox("on-demand-login",
                IConfigProp.Key.USER_INSERT_LAZY_LOGIN);
        labelledCheckbox("on-demand-print",
                IConfigProp.Key.USER_INSERT_LAZY_PRINT);

        /*
         *
         */
        labelledCheckbox("internal-users-enable",
                IConfigProp.Key.INTERNAL_USERS_ENABLE);
        labelledCheckbox("internal-users-change-pw",
                IConfigProp.Key.INTERNAL_USERS_CAN_CHANGE_PW);

        /*
         *
         */
        labelledCheckbox("user-auth-mode-name-pw",
                IConfigProp.Key.AUTH_MODE_NAME);
        labelledCheckbox("user-auth-mode-id-number",
                IConfigProp.Key.AUTH_MODE_ID);
        labelledCheckbox("user-auth-mode-card-local",
                IConfigProp.Key.AUTH_MODE_CARD_LOCAL);
        //
        labelledCheckbox("user-auth-mode-name-pw-dialog",
                IConfigProp.Key.AUTH_MODE_NAME_SHOW);
        //
        labelledCheckbox("user-auth-mode-id-number-pin",
                IConfigProp.Key.AUTH_MODE_ID_PIN_REQUIRED);
        labelledCheckbox("user-auth-mode-id-number-mask",
                IConfigProp.Key.AUTH_MODE_ID_IS_MASKED);
        labelledCheckbox("user-auth-mode-id-number-dialog",
                IConfigProp.Key.AUTH_MODE_ID_SHOW);
        //
        labelledCheckbox("user-auth-mode-card-local-dialog",
                IConfigProp.Key.AUTH_MODE_CARD_LOCAL_SHOW);
        labelledCheckbox("user-auth-mode-card-pin",
                IConfigProp.Key.AUTH_MODE_CARD_PIN_REQUIRED);
        labelledCheckbox("user-auth-mode-card-self-assoc",
                IConfigProp.Key.AUTH_MODE_CARD_SELF_ASSOCIATION);

        //
        labelledRadio("local-card-format", "-hex",
                IConfigProp.Key.CARD_NUMBER_FORMAT,
                IConfigProp.CARD_NUMBER_FORMAT_V_HEX);
        labelledRadio("local-card-format", "-dec",
                IConfigProp.Key.CARD_NUMBER_FORMAT,
                IConfigProp.CARD_NUMBER_FORMAT_V_DEC);

        //
        labelledRadio("local-card-format", "-lsb",
                IConfigProp.Key.CARD_NUMBER_FIRST_BYTE,
                IConfigProp.CARD_NUMBER_FIRSTBYTE_V_LSB);
        labelledRadio("local-card-format", "-msb",
                IConfigProp.Key.CARD_NUMBER_FIRST_BYTE,
                IConfigProp.CARD_NUMBER_FIRSTBYTE_V_MSB);

        //
        labelledCheckbox("users-can-change-pin",
                IConfigProp.Key.USER_CAN_CHANGE_PIN);

        //
        labelledCheckbox("webapp-user-auth-trust-cliapp-auth",
                IConfigProp.Key.WEBAPP_USER_AUTH_TRUST_CLIAPP_AUTH);

        //
        labelledRadio("user-auth-mode-default", "-user",
                IConfigProp.Key.AUTH_MODE_DEFAULT, IConfigProp.AUTH_MODE_V_NAME);
        labelledRadio("user-auth-mode-default", "-number",
                IConfigProp.Key.AUTH_MODE_DEFAULT, IConfigProp.AUTH_MODE_V_ID);
        labelledRadio("user-auth-mode-default", "-card",
                IConfigProp.Key.AUTH_MODE_DEFAULT,
                IConfigProp.AUTH_MODE_V_CARD_LOCAL);
        /*
         *
         */
        labelledInput("smtp-host", IConfigProp.Key.MAIL_SMTP_HOST);
        labelledInput("smtp-port", IConfigProp.Key.MAIL_SMTP_PORT);
        labelledInput("smtp-username", IConfigProp.Key.MAIL_SMTP_USER_NAME);
        labelledInput("smtp-password", IConfigProp.Key.MAIL_SMTP_PASSWORD);

        labelledRadio("smtp-security", "-none",
                IConfigProp.Key.MAIL_SMTP_SECURITY,
                IConfigProp.SMTP_SECURITY_V_NONE);
        labelledRadio("smtp-security", "-starttls",
                IConfigProp.Key.MAIL_SMTP_SECURITY,
                IConfigProp.SMTP_SECURITY_V_STARTTLS);
        labelledRadio("smtp-security", "-ssl-tls",
                IConfigProp.Key.MAIL_SMTP_SECURITY,
                IConfigProp.SMTP_SECURITY_V_SSL);
        /*
         *
         */
        labelledInput("mail-from-address", IConfigProp.Key.MAIL_FROM_ADDRESS);
        labelledInput("mail-from-name", IConfigProp.Key.MAIL_FROM_NAME);
        labelledInput("mail-reply-address",
                IConfigProp.Key.MAIL_REPLY_TO_ADDRESS);
        labelledInput("mail-reply-name", IConfigProp.Key.MAIL_REPLY_TO_NAME);

        /*
         * SmartSchool.
         */
        final String headerId = "smartschool-print-header";

        String msgKey;
        String cssColor = null;
        String msgText = null;
        Label labelWrk;

        if (ConfigManager.isSmartSchoolPrintModuleActivated()) {

            helper.encloseLabel(headerId, "SmartSchool Print", true);

            IConfigProp.Key keyWlk;

            // SmartSchool #1
            keyWlk = IConfigProp.Key.SMARTSCHOOL_1_ENABLE;
            tagLabel("smartschool-print-enable-label-1",
                    "smartschool-print-enable", keyWlk);
            tagCheckbox("smartschool-print-enable-1", keyWlk);

            keyWlk = IConfigProp.Key.SMARTSCHOOL_1_SOAP_PRINT_ENDPOINT_URL;
            tagLabel("smartschool-print-endpoint-label-1",
                    "smartschool-print-endpoint", keyWlk);
            tagInput("smartschool-print-endpoint-1", keyWlk);

            keyWlk = IConfigProp.Key.SMARTSCHOOL_1_SOAP_PRINT_ENDPOINT_PASSWORD;
            tagLabel("smartschool-print-password-label-1",
                    "smartschool-print-password", keyWlk);
            tagInput("smartschool-print-password-1", keyWlk);

            keyWlk = IConfigProp.Key.SMARTSCHOOL_1_SOAP_PRINT_PROXY_PRINTER;
            tagLabel("smartschool-print-proxyprinter-label-1",
                    "smartschool-print-proxyprinter", keyWlk);
            tagInput("smartschool-print-proxyprinter-1", keyWlk);

            // SmartSchool #2
            keyWlk = IConfigProp.Key.SMARTSCHOOL_2_ENABLE;
            tagLabel("smartschool-print-enable-label-2",
                    "smartschool-print-enable", keyWlk);
            tagCheckbox("smartschool-print-enable-2", keyWlk);

            keyWlk = IConfigProp.Key.SMARTSCHOOL_2_SOAP_PRINT_ENDPOINT_URL;
            tagLabel("smartschool-print-endpoint-label-2",
                    "smartschool-print-endpoint", keyWlk);
            tagInput("smartschool-print-endpoint-2", keyWlk);

            keyWlk = IConfigProp.Key.SMARTSCHOOL_2_SOAP_PRINT_ENDPOINT_PASSWORD;
            tagLabel("smartschool-print-password-label-2",
                    "smartschool-print-password", keyWlk);
            tagInput("smartschool-print-password-2", keyWlk);

            keyWlk = IConfigProp.Key.SMARTSCHOOL_2_SOAP_PRINT_PROXY_PRINTER;
            tagLabel("smartschool-print-proxyprinter-label-2",
                    "smartschool-print-proxyprinter", keyWlk);
            tagInput("smartschool-print-proxyprinter-2", keyWlk);

            //
            keyWlk = IConfigProp.Key.SMARTSCHOOL_USER_INSERT_LAZY_PRINT;
            tagLabel("smartschool-on-demand-user-creation-label",
                    "smartschool-on-demand-user-creation", keyWlk);
            tagCheckbox("smartschool-on-demand-user-creation", keyWlk);

            //
            labelledCheckbox("smartschool-papercut-enable",
                    IConfigProp.Key.SMARTSCHOOL_PAPERCUT_ENABLE);
            //
            labelledInput("papercut-host", IConfigProp.Key.PAPERCUT_SERVER_HOST);
            labelledInput("papercut-port", IConfigProp.Key.PAPERCUT_SERVER_PORT);
            labelledInput("papercut-token",
                    IConfigProp.Key.PAPERCUT_SERVER_AUTH_TOKEN);

            //
            labelledInput("papercut-db-driver",
                    IConfigProp.Key.PAPERCUT_DB_JDBC_DRIVER);
            labelledInput("papercut-db-url",
                    IConfigProp.Key.PAPERCUT_DB_JDBC_URL);
            labelledInput("papercut-db-user", IConfigProp.Key.PAPERCUT_DB_USER);
            labelledInput("papercut-db-password",
                    IConfigProp.Key.PAPERCUT_DB_PASSWORD);

            //
            if (SmartSchoolPrinter.isBlocked()) {
                msgKey = "blocked";
                cssColor = MarkupHelper.CSS_TXT_WARN;
            } else {
                msgKey = null;

                final CircuitStateEnum circuitState =
                        ConfigManager.getCircuitBreaker(
                                CircuitBreakerEnum.SMARTSCHOOL_CONNECTION)
                                .getCircuitState();

                switch (circuitState) {

                case CLOSED:
                    break;

                case DAMAGED:
                    msgKey = "circuit-damaged";
                    cssColor = MarkupHelper.CSS_TXT_ERROR;
                    break;

                case HALF_OPEN:
                    // no break intended
                case OPEN:
                    msgKey = "circuit-open";
                    cssColor = MarkupHelper.CSS_TXT_WARN;
                    break;

                default:
                    throw new SpException("Oops we missed "
                            + CircuitStateEnum.class.getSimpleName()
                            + " value [" + circuitState.toString() + "]");
                }
            }

            if (msgKey == null) {
                msgText = "";
            } else {
                msgText = getLocalizer().getString(msgKey, this);
            }

            labelWrk =
                    helper.encloseLabel("smartschool-print-status", msgText,
                            true);

            MarkupHelper.modifyLabelAttr(labelWrk, "class", cssColor);

            labelWrk =
                    helper.addCheckbox("flipswitch-smartschool-online",
                            SmartSchoolPrinter.isOnline());

            setFlipswitchOnOffText(labelWrk);

        } else {
            helper.discloseLabel(headerId);
        }

        /*
         * Mail Print.
         */
        labelledCheckbox("imap-enable", IConfigProp.Key.PRINT_IMAP_ENABLE);

        labelledInput("imap-host", IConfigProp.Key.PRINT_IMAP_HOST);
        labelledInput("imap-port", IConfigProp.Key.PRINT_IMAP_PORT);
        labelledInput("imap-username", IConfigProp.Key.PRINT_IMAP_USER_NAME);
        labelledInput("imap-password", IConfigProp.Key.PRINT_IMAP_PASSWORD);

        labelledRadio("imap-security", "-none",
                IConfigProp.Key.PRINT_IMAP_SECURITY,
                IConfigProp.IMAP_SECURITY_V_NONE);
        labelledRadio("imap-security", "-starttls",
                IConfigProp.Key.PRINT_IMAP_SECURITY,
                IConfigProp.IMAP_SECURITY_V_STARTTLS);
        labelledRadio("imap-security", "-ssl-tls",
                IConfigProp.Key.PRINT_IMAP_SECURITY,
                IConfigProp.IMAP_SECURITY_V_SSL);

        labelledInput("imap-folder-inbox",
                IConfigProp.Key.PRINT_IMAP_INBOX_FOLDER);
        labelledInput("imap-folder-trash",
                IConfigProp.Key.PRINT_IMAP_TRASH_FOLDER);

        labelledInput("imap-max-file-mb",
                IConfigProp.Key.PRINT_IMAP_MAX_FILE_MB);
        labelledInput("imap-max-files", IConfigProp.Key.PRINT_IMAP_MAX_FILES);

        msgKey = null;

        final CircuitStateEnum circuitState =
                ConfigManager.getCircuitBreaker(
                        CircuitBreakerEnum.MAILPRINT_CONNECTION)
                        .getCircuitState();

        switch (circuitState) {

        case CLOSED:
            break;

        case DAMAGED:
            msgKey = "circuit-damaged";
            cssColor = MarkupHelper.CSS_TXT_ERROR;
            break;

        case HALF_OPEN:
            // no break intended
        case OPEN:
            msgKey = "circuit-open";
            cssColor = MarkupHelper.CSS_TXT_WARN;
            break;

        default:
            throw new SpException("Oops we missed "
                    + CircuitStateEnum.class.getSimpleName() + " value ["
                    + circuitState.toString() + "]");
        }

        if (msgKey == null) {
            msgText = "";
        } else {
            msgText = getLocalizer().getString(msgKey, this);
        }

        labelWrk = helper.encloseLabel("mailprint-status", msgText, true);
        MarkupHelper.modifyLabelAttr(labelWrk, "class", cssColor);

        labelWrk =
                helper.addCheckbox("flipswitch-mailprint-online",
                        ImapPrinter.isOnline());
        setFlipswitchOnOffText(labelWrk);

        /*
         * Web Print
         */
        labelledCheckbox("webprint-enable", IConfigProp.Key.WEB_PRINT_ENABLE);
        labelledInput("webprint-max-file-mb",
                IConfigProp.Key.WEB_PRINT_MAX_FILE_MB);
        labelledInput("webprint-ip-allowed",
                IConfigProp.Key.WEB_PRINT_LIMIT_IP_ADDRESSES);

        /*
         * Google Cloud Print.
         */
        final ConfigManager cm = ConfigManager.instance();

        labelledCheckbox("gcp-enable", IConfigProp.Key.GCP_ENABLE);

        helper.addTextInput("gcp-client-id", GcpPrinter.getOAuthClientId());
        helper.addTextInput("gcp-client-secret",
                GcpPrinter.getOAuthClientSecret());
        helper.addTextInput("gcp-printer-name", GcpPrinter.getPrinterName());

        addCheckbox("gcp-mail-after-cancel-enable",
                cm.isConfigValue(Key.GCP_JOB_OWNER_UNKNOWN_CANCEL_MAIL_ENABLE));

        helper.addTextInput("gcp-mail-after-cancel-subject", cm
                .getConfigValue(Key.GCP_JOB_OWNER_UNKNOWN_CANCEL_MAIL_SUBJECT));

        add(new Label("gcp-mail-after-cancel-body", ConfigManager.instance()
                .getConfigValue(Key.GCP_JOB_OWNER_UNKNOWN_CANCEL_MAIL_BODY)));

        add(new Label("gcp-summary-printer-name", GcpPrinter.getPrinterName()));
        add(new Label("gcp-summary-printer-owner", GcpPrinter.getOwnerId()));
        add(new Label("gcp-summary-printer-state", GcpPrinter.getState()
                .toString()));

        boolean enabled = ConfigManager.isGcpEnabled();
        final GcpPrinter.State gcpStatus = GcpPrinter.getState();

        if (enabled && gcpStatus == GcpPrinter.State.ON_LINE) {
            cssColor = MarkupHelper.CSS_TXT_VALID;
        } else {
            cssColor = MarkupHelper.CSS_TXT_WARN;
        }

        labelWrk =
                new Label("gcp-summary-printer-state-display",
                        GcpPrinter.localized(enabled, gcpStatus));
        labelWrk.add(new AttributeModifier("class", cssColor));
        add(labelWrk);

        /*
         * Financial
         */

        // General

        try {
            final String creditLimit =
                    BigDecimalUtil
                            .localize(
                                    cm.getConfigBigDecimal(IConfigProp.Key.FINANCIAL_GLOBAL_CREDIT_LIMIT),
                                    cm.getConfigInt(Key.FINANCIAL_USER_BALANCE_DECIMALS),
                                    getLocale(), true);

            labelledInput("financial-global-credit-limit",
                    IConfigProp.Key.FINANCIAL_GLOBAL_CREDIT_LIMIT, creditLimit);

        } catch (ParseException e) {
            throw new SpException(e);
        }

        labelledInput("financial-printer-cost-decimals",
                IConfigProp.Key.FINANCIAL_PRINTER_COST_DECIMALS);

        labelledInput("financial-decimals-other",
                IConfigProp.Key.FINANCIAL_USER_BALANCE_DECIMALS);

        //
        boolean disableCurrencyChange =
                ConfigManager.instance().isAppReadyToUse();

        final Label labelCurrency =
                tagInput("financial-currency-code",
                        Key.FINANCIAL_GLOBAL_CURRENCY_CODE);

        if (disableCurrencyChange) {
            labelCurrency.add(new AttributeModifier("disabled", "disabled"));
        }

        helper.encloseLabel("button-change-financial-currency-code",
                localized("button-change"), !disableCurrencyChange);

        helper.encloseLabel("financial-currency-code-help",
                localized("financial-currency-code-help"),
                disableCurrencyChange);

        // POS

        labelledInput("financial-payment-methods",
                IConfigProp.Key.FINANCIAL_POS_PAYMENT_METHODS);

        tagTextarea("financial-pos-receipt-header",
                IConfigProp.Key.FINANCIAL_POS_RECEIPT_HEADER);

        // Vouchers

        labelledCheckbox("financial-user-vouchers-enable",
                IConfigProp.Key.FINANCIAL_USER_VOUCHERS_ENABLE);

        labelledInput("financial-voucher-card-header",
                IConfigProp.Key.FINANCIAL_VOUCHER_CARD_HEADER);

        labelledInput("financial-voucher-card-footer",
                IConfigProp.Key.FINANCIAL_VOUCHER_CARD_FOOTER);

        //
        labelledCheckbox("financial-user-transfer-enable",
                IConfigProp.Key.FINANCIAL_USER_TRANSFER_ENABLE);

        labelledCheckbox("financial-user-transfer-enable-comments",
                IConfigProp.Key.FINANCIAL_USER_TRANSFER_ENABLE_COMMENTS);

        /*
         * Report Font Family.
         */
        FontOptionsPanel fontOptionsPanel =
                new FontOptionsPanel("voucher-fontfamily-options");
        add(fontOptionsPanel);

        fontOptionsPanel.populate(ConfigManager
                .getConfigFontFamily(Key.FINANCIAL_VOUCHER_CARD_FONT_FAMILY));

        /*
         * JMX
         */
        final String jmcRemoteProcess;

        try {
            jmcRemoteProcess =
                    ConfigManager.getServerHostAddress() + ":"
                            + JmxRemoteProperties.getPort();
        } catch (UnknownHostException e) {
            throw new SpException("Server IP address could not be found", e);
        }

        labelWrk = new Label("jmx-remote-process", jmcRemoteProcess);
        labelWrk.add(new AttributeModifier("class", MarkupHelper.CSS_TXT_VALID));
        add(labelWrk);

        add(new Label("jmx-password-reset", localized("jmx-password-reset",
                JmxRemoteProperties.getAdminUsername())));

        /*
         * User Client
         */
        labelledCheckbox("cliapp-auth-trust-user-account",
                IConfigProp.Key.CLIAPP_AUTH_TRUST_USER_ACCOUNT);

        labelledCheckbox("cliapp-auth-trust-webapp-user-auth",
                IConfigProp.Key.CLIAPP_AUTH_TRUST_WEBAPP_USER_AUTH);

        labelledInput("cliapp-auth-admin-passkey",
                IConfigProp.Key.CLIAPP_AUTH_ADMIN_PASSKEY);

        /*
         *
         */
        labelledCheckbox("proxyprint-non-secure",
                IConfigProp.Key.PROXY_PRINT_NON_SECURE);

        labelledInput("proxyprint-non-secure-printer-group",
                IConfigProp.Key.PROXY_PRINT_NON_SECURE_PRINTER_GROUP);

        labelledInput("proxyprint-fast-expiry-mins",
                IConfigProp.Key.PROXY_PRINT_FAST_EXPIRY_MINS);

        labelledInput("proxyprint-hold-expiry-mins",
                IConfigProp.Key.PROXY_PRINT_HOLD_EXPIRY_MINS);

        labelledInput("proxyprint-direct-expiry-secs",
                IConfigProp.Key.PROXY_PRINT_DIRECT_EXPIRY_SECS);

        labelledInput("proxyprint-max-copies",
                IConfigProp.Key.WEBAPP_USER_PROXY_PRINT_MAX_COPIES);

        labelledCheckbox("proxyprint-clear-inbox",
                IConfigProp.Key.WEBAPP_USER_PROXY_PRINT_CLEAR_INBOX);

        /*
         * xpstopdf
         */
        labelledCheckbox("xpstopdf-enable",
                IConfigProp.Key.DOC_CONVERT_XPS_TO_PDF_ENABLED);

        boolean installed = XpsToPdf.isInstalled();
        String keyInstalled = installed ? "installed" : "not-installed";
        String colorInstalled =
                installed ? MarkupHelper.CSS_TXT_VALID
                        : MarkupHelper.CSS_TXT_WARN;

        labelWrk =
                new Label("version-xpstopdf", XpsToPdf.name() + " ("
                        + localized(keyInstalled) + ")");

        labelWrk.add(new AttributeModifier("class", colorInstalled));
        add(labelWrk);

        /*
         * LibreOffice
         */
        labelledCheckbox("libreoffice-enable",
                IConfigProp.Key.DOC_CONVERT_LIBRE_OFFICE_ENABLED);

        String version = OfficeToPdf.getLibreOfficeVersion();
        installed = StringUtils.isNotBlank(version);
        colorInstalled =
                installed ? MarkupHelper.CSS_TXT_VALID
                        : MarkupHelper.CSS_TXT_WARN;
        keyInstalled = installed ? "installed" : "not-installed";

        if (!installed) {
            version = OfficeToPdf.name();
        }

        labelWrk =
                new Label("version-libreoffice", version + " ("
                        + localized(keyInstalled) + ")");
        labelWrk.add(new AttributeModifier("class", colorInstalled));
        add(labelWrk);

        /*
         *
         */
        add(new Label("backup-location", ConfigManager.getDbBackupHome()));

        long time =
                ConfigManager.instance().getConfigLong(
                        IConfigProp.Key.SYS_BACKUP_LAST_RUN_TIME);
        String lastRun = "-";
        if (time > 0) {
            lastRun = localizedDateTime(new Date(time));
        }
        add(new Label("backup-last-run", lastRun));

        /*
         *
         */
        labelledCheckbox("enable-automatic-backup",
                IConfigProp.Key.SYS_BACKUP_ENABLE_AUTOMATIC);
        labelledInput("backups-keep-days",
                IConfigProp.Key.SYS_BACKUP_DAYS_TO_KEEP);

        labelledCheckbox("backup-delete-account-trxs",
                IConfigProp.Key.DELETE_ACCOUNT_TRX_LOG);
        labelledInput("backup-delete-account-trx-days",
                IConfigProp.Key.DELETE_ACCOUNT_TRX_DAYS);

        labelledCheckbox("backup-delete-doc-logs",
                IConfigProp.Key.DELETE_DOC_LOG);
        labelledInput("backup-delete-doc-log-days",
                IConfigProp.Key.DELETE_DOC_LOG_DAYS);

        labelledCheckbox("backup-delete-app-logs",
                IConfigProp.Key.DELETE_APP_LOG);
        labelledInput("backup-delete-app-log-days",
                IConfigProp.Key.DELETE_APP_LOG_DAYS);

        /*
         *
         */
        labelledCheckbox("print-in-allow-encrypted-pdf",
                IConfigProp.Key.PRINT_IN_ALLOW_ENCRYPTED_PDF);

        /*
         *
         */
        labelledCheckbox("pagometer-reset-users", "pagometer-reset-users",
                false);
        labelledCheckbox("pagometer-reset-queues", "pagometer-reset-queues",
                false);
        labelledCheckbox("pagometer-reset-printers",
                "pagometer-reset-printers", false);
        labelledCheckbox("pagometer-reset-dashboard",
                "pagometer-reset-dashboard", false);

        /*
         *
         */
        tagInput("locale-description", Key.SYS_DEFAULT_LOCALE);
        add(new Label("locale-description-label", MessageFormat.format(
                getLocalizer().getString("locale-description", this),
                ConfigManager.getServerHostLocale().toLanguageTag())));

        /*
         * Report Font Family.
         */
        fontOptionsPanel = new FontOptionsPanel("report-fontfamily-options");
        add(fontOptionsPanel);

        fontOptionsPanel.populate(ConfigManager
                .getConfigFontFamily(Key.REPORTS_PDF_INTERNAL_FONT_FAMILY));

        /*
         *
         */
        final MediaSizeName hostMediaSize =
                MediaUtils.getHostDefaultMediaSize();

        String hostPaperSize;

        if (hostMediaSize == MediaSizeName.ISO_A4) {
            hostPaperSize =
                    getLocalizer().getString("default-papersize-a4", this);
        } else if (hostMediaSize == MediaSizeName.NA_LETTER) {
            hostPaperSize =
                    getLocalizer().getString("default-papersize-letter", this);
        } else {
            hostPaperSize = hostMediaSize.toString();
        }

        add(new Label("papersize-description", MessageFormat.format(
                getLocalizer().getString("papersize-description", this),
                hostPaperSize)));

        labelledRadio("default-papersize", "-system",
                IConfigProp.Key.SYS_DEFAULT_PAPER_SIZE,
                IConfigProp.PAPERSIZE_V_SYSTEM);
        labelledRadio("default-papersize", "-letter",
                IConfigProp.Key.SYS_DEFAULT_PAPER_SIZE,
                IConfigProp.PAPERSIZE_V_LETTER);
        labelledRadio("default-papersize", "-a4",
                IConfigProp.Key.SYS_DEFAULT_PAPER_SIZE,
                IConfigProp.PAPERSIZE_V_A4);

    }

    private void
            labelledInput(final String id, final IConfigProp.Key configKey) {
        tagInput(id, configKey);
        tagLabel(id + "-label", id, configKey);
    }

    private void labelledInput(final String id,
            final IConfigProp.Key configKey, final String value) {
        tagInput(id, configKey, value);
        tagLabel(id + "-label", id, configKey);
    }

    /**
     * Adds an input label for {@link IConfigProp.Key}.
     *
     * @param id
     *            The label id.
     * @param key
     *            The {@link IConfigProp.Key}.
     * @param value
     *            The value.
     * @return The added {@link Label}.
     */
    private Label tagInput(final String id, final IConfigProp.Key key,
            final String value) {
        Label labelWrk = new Label(id);
        labelWrk.add(new AttributeModifier("id", ConfigManager.instance()
                .getConfigKey(key)));
        labelWrk.add(new AttributeModifier("value", value));
        add(labelWrk);
        return labelWrk;
    }

    /**
     * Adds an input label for {@link IConfigProp.Key}.
     *
     * @param id
     *            The label id.
     * @param key
     *            The {@link IConfigProp.Key}.
     * @return The added {@link Label}.
     */
    private Label tagInput(final String id, final IConfigProp.Key key) {
        return tagInput(id, key, ConfigManager.instance().getConfigValue(key));
    }

    /**
     * Adds an textarea label for {@link IConfigProp.Key}.
     *
     * @param id
     *            The label id.
     * @param key
     *            The {@link IConfigProp.Key}.
     * @param value
     *            The value.
     */
    private void tagTextarea(final String id, final IConfigProp.Key key,
            final String value) {
        Label labelWrk = new Label(id, value);
        labelWrk.add(new AttributeModifier("id", ConfigManager.instance()
                .getConfigKey(key)));
        add(labelWrk);
    }

    /**
     *
     */
    private void tagTextarea(final String id, final IConfigProp.Key key) {
        tagTextarea(id, key, ConfigManager.instance().getConfigValue(key));
    }

    /**
     *
     */
    private void tagLabel(final String id, final String localeKey,
            final IConfigProp.Key configKey) {
        tagLabel(id, localeKey, ConfigManager.instance()
                .getConfigKey(configKey));
    }

    /**
     *
     */
    private void addCheckbox(final String wicketId, boolean checked) {
        Label labelWrk = new Label(wicketId);
        if (checked) {
            labelWrk.add(new AttributeModifier("checked", "checked"));
        }
        add(labelWrk);
    }

    /**
     *
     */
    private void tagCheckbox(final String wicketId, final IConfigProp.Key key) {
        Label labelWrk = new Label(wicketId);
        labelWrk.add(new AttributeModifier("id", ConfigManager.instance()
                .getConfigKey(key)));
        if (ConfigManager.instance().isConfigValue(key)) {
            labelWrk.add(new AttributeModifier("checked", "checked"));
        }
        add(labelWrk);
    }

    /**
     * Adds a label/input checkbox.
     *
     * @param wicketId
     *            The {@code wicket:id} of the {@code <input>} part. The
     *            {@code <label>} part must have the same {@code wicket:id} with
     *            the {@code-label} suffix appended.
     * @param key
     *            The enum key of the Global Configuration item. The string
     *            version of this key becomes the HTML 'id' of the
     *            {@code <input>} part, and the HTML 'for' of the
     *            {@code <label>} part.
     */
    private void labelledCheckbox(final String wicketId,
            final IConfigProp.Key key) {
        tagCheckbox(wicketId, key);
        tagLabel(wicketId + "-label", wicketId, key);
    }

    /**
     *
     */
    private void tagSelect(final String id, final IConfigProp.Key key) {
        Label labelWrk = new Label(id);
        labelWrk.add(new AttributeModifier("id", ConfigManager.instance()
                .getConfigKey(key)));
        add(labelWrk);
    }

    /**
     * Labels a select tag.
     *
     * @param id
     *            The Wicket id.
     * @param key
     *            The enum property key.
     *
     */
    private void labelledSelect(final String id, final IConfigProp.Key key) {
        tagSelect(id, key);
        tagLabel(id + "-label", id, key);
    }

    /**
     *
     * @param wicketIdBase
     *            The base {@code wicket:id} for the radio group items.
     * @param wicketIdSuffix
     *            The {@code wicket:id} suffix for this item.
     * @param configKey
     *            Used to create the value of the HTML 'name' attribute of this
     *            radio button.
     * @param attrValue
     *            The value of the HTML 'value' attribute of this radio button.
     */
    private void labelledRadio(final String wicketIdBase,
            final String wicketIdSuffix, final IConfigProp.Key configKey,
            final String attrValue) {

        final String attrName =
                ConfigManager.instance().getConfigKey(configKey);
        final boolean checked =
                ConfigManager.instance().getConfigValue(configKey)
                        .equals(attrValue);

        labelledRadio(wicketIdBase, wicketIdSuffix, attrName, attrValue,
                checked);
    }

    /**
     * Sets the on/off texts for a Flipswitch.
     *
     * @param label
     *            The flipswitch {@link Label}.
     */
    private void setFlipswitchOnOffText(final Label label) {

        MarkupHelper.modifyLabelAttr(label, "data-on-text", getLocalizer()
                .getString("flipswitch-on", this));
        MarkupHelper.modifyLabelAttr(label, "data-off-text", getLocalizer()
                .getString("flipswitch-off", this));
    }

}
