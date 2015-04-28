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
package org.savapage.server;

import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.IRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class SpRequestCycleListener implements IRequestCycleListener {

    @Override
    public void onBeginRequest(final RequestCycle cycle) {
        // no code intended
    }

    @Override
    public void onEndRequest(final RequestCycle cycle) {
        // no code intended
    }

    @Override
    public void onDetach(final RequestCycle cycle) {
        // no code intended
    }

    @Override
    public void onRequestHandlerResolved(final RequestCycle cycle,
            final IRequestHandler handler) {
        // no code intended
    }

    @Override
    public void onRequestHandlerScheduled(final RequestCycle cycle,
            final IRequestHandler handler) {
        // no code intended
    }

    @Override
    public IRequestHandler onException(final RequestCycle cycle,
            final Exception ex) {

        final Throwable cause = ex.getCause();

        if (cause != null) {
            /*
             * Did user refresh browser (e.g. with F5)?
             */
            if (cause instanceof org.eclipse.jetty.io.EofException) {
                return null;
            }
        }

        final Logger logger = LoggerFactory.getLogger(this.getClass());
        logger.error(ex.getMessage(), ex);
        return null;
    }

    @Override
    public void onExceptionRequestHandlerResolved(final RequestCycle cycle,
            final IRequestHandler handler, final Exception exception) {
        // no code intended
    }

    @Override
    public void onRequestHandlerExecuted(final RequestCycle cycle,
            final IRequestHandler handler) {
        // no code intended
    }

    @Override
    public void onUrlMapped(final RequestCycle cycle,
            final IRequestHandler handler, final Url url) {
        // no code intended
    }

}
