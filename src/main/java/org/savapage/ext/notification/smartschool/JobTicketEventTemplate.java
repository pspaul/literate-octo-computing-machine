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
package org.savapage.ext.notification.smartschool;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.savapage.core.template.TemplateAttrEnum;
import org.savapage.core.template.TemplateMixin;
import org.savapage.core.template.dto.TemplateDto;
import org.savapage.core.template.dto.TemplateJobTicketDto;
import org.savapage.core.template.dto.TemplateUserDto;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class JobTicketEventTemplate extends TemplateMixin {

    /** */
    protected static final String RESOURCE_KEY_SUBJECT = "subject";

    /** */
    protected static final String RESOURCE_KEY_HTML = "html";

    /** */
    protected static final String RESOURCE_KEY_TEXT = "text";

    /** */
    private final File directory;

    /** */
    private final TemplateJobTicketDto ticketDto;

    /** */
    private final TemplateUserDto userDto;

    /**
     *
     * @param customHome
     *            The directory location of the XML resource.
     * @param ticket
     *            The ticket.
     * @param user
     *            The user.
     */
    public JobTicketEventTemplate(final File customHome,
            final TemplateJobTicketDto ticket, final TemplateUserDto user) {
        super();
        this.directory = customHome;
        this.ticketDto = ticket;
        this.userDto = user;
    }

    /** */
    private static void clearCache() {
        ResourceBundle.clearCache();
    }

    /**
     * Renders the template.
     *
     * @param resourceName
     *            The name of the XML resource without the locale suffix and
     *            file extension.
     * @param asHtml
     *            If {@code true} rendered as HTML, otherwise as plain text
     * @param locale
     *            The {@link Locale}.
     * @return The rendered template.
     */
    public JobTicketEventMessage render(final String resourceName,
            final boolean asHtml, final Locale locale) {

        final ResourceBundle rb =
                this.getResourceBundle(this.directory, resourceName, locale);

        final JobTicketEventMessage msg = new JobTicketEventMessage();

        msg.setTitle(
                this.render(rb, rb.getString(RESOURCE_KEY_SUBJECT), locale));
        msg.setBody(this.render(rb, rb.getString(RESOURCE_KEY_HTML), locale));

        return msg;
    }

    @Override
    protected Map<String, TemplateDto> onRender(final Locale locale) {
        final Map<String, TemplateDto> map = new HashMap<>();
        map.put(TemplateAttrEnum.USER.asAttr(), this.userDto);
        map.put(TemplateAttrEnum.TICKET.asAttr(), this.ticketDto);
        return map;
    }

    @Override
    protected Map<String, String> onRender(final ResourceBundle rcBundle) {
        return null;
    }
}
