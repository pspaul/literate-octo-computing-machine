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

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.IppQueueDao;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.IppQueueAttrEnum;
import org.savapage.core.dao.enums.ReservedIppQueueEnum;
import org.savapage.core.dao.helpers.AbstractPagerReq;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.jpa.IppQueue;
import org.savapage.core.json.JsonRollingTimeSeries;
import org.savapage.core.json.TimeSeriesInterval;
import org.savapage.core.services.AccessControlService;
import org.savapage.core.services.QueueService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.InetUtils;
import org.savapage.core.util.NumberUtil;
import org.savapage.server.WebApp;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.helpers.SparklineHtml;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.session.SpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class QueuesPage extends AbstractAdminListPage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(QueuesPage.class);

    /**
     * Note: must be odd number.
     */
    private static final int MAX_PAGES_IN_NAVBAR = 5;

    /** */
    private static final AccessControlService ACCESS_CONTROL_SERVICE =
            ServiceContext.getServiceFactory().getAccessControlService();

    /** */
    private static final QueueService QUEUE_SERVICE =
            ServiceContext.getServiceFactory().getQueueService();

    /** */
    private static final IppQueueDao QUEUE_DAO =
            ServiceContext.getDaoContext().getIppQueueDao();

    /** */
    private static final String WID_QUEUE_SPARKLINE = "queue-sparkline";

    /** */
    private static final String WID_QUEUE_IPP_ROUTING_IMG = "ipp-routing-img";

    /**
     * Bean for mapping JSON page request.
     * <p>
     * Note: class must be static.
     * </p>
     */
    private static class Req extends AbstractPagerReq {

        /**
         *
         */
        public static class Select {

            @JsonProperty("text")
            private String containingText = null;

            private Boolean trusted = null;
            private Boolean disabled = null;
            private Boolean deleted = null;

            public String getContainingText() {
                return containingText;
            }

            @SuppressWarnings("unused")
            public void setContainingText(String containingText) {
                this.containingText = containingText;
            }

            public Boolean getTrusted() {
                return trusted;
            }

            @SuppressWarnings("unused")
            public void setTrusted(Boolean trusted) {
                this.trusted = trusted;
            }

            public Boolean getDisabled() {
                return disabled;
            }

            @SuppressWarnings("unused")
            public void setDisabled(Boolean disabled) {
                this.disabled = disabled;
            }

            public Boolean getDeleted() {
                return deleted;
            }

            @SuppressWarnings("unused")
            public void setDeleted(Boolean deleted) {
                this.deleted = deleted;
            }
        }

        /**
         *
         */
        public static class Sort {

            private Boolean ascending = true;

            public Boolean getAscending() {
                return ascending;
            }

            @SuppressWarnings("unused")
            public void setAscending(Boolean ascending) {
                this.ascending = ascending;
            }

        }

        private Select select;
        private Sort sort;

        public Select getSelect() {
            return select;
        }

        @SuppressWarnings("unused")
        public void setSelect(Select select) {
            this.select = select;
        }

        public Sort getSort() {
            return sort;
        }

        @SuppressWarnings("unused")
        public void setSort(Sort sort) {
            this.sort = sort;
        }

    }

    /**
     *
     * @author Rijk Ravestein
     *
     */
    private final class QueueListView extends PropertyListView<IppQueue> {

        /** */
        private static final long serialVersionUID = 1L;

        /** */
        private static final String WID_BUTTON_EDIT = "button-edit";

        /** */
        private static final String WID_BUTTON_LOG = "button-log";

        /** */
        private final String urlDefault;

        /** */
        private final String urlWindows;

        /***/
        private final boolean isEditor;

        /***/
        private final boolean hasAccessDoc;

        /**
         *
         * @param id
         * @param list
         * @param isEditor
         */
        public QueueListView(String id, List<IppQueue> list,
                final boolean isEditor) {

            super(id, list);

            /*
             * Standard URL's
             */
            String serverName = ConfigManager.instance()
                    .getConfigValue(Key.SYS_SERVER_DNS_NAME);

            if (StringUtils.isBlank(serverName)) {
                try {
                    serverName = InetUtils.getServerHostAddress();
                } catch (UnknownHostException e) {
                    serverName = "[?????]";
                }
            }

            this.urlDefault =
                    new StringBuilder().append("ipps://").append(serverName)
                            .append(":").append(WebApp.getServerSslPort())
                            .append(WebApp.MOUNT_PATH_PRINTERS).toString();

            this.urlWindows =
                    new StringBuilder().append("https://").append(serverName)
                            .append(":").append(WebApp.getServerSslPort())
                            .append(WebApp.MOUNT_PATH_PRINTERS).toString();

            this.isEditor = isEditor;
            this.hasAccessDoc = ACCESS_CONTROL_SERVICE.hasAccess(
                    SpSession.get().getUserIdDto(), ACLOidEnum.A_DOCUMENTS);
        }

        @Override
        protected void populateItem(final ListItem<IppQueue> item) {

            final IppQueue queue =
                    QUEUE_DAO.findById(item.getModelObject().getId());

            /*
             * The sparkline.
             */
            final Date observationTime = new Date();
            final JsonRollingTimeSeries<Integer> series =
                    new JsonRollingTimeSeries<>(TimeSeriesInterval.DAY, 30, 0);

            series.clear();

            try {
                series.init(observationTime, QUEUE_SERVICE.getAttrValue(queue,
                        IppQueueAttrEnum.PRINT_IN_ROLLING_DAY_PAGES));
            } catch (IOException e) {
                throw new SpException(e);
            }

            String sparklineData = "";

            final List<Integer> data = series.getData();

            if (data.size() > 1
                    || data.size() == 1 && data.get(0).intValue() > 0) {
                for (int i = data.size(); i > 0; i--) {
                    if (i < data.size()) {
                        sparklineData += ",";
                    }
                    sparklineData += data.get(i - 1).toString();
                }
            }

            //
            final MarkupHelper helper = new MarkupHelper(item);
            Label labelWrk = null;

            //
            final boolean isIppRouting = ConfigManager.instance()
                    .isConfigValue(Key.IPP_ROUTING_ENABLE)
                    && QUEUE_SERVICE.isIppRoutingQueue(queue);

            labelWrk = helper.encloseLabel(WID_QUEUE_IPP_ROUTING_IMG, "",
                    isIppRouting);

            if (isIppRouting) {
                MarkupHelper.modifyLabelAttr(labelWrk, MarkupHelper.ATTR_SRC,
                        String.format("%s/%s", WebApp.PATH_IMAGES,
                                "printer-26x26.png"));
            }

            //
            final boolean hasLine = sparklineData.length() > 0;
            labelWrk = helper.encloseLabel(WID_QUEUE_SPARKLINE,
                    sparklineData.toString(), hasLine);

            if (hasLine) {
                MarkupHelper.modifyLabelAttr(labelWrk,
                        SparklineHtml.ATTR_LINE_COLOR,
                        SparklineHtml.COLOR_QUEUE);
                MarkupHelper.modifyLabelAttr(labelWrk,
                        SparklineHtml.ATTR_FILL_COLOR,
                        SparklineHtml.COLOR_QUEUE);
                MarkupHelper.modifyLabelAttr(labelWrk, MarkupHelper.ATTR_CLASS,
                        SparklineHtml.CSS_CLASS_QUEUE);
            }

            //
            String color = null;
            String signalKey = null;

            final ReservedIppQueueEnum reservedQueue =
                    QUEUE_SERVICE.getReservedQueue(queue.getUrlPath());

            if (reservedQueue == null || reservedQueue.isDriverPrint()
                    || reservedQueue == ReservedIppQueueEnum.WEBSERVICE) {
                if (queue.getDeleted()) {
                    color = MarkupHelper.CSS_TXT_ERROR;
                    signalKey = "signal-deleted";
                } else if (queue.getDisabled()) {
                    color = MarkupHelper.CSS_TXT_ERROR;
                    signalKey = "signal-disabled";
                } else if (queue.getTrusted()) {
                    color = MarkupHelper.CSS_TXT_VALID;
                    signalKey = "signal-trusted";
                } else {
                    color = MarkupHelper.CSS_TXT_WARN;
                    signalKey = "signal-untrusted";
                }
            } else {
                color = MarkupHelper.CSS_TXT_WARN;
                if (QUEUE_SERVICE.isActiveQueue(queue)) {
                    signalKey = null;
                } else {
                    signalKey = "signal-disabled";
                }
            }

            String signal = "";
            if (signalKey != null) {
                signal = localized(signalKey);
            }
            labelWrk = new Label("signal", signal);
            labelWrk.add(new AttributeModifier("class", color));
            item.add(labelWrk);

            //
            final boolean isDefaultQueue = queue.getUrlPath()
                    .equals(ReservedIppQueueEnum.IPP_PRINT.getUrlPath());
            final String urlPathPfx;
            if (isDefaultQueue) {
                urlPathPfx = "";
            } else {
                urlPathPfx = "/";
            }
            labelWrk =
                    new Label("urlPath", urlPathPfx.concat(queue.getUrlPath()));
            labelWrk.add(new AttributeModifier("class", color));
            item.add(labelWrk);

            //
            if (reservedQueue == null || (reservedQueue.isDriverPrint()
                    && reservedQueue != ReservedIppQueueEnum.RAW_PRINT
                    && reservedQueue != ReservedIppQueueEnum.IPP_PRINT_INTERNET
                    && reservedQueue != ReservedIppQueueEnum.AIRPRINT)) {

                helper.encloseLabel("url-default",
                        String.format("%s%s%s", urlDefault, urlPathPfx,
                                queue.getUrlPath()),
                        true).setEscapeModelStrings(false);

                // For now, do NOT show the Windows URL.
                helper.encloseLabel("url-windows",
                        String.format("%s%s%s", urlWindows, urlPathPfx,
                                queue.getUrlPath()),
                        false).setEscapeModelStrings(false);

            } else {
                helper.discloseLabel("url-default");
                helper.discloseLabel("url-windows");
            }

            /*
            *
            */
            final StringBuilder reservedText = new StringBuilder();

            if (reservedQueue == null) {
                reservedText.append(ReservedIppQueueEnum.IPP_PRINT.getUiText());
            } else {
                reservedText.append(reservedQueue.getUiText());

                if (reservedQueue == ReservedIppQueueEnum.RAW_PRINT) {
                    reservedText.append(" Port ")
                            .append(ConfigManager.getRawPrinterPort());
                }

                reservedText
                        .append(" (").append(getLocalizer()
                                .getString("signal-reserved", this))
                        .append(")");

            }
            helper.encloseLabel("reserved-queue", reservedText.toString(),
                    true);

            /*
            *
            */
            final ConfigManager cm = ConfigManager.instance();

            String proxyPrinterNames = null;

            if (reservedQueue != null
                    && reservedQueue == ReservedIppQueueEnum.SMARTSCHOOL) {

                final StringBuilder builder = new StringBuilder();

                if (cm.isConfigValue(Key.SMARTSCHOOL_1_ENABLE)) {
                    builder.append(cm.getConfigValue(
                            Key.SMARTSCHOOL_1_SOAP_PRINT_PROXY_PRINTER));

                    final String grayscalePrinter = cm.getConfigValue(
                            Key.SMARTSCHOOL_1_SOAP_PRINT_PROXY_PRINTER_GRAYSCALE);

                    if (StringUtils.isNotBlank(grayscalePrinter)) {
                        builder.append(" (").append(grayscalePrinter)
                                .append(")");
                    }
                }

                if (cm.isConfigValue(Key.SMARTSCHOOL_2_ENABLE)) {
                    if (builder.length() > 0) {
                        builder.append(", ");
                    }
                    builder.append(cm.getConfigValue(
                            Key.SMARTSCHOOL_2_SOAP_PRINT_PROXY_PRINTER));

                    final String grayscalePrinter = cm.getConfigValue(
                            Key.SMARTSCHOOL_2_SOAP_PRINT_PROXY_PRINTER_GRAYSCALE);

                    if (StringUtils.isNotBlank(grayscalePrinter)) {
                        builder.append(" (").append(grayscalePrinter)
                                .append(")");
                    }
                }

                proxyPrinterNames = builder.toString();

            }

            helper.encloseLabel("proxy-printer", proxyPrinterNames,
                    StringUtils.isNotBlank(proxyPrinterNames));

            /*
            *
            */
            item.add(new Label("ipAllowed"));

            /*
             * Period + Totals
             */
            final StringBuilder period = new StringBuilder();
            final StringBuilder totals = new StringBuilder();

            if (queue.getResetDate() == null) {
                period.append(localizedMediumDate(queue.getCreatedDate()));
            } else {
                period.append(localizedMediumDate(queue.getResetDate()));
            }

            period.append(" ~ ");

            if (queue.getLastUsageDate() != null) {
                period.append(localizedMediumDate(queue.getLastUsageDate()));

                //
                String key = null;
                Integer total = null;

                //
                total = queue.getTotalJobs();
                totals.append(helper.localizedNumber(total));
                key = (total == 1) ? "job" : "jobs";
                totals.append(" ").append(localized(key));

                //
                total = queue.getTotalPages();
                totals.append(" &bull; ").append(helper.localizedNumber(total));
                key = (total == 1) ? "page" : "pages";
                totals.append(" ").append(localized(key));

                //
                totals.append(" &bull; ")
                        .append(NumberUtil.humanReadableByteCountSI(getLocale(),
                                queue.getTotalBytes()));
            }

            item.add(new Label("period", period.toString()));

            labelWrk = new Label("totals", totals.toString());
            labelWrk.setEscapeModelStrings(false);
            item.add(labelWrk);

            /*
             * Set the primary key in 'data-savapage' attribute, so it can be
             * picked up in JavaScript for editing.
             */
            boolean hasButtons = false;

            if (this.hasAccessDoc) {
                hasButtons = true;
                labelWrk = new Label(WID_BUTTON_LOG,
                        getLocalizer().getString("button-log", this));
                MarkupHelper.modifyLabelAttr(labelWrk,
                        MarkupHelper.ATTR_DATA_SAVAPAGE,
                        queue.getId().toString());
                MarkupHelper.modifyLabelAttr(labelWrk, MarkupHelper.ATTR_TITLE,
                        NounEnum.DOCUMENT.uiText(getLocale(), true));
                item.add(labelWrk);
            } else {
                helper.discloseLabel(WID_BUTTON_LOG);
            }

            if (this.isEditor && (reservedQueue == null
                    || reservedQueue.isDriverPrint()
                    || reservedQueue == ReservedIppQueueEnum.WEBSERVICE)) {
                hasButtons = true;
                labelWrk = helper.addLabel(WID_BUTTON_EDIT,
                        getLocalizer().getString("button-edit", this));
                MarkupHelper.modifyLabelAttr(labelWrk,
                        MarkupHelper.ATTR_DATA_SAVAPAGE,
                        queue.getId().toString());
                MarkupHelper.modifyLabelAttr(labelWrk, MarkupHelper.ATTR_TITLE,
                        HtmlButtonEnum.EDIT.uiText(getLocale()));
                item.add(labelWrk);

            } else {
                helper.discloseLabel(WID_BUTTON_EDIT);
            }

            helper.addTransparantInvisible("sect-buttons", !hasButtons);
        }

    }

    /**
     *
     * @param parameters
     *            The page parameters.
     */
    public QueuesPage(final PageParameters parameters) {

        super(parameters);

        final Req req = readReq();

        final IppQueueDao.ListFilter filter = new IppQueueDao.ListFilter();

        filter.setContainingText(req.getSelect().getContainingText());
        filter.setTrusted(req.getSelect().getTrusted());
        filter.setDisabled(req.getSelect().getDisabled());
        filter.setDeleted(req.getSelect().getDeleted());

        final long queueCount = QUEUE_DAO.getListCount(filter);

        /*
         * Display the requested page.
         */
        final List<IppQueue> entryList = QUEUE_DAO.getListChunk(filter,
                req.calcStartPosition(), req.getMaxResults(),
                IppQueueDao.Field.URL_PATH, req.getSort().getAscending());

        add(new QueueListView("queues-view", entryList,
                this.probePermissionToEdit(ACLOidEnum.A_QUEUES)));

        /*
         * Display the navigation bars and write the response.
         */
        createNavBarResponse(req, queueCount, MAX_PAGES_IN_NAVBAR,
                "sp-queues-page", new String[] { "nav-bar-1", "nav-bar-2" });
    }

    /**
     * Reads the page request from the POST parameter.
     *
     * @return The page request.
     */
    private Req readReq() {

        final String data = getParmValue(POST_PARM_DATA);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("data : " + data);
        }

        Req req = null;

        if (data != null) {
            /*
             * Use passed JSON values
             */
            ObjectMapper mapper = new ObjectMapper();
            try {
                req = mapper.readValue(data, Req.class);
            } catch (IOException e) {
                throw new SpException(e.getMessage());
            }
        }
        /*
         * Check inputData separately, since JSON might not have delivered the
         * right parameters and the mapper returned null.
         */
        if (req == null) {
            /*
             * Use the defaults
             */
            req = new Req();
        }
        return req;
    }

}
