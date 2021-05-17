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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.EnumSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.savapage.core.SpException;
import org.savapage.core.VersionInfo;
import org.savapage.core.circuitbreaker.CircuitBreaker;
import org.savapage.core.circuitbreaker.CircuitBreakerException;
import org.savapage.core.circuitbreaker.CircuitBreakerOperation;
import org.savapage.core.circuitbreaker.CircuitDamagingException;
import org.savapage.core.circuitbreaker.CircuitStateEnum;
import org.savapage.core.circuitbreaker.CircuitTrippingException;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.community.MemberCard;
import org.savapage.core.config.CircuitBreakerEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.config.SetupNeededEnum;
import org.savapage.core.config.SslCertInfo;
import org.savapage.core.config.SystemStatusEnum;
import org.savapage.core.dao.enums.AppLogLevelEnum;
import org.savapage.core.dao.enums.ReservedIppQueueEnum;
import org.savapage.core.dao.impl.DaoContextImpl;
import org.savapage.core.dto.UserHomeStatsDto;
import org.savapage.core.i18n.AdverbEnum;
import org.savapage.core.i18n.JobTicketNounEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.i18n.PrintOutNounEnum;
import org.savapage.core.i18n.SystemModeEnum;
import org.savapage.core.print.imap.MailPrinter;
import org.savapage.core.print.proxy.ProxyPrintJobStatusMonitor;
import org.savapage.core.services.AppLogService;
import org.savapage.core.services.JobTicketService;
import org.savapage.core.services.QueueService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.system.SystemFileDescriptorCount;
import org.savapage.core.system.SystemInfo;
import org.savapage.core.util.DateUtil;
import org.savapage.core.util.DeadlockedThreadsDetector;
import org.savapage.core.util.IOHelper;
import org.savapage.core.util.LocaleHelper;
import org.savapage.core.util.NumberUtil;
import org.savapage.ext.payment.PaymentGateway;
import org.savapage.ext.payment.PaymentGatewayException;
import org.savapage.ext.payment.bitcoin.BitcoinGateway;
import org.savapage.lib.pgp.PGPPublicKeyInfo;
import org.savapage.server.WebApp;
import org.savapage.server.cometd.UserEventService;
import org.savapage.server.ext.ServerPluginManager;
import org.savapage.server.pages.JobTicketQueueInfoPanel;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.pages.MessageContent;
import org.savapage.server.pages.StatsEnvImpactPanel;
import org.savapage.server.pages.StatsPageTotalPanel;
import org.savapage.server.pages.StatsPrintInTotalPanel;
import org.savapage.server.pages.TooltipPanel;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class SystemStatusPanel extends Panel {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Duration after which news expires.
     */
    private static final long RETRIEVE_NEWS_TIME_EXPIRY =
            12 * DateUtil.DURATION_MSEC_HOUR;

    /**
     * Last time the news was retrieved.
     */
    private static long retrieveNewsTime = 0L;

    /**
     * Last time news HTML.
     */
    private static String retrieveNewsHtml;

    /** */
    private static final AppLogService APP_LOG_SERVICE =
            ServiceContext.getServiceFactory().getAppLogService();

    /** */
    private static final QueueService QUEUE_SERVICE =
            ServiceContext.getServiceFactory().getQueueService();

    /** */
    private static final JobTicketService TICKET_SERVICE =
            ServiceContext.getServiceFactory().getJobTicketService();

    /** */
    private static final String WID_ENV_IMPACT = "environmental-impact";

    /** */
    private static final String WID_PANEL_JOB_TICKETS_QUEUE =
            "job-tickets-queue-panel";

    /** */
    private static final String WID_PANEL_USERHOME_STATS =
            "userhome-stats-panel";

    /** */
    private static final String WID_TOOLTIP_USERHOME_STATS =
            "tooltip-userhome-stats";

    /** */
    private static final String WID_BTN_SMTP = "btn-setup-smtp";

    /** */
    private static final String WID_BTN_CURRENCY = "btn-setup-currency";

    /**
     * @param panelId
     *            The panel id.
     */
    public SystemStatusPanel(final String panelId) {
        super(panelId);
    }

    /**
     *
     * @param helper
     *            Markup helper.
     * @param cm
     *            ConfigManager.
     * @param isSetupNeeded
     *            {@code true} if setup is needed.
     */
    private void handleSetupNeeded(final MarkupHelper helper,
            final ConfigManager cm, final boolean isSetupNeeded) {

        if (!isSetupNeeded) {
            helper.discloseLabel(WID_BTN_SMTP);
            helper.discloseLabel(WID_BTN_CURRENCY);
            return;
        }

        final EnumSet<SetupNeededEnum> needed = cm.getReadyToUseSetupNeeded();
        helper.encloseLabel(WID_BTN_SMTP,
                SetupNeededEnum.MAIL.uiText(getLocale()),
                needed.contains(SetupNeededEnum.MAIL));
        helper.encloseLabel(WID_BTN_CURRENCY,
                SetupNeededEnum.CURRENCY.uiText(getLocale()),
                needed.contains(SetupNeededEnum.CURRENCY));
    }

    /**
     *
     * @param hasEditorAccess
     *            {@code true} If editor access.
     */
    public void populate(final boolean hasEditorAccess) {

        final MarkupHelper helper = new MarkupHelper(this);
        final LocaleHelper localeHelper = new LocaleHelper(getLocale());

        final ConfigManager cm = ConfigManager.instance();
        final MemberCard memberCard = MemberCard.instance();

        Label labelWrk;
        String cssColor;
        String msg;

        //
        final SystemStatusEnum systemStatus = cm.getSystemStatus();

        final boolean isSetupNeeded = systemStatus == SystemStatusEnum.SETUP;
        this.handleSetupNeeded(helper, cm, isSetupNeeded);

        if (isSetupNeeded) {
            cssColor = MarkupHelper.CSS_TXT_ERROR;
            msg = getLocalizer().getString("sys-status-setup-needed", this);
        } else if (systemStatus == SystemStatusEnum.UNAVAILABLE) {
            cssColor = MarkupHelper.CSS_TXT_WARN;
            msg = getLocalizer().getString("sys-status-not-available", this);
        } else if (memberCard.isMembershipDesirable()) {
            cssColor = MarkupHelper.CSS_TXT_WARN;
            msg = MessageFormat.format(getLocalizer()
                    .getString("sys-status-membercard-missing", this),
                    CommunityDictEnum.MEMBER_CARD.getWord());
        } else {
            cssColor = MarkupHelper.CSS_TXT_VALID;
            msg = getLocalizer().getString("sys-status-ready", this);
        }

        //
        labelWrk = new Label("sys-status", msg);
        labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_CLASS, cssColor));
        add(labelWrk);

        //
        add(new Label("sys-mode-prompt", NounEnum.MODE.uiText(getLocale())));

        final SystemModeEnum systemMode = ConfigManager.getSystemMode();

        if (systemMode == SystemModeEnum.MAINTENANCE) {
            cssColor = MarkupHelper.CSS_TXT_WARN;
        } else {
            cssColor = MarkupHelper.CSS_TXT_VALID;
        }

        if (hasEditorAccess) {
            MarkupHelper.modifyLabelAttr(
                    helper.addLabel("sys-mode-btn",
                            systemMode.uiText(getLocale())),
                    MarkupHelper.ATTR_CLASS, cssColor);
            helper.discloseLabel("sys-mode");
        } else {
            helper.discloseLabel("sys-mode-btn");
            MarkupHelper.modifyLabelAttr(
                    helper.addLabel("sys-mode", systemMode.uiText(getLocale())),
                    MarkupHelper.ATTR_CLASS, cssColor);
        }

        //
        add(new Label("sys-uptime",
                DateUtil.formatDuration(SystemInfo.getUptime())));

        /*
         * Users
         */
        helper.addLabel("users-prompt",
                NounEnum.USER.uiText(getLocale(), true));

        final UserHomeStatsPanel userHomePanel =
                new UserHomeStatsPanel(WID_PANEL_USERHOME_STATS);
        add(userHomePanel);

        final String json =
                ConfigManager.instance().getConfigValue(Key.STATS_USERHOME);

        final UserHomeStatsDto dto = UserHomeStatsDto.create(json);
        userHomePanel.populate(dto, hasEditorAccess);
        this.addSafePagesTooltip(helper, localeHelper, dto);

        /*
         * Job Tickets
         */
        helper.addLabel("job-tickets-prompt",
                JobTicketNounEnum.TICKET.uiText(getLocale(), true));

        final JobTicketQueueInfoPanel jobticketPanel =
                new JobTicketQueueInfoPanel(WID_PANEL_JOB_TICKETS_QUEUE);
        add(jobticketPanel);
        jobticketPanel.populate(TICKET_SERVICE.getJobTicketQueueSize() == 0);

        /*
         * Mail Print
         */
        add(new Label("prompt-mail-print",
                CommunityDictEnum.MAIL_PRINT.getWord(getLocale())));

        String msgKey;
        String msgText;

        final boolean isMailPrintEnabled = ConfigManager.isMailPrintEnabled();

        if (isMailPrintEnabled) {

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
            MarkupHelper.modifyLabelAttr(labelWrk, MarkupHelper.ATTR_CLASS,
                    cssColor);

            labelWrk = helper.addCheckbox("flipswitch-mailprint-online",
                    MailPrinter.isOnline());
            setFlipswitchOnOffText(labelWrk);

        } else {

            helper.discloseLabel("mailprint-status");
        }

        /*
         * OpenPGP
         */
        final PGPPublicKeyInfo pgpPubKey = cm.getPGPPublicKeyInfo();
        if (pgpPubKey != null) {
            helper.addLabel("pgp-uid", pgpPubKey.getUids().get(0).toString());
            final TooltipPanel tooltip = new TooltipPanel("tooltip-pgp");
            tooltip.populate(
                    String.format("<p>KeyID: %s</p><p>Fingerprint: %s</p>",
                            pgpPubKey.formattedKeyID(),
                            pgpPubKey.formattedFingerPrint()),
                    false);
            tooltip.setEscapeModelStrings(false);
            add(tooltip);
        } else {
            helper.discloseLabel("pgp-uid");
        }

        /*
         * Payment Gateways
         */
        final ServerPluginManager pluginMgr = WebApp.get().getPluginManager();

        /*
         * Bitcoin Gateway?
         */
        final BitcoinGateway bitcoinPlugin = pluginMgr.getBitcoinGateway();

        if (bitcoinPlugin == null) {

            helper.discloseLabel("payment-plugin-bitcoin");

        } else {

            helper.encloseLabel("payment-plugin-bitcoin",
                    bitcoinPlugin.getName(), true);

            labelWrk = helper.addCheckbox(
                    "flipswitch-payment-plugin-bitcoin-online",
                    bitcoinPlugin.isOnline());
            setFlipswitchOnOffText(labelWrk);
        }

        /*
         * External Gateway?
         */
        try {
            final PaymentGateway externalPlugin =
                    pluginMgr.getExternalPaymentGateway();

            if (externalPlugin == null) {
                helper.discloseLabel("payment-plugin-generic");
            } else {
                helper.encloseLabel("payment-plugin-generic",
                        externalPlugin.getName(), true);
                labelWrk = helper.addCheckbox(
                        "flipswitch-payment-plugin-generic-online",
                        externalPlugin.isOnline());

                setFlipswitchOnOffText(labelWrk);
            }

        } catch (PaymentGatewayException e) {
            setResponsePage(
                    new MessageContent(AppLogLevelEnum.ERROR, e.getMessage()));
            return;
        }

        /*
         * Proxy Print Service (CUPS connection status)
         */
        add(new Label("prompt-proxy-print",
                CommunityDictEnum.PROXY_PRINT.getWord(getLocale())));

        final CircuitBreaker circuit = ConfigManager.getCircuitBreaker(
                CircuitBreakerEnum.CUPS_LOCAL_IPP_CONNECTION);

        final String clazz;

        if (circuit.getCircuitState() == CircuitStateEnum.CLOSED) {
            clazz = null;
        } else {

            switch (circuit.getCircuitState()) {
            case CLOSED:
                clazz = MarkupHelper.CSS_TXT_VALID;
                break;

            case DAMAGED:
                clazz = MarkupHelper.CSS_TXT_ERROR;
                break;

            case HALF_OPEN:
            case OPEN:
            default:
                clazz = MarkupHelper.CSS_TXT_WARN;
                break;
            }
        }
        //
        labelWrk = helper.addCheckbox("flipswitch-proxyprint-online",
                clazz == null);
        setFlipswitchOnOffText(labelWrk);

        //
        if (clazz == null) {
            helper.discloseLabel("cups-connection");
        } else {
            labelWrk = helper.encloseLabel("cups-connection",
                    circuit.getCircuitState().uiText(getLocale()), true);
            labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_CLASS,
                    String.format("%s %s", MarkupHelper.CSS_TXT_WRAP, clazz)));
        }

        /*
         * Web Print
         */
        add(new Label("prompt-web-print",
                CommunityDictEnum.WEB_PRINT.getWord(getLocale())));

        labelWrk = helper.addCheckbox("flipswitch-webprint-online",
                ConfigManager.isWebPrintEnabled());
        setFlipswitchOnOffText(labelWrk);
        add(labelWrk);

        /*
         * Internet Print
         */
        add(new Label("prompt-internet-print",
                CommunityDictEnum.INTERNET_PRINT.getWord(getLocale())));

        labelWrk = helper.addCheckbox("flipswitch-internetprint-online",
                QUEUE_SERVICE.isQueueEnabled(
                        ReservedIppQueueEnum.IPP_PRINT_INTERNET));
        setFlipswitchOnOffText(labelWrk);
        add(labelWrk);

        /*
         * RESTful Print
         */
        add(new Label("prompt-restful-print",
                CommunityDictEnum.RESTFUL_PRINT.getWord(getLocale())));

        labelWrk = helper.addCheckbox("flipswitch-restful-online",
                QUEUE_SERVICE.isQueueEnabled(ReservedIppQueueEnum.WEBSERVICE));
        setFlipswitchOnOffText(labelWrk);
        add(labelWrk);

        /*
         *
         */
        add(new Label("client-sessions", String.format("%s (%s) • web (client)",
                helper.localizedNumber(UserEventService.getUserWebAppCount()),
                helper.localizedNumber(UserEventService.getClientAppCount()))));

        /*
         * Community Membership
         */
        add(new Label("membership-status-prompt",
                CommunityDictEnum.MEMBERSHIP.getWord()));

        cssColor = MarkupHelper.CSS_TXT_COMMUNITY;

        final String memberStat;

        switch (memberCard.getStatus()) {

        case EXCEEDED:
            cssColor = MarkupHelper.CSS_TXT_WARN;
            memberStat = getLocalizer().getString("membership-status-exceeded",
                    this);
            break;

        case EXPIRED:
            cssColor = MarkupHelper.CSS_TXT_WARN;
            memberStat =
                    getLocalizer().getString("membership-status-expired", this);
            break;

        case VISITOR:
            cssColor = MarkupHelper.CSS_TXT_COMMUNITY;
            memberStat = CommunityDictEnum.VISITOR.getWord();
            break;

        case VISITOR_EXPIRED:
            cssColor = MarkupHelper.CSS_TXT_WARN;
            memberStat = String.format("%s (%s)",
                    CommunityDictEnum.VISITOR.getWord(), getLocalizer()
                            .getString("membership-status-expired", this));

            break;

        case VISITOR_EDITION:
            cssColor = MarkupHelper.CSS_TXT_COMMUNITY;
            memberStat = CommunityDictEnum.VISITING_GUEST.getWord();
            break;

        case VALID:
            cssColor = MarkupHelper.CSS_TXT_COMMUNITY;
            if (memberCard.isVisitorCard()) {
                memberStat = CommunityDictEnum.VISITOR.getWord();
            } else {
                memberStat = CommunityDictEnum.CARD_HOLDER.getWord(getLocale());
            }
            break;

        case WRONG_MODULE:
            cssColor = MarkupHelper.CSS_TXT_ERROR;
            memberStat =
                    getLocalizer().getString("membership-status-wrong", this);
            break;

        case WRONG_COMMUNITY:
            cssColor = MarkupHelper.CSS_TXT_ERROR;
            memberStat =
                    getLocalizer().getString("membership-status-wrong", this);
            break;

        case WRONG_VERSION:
            cssColor = MarkupHelper.CSS_TXT_WARN;
            memberStat = getLocalizer()
                    .getString("membership-status-wrong-version", this);
            break;

        default:
            throw new SpException(CommunityDictEnum.MEMBERSHIP.getWord()
                    + " status [" + memberCard.getStatus() + "] not handled");
        }

        //
        add(new Label("membership-org-prompt",
                String.format("%s %s", CommunityDictEnum.COMMUNITY.getWord(),
                        CommunityDictEnum.MEMBER.getWord().toLowerCase())));
        labelWrk =
                new Label("membership-org", memberCard.getMemberOrganisation());
        labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_CLASS, cssColor));
        add(labelWrk);

        //
        labelWrk = new Label("membership-status", memberStat);
        labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_CLASS, cssColor));
        add(labelWrk);

        //
        labelWrk = new Label("membership-participants",
                helper.localizedNumber(memberCard.getMemberParticipants()));
        labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_CLASS, cssColor));
        add(labelWrk);

        //
        String enclosedValue = null;

        if (memberCard.getExpirationDate() != null) {
            enclosedValue =
                    helper.localizedDate(memberCard.getExpirationDate());
        }
        labelWrk = MarkupHelper.createEncloseLabel("membership-valid-till",
                enclosedValue, enclosedValue != null);

        labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_CLASS, cssColor));
        add(labelWrk);

        //
        if (memberCard.getDaysTillExpiry() != null) {
            enclosedValue = helper.localizedNumber(
                    memberCard.getDaysTillExpiry().longValue());
            if (memberCard.isDaysTillExpiryWarning()) {
                cssColor = MarkupHelper.CSS_TXT_WARN;
            }
        }
        labelWrk = MarkupHelper.createEncloseLabel(
                "membership-valid-days-remaining", enclosedValue,
                enclosedValue != null);
        labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_CLASS, cssColor));
        add(labelWrk);

        /*
         * Errors.
         */
        final Date oneHourAgo = DateUtils.addHours(new Date(), -1);

        final long errors = APP_LOG_SERVICE.countErrors(oneHourAgo);
        Label labelErr =
                new Label("error-count", helper.localizedNumber(errors));
        if (errors > 0) {
            labelErr.add(new AttributeModifier(MarkupHelper.ATTR_CLASS,
                    MarkupHelper.CSS_TXT_ERROR));
        }
        add(labelErr);

        /*
         * Warnings.
         */
        final long warnings = APP_LOG_SERVICE.countWarnings(oneHourAgo);
        Label labelWarn =
                new Label("warning-count", helper.localizedNumber(warnings));
        if (warnings > 0) {
            labelWarn.add(new AttributeModifier(MarkupHelper.ATTR_CLASS,
                    MarkupHelper.CSS_TXT_WARN));
        }
        add(labelWarn);

        /*
         * SSL Certificate
         */
        final SslCertInfo sslCert = ConfigManager.getSslCertInfo();
        String certText = null;
        String certClass = null;

        if (sslCert != null) {
            final Date dateRef = new Date();
            if (sslCert.isNotAfterWithinYear(dateRef)) {
                certText = localeHelper.getMediumDate(sslCert.getNotAfter());
                if (sslCert.isNotAfterWithinDay(dateRef)) {
                    certClass = "sp-txt-error";
                } else if (sslCert.isNotAfterWithinMonth(dateRef)) {
                    certClass = "sp-txt-warn";
                } else {
                    certClass = "sp-txt-info";
                }
            }
        }

        labelWrk =
                helper.encloseLabel("ssl-expiry", certText, certText != null);
        if (certClass != null) {
            labelWrk.add(AttributeAppender.append(MarkupHelper.ATTR_CLASS,
                    certClass));
        }

        /*
         * Show technical info?
         */
        final boolean showTechInfo = ConfigManager.instance()
                .isConfigValue(Key.WEBAPP_ADMIN_DASHBOARD_SHOW_TECH_INFO);

        /*
         * JVM memory.
         *
         * Max: the maximum amount of memory that the virtual machine will
         * attempt to use.
         *
         * Total: the total amount of memory currently available for current and
         * future objects.
         *
         * Free: an approximation to the total amount of memory currently
         * available for future allocated objects.
         */
        final String memoryInfo;

        if (showTechInfo) {
            memoryInfo = String.format("%s Max • %s Total • %s Free",
                    NumberUtil.humanReadableByteCountSI(getLocale(),
                            Runtime.getRuntime().maxMemory()),
                    NumberUtil.humanReadableByteCountSI(getLocale(),
                            Runtime.getRuntime().totalMemory()),
                    NumberUtil.humanReadableByteCountSI(getLocale(),
                            Runtime.getRuntime().freeMemory()));
        } else {
            memoryInfo = "";
        }

        helper.encloseLabel("jvm-memory", memoryInfo, showTechInfo);

        if (showTechInfo) {
            final TooltipPanel tooltip = new TooltipPanel("tooltip-jvm-memory");
            tooltip.populate(helper.localized("tooltip-jvm-memory"), true);
            add(tooltip);
        }

        //
        String webSessions = null;
        if (showTechInfo) {
            webSessions = String.format("%s (%s) • id (ip)",
                    helper.localizedNumber(WebApp.getAuthSessionCount()),
                    helper.localizedNumber(WebApp.getAuthIpAddrCount()));
        }
        helper.encloseLabel("web-sessions", webSessions, showTechInfo);

        //
        String openFiles = "-";
        if (showTechInfo) {
            helper.addLabel("open-files-prompt", "Open Files");
            final SystemFileDescriptorCount fileDesc =
                    SystemInfo.getFileDescriptorCount();
            if (fileDesc.getOpenFileCount() != null) {
                openFiles = helper.localizedNumber(fileDesc.getOpenFileCount());
            }
        }
        helper.encloseLabel("open-files", openFiles, showTechInfo);

        /*
         *
         */
        if (showTechInfo) {
            final File file = new File(File.separator);
            helper.addLabel("disk-space",
                    String.format("%s Total • %s Free",
                            NumberUtil.humanReadableByteCountSI(getLocale(),
                                    file.getTotalSpace()),
                            NumberUtil.humanReadableByteCountSI(getLocale(),
                                    file.getUsableSpace())));

            helper.addLabel("disk-space-prompt", NounEnum.DISK_SPACE);

        } else {
            helper.discloseLabel("disk-space");
        }

        /*
         * Threads info.
         */
        String threadInfo = "";
        String deadlockedThreads = "";

        if (showTechInfo) {
            threadInfo = String.format("%d", Thread.activeCount());
            final int count =
                    DeadlockedThreadsDetector.getDeadlockedThreadsCount();
            if (count > 0) {
                deadlockedThreads = String.format("(%d)", count);
            }
        }

        helper.encloseLabel("threads-info", threadInfo, showTechInfo);
        helper.encloseLabel("threads-info-deadlocks", deadlockedThreads,
                !deadlockedThreads.isEmpty());

        /*
         * Connections info: correct Dao/Service count for this connection.
         */
        final String connectionInfo;

        if (showTechInfo) {

            connectionInfo = String.format("%d (%d) • services (database)",
                    ServiceContext.getOpenCount(),
                    DaoContextImpl.getOpenCount());

        } else {
            connectionInfo = "";
        }

        helper.encloseLabel("connections-info", connectionInfo, showTechInfo);

        /*
         * Proxy Print
         */
        int size = 0;
        String printJobQueue = "";

        if (showTechInfo) {
            final long sizeDb = ServiceContext.getDaoContext().getPrintOutDao()
                    .countActiveCupsJobs(false);
            size = ProxyPrintJobStatusMonitor.getPendingJobs();

            printJobQueue =
                    String.format("%d (%d) • monitor (database)", size, sizeDb);
        }
        helper.encloseLabel("proxy-print-queue-size", printJobQueue,
                showTechInfo);

        /*
         * Page Totals.
         */
        final StatsPageTotalPanel pageTotalPanel =
                new StatsPageTotalPanel("stats-pages-total");
        add(pageTotalPanel);
        pageTotalPanel.populate();

        /*
         * Page Totals.
         */
        boolean showPrintIn = true;
        if (showPrintIn) {
            final StatsPrintInTotalPanel printInTotalPanel =
                    new StatsPrintInTotalPanel("stats-printin-total");
            add(printInTotalPanel);
            printInTotalPanel.populate();
        } else {
            helper.discloseLabel("stats-printin-total");
        }

        /*
         * Financial Totals.
         */
        final StatsFinancialPanel finTotalPanel =
                new StatsFinancialPanel("stats-financial-totals");
        add(finTotalPanel);
        finTotalPanel.populate();

        /*
         * Environmental Impact.
         */
        if (cm.isConfigValue(Key.WEBAPP_ADMIN_DASHBOARD_SHOW_ENV_INFO)) {
            final Double esu =
                    (double) (cm.getConfigLong(Key.STATS_TOTAL_PRINT_OUT_ESU)
                            / 100);
            final StatsEnvImpactPanel envImpactPanel =
                    new StatsEnvImpactPanel(WID_ENV_IMPACT);
            add(envImpactPanel);
            envImpactPanel.populate(esu);
        } else {
            helper.discloseLabel(WID_ENV_IMPACT);
        }

        /*
         * News from outside?
         */
        String html;

        if (ConfigManager.isConnectedToInternet()) {

            html = retrieveNews();

            if (html == null) {
                html = "<li>" + helper.localized("news-fetch-error") + "</li>";
                html = "<li><span class=\"" + MarkupHelper.CSS_TXT_ERROR + "\">"
                        + helper.localized("news-fetch-error") + "</span></li>";
            }

        } else {
            html = "<li>" + helper.localized("news-not-available") + "</li>";
        }

        Label labelNews = new Label("savapage-news", html);
        labelNews.setEscapeModelStrings(false);

        add(labelNews);

        helper.addTransparantDisabled("sect-services", !hasEditorAccess);
    }

    /**
     * Adds SafePages tooltip (or not).
     *
     * @param helper
     *            Markup Helper.
     * @param localeHelper
     *            Local helper.
     * @param dto
     *            SafePages info. If {@code null} tooltip is not displayed.
     */
    private void addSafePagesTooltip(final MarkupHelper helper,
            final LocaleHelper localeHelper, final UserHomeStatsDto dto) {

        if (dto == null) {
            helper.discloseLabel(WID_TOOLTIP_USERHOME_STATS);
            return;
        }
        final TooltipPanel tooltip =
                new TooltipPanel(WID_TOOLTIP_USERHOME_STATS);

        final StringBuilder html = new StringBuilder();
        html.append("<p><b>");
        if (dto.isCleaned()) {
            html.append(AdverbEnum.CLEANED.uiText(getLocale()));
        } else {
            html.append(AdverbEnum.CLEANABLE.uiText(getLocale()));
        }
        html.append("</b>: ");

        final UserHomeStatsDto.Stats stats = dto.getCleanup();

        final long nUsers = stats.getUsers().getCount();
        html.append(localeHelper.getNumber(nUsers));
        html.append("&nbsp;")
                .append(NounEnum.USER.uiText(getLocale(), nUsers != 1))
                .append(".");

        if (nUsers > 0) {

            UserHomeStatsDto.Scope scope = stats.getInbox();
            long nCount = scope.getCount();

            if (nCount > 0) {
                html.append(" ").append(localeHelper.getNumber(nCount));
                html.append("&nbsp;").append(
                        NounEnum.DOCUMENT.uiText(getLocale(), nCount != 1));
                html.append(": ")
                        .append(FileUtils
                                .byteCountToDisplaySize(scope.getSize())
                                .replace(" ", "&nbsp;"))
                        .append(".");
            }
            scope = stats.getOutbox();
            nCount = scope.getCount();
            if (nCount > 0) {
                html.append(" ").append(localeHelper.getNumber(nCount));
                html.append("&nbsp;").append(
                        PrintOutNounEnum.JOB.uiText(getLocale(), nCount != 1));
                html.append(": ")
                        .append(FileUtils
                                .byteCountToDisplaySize(scope.getSize())
                                .replace(" ", "&nbsp;"))
                        .append(".");
            }
        }

        html.append(" ");

        if (dto.getDuration() < DateUtil.DURATION_MSEC_SECOND) {
            html.append(dto.getDuration()).append("&nbsp;msec");
        } else {
            html.append(DurationFormatUtils
                    .formatDurationWords(dto.getDuration(), true, true)
                    .replace(" ", "&nbsp;"));
        }
        html.append(".");

        html.append("</p>");
        tooltip.populate(html.toString(), false);
        add(tooltip);
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
     * Retrieves the news as HTML from local cache or from the SavaPage site
     * when cache expired.
     *
     * @return HTML string with the news, or {@code null when no news is found}.
     */
    private synchronized String retrieveNews() {

        if (retrieveNewsTime > 0 && retrieveNewsTime
                + RETRIEVE_NEWS_TIME_EXPIRY > System.currentTimeMillis()) {
            return retrieveNewsHtml;
        }

        final CircuitBreakerOperation operation =
                new CircuitBreakerOperation() {

                    private static final int RETRIEVE_NEWS_TIMEOUT_MSEC = 3000;

                    /**
                     * @return The HTTP request.
                     */
                    private HttpGet getNewsRequest() {

                        final StringBuilder url = new StringBuilder();

                        url.append(CommunityDictEnum.SAVAPAGE_WWW_DOT_ORG_URL
                                .getWord());
                        url.append("/news-embedded.php?embedded=y");
                        url.append("&v_major=")
                                .append(VersionInfo.VERSION_A_MAJOR);
                        url.append("&v_minor=")
                                .append(VersionInfo.VERSION_B_MINOR);
                        url.append("&v_revision=")
                                .append(VersionInfo.VERSION_C_REVISION)
                                .append(VersionInfo.VERSION_D_STATUS);
                        url.append("&v_build=")
                                .append(VersionInfo.VERSION_E_BUILD);

                        return new HttpGet(url.toString());
                    }

                    @Override
                    public Object execute(final CircuitBreaker circuitBreaker) {

                        String html = null;
                        BufferedReader reader = null;
                        HttpGet request = null;

                        try {
                            final HttpClient client =
                                    HttpClientBuilder.create().build();

                            request = this.getNewsRequest();

                            request.setHeader(HttpHeaders.USER_AGENT,
                                    ConfigManager.getAppNameVersion());

                            request.setConfig(RequestConfig.custom()
                                    .setConnectTimeout(
                                            RETRIEVE_NEWS_TIMEOUT_MSEC)
                                    .setSocketTimeout(
                                            RETRIEVE_NEWS_TIMEOUT_MSEC)
                                    .setConnectionRequestTimeout(
                                            RETRIEVE_NEWS_TIMEOUT_MSEC)
                                    .build());

                            final HttpResponse response =
                                    client.execute(request);

                            reader = new BufferedReader(new InputStreamReader(
                                    response.getEntity().getContent()));

                            final StringBuilder htmlBuffer =
                                    new StringBuilder();

                            String line;

                            while ((line = reader.readLine()) != null) {
                                htmlBuffer.append(line);
                            }

                            // throw new IOException("Circuit tripping test");

                            html = htmlBuffer.toString();

                        } catch (MalformedURLException e) {

                            throw new CircuitDamagingException(e);

                        } catch (UnknownHostException e) {
                            /*
                             * This exception occurs when DNS cannot be reached.
                             * Unfortunately this exception takes MUCH longer
                             * (about 20 seconds) to occur.
                             */
                            throw new CircuitTrippingException(e);

                        } catch (IOException e) {

                            throw new CircuitTrippingException(e);

                        } catch (Exception e) {

                            throw new CircuitDamagingException(e);

                        } finally {

                            /*
                             * Mantis #487: release the connection.
                             */
                            if (request != null) {
                                request.reset();
                            }

                            IOHelper.closeQuietly(reader);
                        }
                        return html;
                    }

                };

        final CircuitBreaker breaker = ConfigManager
                .getCircuitBreaker(CircuitBreakerEnum.INTERNET_URL_CONNECTION);

        String html;

        try {
            html = (String) breaker.execute(operation);
        } catch (InterruptedException | CircuitBreakerException e) {
            html = null;
        }

        retrieveNewsTime = System.currentTimeMillis();
        retrieveNewsHtml = html;

        return retrieveNewsHtml;
    }

}
