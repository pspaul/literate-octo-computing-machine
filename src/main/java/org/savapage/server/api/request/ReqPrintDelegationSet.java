/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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

import org.savapage.core.dto.AbstractDto;
import org.savapage.core.dto.PrintDelegationDto;
import org.savapage.core.jpa.User;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqPrintDelegationSet extends ApiRequestMixin {

    @Override
    protected void
            onRequest(final String requestingUser, final User lockedUser)
                    throws IOException {

        @SuppressWarnings("unused")
        final PrintDelegationDto dto =
                AbstractDto.create(PrintDelegationDto.class,
                        getParmValue("dto"));

        // System.out.println(dto.stringify());
        // System.out.println(dto.stringifyPrettyPrinted());
        // System.out.println(JsonPrintDelegation.create(dto).stringify());

        // this.setApiResultOk();
        this.setApiResultText(ApiResultCodeEnum.INFO,
                "This function is not implemented yet.");
    }

}
