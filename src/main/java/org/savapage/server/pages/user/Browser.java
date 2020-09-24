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
package org.savapage.server.pages.user;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.i18n.AdjectiveEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.server.WebServer;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.pages.MarkupHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class Browser extends AbstractUserPage {

    private static final long serialVersionUID = 1L;

    /** Tools panel pixel width. */
    private static final int PANEL_WIDTH_PX = 85;

    /** */
    private static final String WID_TOOLS_PANEL = "tools-panel";

    /**
     * HTML titles for Wicket IDs.
     */
    private static final Object[][] TITLE_WID_HTMLBUTTON = { //
            { "btn-clear-all", HtmlButtonEnum.CLEAR_ALL }, //
            { "btn-clear-selection", HtmlButtonEnum.CLEAR_SELECTION }, //
            { "btn-save", HtmlButtonEnum.SAVE }, //
            { "btn-select-all", HtmlButtonEnum.SELECT_ALL }, //
            { "btn-undo-all", HtmlButtonEnum.RESTORE }, //
            { "label-stroke-color-apply", HtmlButtonEnum.APPLY }, //
            { "label-fill-color-apply", HtmlButtonEnum.APPLY }, //
            { "label-stroke-width-apply", HtmlButtonEnum.APPLY }, //
            { "label-opacity-apply", HtmlButtonEnum.APPLY },//
    };

    /**
     * HTML titles for Wicket IDs.
     */
    private static final Object[][] TITLE_WID_NOUN = { //
            { "input-drawing-mode-free", NounEnum.DRAWING }, //
            { "input-drawing-mode-select", NounEnum.SHAPE }, //
            { "input-drawing-brush-color", NounEnum.LINE }, //
            { "input-drawing-select-stroke-color", NounEnum.LINE }, //
            { "input-drawing-select-fill-color", NounEnum.FILL }, //
            { "input-drawing-opacity", NounEnum.OPACITY }, //
            { "input-brush-width", NounEnum.WIDTH }, //
            { "input-shape-width", NounEnum.WIDTH }, //
            { "btn-add-line", NounEnum.LINE }, //
            { "btn-add-circle", NounEnum.CIRCLE }, //
            { "btn-add-rect", NounEnum.RECTANGLE }, //
            { "btn-add-triangle", NounEnum.TRIANGLE }, //
            { "btn-add-text", NounEnum.TEXT }, //
    };

    /**
     * @param parms
     *            The page parameters.
     */
    public Browser(final PageParameters parms) {
        super(parms);

        final MarkupHelper helper = new MarkupHelper(this);

        helper.addModifyLabelAttr("btn-back", MarkupHelper.ATTR_TITLE,
                HtmlButtonEnum.BACK.uiText(getLocale()));

        helper.addModifyLabelAttr("btn-delete", MarkupHelper.ATTR_TITLE,
                HtmlButtonEnum.DELETE.uiText(getLocale()));

        helper.addModifyLabelAttr("btn-previous", MarkupHelper.ATTR_TITLE,
                HtmlButtonEnum.PREVIOUS.uiText(getLocale()));

        helper.addModifyLabelAttr("btn-next", MarkupHelper.ATTR_TITLE,
                HtmlButtonEnum.NEXT.uiText(getLocale()));

        final boolean hasCanvas = ConfigManager.isPdfOverlayEditorEnabled();

        final Component compContent = helper.addTransparant("browser-content");
        final Component compFooter =
                helper.addTransparant("browser-footer-div");

        if (hasCanvas) {
            compContent.add(new AttributeAppender(MarkupHelper.ATTR_STYLE,
                    String.format("margin-left: %dpx;", PANEL_WIDTH_PX)));
            compFooter.add(new AttributeAppender(MarkupHelper.ATTR_STYLE,
                    String.format("padding-left: %dpx; padding-right: 20px;",
                            PANEL_WIDTH_PX)));

            final Component compPanel = helper.addTransparant(WID_TOOLS_PANEL);
            compPanel.add(new AttributeAppender(MarkupHelper.ATTR_STYLE,
                    String.format("width: %dpx;", PANEL_WIDTH_PX)));

            this.populateToolsPanelExt(helper);

        } else {
            helper.discloseLabel(WID_TOOLS_PANEL);
        }

        helper.encloseLabel("canvas-btn-info", "&nbsp;",
                hasCanvas && WebServer.isDeveloperEnv())
                .setEscapeModelStrings(false);

        helper.encloseLabel("canvas-browser-img", "", hasCanvas);
    }

    /**
     * Populates Tools Panel with extra mark-up.
     *
     * @param helper
     *            {@link MarkupHelper}.
     */
    private void populateToolsPanelExt(final MarkupHelper helper) {

        for (final Object[] title : TITLE_WID_HTMLBUTTON) {
            helper.addTransparentWithAttrTitle(title[0].toString(),
                    (HtmlButtonEnum) title[1]);
        }
        for (final Object[] title : TITLE_WID_NOUN) {
            helper.addTransparentWithAttrTitle(title[0].toString(),
                    (NounEnum) title[1]);
        }

        helper.addTransparentWithAttrTitle("input-drawing-fill-transparent",
                AdjectiveEnum.TRANSPARENT.uiText(getLocale()));
    }
}
