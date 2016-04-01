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
package org.savapage.server.pages.admin;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.jpa.User;
import org.savapage.core.services.AccessControlService;
import org.savapage.core.services.ServiceContext;
import org.savapage.server.SpSession;
import org.savapage.server.pages.CommunityStatusFooterPanel;
import org.savapage.server.pages.MarkupHelper;

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

    /**
     * .
     */
    private static final AccessControlService ACCESSCONTROL_SERVICE =
            ServiceContext.getServiceFactory().getAccessControlService();

    @Override
    protected boolean needMembership() {
        return false;
    }

    /**
     *
     */
    public Main(final PageParameters parameters) {

        super(parameters);

        MarkupHelper helper = new MarkupHelper(this);

        add(new CommunityStatusFooterPanel("community-status-footer-panel"));

        //
        helper.addModifyLabelAttr("savapage-org-link",
                CommunityDictEnum.SAVAPAGE_DOT_ORG.getWord(), "href",
                CommunityDictEnum.SAVAPAGE_DOT_ORG_URL.getWord());

        //
        final User user = SpSession.get().getUser();
        final boolean enclosePos;
        final boolean encloseJobtickets;

        //
        if (ConfigManager.isInternalAdmin(user.getUserId())) {
            enclosePos = true;
            encloseJobtickets = true;
        } else {
            enclosePos = ACCESSCONTROL_SERVICE.hasAccess(user,
                    ACLRoleEnum.WEB_CASHIER);
            encloseJobtickets = ACCESSCONTROL_SERVICE.hasAccess(user,
                    ACLRoleEnum.JOB_TICKET_OPERATOR);
        }

        helper.encloseLabel("point-of-sale", localized("point-of-sale"),
                enclosePos);

        helper.encloseLabel("job-tickets", localized("job-tickets"),
                encloseJobtickets);

    }

}
