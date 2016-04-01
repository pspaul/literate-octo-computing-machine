/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
 * Authors: Rijk Ravestein.
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
package org.savapage.server.api.request;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.savapage.core.config.IConfigProp;
import org.savapage.core.dao.DeviceDao;
import org.savapage.core.dao.enums.DeviceTypeEnum;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.jpa.Device;
import org.savapage.core.jpa.DeviceAttr;
import org.savapage.core.jpa.User;
import org.savapage.core.services.ServiceContext;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqDeviceGet extends ApiRequestMixin {

    /**
     *
     * The request.
     *
     */
    private static class DtoReq extends AbstractDto {

        private Long id;

        public Long getId() {
            return id;
        }

        @SuppressWarnings("unused")
        public void setId(Long id) {
            this.id = id;
        }
    }

    /**
     * The response.
     */
    @JsonInclude(Include.NON_NULL)
    public static class DtoRsp extends AbstractDto {

        private Long id;

        private DeviceTypeEnum deviceType;
        private String deviceName;
        private String displayName;
        private String location;
        private String notes;
        private String hostname;
        private Integer port;
        private Boolean disabled;

        private String printerName;
        private String printerGroup;
        private String readerName;
        private String terminalName;

        private Map<String, Object> attr;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public DeviceTypeEnum getDeviceType() {
            return deviceType;
        }

        public void setDeviceType(DeviceTypeEnum deviceType) {
            this.deviceType = deviceType;
        }

        public String getDeviceName() {
            return deviceName;
        }

        public void setDeviceName(String deviceName) {
            this.deviceName = deviceName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public Boolean getDisabled() {
            return disabled;
        }

        public void setDisabled(Boolean disabled) {
            this.disabled = disabled;
        }

        public String getPrinterName() {
            return printerName;
        }

        public void setPrinterName(String printerName) {
            this.printerName = printerName;
        }

        public String getPrinterGroup() {
            return printerGroup;
        }

        public void setPrinterGroup(String printerGroup) {
            this.printerGroup = printerGroup;
        }

        public String getReaderName() {
            return readerName;
        }

        public void setReaderName(String readerName) {
            this.readerName = readerName;
        }

        public String getTerminalName() {
            return terminalName;
        }

        public void setTerminalName(String terminalName) {
            this.terminalName = terminalName;
        }

        public Map<String, Object> getAttr() {
            return attr;
        }

        public void setAttr(Map<String, Object> attr) {
            this.attr = attr;
        }

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final DtoReq dtoReq = DtoReq.create(DtoReq.class, getParmValue("dto"));

        final DeviceDao deviceDao =
                ServiceContext.getDaoContext().getDeviceDao();

        final Device device = deviceDao.findById(dtoReq.getId());

        if (device == null) {
            setApiResult(ApiResultCodeEnum.ERROR, "msg-device-not-found",
                    dtoReq.getId().toString());
            return;
        }

        final DtoRsp dtoRsp = new DtoRsp();

        dtoRsp.setId(device.getId());
        dtoRsp.setDeviceType(DeviceTypeEnum.valueOf(device.getDeviceType()));
        dtoRsp.setDeviceName(device.getDeviceName());
        dtoRsp.setDisplayName(device.getDisplayName());
        dtoRsp.setLocation(device.getLocation());
        dtoRsp.setNotes(device.getNotes());
        dtoRsp.setHostname(device.getHostname());
        dtoRsp.setPort(device.getPort());
        dtoRsp.setDisabled(device.getDisabled());

        if (device.getPrinter() != null) {
            dtoRsp.setPrinterName(device.getPrinter().getPrinterName());
        }

        if (device.getPrinterGroup() != null) {
            dtoRsp.setPrinterGroup(device.getPrinterGroup().getDisplayName());
        }

        if (device.getCardReader() != null) {
            dtoRsp.setReaderName(device.getCardReader().getDeviceName());
        }

        if (device.getCardReaderTerminal() != null) {
            dtoRsp.setTerminalName(
                    device.getCardReaderTerminal().getDeviceName());
        }

        if (device.getAttributes() != null) {

            Map<String, Object> attrMap = new HashMap<String, Object>();

            for (DeviceAttr attr : device.getAttributes()) {

                final String value = attr.getValue();

                if (value.equals(IConfigProp.V_YES)
                        || value.equals(IConfigProp.V_NO)) {

                    attrMap.put(attr.getName(),
                            Boolean.valueOf(value.equals(IConfigProp.V_YES)));
                } else {
                    attrMap.put(attr.getName(), attr.getValue());
                }

            }

            dtoRsp.setAttr(attrMap);
        }

        this.setResponse(dtoRsp);
        setApiResultOk();
    }

}
