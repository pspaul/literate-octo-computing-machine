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
package org.savapage.ext.oauth;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.StringUtils;
import org.savapage.ext.ServerPluginException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.scribejava.apis.GoogleApi20;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class GoogleOAuthPlugin implements OAuthClientPlugin {

    /**
     * The {@link Logger}.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GoogleOAuthPlugin.class);

    /**
     * Property key prefix.
     */
    private static final String PROP_KEY_PFX = "google.";

    /**
     * OAuth property key prefix.
     */
    private static final String PROP_KEY_OAUTH_PFX = PROP_KEY_PFX + "oauth.";

    /**
     * .
     */
    private static final String PROP_KEY_OAUTH_CLIENT_ID =
            PROP_KEY_OAUTH_PFX + "client-id";

    /**
     *
     */
    private static final String PROP_KEY_OAUTH_CLIENT_SECRET =
            PROP_KEY_OAUTH_PFX + "client-secret";

    /**
     *
     */
    private static final String PROP_KEY_OAUTH_CALLBACK_URL =
            PROP_KEY_OAUTH_PFX + "callback-url";

    /**
     * Hosted domain.
     */
    private static final String PROP_KEY_OAUTH_PARM_HD =
            PROP_KEY_OAUTH_PFX + "parm.hd";

    /**
     * OAuth scope.
     */
    private static final String OAUTH_SCOPE = "email";

    /**
     *
     */
    private static final String CALLBACK_URL_PARM_CODE = "code";

    /**
     *
     */
    private static final String PROTECTED_RESOURCE_URL =
            "https://www.googleapis.com/plus/v1/people/me";

    private static final String NETWORK_NAME = "G+";

    // ------------------------------------------------------------------
    // https://developers.google.com/identity/protocols/OpenIDConnect
    // ------------------------------------------------------------------
    private static final String OAUTH_ACCESS_TYPE_PARM = "access_type";
    private static final String OAUTH_ACCESS_TYPE_VAL_ONLINE = "online";

    private static final String OAUTH_PROMPT_PARM = "prompt";
    private static final String OAUTH_PROMPT_VAL_VOID = "";

    private static final String OAUTH_HD_PARM = "hd";

    /**
     *
     */
    private String id;

    /**
     *
     */
    private String name;

    /**
     * The singleton {@link OAuth20Service}.
     */
    private OAuth20Service oauthService;

    /**
     * URL of OAuth provider where users authorize SavaPage to do OAuth calls.
     */
    private URL authorizationUrl;

    /**
     * URL the OAuth provider should redirect after authorization.
     */
    private URL callbackUrl;

    /**
     * Formatted as "example.com". When {@code null} all authenticated Google
     * accounts are welcome.
     */
    private String hostedDomain;

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public OAuthProviderEnum getProvider() {
        return OAuthProviderEnum.GOOGLE;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onStop() {
    }

    @Override
    public void onInit(final String pluginId, final String pluginName,
            final boolean live, final boolean online, final Properties props)
            throws ServerPluginException {

        this.id = pluginId;
        this.name = pluginName;

        final String secretState = String.format("%s%d",
                this.getClass().getSimpleName(), new Random().nextInt(999_999));

        this.oauthService =
                new ServiceBuilder()
                        .apiKey(props.getProperty(PROP_KEY_OAUTH_CLIENT_ID))
                        .apiSecret(
                                props.getProperty(PROP_KEY_OAUTH_CLIENT_SECRET))
                        .scope(OAUTH_SCOPE)
                        //
                        .state(secretState)
                        .callback(
                                props.getProperty(PROP_KEY_OAUTH_CALLBACK_URL))
                        .build(GoogleApi20.instance());

        /*
         * Prepare the authorization URL.
         */
        final Map<String, String> additionalParams = new HashMap<>();

        additionalParams.put(OAUTH_ACCESS_TYPE_PARM,
                OAUTH_ACCESS_TYPE_VAL_ONLINE);
        additionalParams.put(OAUTH_PROMPT_PARM, OAUTH_PROMPT_VAL_VOID);

        hostedDomain = props.getProperty(PROP_KEY_OAUTH_PARM_HD);
        if (StringUtils.isNotBlank(hostedDomain)) {
            hostedDomain = hostedDomain.trim();
            additionalParams.put(OAUTH_HD_PARM, hostedDomain);
        }

        try {

            this.callbackUrl =
                    new URL(props.getProperty(PROP_KEY_OAUTH_CALLBACK_URL));

            this.authorizationUrl = new URL(
                    this.oauthService.getAuthorizationUrl(additionalParams));

        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public URL getAuthorizationUrl() {
        return this.authorizationUrl;
    }

    @Override
    public URL getCallbackUrl() {
        return this.callbackUrl;
    }

    @Override
    public OAuthUserInfo onCallBack(final Map<String, String> parameterMap)
            throws IOException, OAuthPluginException {

        if (!parameterMap.containsKey(CALLBACK_URL_PARM_CODE)) {
            return null;
        }

        final String code = parameterMap.get(CALLBACK_URL_PARM_CODE);

        try {
            //
            // See: https://github.com/scribejava/scribejava
            // com.github.scribejava.apis.examples.Google20Example
            //

            /*
             * Get the Access Token. Since this is just a quick peek at the
             * email address, a refresh token is not relevant.
             */
            final OAuth2AccessToken accessToken =
                    oauthService.getAccessToken(code);

            /*
             * Ask for a protected resource.
             */
            final OAuthRequest request =
                    new OAuthRequest(Verb.GET, PROTECTED_RESOURCE_URL);

            oauthService.signRequest(accessToken, request);

            final Response response = oauthService.execute(request);

            if (!response.isSuccessful()) {
                LOGGER.error(String.format("Error %d", response.getCode()));
                return null;
            }
            //
            final String json = response.getBody();
            LOGGER.trace(json);

            final GoogleOAuthPayload payload = GoogleOAuthPayload.create(json);
            final OAuthUserInfo userInfo = new OAuthUserInfo();
            userInfo.setEmail(payload.getFirstEmail());

            if (userInfo.getEmail() == null) {
                LOGGER.error(String.format("No email found:\n%s", json));
                return null;
            }
            /*
             * Just to be sure...
             */
            if (hostedDomain != null
                    && !userInfo.getEmail().endsWith(hostedDomain)) {
                LOGGER.error(
                        String.format("User [%s] is not a member [%s] domain.",
                                userInfo.getEmail(), hostedDomain));
                return null;
            }

            return userInfo;

        } catch (InterruptedException e) {
            // no code intended
        } catch (Exception e) {
            /*
             * getAccessToken() can throw an exception caused by:
             * com.github.scribejava.core.model.OAuth2AccessTokenErrorResponse:
             * { "error": "invalid_grant", "error_description":
             * "Code was already redeemed." }
             *
             * For now, just logging.
             */
            LOGGER.warn(e.getMessage());
        }
        return null;
    }

    /**
     * Test.
     *
     * See: https://github.com/scribejava/scribejava
     *
     * com.github.scribejava.apis.examples.Google20Example
     *
     * @param args
     * @throws IOException
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public static void main(String[] args)
            throws IOException, InterruptedException, ExecutionException {

        final String clientId = args[0];
        final String clientSecret = args[1];
        final String secretState =
                String.format("%s%d", GoogleOAuthPlugin.class.getSimpleName(),
                        new Random().nextInt(999_999));

        final OAuth20Service service = new ServiceBuilder().apiKey(clientId)
                .apiSecret(clientSecret).scope(OAUTH_SCOPE).state(secretState)
                .callback(args[2]).build(GoogleApi20.instance());

        final Scanner in = new Scanner(System.in, "UTF-8");

        System.out.println("=== " + NETWORK_NAME + "'s OAuth Workflow ===");
        System.out.println();

        /*
         * Obtain the Authorization URL.
         */
        System.out.println("Fetching the Authorization URL...");

        final Map<String, String> additionalParams = new HashMap<>();

        /*
         * pass access_type=offline to get refresh token
         */
        additionalParams.put("access_type", "offline");

        /*
         * Force to re-get refresh token (if users are asked not the first
         * time).
         */
        additionalParams.put("prompt", "consent");

        final String authorizationUrl =
                service.getAuthorizationUrl(additionalParams);

        System.out.println("Got the Authorization URL!");
        System.out.println("Now go and authorize ScribeJava here:");
        System.out.println(authorizationUrl);
        System.out.println("And paste the authorization code here");
        System.out.print(">>");
        final String code = in.nextLine();
        System.out.println();

        System.out.println("And paste the state from server here. "
                + "We have set 'secretState'='" + secretState + "'.");
        System.out.print(">>");

        final String value = in.nextLine();

        if (secretState.equals(value)) {
            System.out.println("State value does match!");
        } else {
            System.out.println("Ooops, state value does not match!");
            System.out.println("Expected = " + secretState);
            System.out.println("Got      = " + value);
            System.out.println();
        }

        /*
         * Trade the Request Token and Verifier for the Access Token.
         */
        System.out.println("Trading the Request Token for an Access Token...");
        OAuth2AccessToken accessToken = service.getAccessToken(code);

        System.out.println("Got the Access Token!");
        System.out.println("(if your curious it looks like this: " + accessToken
                + ", 'rawResponse'='" + accessToken.getRawResponse() + "')");

        System.out.println("Refreshing the Access Token...");

        accessToken = service.refreshAccessToken(accessToken.getRefreshToken());

        System.out.println("Refreshed the Access Token!");
        System.out.println("(if your curious it looks like this: " + accessToken
                + ", 'rawResponse'='" + accessToken.getRawResponse() + "')");
        System.out.println();

        /*
         * Now let's go and ask for a protected resource!
         */
        System.out.println("Now we're going to access a protected resource...");

        while (true) {
            System.out.println(
                    "Paste fieldnames to fetch (leave empty to get profile, "
                            + "'exit' to stop example)");
            System.out.print(">>");
            final String query = in.nextLine();
            System.out.println();

            final String requestUrl;
            if ("exit".equals(query)) {
                break;
            } else if (query == null || query.isEmpty()) {
                requestUrl = PROTECTED_RESOURCE_URL;
            } else {
                requestUrl = PROTECTED_RESOURCE_URL + "?fields=" + query;
            }

            final OAuthRequest request = new OAuthRequest(Verb.GET, requestUrl);
            service.signRequest(accessToken, request);
            final Response response = service.execute(request);
            System.out.println();
            System.out.println(response.getCode());
            System.out.println(response.getBody());

            System.out.println();
        }
    }

}
