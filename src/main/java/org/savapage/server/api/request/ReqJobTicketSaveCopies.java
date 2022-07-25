/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Authors: Rijk Ravestein.
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.i18n.PrintOutNounEnum;
import org.savapage.core.jpa.User;
import org.savapage.core.outbox.OutboxCopiesEditor;
import org.savapage.core.outbox.OutboxInfoDto.OutboxAccountTrxInfoSet;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobDto;
import org.savapage.core.print.proxy.JsonProxyPrinter;
import org.savapage.core.services.helpers.JobTicketWrapperDto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqJobTicketSaveCopies extends ReqJobTicketSave {

    /**
     * The request.
     */
    @JsonInclude(Include.NON_NULL)
    private static class CopiesReq {

        private int copies;
        private String accountName;

        public int getCopies() {
            return copies;
        }

        @SuppressWarnings("unused")
        public void setCopies(int copies) {
            this.copies = copies;
        }

        @SuppressWarnings("unused")
        public String getAccountName() {
            return accountName;
        }

        @SuppressWarnings("unused")
        public void setAccountName(String accountName) {
            this.accountName = accountName;
        }

    }

    private static class DtoReq extends AbstractDto {

        private String jobFileName;
        private Map<Long, CopiesReq> accounts;

        public String getJobFileName() {
            return jobFileName;
        }

        @SuppressWarnings("unused")
        public void setJobFileName(String jobFileName) {
            this.jobFileName = jobFileName;
        }

        public Map<Long, CopiesReq> getAccounts() {
            return accounts;
        }

        @SuppressWarnings("unused")
        public void setAccounts(Map<Long, CopiesReq> accounts) {
            this.accounts = accounts;
        }

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final DtoReq dtoReq =
                DtoReq.create(DtoReq.class, this.getParmValueDto());

        // Validate
        for (final CopiesReq copies : dtoReq.getAccounts().values()) {
            if (copies.getCopies() == 0) {
                this.setApiResult(ApiResultCodeEnum.WARN, "msg-value-invalid",
                        PrintOutNounEnum.COPY.uiText(getLocale(), true), "0");
                return;
            }
        }

        final OutboxJobDto dto =
                JOBTICKET_SERVICE.getTicket(dtoReq.getJobFileName());

        final ApiResultCodeEnum resultCode;
        final String msgKey;

        if (this.recalcCopies(dto, dtoReq.getAccounts())) {

            final JsonProxyPrinter proxyPrinter =
                    PROXY_PRINT_SERVICE.getCachedPrinter(dto.getPrinter());

            this.recalcCost(dto, proxyPrinter);

            JOBTICKET_SERVICE.updateTicket(new JobTicketWrapperDto(dto));

            this.notifyUser(dto.getUserId());

            resultCode = ApiResultCodeEnum.OK;
            msgKey = "msg-apply-ok";

        } else {
            resultCode = ApiResultCodeEnum.WARN;
            msgKey = "msg-apply-no-changes";
        }

        this.setApiResult(resultCode, msgKey);
    }

    /**
     *
     * @param dto
     *            Job ticket.
     * @param accountCopies
     *            Account copies.
     * @return {@code true} if copies were changed.
     */
    private boolean recalcCopies(final OutboxJobDto dto,
            final Map<Long, CopiesReq> accountCopies) {

        final Map<Long, String> groupAccounts = new HashMap<>();
        final Map<Long, Integer> otherAccounts = new HashMap<>();

        // Split user group accounts from other accounts.
        for (final Entry<Long, CopiesReq> entry : accountCopies.entrySet()) {
            final Long accountID = entry.getKey();
            final String accountName = entry.getValue().accountName;
            if (StringUtils.isBlank(accountName)) {
                otherAccounts.put(accountID,
                        Integer.valueOf(entry.getValue().getCopies()));
            } else {
                groupAccounts.put(accountID, accountName);
            }
        }

        // Initialize editor with the user group accounts.
        final OutboxCopiesEditor editor = new OutboxCopiesEditor(
                dto.getAccountTransactions(), groupAccounts);

        // Recalculate user group accounts.
        OutboxAccountTrxInfoSet trxInfoSetWlk = null;

        for (final Entry<Long, String> entry : groupAccounts.entrySet()) {

            final Long accountID = entry.getKey();

            final int copiesOld =
                    editor.getGroupAccountCopies(accountID).intValue();
            final int copiesNew = accountCopies.get(accountID).getCopies();

            if (copiesOld != copiesNew) {
                trxInfoSetWlk = editor.recalcGroupCopies(accountID, copiesNew);
            }
        }

        // Recalculate other accounts.
        if (editor.evaluateOtherAccountCopies(otherAccounts)) {
            trxInfoSetWlk = editor.recalcOtherCopies(otherAccounts);
        }

        // Evaluate resulting transactions.
        if (trxInfoSetWlk == null) {
            return false;
        }

        dto.setAccountTransactions(trxInfoSetWlk);
        dto.setCopies(trxInfoSetWlk.getWeightTotal());

        return true;
    }
}
