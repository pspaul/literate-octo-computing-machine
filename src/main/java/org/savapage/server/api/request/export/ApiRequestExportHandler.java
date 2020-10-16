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
package org.savapage.server.api.request.export;

import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.WebAppTypeEnum;
import org.savapage.core.jpa.User;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface ApiRequestExportHandler {

    /**
     * Processes an export request.
     *
     * @param webAppType
     *            The {@link WebAppTypeEnum}.
     * @param requestCycle
     *            The {@link RequestCycle}.
     * @param parameters
     *            The {@link PageParameters}.
     * @param isGetAction
     *            {@code true} when this is an HTML GET request.
     * @param requestingUser
     *            The user if of the requesting user.
     * @param lockedUser
     *            The locked {@link User} instance, can be {@code null}.
     * @return The request handler.
     * @throws Exception
     *             When an unexpected error is encountered.
     */
    IRequestHandler export(WebAppTypeEnum webAppType, RequestCycle requestCycle,
            PageParameters parameters, boolean isGetAction,
            String requestingUser, User lockedUser) throws Exception;

}
