/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
 * Authors: Rijk Ravestein.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.GenericDao;
import org.savapage.core.i18n.AdjectiveEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.services.ServiceContext;
import org.savapage.server.pages.AbstractAuthPage;
import org.savapage.server.pages.MarkupHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class DatabaseStatsAddin extends AbstractAuthPage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * .
     */
    private static class DatabaseStatsView extends PropertyListView<String[]> {

        /**
         * .
         */
        private static final long serialVersionUID = 1L;

        /**
         *
         * @param id
         *            The wicket id.
         * @param list
         *            The item list.
         */
        DatabaseStatsView(final String id, final List<String[]> list) {
            super(id, list);
        }

        @Override
        protected void populateItem(final ListItem<String[]> item) {

            final String[] column = item.getModelObject();
            final MarkupHelper helper = new MarkupHelper(item);

            helper.addLabel("table", column[0]);
            helper.addLabel("rows", column[1]);
        }
    }

    @Override
    protected boolean needMembership() {
        return false;
    }

    /**
     * @param parameters
     *            The {@link PageParameters}.
     */
    public DatabaseStatsAddin(final PageParameters parameters) {
        super(parameters);
        populate();
    }

    /**
     * Populates the html.
     */
    private void populate() {

        final MarkupHelper helper = new MarkupHelper(this);

        final List<String[]> displayOptions = new ArrayList<>();

        final DaoContext daoCtx = ServiceContext.getDaoContext();

        final GenericDao<?>[] daoList = { //
                daoCtx.getPrintInDao(),
                //
                daoCtx.getPrintOutDao(),
                //
                daoCtx.getPdfOutDao(),
                //
                daoCtx.getAccountTrxDao() //
        };

        final Locale locale = getLocale();
        helper.addLabel("th-1", NounEnum.DOCUMENT.uiText(locale, true));

        final String[] txtList = { //
                AdjectiveEnum.RECEIVED.uiText(locale),
                //
                AdjectiveEnum.PRINTED.uiText(locale),
                //
                AdjectiveEnum.DOWLOADED.uiText(locale),
                //
                NounEnum.TRANSACTION.uiText(locale, true) //
        };

        for (int i = 0; i < txtList.length; i++) {
            final String[] values = new String[2];
            values[0] = txtList[i];
            values[1] = helper.localizedNumber(daoList[i].count());
            displayOptions.add(values);
        }

        add(new DatabaseStatsView("database-table", displayOptions));
    }

}
