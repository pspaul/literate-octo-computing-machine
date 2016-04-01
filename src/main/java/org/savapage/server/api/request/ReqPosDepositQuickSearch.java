/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.api.request;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.AccountTrxDao;
import org.savapage.core.dao.UserAccountDao;
import org.savapage.core.dao.enums.AccountTrxTypeEnum;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.dto.QuickSearchFilterDto;
import org.savapage.core.dto.QuickSearchItemDto;
import org.savapage.core.dto.QuickSearchPosPurchaseItemDto;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.jpa.PosPurchase;
import org.savapage.core.jpa.User;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.LocaleHelper;
import org.savapage.core.util.QuickSearchDate;
import org.savapage.server.SpSession;

/**
 * POS Deposit Quicksearch.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqPosDepositQuickSearch extends ApiRequestMixin {

    /**
     *
     * @author Rijk Ravestein
     *
     */
    private static class DtoRsp extends AbstractDto {

        private List<QuickSearchItemDto> items;

        @SuppressWarnings("unused")
        public List<QuickSearchItemDto> getItems() {
            return items;
        }

        public void setItems(List<QuickSearchItemDto> items) {
            this.items = items;
        }

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final QuickSearchFilterDto dto = AbstractDto
                .create(QuickSearchFilterDto.class, this.getParmValue("dto"));

        final AccountTrxDao accountTrxDao =
                ServiceContext.getDaoContext().getAccountTrxDao();

        final UserAccountDao userAccountDao =
                ServiceContext.getDaoContext().getUserAccountDao();

        final AccountTrxDao.ListFilter filter = new AccountTrxDao.ListFilter();
        filter.setAccountType(AccountTypeEnum.USER);
        filter.setTrxType(AccountTrxTypeEnum.DEPOSIT);

        final List<QuickSearchItemDto> list = new ArrayList<>();

        //
        Date dateFrom = null;
        try {
            dateFrom = QuickSearchDate.toDate(dto.getFilter());
        } catch (ParseException e) {
            dateFrom = null;
        }
        filter.setDateFrom(dateFrom);

        //
        final LocaleHelper localeHelper =
                new LocaleHelper(SpSession.get().getLocale());

        final String currencySymbol = SpSession.getAppCurrencySymbol();
        final int balanceDecimals = ConfigManager.getUserBalanceDecimals();

        QuickSearchPosPurchaseItemDto itemWlk;

        for (final AccountTrx accountTrx : accountTrxDao.getListChunk(filter, 0,
                dto.getMaxResults(), AccountTrxDao.Field.TRX_DATE, false)) {

            final PosPurchase purchase = accountTrx.getPosPurchase();
            final User user = userAccountDao
                    .findByAccountId(accountTrx.getAccount().getId()).getUser();

            itemWlk = new QuickSearchPosPurchaseItemDto();

            itemWlk.setKey(accountTrx.getId());

            itemWlk.setComment(purchase.getComment());
            itemWlk.setPaymentType(purchase.getPaymentType());
            itemWlk.setReceiptNumber(purchase.getReceiptNumber());

            itemWlk.setDateTime(localeHelper
                    .getShortDateTime(accountTrx.getTransactionDate()));

            try {
                itemWlk.setTotalCost(
                        localeHelper.getCurrencyDecimal(purchase.getTotalCost(),
                                balanceDecimals, currencySymbol));
            } catch (ParseException e) {
                throw new IllegalArgumentException(e.getMessage());
            }

            itemWlk.setUserId(user.getUserId());
            itemWlk.setUserEmail(USER_SERVICE.getPrimaryEmailAddress(user));

            list.add(itemWlk);

        }
        //
        final DtoRsp rsp = new DtoRsp();
        rsp.setItems(list);

        setResponse(rsp);
        setApiResultOk();
    }

}
