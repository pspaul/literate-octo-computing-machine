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
package org.savapage.server.pages.admin;

import javax.print.attribute.standard.MediaSizeName;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.reports.JrVoucherPageLayoutEnum;
import org.savapage.core.util.MediaUtils;
import org.savapage.server.pages.VoucherDesignOptionsPanel;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PageAccountVoucherCreate extends AbstractAdminPage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * @param parameters
     *            The page parameters.
     */
    public PageAccountVoucherCreate(final PageParameters parameters) {

        super(parameters, ACLOidEnum.A_VOUCHERS, RequiredPermission.EDIT);

        /*
         * Option list: voucher card design
         */
        final VoucherDesignOptionsPanel voucherDesignOptions =
                new VoucherDesignOptionsPanel("voucher-card-format-options");

        add(voucherDesignOptions);

        final JrVoucherPageLayoutEnum designDefault;

        if (MediaUtils.getDefaultMediaSize() == MediaSizeName.NA_LETTER) {
            designDefault = JrVoucherPageLayoutEnum.LETTER_2X5;
        } else {
            designDefault = JrVoucherPageLayoutEnum.A4_2X5;
        }

        voucherDesignOptions.populate(designDefault);
    }
}
