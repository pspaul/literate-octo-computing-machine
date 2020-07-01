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

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableInt;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.ACLPermissionEnum;
import org.savapage.core.dao.enums.UserGroupAttrEnum;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class ACLOidSummaryPanel extends ACLSummaryPanel {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     * @param id
     *            The non-null id of this component.
     * @param imgSrcEnabled
     *            Enabled IMG src.
     * @param imgSrcDisabled
     *            Disabled IMG src.
     */
    protected ACLOidSummaryPanel(final String id, final String imgSrcEnabled,
            final String imgSrcDisabled) {
        super(id, imgSrcEnabled, imgSrcDisabled);
    }

    /**
     * Populates the panel.
     *
     * @param acl
     *            A map of bitwise OR-ed ACL values by Admin ACL keys. See
     *            {@link UserGroupAttrEnum#ACL_OIDS_ADMIN}
     * @param locale
     *            The Locale.
     */
    public void populate(final Map<ACLOidEnum, Integer> acl,
            final Locale locale) {

        final Map<ACLOidEnum, List<ACLPermissionEnum>> perms =
                ACLOidEnum.asMapPerms(acl);

        final MutableInt nAclEnabled = new MutableInt();
        final MutableInt nAclDisabled = new MutableInt();

        StringBuilder txtAclEnabled = new StringBuilder();
        StringBuilder txtAclDisabled = new StringBuilder();

        for (final Map.Entry<ACLOidEnum, Integer> entry : acl.entrySet()) {

            if (entry.getValue() != null) {

                final StringBuilder txtAcl;
                final MutableInt nAcl;

                if (entry.getValue().intValue() > 0) {
                    nAcl = nAclEnabled;
                    txtAcl = txtAclEnabled;
                } else {
                    nAcl = nAclDisabled;
                    txtAcl = txtAclDisabled;
                }
                if (nAcl.intValue() > 0) {
                    txtAcl.append(", ");
                }
                txtAcl.append(entry.getKey().uiText(locale));
                nAcl.increment();

                if (nAcl == nAclEnabled) {
                    txtAcl.append(" (");
                    int i = 0;
                    for (final ACLPermissionEnum perm : perms
                            .get(entry.getKey())) {
                        if (i++ > 0) {
                            txtAcl.append(", ");
                        }
                        txtAcl.append(perm.uiText(locale));
                    }
                    txtAcl.append(")");
                }
            }
        }
        this.populate(nAclEnabled, nAclDisabled, txtAclEnabled, txtAclDisabled);
    }

}
