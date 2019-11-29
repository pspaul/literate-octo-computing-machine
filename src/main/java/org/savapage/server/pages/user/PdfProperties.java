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
package org.savapage.server.pages.user;

import java.util.List;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.ACLPermissionEnum;
import org.savapage.core.dto.UserIdDto;
import org.savapage.core.services.AccessControlService;
import org.savapage.core.services.ServiceContext;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.session.SpSession;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class PdfProperties extends AbstractUserPage {

    private static final long serialVersionUID = 1L;

    /**
     *
     */
    private static final AccessControlService ACCESS_CONTROL_SERVICE =
            ServiceContext.getServiceFactory().getAccessControlService();

    /**
     *
     * @param parameters
     *            The parms.
     */
    public PdfProperties(final PageParameters parameters) {

        super(parameters);

        final MarkupHelper helper = new MarkupHelper(this);

        //
        helper.encloseLabel("pdf-ecoprint", "",
                ConfigManager.isEcoPrintEnabled());

        //
        final UserIdDto user = SpSession.get().getUserIdDto();

        final List<ACLPermissionEnum> permissions =
                ACCESS_CONTROL_SERVICE.getPermission(user, ACLOidEnum.U_INBOX);

        helper.encloseLabel("button-pdf-download",
                HtmlButtonEnum.DOWNLOAD.uiText(getLocale()),
                permissions == null || ACCESS_CONTROL_SERVICE.hasPermission(
                        permissions, ACLPermissionEnum.DOWNLOAD));

        helper.encloseLabel("button-pdf-send",
                HtmlButtonEnum.SEND.uiText(getLocale()),
                permissions == null || ACCESS_CONTROL_SERVICE
                        .hasPermission(permissions, ACLPermissionEnum.SEND));

        helper.addButton("btn-back", HtmlButtonEnum.BACK);
        helper.addButton("btn-default", HtmlButtonEnum.DEFAULT);

        //
        helper.encloseLabel("pdf-pgp-signature",
                ACLPermissionEnum.SIGN.uiText(getLocale()),
                ConfigManager.isPdfPgpAvailable() && (permissions == null
                        || ACCESS_CONTROL_SERVICE.hasPermission(permissions,
                                ACLPermissionEnum.SIGN)));
        //
        final Integer privsLetterhead = ACCESS_CONTROL_SERVICE
                .getPrivileges(user, ACLOidEnum.U_LETTERHEAD);

        helper.encloseLabel("prompt-letterhead", localized("prompt-letterhead"),
                privsLetterhead == null || ACLPermissionEnum.READER
                        .isPresent(privsLetterhead.intValue()));
    }

}
