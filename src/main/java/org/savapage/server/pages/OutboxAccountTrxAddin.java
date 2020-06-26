/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Authors: Rijk Ravestein.
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

import java.math.BigDecimal;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.WebAppTypeEnum;
import org.savapage.core.dao.enums.AppLogLevelEnum;
import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.jpa.User;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobDto;
import org.savapage.core.services.ServiceContext;
import org.savapage.server.session.SpSession;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class OutboxAccountTrxAddin extends AbstractAccountTrxAddin {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * URL parameter for OutboxJob file name.
     */
    private static final String PARM_JOBFILENAME = "jobFileName";

    /**
     * Boolean.
     */
    private static final String PARM_JOBTICKET = "jobticket";

    /**
     * Optional
     */
    private static final String PARM_USER_DB_ID = "userDbId";

    @Override
    protected String getJobTicketFileName() {
        return this.getParmValue(PARM_JOBFILENAME);
    }

    /**
     * Gets the job ticket.
     *
     * @return {@code null} when job is not found, due to being processed by
     *         another user. In that case the response page is set.
     */
    protected OutboxJobDto getOutboxJob() {

        final String jobFileName = this.getParmValue(PARM_JOBFILENAME);

        if (StringUtils.isBlank(jobFileName)) {
            setResponsePage(new MessageContent(AppLogLevelEnum.ERROR, String
                    .format("\"%s\" parameter missing", PARM_JOBFILENAME)));
            return null;
        }

        if (StringUtils.isBlank(this.getParmValue(PARM_JOBTICKET))) {
            setResponsePage(new MessageContent(AppLogLevelEnum.ERROR,
                    String.format("\"%s\" parameter missing", PARM_JOBTICKET)));
            return null;
        }

        final boolean isJobTicket = this.getParmBoolean(PARM_JOBTICKET, false);
        final OutboxJobDto job;

        if (isJobTicket) {
            job = ServiceContext.getServiceFactory().getJobTicketService()
                    .getTicket(jobFileName);
            if (job == null) {
                setResponsePage(JobTicketNotFound.class);
            }
        } else {

            final String userid;

            if (this.getSessionWebAppType() == WebAppTypeEnum.USER) {
                userid = SpSession.get().getUserId();
            } else {
                final User user = ServiceContext.getDaoContext().getUserDao()
                        .findActiveUserById(this.getParmLong(PARM_USER_DB_ID));
                userid = user.getUserId();
            }

            job = ServiceContext.getServiceFactory().getOutboxService()
                    .getOutboxJob(userid, jobFileName);
        }
        return job;
    }

    /**
     * @param parameters
     *            The {@link PageParameters}.
     */
    public OutboxAccountTrxAddin(final PageParameters parameters) {

        super(parameters);

        final OutboxJobDto outboxJob = this.getOutboxJob();
        final List<AccountTrx> trxList;

        final BigDecimal totalAmount;
        final int totalCopies;

        if (outboxJob == null) {
            trxList = null;
            totalCopies = 0;
            totalAmount = BigDecimal.ZERO;
        } else {
            if (outboxJob.getAccountTransactions() == null
                    && outboxJob.getUserId() == null) {
                outboxJob.setUserId(SpSession.get().getUserDbKey());
            }
            trxList = ServiceContext.getServiceFactory().getAccountingService()
                    .createAccountTrxsUI(outboxJob);
            totalCopies = outboxJob.getCopies();
            totalAmount = outboxJob.getCostTotal();
        }

        final boolean editCopies = ConfigManager.instance().isConfigValue(
                IConfigProp.Key.WEBAPP_JOBTICKETS_COPIES_EDIT_ENABLE)
                && this.getSessionWebAppType() == WebAppTypeEnum.JOBTICKETS
                && StringUtils.isBlank(outboxJob.getPrinterRedirect());

        populate(totalAmount, totalCopies, trxList, editCopies);
    }

}
