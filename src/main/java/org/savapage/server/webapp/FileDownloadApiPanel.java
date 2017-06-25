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
package org.savapage.server.webapp;

import org.apache.wicket.markup.html.panel.Panel;
import org.savapage.server.api.JsonApiDict;
import org.savapage.server.pages.MarkupHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class FileDownloadApiPanel extends Panel {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     * @param id
     *            The Wicket ID.
     */
    public FileDownloadApiPanel(final String id) {
        super(id);
    }

    /**
     *
     * @param webAppType
     *            The {@link WebAppTypeEnum} context.
     */
    public void populate(final WebAppTypeEnum webAppType) {
        final MarkupHelper helper = new MarkupHelper(this);

        MarkupHelper.modifyLabelAttr(
                helper.addModifyLabelAttr("webAppType", "name",
                        JsonApiDict.PARM_WEBAPP_TYPE),
                "value", webAppType.toString());
    }
}
