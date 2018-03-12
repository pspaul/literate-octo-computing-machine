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

import java.util.List;

import javax.print.attribute.standard.MediaSizeName;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.reports.JrVoucherPageLayoutEnum;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.MediaUtils;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.pages.VoucherDesignOptionsPanel;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class AccountVoucherBase extends AbstractAdminPage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * @param parameters
     *            The page parameters.
     */
    public AccountVoucherBase(final PageParameters parameters) {

        super(parameters);

        final boolean hasEditorAccess =
                this.probePermissionToEdit(ACLOidEnum.A_VOUCHERS);

        final MarkupHelper helper = new MarkupHelper(this);

        helper.encloseLabel("button-new-batch",
                HtmlButtonEnum.ADD.uiText(getLocale()), hasEditorAccess);

        /*
         * Option list: batches
         */
        final List<String> batchList = ServiceContext.getDaoContext()
                .getAccountVoucherDao().getBatches();

        add(new PropertyListView<String>("option-list-batch", batchList) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<String> item) {
                final String batch = item.getModelObject();
                final Label label = new Label("option-batch", batch);
                label.add(new AttributeModifier("value", batch));
                item.add(label);
            }

        });

        /*
         * Option list: voucher card design.
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
