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
package org.savapage.server.webapp;

import org.apache.wicket.markup.html.panel.Panel;
import org.savapage.core.config.WebAppTypeEnum;
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
     */
    private static final String[] WICKET_ID_DONWLOAD_PARMS = {
            JsonApiDict.PARM_DATA, JsonApiDict.PARM_USER, JsonApiDict.PARM_REQ,
            JsonApiDict.PARM_REQ_PARM, JsonApiDict.PARM_REQ_SUB };

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
                helper.addModifyLabelAttr("webAppType", MarkupHelper.ATTR_NAME,
                        JsonApiDict.PARM_WEBAPP_TYPE),
                MarkupHelper.ATTR_VALUE, webAppType.toString());

        for (final String parm : WICKET_ID_DONWLOAD_PARMS) {
            helper.addModifyLabelAttr(parm, MarkupHelper.ATTR_NAME, parm);
        }
    }
}
