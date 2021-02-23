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
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.PrinterAttrDao;
import org.savapage.core.dao.PrinterDao;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.AccessControlScopeEnum;
import org.savapage.core.dao.enums.AppLogLevelEnum;
import org.savapage.core.dao.enums.PrinterAttrEnum;
import org.savapage.core.dao.enums.ProxyPrintAuthModeEnum;
import org.savapage.core.dao.helpers.AbstractPagerReq;
import org.savapage.core.dao.helpers.JsonUserGroupAccess;
import org.savapage.core.dao.helpers.ProxyPrinterSnmpInfoDto;
import org.savapage.core.doc.store.DocStoreBranchEnum;
import org.savapage.core.doc.store.DocStoreTypeEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.ipp.IppSyntaxException;
import org.savapage.core.ipp.client.IppConnectException;
import org.savapage.core.jpa.Device;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.PrinterGroupMember;
import org.savapage.core.json.JsonRollingTimeSeries;
import org.savapage.core.json.TimeSeriesInterval;
import org.savapage.core.print.proxy.JsonProxyPrinter;
import org.savapage.core.services.AccessControlService;
import org.savapage.core.services.DeviceService;
import org.savapage.core.services.DocStoreService;
import org.savapage.core.services.PrinterService;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.PrinterAttrLookup;
import org.savapage.core.services.helpers.ThirdPartyEnum;
import org.savapage.core.util.NumberUtil;
import org.savapage.ext.papercut.PaperCutHelper;
import org.savapage.server.WebApp;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.helpers.HtmlPrinterImgEnum;
import org.savapage.server.helpers.SparklineHtml;
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
public final class PrintersPage extends AbstractAdminListPage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PrintersPage.class);

    /** */
    private static final AccessControlService ACCESS_CONTROL_SERVICE =
            ServiceContext.getServiceFactory().getAccessControlService();

    /** */
    private static final DeviceService DEVICE_SERVICE =
            ServiceContext.getServiceFactory().getDeviceService();

    /** */
    private static final DocStoreService DOC_STORE_SERVICE =
            ServiceContext.getServiceFactory().getDocStoreService();

    /** */
    private static final PrinterService PRINTER_SERVICE =
            ServiceContext.getServiceFactory().getPrinterService();

    /** */
    private static final PrinterAttrDao PRINTER_ATTR_DAO =
            ServiceContext.getDaoContext().getPrinterAttrDao();

    /** */
    private static final ProxyPrintService PROXY_PRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();

    /**
     * Note: must be odd number.
     */
    private static final int MAX_PAGES_IN_NAVBAR = 5;

    /** */
    private static final String CSS_CLASS_PRINTER_OPT_DOWNLOAD =
            "sp-printer-opt-download";

    /** */
    private static final String CSS_CLASS_PRINTER_PPD_DOWNLOAD =
            "sp-printer-ppd-download";

    /** */
    private static final String CSS_CLASS_PRINTER_PPDE_DOWNLOAD =
            "sp-printer-ppde-download";

    private static final String DOWNLOAD_INDCICATOR = " ⇩ ";

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

            @JsonProperty("group")
            private String printerGroup = null;

            private Boolean disabled = null;
            private Boolean ticket = null;
            private Boolean internal = null;
            private Boolean deleted = null;

            public String getContainingText() {
                return containingText;
            }

            @SuppressWarnings("unused")
            public void setContainingText(String containingText) {
                this.containingText = containingText;
            }

            public String getPrinterGroup() {
                return printerGroup;
            }

            @SuppressWarnings("unused")
            public void setPrinterGroup(String printerGroup) {
                this.printerGroup = printerGroup;
            }

            public Boolean getDisabled() {
                return disabled;
            }

            @SuppressWarnings("unused")
            public void setDisabled(Boolean disabled) {
                this.disabled = disabled;
            }

            public Boolean getTicket() {
                return ticket;
            }

            @SuppressWarnings("unused")
            public void setTicket(Boolean ticket) {
                this.ticket = ticket;
            }

            public Boolean getInternal() {
                return internal;
            }

            @SuppressWarnings("unused")
            public void setInternal(Boolean internal) {
                this.internal = internal;
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

        /** */
        private static final String WID_BUTTON_EDIT = "button-edit";

        /** */
        private static final String WID_BUTTON_LOG = "button-log";

        /** */
        private static final String WID_BUTTON_CUPS = "button-cups";

        /** */
        private static final String WID_TICKET_GROUP = "ticketGroup";

        /** */
        private static final String WID_PRINTER_SPARKLINE = "printer-sparkline";

        /** */
        private static final String WID_PRINTER_SNMP = "printer-snmp";

        /** */
        private final boolean isEditor;
        /** */
        private final boolean hasAccessDoc;
        /** */
        private final boolean showSnmp;
        /** */
        private final boolean showCups;

        /** */
        private final boolean isArchiveEnabled;
        /** */
        private final boolean isJournalEnabled;

        /**
         *
         * @param id
         * @param entryList
         * @param isEditor
         */
        public PrintersListView(final String id, final List<Printer> entryList,
                final boolean isEditor, final boolean showCupsBtn) {

            super(id, entryList);

            this.isEditor = isEditor;
            this.hasAccessDoc = ACCESS_CONTROL_SERVICE.hasAccess(
                    SpSession.get().getUserIdDto(), ACLOidEnum.A_DOCUMENTS);

            this.showSnmp = ConfigManager.instance()
                    .isConfigValue(Key.PRINTER_SNMP_ENABLE);

            this.showCups = showCupsBtn;

            this.isArchiveEnabled = DOC_STORE_SERVICE.isEnabled(
                    DocStoreTypeEnum.ARCHIVE, DocStoreBranchEnum.OUT_PRINT);

            this.isJournalEnabled = DOC_STORE_SERVICE.isEnabled(
                    DocStoreTypeEnum.JOURNAL, DocStoreBranchEnum.OUT_PRINT);
        }

        /**
         *
         * @param device
         *            Device.
         * @return Auth mode string.
         */
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

        /**
         * @param helper
         *            Mark-up helper.
         * @param enabled
         *            {@code true} if enabled.
         * @param png
         *            PNG source (src).
         * @param widImg
         *            Wicket ID.
         * @param nounTitle
         *            Title.
         */
        private void addDocStoreImg(final MarkupHelper helper,
                final boolean enabled, final String png, final String widImg,
                final NounEnum nounTitle) {

            if (enabled) {

                final StringBuilder imgSrc = new StringBuilder();
                imgSrc.setLength(0);
                imgSrc.append(WebApp.PATH_IMAGES).append('/');
                imgSrc.append(png);

                MarkupHelper.modifyLabelAttr(
                        helper.addModifyLabelAttr(widImg, MarkupHelper.ATTR_SRC,
                                imgSrc.toString()),
                        MarkupHelper.ATTR_TITLE, nounTitle.uiText(getLocale()));
            } else {
                helper.discloseLabel(widImg);
            }
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
            MarkupHelper
                    .modifyLabelAttr(
                            helper.addModifyLabelAttr("printer-pie",
                                    SparklineHtml.valueString(
                                            printer.getTotalPages().toString(),
                                            printer.getTotalSheets()
                                                    .toString()),
                                    SparklineHtml.ATTR_SLICE_COLORS,
                                    SparklineHtml.arrayAttr(
                                            SparklineHtml.COLOR_PRINTER,
                                            SparklineHtml.COLOR_SHEET)),
                            MarkupHelper.ATTR_CLASS,
                            SparklineHtml.CSS_CLASS_PRINTER);
            /*
             * Sparklines: line.
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

            final StringBuilder sparklineData = new StringBuilder();

            final List<Integer> data = series.getData();

            if (data.size() > 1
                    || data.size() == 1 && data.get(0).intValue() > 0) {

                for (int i = data.size(); i > 0; i--) {
                    if (i < data.size()) {
                        sparklineData.append(",");
                    }
                    sparklineData.append(data.get(i - 1));
                }
            }
            final boolean hasLine = sparklineData.length() > 0;
            labelWrk = helper.encloseLabel(WID_PRINTER_SPARKLINE,
                    sparklineData.toString(), hasLine);

            if (hasLine) {
                MarkupHelper.modifyLabelAttr(labelWrk,
                        SparklineHtml.ATTR_LINE_COLOR,
                        SparklineHtml.COLOR_PRINTER);
                MarkupHelper.modifyLabelAttr(labelWrk,
                        SparklineHtml.ATTR_FILL_COLOR,
                        SparklineHtml.COLOR_PRINTER);
                MarkupHelper.modifyLabelAttr(labelWrk, MarkupHelper.ATTR_CLASS,
                        SparklineHtml.CSS_CLASS_PRINTER);
            }

            //
            addDocStoreImg(helper, this.isArchiveEnabled && !PRINTER_SERVICE
                    .isDocStoreDisabled(DocStoreTypeEnum.ARCHIVE, printer),
                    "archive-16x16.png", "img-archive", NounEnum.ARCHIVE);

            addDocStoreImg(helper, this.isJournalEnabled && !PRINTER_SERVICE
                    .isDocStoreDisabled(DocStoreTypeEnum.JOURNAL, printer),
                    "journal-16x16.png", "img-journal", NounEnum.JOURNAL);

            //
            item.add(new Label("displayName"));

            final Map<String, Device> terminalDevices = new HashMap<>();
            final Map<String, Device> readerDevices = new HashMap<>();

            final HtmlPrinterImgEnum printerImg =
                    getImgSrc(printer, terminalDevices, readerDevices);

            if (printerImg == HtmlPrinterImgEnum.JOBTICKET) {

                helper.addLabel("ticketGroupPrompt",
                        NounEnum.GROUP.uiText(getLocale()));

                helper.addLabel(WID_TICKET_GROUP,
                        PRINTER_SERVICE.getAttributeValue(printer,
                                PrinterAttrEnum.JOBTICKET_PRINTER_GROUP));
            } else {
                helper.discloseLabel(WID_TICKET_GROUP);
            }

            labelWrk = helper.addModifyLabelAttr("printerImage",
                    MarkupHelper.ATTR_SRC, printerImg.urlPath());

            MarkupHelper.modifyLabelAttr(labelWrk, MarkupHelper.ATTR_TITLE,
                    printerImg.uiToolTip(getLocale()));

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
            labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_CLASS, color));
            item.add(labelWrk);

            labelWrk = createVisibleLabel(userGroups.length() > 0, "userGroups",
                    userGroups.toString());
            labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_CLASS, color));

            item.add(labelWrk);

            /*
             * Device URI + Signal
             */
            final JsonProxyPrinter cupsPrinter = PROXY_PRINT_SERVICE
                    .getCachedPrinter(printer.getPrinterName());

            String deviceUriText = "";
            String deviceUriImgUrl = null;

            final int nCupsClassMembers;
            if (cupsPrinter != null) {
                final URI deviceUri = cupsPrinter.getDeviceUri();
                if (deviceUri != null) {
                    deviceUriText = deviceUri.toString();
                    if (PaperCutHelper.isPaperCutPrinter(deviceUri)) {
                        deviceUriImgUrl = WebApp.getThirdPartyEnumImgUrl(
                                ThirdPartyEnum.PAPERCUT);
                    }
                }
                nCupsClassMembers = cupsPrinter.getCupsClassMembers();
            } else {
                nCupsClassMembers = 0;
            }

            item.add(new Label("deviceUri", deviceUriText));

            //
            item.add(createVisibleLabel(nCupsClassMembers > 0,
                    "cups-printer-class-members",
                    String.valueOf(nCupsClassMembers)));

            //
            labelWrk = createVisibleLabel(deviceUriImgUrl != null,
                    "deviceUriImg", "");
            if (deviceUriImgUrl != null) {
                labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_SRC,
                        deviceUriImgUrl));
            }
            item.add(labelWrk);

            //
            final PrinterAttrLookup attrLookup = new PrinterAttrLookup(printer);

            //
            final String filePPDExt =
                    attrLookup.get(PrinterAttrEnum.CUSTOM_PPD_EXT_FILE);
            final boolean hasPPDExt = StringUtils.isNotBlank(filePPDExt);
            final boolean doesPPDExtExist = hasPPDExt
                    && PROXY_PRINT_SERVICE.getPPDExtFile(filePPDExt).exists();

            String namePPDExt = filePPDExt;
            if (doesPPDExtExist) {
                namePPDExt = namePPDExt.concat(DOWNLOAD_INDCICATOR);
            }

            labelWrk =
                    createVisibleLabel(hasPPDExt, "ppd-ext-file", namePPDExt);

            if (hasPPDExt) {
                if (doesPPDExtExist) {
                    color = MarkupHelper.CSS_TXT_VALID;

                    MarkupHelper.modifyLabelAttr(labelWrk,
                            MarkupHelper.ATTR_DATA_SAVAPAGE,
                            printer.getId().toString());

                    MarkupHelper.appendLabelAttr(labelWrk,
                            MarkupHelper.ATTR_CLASS,
                            CSS_CLASS_PRINTER_PPDE_DOWNLOAD);

                    MarkupHelper.appendLabelAttr(labelWrk,
                            MarkupHelper.ATTR_TITLE,
                            localized("title-download-ppde"));

                } else {
                    color = MarkupHelper.CSS_TXT_WARN;
                }
                MarkupHelper.appendLabelAttr(labelWrk, MarkupHelper.ATTR_CLASS,
                        color);
            }
            item.add(labelWrk);

            /*
             * SNMP
             */
            final ProxyPrinterSnmpInfoDto snmpDto;
            final Date snmpDate;

            if (this.showSnmp) {

                snmpDate = PRINTER_ATTR_DAO.getSnmpDate(attrLookup);

                final String json = PRINTER_ATTR_DAO.getSnmpJson(attrLookup);
                if (json == null) {
                    snmpDto = null;
                } else {
                    snmpDto = PRINTER_SERVICE.getSnmpInfo(json);
                    if (snmpDto == null) {
                        this.removeSnmpAttr(printer);
                    }
                }
            } else {
                snmpDto = null;
                snmpDate = null;
            }

            if (snmpDate == null && snmpDto == null) {
                helper.discloseLabel(WID_PRINTER_SNMP);
            } else {
                final Long printerID;
                if (this.isEditor) {
                    printerID = printer.getId();
                } else {
                    printerID = null;
                }
                item.add(new PrinterSnmpPanel("printer-snmp", printerID,
                        snmpDate, snmpDto, false));
            }

            //
            final boolean isInternal =
                    PRINTER_ATTR_DAO.isInternalPrinter(attrLookup);

            final boolean isConfigured = cupsPrinter == null
                    || !cupsPrinter.isPpdPresent() || PROXY_PRINT_SERVICE
                            .isPrinterConfigured(cupsPrinter, attrLookup);

            item.add(createVisibleLabel(!isConfigured,
                    "printer-needs-configuration",
                    localized("printer-needs-configuration")));

            //
            String signalKey = null;
            color = null;

            final Label labelPrinterName = new Label("printerName");
            item.add(labelPrinterName);

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

                helper.discloseLabel("driver");
                helper.discloseLabel("manufacturer");

            } else {

                MarkupHelper.modifyLabelAttr(labelPrinterName,
                        MarkupHelper.ATTR_DATA_SAVAPAGE,
                        printer.getId().toString());

                MarkupHelper.modifyLabelAttr(labelPrinterName,
                        MarkupHelper.ATTR_CLASS,
                        CSS_CLASS_PRINTER_OPT_DOWNLOAD);

                MarkupHelper.appendLabelAttr(labelPrinterName,
                        MarkupHelper.ATTR_TITLE,
                        localized("title-download-ipp"));

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

                String nameWlk = cupsPrinter.getModelName();
                if (StringUtils.isBlank(nameWlk)) {
                    nameWlk = null;
                } else {
                    if (cupsPrinter.isPpdPresent()) {
                        nameWlk = nameWlk.concat(DOWNLOAD_INDCICATOR);
                    }
                }

                labelWrk = helper.addLabel("driver",
                        StringUtils.defaultString(nameWlk, "-"));

                if (nameWlk != null && cupsPrinter.isPpdPresent()) {

                    MarkupHelper.modifyLabelAttr(labelWrk,
                            MarkupHelper.ATTR_DATA_SAVAPAGE,
                            printer.getId().toString());

                    MarkupHelper.appendLabelAttr(labelWrk,
                            MarkupHelper.ATTR_CLASS,
                            CSS_CLASS_PRINTER_PPD_DOWNLOAD);

                    MarkupHelper.appendLabelAttr(labelWrk,
                            MarkupHelper.ATTR_TITLE,
                            localized("title-download-ppd"));
                }
                helper.encloseLabel("manufacturer",
                        cupsPrinter.getManufacturer(),
                        StringUtils.isNotBlank(cupsPrinter.getManufacturer()));
            }

            String signal = "";

            if (signalKey != null) {
                signal = localized(signalKey);
            }

            labelWrk = new Label("signal", signal);

            if (color != null) {
                labelWrk.add(
                        new AttributeModifier(MarkupHelper.ATTR_CLASS, color));
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
                totals.append(" &bull; " + helper.localizedNumber(total));
                key = (total == 1) ? "page" : "pages";
                totals.append(" ").append(localized(key));

                //
                total = printer.getTotalSheets();
                totals.append(" &bull; " + helper.localizedNumber(total));
                key = (total == 1) ? "sheet" : "sheets";
                totals.append(" ").append(localized(key));

                //
                totals.append(" &bull; ")
                        .append(NumberUtil.humanReadableByteCountSI(getLocale(),
                                printer.getTotalBytes()));
            }

            item.add(new Label("period", period.toString()));

            labelWrk = new Label("totals", totals.toString());
            labelWrk.setEscapeModelStrings(false);
            item.add(labelWrk);

            /*
             * Set the uid in 'data-savapage' attribute, so it can be picked up
             * in JavaScript for editing.
             */
            if (this.isEditor) {
                labelWrk = new Label(WID_BUTTON_EDIT,
                        getLocalizer().getString("button-edit", this));
                MarkupHelper.modifyLabelAttr(labelWrk,
                        MarkupHelper.ATTR_DATA_SAVAPAGE,
                        printer.getId().toString());
                MarkupHelper.modifyLabelAttr(labelWrk, MarkupHelper.ATTR_TITLE,
                        HtmlButtonEnum.EDIT.uiText(getLocale()));
                item.add(labelWrk);
            } else {
                helper.discloseLabel(WID_BUTTON_EDIT);
            }

            if (this.hasAccessDoc) {
                labelWrk = new Label(WID_BUTTON_LOG,
                        getLocalizer().getString("button-log", this));
                MarkupHelper.modifyLabelAttr(labelWrk,
                        MarkupHelper.ATTR_DATA_SAVAPAGE,
                        printer.getId().toString());
                MarkupHelper.modifyLabelAttr(labelWrk, MarkupHelper.ATTR_TITLE,
                        NounEnum.DOCUMENT.uiText(getLocale(), true));
                item.add(labelWrk);
            } else {
                helper.discloseLabel(WID_BUTTON_LOG);
            }

            //
            final boolean showButtonCups =
                    this.showCups && this.isEditor && cupsPrinter != null;

            if (showButtonCups) {
                MarkupHelper
                        .modifyLabelAttr(
                                helper.encloseLabel(WID_BUTTON_CUPS,
                                        getLocalizer().getString("button-cups",
                                                this),
                                        true),
                                MarkupHelper.ATTR_HREF,
                                getCupsPrinterUrl(printer.getPrinterName()));
            } else {
                helper.discloseLabel(WID_BUTTON_CUPS);
            }

            helper.addTransparantInvisible("sect-buttons",
                    !this.isEditor && !this.hasAccessDoc);
        }

        /**
         * Removes SNMP attributes from printer.
         *
         * @param printer
         *            The printer.
         */
        private void removeSnmpAttr(final Printer printer) {
            final DaoContext ctx = ServiceContext.getDaoContext();

            ctx.beginTransaction();
            try {
                PRINTER_SERVICE.removeSnmpAttr(printer);
                ctx.commit();
                LOGGER.warn("Removed SNMP info from printer {} ({}).",
                        printer.getPrinterName(), printer.getDisplayName());
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
                ctx.rollback();
            }
        }

    }

    /**
     * @param parameters
     *            The page parameters.
     */
    public PrintersPage(final PageParameters parameters) {

        super(parameters);

        /*
         * Check for new/changed printers.
         */
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
        filter.setPrinterGroup(req.getSelect().getPrinterGroup());
        filter.setDeleted(req.getSelect().getDeleted());
        filter.setInternal(req.getSelect().getInternal());
        filter.setJobTicket(req.getSelect().getTicket());
        filter.setDisabled(req.getSelect().getDisabled());

        final long printerCount = printerDao.getListCount(filter);

        /*
         * Display the requested page.
         */
        final List<Printer> entryList = printerDao.getListChunk(filter,
                req.calcStartPosition(), req.getMaxResults(),
                PrinterDao.Field.DISPLAY_NAME, req.getSort().getAscending());

        add(new PrintersListView("printers-view", entryList,
                this.probePermissionToEdit(ACLOidEnum.A_PRINTERS),
                this.isIntranetRequest()));

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
