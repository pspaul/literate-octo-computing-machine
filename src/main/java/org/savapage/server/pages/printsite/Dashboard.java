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
package org.savapage.server.pages.printsite;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.dao.PrintOutDao;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.ExternalSupplierEnum;
import org.savapage.core.dao.enums.ExternalSupplierStatusEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.i18n.PhraseEnum;
import org.savapage.core.i18n.PrintOutNounEnum;
import org.savapage.core.services.ServiceContext;
import org.savapage.ext.papercut.PaperCutIntegrationEnum;
import org.savapage.ext.papercut.services.PaperCutService;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.pages.MarkupHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class Dashboard extends AbstractPrintSitePage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /** */
    private static final String WID_PROMPT_PENDING_EXT_USERS =
            "prompt-pending-ext-users";

    /** */
    private static final String WID_PROMPT_PENDING_CUPS_USERS =
            "prompt-pending-cups-users";

    /** */
    private static final PrintOutDao PRINT_OUT_DAO =
            ServiceContext.getDaoContext().getPrintOutDao();

    /** */
    private static final PaperCutService PAPERCUT_SERVICE =
            ServiceContext.getServiceFactory().getPaperCutService();

    /**
     * @param parameters
     *            The page parameters.
     */
    public Dashboard(final PageParameters parameters) {

        super(parameters);

        final MarkupHelper helper = new MarkupHelper(this);

        helper.addLabel("title", ACLOidEnum.A_DASHBOARD.uiText(getLocale()));
        helper.addButton("button-clear", HtmlButtonEnum.CLEAR);

        helper.addLabel("realtime-activity",
                PhraseEnum.REALTIME_ACTIVITY.uiText(getLocale()));

        if (PAPERCUT_SERVICE
                .getPrintIntegration() == PaperCutIntegrationEnum.NONE) {

            helper.discloseLabel(WID_PROMPT_PENDING_EXT_USERS);

            // CUPS
            helper.addLabel(WID_PROMPT_PENDING_CUPS_USERS,
                    NounEnum.USER.uiText(getLocale(), true));

            helper.addLabel("prompt-pending-cups-jobs",
                    PrintOutNounEnum.JOB.uiText(getLocale(), true));

            final long nJobs = PRINT_OUT_DAO.countActiveCupsJobs();

            final String countUsers;
            final String countJobs;

            if (nJobs == 0) {
                countJobs = "-";
                countUsers = "-";
            } else {
                countJobs = helper.localizedNumber(nJobs);
                if (nJobs == 1) {
                    countUsers = countJobs;
                } else {
                    countUsers = helper.localizedNumber(
                            PRINT_OUT_DAO.countActiveCupsJobUsers());
                }
            }

            helper.addLabel("pending-cups-users", countUsers);
            helper.addLabel("pending-cups-jobs", countJobs);

        } else {

            helper.discloseLabel(WID_PROMPT_PENDING_CUPS_USERS);

            helper.addLabel(WID_PROMPT_PENDING_EXT_USERS,
                    NounEnum.USER.uiText(getLocale(), true));

            helper.addLabel("prompt-pending-ext-jobs",
                    PrintOutNounEnum.JOB.uiText(getLocale(), true));

            final long nJobs = PRINT_OUT_DAO.countExtSupplierJobs(
                    ExternalSupplierEnum.SAVAPAGE,
                    ExternalSupplierStatusEnum.PENDING_EXT);

            final String countUsers;
            final String countJobs;

            if (nJobs == 0) {
                countJobs = "-";
                countUsers = "-";
            } else {
                countJobs = helper.localizedNumber(nJobs);
                if (nJobs == 1) {
                    countUsers = countJobs;
                } else {
                    countUsers = helper.localizedNumber(
                            PRINT_OUT_DAO.countExtSupplierJobUsers(
                                    ExternalSupplierEnum.SAVAPAGE,
                                    ExternalSupplierStatusEnum.PENDING_EXT));
                }
            }

            helper.addLabel("pending-ext-users", countUsers);
            helper.addLabel("pending-ext-jobs", countJobs);
        }
    }

}
