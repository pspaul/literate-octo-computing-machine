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
package org.savapage.server.api.request;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.dao.DeviceDao;
import org.savapage.core.dao.PrinterDao;
import org.savapage.core.dao.PrinterGroupDao;
import org.savapage.core.dao.enums.DeviceAttrEnum;
import org.savapage.core.dao.enums.ProxyPrintAuthModeEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.jpa.Device;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.PrinterGroup;
import org.savapage.core.jpa.User;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.InetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqDeviceSet extends ApiRequestMixin {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ReqDeviceSet.class);

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final ReqDeviceGet.DtoRsp dtoReq = ReqDeviceGet.DtoRsp
                .create(ReqDeviceGet.DtoRsp.class, this.getParmValueDto());

        final DeviceDao deviceDao =
                ServiceContext.getDaoContext().getDeviceDao();

        final boolean isNew = dtoReq.getId() == null;
        final Date now = new Date();
        final String deviceName = dtoReq.getDeviceName();

        // Force displayName == devicenName (Mantis #1105)
        dtoReq.setDisplayName(deviceName);

        // INVARIANT: Mantis #1105
        if (StringUtils.isBlank(deviceName)) {
            setApiResult(ApiResultCodeEnum.WARN, "msg-value-cannot-be-empty",
                    NounEnum.NAME.uiText(getLocale()));
            return;
        }

        // INVARIANT: Mantis #1105
        if (StringUtils.isBlank(dtoReq.getHostname())) {
            setApiResult(ApiResultCodeEnum.WARN, "msg-value-cannot-be-empty",
                    "IP");
            return;
        }

        if (!InetUtils.isInetAddressValid(dtoReq.getHostname())) {
            setApiResult(ApiResultCodeEnum.WARN, "msg-value-invalid", "IP",
                    dtoReq.getHostname());
            return;
        }

        final PrinterDao printerDao =
                ServiceContext.getDaoContext().getPrinterDao();

        final PrinterGroupDao printerGroupDao =
                ServiceContext.getDaoContext().getPrinterGroupDao();

        /*
         * Note: returns null when not found.
         */
        final Device jpaDeviceDuplicate = deviceDao.findByName(deviceName);

        Device jpaDevice = null;

        boolean isDuplicate = true;

        if (isNew) {

            if (jpaDeviceDuplicate == null) {

                jpaDevice = new Device();
                jpaDevice.setCreatedBy(requestingUser);
                jpaDevice.setCreatedDate(now);

                isDuplicate = false;
            }

        } else {

            jpaDevice = deviceDao.findById(dtoReq.getId());

            if (jpaDeviceDuplicate == null
                    || jpaDeviceDuplicate.getId().equals(jpaDevice.getId())) {

                jpaDevice.setModifiedBy(requestingUser);
                jpaDevice.setModifiedDate(now);

                isDuplicate = false;
            }
        }

        // INVARIANT
        if (isDuplicate) {
            setApiResult(ApiResultCodeEnum.ERROR, "msg-device-duplicate-name",
                    deviceName);
            return;
        }

        final Device jpaDeviceDuplicateHost = deviceDao.findByHostDeviceType(
                dtoReq.getHostname(), dtoReq.getDeviceType());
        isDuplicate = true;
        if (isNew) {
            if (jpaDeviceDuplicateHost == null) {
                isDuplicate = false;
            }
        } else {
            if (jpaDeviceDuplicateHost == null || jpaDeviceDuplicateHost.getId()
                    .equals(jpaDevice.getId())) {
                isDuplicate = false;
            }
        }

        //
        if (!this.checkDuplicateHostIP(deviceDao, isNew, dtoReq)) {
            return;
        }

        //
        jpaDevice.setDeviceType(dtoReq.getDeviceType().toString());
        jpaDevice.setDeviceName(deviceName);
        jpaDevice.setDisplayName(dtoReq.getDisplayName());
        jpaDevice.setLocation(dtoReq.getLocation());
        jpaDevice.setNotes(dtoReq.getNotes());
        jpaDevice.setHostname(dtoReq.getHostname());
        jpaDevice.setDisabled(dtoReq.getDisabled());

        final boolean isTerminal = deviceDao.isTerminal(jpaDevice);

        final Integer iPort;

        if (deviceDao.isCardReader(jpaDevice)) {
            iPort = dtoReq.getPort();
            if (iPort == null) {
                setApiResult(ApiResultCodeEnum.ERROR, "msg-device-port-error");
                return;
            }
        } else {
            iPort = null;
        }
        jpaDevice.setPort(iPort);

        /*
         * Get JSON Attributes
         */
        final Map<String, Object> jsonAttributes = dtoReq.getAttr();

        /*
         * Reference to Card Reader.
         */
        Device jpaCardReader = null;

        final String cardReaderName = dtoReq.getReaderName();

        final Boolean authModeCardIp;

        if (jsonAttributes == null) {
            authModeCardIp = null;
        } else {
            authModeCardIp = (Boolean) jsonAttributes
                    .get(DeviceAttrEnum.AUTH_MODE_CARD_IP.getDbName());
        }

        final boolean linkCardReader =
                authModeCardIp != null && authModeCardIp.booleanValue();

        if (linkCardReader) {
            /*
             * INVARIANT: Card Reader must be specified.
             */
            if (StringUtils.isBlank(cardReaderName)) {
                setApiResult(ApiResultCodeEnum.ERROR,
                        "msg-device-card-reader-missing");
                return;
            }

            jpaCardReader = deviceDao.findByName(cardReaderName);

            /*
             * INVARIANT: Card Reader must exist.
             */
            if (jpaCardReader == null) {
                setApiResult(ApiResultCodeEnum.ERROR,
                        "msg-device-card-reader-not-found", cardReaderName);
                return;
            }

            /*
             * INVARIANT: Card Reader must not already be a Login Authenticator.
             */
            Device jpaCardReaderTerminal =
                    jpaCardReader.getCardReaderTerminal();

            if (jpaCardReaderTerminal != null) {
                if (isNew || !jpaDevice.getId()
                        .equals(jpaCardReaderTerminal.getId())) {
                    setApiResult(ApiResultCodeEnum.ERROR,
                            "msg-device-card-reader-reserved-for-terminal",
                            cardReaderName,
                            jpaCardReaderTerminal.getDisplayName());
                    return;
                }
            }

            /*
             * INVARIANT: Card Reader must not already be a Proxy Print
             * Authenticator.
             */
            if (jpaCardReader.getPrinter() != null) {
                setApiResult(ApiResultCodeEnum.ERROR,
                        "msg-device-card-reader-reserved-for-printer",
                        cardReaderName,
                        jpaCardReader.getPrinter().getPrinterName());
                return;
            }
            if (jpaCardReader.getPrinterGroup() != null) {
                setApiResult(ApiResultCodeEnum.ERROR,
                        "msg-device-card-reader-reserved-for-printer-group",
                        cardReaderName,
                        jpaCardReader.getPrinterGroup().getDisplayName());
                return;
            }
        }

        jpaDevice.setCardReader(jpaCardReader);

        if (jpaCardReader != null) {
            jpaCardReader.setCardReaderTerminal(jpaDevice);
        }

        /*
         * Screening and determination of attribute values.
         */
        ProxyPrintAuthModeEnum printAuthMode = null;

        if (jsonAttributes != null) {

            for (final Entry<String, Object> entry : jsonAttributes
                    .entrySet()) {

                final String attrKey = entry.getKey();
                final Object attrValue = entry.getValue();

                /*
                 * INVARIANT: USER_MAX_IDLE_SECS must be numeric.
                 */
                if (attrKey.equals(
                        DeviceAttrEnum.WEBAPP_USER_MAX_IDLE_SECS.getDbName())
                        && !StringUtils.isNumeric(attrValue.toString())) {

                    setApiResult(ApiResultCodeEnum.ERROR,
                            "msg-device-idle-seconds-error");
                    return;
                }

                /*
                 * PROXY_PRINT_AUTH_MODE
                 */
                if (attrKey.equals(
                        DeviceAttrEnum.PROXY_PRINT_AUTH_MODE.getDbName())) {
                    printAuthMode = ProxyPrintAuthModeEnum
                            .valueOf(attrValue.toString());
                }
            }
        }

        //
        final String printerName;
        final String printerGroup;

        if (StringUtils.isBlank(dtoReq.getPrinterName())) {
            printerName = null;
        } else {
            printerName = dtoReq.getPrinterName();
        }

        if (StringUtils.isBlank(dtoReq.getPrinterGroup())) {
            printerGroup = null;
        } else {
            printerGroup = dtoReq.getPrinterGroup();
        }

        final PrinterGroup jpaPrinterGroup;
        final Printer jpaPrinter;

        if (isTerminal) {

            if (printerName != null && printerGroup != null) {
                setApiResult(ApiResultCodeEnum.ERROR,
                        "msg-device-proxy-printer-either-group");
                return;
            }

            if (printerName == null) {
                jpaPrinter = null;
            } else {
                jpaPrinter = this.getPrinter(printerDao, jpaDevice.getPrinter(),
                        printerName);
                if (jpaPrinter == null) {
                    setApiResult(ApiResultCodeEnum.ERROR,
                            "msg-device-printer-not-found", printerName);
                    return;
                }
            }

            if (printerGroup == null) {
                jpaPrinterGroup = null;
            } else {
                jpaPrinterGroup = this.getPrinterGroup(printerGroupDao,
                        jpaDevice.getPrinterGroup(), printerGroup);

                if (jpaPrinterGroup == null) {
                    setApiResult(ApiResultCodeEnum.ERROR,
                            "msg-device-printer-group-not-found", printerGroup);
                    return;
                }
            }

        } else if (printAuthMode == null) {
            /*
             * No authenticated proxy print applicable.
             */
            jpaPrinter = null;
            jpaPrinterGroup = null;

        } else {
            /*
             * Authenticated proxy print for single printer or printer group.
             */

            if (printerGroup == null && printerName == null) {
                setApiResult(ApiResultCodeEnum.ERROR,
                        "msg-device-proxy-printer-or-group-needed");
                return;
            }

            if (printAuthMode == ProxyPrintAuthModeEnum.FAST) {
                /*
                 * INVARIANT: FAST proxy print MUST target a SINGLE printer. A
                 * printer group is irrelevant for FAST proxy print.
                 */
                if (printerName == null || printerGroup != null) {
                    setApiResult(ApiResultCodeEnum.ERROR,
                            "msg-device-single-proxy-printer-needed");
                    return;
                }

            } else if (printAuthMode == ProxyPrintAuthModeEnum.FAST_DIRECT
                    || printAuthMode == ProxyPrintAuthModeEnum.FAST_HOLD) {
                /*
                 * INVARIANT: FAST proxy print MUST target a SINGLE printer.
                 */
                if (printerGroup != null && printerName == null) {
                    setApiResult(ApiResultCodeEnum.ERROR,
                            "msg-device-single-proxy-printer-needed");
                    return;
                }
            } else if (printerGroup != null && printerName != null) {
                setApiResult(ApiResultCodeEnum.ERROR,
                        "msg-device-proxy-printer-either-group");
                return;
            }

            /*
             * Single Printer.
             */
            if (printerName == null) {

                jpaPrinter = null;

            } else {

                final Printer jpaPrinterWlk = this.getPrinter(printerDao,
                        jpaDevice.getPrinter(), printerName);

                if (jpaPrinterWlk == null) {
                    setApiResult(ApiResultCodeEnum.ERROR,
                            "msg-device-printer-not-found", printerName);
                    return;
                }

                jpaPrinter = jpaPrinterWlk;
            }

            /*
             * Printer Group.
             */
            if (printerGroup == null) {

                jpaPrinterGroup = null;

            } else {

                final PrinterGroup jpaPrinterGroupWlk =
                        this.getPrinterGroup(printerGroupDao,
                                jpaDevice.getPrinterGroup(), printerGroup);

                if (jpaPrinterGroupWlk == null) {
                    setApiResult(ApiResultCodeEnum.ERROR,
                            "msg-device-printer-group-not-found", printerGroup);
                    return;
                }

                jpaPrinterGroup = jpaPrinterGroupWlk;
            }
        }

        jpaDevice.setPrinter(jpaPrinter);
        jpaDevice.setPrinterGroup(jpaPrinterGroup);

        /*
         * Device: Persist | Merge
         */
        String resultMsgKey;

        if (isNew) {
            deviceDao.create(jpaDevice);
            resultMsgKey = "msg-device-created-ok";

        } else {
            deviceDao.update(jpaDevice);
            resultMsgKey = "msg-device-saved-ok";
        }

        /*
         * Write Attributes.
         */
        if (jsonAttributes != null) {

            for (final Entry<String, Object> entry : jsonAttributes
                    .entrySet()) {

                final String value;

                if (entry.getValue() instanceof Boolean) {
                    final Boolean booleanWlk = (Boolean) entry.getValue();

                    if (booleanWlk.booleanValue()) {
                        value = IConfigProp.V_YES;
                    } else {
                        value = IConfigProp.V_NO;
                    }

                } else {
                    value = entry.getValue().toString();
                }

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(String.format("write [%s] : [%s]",
                            entry.getKey(), value));
                }

                deviceDao.writeAttribute(jpaDevice, entry.getKey(), value);
            }
        }

        setApiResult(ApiResultCodeEnum.OK, resultMsgKey);
    }

    /**
     * Checks presence of a duplicate Host/IP device for a device type.
     *
     * @param deviceDao
     *            DAO.
     * @param isNew
     *            {@code true} if Add request.
     * @param dtoReq
     *            Add/Update request.
     * @return {@code true} if OK (no duplicate present).
     */
    private boolean checkDuplicateHostIP(final DeviceDao deviceDao,
            final boolean isNew, final ReqDeviceGet.DtoRsp dtoReq) {

        final Device jpaDeviceDuplicateHost = deviceDao.findByHostDeviceType(
                dtoReq.getHostname(), dtoReq.getDeviceType());

        boolean isDuplicate = true;

        if (isNew) {
            if (jpaDeviceDuplicateHost == null) {
                isDuplicate = false;
            }
        } else {
            if (jpaDeviceDuplicateHost == null
                    || jpaDeviceDuplicateHost.getId().equals(dtoReq.getId())) {
                isDuplicate = false;
            }
        }

        if (isDuplicate) {
            setApiResult(ApiResultCodeEnum.ERROR, "msg-device-duplicate-name",
                    dtoReq.getHostname());
            return false;
        }
        return true;
    }

    /**
     * Gets the printer for a device.
     *
     * @param printerDao
     *            The DAO.
     * @param printerCurrent
     *            The current {@link Printer} (can be {@code null}.
     * @param printerName
     *            The printer name.
     * @return The {@link Printer}.
     */
    private Printer getPrinter(final PrinterDao printerDao,
            final Printer printerCurrent, final String printerName) {

        if (printerCurrent != null
                && printerCurrent.getPrinterName().equals(printerName)) {
            return printerCurrent;
        }
        return printerDao.findByName(printerName);
    }

    /**
     * Gets the printer group for a device.
     *
     * @param printerGroupDao
     * @param printerGroupCurrent
     * @param printerGroup
     * @return The {@link PrinterGroup}.
     */
    private PrinterGroup getPrinterGroup(final PrinterGroupDao printerGroupDao,
            final PrinterGroup printerGroupCurrent, final String printerGroup) {

        if (printerGroupCurrent != null
                && printerGroupCurrent.getGroupName().equals(printerGroup)) {
            return printerGroupCurrent;
        }
        return printerGroupDao.findByName(printerGroup);
    }

}
