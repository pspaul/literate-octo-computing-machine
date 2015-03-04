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
package org.savapage.server.pages.admin;

import org.savapage.core.config.IConfigProp;
import org.savapage.core.dao.DeviceAttrDao;
import org.savapage.core.dao.helpers.DeviceAttrEnum;
import org.savapage.core.dao.helpers.ProxyPrintAuthModeEnum;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class PageDevice extends AbstractAdminPage {

    private static final long serialVersionUID = 1L;

    /**
     *
     */
    public PageDevice() {

        String attrName;
        String wicketIdBase;

        //
        labelledCheckbox("user-auth-mode",
                DeviceAttrEnum.AUTH_MODE_IS_CUSTOM.getDbName(), false);
        //
        labelledCheckbox("user-auth-mode-name",
                DeviceAttrEnum.AUTH_MODE_NAME.getDbName(), false);
        labelledCheckbox("user-auth-mode-id",
                DeviceAttrEnum.AUTH_MODE_ID.getDbName(), false);
        labelledCheckbox("user-auth-mode-card-local",
                DeviceAttrEnum.AUTH_MODE_CARD_LOCAL.getDbName(), false);
        labelledCheckbox("user-auth-mode-card-network",
                DeviceAttrEnum.AUTH_MODE_CARD_IP.getDbName(), false);

        //
        labelledText("terminal-auto-logout",
                DeviceAttrEnum.WEBAPP_USER_MAX_IDLE_SECS.getDbName(), "0");

        //
        labelledCheckbox("user-auth-mode-id-pin",
                DeviceAttrEnum.AUTH_MODE_ID_PIN_REQ.getDbName(), false);
        labelledCheckbox("user-auth-mode-id-mask",
                DeviceAttrEnum.AUTH_MODE_ID_IS_MASKED.getDbName(), false);
        //
        labelledCheckbox("user-auth-mode-card-pin",
                DeviceAttrEnum.AUTH_MODE_CARD_PIN_REQ.getDbName(), false);
        labelledCheckbox("user-auth-mode-card-self-assoc",
                DeviceAttrEnum.AUTH_MODE_CARD_SELF_ASSOC.getDbName(), false);

        //
        attrName = DeviceAttrEnum.CARD_NUMBER_FORMAT.getDbName();
        wicketIdBase = "card-format";

        labelledRadio(wicketIdBase, "-hex", attrName,
                DeviceAttrDao.VALUE_CARD_NUMBER_HEX, true);
        labelledRadio(wicketIdBase, "-dec", attrName,
                DeviceAttrDao.VALUE_CARD_NUMBER_DEC, false);

        //
        attrName = DeviceAttrEnum.CARD_NUMBER_FIRST_BYTE.getDbName();
        wicketIdBase = "card-format";

        labelledRadio(wicketIdBase, "-lsb", attrName,
                DeviceAttrDao.VALUE_CARD_NUMBER_LSB, true);
        labelledRadio(wicketIdBase, "-msb", attrName,
                DeviceAttrDao.VALUE_CARD_NUMBER_MSB, false);

        //
        attrName = DeviceAttrEnum.PROXY_PRINT_AUTH_MODE.getDbName();
        wicketIdBase = attrName;

        for (final ProxyPrintAuthModeEnum value : ProxyPrintAuthModeEnum
                .values()) {
            labelledRadio(wicketIdBase, "-" + value.toString().toLowerCase(),
                    attrName, value.toString(), false);
        }

        //
        attrName = DeviceAttrEnum.AUTH_MODE_DEFAULT.getDbName();
        wicketIdBase = "user-" + attrName;

        labelledRadio(wicketIdBase, "-user", attrName,
                IConfigProp.AUTH_MODE_V_NAME, false);
        labelledRadio(wicketIdBase, "-id", attrName,
                IConfigProp.AUTH_MODE_V_ID, false);
        labelledRadio(wicketIdBase, "-card-local", attrName,
                IConfigProp.AUTH_MODE_V_CARD_LOCAL, false);
        labelledRadio(wicketIdBase, "-card-network", attrName,
                IConfigProp.AUTH_MODE_V_CARD_IP, false);

    }

}
