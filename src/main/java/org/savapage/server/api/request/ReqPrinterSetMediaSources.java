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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.api.request;

import java.io.IOException;

import org.savapage.core.dao.PrinterDao;
import org.savapage.core.dto.ProxyPrinterMediaSourcesDto;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.User;
import org.savapage.core.json.JsonAbstractBase;
import org.savapage.core.json.rpc.AbstractJsonRpcMethodResponse;
import org.savapage.core.services.ServiceContext;

/**
 * Sets the Media Sources of a Proxy {@link Printer}.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqPrinterSetMediaSources extends ApiRequestMixin {

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final PrinterDao printerDao =
                ServiceContext.getDaoContext().getPrinterDao();

        final ProxyPrinterMediaSourcesDto dto = JsonAbstractBase
                .create(ProxyPrinterMediaSourcesDto.class, getParmValueDto());

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

        final AbstractJsonRpcMethodResponse rpcResponse = PROXY_PRINT_SERVICE
                .setProxyPrinterCostMediaSources(jpaPrinter, dto);

        if (rpcResponse.isResult()) {
            setApiResult(ApiResultCodeEnum.OK, "msg-printer-saved-ok");
        } else {
            setApiResultText(rpcResponse);
        }
    }

}
