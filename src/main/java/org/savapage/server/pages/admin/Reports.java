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
import org.savapage.core.dao.helpers.UserPrintOutTotalsReq;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.i18n.PrepositionEnum;
import org.savapage.server.pages.JrExportFileExtButtonPanel;
import org.savapage.server.pages.MarkupHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class Reports extends AbstractAdminPage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     * @param parameters
     *            Page parameters.
     */
    public Reports(final PageParameters parameters) {
        super(parameters);

        final MarkupHelper helper = new MarkupHelper(this);
        helper.addLabel("title", ACLOidEnum.A_REPORTS.uiText(getLocale()));

        helper.addLabel("cat-users", NounEnum.USER.uiText(getLocale(), true));

        //
        helper.addLabel("report-1-totals",
                NounEnum.TOTAL.uiText(getLocale(), true));

        helper.addLabel("prompt-options",
                NounEnum.OPTION.uiText(getLocale(), true));

        UserPrintOutTotalsReq.Aspect aspectWlk;

        aspectWlk = UserPrintOutTotalsReq.Aspect.PAGES;
        helper.addLabel("opt-pages-label", aspectWlk.uiText(this.getLocale()));
        helper.addTransparentModifyAttr("opt-pages", MarkupHelper.ATTR_VALUE,
                aspectWlk.toString());

        aspectWlk = UserPrintOutTotalsReq.Aspect.JOBS;
        helper.addLabel("opt-jobs-label", aspectWlk.uiText(this.getLocale()));
        helper.addTransparentModifyAttr("opt-jobs", MarkupHelper.ATTR_VALUE,
                aspectWlk.toString());

        aspectWlk = UserPrintOutTotalsReq.Aspect.COPIES;
        helper.addLabel("opt-copies-label", aspectWlk.uiText(this.getLocale()));
        helper.addTransparentModifyAttr("opt-copies", MarkupHelper.ATTR_VALUE,
                aspectWlk.toString());

        //
        helper.addLabel("prompt-pages",
                UserPrintOutTotalsReq.Aspect.PAGES.uiText(this.getLocale()));

        UserPrintOutTotalsReq.Pages pagesWlk;

        pagesWlk = UserPrintOutTotalsReq.Pages.SENT;
        helper.addLabel("pages-sent-label", pagesWlk.uiText(this.getLocale()));
        helper.addTransparentModifyAttr("pages-sent", MarkupHelper.ATTR_VALUE,
                pagesWlk.toString());

        pagesWlk = UserPrintOutTotalsReq.Pages.PRINTED;
        helper.addLabel("pages-printed-label",
                pagesWlk.uiText(this.getLocale()));
        helper.addTransparentModifyAttr("pages-printed",
                MarkupHelper.ATTR_VALUE, pagesWlk.toString());

        //
        helper.addLabel("div-period", NounEnum.PERIOD);
        helper.addLabel("date-from", PrepositionEnum.FROM_TIME);
        helper.addLabel("date-to", PrepositionEnum.TO_TIME);
        helper.addLabel("div-groups", NounEnum.GROUP.uiText(getLocale(), true));

        add(new JrExportFileExtButtonPanel("report-button-panel-trx",
                "sp-btn-reports-user-printout-tot"));
    }
}
