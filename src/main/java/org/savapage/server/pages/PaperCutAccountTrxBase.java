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

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.WebAppTypeEnum;
import org.savapage.core.i18n.AdjectiveEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.i18n.PhraseEnum;
import org.savapage.core.i18n.PrepositionEnum;
import org.savapage.core.jpa.User;
import org.savapage.core.services.ServiceContext;
import org.savapage.ext.papercut.PaperCutAccountTrxPagerReq;
import org.savapage.ext.papercut.PaperCutAccountTrxTypeEnum;
import org.savapage.ext.papercut.PaperCutDb;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.session.SpSession;

/**
 *
 * @author Rijk Ravestein
 */
public final class PaperCutAccountTrxBase extends AbstractAuthPage {

    /**
     * .
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     * @param parameters
     *            The {@code PageParameters}.
     */
    public PaperCutAccountTrxBase(final PageParameters parameters) {

        super(parameters);

        final WebAppTypeEnum webAppType = this.getSessionWebAppType();

        handlePage(webAppType);
    }

    @Override
    protected boolean needMembership() {
        return false;
    }

    /**
     * @param webAppType
     *            The type of Web App.
     */
    private void handlePage(final WebAppTypeEnum webAppType) {

        final String data = getParmValue(POST_PARM_DATA);

        final PaperCutAccountTrxPagerReq req =
                PaperCutAccountTrxPagerReq.read(data);

        Long userId = null;

        boolean userNameVisible = false;

        if (webAppType == WebAppTypeEnum.ADMIN
                || webAppType == WebAppTypeEnum.PRINTSITE) {

            userId = req.getSelect().getUserId();
            userNameVisible = (userId != null);

        } else {
            /*
             * If we are called in a User WebApp context we ALWAYS use the user
             * of the current session.
             */
            userId = SpSession.get().getUser().getId();
        }

        String userName = null;

        if (userNameVisible) {
            final User user = ServiceContext.getDaoContext().getUserDao()
                    .findById(userId);
            userName = user.getUserId();
        }

        final MarkupHelper helper = new MarkupHelper(this);

        String hiddenValue = "";
        if (userId != null) {
            hiddenValue = userId.toString();
        }
        helper.addTransparant("hidden-user-id").add(
                new AttributeModifier(MarkupHelper.ATTR_VALUE, hiddenValue));

        helper.addLabel("select-and-sort",
                PhraseEnum.SELECT_AND_SORT.uiText(getLocale()));
        helper.addLabel("prompt-user", NounEnum.USER);
        helper.addLabel("prompt-transactions",
                NounEnum.TRANSACTION.uiText(getLocale(), true));

        helper.addButton("button-apply", HtmlButtonEnum.APPLY);
        helper.addButton("button-default", HtmlButtonEnum.DEFAULT);

        //
        helper.encloseLabel("select-and-sort-user", userName, userNameVisible);

        helper.addLabel("prompt-comment-containing-text", NounEnum.COMMENT);

        //
        helper.addLabel("prompt-type-select", NounEnum.TYPE);

        helper.addLabel("type-initial",
                PaperCutAccountTrxTypeEnum.INITIAL.uiText(getLocale()));
        helper.addLabel("type-adjust",
                PaperCutAccountTrxTypeEnum.ADJUST.uiText(getLocale()));
        helper.addLabel("type-print",
                PaperCutAccountTrxTypeEnum.PRINT.uiText(getLocale()));
        helper.addLabel("type-print-refund",
                PaperCutAccountTrxTypeEnum.PRINT_REFUND.uiText(getLocale()));
        helper.addLabel("type-web-cashier",
                PaperCutAccountTrxTypeEnum.WEB_CASHIER.uiText(getLocale()));

        helper.addModifyLabelAttr("accounttrx-select-type-initial",
                MarkupHelper.ATTR_VALUE,
                PaperCutAccountTrxTypeEnum.INITIAL.toString());
        helper.addModifyLabelAttr("accounttrx-select-type-adjust",
                MarkupHelper.ATTR_VALUE,
                PaperCutAccountTrxTypeEnum.ADJUST.toString());
        helper.addModifyLabelAttr("accounttrx-select-type-print",
                MarkupHelper.ATTR_VALUE,
                PaperCutAccountTrxTypeEnum.PRINT.toString());
        helper.addModifyLabelAttr("accounttrx-select-type-print-refund",
                MarkupHelper.ATTR_VALUE,
                PaperCutAccountTrxTypeEnum.PRINT_REFUND.toString());
        helper.addModifyLabelAttr("accounttrx-select-type-web-cashier",
                MarkupHelper.ATTR_VALUE,
                PaperCutAccountTrxTypeEnum.WEB_CASHIER.toString());

        //
        helper.addLabel("prompt-period", NounEnum.PERIOD);
        helper.addLabel("prompt-period-from", PrepositionEnum.FROM_TIME);
        helper.addLabel("prompt-period-to", PrepositionEnum.TO_TIME);

        helper.addLabel("prompt-sort-by", NounEnum.SORTING);

        helper.addLabel("sort-by-date", NounEnum.DATE);
        MarkupHelper.modifyComponentAttr(
                helper.addTransparant("sort-by-date-input"),
                MarkupHelper.ATTR_VALUE, PaperCutDb.Field.TRX_DATE.toString());

        helper.addLabel("sort-by-type", NounEnum.TYPE);
        MarkupHelper.modifyComponentAttr(
                helper.addTransparant("sort-by-type-input"),
                MarkupHelper.ATTR_VALUE, PaperCutDb.Field.TRX_TYPE.toString());

        helper.addLabel("sort-asc", AdjectiveEnum.ASCENDING);
        helper.addLabel("sort-desc", AdjectiveEnum.DESCENDING);
    }

}
