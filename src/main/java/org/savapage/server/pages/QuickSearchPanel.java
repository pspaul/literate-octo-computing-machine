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

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class QuickSearchPanel extends Panel {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     * @param id
     *            The non-null id of this component.
     */
    public QuickSearchPanel(final String id) {
        super(id);
    }

    /**
     *
     */
    public void populate(final String htmlBaseId,
            final String searchPrompt, final String searchPlaceholder) {

        Label labelWrk;

        //
        labelWrk = new Label("search-label", searchPrompt);
        labelWrk.add(new AttributeModifier("for", htmlBaseId));

        add(labelWrk);

        //
        labelWrk = new Label("search-input", "");
        labelWrk.add(new AttributeModifier("id", htmlBaseId));

        add(labelWrk);

        //
        labelWrk = new Label("search-filter", "");
        labelWrk.add(new AttributeModifier("id", htmlBaseId + "-filter"));
        labelWrk.add(new AttributeModifier("data-input", "#" + htmlBaseId));
        labelWrk.add(new AttributeModifier("data-filter-placeholder",
                searchPlaceholder));

        add(labelWrk);

    }

}
