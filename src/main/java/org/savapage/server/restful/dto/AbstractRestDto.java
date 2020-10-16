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
package org.savapage.server.restful.dto;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.ws.rs.WebApplicationException;

import org.savapage.core.dto.AbstractDto;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class AbstractRestDto extends AbstractDto {

    /**
     * @param pojo
     *            POJO object.
     * @return Pretty printed JSON.
     */
    protected static String toJSON(final Object pojo) {
        try {
            return prettyPrinted(pojo);
        } catch (IOException e) {
            throw new WebApplicationException(e.getMessage());
        }
    }

    /**
     * Formats a {@link Date} to a ISO 8601 formatted date-time string
     * <i>with</i> time-zone.
     * <p>
     * <a href="https://en.wikipedia.org/wiki/ISO_8601">ISO 8601</a>
     * </p>
     *
     * @param date
     *            The date to convert.
     * @return The formatted date string.
     */
    public static String toISODateTimeZ(final Date date) {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(date);
    }

}
