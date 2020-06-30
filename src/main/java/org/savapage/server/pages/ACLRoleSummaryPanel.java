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
package org.savapage.server.pages;

import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableInt;
import org.savapage.core.dao.enums.ACLRoleEnum;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ACLRoleSummaryPanel extends ACLSummaryPanel {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     * @param id
     *            The non-null id of this component.
     */
    public ACLRoleSummaryPanel(final String id) {
        super(id, MarkupHelper.IMG_PATH_USER_ROLES_ENABLED,
                MarkupHelper.IMG_PATH_USER_ROLES_DISABLED);
    }

    /**
     * Populates the panel.
     *
     * @param roles
     *            A map of enabled/disabled (value) user roles (key).
     * @param locale
     *            The Locale.
     */
    public void populate(final Map<ACLRoleEnum, Boolean> roles,
            final Locale locale) {

        final MutableInt nRolesEnabled = new MutableInt();
        final MutableInt nRolesDisabled = new MutableInt();

        StringBuilder txtRolesEnabled = new StringBuilder();
        StringBuilder txtRolesDisabled = new StringBuilder();

        for (final Map.Entry<ACLRoleEnum, Boolean> entry : roles.entrySet()) {
            if (entry.getValue() != null) {
                final StringBuilder txtRoles;
                final MutableInt nRoles;

                if (entry.getValue()) {
                    nRoles = nRolesEnabled;
                    txtRoles = txtRolesEnabled;
                } else {
                    nRoles = nRolesDisabled;
                    txtRoles = txtRolesDisabled;
                }
                if (nRoles.intValue() > 0) {
                    txtRoles.append(", ");
                }
                txtRoles.append(entry.getKey().uiText(locale));
                nRoles.increment();
            }
        }
        this.populate(nRolesEnabled, nRolesDisabled, txtRolesEnabled,
                txtRolesDisabled);
    }

}
