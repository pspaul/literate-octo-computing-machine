/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2021 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2021 Datraverse B.V. <info@datraverse.com>
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.api.request;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dto.AccountDisplayInfoDto;
import org.savapage.core.dto.PosSalesDto;
import org.savapage.core.i18n.AdjectiveEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.i18n.PhraseEnum;
import org.savapage.core.jpa.User;
import org.savapage.core.json.rpc.AbstractJsonRpcMethodResponse;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.account.UserAccountContext;
import org.savapage.core.services.helpers.account.UserAccountContextEnum;
import org.savapage.core.services.helpers.account.UserAccountContextFactory;
import org.savapage.ext.papercut.PaperCutServerProxy;
import org.savapage.server.pages.MarkupHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqPosSales extends ApiRequestMixin {

    /** */
    private static final ConfigManager CONFIG_MNGR = ConfigManager.instance();

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final PosSalesDto dto =
                PosSalesDto.create(PosSalesDto.class, this.getParmValueDto());

        // Correct for empty values;
        if (dto.getAmountMain().length() == 0) {
            dto.setAmountMain("0");
        }
        if (dto.getAmountCents().length() == 0) {
            dto.setAmountCents("00");
        }
        // INVARIANT: cents must be 2 digits.
        if (dto.getAmountCents().length() != 2) {
            this.setApiResult(ApiResultCodeEnum.ERROR, "msg-value-invalid",
                    NounEnum.VALUE.uiText(getLocale()), dto.formatAmount());
            return;
        }

        final Object[][] enabledInvariantCheck = { //
                { Key.FINANCIAL_POS_SALES_LABEL_LOCATIONS_ENABLE,
                        NounEnum.LOCATION, dto.getPosLocation() }, //
                { Key.FINANCIAL_POS_SALES_LABEL_SHOPS_ENABLE, NounEnum.SHOP,
                        dto.getPosShop() }, //
                { Key.FINANCIAL_POS_SALES_LABEL_ITEMS_ENABLE, NounEnum.ITEM,
                        dto.getPosItem() } //
        };

        for (final Object[] entry : enabledInvariantCheck) {
            if (CONFIG_MNGR.isConfigValue((Key) entry[0]) && (entry[2] == null
                    || StringUtils.isBlank(entry[2].toString()))) {
                this.setApiResult(ApiResultCodeEnum.ERROR,
                        "msg-value-cannot-be-empty",
                        ((NounEnum) entry[1]).uiText(getLocale()));
                return;
            }
        }

        final boolean isPaperCutAccount;
        final UserAccountContext accountCtx;

        if (dto.getAccountContext() == UserAccountContextEnum.PAPERCUT) {

            if (UserAccountContextFactory.hasContextPaperCut()) {

                final Set<UserAccountContextEnum> accountContextSet =
                        CONFIG_MNGR.getConfigEnumSet(
                                UserAccountContextEnum.class,
                                Key.FINANCIAL_POS_SALES_ACCOUNTS);

                isPaperCutAccount =
                        accountContextSet.isEmpty() || accountContextSet
                                .contains(UserAccountContextEnum.PAPERCUT);
            } else {
                isPaperCutAccount = false;
            }
            if (!isPaperCutAccount) {
                this.setApiResultText(ApiResultCodeEnum.ERROR,
                        "PaperCut action is not allowed.");
                return;
            }
            accountCtx = UserAccountContextFactory.getContextPaperCut();
        } else {
            isPaperCutAccount = false;
            accountCtx = UserAccountContextFactory.getContextSavaPage();
        }

        // INVARIANT: there must be enough balance left after the sales.
        if (!this.checkCreditLimit(dto, accountCtx, isPaperCutAccount)) {
            return;
        }

        // Charge POS sales.
        final AbstractJsonRpcMethodResponse rpcResponse;

        if (isPaperCutAccount) {
            final PaperCutServerProxy proxy =
                    PaperCutServerProxy.create(ConfigManager.instance(), true);
            rpcResponse = ACCOUNTING_SERVICE.chargePosSales(proxy, dto);
        } else {
            rpcResponse = ACCOUNTING_SERVICE.chargePosSales(dto);
        }

        if (rpcResponse.isResult()) {
            this.setApiResultText(ApiResultCodeEnum.INFO,
                    String.format("<div style=\"text-align: center; "
                            + "min-width: 250px; font-size: 32pt;\" class=\""
                            + MarkupHelper.CSS_TXT_VALID + "\">" //
                            + "<b>%s</b><br>%s&nbsp;%s.%s" //
                            + "</div>" //
                            + "<div style=\"text-align: center; "
                            + "font-size: 16pt;\"  class=\"" //
                            + MarkupHelper.CSS_TXT_VALID + "\">" + "%s</div>",
                            dto.getUserId(),
                            // NounEnum.ITEM.uiText(getLocale()),
                            // dto.createComment(),
                            ServiceContext.getAppCurrencySymbol(),
                            dto.getAmountMain(), dto.getAmountCents(),
                            AdjectiveEnum.PAID.uiText(getLocale())));
        } else {
            this.setApiResultText(rpcResponse);
        }
    }

    /**
     * Checks if there is enough balance left after the sales.
     *
     * @param dto
     *            Sales.
     * @param accountCtx
     *            Account context.
     * @param isPaperCutAccount
     *            {@code true} is POS sales is charged to PaperCut account.
     * @return {@code true} if checked OK.
     */
    private boolean checkCreditLimit(final PosSalesDto dto,
            final UserAccountContext accountCtx,
            final boolean isPaperCutAccount) {

        final BigDecimal userBalance;
        final BigDecimal userCreditLimit;

        if (CONFIG_MNGR
                .isConfigValue(Key.FINANCIAL_POS_SALES_CREDIT_LIMIT_ENABLE)) {

            userBalance = accountCtx.getUserBalance(
                    USER_DAO.findActiveUserById(dto.getUserKey()));
            userCreditLimit = CONFIG_MNGR
                    .getConfigBigDecimal(Key.FINANCIAL_POS_SALES_CREDIT_LIMIT);

        } else {

            if (isPaperCutAccount) {

                // Let PaperCut apply its credit limit check.
                userBalance = null;
                userCreditLimit = null;

            } else {

                final AccountDisplayInfoDto accountInfo =
                        ACCOUNTING_SERVICE.getAccountDisplayInfo(
                                USER_DAO.findActiveUserById(dto.getUserKey()),
                                Locale.ENGLISH, null);

                if (accountInfo.getCreditLimit() == null) {
                    userBalance = null;
                    userCreditLimit = null;
                } else {
                    userBalance = BigDecimal.valueOf(
                            Double.parseDouble(accountInfo.getBalance()));
                    userCreditLimit = BigDecimal.valueOf(
                            Double.parseDouble(accountInfo.getCreditLimit()));
                }
            }
        }

        // Check
        if (userBalance != null && userCreditLimit != null) {

            final BigDecimal balanceAfter =
                    userBalance.subtract(dto.amountBigDecimal());

            // Note: credit limit is a positive amount.
            if (balanceAfter.compareTo(userCreditLimit.negate()) < 0) {

                this.setApiResultText(ApiResultCodeEnum.ERROR,
                        PhraseEnum.AMOUNT_EXCEEDS_CREDIT.uiText(getLocale(),
                                dto.getUserId()));
                return false;
            }
        }
        return true;
    }
}
