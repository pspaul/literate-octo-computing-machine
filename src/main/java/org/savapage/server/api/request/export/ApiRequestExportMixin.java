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
import java.nio.file.Paths;
import java.util.UUID;

import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.TextRequestHandler;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.WebAppTypeEnum;
import org.savapage.core.jpa.User;
import org.savapage.core.services.JobTicketService;
import org.savapage.core.services.OutboxService;
import org.savapage.core.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class ApiRequestExportMixin implements ApiRequestExportHandler {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ApiRequestExportMixin.class);

    /** */
    protected static final JobTicketService JOBTICKET_SERVICE =
            ServiceContext.getServiceFactory().getJobTicketService();

    /** */
    protected static final OutboxService OUTBOX_SERVICE =
            ServiceContext.getServiceFactory().getOutboxService();

    /**
     *
     * @param webAppType
     *            The {@link WebAppTypeEnum}.
     * @param requestingUser
     *            The requesting user.
     * @param requestCycle
     *            The {@link RequestCycle}.
     * @param parameters
     *            The {@link PageParameters}.
     * @param isGetAction
     *            {@code true} when this is an HTML GET request.
     * @param tempExportFile
     *            The unique temporary file.
     * @return The request handler.
     * @throws Exception
     *             When error.
     */
    protected abstract IRequestHandler onExport(WebAppTypeEnum webAppType,
            String requestingUser, RequestCycle requestCycle,
            PageParameters parameters, boolean isGetAction, File tempExportFile)
            throws Exception;

    /**
     *
     * @param requestCycle
     *            The {@link RequestCycle}.
     * @param getParms
     *            The {@link PageParameters}.
     * @param isGetAction
     *            {@code true} when a GET parameter.
     * @param parm
     *            The parameter name.
     * @return {@code null} when parameter is not present.
     */
    protected final String getParmValue(final RequestCycle requestCycle,
            final PageParameters getParms, final boolean isGetAction,
            final String parm) {
        if (isGetAction) {
            return getParms.get(parm).toString();
        }
        return requestCycle.getRequest().getPostParameters()
                .getParameterValue(parm).toString();
    }

    /**
    *
    * @param requestCycle
    *            The {@link RequestCycle}.
    * @param getParms
    *            The {@link PageParameters}.
    * @param isGetAction
    *            {@code true} when a GET parameter.
    * @param parm
    *            The parameter name.
    * @return {@code null} when parameter is not present.
    */
   protected final Long getParmLong(final RequestCycle requestCycle,
           final PageParameters getParms, final boolean isGetAction,
           final String parm) {
       if (isGetAction) {
           return getParms.get(parm).toLongObject();
       }
       return requestCycle.getRequest().getPostParameters()
               .getParameterValue(parm).toLongObject();
   }

    @Override
    public final IRequestHandler export(final WebAppTypeEnum webAppType,
            final RequestCycle requestCycle, final PageParameters parameters,
            final boolean isGetAction, final String requestingUser,
            final User lockedUser) throws Exception {

        final File tempExportFile = Paths
                .get(ConfigManager.getAppTmpDir(), String
                        .format("export-%s.tmp", UUID.randomUUID().toString()))
                .toFile();

        IRequestHandler requestHandler = null;

        try {

            requestHandler = onExport(webAppType, requestingUser, requestCycle,
                    parameters, isGetAction, tempExportFile);

        } catch (Exception e) {

            LOGGER.error(e.getMessage(), e);

            if (tempExportFile != null && tempExportFile.exists()) {
                tempExportFile.delete();
            }

            requestHandler = new TextRequestHandler("text/html", "UTF-8",
                    "<h2 style='color: red;'>" + e.getClass().getSimpleName()
                            + "</h2><p>" + e.getMessage() + "</p>");
        }

        return requestHandler;
    }
}
