/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
 * Authors: Rijk Ravestein.
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

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.i18n.JobTicketNounEnum;
import org.savapage.core.i18n.SystemModeEnum;
import org.savapage.server.helpers.HtmlButtonEnum;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PageJobTickets extends AbstractAuthPage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /** */
    private static final String WICKET_ID_BUTTON_PRINT_ALL = "button-print-all";

    @Override
    protected boolean needMembership() {
        return false;
    }

    /**
     *
     * @param parameters
     *            The page parameters.
     */
    public PageJobTickets(final PageParameters parameters) {

        super(parameters);

        final ConfigManager cm = ConfigManager.instance();
        final MarkupHelper helper = new MarkupHelper(this);

        addTitle(helper.addButton("button-refresh", HtmlButtonEnum.REFRESH));
        addTitle(helper.addButton("button-logout", HtmlButtonEnum.LOGOUT));

        if (cm.isConfigValue(Key.WEBAPP_JOBTICKETS_PRINT_ALL_ENABLE)) {
            add(addTitle(new Label(WICKET_ID_BUTTON_PRINT_ALL,
                    String.format("%s%s", localized("button-print-all"),
                            HtmlButtonEnum.DOTTED_SUFFIX))));
        } else {
            helper.discloseLabel(WICKET_ID_BUTTON_PRINT_ALL);
        }

        add(addTitle(new Label("button-cancel-all",
                String.format("%s%s", localized("button-cancel-all"),
                        HtmlButtonEnum.DOTTED_SUFFIX))));

        final Label slider = helper.addModifyLabelAttr("jobtickets-max-items",
                MarkupHelper.ATTR_VALUE, String.valueOf(
                        cm.getConfigInt(Key.WEBAPP_JOBTICKETS_LIST_SIZE, 0)));

        MarkupHelper.modifyLabelAttr(slider, MarkupHelper.ATTR_SLIDER_MIN,
                String.valueOf(cm
                        .getConfigInt(Key.WEBAPP_JOBTICKETS_LIST_SIZE_MIN, 0)));

        MarkupHelper.modifyLabelAttr(slider, MarkupHelper.ATTR_SLIDER_MAX,
                String.valueOf(cm
                        .getConfigInt(Key.WEBAPP_JOBTICKETS_LIST_SIZE_MAX, 0)));

        //
        helper.addLabel("prompt-ticket",
                JobTicketNounEnum.TICKET.uiText(getLocale()));
        //
        helper.encloseLabel("mini-sys-maintenance",
                SystemModeEnum.MAINTENANCE.uiText(getLocale()),
                ConfigManager.isSysMaintenance());

        add(new CommunityStatusFooterPanel("community-status-footer-panel",
                true));
    }

    /**
     *
     * @param label
     *            The label.
     * @return The same label for chaining.
     */
    private Label addTitle(final Label label) {

        label.add(new AttributeModifier(MarkupHelper.ATTR_TITLE,
                label.getDefaultModelObjectAsString()));

        return label;
    }
}
