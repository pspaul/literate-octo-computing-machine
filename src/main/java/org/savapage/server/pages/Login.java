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
package org.savapage.server.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.EnumUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.DeviceDao;
import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.dao.enums.DeviceTypeEnum;
import org.savapage.core.dao.enums.ExternalSupplierEnum;
import org.savapage.core.jpa.Device;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.UserAuth;
import org.savapage.core.util.LocaleHelper;
import org.savapage.ext.oauth.OAuthProviderEnum;
import org.savapage.server.WebApp;
import org.savapage.server.WebAppParmEnum;
import org.savapage.server.ext.ServerPluginHelper;
import org.savapage.server.ext.ServerPluginManager;
import org.savapage.server.session.SpSession;
import org.savapage.server.webapp.WebAppTypeEnum;

/**
 * Note that this Page is not extended from Page.
 *
 * @author Rijk Ravestein
 *
 */
public final class Login extends AbstractPage {

    private static final long serialVersionUID = 1L;

    /**
     *
     * @param parameters
     *            The page parameters.
     */
    public Login(final PageParameters parameters) {

        super(parameters);

        final MarkupHelper helper = new MarkupHelper(this);

        final List<Locale> availableLocales =
                LocaleHelper.getAvailableLanguages();

        if (availableLocales.size() == 1) {
            SpSession.get().setLocale(availableLocales.get(0));
        }

        helper.encloseLabel("button-lang", getString("button-lang"),
                availableLocales.size() > 1);

        //
        add(new Label("title",
                localized("title", CommunityDictEnum.SAVAPAGE.getWord())));

        add(new Label("title-assoc", CommunityDictEnum.SAVAPAGE.getWord()));

        final String loginDescript;

        /*
         * At this point we can NOT use the authenticated Web App Type from the
         * session, since this is the first Web App in the browser, or a new
         * browser tab with another Web App Type. So, we use a request parameter
         * to determine the Web App Type.
         */
        final IRequestParameters parms =
                getRequestCycle().getRequest().getPostParameters();

        final WebAppTypeEnum webAppType =
                EnumUtils.getEnum(WebAppTypeEnum.class,
                        parms.getParameterValue(POST_PARM_WEBAPPTYPE)
                                .toString(WebAppTypeEnum.UNDEFINED.toString()));

        final HtmlInjectComponent htmlInject = new HtmlInjectComponent(
                "login-inject", webAppType, HtmlInjectEnum.LOGIN);

        if (htmlInject.isInjectAvailable()) {
            add(htmlInject);
            loginDescript = null;
        } else {
            helper.discloseLabel("login-inject");
            switch (webAppType) {
            case ADMIN:
                loginDescript = localized("login-descript-admin");
                break;
            case JOBTICKETS:
                loginDescript = localized("login-descript-role",
                        ACLRoleEnum.JOB_TICKET_OPERATOR.uiText(getLocale()));
                break;
            case POS:
                loginDescript = localized("login-descript-role",
                        ACLRoleEnum.WEB_CASHIER.uiText(getLocale()));
                break;
            default:
                loginDescript = localized("login-descript-user");
                break;
            }
        }

        helper.encloseLabel("login-descript", loginDescript,
                loginDescript != null);

        final DeviceDao deviceDao =
                ServiceContext.getDaoContext().getDeviceDao();

        final Device terminal = deviceDao.findByHostDeviceType(
                this.getClientIpAddr(), DeviceTypeEnum.TERMINAL);

        final UserAuth userAuth = new UserAuth(terminal, null,
                this.getSessionWebAppType() == WebAppTypeEnum.ADMIN);

        //
        Label label = new Label("login-id-number");

        String inputType;
        if (userAuth.isAuthIdMasked()) {
            inputType = "password";
        } else {
            inputType = "text";
        }
        label.add(new AttributeModifier("type", inputType));
        add(label);

        //
        addVisible(userAuth.isAuthIdPinReq(), "login-id-pin", "");

        // For now, restrict OAuth to User Web App only...
        final boolean localLoginRestricted = webAppType != WebAppTypeEnum.USER
                || parms.getParameterValue(WebAppParmEnum.SP_LOGIN_LOCAL.parm())
                        .toString() != null;

        addOAuthButtons(localLoginRestricted);

        //
        if (ConfigManager.isSysMaintenance()) {
            helper.addLabel("maintenance-header",
                    localized("maintenance-header"));
            helper.addLabel("maintenance-body", localized("maintenance-body"));
        } else {
            helper.discloseLabel("maintenance-header");
        }
    }

    /**
     *
     * @param localLoginRestricted
     *            If {@code true}, login is restricted to local methods.
     */
    private void addOAuthButtons(final boolean localLoginRestricted) {

        final List<OAuthProviderEnum> list = new ArrayList<>();

        if (!localLoginRestricted) {
            final ServerPluginManager mgr = WebApp.get().getPluginManager();

            for (final OAuthProviderEnum value : mgr.getOAuthClientPlugins()
                    .keySet()) {
                list.add(value);
            }
        }

        add(new PropertyListView<OAuthProviderEnum>("ext-supplier-icons",
                list) {

            private static final long serialVersionUID = 1L;

            private static final String OAUTH_PROVIDER_ICON_FORMAT =
                    ".ui-icon-ext-oauth-provider-%s:after { " + "background: "
                            + "url(%s) " + "50%% 50%% no-repeat; "
                            + "background-size: 22px 22px; "
                            + "padding-left: 15px; "
                            + "-webkit-border-radius: 0 !important; "
                            + "border-radius: 0 !important; }";

            @Override
            protected void
                    populateItem(final ListItem<OAuthProviderEnum> item) {

                final OAuthProviderEnum provider = item.getModelObject();
                final ExternalSupplierEnum supplier =
                        ServerPluginHelper.getEnum(provider);

                final Label label = new Label("ext-supplier-icon",
                        String.format(OAUTH_PROVIDER_ICON_FORMAT,
                                provider.toString().toLowerCase(), WebApp
                                        .getExtSupplierEnumImgUrl(supplier)));

                label.setEscapeModelStrings(false);
                item.add(label);
            }
        });

        add(new PropertyListView<OAuthProviderEnum>("oauth-buttons", list) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void
                    populateItem(final ListItem<OAuthProviderEnum> item) {

                final OAuthProviderEnum provider = item.getModelObject();

                final Label label = new Label("oauth-button", "&nbsp;");

                MarkupHelper.appendLabelAttr(label, MarkupHelper.ATTR_CLASS,
                        String.format("ui-icon-ext-oauth-provider-%s",
                                provider.toString().toLowerCase()));

                MarkupHelper.modifyLabelAttr(label, MarkupHelper.ATTR_TITLE,
                        provider.uiText());

                MarkupHelper.modifyLabelAttr(label,
                        MarkupHelper.ATTR_DATA_SAVAPAGE, provider.toString());

                label.setEscapeModelStrings(false);
                item.add(label);
            }

        });
    }
}
