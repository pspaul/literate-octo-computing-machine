/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.services.helpers.PageScalingEnum;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.pages.MarkupHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class PrinterSettings extends AbstractUserPage {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    public PrinterSettings(final PageParameters parameters) {

        super(parameters);

        final String htmlRadioName = "print-page-scaling-enum";

        labelledRadio("page-scaling", "-none", htmlRadioName,
                PageScalingEnum.NONE.toString(), false);

        labelledRadio("page-scaling", "-fit", htmlRadioName,
                PageScalingEnum.FIT.toString(), false);

        final MarkupHelper helper = new MarkupHelper(this);

        helper.addButton("button-next", HtmlButtonEnum.NEXT);
        helper.addButton("button-default", HtmlButtonEnum.DEFAULT);
    }

}
