/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
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
package org.savapage.server.xmlrpc;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.savapage.common.dto.ClientAppConnectDto;
import org.savapage.common.dto.CometdConnectDto;
import org.savapage.core.cometd.CometdClientMixin;
import org.savapage.core.community.MemberCard;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.jpa.User;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.users.IExternalUserAuthenticator;
import org.savapage.core.users.InternalUserAuthenticator;
import org.savapage.core.users.UserAliasList;
import org.savapage.server.WebApp;
import org.savapage.server.auth.ClientAppUserAuthManager;
import org.savapage.server.auth.UserAuthToken;
import org.savapage.server.cometd.AbstractEventService;
import org.savapage.server.cometd.UserEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Handles requests from Client Application.
 *
 * @author Datraverse B.V.
 */
public final class ClientAppHandler {

    /**
     * The identification of the client JSON-RPC. This value is encrypted with
     * the SavaPage private key to generate the API key.
     * <p>
     * IMPORTANT: changing this value invalidates any previously issued API KEY.
     * </p>
     */
    public final static String API_CLIENT_ID = "savapage-client";

    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(ClientAppHandler.class);

    /**
     * Checks is the apiKey is valid.
     *
     * @param apiId
     * @param apiKey
     *            The apiKey.
     * @return {@code true} when valid
     */
    private static boolean
            isApiKeyValid(final String apiId, final String apiKey) {
        boolean isValid = true;
        try {
            MemberCard.instance().validateContent(apiId, apiKey);
        } catch (Exception e) {
            isValid = false;
        }
        return isValid;
    }

    /**
     *
     * @return
     * @throws MalformedURLException
     * @throws UnknownHostException
     */
    private static URL getCometdUrl() throws MalformedURLException,
            UnknownHostException {
        return new URL("https", ConfigManager.getServerHostAddress(),
                Integer.parseInt(ConfigManager.getServerSslPort()),
                WebApp.MOUNT_PATH_COMETD);
    }

    /**
     *
     * @param userId
     *            The unique user id.
     * @return The {@link URL} to open the User WebApp.
     * @throws MalformedURLException
     * @throws UnknownHostException
     * @throws URISyntaxException
     */
    private static URL getUserWebAppUrl(final String userId)
            throws URISyntaxException, MalformedURLException,
            UnknownHostException {

        // TODO: IConfigProp.Key to use SSL (or not).

        final URIBuilder builder = new URIBuilder();

        builder.setScheme("http").setHost(ConfigManager.getServerHostAddress())
                .setPort(Integer.parseInt(ConfigManager.getServerPort()))
                .setPath(WebApp.MOUNT_PATH_WEBAPP_USER)
                .addParameter(WebApp.URL_PARM_USER, userId);

        return builder.build().toURL();
    }

    /**
     *
     * @return The JSON string of this object.
     * @throws IOException
     *             When something goes wrong.
     */
    private String stringify(final Object object) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.generateJsonSchema(object.getClass()).toString();
        return mapper.writeValueAsString(object);
    }

    /**
     * Checks if user can be authenticated.
     *
     * @param userId
     *            The unique user id.
     * @param userPassword
     *            The user password. Can be {@code null} or empty when admin
     *            passkey is supplied.
     * @param userToken
     *            The Client App user token as supplied in an earlier
     *            authentication stage.
     * @param adminPassKey
     *            The admin passkey (can be null or empty).
     * @return {@code null} if user can NOT be authenticated.
     */
    private static UserAuthToken getUserAuthToken(final String userId,
            final String userPassword, final String userToken,
            final String adminPassKey) {

        UserAuthToken authToken;

        final String clientIpAddress = SpXmlRpcServlet.getClientIpAddress();

        final ConfigManager cm = ConfigManager.instance();

        final boolean isAuth;

        if (cm.isConfigValue(Key.CLIAPP_AUTH_TRUST_USER_ACCOUNT)) {

            isAuth = true;

        } else if (StringUtils.isNotBlank(adminPassKey)) {

            isAuth =
                    cm.getConfigValue(Key.CLIAPP_AUTH_ADMIN_PASSKEY).equals(
                            adminPassKey);
        } else {

            final String authenticatedWebAppUser;

            if (ConfigManager.instance().isConfigValue(
                    Key.CLIAPP_AUTH_TRUST_WEBAPP_USER_AUTH)) {
                authenticatedWebAppUser =
                        WebApp.getAuthUserByIpAddr(clientIpAddress);
            } else {
                authenticatedWebAppUser = null;
            }

            if (authenticatedWebAppUser != null
                    && authenticatedWebAppUser.equalsIgnoreCase(userId)) {
                /*
                 * Trust a successful user login into WebApp at same IP address.
                 */
                isAuth = true;

            } else {

                authToken =
                        ClientAppUserAuthManager
                                .getIpAuthToken(clientIpAddress);

                if (authToken != null && authToken.getUser().equals(userId)
                        && authToken.getToken().equals(userToken)) {
                    /*
                     * Trust a successful user login into Client App at same IP
                     * address with same token.
                     */
                    isAuth = true;

                } else {

                    final IExternalUserAuthenticator userAuthenticator =
                            cm.getUserAuthenticator();
                    /*
                     * Get the "real" username from the alias.
                     */
                    String uid =
                            UserAliasList.instance().getUserName(
                                    userAuthenticator.asDbUserId(userId));
                    uid = userAuthenticator.asDbUserId(uid);

                    ServiceContext.open();

                    try {
                        /*
                         * Read real user from database.
                         */
                        final User userDb =
                                ServiceContext.getDaoContext().getUserDao()
                                        .findActiveUserByUserId(uid);

                        if (userDb == null || !userDb.getPerson()) {

                            isAuth = false;

                        } else if (userDb.getInternal()) {

                            isAuth =
                                    InternalUserAuthenticator.authenticate(
                                            userDb, userPassword);
                        } else {

                            isAuth =
                                    userAuthenticator.authenticate(uid,
                                            userPassword) != null;
                        }

                    } finally {
                        ServiceContext.close();
                    }
                }
            }
        }

        if (isAuth) {
            authToken = new UserAuthToken(userId);
            ClientAppUserAuthManager.putIpAuthToken(clientIpAddress, authToken);
        } else {
            authToken = null;
            ClientAppUserAuthManager.removeUserAuthToken(clientIpAddress);
        }

        return authToken;
    }

    /**
     *
     * @param apiKey
     *            The API key.
     * @param userId
     *            The unique user id.
     * @param userPassword
     *            The user password. Can be {@code null} or empty when
     *            adminPassKey is supplied.
     * @param userToken
     *            The ClientApp user token as supplied in an earlier
     *            authentication stage.
     * @param adminPassKey
     *            The admin passkey (can be null or empty).
     * @return The {@link ClientAppConnectDto} JSON string.
     */
    public String getConnectInfo(final String apiKey, final String userId,
            final String userPassword, final String userToken,
            final String adminPassKey) {

        final ClientAppConnectDto dto = new ClientAppConnectDto();

        try {

            if (!SpXmlRpcServlet.isSslConnection()) {

                dto.setStatus(ClientAppConnectDto.Status.ERROR_FATAL);
                dto.setStatusMessage("Secure connection required.");

            } else if (!isApiKeyValid(API_CLIENT_ID, apiKey)) {

                dto.setStatus(ClientAppConnectDto.Status.ERROR_FATAL);
                dto.setStatusMessage("Invalid API Key.");

            } else {

                final UserAuthToken authToken =
                        getUserAuthToken(userId, userPassword, userToken,
                                adminPassKey);

                if (authToken == null) {

                    dto.setStatus(ClientAppConnectDto.Status.ERROR_AUTH);
                    dto.setStatusMessage("Authentication needed");

                } else {

                    dto.setUserAuthToken(authToken.getToken());
                    dto.setStatus(ClientAppConnectDto.Status.OK);

                    dto.setServerTime(new Date().getTime());
                    dto.setWebAppUrl(getUserWebAppUrl(userId).toString());

                    final CometdConnectDto cometdConnect =
                            new CometdConnectDto();

                    cometdConnect
                            .setAuthToken(CometdClientMixin.SHARED_USER_TOKEN);
                    cometdConnect.setMaxNetworkDelay(AbstractEventService
                            .getMaxNetworkDelay());
                    /*
                     * Note: public/subscribe channels are inverse for
                     * client/server.
                     */
                    cometdConnect
                            .setChannelPublish(UserEventService.CHANNEL_SUBSCRIPTION);
                    cometdConnect
                            .setChannelSubscribe(UserEventService.CHANNEL_PUBLISH);

                    cometdConnect.setUrl(getCometdUrl().toString());

                    dto.setCometd(cometdConnect);
                }
            }

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            dto.setStatus(ClientAppConnectDto.Status.ERROR_FATAL);
            dto.setStatusMessage(e.getMessage());
        }

        try {
            return stringify(dto);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            return "create JSON parse exception in client";
        }
    }
}
