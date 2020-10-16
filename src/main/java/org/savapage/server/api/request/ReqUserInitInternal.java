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
package org.savapage.server.api.request;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.ACLPermissionEnum;
import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.dto.UserAccountingDto;
import org.savapage.core.dto.UserDto;
import org.savapage.core.dto.UserEmailDto;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserGroup;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqUserInitInternal extends ApiRequestMixin {

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final UserDto dto = new UserDto();

        final UserGroup group = USER_GROUP_SERVICE.getInternalUserGroup();

        final UserAccountingDto accounting;

        if (group.getInitialSettingsEnabled()) {
            accounting = ACCOUNTING_SERVICE.getInitialUserAccounting(group);
        } else {
            accounting =
                    ACCOUNTING_SERVICE.createUserAccounting(BigDecimal.ZERO,
                            Boolean.TRUE, Boolean.FALSE, BigDecimal.ZERO);
        }

        dto.setAccounting(accounting);
        dto.setAclRoles(new HashMap<ACLRoleEnum, Boolean>());
        dto.setAclOidsUser(new HashMap<ACLOidEnum, List<ACLPermissionEnum>>());
        dto.setAclOidsAdmin(new HashMap<ACLOidEnum, List<ACLPermissionEnum>>());
        dto.setAdmin(Boolean.FALSE);
        dto.setDatabaseId(null);
        dto.setDisabled(Boolean.FALSE);
        dto.setEmail("");
        dto.setEmailOther(new ArrayList<UserEmailDto>());
        dto.setFullName("");
        dto.setInternal(Boolean.TRUE);
        dto.setPerson(Boolean.TRUE);
        dto.setPassword("");
        dto.setUserName(ConfigManager.instance()
                .getConfigValue(Key.INTERNAL_USERS_NAME_PREFIX));

        this.setResponse(dto);
        setApiResultOk();

    }
}
