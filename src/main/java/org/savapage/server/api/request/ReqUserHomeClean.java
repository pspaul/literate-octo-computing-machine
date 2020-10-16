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
package org.savapage.server.api.request;

import java.io.IOException;
import java.util.EnumSet;

import org.savapage.core.job.SpJobScheduler;
import org.savapage.core.job.SpJobType;
import org.savapage.core.job.UserHomeClean;
import org.savapage.core.jpa.User;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqUserHomeClean extends ApiRequestMixin {

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        if (SpJobScheduler.isJobCurrentlyExecuting(
                EnumSet.of(SpJobType.USER_HOME_CLEAN))) {

            setApiResult(ApiResultCodeEnum.WARN, "msg-userhome-clean-busy");

        } else {

            try {
                SpJobScheduler.instance()
                        .scheduleOneShotJob(SpJobType.USER_HOME_CLEAN, 1L);

                setApiResult(ApiResultCodeEnum.OK, "msg-userhome-clean-started",
                        UserHomeClean.getModeTag());

            } catch (Exception e) {
                setApiResult(ApiResultCodeEnum.ERROR, "msg-single-parm",
                        e.getMessage());
            }
        }
    }

}
