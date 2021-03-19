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

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.savapage.core.dao.DocLogDao;
import org.savapage.core.doc.store.DocStoreException;
import org.savapage.core.doc.store.DocStoreTypeEnum;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.User;
import org.savapage.core.services.DocStoreService;
import org.savapage.core.services.ServiceContext;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Deletes an archived or journaled {@link DocLog} PDF.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqDocLogStoreDelete extends ApiRequestMixin {

    /** */
    protected static final DocStoreService DOCSTORE_SERVICE =
            ServiceContext.getServiceFactory().getDocStoreService();

    /** */
    private static final DocLogDao DOCLOG_DAO =
            ServiceContext.getDaoContext().getDocLogDao();

    @JsonInclude(Include.NON_NULL)
    private static class DtoReq extends AbstractDto {

        private Long docLogId;

        public Long getDocLogId() {
            return docLogId;
        }

        @SuppressWarnings("unused")
        public void setDocLogId(Long docLogId) {
            this.docLogId = docLogId;
        }

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final DtoReq dtoReq =
                DtoReq.create(DtoReq.class, this.getParmValueDto());

        final DocLog docLog = DOCLOG_DAO.findById(dtoReq.getDocLogId());
        if (docLog == null) {
            this.setApiResultText(ApiResultCodeEnum.ERROR, "DocLog not found.");
            return;
        }

        File srcPdf = null;
        try {
            srcPdf = DOCSTORE_SERVICE.retrievePdf(DocStoreTypeEnum.ARCHIVE,
                    docLog);
        } catch (DocStoreException e) {
            try {
                srcPdf = DOCSTORE_SERVICE.retrievePdf(DocStoreTypeEnum.JOURNAL,
                        docLog);
            } catch (DocStoreException e1) {
                this.setApiResult(ApiResultCodeEnum.WARN,
                        "msg-doclog-store-unavailable");
                return;
            }
        }

        FileUtils.deleteDirectory(srcPdf.getParentFile());

        this.setApiResult(ApiResultCodeEnum.OK, "msg-deleted-ok");
    }

}
