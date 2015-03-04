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
package org.savapage.server.pages;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.DeviceDao;
import org.savapage.core.dao.helpers.DeviceTypeEnum;
import org.savapage.core.jpa.Device;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.UserAuth;

/**
 * Note that this Page is not extended from Page.
 *
 * @author Datraverse B.V.
 *
 */
public class Login extends AbstractPage {

    private static final long serialVersionUID = 1L;

    /**
     *
     */
    public Login() {

        // this.openServiceContext();

        add(new Label("title", localized("title",
                CommunityDictEnum.SAVAPAGE.getWord())));
        add(new Label("title-assoc", CommunityDictEnum.SAVAPAGE.getWord()));

        String key;
        switch (this.getWebAppType()) {
        case ADMIN:
            key = "login-descript-admin";
            break;
        case POS:
            key = "login-descript-admin";
            break;
        default:
            key = "login-descript-user";
            break;
        }

        add(new Label("login-descript", localized(key)));

        final ConfigManager cm = ConfigManager.instance();

        final DeviceDao deviceDao =
                ServiceContext.getDaoContext().getDeviceDao();

        final Device terminal =
                deviceDao.findByHostDeviceType(this.getClientIpAddr(),
                        DeviceTypeEnum.TERMINAL);

        final UserAuth userAuth =
                new UserAuth(terminal, null, this.isAdminRoleContext());

        /*
         *
         */
        Label label = new Label("login-id-number");

        String inputType;
        if (userAuth.isAuthIdMasked()) {
            inputType = "password";
        } else {
            inputType = "text";
        }
        label.add(new AttributeModifier("type", inputType));
        add(label);

        /*
         *
         */
        addVisible(userAuth.isAuthIdPinReq(), "login-id-pin", "");
    }

}
