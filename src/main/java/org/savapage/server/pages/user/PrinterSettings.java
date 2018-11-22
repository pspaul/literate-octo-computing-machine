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
package org.savapage.server.pages.user;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.i18n.PrintOutNounEnum;
import org.savapage.core.services.AccessControlService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.PrintScalingEnum;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.pages.NumberUpPreviewPanel;
import org.savapage.server.pages.TooltipPanel;
import org.savapage.server.session.SpSession;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class PrinterSettings extends AbstractUserPage {

    /** */
    private static final long serialVersionUID = 1L;

    /** */
    private static final AccessControlService ACCESS_CONTROL_SERVICE =
            ServiceContext.getServiceFactory().getAccessControlService();

    /** */
    private static final String WICKET_ID_NUMBER_UP_PREVIEW =
            "number-up-preview";

    /** */
    private static final String WICKET_ID_LABEL_LANDSCAPE = "label-landscape";

    /** */
    private static final String WICKET_ID_TOOLTIP_LANDSCAPE_COPY =
            "tooltip-flipswitch-landscape-copy";
    /** */
    private static final String WICKET_ID_TOOLTIP_LANDSCAPE_PRINT =
            "tooltip-flipswitch-landscape-print";

    /**
     *
     * @param parameters
     *            Page parameters.
     */
    public PrinterSettings(final PageParameters parameters) {

        super(parameters);

        final MarkupHelper helper = new MarkupHelper(this);

        final String htmlRadioName = "print-page-scaling-enum";

        labelledRadio("page-scaling", "-none", htmlRadioName,
                PrintScalingEnum.NONE.toString(), false);

        labelledRadio("page-scaling", "-fit", htmlRadioName,
                PrintScalingEnum.FIT.toString(), false);

        helper.addButton("button-default", HtmlButtonEnum.DEFAULT);

        if (ConfigManager.instance()
                .isConfigValue(Key.WEBAPP_NUMBER_UP_PREVIEW_ENABLE)) {

            add(new NumberUpPreviewPanel(WICKET_ID_NUMBER_UP_PREVIEW));

            helper.addLabel(WICKET_ID_LABEL_LANDSCAPE,
                    PrintOutNounEnum.LANDSCAPE);

            TooltipPanel tooltip =
                    new TooltipPanel(WICKET_ID_TOOLTIP_LANDSCAPE_PRINT);
            tooltip.populate(localized("sp-tooltip-page-orientation-print"),
                    true);
            add(tooltip);

            tooltip = new TooltipPanel(WICKET_ID_TOOLTIP_LANDSCAPE_COPY);
            tooltip.populate(localized("sp-tooltip-page-orientation-copy"),
                    true);
            add(tooltip);

        } else {
            helper.discloseLabel(WICKET_ID_NUMBER_UP_PREVIEW);
            helper.discloseLabel(WICKET_ID_LABEL_LANDSCAPE);
        }

        helper.addButton("button-inbox", HtmlButtonEnum.BACK);

        if (ACCESS_CONTROL_SERVICE.hasAccess(SpSession.get().getUser(),
                ACLRoleEnum.PRINT_DELEGATE)) {
            helper.addLabel("button-next-invoicing", NounEnum.INVOICING);
        } else {
            helper.discloseLabel("button-next-invoicing");
        }

        helper.addLabel("button-next", PrintOutNounEnum.JOB);

        helper.addLabel("label-copy",
                PrintOutNounEnum.COPY.uiText(getLocale()));

    }

}
