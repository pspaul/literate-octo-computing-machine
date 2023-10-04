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
package org.savapage.server.pages.user;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dao.enums.ReservedIppQueueEnum;
import org.savapage.core.dao.enums.UserAttrEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.i18n.PhraseEnum;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserService;
import org.savapage.server.ipp.IppPrintServerUrlParms;
import org.savapage.server.pages.AbstractAuthPage;
import org.savapage.server.pages.CopyToClipBoardPanel;
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

    /** */
    private static final String WID_MAC_OS_ADDRESS = "macos-address";

    /** */
    private static final String WID_COPY_SUFFIX = "-copy";

    /**
     * @param parameters
     *            Page parameters.
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

        this.addLabelAndClipBoardCopy(helper, "internet-printer-uri-cups",
                text);

        this.addLabelAndClipBoardCopy(helper, "internet-printer-uri-windows",
                StringUtils.replace(text, "ipps://", "https://"));

        helper.addLabel("windows-driver-msg", PhraseEnum.WINDOWS_DRIVER_MSG);

        //
        if (urlParms == null) {
            helper.discloseLabel(WID_MAC_OS_ADDRESS);
            helper.discloseLabel(WID_CHROME_ADDRESS);
            helper.discloseLabel(WID_FDROID_URL);
        } else {

            final URI uriBaseObj = URI.create(uriBase);

            helper.addLabel("macos-address-prompt",
                    NounEnum.ADDRESS.uiText(getLocale()));

            final StringBuilder addr = new StringBuilder();
            final int port = uriBaseObj.getPort();

            addr.append(uriBaseObj.getHost());

            if (port < 0) {
                if (uriBaseObj.getScheme().endsWith("s")) {
                    addr.append(":443");
                }
            } else {
                addr.append(":").append(port);
            }

            this.addLabelAndClipBoardCopy(helper, WID_MAC_OS_ADDRESS,
                    addr.toString());

            helper.addLabel("macos-protocol-prompt",
                    NounEnum.PROTOCOL.uiText(getLocale()));

            helper.addLabel("macos-queue-prompt",
                    NounEnum.QUEUE.uiText(getLocale()));

            try {
                this.addLabelAndClipBoardCopy(helper, "macos-queue", StringUtils
                        .removeStart(urlParms.asUri().getPath(), "/"));
            } catch (URISyntaxException e) {
                // no code intended
            }

            //
            helper.addLabel("chrome-address-prompt",
                    NounEnum.ADDRESS.uiText(getLocale()));

            this.addLabelAndClipBoardCopy(helper, WID_CHROME_ADDRESS,
                    StringUtils.removeStart(
                            StringUtils.removeStart(uriBase, "https://"),
                            "http://"));

            helper.addLabel("chrome-protocol-prompt",
                    NounEnum.PROTOCOL.uiText(getLocale()));

            helper.addLabel("chrome-queue-prompt",
                    NounEnum.QUEUE.uiText(getLocale()));

            try {
                this.addLabelAndClipBoardCopy(helper, "chrome-queue",
                        StringUtils.removeStart(urlParms.asUri().getPath(),
                                "/"));
            } catch (URISyntaxException e) {
                // no code intended
            }
            helper.addLabel("chrome-manufacturer-prompt",
                    NounEnum.MANUFACTURER.uiText(getLocale()));
            helper.addLabel("chrome-model-prompt",
                    NounEnum.MODEL.uiText(getLocale()));

            //
            this.addLabelAndClipBoardCopy(helper, WID_FDROID_URL,
                    uriBase.replaceFirst("ipp", "http"));

            helper.addLabel("fdroid-user-prompt", NounEnum.USER);
            this.addLabelAndClipBoardCopy(helper, "fdroid-user",
                    user.getUserId());

            this.addLabelAndClipBoardCopy(helper, "fdroid-uuid", userUuid);
        }
    }

    /**
     * @param helper
     *            Markup helper.
     * @param wid
     *            Wicket ID.
     * @param text
     *            Label text
     */
    private void addLabelAndClipBoardCopy(final MarkupHelper helper,
            final String wid, final String text) {

        final Label labelToCopy = helper.addLabel(wid, text);
        final String panelId = wid + WID_COPY_SUFFIX;

        final String uuid = UUID.randomUUID().toString();

        MarkupHelper.modifyLabelAttr(labelToCopy, MarkupHelper.ATTR_ID, uuid);

        final CopyToClipBoardPanel panel = new CopyToClipBoardPanel(panelId);
        panel.populate(uuid);
        add(panel);
    }

    @Override
    protected boolean needMembership() {
        return false;
    }
}
