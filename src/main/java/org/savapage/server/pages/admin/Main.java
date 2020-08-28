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
package org.savapage.server.pages.admin;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.SpException;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.ACLPermissionEnum;
import org.savapage.core.dto.UserIdDto;
import org.savapage.core.services.AccessControlService;
import org.savapage.core.services.ServiceContext;
import org.savapage.server.pages.CommunityStatusFooterPanel;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.session.SpSession;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class Main extends AbstractAdminPage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /** */
    private static final AccessControlService ACCESS_CONTROL_SERVICE =
            ServiceContext.getServiceFactory().getAccessControlService();

    @Override
    protected boolean needMembership() {
        return false;
    }

    /**
     * @param parameters
     *            The {@link PageParameters}.
     */
    public Main(final PageParameters parameters) {

        super(parameters);

        final MarkupHelper helper = new MarkupHelper(this);

        add(new CommunityStatusFooterPanel("community-status-footer-panel",
                true));

        final Component logoLink = helper.addTransparant("jqm-logo-link");
        logoLink.add(new AttributeAppender(MarkupHelper.ATTR_HREF,
                CommunityDictEnum.SAVAPAGE_WWW_DOT_ORG_URL.getWord()));
        logoLink.add(new AttributeAppender(MarkupHelper.ATTR_TITLE,
                CommunityDictEnum.SAVAPAGE_WWW_DOT_ORG.getWord()));

        //
        helper.addModifyLabelAttr("savapage-org-link",
                CommunityDictEnum.SAVAPAGE_DOT_ORG.getWord(),
                MarkupHelper.ATTR_HREF,
                CommunityDictEnum.SAVAPAGE_WWW_DOT_ORG_URL.getWord());

        handleACL(helper, SpSession.get().getUserIdDto());
    }

    /**
     * Handles visibility according to user ACL.
     *
     * @param helper
     *            The markup helper.
     * @param userDto
     *            The requesting user.
     */
    private void handleACL(final MarkupHelper helper, final UserIdDto userDto) {

        for (final ACLOidEnum oid : ACLOidEnum.getAdminOids()) {

            final String wicketId;

            switch (oid) {
            case A_ABOUT:
                wicketId = "cat-about";
                break;
            case A_DASHBOARD:
                wicketId = "cat-dashboard";
                break;
            case A_USERS:
                wicketId = "cat-users";
                break;
            case A_USER_GROUPS:
                wicketId = "cat-groups";
                break;
            case A_ACCOUNTS:
                wicketId = "cat-accounts";
                break;
            case A_DEVICES:
                wicketId = "cat-devices";
                break;
            case A_REPORTS:
                wicketId = "cat-reports";
                break;
            case A_DOCUMENTS:
                wicketId = "cat-doclog";
                break;
            case A_LOG:
                wicketId = "cat-applog";
                break;
            case A_OPTIONS:
                wicketId = "cat-options";
                break;
            case A_PRINTERS:
                wicketId = "cat-printers";
                break;
            case A_QUEUES:
                wicketId = "cat-queues";
                break;
            case A_CONFIG_EDITOR:
                wicketId = "cat-config-editor";
                break;
            case A_VOUCHERS:
                wicketId = "cat-vouchers";
                break;
            case A_TRANSACTIONS:
                wicketId = null;
                break;
            default:
                throw new SpException(oid.toString() + " not handled.");
            }

            if (wicketId != null) {
                helper.encloseLabel(wicketId, oid.uiText(getLocale()),
                        ACCESS_CONTROL_SERVICE.hasPermission(userDto, oid,
                                ACLPermissionEnum.READER));
            }
        }

    }

}
