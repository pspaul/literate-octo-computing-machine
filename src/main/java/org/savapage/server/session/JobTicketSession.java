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
package org.savapage.server.session;

import java.util.Map;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class JobTicketSession {

    public enum PrinterOpt {
        MEDIA_SOURCE, MEDIA_SOURCE_SHEET, OUTPUT_BIN
    }

    private String jogOffsetOption;

    /**
     * Printer ID (DB primary key) of last selected redirect printer.
     */
    private Long lastRedirectPrinterId;

    /**
     * Map with key (printer db key) and most recent IPP option choices.
     */
    private Map<Long, Map<PrinterOpt, String>> redirectPrinterOptions;

    public String getJogOffsetOption() {
        return jogOffsetOption;
    }

    public void setJogOffsetOption(String jogOffsetOption) {
        this.jogOffsetOption = jogOffsetOption;
    }

    public Map<Long, Map<PrinterOpt, String>> getRedirectPrinterOptions() {
        return redirectPrinterOptions;
    }

    public void setRedirectPrinterOptions(
            Map<Long, Map<PrinterOpt, String>> redirectPrinterOptions) {
        this.redirectPrinterOptions = redirectPrinterOptions;
    }

    /**
     * @return Printer ID (DB primary key) of last selected redirect printer.
     */
    public Long getLastRedirectPrinterId() {
        return lastRedirectPrinterId;
    }

    /**
     * @param lastRedirectPrinterId
     *            Printer ID (DB primary key) of last selected redirect printer.
     */
    public void setLastRedirectPrinterId(Long lastRedirectPrinterId) {
        this.lastRedirectPrinterId = lastRedirectPrinterId;
    }

}
