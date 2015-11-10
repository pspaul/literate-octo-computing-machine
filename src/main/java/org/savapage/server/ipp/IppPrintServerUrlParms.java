/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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
package org.savapage.server.ipp;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.mapper.parameter.INamedParameters.NamedPair;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.mapper.parameter.UrlPathPageParametersEncoder;
import org.savapage.server.WebApp;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class IppPrintServerUrlParms {

    /**
     * The requesting printers parameter.
     */
    public static final String PARM_PRINTERS = WebApp.PATH_PRINTERS;

    /**
     * The requesting user number parameter.
     */
    public static final String PARM_USER_NUMBER = "user";

    /**
     * The requesting user UUID parameter.
     */
    public static final String PARM_USER_UUID = "uuid";

    /**
     * The value of {@link #PARM_PRINTERS}.
     */
    private final String printer;

    /**
     * The value of {@link #PARM_USER_NUMBER}.
     */
    private final String userNumber;

    /**
     * The value of {@link #PARM_USER_UUID}.
     */
    private final UUID userUuid;

    /**
     * URL as delivered by Wicket.
     */
    private final Url url;

    /**
     * Base URI as delivered by
     * {@link #IppPrintServerUrlParms(String, String, String, UUID)};
     */
    private final String uriBase;

    /**
     * Create from Wicket {@link Url}.
     *
     * @param url
     *            The {@link Url}.
     */
    public IppPrintServerUrlParms(final Url url) {

        this.url = url;
        this.uriBase = null;

        final UrlPathPageParametersEncoder encoder =
                new UrlPathPageParametersEncoder();

        String tmpPrinter = null;
        String tmpUserNumber = null;
        UUID tmpUserUuid = null;

        final PageParameters parameters = encoder.decodePageParameters(url);

        if (parameters != null) {

            final List<NamedPair> list = parameters.getAllNamed();

            for (final NamedPair pair : list) {

                final String parm = pair.getKey();
                final String value = pair.getValue();

                switch (parm) {
                case PARM_PRINTERS:
                    tmpPrinter = value;
                    break;
                case PARM_USER_NUMBER:
                    tmpUserNumber = value;
                    break;
                case PARM_USER_UUID:
                    try {
                        tmpUserUuid = UUID.fromString(value);
                    } catch (Exception e) {
                        // noop
                    }
                    break;
                default:
                    break;
                }
            }
        }

        /*
         * Make sure printer is an empty string (default queue) when no value
         * was found.
         */
        this.printer = StringUtils.defaultString(tmpPrinter);
        this.userNumber = tmpUserNumber;
        this.userUuid = tmpUserUuid;
    }

    /**
     *
     * @param printerPath
     * @param userNumber
     * @param userUuid
     */
    public IppPrintServerUrlParms(final String uriBase,
            final String printerPath, final String userNumber,
            final UUID userUuid) {

        this.uriBase = uriBase;
        this.printer = StringUtils.defaultString(printerPath);
        this.userNumber = userNumber;
        this.userUuid = userUuid;
        this.url = null;
    }

    /**
     * @return The value of {@link #PARM_PRINTERS}.
     */
    public String getPrinter() {
        return printer;
    }

    /**
     *
     * @return The requesting user number. Optional, can be {@code null}.
     */
    public String getUserNumber() {
        return userNumber;
    }

    /**
     *
     * @return The requesting user UUID parameter value. Optional, can be
     *         {@code null}.
     */
    public UUID getUserUuid() {
        return userUuid;
    }

    /**
     *
     * @return The {@link URI} representation.
     * @throws URISyntaxException
     *             When URI syntax errors.
     */
    public URI asUri() throws URISyntaxException {

        if (this.url != null) {
            return new URI(this.url.toString());
        }

        final StringBuilder builder = new StringBuilder();

        builder.append(this.uriBase).append('/').append(PARM_PRINTERS)
                .append('/').append(this.printer).append('/')
                .append(PARM_USER_NUMBER).append('/').append(this.userNumber)
                .append('/').append(PARM_USER_UUID).append('/')
                .append(this.userUuid);

        return new URI(builder.toString());
    }
}
