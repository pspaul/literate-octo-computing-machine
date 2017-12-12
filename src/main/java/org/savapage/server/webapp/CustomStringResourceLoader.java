/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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
package org.savapage.server.webapp;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.internal.Enclosure;
import org.apache.wicket.resource.loader.IStringResourceLoader;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.util.Messages;

/**
 * A Wicket resource loader that uses custom strings (if available).
 *
 * @author Rijk Ravestein
 *
 */
public final class CustomStringResourceLoader implements IStringResourceLoader {

    @Override
    public String loadStringResource(final Class<?> clazz, final String key,
            final Locale locale, final String style, final String variation) {

        try {
            final ResourceBundle bundle = Messages.loadXmlResource(
                    ConfigManager.getServerCustomI18nHome(clazz),
                    clazz.getSimpleName(), locale);

            if (bundle.getLocale().getLanguage().equals(locale.getLanguage())
                    && bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        } catch (MissingResourceException e) {
            // no code intended;
        }
        return null;
    }

    @Override
    public String loadStringResource(final Component component,
            final String key, final Locale locale, final String style,
            final String variation) {

        final Component componentWlk;

        if (component instanceof Enclosure) {
            componentWlk = component.getParent();
        } else {
            componentWlk = component;
        }

        return loadStringResource(componentWlk.getClass(), key, locale, style,
                variation);
    }

}
