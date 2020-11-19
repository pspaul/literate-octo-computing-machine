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

import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.dao.AppLogDao;
import org.savapage.core.dao.AppLogDao.ListFilter;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.AppLogLevelEnum;
import org.savapage.core.dao.helpers.AppLogPagerReq;
import org.savapage.core.jpa.AppLog;
import org.savapage.core.services.ServiceContext;
import org.savapage.server.pages.MarkupHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class AppLogPage extends AbstractAdminListPage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AppLogPage.class);

    /**
     * Maximum number of pages in the navigation bar. IMPORTANT: this must be an
     * ODD number.
     */
    private static final int MAX_PAGES_IN_NAVBAR = 5;

    /**
     * @param parameters
     *            The page parameters.
     */
    public AppLogPage(final PageParameters parameters) {

        super(parameters);
        this.probePermissionToRead(ACLOidEnum.A_LOG);

        //
        final String data = getParmValue(POST_PARM_DATA);
        final AppLogPagerReq req = AppLogPagerReq.readReq(data);

        final ListFilter filter = new ListFilter();
        filter.setContainingText(req.getSelect().getContainingText());
        filter.setDateFrom(req.getSelect().dateFrom());
        filter.setDateTo(req.getSelect().dateTo());
        filter.setLevel(req.getSelect().getLevel());

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("level [" + filter.getLevel() + "] dateFrom ["
                    + filter.getDateFrom() + "] dateTo [" + filter.getDateTo()
                    + "] text [" + filter.getContainingText() + "]");
        }

        // this.openServiceContext();
        //
        final AppLogDao appLogDao =
                ServiceContext.getDaoContext().getAppLogDao();

        final long logCount = appLogDao.getListCount(filter);

        /*
         * Display the requested page.
         */

        // add(new Label("applog-count", Long.toString(logCount)));

        List<AppLog> entryList = appLogDao.getListChunk(filter,
                req.calcStartPosition(), req.getMaxResults(),
                req.getSort().getSortField(), req.getSort().getAscending());

        add(new PropertyListView<AppLog>("log-entry-view", entryList) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<AppLog> item) {

                item.add(new Label("message"));
                item.add(new Label("logDate", localizedShortDateTime(
                        item.getModelObject().getLogDate())));

                final AppLogLevelEnum level = AppLogLevelEnum
                        .asEnum(item.getModelObject().getLogLevel());
                final String cssClass;

                switch (level) {
                case ERROR:
                    cssClass = MarkupHelper.CSS_TXT_ERROR;
                    break;
                case WARN:
                    cssClass = MarkupHelper.CSS_TXT_WARN;
                    break;
                default:
                    cssClass = MarkupHelper.CSS_TXT_INFO;
                    break;
                }

                final Label labelWlk =
                        new Label("logLevel", level.uiText(getLocale()));
                labelWlk.add(new AttributeModifier(MarkupHelper.ATTR_CLASS,
                        cssClass));
                item.add(labelWlk);

            }
        });

        /*
         * Display the navigation bars and write the response.
         */
        createNavBarResponse(req, logCount, MAX_PAGES_IN_NAVBAR,
                "sp-applog-page", new String[] { "nav-bar-1", "nav-bar-2" });
    }

}
