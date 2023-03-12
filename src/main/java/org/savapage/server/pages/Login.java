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
package org.savapage.server.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.WebAppTypeEnum;
import org.savapage.core.dao.DeviceDao;
import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.dao.enums.DeviceTypeEnum;
import org.savapage.core.dao.enums.ExternalSupplierEnum;
import org.savapage.core.i18n.LabelEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.i18n.PhraseEnum;
import org.savapage.core.i18n.SystemModeEnum;
import org.savapage.core.jpa.Device;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.UserAuth;
import org.savapage.core.util.InetUtils;
import org.savapage.core.util.LocaleHelper;
import org.savapage.ext.oauth.OAuthClientPlugin;
import org.savapage.ext.oauth.OAuthProviderEnum;
import org.savapage.ext.telegram.TelegramHelper;
import org.savapage.server.WebApp;
import org.savapage.server.WebAppParmEnum;
import org.savapage.server.ext.ServerPluginHelper;
import org.savapage.server.ext.ServerPluginManager;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.session.SpSession;

/**
 * Note that this Page is not extended from Page.
 *
 * @author Rijk Ravestein
 *
 */
public final class Login extends AbstractPage {

    private static final long serialVersionUID = 1L;

    /** */
    private static final String WID_MAINTENANCE_HEADER = "maintenance-header";
    /** */
    private static final String WID_MAINTENANCE_BODY = "maintenance-body";

    /** */
    private static final String WID_RESET_HEADER = "reset-header";
    /** */
    private static final String WID_RESET_BODY = "reset-body";

    /**
     *
     * @param parameters
     *            The page parameters.
     */
    public Login(final PageParameters parameters) {

        super(parameters);

        final MarkupHelper helper = new MarkupHelper(this);
        final SpSession session = SpSession.get();

        final Set<Locale> availableLocales = LocaleHelper.getI18nAvailable();

        if (availableLocales.size() == 1) {
            session.setLocale(availableLocales.iterator().next());
        }

        helper.encloseLabel("button-lang", getString("button-lang"),
                availableLocales.size() > 1);

        helper.addLabel("label-email-1", NounEnum.EMAIL);
        helper.addLabel("label-email-2", NounEnum.EMAIL);

        //
        helper.addLabel("header-2-step", LabelEnum.TWO_STEP_VERIFICATION);

        final String ttKey;
        if (TelegramHelper.isTOTPEnabled()) {
            ttKey = "tooltip-totp-telegram";
        } else {
            ttKey = "tooltip-totp";
        }
        final TooltipPanel tooltip = new TooltipPanel("tooltip-totp");
        tooltip.populate(helper.localized(ttKey), true);
        add(tooltip);

        helper.encloseLabel("btn-login-totp-send",
                HtmlButtonEnum.SEND.uiText(getLocale()),
                this.getWebAppTypeEnum(parameters) == WebAppTypeEnum.USER);

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
            case PRINTSITE:
                loginDescript = localized("login-descript-role",
                        ACLRoleEnum.PRINT_SITE_OPERATOR.uiText(getLocale()));
                break;
            case JOBTICKETS:
                loginDescript = localized("login-descript-role",
                        ACLRoleEnum.JOB_TICKET_OPERATOR.uiText(getLocale()));
                break;
            case MAILTICKETS:
                loginDescript = localized("login-descript-role",
                        ACLRoleEnum.MAIL_TICKET_OPERATOR.uiText(getLocale()));
                break;
            case POS:
                loginDescript = localized("login-descript-role",
                        ACLRoleEnum.WEB_CASHIER.uiText(getLocale()));
                break;
            case PAYMENT:
                loginDescript = localized("login-descript-user-payment");
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

        final String clientIpAddr = this.getClientIpAddr();

        final Device terminal = deviceDao.findByHostDeviceType(clientIpAddr,
                DeviceTypeEnum.TERMINAL);

        final UserAuth userAuth = new UserAuth(terminal, null, webAppType,
                InetUtils.isPublicAddress(clientIpAddr));

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

        if (ConfigManager.getSystemMode() == SystemModeEnum.MAINTENANCE) {
            helper.addLabel(WID_MAINTENANCE_HEADER,
                    localized("maintenance-header"));
            helper.addLabel(WID_MAINTENANCE_BODY,
                    localized("maintenance-body"));
        } else {
            helper.discloseLabel(WID_MAINTENANCE_HEADER);
        }

        final boolean anotherSessionActive = session.getAuthWebAppCount() != 0;
        final String btnTextReset = HtmlButtonEnum.RESET.uiText(getLocale());
        helper.encloseLabel("button-reset", btnTextReset, anotherSessionActive);
        if (anotherSessionActive) {
            helper.addLabel(WID_RESET_HEADER,
                    PhraseEnum.ANOTHER_BROWSER_SESSION_ACTIVE
                            .uiText(getLocale()));
            helper.addLabel(WID_RESET_BODY,
                    localized("reset-body", btnTextReset));
        } else {
            helper.discloseLabel(WID_RESET_HEADER);
        }
    }

    /**
     *
     * @param localLoginRestricted
     *            If {@code true}, login is restricted to local methods.
     */
    private void addOAuthButtons(final boolean localLoginRestricted) {

        final List<OAuthClientPlugin> pluginList = new ArrayList<>();
        final List<Pair<OAuthProviderEnum, String>> oauthList =
                new ArrayList<>();

        final ServerPluginManager mgr = WebApp.get().getPluginManager();

        if (!localLoginRestricted) {

            for (final Entry<OAuthProviderEnum, //
                    Map<String, OAuthClientPlugin>> entry : mgr
                            .getOAuthClientPlugins().entrySet()) {

                final OAuthProviderEnum provider = entry.getKey();

                for (final Entry<String, OAuthClientPlugin> entry2 : entry
                        .getValue().entrySet()) {
                    final String instanceId = entry2.getKey();
                    oauthList.add(new ImmutablePair<OAuthProviderEnum, String>(
                            provider, instanceId));
                    pluginList.add(entry2.getValue());
                }
            }
        }

        add(new PropertyListView<OAuthClientPlugin>("ext-supplier-icons",
                pluginList) {

            private static final long serialVersionUID = 1L;

            private static final String OAUTH_PROVIDER_ICON_FORMAT =
                    ".ui-icon-ext-oauth-provider-%s%s:after { " + "background: "
                            + "url(%s) " + "50%% 50%% no-repeat; "
                            + "background-size: 22px 22px; "
                            + "padding-left: 15px; "
                            + "-webkit-border-radius: 0 !important; "
                            + "border-radius: 0 !important; }";

            @Override
            protected void
                    populateItem(final ListItem<OAuthClientPlugin> item) {

                final OAuthClientPlugin plugin = item.getModelObject();

                @SuppressWarnings("unused")
                final ExternalSupplierEnum supplier =
                        ServerPluginHelper.getEnum(plugin.getProvider());

                final Label label = new Label("ext-supplier-icon",
                        String.format(OAUTH_PROVIDER_ICON_FORMAT,
                                plugin.getProvider().toString().toLowerCase(),
                                StringUtils
                                        .defaultString(plugin.getInstanceId()),
                                mgr.getOAuthClientIconPath(plugin)));

                label.setEscapeModelStrings(false);
                item.add(label);
            }
        });

        add(new PropertyListView<Pair<OAuthProviderEnum, String>>(
                "oauth-buttons", oauthList) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(
                    final ListItem<Pair<OAuthProviderEnum, String>> item) {

                final Pair<OAuthProviderEnum, String> pair =
                        item.getModelObject();

                final OAuthProviderEnum provider = pair.getLeft();
                final String instanceId = pair.getRight();

                final Label label = new Label("oauth-button", "&nbsp;");

                MarkupHelper.appendLabelAttr(label, MarkupHelper.ATTR_CLASS,
                        String.format("ui-icon-ext-oauth-provider-%s%s",
                                provider.toString().toLowerCase(),
                                StringUtils.defaultString(instanceId)));

                MarkupHelper.modifyLabelAttr(label,
                        MarkupHelper.ATTR_DATA_SAVAPAGE, provider.toString());

                final StringBuilder title = new StringBuilder();
                title.append(provider.uiText());

                if (instanceId != null) {
                    MarkupHelper.modifyLabelAttr(label,
                            MarkupHelper.ATTR_DATA_SAVAPAGE_TYPE, instanceId);
                    title.append(" | ").append(instanceId);
                }

                MarkupHelper.modifyLabelAttr(label, MarkupHelper.ATTR_TITLE,
                        title.toString());

                label.setEscapeModelStrings(false);
                item.add(label);
            }

        });
    }
}
