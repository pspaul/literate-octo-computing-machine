/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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

import org.eclipse.jetty.server.Server;
import org.savapage.core.SpInfo;

/**
 *
 * @author Datraverse B.V.
 * @since 0.9.9
 */
public final class WebServerShutdownHook extends Thread {

    /**
    *
    */
    private final Server myServer;

    /**
     * The constructor.
     *
     * @param server
     *            The {@link Server}.
     */
    public WebServerShutdownHook(final Server server) {
        super("WebServerShutdownHook");
        myServer = server;
    }

    @Override
    public void run() {

        SpInfo.instance().log("Shutting down Web Server...");

        try {

            myServer.stop();
            myServer.join();

            SpInfo.instance().log("... Web Server shutdown completed.");
            SpInfo.instance().log("Bye!");

        } catch (Exception e) {

            SpInfo.instance().log(
                    e.getClass().getSimpleName() + " : " + e.getMessage());

            System.exit(1);
        }
    }

}
