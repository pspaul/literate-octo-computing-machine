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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.feed;

import java.security.Principal;

import javax.security.auth.Subject;

import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.DefaultUserIdentity;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.component.AbstractLifeCycle;

/**
 * LoginService that assigns {@link AtomFeedServlet#ROLE_ALLOWED} and validates
 * true for <b>all</b> users. User authentication is done in
 * {@link AtomFeedServlet}.
 *
 * @author Rijk Ravestein
 *
 */
public final class AtomFeedLoginService extends AbstractLifeCycle
        implements LoginService {

    /** */
    private static final String[] ROLES_ALLOWED =
            new String[] { AtomFeedServlet.ROLE_ALLOWED };

    /**
     * Name of the login service (aka Realm name).
     */
    private final String realmName;

    /** */
    private IdentityService identityService = new DefaultIdentityService();

    /**
     *
     */
    public AtomFeedLoginService() {
        this.realmName = this.getClass().getSimpleName();
    }

    @Override
    public String getName() {
        return realmName;
    }

    @Override
    public UserIdentity login(final String username, final Object credentials) {

        // Note: credentials.toString() contains password.

        final Principal principal = new Principal() {
            @Override
            public String getName() {
                return username;
            }
        };

        final Subject subject = new Subject();
        subject.getPrincipals().add(principal);

        return new DefaultUserIdentity(subject, principal, ROLES_ALLOWED);
    }

    @Override
    public boolean validate(final UserIdentity user) {
        return true;
    }

    @Override
    public IdentityService getIdentityService() {
        return identityService;
    }

    @Override
    public void setIdentityService(final IdentityService service) {
        this.identityService = service;
    }

    @Override
    public void logout(final UserIdentity user) {
    }
}
