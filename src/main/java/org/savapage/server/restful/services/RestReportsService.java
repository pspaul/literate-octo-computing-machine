/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2023 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2023 Datraverse B.V. <info@datraverse.com>
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
package org.savapage.server.restful.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.AccountDao;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dao.helpers.AccountTrxPagerReq;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.User;
import org.savapage.core.reports.impl.AccountTrxListReport;
import org.savapage.core.reports.impl.ReportCreator;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.JsonHelper;
import org.savapage.server.restful.RestAuthFilter;
import org.savapage.server.restful.dto.AccountTrxReportReqDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST reports API.
 *
 * @author Rijk Ravestein
 *
 */
@Path("/" + RestReportsService.PATH_MAIN)
public class RestReportsService implements IRestService {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(RestReportsService.class);

    /** */
    private static final String PATH_SUB_ACCOUNT_TRX = "accounttrx";

    /** */
    public static final String PATH_MAIN = "reports";

    /** */
    private static final UserDao USER_DAO =
            ServiceContext.getDaoContext().getUserDao();
    /** */
    private static final AccountDao ACCOUNT_DAO =
            ServiceContext.getDaoContext().getAccountDao();

    /**
     * POST a selection and get a CSV report as response.
     *
     * @param jsonInput
     *            JSON select/sort.
     * @return {@link Response} with CSV data.
     * @throws Exception
     *             If error.
     */
    @POST
    @Path("/" + PATH_SUB_ACCOUNT_TRX)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @RolesAllowed(RestAuthFilter.ROLE_ADMIN)
    public Response printTransactions(//
            final String jsonInput) {

        final File tempExportFile = ConfigManager.createAppTmpFile("export-");

        try {
            final AccountTrxReportReqDto requestIn = AccountTrxReportReqDto
                    .create(AccountTrxReportReqDto.class, jsonInput);

            final AccountTrxPagerReq requestReport = new AccountTrxPagerReq();
            requestReport.createSelect();
            requestReport.createSort();

            // VALIDATE
            final Long userIdKey;
            final Long accountIdKey;

            if (requestIn.getSelect() == null
                    || requestIn.getSelect().getAccountType() == null
                    || requestIn.getSelect().getAccountName() == null) {
                userIdKey = null;
                accountIdKey = null;
                LOGGER.warn("Account is missing.");
            } else {
                final String accountName =
                        requestIn.getSelect().getAccountName();

                switch (requestIn.getSelect().getAccountType()) {
                case GROUP:
                case SHARED:
                    userIdKey = null;
                    final Account account =
                            ACCOUNT_DAO.findActiveAccountByName(accountName,
                                    requestIn.getSelect().getAccountType());
                    if (account == null) {
                        accountIdKey = null;
                        LOGGER.warn("Account [{}] not found.", accountName);
                    } else {
                        accountIdKey = account.getId();
                    }
                    break;
                case USER:
                    accountIdKey = null;
                    final User user =
                            USER_DAO.findActiveUserByUserId(accountName);
                    if (user == null) {
                        userIdKey = null;
                        LOGGER.warn("User [{}] not found.", accountName);
                    } else {
                        userIdKey = user.getId();
                    }
                    break;
                default:
                    userIdKey = null;
                    accountIdKey = null;
                    break;
                }
            }

            if (userIdKey == null && accountIdKey == null) {
                return Response.serverError().status(Status.NOT_FOUND).build();
            }

            // Selection
            final AccountTrxReportReqDto.Select selIn = requestIn.getSelect();
            final AccountTrxPagerReq.Select selReport =
                    requestReport.getSelect();

            selReport.setUserId(userIdKey);
            selReport.setAccountId(accountIdKey);

            selReport.setContainingText(selIn.getContainingText());
            selReport.setTrxType(selIn.getTrxType());

            if (selIn.getDateFrom() != null) {
                selReport.setDateFrom(selIn.getDateFrom().asTime());
            }
            if (selIn.getDateTo() != null) {
                selReport.setDateTo(selIn.getDateTo().asTime());
            }

            // Sort
            final AccountTrxReportReqDto.Sort sortIn = requestIn.getSort();
            final AccountTrxPagerReq.Sort sortReport = requestReport.getSort();

            if (sortIn != null) {
                sortReport.setAscending(sortIn.getAscending());
                sortReport.setField(sortIn.getField());
            }

            // Create report
            final String jsonInputReport =
                    JsonHelper.stringifyObject(requestReport);
            final boolean requestingUserAdmin = true;

            final ReportCreator report = new AccountTrxListReport("",
                    requestingUserAdmin, jsonInputReport, Locale.getDefault());

            report.create(tempExportFile, requestIn.getMediaType());

            final StreamingOutput stream = new StreamingOutput() {
                @Override
                public void write(final OutputStream output)
                        throws IOException, WebApplicationException {
                    try (FileInputStream in =
                            new FileInputStream(tempExportFile)) {
                        int c;
                        while ((c = in.read()) != -1) {
                            output.write(c);
                        }
                    } finally {
                        tempExportFile.delete();
                    }
                }
            };
            return Response.ok(stream).build();

        } catch (Exception e) {
            if (tempExportFile != null && tempExportFile.exists()) {
                tempExportFile.delete();
            }
            LOGGER.error(e.toString(), e);
            throw new SpException(e.getMessage());
        }
    }

}
