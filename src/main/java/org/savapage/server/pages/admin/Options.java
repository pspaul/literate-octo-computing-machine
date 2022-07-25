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
package org.savapage.server.pages.admin;

import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.EnumSet;

import javax.print.attribute.standard.MediaSizeName;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.circuitbreaker.CircuitStateEnum;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.config.CircuitBreakerEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.config.SystemStatusEnum;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.ACLPermissionEnum;
import org.savapage.core.dao.enums.AppLogLevelEnum;
import org.savapage.core.doc.XpsToPdf;
import org.savapage.core.doc.soffice.SOfficeHelper;
import org.savapage.core.i18n.AdverbEnum;
import org.savapage.core.i18n.LabelEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.i18n.PhraseEnum;
import org.savapage.core.ipp.IppSyntaxException;
import org.savapage.core.ipp.client.IppConnectException;
import org.savapage.core.jmx.JmxRemoteProperties;
import org.savapage.core.job.SpJobScheduler;
import org.savapage.core.job.SpJobType;
import org.savapage.core.print.imap.MailPrinter;
import org.savapage.core.services.AccessControlService;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.InboxSelectScopeEnum;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.core.util.InetUtils;
import org.savapage.core.util.LocaleHelper;
import org.savapage.core.util.MediaUtils;
import org.savapage.ext.google.GoogleLdapClient;
import org.savapage.ext.google.GoogleLdapUserSource;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.pages.EnumRadioPanel;
import org.savapage.server.pages.FontOptionsPanel;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.pages.MessageContent;
import org.savapage.server.session.SpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class Options extends AbstractAdminPage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /** */
    private static final Logger LOGGER = LoggerFactory.getLogger(Options.class);

    /** */
    private static final String OBFUSCATED_PASSWORD = "* * * * *";

    /** */
    private static final AccessControlService ACCESS_CONTROL_SERVICE =
            ServiceContext.getServiceFactory().getAccessControlService();

    /** */
    private static final ProxyPrintService PROXYPRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();

    /** */
    private static final String WID_GOOGLE_CLOUD_EXPIRY =
            "ldap-google-cloud-expiry";

    /** */
    private static final String WID_GOOGLE_CLOUD_HOST =
            "ldap-google-cloud-host";

    @Override
    protected boolean needMembership() {
        return false;
    }

    /**
     *
     */
    public Options(final PageParameters parameters) {

        super(parameters);

        final boolean isReadOnlyAccess =
                !this.probePermissionToEdit(ACLOidEnum.A_OPTIONS);

        /*
         * We need the printer cache for user input validation.
         */
        try {
            PROXYPRINT_SERVICE.lazyInitPrinterCache();
        } catch (IppConnectException | IppSyntaxException e) {
            LOGGER.error(e.getMessage());
            throw new RestartResponseException(
                    new MessageContent(AppLogLevelEnum.ERROR, e.getMessage()));
        }

        //
        final MarkupHelper helper = new MarkupHelper(this);
        final LocaleHelper localeHelper = new LocaleHelper(this.getLocale());

        final ConfigManager cm = ConfigManager.instance();

        //
        labelledRadio("auth-method", "-none", IConfigProp.Key.AUTH_METHOD,
                IConfigProp.AUTH_METHOD_V_NONE);
        labelledRadio("auth-method", "-unix", IConfigProp.Key.AUTH_METHOD,
                IConfigProp.AUTH_METHOD_V_UNIX);
        labelledRadio("auth-method", "-ldap", IConfigProp.Key.AUTH_METHOD,
                IConfigProp.AUTH_METHOD_V_LDAP);
        labelledRadio("auth-method", "-custom", IConfigProp.Key.AUTH_METHOD,
                IConfigProp.AUTH_METHOD_V_CUSTOM);

        //
        helper.addLabel("custom-user-sync-label", NounEnum.SYNCHRONIZATION);
        helper.addLabel("custom-user-auth-label", NounEnum.AUTHENTICATION);

        tagInput("custom-user-sync", Key.AUTH_CUSTOM_USER_SYNC);
        tagInput("custom-user-auth", Key.AUTH_CUSTOM_USER_AUTH);

        //
        final String configLdapTypeName = ConfigManager.instance()
                .getConfigKey(IConfigProp.Key.LDAP_SCHEMA_TYPE);

        final String configLdapTypeValue = ConfigManager.instance()
                .getConfigValue(IConfigProp.Key.LDAP_SCHEMA_TYPE);

        tagLdapTypeOption(helper, "ldap-schema-type", "-open",
                configLdapTypeName, configLdapTypeValue,
                IConfigProp.LDAP_TYPE_V_OPEN_LDAP);

        tagLdapTypeOption(helper, "ldap-schema-type", "-free",
                configLdapTypeName, configLdapTypeValue,
                IConfigProp.LDAP_TYPE_V_FREE_IPA);

        tagLdapTypeOption(helper, "ldap-schema-type", "-apple",
                configLdapTypeName, configLdapTypeValue,
                IConfigProp.LDAP_TYPE_V_APPLE);

        tagLdapTypeOption(helper, "ldap-schema-type", "-activ",
                configLdapTypeName, configLdapTypeValue,
                IConfigProp.LDAP_TYPE_V_ACTIV);

        tagLdapTypeOption(helper, "ldap-schema-type", "-edir",
                configLdapTypeName, configLdapTypeValue,
                IConfigProp.LDAP_TYPE_V_E_DIR);

        if (GoogleLdapClient.isConfigured()) {

            tagLdapTypeOption(helper, "ldap-schema-type", "-google",
                    configLdapTypeName, configLdapTypeValue,
                    IConfigProp.LDAP_TYPE_V_GOOGLE_CLOUD);

            this.addGoogleCloudExpiry(helper, localeHelper);
            this.addGoogleCloudReadOnlyParms(helper);

        } else {
            helper.discloseLabel("ldap-schema-type-google");
            helper.discloseLabel(WID_GOOGLE_CLOUD_EXPIRY);
            helper.discloseLabel(WID_GOOGLE_CLOUD_HOST);
        }

        labelledInput("ldap-host", Key.AUTH_LDAP_HOST);
        labelledInput("ldap-port", Key.AUTH_LDAP_PORT);

        labelledCheckbox("ldap-use-ssl", Key.AUTH_LDAP_USE_SSL);
        labelledCheckbox("ldap-use-ssl-trust-self-signed",
                Key.AUTH_LDAP_USE_SSL_TRUST_SELF_SIGNED);

        labelledInput("ldap-basedn", Key.AUTH_LDAP_BASE_DN);
        labelledInput("ldap-admin-dn", Key.AUTH_LDAP_ADMIN_DN);
        labelledPassword("ldap-admin-pw", Key.AUTH_LDAP_ADMIN_PASSWORD,
                isReadOnlyAccess);

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
        String sourceGroup = ConfigManager.instance()
                .getConfigValue(Key.USER_SOURCE_GROUP).trim();
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

        helper.labelledCheckbox("sync-delete-users", "sync-delete-users",
                false);
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
        labelledCheckbox("user-auth-mode-email-pw",
                IConfigProp.Key.AUTH_MODE_EMAIL);
        labelledCheckbox("user-auth-mode-id-number",
                IConfigProp.Key.AUTH_MODE_ID);
        labelledCheckbox("user-auth-mode-card-local",
                IConfigProp.Key.AUTH_MODE_CARD_LOCAL);
        labelledCheckbox("user-auth-mode-yubikey",
                IConfigProp.Key.AUTH_MODE_YUBIKEY);

        //
        labelledCheckbox("user-auth-mode-name-pw-dialog",
                IConfigProp.Key.AUTH_MODE_NAME_SHOW);
        //
        add(new Label("user-auth-mode-email-pw-prompt",
                NounEnum.EMAIL.uiText(getLocale())));
        labelledCheckbox("user-auth-mode-email-pw-dialog",
                IConfigProp.Key.AUTH_MODE_EMAIL_SHOW);
        //
        labelledCheckbox("user-auth-mode-yubikey-dialog",
                IConfigProp.Key.AUTH_MODE_YUBIKEY_SHOW);

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
        labelledCheckbox("browser-local-storage",
                IConfigProp.Key.WEB_LOGIN_AUTHTOKEN_ENABLE);

        //
        labelledCheckbox("users-can-change-pin",
                IConfigProp.Key.USER_CAN_CHANGE_PIN);

        //
        labelledCheckbox("webapp-user-auth-trust-cliapp-auth",
                IConfigProp.Key.WEBAPP_USER_AUTH_TRUST_CLIAPP_AUTH);

        //
        labelledRadio("user-auth-mode-default", "-user",
                IConfigProp.Key.AUTH_MODE_DEFAULT,
                IConfigProp.AUTH_MODE_V_NAME);

        labelledRadio("user-auth-mode-default", "-email",
                IConfigProp.Key.AUTH_MODE_DEFAULT,
                IConfigProp.AUTH_MODE_V_EMAIL);

        labelledRadio("user-auth-mode-default", "-number",
                IConfigProp.Key.AUTH_MODE_DEFAULT, IConfigProp.AUTH_MODE_V_ID);

        labelledRadio("user-auth-mode-default", "-card",
                IConfigProp.Key.AUTH_MODE_DEFAULT,
                IConfigProp.AUTH_MODE_V_CARD_LOCAL);

        labelledRadio("user-auth-mode-default", "-yubikey",
                IConfigProp.Key.AUTH_MODE_DEFAULT,
                IConfigProp.AUTH_MODE_V_YUBIKEY);

        //
        helper.addLabel("user-auth-mode-2-step-header",
                LabelEnum.TWO_STEP_VERIFICATION);

        tagCheckbox("user-auth-mode-2-step", IConfigProp.Key.USER_TOTP_ENABLE);
        helper.addModifyLabelAttr("user-auth-mode-2-step-label",
                AdverbEnum.ENABLED.uiText(getLocale()), MarkupHelper.ATTR_FOR,
                cm.getConfigKey(Key.USER_TOTP_ENABLE));

        /*
         *
         */
        labelledInput("smtp-host", IConfigProp.Key.MAIL_SMTP_HOST);
        labelledInput("smtp-port", IConfigProp.Key.MAIL_SMTP_PORT);
        labelledInput("smtp-username", IConfigProp.Key.MAIL_SMTP_USER_NAME);
        labelledPassword("smtp-password", IConfigProp.Key.MAIL_SMTP_PASSWORD,
                isReadOnlyAccess);

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

        //
        String msgKey;
        String cssColor = null;
        String msgText = null;
        Label labelWrk;

        /*
         * PaperCut Integration.
         */
        labelledCheckbox("papercut-enable", IConfigProp.Key.PAPERCUT_ENABLE);
        labelledCheckbox("papercut-db-enable",
                IConfigProp.Key.PAPERCUT_DB_ENABLE);

        labelledInput("papercut-host", IConfigProp.Key.PAPERCUT_SERVER_HOST);
        labelledInput("papercut-port", IConfigProp.Key.PAPERCUT_SERVER_PORT);
        labelledPassword("papercut-token",
                IConfigProp.Key.PAPERCUT_SERVER_AUTH_TOKEN, isReadOnlyAccess);

        //
        labelledInput("papercut-db-driver",
                IConfigProp.Key.PAPERCUT_DB_JDBC_DRIVER);
        labelledInput("papercut-db-url", IConfigProp.Key.PAPERCUT_DB_JDBC_URL);
        labelledInput("papercut-db-user", IConfigProp.Key.PAPERCUT_DB_USER);
        labelledPassword("papercut-db-password",
                IConfigProp.Key.PAPERCUT_DB_PASSWORD, isReadOnlyAccess);

        /*
         * Mail Print.
         */
        labelledCheckbox("imap-enable", IConfigProp.Key.PRINT_IMAP_ENABLE);

        labelledInput("imap-host", IConfigProp.Key.PRINT_IMAP_HOST);
        labelledInput("imap-port", IConfigProp.Key.PRINT_IMAP_PORT);
        labelledInput("imap-username", IConfigProp.Key.PRINT_IMAP_USER_NAME);
        labelledPassword("imap-password", IConfigProp.Key.PRINT_IMAP_PASSWORD,
                isReadOnlyAccess);

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

        final CircuitStateEnum circuitState = ConfigManager
                .getCircuitBreaker(CircuitBreakerEnum.MAILPRINT_CONNECTION)
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
            final String exMsg =
                    "Unhandled " + CircuitStateEnum.class.getSimpleName()
                            + " value [" + circuitState.toString() + "]";
            LOGGER.error(exMsg);
            throw new RestartResponseException(
                    new MessageContent(AppLogLevelEnum.ERROR, exMsg));
        }

        if (msgKey == null) {
            msgText = "";
        } else {
            msgText = getLocalizer().getString(msgKey, this);
        }

        labelWrk = helper.encloseLabel("mailprint-status", msgText, true);
        MarkupHelper.modifyLabelAttr(labelWrk, "class", cssColor);

        labelWrk = helper.addCheckbox("flipswitch-mailprint-online",
                MailPrinter.isOnline());
        setFlipswitchOnOffText(labelWrk);

        /*
         * Web Print
         */
        labelledCheckbox("webprint-enable", IConfigProp.Key.WEB_PRINT_ENABLE);
        labelledCheckbox("webprint-enable-dropzone",
                IConfigProp.Key.WEB_PRINT_DROPZONE_ENABLE);
        labelledInput("webprint-max-file-mb",
                IConfigProp.Key.WEB_PRINT_MAX_FILE_MB);
        labelledInput("webprint-ip-allowed",
                IConfigProp.Key.WEB_PRINT_LIMIT_IP_ADDRESSES);

        /*
         * Eco Print
         */
        labelledCheckbox("ecoprint-enable", IConfigProp.Key.ECO_PRINT_ENABLE);

        labelledInput("ecoprint-discount-perc",
                IConfigProp.Key.ECO_PRINT_DISCOUNT_PERC);

        labelledInput("ecoprint-auto-threshold-page-count",
                IConfigProp.Key.ECO_PRINT_AUTO_THRESHOLD_SHADOW_PAGE_COUNT);

        labelledInput("ecoprint-resolution-dpi",
                IConfigProp.Key.ECO_PRINT_RESOLUTION_DPI);

        /*
         * Internet Print
         */
        tagInput("internetprint-base-uri",
                IConfigProp.Key.IPP_INTERNET_PRINTER_URI_BASE);

        /*
         * Financial
         */
        try {
            final String creditLimit = BigDecimalUtil.localize(
                    cm.getConfigBigDecimal(
                            IConfigProp.Key.FINANCIAL_GLOBAL_CREDIT_LIMIT),
                    cm.getConfigInt(Key.FINANCIAL_USER_BALANCE_DECIMALS),
                    getLocale(), true);

            labelledInput("financial-global-credit-limit",
                    IConfigProp.Key.FINANCIAL_GLOBAL_CREDIT_LIMIT, creditLimit);

        } catch (ParseException e) {
            LOGGER.error(e.getMessage());
            throw new RestartResponseException(
                    new MessageContent(AppLogLevelEnum.ERROR, e.getMessage()));
        }

        labelledInput("financial-printer-cost-decimals",
                IConfigProp.Key.FINANCIAL_PRINTER_COST_DECIMALS);

        labelledInput("financial-decimals-other",
                IConfigProp.Key.FINANCIAL_USER_BALANCE_DECIMALS);

        //
        final boolean disableCurrencyChange = ConfigManager.instance()
                .getSystemStatus() != SystemStatusEnum.SETUP;

        final Label labelCurrency = tagInput("financial-currency-code",
                Key.FINANCIAL_GLOBAL_CURRENCY_CODE);

        if (disableCurrencyChange) {
            labelCurrency.add(new AttributeModifier(MarkupHelper.ATTR_DISABLED,
                    "disabled"));
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
        String jmcRemoteProcess;

        try {
            jmcRemoteProcess = InetUtils.getServerHostAddress() + ":"
                    + JmxRemoteProperties.getPort();
        } catch (UnknownHostException e) {
            jmcRemoteProcess = "?????:" + JmxRemoteProperties.getPort();
            LOGGER.warn("Server IP address could not be found: {}", e);
        }

        labelWrk = new Label("jmx-remote-process", jmcRemoteProcess);
        labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_CLASS,
                MarkupHelper.CSS_TXT_VALID));
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
         * Proxy Print
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

        labelledInput("proxyprint-max-pages",
                IConfigProp.Key.PROXY_PRINT_MAX_PAGES);

        labelledCheckbox("proxyprint-clear-inbox",
                IConfigProp.Key.WEBAPP_USER_PROXY_PRINT_CLEAR_INBOX_ENABLE);

        labelledCheckbox("proxyprint-clear-inbox-scope-show-user",
                IConfigProp.Key.WEBAPP_USER_PROXY_PRINT_CLEAR_INBOX_PROMPT);

        final EnumRadioPanel clearInboxScopePanel =
                new EnumRadioPanel("proxyprint-clear-inbox-scope");

        clearInboxScopePanel.populate(
                EnumSet.complementOf(EnumSet.of(InboxSelectScopeEnum.NONE)),
                cm.getConfigEnum(InboxSelectScopeEnum.class,
                        Key.WEBAPP_USER_PROXY_PRINT_CLEAR_INBOX_SCOPE),
                InboxSelectScopeEnum.uiTextMap(getLocale()),
                cm.getConfigKey(Key.WEBAPP_USER_PROXY_PRINT_CLEAR_INBOX_SCOPE));

        add(clearInboxScopePanel);

        // Delegated Print
        helper.addLabel("proxyprint-delegate-enable-header",
                CommunityDictEnum.DELEGATED_PRINT.getWord(getLocale()));

        labelledCheckbox("proxyprint-delegate-enable",
                IConfigProp.Key.PROXY_PRINT_DELEGATE_ENABLE);

        // PaperCut Integration
        labelledCheckbox("proxyprint-delegate-papercut-enable",
                IConfigProp.Key.PROXY_PRINT_DELEGATE_PAPERCUT_ENABLE);

        labelledCheckbox("proxyprint-personal-papercut-enable",
                IConfigProp.Key.PROXY_PRINT_PERSONAL_PAPERCUT_ENABLE);

        helper.addLabel("papercut-delegator-cost-period-header",
                NounEnum.PERIOD.uiText(getLocale()));
        helper.addLabel(
                "proxyprint-delegate-papercut-invoicing-accounts-header",
                NounEnum.GROUP.uiText(getLocale()));

        /*
         * xpstopdf
         */
        labelledCheckbox("xpstopdf-enable",
                IConfigProp.Key.DOC_CONVERT_XPS_TO_PDF_ENABLED);

        boolean installed = XpsToPdf.isInstalled();
        String keyInstalled = installed ? "installed" : "not-installed";
        String colorInstalled = installed ? MarkupHelper.CSS_TXT_VALID
                : MarkupHelper.CSS_TXT_WARN;

        labelWrk = new Label("version-xpstopdf", String.format("%s (%s)",
                XpsToPdf.name(), localized(keyInstalled)));

        labelWrk.add(
                new AttributeModifier(MarkupHelper.ATTR_CLASS, colorInstalled));
        add(labelWrk);

        /*
         * LibreOffice
         */
        labelledCheckbox("libreoffice-enable",
                IConfigProp.Key.DOC_CONVERT_LIBRE_OFFICE_ENABLED);

        String version = SOfficeHelper.getLibreOfficeVersion();
        installed = StringUtils.isNotBlank(version);
        colorInstalled = installed ? MarkupHelper.CSS_TXT_VALID
                : MarkupHelper.CSS_TXT_WARN;
        keyInstalled = installed ? "installed" : "not-installed";

        if (!installed) {
            version = SOfficeHelper.name();
        }

        labelWrk = new Label("version-libreoffice",
                version + " (" + localized(keyInstalled) + ")");
        labelWrk.add(
                new AttributeModifier(MarkupHelper.ATTR_CLASS, colorInstalled));
        add(labelWrk);

        //
        labelledCheckbox("libreoffice-soffice-enable",
                IConfigProp.Key.SOFFICE_ENABLE);

        /*
         *
         */
        add(new Label("backup-location", ConfigManager.getDbBackupHome()));

        long time = ConfigManager.instance()
                .getConfigLong(IConfigProp.Key.SYS_BACKUP_LAST_RUN_TIME);
        String lastRun = "-";
        if (time > 0) {
            lastRun = localizedDateTime(new Date(time));
        }

        add(new Label("backup-last-run", lastRun));

        add(new Label("backup-next-run", localizedDateTime(
                SpJobScheduler.getNextScheduledTime(SpJobType.DB_BACKUP))));

        helper.addModifyLabelAttr("sp-database-stats",
                NounEnum.DATABASE.uiText(getLocale()), MarkupHelper.ATTR_TITLE,
                NounEnum.STATISTICS.uiText(getLocale()));

        //
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

        //
        labelledCheckbox("print-in-allow-encrypted-pdf",
                IConfigProp.Key.PRINT_IN_PDF_ENCRYPTED_ALLOW);

        labelledCheckbox("print-in-clear-at-logout",
                IConfigProp.Key.WEBAPP_USER_LOGOUT_CLEAR_INBOX);

        labelledInput("print-in-expiry-mins",
                IConfigProp.Key.PRINT_IN_JOB_EXPIRY_MINS);

        labelledInput("print-in-expiry-signal-mins",
                IConfigProp.Key.WEBAPP_USER_PRINT_IN_JOB_EXPIRY_SIGNAL_MINS);

        //
        helper.addButton("btn-username-aliases-refresh",
                HtmlButtonEnum.REFRESH);

        //
        helper.labelledCheckbox("pagometer-reset-users",
                "pagometer-reset-users", false);
        helper.labelledCheckbox("pagometer-reset-queues",
                "pagometer-reset-queues", false);
        helper.labelledCheckbox("pagometer-reset-printers",
                "pagometer-reset-printers", false);
        helper.labelledCheckbox("pagometer-reset-dashboard",
                "pagometer-reset-dashboard", false);

        //
        tagInput("locale-description", Key.SYS_DEFAULT_LOCALE);
        add(new Label("locale-description-label",
                MessageFormat.format(
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

        add(new Label("papersize-description",
                MessageFormat.format(
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

        //
        tagCheckbox("telegram-messaging", IConfigProp.Key.EXT_TELEGRAM_ENABLE);
        helper.addModifyLabelAttr("telegram-messaging-label",
                AdverbEnum.ENABLED.uiText(getLocale()), MarkupHelper.ATTR_FOR,
                cm.getConfigKey(Key.EXT_TELEGRAM_ENABLE));

        //
        this.setReadOnlyAccess(helper, isReadOnlyAccess);

        if (!isReadOnlyAccess) {
            if (ACCESS_CONTROL_SERVICE.hasPermission(
                    SpSession.get().getUserIdDto(), ACLOidEnum.A_CONFIG_EDITOR,
                    ACLPermissionEnum.READER)) {
                helper.encloseLabel("btn-config-editor",
                        localized("button-config-editor"), true);
            } else {
                helper.discloseLabel("btn-config-editor");
            }
        }
    }

    /**
     *
     * @param id
     *            The Wicket ID.
     * @param configKey
     *            The config key.
     */
    private void labelledInput(final String id,
            final IConfigProp.Key configKey) {
        tagInput(id, configKey);
        tagLabel(id + "-label", id, configKey);
    }

    /**
     *
     * @param id
     *            The Wicket ID.
     * @param configKey
     *            The config key.
     * @param obfuscate
     *            {@code true} when HTML value must me obfuscated.
     */
    private void labelledPassword(final String id,
            final IConfigProp.Key configKey, final boolean obfuscate) {
        if (obfuscate) {
            tagInput(id, configKey, OBFUSCATED_PASSWORD);
        } else {
            tagInput(id, configKey);
        }
        tagLabel(id + "-label", id, configKey);
    }

    /**
     *
     * @param id
     *            The Wicket ID.
     * @param configKey
     *            The config key.
     * @param value
     */
    private void labelledInput(final String id, final IConfigProp.Key configKey,
            final String value) {
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
     *            Value of the HTML value attribute.
     * @return The added {@link Label}.
     */
    private Label tagInput(final String id, final IConfigProp.Key key,
            final String value) {
        Label labelWrk = new Label(id);
        labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_ID,
                ConfigManager.instance().getConfigKey(key)));
        labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_VALUE, value));
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
     * Adds an input label for password {@link IConfigProp.Key}.
     *
     * @param id
     *            The label id.
     * @param key
     *            The {@link IConfigProp.Key}.
     * @param obfuscate
     *            {@code true} when HTML value must me obfuscated.
     * @return The added {@link Label}.
     */
    @SuppressWarnings("unused")
    private Label tagPassword(final String id, final IConfigProp.Key key,
            final boolean obfuscate) {
        if (obfuscate) {
            return tagInput(id, key, OBFUSCATED_PASSWORD);
        } else {
            return tagInput(id, key,
                    ConfigManager.instance().getConfigValue(key));
        }
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
        labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_ID,
                ConfigManager.instance().getConfigKey(key)));
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
        tagLabel(id, localeKey,
                ConfigManager.instance().getConfigKey(configKey));
    }

    /**
     *
     */
    @SuppressWarnings("unused")
    private void addCheckbox(final String wicketId, boolean checked) {
        Label labelWrk = new Label(wicketId);
        if (checked) {
            labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_CHECKED,
                    "checked"));
        }
        add(labelWrk);
    }

    /**
     *
     */
    private void tagCheckbox(final String wicketId, final IConfigProp.Key key) {
        Label labelWrk = new Label(wicketId);
        labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_ID,
                ConfigManager.instance().getConfigKey(key)));
        if (ConfigManager.instance().isConfigValue(key)) {
            labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_CHECKED,
                    "checked"));
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
        final Label label = new Label(id);
        label.add(new AttributeModifier(MarkupHelper.ATTR_ID,
                ConfigManager.instance().getConfigKey(key)));
        add(label);
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
        final boolean checked = ConfigManager.instance()
                .getConfigValue(configKey).equals(attrValue);

        labelledRadio(wicketIdBase, wicketIdSuffix, attrName, attrValue,
                checked);
    }

    /**
     *
     * @param wicketIdBase
     *            The base {@code wicket:id} for the select options.
     * @param wicketIdSuffix
     *            The {@code wicket:id} suffix for this option.
     * @param configKeyName
     *            Name of config key.
     * @param configKeyValue
     *            Configured vale of this option.
     * @param attrValue
     *            The value of the HTML 'value' attribute of this radio button.
     */
    private void tagLdapTypeOption(final MarkupHelper helper,
            final String wicketIdBase, final String wicketIdSuffix,
            final String configKeyName, final String configKeyValue,
            final String attrValue) {

        final boolean selected = configKeyValue.equals(attrValue);

        final Component comp =
                helper.addTransparant(wicketIdBase.concat(wicketIdSuffix));

        MarkupHelper.modifyComponentAttr(comp, MarkupHelper.ATTR_VALUE,
                attrValue);
        MarkupHelper.modifyComponentAttr(comp, MarkupHelper.ATTR_ID,
                configKeyName.concat(wicketIdSuffix));

        if (selected) {
            MarkupHelper.modifyComponentAttr(comp, MarkupHelper.ATTR_SELECTED,
                    MarkupHelper.ATTR_SELECTED);
        }
    }

    /**
     * Sets the on/off texts for a Flipswitch.
     *
     * @param label
     *            The flipswitch {@link Label}.
     */
    private void setFlipswitchOnOffText(final Label label) {
        MarkupHelper.setFlipswitchOnOffText(label, getLocale());
    }

    /**
     *
     * @param helper
     * @param readonly
     */
    private void setReadOnlyAccess(final MarkupHelper helper,
            final boolean readonly) {

        // Disable
        for (final String wicketId : new String[] { "sect-user-source",
                "sect-user-creation", "sect-user-authentication", "sect-mail",
                "sect-papercut", "sect-mail-print", "sect-web-print",
                "sect-internet-print", "sect-proxy-print", "sect-eco-print",
                "sect-financial", "sect-backups", "sect-telegram",
                "sect-advanced" }) {
            helper.addTransparantDisabled(wicketId, readonly);
        }

        // Apply
        for (final String wicketId : new String[] { "btn-apply-auth",
                "btn-apply-internal-users", "btn-apply-user-source-group-apply",
                "btn-user-sync-apply", "btn-apply-user-create",
                "btn-apply-user-auth-mode-local", "btn-apply-smtp",
                "btn-apply-mail", "btn-apply-papercut", "btn-apply-imap",
                "btn-apply-webprint", "btn-apply-internetprint",
                "btn-apply-proxyprint", "btn-apply-proxyprint-papercut",
                "btn-apply-ecoprint", "btn-apply-financial-general",
                "btn-apply-financial-pos", "btn-apply-financial-vouchers",
                "btn-apply-financial-user-transfers",
                "btn-apply-backup-automatic", "btn-apply-cliapp-auth",
                "btn-admin-pw-reset", "btn-jmx-pw-reset", "btn-apply-locale",
                "btn-apply-papersize", "btn-apply-report-fontfamily",
                "btn-apply-converters", "btn-apply-print-in",
                "btn-apply-telegram", "btn-pagometer-reset" }) {
            if (readonly) {
                helper.discloseLabel(wicketId);
            } else {
                helper.addButton(wicketId, HtmlButtonEnum.APPLY);
            }
        }

        // Test
        for (final String wicketId : new String[] { "btn-sync-test" }) {
            if (readonly) {
                helper.discloseLabel(wicketId);
            } else {
                helper.addButton(wicketId, HtmlButtonEnum.TEST);
            }
        }

        // Misc ...
        helper.encloseLabel("btn-backup-now", localized("button-backup-now"),
                !readonly);
    }

    /**
     * Adds Google Cloud Directory expiration message.
     *
     * @param helper
     *            Markup helper
     * @param localeHelper
     *            Locale helper.
     */
    private void addGoogleCloudExpiry(final MarkupHelper helper,
            final LocaleHelper localeHelper) {

        final Date now = ServiceContext.getTransactionDate();

        final String clazz;
        final PhraseEnum phrase;

        if (GoogleLdapClient.isCertExpireNearing(now)) {
            phrase = PhraseEnum.CERT_EXPIRES_ON;
            clazz = MarkupHelper.CSS_TXT_WARN;
        } else if (GoogleLdapClient.isCertExpired(now)) {
            phrase = PhraseEnum.CERT_EXPIRED_ON;
            clazz = MarkupHelper.CSS_TXT_ERROR;
        } else {
            phrase = PhraseEnum.CERT_VALID_UNTIL;
            clazz = MarkupHelper.CSS_TXT_VALID;
        }

        helper.addAppendLabelAttr(WID_GOOGLE_CLOUD_EXPIRY,
                phrase.uiText(getLocale(),
                        localeHelper.getLongDate(
                                GoogleLdapClient.getCertExpireDate())),
                MarkupHelper.ATTR_CLASS, clazz);
    }

    /**
     *
     * @param helper
     *            Markup helper.
     */
    private void addGoogleCloudReadOnlyParms(final MarkupHelper helper) {

        helper.addLabel("ldap-google-cloud-host-label",
                this.getString("ldap-host"));
        helper.addModifyLabelAttr(WID_GOOGLE_CLOUD_HOST,
                MarkupHelper.ATTR_VALUE, GoogleLdapUserSource.LDAP_HOST);

        helper.addLabel("ldap-google-cloud-port-label",
                this.getString("ldap-port"));

        helper.addModifyLabelAttr("ldap-google-cloud-port",
                MarkupHelper.ATTR_VALUE,
                GoogleLdapUserSource.getLdapPortValue());
    }
}
