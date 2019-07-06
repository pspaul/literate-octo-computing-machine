/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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
package org.savapage.server.restful.services;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.savapage.core.SpInfo;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.services.ServiceContext;
import org.savapage.server.restful.RestApplication;
import org.savapage.server.restful.RestAuthFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST system API.
 * <p>
 * Implementation of Jersey extended WADL support is <i>under construction</i>.
 * <p>
 *
 * @author Rijk Ravestein
 *
 */
@Path("/" + RestSystemService.PATH_MAIN)
public final class RestSystemService implements IRestService {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(RestSystemService.class);

    /** */
    public static final String PATH_MAIN = "system";

    /** */
    private static final String PATH_SUB_VERSION = "version";

    /**
     * @return Application version.
     */
    @RolesAllowed(RestAuthFilter.ROLE_ALLOWED)
    @GET
    @Path(PATH_SUB_VERSION)
    @Produces(MediaType.TEXT_PLAIN)
    public String version() {
        return ConfigManager.getAppNameVersionBuild();
    }

    /**
     * Tests the service by calling a simple GET of this RESTful service. The
     * result is logged.
     */
    public static void test() {

        final ConfigManager cm = ConfigManager.instance();

        final Client client = ServiceContext.getServiceFactory()
                .getRestClientService().createClientAuth(
                        cm.getConfigValue(
                                IConfigProp.Key.API_RESTFUL_AUTH_USERNAME),
                        cm.getConfigValue(
                                IConfigProp.Key.API_RESTFUL_AUTH_PASSWORD));

        final WebTarget[] webTargets = new WebTarget[] { //
                client.target(
                        "http://localhost:" + ConfigManager.getServerPort())
                        .path(RestApplication.RESTFUL_URL_PATH).path(PATH_MAIN)
                        .path(PATH_SUB_VERSION), //
                client.target(
                        "https://localhost:" + ConfigManager.getServerSslPort())
                        .path(RestApplication.RESTFUL_URL_PATH).path(PATH_MAIN)
                        .path(PATH_SUB_VERSION) };

        for (final WebTarget webTarget : webTargets) {

            final Invocation.Builder invocationBuilder =
                    webTarget.request(MediaType.TEXT_PLAIN);

            try (Response response = invocationBuilder.get();) {
                final String version = response.readEntity(String.class);
                SpInfo.instance()
                        .log(String.format("%s test: GET %s -> %s [%s] [%s]",
                                RestSystemService.class.getSimpleName(),
                                webTarget.getUri().toString(),
                                response.getStatus(), response.getStatusInfo(),
                                version));

            } catch (ProcessingException e) {
                LOGGER.error(e.getMessage());
            }
        }
    }
}
