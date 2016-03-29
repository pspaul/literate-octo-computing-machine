/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
 * Author: Rijk Ravestein.
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
package org.savapage.server.cometd;

import org.savapage.core.jpa.PrintOut;

/**
 * TODO: move this class to savapage-common project.
 *
 * @author Rijk Ravestein
 *
 */
public enum UserEventEnum {

    /**
     * Changes in user account.
     */
    ACCOUNT("Account"),

    /**
     * Job Ticket message.
     */
    JOBTICKET("Job Ticket"),

    /**
     * SafePages are printed.
     */
    PRINT_IN("Print"),

    /**
     * SafePages are expired.
     */
    PRINT_IN_EXPIRED("Print-in Expired"),

    /**
     * {@link PrintOut} notification message.
     */
    PRINT_MSG("Proxy Print"),

    /**
     * An error occurred.
     */
    ERROR("Error"),

    /**
     * The NULL event.
     */
    NULL(""),

    /**
     * Server shutdown in progress.
     */
    SERVER_SHUTDOWN("Server Shutdowm");

    private final String uiText;

    private UserEventEnum(final String uiText) {
        this.uiText = uiText;
    }

    public String getUiText() {
        return uiText;
    }

}
