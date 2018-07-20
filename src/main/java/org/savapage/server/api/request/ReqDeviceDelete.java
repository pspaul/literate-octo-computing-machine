/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.api.request;

import java.io.IOException;

import org.savapage.core.dao.DeviceDao;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.jpa.Device;
import org.savapage.core.jpa.User;
import org.savapage.core.services.ServiceContext;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqDeviceDelete extends ApiRequestMixin {

    /**
     *
     * The request.
     *
     */
    private static class DtoReq extends AbstractDto {

        private Long id;
        private String deviceName;

        public Long getId() {
            return id;
        }

        @SuppressWarnings("unused")
        public void setId(Long id) {
            this.id = id;
        }

        public String getDeviceName() {
            return deviceName;
        }

        @SuppressWarnings("unused")
        public void setDeviceName(String deviceName) {
            this.deviceName = deviceName;
        }

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final DtoReq dtoReq =
                DtoReq.create(DtoReq.class, this.getParmValueDto());

        final DeviceDao deviceDao =
                ServiceContext.getDaoContext().getDeviceDao();

        final Device device = deviceDao.findById(dtoReq.getId());

        if (device == null) {
            setApiResult(ApiResultCodeEnum.ERROR, "msg-device-not-found",
                    dtoReq.getDeviceName());
            return;
        }

        if (device.getCardReaderTerminal() != null) {
            setApiResult(ApiResultCodeEnum.INFO,
                    "msg-device-delete-reader-in-use", device.getDisplayName(),
                    device.getCardReaderTerminal().getDisplayName());
            return;
        }

        deviceDao.delete(device);

        setApiResult(ApiResultCodeEnum.OK, "msg-device-deleted-ok");
    }

}
