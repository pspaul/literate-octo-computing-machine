/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.wicket.AttributeModifier;
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
import org.savapage.core.dao.UserDao;
import org.savapage.core.dao.enums.AppLogLevelEnum;
import org.savapage.core.dao.enums.ReservedIppQueueEnum;
import org.savapage.core.dao.impl.DaoContextImpl;
import org.savapage.core.print.gcp.GcpPrinter;
import org.savapage.core.print.imap.ImapPrinter;
import org.savapage.core.print.proxy.ProxyPrintJobStatusMonitor;
import org.savapage.core.services.AppLogService;
import org.savapage.core.services.QueueService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.DateUtil;
import org.savapage.core.util.NumberUtil;
import org.savapage.ext.payment.PaymentGateway;
import org.savapage.ext.payment.PaymentGatewayException;
import org.savapage.ext.payment.bitcoin.BitcoinGateway;
import org.savapage.ext.smartschool.SmartschoolPrinter;
import org.savapage.server.WebApp;
import org.savapage.server.cometd.UserEventService;
import org.savapage.server.ext.ServerPluginManager;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.pages.MessageContent;
import org.savapage.server.pages.StatsEnvImpactPanel;
import org.savapage.server.pages.StatsPageTotalPanel;

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

    /**
     * .
     */
    private static final AppLogService APP_LOG_SERVICE =
            ServiceContext.getServiceFactory().getAppLogService();

    /**
     * .
     */
    private static final QueueService QUEUE_SERVICE =
            ServiceContext.getServiceFactory().getQueueService();

    /**
     * @param panelId
     *            The panel id.
     */
    public SystemStatusPanel(final String panelId) {
        super(panelId);
    }

    /**
     *
     */
    public void populate() {

        final MarkupHelper helper = new MarkupHelper(this);

        final ConfigManager cm = ConfigManager.instance();
        final MemberCard memberCard = MemberCard.instance();

        Label labelWrk;
        String cssColor;
        String msg;

        /*
         *
         */
        if (!cm.isAppReadyToUse()) {
            cssColor = MarkupHelper.CSS_TXT_ERROR;
            msg = getLocalizer().getString("sys-status-setup-needed", this);
        } else if (memberCard.isMembershipDesirable()) {
            cssColor = MarkupHelper.CSS_TXT_WARN;
            msg = MessageFormat.format(getLocalizer()
                    .getString("sys-status-membercard-missing", this),
                    CommunityDictEnum.MEMBER_CARD.getWord());
        } else {
            cssColor = MarkupHelper.CSS_TXT_VALID;
            msg = getLocalizer().getString("sys-status-ready", this);
        }

        labelWrk = new Label("sys-status", msg);

        labelWrk.add(new AttributeModifier("class", cssColor));
        add(labelWrk);

        //
        add(new Label("sys-uptime", DateUtil.formatDuration(
                ManagementFactory.getRuntimeMXBean().getUptime())));

        //
        final UserDao userDAO = ServiceContext.getDaoContext().getUserDao();

        add(new Label("user-count",
                helper.localizedNumber(userDAO.countActiveUsers())));

        /*
         * Mail Print
         */
        add(new Label("prompt-mail-print",
                CommunityDictEnum.MAIL_PRINT.getWord(getLocale())));

        String msgKey;
        String msgText;

        final boolean isMailPrintEnabled = ConfigManager.isPrintImapEnabled();

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
            MarkupHelper.modifyLabelAttr(labelWrk, "class", cssColor);

            labelWrk = helper.addCheckbox("flipswitch-mailprint-online",
                    ImapPrinter.isOnline());
            setFlipswitchOnOffText(labelWrk);

        } else {

            helper.discloseLabel("mailprint-status");
        }

        /*
         * Google Cloud Print
         */
        if (ConfigManager.isGcpEnabled()) {

            final GcpPrinter.State gcpStatus = GcpPrinter.getState();
            final boolean isGcpOnline = GcpPrinter.State.ON_LINE == gcpStatus;

            msgKey = null;

            switch (gcpStatus) {
            case NOT_CONFIGURED:
                msgKey = "not-configured";
                cssColor = MarkupHelper.CSS_TXT_WARN;
                break;
            case NOT_FOUND:
                msgKey = "not-found";
                cssColor = MarkupHelper.CSS_TXT_WARN;
                break;
            case OFF_LINE:
            case ON_LINE:
                break;
            default:
                throw new SpException(
                        "Unhandled GcpPrinter.Status [" + gcpStatus + "]");
            }

            if (msgKey == null) {
                msgText = "";
            } else {
                msgText = getLocalizer().getString(msgKey, this);
            }

            labelWrk = helper.encloseLabel("gcp-status", msgText, true);

            if (msgKey != null) {
                MarkupHelper.modifyLabelAttr(labelWrk, "class", cssColor);
            }

            labelWrk = helper.addCheckbox("flipswitch-gcp-online", isGcpOnline);
            setFlipswitchOnOffText(labelWrk);

        } else {
            helper.discloseLabel("gcp-status");
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
         * Smartschool Print.
         */
        if (ConfigManager.isSmartSchoolPrintActiveAndEnabled()) {

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
            helper.discloseLabel("smartschool-print-status");
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
            labelWrk.add(new AttributeModifier("class",
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
         *
         */
        add(new Label("client-sessions",
                helper.localizedNumber(UserEventService.getClientAppCount())));

        add(new Label("web-sessions",
                helper.localizedNumber(WebApp.getAuthUserSessionCount())));

        /*
         * Community Membership
         */
        //
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
                memberStat = CommunityDictEnum.FELLOW.getWord();
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

        case WRONG_VERSION_WITH_GRACE:
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
        labelWrk.add(new AttributeModifier("class", cssColor));
        add(labelWrk);

        //
        labelWrk = new Label("membership-status", memberStat);
        labelWrk.add(new AttributeModifier("class", cssColor));
        add(labelWrk);

        //
        labelWrk = new Label("membership-participants",
                helper.localizedNumber(memberCard.getMemberParticipants()));
        labelWrk.add(new AttributeModifier("class", cssColor));
        add(labelWrk);

        //
        String enclosedValue = null;

        if (memberCard.getExpirationDate() != null) {
            enclosedValue =
                    helper.localizedDate(memberCard.getExpirationDate());
        }
        labelWrk = MarkupHelper.createEncloseLabel("membership-valid-till",
                enclosedValue, enclosedValue != null);

        labelWrk.add(new AttributeModifier("class", cssColor));
        add(labelWrk);

        //
        if (memberCard.getDaysTillExpiry() != null) {
            enclosedValue = helper.localizedNumber(
                    memberCard.getDaysTillExpiry().longValue());
        }
        labelWrk = MarkupHelper.createEncloseLabel(
                "membership-valid-days-remaining", enclosedValue,
                enclosedValue != null);
        labelWrk.add(new AttributeModifier("class", cssColor));
        add(labelWrk);

        /*
         * Errors.
         */
        final Date oneHourAgo = DateUtils.addHours(new Date(), -1);

        final long errors = APP_LOG_SERVICE.countErrors(oneHourAgo);
        Label labelErr =
                new Label("error-count", helper.localizedNumber(errors));
        if (errors > 0) {
            labelErr.add(
                    new AttributeModifier("class", MarkupHelper.CSS_TXT_ERROR));
        }
        add(labelErr);

        /*
         * Warnings.
         */
        final long warnings = APP_LOG_SERVICE.countWarnings(oneHourAgo);
        Label labelWarn =
                new Label("warning-count", helper.localizedNumber(warnings));
        if (warnings > 0) {
            labelWarn.add(
                    new AttributeModifier("class", MarkupHelper.CSS_TXT_WARN));
        }
        add(labelWarn);

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
                    NumberUtil.humanReadableByteCount(
                            Runtime.getRuntime().maxMemory(), true),
                    NumberUtil.humanReadableByteCount(
                            Runtime.getRuntime().totalMemory(), true),
                    NumberUtil.humanReadableByteCount(
                            Runtime.getRuntime().freeMemory(), true));
        } else {
            memoryInfo = "";
        }

        helper.encloseLabel("jvm-memory", memoryInfo, showTechInfo);

        /*
         * Threads info.
         */
        final String threadInfo;

        if (showTechInfo) {
            threadInfo = String.format("%d", Thread.activeCount());
        } else {
            threadInfo = "";
        }

        helper.encloseLabel("threads-info", threadInfo, showTechInfo);

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

        if (showTechInfo) {
            size = ProxyPrintJobStatusMonitor.getPendingJobs();
        }

        helper.encloseLabel("proxy-print-queue-size", String.valueOf(size),
                showTechInfo);

        /*
         * Page Totals.
         */
        StatsPageTotalPanel pageTotalPanel =
                new StatsPageTotalPanel("stats-pages-total");
        add(pageTotalPanel);
        pageTotalPanel.populate();

        /*
         * Financial Totals.
         */
        StatsFinancialPanel finTotalPanel =
                new StatsFinancialPanel("stats-financial-totals");
        add(finTotalPanel);
        finTotalPanel.populate();

        /*
         * Environmental Impact.
         */
        Double esu = (double) (cm.getConfigLong(Key.STATS_TOTAL_PRINT_OUT_ESU)
                / 100);
        StatsEnvImpactPanel envImpactPanel =
                new StatsEnvImpactPanel("environmental-impact");
        add(envImpactPanel);
        envImpactPanel.populate(esu);

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

    }

    /**
     * Sets the on/off texts for a Flipswitch.
     *
     * @param label
     *            The flipswitch {@link Label}.
     */
    private void setFlipswitchOnOffText(final Label label) {

        MarkupHelper.modifyLabelAttr(label, "data-on-text",
                getLocalizer().getString("flipswitch-on", this));
        MarkupHelper.modifyLabelAttr(label, "data-off-text",
                getLocalizer().getString("flipswitch-off", this));
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

                    private static final String URL_SAVAPAGE_NEWS =
                            "http://www.savapage.org/"
                                    //
                                    + "news-embedded.php?embedded=y"
                    //
                                    + "&v_major=" + VersionInfo.VERSION_A_MAJOR
                    //
                                    + "&v_minor=" + VersionInfo.VERSION_B_MINOR
                    //
                                    + "&v_revision="
                                    + VersionInfo.VERSION_C_REVISION
                    //
                                    + "&v_build=" + VersionInfo.VERSION_D_BUILD;

                    @Override
                    public Object execute(final CircuitBreaker circuitBreaker) {

                        String html = null;
                        BufferedReader reader = null;
                        HttpGet request = null;

                        try {
                            final String url = URL_SAVAPAGE_NEWS;

                            final HttpClient client =
                                    HttpClientBuilder.create().build();

                            request = new HttpGet(url);

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

                            IOUtils.closeQuietly(reader);
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
