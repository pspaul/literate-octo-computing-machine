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
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.DocLogDao;
import org.savapage.core.doc.store.DocStoreException;
import org.savapage.core.doc.store.DocStoreTypeEnum;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.PrintIn;
import org.savapage.core.jpa.User;
import org.savapage.core.services.DocStoreService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.FileSystemHelper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Restores an archived or journaled {@link PrintIn} PDF into requesting user
 * inbox.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqInboxRestorePrintIn extends ApiRequestMixin {

    /** */
    protected static final DocStoreService DOCSTORE_SERVICE =
            ServiceContext.getServiceFactory().getDocStoreService();

    /** */
    private static final DocLogDao DOCLOG_DAO =
            ServiceContext.getDaoContext().getDocLogDao();

    @JsonInclude(Include.NON_NULL)
    private static class DtoReq extends AbstractDto {

        private Long docLogId;
        private Boolean replace;

        public Long getDocLogId() {
            return docLogId;
        }

        @SuppressWarnings("unused")
        public void setDocLogId(Long docLogId) {
            this.docLogId = docLogId;
        }

        public Boolean getReplace() {
            return replace;
        }

        @SuppressWarnings("unused")
        public void setReplace(Boolean replace) {
            this.replace = replace;
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
                        "msg-inbox-restore-printin-unavailable");
                return;
            }
        }

        final boolean isReplaced = BooleanUtils.isTrue(dtoReq.getReplace());
        if (isReplaced) {
            INBOX_SERVICE.deleteAllJobs(requestingUser);
        }

        // Copy via temp, to user home.
        final Path tmpPdfPath =
                Paths.get(ConfigManager.getAppTmpDir(), srcPdf.getName());
        final File tmpPdf = tmpPdfPath.toFile();

        FileUtils.copyFile(srcPdf, tmpPdf);
        tmpPdf.setLastModified(System.currentTimeMillis());

        final Path desPdfPath = Paths.get(
                ConfigManager.getUserHomeDir(requestingUser), srcPdf.getName());

        FileSystemHelper.doAtomicFileMove(tmpPdfPath, desPdfPath);

        final String msgKey;

        if (isReplaced) {
            msgKey = "msg-inbox-restore-printin-replaced";
        } else {
            msgKey = "msg-inbox-restore-printin-added";
        }

        boolean adminMsg = false; // for now
        if (adminMsg) {
            AdminPublisher.instance().publish(PubTopicEnum.USER,
                    PubLevelEnum.INFO,
                    String.format("%s \"%s\" - %s",
                            NounEnum.USER.uiText(getLocale()), requestingUser,
                            this.localize(msgKey)));
        }
        this.setApiResult(ApiResultCodeEnum.OK, msgKey);
    }

}
