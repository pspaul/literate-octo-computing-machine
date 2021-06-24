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
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.dto.QuickSearchFilterPrinterDto;
import org.savapage.core.dto.QuickSearchItemDto;
import org.savapage.core.dto.QuickSearchPrinterItemDto;
import org.savapage.core.jpa.User;
import org.savapage.core.json.JsonPrinter;
import org.savapage.core.json.JsonPrinterList;
import org.savapage.server.api.request.ReqQuickSearchMixin.DtoQuickSearchRsp;

/**
 * Proxy Printers Quick Search.
 *
 * @author Rijk Ravestein
 *
 */
public abstract class ReqPrinterQuickSearchMixin extends ApiRequestMixin {

    /**
     *
     * @author Rijk Ravestein
     *
     */
    private static class DtoRsp extends DtoQuickSearchRsp {

        private List<QuickSearchItemDto> items;
        private Boolean fastPrintAvailable;

        @SuppressWarnings("unused")
        public List<QuickSearchItemDto> getItems() {
            return items;
        }

        public void setItems(List<QuickSearchItemDto> items) {
            this.items = items;
        }

        @SuppressWarnings("unused")
        public Boolean getFastPrintAvailable() {
            return fastPrintAvailable;
        }

        public void setFastPrintAvailable(Boolean fastPrintAvailable) {
            this.fastPrintAvailable = fastPrintAvailable;
        }

    }

    /**
     * Gets the printers.
     *
     * @param requestingUser
     *            Unique ID of requesting user.
     * @return The {@link JsonPrinterList}.
     */
    protected abstract JsonPrinterList getPrinterList(String requestingUser);

    @Override
    protected final void onRequest(final String requestingUser,
            final User lockedUser) throws IOException {

        final QuickSearchFilterPrinterDto dto = AbstractDto.create(
                QuickSearchFilterPrinterDto.class, this.getParmValueDto());

        final int startPosition;
        if (dto.getStartPosition() == null) {
            startPosition = 0;
        } else {
            startPosition = dto.getStartPosition().intValue();
        }

        final boolean searchCupsName =
                BooleanUtils.isTrue(dto.getSearchCupsName());

        final JsonPrinterList jsonPrinterList =
                this.getPrinterList(requestingUser);

        final int maxItems = dto.getMaxResults().intValue();
        final String filter = dto.getFilter().toLowerCase();

        final List<QuickSearchItemDto> items = new ArrayList<>();

        /*
         * First iteration to collect the items, fastPrintAvaliable.
         */
        final Iterator<JsonPrinter> iter = jsonPrinterList.getList().iterator();

        // Is printer with Fast Proxy Print available?
        Boolean fastPrintAvailable = null;

        int totalResults = 0;
        int iPosition = 0;

        while (iter.hasNext()) {

            final JsonPrinter printer = iter.next();

            final boolean isJobTicketPrinter =
                    BooleanUtils.isTrue(printer.getJobTicket());

            if (dto.getJobTicket() != null && dto.getJobTicket()
                    .booleanValue() != isJobTicketPrinter) {
                continue;
            }

            if (printer.getAuthMode() != null
                    && printer.getAuthMode().isFast()) {
                fastPrintAvailable = Boolean.TRUE;
            }

            final String location = printer.getLocation();

            if (StringUtils.isEmpty(filter)
                    || printer.getAlias().toLowerCase().contains(filter)
                    || (StringUtils.isNotBlank(location)
                            && location.toLowerCase().contains(filter))
                    || (searchCupsName && printer.getName().toLowerCase()
                            .contains(filter))) {

                totalResults++;
                iPosition++;

                if (iPosition > startPosition && items.size() < maxItems) {

                    final QuickSearchPrinterItemDto itemWlk =
                            new QuickSearchPrinterItemDto();

                    itemWlk.setKey(printer.getDbKey());
                    itemWlk.setText(printer.getAlias());
                    itemWlk.setPrinter(printer);

                    items.add(itemWlk);
                }
            }
        }

        /*
         * We need to know if there are any "Fast Release" printers available
         * (even if not part of this search list). So, if fastPrintAvaliable was
         * NOT collected iterate the remainder of the list.
         *
         * Reason: the client may want to display a button to extend the Fast
         * Print Closing Time.
         */
        if (fastPrintAvailable == null) {

            fastPrintAvailable = Boolean.FALSE;

            while (iter.hasNext()) {

                final JsonPrinter printer = iter.next();

                if (printer.getAuthMode() != null
                        && printer.getAuthMode().isFast()) {
                    fastPrintAvailable = Boolean.TRUE;
                    break;
                }
            }
        }

        final DtoRsp rsp = new DtoRsp();
        rsp.setItems(items);
        rsp.setFastPrintAvailable(fastPrintAvailable);
        rsp.calcNavPositions(dto.getMaxResults().intValue(), startPosition,
                totalResults);

        setResponse(rsp);
        setApiResultOk();
    }

}
