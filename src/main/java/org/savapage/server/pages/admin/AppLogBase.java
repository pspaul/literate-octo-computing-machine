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

import org.savapage.core.dao.helpers.AppLogLevelEnum;
import org.savapage.server.pages.MarkupHelper;

/**
 *
 * @author Datraverse B.V.
 */
public class AppLogBase extends AbstractAdminPage {

    private static final long serialVersionUID = 1L;

    /**
     *
     */
    public AppLogBase() {

        MarkupHelper helper = new MarkupHelper(this);

        helper.addModifyLabelAttr("level-all", "value", "");
        helper.addModifyLabelAttr("level-info", "value",
                AppLogLevelEnum.INFO.toString());
        helper.addModifyLabelAttr("level-warn", "value",
                AppLogLevelEnum.WARN.toString());
        helper.addModifyLabelAttr("level-error", "value",
                AppLogLevelEnum.ERROR.toString());

    }
}
