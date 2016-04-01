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
package org.savapage.server.raw;

/**
 *
 * @author Rijk Ravestein
 * @since 0.9.9
 */
public class RawPrintException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@link RawPrintException} without a message.
     */
    @SuppressWarnings("unused")
    private RawPrintException() {
        super("");
    }

    /**
     * Constructs a new {@link RawPrintException}.
     *
     * @param message
     *            The detail message.
     */
    public RawPrintException(final String message) {
        super(message);
    }

    /**
     * Constructs a new {@link RawPrintException}.
     *
     * @param message
     *            The detail message.
     * @param cause
     *            The cause.
     */
    public RawPrintException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
