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
package org.savapage.server.pages;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.savapage.core.community.MemberCard;
import org.savapage.server.helpers.CssClassEnum;
import org.savapage.server.session.SpSession;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class CommunityStatusFooterPanel extends Panel {

    /**
     * .
     */
    private static final long serialVersionUID = 1L;

    /**
     * @param id
     *            The panel identification.
     * @param showUserId
     *            {@code true} when logged-in user id must be shown.
     */
    public CommunityStatusFooterPanel(final String id,
            final boolean showUserId) {

        super(id);

        final MarkupHelper helper = new MarkupHelper(this);

        //
        final MemberCard card = MemberCard.instance();
        final String memberStatus = card.getStatusUserText(getLocale());
        final boolean cardDesirable = card.isMembershipDesirable();

        final Label labelOrg;

        if (card.isVisitorCard()) {
            labelOrg = helper.encloseLabel("membership-org", memberStatus,
                    !cardDesirable);
        } else {
            labelOrg = helper.encloseLabel("membership-org",
                    card.getMemberOrganisation(),
                    StringUtils.isNotBlank(card.getMemberOrganisation()));
        }
        MarkupHelper.appendLabelAttr(labelOrg, MarkupHelper.ATTR_CLASS,
                CssClassEnum.SP_BTN_ABOUT_ORG.clazz());
        //
        helper.encloseLabel("membership-status", memberStatus, cardDesirable);
        //
        final Label labelUserId = helper.encloseLabel("user-id",
                SpSession.get().getUserId(), showUserId);

        if (showUserId) {
            MarkupHelper.appendLabelAttr(labelUserId, MarkupHelper.ATTR_CLASS,
                    CssClassEnum.SP_BTN_ABOUT_USER_ID.clazz());
        }
    }
}
