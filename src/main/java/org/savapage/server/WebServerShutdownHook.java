/*
 * This file is part of the SavaPage project <https://savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server;

import org.eclipse.jetty.server.Server;
import org.savapage.core.SpInfo;

/**
 *
 * @author Rijk Ravestein
 *
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

        SpInfo.instance()
                .log("\n+-------------------------------------+"
                        + "\n| Shutting down Web Server...         |"
                        + "\n+-------------------------------------+");

        try {

            myServer.stop();
            myServer.join();

            SpInfo.instance()
                    .log("\n+-------------------------------------+"
                            + "\n| Web Server shutdown completed.      |"
                            + "\n+-------------------------------------+");

        } catch (Exception e) {

            SpInfo.instance()
                    .log(e.getClass().getSimpleName() + " : " + e.getMessage());

            System.exit(1);
        }
    }

}
