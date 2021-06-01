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
package org.savapage.server.api.request.export;

import java.io.File;
import java.nio.file.Paths;

import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.TextRequestHandler;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.util.resource.FileResourceStream;
import org.apache.wicket.util.time.Duration;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.WebAppTypeEnum;
import org.savapage.core.jpa.User;
import org.savapage.core.services.ServiceContext;
import org.savapage.server.api.JsonApiDict;
import org.savapage.server.session.SpSession;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class ReqExportOutboxPdf extends ApiRequestExportMixin {

    /**
     * {@code true} when PDF is from Job Ticket.
     */
    private final boolean isJobTicket;

    /**
     *
     * @param jobTicket
     *            {@code true} when PDF is from Job Ticket.
     */
    public ReqExportOutboxPdf(final boolean jobTicket) {
        this.isJobTicket = jobTicket;
    }

    @Override
    protected final IRequestHandler onExport(final WebAppTypeEnum webAppType,
            final String requestingUser, final RequestCycle requestCycle,
            final PageParameters parameters, final boolean isGetAction,
            final File tempExportFile) {

        // tempExportFile is NOT applicable.

        final String userid;

        if (this.isJobTicket) {
            userid = null;
        } else if (webAppType.isUserTypeOrVariant()) {
            userid = requestingUser;
        } else {
            final User user =
                    ServiceContext.getDaoContext().getUserDao()
                            .findActiveUserById(this.getParmLong(requestCycle,
                                    parameters, isGetAction,
                                    JsonApiDict.PARM_REQ_PARM));
            userid = user.getUserId();
        }

        return exportOutboxPdf(
                webAppType, userid, this.getParmValue(requestCycle, parameters,
                        isGetAction, JsonApiDict.PARM_REQ_SUB),
                this.isJobTicket);
    }

    /**
     * Exports user outbox or job ticket PDF.
     *
     * @param webAppType
     * @param fileName
     *            The base file name.
     * @param requestingUser
     *            The user requesting the export.
     * @param isJobTicket
     *            {@code true} when PDF is a job ticket.
     * @return The {@link IRequestHandler}}
     */
    protected static IRequestHandler exportOutboxPdf(
            final WebAppTypeEnum webAppType, final String requestingUser,
            final String fileName, final boolean isJobTicket) {

        final File pdfFile;

        if (isJobTicket) {

            /*
             * INVARIANT: A user can only see his own job ticket.
             */
            if (webAppType != WebAppTypeEnum.JOBTICKETS
                    && webAppType != WebAppTypeEnum.ADMIN
                    && webAppType != WebAppTypeEnum.PRINTSITE
                    && JOBTICKET_SERVICE.getTicket(
                            SpSession.get().getUserDbKey(), fileName) == null) {
                pdfFile = null;
            } else {
                pdfFile =
                        Paths.get(ConfigManager.getJobTicketsHome().toString(),
                                fileName).toFile();
            }

        } else {
            pdfFile = OUTBOX_SERVICE.getOutboxFile(requestingUser, fileName);
        }

        if (pdfFile == null || !pdfFile.exists()) {

            final StringBuilder html = new StringBuilder();

            html.append("<h2 style='color: red;'>");

            if (isJobTicket) {
                html.append("Job Ticket");
            } else {
                html.append("Hold Job");
            }

            html.append(" not found.</h2>");
            return new TextRequestHandler("text/html", "UTF-8",
                    html.toString());
        }

        final ResourceStreamRequestHandler handler =
                new ResourceStreamRequestHandler(
                        new FileResourceStream(pdfFile));

        handler.setContentDisposition(ContentDisposition.ATTACHMENT);
        handler.setFileName(fileName);
        handler.setCacheDuration(Duration.NONE);

        return handler;
    }

}
