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
import java.util.ArrayList;
import java.util.List;

import org.savapage.core.dao.AccountDao;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.dto.QuickSearchFilterDto;
import org.savapage.core.dto.QuickSearchItemDto;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.User;
import org.savapage.core.services.ServiceContext;

/**
 * Shared Account Quicksearch.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqSharedAccountQuickSearch extends ApiRequestMixin {

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
    protected void
            onRequest(final String requestingUser, final User lockedUser)
                    throws IOException {

        final QuickSearchFilterDto dto =
                AbstractDto.create(QuickSearchFilterDto.class,
                        this.getParmValue("dto"));

        final List<QuickSearchItemDto> items = new ArrayList<>();

        //
        final AccountDao.ListFilter filter = new AccountDao.ListFilter();

        filter.setAccountType(AccountTypeEnum.SHARED);
        filter.setContainingNameText(dto.getFilter());
        filter.setDeleted(Boolean.FALSE);

        final AccountDao accountDao =
                ServiceContext.getDaoContext().getAccountDao();

        final List<Account> accountList =
                accountDao.getListChunk(filter, null, dto.getMaxResults(),
                        AccountDao.Field.NAME, true);

        final StringBuilder name = new StringBuilder();

        for (final Account account : accountList) {

            final QuickSearchItemDto itemWlk = new QuickSearchItemDto();

            itemWlk.setKey(account.getId());

            name.setLength(0);
            name.append(account.getName());

            final Account parent = account.getParent();
            if (parent != null) {
                name.append(" (").append(parent.getName()).append(")");
            }
            itemWlk.setText(name.toString());

            items.add(itemWlk);
        }

        //
        final DtoRsp rsp = new DtoRsp();
        rsp.setItems(items);

        setResponse(rsp);
        setApiResultOk();
    }

}
