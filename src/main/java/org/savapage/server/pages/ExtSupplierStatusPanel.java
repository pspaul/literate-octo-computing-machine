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
package org.savapage.server.pages;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.savapage.core.dao.enums.ExternalSupplierEnum;
import org.savapage.core.dao.enums.ExternalSupplierStatusEnum;
import org.savapage.core.services.helpers.ThirdPartyEnum;
import org.savapage.server.WebApp;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ExtSupplierStatusPanel extends Panel {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     * @param id
     *            The non-null id of this component.
     */
    public ExtSupplierStatusPanel(final String id) {
        super(id);
    }

    /**
     * Populates the panel.
     *
     * @param supplier
     *            The supplier.
     * @param extSupplierStatus
     *            The status.
     * @param extSupplierStatusText
     *            Extra status text.
     * @param extPrintManager
     *            The external print manager, e.g.
     *            {@link ThirdPartyEnum#PAPERCUT}. Can be {@code null}.
     * @param locale
     *            The Locale.
     */
    public void populate(final ExternalSupplierEnum supplier,
            final ExternalSupplierStatusEnum extSupplierStatus,
            final String extSupplierStatusText,
            final ThirdPartyEnum extPrintManager, final Locale locale) {

        final MarkupHelper helper = new MarkupHelper(this);

        add(new Label("extSupplier", supplier.getUiText()));

        final String extSupplierImgUrl =
                WebApp.getExtSupplierEnumImgUrl(supplier);

        if (StringUtils.isBlank(extSupplierImgUrl)) {

            helper.discloseLabel("extSupplierImg");

        } else {
            helper.encloseLabel("extSupplierImg", "", true)
                    .add(new AttributeModifier("src", extSupplierImgUrl));

            final String thirdPartyUrl;

            if (extPrintManager == null) {
                thirdPartyUrl = null;
            } else {
                thirdPartyUrl = WebApp.getThirdPartyEnumImgUrl(extPrintManager);
            }

            if (StringUtils.isBlank(thirdPartyUrl)) {
                helper.discloseLabel("thirdPartyImg");
            } else {
                helper.encloseLabel("thirdPartyImg", "", true)
                        .add(new AttributeModifier("src", thirdPartyUrl));
            }
        }

        if (extSupplierStatus == null) {
            helper.discloseLabel("extStatus");
        } else {
            final String cssClass =
                    MarkupHelper.getCssTxtClass(extSupplierStatus);

            helper.encloseLabel("extStatus", extSupplierStatus.uiText(locale),
                    true)
                    .add(new AttributeModifier(MarkupHelper.ATTR_CLASS,
                            cssClass));

            if (StringUtils.isBlank(extSupplierStatusText)) {
                helper.discloseLabel("extStatusText");
            } else {
                helper.encloseLabel("extStatusText", extSupplierStatusText,
                        true)
                        .add(new AttributeModifier(MarkupHelper.ATTR_CLASS,
                                cssClass));
            }

        }

    }

}
