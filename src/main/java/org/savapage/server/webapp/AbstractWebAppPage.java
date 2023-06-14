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
package org.savapage.server.webapp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.head.PriorityHeaderItem;
import org.apache.wicket.markup.head.StringHeaderItem;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.request.mapper.parameter.INamedParameters.NamedPair;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.UrlResourceReference;
import org.savapage.core.VersionInfo;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.community.MemberCard;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.config.WebAppTypeEnum;
import org.savapage.core.dao.enums.AppLogLevelEnum;
import org.savapage.core.util.InetUtils;
import org.savapage.server.CustomWebServlet;
import org.savapage.server.LibreJsLicenseServlet;
import org.savapage.server.WebApp;
import org.savapage.server.WebAppParmEnum;
import org.savapage.server.WebServer;
import org.savapage.server.api.UserAgentHelper;
import org.savapage.server.pages.AbstractPage;
import org.savapage.server.pages.LibreJsHelper;
import org.savapage.server.pages.LibreJsLicenseEnum;
import org.savapage.server.pages.LibreJsLicensePanel;
import org.savapage.server.pages.MessageContent;
import org.savapage.server.session.SpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class AbstractWebAppPage extends AbstractPage
        implements IHeaderContributor {

    /** */
    private static final long serialVersionUID = 1L;

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AbstractWebAppPage.class);

    /** */
    private static final String[] CSS_REQ_FILENAMES_DEFAULT = new String[] {};

    /**
     * JavaScript snippet format to render initially. The %s is the mountPath of
     * this page.
     */
    private static final String INITIAL_JAVASCRIPT_FORMAT =
            "(function(){" + "if(window.location.href.search('#')>0){"
                    + "window.location.replace("
                    + "window.location.protocol+\"//\"+window.location.host+\""
                    + "/" + "%s" + "\");}})();";

    /**
     * {@link StringHeaderItem} pattern to render initially. The first %s is the
     * SavaPage version. The second %s is the initial JavaScript as defined in
     * {@link #INITIAL_JAVASCRIPT_PATTERN}.
     */
    private static final String INITIAL_HEADERITEM_FORMAT =
            "\n<script type=\"text/javascript\">\n" + "/* %s */\n"
                    + "/*<![CDATA[*/" + "\n" + "%s" + "\n" + "/*]]>*/"
                    + "\n</script>\n";

    /**
     * .
     */
    public static final String WEBJARS_PATH_JQUERY_MOBILE_JS =
            "jquery-mobile/current/jquery.mobile.js";

    /**
     * Stylesheet of the standard jQuery Mobile theme.
     */
    public static final String WEBJARS_PATH_JQUERY_MOBILE_CSS =
            "jquery-mobile/current/jquery.mobile.css";

    /**
     * Stylesheet to be used with custom jQuery Mobile theme as produced with
     * <a href="http://themeroller.jquerymobile.com/">themeroller</a>.
     */
    public static final String WEBJARS_PATH_JQUERY_MOBILE_STRUCTURE_CSS =
            "jquery-mobile/current/jquery.mobile.structure.css";

    /**
     * .
     */
    public static final String WEBJARS_PATH_JQUERY_SPARKLINE =
            "jquery.sparkline/current/jquery.sparkline.js";

    /**
     * .
     */
    public static final String WEBJARS_PATH_JQUERY_JQPLOT_JS =
            "jqplot/current/jquery.jqplot.js";

    /**
     * .
     */
    public static final String WEBJARS_PATH_JQUERY_JQPLOT_CSS =
            "jqplot/current/jquery.jqplot.css";

    /**
     * JavaScript libraries available for rendering.
     */
    protected enum JavaScriptLibrary {
        /** */
        COMETD,
        /** */
        JQPLOT,
        /** */
        MOBIPICK,
        /** */
        SPARKLINE
    }

    protected static final LibreJsLicenseEnum SAVAPAGE_JS_LICENSE =
            LibreJsLicenseEnum.AGPL_3_0;

    /**
     * .
     */
    private static final String CSS_FILE_WEBAPP_DEFAULT = "webapp.css";

    /**
     * .
     */
    private static final String CSS_FILE_WICKET_SAVAPAGE =
            "wicket.savapage.css";

    /**
     * .
     */
    private static final String CSS_FILE_JQUERY_SAVAPAGE =
            "jquery.savapage.css";

    /**
     * .
     */
    static final String CSS_FILE_JQUERY_MOBILE_THEME_ICONS =
            "jquery.mobile.icons.min.css";

    /**
     *
     * @param suffix
     *            The title suffix, {@code null}. when not applicable.
     * @return The web title
     */
    protected static String getWebAppTitle(final String suffix) {

        final StringBuilder title = new StringBuilder();

        title.append(CommunityDictEnum.SAVAPAGE.getWord());

        final String organisation =
                MemberCard.instance().getMemberOrganization();

        if (StringUtils.isNotBlank(organisation)) {
            title.append(" :: ").append(organisation);
        }

        if (StringUtils.isNotBlank(suffix)) {
            title.append(" :: ").append(suffix);
        }
        return title.toString();
    }

    /**
     * Common initialization.
     */
    private static void commonInit() {
        WebApp.get().lazyReplaceJsReference();
    }

    /**
     * .
     */
    protected AbstractWebAppPage() {
        super();
        commonInit();
    }

    /**
     *
     * @param parameters
     *            The {@link PageParameters}.
     */
    protected AbstractWebAppPage(final PageParameters parameters) {

        super(parameters);

        commonInit();

        final String language =
                parameters.get(WebAppParmEnum.SP_LANG.parm()).toString();

        if (language != null) {
            getSession().setLocale(new Locale(language));
        }
    }

    /**
     * Checks if Web App access from Internet is allowed.
     *
     * @param keyAccessEnabled
     *            Key to check if internet access is enabled.
     * @throws RestartResponseException
     *             When access from Internet is not allowed.
     */
    protected final void
            checkInternetAccess(final IConfigProp.Key keyAccessEnabled) {

        final ConfigManager cm = ConfigManager.instance();

        if (cm.isConfigValue(Key.WEBAPP_INTERNET_ENABLE)
                && cm.isConfigValue(keyAccessEnabled)) {
            return;
        }

        final String remoteAddr = this.getClientIpAddr();

        if (InetUtils.isPublicAddress(remoteAddr)) {

            LOGGER.warn("Access to {} Web App denied for {}",
                    this.getWebAppType(), remoteAddr);

            throw new RestartResponseException(new MessageContent(
                    AppLogLevelEnum.ERROR,
                    "This application is not available on the Internet."));
        }
    }

    /**
     * Sets {@link WebAppCountExceededMsg} as response page with
     * {@link WebAppTypeEnum} as {@link PageParameter}.
     */
    protected final void setWebAppCountExceededResponse() {
        final PageParameters parms = new PageParameters();
        parms.set(WebAppParmEnum.SP_APP.parm(), this.getWebAppType());
        setResponsePage(WebAppCountExceededMsg.class, parms);
    }

    /**
     * Checks if the WebApp count is exceeded.
     * <p>
     * Note: we do NOT check on mobile and Mac OS X Safari browsers, i.e. we
     * always return {@code false} in this case.
     * </p>
     *
     * @param parameters
     *            The {@link PageParameters}.
     * @return {@code true} when WebApp count is exceeded.
     */
    protected final boolean
            isWebAppCountExceeded(final PageParameters parameters) {

        final UserAgentHelper userAgentHelper = createUserAgentHelper();

        if (userAgentHelper.isMobileBrowser()
                || userAgentHelper.isSafariBrowserMacOsX()) {
            return false;
        }

        final boolean isZeroPanel =
                !parameters.get(WebAppParmEnum.SP_ZERO.parm()).isEmpty();

        return !isZeroPanel && SpSession.get().getAuthWebAppCount() > 1;
    }

    /**
     * @return The {@link WebAppTypeEnum} of this Web App.
     */
    protected abstract WebAppTypeEnum getWebAppType();

    /**
     * Gets the specialized CSS base filenames that are required by
     * {@link #getSpecializedCssFileName()}.
     *
     * @return Empty array if no CSS files are applicable.
     */
    protected String[] getSpecializedCssReqFileNames() {
        return CSS_REQ_FILENAMES_DEFAULT;
    }

    /**
     * Gets the specialized CSS base filename.
     *
     * @return {@code null} if no specialized CSS files are applicable.
     */
    protected abstract String getSpecializedCssFileName();

    /**
     * Gets the specialized JS base filename.
     *
     * @return The specialized JS filename.
     */
    protected abstract String getSpecializedJsFileName();

    /**
     * Gets the JavaScript libraries to render.
     *
     * @return The JavaScript libraries to render.
     */
    protected abstract Set<JavaScriptLibrary> getJavaScriptToRender();

    /**
     * Renders tiny JavaScript snippet at the very start of the page to
     * workaround the Browser F5 refresh problem.
     * <p>
     * This method is WORKAROUND (not a final solution) of the browser F5
     * refresh PROBLEM. The F5 is special, since it does not "deep refresh" the
     * Web App Page: the result is a page NOT rendered by JQM.
     * </p>
     * <p>
     * We render a tiny JavaScript snippet at the very start of the page that:
     * <ol>
     * <li>Identifies the F5 by checking if a hash (#) is part of the URL. The
     * hash is injected into the URL by JQM, so we know we are in the midst of a
     * SavaPage WebApp dialog, and F5 is the most likely cause we are here.</li>
     *
     * <li>Redirects to the clean URL when F5 is identified. As the redirect
     * happens at the top of the page there is a minimum load, since .css and
     * .js did not get a changes to leaded.</li>
     * </ol>
     * </p>
     * <p>
     * <b>NOTE</b>: If the # is part of a bookmarked page revisited, the page is
     * unnecessary re-loaded.
     * </p>
     *
     * @see Mantis #254.
     * @param response
     *            The {@link IHeaderResponser}.
     */
    protected final void
            renderInitialJavaScript(final IHeaderResponse response) {
        /*
         * Use the current URL path as mount path ...
         */
        final StringBuilder mountPath = new StringBuilder();
        mountPath.append(this.getRequest().getUrl().getPath());

        /*
         * ... and append our own sp-* URL parameters.
         */
        int nParms = 0;

        for (final NamedPair pair : this.getPageParameters().getAllNamed()) {

            /*
             * Skip transient parameters.
             */
            if (pair.getKey().equals(WebAppParmEnum.SP_ZERO.parm())) {
                continue;
            }

            /*
             * IMPORTANT: The WebAppParmEnum.SP_OAUTH, at this point, means that
             * the OAuth callback is already handled. Therefore we add a
             * separate indicator parameter so javascript can identify OAuth and
             * further handle the login.
             */
            if (pair.getKey().equals(WebAppParmEnum.SP_OAUTH.parm())) {
                mountPath.append("?")
                        .append(WebAppParmEnum.SP_LOGIN_OAUTH.parm())
                        .append("=").append(pair.getValue());
                nParms++;
                continue;
            }

            /*
             * IMPORTANT: Keep the WebAppParmEnum.SP_LOGIN_OAUTH.parm().
             */

            /*
             * Preserve sp-* parms.
             */
            if (pair.getKey().startsWith(WebAppParmEnum.parmPrefix())) {

                if (nParms == 0) {
                    mountPath.append("?");
                } else {
                    mountPath.append("&");
                }

                mountPath.append(pair.getKey()).append("=")
                        .append(pair.getValue());

                nParms++;
            }
        }

        response.render(new PriorityHeaderItem(
                new StringHeaderItem(LibreJsHelper.getInitialHeaderScript())));

        final String javascript =
                String.format(INITIAL_JAVASCRIPT_FORMAT, mountPath.toString());

        final HeaderItem firstItem = new PriorityHeaderItem(
                new StringHeaderItem(String.format(INITIAL_HEADERITEM_FORMAT,
                        ConfigManager.getAppNameVersionBuild(), javascript)));

        response.render(firstItem);
    }

    /**
     * Renders a JavaScript file.
     *
     * @param response
     *            The {@link IHeaderResponser}.
     * @param url
     *            The URL of the file to render.
     */
    private final void renderJs(final IHeaderResponse response,
            final String url) {
        response.render(JavaScriptHeaderItem.forUrl(url));
    }

    /**
     * Renders a Wicket JavaScript headeritem.
     *
     * @param response
     *            Header response.
     * @param lic
     *            License.
     * @param orgItem
     *            Original header item.
     */
    private void renderLibreJs(final IHeaderResponse response,
            final LibreJsLicenseEnum lic,
            final JavaScriptReferenceHeaderItem orgItem) {

        final String urlPath = WebApp.getLibreJsResourceRef(true, lic, orgItem);

        final UrlResourceReference urlResRef =
                WebApp.createUrlResourceRef(urlPath);

        response.render(new JavaScriptReferenceHeaderItem(urlResRef,
                orgItem.getPageParameters(), orgItem.getId(), orgItem.isDefer(),
                orgItem.getCharset(), orgItem.getCondition()));
    }

    /**
     * Returns the 'nocache' URL parameter to be appended to rendered SavaPage
     * files.
     * <p>
     * This is needed so the web browser is triggered to reload the JS and CSS
     * files when there is a new Web App version.
     * </p>
     *
     * @return the URL parameter.
     */
    protected final String getNoCacheUrlParm() {
        final long nocache;
        if (WebServer.isDeveloperEnv()) {
            nocache = System.currentTimeMillis();
        } else {
            nocache = VersionInfo.BUILD_EPOCH_SECS;
        }
        return new StringBuilder().append("?").append(nocache).toString();
    }

    /**
     * Checks of JQuery Core is "automatically" rendered by Wicket, using the
     * replacement we defined in {@link WebApp#replaceJQueryCore()}.
     *
     * @return {@code true} if JQuery Core is already rendered.
     */
    abstract boolean isJqueryCoreRenderedByWicket();

    /**
     * The file name from the web property file.
     *
     * @param key
     *            The property key.
     * @return {@code null} when not found (or empty).
     */
    private static String getCssCustomFile(final String key) {

        if (key == null) {
            return null;
        }

        final String cssFile = WebApp.getWebProperty(key);

        if (StringUtils.isBlank(cssFile)) {
            return null;
        }
        return cssFile;
    }

    /**
     * The file name from configuration property.
     *
     * @param key
     *            The property key.
     * @return {@code null} when not found (or empty).
     */
    private static String getCssFileName(final IConfigProp.Key key) {
        final String fileName = ConfigManager.instance().getConfigValue(key);
        if (StringUtils.isNotBlank(fileName)) {
            return fileName;
        }
        return null;
    }

    /**
     * @param webAppType
     *            The {@link WebAppTypeEnum}.
     * @return The jQuery Mobile Theme CSS file, or {@code null} when not
     *         applicable.
     */
    private String getCssThemeFileName(final WebAppTypeEnum webAppType) {

        final IConfigProp.Key configKey;
        switch (webAppType) {
        case ADMIN:
            configKey = Key.WEBAPP_THEME_ADMIN;
            break;
        case PRINTSITE:
            configKey = Key.WEBAPP_THEME_PRINTSITE;
            break;
        case JOBTICKETS:
            configKey = Key.WEBAPP_THEME_JOBTICKETS;
            break;
        case MAILTICKETS:
            configKey = Key.WEBAPP_THEME_MAILTICKETS;
            break;
        case POS:
            configKey = Key.WEBAPP_THEME_POS;
            break;
        case USER:
            configKey = Key.WEBAPP_THEME_USER;
            break;
        case PAYMENT:
            configKey = Key.WEBAPP_THEME_PAYMENT;
            break;
        default:
            configKey = null;
            break;
        }

        if (configKey != null) {
            final String cssFile = getCssFileName(configKey);
            if (cssFile != null) {
                return cssFile;
            }
        }

        final StringBuilder key = new StringBuilder();
        key.append(Key.WEBAPP_THEME_PFX)
                .append(webAppType.toString().toLowerCase());
        return getCssCustomFile(key.toString());
    }

    /**
     * @param webAppType
     *            The type of Web App.
     * @return The custom CSS filename, or {@code null} when not applicable.
     */
    private String getCssCustomFileName(final WebAppTypeEnum webAppType) {

        final IConfigProp.Key configKey;
        switch (webAppType) {
        case ADMIN:
            configKey = Key.WEBAPP_CUSTOM_ADMIN;
            break;
        case PRINTSITE:
            configKey = Key.WEBAPP_CUSTOM_PRINTSITE;
            break;
        case JOBTICKETS:
            configKey = Key.WEBAPP_CUSTOM_JOBTICKETS;
            break;
        case MAILTICKETS:
            configKey = Key.WEBAPP_CUSTOM_MAILTICKETS;
            break;
        case POS:
            configKey = Key.WEBAPP_CUSTOM_POS;
            break;
        case USER:
            configKey = Key.WEBAPP_CUSTOM_USER;
            break;
        case PAYMENT:
            configKey = Key.WEBAPP_CUSTOM_PAYMENT;
            break;
        default:
            configKey = null;
            break;
        }

        if (configKey != null) {
            final String cssFile = getCssFileName(configKey);
            if (cssFile != null) {
                return cssFile;
            }
        }

        final StringBuilder key = new StringBuilder();
        key.append(Key.WEBAPP_CUSTOM_PFX)
                .append(webAppType.toString().toLowerCase());
        return getCssCustomFile(key.toString());
    }

    /**
     * Adds contributions to the html head.
     * <p>
     * <b>Note</b>:
     * <ul>
     * <li>The jQuery Core library is NOT rendered here. This is because Wicket
     * renders its own built-in version (when needed by the Wicket framework),
     * and we do NOT want jQuery Core to be rendered twice.</li>
     * <li>Since the built-in version (probably) is different from the one we
     * need for JQuery Mobile and other jQuery plugins, we replace the built-in
     * version with the one we need in {@link WebApp#init()}</li>
     * <li>When Wicket does NOT include the built-in jQuery Core, because no
     * widget makes use of it, we should render it here.</li>
     * </ul>
     * </p>
     *
     * @see {@link WebApp#replaceJQueryCore()}.
     */
    @Override
    public final void renderHead(final IHeaderResponse response) {

        this.renderInitialJavaScript(response);

        final String nocache = this.getNoCacheUrlParm();

        final Set<JavaScriptLibrary> jsToRender = this.getJavaScriptToRender();

        WebAppTypeEnum webAppType = this.getWebAppType();

        if (webAppType == null) {
            webAppType = this.getSessionWebAppType();
        }
        if (webAppType == null) {
            webAppType = WebAppTypeEnum.UNDEFINED;
        }
        /*
         * jQuery Mobile CSS files.
         */
        final String customThemeCssFileName =
                this.getCssThemeFileName(webAppType);

        if (customThemeCssFileName != null) {
            response.render(CssHeaderItem.forUrl(
                    String.format("/%s/%s%s", CustomWebServlet.PATH_BASE_THEMES,
                            customThemeCssFileName, nocache)));

            response.render(CssHeaderItem.forUrl(String.format("/%s/%s%s%s",
                    CustomWebServlet.PATH_BASE_THEMES,
                    FilenameUtils.getPath(customThemeCssFileName),
                    CSS_FILE_JQUERY_MOBILE_THEME_ICONS, nocache)));

            response.render(WebApp.getWebjarsCssRef(
                    WEBJARS_PATH_JQUERY_MOBILE_STRUCTURE_CSS));

        } else {
            response.render(
                    WebApp.getWebjarsCssRef(WEBJARS_PATH_JQUERY_MOBILE_CSS));
        }

        /*
         * Other CSS files.
         */
        if (jsToRender.contains(JavaScriptLibrary.JQPLOT)) {
            response.render(
                    WebApp.getWebjarsCssRef(WEBJARS_PATH_JQUERY_JQPLOT_CSS));
        }

        response.render(CssHeaderItem.forUrl(
                String.format("%s%s", CSS_FILE_WICKET_SAVAPAGE, nocache)));

        response.render(CssHeaderItem.forUrl(
                String.format("%s%s", CSS_FILE_JQUERY_SAVAPAGE, nocache)));

        for (final String cssFile : this.getSpecializedCssReqFileNames()) {
            response.render(CssHeaderItem
                    .forUrl(String.format("%s%s", cssFile, nocache)));
        }

        final String specializedCssFile = this.getSpecializedCssFileName();

        if (specializedCssFile != null) {
            response.render(CssHeaderItem.forUrl(
                    String.format("%s%s", specializedCssFile, nocache)));
        }

        if (jsToRender.contains(JavaScriptLibrary.MOBIPICK)) {
            response.render(CssHeaderItem.forUrl(String.format("%s%s",
                    WebApp.getJqMobiPickLocation(), "mobipick.css")));
        }

        // Custom CSS as last.
        final String customCssFileName = this.getCssCustomFileName(webAppType);

        if (customCssFileName == null) {
            if (ConfigManager.instance()
                    .isConfigValue(Key.WEBAPP_STYLE_DEFAULT)) {
                response.render(CssHeaderItem.forUrl(String.format("%s%s%s",
                        WebApp.getDefaultStyleLocation(),
                        CSS_FILE_WEBAPP_DEFAULT, nocache)));
            }
        } else {
            response.render(CssHeaderItem.forUrl(String.format("/%s/%s%s",
                    CustomWebServlet.PATH_BASE, customCssFileName, nocache)));
        }

        /*
         * JS files.
         */
        if (!isJqueryCoreRenderedByWicket()) {
            this.renderLibreJs(response, LibreJsLicenseEnum.MIT,
                    WebApp.getWebjarsJsRef(WebApp.WEBJARS_PATH_JQUERY_CORE_JS));
        }

        for (final Pair<String, LibreJsLicenseEnum> pair : this
                .getJavaScriptFiles()) {
            this.renderJs(response, pair.getKey());
        }
    }

    /**
     * Gets he {@link WebAppTypeEnum} specific JS files.
     *
     * @param list
     *            List to append opn.
     * @param nocache
     *            The "nocache" string.
     *
     */
    protected abstract void appendWebAppTypeJsFiles(
            List<Pair<String, LibreJsLicenseEnum>> list, String nocache);

    /**
     * Gets JavaScript files to render, <i>except</i> the main (first)
     * {@link WebApp#WEBJARS_PATH_JQUERY_CORE_JS}.
     *
     * @return List of files in render order.
     */
    private List<Pair<String, LibreJsLicenseEnum>> getJavaScriptFiles() {

        final Set<JavaScriptLibrary> jsToRender = this.getJavaScriptToRender();
        final String nocache = this.getNoCacheUrlParm();
        final List<Pair<String, LibreJsLicenseEnum>> list = new ArrayList<>();

        if (jsToRender.contains(JavaScriptLibrary.SPARKLINE)) {

            final LibreJsLicenseEnum license = LibreJsLicenseEnum.BSD_3_CLAUSE;

            list.add(
                    new ImmutablePair<>(
                            WebApp.getLibreJsResourceRef(true, license,
                                    WebApp.getWebjarsJsRef(
                                            WEBJARS_PATH_JQUERY_SPARKLINE)),
                            license));
        }

        if (jsToRender.contains(JavaScriptLibrary.JQPLOT)) {

            final LibreJsLicenseEnum license = LibreJsLicenseEnum.GPL_2_0;

            list.add(
                    new ImmutablePair<>(
                            WebApp.getLibreJsResourceRef(true, license,
                                    WebApp.getWebjarsJsRef(
                                            WEBJARS_PATH_JQUERY_JQPLOT_JS)),
                            license));

            for (String plugin : new String[] { "jqplot.highlighter.js",
                    "jqplot.pieRenderer.js", "jqplot.json2.js",
                    "jqplot.logAxisRenderer.js",
                    "jqplot.dateAxisRenderer.js" }) {

                list.add(new ImmutablePair<>(
                        WebApp.getLibreJsResourceRef(true, license,
                                WebApp.getWebjarsJsRef(String.format(
                                        "jqplot/current/plugins/%s", plugin))),
                        license));
            }
        }

        list.add(new ImmutablePair<>(
                this.getJsPathForRender("jquery/json2.js",
                        LibreJsLicenseEnum.CC0_1_0),
                LibreJsLicenseEnum.CC0_1_0));

        if (jsToRender.contains(JavaScriptLibrary.COMETD)) {

            // Use nocache, to prevent loading of old browser cached .js files,
            // when cometd is upgraded.

            list.add(
                    new ImmutablePair<>(
                            this.getJsPathForRender(
                                    String.format("%s%s",
                                            "org/cometd/cometd.js", nocache),
                                    LibreJsLicenseEnum.APACHE_2_0),
                            LibreJsLicenseEnum.APACHE_2_0));

            list.add(
                    new ImmutablePair<>(
                            this.getJsPathForRender(
                                    String.format("%s%s",
                                            "jquery/jquery.cometd.js", nocache),
                                    LibreJsLicenseEnum.APACHE_2_0),
                            LibreJsLicenseEnum.APACHE_2_0));
        }

        list.add(new ImmutablePair<>(
                String.format("%s%s", "savapage.js", nocache),
                SAVAPAGE_JS_LICENSE));
        list.add(new ImmutablePair<>(
                String.format("%s%s", "jquery.savapage.js", nocache),
                SAVAPAGE_JS_LICENSE));

        this.appendWebAppTypeJsFiles(list, nocache);

        /*
         * Note: render jQuery Mobile AFTER jquery.savapage.js, because the
         * $(document).bind("mobileinit") is implemented in jquery.savapage.js
         */
        list.add(
                new ImmutablePair<>(
                        WebApp.getLibreJsResourceRef(true,
                                LibreJsLicenseEnum.MIT,
                                WebApp.getWebjarsJsRef(
                                        WEBJARS_PATH_JQUERY_MOBILE_JS)),
                        LibreJsLicenseEnum.MIT));

        /*
         * Render after JQM.
         */
        if (jsToRender.contains(JavaScriptLibrary.MOBIPICK)) {

            final LibreJsLicenseEnum license = LibreJsLicenseEnum.MIT;

            list.add(new ImmutablePair<>(this.getJsPathForRender(
                    WebApp.getJqMobiPickLocation().concat("xdate.js"), license),
                    license));

            list.add(new ImmutablePair<>(this.getJsPathForRender(
                    WebApp.getJqMobiPickLocation().concat("xdate.i18n.js"),
                    license), license));

            list.add(new ImmutablePair<>(this.getJsPathForRender(
                    WebApp.getJqMobiPickLocation().concat("mobipick.js"),
                    license), license));
        }

        return list;
    }

    /**
     * Gets JavaScript URL path for render in head.
     *
     * @param path
     *            URL path
     * @param license
     *            License.
     * @return URL path for render.
     */
    protected final String getJsPathForRender(final String path,
            final LibreJsLicenseEnum license) {

        return LibreJsLicenseServlet.wrapLibreJsPath(license, path);
    }

    /**
     * Adds LibreJS license panel.
     *
     * @param panelId
     *            Wicket id.
     */
    protected final void addLibreJsLicensePanel(final String panelId) {

        // Create insertion-ordered LinkedHashMap.
        final Map<String, LibreJsLicenseEnum> jsRendered =
                new LinkedHashMap<>();

        // Main.
        jsRendered.put(
                WebApp.getLibreJsResourceRef(true, LibreJsLicenseEnum.MIT,
                        WebApp.getWebjarsJsRef(
                                WebApp.WEBJARS_PATH_JQUERY_CORE_JS)),
                LibreJsLicenseEnum.MIT);

        // Wicket JavaScript resources.
        jsRendered.put(WebApp.get().getWicketJavaScriptAjaxReferenceUrl(),
                LibreJsLicenseEnum.APACHE_2_0);

        if (WebServer.isDeveloperEnv()) {
            jsRendered.put(
                    WebApp.get().getWicketJavaScriptAjaxDebugReferenceUrl(),
                    LibreJsLicenseEnum.APACHE_2_0);
        }

        // Other files.
        for (final Pair<String, LibreJsLicenseEnum> pair : this
                .getJavaScriptFiles()) {
            jsRendered.put(pair.getKey(), pair.getValue());
        }

        //
        this.add(new LibreJsLicensePanel(panelId, jsRendered));
    }

    /**
     *
     */
    protected final void addFileDownloadApiPanel() {
        FileDownloadApiPanel apiPanel =
                new FileDownloadApiPanel("file-download-api-panel");
        add(apiPanel);
        apiPanel.populate(this.getWebAppType());
    }

    /**
     * @param webAppType
     *            The type to Web App.
     */
    protected final void addZeroPagePanel(final WebAppTypeEnum webAppType) {
        final ZeroPagePanel zeroPanel = new ZeroPagePanel("zero-page-panel");
        zeroPanel.populate(webAppType, this.getPageParameters());
        add(zeroPanel);
    }

}
