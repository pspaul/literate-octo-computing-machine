/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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

import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.IppQueueDao;
import org.savapage.core.dao.helpers.ReservedIppQueueEnum;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.jpa.IppQueue;
import org.savapage.core.jpa.User;
import org.savapage.core.services.QueueService;
import org.savapage.core.services.ServiceContext;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqQueueGet extends ApiRequestMixin {

    /**
     * .
     */
    private static final QueueService QUEUE_SERVICE = ServiceContext
            .getServiceFactory().getQueueService();

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

        public void setId(Long id) {
            this.id = id;
        }
    }

    /**
     * The response.
     */
    private static class DtoRsp extends AbstractDto {

        private Long id;
        private String urlpath;
        private String ipallowed;
        private Boolean trusted;
        private Boolean fixedTrust;
        private Boolean disabled;
        private Boolean deleted;
        private String reserved;
        private String uiText;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUrlpath() {
            return urlpath;
        }

        public void setUrlpath(String urlpath) {
            this.urlpath = urlpath;
        }

        public String getIpallowed() {
            return ipallowed;
        }

        public void setIpallowed(String ipallowed) {
            this.ipallowed = ipallowed;
        }

        public Boolean getTrusted() {
            return trusted;
        }

        public void setTrusted(Boolean trusted) {
            this.trusted = trusted;
        }

        public Boolean getFixedTrust() {
            return fixedTrust;
        }

        public void setFixedTrust(Boolean fixedTrust) {
            this.fixedTrust = fixedTrust;
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

        public String getReserved() {
            return reserved;
        }

        public void setReserved(String reserved) {
            this.reserved = reserved;
        }

        public String getUiText() {
            return uiText;
        }

        public void setUiText(String uiText) {
            this.uiText = uiText;
        }

    }

    @Override
    protected void
            onRequest(final String requestingUser, final User lockedUser)
                    throws IOException {

        final DtoReq dtoReq = DtoReq.create(DtoReq.class, getParmValue("dto"));

        final IppQueueDao ippQueueDao =
                ServiceContext.getDaoContext().getIppQueueDao();

        final IppQueue queue = ippQueueDao.findById(dtoReq.getId());

        if (queue == null) {

            setApiResult(ApiResultCodeEnum.ERROR, "msg-queue-not-found", dtoReq
                    .getId().toString());
            return;
        }

        final DtoRsp dtoRsp = new DtoRsp();

        dtoRsp.setId(queue.getId());
        dtoRsp.setUrlpath(queue.getUrlPath());
        dtoRsp.setIpallowed(queue.getIpAllowed());
        dtoRsp.setTrusted(queue.getTrusted());
        dtoRsp.setDisabled(queue.getDisabled());
        dtoRsp.setDeleted(queue.getDeleted());

        final ReservedIppQueueEnum reservedQueue =
                QUEUE_SERVICE.getReservedQueue(queue.getUrlPath());

        final String reserved;
        final String uiText;
        final boolean fixedTrust;

        if (reservedQueue == null) {

            uiText = ReservedIppQueueEnum.IPP_PRINT.getUiText();
            reserved = null;
            fixedTrust = false;

        } else {

            reserved = reservedQueue.getUiText();

            if (reservedQueue == ReservedIppQueueEnum.RAW_PRINT) {
                uiText =
                        String.format("%s Port %s", reservedQueue.getUiText(),
                                ConfigManager.getRawPrinterPort());
            } else {
                uiText = reservedQueue.getUiText();
            }

            fixedTrust = reservedQueue.isNotTrusted();
        }

        dtoRsp.setFixedTrust(Boolean.valueOf(fixedTrust));

        dtoRsp.setReserved(reserved);
        dtoRsp.setUiText(uiText);

        this.setResponse(dtoRsp);

        setApiResultOk();
    }

}
