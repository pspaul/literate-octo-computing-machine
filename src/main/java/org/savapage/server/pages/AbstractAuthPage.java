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
package org.savapage.server.pages;

import org.apache.wicket.request.cycle.RequestCycle;
import org.savapage.core.community.MemberCard;
import org.savapage.server.SpSession;
import org.savapage.server.pages.admin.MembershipMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract page for all pages which need authorized access. The constructor
 * checks the SpSession to see if the user is authorized to see the pages.
 *
 * @author Datraverse B.V.
 *
 */
public abstract class AbstractAuthPage extends AbstractPage {

    private static final long serialVersionUID = 1L;

    protected static final String POST_PARM_DATA = "data";
    protected static final String POST_PARM_USER = "user";

    private static final Logger LOGGER = LoggerFactory
            .getLogger(AbstractAuthPage.class);

    private boolean authErrorHandled = false;
    private boolean adminUser = false;

    /**
     * Informs if an authorization error was already handled, by setting
     * {@link NotAuthorized} as the response page.
     *
     * @return
     */
    protected boolean isAuthErrorHandled() {
        return authErrorHandled;
    }

    protected void setAuthErrorHandled(boolean handled) {
        authErrorHandled = handled;
    }

    protected boolean isAdminUser() {
        return adminUser;
    }

    /**
     * Do we need a viable membership status to show this page?
     *
     * @return
     */
    protected abstract boolean needMembership();

    /**
     * Checks the following contraints:
     * <ul>
     * <li>
     * User must be an administrator.</li>
     * <li>
     * If this page needs a valid membership status (see
     * {@link #needMembership()}), the Admin WebApp must not be blocked (see
     * {@link MemberCard#isMembershipDesirable()}.</li>
     * </ul>
     */
    protected void checkAdminAuthorization() {

        boolean authErr = false;

        if (isAdminUser()) {
            /*
             * Check membership status, and redirect to message page if needed.
             */
            if (needMembership()
                    && MemberCard.instance().isMembershipDesirable()) {
                setResponsePage(MembershipMsg.class);
                authErr = true;
            }
        } else {

            if (LOGGER.isErrorEnabled()) {
                SpSession session = SpSession.get();

                final String error =
                        "user [" + session.getUser().getUserId()
                                + "] is not authorized";
                LOGGER.error(error);
            }
            this.setResponsePage(NotAuthorized.class);
            authErr = true;
        }

        setAuthErrorHandled(authErr);
    }

    /**
     * Constructor.
     * <p>
     * This method sets the {@link NotAuthorized} response page when no session
     * or session user is present. The response page will not be in effect
     * immediately, so this constructor will return.
     * </p>
     * <p>
     * IMPORTANT: Any subclass should check {@link #isAuthErrorHandled()} in its
     * constructor before doing additional checking.
     * </p>
     */
    public AbstractAuthPage() {

        final RequestCycle requestCycle = getRequestCycle();

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(requestCycle.getRequest().getClientUrl().toString());
        }

        String message = null;

        SpSession session = SpSession.get();

        if (session == null) {
            message = "no session";
        } else if (session.getUser() == null) {
            message = "unknown user not authorized";
        } else {
            adminUser = session.getUser().getAdmin();
        }

        authErrorHandled = (message != null);

        if (authErrorHandled) {

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(message);
            }

            this.setResponsePage(NotAuthorized.class);
            /*
             * Setting the response page will not be in effect immediately, so
             * this constructor will return ...
             */
        }
    }

}
