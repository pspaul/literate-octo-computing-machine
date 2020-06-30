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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.EnumSet;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.dto.CreditLimitDtoEnum;
import org.savapage.core.i18n.LabelEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.i18n.PhraseEnum;
import org.savapage.core.services.PGPPublicKeyService;
import org.savapage.core.services.ServiceContext;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.pages.MarkupHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PageUser extends AbstractAdminPage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /** */
    private static final PGPPublicKeyService PGP_PUBLICKEY_SERVICE =
            ServiceContext.getServiceFactory().getPGPPublicKeyService();

    /**
     * @param parameters
     *            The {@link PageParameters}.
     */
    public PageUser(final PageParameters parameters) {

        super(parameters, ACLOidEnum.A_USERS, RequiredPermission.EDIT);

        final MarkupHelper helper = new MarkupHelper(this);

        //
        Label labelWrk = new Label("internal-user",
                getLocalizer().getString("internal-user", this));

        labelWrk.add(new AttributeModifier("class",
                MarkupHelper.CSS_TXT_INTERNAL_USER));

        add(labelWrk);

        //
        helper.addModifyLabelAttr("img-header-roles", "", MarkupHelper.ATTR_SRC,
                MarkupHelper.IMG_PATH_USER_ROLES);

        final ACLRoleEnumPanel aclRolePanel =
                new ACLRoleEnumPanel("ACLRoleEnumCheckboxes");
        aclRolePanel.populate(EnumSet.noneOf(ACLRoleEnum.class));

        add(aclRolePanel);

        //
        helper.addModifyLabelAttr("credit-limit-none", MarkupHelper.ATTR_VALUE,
                CreditLimitDtoEnum.NONE.toString());
        helper.addModifyLabelAttr("credit-limit-default",
                MarkupHelper.ATTR_VALUE, CreditLimitDtoEnum.DEFAULT.toString());
        helper.addModifyLabelAttr("credit-limit-individual",
                MarkupHelper.ATTR_VALUE,
                CreditLimitDtoEnum.INDIVIDUAL.toString());

        helper.addLabel("label-credit-limit-none",
                CreditLimitDtoEnum.NONE.uiText(getLocale()));
        helper.addLabel("label-credit-limit-default",
                CreditLimitDtoEnum.DEFAULT.uiText(getLocale()));
        helper.addLabel("label-credit-limit-individual",
                CreditLimitDtoEnum.INDIVIDUAL.uiText(getLocale()));

        //
        helper.addButton("button-user-pw-reset", HtmlButtonEnum.RESET);
        helper.addButton("button-user-pw-erase", HtmlButtonEnum.ERASE);
        helper.addButton("button-ok", HtmlButtonEnum.OK);
        helper.addButton("button-cancel-1", HtmlButtonEnum.CANCEL);
        helper.addButton("button-cancel-2", HtmlButtonEnum.CANCEL);
        helper.addButton("button-delete", HtmlButtonEnum.DELETE);
        helper.addButton("button-generate-1", HtmlButtonEnum.GENERATE);
        helper.addButton("button-generate-2", HtmlButtonEnum.GENERATE);

        helper.addLabel("button-password", NounEnum.PASSWORD);

        helper.addLabel("user-email", LabelEnum.PRIMARY_EMAIL);
        helper.addLabel("user-email-other", LabelEnum.OTHER_EMAILS);

        helper.addLabel("user-delete-warning", PhraseEnum.USER_DELETE_WARNING);

        helper.addLabel("user-card-number", NounEnum.CARD_NUMBER);
        helper.addLabel("user-id-number", NounEnum.ID_NUMBER);

        try {

            final URL pgpPublicKeySearchUrl =
                    ConfigManager.instance().getPGPPublicKeyServerUrl();

            final String pgpPublicKeyPreviewUrlTemplate =
                    PGP_PUBLICKEY_SERVICE.getPublicKeyPreviewUrlTpl();

            if (pgpPublicKeySearchUrl == null
                    || pgpPublicKeyPreviewUrlTemplate == null) {

                helper.discloseLabel("button-pgp-pubkey-search");

            } else {

                helper.addModifyLabelAttr("button-pgp-pubkey-search",
                        HtmlButtonEnum.SEARCH.uiText(getLocale()),
                        MarkupHelper.ATTR_HREF,
                        pgpPublicKeySearchUrl.toString());

                helper.addLabel("button-pgp-pubkey-preview",
                        HtmlButtonEnum.CHECK.uiText(getLocale()));

                helper.addModifyLabelAttr("pgp-pubkey-preview-url-template",
                        MarkupHelper.ATTR_VALUE,
                        pgpPublicKeyPreviewUrlTemplate);
            }

        } catch (MalformedURLException e) {
            throw new SpException(e.getMessage(), e);
        }

    }
}
