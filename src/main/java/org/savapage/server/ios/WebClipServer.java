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
package org.savapage.server.ios;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.handler.TextRequestHandler;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.SpException;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.util.Messages;

import net.iharder.Base64;

/**
 * Serving the {@link #CONTENT_TYPE_WEBCLIP} for iOS devices.
 * <p>
 * Zie <A href=
 * "http://developer.apple.com/library/ios/#featuredarticles/iPhoneConfigurationProfileRef/Introduction/Introduction.html"
 * >iPhoneConfigurationProfileRef</a>.
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public class WebClipServer extends WebPage {

    private static final long serialVersionUID = 1L;

    private static final String CONTENT_TYPE_WEBCLIP =
            "application/x-apple-aspen-config";

    private final static String ICON_57X57 = "apple-touch-icon.png";
    private final static String ICON_72X72 = "apple-touch-icon-72x72.png";

    private final static String ICON_IPHONE = ICON_57X57;

    private final static String ICON_IPOD = ICON_57X57;

    private final static String ICON_IPAD = ICON_72X72; // including iPad Mini

    /**
     * Delivers the {@link WebClipServer#CONTENT_TYPE_WEBCLIP}.
     *
     * @param parameters
     *            The page paramaters.
     */
    public WebClipServer(final PageParameters parameters) {

        HttpServletRequest request = (HttpServletRequest) getRequestCycle()
                .getRequest().getContainerRequest();

        final StringBuilder buffer = new StringBuilder();

        buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        buffer.append(
                "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n");

        buffer.append("<plist version=\"1.0\">\n<dict>\n");

        final String indent1 = "    ";
        final String indent2 = indent1 + indent1;
        final String indent3 = indent2 + indent1;

        final String basePayloadIdentifier = "org.savapage.webapp";

        // ====================================================================
        //
        // ====================================================================
        addKeyValue(buffer, indent1, "PayloadType", "Configuration");

        /*
         * A human-readable name for the profile. This name is displayed on the
         * Detail screen. It does not have to be unique.
         */
        addKeyValue(buffer, indent1, "PayloadDisplayName",
                CommunityDictEnum.SAVAPAGE.getWord());

        /*
         * A human-readable description of this payload. This description is
         * shown on the Detail screen.
         */

        String desc =
                localize("webclip-motto", CommunityDictEnum.SAVAPAGE.getWord());

        // String desc =
        // "Druk op \"Installeren\", en start de SavaPage app vanaf uw
        // home-scherm om te beginnen.";

        addKeyValue(buffer, indent1, "PayloadDescription", desc);

        /*
         * A human-readable string containing the name of the organization that
         * provided the profile. The payload organization for a payload need not
         * match the payload organization in the enclosing profile.
         */
        addKeyValue(buffer, indent1, "PayloadOrganization",
                ConfigManager.getAppOwner());

        /*
         * A reverse-DNS-style identifier for the specific payload.
         */
        addKeyValue(buffer, indent1, "PayloadIdentifier",
                basePayloadIdentifier);

        /*
         * Optional. If present and set to true, the user cannot delete the
         * profile (unless the profile has a removal password and the user
         * provides it).
         */
        addKeyValue(buffer, indent1, "PayloadRemovalDisallowed", false);

        /*
         *
         */
        addKeyValue(buffer, indent1, "PayloadVersion", Integer.valueOf(1));

        /*
         *
         */
        addKeyValue(buffer, indent1, "PayloadUUID",
                "de5354c2-a749-11e2-b26f-fb586f0fb8e5");

        // ====================================================================
        // Array of (1) payload dictionaries.
        // ====================================================================

        addKey(buffer, indent1, "PayloadContent").append(indent1 + "<array>\n");
        buffer.append(indent2 + "<dict>\n");

        // ---------------------------------------------------------------------
        // Undocumented by Apple (?), but needed !!!
        //
        // http://nwalker.org/2012/10/the-complete-guide-to-profile-manager-part-1/
        // ---------------------------------------------------------------------

        /*
         * Makes the web clip a standalone application. Otherwise, the web clip
         * will simply open in Safari.
         *
         * Fullscreen mode is nicer, because it gives an icon on the
         * multitasking bar. However, it does NOT work as expected. When tapping
         * this icon, fullscreen SavaPage gets the focus again, but goes back to
         * the login screen. Solution: see Mantis #162.
         */
        addKeyValue(buffer, indent3, "FullScreen", true);

        /*
         * Precomposed Icon (iOS only). Removes the glossy effect applied to iOS
         * icons. Visible in icon preview in Profile Manager.
         *
         * RRA: we want the glossy effect, so set to false.
         */
        addKeyValue(buffer, indent3, "Precomposed", false);

        // ---------------------------------------------------------------------
        // Payload Dictionary Keys Common to All Payloads.
        // ---------------------------------------------------------------------

        /*
         * Designate as Web Clip Payload.
         */
        addKeyValue(buffer, indent3, "PayloadType",
                "com.apple.webClip.managed");

        /*
         * The version number of the individual payload.
         */
        addKeyValue(buffer, indent3, "PayloadVersion", Integer.valueOf(1));

        /*
         * A reverse-DNS-style identifier for the specific payload. It is
         * usually the same identifier as the root-level PayloadIdentifier value
         * with an additional component appended.
         */
        addKeyValue(buffer, indent3, "PayloadIdentifier",
                basePayloadIdentifier + ".webclip");

        /*
         * A globally unique identifier for the payload. The actual content is
         * unimportant, but it must be globally unique. In Linux you can use
         * uuid to generate reasonable UUIDs:
         *
         * $ uuid -m -1
         */
        addKeyValue(buffer, indent3, "PayloadUUID",
                "ea8a9a18-a747-11e2-8867-6746135f76ba");

        // ---------------------------------------------------------------------
        // Web Clip Payload-Specific Property Keys.
        // ---------------------------------------------------------------------

        /*
         * The URL that the Web Clip should open when clicked. The URL must
         * begin with HTTP or HTTPS or it won't work.
         */
        addURL(request, buffer, indent3, "/user");

        /*
         * The name of the Web Clip as displayed on the Home screen.
         */
        addKeyValue(buffer, indent3, "Label",
                CommunityDictEnum.SAVAPAGE.getWord());

        /*
         * A PNG icon to be shown on the Home screen.
         */
        addIcon(buffer, indent3, getIconFile(request));

        /*
         * Removable (iOS only) Whether or not the web clip can be removed from
         * the device. On OS X, the web clip can always be removed from the
         * Dock.
         *
         * If No, the user cannot remove the Web Clip, but it will be removed if
         * the profile is deleted.
         */
        addKeyValue(buffer, indent3, "IsRemovable", true);

        // ---------------------------------------------------------------------
        buffer.append(indent2 + "</dict>\n");
        buffer.append(indent1 + "</array>\n");

        // ---------------------------------------------------------------------
        //
        // ---------------------------------------------------------------------
        buffer.append("</dict>\n" + "</plist>");

        // ---------------------------------------------------------------------
        final String text = StringUtils.toEncodedString(
                buffer.toString().getBytes(), Charset.forName("UTF-8"));

        getRequestCycle().scheduleRequestHandlerAfterCurrent(
                new TextRequestHandler(CONTENT_TYPE_WEBCLIP, "UTF-8", text));
    }

    /**
     *
     * @param buffer
     * @param indent
     * @param key
     * @return
     */
    private StringBuilder addKey(StringBuilder buffer, String indent,
            String key) {
        return buffer.append(indent + "<key>" + key + "</key>\n");
    }

    /**
     *
     * @param buffer
     * @param indent
     * @param key
     * @param value
     * @return
     */
    private StringBuilder addKeyValue(StringBuilder buffer, String indent,
            String key, String value) {
        return addKey(buffer, indent, key)
                .append(indent + "<string>" + value + "</string>\n");
    }

    /**
     *
     * @param buffer
     * @param indent
     * @param key
     * @param value
     * @return
     */
    private StringBuilder addKeyValue(StringBuilder buffer, String indent,
            String key, boolean value) {
        addKey(buffer, indent, key).append(indent);
        if (value) {
            buffer.append("<true/>");
        } else {
            buffer.append("<false/>");
        }
        return buffer.append("\n");
    }

    /**
     * Gets the icon file belonging to the requesting device.
     *
     * @return The icon PNG file.
     */
    private File getIconFile(HttpServletRequest request) {

        ServletContext servletContext =
                WebApplication.get().getServletContext();
        String docroot = servletContext.getRealPath("/");

        File icon = new File(docroot, getIconFileName(request));

        return icon;
    }

    /**
     *
     * @param request
     * @return
     */
    private String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    /**
     * Gets the icon file name belonging to the requesting device.
     * <p>
     * Size for:
     * </p>
     *
     * <pre>
     * iPhone and iPod touch                    :  57 x  57
     * iPhone 5 and iPod touch (5th generation) : 114 x 114
     * High-resolution iPhone and iPod touch    : 114 x 114
     * iPad                                     :  72 x  72
     * high-resolution iPad                     : 144 x 144
     * </pre>
     *
     * <p>
     * NOTE: Only low-res 57x57 and 72x72 icons and are returned, because how to
     * recognize high-res devices, so 114x114 and 144x144 can be used?
     * </p>
     *
     * @param request
     *            The HTTP request.
     * @return The filename of the PNG icon file name.
     */
    private String getIconFileName(HttpServletRequest request) {

        String iconFileName = null;
        final String userAgent = getUserAgent(request).toLowerCase();

        if (StringUtils.contains(userAgent, "iphone")) {
            iconFileName = ICON_IPHONE;
        } else if (StringUtils.contains(userAgent, "ipod")) {
            iconFileName = ICON_IPOD;
        } else if (StringUtils.contains(userAgent, "ipad")) {
            iconFileName = ICON_IPAD;
        } else {
            iconFileName = ICON_72X72;
        }

        return iconFileName;
    }

    /**
     *
     *
     * @param buffer
     * @param indent
     * @param icon
     * @return
     */
    private StringBuilder addIcon(StringBuilder buffer, String indent,
            File icon) {

        byte[] iconBytes;
        try {
            iconBytes = FileUtils.readFileToByteArray(icon);
        } catch (IOException e) {
            throw new SpException(e);
        }

        return addKey(buffer, indent, "Icon").append(indent + "<data>"
                + Base64.encodeBytes(iconBytes) + "</data>\n");
    }

    /**
     *
     * @param buffer
     * @param indent
     * @param pathApp
     */
    private void addURL(HttpServletRequest request, StringBuilder buffer,
            String indent, String pathApp) {

        URI uriReq = URI.create(request.getRequestURL().toString());
        URI uriApp = null;

        try {
            uriApp = new URI(uriReq.getScheme(), uriReq.getUserInfo(),
                    uriReq.getHost(), uriReq.getPort(), pathApp,
                    uriReq.getQuery(), uriReq.getFragment());
        } catch (URISyntaxException e) {
            throw new SpException(e);
        }

        addKeyValue(buffer, indent, "URL", uriApp.toString());
    }

    /**
     *
     * @param buffer
     * @param indent
     * @param key
     * @param value
     * @return
     */
    private StringBuilder addKeyValue(StringBuilder buffer, String indent,
            String key, Integer value) {
        return addKey(buffer, indent, key)
                .append(indent + "<integer>" + value + "</integer>\n");
    }

    /**
     * Return a localized message string. IMPORTANT: The locale from the session
     * is used.
     *
     * @param key
     *            The key of the message.
     * @param args
     *            The placeholder arguments for the message template.
     *
     * @return The message text.
     */
    private String localize(final String key, final String... args) {
        return Messages.getMessage(getClass(), getSession().getLocale(), key,
                args);
    }

}
