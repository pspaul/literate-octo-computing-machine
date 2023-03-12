/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Authors: Rijk Ravestein.
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
package org.savapage.server.api.request;

import java.io.IOException;
import java.util.Date;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebRequest;
import org.savapage.core.SpException;
import org.savapage.core.auth.YubiKeyOTP;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.CometdClientMixin;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.community.MemberCard;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.config.SetupNeededEnum;
import org.savapage.core.config.SystemStatusEnum;
import org.savapage.core.config.WebAppTypeEnum;
import org.savapage.core.crypto.CryptoUser;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dao.UserNumberDao;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.ACLPermissionEnum;
import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.dao.enums.UserAttrEnum;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.dto.UserIdDto;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.i18n.PhraseEnum;
import org.savapage.core.i18n.SystemModeEnum;
import org.savapage.core.jpa.Device;
import org.savapage.core.jpa.Entity;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserNumber;
import org.savapage.core.msg.UserMsgIndicator;
import org.savapage.core.rfid.RfidNumberFormat;
import org.savapage.core.services.DeviceService.DeviceAttrLookup;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.UserAuth;
import org.savapage.core.services.helpers.UserAuthModeEnum;
import org.savapage.core.totp.TOTPHelper;
import org.savapage.core.users.IExternalUserAuthenticator;
import org.savapage.core.users.IUserSource;
import org.savapage.core.users.InternalUserAuthenticator;
import org.savapage.core.users.conf.UserAliasList;
import org.savapage.core.util.AppLogHelper;
import org.savapage.core.util.DateUtil;
import org.savapage.core.util.EmailValidator;
import org.savapage.core.util.InetUtils;
import org.savapage.core.util.Messages;
import org.savapage.server.WebApp;
import org.savapage.server.api.UserAgentHelper;
import org.savapage.server.auth.ClientAppUserAuthManager;
import org.savapage.server.auth.UserAuthToken;
import org.savapage.server.auth.WebAppUserAuthManager;
import org.savapage.server.cometd.UserEventService;
import org.savapage.server.session.SpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.yubico.client.v2.exceptions.YubicoValidationFailure;
import com.yubico.client.v2.exceptions.YubicoVerificationException;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqLogin extends ApiRequestMixin {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ReqLogin.class);

    /**
     * <b>Static</b> lock object for synchronization of lazy user creation.
     */
    private static final Object LAZY_CREATE_USER_LOCK = new Object();

    /**
     * .
     */
    @JsonInclude(Include.NON_NULL)
    private static class DtoReq extends AbstractDto {

        private WebAppTypeEnum webAppType;

        private String authMode;
        private String authId;
        private String authPw;
        private String authToken;
        private String assocCardNumber;

        public WebAppTypeEnum getWebAppType() {
            return webAppType;
        }

        @SuppressWarnings("unused")
        public void setWebAppType(WebAppTypeEnum webAppType) {
            this.webAppType = webAppType;
        }

        public String getAuthMode() {
            return authMode;
        }

        @SuppressWarnings("unused")
        public void setAuthMode(String authMode) {
            this.authMode = authMode;
        }

        public String getAuthId() {
            return authId;
        }

        public void setAuthId(String authId) {
            this.authId = authId;
        }

        public String getAuthPw() {
            return authPw;
        }

        @SuppressWarnings("unused")
        public void setAuthPw(String authPw) {
            this.authPw = authPw;
        }

        public String getAuthToken() {
            return authToken;
        }

        @SuppressWarnings("unused")
        public void setAuthToken(String authToken) {
            this.authToken = authToken;
        }

        public String getAssocCardNumber() {
            return assocCardNumber;
        }

        @SuppressWarnings("unused")
        public void setAssocCardNumber(String assocCardNumber) {
            this.assocCardNumber = assocCardNumber;
        }
    }

    /**
     * .
     */
    @SuppressWarnings("unused")
    private static class DtoRsp extends AbstractDto {

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final DtoReq dtoReq =
                DtoReq.create(DtoReq.class, this.getParmValueDto());

        final UserAuthModeEnum authMode;

        if (dtoReq.getAuthMode() == null) {
            authMode = null;
        } else {
            authMode = UserAuthModeEnum.fromDbValue(dtoReq.getAuthMode());
        }

        MemberCard.instance().recalcStatus(new Date());

        // Sanitize.
        if (authMode == UserAuthModeEnum.EMAIL) {
            dtoReq.setAuthId(dtoReq.getAuthId().trim());
        }

        reqLogin(authMode, dtoReq.getAuthId(), dtoReq.getAuthPw(),
                dtoReq.getAuthToken(), dtoReq.getAssocCardNumber(),
                dtoReq.getWebAppType());
    }

    /**
     * Handles the login request for both the User and Admin WebApp.
     * <p>
     * NOTE: If auth mode NAME and authPw is {@code null} the user is validated
     * against the authToken.
     * </p>
     * <p>
     * When an assocCardNumber (not null) is passed, the User is authenticated
     * according to the authMode. A login is NOT granted, just the card is
     * associated.
     * </p>
     * Invariants:
     * <ul>
     * <li>The application SHOULD be initialized.</li>
     * <li>If set-up is not completed then the only login possible is as
     * internal admin in the Admin WebApp.</li>
     * <li>If Application is NOT ready-to-use the only login possible is as
     * admin in the admin application.</li>
     * <li>See
     * {@link #reqLoginNew(org.savapage.core.services.helpers.UserAuthModeEnum, String, String, String, boolean)}
     * , {@link #reqLoginAuthTokenCliApp(Map, String, String, boolean)(} and
     * {@link #reqLoginAuthTokenWebApp(String, String, boolean)}.</li>
     * </ul>
     *
     * @param authMode
     *            The authentication mode. If {@code null} then TOTP response.
     * @param authId
     *            Offered use name (handled as user alias), ID Number, Card
     *            Number or TOTP code.
     * @param authPw
     *            The password, PIN or TOTP recovery code. When {@code null} AND
     *            auth mode NAME, the authToken is used to validate.
     * @param authToken
     *            The authentication token (interpreted in authMode context).
     * @param assocCardNumber
     *            The card number to associate with this user account. When
     *            {@code null} NO card will be associated.
     * @param webAppType
     *            The {@link WebAppTypeEnum}.
     * @throws IOException
     *             When IO error.
     */
    private void reqLogin(final UserAuthModeEnum authMode, final String authId,
            final String authPw, final String authToken,
            final String assocCardNumber, final WebAppTypeEnum webAppType)
            throws IOException {

        final UserAgentHelper userAgentHelper = this.createUserAgentHelper();

        /*
         * Browser diagnostics.
         */
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(
                    "HTML Browser AuthID [{}] Mobile [{}] "
                            + "Mobile Safari [{}] macOS Safari [{}]"
                            + " UserAgent [{}]",
                    authId, userAgentHelper.isMobileBrowser(),
                    userAgentHelper.isSafariBrowserMobile(),
                    userAgentHelper.isSafariBrowserMacOsX(),
                    userAgentHelper.getUserAgentHeader());
        }

        final ConfigManager cm = ConfigManager.instance();
        final SpSession session = SpSession.get();

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Session [{}] WebAppCount [{}]", session.getId(),
                    session.getAuthWebAppCount());
        }

        /*
         * INVARIANT: Only one (1) authenticated session allowed.
         */
        if (session.getAuthWebAppCount() != 0) {
            this.setApiResultText(ApiResultCodeEnum.RELOAD,
                    PhraseEnum.ANOTHER_BROWSER_SESSION_ACTIVE
                            .uiText(getLocale()));
            return;
        }

        /*
         * INVARIANT: The application SHOULD be initialized.
         */
        if (!cm.isInitialized()) {
            this.setApiResult(ApiResultCodeEnum.ERROR,
                    "msg-login-not-possible");
            this.onSessionDenied(session);
            return;
        }

        /*
         * INVARIANT: If setup is NOT completed the only login possible is auth
         * mode NAME as INTERNAL admin in the admin application.
         */
        if (!cm.isSetupCompleted()) {
            if (webAppType != WebAppTypeEnum.ADMIN
                    || authMode != UserAuthModeEnum.NAME) {
                this.setApiResult(ApiResultCodeEnum.ERROR,
                        "msg-login-install-mode");
                this.onSessionDenied(session);
                return;
            }
            if (!ConfigManager.isInternalAdmin(authId)) {
                this.setApiResult(ApiResultCodeEnum.ERROR,
                        "msg-login-as-internal-admin");
                this.onSessionDenied(session);
                return;
            }
        }

        /*
         * INVARIANT: If application must be setup the only login possible is as
         * "admin" user in the Admin Web App.
         */
        if (cm.getSystemStatus() == SystemStatusEnum.SETUP
                && webAppType != WebAppTypeEnum.ADMIN) {
            this.setApiResult(ApiResultCodeEnum.ERROR, "msg-login-app-config");
            this.onSessionDenied(session);
            return;
        }

        final boolean isAuthTokenLoginEnabled =
                ApiRequestHelper.isAuthTokenLoginEnabled();

        final UserDao userDao = ServiceContext.getDaoContext().getUserDao();

        if (authMode == null) {

            this.onTOTPResponse(session, webAppType, authId, authPw);

        } else if (authMode == UserAuthModeEnum.OAUTH) {

            /*
             * If user was AOuth Sign-In is enabled.
             *
             * TODO: check if OAuth is enabled.
             */

            /*
             * INVARIANT: User must exist in database.
             */

            // We need the JPA attached User.
            final User userDb;

            if (session.getUserId() == null) {
                /*
                 * User logged out?
                 */
                userDb = null;
            } else {
                /*
                 * User was authenticated by OAuth provider.
                 */
                userDb = userDao.findActiveUserByUserId(session.getUserId());
            }

            if (userDb == null) {

                this.onLoginFailed(null);

            } else if (userDb != null) {

                final UserAuthToken token;

                if (ApiRequestHelper.isAuthTokenLoginEnabled()) {
                    token = reqLoginLazyCreateAuthToken(userDb.getUserId(),
                            webAppType);
                } else {
                    token = null;
                }

                this.onUserLoginGranted(getUserData(), session, webAppType,
                        authMode, session.getUserId(), userDb, token);

                this.setApiResultOk();
            }

        } else {
            /*
             * If user authentication token (browser local storage) is disabled
             * or user was authenticated by OneTimeAuthToken, we fall back to
             * the user in the active session.
             */
            if ((!isAuthTokenLoginEnabled || session.isOneTimeAuthToken())
                    && session.getUserId() != null) {

                /*
                 * INVARIANT: User must exist in database.
                 */

                // We need the JPA attached User.
                final User userDb =
                        userDao.findActiveUserByUserId(session.getUserId());

                if (userDb == null) {
                    this.onLoginFailed(null);
                } else {
                    this.onUserLoginGranted(getUserData(), session, webAppType,
                            authMode, session.getUserId(), userDb, null);
                    this.setApiResultOk();
                }

            } else {

                final boolean isCliAppAuthApplied;

                if (isAuthTokenLoginEnabled && authMode == UserAuthModeEnum.NAME
                        && StringUtils.isBlank(authPw)) {
                    isCliAppAuthApplied =
                            this.reqLoginAuthTokenCliApp(session, getUserData(),
                                    authId, this.getClientIP(), webAppType);
                } else {
                    isCliAppAuthApplied = false;
                }

                if (!isCliAppAuthApplied) {

                    if (isAuthTokenLoginEnabled
                            && authMode == UserAuthModeEnum.NAME
                            && StringUtils.isBlank(authPw)
                            && StringUtils.isNotBlank(authToken)) {

                        reqLoginAuthTokenWebApp(session, authId, authToken,
                                webAppType);

                    } else {
                        reqLoginNew(session, authMode, authId, authPw,
                                assocCardNumber, webAppType);
                    }
                }
            }
        }

        /*
         * INVARIANT: If system maintenance the only login possible is as admin.
         */
        if (ConfigManager.getSystemMode() == SystemModeEnum.MAINTENANCE
                && session.getUserId() != null && !session.isAdmin()) {
            this.setApiResult(ApiResultCodeEnum.ERROR,
                    "msg-login-not-possible");
            this.onSessionDenied(session);
            return;
        }

        /*
         * INVARIANT: Authenticated user must have privilege.
         */
        if (isApiResultOk()
                && !this.validateWebAppUserPrivilege(authMode, webAppType)) {
            this.onSessionDenied(session);
            return;
        }

        getUserData().put("sessionid", session.getId());

        //
        final EnumSet<ApiResultCodeEnum> validResultCodes;

        if (webAppType == WebAppTypeEnum.ADMIN) {
            validResultCodes = EnumSet.of(ApiResultCodeEnum.OK,
                    ApiResultCodeEnum.INFO, ApiResultCodeEnum.WARN);
        } else {
            validResultCodes = EnumSet.of(ApiResultCodeEnum.OK);
        }

        if (isApiResultCode(validResultCodes)) {

            this.setSessionTimeoutSeconds(webAppType);

            session.setTOTPRequest(null);
            session.setWebAppType(webAppType);
            session.incrementAuthWebAppCount();

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(
                        "Increment user [{}] in session [{}] of {} WebApp"
                                + ": {}",
                        session.getUserId(), session.getId(),
                        webAppType.toString(), session.getAuthWebAppCount());
            }
        } else {
            this.onSessionDenied(session);
        }
    }

    /**
     * Validates user privilege.
     *
     * @param authMode
     *            The authentication mode. If {@code null} then TOTP response.
     * @param webAppType
     *            The {@link WebAppTypeEnum}. *
     * @return {@code false} if non-privileged.
     */
    private boolean validateWebAppUserPrivilege(final UserAuthModeEnum authMode,
            final WebAppTypeEnum webAppType) {
        /*
         * INVARIANT: Authenticated user must have Financial Editor rights for
         * Payment Web App.
         */
        if (webAppType == WebAppTypeEnum.PAYMENT) {

            final UserIdDto userIdDto = SpSession.get().getUserIdDto();

            if (userIdDto != null) {

                final Integer financialPriv = ACCESSCONTROL_SERVICE
                        .getPrivileges(userIdDto, ACLOidEnum.U_FINANCIAL);

                if (financialPriv != null
                        && !ACLPermissionEnum.EDITOR.isPresent(financialPriv)) {

                    final Locale localePriv = ConfigManager.getDefaultLocale();

                    final String msg = this.localize(localePriv,
                            "msg-login-non-privileged", webAppType.getUiText(),
                            UserAuth.getUiText(authMode), userIdDto.getUserId(),
                            String.format("%s/%s",
                                    ACLOidEnum.U_FINANCIAL.uiText(localePriv),
                                    ACLPermissionEnum.EDITOR
                                            .uiText(localePriv)));

                    LOGGER.warn(msg);
                    AdminPublisher.instance().publish(PubTopicEnum.USER,
                            PubLevelEnum.WARN, msg);
                    this.setApiResult(ApiResultCodeEnum.ERROR,
                            "msg-login-missing-privilege");

                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Handles a new login request for both the User and Admin WebApp.
     * <p>
     * When an assocCardNumber (not null) is passed, the User is authenticated
     * according to the authMode. A login is NOT granted, just the card is
     * associated.
     * </p>
     * <ul>
     * <li>Internal admin is NOT allowed to login to the User WebApp.</li>
     * <li>User MUST exist to login to Admin WebApp (no lazy user insert allowed
     * in this case).</li>
     * <li>User MUST exist to login when NO external user source (no lazy user
     * insert allowed in this case).</li>
     * <li>User MUST exist to login when lazy user insert is disabled.</li>
     * <li>User MUST have admin rights to login to Admin WebApp.</li>
     * <li>User MUST be a Person to login.</li>
     * <li>User MUST be active (enabled) at moment of login.</li>
     * </ul>
     *
     * @param session
     *            {@link SpSession}.
     * @param authMode
     *            The authentication mode.
     * @param authId
     *            Offered user name (handled as user alias), ID Number or Card
     *            Number.
     * @param authPw
     *            The password to be validated against the user source or user
     *            PIN.
     * @param assocCardNumber
     *            The card number to associate with this user account. When
     *            {@code null} NO card will be associated.
     * @param webAppType
     *            The {@link WebAppTypeEnum}.
     * @throws IOException
     *             When IO error.
     */
    private void reqLoginNew(final SpSession session,
            final UserAuthModeEnum authMode, final String authId,
            final String authPw, final String assocCardNumber,
            final WebAppTypeEnum webAppType) throws IOException {

        // INVARIANT: Password can NOT be empty in Name authentication.
        if ((authMode == UserAuthModeEnum.NAME
                || authMode == UserAuthModeEnum.EMAIL)
                && StringUtils.isBlank(authPw)) {
            this.onLoginFailed(null);
            return;
        }

        // INVARIANT: valid email address.
        if (authMode == UserAuthModeEnum.EMAIL
                && !EmailValidator.validate(authId)) {
            this.onLoginFailed(null);
            return;
        }

        final String remoteAddr = this.getClientIP();
        final boolean isPublicAddress = InetUtils.isPublicAddress(remoteAddr);

        final Device terminal = ApiRequestHelper.getHostTerminal(remoteAddr);

        final UserAuth theUserAuth =
                new UserAuth(terminal, null, webAppType, isPublicAddress);

        if (authMode != UserAuthModeEnum.OAUTH
                && authMode != UserAuthModeEnum.YUBIKEY) { // TEST TEST
            if (!theUserAuth.isAuthModeAllowed(authMode)) {
                this.setApiResult(ApiResultCodeEnum.ERROR,
                        "msg-auth-mode-not-available", authMode.toString());
                return;
            }
        }

        final UserDao userDao = ServiceContext.getDaoContext().getUserDao();
        final ConfigManager cm = ConfigManager.instance();

        final IExternalUserAuthenticator userAuthenticator =
                cm.getUserAuthenticator();

        /*
         * Initialize pessimistic.
         */
        session.setUser(null);

        /*
         * The facts.
         */
        final boolean allowInternalUsersOnly = (userAuthenticator == null);

        final boolean isInternalAdmin = authMode == UserAuthModeEnum.NAME
                && ConfigManager.isInternalAdmin(authId);

        final boolean isLazyUserInsert = webAppType == WebAppTypeEnum.USER
                && !allowInternalUsersOnly && cm.isUserInsertLazyLogin();

        /*
         * To find out.
         */
        boolean isAuthenticated = false;
        boolean isInternalUser = false;
        String uid = null;
        User userDb = null;

        /*
         * RFID format.
         */
        final RfidNumberFormat rfidNumberFormat;

        if (authMode == UserAuthModeEnum.CARD_LOCAL
                || assocCardNumber != null) {
            if (terminal == null) {
                rfidNumberFormat = new RfidNumberFormat();
            } else {
                final DeviceAttrLookup lookup = new DeviceAttrLookup(terminal);
                rfidNumberFormat =
                        DEVICE_SERVICE.createRfidNumberFormat(terminal, lookup);
            }
        } else {
            rfidNumberFormat = null;
        }

        /*
         * Internal admin?
         */
        if (isInternalAdmin) {

            /*
             * INVARIANT: internal admin is allowed to login to the Admin WebApp
             * only.
             *
             * INVARIANT: when Internet auth mode is enabled, internal admin is
             * allowed to login to intranet Admin WebApp only.
             */
            if (webAppType != WebAppTypeEnum.ADMIN
                    || (isPublicAddress && cm.isConfigValue(
                            Key.WEBAPP_INTERNET_ADMIN_AUTH_MODE_ENABLE))) {
                this.onLoginFailed("msg-login-denied", webAppType.getUiText(),
                        UserAuth.getUiText(authMode), authId, remoteAddr);
                return;
            }

            /*
             * INVARIANT: internal admin password must be correct.
             */
            final User userAuth = ConfigManager.instance()
                    .isInternalAdminValid(authId, authPw);

            isAuthenticated = (userAuth != null);

            if (!isAuthenticated) {
                this.onLoginFailed("msg-login-invalid-password",
                        webAppType.getUiText(), authId, remoteAddr);
                return;
            }

            uid = authId;
            userDb = userAuth;

        } else {

            if (authMode == UserAuthModeEnum.YUBIKEY) {

                if (authId == null || !YubiKeyOTP.isValidOTPFormat(authId)) {
                    this.onLoginFailed(null);
                    return;
                }

                final YubiKeyOTP otp = new YubiKeyOTP(authId);

                try {
                    userDb = reqLoginYubico(otp, webAppType);
                } catch (YubicoVerificationException e) {
                    this.onLoginFailed("msg-login-yubikey-connection-error",
                            webAppType.getUiText(), otp.getPublicId(),
                            e.getMessage());
                } catch (YubicoValidationFailure e) {
                    throw new SpException(e.getMessage());
                }

                if (userDb == null) {
                    this.onLoginFailed(null);
                    return;
                }
                uid = userDb.getUserId();

            } else if (authMode == UserAuthModeEnum.NAME) {

                final String uidRaw =
                        UserAliasList.instance().getUserName(authId);
                if (allowInternalUsersOnly) {
                    uid = uidRaw;
                } else {
                    uid = userAuthenticator.asDbUserId(uidRaw);
                }
                // Read "real" user from database.
                userDb = userDao.findActiveUserByUserId(uid);
                if (userDb == null && !uid.equals(uidRaw)) {
                    // Is internal user?
                    userDb = userDao.findActiveUserByUserId(uidRaw);
                    if (userDb.getInternal().booleanValue()) {
                        uid = uidRaw;
                    } else {
                        userDb = null;
                    }
                }

            } else if (authMode == UserAuthModeEnum.EMAIL) {

                userDb = USER_SERVICE.findActiveUserByEmail(authId);
                /*
                 * INVARIANT: User MUST be present in database.
                 */
                if (userDb == null) {
                    this.onLoginFailed("msg-login-failed",
                            webAppType.getUiText(), authId, remoteAddr);
                    return;
                }
                uid = userDb.getUserId();

            } else if (authMode == UserAuthModeEnum.ID) {

                userDb = USER_SERVICE.findUserByNumber(authId);

                /*
                 * INVARIANT: User MUST be present in database.
                 */
                if (userDb == null) {
                    this.onLoginFailed("msg-login-invalid-number",
                            webAppType.getUiText(), authId, remoteAddr);
                    return;
                }
                uid = userDb.getUserId();

            } else if (authMode == UserAuthModeEnum.CARD_IP
                    || authMode == UserAuthModeEnum.CARD_LOCAL) {

                String normalizedCardNumber = authId;

                if (authMode == UserAuthModeEnum.CARD_LOCAL) {
                    normalizedCardNumber =
                            rfidNumberFormat.getNormalizedNumber(authId);
                }

                userDb = USER_SERVICE
                        .findUserByCardNumber(normalizedCardNumber);

                /*
                 * INVARIANT: User MUST be present.
                 */
                if (userDb == null) {

                    if (ConfigManager
                            .getSystemMode() == SystemModeEnum.MAINTENANCE) {
                        this.setApiResult(ApiResultCodeEnum.ERROR,
                                "msg-login-not-possible");
                    } else {
                        final boolean selfAssoc =
                                webAppType == WebAppTypeEnum.USER
                                        && Boolean.valueOf(theUserAuth
                                                .isAuthCardSelfAssoc());

                        getUserData().put("authCardSelfAssoc", selfAssoc);

                        if (selfAssoc) {
                            this.setApiResult(ApiResultCodeEnum.ERROR,
                                    "msg-login-unregistered-card");
                        } else {
                            this.onLoginFailed("msg-login-card-unknown",
                                    webAppType.getUiText(),
                                    UserAuth.getUiText(authMode),
                                    normalizedCardNumber, remoteAddr);
                        }
                    }
                    return;
                }
                uid = userDb.getUserId();
            }

            /*
             * Check invariants based on user presence in database.
             */
            if (userDb == null) {

                /*
                 * INVARIANT: User MUST exist to login to non-User WebApp (no
                 * lazy user insert allowed in this case)
                 */
                if (webAppType != WebAppTypeEnum.USER) {
                    this.onLoginFailed("msg-login-user-not-present",
                            webAppType.getUiText(),
                            UserAuth.getUiText(authMode), authId, remoteAddr);
                    return;
                }

                /*
                 * INVARIANT: User MUST exist to login when NO external user
                 * source (no lazy user insert allowed in this case).
                 */
                if (allowInternalUsersOnly) {
                    this.onLoginFailed("msg-login-user-not-present",
                            webAppType.getUiText(),
                            UserAuth.getUiText(authMode), authId, remoteAddr);
                    return;
                }

                /*
                 * INVARIANT: User MUST exist to login when lazy user insert is
                 * disabled.
                 */
                if (!isLazyUserInsert) {
                    this.onLoginFailed("msg-login-user-not-present",
                            webAppType.getUiText(),
                            UserAuth.getUiText(authMode), authId, remoteAddr);
                    return;
                }

            } else {

                /*
                 * INVARIANT: User MUST have admin rights to login to Admin
                 * WebApp.
                 */
                if (webAppType == WebAppTypeEnum.ADMIN && !userDb.getAdmin()) {
                    this.onLoginFailed("msg-login-no-admin-rights",
                            webAppType.getUiText(),
                            UserAuth.getUiText(authMode), userDb.getUserId(),
                            remoteAddr);
                    return;
                }

                /*
                 * INVARIANT: User MUST be a Person to login.
                 */
                if (!userDb.getPerson()) {
                    this.onLoginFailed("msg-login-no-person",
                            webAppType.getUiText(),
                            UserAuth.getUiText(authMode), userDb.getUserId(),
                            remoteAddr);
                    return;
                }

                final Date onDate = new Date();

                /*
                 * INVARIANT: User MUST be active (enabled) at moment of login.
                 */
                if (USER_SERVICE.isUserFullyDisabled(userDb, onDate)) {
                    this.onLoginFailed("msg-login-disabled",
                            webAppType.getUiText(),
                            UserAuth.getUiText(authMode), userDb.getUserId(),
                            remoteAddr);
                    return;
                }

                /*
                 * INVARIANT: User Role MUST match Web App Type.
                 */
                final ACLRoleEnum webAppTypeRole;

                switch (webAppType) {
                case POS:
                    webAppTypeRole = ACLRoleEnum.WEB_CASHIER;
                    break;
                case JOBTICKETS:
                    webAppTypeRole = ACLRoleEnum.JOB_TICKET_OPERATOR;
                    break;
                case MAILTICKETS:
                    webAppTypeRole = ACLRoleEnum.MAIL_TICKET_OPERATOR;
                    break;
                case PRINTSITE:
                    webAppTypeRole = ACLRoleEnum.PRINT_SITE_OPERATOR;
                    break;
                default:
                    webAppTypeRole = null;
                    break;
                }

                if (webAppTypeRole != null) {
                    if (!ACCESSCONTROL_SERVICE.hasAccess(userDb,
                            webAppTypeRole)) {
                        this.onLoginFailed("msg-login-no-access-to-role",
                                webAppType.getUiText(),
                                UserAuth.getUiText(authMode),
                                userDb.getUserId(),
                                webAppTypeRole.uiText(getLocale()), remoteAddr);
                        return;
                    }
                }
                /*
                 * INVARIANT: User and MailTickets Web App can NOT coexist for
                 * one (1) user.
                 */
                if (webAppType.isUserTypeOrVariant()) {
                    String roleConflict = null;
                    if (webAppType == WebAppTypeEnum.USER) {
                        if (WebApp.getWebAppMailTicketsSessions(
                                userDb.getUserId()) > 0) {
                            roleConflict = ACLRoleEnum.MAIL_TICKET_OPERATOR
                                    .uiText(getLocale());
                        }
                    } else if (webAppType == WebAppTypeEnum.MAILTICKETS) {
                        if (WebApp.getWebAppUserSessions(
                                userDb.getUserId()) > 0) {
                            roleConflict = NounEnum.USER.uiText(getLocale())
                                    .toLowerCase();
                        }
                    }
                    if (roleConflict != null) {
                        this.setApiResult(ApiResultCodeEnum.WARN,
                                "msg-login-conflict", roleConflict);
                        return;
                    }
                }

                // Identify internal user.
                isInternalUser = userDb.getInternal();
            }

            /*
             * Authenticate
             */
            if (authMode == UserAuthModeEnum.YUBIKEY
                    || authMode == UserAuthModeEnum.OAUTH) {

                // no code intended

            } else if (authMode == UserAuthModeEnum.NAME
                    || authMode == UserAuthModeEnum.EMAIL) {

                if (isInternalUser) {

                    isAuthenticated = InternalUserAuthenticator
                            .authenticate(userDb, authPw);

                    if (!isAuthenticated) {
                        /*
                         * INVARIANT: Password of Internal User must be correct.
                         */
                        this.onLoginFailed("msg-login-invalid-password",
                                webAppType.getUiText(), userDb.getUserId(),
                                remoteAddr);
                        return;
                    }

                    // No lazy insert for internal user.

                } else {

                    final User userAuth;

                    if (allowInternalUsersOnly) {
                        userAuth = null;
                        isAuthenticated = false;
                    } else {
                        userAuth = userAuthenticator.authenticate(uid, authPw);
                        isAuthenticated = (userAuth != null);
                    }

                    if (!isAuthenticated) {
                        /*
                         * INVARIANT: Password of External User must be correct.
                         */
                        this.onLoginFailed("msg-login-invalid-password",
                                webAppType.getUiText(), uid, remoteAddr);
                        return;
                    }

                    /*
                     * Lazy user insert for NAME authentication only.
                     */
                    if (userDb == null && authMode == UserAuthModeEnum.NAME) {

                        boolean lazyInsert = false;

                        final String group =
                                cm.getConfigValue(Key.USER_SOURCE_GROUP).trim();

                        if (group.isEmpty()) {
                            lazyInsert = true;
                        } else {
                            IUserSource userSource = cm.getUserSource();
                            lazyInsert = userSource.isUserInGroup(uid, group);
                        }

                        if (lazyInsert) {
                            userDb = onLazyCreateUser(userDao, userAuth,
                                    webAppType);
                            onUserLazyCreated(webAppType, uid, userDb != null);
                        }
                    }
                }

            } else {

                /*
                 * Check PIN for both ID Number, Local and Network Card.
                 */
                isAuthenticated = (authMode == UserAuthModeEnum.ID
                        && !theUserAuth.isAuthIdPinReq())
                        || (authMode == UserAuthModeEnum.CARD_IP
                                && !theUserAuth.isAuthCardPinReq())
                        || (authMode == UserAuthModeEnum.CARD_LOCAL
                                && !theUserAuth.isAuthCardPinReq());

                if (!isAuthenticated) {

                    if (StringUtils.isBlank(authPw)) {
                        /*
                         * INVARIANT: PIN can NOT be empty.
                         */
                        this.onLoginFailed("msg-login-no-pin-available",
                                webAppType.getUiText(), authId, remoteAddr);
                        return;
                    }

                    final String encryptedPin = USER_SERVICE.findUserAttrValue(
                            userDb.getId(), UserAttrEnum.PIN);
                    String pin = "";
                    if (encryptedPin != null) {
                        pin = CryptoUser.decryptUserAttr(userDb.getId(),
                                encryptedPin);
                    }
                    isAuthenticated = pin.equals(authPw);
                }

                if (!isAuthenticated) {
                    /*
                     * INVARIANT: PIN must be correct.
                     */
                    this.onLoginFailed("msg-login-invalid-pin",
                            webAppType.getUiText(), authId, remoteAddr);
                    return;
                }
            }

            /*
             * Lazy create user home directory
             */
            if (!isInternalAdmin && userDb != null) {

                /*
                 * Ad-hoc user lock
                 */
                userDb = USER_SERVICE.lockUser(userDb.getId());

                try {
                    USER_SERVICE.lazyUserHomeDir(uid);
                } catch (IOException e) {
                    this.setApiResult(ApiResultCodeEnum.ERROR,
                            "msg-user-home-dir-create-error");
                    return;
                }
            }
        }

        /*
         * Associate Card Number to User?
         */
        if (assocCardNumber != null && userDb != null) {

            USER_SERVICE.assocPrimaryCardNumber(userDb,
                    rfidNumberFormat.getNormalizedNumber(assocCardNumber));
            /*
             * Do NOT grant a login, just associate the card.
             */
            this.setApiResult(ApiResultCodeEnum.INFO, "msg-card-registered-ok");
            return;
        }

        /*
         * Warnings for Admin WebApp.
         */
        if (webAppType == WebAppTypeEnum.ADMIN) {

            if (!cm.isSetupCompleted()) {
                final StringBuilder parm = new StringBuilder();
                for (final SetupNeededEnum value : cm
                        .getReadyToUseSetupNeeded()) {
                    if (parm.length() > 0) {
                        parm.append(", ");
                    }
                    parm.append(value.uiText(getLocale()));
                }
                parm.append(".");
                this.setApiResult(ApiResultCodeEnum.WARN, "msg-setup-is-needed",
                        parm.toString(), parm.toString());
            } else if (cm.doesInternalAdminHasDefaultPassword()) {
                this.setApiResult(ApiResultCodeEnum.WARN,
                        "msg-change-internal-admin-password");
            } else {
                this.setApiResultMembershipMsg();
            }

        } else {
            this.setApiResultOk();
        }

        /*
         * Deny access, when the user is still not found in the database.
         */
        if (userDb == null) {
            if (authMode == UserAuthModeEnum.OAUTH) {
                /*
                 * This happens when "sp-login-oauth" URL parameter is still
                 * there, as backlash of a previous OAuth, and the Login page is
                 * shown, and user is not authenticated by OAuth (yet).
                 */
                this.onLoginFailed(null);
            } else {
                this.onLoginFailed("msg-login-user-not-present",
                        webAppType.getUiText(), authMode.toString(), "?",
                        remoteAddr);
            }
            return;
        }

        if (this.onTOTPRequest(session, webAppType, authMode, userDb)) {
            return;
        }

        final UserAuthToken authToken;

        if (ApiRequestHelper.isAuthTokenLoginEnabled()) {
            authToken = reqLoginLazyCreateAuthToken(uid, webAppType);
        } else {
            authToken = null;
        }

        this.onUserLoginGranted(getUserData(), session, webAppType, authMode,
                uid, userDb, authToken);

        /*
         * Update session.
         */
        session.setUser(userDb);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(String.format(
                    "Setting user [%s] in session of %s WebApp"
                            + ": isAuthenticated [%s]",
                    uid, webAppType.toString(), session.isAuthenticated()));
        }
    }

    /**
     *
     * @param otp
     * @param webAppType
     * @return The authenticated user or {@code null} when not found or not
     *         authorized.
     * @throws IOException
     * @throws YubicoVerificationException
     * @throws YubicoValidationFailure
     */
    private User reqLoginYubico(final YubiKeyOTP otp,
            final WebAppTypeEnum webAppType) throws IOException,
            YubicoVerificationException, YubicoValidationFailure {

        final String publicId = otp.getPublicId();

        final UserNumberDao userNumberDao =
                ServiceContext.getDaoContext().getUserNumberDao();

        // We need the JPA attached UserNumber and User.
        final UserNumber userNumberDb =
                userNumberDao.findByYubiKeyPubID(publicId);

        final ConfigManager cm = ConfigManager.instance();

        if (userNumberDb == null) {
            return null;
        }

        final User userDb = userNumberDb.getUser();

        if (webAppType == WebAppTypeEnum.ADMIN
                && !userDb.getAdmin().booleanValue()) {
            return null;
        }

        if (otp.isOk(cm.getConfigInteger(Key.AUTH_MODE_YUBIKEY_API_CLIENT_ID),
                cm.getConfigValue(Key.AUTH_MODE_YUBIKEY_API_SECRET_KEY))) {
            return userDb;
        }

        return null;
    }

    /**
     * Tries to login with WebApp authentication token.
     *
     * @param session
     *            {@link SpSession}.
     * @param uid
     *            user id.
     * @param authtoken
     *            token.
     * @param webAppType
     *            The {@link WebAppTypeEnum}.
     * @throws IOException
     *             When IO error.
     */
    private void reqLoginAuthTokenWebApp(final SpSession session,
            final String uid, final String authtoken,
            final WebAppTypeEnum webAppType) throws IOException {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Login [{}] with WebApp {} AuthToken [{}].", uid,
                    webAppType, authtoken);
        }

        final UserDao userDao = ServiceContext.getDaoContext().getUserDao();

        final WebAppUserAuthManager userAuthManager =
                WebAppUserAuthManager.instance();

        final UserAuthToken authTokenObj =
                userAuthManager.getUserAuthToken(authtoken, webAppType);

        String uidAuth = null;

        if (authTokenObj != null
                && authTokenObj.getWebAppType() == webAppType) {

            if (uid.equals(authTokenObj.getUser())) {

                uidAuth = uid;

            } else if (webAppType == WebAppTypeEnum.MAILTICKETS) {

                final String uidOperator = StringUtils.defaultString(
                        ConfigManager.getMailPrintTicketOperator());

                if (uid.equals(uidOperator)) {
                    uidAuth = authTokenObj.getUser();
                }
            }
        }

        final User userDb;

        if (uidAuth == null) {
            userDb = null;
        } else if (webAppType == WebAppTypeEnum.ADMIN
                && ConfigManager.isInternalAdmin(uidAuth)) {
            userDb = ConfigManager.createInternalAdminUser();
        } else {
            userDb = userDao.findActiveUserByUserId(uidAuth);
        }

        if (userDb != null) {

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format(
                        "WebApp AuthToken Login [%s] granted.", uidAuth));
            }
            session.setUser(userDb);

            this.onUserLoginGranted(getUserData(), session, webAppType, null,
                    uidAuth, userDb, authTokenObj);

            this.setApiResultOk();

        } else {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(
                        "{} WebApp AuthToken {} Login [{}] denied: "
                                + "user NOT found.",
                        webAppType, authtoken, uidAuth);
            }
            this.onLoginFailed(null);
        }
    }

    /**
     *
     * @param webAppType
     *            The {@link WebAppTypeEnum}.
     */
    private void setSessionTimeoutSeconds(final WebAppTypeEnum webAppType) {

        final Request request = RequestCycle.get().getRequest();

        if (request instanceof WebRequest) {

            final ServletWebRequest wr = (ServletWebRequest) request;
            final HttpSession session = wr.getContainerRequest().getSession();

            if (session == null) {
                return;
            }

            final IConfigProp.Key key;

            if (webAppType == WebAppTypeEnum.ADMIN) {
                key = IConfigProp.Key.WEB_LOGIN_ADMIN_SESSION_TIMEOUT_MINS;
            } else {
                key = IConfigProp.Key.WEB_LOGIN_USER_SESSION_TIMEOUT_MINS;
            }

            final int timeout = ConfigManager.instance().getConfigInt(key)
                    * DateUtil.SECONDS_IN_MINUTE;

            final int interval;
            if (ApiRequestHelper.isAuthTokenLoginEnabled()) {
                // Mantis #1048
                interval = timeout;
            } else {
                interval = timeout;
            }

            session.setMaxInactiveInterval(interval);
        }
    }

    /**
     * Tries to login with Client App authentication token.
     *
     * @param session
     *            {@link SpSession}.
     * @param userData
     *            The user data to be filled after applying the authentication
     *            method. If method is NOT applied, the userData in not touched.
     * @param userId
     *            The unique user id.
     * @param clientIpAddress
     *            The remote IP address.
     * @param webAppType
     *            The {@link WebAppTypeEnum}.
     *
     * @return {@code false} when Client App Authentication was NOT applied.
     * @throws IOException
     *             When IO error.
     */
    private boolean reqLoginAuthTokenCliApp(final SpSession session,
            final Map<String, Object> userData, final String userId,
            final String clientIpAddress, final WebAppTypeEnum webAppType)
            throws IOException {

        /*
         * INVARIANT: authenticate for User Web App only.
         */
        if (webAppType != WebAppTypeEnum.USER) {
            return false;
        }

        /*
         * INVARIANT: Trust between User Web App and User Client App MUST be
         * enabled.
         */
        if (!ConfigManager.instance()
                .isConfigValue(Key.WEBAPP_USER_AUTH_TRUST_CLIAPP_AUTH)) {
            return false;
        }

        /*
         * INVARIANT: authentication token MUST be present for IP address.
         */
        final UserAuthToken authTokenCliApp =
                ClientAppUserAuthManager.getIpAuthToken(clientIpAddress);

        if (authTokenCliApp == null) {
            return false;
        }

        /*
         * INVARIANT: authentication token MUST match requesting user.
         */
        final String userIdToken = authTokenCliApp.getUser();

        if (userIdToken == null || !userIdToken.equalsIgnoreCase(userId)) {
            return false;
        }

        /*
         * INVARIANT: authentication token MUST not be older than 2 times the
         * max time it takes a long poll to finish.
         */
        if (ServiceContext.getTransactionDate().getTime() - authTokenCliApp
                .getCreateTime() > 2 * UserEventService.getMaxMonitorMsec()) {
            return false;
        }

        /*
         * INVARIANT: User must exist in database.
         */
        final UserDao userDao = ServiceContext.getDaoContext().getUserDao();
        final User userDb = userDao.findActiveUserByUserId(userId);

        if (userDb != null) {

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(
                        "CliApp AuthToken Login [" + userId + "] granted.");
            }

            session.setUser(userDb);

            final UserAuthToken authTokenWebApp =
                    reqLoginLazyCreateAuthToken(userId, webAppType);

            this.onUserLoginGranted(userData, session, webAppType, null, userId,
                    userDb, authTokenWebApp);

            this.setApiResultOk();

        } else {

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("CliApp AuthToken Login [" + userId
                        + "] denied: user NOT found.");
            }

            this.onLoginFailed(null);
        }

        return true;
    }

    /**
     * Uses the existing {@link UserAuthToken} (if found) or creates a new one.
     *
     * @param userId
     *            The user id.
     * @param webAppType
     *            The {@link WebAppTypeEnum}.
     * @return The {@link UserAuthToken}.
     */
    private UserAuthToken reqLoginLazyCreateAuthToken(final String userId,
            final WebAppTypeEnum webAppType) {

        UserAuthToken authToken = WebAppUserAuthManager.instance()
                .getAuthTokenOfUser(userId, webAppType);

        if (LOGGER.isTraceEnabled()) {
            if (authToken == null) {
                LOGGER.trace(String.format("No auth token found for user [%s]",
                        userId));
            } else {
                LOGGER.trace(String.format(
                        "%s WebApp:  AuthToken [%s] [%s] found for user [%s]",
                        webAppType.toString(),
                        authToken.getWebAppType().toString(),
                        authToken.getToken(), userId));
            }
        }

        if (authToken == null || authToken.getWebAppType() != webAppType) {

            authToken = new UserAuthToken(userId, webAppType);

            WebAppUserAuthManager.instance().putUserAuthToken(authToken,
                    webAppType);
        }

        return authToken;
    }

    /**
     * Gets the current Session object.
     *
     * @return The Session that this component is in.
     */
    public Session getSession() {
        return Session.get();
    }

    /**
     * Gets the {@link DocLog} user.
     *
     * @param webAppType
     *            The {@link WebAppTypeEnum}.
     * @param userDbAuth
     *            The authenticated {@link User}. Is {@code null} in case of
     *            {@link WebAppTypeEnum#ADMIN} and reserved user id "admin".
     * @return The {@link DocLog} user, or {@code null} in case of
     *         {@link WebAppTypeEnum#ADMIN} and reserved user id "admin".
     */
    private User resolveDocLogUser(final WebAppTypeEnum webAppType,
            final User userDbAuth) {

        final User docLogUser;

        if (webAppType == WebAppTypeEnum.MAILTICKETS) {
            docLogUser = USER_DAO.findActiveUserByUserId(
                    ConfigManager.getMailPrintTicketOperator());
        } else {
            docLogUser = userDbAuth;
        }
        return docLogUser;
    }

    /**
     * Sets the user data for login request and notifies the authenticated user
     * to the Admin WebApp.
     *
     * @param userData
     *            The user data.
     * @param session
     *            The session.
     * @param webAppType
     *            The {@link WebAppTypeEnum}.
     * @param authMode
     *            The {@link UserAuthModeEnum}. {@code null} when authenticated
     *            by (WebApp or Client) token.
     * @param uidAuth
     *            The authenticated User ID.
     * @param userDbAuth
     *            The authenticated {@link User}.
     * @param authToken
     *            {@code null} when not available.
     * @throws IOException
     *             When IO errors.
     */
    private void onUserLoginGranted(final Map<String, Object> userData,
            final SpSession session, final WebAppTypeEnum webAppType,
            final UserAuthModeEnum authMode, final String uidAuth,
            final User userDbAuth, final UserAuthToken authToken)
            throws IOException {

        final User docLogUser = this.resolveDocLogUser(webAppType, userDbAuth);

        if (webAppType == WebAppTypeEnum.ADMIN
                || docLogUser.getId().equals(userDbAuth.getId())) {
            session.setUserIdDtoDocLog(null);
        } else {
            session.setUserIdDtoDocLog(UserIdDto.create(docLogUser));
        }

        userData.put("id", userDbAuth.getUserId());
        userData.put("id_internal", userDbAuth.getInternal());
        userData.put("key_id", userDbAuth.getId());

        userData.put("doclog_id", docLogUser.getUserId());
        userData.put("doclog_id_internal", docLogUser.getInternal());
        userData.put("doclog_key_id", docLogUser.getId());

        userData.put("fullname", userDbAuth.getFullName());
        userData.put("admin", userDbAuth.getAdmin());
        userData.put("internal", userDbAuth.getInternal());
        userData.put("systime", Long.valueOf(System.currentTimeMillis()));
        userData.put("language", getSession().getLocale().getLanguage());
        userData.put("country", getSession().getLocale().getCountry());
        userData.put("mail", USER_SERVICE.getPrimaryEmailAddress(userDbAuth));

        userData.put("number", StringUtils
                .defaultString(USER_SERVICE.getPrimaryIdNumber(userDbAuth)));

        if (webAppType.isUserTypeOrVariant()) {
            userData.put("uuid",
                    USER_SERVICE.lazyAddUserAttrUuid(userDbAuth).toString());
        }

        if (authToken != null) {
            userData.put("authtoken", authToken.getToken());
        }

        final String cometdToken;

        if (webAppType == WebAppTypeEnum.ADMIN
                || webAppType == WebAppTypeEnum.PRINTSITE) {
            cometdToken = CometdClientMixin.SHARED_USER_ADMIN_TOKEN;
        } else {
            cometdToken = CometdClientMixin.SHARED_USER_TOKEN;
        }
        userData.put("cometdToken", cometdToken);

        WebApp.get().onAuthenticatedUser(webAppType, authMode, session.getId(),
                this.getClientIP(), uidAuth);

        if (webAppType.isUserTypeOrVariant()) {

            ApiRequestHelper.addUserStats(userData, userDbAuth,
                    this.getSession().getLocale(),
                    SpSession.getAppCurrencySymbol());

            /*
             * Make sure that any User Web App long poll for this user is
             * interrupted.
             *
             * Note that user home directory might not exist (yet). If it does
             * not exists, there is no point interrupting the long poll, since
             * we know this poll cannot be pending. See Mantis #792.
             */
            if (UserMsgIndicator.isSafePagesDirPresent(uidAuth)) {
                ApiRequestHelper.interruptPendingLongPolls(uidAuth,
                        this.getClientIP());
            }
            /*
             * Check for expired inbox jobs.
             */
            final long msecJobExpiry = ConfigManager.instance()
                    .getConfigInt(Key.PRINT_IN_JOB_EXPIRY_MINS, 0)
                    * DateUtil.DURATION_MSEC_MINUTE;

            if (msecJobExpiry > 0) {
                INBOX_SERVICE.deleteJobs(uidAuth, System.currentTimeMillis(),
                        msecJobExpiry);
            }

            INBOX_SERVICE.pruneOrphanJobs(uidAuth,
                    ConfigManager.getUserHomeDir(uidAuth), docLogUser);
        }
    }

    /**
     * Lazy creates a user.
     *
     * @param userDao
     *            The {@link UserDao}.
     * @param userAuth
     *            The authenticated user.
     * @param webAppType
     *            The type of web app.
     * @return The lazy created user, or {@code null} when user was NOT created.
     */
    private User onLazyCreateUser(final UserDao userDao, final User userAuth,
            final WebAppTypeEnum webAppType) {
        /*
         * Since the user does not exist in the database (yet) we cannot use SQL
         * row locking to protect concurrent user creation: therefore we use the
         * cruder synchronized block.
         */
        synchronized (LAZY_CREATE_USER_LOCK) {

            final User userDb = userDao.findActiveUserByUserIdInsert(userAuth,
                    new Date(), Entity.ACTOR_SYSTEM);

            /*
             * IMPORTANT: ad-hoc commit + begin transaction
             */
            if (userDb != null) {
                ServiceContext.getDaoContext().commit();
                ServiceContext.getDaoContext().beginTransaction();
            }
            return userDb;
        }
    }

    /**
     * Writes Logging and send notifications.
     *
     * @param webAppType
     *            The type of web app.
     * @param userid
     *            The user id.
     * @param success
     *            {@code true} when user was created, {@code false} when not.
     */
    private void onUserLazyCreated(final WebAppTypeEnum webAppType,
            final String userid, final boolean success) {

        final String msgKey;

        if (success) {
            msgKey = "msg-login-user-lazy-create-success";

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(Messages.getLogFileMessage(getClass(), msgKey,
                        webAppType.getUiText(), userid));
            }

            final String msg = AppLogHelper.logInfo(getClass(), msgKey,
                    webAppType.getUiText(), userid);

            AdminPublisher.instance().publish(PubTopicEnum.USER,
                    PubLevelEnum.CLEAR, msg);

        } else {

            msgKey = "msg-login-user-lazy-create-failed";

            if (LOGGER.isErrorEnabled()) {
                LOGGER.error(Messages.getLogFileMessage(getClass(), msgKey,
                        webAppType.getUiText(), userid));
            }

            final String msg = AppLogHelper.logError(getClass(), msgKey,
                    webAppType.getUiText(), userid);

            AdminPublisher.instance().publish(PubTopicEnum.USER,
                    PubLevelEnum.ERROR, msg);
        }
    }

    /**
     * Deny requested session.
     *
     * @param session
     *            Requested session.
     */
    private void onSessionDenied(final SpSession session) {
        /*
         * TOTPRequest must be held in session to handle TOTPResponse. Also, any
         * session change by human interaction (so not a crawler) must be
         * preserved.
         */
        if (session.getTOTPRequest() == null && !session.isHumanDetected()) {
            session.invalidate();
        }
    }

    /**
     * Handle login failure.
     *
     * @param msgKeyAdminPublish
     *            When {@code null} no log entry is written and no admin message
     *            is published.
     * @param args
     *            The arguments of the message
     */
    private void onLoginFailed(final String msgKeyAdminPublish,
            final String... args) {

        if (StringUtils.isNotBlank(msgKeyAdminPublish)) {

            final String msg = AppLogHelper.logWarning(getClass(),
                    msgKeyAdminPublish, args);

            AdminPublisher.instance().publish(PubTopicEnum.USER,
                    PubLevelEnum.WARN, msg);
        }

        this.setApiResult(ApiResultCodeEnum.ERROR, "msg-login-failed");
    }

    /**
     * Handle TOTP request or not.
     *
     * @param session
     *            {@link SpSession}.
     * @param webAppType
     *            WebAppTypeEnum.
     * @param authMode
     *            Authentication mode.
     * @param userDb
     *            Requesting user.
     * @return {@code true} when TOTP is needed.
     */
    private boolean onTOTPRequest(final SpSession session,
            final WebAppTypeEnum webAppType, final UserAuthModeEnum authMode,
            final User userDb) {

        if (authMode == UserAuthModeEnum.OAUTH
                || !TOTPHelper.isTOTPActivated(userDb)) {
            return false;
        }

        try {
            TOTPHelper.sendCodeToTelegram(userDb);
        } catch (IOException e) {
            LOGGER.warn("Sending TOTP code to Telegram ID of User [{}] failed.",
                    userDb.getUserId());
        }

        session.setWebAppType(webAppType);
        session.setTOTPRequest(UserIdDto.create(userDb));

        this.getUserData().put("authTOTPRequired", true);
        this.setApiResult(ApiResultCodeEnum.UNAUTH, "msg-login-totp-required");
        return true;
    }

    /**
     * Handle TOTP response. Either "code" or "codeRecovery" is {@code null}.
     *
     * @param session
     *            Session.
     * @param webAppType
     *            WebAppTypeEnum
     * @param code
     *            TOTP code from authenticator device.
     * @param codeRecovery
     *            Recovery code as sent to user email address.
     * @return {@code true} if TOTP is valid.
     * @throws IOException
     *             If IO error.
     */
    private boolean onTOTPResponse(final SpSession session,
            final WebAppTypeEnum webAppType, final String code,
            final String codeRecovery) throws IOException {

        final UserIdDto dto = session.getTOTPRequest();

        if (dto == null) {
            throw new IllegalStateException("Application error: no user info.");
        }

        final User userDb = USER_DAO.findActiveUserById(dto.getDbKey());
        if (userDb == null) {
            throw new IllegalStateException(
                    "Application error: user not found.");
        }

        final boolean isAuthenticated;

        if (StringUtils.isBlank(code)) {
            isAuthenticated =
                    TOTPHelper.verifyRecoveryCode(userDb, codeRecovery);
        } else {
            isAuthenticated = TOTPHelper.verifyCode(userDb, code);
        }

        if (isAuthenticated) {

            session.setUser(userDb);
            session.setTOTPRequest(null);

            final UserAuthToken token;

            if (ApiRequestHelper.isAuthTokenLoginEnabled()) {
                token = reqLoginLazyCreateAuthToken(userDb.getUserId(),
                        webAppType);
            } else {
                token = null;
            }

            this.onUserLoginGranted(getUserData(), session, webAppType, null,
                    userDb.getUserId(), userDb, token);

            this.setApiResultOk();

        } else {
            this.onLoginFailed(null);
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("{} WebApp user [{}] TOTP verification [{}]",
                    webAppType, userDb.getUserId(),
                    Boolean.valueOf(isAuthenticated).toString().toUpperCase());
        }

        return isAuthenticated;
    }

    /**
     * Sets the userData message with an error or warning depending on the
     * Membership position. If the membership is ok, the OK message is applied.
     *
     * @throws NumberFormatException
     *             If number error.
     */
    private void setApiResultMembershipMsg() throws NumberFormatException {

        final MemberCard memberCard = MemberCard.instance();

        final Long daysLeft = memberCard.getDaysLeftInVisitorPeriod(
                ServiceContext.getTransactionDate());

        switch (memberCard.getStatus()) {
        case EXCEEDED:
            this.setApiResult(ApiResultCodeEnum.INFO,
                    "msg-membership-exceeded-user-limit",
                    CommunityDictEnum.MEMBERSHIP.getWord(getLocale()),
                    CommunityDictEnum.SAVAPAGE_SUPPORT.getWord(getLocale()),
                    CommunityDictEnum.MEMBER_CARD.getWord(getLocale()));
            break;
        case EXPIRED:
            this.setApiResult(ApiResultCodeEnum.INFO, "msg-membership-expired",
                    CommunityDictEnum.MEMBERSHIP.getWord(getLocale()),
                    CommunityDictEnum.SAVAPAGE_SUPPORT.getWord(getLocale()),
                    CommunityDictEnum.MEMBER_CARD.getWord(getLocale()));

            break;
        case VISITOR:
            this.setApiResult(ApiResultCodeEnum.INFO, "msg-membership-visit",
                    daysLeft.toString(),
                    CommunityDictEnum.VISITOR.getWord(getLocale()));
            break;
        case VISITOR_EXPIRED:
            this.setApiResult(ApiResultCodeEnum.INFO,
                    "msg-membership-visit-expired",
                    CommunityDictEnum.VISITOR.getWord(getLocale()),
                    CommunityDictEnum.SAVAPAGE_SUPPORT.getWord(getLocale()),
                    CommunityDictEnum.MEMBER_CARD.getWord(getLocale()));
            break;
        case WRONG_MODULE:
        case WRONG_COMMUNITY:
            this.setApiResult(ApiResultCodeEnum.INFO,
                    "msg-membership-wrong-product",
                    CommunityDictEnum.MEMBERSHIP.getWord(getLocale()),
                    CommunityDictEnum.SAVAPAGE_SUPPORT.getWord(getLocale()),
                    CommunityDictEnum.MEMBER_CARD.getWord(getLocale()));
            break;
        case WRONG_VERSION:
            this.setApiResult(ApiResultCodeEnum.INFO, "msg-membership-version",
                    CommunityDictEnum.MEMBERSHIP.getWord(getLocale()),
                    CommunityDictEnum.SAVAPAGE_SUPPORT.getWord(getLocale()),
                    CommunityDictEnum.MEMBER_CARD.getWord(getLocale()));
            break;
        case VISITOR_EDITION:
        case VALID:
        default:
            this.setApiResultOk();
            break;
        }
    }

}
