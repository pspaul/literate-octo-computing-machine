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
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpException;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.dto.QuickSearchFilterDto;
import org.savapage.core.dto.QuickSearchItemDto;
import org.savapage.core.dto.QuickSearchPrinterItemDto;
import org.savapage.core.ipp.IppSyntaxException;
import org.savapage.core.ipp.client.IppConnectException;
import org.savapage.core.jpa.User;
import org.savapage.core.json.JsonPrinter;
import org.savapage.core.json.JsonPrinterList;

/**
 * Proxy Printers Quicksearch.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqPrinterQuickSearch extends ApiRequestMixin {

    /**
     *
     * @author Rijk Ravestein
     *
     */
    private static class DtoRsp extends AbstractDto {

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
     * Gets the printers for requesting user on this terminal.
     *
     * @param userName
     *            The requesting user.
     * @return The {@linkJsonPrinterList}.
     * @throws Exception
     */
    private JsonPrinterList getUserPrinterList(final String userName) {

        final JsonPrinterList jsonPrinterList;

        try {
            jsonPrinterList = PROXY_PRINT_SERVICE.getUserPrinterList(
                    ApiRequestHelper.getHostTerminal(this.getRemoteAddr()),
                    userName);

        } catch (IppConnectException | IppSyntaxException e) {
            throw new SpException(e.getMessage());
        }

        if (jsonPrinterList.getDfault() != null) {
            PROXY_PRINT_SERVICE.localize(this.getLocale(),
                    jsonPrinterList.getDfault());
        }

        return jsonPrinterList;
    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final QuickSearchFilterDto dto = AbstractDto
                .create(QuickSearchFilterDto.class, this.getParmValue("dto"));

        final List<QuickSearchItemDto> items = new ArrayList<>();

        final JsonPrinterList jsonPrinterList =
                this.getUserPrinterList(requestingUser);

        final int maxItems = dto.getMaxResults().intValue();

        final String filter = dto.getFilter().toLowerCase();

        /*
         * First iteration to collect the items, fastPrintAvaliable.
         */
        final Iterator<JsonPrinter> iter = jsonPrinterList.getList().iterator();

        // Is printer with Fast Proxy Print available?
        Boolean fastPrintAvailable = null;

        while (iter.hasNext()) {

            final JsonPrinter printer = iter.next();

            if (printer.getAuthMode() != null
                    && printer.getAuthMode().isFast()) {
                fastPrintAvailable = Boolean.TRUE;
            }

            final String location = printer.getLocation();

            if (StringUtils.isEmpty(filter)
                    || printer.getAlias().toLowerCase().contains(filter)
                    || (StringUtils.isNotBlank(location)
                            && location.toLowerCase().contains(filter))) {

                if (items.size() < maxItems) {

                    final QuickSearchPrinterItemDto itemWlk =
                            new QuickSearchPrinterItemDto();

                    itemWlk.setKey(printer.getDbKey());
                    itemWlk.setText(printer.getAlias());
                    itemWlk.setPrinter(printer);

                    items.add(itemWlk);
                } else {
                    break;
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

        //
        final DtoRsp rsp = new DtoRsp();
        rsp.setItems(items);
        rsp.setFastPrintAvailable(fastPrintAvailable);

        setResponse(rsp);
        setApiResultOk();
    }

}
