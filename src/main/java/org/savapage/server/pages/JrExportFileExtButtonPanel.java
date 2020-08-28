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

import org.apache.wicket.markup.html.panel.Panel;
import org.savapage.core.reports.JrExportFileExtEnum;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class JrExportFileExtButtonPanel extends Panel {

    /** */
    private static final long serialVersionUID = 1L;

    /**
     *
     * @param id
     *            The non-null id of this component.
     * @param cssClassReport
     *            The CSS class as identification of the Jasper Report.
     */
    public JrExportFileExtButtonPanel(final String id,
            final String cssClassReport) {
        super(id);

        final MarkupHelper helper = new MarkupHelper(this);

        MarkupHelper.appendLabelAttr(
                helper.addModifyLabelAttr("button-report-pdf",
                        JrExportFileExtEnum.PDF.toString(),
                        MarkupHelper.ATTR_DATA_SAVAPAGE,
                        JrExportFileExtEnum.PDF.toString()),
                MarkupHelper.ATTR_CLASS, cssClassReport);

        MarkupHelper.appendLabelAttr(
                helper.addModifyLabelAttr("button-report-csv",
                        JrExportFileExtEnum.CSV.toString(),
                        MarkupHelper.ATTR_DATA_SAVAPAGE,
                        JrExportFileExtEnum.CSV.toString()),
                MarkupHelper.ATTR_CLASS, cssClassReport);
    }

}
