/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.pages.admin;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.EnumUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.services.AccessControlService;
import org.savapage.core.services.ServiceContext;
import org.savapage.server.pages.MarkupHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ACLRoleEnumPanel extends Panel {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private static final AccessControlService ACCESSCONTROL_SERVICE =
            ServiceContext.getServiceFactory().getAccessControlService();

    /**
    *
    */
    private class EnumView extends PropertyListView<ACLRoleEnum> {

        private static final long serialVersionUID = 1L;

        private final EnumSet<ACLRoleEnum> selected;

        public EnumView(final String id, final List<ACLRoleEnum> entryList,
                final EnumSet<ACLRoleEnum> selected) {
            super(id, entryList);
            this.selected = selected;
        }

        @Override
        protected void populateItem(final ListItem<ACLRoleEnum> item) {

            final ACLRoleEnum value = item.getModelObject();
            final String htmlId = UUID.randomUUID().toString();

            //
            final String uiText = value.uiText(getLocale());

            Label label;

            if (ACCESSCONTROL_SERVICE.getTopIndeterminateGranted()
                    .contains(value)) {
                label = new Label("label",
                        String.format("<span class=\"%s\">%s</span>",
                                MarkupHelper.CSS_TXT_VALID, uiText));
                label.setEscapeModelStrings(false);
            } else {
                label = new Label("label", uiText);
            }

            MarkupHelper.modifyLabelAttr(label, "for", htmlId);
            item.add(label);

            //
            label = new Label("checkbox", "");

            MarkupHelper.modifyLabelAttr(label, "id", htmlId);
            MarkupHelper.modifyLabelAttr(label, "value", value.toString());
            if (selected.contains(value)) {
                MarkupHelper.modifyLabelAttr(label, "checked", "checked");
            }
            item.add(label);
        }
    }

    /**
     *
     * @param id
     *            The non-null id of this component.
     */
    public ACLRoleEnumPanel(final String id) {
        super(id);
    }

    /**
     *
     */
    public void populate(final EnumSet<ACLRoleEnum> selected) {
        ACLRoleEnum.values();

        add(new EnumView("enum-checkboxes",
                EnumUtils.getEnumList(ACLRoleEnum.class), selected));
    }

}
