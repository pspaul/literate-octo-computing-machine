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
package org.savapage.server.xmlrpc;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.savapage.common.dto.ClientAppConnectDto;
import org.savapage.common.dto.CometdConnectDto;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.CometdClientMixin;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.community.MemberCard;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.jpa.User;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.users.IExternalUserAuthenticator;
import org.savapage.core.users.InternalUserAuthenticator;
import org.savapage.core.users.conf.UserAliasList;
import org.savapage.core.util.InetUtils;
import org.savapage.core.util.Messages;
import org.savapage.server.WebApp;
import org.savapage.server.WebAppParmEnum;
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
 * @author Rijk Ravestein
 *
 */
public final class ClientAppHandler {

    /**
     * The identification of the client JSON-RPC. This value is encrypted with
     * the SavaPage private key to generate the API key.
     * <p>
     * IMPORTANT: changing this value invalidates any previously issued API KEY.
     * </p>
     */
    public static final String API_CLIENT_ID = "savapage-client";

    /** */
    private static final int SSL_PORT = 443;

    /**
     *
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ClientAppHandler.class);

    /**
     * Checks is the apiKey is valid.
     *
     * @param apiId
     *            The API id.
     * @param apiKey
     *            The apiKey.
     * @return {@code true} when valid
     */
    private static boolean isApiKeyValid(final String apiId,
            final String apiKey) {
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
     * @return the ComnetD URL path.
     * @throws MalformedURLException
     *             If URL syntax error.
     * @throws UnknownHostException
     *             If URL host error.
     */
    private static String getCometdUrlPath()
            throws MalformedURLException, UnknownHostException {
        return new URL("https", "dummy", SSL_PORT, WebApp.MOUNT_PATH_COMETD)
                .getPath();
    }

    /**
     * Gets the {@link URL} template to open the User WebApp.
     *
     * @param userId
     *            The unique user id.
     * @return The query part of the {@link URL} to open the User WebApp.
     * @throws MalformedURLException
     *             If URL syntax error.
     * @throws UnknownHostException
     *             If URL host error.
     * @throws URISyntaxException
     *             If URI syntax error.
     */
    private static URL getUserWebAppURLTemplate(final String userId)
            throws URISyntaxException, MalformedURLException,
            UnknownHostException {

        final URIBuilder builder = new URIBuilder();

        builder.setScheme("http").setHost("dummy").setPort(SSL_PORT)
                .setPath(WebApp.MOUNT_PATH_WEBAPP_USER)
                .addParameter(WebAppParmEnum.SP_USER.parm(), userId);

        return builder.build().toURL();
    }

    /**
     * @param object
     *            The object to stringify.
     * @return The JSON string of this object.
     * @throws IOException
     *             When something goes wrong.
     */
    private String stringify(final Object object) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(object);
    }

    /**
     * Checks if user can be authenticated.
     *
     * @param clientIpAddress
     *            Client IP address of client.
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
    private static UserAuthToken getUserAuthToken(final String clientIpAddress,
            final String userId, final String userPassword,
            final String userToken, final String adminPassKey) {

        UserAuthToken authToken;

        final ConfigManager cm = ConfigManager.instance();

        final boolean isAuth;

        if (cm.isConfigValue(Key.CLIAPP_AUTH_TRUST_USER_ACCOUNT)) {

            isAuth = true;

        } else if (StringUtils.isNotBlank(adminPassKey)) {

            isAuth = cm.getConfigValue(Key.CLIAPP_AUTH_ADMIN_PASSKEY)
                    .equals(adminPassKey);
        } else {

            final String authenticatedWebAppUser;

            if (ConfigManager.instance()
                    .isConfigValue(Key.CLIAPP_AUTH_TRUST_WEBAPP_USER_AUTH)) {
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

                authToken = ClientAppUserAuthManager
                        .getIpAuthToken(clientIpAddress);

                if (authToken != null && authToken.getUser().equals(userId)
                        && authToken.getToken().equals(userToken)) {
                    /*
                     * Trust a successful user login into Client App at same IP
                     * address with same token.
                     */
                    isAuth = true;

                } else {
                    isAuth = isUserPasswordValid(userId, userPassword);
                }
            }
        }

        if (isAuth) {
            authToken = createUserAuthToken(clientIpAddress, userId);
        } else {
            authToken = null;
            ClientAppUserAuthManager.removeUserAuthToken(clientIpAddress);
        }

        return authToken;
    }

    /**
     * Creates {@link UserAuthToken} for authenticated user.
     *
     * @param clientIpAddress
     *            Client IP address of client.
     * @param userId
     *            The unique user id.
     * @return The token.
     */
    private static UserAuthToken createUserAuthToken(
            final String clientIpAddress, final String userId) {

        final UserAuthToken authToken = new UserAuthToken(userId);
        ClientAppUserAuthManager.putIpAuthToken(clientIpAddress, authToken);
        return authToken;
    }

    /**
     *
     * @param userId
     *            The unique user id.
     * @param userPassword
     *            The user password.
     * @return {@code true} when valid.
     */
    private static boolean isUserPasswordValid(final String userId,
            final String userPassword) {

        final IExternalUserAuthenticator userAuthenticator =
                ConfigManager.instance().getUserAuthenticator();

        /*
         * Get the "real" username from the alias.
         */
        String uid = UserAliasList.instance()
                .getUserName(userAuthenticator.asDbUserId(userId));
        uid = userAuthenticator.asDbUserId(uid);

        //
        final boolean isAuth;

        ServiceContext.open();

        try {
            /*
             * Read real user from database.
             */
            final User userDb = ServiceContext.getDaoContext().getUserDao()
                    .findActiveUserByUserId(uid);

            if (userDb == null || !userDb.getPerson()) {

                isAuth = false;

            } else if (userDb.getInternal()) {

                isAuth = InternalUserAuthenticator.authenticate(userDb,
                        userPassword);
            } else {

                isAuth = userAuthenticator.authenticate(uid,
                        userPassword) != null;
            }

        } finally {
            ServiceContext.close();
        }

        return isAuth;
    }

    /**
     *
     * @param apiKey
     *            The API key.
     * @param userId
     *            The unique user id.
     * @param userToken
     *            The ClientApp user token as supplied in an earlier
     *            authentication stage.
     * @return {@code true} when notification is handled.
     */
    public Boolean notifyExit(final String apiKey, final String userId,
            final String userToken) {

        if (!SpXmlRpcServlet.isSslConnection()
                || !isApiKeyValid(API_CLIENT_ID, apiKey)) {
            return Boolean.FALSE;
        }
        AdminPublisher.instance().publish(PubTopicEnum.USER, PubLevelEnum.INFO,
                Messages.getSystemMessage(this.getClass(),
                        "cliapp-logout-success", userId,
                        SpXmlRpcServlet.getClientIpAddress()));
        return Boolean.TRUE;
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

        final ConfigManager cm = ConfigManager.instance();
        final String clientIpAddress = SpXmlRpcServlet.getClientIpAddress();

        final String ipAllowed = StringUtils.defaultString(
                cm.getConfigValue(Key.CLIAPP_IP_ADDRESSES_ALLOWED));

        try {

            if (!SpXmlRpcServlet.isSslConnection()) {

                dto.setStatus(ClientAppConnectDto.Status.ERROR_FATAL);
                dto.setStatusMessage("Secure connection required.");

            } else if (!isApiKeyValid(API_CLIENT_ID, apiKey)) {

                dto.setStatus(ClientAppConnectDto.Status.ERROR_FATAL);
                dto.setStatusMessage("Invalid API Key.");

            } else if (!InetUtils.isIpAddrInCidrRanges(ipAllowed,
                    clientIpAddress)) {

                if (cm.isConfigValue(
                        Key.CLIAPP_AUTH_IP_ADDRESSES_DENIED_ENABLE)) {

                    if (StringUtils.isBlank(userPassword)
                            || !isUserPasswordValid(userId, userPassword)) {

                        if (StringUtils.isNotBlank(userPassword)) {
                            publishLoginFailure(userId, clientIpAddress);
                        }

                        dto.setStatus(ClientAppConnectDto.Status.ERROR_AUTH);
                        dto.setStatusMessage("Authentication needed");

                    } else {
                        onUserLoginGranted(dto, userId,
                                createUserAuthToken(clientIpAddress, userId),
                                clientIpAddress);
                        publishLoginSuccess(userId, clientIpAddress);
                    }

                } else {
                    dto.setStatus(ClientAppConnectDto.Status.ERROR_FATAL);
                    dto.setStatusMessage("Access denied.");
                }

            } else {

                final UserAuthToken authToken =
                        getUserAuthToken(clientIpAddress, userId, userPassword,
                                userToken, adminPassKey);

                if (authToken == null) {

                    dto.setStatus(ClientAppConnectDto.Status.ERROR_AUTH);
                    dto.setStatusMessage("Authentication needed");

                    publishLoginFailure(userId, clientIpAddress);

                } else {
                    onUserLoginGranted(dto, userId, authToken, clientIpAddress);
                    publishLoginSuccess(userId, clientIpAddress);
                }
            }

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            dto.setStatus(ClientAppConnectDto.Status.ERROR_FATAL);
            dto.setStatusMessage(e.getMessage());
        }

        if (dto.getStatus() == ClientAppConnectDto.Status.ERROR_FATAL) {
            publishError(userId, clientIpAddress, dto.getStatusMessage());
        }

        try {
            return stringify(dto);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            return "create JSON parse exception in client";
        }
    }

    /**
     * Acknowledges a valid login and fills the {@link ClientAppConnectDto}
     * accordingly.
     *
     * @param dto
     *            The {@link ClientAppConnectDto} to fill.
     * @param userId
     *            The unique user id.
     * @param authToken
     *            The authentication token (can be {@code null}).
     * @param clientIpAddress
     *            Client IP address of client.
     * @throws Exception
     *             When URL exceptions.
     */
    private static void onUserLoginGranted(final ClientAppConnectDto dto,
            final String userId, final UserAuthToken authToken,
            final String clientIpAddress) throws Exception {

        final ConfigManager cm = ConfigManager.instance();

        final URL urlTemplate = getUserWebAppURLTemplate(userId);

        if (authToken != null) {
            dto.setUserAuthToken(authToken.getToken());
        }

        dto.setStatus(ClientAppConnectDto.Status.OK);

        dto.setServerTime(System.currentTimeMillis());
        dto.setWebAppPath(urlTemplate.getPath());
        dto.setWebAppQuery(urlTemplate.getQuery());

        dto.setWebAppQueryPrintIn(
                cm.getConfigValue(Key.CLIAPP_PRINT_IN_URL_QUERY));

        dto.setPrintInActionButton(
                cm.getConfigValue(Key.CLIAPP_PRINT_IN_DIALOG_BUTTON_OPEN));

        final CometdConnectDto cometdConnect = new CometdConnectDto();

        cometdConnect.setAuthToken(CometdClientMixin.SHARED_USER_TOKEN);
        cometdConnect
                .setMaxNetworkDelay(AbstractEventService.getMaxNetworkDelay());

        /*
         * Note: public/subscribe channels are inverse for client/server.
         */
        cometdConnect.setChannelPublish(UserEventService.CHANNEL_SUBSCRIPTION);
        cometdConnect.setChannelSubscribe(UserEventService.CHANNEL_PUBLISH);

        cometdConnect.setUrlPath(getCometdUrlPath());

        dto.setCometd(cometdConnect);

        /*
         * Since this may be a first time login, create SafePages home.
         */
        ServiceContext.getServiceFactory().getUserService()
                .lazyUserHomeDir(userId);
    }

    /**
     * Publishes login success.
     *
     * @param userId
     *            The unique user id.
     * @param clientIpAddress
     *            Client IP address of client.
     */
    private void publishLoginSuccess(final String userId,
            final String clientIpAddress) {
        AdminPublisher.instance().publish(PubTopicEnum.USER, PubLevelEnum.INFO,
                Messages.getSystemMessage(this.getClass(),
                        "cliapp-login-success", userId, clientIpAddress));
    }

    /**
     * Publishes login failure.
     *
     * @param userId
     *            The unique user id.
     * @param clientIpAddress
     *            Client IP address of client.
     */
    private void publishLoginFailure(final String userId,
            final String clientIpAddress) {
        AdminPublisher.instance().publish(PubTopicEnum.USER, PubLevelEnum.WARN,
                Messages.getSystemMessage(this.getClass(),
                        "cliapp-login-failure", userId, clientIpAddress));
    }

    /**
     * Publishes an error.
     *
     * @param userId
     *            The unique user id.
     * @param clientIpAddress
     *            Client IP address of client.
     * @param msg
     *            The error message.
     */
    private void publishError(final String userId, final String clientIpAddress,
            final String msg) {
        AdminPublisher.instance().publish(PubTopicEnum.USER, PubLevelEnum.ERROR,
                Messages.getSystemMessage(this.getClass(), "cliapp-login-error",
                        userId, clientIpAddress, msg));
    }

}
