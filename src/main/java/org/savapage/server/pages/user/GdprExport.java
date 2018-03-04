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
package org.savapage.server.pages.user;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.server.WebApp;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.pages.MarkupHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class GdprExport extends AbstractUserPage {

    private static final long serialVersionUID = 1L;

    /** */
    private static final String WICKET_ID_EMAIL = "btn-mailto";

    /**
     *
     * @param parameters
     *            The page parameters.
     */
    public GdprExport(final PageParameters parameters) {

        super(parameters);

        final MarkupHelper helper = new MarkupHelper(this);

        helper.addLabel("txt-header",
                localized("txt-header", CommunityDictEnum.SAVAPAGE.getWord()));

        helper.addModifyLabelAttr("gdpr-url-img",
                String.format(
                        "<img width=\"140\" class=\"sp-img-round-corners-6\""
                                + " src=\"%s/gdpr-banner-200x134.png\" />",
                        WebApp.PATH_IMAGES_THIRDPARTY),
                MarkupHelper.ATTR_HREF, CommunityDictEnum.EU_GDPR_URL.getWord())
                .setEscapeModelStrings(false);

        helper.addButton("btn-download", HtmlButtonEnum.DOWNLOAD);
        helper.addButton("btn-back", HtmlButtonEnum.BACK);

        final String email = ConfigManager.instance()
                .getConfigValue(Key.WEBAPP_USER_GDPR_CONTACT_EMAIL);

        if (StringUtils.isBlank(email)) {
            helper.discloseLabel(WICKET_ID_EMAIL);
        } else {
            final Label label = helper.encloseLabel(WICKET_ID_EMAIL,
                    HtmlButtonEnum.ERASE.uiText(getLocale(), true), true);
            MarkupHelper.modifyLabelAttr(label, MarkupHelper.ATTR_HREF,
                    String.format("mailto:%s", email));
        }

    }

}
