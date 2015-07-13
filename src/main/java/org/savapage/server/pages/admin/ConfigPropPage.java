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
package org.savapage.server.pages.admin;

import java.io.IOException;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import org.savapage.core.SpException;
import org.savapage.core.dao.ConfigPropertyDao;
import org.savapage.core.dao.helpers.AbstractPagerReq;
import org.savapage.core.jpa.ConfigProperty;
import org.savapage.core.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Datraverse B.V.
 */
public class ConfigPropPage extends AbstractAdminListPage {

    private static final long serialVersionUID = 1L;

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(ConfigPropPage.class);

    /**
     * Maximum number of pages in the navigation bar. IMPORTANT: this must be an
     * ODD number.
     */
    private static final int MAX_PAGES_IN_NAVBAR = 5;

    /**
     * Bean for mapping JSON page request.
     * <p>
     * Note: class must be static.
     * </p>
     */
    private static class Req extends AbstractPagerReq {

        public static class Select {
            @JsonProperty("text")
            private String containingText = null;

            public String getContainingText() {
                return containingText;
            }

            public void setContainingText(String containingText) {
                this.containingText = containingText;
            }
        }

        public static class Sort {

            private Boolean ascending = true;

            public Boolean getAscending() {
                return ascending;
            }

            public void setAscending(Boolean ascending) {
                this.ascending = ascending;
            }

        }

        private Select select;
        private Sort sort;

        public Select getSelect() {
            return select;
        }

        public void setSelect(Select select) {
            this.select = select;
        }

        public Sort getSort() {
            return sort;
        }

        public void setSort(Sort sort) {
            this.sort = sort;
        }

    }

    /**
     *
     */
    public ConfigPropPage() {

        // this.openServiceContext();

        Req req = readReq();

        // final String containingText = req.getSelect().getContainingText();

        final ConfigPropertyDao.ListFilter filter =
                new ConfigPropertyDao.ListFilter();

        filter.setContainingText(req.getSelect().getContainingText());

        final ConfigPropertyDao dao =
                ServiceContext.getDaoContext().getConfigPropertyDao();

        final long logCount = dao.getListCount(filter);

        /*
         * Display the requested page.
         */

        // add(new Label("applog-count", Long.toString(logCount)));

        final List<ConfigProperty> entryList =
                dao.getListChunk(filter, req.calcStartPosition(), req
                        .getMaxResults(), ConfigPropertyDao.Field.NAME, req
                        .getSort().getAscending());

        add(new PropertyListView<ConfigProperty>("config-entry-view", entryList) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<ConfigProperty> item) {

                final ConfigProperty prop = item.getModelObject();

                item.add(new Label("propertyName"));
                item.add(new Label("value"));
                item.add(new Label("modifiedBy"));

                /*
                 * Set the uid in 'data-savapage' attribute, so it can be picked
                 * up in JavaScript for editing.
                 */
                Label labelWrk =
                        new Label("button-edit", getLocalizer().getString(
                                "button-edit", this));
                labelWrk.add(new AttributeModifier("data-savapage", prop
                        .getPropertyName()));
                item.add(labelWrk);

            }
        });

        /*
         * Display the navigation bars and write the response.
         */
        createNavBarResponse(req, logCount, MAX_PAGES_IN_NAVBAR,
                "sp-config-page", new String[] { "nav-bar-1", "nav-bar-2" });

    }

    /**
     * Reads the page request from the POST parameter.
     *
     * @return The page request.
     */
    private Req readReq() {

        final String data = getParmValue(POST_PARM_DATA);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("data : " + data);
        }

        Req req = null;

        if (data != null) {
            /*
             * Use passed JSON values
             */
            ObjectMapper mapper = new ObjectMapper();
            try {
                req = mapper.readValue(data, Req.class);
            } catch (IOException e) {
                throw new SpException(e.getMessage());
            }
        }
        /*
         * Check inputData separately, since JSON might not have delivered the
         * right parameters and the mapper returned null.
         */
        if (req == null) {
            /*
             * Use the defaults
             */
            req = new Req();
        }
        return req;
    }

}
