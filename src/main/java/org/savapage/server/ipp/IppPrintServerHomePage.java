/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
 * Author: Rijk Ravestein.
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
package org.savapage.server.ipp;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.server.WebApp;

/**
 * An extension of {@link IppPrintServer} WITHOUT any added-value, except for
 * its class name.
 * <p>
 * This class is used as home page of the SavaPage wicket server, i.e. when the
 * URL context path is empty, this class is used as page handler. See
 * {@link WebApp#getHomePage()}.
 * </p>
 * <p>
 * NOTE: The {@link IppPrintServer} class is still needed, since a SEPARATE
 * class must to be mounted to the {@link WebApp#MOUNT_PATH_PRINTERS} context.
 * Because, if {@link IppPrintServer} is used as BOTH home page AND
 * {@link WebApp#MOUNT_PATH_PRINTERS} handler, a request for the home page will
 * be redirected to the {@link WebApp#MOUNT_PATH_PRINTERS} context. This will
 * confuse clients sending IPP request to the home page, since they probably
 * cannot handle HTTP redirection. See Mantis #154.
 * </p>
 *
 * @author Datraverse B.V.
 *
 */
public class IppPrintServerHomePage extends IppPrintServer {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     * @param parameters
     */
    public IppPrintServerHomePage(final PageParameters parameters) {
        super(parameters);
    }

}
