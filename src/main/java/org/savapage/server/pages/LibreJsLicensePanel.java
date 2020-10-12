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

import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.html.panel.Panel;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class LibreJsLicensePanel extends Panel {

    /** */
    private static final long serialVersionUID = 1L;

    /**
     *
     * @param id
     *            The non-null id of this component.
     * @param jsRendered
     *            Rendered JavaScript URL (key) and licenses (value).
     */
    public LibreJsLicensePanel(final String id,
            final Map<String, LibreJsLicenseEnum> jsRendered) {

        super(id);

        add(new PropertyListView<String>("script-entry",
                new ArrayList<>(jsRendered.keySet())) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<String> item) {

                final String jsUrlModel = item.getModelObject();

                final LibreJsLicenseEnum license = jsRendered.get(jsUrlModel);

                final String jsUrl;
                if (!jsUrlModel.startsWith("/")
                        && !jsUrlModel.startsWith("../")) {
                    jsUrl = "/" + jsUrlModel;
                } else {
                    jsUrl = jsUrlModel;
                }

                final String[] arr = StringUtils.split(jsUrl, '/');
                final String jsName =
                        StringUtils.split(arr[arr.length - 1], '?')[0];

                item.add(MarkupHelper.modifyLabelAttr(
                        new Label("script", jsName), MarkupHelper.ATTR_HREF,
                        jsUrl));

                item.add(MarkupHelper.modifyLabelAttr(
                        new Label("license", license.getId()),
                        MarkupHelper.ATTR_HREF, license.getUrl()));

                item.add(MarkupHelper.modifyLabelAttr(
                        new Label("source", jsName), MarkupHelper.ATTR_HREF,
                        jsUrl));
            }
        });
    }

}
