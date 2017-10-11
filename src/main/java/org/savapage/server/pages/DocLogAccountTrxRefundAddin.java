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
package org.savapage.server.pages;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.services.ServiceContext;
import org.savapage.server.helpers.HtmlButtonEnum;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class DocLogAccountTrxRefundAddin extends AbstractAuthPage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * URL parameter for the {@link DocLog} database key.
     */
    private static final String PARM_DOCLOG_ID = "docLogId";

    /**
     * Uses {@link #PARM_DOCLOG_ID} to get the {@link DocLog}.
     *
     * @return The {@link DocLog}.
     */
    protected DocLog getDocLog() {
        return ServiceContext.getDaoContext().getDocLogDao()
                .findById(Long.valueOf(this.getParmValue(PARM_DOCLOG_ID)));
    }

    /**
     * @param parameters
     *            The {@link PageParameters}.
     */
    public DocLogAccountTrxRefundAddin(final PageParameters parameters) {
        super(parameters);

        final DocLog docLog = this.getDocLog();

        final MarkupHelper helper = new MarkupHelper(this);

        helper.addLabel("btn-no", HtmlButtonEnum.NO.uiText(getLocale()));

        helper.addModifyLabelAttr("btn-yes",
                HtmlButtonEnum.YES.uiText(getLocale()),
                MarkupHelper.ATTR_DATA_SAVAPAGE, docLog.getId().toString());
    }

    @Override
    protected boolean needMembership() {
        return false;
    }

}
