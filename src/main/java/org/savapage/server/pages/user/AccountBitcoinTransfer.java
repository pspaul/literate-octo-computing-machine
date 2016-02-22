/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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
package org.savapage.server.pages.user;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Hashtable;

import javax.imageio.ImageIO;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.UserAttrDao;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dao.enums.AppLogLevelEnum;
import org.savapage.core.dao.enums.UserAttrEnum;
import org.savapage.core.jpa.UserAttr;
import org.savapage.core.services.ServiceContext;
import org.savapage.ext.payment.PaymentGatewayException;
import org.savapage.ext.payment.PaymentGatewayPlugin;
import org.savapage.ext.payment.PaymentGatewayPlugin.PaymentRequest;
import org.savapage.ext.payment.PaymentGatewayTrx;
import org.savapage.ext.payment.bitcoin.BitcoinGateway;
import org.savapage.server.SpSession;
import org.savapage.server.WebApp;
import org.savapage.server.ext.ServerPluginManager;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.pages.MessageContent;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import net.iharder.Base64;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class AccountBitcoinTransfer extends AbstractUserPage {

    private static final long serialVersionUID = 1L;

    /**
     * Creates and displays a bitcoin payment address in <a
     * href="https://en.bitcoin.it/wiki/BIP_0021">BIP 0021</a> format.
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
        final String userId = SpSession.get().getUser().getUserId();

        final DaoContext daoCtx = ServiceContext.getDaoContext();
        daoCtx.beginTransaction();

        try {

            final String address = getBitcoinAddress(daoCtx, plugin, userId);

            final String message =
                    String.format("Increment SavaPage account of %s.", userId);

            StringBuilder builder = new StringBuilder();

            builder.append("bitcoin:")
                    .append(address)
                    .append("?message=")
                    .append(URLEncoder.encode(message, "UTF-8").replaceAll(
                            "\\+", "%20"));

            final URI uri = new URI(builder.toString());

            builder = new StringBuilder(1024);
            builder.append("data:image/png;base64,").append(
                    createQrCodePngBase64(uri.toString(), 200));

            helper.addModifyLabelAttr("qr-code", "src", builder.toString());

            helper.addModifyLabelAttr("bitcoin-uri-button",
                    localized("button-start"), "href", uri.toString());
            helper.addLabel("bitcoin-trx-id", address);

        } catch (URISyntaxException | IOException | WriterException
                | PaymentGatewayException e) {

            setResponsePage(new MessageContent(AppLogLevelEnum.ERROR,
                    e.getMessage()));

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

        final UserDao userDao = daoCtx.getUserDao();
        final UserAttrDao userAttrDao = daoCtx.getUserAttrDao();

        final String address;

        /*
         * Lock the user.
         */
        final org.savapage.core.jpa.User user = userDao.lockByUserId(userId);

        /*
         * Use assigned bitcoin address when present.
         */
        final UserAttr bitcoinAddr =
                userAttrDao.findByName(user,
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

    /**
     *
     * @param codeText
     * @param squareWidth
     * @return The base64 encoded PNG file with QR code
     * @throws WriterException
     * @throws IOException
     */
    private static String createQrCodePngBase64(final String codeText,
            final int squareWidth) throws WriterException, IOException {

        final Hashtable<EncodeHintType, ErrorCorrectionLevel> hintMap =
                new Hashtable<EncodeHintType, ErrorCorrectionLevel>();

        hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);

        final QRCodeWriter qrCodeWriter = new QRCodeWriter();

        final BitMatrix byteMatrix =
                qrCodeWriter.encode(codeText, BarcodeFormat.QR_CODE,
                        squareWidth, squareWidth, hintMap);

        final int byteMatrixWidth = byteMatrix.getWidth();

        final BufferedImage image =
                new BufferedImage(byteMatrixWidth, byteMatrixWidth,
                        BufferedImage.TYPE_INT_RGB);

        image.createGraphics();

        final Graphics2D graphics = (Graphics2D) image.getGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, byteMatrixWidth, byteMatrixWidth);
        graphics.setColor(Color.BLACK);

        for (int i = 0; i < byteMatrixWidth; i++) {
            for (int j = 0; j < byteMatrixWidth; j++) {
                if (byteMatrix.get(i, j)) {
                    graphics.fillRect(i, j, 1, 1);
                }
            }
        }

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final OutputStream b64 = new Base64.OutputStream(out);
        ImageIO.write(image, "png", b64);

        return out.toString();
    }

}
