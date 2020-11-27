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
import java.text.DateFormat;
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
import org.savapage.core.dao.DeviceDao;
import org.savapage.core.dao.DeviceDao.Field;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.DeviceTypeEnum;
import org.savapage.core.dao.enums.ProxyPrintAuthModeEnum;
import org.savapage.core.dao.helpers.AbstractPagerReq;
import org.savapage.core.dto.RfIdReaderStatusDto;
import org.savapage.core.jpa.Device;
import org.savapage.core.jpa.PrinterGroup;
import org.savapage.core.jpa.PrinterGroupMember;
import org.savapage.core.services.DeviceService;
import org.savapage.core.services.RfIdReaderService;
import org.savapage.core.services.ServiceContext;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.helpers.HtmlDeviceImgEnum;
import org.savapage.server.pages.MarkupHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class DevicesPage extends AbstractAdminListPage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(DevicesPage.class);

    /**
     * Note: must be odd number.
     */
    private static final int MAX_PAGES_IN_NAVBAR = 5;

    /** */
    private static final DeviceService DEVICE_SERVICE =
            ServiceContext.getServiceFactory().getDeviceService();

    /** */
    private static final RfIdReaderService RFID_READER_SERVICE =
            ServiceContext.getServiceFactory().getRfIdReaderService();

    /** */
    private static final DeviceDao DEVICE_DAO =
            ServiceContext.getDaoContext().getDeviceDao();

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
            private Boolean reader = null;

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

            public Boolean getReader() {
                return reader;
            }

            @SuppressWarnings("unused")
            public void setReader(Boolean reader) {
                this.reader = reader;
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
    private final class DeviceListView extends PropertyListView<Device> {

        /** */
        private static final long serialVersionUID = 1L;

        /** */
        private static final String WID_BUTTON_EDIT = "button-edit";

        /** */
        private final DateFormat dateFormat;

        /***/
        private final boolean isEditor;

        /**
         *
         * @param id
         * @param list
         */
        public DeviceListView(final String id, final List<Device> list,
                final boolean isEditor) {

            super(id, list);

            this.dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
                    DateFormat.MEDIUM, getLocale());

            this.isEditor = isEditor;
        }

        @Override
        protected void populateItem(final ListItem<Device> item) {

            final Device device =
                    DEVICE_DAO.findById(item.getModelObject().getId());

            Label labelWrk = null;

            /*
             *
             */
            item.add(new Label("deviceName"));
            item.add(new Label("location"));

            String signalKey = null;
            String color = null;

            /*
             * Network Reader Connected?
             */
            String readerConnectStatus = null;

            if (!device.getDisabled() && DEVICE_DAO.isCardReader(device)) {

                RfIdReaderStatusDto status =
                        RFID_READER_SERVICE.getReaderStatus(
                                device.getHostname(), device.getPort());

                if (status.isConnected()) {
                    color = MarkupHelper.CSS_TXT_VALID;
                    signalKey = "signal-connected";
                } else {
                    color = MarkupHelper.CSS_TXT_ERROR;
                    signalKey = "signal-disconnected";
                }

                readerConnectStatus = localized(signalKey,
                        dateFormat.format(status.getDate()));
            } else {
                readerConnectStatus = "";
            }

            item.add(createVisibleLabel(DEVICE_DAO.isCardReader(device),
                    "readerConnectStatus", readerConnectStatus,
                    color + " " + MarkupHelper.CSS_TXT_WRAP));

            /*
             * Signal
             */
            String hostname = device.getHostname();

            if (device.getPort() != null && device.getPort() > 0) {
                hostname += ":" + device.getPort();
            }
            item.add(new Label("hostname", hostname));

            if (device.getDisabled()) {
                color = MarkupHelper.CSS_TXT_ERROR;
                signalKey = "signal-disabled";
            } else {
                color = MarkupHelper.CSS_TXT_VALID;
                signalKey = "signal-active";
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

            /*
             *
             */
            String assocTerminal = null;
            String assocCardReader = null;

            if (device.getCardReader() != null) {
                assocCardReader = device.getCardReader().getDisplayName();
            } else if (device.getCardReaderTerminal() != null) {
                assocTerminal = device.getCardReaderTerminal().getDisplayName();
            }

            item.add(createVisibleLabel(assocTerminal != null, "assocTerminal",
                    assocTerminal));
            item.add(createVisibleLabel(assocCardReader != null,
                    "assocCardReader", assocCardReader));

            /*
             * Authenticated printing.
             */
            String printerAuth = null;
            String printerGroupAuth = null;

            if (device.getPrinter() != null) {
                printerAuth = device.getPrinter().getPrinterName();
            } else {
                PrinterGroup group = device.getPrinterGroup();
                if (group != null) {
                    printerGroupAuth = group.getDisplayName() + " (";
                    String members = null;
                    for (PrinterGroupMember member : group.getMembers()) {
                        if (members == null) {
                            members = "";
                        } else {
                            members += ", ";
                        }
                        members += member.getPrinter().getDisplayName();
                    }
                    if (StringUtils.isBlank(members)) {
                        members = "-";
                    }
                    printerGroupAuth += members + ")";
                }
            }

            /*
             * Authenticated printing mode.
             */
            String proxyPrintAuthMode = null;

            if (printerAuth != null || printerGroupAuth != null) {

                final ProxyPrintAuthModeEnum authModeEnum =
                        DEVICE_SERVICE.getProxyPrintAuthMode(device.getId());

                if (authModeEnum == null) {

                    proxyPrintAuthMode = "";

                } else {
                    proxyPrintAuthMode = "&bull;&nbsp;";

                    switch (authModeEnum) {
                    case DIRECT:
                        proxyPrintAuthMode += "Direct";
                        break;

                    case FAST:
                        proxyPrintAuthMode += "Fast";
                        break;

                    case FAST_DIRECT:
                        proxyPrintAuthMode += "Fast &bull; Direct";
                        break;

                    case FAST_HOLD:
                        proxyPrintAuthMode += "Fast &bull; Hold";
                        break;

                    case HOLD:
                        proxyPrintAuthMode += "Hold";
                        break;

                    default:
                        throw new SpException("Oops, missed auth mode ["
                                + authModeEnum + "]");
                    }

                }

            }
            item.add(createVisibleLabel(printerAuth != null, "printerAuthMode",
                    proxyPrintAuthMode).setEscapeModelStrings(false));
            item.add(createVisibleLabel(printerGroupAuth != null,
                    "printerGroupAuthMode", proxyPrintAuthMode)
                            .setEscapeModelStrings(false));

            item.add(createVisibleLabel(printerAuth != null, "printerAuth",
                    printerAuth));
            item.add(createVisibleLabel(printerGroupAuth != null,
                    "printerGroupAuth", printerGroupAuth));

            //
            final MarkupHelper helper = new MarkupHelper(item);

            /*
             * Device Image
             */
            final HtmlDeviceImgEnum deviceImg;

            if (device.getCardReader() != null) {
                deviceImg = HtmlDeviceImgEnum.TERMINAL_READER_LOGON;
            } else if (device.getCardReaderTerminal() != null) {
                deviceImg = HtmlDeviceImgEnum.READER_LOGON;
            } else if (device.getDeviceType()
                    .equals(DeviceTypeEnum.CARD_READER.toString())) {
                deviceImg = HtmlDeviceImgEnum.READER_PRINT_AUTH;
            } else {
                deviceImg = HtmlDeviceImgEnum.TERMINAL;
            }

            labelWrk = helper.addModifyLabelAttr("deviceImage",
                    MarkupHelper.ATTR_SRC, deviceImg.urlPath());
            MarkupHelper.modifyLabelAttr(labelWrk, MarkupHelper.ATTR_TITLE,
                    deviceImg.uiToolTip(getLocale()));

            item.add(labelWrk);

            /*
             * Set the uid in 'data-savapage' attribute, so it can be picked up
             * in JavaScript for editing.
             */
            if (this.isEditor) {
                MarkupHelper.modifyLabelAttr(
                        MarkupHelper.modifyLabelAttr(
                                helper.encloseLabel(WID_BUTTON_EDIT,
                                        getLocalizer().getString("button-edit",
                                                this),
                                        true),
                                MarkupHelper.ATTR_DATA_SAVAPAGE,
                                device.getId().toString()),
                        MarkupHelper.ATTR_TITLE,
                        HtmlButtonEnum.EDIT.uiText(getLocale()));

                item.add(labelWrk);
            } else {
                helper.discloseLabel(WID_BUTTON_EDIT);
            }
        }
    }

    /**
     * @param parameters
     *            The page parameters.
     */
    public DevicesPage(final PageParameters parameters) {

        super(parameters);

        final Req req = readReq();

        final String containingText = req.getSelect().getContainingText();
        final Boolean reader = req.getSelect().getReader();

        //
        final DeviceDao.ListFilter filter = new DeviceDao.ListFilter();
        if (reader != null) {
            if (reader.booleanValue()) {
                filter.setDeviceType(DeviceTypeEnum.CARD_READER);
            } else {
                filter.setDeviceType(DeviceTypeEnum.TERMINAL);
            }
        }
        filter.setContainingText(containingText);
        filter.setDisabled(req.getSelect().getDisabled());

        //
        final long devicesCount = DEVICE_DAO.getListCount(filter);

        final List<Device> entryList = DEVICE_DAO.getListChunk(filter,
                req.calcStartPosition(), req.getMaxResults(), Field.NAME,
                req.getSort().getAscending());

        /*
         * Display the requested page.
         */
        add(new DeviceListView("devices-view", entryList,
                this.probePermissionToEdit(ACLOidEnum.A_DEVICES)));

        /*
         * Display the navigation bars and write the response.
         */
        createNavBarResponse(req, devicesCount, MAX_PAGES_IN_NAVBAR,
                "sp-devices-page", new String[] { "nav-bar-1", "nav-bar-2" });
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
