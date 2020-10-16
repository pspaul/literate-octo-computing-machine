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
package org.savapage.server;

import java.security.Principal;

import javax.security.auth.Subject;
import javax.servlet.ServletRequest;

import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.DefaultUserIdentity;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.component.AbstractLifeCycle;

/**
 * LoginService that assigns allowed user roles and validates true for
 * <b>all</b> users. User authentication is done in {@link BasicAuthServlet}.
 *
 * @author Rijk Ravestein
 *
 */
public final class BasicAuthLoginService extends AbstractLifeCycle
        implements LoginService {

    /** */
    private final String[] rolesAllowed;

    /**
     * Name of the login service (aka Realm name).
     */
    private final String realmName;

    /** */
    private IdentityService identityService = new DefaultIdentityService();

    /**
     *
     */
    public BasicAuthLoginService(final String[] allowedRoles) {
        this.realmName = this.getClass().getSimpleName();
        this.rolesAllowed = allowedRoles;
    }

    @Override
    public String getName() {
        return realmName;
    }

    @Override
    public UserIdentity login(final String username, final Object credentials,
            final ServletRequest req) {

        // Note: credentials.toString() contains password.

        final Principal principal = new Principal() {
            @Override
            public String getName() {
                return username;
            }
        };

        final Subject subject = new Subject();
        subject.getPrincipals().add(principal);

        return new DefaultUserIdentity(subject, principal, this.rolesAllowed);
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
