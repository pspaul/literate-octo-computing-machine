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

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.i18n.AdjectiveEnum;
import org.savapage.core.i18n.JobTicketNounEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.i18n.SystemModeEnum;
import org.savapage.core.jpa.PrinterGroup;
import org.savapage.core.services.ServiceContext;
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
    private static final String WICKET_ID_BUTTON_CANCEL_ALL =
            "button-cancel-all";

    /** */
    private static final String WICKET_ID_BUTTON_PRINT_ALL = "button-print-all";

    /** */
    private static final String WICKET_ID_BUTTON_CLOSE_ALL = "button-close-all";

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

        if (cm.isConfigValue(Key.WEBAPP_JOBTICKETS_CANCEL_ALL_ENABLE)) {
            add(addTitle(new Label(WICKET_ID_BUTTON_CANCEL_ALL,
                    String.format("%s%s", localized("button-cancel-all"),
                            HtmlButtonEnum.DOTTED_SUFFIX))));
        } else {
            helper.discloseLabel(WICKET_ID_BUTTON_CANCEL_ALL);
        }

        if (cm.isConfigValue(Key.WEBAPP_JOBTICKETS_PRINT_ALL_ENABLE)) {
            add(addTitle(new Label(WICKET_ID_BUTTON_PRINT_ALL,
                    String.format("%s%s", localized("button-print-all"),
                            HtmlButtonEnum.DOTTED_SUFFIX))));
        } else {
            helper.discloseLabel(WICKET_ID_BUTTON_PRINT_ALL);
        }

        if (cm.isConfigValue(Key.WEBAPP_JOBTICKETS_CLOSE_ALL_ENABLE)) {
            MarkupHelper
                    .modifyComponentAttr(
                            helper.addTransparant(WICKET_ID_BUTTON_CLOSE_ALL),
                            MarkupHelper.ATTR_TITLE,
                            String.format("%s%s", localized("button-close-all"),
                                    HtmlButtonEnum.DOTTED_SUFFIX))
                    .setEscapeModelStrings(false);
        } else {
            helper.discloseLabel(WICKET_ID_BUTTON_CLOSE_ALL);
        }

        //
        final List<PrinterGroup> printerGroups = ServiceContext.getDaoContext()
                .getPrinterGroupMemberDao().getGroupsWithJobTicketMembers();

        helper.encloseLabel("label-jobticket-group",
                NounEnum.GROUP.uiText(getLocale()), !printerGroups.isEmpty());

        if (!printerGroups.isEmpty()) {
            helper.addLabel("option-jobticket-group-all", String
                    .format("― %s ―", AdjectiveEnum.ALL.uiText(getLocale())));

            add(new PropertyListView<PrinterGroup>(
                    "option-list-jobticket-groups", printerGroups) {

                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(final ListItem<PrinterGroup> item) {
                    final PrinterGroup group = item.getModel().getObject();
                    final Label label = new Label("option-jobticket-group",
                            group.getDisplayName());
                    label.add(new AttributeModifier(MarkupHelper.ATTR_VALUE,
                            group.getId()));
                    item.add(label);
                }
            });
        }
        //
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
