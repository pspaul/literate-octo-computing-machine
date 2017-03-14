/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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
package org.savapage.server.webapp;

import java.text.MessageFormat;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.server.WebAppParmEnum;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ZeroPagePanel extends Panel {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     * @param id
     *            The id.
     */
    public ZeroPagePanel(final String id) {
        super(id);
    }

    /**
     *
     * @param webAppType
     *            The {@link WebAppTypeEnum} this panel is used in.
     * @param webAppParms
     *            The page parameters of the Web App container.
     */
    public void populate(final WebAppTypeEnum webAppType,
            final PageParameters webAppParms) {

        final Form<?> form = new Form<Void>("refresh-page-form") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit() {
                // no code intended
            }
        };

        final AjaxButton continueButton =
                new AjaxButton("refresh-page-submit") {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onSubmit(final AjaxRequestTarget target,
                            final Form<?> form) {

                        final PageParameters parms = new PageParameters();
                        parms.set(WebAppParmEnum.SP_ZERO.parm(), "1");

                        switch (webAppType) {
                        case ADMIN:
                            setResponsePage(WebAppAdmin.class, parms);
                            break;
                        case JOBTICKETS:
                            setResponsePage(WebAppJobTickets.class, parms);
                            break;
                        case POS:
                            setResponsePage(WebAppPos.class, parms);
                            break;
                        default:
                            setResponsePage(WebAppUser.class, parms);
                            break;
                        }
                    }

                    @Override
                    protected void onError(AjaxRequestTarget target,
                            Form<?> form) {
                        // no code intended
                    }
                };

        form.add(continueButton);
        add(form);

        add(new Label("zero-page-title",
                MessageFormat.format(
                        this.getLocalizer().getString("zero-page-title", this),
                        CommunityDictEnum.SAVAPAGE.getWord())));

        continueButton.add(new AttributeModifier("value",
                this.getLocalizer().getString("button-continue", this)));
    }
}
