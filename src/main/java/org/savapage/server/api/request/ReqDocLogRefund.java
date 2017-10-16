/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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
import java.math.BigDecimal;
import java.util.Date;

import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.enums.CostChangeStatusEnum;
import org.savapage.core.dao.enums.CostChangeTypeEnum;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.jpa.CostChange;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.PrintOut;
import org.savapage.core.jpa.User;
import org.savapage.core.services.ServiceContext;
import org.savapage.ext.papercut.PaperCutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqDocLogRefund extends ApiRequestMixin {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ReqDocLogRefund.class);

    /**
     * The request.
     */
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

        final DtoReq dtoReq = DtoReq.create(DtoReq.class, getParmValue("dto"));

        final DocLog docLog = ServiceContext.getDaoContext().getDocLogDao()
                .findById(dtoReq.getDocLogId());

        if (!isRefund(docLog)) {
            return;
        }

        if (!ServiceContext.getDaoContext().isTransactionActive()) {
            ServiceContext.getDaoContext().beginTransaction();
        }

        try {

            final CostChange chg = refund(docLog, requestingUser);

            /* Commit changes... */
            ServiceContext.getDaoContext().commit();

            /* ... and get full context back from database. */
            ServiceContext.getDaoContext().getCostChangeDao().refresh(chg);

            if (isPaperCutRefund(chg)) {
                PROXY_PRINT_SERVICE.refundProxyPrintPaperCut(chg);
            }

            this.setApiResult(ApiResultCodeEnum.OK, "msg-refund-ok");

        } catch (PaperCutException e) {
            /*
             * TODO: Changes are committed in the database, so what to do next?
             */
            this.setApiResultText(ApiResultCodeEnum.ERROR, e.getMessage());

        } finally {
            ServiceContext.getDaoContext().rollback();
        }
    }

    /**
     *
     * @param docLog
     *            The {@link DocLog} to refund (can be {@code null}.
     * @return {@code true} when the refund can be applied.
     */
    private boolean isRefund(final DocLog docLog) {

        if (docLog == null) {
            this.setApiResultText(ApiResultCodeEnum.ERROR,
                    "No document found."); // TODO
            return false;
        }

        /*
         * INVARIANT: vanilla transaction.
         */
        if (!docLog.getCost().equals(docLog.getCostOriginal())) {
            // TODO
            this.setApiResultText(ApiResultCodeEnum.ERROR, "Already refunded.");
            return false;
        }

        return true;
    }

    /**
     * @param chg
     *            The {@link CostChange} refund.
     * @return {@code true} when the refund must be propagated to PaperCut.
     */
    private boolean isPaperCutRefund(final CostChange chg) {

        final DocLog docLog = chg.getDocLog();

        if (docLog.getDocOut() != null) {

            final PrintOut printOut = docLog.getDocOut().getPrintOut();

            return printOut != null && PAPERCUT_SERVICE
                    .isExtPaperCutPrint(printOut.getPrinter().getPrinterName());
        }
        return false;
    }

    /**
     * Refunds the full {@link DocLog#getCost()} to all accounts that were
     * originally charged.
     *
     * @param docLog
     *            The {@link DocLog} to refund.
     * @param trxUser
     *            The requesting user.
     * @return The resulting {@link CostChange} of the refund.
     */
    private CostChange refund(final DocLog docLog, final String trxUser) {

        final BigDecimal costOrg = docLog.getCostOriginal();
        final BigDecimal costCur = docLog.getCost();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Cost (Cur) : %s | Cost (Org) : %s",
                    costCur.toPlainString(), costOrg.toPlainString()));
        }

        final DaoContext daoCtx = ServiceContext.getDaoContext();

        //
        final String currencyCode = ConfigManager.getAppCurrencyCode();
        final Date trxDate = ServiceContext.getTransactionDate();

        final BigDecimal costNew = BigDecimal.ZERO;

        //
        docLog.setCost(costNew);
        docLog.setRefunded(Boolean.TRUE);

        daoCtx.getDocLogDao().update(docLog);

        //
        final CostChange chg = new CostChange();

        chg.setDocLog(docLog);

        chg.setChgAmount(costCur);
        chg.setChgBy(trxUser);
        chg.setChgCost(costNew);
        chg.setChgDate(trxDate);
        chg.setChgStatus(CostChangeStatusEnum.APPROVED.toString());
        chg.setChgType(CostChangeTypeEnum.AUTO.toString());
        chg.setCurrencyCode(currencyCode);
        chg.setReqAmount(costCur);
        chg.setReqDate(ServiceContext.getTransactionDate());

        daoCtx.getCostChangeDao().create(chg);

        for (final AccountTrx trxOrg : docLog.getTransactions()) {

            final BigDecimal costRefund = trxOrg.getAmount().negate();

            //
            final Account account = trxOrg.getAccount();

            final BigDecimal balanceNew = account.getBalance().add(costRefund);

            account.setBalance(balanceNew);
            account.setModifiedBy(trxUser);
            account.setModifiedDate(trxDate);

            daoCtx.getAccountDao().update(account);

            //
            final AccountTrx trxNew = new AccountTrx();

            trxNew.setTrxType(trxOrg.getTrxType());

            trxNew.setCostChange(chg);
            trxNew.setDocLog(docLog);
            trxNew.setAccount(trxOrg.getAccount());

            trxNew.setAmount(costRefund);
            trxNew.setCurrencyCode(currencyCode);
            trxNew.setIsCredit(Boolean.TRUE);

            trxNew.setBalance(balanceNew);

            trxNew.setTransactedBy(trxUser);
            trxNew.setTransactionDate(trxDate);

            trxNew.setTransactionWeight(trxOrg.getTransactionWeight());
            trxNew.setTransactionWeightUnit(trxOrg.getTransactionWeightUnit());

            daoCtx.getAccountTrxDao().create(trxNew);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("%s : %s -> %s",
                        trxOrg.getAccount().getName(),
                        trxOrg.getAmount().toPlainString(),
                        costRefund.toPlainString()));
            }
        }

        return chg;
    }

}
