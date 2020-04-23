/*
 * This file is part of the SavaPage project <https://savapage.org>.
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

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.community.MemberCard;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.crypto.CryptoUser;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dao.enums.UserAttrEnum;
import org.savapage.core.i18n.AdverbEnum;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserService;
import org.savapage.core.util.QRCodeException;
import org.savapage.core.util.QRCodeHelper;
import org.savapage.ext.telegram.TelegramHelper;
import org.savapage.lib.totp.TOTPAuthenticator;
import org.savapage.server.pages.AbstractAuthPage;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.session.SpSession;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class TOTPUserAddIn extends AbstractAuthPage {

    /** */
    private static final long serialVersionUID = 1L;

    /** */
    private static final UserDao USER_DAO =
            ServiceContext.getDaoContext().getUserDao();

    /** */
    private static final UserService USER_SERVICE =
            ServiceContext.getServiceFactory().getUserService();

    /** */
    private static final String WID_TOTP_ENABLE = "totp-enable";
    /** */
    private static final String WID_TOTP_ENABLE_TXT = "totp-enable-txt";

    /** */
    private static final String WID_TOTP_TELEGRAM_ENABLE_SPAN =
            "totp-telegram-enable-span";
    /** */
    private static final String WID_TOTP_TELEGRAM_ENABLE =
            "totp-telegram-enable";

    /** */
    private static final String WID_QR_CODE_SPAN = "qr-code-span";
    /** */
    private static final String WID_QR_CODE = "qr-code";
    /** */
    private static final String WID_QR_CODE_URI = "qr-code-uri";

    /** */
    private static final int QR_CODE_WIDTH = 200;

    /**
     * @param parms
     *            Parameters.
     */
    public TOTPUserAddIn(final PageParameters parms) {

        super(parms);

        final MarkupHelper helper = new MarkupHelper(this);

        final org.savapage.core.jpa.User jpaUser =
                USER_DAO.findById(SpSession.get().getUserDbKey());

        String secretKey = USER_SERVICE.getUserAttrValue(jpaUser,
                UserAttrEnum.TOTP_SECRET);

        final TOTPAuthenticator.Builder builder;
        final boolean totpEnabled;
        if (secretKey == null) {
            builder = TOTPAuthenticator.buildKey();
            secretKey = builder.getKey();

            final DaoContext ctx = ServiceContext.getDaoContext();
            final boolean doTransaction = !ctx.isTransactionActive();
            try {
                if (doTransaction) {
                    ctx.beginTransaction();
                }
                totpEnabled = false;
                USER_SERVICE.setUserAttrValue(jpaUser, UserAttrEnum.TOTP_ENABLE,
                        totpEnabled);
                USER_SERVICE.setUserAttrValue(jpaUser, UserAttrEnum.TOTP_SECRET,
                        CryptoUser.encryptUserAttr(jpaUser.getId(), secretKey));
                if (doTransaction) {
                    ctx.commit();
                }
            } finally {
                if (doTransaction) {
                    ctx.rollback();
                }
            }
        } else {
            totpEnabled = USER_SERVICE.isUserAttrValue(jpaUser,
                    UserAttrEnum.TOTP_ENABLE);
            builder = new TOTPAuthenticator.Builder(secretKey);
        }

        final TOTPAuthenticator auth = builder.build();

        final ConfigManager cm = ConfigManager.instance();

        String issuer = cm.getConfigValue(Key.USER_TOTP_ISSUER);
        if (StringUtils.isBlank(issuer)) {
            issuer = MemberCard.instance().getMemberOrganisation();
        }
        if (StringUtils.isBlank(issuer)) {
            issuer = CommunityDictEnum.SAVAPAGE.getWord();
        }

        final String uri = auth.getURI(issuer, jpaUser.getUserId());

        MarkupHelper.modifyComponentAttr(helper.addTransparant(WID_QR_CODE_URI),
                MarkupHelper.ATTR_HREF, uri);

        try {
            helper.addModifyImagePngBase64Attr(WID_QR_CODE,
                    QRCodeHelper.createImagePngBase64(uri, QR_CODE_WIDTH));
        } catch (QRCodeException e) {
            throw new IllegalArgumentException(e);
        }

        helper.addLabel(WID_TOTP_ENABLE_TXT, AdverbEnum.ENABLED);
        helper.addCheckbox(WID_TOTP_ENABLE, totpEnabled);

        //
        final boolean telegram = TelegramHelper.isTOTPEnabled()
                && StringUtils.isNotBlank(USER_SERVICE.getUserAttrValue(jpaUser,
                        UserAttrEnum.EXT_TELEGRAM_ID));

        final boolean telegramEnabled;

        if (telegram) {
            telegramEnabled = USER_SERVICE.isUserAttrValue(jpaUser,
                    UserAttrEnum.EXT_TELEGRAM_TOTP_ENABLE);

            helper.addCheckbox(WID_TOTP_TELEGRAM_ENABLE, telegramEnabled);

            helper.addTransparantInvisible(WID_TOTP_TELEGRAM_ENABLE_SPAN,
                    !totpEnabled);

        } else {
            telegramEnabled = false;
            helper.discloseLabel(WID_TOTP_TELEGRAM_ENABLE);
        }

        //
        helper.addTransparantInvisible(WID_QR_CODE_SPAN,
                !totpEnabled || telegramEnabled);
    }

    @Override
    protected boolean needMembership() {
        return false;
    }
}
