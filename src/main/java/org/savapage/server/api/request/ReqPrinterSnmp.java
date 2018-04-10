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

import java.util.EnumSet;

import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.ipp.IppSyntaxException;
import org.savapage.core.job.SpJobScheduler;
import org.savapage.core.job.SpJobType;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.User;

/**
 * Retrieve SNMP data for printer(s).
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqPrinterSnmp extends ApiRequestMixin {

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

        if (SpJobScheduler
                .isJobCurrentlyExecuting(EnumSet.of(SpJobType.PRINTER_SNMP))) {

            setApiResult(ApiResultCodeEnum.WARN, "msg-printer-snmp-busy");
            return;
        }

        final DtoReq dtoReq = DtoReq.create(DtoReq.class, getParmValueDto());

        SpJobScheduler.instance()
                .scheduleOneShotPrinterSnmp(dtoReq.getPrinterId(), 0L);

        setApiResult(ApiResultCodeEnum.OK, "msg-printer-snmp-ok");
    }

}
