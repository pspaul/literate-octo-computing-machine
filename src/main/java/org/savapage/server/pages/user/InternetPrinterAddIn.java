/*
 * This file is part of the SavaPage project <https://savapage.org>.
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
package org.savapage.server.pages.user;

import java.net.URISyntaxException;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dao.enums.ReservedIppQueueEnum;
import org.savapage.core.dao.enums.UserAttrEnum;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserService;
import org.savapage.server.SpSession;
import org.savapage.server.ipp.IppPrintServerUrlParms;
import org.savapage.server.pages.AbstractAuthPage;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.webapp.WebAppTypeEnum;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class InternetPrinterAddIn extends AbstractAuthPage {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
    *
    */
    private static final UserService USER_SERVICE =
            ServiceContext.getServiceFactory().getUserService();

    /**
     *
     */
    public InternetPrinterAddIn(final PageParameters parameters) {

        super(parameters);

        if (isAuthErrorHandled()) {
            return;
        }

        /**
         * If this page is displayed in the Admin WebApp context, we check the
         * admin authentication (including the need for a valid Membership).
         */
        if (this.getSessionWebAppType() == WebAppTypeEnum.ADMIN) {
            checkAdminAuthorization();
        }

        final MarkupHelper helper = new MarkupHelper(this);

        final UserDao userDao = ServiceContext.getDaoContext().getUserDao();

        /*
         * Get the most recent situation (do not depend on cached User in
         * Session).
         */
        final org.savapage.core.jpa.User user =
                userDao.findById(SpSession.get().getUser().getId());

        final String userNumber = USER_SERVICE.getPrimaryIdNumber(user);
        final String userUuid =
                USER_SERVICE.getUserAttrValue(user, UserAttrEnum.UUID);
        final String uriBase = ConfigManager.instance()
                .getConfigValue(Key.IPP_INTERNET_PRINTER_URI_BASE);

        String text;

        if (StringUtils.isBlank(userNumber) || StringUtils.isBlank(userUuid)
                || StringUtils.isBlank(uriBase)) {
            text = localized("no-url-available");
        } else {
            final IppPrintServerUrlParms urlParms =
                    new IppPrintServerUrlParms(uriBase,
                            ReservedIppQueueEnum.IPP_PRINT_INTERNET
                                    .getUrlPath(),
                            userNumber, UUID.fromString(userUuid));
            try {
                text = urlParms.asUri().toString();
            } catch (URISyntaxException e) {
                text = e.getMessage();
            }
        }

        helper.addLabel("internet-printer-uri-cups", text);
        helper.addLabel("internet-printer-uri-windows",
                StringUtils.replace(text, "ipps://", "https://"));
    }

    @Override
    protected boolean needMembership() {
        return false;
    }
}
