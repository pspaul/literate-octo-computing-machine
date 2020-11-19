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

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.AppLogLevelEnum;
import org.savapage.core.i18n.AdjectiveEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.i18n.PhraseEnum;
import org.savapage.core.i18n.PrepositionEnum;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.pages.MarkupHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class AppLogBase extends AbstractAdminPage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * @param parameters
     *            The page parameters.
     */
    public AppLogBase(final PageParameters parameters) {

        super(parameters, ACLOidEnum.A_LOG, RequiredPermission.READ);

        MarkupHelper helper = new MarkupHelper(this);

        helper.addLabel("select-and-sort", PhraseEnum.SELECT_AND_SORT);

        helper.addLabel("prompt-message", NounEnum.MESSAGE);

        //
        helper.addLabel("prompt-level", NounEnum.LEVEL);
        helper.addLabel("level-info-label",
                AppLogLevelEnum.INFO.uiText(getLocale()));
        helper.addLabel("level-warning-label",
                AppLogLevelEnum.WARN.uiText(getLocale()));
        helper.addLabel("level-error-label",
                AppLogLevelEnum.ERROR.uiText(getLocale()));

        helper.addModifyLabelAttr("level-all", MarkupHelper.ATTR_VALUE, "");
        helper.addModifyLabelAttr("level-info", MarkupHelper.ATTR_VALUE,
                AppLogLevelEnum.INFO.toString());
        helper.addModifyLabelAttr("level-warn", MarkupHelper.ATTR_VALUE,
                AppLogLevelEnum.WARN.toString());
        helper.addModifyLabelAttr("level-error", MarkupHelper.ATTR_VALUE,
                AppLogLevelEnum.ERROR.toString());

        //
        helper.addLabel("prompt-period", NounEnum.PERIOD);
        helper.addLabel("prompt-period-from", PrepositionEnum.FROM_TIME);
        helper.addLabel("prompt-period-to", PrepositionEnum.TO_TIME);

        helper.addLabel("prompt-sort-by", NounEnum.SORTING);
        helper.addLabel("sort-by-level", NounEnum.LEVEL);
        helper.addLabel("sort-by-date", NounEnum.DATE);
        helper.addLabel("sort-asc", AdjectiveEnum.ASCENDING);
        helper.addLabel("sort-desc", AdjectiveEnum.DESCENDING);

        helper.addButton("button-apply", HtmlButtonEnum.APPLY);
        helper.addButton("button-default", HtmlButtonEnum.DEFAULT);
    }
}
