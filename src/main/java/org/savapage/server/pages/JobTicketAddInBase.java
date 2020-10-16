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

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.dao.enums.AppLogLevelEnum;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobDto;
import org.savapage.core.services.JobTicketService;
import org.savapage.core.services.ServiceContext;

/**
 *
 * @author Rijk Ravestein
 *
 */
abstract class JobTicketAddInBase extends AbstractAuthPage {

    /**
     * .
     */
    protected static final JobTicketService JOBTICKET_SERVICE =
            ServiceContext.getServiceFactory().getJobTicketService();

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * URL parameter for Job Ticket file name.
     */
    protected static final String PARM_JOBFILENAME = "jobFileName";

    /**
     * Uses {@link #PARM_JOBFILENAME} to get the Job Ticket file name.
     *
     * @return The file name.
     */
    protected final String getJobFileName() {
        return this.getParmValue(PARM_JOBFILENAME);
    }

    @Override
    protected boolean needMembership() {
        return false;
    }

    /**
     * Gets the job ticket.
     *
     * @return {@code null} when job is not found, due to being processed by
     *         another user. In that case the response page is set.
     */
    protected OutboxJobDto getJobTicket() {

        final String jobFileName = this.getParmValue(PARM_JOBFILENAME);
        final OutboxJobDto job;

        if (StringUtils.isBlank(jobFileName)) {
            setResponsePage(new MessageContent(AppLogLevelEnum.ERROR, String
                    .format("\"%s\" parameter missing", PARM_JOBFILENAME)));
            job = null;
        } else {
            job = JOBTICKET_SERVICE.getTicket(jobFileName);
            if (job == null) {
                setResponsePage(JobTicketNotFound.class);
            }
        }
        return job;
    }

    /**
     * @param parameters
     *            The {@link PageParameters}.
     */
    JobTicketAddInBase(final PageParameters parameters) {
        super(parameters);
    }

}
