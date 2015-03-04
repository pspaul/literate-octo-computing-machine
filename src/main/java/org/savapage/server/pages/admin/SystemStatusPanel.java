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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Date;

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
import org.savapage.core.dao.impl.DaoContextImpl;
import org.savapage.core.print.gcp.GcpPrinter;
import org.savapage.core.print.proxy.ProxyPrintJobStatusMonitor;
import org.savapage.core.services.AppLogService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.NumberUtil;
import org.savapage.server.WebApp;
import org.savapage.server.cometd.UserEventService;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.pages.StatsEnvImpactPanel;
import org.savapage.server.pages.StatsPageTotalPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Datraverse B.V.
 */
public class SystemStatusPanel extends Panel {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(SystemStatusPanel.class);

    /**
     *
     */
    private static final AppLogService APP_LOG_SERVICE = ServiceContext
            .getServiceFactory().getAppLogService();

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
    /**
     *
     */
    public final void populate() {

        final MarkupHelper helper = new MarkupHelper(this);

        final ConfigManager cm = ConfigManager.instance();
        final MemberCard memberCard = MemberCard.instance();

        Label labelWrk;
        String cssColor;
        String msgKey;

        /*
         *
         */
        if (!cm.isAppReadyToUse()) {
            cssColor = MarkupHelper.CSS_TXT_ERROR;
            msgKey = "sys-status-setup-needed";
        } else if (memberCard.isAdminAppBlocked()) {
            cssColor = MarkupHelper.CSS_TXT_WARN;
            msgKey = "sys-status-restricted";
        } else {
            cssColor = MarkupHelper.CSS_TXT_VALID;
            msgKey = "sys-status-ready";
        }

        labelWrk =
                new Label("sys-status", getLocalizer().getString(msgKey, this));
        labelWrk.add(new AttributeModifier("class", cssColor));
        add(labelWrk);

        /*
         *
         */
        add(new Label("sys-uptime", formatDuration(ManagementFactory
                .getRuntimeMXBean().getUptime())));

        /*
         *
         */
        final UserDao userDAO = ServiceContext.getDaoContext().getUserDao();

        add(new Label("user-count", helper.localizedNumber(userDAO
                .countActiveUsers())));

        /*
         * Mail Print
         */
        if (ConfigManager.isPrintImapEnabled()) {

            msgKey = "enabled";
            cssColor = MarkupHelper.CSS_TXT_VALID;

            final CircuitStateEnum circuitState =
                    ConfigManager.getCircuitBreaker(
                            CircuitBreakerEnum.MAILPRINT_CONNECTION)
                            .getCircuitState();

            switch (circuitState) {

            case CLOSED:
                msgKey = "circuit-closed";
                cssColor = MarkupHelper.CSS_TXT_VALID;
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

        } else {
            msgKey = "disabled";
            cssColor = MarkupHelper.CSS_TXT_WARN;
        }
        labelWrk =
                new Label("mailprint-enabled", getLocalizer().getString(msgKey,
                        this));
        labelWrk.add(new AttributeModifier("class", cssColor));
        add(labelWrk);

        /*
         * Web Print
         */
        if (ConfigManager.isWebPrintEnabled()) {
            msgKey = "online";
            cssColor = MarkupHelper.CSS_TXT_VALID;
        } else {
            msgKey = "offline";
            cssColor = MarkupHelper.CSS_TXT_WARN;
        }
        labelWrk =
                new Label("webprint-enabled", getLocalizer().getString(msgKey,
                        this));
        labelWrk.add(new AttributeModifier("class", cssColor));
        add(labelWrk);

        /*
         * Google Cloud Print
         */
        if (ConfigManager.isGcpEnabled()) {

            final GcpPrinter.State gcpStatus = GcpPrinter.getState();

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
                msgKey = "offline";
                cssColor = MarkupHelper.CSS_TXT_WARN;
                break;
            case ON_LINE:
                msgKey = "online";
                cssColor = MarkupHelper.CSS_TXT_VALID;
                break;
            default:
                throw new SpException("Unhandled GcpPrinter.Status ["
                        + gcpStatus + "]");
            }

        } else {
            msgKey = "disabled";
            cssColor = MarkupHelper.CSS_TXT_WARN;
        }

        labelWrk =
                new Label("gcp-enabled", getLocalizer().getString(msgKey, this));
        labelWrk.add(new AttributeModifier("class", cssColor));
        add(labelWrk);

        /*
         * SmartSchool Print
         */
        if (ConfigManager.isSmartSchoolPrintModuleActivated()) {

            if (ConfigManager.isSmartSchoolPrintEnabled()) {

                msgKey = "enabled";
                cssColor = MarkupHelper.CSS_TXT_VALID;

                final CircuitStateEnum circuitState =
                        ConfigManager.getCircuitBreaker(
                                CircuitBreakerEnum.SMARTSCHOOL_CONNECTION)
                                .getCircuitState();

                switch (circuitState) {

                case CLOSED:
                    msgKey = "circuit-closed";
                    cssColor = MarkupHelper.CSS_TXT_VALID;
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

            } else {
                msgKey = "disabled";
                cssColor = MarkupHelper.CSS_TXT_WARN;
            }

            labelWrk =
                    helper.encloseLabel("smartschool-print-enabled",
                            getLocalizer().getString(msgKey, this), true);
            labelWrk.add(new AttributeModifier("class", cssColor));

        } else {
            helper.discloseLabel("smartschool-print-enabled");
        }

        /*
         *
         */
        add(new Label("client-sessions",
                helper.localizedNumber(UserEventService.getClientAppCount())));

        add(new Label("web-sessions", helper.localizedNumber(WebApp
                .getAuthUserSessionCount())));

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
            cssColor = MarkupHelper.CSS_TXT_ERROR;
            memberStat =
                    getLocalizer()
                            .getString("membership-status-exceeded", this);
            break;

        case EXPIRED:
            cssColor = MarkupHelper.CSS_TXT_ERROR;
            memberStat =
                    getLocalizer().getString("membership-status-expired", this);
            break;

        case VISITOR:
            cssColor = MarkupHelper.CSS_TXT_WARN;
            memberStat = CommunityDictEnum.VISITOR.getWord();
            break;

        case VISITOR_EXPIRED:
            cssColor = MarkupHelper.CSS_TXT_ERROR;
            memberStat =
                    String.format(
                            "%s (%s)",
                            CommunityDictEnum.VISITOR.getWord(),
                            getLocalizer().getString(
                                    "membership-status-expired", this));

            break;

        case VISITOR_EDITION:
            cssColor = MarkupHelper.CSS_TXT_WARN;
            memberStat = CommunityDictEnum.VISITOR_EDITION.getWord();
            break;

        case VALID:
            if (memberCard.isVisitorCard()) {
                cssColor = MarkupHelper.CSS_TXT_WARN;
                memberStat = CommunityDictEnum.VISITOR.getWord();
            } else {
                cssColor = MarkupHelper.CSS_TXT_COMMUNITY;
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
            cssColor = MarkupHelper.CSS_TXT_ERROR;
            memberStat =
                    getLocalizer().getString("membership-status-wrong-version",
                            this);
            break;

        case WRONG_VERSION_WITH_GRACE:
            cssColor = MarkupHelper.CSS_TXT_WARN;
            memberStat =
                    getLocalizer().getString("membership-status-wrong-version",
                            this);
            break;

        default:
            throw new SpException(CommunityDictEnum.MEMBERSHIP.getWord()
                    + " status ["
                    + memberCard.getStatus() + "] not handled");
        }

        //
        add(new Label("membership-org-prompt", String.format("%s %s",
                CommunityDictEnum.COMMUNITY.getWord(), CommunityDictEnum.MEMBER
                        .getWord().toLowerCase())));
        labelWrk =
                new Label("membership-org", memberCard.getMemberOrganisation());
        labelWrk.add(new AttributeModifier("class", cssColor));
        add(labelWrk);

        //
        labelWrk = new Label("membership-status", memberStat);
        labelWrk.add(new AttributeModifier("class", cssColor));
        add(labelWrk);

        //
        labelWrk =
                new Label("membership-participants",
                        helper.localizedNumber(memberCard
                                .getMemberParticipants()));
        labelWrk.add(new AttributeModifier("class", cssColor));
        add(labelWrk);

        //
        String enclosedValue = null;

        if (memberCard.getExpirationDate() != null) {
            enclosedValue =
                    helper.localizedDate(memberCard.getExpirationDate());
        }
        labelWrk =
                MarkupHelper.createEncloseLabel("membership-valid-till",
                        enclosedValue, enclosedValue != null);

        labelWrk.add(new AttributeModifier("class", cssColor));
        add(labelWrk);

        //
        if (memberCard.getDaysTillExpiry() != null) {
            enclosedValue =
                    helper.localizedNumber(memberCard.getDaysTillExpiry()
                            .longValue());
        }
        labelWrk =
                MarkupHelper.createEncloseLabel(
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
            labelErr.add(new AttributeModifier("class",
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
            labelWarn.add(new AttributeModifier("class",
                    MarkupHelper.CSS_TXT_WARN));
        }
        add(labelWarn);

        /*
         * Show technical info?
         */
        final boolean showTechInfo =
                ConfigManager.instance().isConfigValue(
                        Key.WEBAPP_ADMIN_DASHBOARD_SHOW_TECH_INFO);

        /*
         * JVM memory.
         *
         * max: the maximum amount of memory that the virtual machine will
         * attempt to use.
         *
         * total: the total amount of memory currently available for current and
         * future objects.
         *
         * free: an approximation to the total amount of memory currently
         * available for future allocated objects.
         */
        final String memoryInfo;

        if (showTechInfo) {
            memoryInfo =
                    String.format("%s max, %s total, %s free", NumberUtil
                            .humanReadableByteCount(Runtime.getRuntime()
                                    .maxMemory(), true), NumberUtil
                            .humanReadableByteCount(Runtime.getRuntime()
                                    .totalMemory(), true), NumberUtil
                            .humanReadableByteCount(Runtime.getRuntime()
                                    .freeMemory(), true));
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

            final int serviceCount = ServiceContext.getOpenCount() - 1;
            final int daoCount = DaoContextImpl.getOpenCount() - 1;

            if (serviceCount == daoCount) {
                connectionInfo = String.valueOf(daoCount);
            } else {
                connectionInfo =
                        String.format("%d (%d)", DaoContextImpl.getOpenCount(),
                                ServiceContext.getOpenCount());
            }
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
         * Environmental Impact.
         */
        Double esu =
                (double) (cm.getConfigLong(Key.STATS_TOTAL_PRINT_OUT_ESU) / 100);
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
                html =
                        "<li><span class=\"" + MarkupHelper.CSS_TXT_ERROR
                                + "\">" + helper.localized("news-fetch-error")
                                + "</span></li>";
            }

        } else {
            html = "<li>" + helper.localized("news-not-available") + "</li>";
        }

        Label labelNews = new Label("savapage-news", html);
        labelNews.setEscapeModelStrings(false);

        add(labelNews);

    }

    /**
     * Retrieves the news as HTML from the SavaPage site.
     *
     * @return HTML string with the news, or {@code null when no news is found}.
     */
    private String retrieveNews() {

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
                            // url = new URL("malformed://url-exception-test");

                            final String url = URL_SAVAPAGE_NEWS;

                            final HttpClient client =
                                    HttpClientBuilder.create().build();

                            request = new HttpGet(url);

                            request.setHeader(HttpHeaders.USER_AGENT,
                                    ConfigManager.getAppNameVersion());

                            request.setConfig(RequestConfig
                                    .custom()
                                    .setConnectTimeout(
                                            RETRIEVE_NEWS_TIMEOUT_MSEC)
                                    .setSocketTimeout(
                                            RETRIEVE_NEWS_TIMEOUT_MSEC).build());

                            final HttpResponse response =
                                    client.execute(request);

                            reader =
                                    new BufferedReader(new InputStreamReader(
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

                            try {
                                if (reader != null) {
                                    reader.close();
                                }
                            } catch (IOException e) {
                                LOGGER.error(e.getMessage(), e);
                            }
                        }
                        return html;
                    }

                };

        final CircuitBreaker breaker =
                ConfigManager
                        .getCircuitBreaker(CircuitBreakerEnum.INTERNET_URL_CONNECTION);

        String html;

        try {
            html = (String) breaker.execute(operation);
        } catch (InterruptedException | CircuitBreakerException e) {
            html = null;
        }

        return html;

    }

    /**
     * Formats elapsed milliseconds into readable string.
     *
     * @param diff
     *            milliseconds
     * @return formatted string
     */
    private static String formatDuration(long duration) {

        long durationSeconds = duration / 1000;

        long days = durationSeconds / 86400;
        long hours = (durationSeconds % 86400) / 3600;
        long minutes = ((durationSeconds % 86400) % 3600) / 60;

        if (days == 0) {
            if (hours == 0) {
                if (minutes == 0) {
                    long seconds = ((durationSeconds % 86400) % 3600) % 60;
                    return String.format("%ds", seconds);
                }
                return String.format("%dm", minutes);
            }
            return String.format("%dh %dm", hours, minutes);
        }
        return String.format("%dd %dh %dm", days, hours, minutes);
    }

}
