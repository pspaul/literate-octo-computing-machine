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

import org.savapage.core.dto.AbstractDto;
import org.savapage.core.dto.QuickSearchFilterJobTicketDto;
import org.savapage.core.dto.QuickSearchItemDto;
import org.savapage.core.dto.QuickSearchJobTicketItemDto;
import org.savapage.core.jpa.User;
import org.savapage.core.services.JobTicketService;

/**
 * Job Ticket Quick Search.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqJobTicketQuickSearch extends ApiRequestMixin {

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

        final QuickSearchFilterJobTicketDto dto = AbstractDto.create(
                QuickSearchFilterJobTicketDto.class, this.getParmValueDto());

        final List<QuickSearchItemDto> list = new ArrayList<>();

        final JobTicketService.JobTicketFilter filter =
                new JobTicketService.JobTicketFilter();

        filter.setUserId(dto.getUserId());
        filter.setSearchTicketId(dto.getFilter());

        for (final String number : JOBTICKET_SERVICE.getTicketNumbers(filter,
                dto.getMaxResults())) {

            final QuickSearchJobTicketItemDto item =
                    new QuickSearchJobTicketItemDto();

            item.setKey(null);
            item.setText(number);

            list.add(item);
        }

        final DtoRsp rsp = new DtoRsp();
        rsp.setItems(list);

        setResponse(rsp);
        setApiResultOk();
    }

}
