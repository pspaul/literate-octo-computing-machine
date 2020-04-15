/*
 * This file is part of the SavaPage project <https://savapage.org>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2020 Datraverse B.V. <info@datraverse.com>
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
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserService;
import org.savapage.server.ipp.IppPrintServerUrlParms;
import org.savapage.server.pages.AbstractAuthPage;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.session.SpSession;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class InternetPrinterAddIn extends AbstractAuthPage {

    /** */
    private static final long serialVersionUID = 1L;

    /** */
    private static final UserService USER_SERVICE =
            ServiceContext.getServiceFactory().getUserService();

    /** */
    private static final String WID_FDROID_URL = "fdroid-url";

    /** */
    private static final String WID_CHROME_ADDRESS = "chrome-address";

    /**
     *
     */
    public InternetPrinterAddIn(final PageParameters parameters) {

        super(parameters);

        final MarkupHelper helper = new MarkupHelper(this);

        final UserDao userDao = ServiceContext.getDaoContext().getUserDao();

        /*
         * Get the most recent situation (do not depend on cached User in
         * Session).
         */
        final org.savapage.core.jpa.User user =
                userDao.findById(SpSession.get().getUserDbKey());

        final String userNumber = USER_SERVICE.lazyAddUserPrimaryIdNumber(user);
        final String userUuid =
                USER_SERVICE.getUserAttrValue(user, UserAttrEnum.UUID);
        final String uriBase = ConfigManager.instance()
                .getConfigValue(Key.IPP_INTERNET_PRINTER_URI_BASE);

        IppPrintServerUrlParms urlParms = null;

        String text;

        if (StringUtils.isBlank(userNumber) || StringUtils.isBlank(userUuid)
                || StringUtils.isBlank(uriBase)) {
            text = localized("no-url-available");
        } else {
            final IppPrintServerUrlParms parms =
                    new IppPrintServerUrlParms(uriBase,
                            ReservedIppQueueEnum.IPP_PRINT_INTERNET
                                    .getUrlPath(),
                            userNumber, UUID.fromString(userUuid));
            try {
                text = parms.asUri().toString();
                urlParms = parms;
            } catch (URISyntaxException e) {
                text = e.getMessage();
            }
        }

        helper.addLabel("internet-printer-uri-cups", text);
        helper.addLabel("internet-printer-uri-windows",
                StringUtils.replace(text, "ipps://", "https://"));

        //
        if (urlParms == null) {
            helper.discloseLabel(WID_CHROME_ADDRESS);
            helper.discloseLabel(WID_FDROID_URL);
        } else {
            helper.addLabel("chrome-address-prompt",
                    NounEnum.ADDRESS.uiText(getLocale()));
            helper.addLabel(WID_CHROME_ADDRESS, StringUtils.removeStart(
                    StringUtils.removeStart(uriBase, "https://"), "http://"));

            helper.addLabel("chrome-protocol-prompt",
                    NounEnum.PROTOCOL.uiText(getLocale()));

            helper.addLabel("chrome-queue-prompt",
                    NounEnum.QUEUE.uiText(getLocale()));

            try {
                helper.addLabel("chrome-queue", StringUtils
                        .removeStart(urlParms.asUri().getPath(), "/"));
            } catch (URISyntaxException e) {
                //
            }
            helper.addLabel("chrome-manufacturer-prompt",
                    NounEnum.MANUFACTURER.uiText(getLocale()));
            helper.addLabel("chrome-model-prompt",
                    NounEnum.MODEL.uiText(getLocale()));

            //
            helper.addLabel(WID_FDROID_URL,
                    StringUtils.replaceFirst(uriBase, "ipp", "http"));
            helper.addLabel("fdroid-user-prompt", NounEnum.USER);
            helper.addLabel("fdroid-user", user.getUserId());
            helper.addLabel("fdroid-uuid", userUuid);
        }
    }

    @Override
    protected boolean needMembership() {
        return false;
    }
}
