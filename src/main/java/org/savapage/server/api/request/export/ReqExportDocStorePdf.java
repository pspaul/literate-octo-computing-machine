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
package org.savapage.server.api.request.export;

import java.io.File;

import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.TextRequestHandler;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.util.resource.FileResourceStream;
import org.apache.wicket.util.time.Duration;
import org.savapage.core.config.WebAppTypeEnum;
import org.savapage.core.doc.DocContent;
import org.savapage.core.doc.store.DocStoreException;
import org.savapage.core.doc.store.DocStoreTypeEnum;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.services.ServiceContext;
import org.savapage.server.api.JsonApiDict;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class ReqExportDocStorePdf extends ApiRequestExportMixin {

    /** */
    private final DocStoreTypeEnum docStore;

    /** */
    private static final String PDF_FILE_SFX =
            "." + DocContent.FILENAME_EXT_PDF;

    /**
     *
     * @param store
     *            The document store.
     */
    public ReqExportDocStorePdf(final DocStoreTypeEnum store) {
        this.docStore = store;
    }

    @Override
    protected final IRequestHandler onExport(final WebAppTypeEnum webAppType,
            final String requestingUser, final RequestCycle requestCycle,
            final PageParameters parameters, final boolean isGetAction,
            final File tempExportFile) {

        // tempExportFile is NOT applicable.

        try {
            final String reqParm = this.getParmValue(requestCycle, parameters,
                    isGetAction, JsonApiDict.PARM_REQ_SUB);

            if (reqParm == null) {
                throw new DocStoreException("DocLog not specified.");
            }

            final Long docLogID = Long.valueOf(reqParm);

            final DocLog docLog = ServiceContext.getDaoContext().getDocLogDao()
                    .findById(docLogID);
            if (docLog == null) {
                throw new DocStoreException("DocLog not found.");
            }

            final File pdfFile = ServiceContext.getServiceFactory()
                    .getDocStoreService().retrievePdf(this.docStore, docLog);

            final ResourceStreamRequestHandler handler =
                    new ResourceStreamRequestHandler(
                            new FileResourceStream(pdfFile));

            String fileName = docLog.getTitle();

            if (!fileName.toLowerCase().endsWith(PDF_FILE_SFX)) {
                fileName = fileName.concat(PDF_FILE_SFX);
            }

            handler.setFileName(fileName);
            handler.setContentDisposition(ContentDisposition.ATTACHMENT);
            handler.setCacheDuration(Duration.NONE);

            return handler;

        } catch (DocStoreException e) {

            final StringBuilder html = new StringBuilder();

            html.append("<h3 style='color: red;'>");
            html.append(this.docStore.toString());
            html.append(": ").append(e.getMessage()).append("</h3>");
            return new TextRequestHandler("text/html", "UTF-8",
                    html.toString());
        }
    }
}
