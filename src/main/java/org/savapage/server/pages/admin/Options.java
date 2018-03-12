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
package org.savapage.server.pages.admin;

import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.EnumSet;

import javax.print.attribute.standard.MediaSizeName;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.SpException;
import org.savapage.core.circuitbreaker.CircuitStateEnum;
import org.savapage.core.config.CircuitBreakerEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.ACLPermissionEnum;
import org.savapage.core.dao.enums.AppLogLevelEnum;
import org.savapage.core.dao.enums.ExternalSupplierEnum;
import org.savapage.core.doc.XpsToPdf;
import org.savapage.core.doc.soffice.SOfficeHelper;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.ipp.IppSyntaxException;
import org.savapage.core.ipp.client.IppConnectException;
import org.savapage.core.jmx.JmxRemoteProperties;
import org.savapage.core.job.SpJobScheduler;
import org.savapage.core.job.SpJobType;
import org.savapage.core.print.gcp.GcpPrinter;
import org.savapage.core.print.imap.ImapPrinter;
import org.savapage.core.services.AccessControlService;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.InboxSelectScopeEnum;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.core.util.InetUtils;
import org.savapage.core.util.MediaUtils;
import org.savapage.ext.smartschool.SmartschoolPrinter;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.pages.EnumRadioPanel;
import org.savapage.server.pages.FontOptionsPanel;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.pages.MessageContent;
import org.savapage.server.session.SpSession;

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
    private static final String OBFUSCATED_PASSWORD = "* * * * *";

    /** */
    private static final AccessControlService ACCESS_CONTROL_SERVICE =
            ServiceContext.getServiceFactory().getAccessControlService();

    /** */
    private static final ProxyPrintService PROXYPRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();

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
            throw new RestartResponseException(
                    new MessageContent(AppLogLevelEnum.ERROR, e.getMessage()));
        }

        //
        final MarkupHelper helper = new MarkupHelper(this);
        final ConfigManager cm = ConfigManager.instance();

        //
        labelledRadio("auth-method", "-none", IConfigProp.Key.AUTH_METHOD,
                IConfigProp.AUTH_METHOD_V_NONE);
        labelledRadio("auth-method", "-unix", IConfigProp.Key.AUTH_METHOD,
                IConfigProp.AUTH_METHOD_V_UNIX);
        labelledRadio("auth-method", "-ldap", IConfigProp.Key.AUTH_METHOD,
                IConfigProp.AUTH_METHOD_V_LDAP);

        //
        labelledRadio("ldap-schema-type", "-open",
                IConfigProp.Key.LDAP_SCHEMA_TYPE,
                IConfigProp.LDAP_TYPE_V_OPEN_LDAP);

        labelledRadio("ldap-schema-type", "-apple",
                IConfigProp.Key.LDAP_SCHEMA_TYPE,
                IConfigProp.LDAP_TYPE_V_APPLE);

        labelledRadio("ldap-schema-type", "-activ",
                IConfigProp.Key.LDAP_SCHEMA_TYPE,
                IConfigProp.LDAP_TYPE_V_ACTIV);

        labelledRadio("ldap-schema-type", "-edir",
                IConfigProp.Key.LDAP_SCHEMA_TYPE,
                IConfigProp.LDAP_TYPE_V_E_DIR);

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

        labelledRadio("user-auth-mode-default", "-number",
                IConfigProp.Key.AUTH_MODE_DEFAULT, IConfigProp.AUTH_MODE_V_ID);

        labelledRadio("user-auth-mode-default", "-card",
                IConfigProp.Key.AUTH_MODE_DEFAULT,
                IConfigProp.AUTH_MODE_V_CARD_LOCAL);

        labelledRadio("user-auth-mode-default", "-yubikey",
                IConfigProp.Key.AUTH_MODE_DEFAULT,
                IConfigProp.AUTH_MODE_V_YUBIKEY);

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

        /*
         * SmartSchool.
         */
        final String headerId = "smartschool-print-header";

        String msgKey;
        String cssColor = null;
        String msgText = null;
        Label labelWrk;

        if (ConfigManager.isSmartSchoolPrintModuleActivated()) {

            helper.encloseLabel(headerId,
                    String.format("%s Print",
                            ExternalSupplierEnum.SMARTSCHOOL.getUiText()),
                    true);

            IConfigProp.Key keyWlk;

            // SmartSchool #1
            keyWlk = IConfigProp.Key.SMARTSCHOOL_1_ENABLE;
            tagLabel("smartschool-print-enable-label-1",
                    "smartschool-print-enable-1", keyWlk);
            tagCheckbox("smartschool-print-enable-1", keyWlk);

            keyWlk = IConfigProp.Key.SMARTSCHOOL_1_SOAP_PRINT_ENDPOINT_URL;
            tagLabel("smartschool-print-endpoint-label-1",
                    "smartschool-print-endpoint", keyWlk);
            tagInput("smartschool-print-endpoint-1", keyWlk);

            keyWlk = IConfigProp.Key.SMARTSCHOOL_1_SOAP_PRINT_ENDPOINT_PASSWORD;
            tagLabel("smartschool-print-password-label-1",
                    "smartschool-print-password", keyWlk);
            tagPassword("smartschool-print-password-1", keyWlk,
                    isReadOnlyAccess);

            keyWlk = IConfigProp.Key.SMARTSCHOOL_1_SOAP_PRINT_PROXY_PRINTER;
            tagLabel("smartschool-print-proxyprinter-label-1",
                    "smartschool-print-proxyprinter", keyWlk);
            tagInput("smartschool-print-proxyprinter-1", keyWlk);

            keyWlk = IConfigProp.Key.SMARTSCHOOL_1_SOAP_PRINT_PROXY_PRINTER_DUPLEX;
            tagLabel("smartschool-print-proxyprinter-duplex-label-1",
                    "smartschool-print-proxyprinter-duplex", keyWlk);
            tagInput("smartschool-print-proxyprinter-duplex-1", keyWlk);

            keyWlk = IConfigProp.Key.SMARTSCHOOL_1_SOAP_PRINT_PROXY_PRINTER_GRAYSCALE;
            tagLabel("smartschool-print-proxyprinter-grayscale-label-1",
                    "smartschool-print-proxyprinter-grayscale", keyWlk);
            tagInput("smartschool-print-proxyprinter-grayscale-1", keyWlk);

            keyWlk = IConfigProp.Key.SMARTSCHOOL_1_SOAP_PRINT_PROXY_PRINTER_GRAYSCALE_DUPLEX;
            tagLabel("smartschool-print-proxyprinter-grayscale-duplex-label-1",
                    "smartschool-print-proxyprinter-grayscale-duplex", keyWlk);
            tagInput("smartschool-print-proxyprinter-grayscale-duplex-1",
                    keyWlk);

            keyWlk = IConfigProp.Key.SMARTSCHOOL_1_SOAP_PRINT_CHARGE_TO_STUDENTS;
            tagLabel("smartschool-print-charge-to-students-label-1",
                    "smartschool-print-charge-to-students", keyWlk);
            tagCheckbox("smartschool-print-charge-to-students-1", keyWlk);

            //
            keyWlk = IConfigProp.Key.SMARTSCHOOL_1_SOAP_PRINT_NODE_ENABLE;
            tagLabel("smartschool-print-node-enable-label-1",
                    "smartschool-print-node-enable", keyWlk);
            tagCheckbox("smartschool-print-node-enable-1", keyWlk);

            keyWlk = IConfigProp.Key.SMARTSCHOOL_1_SOAP_PRINT_NODE_PROXY_ENABLE;
            tagLabel("smartschool-print-node-proxy-enable-label-1",
                    "smartschool-print-node-proxy-enable", keyWlk);
            tagCheckbox("smartschool-print-node-proxy-enable-1", keyWlk);

            keyWlk = IConfigProp.Key.SMARTSCHOOL_1_SOAP_PRINT_NODE_ID;
            tagLabel("smartschool-print-node-id-label-1",
                    "smartschool-print-node-id", keyWlk);
            tagInput("smartschool-print-node-id-1", keyWlk);

            keyWlk = IConfigProp.Key.SMARTSCHOOL_1_SOAP_PRINT_NODE_PROXY_ENDPOINT_URL;
            tagLabel("smartschool-print-node-proxy-endpoint-label-1",
                    "smartschool-print-node-proxy-endpoint", keyWlk);
            tagInput("smartschool-print-node-proxy-endpoint-1", keyWlk);

            // SmartSchool #2
            keyWlk = IConfigProp.Key.SMARTSCHOOL_2_ENABLE;
            tagLabel("smartschool-print-enable-label-2",
                    "smartschool-print-enable-2", keyWlk);
            tagCheckbox("smartschool-print-enable-2", keyWlk);

            keyWlk = IConfigProp.Key.SMARTSCHOOL_2_SOAP_PRINT_ENDPOINT_URL;
            tagLabel("smartschool-print-endpoint-label-2",
                    "smartschool-print-endpoint", keyWlk);
            tagInput("smartschool-print-endpoint-2", keyWlk);

            keyWlk = IConfigProp.Key.SMARTSCHOOL_2_SOAP_PRINT_ENDPOINT_PASSWORD;
            tagLabel("smartschool-print-password-label-2",
                    "smartschool-print-password", keyWlk);
            tagPassword("smartschool-print-password-2", keyWlk,
                    isReadOnlyAccess);

            keyWlk = IConfigProp.Key.SMARTSCHOOL_2_SOAP_PRINT_PROXY_PRINTER;
            tagLabel("smartschool-print-proxyprinter-label-2",
                    "smartschool-print-proxyprinter", keyWlk);
            tagInput("smartschool-print-proxyprinter-2", keyWlk);

            keyWlk = IConfigProp.Key.SMARTSCHOOL_2_SOAP_PRINT_PROXY_PRINTER_DUPLEX;
            tagLabel("smartschool-print-proxyprinter-duplex-label-2",
                    "smartschool-print-proxyprinter-duplex", keyWlk);
            tagInput("smartschool-print-proxyprinter-duplex-2", keyWlk);

            keyWlk = IConfigProp.Key.SMARTSCHOOL_2_SOAP_PRINT_PROXY_PRINTER_GRAYSCALE;
            tagLabel("smartschool-print-proxyprinter-grayscale-label-2",
                    "smartschool-print-proxyprinter-grayscale", keyWlk);
            tagInput("smartschool-print-proxyprinter-grayscale-2", keyWlk);

            keyWlk = IConfigProp.Key.SMARTSCHOOL_2_SOAP_PRINT_PROXY_PRINTER_GRAYSCALE_DUPLEX;
            tagLabel("smartschool-print-proxyprinter-grayscale-duplex-label-2",
                    "smartschool-print-proxyprinter-grayscale-duplex", keyWlk);
            tagInput("smartschool-print-proxyprinter-grayscale-duplex-2",
                    keyWlk);

            keyWlk = IConfigProp.Key.SMARTSCHOOL_2_SOAP_PRINT_CHARGE_TO_STUDENTS;
            tagLabel("smartschool-print-charge-to-students-label-2",
                    "smartschool-print-charge-to-students", keyWlk);
            tagCheckbox("smartschool-print-charge-to-students-2", keyWlk);

            //
            keyWlk = IConfigProp.Key.SMARTSCHOOL_2_SOAP_PRINT_NODE_ENABLE;
            tagLabel("smartschool-print-node-enable-label-2",
                    "smartschool-print-node-enable", keyWlk);
            tagCheckbox("smartschool-print-node-enable-2", keyWlk);

            keyWlk = IConfigProp.Key.SMARTSCHOOL_2_SOAP_PRINT_NODE_PROXY_ENABLE;
            tagLabel("smartschool-print-node-proxy-enable-label-2",
                    "smartschool-print-node-proxy-enable", keyWlk);
            tagCheckbox("smartschool-print-node-proxy-enable-2", keyWlk);

            keyWlk = IConfigProp.Key.SMARTSCHOOL_2_SOAP_PRINT_NODE_ID;
            tagLabel("smartschool-print-node-id-label-2",
                    "smartschool-print-node-id", keyWlk);
            tagInput("smartschool-print-node-id-2", keyWlk);

            keyWlk = IConfigProp.Key.SMARTSCHOOL_2_SOAP_PRINT_NODE_PROXY_ENDPOINT_URL;
            tagLabel("smartschool-print-node-proxy-endpoint-label-2",
                    "smartschool-print-node-proxy-endpoint", keyWlk);
            tagInput("smartschool-print-node-proxy-endpoint-2", keyWlk);

            //
            keyWlk = IConfigProp.Key.SMARTSCHOOL_USER_INSERT_LAZY_PRINT;
            tagLabel("smartschool-on-demand-user-creation-label",
                    "smartschool-on-demand-user-creation", keyWlk);
            tagCheckbox("smartschool-on-demand-user-creation", keyWlk);

            //
            labelledCheckbox("smartschool-papercut-enable",
                    IConfigProp.Key.SMARTSCHOOL_PAPERCUT_ENABLE);

            //
            if (SmartschoolPrinter.isBlocked()) {
                msgKey = "blocked";
                cssColor = MarkupHelper.CSS_TXT_WARN;
            } else {
                msgKey = null;

                final CircuitStateEnum circuitState = ConfigManager
                        .getCircuitBreaker(
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

            labelWrk = helper.encloseLabel("smartschool-print-status", msgText,
                    true);

            MarkupHelper.modifyLabelAttr(labelWrk, "class", cssColor);

            labelWrk = helper.addCheckbox("flipswitch-smartschool-online",
                    SmartschoolPrinter.isOnline());

            setFlipswitchOnOffText(labelWrk);

        } else {
            helper.discloseLabel(headerId);
        }

        /*
         * PaperCut Integration.
         */
        labelledCheckbox("papercut-enable", IConfigProp.Key.PAPERCUT_ENABLE);

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
            throw new SpException(
                    "Oops we missed " + CircuitStateEnum.class.getSimpleName()
                            + " value [" + circuitState.toString() + "]");
        }

        if (msgKey == null) {
            msgText = "";
        } else {
            msgText = getLocalizer().getString(msgKey, this);
        }

        labelWrk = helper.encloseLabel("mailprint-status", msgText, true);
        MarkupHelper.modifyLabelAttr(labelWrk, "class", cssColor);

        labelWrk = helper.addCheckbox("flipswitch-mailprint-online",
                ImapPrinter.isOnline());
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
         * Google Cloud Print.
         */
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
        add(new Label("gcp-summary-printer-state",
                GcpPrinter.getState().toString()));

        boolean enabled = ConfigManager.isGcpEnabled();
        final GcpPrinter.State gcpStatus = GcpPrinter.getState();

        if (enabled && gcpStatus == GcpPrinter.State.ON_LINE) {
            cssColor = MarkupHelper.CSS_TXT_VALID;
        } else {
            cssColor = MarkupHelper.CSS_TXT_WARN;
        }

        labelWrk = new Label("gcp-summary-printer-state-display",
                GcpPrinter.localized(enabled, gcpStatus));
        labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_CLASS, cssColor));
        add(labelWrk);

        /*
         * Financial
         */

        // General

        try {
            final String creditLimit = BigDecimalUtil.localize(
                    cm.getConfigBigDecimal(
                            IConfigProp.Key.FINANCIAL_GLOBAL_CREDIT_LIMIT),
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
        final String jmcRemoteProcess;

        try {
            jmcRemoteProcess = InetUtils.getServerHostAddress() + ":"
                    + JmxRemoteProperties.getPort();
        } catch (UnknownHostException e) {
            throw new SpException("Server IP address could not be found", e);
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

        //
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
        labelledCheckbox("proxyprint-delegate-enable",
                IConfigProp.Key.PROXY_PRINT_DELEGATE_ENABLE);

        labelledCheckbox("proxyprint-delegate-papercut-enable",
                IConfigProp.Key.PROXY_PRINT_DELEGATE_PAPERCUT_ENABLE);

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

        labelledCheckbox("print-in-clear-at-logout",
                IConfigProp.Key.WEBAPP_USER_LOGOUT_CLEAR_INBOX);

        labelledInput("print-in-expiry-mins",
                IConfigProp.Key.PRINT_IN_JOB_EXPIRY_MINS);

        labelledInput("print-in-expiry-signal-mins",
                IConfigProp.Key.WEBAPP_USER_PRINT_IN_JOB_EXPIRY_SIGNAL_MINS);

        /*
         *
         */
        helper.labelledCheckbox("pagometer-reset-users",
                "pagometer-reset-users", false);
        helper.labelledCheckbox("pagometer-reset-queues",
                "pagometer-reset-queues", false);
        helper.labelledCheckbox("pagometer-reset-printers",
                "pagometer-reset-printers", false);
        helper.labelledCheckbox("pagometer-reset-dashboard",
                "pagometer-reset-dashboard", false);

        /*
         *
         */
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
        this.setReadOnlyAccess(helper, isReadOnlyAccess);

        if (!isReadOnlyAccess) {
            if (ACCESS_CONTROL_SERVICE.hasPermission(SpSession.get().getUser(),
                    ACLOidEnum.A_CONFIG_EDITOR, ACLPermissionEnum.READER)) {
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
        Label labelWrk = new Label(id);
        labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_ID,
                ConfigManager.instance().getConfigKey(key)));
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
        final boolean checked = ConfigManager.instance()
                .getConfigValue(configKey).equals(attrValue);

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
                "sect-papercut", "sect-gcp", "sect-mail-print",
                "sect-web-print", "sect-internet-print", "sect-proxy-print",
                "sect-eco-print", "sect-financial", "sect-backups",
                "sect-advanced" }) {
            helper.addTransparantDisabled(wicketId, readonly);
        }

        // Apply
        for (final String wicketId : new String[] { "btn-apply-auth",
                "btn-apply-internal-users", "btn-apply-user-source-group-apply",
                "btn-user-sync-apply", "btn-apply-user-create",
                "btn-apply-user-auth-mode-local", "btn-apply-smtp",
                "btn-apply-mail", "btn-apply-papercut", "btn-gcp-apply-enable",
                "btn-gcp-apply-notification", "btn-apply-imap",
                "btn-apply-webprint", "btn-apply-internetprint",
                "btn-apply-proxyprint", "btn-apply-ecoprint",
                "btn-apply-financial-general", "btn-apply-financial-pos",
                "btn-apply-financial-vouchers",
                "btn-apply-financial-user-transfers",
                "btn-apply-backup-automatic", "btn-apply-cliapp-auth",
                "btn-admin-pw-reset", "btn-jmx-pw-reset", "btn-apply-locale",
                "btn-apply-papersize", "btn-apply-report-fontfamily",
                "btn-apply-converters", "btn-apply-print-in",
                "btn-pagometer-reset" }) {
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

        // Refresh
        for (final String wicketId : new String[] { "btn-gcp-refresh" }) {
            if (readonly) {
                helper.discloseLabel(wicketId);
            } else {
                helper.addButton(wicketId, HtmlButtonEnum.REFRESH);
            }
        }

        // Misc ...
        helper.encloseLabel("btn-backup-now", localized("button-backup-now"),
                !readonly);

    }
}
