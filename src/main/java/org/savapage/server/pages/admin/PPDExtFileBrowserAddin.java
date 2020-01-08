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
package org.savapage.server.pages.admin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.print.proxy.JsonProxyPrinter;
import org.savapage.core.print.proxy.JsonProxyPrinterOpt;
import org.savapage.core.print.proxy.JsonProxyPrinterOptChoice;
import org.savapage.core.print.proxy.JsonProxyPrinterOptGroup;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.PpdExtFileReader;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PPDExtFileBrowserAddin extends AbstractAdminPage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * URL parameter for PPDE file name.
     */
    private static final String PARM_PPDE_FILENAME = "ppdeFileName";

    /**
     * @param parameters
     *            The page parameters.
     * @throws IOException
     *             If IO error.
     */
    public PPDExtFileBrowserAddin(final PageParameters parameters)
            throws IOException {
        super(parameters);

        final String ppdeFileName = this.getParmValue(PARM_PPDE_FILENAME);
        final JsonProxyPrinter proxyPrinter = new JsonProxyPrinter();

        proxyPrinter.setGroups(new ArrayList<JsonProxyPrinterOptGroup>());

        final Map<String, JsonProxyPrinterOpt> ppdOptionMap = PpdExtFileReader
                .injectPpdExt(proxyPrinter, ServiceContext.getServiceFactory()
                        .getProxyPrintService().getPPDExtFile(ppdeFileName));

        final StringBuilder msg = new StringBuilder();
        for (final Entry<String, JsonProxyPrinterOpt> entry : ppdOptionMap
                .entrySet()) {
            final String ppdKeyword = entry.getKey();
            final JsonProxyPrinterOpt opt = entry.getValue();

            if (msg.length() > 0) {
                msg.append("<br>");
            }
            msg.append(String.format("%s %s", ppdKeyword, opt.getKeyword()));
            msg.append("<br>");
            for (final JsonProxyPrinterOptChoice choice : opt.getChoices()) {
                msg.append(String.format("%s %s ", ppdKeyword,
                        choice.getChoicePpd()));
                if (opt.getDefchoice().equals(choice.getChoice())) {
                    msg.append(String.format("<b>%s</b>", choice.getChoice()));
                } else {
                    msg.append(choice.getChoice());
                }
                msg.append("<br>");
            }
        }
        final Label label = new Label("content", msg.toString());
        label.setEscapeModelStrings(false);

        add(label);
    }

}
