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

import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
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
import org.savapage.core.dao.PrinterAttrDao;
import org.savapage.core.dao.PrinterDao;
import org.savapage.core.dao.enums.AccessControlScopeEnum;
import org.savapage.core.dao.enums.AppLogLevelEnum;
import org.savapage.core.dao.enums.PrinterAttrEnum;
import org.savapage.core.dao.enums.ProxyPrintAuthModeEnum;
import org.savapage.core.dao.helpers.AbstractPagerReq;
import org.savapage.core.dao.helpers.JsonUserGroupAccess;
import org.savapage.core.ipp.IppSyntaxException;
import org.savapage.core.ipp.client.IppConnectException;
import org.savapage.core.jpa.Device;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.PrinterGroupMember;
import org.savapage.core.json.JsonRollingTimeSeries;
import org.savapage.core.json.TimeSeriesInterval;
import org.savapage.core.print.proxy.JsonProxyPrinter;
import org.savapage.core.services.DeviceService;
import org.savapage.core.services.PrinterService;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.PrinterAttrLookup;
import org.savapage.core.services.helpers.ThirdPartyEnum;
import org.savapage.core.util.NumberUtil;
import org.savapage.ext.papercut.PaperCutHelper;
import org.savapage.server.WebApp;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.pages.MessageContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PrintersPage extends AbstractAdminListPage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PrintersPage.class);

    /**
     *
     */
    private static final DeviceService DEVICE_SERVICE =
            ServiceContext.getServiceFactory().getDeviceService();

    /**
     *
     */
    private static final PrinterService PRINTER_SERVICE =
            ServiceContext.getServiceFactory().getPrinterService();

    /**
     *
     */
    private static final PrinterAttrDao PRINTER_ATTR_DAO =
            ServiceContext.getDaoContext().getPrinterAttrDao();

    /**
    *
    */
    private static final ProxyPrintService PROXY_PRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();

    /**
     *
     */
    private static final int MAX_PAGES_IN_NAVBAR = 5; // must be odd number

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

            private Boolean disabled = null;
            private Boolean deleted = null;

            public String getContainingText() {
                return containingText;
            }

            @SuppressWarnings("unused")
            public void setContainingText(String containingText) {
                this.containingText = containingText;
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
    */
    private class PrintersListView extends PropertyListView<Printer> {

        private static final long serialVersionUID = 1L;
        private final String jobTicketPrinterName;

        public PrintersListView(final String id,
                final List<Printer> entryList) {
            super(id, entryList);
            this.jobTicketPrinterName = StringUtils.defaultString(ConfigManager
                    .instance().getConfigValue(Key.JOBTICKET_PROXY_PRINTER));
        }

        private String getProxyPrintAuthMode(final Device device) {

            final StringBuilder proxyPrintAuthMode = new StringBuilder();

            final ProxyPrintAuthModeEnum authModeEnum =
                    DEVICE_SERVICE.getProxyPrintAuthMode(device.getId());

            if (authModeEnum != null) {

                switch (authModeEnum) {
                case DIRECT:
                    proxyPrintAuthMode.append("Direct");
                    break;

                case FAST:
                    proxyPrintAuthMode.append("Fast");
                    break;

                case FAST_DIRECT:
                    proxyPrintAuthMode.append("Fast &bull; Direct");
                    break;

                case FAST_HOLD:
                    proxyPrintAuthMode.append("Fast &bull; Hold");
                    break;

                case HOLD:
                    proxyPrintAuthMode.append("Hold");
                    break;

                default:
                    throw new SpException(
                            "Oops, missed auth mode [" + authModeEnum + "]");
                }
            }
            return proxyPrintAuthMode.toString();
        }

        @Override
        protected void populateItem(final ListItem<Printer> item) {

            final Printer printer = ServiceContext.getDaoContext()
                    .getPrinterDao().findById(item.getModelObject().getId());

            final MarkupHelper helper = new MarkupHelper(item);

            Label labelWrk = null;

            /*
             * Sparklines: pie-chart.
             */
            String sparklineData = printer.getTotalPages().toString() + ","
                    + printer.getTotalSheets();

            item.add(new Label("printer-pie", sparklineData));

            /*
             * The sparkline.
             */
            final Date observationTime = new Date();
            final JsonRollingTimeSeries<Integer> series =
                    new JsonRollingTimeSeries<>(TimeSeriesInterval.DAY, 30, 0);

            series.clear();

            try {
                series.init(observationTime, PRINTER_SERVICE.getAttributeValue(
                        printer, PrinterAttrEnum.PRINT_OUT_ROLLING_DAY_PAGES));
            } catch (IOException e) {
                throw new SpException(e);
            }

            sparklineData = "";

            final List<Integer> data = series.getData();
            for (int i = data.size(); i > 0; i--) {
                if (i < data.size()) {
                    sparklineData += ",";
                }
                sparklineData += data.get(i - 1).toString();
            }

            item.add(new Label("printer-sparkline", sparklineData));

            /*
             *
             */
            item.add(new Label("displayName"));

            labelWrk = new Label("printerName");
            labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_DATA_SAVAPAGE,
                    printer.getId()));

            item.add(labelWrk);

            final boolean isJobTicketPrinter = this.jobTicketPrinterName
                    .equalsIgnoreCase(printer.getPrinterName());

            final String imageSrc;

            final Map<String, Device> terminalDevices = new HashMap<>();
            final Map<String, Device> readerDevices = new HashMap<>();

            if (isJobTicketPrinter) {
                imageSrc = "printer-jobticket-32x32.png";
            } else {
                final MutableBoolean terminalSecured = new MutableBoolean();
                final MutableBoolean readerSecured = new MutableBoolean();

                final boolean isSecured = PRINTER_SERVICE.checkPrinterSecurity(
                        printer, terminalSecured, readerSecured,
                        terminalDevices, readerDevices);

                if (isSecured) {

                    if (terminalSecured.booleanValue()
                            && readerSecured.booleanValue()) {
                        imageSrc = "printer-terminal-custom-or-auth-16x16.png";
                    } else if (terminalSecured.booleanValue()) {
                        imageSrc = "printer-terminal-custom-16x16.png";
                    } else {
                        imageSrc = "printer-terminal-auth-16x16.png";
                    }
                } else if (ConfigManager.instance()
                        .isNonSecureProxyPrinter(printer)) {
                    imageSrc = "printer-terminal-any-16x16.png";
                } else {
                    imageSrc = "printer-terminal-none-16x16.png";
                }
            }
            labelWrk = new Label("printerImage", "");

            labelWrk.add(new AttributeModifier("src",
                    String.format("%s/%s", WebApp.PATH_IMAGES, imageSrc)));

            item.add(labelWrk);

            String assocTerminal = null;
            String assocCardReader = null;

            for (final Entry<String, Device> entry : terminalDevices
                    .entrySet()) {
                if (assocTerminal == null) {
                    assocTerminal = "";
                } else {
                    assocTerminal += ", ";
                }
                assocTerminal += entry.getValue().getDisplayName();
            }

            for (final Entry<String, Device> entry : readerDevices.entrySet()) {

                if (assocCardReader == null) {
                    assocCardReader = "";
                } else {
                    assocCardReader += ", ";
                }

                assocCardReader += entry.getValue().getDisplayName();

                final String authMode = getProxyPrintAuthMode(entry.getValue());
                if (StringUtils.isNotBlank(authMode)) {
                    assocCardReader += " &bull; " + authMode;
                }

            }

            item.add(createVisibleLabel((assocTerminal != null),
                    "assocTerminal", assocTerminal));

            item.add(createVisibleLabel((assocCardReader != null),
                    "assocCardReader", assocCardReader)
                            .setEscapeModelStrings(false));

            /*
             * Printer Groups
             */
            final List<PrinterGroupMember> groupMembers =
                    printer.getPrinterGroupMembers();

            String printerGroups = null;

            if (groupMembers != null) {

                for (final PrinterGroupMember member : groupMembers) {
                    if (printerGroups == null) {
                        printerGroups = "";
                    } else {
                        printerGroups += ", ";
                    }
                    printerGroups += member.getGroup().getDisplayName();
                }

            }

            item.add(createVisibleLabel((printerGroups != null),
                    "printerGroups", printerGroups));

            /*
             * User Groups
             */
            final StringBuilder userGroups = new StringBuilder();

            final JsonUserGroupAccess userAccess =
                    PRINTER_SERVICE.getAccessControl(printer);

            for (final String userGroup : userAccess.getGroups()) {
                if (userGroups.length() > 0) {
                    userGroups.append(", ");
                }
                userGroups.append(userGroup);
            }

            final String userGroupsPrompt;
            String color = null;

            if (userAccess.getScope() == AccessControlScopeEnum.ALLOW) {
                userGroupsPrompt = localized("prompt-allow-access");
                color = MarkupHelper.CSS_TXT_VALID;
            } else {
                userGroupsPrompt = localized("prompt-deny-access");
                color = MarkupHelper.CSS_TXT_WARN;
            }

            labelWrk = new Label("userGroupsPrompt", userGroupsPrompt);
            labelWrk.add(new AttributeModifier("class", color));
            item.add(labelWrk);

            labelWrk = createVisibleLabel(userGroups.length() > 0, "userGroups",
                    userGroups.toString());
            labelWrk.add(new AttributeModifier("class", color));

            item.add(labelWrk);

            /*
             * Device URI + Signal
             */
            final JsonProxyPrinter cupsPrinter = PROXY_PRINT_SERVICE
                    .getCachedPrinter(printer.getPrinterName());

            String deviceUriText = "";
            String deviceUriImgUrl = null;

            if (cupsPrinter != null) {
                final URI deviceUri = cupsPrinter.getDeviceUri();
                if (deviceUri != null) {
                    deviceUriText = deviceUri.toString();
                    if (PaperCutHelper.isPaperCutPrinter(deviceUri)) {
                        deviceUriImgUrl = WebApp.getThirdPartyEnumImgUrl(
                                ThirdPartyEnum.PAPERCUT);
                    }
                }
            }

            item.add(new Label("deviceUri", deviceUriText));

            //
            labelWrk = createVisibleLabel(deviceUriImgUrl != null,
                    "deviceUriImg", "");
            if (deviceUriImgUrl != null) {
                labelWrk.add(new AttributeModifier("src", deviceUriImgUrl));
            }
            item.add(labelWrk);

            //
            final PrinterAttrLookup attrLookup = new PrinterAttrLookup(printer);

            final boolean isInternal =
                    PRINTER_ATTR_DAO.isInternalPrinter(attrLookup);

            final boolean isConfigured =
                    cupsPrinter == null || PROXY_PRINT_SERVICE
                            .isPrinterConfigured(cupsPrinter, attrLookup);

            item.add(createVisibleLabel(!isConfigured,
                    "printer-needs-configuration",
                    localized("printer-needs-configuration")));

            //
            String signalKey = null;
            color = null;

            String location = "";
            String info = "";

            if (cupsPrinter == null) {
                if (printer.getDeleted()) {
                    color = MarkupHelper.CSS_TXT_ERROR;
                    signalKey = "signal-deleted";
                } else if (PROXY_PRINT_SERVICE.isConnectedToCups()) {
                    color = MarkupHelper.CSS_TXT_WARN;
                    signalKey = "signal-not-present";
                } else {
                    color = MarkupHelper.CSS_TXT_ERROR;
                    signalKey = "signal-disconnected";
                }
            } else {
                if (printer.getDisabled()) {
                    color = MarkupHelper.CSS_TXT_ERROR;
                    signalKey = "signal-disabled";
                } else if (isInternal) {
                    color = MarkupHelper.CSS_TXT_WARN;
                    signalKey = "signal-internal";
                } else {
                    color = MarkupHelper.CSS_TXT_VALID;
                    signalKey = "signal-active";
                }
                location = cupsPrinter.getLocation();

                if (StringUtils.isNotBlank(cupsPrinter.getPpd())) {
                    info = cupsPrinter.getPpd() + " version "
                            + cupsPrinter.getPpdVersion() + ": "
                            + cupsPrinter.getModelName();
                } else {
                    info = cupsPrinter.getModelName();
                }
            }

            String signal = "";

            if (signalKey != null) {
                signal = localized(signalKey);
            }

            labelWrk = new Label("signal", signal);

            if (color != null) {
                labelWrk.add(new AttributeModifier("class", color));
            }
            item.add(labelWrk);

            // item.add(new Label("info", info));
            item.add(new Label("info", ""));

            /*
             * Period + Totals
             */
            final StringBuilder period = new StringBuilder();
            final StringBuilder totals = new StringBuilder();

            if (printer.getResetDate() == null) {
                period.append(localizedMediumDate(printer.getCreatedDate()));
            } else {
                period.append(localizedMediumDate(printer.getResetDate()));
            }

            period.append(" ~ ");

            if (printer.getLastUsageDate() != null) {
                period.append(localizedMediumDate(printer.getLastUsageDate()));

                //
                String key = null;
                Integer total = null;

                //
                total = printer.getTotalJobs();
                totals.append(helper.localizedNumber(total));
                key = (total == 1) ? "job" : "jobs";
                totals.append(" ").append(localized(key));

                //
                total = printer.getTotalPages();
                totals.append(", " + helper.localizedNumber(total));
                key = (total == 1) ? "page" : "pages";
                totals.append(" ").append(localized(key));

                //
                total = printer.getTotalSheets();
                totals.append(", " + helper.localizedNumber(total));
                key = (total == 1) ? "sheet" : "sheets";
                totals.append(" ").append(localized(key));

                //
                totals.append(", ").append(NumberUtil
                        .humanReadableByteCount(printer.getTotalBytes(), true));
            }

            item.add(new Label("period", period.toString()));
            item.add(new Label("totals", totals.toString()));

            /*
             * Set the uid in 'data-savapage' attribute, so it can be picked up
             * in JavaScript for editing.
             */
            labelWrk = new Label("button-edit",
                    getLocalizer().getString("button-edit", this));
            labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_DATA_SAVAPAGE,
                    printer.getId()));
            item.add(labelWrk);

            /*
             *
             */
            labelWrk = new Label("button-log",
                    getLocalizer().getString("button-log", this));
            labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_DATA_SAVAPAGE,
                    printer.getId()));
            item.add(labelWrk);

            /*
             *
             */
            labelWrk = new Label("button-cups",
                    getLocalizer().getString("button-cups", this)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean isVisible() {
                    return cupsPrinter != null;
                }
            };

            labelWrk.add(new AttributeModifier("href",
                    getCupsPrinterUrl(printer.getPrinterName())));
            item.add(labelWrk);

        }
    }

    /**
     *
     */
    public PrintersPage(final PageParameters parameters) {

        super(parameters);

        /*
         * Check for new/changed printers.
         *
         * We need a transaction because of the lazy creation of Printer
         * objects.
         */
        ServiceContext.getDaoContext().beginTransaction();

        try {

            PROXY_PRINT_SERVICE.lazyInitPrinterCache();
            handlePage();

        } catch (IppConnectException e) {

            setResponsePage(new MessageContent(AppLogLevelEnum.WARN,
                    localized("ipp-connect-error", e.getMessage())));

        } catch (IppSyntaxException e) {
            throw new SpException(e);
        }
    }

    /**
     *
     */
    private void handlePage() {

        final Req req = readReq();

        final PrinterDao printerDao =
                ServiceContext.getDaoContext().getPrinterDao();

        final PrinterDao.ListFilter filter = new PrinterDao.ListFilter();
        filter.setContainingText(req.getSelect().getContainingText());
        filter.setDeleted(req.getSelect().getDeleted());
        filter.setDisabled(req.getSelect().getDisabled());

        final long printerCount = printerDao.getListCount(filter);

        /*
         * Display the requested page.
         */
        final List<Printer> entryList = printerDao.getListChunk(filter,
                req.calcStartPosition(), req.getMaxResults(),
                PrinterDao.Field.DISPLAY_NAME, req.getSort().getAscending());

        add(new PrintersListView("printers-view", entryList));

        /*
         * Display the navigation bars and write the response.
         */
        createNavBarResponse(req, printerCount, MAX_PAGES_IN_NAVBAR,
                "sp-printers-page", new String[] { "nav-bar-1", "nav-bar-2" });
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
            final ObjectMapper mapper = new ObjectMapper();

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

    /**
     * Gets the CUPS URL for a printer.
     *
     * @param printerName
     *            The CUPS printer name.
     * @return The URL.
     */
    private String getCupsPrinterUrl(final String printerName) {
        return PROXY_PRINT_SERVICE.getCupsPrinterUrl(printerName).toString();
    }
}
