/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2020 Datraverse B.V. <info@datraverse.com>
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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.UserAttrDao;
import org.savapage.core.dao.enums.AppLogLevelEnum;
import org.savapage.core.dao.enums.UserAttrEnum;
import org.savapage.core.jpa.UserAttr;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserService;
import org.savapage.core.util.QRCodeException;
import org.savapage.core.util.QRCodeHelper;
import org.savapage.ext.payment.PaymentGatewayException;
import org.savapage.ext.payment.PaymentGatewayPlugin;
import org.savapage.ext.payment.PaymentGatewayPlugin.PaymentRequest;
import org.savapage.ext.payment.PaymentGatewayTrx;
import org.savapage.ext.payment.bitcoin.BitcoinGateway;
import org.savapage.server.WebApp;
import org.savapage.server.ext.ServerPluginManager;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.pages.MessageContent;
import org.savapage.server.session.SpSession;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class AccountBitcoinTransfer extends AbstractUserPage {

    private static final long serialVersionUID = 1L;

    /** */
    private static final int QR_CODE_WIDTH = 200;

    /**
     * Creates and displays a bitcoin payment address in
     * <a href="https://en.bitcoin.it/wiki/BIP_0021">BIP 0021</a> format.
     */
    public AccountBitcoinTransfer(final PageParameters parameters) {

        super(parameters);

        final BitcoinGateway plugin =
                WebApp.get().getPluginManager().getBitcoinGateway();

        if (!plugin.isOnline()) {
            setResponsePage(new MessageContent(AppLogLevelEnum.INFO,
                    this.localized("msg-bitcoin-disabled")));
            return;
        }

        final MarkupHelper helper = new MarkupHelper(this);

        //
        helper.addModifyLabelAttr("money-transfer-gateway", "value",
                plugin.getId());

        //
        final String userId = SpSession.get().getUserId();

        final DaoContext daoCtx = ServiceContext.getDaoContext();
        daoCtx.beginTransaction();

        try {

            final String address = getBitcoinAddress(daoCtx, plugin, userId);

            final String message =
                    String.format("Increment SavaPage account of %s.", userId);

            final StringBuilder builder = new StringBuilder();

            builder.append("bitcoin:").append(address).append("?message=")
                    .append(URLEncoder.encode(message, "UTF-8")
                            .replaceAll("\\+", "%20"));

            final URI uri = new URI(builder.toString());

            helper.addModifyImagePngBase64Attr("qr-code", QRCodeHelper
                    .createImagePngBase64(uri.toString(), QR_CODE_WIDTH));

            helper.addModifyLabelAttr("bitcoin-uri-button",
                    localized("button-start"), MarkupHelper.ATTR_HREF,
                    uri.toString());
            helper.addLabel("bitcoin-trx-id", address);

        } catch (URISyntaxException | IOException | PaymentGatewayException
                | QRCodeException e) {

            setResponsePage(
                    new MessageContent(AppLogLevelEnum.ERROR, e.getMessage()));

        } finally {
            // Release the user lock.
            daoCtx.rollback();
        }

    }

    /**
     *
     * @param daoCtx
     * @param plugin
     * @param userId
     * @return
     * @throws IOException
     * @throws PaymentGatewayException
     */
    private static String getBitcoinAddress(final DaoContext daoCtx,
            final PaymentGatewayPlugin plugin, final String userId)
            throws IOException, PaymentGatewayException {

        final UserService userService =
                ServiceContext.getServiceFactory().getUserService();
        final UserAttrDao userAttrDao = daoCtx.getUserAttrDao();

        final String address;

        /*
         * Lock the user.
         */
        final org.savapage.core.jpa.User user =
                userService.lockByUserId(userId);

        /*
         * Use assigned bitcoin address when present.
         */
        final UserAttr bitcoinAddr = userAttrDao.findByName(user.getId(),
                UserAttrEnum.BITCOIN_PAYMENT_ADDRESS);

        if (bitcoinAddr == null) {

            /*
             * Create bitcoin address and save as User attribute.
             */
            final PaymentRequest req = new PaymentRequest();
            req.setUserId(userId);
            req.setCallbackUrl(ServerPluginManager.getCallBackUrl(plugin));
            req.setCurrency(ConfigManager.getAppCurrency());

            final PaymentGatewayTrx trx = plugin.onPaymentRequest(req);
            address = trx.getTransactionId();

            final UserAttr attr = new UserAttr();
            attr.setName(UserAttrEnum.BITCOIN_PAYMENT_ADDRESS.getName());
            attr.setUser(user);
            attr.setValue(address);
            userAttrDao.create(attr);

            daoCtx.commit();

        } else {
            address = bitcoinAddr.getValue();
        }

        return address;
    }

}
