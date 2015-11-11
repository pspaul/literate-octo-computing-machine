/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
 * Authors: Rijk Ravestein.
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
package org.savapage.server.api.request;

import java.io.IOException;

import org.savapage.core.job.SpJobScheduler;
import org.savapage.core.job.SpJobType;
import org.savapage.core.jpa.User;

/**
 * Creates a backup of the database.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqDbBackup extends ApiRequestMixin {

    @Override
    protected void
            onRequest(final String requestingUser, final User lockedUser)
                    throws IOException {

        try {
            SpJobScheduler.instance().scheduleOneShotJob(SpJobType.DB_BACKUP,
                    1L);

            setApiResult(ApiResultCodeEnum.OK, "msg-db-backup-busy");

        } catch (Exception e) {
            setApiResultText(ApiResultCodeEnum.ERROR, e.getMessage());
        }
    }

}
