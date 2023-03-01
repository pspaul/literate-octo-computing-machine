/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2023 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2023 Datraverse B.V. <info@datraverse.com>
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
package org.savapage.ext;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Office365 Utility.
 *
 * @author Rijk Ravestein
 *
 */
public final class Office365Util {

    /** */
    private static final String OAUTH_SCOPE_DEFAULT =
            "https://outlook.office365.com/.default";

    /** */
    private static final String OAUTH_GRANT_TYPE_CLIENT_CREDENTIALS =
            "client_credentials";

    /** Utility class. */
    private Office365Util() {
    }

    /**
     * Retrieves OAuth access token for SMTP.
     * <p>
     * <b>Reserved for future use.</b>
     * </p>
     *
     * @param tenantId
     *            Tenant ID
     * @param oauthClientId
     *            Client ID
     * @param oauthClientSecretValue
     * @return token The OAuth token.
     * @throws IOException
     *             IO error.
     */
    public static String retrieveOAuthTokenSmtp(final String tenantId,
            final String oauthClientId, final String oauthClientSecretValue)
            throws IOException {
        return retrieveOAuthToken(tenantId, oauthClientId,
                oauthClientSecretValue, OAUTH_SCOPE_DEFAULT,
                OAUTH_GRANT_TYPE_CLIENT_CREDENTIALS);
    }

    /**
     * Retrieves OAuth access token for IMAP.
     *
     * @param tenantId
     *            Tenant ID
     * @param oauthClientId
     *            Client ID
     * @param oauthClientSecretValue
     * @return token The OAuth token.
     * @throws IOException
     *             IO error.
     */
    public static String retrieveOAuthTokenImap(final String tenantId,
            final String oauthClientId, final String oauthClientSecretValue)
            throws IOException {
        return retrieveOAuthToken(tenantId, oauthClientId,
                oauthClientSecretValue, OAUTH_SCOPE_DEFAULT,
                OAUTH_GRANT_TYPE_CLIENT_CREDENTIALS);
    }

    /**
     * Retrieves OAuth access token.
     *
     * @param tenantId
     *            Tenant ID
     * @param oauthClientId
     *            Client ID
     * @param oauthClientSecretValue
     * @param scope
     *            Scope
     * @param grantType
     *            Grant type.
     * @return token The OAuth token.
     * @throws IOException
     *             IO error.
     */
    private static String retrieveOAuthToken(final String tenantId,
            final String oauthClientId, final String oauthClientSecretValue,
            final String scope, final String grantType) throws IOException {

        final HttpPost loginPost =
                new HttpPost("https://login.microsoftonline.com/" + tenantId
                        + "/oauth2/v2.0/token");

        final String encodedBody = "client_id=" + oauthClientId + "&scope="
                + scope + "&client_secret=" + oauthClientSecretValue
                + "&grant_type=" + grantType;

        loginPost.setEntity(new StringEntity(encodedBody,
                ContentType.APPLICATION_FORM_URLENCODED));
        loginPost.addHeader(new BasicHeader("cache-control", "no-cache"));

        try (CloseableHttpClient client = HttpClients.createDefault();
                CloseableHttpResponse loginResponse = client.execute(loginPost);
                InputStream inputStream =
                        loginResponse.getEntity().getContent()) {

            // Note: inputStream.readAllBytes() is not available in Java 1.8
            final byte[] response = IOUtils.toByteArray(inputStream);

            final ObjectMapper objectMapper = new ObjectMapper();
            final JavaType type = objectMapper.constructType(
                    objectMapper.getTypeFactory().constructParametricType(
                            Map.class, String.class, String.class));

            final Map<String, String> parsed =
                    new ObjectMapper().readValue(response, type);

            return parsed.get("access_token");
        }
    }

}
