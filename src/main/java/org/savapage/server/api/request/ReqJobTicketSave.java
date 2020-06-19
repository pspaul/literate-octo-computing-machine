/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Authors: Rijk Ravestein.
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
package org.savapage.server.api.request;

import java.io.IOException;

import org.savapage.core.dao.UserDao;
import org.savapage.core.dto.IppMediaSourceCostDto;
import org.savapage.core.ipp.IppMediaSizeEnum;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.ipp.helpers.IppOptionMap;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.User;
import org.savapage.core.msg.UserMsgIndicator;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobDto;
import org.savapage.core.pdf.PdfPrintCollector;
import org.savapage.core.print.proxy.JsonProxyPrinter;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.PrinterAttrLookup;
import org.savapage.core.services.helpers.ProxyPrintCostDto;
import org.savapage.core.services.helpers.ProxyPrintCostParms;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class ReqJobTicketSave extends ApiRequestMixin {

    /**
     * @param userKey
     *            The user database key
     * @throws IOException
     *             When IO error.
     */
    protected final void notifyUser(final Long userKey) throws IOException {

        final UserDao userDao = ServiceContext.getDaoContext().getUserDao();

        final User user = userDao.findById(userKey);

        if (UserMsgIndicator.isSafePagesDirPresent(user.getUserId())) {

            UserMsgIndicator.write(user.getUserId(),
                    ServiceContext.getTransactionDate(),
                    UserMsgIndicator.Msg.JOBTICKET_CHANGED, null);
        }
    }

    /**
     * Recalculates the cost of an {@link OutboxJobDto}, based on its intrinsic
     * cost parameters.
     * <p>
     * Note: {@link OutboxJobDto#setCostResult(ProxyPrintCostDto)} is called to
     * update the cost.
     * </p>
     *
     * @param dto
     *            The {@link OutboxJobDto}.
     * @param proxyPrinter
     *            The {@link JsonProxyPrinter}.
     */
    protected final void recalcCost(final OutboxJobDto dto,
            final JsonProxyPrinter proxyPrinter) {

        final ProxyPrintCostParms costParms =
                new ProxyPrintCostParms(proxyPrinter);

        final IppOptionMap optionMap = new IppOptionMap(dto.getOptionValues());

        // Set parameters.
        costParms.setDuplex(optionMap.isDuplexJob());
        costParms.setEcoPrint(dto.isEcoPrint());
        costParms.setGrayscale(!optionMap.isColorJob());
        costParms.setPagesPerSide(optionMap.getNumberUp());

        costParms.setIppMediaOption(
                optionMap.getOptionValue(IppDictJobTemplateAttr.ATTR_MEDIA));
        costParms.importIppOptionValues(dto.getOptionValues());

        // Calculate custom cost metrics.
        costParms.calcCustomCost();

        //
        final Printer printer = ServiceContext.getDaoContext().getPrinterDao()
                .findByName(dto.getPrinter());

        // Retrieve the regular media source cost.
        final PrinterAttrLookup printerAttrLookup =
                new PrinterAttrLookup(printer);

        final IppMediaSizeEnum ippMediaSize =
                IppMediaSizeEnum.find(costParms.getIppMediaOption());

        final IppMediaSourceCostDto mediaSourceCost =
                printerAttrLookup.findAnyMediaSourceForMedia(ippMediaSize);

        costParms.setMediaSourceCost(mediaSourceCost);

        // Set number of sheets first ...
        dto.setSheets(PdfPrintCollector.calcNumberOfPrintedSheets(
                dto.getPages(), dto.getCopies(), costParms.isDuplex(),
                costParms.getPagesPerSide(), false, false, false));

        // .. and set ...
        costParms.setNumberOfCopies(dto.getCopies());
        costParms.setNumberOfPages(dto.getPages());
        costParms.setNumberOfSheets(dto.getSheets());

        // .. and finally calculate print cost.
        final ProxyPrintCostDto costResult =
                ACCOUNTING_SERVICE.calcProxyPrintCost(printer, costParms);

        dto.setCostResult(costResult);
    }

}
