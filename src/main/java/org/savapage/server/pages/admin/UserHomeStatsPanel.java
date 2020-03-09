/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Authors: Rijk Ravestein.
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
package org.savapage.server.pages.admin;

import org.apache.commons.io.FileUtils;
import org.apache.wicket.markup.html.panel.Panel;
import org.savapage.core.dto.UserHomeStatsDto;
import org.savapage.core.util.LocaleHelper;
import org.savapage.server.WebApp;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.pages.MarkupHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class UserHomeStatsPanel extends Panel {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /** */
    private static final String WID_BTN_USERHOME_REFRESH =
            "btn-userhome-refresh";

    /**
     *
     * @param id
     *            The non-null id of this component.
     */
    public UserHomeStatsPanel(final String id) {
        super(id);
    }

    /**
     * @param dto
     *            User Home statistics. If {@code null} no statistics are
     *            available.
     * @param hasEditorAccess
     *            {@code true} If editor access.
     */
    public void populate(final UserHomeStatsDto dto,
            final boolean hasEditorAccess) {

        final MarkupHelper helper = new MarkupHelper(this);
        final LocaleHelper localeHelper = new LocaleHelper(getLocale());

        final long userCount;
        if (dto == null) {
            userCount = 0;
        } else {
            userCount = dto.getCurrent().getUsers().getCount();
        }
        if (userCount > 0) {
            helper.addLabel("user-count", helper.localizedNumber(userCount));
            helper.addModifyLabelAttr("img-user", MarkupHelper.ATTR_SRC,
                    String.format("%s/%s", WebApp.PATH_IMAGES_FAMFAM,
                            "user_gray.png"));
            helper.addLabel("stats-date",
                    localeHelper.getLongMediumDateTime(dto.getDate()));

        } else {
            helper.discloseLabel("user-count");
            helper.discloseLabel("stats-date");
        }

        final long inboxCount;
        if (dto == null) {
            inboxCount = 0;
        } else {
            inboxCount = dto.getCurrent().getInbox().getCount();
        }

        boolean encloseSize = false;

        if (inboxCount > 0) {

            helper.addModifyLabelAttr("img-stats-inbox", MarkupHelper.ATTR_SRC,
                    String.format("%s/%s", WebApp.PATH_IMAGES_FILETYPE,
                            "pdf-32.png"));
            helper.addLabel("stats-inbox-count",
                    String.format("%d", inboxCount));

            if (encloseSize) {
                helper.addLabel("stats-inbox-size",
                        FileUtils.byteCountToDisplaySize(
                                dto.getCurrent().getInbox().getSize()));
            } else {
                helper.discloseLabel("stats-inbox-size");
            }

        } else {
            helper.discloseLabel("stats-inbox-count");
        }

        final long outboxCount;
        if (dto == null) {
            outboxCount = 0;
        } else {
            outboxCount = dto.getCurrent().getOutbox().getCount();
        }
        if (outboxCount > 0) {

            helper.addModifyLabelAttr("img-stats-outbox", MarkupHelper.ATTR_SRC,
                    String.format("%s/%s", WebApp.PATH_IMAGES,
                            "printer-terminal-auth-16x16.png"));
            helper.addLabel("stats-outbox-count",
                    String.format("%d", outboxCount));

            if (encloseSize) {
                helper.addLabel("stats-outbox-size",
                        FileUtils.byteCountToDisplaySize(
                                dto.getCurrent().getOutbox().getSize()));
            } else {
                helper.discloseLabel("stats-outbox-size");
            }

        } else {
            helper.discloseLabel("stats-outbox-count");
        }

        if (hasEditorAccess) {
            MarkupHelper.modifyComponentAttr(
                    helper.addTransparant(WID_BTN_USERHOME_REFRESH),
                    MarkupHelper.ATTR_TITLE,
                    HtmlButtonEnum.REFRESH.uiText(getLocale(), true));
        } else {
            helper.discloseLabel(WID_BTN_USERHOME_REFRESH);
        }

    }

}
