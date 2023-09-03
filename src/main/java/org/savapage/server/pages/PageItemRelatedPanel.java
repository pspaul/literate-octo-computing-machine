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
package org.savapage.server.pages;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class PageItemRelatedPanel extends PageItemIconPanel {

    /** */
    private static final long serialVersionUID = 1L;

    /**
     * @param id
     *            The non-null id of this component.
     */
    public PageItemRelatedPanel(final String id) {
        super(id);
    }

    /**
     * @param nRelated
     *            Number of relations.
     * @param txtRelated
     *            Relations.
     * @param imgRelated
     *            Path to img file.
     */
    protected final void populate(final int nRelated, final String txtRelated,
            final String imgRelated) {

        this.populate(new MarkupHelper(this), "related-count", nRelated,
                "related", txtRelated, "related-img", imgRelated);
    }

}
