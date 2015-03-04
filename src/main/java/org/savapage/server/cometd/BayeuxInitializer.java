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
package org.savapage.server.cometd;

import java.io.IOException;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ConfigurableServerChannel;
import org.cometd.server.authorizer.GrantAuthorizer;
import org.savapage.core.cometd.AdminPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The initializer for the cometd framework.
 * <p>
 * Note: this class is referenced as part of {@code <servlet>} with
 * {@code <servlet-name>initializer</servlet-name>} in {@code web.xml}
 * configuration file.
 * </p>
 * <p>
 * See the documentation for cometd server <a
 * href="http://cometd.org/documentation/2.x/cometd-java/server/configuration">
 * configuration</a>.
 * </p>
 * <p>
 * These are <init-param> settings for the cometd servlet in web.xml file. The
 * following timeout parameters are important:
 * </p>
 * <ul>
 * <li>
 * 'timeout' (default 30000) : time, in milliseconds, that a server will wait
 * for a message before responding to a long poll with an empty response.</li>
 * <li>
 * 'interval' (default 0) : time, in milliseconds, that specifies how long the
 * client must wait between the end of one long poll requests and the start of
 * the next.</li>
 * <li>
 * 'maxInterval' (default 10000) : max period of time, in milliseconds, that the
 * server will wait for a new long poll from a client before that client is
 * considered invalid and is removed.</li>
 * <li>
 * Also see 'maxSessionsPerBrowser'(default 1)</li>
 * </ul>
 * <p>
 * See the documentation for cometd javascript client <a href=
 * "http://cometd.org/documentation/2.x/cometd-javascript/configuration">
 * configuration</a>. Here the following timeout setting is important:
 * </p>
 * <ul>
 * <li>
 * 'maxNetworkDelay' (default 10000) : max number of milliseconds to wait before
 * considering a request to the Bayeux server failed.</li>
 * </ul>
 *
 * @author Datraverse B.V.
 *
 */
public class BayeuxInitializer extends GenericServlet {

    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(BayeuxInitializer.class);

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     * @throws ServletException
     *             When anything goes wrong.
     */
    @Override
    public final void init() throws ServletException {

        LOGGER.trace("INIT");

        BayeuxServer bayeux =
                (BayeuxServer) getServletContext().getAttribute(
                        BayeuxServer.ATTRIBUTE);

        /*
         * Use our own authenticator, to make sure that only valid users can use
         * our CometD services.
         */
        BayeuxAuthenticator authenticator = new BayeuxAuthenticator();
        bayeux.setSecurityPolicy(authenticator);

        /*
         * Listener and handler of user published requests.
         */
        new DeviceEventService(bayeux, "device");
        new UserEventService(bayeux, "user");
        new ProxyPrintEventService(bayeux, "user-proxy-print");

        /*
         * "If channels are created while load is being offered to the server,
         * it is important to create the channel and register the authorizers in
         * an atomic operation, so that no messages are processed before the
         * authorizers are registered. This can be achieved by using
         * initializers:"
         */
        bayeux.createIfAbsent(AdminPublisher.CHANNEL_PUBLISH + "/**",
                new ConfigurableServerChannel.Initializer() {

                    @Override
                    public void configureChannel(
                            ConfigurableServerChannel channel) {
                        /*
                         * "This ensures that the authorizer set is not empty,
                         * and that by default (if no other authorizer grants or
                         * deny) the authorization is ignored and hence denied."
                         */
                        channel.addAuthorizer(GrantAuthorizer.GRANT_NONE);

                        /*
                         * Only admins can start a new topic. To do so they
                         * create a new channel, for example /admin/dbbackup
                         * (where dbbackup is the topic):
                         */
                        channel.addAuthorizer(new AdminChannelAuthorizer());
                    }

                });

    }

    @Override
    public void destroy() {
    }

    /**
     *
     */
    @Override
    public void service(ServletRequest request, ServletResponse response)
            throws ServletException, IOException {
        throw new ServletException();
    }
}