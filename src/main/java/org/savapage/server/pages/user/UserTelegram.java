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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.pages.user;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.enums.UserAttrEnum;
import org.savapage.core.dto.UserIdDto;
import org.savapage.core.i18n.LabelEnum;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserService;
import org.savapage.core.totp.TOTPHelper;
import org.savapage.ext.telegram.TelegramHelper;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.session.SpSession;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class UserTelegram extends AbstractUserPage {

    /** */
    private static final UserService USER_SERVICE =
            ServiceContext.getServiceFactory().getUserService();

    /** */
    private static final long serialVersionUID = 1L;

    /**
     *
     * @param parameters
     *            Page parameters.
     */
    public UserTelegram(final PageParameters parameters) {
        super(parameters);

        final UserIdDto authUser = SpSession.get().getUserIdDto();

        final org.savapage.core.jpa.User jpaUser = ServiceContext
                .getDaoContext().getUserDao().findById(authUser.getDbKey());

        final MarkupHelper helper = new MarkupHelper(this);

        helper.addLabel("header", "Telegram Configuration");
        helper.addModifyLabelAttr("telegram-id", MarkupHelper.ATTR_VALUE,
                USER_SERVICE.getUserAttrValue(jpaUser,
                        UserAttrEnum.EXT_TELEGRAM_ID));

        helper.addLabel("step-1", helper.localized("step-1", TelegramHelper
                .userNameFormatted(TelegramHelper.MY_ID_BOT_USERNAME)));
        helper.addLabel("step-3", helper.localized("step-3",
                TelegramHelper.userNameFormatted(ConfigManager.instance()
                        .getConfigValue(Key.EXT_TELEGRAM_BOT_USERNAME))));

        helper.addButton("btn-telegram-apply", HtmlButtonEnum.APPLY);
        helper.addButton("btn-telegram-test", HtmlButtonEnum.SEND);
        helper.addButton("btn-telegram-back", HtmlButtonEnum.BACK);
        helper.encloseLabel("btn-telegram-2-step",
                LabelEnum.TWO_STEP_VERIFICATION.uiText(getLocale()),
                TOTPHelper.isTOTPEnabled());
    }

}
