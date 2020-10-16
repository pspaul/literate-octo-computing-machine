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
package org.savapage.server.restful;

import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.savapage.server.restful.services.RestDocumentsService;
import org.savapage.server.restful.services.RestSystemService;
import org.savapage.server.restful.services.RestTestService;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class RestApplication extends ResourceConfig {

    /** */
    public static final String RESTFUL_URL_PATH = "/restful/v1";

    /** */
    public static final String SERVLET_URL_PATTERN = RESTFUL_URL_PATH + "/*";

    /**
     * RESTFul services to register.
     */
    private static final Class<?>[] SERVICES_REGISTRY = new Class<?>[] { //
            RestSystemService.class, //
            RestDocumentsService.class, //
            RestTestService.class //
    };

    /**
     * RESTful Server Application.
     */
    public RestApplication() {

        this.packages(org.savapage.server.restful.services.IRestService.class
                .getPackage().getName());

        /*
         * Log level can be overwritten in log4j.properties. For example:
         *
         * log4j.logger.org.glassfish.jersey=TRACE
         */
        this.register(new LoggingFeature(
                java.util.logging.Logger
                        .getLogger(LoggingFeature.DEFAULT_LOGGER_NAME),
                java.util.logging.Level.WARNING,
                LoggingFeature.Verbosity.PAYLOAD_ANY,
                LoggingFeature.DEFAULT_MAX_ENTITY_SIZE));
        //
        this.register(RestAuthFilter.class);
        //
        this.register(MultiPartFeature.class);
        //
        for (final Class<?> clazz : SERVICES_REGISTRY) {
            this.register(clazz);
        }
    }

}
