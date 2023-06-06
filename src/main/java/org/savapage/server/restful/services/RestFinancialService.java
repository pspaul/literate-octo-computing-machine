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

import java.io.IOException;
import java.math.BigDecimal;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.dao.AccountDao;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dto.UserCreditTransferDto;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.jpa.User;
import org.savapage.core.json.rpc.AbstractJsonRpcMethodResponse;
import org.savapage.core.services.AccountingService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.server.restful.RestAuthException;
import org.savapage.server.restful.RestAuthFilter;
import org.savapage.server.restful.dto.RestResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST reports API.
 *
 * @author Rijk Ravestein
 *
 */
@Path("/" + RestFinancialService.PATH_MAIN)
public class RestFinancialService extends AbstractRestService {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(RestFinancialService.class);

    /** */
    public static final String PATH_MAIN = "financial";

    /** */
    private static final String ROLE_ADMIN = "admin";

    /** */
    private static final String PATH_SUB_BALANCE = "balance";

    /** */
    private static final String PATH_SUB_ACCOUNT_BALANCE =
            "account/" + PATH_SUB_BALANCE;

    /** */
    private static final String PATH_SUB_TRANSFER = "transfer";

    /** */
    private static final String QUERY_PARAM_ACCOUNT_TYPE = "type";
    /** */
    private static final String QUERY_PARAM_ACCOUNT_NAME = "name";
    /** */
    private static final String QUERY_PARAM_AMOUNT = "amount";

    /** */
    private static final String TRX_COMMENT = "Web Service";

    /** */
    private static final AccountDao ACCOUNT_DAO =
            ServiceContext.getDaoContext().getAccountDao();
    /** */
    private static final AccountingService ACCOUNTING_SERVICE =
            ServiceContext.getServiceFactory().getAccountingService();

    /**
     * Creates, logs and notifies a warning response.
     *
     * @param status
     *            Response status
     * @param msg
     *            Message.
     * @return {@link Response}.
     */
    private static Response execWarning(final Response.Status status,
            final String msg) {
        AdminPublisher.instance().publish(PubTopicEnum.WEB_SERVICE,
                PubLevelEnum.WARN, msg);
        LOGGER.warn(msg);
        // AppLogHelper.log(AppLogLevelEnum.WARN, msg);
        return Response.status(status).build();
    }

    /**
     * Creates an OK response.
     *
     * @param rspDto
     *            RestResponseDto
     * @param logMsg
     * @return Response
     */
    private static Response execResponse(final RestResponseDto rspDto,
            final String logMsg) {
        try {
            final PubLevelEnum level;
            if (rspDto.getSucces().booleanValue()) {
                LOGGER.info(logMsg);
                level = PubLevelEnum.INFO;
            } else {
                LOGGER.warn(logMsg);
                level = PubLevelEnum.WARN;
                // AppLogHelper.log(AppLogLevelEnum.WARN, logMsg);
            }
            AdminPublisher.instance().publish(PubTopicEnum.WEB_SERVICE, level,
                    logMsg);

            return Response.ok(rspDto.stringifyPrettyPrinted().concat("\n"))
                    .build();

        } catch (IOException e) {
            return execWarning(Status.NO_CONTENT,
                    logMsg.concat(" : ").concat(e.getMessage()));
        }
    }

    /**
     *
     * @param accountType
     * @param accountName
     * @param logMsg
     *            for logging purposes.
     * @return Response
     */
    private static Response getBalance(final AccountTypeEnum accountType,
            final String accountName, final StringBuilder logMsg) {

        final Account acc =
                ACCOUNT_DAO.findActiveAccountByName(accountName, accountType);

        final RestResponseDto rspDto = new RestResponseDto();
        rspDto.setSucces(acc != null);

        if (acc == null) {
            rspDto.setError("account not found.");
            logMsg.append(": ").append(rspDto.getError());
        } else {
            rspDto.setResult(acc.getBalance().toPlainString());
        }
        return execResponse(rspDto, logMsg.toString());
    }

    /**
     * Sets balance for an account.
     *
     * @param accountType
     * @param accountName
     * @param balanceNew
     * @param actorUserid
     * @return "true" if successful, "false" if not.
     */
    private static Response setBalance(final AccountTypeEnum accountType,
            final String accountName, final BigDecimal balanceNew,
            final String actorUserid) {

        final Account acc =
                ACCOUNT_DAO.findActiveAccountByName(accountName, accountType);

        final RestResponseDto rspDto = new RestResponseDto();
        DaoContext daoCtx = null;
        Boolean isCommitted = Boolean.FALSE;

        final StringBuilder logMsg = new StringBuilder(String.format(
                "%s.setBalance of %s [%s] to [%s] by [%s]",
                RestFinancialService.class.getSimpleName(), accountType.name(),
                accountName, balanceNew.toPlainString(), actorUserid));

        try {
            if (acc == null) {
                rspDto.setError("account not found.");
                logMsg.append(": ").append(rspDto.getError());
                execResponse(rspDto, logMsg.toString());
            } else {
                ServiceContext.resetTransactionDate();
                ServiceContext.setActor(actorUserid);

                daoCtx = ServiceContext.getDaoContext();
                daoCtx.beginTransaction();

                final AccountTrx trx = ACCOUNTING_SERVICE
                        .checkCreateAccountTrx(acc, balanceNew, TRX_COMMENT);

                if (trx == null) {
                    rspDto.setError("amount identical to current balance.");
                    logMsg.append(" : ").append(rspDto.getError());
                } else {
                    ACCOUNT_DAO.update(acc);
                    isCommitted = Boolean.TRUE;
                    daoCtx.commit();
                }
            }
        } finally {
            if (daoCtx != null && !isCommitted.booleanValue()) {
                daoCtx.rollback();
            }
        }

        rspDto.setSucces(isCommitted);
        return execResponse(rspDto, logMsg.toString());
    }

    /**
     * Gets balance amount for user by RESTful admin.
     *
     * @param accountType
     * @param accountName
     *
     * @return Balance amount for an account.
     */
    @GET
    @RolesAllowed(RestAuthFilter.ROLE_ADMIN)
    @Path(PATH_SUB_ACCOUNT_BALANCE)
    @Produces(MediaType.TEXT_PLAIN)
    public Response adminGetBalance(//
            @QueryParam(QUERY_PARAM_ACCOUNT_TYPE) //
            final AccountTypeEnum accountType,
            @QueryParam(QUERY_PARAM_ACCOUNT_NAME) //
            final String accountName) {

        final StringBuilder logMsg = new StringBuilder(
                String.format("%s.adminGetBalance of %s [%s] by [%s]",
                        RestFinancialService.class.getSimpleName(),
                        accountType.name(), accountName, ROLE_ADMIN));

        return getBalance(accountType, accountName, logMsg);
    }

    /**
     * Sets balance amount for user by RESTful admin.
     *
     * @param authString
     *            base64(user:password)
     * @param accountType
     * @param accountName
     * @param amount
     *
     * @return "true" if successful, "false" if not.
     */
    @POST
    @RolesAllowed(RestAuthFilter.ROLE_ADMIN)
    @Path(PATH_SUB_ACCOUNT_BALANCE)
    @Produces(MediaType.TEXT_PLAIN)
    public Response adminSetBalance(//
            @HeaderParam(RestAuthFilter.HEADER_AUTHORIZATION_PROPERTY) //
            final String authString, //
            @QueryParam(QUERY_PARAM_ACCOUNT_TYPE) //
            final AccountTypeEnum accountType,
            @QueryParam(QUERY_PARAM_ACCOUNT_NAME) //
            final String accountName, //
            @QueryParam(QUERY_PARAM_AMOUNT) //
            final BigDecimal amount) {

        final String actorUserid = this.getUserPasswordFromAuth(authString)[0];
        return setBalance(accountType, accountName, amount, actorUserid);
    }

    /**
     * Gets balance amount for user by user.
     *
     * @param authString
     *            base64(user:password)
     * @return Balance amount.
     */
    @GET
    @Path(PATH_SUB_BALANCE)
    @Produces(MediaType.TEXT_PLAIN)
    public Response userGetBalance(//
            @HeaderParam(RestAuthFilter.HEADER_AUTHORIZATION_PROPERTY) //
            final String authString) {

        final StringBuilder logMsg =
                new StringBuilder(String.format("%s.userGetBalance",
                        RestFinancialService.class.getSimpleName()));
        try {
            final User user = this.isUserAuthenticated(authString, "");
            logMsg.append(String.format(" of [%s]", user.getUserId()));
            return getBalance(AccountTypeEnum.USER, user.getUserId(), logMsg);
        } catch (RestAuthException e) {
            logMsg.append(e.getMessage());
            return execWarning(Response.Status.UNAUTHORIZED, logMsg.toString());
        }
    }

    /**
     * Transfers balance amount from user to user.
     *
     * @param authString
     *            base64(user:password)
     * @param accountName
     *            User to transfer to.
     * @param amount
     *            Decimal point amount with two decimal fraction.
     * @return "true" if successful, "false" if not.
     */
    @POST
    @Path(PATH_SUB_TRANSFER)
    @Produces(MediaType.TEXT_PLAIN)
    public Response userTransferBalance(//
            @HeaderParam(RestAuthFilter.HEADER_AUTHORIZATION_PROPERTY) //
            final String authString, //
            @QueryParam(QUERY_PARAM_ACCOUNT_NAME) //
            final String accountName, //
            @QueryParam(QUERY_PARAM_AMOUNT) //
            final String amount //
    ) {
        final StringBuilder logMsg = new StringBuilder(
                String.format("%s.userTransferBalance [%s] to %s [%s]",
                        RestFinancialService.class.getSimpleName(), amount,
                        AccountTypeEnum.USER.name(), accountName));
        DaoContext daoCtx = null;
        Boolean isCommitted = Boolean.FALSE;

        try {
            final User user =
                    this.isUserAuthenticated(authString, logMsg.toString());

            final String[] amountParts = BigDecimalUtil.getAmountParts(amount);

            final UserCreditTransferDto dto = new UserCreditTransferDto();
            dto.setUserIdFrom(user.getUserId());
            dto.setUserIdTo(accountName);
            dto.setAmountMain(amountParts[0]);
            dto.setAmountCents(amountParts[1]);
            dto.setComment(TRX_COMMENT);

            logMsg.append(String.format(" from [%s]", dto.getUserIdFrom()));

            ServiceContext.resetTransactionDate();
            ServiceContext.setActor(dto.getUserIdFrom());

            daoCtx = ServiceContext.getDaoContext();
            daoCtx.beginTransaction();

            final AbstractJsonRpcMethodResponse rsp =
                    ACCOUNTING_SERVICE.transferUserCredit(dto);

            final RestResponseDto rspDto = new RestResponseDto();
            if (rsp.isError()) {
                rspDto.setError(rsp.asError().getError().getMessage());
                logMsg.append(": ").append(rspDto.getError());
            } else {
                daoCtx.commit();
                isCommitted = Boolean.TRUE;
            }
            rspDto.setSucces(isCommitted);

            return execResponse(rspDto, logMsg.toString());

        } catch (RestAuthException e) {
            logMsg.append(" : ").append(e.getMessage());
            return execWarning(Response.Status.UNAUTHORIZED, logMsg.toString());
        } finally {
            if (daoCtx != null && !isCommitted.booleanValue()) {
                daoCtx.rollback();
            }
        }
    }

}
