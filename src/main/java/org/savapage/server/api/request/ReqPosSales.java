/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2021 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2021 Datraverse B.V. <info@datraverse.com>
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.api.request;

import java.io.IOException;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dto.PosSalesDto;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.jpa.User;
import org.savapage.core.json.rpc.AbstractJsonRpcMethodResponse;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.account.UserAccountContextEnum;
import org.savapage.core.services.helpers.account.UserAccountContextFactory;
import org.savapage.ext.papercut.PaperCutServerProxy;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqPosSales extends ApiRequestMixin {

    /** */
    private static final ConfigManager CONFIG_MNGR = ConfigManager.instance();

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final PosSalesDto dto =
                PosSalesDto.create(PosSalesDto.class, this.getParmValueDto());

        final Object[][] enabledInvariantCheck = { //
                { Key.FINANCIAL_POS_SALES_LABEL_LOCATIONS_ENABLE,
                        NounEnum.LOCATION, dto.getPosLocation() }, //
                { Key.FINANCIAL_POS_SALES_LABEL_SHOPS_ENABLE, NounEnum.SHOP,
                        dto.getPosShop() }, //
                { Key.FINANCIAL_POS_SALES_LABEL_ITEMS_ENABLE, NounEnum.ITEM,
                        dto.getPosItem() } //
        };

        for (final Object[] entry : enabledInvariantCheck) {
            if (CONFIG_MNGR.isConfigValue((Key) entry[0]) && (entry[2] == null
                    || StringUtils.isBlank(entry[2].toString()))) {
                this.setApiResult(ApiResultCodeEnum.ERROR,
                        "msg-value-cannot-be-empty",
                        ((NounEnum) entry[1]).uiText(getLocale()));
                return;
            }
        }

        final boolean isPaperCutAccount;

        if (dto.getAccountContext() == UserAccountContextEnum.PAPERCUT) {

            if (UserAccountContextFactory.hasContextPaperCut()) {

                final Set<UserAccountContextEnum> accountContextSet =
                        CONFIG_MNGR.getConfigEnumSet(
                                UserAccountContextEnum.class,
                                Key.FINANCIAL_POS_SALES_ACCOUNTS);

                isPaperCutAccount =
                        accountContextSet.isEmpty() || accountContextSet
                                .contains(UserAccountContextEnum.PAPERCUT);
            } else {
                isPaperCutAccount = false;
            }
            if (!isPaperCutAccount) {
                this.setApiResultText(ApiResultCodeEnum.ERROR,
                        "PaperCut action is not allowed.");
                return;
            }
        } else {
            isPaperCutAccount = false;
        }

        final AbstractJsonRpcMethodResponse rpcResponse;

        if (isPaperCutAccount) {
            final PaperCutServerProxy proxy =
                    PaperCutServerProxy.create(ConfigManager.instance(), true);
            rpcResponse = ACCOUNTING_SERVICE.chargePosSales(proxy, dto);
        } else {
            rpcResponse = ACCOUNTING_SERVICE.chargePosSales(dto);
        }

        if (rpcResponse.isResult()) {
            this.setApiResultText(ApiResultCodeEnum.INFO,
                    String.format("%s [%s] %s [%s]: %s %s.%s",
                            NounEnum.USER.uiText(getLocale()), dto.getUserId(),
                            NounEnum.ITEM.uiText(getLocale()),
                            dto.createComment(),
                            ServiceContext.getAppCurrencySymbol(),
                            dto.getAmountMain(), dto.getAmountCents()));
        } else {
            this.setApiResultText(rpcResponse);
        }
    }

}
