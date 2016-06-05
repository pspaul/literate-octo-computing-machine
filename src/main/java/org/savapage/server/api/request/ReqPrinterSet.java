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

import org.savapage.core.dao.PrinterDao;
import org.savapage.core.dto.ProxyPrinterDto;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.User;
import org.savapage.core.json.JsonAbstractBase;
import org.savapage.core.services.ServiceContext;

/**
 * Sets the (basic) properties of a Proxy {@link Printer}.
 * <p>
 * Also, a logical delete can be applied or reversed.
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqPrinterSet extends ApiRequestMixin {

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final PrinterDao printerDao =
                ServiceContext.getDaoContext().getPrinterDao();

        final ProxyPrinterDto dto = JsonAbstractBase
                .create(ProxyPrinterDto.class, getParmValue("dto"));

        final long id = dto.getId();

        final Printer jpaPrinter = printerDao.findById(id);

        /*
         * INVARIANT: printer MUST exist.
         */
        if (jpaPrinter == null) {
            setApiResult(ApiResultCodeEnum.ERROR, "msg-printer-not-found",
                    String.valueOf(id));
            return;
        }

        PROXY_PRINT_SERVICE.setProxyPrinterProps(jpaPrinter, dto);

        setApiResult(ApiResultCodeEnum.OK, "msg-printer-saved-ok");
    }

}
