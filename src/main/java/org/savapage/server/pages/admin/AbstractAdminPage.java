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
package org.savapage.server.pages.admin;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.SpException;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.server.pages.AbstractAuthPage;
import org.savapage.server.pages.NotAuthorized;

/**
 * Abstract page for all pages that need authorized access.
 *
 * @author Rijk Ravestein
 *
 */
public abstract class AbstractAdminPage extends AbstractAuthPage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /** */
    protected enum RequiredPermission {
        READ, EDIT
    }

    @Override
    protected boolean needMembership() {
        return false;
    }

    /**
     * Construct page.
     *
     * @param parameters
     *            The page parameters.
     */
    public AbstractAdminPage(final PageParameters parameters) {
        super(parameters);
    }

    /**
     * Construct page, after probing the required permission.
     *
     * @param parameters
     *            The page parameters.
     * @param oid
     *            The OID to check read permission.
     * @param perm
     *            The required permission.
     * @throws RestartResponseException
     *             When access is denied.
     */
    public AbstractAdminPage(final PageParameters parameters,
            final ACLOidEnum oid, final RequiredPermission perm) {

        super(parameters);

        switch (perm) {
        case EDIT:
            if (!this.probePermissionToEdit(oid)) {
                throw new RestartResponseException(NotAuthorized.class);
            }
            break;

        case READ:
            this.probePermissionToRead(oid);
            break;
        default:
            throw new SpException("Unhandled emum:" + perm.toString());
        }
    }

}
