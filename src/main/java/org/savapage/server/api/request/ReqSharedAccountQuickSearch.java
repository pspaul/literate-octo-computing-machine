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
import java.util.ArrayList;
import java.util.List;

import org.savapage.core.dao.UserGroupAccountDao;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.dto.QuickSearchFilterDto;
import org.savapage.core.dto.QuickSearchItemDto;
import org.savapage.core.dto.SharedAccountDto;
import org.savapage.core.jpa.User;
import org.savapage.core.services.ServiceContext;
import org.savapage.server.session.SpSession;

/**
 * Shared Account Quicksearch.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqSharedAccountQuickSearch extends ReqQuickSearchMixin {

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

        final QuickSearchFilterDto dto = AbstractDto
                .create(QuickSearchFilterDto.class, this.getParmValue("dto"));

        final int maxResult = dto.getMaxResults().intValue();
        final int currPosition = dto.getStartPosition();

        final List<QuickSearchItemDto> items = new ArrayList<>();

        //
        final UserGroupAccountDao.ListFilter filter =
                new UserGroupAccountDao.ListFilter();

        // shortcut user
        filter.setUserId(SpSession.get().getUser().getId());

        //
        filter.setContainingNameText(dto.getFilter());
        filter.setDisabled(Boolean.FALSE);

        final UserGroupAccountDao dao =
                ServiceContext.getDaoContext().getUserGroupAccountDao();

        final int totalResults;

        if (dto.getTotalResults() == null) {
            totalResults = (int) dao.getListCount(filter);
        } else {
            totalResults = dto.getTotalResults().intValue();
        }

        final List<SharedAccountDto> accountList = dao.getListChunk(filter,
                Integer.valueOf(currPosition), dto.getMaxResults());

        for (final SharedAccountDto account : accountList) {

            final QuickSearchItemDto itemWlk = new QuickSearchItemDto();

            itemWlk.setKey(account.getId());
            itemWlk.setText(account.nameAsQuickSearch());

            items.add(itemWlk);
        }

        //
        final DtoRsp rsp = new DtoRsp();
        rsp.setItems(items);
        rsp.calcNavPositions(maxResult, currPosition, totalResults);

        setResponse(rsp);
        setApiResultOk();
    }

}
