/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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
package org.savapage.server.api.request;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Map;

import org.codehaus.jackson.JsonProcessingException;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.jpa.User;
import org.savapage.core.print.proxy.JsonProxyPrinter;
import org.savapage.core.print.proxy.ProxyPrintException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Validates proxy printer option values.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqPrinterOptValidate extends ApiRequestMixin {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ReqPrinterOptValidate.class);

    /**
     *
     * @author Rijk Ravestein
     *
     */
    @JsonInclude(Include.NON_NULL)
    private static class DtoReq extends AbstractDto {

        private String printer;
        private Map<String, String> options;

        public String getPrinter() {
            return printer;
        }

        @SuppressWarnings("unused")
        public void setPrinter(String printer) {
            this.printer = printer;
        }

        public Map<String, String> getOptions() {
            return options;
        }

        @SuppressWarnings("unused")
        public void setOptions(Map<String, String> options) {
            this.options = options;
        }

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws JsonProcessingException, IOException, ProxyPrintException,
            ParseException {

        final DtoReq dtoReq = DtoReq.create(DtoReq.class, getParmValue("dto"));

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(DtoReq.prettyPrint(getParmValue("dto")));
        }

        final JsonProxyPrinter proxyPrinter =
                PROXY_PRINT_SERVICE.getCachedPrinter(dtoReq.getPrinter());

        if (validateIppOptions(proxyPrinter, dtoReq.getOptions())) {
            setApiResultOk();
        }
    }

    /**
     * Validates IPP Media choices according to the list of cost rules. When not
     * valid, {@link #setApiResultText(ApiResultCodeEnum, String)} is called
     * with {@link ApiResultCodeEnum#WARN}.
     *
     * @param proxyPrinter
     *            The proxy printer.
     * @param ippOptions
     *            The IPP attribute key/choices.
     * @return {@code true} when choices are valid.
     */
    private boolean validateIppOptions(final JsonProxyPrinter proxyPrinter,
            final Map<String, String> ippOptions) {
        /*
         * Media
         */
        if (proxyPrinter.hasCustomCostRulesMedia()) {

            final BigDecimal cost =
                    proxyPrinter.calcCustomCostMedia(ippOptions);

            if (cost == null || cost.compareTo(BigDecimal.ZERO) == 0) {

                final String ippKeyword =
                        IppDictJobTemplateAttr.ATTR_MEDIA_TYPE;

                setApiResultText(ApiResultCodeEnum.WARN, String.format(
                        "Combination of media options for \"%s / %s\" "
                                + "is not supported.",
                        PROXY_PRINT_SERVICE.localizePrinterOpt(getLocale(),
                                ippKeyword),
                        PROXY_PRINT_SERVICE.localizePrinterOptValue(getLocale(),
                                ippKeyword, ippOptions.get(ippKeyword))));
                return false;
            }
        }

        // Copy
        if (proxyPrinter.hasCustomCostRulesCopy()) {

            final BigDecimal cost = proxyPrinter.calcCustomCostCopy(ippOptions);

            if (cost == null || cost.compareTo(BigDecimal.ZERO) == 0) {

                StringBuilder msg = null;

                final String[][] copyOptions =
                        IppDictJobTemplateAttr.JOBTICKET_ATTR_COPY_V_NONE;

                for (final String[] attrArray : copyOptions) {

                    final String ippKey = attrArray[0];
                    final String ippChoice = ippOptions.get(ippKey);

                    if (ippChoice == null || ippChoice.equals(attrArray[1])) {
                        continue;
                    }

                    if (msg == null) {
                        msg = new StringBuilder();
                        msg.append("Combination of options for ");
                    } else {
                        msg.append(", ");
                    }

                    msg.append("\"")
                            .append(PROXY_PRINT_SERVICE
                                    .localizePrinterOpt(getLocale(), ippKey))
                            .append(" / ")
                            .append(PROXY_PRINT_SERVICE.localizePrinterOptValue(
                                    getLocale(), ippKey, ippChoice))
                            .append("\"");
                }

                if (msg != null) {
                    msg.append(" is not supported.");
                    setApiResultText(ApiResultCodeEnum.WARN, msg.toString());
                    return false;
                }
            }
        }
        return true;
    }

}
