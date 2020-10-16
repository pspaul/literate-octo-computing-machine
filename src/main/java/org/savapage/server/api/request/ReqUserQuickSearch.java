/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Author: Rijk Ravestein.
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
import java.util.ArrayList;
import java.util.List;

import org.savapage.core.dao.UserDao;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.dto.QuickSearchFilterDto;
import org.savapage.core.dto.QuickSearchItemDto;
import org.savapage.core.dto.QuickSearchUserItemDto;
import org.savapage.core.jpa.User;
import org.savapage.core.services.ServiceContext;
import org.savapage.server.session.SpSession;

/**
 * User Quicksearch.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqUserQuickSearch extends ApiRequestMixin {

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

        final UserDao userDao = ServiceContext.getDaoContext().getUserDao();

        final QuickSearchFilterDto dto = AbstractDto
                .create(QuickSearchFilterDto.class, this.getParmValueDto());

        final String currencySymbol = SpSession.getAppCurrencySymbol();

        final UserDao.ListFilter filter = new UserDao.ListFilter();

        filter.setContainingIdText(dto.getFilter());
        filter.setDeleted(Boolean.FALSE);
        filter.setPerson(Boolean.TRUE);

        final List<QuickSearchItemDto> list = new ArrayList<>();

        QuickSearchUserItemDto itemWlk;

        for (final User user : userDao.getListChunk(filter, 0,
                dto.getMaxResults(), UserDao.Field.USERID, true)) {

            itemWlk = new QuickSearchUserItemDto();

            itemWlk.setKey(user.getId());
            itemWlk.setText(user.getUserId());
            itemWlk.setEmail(USER_SERVICE.getPrimaryEmailAddress(user));

            itemWlk.setBalance(
                    ACCOUNTING_SERVICE.getFormattedUserBalance(user.getUserId(),
                            ServiceContext.getLocale(), currencySymbol));

            list.add(itemWlk);
        }

        //
        final DtoRsp rsp = new DtoRsp();
        rsp.setItems(list);

        setResponse(rsp);
        setApiResultOk();
    }

}
