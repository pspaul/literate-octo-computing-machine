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
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.IppQueueDao;
import org.savapage.core.dao.helpers.AbstractPagerReq;
import org.savapage.core.dao.helpers.IppQueueAttrEnum;
import org.savapage.core.dao.helpers.ReservedIppQueueEnum;
import org.savapage.core.jpa.IppQueue;
import org.savapage.core.json.JsonRollingTimeSeries;
import org.savapage.core.json.TimeSeriesInterval;
import org.savapage.core.services.QueueService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.NumberUtil;
import org.savapage.server.WebApp;
import org.savapage.server.pages.MarkupHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class QueuesPage extends AbstractAdminListPage {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory
            .getLogger(QueuesPage.class);

    private static final int MAX_PAGES_IN_NAVBAR = 5; // must be odd number

    private static final QueueService QUEUE_SERVICE = ServiceContext
            .getServiceFactory().getQueueService();

    /**
     * Bean for mapping JSON page request.
     * <p>
     * Note: class must be static.
     * </p>
     */
    private static class Req extends AbstractPagerReq {

        public static class Select {
            @JsonProperty("text")
            private String containingText = null;

            private Boolean trusted = null;
            private Boolean disabled = null;
            private Boolean deleted = null;

            public String getContainingText() {
                return containingText;
            }

            public void setContainingText(String containingText) {
                this.containingText = containingText;
            }

            public Boolean getTrusted() {
                return trusted;
            }

            public void setTrusted(Boolean trusted) {
                this.trusted = trusted;
            }

            public Boolean getDisabled() {
                return disabled;
            }

            public void setDisabled(Boolean disabled) {
                this.disabled = disabled;
            }

            public Boolean getDeleted() {
                return deleted;
            }

            public void setDeleted(Boolean deleted) {
                this.deleted = deleted;
            }
        }

        public static class Sort {

            private Boolean ascending = true;

            public Boolean getAscending() {
                return ascending;
            }

            public void setAscending(Boolean ascending) {
                this.ascending = ascending;
            }

        }

        private Select select;
        private Sort sort;

        public Select getSelect() {
            return select;
        }

        public void setSelect(Select select) {
            this.select = select;
        }

        public Sort getSort() {
            return sort;
        }

        public void setSort(Sort sort) {
            this.sort = sort;
        }

    }

    /**
     *
     */
    public QueuesPage() {

        //this.openServiceContext();

        final Req req = readReq();

        final IppQueueDao queueDao =
                ServiceContext.getDaoContext().getIppQueueDao();

        final IppQueueDao.ListFilter filter = new IppQueueDao.ListFilter();

        filter.setContainingText(req.getSelect().getContainingText());
        filter.setTrusted(req.getSelect().getTrusted());
        filter.setDisabled(req.getSelect().getDisabled());
        filter.setDeleted(req.getSelect().getDeleted());

        final long queueCount = queueDao.getListCount(filter);

        /*
         * Standard URL's
         */
        String serverName =
                ConfigManager.instance()
                        .getConfigValue(Key.SYS_SERVER_DNS_NAME);

        if (serverName == null || serverName.trim().isEmpty()) {
            try {
                serverName = ConfigManager.getServerHostAddress();
            } catch (UnknownHostException e) {
                serverName = "[?????]";
            }
        }

        final String server =
                serverName + ":" + WebApp.getServerPort()
                        + WebApp.MOUNT_PATH_PRINTERS;

        final String urlDefault = "&nbsp;ipp://" + server;
        final String urlWindows = "http://" + server;

        /*
         * Display the requested page.
         */
        final List<IppQueue> entryList =
                queueDao.getListChunk(filter, req.calcStartPosition(), req
                        .getMaxResults(), IppQueueDao.Field.URL_PATH, req
                        .getSort().getAscending());

        final QueueService queueService =
                ServiceContext.getServiceFactory().getQueueService();

        add(new PropertyListView<IppQueue>("queues-view", entryList) {

            /*
             * It seems that class is used for rendering AFTER the request is
             * done. At that point the EntityManager of the parent class is
             * closed: so, we need our own.
             */

            private static final long serialVersionUID = 1L;

            /**
             *
             * @param item
             */
            @Override
            protected void populateItem(final ListItem<IppQueue> item) {

                final IppQueue queue =
                        queueDao.findById(item.getModelObject().getId());

                /*
                 * The sparkline.
                 */
                final Date observationTime = new Date();
                final JsonRollingTimeSeries<Integer> series =
                        new JsonRollingTimeSeries<>(TimeSeriesInterval.DAY, 30,
                                0);

                series.clear();

                try {
                    series.init(observationTime, queueService
                            .getAttributeValue(queue,
                                    IppQueueAttrEnum.PRINT_IN_ROLLING_DAY_PAGES
                                            .getDbName()));
                } catch (IOException e) {
                    throw new SpException(e);
                }

                String sparklineData = "";

                final List<Integer> data = series.getData();

                for (int i = data.size(); i > 0; i--) {
                    if (i < data.size()) {
                        sparklineData += ",";
                    }
                    sparklineData += data.get(i - 1).toString();
                }

                item.add(new Label("queue-sparkline", sparklineData));

                String color = null;
                Label labelWrk = null;
                String signalKey = null;

                final ReservedIppQueueEnum reservedQueue =
                        QUEUE_SERVICE.getReservedQueue(queue.getUrlPath());

                if (reservedQueue == null || reservedQueue.isDriverPrint()) {
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
                    signalKey = null;
                }

                String signal = "";
                if (signalKey != null) {
                    signal = localized(signalKey);
                }
                labelWrk = new Label("signal", signal);
                labelWrk.add(new AttributeModifier("class", color));
                item.add(labelWrk);

                /*
                 *
                 */
                labelWrk = new Label("urlPath", "/" + queue.getUrlPath());
                labelWrk.add(new AttributeModifier("class", color));
                item.add(labelWrk);

                /*
                 *
                 */
                final MarkupHelper helper = new MarkupHelper(item);

                if (reservedQueue == null
                        || (reservedQueue != ReservedIppQueueEnum.RAW_PRINT && reservedQueue
                                .isDriverPrint())) {

                    helper.encloseLabel("url-default",
                            urlDefault + "/" + queue.getUrlPath(), true)
                            .setEscapeModelStrings(false);

                    helper.encloseLabel("url-windows",
                            urlWindows + "/" + queue.getUrlPath(), true)
                            .setEscapeModelStrings(false);

                } else {
                    helper.discloseLabel("url-default");
                    helper.discloseLabel("url-windows");
                }

                /*
                 *
                 */
                final String reservedText;

                if (reservedQueue == null) {
                    reservedText = ReservedIppQueueEnum.IPP_PRINT.getUiText();
                } else {
                    switch (reservedQueue) {
                    case IPP_PRINT:
                        reservedText =
                                reservedQueue.getUiText()
                                        + " ("
                                        + getLocalizer().getString(
                                                "signal-reserved", this) + ")";
                        break;
                    case RAW_PRINT:
                        reservedText =
                                reservedQueue.getUiText() + " Port "
                                        + ConfigManager.getRawPrinterPort();
                        break;
                    default:
                        reservedText = reservedQueue.getUiText();
                        break;
                    }
                }
                helper.encloseLabel("reserved-queue", reservedText, true);

                /*
                 *
                 */
                final ConfigManager cm = ConfigManager.instance();

                String proxyPrinterNames = null;

                if (reservedQueue != null
                        && reservedQueue == ReservedIppQueueEnum.SMARTSCHOOL) {

                    final StringBuilder builder = new StringBuilder();

                    if (cm.isConfigValue(Key.SMARTSCHOOL_1_ENABLE)) {
                        builder.append(cm
                                .getConfigValue(Key.SMARTSCHOOL_1_SOAP_PRINT_PROXY_PRINTER));
                    }

                    if (cm.isConfigValue(Key.SMARTSCHOOL_2_ENABLE)) {
                        if (builder.length() > 0) {
                            builder.append(", ");
                        }
                        builder.append(cm
                                .getConfigValue(Key.SMARTSCHOOL_2_SOAP_PRINT_PROXY_PRINTER));
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
                String period = ""; // localized("period") + ": ";
                String totals = "";

                if (queue.getResetDate() == null) {
                    period += localizedMediumDate(queue.getCreatedDate());
                } else {
                    period += localizedMediumDate(queue.getResetDate());
                }

                period += " ~ ";

                if (queue.getLastUsageDate() != null) {
                    period += localizedMediumDate(queue.getLastUsageDate());

                    //
                    String key = null;
                    Integer total = null;

                    //
                    total = queue.getTotalJobs();
                    totals += localizedNumber(total);
                    key = (total == 1) ? "job" : "jobs";
                    totals += " " + localized(key);

                    //
                    total = queue.getTotalPages();
                    totals += ", " + localizedNumber(total);
                    key = (total == 1) ? "page" : "pages";
                    totals += " " + localized(key);

                    //
                    totals +=
                            ", "
                                    + NumberUtil.humanReadableByteCount(
                                            queue.getTotalBytes(), true);
                }

                item.add(new Label("period", period));
                item.add(new Label("totals", totals));

                /*
                 *
                 */
                labelWrk =
                        new Label("button-log", getLocalizer().getString(
                                "button-log", this));
                labelWrk.add(new AttributeModifier("data-savapage", queue
                        .getId()));
                item.add(labelWrk);

                /*
                 * Set the primary key in 'data-savapage' attribute, so it can
                 * be picked up in JavaScript for editing.
                 */
                if (reservedQueue == null || reservedQueue.isDriverPrint()) {
                    // labelWrk =
                    // new Label("button-edit", getLocalizer().getString(
                    // "button-edit", this));
                    // labelWrk.add(new AttributeModifier("data-savapage", queue
                    // .getId()));
                    // item.add(labelWrk);

                    helper.encloseLabel("button-edit",
                            getLocalizer().getString("button-edit", this), true)
                            .add(new AttributeModifier("data-savapage", queue
                                    .getId()));

                } else {
                    helper.discloseLabel("button-edit");
                }

            }

        });

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
