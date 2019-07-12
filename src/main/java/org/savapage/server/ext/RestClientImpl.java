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
package org.savapage.server.ext;

import java.net.URI;
import java.util.Date;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.savapage.core.services.ServiceContext;
import org.savapage.ext.rest.RestClient;
import org.savapage.server.restful.dto.AbstractRestDto;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class RestClientImpl implements RestClient {

    /** */
    private final WebTarget webTarget;

    /**
     *
     * @param target
     *            REST web target.
     */
    private RestClientImpl(final WebTarget target) {
        this.webTarget = target;
    }

    @Override
    public String post(final String entity, final String mediaTypeReq,
            final String mediaTypeRsp) {

        final Invocation.Builder builder = this.webTarget.request(mediaTypeRsp);

        try (Response response =
                builder.post(Entity.entity(entity, mediaTypeReq));) {
            switch (response.getStatusInfo().toEnum()) {
            case CREATED:
                return response.readEntity(String.class);
            default:
                throw new IllegalStateException(
                        response.getStatusInfo().toString());
            }
        }
    }

    /**
     * @param uri
     *            Target URI.
     * @return A new {@link RestClient}.
     */
    public static RestClient create(final URI uri) {
        final Client client = ServiceContext.getServiceFactory()
                .getRestClientService().createClient();
        final WebTarget webTarget = client.target(uri);
        return new RestClientImpl(webTarget);
    }

    /**
     * @param uri
     *            Target URI.
     * @param username
     *            Basic Auth user.
     * @param password
     *            Basic Auth password.
     * @return A new {@link RestClient}.
     */
    public static RestClient create(final URI uri, final String username,
            final String password) {
        final Client client = ServiceContext.getServiceFactory()
                .getRestClientService().createClientAuth(username, password);
        final WebTarget webTarget = client.target(uri);
        return new RestClientImpl(webTarget);
    }

    @Override
    public String toISODateTimeZ(final Date date) {
        return AbstractRestDto.toISODateTimeZ(date);
    }
}
