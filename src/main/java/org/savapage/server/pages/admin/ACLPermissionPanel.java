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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.ACLPermissionEnum;
import org.savapage.server.pages.MarkupHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ACLPermissionPanel extends Panel {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     * @author Rijk Ravestein
     *
     */
    private class OidView extends PropertyListView<ACLOidEnum> {

        private static final long serialVersionUID = 1L;

        OidView(final String id, final List<ACLOidEnum> entryList) {
            super(id, entryList);
        }

        @Override
        protected void populateItem(final ListItem<ACLOidEnum> item) {
            final ACLOidEnum value = item.getModelObject();
            final String htmlId = UUID.randomUUID().toString();

            Label label = new Label("label-oid", value.uiText(getLocale()));

            MarkupHelper.modifyLabelAttr(label, "for", htmlId);
            item.add(label);

            label = new Label("checkbox-oid", "");

            MarkupHelper.modifyLabelAttr(label, "id", htmlId);
            MarkupHelper.modifyLabelAttr(label, "value", value.toString());
            item.add(label);

            // ----
            List<ACLPermissionEnum> entryList = new ArrayList<>();
            for (final ACLPermissionEnum perm : value.getPermissionRoles()) {
                entryList.add(perm);
            }
            item.add(new PermissionRadioView("enum-perms", entryList,
                    UUID.randomUUID().toString()));

            // ----
            entryList = new ArrayList<>();
            for (final ACLPermissionEnum perm : value.getReaderPermissions()) {
                entryList.add(perm);
            }
            item.add(
                    new PermissionCheckboxView("enum-perms-reader", entryList));

            // ----
            entryList = new ArrayList<>();
            for (final ACLPermissionEnum perm : value.getEditorPermissions()) {
                entryList.add(perm);
            }
            item.add(
                    new PermissionCheckboxView("enum-perms-editor", entryList));
        }
    }

    /**
     * .
     */
    private class PermissionRadioView
            extends PropertyListView<ACLPermissionEnum> {

        private static final long serialVersionUID = 1L;

        private final String htmlName;

        PermissionRadioView(final String id,
                final List<ACLPermissionEnum> entryList,
                final String htmlName) {
            super(id, entryList);
            this.htmlName = htmlName;
        }

        @Override
        protected void populateItem(final ListItem<ACLPermissionEnum> item) {

            final ACLPermissionEnum value = item.getModelObject();
            final String htmlId = UUID.randomUUID().toString();

            //
            Label label = new Label("label-radio", value.uiText(getLocale()));

            MarkupHelper.modifyLabelAttr(label, "for", htmlId);
            item.add(label);

            //
            label = new Label("input-radio", "");

            MarkupHelper.modifyLabelAttr(label, "id", htmlId);
            MarkupHelper.modifyLabelAttr(label, "value", value.toString());
            MarkupHelper.modifyLabelAttr(label, "name", this.htmlName);

            item.add(label);
        }
    }

    /**
     * .
     */
    private class PermissionCheckboxView
            extends PropertyListView<ACLPermissionEnum> {

        private static final long serialVersionUID = 1L;

        PermissionCheckboxView(final String id,
                final List<ACLPermissionEnum> entryList) {
            super(id, entryList);
        }

        @Override
        protected void populateItem(final ListItem<ACLPermissionEnum> item) {

            final ACLPermissionEnum value = item.getModelObject();
            final String htmlId = UUID.randomUUID().toString();

            //
            Label label =
                    new Label("label-checkbox", value.uiText(getLocale()));

            MarkupHelper.modifyLabelAttr(label, "for", htmlId);
            item.add(label);

            //
            label = new Label("input-checkbox", "");

            MarkupHelper.modifyLabelAttr(label, "id", htmlId);
            MarkupHelper.modifyLabelAttr(label, "value", value.toString());
            item.add(label);
        }
    }

    /**
     *
     * @param id
     *            The non-null id of this component.
     */
    public ACLPermissionPanel(final String id) {
        super(id);
    }

    /**
     *
     * @param list
     *            The OIDs.
     */
    public void populate(final List<ACLOidEnum> list) {
        add(new OidView("enum-oids", list));
    }

}
