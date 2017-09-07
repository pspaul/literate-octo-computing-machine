/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.cometd;

import org.savapage.core.jpa.PrintOut;

/**
 * User Events.
 *
 * <p>
 * <b>WARNING</b>: <i>This enum is duplicated in
 * org.savapage.client.cometd.UserEventMsgListener.UserEventEnum</i>
 * </p>
 * <p>
 * <b>TODO</b>: Move to org.savapage.common.
 * </p>
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
    SERVER_SHUTDOWN("Server Shutdown"),

    /**
     * System maintenance.
     */
    SYS_MAINTENANCE("System Maintenance");

    /**
     * The UI text.
     */
    private final String uiText;

    /**
     * @param text
     *            The UI text.
     */
    UserEventEnum(final String text) {
        this.uiText = text;
    }

    /**
     * @return The UI text.
     */
    public String getUiText() {
        return uiText;
    }

}
