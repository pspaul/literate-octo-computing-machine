/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.webapp;

import java.io.File;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.PriorityHeaderItem;
import org.apache.wicket.markup.head.StringHeaderItem;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.VersionInfo;
import org.savapage.core.config.ConfigManager;
import org.savapage.server.SpSession;
import org.savapage.server.WebApp;
import org.savapage.server.pages.AbstractPage;

/**
 *
 * @author Datraverse B.V.
 *
 */
public abstract class AbstractWebAppPage extends AbstractPage implements
        IHeaderContributor {

    /**
     * .
     */
    private static final long serialVersionUID = 1L;

    /**
     * The URL path for custom web files. See {@code web.xml}.
     */
    private static final String URL_PATH_CUSTOM_WEB = "/custom/web";

    /**
     * The URL path for custom web themes.
     */
    private static final String URL_PATH_CUSTOM_THEME = URL_PATH_CUSTOM_WEB
            + "/themes";

    /**
     * The custom Web App theme file name.
     */
    private static final String CUSTOM_THEME_FILE = "custom.min.css";

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
     * Stylesheet to be used with custom jQuery Mobile theme as produced with <a
     * href="http://themeroller.jquerymobile.com/">themeroller</a>.
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

    /**
     *
     */
    protected AbstractWebAppPage() {
    }

    /**
     *
     * @param parameters
     *            The {@link PageParameters}.
     */
    protected AbstractWebAppPage(final PageParameters parameters) {

        SpSession.get().setWebAppType(this.getWebAppType());

        final String language = parameters.get("language").toString();

        if (language != null) {
            getSession().setLocale(new Locale(language));
        }

    }

    /**
     * Checks if the WebApp count is exceeded.
     * <p>
     * Note: we do NOT check on mobile browsers, i.e. we always return
     * {@code false} in this case.
     * </p>
     *
     * @param parameters
     *            The {@link PageParameters}.
     * @return {@code true} when WebApp count is exceeded.
     */
    protected final boolean isWebAppCountExceeded(
            final PageParameters parameters) {

        final boolean isMobile = isMobileBrowser();

        if (isMobile) {
            return false;
        }

        final boolean isZeroPanel =
                !parameters.get(ZeroPagePanel.PARM_SUBMIT_INIDICATOR).isEmpty();

        return !isZeroPanel && (SpSession.get().getAuthWebAppCount() > 0);
    }

    /**
     *
     * @return
     */
    @Override
    protected abstract WebAppTypeEnum getWebAppType();

    /**
     * Gets the specialized CSS filename.
     *
     * @return {@code null} when no specialized CSS files available (needed).
     */
    protected abstract String getSpecializedCssFile();

    /**
     * Gets the specialized JS filename.
     *
     * @return The specialized JS filename.
     */
    protected abstract String getSpecializedJsFile();

    /**
     * Gets the JavaScript libraries to render.
     *
     * @return The JavaScript libraries to render.
     */
    protected abstract Set<JavaScriptLibrary> getJavaScriptToRender();

    /**
     * JavaScript snippet format to render initially. The %s is the mountPath of
     * this page.
     */
    private static final String INITIAL_JAVASCRIPT_FORMAT = "(function(){"
            + "if(window.location.href.search('#')>0){"
            + "window.location.replace("
            + "window.location.protocol+\"//\"+window.location.host+\"" + "/"
            + "%s" + "\");}})();";

    /**
     * {@link StringHeaderItem} pattern to render initially. The %s is the
     * initial JavaScript as defined in {@link #INITIAL_JAVASCRIPT_PATTERN}.
     */
    private static final String INITIAL_HEADERITEM_FORMAT =
            "\n<script type=\"text/javascript\">\n" + "/*<![CDATA[*/" + "\n"
                    + "%s" + "\n" + "/*]]>*/" + "\n</script>\n";

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
         * Use the current URL path as mount path.
         */
        final String mountPath = this.getRequest().getUrl().getPath();

        final String javascript =
                String.format(INITIAL_JAVASCRIPT_FORMAT, mountPath);

        final HeaderItem firstItem =
                new PriorityHeaderItem(new StringHeaderItem(String.format(
                        INITIAL_HEADERITEM_FORMAT, javascript)));

        response.render(firstItem);
    }

    /**
     * Renders the {@link WebAppTypeEnum} specific JS files.
     *
     * @param response
     *            The {@link IHeaderResponser}.
     * @param nocache
     *            The "nocache" string.
     *
     */
    protected abstract void renderWebAppTypeJsFiles(
            final IHeaderResponse response, final String nocache);

    /**
     *
     * @param response
     *            The {@link IHeaderResponser}.
     * @param url
     */
    protected void renderJs(final IHeaderResponse response, final String url) {
        response.render(JavaScriptHeaderItem.forUrl(url));
    }

    /**
     * Returns the 'nocache' URL parameter to be appended to rendered SavaPage
     * files.
     * <p>
     * This is needed so the web browser is triggered to reload the JS and CSS
     * files when the {@link VersionInfo#VERSION_D_BUILD} value changes.
     * </p>
     *
     * @return the URL parameter.
     */
    protected final String getNoCacheUrlParm() {
        return "?" + new Date().getTime();
    }

    /**
     * Checks of JQuery Core is "automatically" rendered by Wicket, using the
     * replacement we defined in {@link WebApp#replaceJQueryCore()}.
     *
     * @return {@code true} if JQuery Core is already rendered.
     */
    abstract boolean isJqueryCoreRenderedByWicket();

    /**
     * @return The custom Web App theme @{link File}.
     */
    private static File getCustomThemeFile() {
        return new File(String.format("%s/custom/web/themes/%s",
                ConfigManager.getServerHome(), CUSTOM_THEME_FILE));
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

        renderInitialJavaScript(response);

        final String nocache = getNoCacheUrlParm();

        final Set<JavaScriptLibrary> jsToRender = getJavaScriptToRender();

        /*
         * jQuery Mobile CSS files.
         */
        final File customThemeCss = getCustomThemeFile();

        if (customThemeCss.isFile()) {

            response.render(CssHeaderItem.forUrl(String.format("%s/%s%s",
                    URL_PATH_CUSTOM_THEME, customThemeCss.getName(), nocache)));

            response.render(CssHeaderItem.forUrl(String.format("%s/%s%s",
                    URL_PATH_CUSTOM_THEME, "jquery.mobile.icons.min.css",
                    nocache)));

            response.render(WebApp
                    .getWebjarsCssRef(WEBJARS_PATH_JQUERY_MOBILE_STRUCTURE_CSS));

        } else {
            response.render(WebApp
                    .getWebjarsCssRef(WEBJARS_PATH_JQUERY_MOBILE_CSS));
        }

        /*
         * Other CSS files.
         */
        if (jsToRender.contains(JavaScriptLibrary.JQPLOT)) {
            response.render(WebApp
                    .getWebjarsCssRef(WEBJARS_PATH_JQUERY_JQPLOT_CSS));
        }

        response.render(CssHeaderItem.forUrl("jquery.savapage.css" + nocache));

        final String specializedCssFile = getSpecializedCssFile();

        if (specializedCssFile != null) {
            response.render(CssHeaderItem.forUrl(specializedCssFile + nocache));
        }

        if (jsToRender.contains(JavaScriptLibrary.MOBIPICK)) {
            response.render(CssHeaderItem.forUrl(WebApp.getJqMobiPickLocation()
                    + "mobipick.css"));
        }

        /*
         * JS files
         */
        if (!isJqueryCoreRenderedByWicket()) {
            response.render(WebApp
                    .getWebjarsJsRef(WebApp.WEBJARS_PATH_JQUERY_CORE_JS));
        }

        if (jsToRender.contains(JavaScriptLibrary.SPARKLINE)) {
            response.render(WebApp
                    .getWebjarsJsRef(WEBJARS_PATH_JQUERY_SPARKLINE));
        }

        if (jsToRender.contains(JavaScriptLibrary.JQPLOT)) {
            response.render(WebApp
                    .getWebjarsJsRef(WEBJARS_PATH_JQUERY_JQPLOT_JS));

            for (String plugin : new String[] { "jqplot.pieRenderer.js",
                    "jqplot.json2.js", "jqplot.logAxisRenderer.js",
                    "jqplot.dateAxisRenderer.js" }) {

                response.render(WebApp.getWebjarsJsRef(String.format(
                        "jqplot/current/plugins/%s", plugin)));

            }
        }

        renderJs(response, "jquery/json2.js");

        if (jsToRender.contains(JavaScriptLibrary.COMETD)) {
            renderJs(response, "org/cometd.js");
            renderJs(response, "jquery/jquery.cometd.js");
        }

        renderJs(response, "savapage.js" + nocache);
        renderJs(response, "jquery.savapage.js" + nocache);

        renderWebAppTypeJsFiles(response, nocache);

        /*
         * Note: render jQuery Mobile AFTER jquery.savapage.js, because the
         * $(document).bind("mobileinit") is implemented in jquery.savapage.js
         */
        response.render(WebApp.getWebjarsJsRef(WEBJARS_PATH_JQUERY_MOBILE_JS));

        /*
         * Render after JQM.
         */
        if (jsToRender.contains(JavaScriptLibrary.MOBIPICK)) {
            renderJs(response, WebApp.getJqMobiPickLocation() + "xdate.js");
            renderJs(response, WebApp.getJqMobiPickLocation() + "xdate.i18n.js");
            renderJs(response, WebApp.getJqMobiPickLocation() + "mobipick.js");
        }
    }

    /**
     *
     */
    protected final void addFileDownloadApiPanel() {
        FileDownloadApiPanel apiPanel =
                new FileDownloadApiPanel("file-download-api-panel");
        add(apiPanel);
    }

    /**
     *
     */
    protected final void addZeroPagePanel(final WebAppTypeEnum webAppType) {
        final ZeroPagePanel zeroPanel = new ZeroPagePanel("zero-page-panel");
        add(zeroPanel);
        zeroPanel.populate(webAppType);
    }

}
