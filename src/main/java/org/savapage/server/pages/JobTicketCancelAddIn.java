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

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobDto;
import org.savapage.server.WebApp;
import org.savapage.server.helpers.HtmlButtonEnum;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class JobTicketCancelAddIn extends JobTicketAddInBase {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /** */
    private static String WICKET_ID_BTN_NO = "btn-no";

    /** */
    private static String WICKET_ID_LABEL_REASON = "label-reason";

    /**
     * @param parameters
     *            The {@link PageParameters}.
     */
    public JobTicketCancelAddIn(final PageParameters parameters) {

        super(parameters);

        final MarkupHelper helper = new MarkupHelper(this);

        final OutboxJobDto job = this.getJobTicket();

        if (job == null) {
            helper.discloseLabel(WICKET_ID_BTN_NO);
            helper.discloseLabel(WICKET_ID_LABEL_REASON);
            return;
        }

        add(new Label(WICKET_ID_BTN_NO, HtmlButtonEnum.NO.uiText(getLocale())));

        final Label labelButton =
                new Label("btn-yes", HtmlButtonEnum.YES.uiText(getLocale()));

        MarkupHelper.modifyLabelAttr(labelButton,
                MarkupHelper.ATTR_DATA_SAVAPAGE, this.getJobFileName());
        add(labelButton);

        if (ConfigManager.instance().isConfigValue(
                IConfigProp.Key.JOBTICKET_NOTIFY_EMAIL_CANCELED_ENABLE)
                || WebApp.get().getPluginManager().hasNotificationListener()) {
            helper.addLabel(WICKET_ID_LABEL_REASON,
                    localized("sp-label-reason"));
        } else {
            helper.discloseLabel(WICKET_ID_LABEL_REASON);
        }
    }

}
