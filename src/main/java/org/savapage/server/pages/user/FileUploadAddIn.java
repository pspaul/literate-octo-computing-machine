/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.pages.user;

import java.util.AbstractMap;
import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.doc.DocContent;
import org.savapage.server.pages.MarkupHelper;

/**
 * A page showing the HOLD proxy print jobs for a user.
 * <p>
 * This page is retrieved from the JavaScript Web App.
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public class FileUploadAddIn extends AbstractUserPage {

    /**
     * .
     */
    private static final long serialVersionUID = 1L;

    private static final String NON_OPEN_INDICATOR = "*";

    /**
     * .
     */
    public FileUploadAddIn(final PageParameters parameters) {

        super(parameters);

        /*
         * Supported types.
         */
        final List<AbstractMap.SimpleEntry<String, Boolean>> list =
                DocContent.getSupportedDocsInfo();

        final StringBuilder supported = new StringBuilder();

        Boolean hasNonOpen = Boolean.FALSE;

        if (list.isEmpty()) {
            supported.append("-");
        } else {
            int i = 0;
            for (final AbstractMap.SimpleEntry<String, Boolean> entry : list) {
                if (i > 0) {
                    supported.append(", ");
                }
                supported.append(entry.getKey());
                if (!entry.getValue().booleanValue()) {
                    supported.append(NON_OPEN_INDICATOR);
                    hasNonOpen = Boolean.TRUE;
                }
                i++;
            }
        }

        final Label label =
                new Label("file-upload-types-docs", supported.toString());
        add(label);

        final String disclaimer;

        if (hasNonOpen.booleanValue()) {
            disclaimer = String.format("%s %s", NON_OPEN_INDICATOR,
                    localized("file-upload-disclaimer"));
        } else {
            disclaimer = null;
        }

        add(MarkupHelper.createEncloseLabel("file-upload-types-docs-warning",
                disclaimer, hasNonOpen.booleanValue()));

        //
        add(new Label("file-upload-types-graphics",
                DocContent.getSupportedGraphicsInfo()));

        final Long maxUploadMb = ConfigManager.instance().getConfigLong(
                Key.WEB_PRINT_MAX_FILE_MB,
                IConfigProp.WEBPRINT_MAX_FILE_MB_V_DEFAULT);

        add(new Label("file-upload-max-size", maxUploadMb.toString() + " MB"));
    }

}
