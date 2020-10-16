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

import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.PrinterDao;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.ipp.IppSyntaxException;
import org.savapage.core.job.SpJobScheduler;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.User;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.SnmpRetrieveService;

/**
 * Retrieve SNMP data for printer(s).
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqPrinterSnmp extends ApiRequestMixin {

    /** */
    private static final ProxyPrintService PROXY_PRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();

    /** */
    private static final SnmpRetrieveService SNMP_RETRIEVE_SERVICE =
            ServiceContext.getServiceFactory().getSnmpRetrieveService();

    /**
     * The request.
     */
    private static class DtoReq extends AbstractDto {

        /**
         * The primary database key of a {@link Printer}, or {@code null} for
         * all printers.
         */
        private Long printerId;

        public Long getPrinterId() {
            return printerId;
        }

        @SuppressWarnings("unused")
        public void setPrinterId(Long printerId) {
            this.printerId = printerId;
        }

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IppSyntaxException {

        if (!ConfigManager.instance().isConfigValue(Key.PRINTER_SNMP_ENABLE)) {
            setApiResultText(ApiResultCodeEnum.ERROR, "Operation is disabled.");
            return;
        }

        if (SpJobScheduler.isAllPrinterSnmpJobExecuting()) {
            setApiResult(ApiResultCodeEnum.WARN, "msg-printer-snmp-busy");
            return;
        }

        final DtoReq dtoReq = DtoReq.create(DtoReq.class, getParmValueDto());
        final Long printerID = dtoReq.getPrinterId();

        if (printerID == null) {

            SNMP_RETRIEVE_SERVICE.retrieveAll();

        } else {

            final PrinterDao dao =
                    ServiceContext.getDaoContext().getPrinterDao();

            final Printer printer = dao.findById(printerID);
            final String host = PROXY_PRINT_SERVICE
                    .getCachedPrinterHost(printer.getPrinterName());

            if (!SNMP_RETRIEVE_SERVICE.claimSnmpRetrieve(host)) {
                setApiResult(ApiResultCodeEnum.WARN, "msg-printer-snmp-busy");
                return;
            }

            SNMP_RETRIEVE_SERVICE.retrieve(printerID);
        }

        setApiResult(ApiResultCodeEnum.OK, "msg-printer-snmp-ok");
    }

}
