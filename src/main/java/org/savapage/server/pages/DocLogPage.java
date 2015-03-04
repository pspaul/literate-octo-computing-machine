/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
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
package org.savapage.server.pages;

import java.util.List;

import javax.persistence.EntityManager;

import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.savapage.core.dao.DocLogDao;
import org.savapage.core.dao.helpers.DocLogPagerReq;
import org.savapage.core.dao.impl.DaoContextImpl;
import org.savapage.server.SpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Datraverse B.V.
 */
public class DocLogPage extends AbstractListPage {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory
            .getLogger(DocLogPage.class);

    /**
     * Maximum number of pages in the navigation bar. IMPORTANT: this must be an
     * ODD number.
     */
    private static final int MAX_PAGES_IN_NAVBAR = 5;

    @Override
    protected boolean needMembership() {
        return isAdminRoleContext();
    }

    /**
     *
     */
    public DocLogPage() {

        if (isAuthErrorHandled()) {
            return;
        }

        final String data = getParmValue(POST_PARM_DATA);
        LOGGER.trace("data : " + data);

        DocLogPagerReq req = DocLogPagerReq.read(data);

        final DocLogDao.Type docType = req.getSelect().getDocType();

        Long userId = null;

        final boolean adminWebApp = isAdminRoleContext();

        if (adminWebApp) {
            userId = req.getSelect().getUserId();
        } else {
            /*
             * If we are called in a User WebApp context we ALWAYS use the user
             * of the current session.
             */
            userId = SpSession.get().getUser().getId();
        }

        /*
         *
         */
        //this.openServiceContext();
        final EntityManager em = DaoContextImpl.peekEntityManager();

        final DocLogItem.AbstractQuery query = DocLogItem.createQuery(docType);

        final long logCount = query.filteredCount(em, userId, req);

        /*
         * Display the requested page.
         */
        List<DocLogItem> entryList = query.getListChunk(em, userId, req);

        add(new PropertyListView<DocLogItem>("doc-entry-view", entryList) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<DocLogItem> item) {

                /*
                 * Step 1: Create panel and add to page.
                 */
                DocLogItemPanel panel =
                        new DocLogItemPanel("doc-entry", item.getModel());

                item.add(panel);

                /*
                 * Step 2: populate the panel.
                 *
                 * Reason: “If the component is not an instance of Page then it
                 * must be a component that has already been added to a page.”
                 * otherwise it will throw the following warning message.
                 *
                 * 12:07:11,726 WARN [Localizer] Tried to retrieve a localized
                 * string for a component that has not yet been added to the
                 * page.
                 *
                 * See:
                 * http://jaibeermalik.wordpress.com/2008/11/12/localization
                 * -of-wicket-applications/
                 */
                panel.populate(item.getModel());
            }
        });

        /*
         * Display the navigation bars and write the response.
         */
        createNavBarResponse(req, logCount, MAX_PAGES_IN_NAVBAR,
                "sp-doclog-page", new String[] { "nav-bar-1", "nav-bar-2" });
    }
}
