/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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
package org.savapage.server.pages.user;

import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.services.AccessControlService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.InetUtils;
import org.savapage.server.SpSession;
import org.savapage.server.pages.CommunityStatusFooterPanel;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class Main extends AbstractUserPage {

    private static final long serialVersionUID = 1L;

    private static final AccessControlService ACCESSCONTROL_SERVICE =
            ServiceContext.getServiceFactory().getAccessControlService();

    /**
     *
     */
    public Main() {

        boolean isUpload =
                (ConfigManager.isWebPrintEnabled() && InetUtils
                        .isIp4AddrInCidrRanges(
                                ConfigManager.instance().getConfigValue(
                                        Key.WEB_PRINT_LIMIT_IP_ADDRESSES),
                                getClientIpAddr()));

        addVisible(isUpload, "button-upload", localized("button-upload"));

        //
        final boolean isPrintDelegate;

        if (ConfigManager.instance().isConfigValue(
                Key.PROXY_PRINT_DELEGATE_ENABLE)) {

            isPrintDelegate =
                    ACCESSCONTROL_SERVICE.isAuthorized(SpSession.get()
                            .getUser(), ACLRoleEnum.PRINT_DELEGATE);
        } else {
            isPrintDelegate = false;
        }

        addVisible(isPrintDelegate, "button-print-delegation", "Delegation");

        //
        add(new CommunityStatusFooterPanel("community-status-footer-panel"));
    }
}
