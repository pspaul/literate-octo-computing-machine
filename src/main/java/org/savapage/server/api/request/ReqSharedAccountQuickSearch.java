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
package org.savapage.server.api.request;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.savapage.core.dao.UserGroupAccountDao;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.dto.QuickSearchFilterPreferredDto;
import org.savapage.core.dto.QuickSearchItemDto;
import org.savapage.core.dto.QuickSearchItemPreferredDto;
import org.savapage.core.dto.SharedAccountDto;
import org.savapage.core.jpa.User;
import org.savapage.core.services.ServiceContext;

/**
 * Shared Account Quicksearch.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqSharedAccountQuickSearch extends ReqQuickSearchMixin {

    /** */
    private static final UserGroupAccountDao USER_GROUP_ACCOUNT_DAO =
            ServiceContext.getDaoContext().getUserGroupAccountDao();

    /**
     *
     * @author Rijk Ravestein
     *
     */
    private static class DtoRsp extends DtoQuickSearchRsp {

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

        final QuickSearchFilterPreferredDto dto = AbstractDto.create(
                QuickSearchFilterPreferredDto.class, this.getParmValueDto());

        final int maxResult = dto.getMaxResults().intValue();
        final int currPosition = dto.getStartPosition();

        final User dbUser = ServiceContext.getDaoContext().getUserDao()
                .findById(this.getSessionUserDbKey());

        final Set<Long> preferredAccounts =
                USER_SERVICE.getPreferredDelegateAccounts(dbUser);

        //
        final UserGroupAccountDao.ListFilter filter =
                new UserGroupAccountDao.ListFilter();

        filter.setUserId(dbUser.getId());
        filter.setContainingNameText(dto.getFilter());
        filter.setDisabled(Boolean.FALSE);

        if (dto.isPreferred()) {
            filter.setAccountIds(preferredAccounts);
        }

        final int totalResults;
        final List<SharedAccountDto> accountList;

        if (dto.isPreferred() && preferredAccounts == null) {

            accountList = new ArrayList<>();
            totalResults = 0;

        } else {

            if (dto.getTotalResults() == null) {
                totalResults =
                        (int) USER_GROUP_ACCOUNT_DAO.getListCount(filter);
            } else {
                totalResults = dto.getTotalResults().intValue();
            }

            accountList = USER_GROUP_ACCOUNT_DAO.getListChunk(filter,
                    Integer.valueOf(currPosition), dto.getMaxResults());
        }

        final List<QuickSearchItemDto> items = new ArrayList<>();

        if (!accountList.isEmpty()) {

            for (final SharedAccountDto account : accountList) {

                final QuickSearchItemPreferredDto itemWlk =
                        new QuickSearchItemPreferredDto();

                itemWlk.setKey(account.getId());
                itemWlk.setText(account.nameAsQuickSearch());

                itemWlk.setPreferred(preferredAccounts != null
                        && preferredAccounts.contains(account.getId()));

                items.add(itemWlk);
            }
        }

        final DtoRsp rsp = new DtoRsp();
        rsp.setItems(items);
        rsp.calcNavPositions(maxResult, currPosition, totalResults);

        setResponse(rsp);
        setApiResultOk();
    }

}
