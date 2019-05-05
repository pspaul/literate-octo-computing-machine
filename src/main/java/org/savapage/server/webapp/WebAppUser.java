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
package org.savapage.server.webapp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.savapage.core.SpException;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.config.WebAppTypeEnum;
import org.savapage.core.crypto.OneTimeAuthToken;
import org.savapage.core.doc.DocContentTypeEnum;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserEmail;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserService;
import org.savapage.core.users.conf.UserAliasList;
import org.savapage.ext.oauth.OAuthClientPlugin;
import org.savapage.ext.oauth.OAuthPluginException;
import org.savapage.ext.oauth.OAuthProviderEnum;
import org.savapage.ext.oauth.OAuthUserInfo;
import org.savapage.server.WebApp;
import org.savapage.server.WebAppParmEnum;
import org.savapage.server.dropzone.WebPrintHelper;
import org.savapage.server.ext.ServerPluginManager;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.pages.FontOptionsPanel;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.session.SpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class WebAppUser extends AbstractWebAppPage {

    private static final long serialVersionUID = 1L;

    private final static String PAGE_PARM_AUTH_TOKEN = "auth_token";
    private final static String PAGE_PARM_AUTH_TOKEN_USERID = "auth_user";

    private final static String WICKET_ID_FILE_UPLOAD_FONTFAMILY_OPT =
            "file-upload-fontfamily-options";

    /**
     *
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(WebAppUser.class);

    /**
     *
     */
    private static final UserService USER_SERVICE =
            ServiceContext.getServiceFactory().getUserService();

    @Override
    protected boolean isJqueryCoreRenderedByWicket() {
        return true;
    }

    @Override
    protected WebAppTypeEnum getWebAppType() {
        return WebAppTypeEnum.USER;
    }

    @Override
    protected void renderWebAppTypeJsFiles(final IHeaderResponse response,
            final String nocache) {
        renderJs(response, String.format("%s%s",
                JS_FILE_JQUERY_SAVAPAGE_PAGE_PRINT_DELEGATION, nocache));
        renderJs(response,
                String.format("%s%s", getSpecializedJsFileName(), nocache));
    }

    /**
     *
     * @author Rijk Ravestein
     *
     */
    private enum UploadNextButton {
        PRINT, PDF, INBOX;
    }

    /**
     * We use this workaround, because <wicket:enclosure> does not work (Wicket
     * bug?).
     */
    private final class UploadNextButtonView
            extends PropertyListView<UploadNextButton> {

        private static final long serialVersionUID = 1L;

        /**
         *
         * @param id
         *            Wicket id.
         * @param entryList
         *            Button list.
         */
        UploadNextButtonView(final String id,
                final List<UploadNextButton> entryList) {
            super(id, entryList);
        }

        @Override
        protected void populateItem(final ListItem<UploadNextButton> item) {

            final UploadNextButton button = item.getModelObject();

            final String htmlId;
            final String cssClass;
            final String uiText;

            switch (button) {
            case PRINT:
                htmlId = "sp-btn-file-upload-print";
                cssClass = "ui-icon-main-print";
                uiText = HtmlButtonEnum.PRINT.uiText(getPage().getLocale());
                break;

            case PDF:
                htmlId = "sp-btn-file-upload-pdf";
                cssClass = "ui-icon-main-pdf-properties";
                uiText = "PDF";
                break;

            default:
                htmlId = "sp-btn-file-upload-inbox";
                cssClass = "ui-icon-main-home";
                uiText = HtmlButtonEnum.BACK.uiText(getPage().getLocale());
                break;
            }

            //
            Label label = new Label("next-button", uiText);

            MarkupHelper.modifyLabelAttr(label, MarkupHelper.ATTR_ID, htmlId);
            MarkupHelper.appendLabelAttr(label, MarkupHelper.ATTR_CLASS,
                    cssClass);
            MarkupHelper.appendLabelAttr(label, MarkupHelper.ATTR_TITLE,
                    localized(htmlId.concat("-tooltip")));

            item.add(label);
        }
    }

    /**
     * Formats userid for logging.
     *
     * @param useridRaw
     *            The raw userid as received.
     * @param userid
     *            The userid used (from alias)
     * @return The formatted userid
     */
    private static String formatUserId(final String useridRaw,
            final String userid) {
        if (useridRaw.equals(userid)) {
            return useridRaw;
        }
        return String.format("%s → %s", useridRaw, userid);
    }

    /**
     * Checks OAuth login request.
     *
     * @param mutableProvider
     *            The AOuth provider, or {@code null} when not found.
     * @return {@code null} when OAuth is <i>not</i> applicable.
     *         {@link Boolean#FALSE} when authentication failed.
     */
    private Boolean checkOAuthToken(
            final MutableObject<OAuthProviderEnum> mutableProvider) {

        final Request request = this.getRequestCycle().getRequest();

        final IRequestParameters reqParms = request.getRequestParameters();

        /*
         * Check remainder of previous successful OAuth. If present, do NOT
         * honor remainder of OAuth URL path.
         */
        final StringValue oauthLogIn = reqParms
                .getParameterValue(WebAppParmEnum.SP_LOGIN_OAUTH.parm());

        if (!oauthLogIn.isEmpty()) {
            return null;
        }

        final StringValue oauthProviderValue =
                reqParms.getParameterValue(WebAppParmEnum.SP_OAUTH.parm());

        String oauthProvider = null;

        if (oauthProviderValue.isEmpty()) {
            final Url requestUrl = request.getUrl();
            final List<String> urlSegments = requestUrl.getSegments();
            if (urlSegments.size() == 3 && urlSegments.get(1)
                    .equals(WebAppParmEnum.SP_OAUTH.parm())) {
                oauthProvider = urlSegments.get(2);
            }
        } else {
            oauthProvider = oauthProviderValue.toString();
        }

        if (oauthProvider == null) {
            return null;
        }

        final StringValue oauthInstanceIdValue =
                reqParms.getParameterValue(WebAppParmEnum.SP_OAUTH_ID.parm());

        final String logPfx;

        final String oauthInstanceId;
        if (oauthInstanceIdValue.isEmpty()) {
            oauthInstanceId = null;
            logPfx = String.format("OAuth [%s]", oauthProviderValue);
        } else {
            oauthInstanceId = oauthInstanceIdValue.toString();
            logPfx = String.format("OAuth [%s][%s]", oauthProviderValue,
                    oauthInstanceId);
        }

        final ServerPluginManager pluginManager =
                WebApp.get().getPluginManager();

        OAuthClientPlugin plugin = null;

        for (final OAuthProviderEnum value : OAuthProviderEnum.values()) {
            if (oauthProvider.equalsIgnoreCase(value.toString())) {
                plugin = pluginManager.getOAuthClient(value, oauthInstanceId);
            }
            if (plugin == null) {
                continue;
            }
            break;
        }

        if (plugin == null) {
            LOGGER.error("{}: plugin not found.", logPfx);
            return Boolean.FALSE;
        }

        mutableProvider.setValue(plugin.getProvider());

        /*
         * Collect the parameters for the callback.
         */
        final Map<String, String> parms = new HashMap<>();

        for (final String name : reqParms.getParameterNames()) {
            final StringValue value = reqParms.getParameterValue(name);
            if (!value.isEmpty()) {
                parms.put(name, value.toString());
            }
        }

        /*
         * Leave no trace, clear all parameters.
         */
        this.getPageParameters().clearNamed();

        // TODO: how to clear /path variant of OAuth request?

        /*
         * Perform the callback.
         */
        final OAuthUserInfo userInfo;
        try {
            userInfo = plugin.onCallBack(parms);
        } catch (IOException | OAuthPluginException e) {
            throw new SpException(e.getMessage());
        }

        //
        if (userInfo == null) {
            LOGGER.error("{}: no userinfo.", logPfx);
            return Boolean.FALSE;
        }

        final String userid = userInfo.getUserId();
        final String email = userInfo.getEmail();

        if (userid == null && email == null) {
            LOGGER.error("{}: no userid or email in userinfo.", logPfx);
            return Boolean.FALSE;
        }

        final User authUser;

        if (userid == null) {
            final UserEmail userEmail = ServiceContext.getDaoContext()
                    .getUserEmailDao().findByEmail(email);
            if (userEmail == null) {
                LOGGER.warn("{}: email [{}]: not found.", logPfx, email);
                return Boolean.FALSE;
            }
            authUser = userEmail.getUser();

        } else if (email == null) {
            authUser = ServiceContext.getDaoContext().getUserDao()
                    .findActiveUserByUserId(userid);
            if (authUser == null) {
                LOGGER.warn("{}: user [{}]: not found.", logPfx, userid);
                return Boolean.FALSE;
            }
        } else {
            return Boolean.FALSE;
        }

        if (authUser.getDeleted().booleanValue()
                || authUser.getDisabledPrintIn().booleanValue()) {
            LOGGER.warn("{}: user [{}]: deleted or disabled.", logPfx,
                    authUser.getUserId());
            return Boolean.FALSE;
        }

        /*
         * Yes, we are authenticated.
         */
        final SpSession session = SpSession.get();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("{}: user [{}]: OK [session: {}]", logPfx,
                    authUser.getUserId(), session.getId());
        }

        session.setUser(authUser);

        /*
         * Pass the WebAppParmEnum.SP_LOGIN_OAUTH.parm() so JavaScript can act
         * upon it.
         */
        this.getPageParameters().add(WebAppParmEnum.SP_LOGIN_OAUTH.parm(),
                oauthProvider);

        return Boolean.TRUE;
    }

    /**
     * Check if a {@link OneTimeAuthToken} is present and, when valid,
     * authenticates by putting the {@link User} in the {@link SpSession}.
     *
     * @param parameters
     *            The {@link PageParameters}.
     */
    private void checkOneTimeAuthToken(final PageParameters parameters) {

        final String token =
                this.getParmValue(parameters, false, PAGE_PARM_AUTH_TOKEN);

        if (token == null) {
            return;
        }

        final String useridRaw = this.getParmValue(parameters, false,
                PAGE_PARM_AUTH_TOKEN_USERID);

        if (useridRaw == null) {
            return;
        }

        final String userid = UserAliasList.instance().getUserName(useridRaw);

        final User sessionUser = SpSession.get().getUser();

        if (sessionUser != null && sessionUser.getUserId().equals(userid)) {
            return;
        }

        final long msecExpiry = ConfigManager.instance()
                .getConfigLong(Key.WEB_LOGIN_TTP_TOKEN_EXPIRY_MSECS);

        if (!OneTimeAuthToken.isTokenValid(useridRaw, token, msecExpiry)) {
            final String msg = localized("msg-authtoken-denied",
                    formatUserId(useridRaw, userid));
            AdminPublisher.instance().publish(PubTopicEnum.USER,
                    PubLevelEnum.WARN, msg);
            LOGGER.warn(msg);
            return;
        }

        final User authUser = ServiceContext.getDaoContext().getUserDao()
                .findActiveUserByUserId(userid);

        if (authUser == null) {

            final String msg = localized("msg-authtoken-user-not-found",
                    formatUserId(useridRaw, userid));
            AdminPublisher.instance().publish(PubTopicEnum.USER,
                    PubLevelEnum.WARN, msg);
            LOGGER.warn(msg);

            return;
        }

        try {
            USER_SERVICE.lazyUserHomeDir(authUser);
        } catch (IOException e) {
            final String msg =
                    String.format("User [%s] inbox could not be created: %s",
                            authUser.getUserId(), e.getMessage());
            AdminPublisher.instance().publish(PubTopicEnum.USER,
                    PubLevelEnum.ERROR, msg);
            LOGGER.error(msg);
            return;
        }

        /*
         * Yes, we are authenticated, and no exceptions.
         */
        SpSession.get().setUser(authUser, true);

        final String msg = localized("msg-authtoken-accepted",
                formatUserId(useridRaw, userid));
        AdminPublisher.instance().publish(PubTopicEnum.USER, PubLevelEnum.INFO,
                msg);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(String.format("User %s authenticated with token: %s",
                    formatUserId(useridRaw, userid), token));
        } else if (LOGGER.isInfoEnabled()) {
            LOGGER.info(msg);
        }
    }

    /** */
    private void discloseAll() {
        final MarkupHelper helper = new MarkupHelper(this);
        helper.discloseLabel(WICKET_ID_FILE_UPLOAD_FONTFAMILY_OPT);
    }

    /**
     *
     * @param parameters
     *            The {@link PageParameters}.
     */
    public WebAppUser(final PageParameters parameters) {

        super(parameters);

        checkInternetAccess(IConfigProp.Key.WEBAPP_INTERNET_USER_ENABLE);

        if (isWebAppCountExceeded(parameters)) {
            discloseAll();
            this.setWebAppCountExceededResponse();
            return;
        }

        final MutableObject<OAuthProviderEnum> mutableProvider =
                new MutableObject<>();

        final Boolean oauth = checkOAuthToken(mutableProvider);

        if (oauth == null) {

            checkOneTimeAuthToken(parameters);

        } else if (!oauth.booleanValue()) {

            discloseAll();

            final PageParameters parms = new PageParameters();

            parms.set(WebAppParmEnum.SP_APP.parm(), this.getWebAppType());
            parms.set(WebAppParmEnum.SP_LOGIN_OAUTH.parm(),
                    mutableProvider.toString());

            setResponsePage(WebAppOAuthMsg.class, parms);
            return;
        }

        //
        final String appTitle = getWebAppTitle(null);

        add(new Label("app-title", appTitle));

        addZeroPagePanel(WebAppTypeEnum.USER);

        webPrintMarkup();

        addFileDownloadApiPanel();

        //
        final List<UploadNextButton> nextButtons = new ArrayList<>();

        nextButtons.add(UploadNextButton.INBOX);
        nextButtons.add(UploadNextButton.PDF);
        nextButtons.add(UploadNextButton.PRINT);

        add(new UploadNextButtonView("next-buttons", nextButtons));

    }

    @Override
    protected String getSpecializedCssFileName() {
        return "jquery.savapage-user.css";
    }

    @Override
    protected String getSpecializedJsFileName() {
        return "jquery.savapage-user.js";
    }

    @Override
    protected Set<JavaScriptLibrary> getJavaScriptToRender() {

        final EnumSet<JavaScriptLibrary> libs =
                EnumSet.allOf(JavaScriptLibrary.class);

        return libs;
    }

    /**
     *
     * @return the
     */
    private static String getHtmlAcceptString() {

        final StringBuilder html = new StringBuilder();

        for (final String ext : WebPrintHelper
                .getSupportedFileExtensions(true)) {
            if (html.length() > 0) {
                html.append(",");
            }
            html.append(ext);
        }

        return html.toString();
    }

    /**
     * Creates the markup for the Web Print File upload.
     */
    private void webPrintMarkup() {

        final Label fileUploadField = new Label("fileUpload");
        fileUploadField.add(new AttributeModifier(MarkupHelper.ATTR_ACCEPT,
                getHtmlAcceptString()));
        add(fileUploadField);

        //
        final Set<DocContentTypeEnum> excludeTypes =
                WebPrintHelper.getExcludeTypes();

        if (excludeTypes.contains(DocContentTypeEnum.TXT)) {
            final MarkupHelper helper = new MarkupHelper(this);
            helper.discloseLabel(WICKET_ID_FILE_UPLOAD_FONTFAMILY_OPT);
        } else {
            final FontOptionsPanel fontOptionsPanel =
                    new FontOptionsPanel(WICKET_ID_FILE_UPLOAD_FONTFAMILY_OPT);
            fontOptionsPanel.populate(ConfigManager
                    .getConfigFontFamily(Key.REPORTS_PDF_INTERNAL_FONT_FAMILY));
            add(fontOptionsPanel);
        }
    }
}
