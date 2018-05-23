/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.community.MemberCard;
import org.savapage.core.config.WebAppTypeEnum;
import org.savapage.server.pages.admin.MembershipMsg;
import org.savapage.server.session.SpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract page for all pages which need authorized access. The constructor
 * checks the SpSession to see if the user is authorized to see the pages.
 *
 * @author Rijk Ravestein
 *
 */
public abstract class AbstractAuthPage extends AbstractPage {

    private static final long serialVersionUID = 1L;

    /** */
    protected static final String POST_PARM_DATA = "data";

    /** */
    protected static final String POST_PARM_USER = "user";

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AbstractAuthPage.class);

    /**
     * {@code true} when current session holds a user with admin role.
     */
    private boolean adminUser = false;

    /**
     * @return {@code true} when current session holds a user with admin role.
     */
    protected boolean isAdminUser() {
        return adminUser;
    }

    /**
     * Do we need a viable Community Membership status to show this page?
     *
     * @return {@code true} when viable membership is needed.
     */
    protected abstract boolean needMembership();

    /**
     * Constructor.
     * <p>
     * This method throws a {@link RestartResponseException} with
     * {@link SessionExpired} response page when no session or session user is
     * present.
     * </p>
     *
     * @param parameters
     *            The page parameters.
     * @throws RestartResponseException
     *             When authorization or Community Membership issues.
     */
    public AbstractAuthPage(final PageParameters parameters) {

        this.probePostMethod();

        final RequestCycle requestCycle = getRequestCycle();

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(requestCycle.getRequest().getClientUrl().toString());
        }

        final WebAppTypeEnum webAppTypeReq = this.getWebAppTypeEnum(parameters);

        final SpSession session = SpSession.get();
        final WebAppTypeEnum webAppTypeAuth = session.getWebAppType();

        final String message;

        if (session.getUser() == null) {
            message = "Unknown user.";
        } else if (webAppTypeAuth != null && webAppTypeAuth != webAppTypeReq) {
            message = "Wrong Web App Type.";
        } else {
            message = null;
            this.adminUser = session.getUser().getAdmin();
        }

        if (message != null) {
            LOGGER.debug(message);
            throw new RestartResponseException(SessionExpired.class);
        }

        if (webAppTypeAuth == WebAppTypeEnum.ADMIN) {
            /*
             * Check admin authentication (including the need for a valid
             * Membership).
             */
            if (this.adminUser) {
                if (needMembership()
                        && MemberCard.instance().isMembershipDesirable()) {

                    throw new RestartResponseException(MembershipMsg.class);
                }
            } else {
                LOGGER.error("User [{}] is not authorized.",
                        SpSession.get().getUser().getUserId());

                throw new RestartResponseException(SessionExpired.class);
            }
        }
    }

}
