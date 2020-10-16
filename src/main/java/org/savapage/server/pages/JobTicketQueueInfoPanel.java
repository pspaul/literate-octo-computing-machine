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
import org.savapage.core.services.JobTicketService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.JobTicketQueueInfo;
import org.savapage.core.services.helpers.JobTicketStats;
import org.savapage.server.WebApp;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class JobTicketQueueInfoPanel extends Panel {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /** */
    private static final JobTicketService JOBTICKET_SERVICE =
            ServiceContext.getServiceFactory().getJobTicketService();

    /**
     *
     * @param id
     *            The non-null id of this component.
     */
    public JobTicketQueueInfoPanel(final String id) {
        super(id);
    }

    /**
     */
    public void populate(final boolean showQueueSize) {

        final MarkupHelper helper = new MarkupHelper(this);

        final JobTicketQueueInfo info = JOBTICKET_SERVICE.getTicketQueueInfo();

        final JobTicketStats statsP = info.getStatsPrintJobs();
        final JobTicketStats statsC = info.getStatsCopyJobs();

        helper.encloseLabel("queue-size",
                String.valueOf(statsP.getJobs() + statsC.getJobs()),
                showQueueSize);

        if (statsP.getJobs() > 0) {
            helper.addModifyLabelAttr("img-stats-printer",
                    MarkupHelper.ATTR_SRC, String.format("%s/%s",
                            WebApp.PATH_IMAGES, "printer-26x26.png"));

            helper.addLabel("stats-printer", String.format("%d:%d",
                    statsP.getJobs(), statsP.getSheets()));
        } else {
            helper.discloseLabel("stats-printer");
        }

        if (statsC.getJobs() > 0) {
            helper.addModifyLabelAttr("img-stats-copier", MarkupHelper.ATTR_SRC,
                    String.format("%s/%s", WebApp.PATH_IMAGES,
                            "scanner-32x32.png"));

            helper.addLabel("stats-copier", String.format("%d:%d",
                    statsC.getJobs(), statsC.getSheets()));
        } else {
            helper.discloseLabel("stats-copier");
        }
    }

}
