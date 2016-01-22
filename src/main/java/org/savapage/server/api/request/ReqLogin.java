/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
 * Authors: Rijk Ravestein.
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
package org.savapage.server.api.request;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebRequest;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.CometdClientMixin;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.community.MemberCard;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.crypto.CryptoUser;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dao.enums.UserAttrEnum;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.jpa.Device;
import org.savapage.core.jpa.Entity;
import org.savapage.core.jpa.User;
import org.savapage.core.rfid.RfidNumberFormat;
import org.savapage.core.services.DeviceService.DeviceAttrLookup;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.UserAuth;
import org.savapage.core.users.IExternalUserAuthenticator;
import org.savapage.core.users.IUserSource;
import org.savapage.core.users.InternalUserAuthenticator;
import org.savapage.core.users.conf.UserAliasList;
import org.savapage.core.util.AppLogHelper;
import org.savapage.core.util.DateUtil;
import org.savapage.server.SpSession;
import org.savapage.server.WebApp;
import org.savapage.server.api.UserAgentHelper;
import org.savapage.server.auth.ClientAppUserAuthManager;
import org.savapage.server.auth.UserAuthToken;
import org.savapage.server.auth.WebAppUserAuthManager;
import org.savapage.server.cometd.UserEventService;
import org.savapage.server.webapp.WebAppTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqLogin extends ApiRequestMixin {

    /**
    *
    */
    private static final String USER_ROLE_ADMIN = "admin";

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(ReqLogin.class);

    /**
     * .
     */
    @JsonInclude(Include.NON_NULL)
    private static class DtoReq extends AbstractDto {

        private String authMode;
        private String authId;
        private String authPw;
        private String authToken;
        private String assocCardNumber;
        private String role;

        public String getAuthMode() {
            return authMode;
        }

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

        public void setAuthPw(String authPw) {
            this.authPw = authPw;
        }

        public String getAuthToken() {
            return authToken;
        }

        public void setAuthToken(String authToken) {
            this.authToken = authToken;
        }

        public String getAssocCardNumber() {
            return assocCardNumber;
        }

        public void setAssocCardNumber(String assocCardNumber) {
            this.assocCardNumber = assocCardNumber;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

    }

    /**
     * .
     */
    private static class DtoRsp extends AbstractDto {

    }

    @Override
    protected void
            onRequest(final String requestingUser, final User lockedUser)
                    throws IOException {

        final DtoReq dtoReq = DtoReq.create(DtoReq.class, getParmValue("dto"));

        final boolean isAdminLogin =
                dtoReq.getRole() != null
                        && dtoReq.getRole().equals(USER_ROLE_ADMIN);

        reqLogin(UserAuth.mode(dtoReq.getAuthMode()), dtoReq.getAuthId(),
                dtoReq.getAuthPw(), dtoReq.getAuthToken(),
                dtoReq.getAssocCardNumber(), isAdminLogin);
    }

    /**
     * Handles the login request for both the User and Admin WebApp.
     * <p>
     * NOTE: If {@link #AUTH_MODE_USER} and authPw is {@code null} the user is
     * validated against the authToken.
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
     * {@link #reqLoginNew(org.savapage.core.services.helpers.UserAuth.Mode, String, String, String, boolean)}, {@link #reqLoginAuthTokenCliApp(Map, String, String, boolean)(} and
     * {@link #reqLoginAuthTokenWebApp(String, String, boolean)}.</li>
     * </ul>
     *
     * @param authMode
     *            The authentication mode.
     * @param authId
     *            Offered use name (handled as user alias), ID Number or Card
     *            Number.
     * @param authPw
     *            The password or PIN. When {@code null} AND
     *            {@link #AUTH_MODE_USER}, the authToken is used to validate.
     * @param authToken
     *            The authentication token.
     * @param assocCardNumber
     *            The card number to associate with this user account. When
     *            {@code null} NO card will be associated.
     * @param isAdminOnlyLogin
     *            <code>true</code> if this is a login for admin only.
     * @return The map which can be converted to a json string by the caller.
     * @throws IOException
     */
    private void reqLogin(final UserAuth.Mode authMode, final String authId,
            final String authPw, final String authToken,
            final String assocCardNumber, final boolean isAdminOnlyLogin)
            throws IOException {

        final UserAgentHelper userAgentHelper = this.createUserAgentHelper();

        /*
         * Browser diagnostics.
         */
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Browser detection for [" + authId + "] Mobile ["
                    + userAgentHelper.isMobileBrowser() + "] Mobile Safari ["
                    + userAgentHelper.isSafariBrowserMobile() + "] UserAgent ["
                    + userAgentHelper.getUserAgentHeader() + "]");
        }

        // final Map<String, Object> userData = new HashMap<String, Object>();

        final ConfigManager cm = ConfigManager.instance();
        final SpSession session = SpSession.get();

        if (LOGGER.isTraceEnabled()) {
            String testLog = "Session [" + session.getId() + "]";
            testLog += " WebAppCount [" + session.getAuthWebAppCount() + "]";
            LOGGER.trace(testLog);
        }

        /*
         * INVARIANT: Only one (1) authenticated session allowed for (non Mac OS
         * X Safari) desktop computers.
         */
        if (!userAgentHelper.isMobileBrowser()
                && !userAgentHelper.isSafariBrowserMacOsX()
                && SpSession.get().getAuthWebAppCount() != 0) {

            this.setApiResult(ApiResultCodeEnum.ERROR,
                    "msg-login-another-session-active");
            return;
        }

        /*
         * INVARIANT: The application SHOULD be initialized.
         */
        if (!cm.isInitialized()) {
            setApiResult(ApiResultCodeEnum.ERROR, "msg-login-not-possible");
            return;
        }

        /*
         * INVARIANT: If setup is NOT completed the only login possible is mode
         * {@link #AUTH_MODE_USER} as INTERNAL admin in the admin application.
         */
        if (!cm.isSetupCompleted()) {
            if (!isAdminOnlyLogin || authMode != UserAuth.Mode.NAME) {
                setApiResult(ApiResultCodeEnum.ERROR, "msg-login-install-mode");
                return;
            }
            if (!ConfigManager.isInternalAdmin(authId)) {
                setApiResult(ApiResultCodeEnum.ERROR,
                        "msg-login-as-internal-admin");
                return;
            }
        }

        /*
         * INVARIANT: If Application is NOT ready-to-use the only login possible
         * is as admin in the admin application.
         */
        if (!cm.isAppReadyToUse()) {
            if (!isAdminOnlyLogin) {
                setApiResult(ApiResultCodeEnum.ERROR, "msg-login-app-config");
                return;
            }
        }

        //
        final boolean isAuthTokenLoginEnabled =
                ApiRequestHelper.isAuthTokenLoginEnabled();

        /*
         * If user authentication token (browser local storage) is disabled or
         * user was authenticated by OneTimeAuthToken, we fall back to the user
         * in the active session.
         */
        if ((!isAuthTokenLoginEnabled || session.isOneTimeAuthToken())
                && session.getUser() != null) {

            /*
             * INVARIANT: User must exist in database.
             */
            final UserDao userDao = ServiceContext.getDaoContext().getUserDao();

            // We need the JPA attached User.
            final User userDb =
                    userDao.findActiveUserByUserId(session.getUser()
                            .getUserId());

            if (userDb == null) {
                onLoginFailed(null);
            } else {
                onUserLoginGranted(getUserData(), session, isAdminOnlyLogin,
                        session.getUser().getUserId(), userDb, null);
                setApiResultOk();
            }

        } else {

            //
            final boolean isCliAppAuthApplied;

            if (isAuthTokenLoginEnabled && authMode == UserAuth.Mode.NAME
                    && StringUtils.isBlank(authPw)) {
                isCliAppAuthApplied =
                        this.reqLoginAuthTokenCliApp(getUserData(), authId,
                                this.getRemoteAddr(), isAdminOnlyLogin);
            } else {
                isCliAppAuthApplied = false;
            }

            if (!isCliAppAuthApplied) {

                if (isAuthTokenLoginEnabled && authMode == UserAuth.Mode.NAME
                        && StringUtils.isBlank(authPw)
                        && StringUtils.isNotBlank(authToken)) {

                    reqLoginAuthTokenWebApp(authId, authToken, isAdminOnlyLogin);

                } else {
                    reqLoginNew(authMode, authId, authPw, assocCardNumber,
                            isAdminOnlyLogin);
                }
            }
        }

        getUserData().put("sessionid", SpSession.get().getId());

        if (isApiResultOk()) {
            setSessionTimeoutSeconds(isAdminOnlyLogin);
            SpSession.get().incrementAuthWebApp();
        }
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
     * @param isAdminOnlyLogin
     *            <code>true</code> if this is a login for admin only.
     * @return Same object as userData param.
     * @throws IOException
     */
    private void reqLoginNew(final UserAuth.Mode authMode, final String authId,
            final String authPw, final String assocCardNumber,
            final boolean isAdminOnlyLogin) throws IOException {

        /*
         * INVARIANT: Password can NOT be empty in Name authentication.
         */
        if (authMode == UserAuth.Mode.NAME) {
            if (StringUtils.isBlank(authPw)) {
                onLoginFailed(null);
                return;
            }
        }

        /*
         *
         */
        final Device terminal =
                ApiRequestHelper.getHostTerminal(this.getRemoteAddr());

        final UserAuth theUserAuth =
                new UserAuth(terminal, null, isAdminOnlyLogin);

        if (!theUserAuth.isAuthModeAllowed(authMode)) {
            setApiResult(ApiResultCodeEnum.ERROR,
                    "msg-auth-mode-not-available", authMode.toString());
            return;
        }

        /*
         *
         */
        final UserDao userDao = ServiceContext.getDaoContext().getUserDao();

        final SpSession session = SpSession.get();
        final ConfigManager cm = ConfigManager.instance();

        final IExternalUserAuthenticator userAuthenticator =
                cm.getUserAuthenticator();

        /*
         * This is the place to set the WebAppType session attribute.
         */
        final WebAppTypeEnum webAppType;

        if (isAdminOnlyLogin) {
            webAppType = WebAppTypeEnum.ADMIN;
        } else {
            webAppType = WebAppTypeEnum.USER;
        }
        session.setWebAppType(webAppType);

        /*
         * Initialize pessimistic.
         */
        session.setUser(null);

        /*
         * The facts.
         */
        final boolean allowInternalUsersOnly = (userAuthenticator == null);

        final boolean isInternalAdmin =
                authMode == UserAuth.Mode.NAME
                        && ConfigManager.isInternalAdmin(authId);

        final boolean isLazyUserInsert =
                cm.isUserInsertLazyLogin() && !isAdminOnlyLogin
                        && !allowInternalUsersOnly;

        /*
         * To find out.
         */
        boolean isAuthenticated = false;
        boolean isInternalUser = false;
        String uid = null;
        User userDb = null;

        /*
         *
         */
        RfidNumberFormat rfidNumberFormat = null;

        if (authMode == UserAuth.Mode.CARD_LOCAL || assocCardNumber != null) {
            if (terminal == null) {
                rfidNumberFormat = new RfidNumberFormat();
            } else {
                final DeviceAttrLookup lookup = new DeviceAttrLookup(terminal);
                rfidNumberFormat =
                        DEVICE_SERVICE.createRfidNumberFormat(terminal, lookup);
            }
        }

        /*
         *
         */
        if (isInternalAdmin) {

            /*
             * Internal admin
             */

            if (isAdminOnlyLogin) {

                User userAuth =
                        ConfigManager.instance().isInternalAdminValid(authId,
                                authPw);

                isAuthenticated = (userAuth != null);

                if (isAuthenticated) {

                    uid = authId;
                    userDb = userAuth;

                } else {
                    /*
                     * INVARIANT: internal admin password must be correct.
                     */
                    onLoginFailed("msg-login-invalid-password",
                            webAppType.getUiText(), authId);
                    return;
                }

            } else {
                /*
                 * INVARIANT: internal admin is NOT allowed to login to the User
                 * WebApp.
                 */
                onLoginFailed("msg-login-denied", webAppType.getUiText(),
                        authId);
                return;
            }

        } else {

            if (authMode == UserAuth.Mode.NAME) {

                /*
                 * Get the "real" username from the alias.
                 */
                if (allowInternalUsersOnly) {
                    uid = UserAliasList.instance().getUserName(authId);
                } else {
                    uid =
                            UserAliasList.instance().getUserName(
                                    userAuthenticator.asDbUserId(authId));
                    uid = userAuthenticator.asDbUserId(uid);
                }
                /*
                 * Read real user from database.
                 */
                userDb = userDao.findActiveUserByUserId(uid);

            } else if (authMode == UserAuth.Mode.ID) {

                userDb = USER_SERVICE.findUserByNumber(authId);

                /*
                 * INVARIANT: User MUST be present in database.
                 */
                if (userDb == null) {
                    onLoginFailed("msg-login-invalid-number",
                            webAppType.getUiText(), authId);
                    return;
                }
                uid = userDb.getUserId();

            } else if (authMode == UserAuth.Mode.CARD_IP
                    || authMode == UserAuth.Mode.CARD_LOCAL) {

                String normalizedCardNumber = authId;

                if (authMode == UserAuth.Mode.CARD_LOCAL) {
                    normalizedCardNumber =
                            rfidNumberFormat.getNormalizedNumber(authId);
                }

                userDb =
                        USER_SERVICE.findUserByCardNumber(normalizedCardNumber);

                /*
                 * INVARIANT: User MUST be present.
                 */
                if (userDb == null) {

                    getUserData().put("authCardSelfAssoc",
                            Boolean.valueOf(theUserAuth.isAuthCardSelfAssoc()));

                    setApiResult(ApiResultCodeEnum.ERROR,
                            "msg-login-unregistered-card");
                    return;
                }
                uid = userDb.getUserId();
            }

            /*
             * Check invariants based on user presence in database.
             */
            if (userDb == null) {

                if (isAdminOnlyLogin) {
                    /*
                     * INVARIANT: User MUST exist to login to Admin WebApp (no
                     * lazy user insert allowed in this case)
                     */
                    onLoginFailed("msg-login-user-not-present",
                            webAppType.getUiText(), authId);
                    return;
                }

                if (allowInternalUsersOnly) {
                    /*
                     * INVARIANT: User MUST exist to login when NO external user
                     * source (no lazy user insert allowed in this case).
                     */
                    onLoginFailed("msg-login-user-not-present",
                            webAppType.getUiText(), authId);
                    return;
                }

                if (!isLazyUserInsert) {
                    /*
                     * INVARIANT: User MUST exist to login when lazy user insert
                     * is disabled.
                     */
                    onLoginFailed("msg-login-user-not-present",
                            webAppType.getUiText(), authId);
                    return;
                }

            } else {

                if (isAdminOnlyLogin && !userDb.getAdmin()) {
                    /*
                     * INVARIANT: User MUST have admin rights to login to Admin
                     * WebApp.
                     */
                    onLoginFailed("msg-login-no-admin-rights",
                            webAppType.getUiText(), userDb.getUserId());
                    return;
                }

                if (!userDb.getPerson()) {
                    /*
                     * INVARIANT: User MUST be a Person to login.
                     */
                    onLoginFailed("msg-login-no-person",
                            webAppType.getUiText(), userDb.getUserId());
                    return;
                }

                final Date onDate = new Date();

                if (USER_SERVICE.isUserFullyDisabled(userDb, onDate)) {
                    /*
                     * INVARIANT: User MUST be active (enabled) at moment of
                     * login.
                     */
                    onLoginFailed("msg-login-disabled", webAppType.getUiText(),
                            userDb.getUserId());
                    return;
                }

                /*
                 * Identify internal user.
                 */
                isInternalUser = userDb.getInternal();

            }

            /*
             * Authenticate
             */
            if (authMode == UserAuth.Mode.NAME) {

                if (isInternalUser) {

                    isAuthenticated =
                            InternalUserAuthenticator.authenticate(userDb,
                                    authPw);

                    if (!isAuthenticated) {
                        /*
                         * INVARIANT: Password of Internal User must be correct.
                         */
                        onLoginFailed("msg-login-invalid-password",
                                webAppType.getUiText(), userDb.getUserId());
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
                        onLoginFailed("msg-login-invalid-password",
                                webAppType.getUiText(), uid);
                        return;
                    }

                    /**
                     * Lazy user insert
                     */
                    if (userDb == null) {

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
                            userDb =
                                    userDao.findActiveUserByUserIdInsert(
                                            userAuth, new Date(),
                                            Entity.ACTOR_SYSTEM);
                            /*
                             * IMPORTANT: ad-hoc commit + begin transaction
                             */
                            if (userDb != null) {
                                ServiceContext.getDaoContext().commit();
                                ServiceContext.getDaoContext()
                                        .beginTransaction();
                            }
                        }
                    }
                }

            } else {

                /*
                 * Check PIN for both ID Number, Local and Network Card.
                 */
                isAuthenticated =
                        (authMode == UserAuth.Mode.ID && !theUserAuth
                                .isAuthIdPinReq())
                                || (authMode == UserAuth.Mode.CARD_IP && !theUserAuth
                                        .isAuthCardPinReq())
                                || (authMode == UserAuth.Mode.CARD_LOCAL && !theUserAuth
                                        .isAuthCardPinReq());

                if (!isAuthenticated) {

                    if (StringUtils.isBlank(authPw)) {
                        /*
                         * INVARIANT: PIN can NOT be empty.
                         */
                        onLoginFailed("msg-login-no-pin-available",
                                webAppType.getUiText(), authId);
                        return;
                    }

                    final String encryptedPin =
                            USER_SERVICE.findUserAttrValue(userDb,
                                    UserAttrEnum.PIN);
                    String pin = "";
                    if (encryptedPin != null) {
                        pin =
                                CryptoUser.decryptUserAttr(userDb.getId(),
                                        encryptedPin);
                    }
                    isAuthenticated = pin.equals(authPw);
                }

                if (!isAuthenticated) {
                    /*
                     * INVARIANT: PIN must be correct.
                     */
                    onLoginFailed("msg-login-invalid-pin",
                            webAppType.getUiText(), authId);
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
                userDb = userDao.lock(userDb.getId());

                try {
                    USER_SERVICE.lazyUserHomeDir(uid);
                } catch (IOException e) {
                    setApiResult(ApiResultCodeEnum.ERROR,
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
            setApiResult(ApiResultCodeEnum.INFO, "msg-card-registered-ok");
            return;
        }

        /*
         * Warnings for Admin WebApp.
         */
        if (isAdminOnlyLogin) {
            if (!cm.isSetupCompleted()) {
                setApiResult(ApiResultCodeEnum.WARN, "msg-setup-is-needed");
            } else if (cm.doesInternalAdminHasDefaultPassword()) {
                setApiResult(ApiResultCodeEnum.WARN,
                        "msg-change-internal-admin-password");
            } else {
                setApiResultMembershipMsg();
            }

        } else {
            setApiResultOk();
        }

        /*
         * Deny access, when the user is still not found in the database.
         */
        if (userDb == null) {
            onLoginFailed("msg-login-user-not-present", webAppType.getUiText(),
                    uid);
            return;
        }

        final UserAuthToken authToken;

        if (ApiRequestHelper.isAuthTokenLoginEnabled()) {
            authToken = reqLoginLazyCreateAuthToken(uid, isAdminOnlyLogin);
        } else {
            authToken = null;
        }

        onUserLoginGranted(getUserData(), session, isAdminOnlyLogin, uid,
                userDb, authToken);

        /*
         * Update session.
         */
        session.setUser(userDb);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Setting user in session of application ["
                    + SpSession.get().getApplication().getClass().toString()
                    + "] isAuthenticated [" + SpSession.get().isAuthenticated()
                    + "]");
        }
    }

    /**
     * Tries to login with WebApp authentication token.
     *
     * @param uid
     * @param authtoken
     * @param isAdminOnly
     * @return
     * @throws IOException
     */
    private void reqLoginAuthTokenWebApp(final String uid,
            final String authtoken, final boolean isAdminOnly)
            throws IOException {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(String
                    .format("Login [%s] with WebApp AuthToken.", uid));
        }

        final UserDao userDao = ServiceContext.getDaoContext().getUserDao();

        final WebAppUserAuthManager userAuthManager =
                WebAppUserAuthManager.instance();

        final UserAuthToken authTokenObj =
                userAuthManager.getUserAuthToken(authtoken, isAdminOnly);

        final User userDb;

        if (authTokenObj != null && uid.equals(authTokenObj.getUser())
                && authTokenObj.isAdminOnly() == isAdminOnly) {

            if (isAdminOnly && ConfigManager.isInternalAdmin(uid)) {
                userDb = ConfigManager.createInternalAdminUser();
            } else {
                userDb = userDao.findActiveUserByUserId(uid);
            }

        } else {
            userDb = null;
        }

        if (userDb != null) {

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format(
                        "WebApp AuthToken Login [%s] granted.", uid));
            }

            final SpSession session = SpSession.get();

            session.setUser(userDb);

            onUserLoginGranted(getUserData(), session, isAdminOnly, uid,
                    session.getUser(), authTokenObj);

            setApiResultOk();

        } else {

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format(
                        "WebApp AuthToken Login [%s] denied: user NOT found.",
                        uid));
            }

            onLoginFailed(null);
        }

    }

    /**
     *
     * @param isAdminSession
     */
    private void setSessionTimeoutSeconds(boolean isAdminSession) {

        final Request request = RequestCycle.get().getRequest();

        if (request instanceof WebRequest) {

            final ServletWebRequest wr = (ServletWebRequest) request;
            final HttpSession session = wr.getContainerRequest().getSession();

            if (session == null) {
                return;
            }

            final int minutes;

            if (ApiRequestHelper.isAuthTokenLoginEnabled()) {

                minutes = 0;

            } else {

                final IConfigProp.Key configKey;

                if (isAdminSession) {
                    configKey =
                            IConfigProp.Key.WEB_LOGIN_ADMIN_SESSION_TIMOUT_MINS;
                } else {
                    configKey =
                            IConfigProp.Key.WEB_LOGIN_USER_SESSION_TIMEOUT_MINS;
                }

                minutes = ConfigManager.instance().getConfigInt(configKey);

            }
            session.setMaxInactiveInterval(minutes * DateUtil.SECONDS_IN_MINUTE);
        }
    }

    /**
     * Tries to login with Client App authentication token.
     *
     * @param userData
     *            The user data to be filled after applying the authentication
     *            method. If method is NOT applied, the userData in not touched.
     * @param userId
     *            The unique user id.
     * @param clientIpAddress
     *            The remote IP address.
     * @return {@code false} when Client App Authentication was NOT applied.
     * @throws IOException
     */
    private boolean reqLoginAuthTokenCliApp(final Map<String, Object> userData,
            final String userId, final String clientIpAddress,
            final boolean isAdminOnly) throws IOException {

        /*
         * INVARIANT: do NOT authenticate for Admin Web App.
         */
        if (isAdminOnly) {
            return false;
        }

        /*
         * INVARIANT: Trust between User Web App and User Client App MUST be
         * enabled.
         */
        if (!ConfigManager.instance().isConfigValue(
                Key.WEBAPP_USER_AUTH_TRUST_CLIAPP_AUTH)) {
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
        if (ServiceContext.getTransactionDate().getTime()
                - authTokenCliApp.getCreateTime() > 2 * UserEventService
                .getMaxMonitorMsec()) {
            return false;
        }

        /*
         * INVARIANT: User must exist in database.
         */
        final UserDao userDao = ServiceContext.getDaoContext().getUserDao();
        final User userDb = userDao.findActiveUserByUserId(userId);

        if (userDb != null) {

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("CliApp AuthToken Login [" + userId + "] granted.");
            }

            final SpSession session = SpSession.get();

            session.setUser(userDb);

            final UserAuthToken authTokenWebApp =
                    reqLoginLazyCreateAuthToken(userId, isAdminOnly);

            onUserLoginGranted(userData, session, isAdminOnly, userId,
                    session.getUser(), authTokenWebApp);

            setApiResultOk();

        } else {

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("CliApp AuthToken Login [" + userId
                        + "] denied: user NOT found.");
            }

            onLoginFailed(null);
        }

        return true;
    }

    /**
     * Uses the existing {@link UserAuthToken} (if found) or creates a new one.
     *
     * @param userId
     *            The user id.
     * @param isAdminOnlyLogin
     * @return The {@link UserAuthToken}.
     */
    private UserAuthToken reqLoginLazyCreateAuthToken(final String userId,
            final boolean isAdminOnlyLogin) {

        UserAuthToken authToken =
                WebAppUserAuthManager.instance().getAuthTokenOfUser(userId,
                        isAdminOnlyLogin);

        if (authToken == null || authToken.isAdminOnly() != isAdminOnlyLogin) {

            authToken = new UserAuthToken(userId, isAdminOnlyLogin);

            WebAppUserAuthManager.instance().putUserAuthToken(authToken,
                    isAdminOnlyLogin);
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
     * Sets the user data for login request and notifies the authenticated user
     * to the Admin WebApp.
     *
     * @param userData
     * @param session
     * @param isAdminOnlyLogin
     * @param uid
     * @param userDb
     * @param authToken
     *            {@code null} when not available.
     * @throws IOException
     */
    private void onUserLoginGranted(final Map<String, Object> userData,
            final SpSession session, final boolean isAdminOnlyLogin,
            final String uid, final User userDb, final UserAuthToken authToken)
            throws IOException {

        userData.put("id", uid);
        userData.put("key_id", userDb.getId());
        userData.put("fullname", userDb.getFullName());
        userData.put("admin", userDb.getAdmin());
        userData.put("internal", userDb.getInternal());
        userData.put("systime", Long.valueOf(System.currentTimeMillis()));
        userData.put("language", getSession().getLocale().getLanguage());
        userData.put("country", getSession().getLocale().getCountry());
        userData.put("mail", USER_SERVICE.getPrimaryEmailAddress(userDb));

        userData.put("number", StringUtils.defaultString(USER_SERVICE
                .getPrimaryIdNumber(userDb)));

        if (!isAdminOnlyLogin) {
            userData.put("uuid", USER_SERVICE.lazyAddUserAttrUuid(userDb)
                    .toString());
        }

        if (authToken != null) {
            userData.put("authtoken", authToken.getToken());
        }

        final String cometdToken;

        if (userDb.getAdmin()) {
            cometdToken = CometdClientMixin.SHARED_USER_ADMIN_TOKEN;
            userData.put("role", "admin"); // TODO
        } else {
            cometdToken = CometdClientMixin.SHARED_USER_TOKEN;
            userData.put("role", "editor"); // TODO
        }
        userData.put("cometdToken", cometdToken);

        // role (editor|admin|reader)

        WebApp.get().onAuthenticatedUser(session.getId(), getRemoteAddr(), uid,
                userDb.getAdmin());

        if (!isAdminOnlyLogin) {

            ApiRequestHelper.addUserStats(userData, userDb, this.getSession()
                    .getLocale(), SpSession.getAppCurrencySymbol());

            /*
             * Make sure that any User Web App long poll for this user is
             * interrupted.
             */
            ApiRequestHelper.interruptPendingLongPolls(userDb.getUserId(),
                    this.getRemoteAddr());

            INBOX_SERVICE.pruneOrphanJobs(ConfigManager.getUserHomeDir(uid),
                    userDb);
        }
    }

    /**
     * Handle login failure.
     *
     * @param msgKeyAdminPublish
     *            When {@code null} no admin message is published.
     * @param args
     *            The arguments of the message
     */
    private void onLoginFailed(final String msgKeyAdminPublish,
            final String... args) {

        if (StringUtils.isNotBlank(msgKeyAdminPublish)) {

            final String msg =
                    AppLogHelper.logWarning(getClass(), msgKeyAdminPublish,
                            args);

            AdminPublisher.instance().publish(PubTopicEnum.USER,
                    PubLevelEnum.WARN, msg);
        }

        setApiResult(ApiResultCodeEnum.ERROR, "msg-login-failed");
    }

    /**
     * Sets the userData message with an error or warning depending on the
     * Membership position. If the membership is ok, the OK message is applied.
     *
     * @throws NumberFormatException
     */
    private void setApiResultMembershipMsg() throws NumberFormatException {

        final MemberCard memberCard = MemberCard.instance();

        Long daysLeft =
                memberCard.getDaysLeftInVisitorPeriod(ServiceContext
                        .getTransactionDate());

        switch (memberCard.getStatus()) {
        case EXCEEDED:
            setApiResult(ApiResultCodeEnum.INFO,
                    "msg-membership-exceeded-user-limit",
                    CommunityDictEnum.MEMBERSHIP.getWord(),
                    CommunityDictEnum.SAVAPAGE_SUPPORT.getWord(),
                    CommunityDictEnum.MEMBER_CARD.getWord());
            break;
        case EXPIRED:
            setApiResult(ApiResultCodeEnum.INFO, "msg-membership-expired",
                    CommunityDictEnum.MEMBERSHIP.getWord(),
                    CommunityDictEnum.SAVAPAGE_SUPPORT.getWord(),
                    CommunityDictEnum.MEMBER_CARD.getWord());

            break;
        case VISITOR:
            setApiResult(ApiResultCodeEnum.INFO, "msg-membership-visit",
                    daysLeft.toString(), CommunityDictEnum.VISITOR.getWord());
            break;
        case VISITOR_EXPIRED:
            setApiResult(ApiResultCodeEnum.INFO,
                    "msg-membership-visit-expired",
                    CommunityDictEnum.VISITOR.getWord(),
                    CommunityDictEnum.SAVAPAGE_SUPPORT.getWord(),
                    CommunityDictEnum.MEMBER_CARD.getWord());
            break;
        case WRONG_MODULE:
        case WRONG_COMMUNITY:
            setApiResult(ApiResultCodeEnum.INFO,
                    "msg-membership-wrong-product",
                    CommunityDictEnum.MEMBERSHIP.getWord(),
                    CommunityDictEnum.SAVAPAGE_SUPPORT.getWord(),
                    CommunityDictEnum.MEMBER_CARD.getWord());
            break;
        case WRONG_VERSION:
            setApiResult(ApiResultCodeEnum.INFO, "msg-membership-version",
                    CommunityDictEnum.MEMBERSHIP.getWord(),
                    CommunityDictEnum.SAVAPAGE_SUPPORT.getWord(),
                    CommunityDictEnum.MEMBER_CARD.getWord());
            break;
        case WRONG_VERSION_WITH_GRACE:
            setApiResult(ApiResultCodeEnum.INFO,
                    "msg-membership-version-grace",
                    CommunityDictEnum.MEMBERSHIP.getWord(),
                    daysLeft.toString(),
                    CommunityDictEnum.MEMBER_CARD.getWord());
            break;
        case VISITOR_EDITION:
        case VALID:
        default:
            setApiResultOk();
            break;
        }
    }

}
