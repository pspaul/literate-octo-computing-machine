/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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
package org.savapage.server.pages;

import org.apache.commons.lang3.EnumUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.IRequestParameters;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.dao.DeviceDao;
import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.dao.enums.DeviceTypeEnum;
import org.savapage.core.jpa.Device;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.UserAuth;
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
     */
    public Login() {

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

        final WebAppTypeEnum webAppType = EnumUtils.getEnum(
                WebAppTypeEnum.class, parms.getParameterValue("webAppType")
                        .toString(WebAppTypeEnum.UNDEFINED.toString()));

        final HtmlInjectComponent htmlInject = new HtmlInjectComponent(
                "login-inject", webAppType, HtmlInjectEnum.LOGIN);

        MarkupHelper helper = new MarkupHelper(this);

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
    }
}
