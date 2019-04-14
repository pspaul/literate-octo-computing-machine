/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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
package org.savapage.server.pages.admin;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.community.MemberCard;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.doc.XpsToPdf;
import org.savapage.core.doc.soffice.SOfficeHelper;
import org.savapage.core.jpa.tools.DbVersionInfo;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.system.SystemInfo;
import org.savapage.core.system.SystemInfo.SysctlEnum;
import org.savapage.core.util.NumberUtil;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.pages.PrinterDriverDownloadPanel;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class About extends AbstractAdminPage {

    /** */
    private static final ProxyPrintService PROXY_PRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    @Override
    protected boolean needMembership() {
        return false;
    }

    /**
     *
     */
    public About(final PageParameters parameters) {

        super(parameters);

        final boolean hasEditorAccess =
                this.probePermissionToEdit(ACLOidEnum.A_ABOUT);

        final MemberCard memberCard = MemberCard.instance();

        final MarkupHelper helper = new MarkupHelper(this);

        Label labelWrk;

        //
        add(new Label("version-build", ConfigManager.getAppVersionBuild()));

        //
        add(new Label("version-date", localizedDateTime(
                org.savapage.core.VersionInfo.getBuildDate())));

        //
        final DbVersionInfo dbVersionInfo =
                ConfigManager.instance().getDbVersionInfo();

        StringBuilder builder = new StringBuilder();

        builder.append(dbVersionInfo.getProdName()).append(" ")
                .append(dbVersionInfo.getProdVersion());

        add(new Label("version-db", builder.toString()));

        builder = new StringBuilder();
        builder.append(org.savapage.core.VersionInfo.DB_SCHEMA_VERSION_MAJOR)
                .append(".")
                .append(org.savapage.core.VersionInfo.DB_SCHEMA_VERSION_MINOR);

        add(new Label("version-db-schema", builder.toString()));

        //
        add(new Label("app-license-name",
                CommunityDictEnum.SAVAPAGE.getWord()));
        add(new Label("current-year",
                String.valueOf(Calendar.getInstance().get(Calendar.YEAR))));

        //
        //
        labelWrk = new Label("app-copyright-owner-url",
                CommunityDictEnum.DATRAVERSE_BV.getWord());
        labelWrk.add(new AttributeModifier("href",
                CommunityDictEnum.DATRAVERSE_BV_URL.getWord()));
        add(labelWrk);

        //
        String version;
        String colorInstalled;
        String keyInstalled;
        boolean installed;

        // Java

        //
        add(new Label("java.version", System.getProperty("java.version")));
        add(new Label("java.vm.name", System.getProperty("java.vm.name")));

        add(new Label("jre-available-processors",
                Integer.valueOf(Runtime.getRuntime().availableProcessors())));

        add(new Label("jre-max-memory", NumberUtil.humanReadableByteCount(
                Runtime.getRuntime().maxMemory(), true)));

        add(new Label("jre-os-name", System.getProperty("os.name")));
        add(new Label("jre-os-version", System.getProperty("os.version")));
        add(new Label("jre-os-arch", System.getProperty("os.arch")));

        add(new Label("jre-java.io.tmpdir",
                System.getProperty(ConfigManager.SYS_PROP_JAVA_IO_TMPDIR)));

        add(new Label("app.dir.tmp-key",
                ConfigManager.SERVER_PROP_APP_DIR_TMP));
        add(new Label("app.dir.tmp", ConfigManager.getAppTmpDir()));

        add(new Label("app.dir.safepages-key",
                ConfigManager.SERVER_PROP_APP_DIR_SAFEPAGES));
        add(new Label("app.dir.safepages",
                ConfigManager.getSafePagesHomeDir()));

        add(new Label("app.dir.letterheads-key",
                ConfigManager.SERVER_PROP_APP_DIR_LETTERHEADS));
        add(new Label("app.dir.letterheads", ConfigManager.getLetterheadDir()));

        helper.encloseLabel("btn-i18n-cache-clear",
                HtmlButtonEnum.CLEAR.uiText(getLocale()), hasEditorAccess);

        // ---------- CUPS
        version = PROXY_PRINT_SERVICE.getCupsVersion();
        installed = StringUtils.isNotBlank(version);

        if (!installed) {
            version = localized("version-unknown");
        }

        final String cupsApiVersion = PROXY_PRINT_SERVICE.getCupsApiVersion();

        if (StringUtils.isNotBlank(cupsApiVersion)) {
            version += " (API " + cupsApiVersion + ")";
        }

        labelWrk = new Label("version-cups", version);
        add(labelWrk);

        // ---------- Imagemagick
        colorInstalled = null;
        version = SystemInfo.getImageMagickVersion();
        installed = StringUtils.isNotBlank(version);
        if (!installed) {
            version = localized("not-installed");
            colorInstalled = MarkupHelper.CSS_TXT_ERROR;
        }

        labelWrk = new Label("version-imagemagick", version);
        if (colorInstalled != null) {
            labelWrk.add(new AttributeModifier("class", colorInstalled));
        }
        add(labelWrk);

        // ---------- pdftoppm
        colorInstalled = null;
        version = SystemInfo.getPdfToPpmVersion();
        installed = StringUtils.isNotBlank(version);
        if (!installed) {
            version = localized("not-installed");
            colorInstalled = MarkupHelper.CSS_TXT_ERROR;
        }

        labelWrk = new Label("version-pdftoppm", version);
        if (colorInstalled != null) {
            labelWrk.add(new AttributeModifier("class", colorInstalled));
        }
        add(labelWrk);

        // ---------- pdftocairo
        colorInstalled = null;
        version = SystemInfo.getPdfToCairoVersion();
        installed = StringUtils.isNotBlank(version);
        if (!installed) {
            version = localized("not-installed");
            colorInstalled = MarkupHelper.CSS_TXT_ERROR;
        }

        labelWrk = new Label("version-pdftocairo", version);
        if (colorInstalled != null) {
            labelWrk.add(new AttributeModifier("class", colorInstalled));
        }
        add(labelWrk);

        // ---------- Ghostscript
        colorInstalled = null;
        version = SystemInfo.getGhostscriptVersion();
        installed = StringUtils.isNotBlank(version);
        if (!installed) {
            version = localized("not-installed");
            colorInstalled = MarkupHelper.CSS_TXT_ERROR;
        }

        labelWrk = new Label("version-ghostscript", version);
        if (colorInstalled != null) {
            labelWrk.add(new AttributeModifier("class", colorInstalled));
        }
        add(labelWrk);

        // ---------- qpdf
        colorInstalled = null;
        version = SystemInfo.getQPdfVersion();
        installed = StringUtils.isNotBlank(version);
        if (!installed) {
            version = localized("not-installed");
            colorInstalled = MarkupHelper.CSS_TXT_ERROR;
        }

        labelWrk = new Label("version-qpdf", version);
        if (colorInstalled != null) {
            labelWrk.add(new AttributeModifier("class", colorInstalled));
        }
        add(labelWrk);

        // ----------xpstopdf
        colorInstalled = null;
        installed = XpsToPdf.isInstalled();
        keyInstalled = installed ? "installed" : "not-installed";
        colorInstalled = installed ? null : MarkupHelper.CSS_TXT_WARN;

        labelWrk = new Label("version-xpstopdf", localized(keyInstalled));
        if (colorInstalled != null) {
            labelWrk.add(new AttributeModifier("class", colorInstalled));
        }
        add(labelWrk);

        // ---------- LibreOffice
        colorInstalled = null;
        version = SOfficeHelper.getLibreOfficeVersion();
        installed = StringUtils.isNotBlank(version);
        if (!installed) {
            version = localized("not-installed");
            colorInstalled = MarkupHelper.CSS_TXT_WARN;
        }

        labelWrk = new Label("version-libreoffice", version);
        if (colorInstalled != null) {
            labelWrk.add(new AttributeModifier("class", colorInstalled));
        }
        add(labelWrk);

        //
        add(new Label("system-ulimits-nofile", SystemInfo.getUlimitsNofile()));

        final ArrayList<SysctlEnum> sysctlList = new ArrayList<>();

        for (final SysctlEnum sysctl : SysctlEnum.values()) {
            sysctlList.add(sysctl);
        }

        //
        add(new PropertyListView<SysctlEnum>("sysctl-entry", sysctlList) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<SysctlEnum> item) {

                final SysctlEnum sysctl = item.getModelObject();
                item.add(new Label("sysctl-key", sysctl.getKey()));
                item.add(new Label("sysctl-value",
                        SystemInfo.getSysctl(sysctl)));
            }
        });

        //
        add(new Label("cat-membership",
                CommunityDictEnum.COMMUNITY.getWord(getLocale())));

        // ---------
        String validDays = null;
        Long validDaysLeft = 0L;
        String txtStatus = null;
        String signalColor = null;

        final Date refDate = ServiceContext.getTransactionDate();

        switch (memberCard.getStatus()) {

        case WRONG_MODULE:
            signalColor = MarkupHelper.CSS_TXT_ERROR;
            txtStatus = localized("membership-status-wrong-module",
                    CommunityDictEnum.MEMBERSHIP.getWord(getLocale()),
                    CommunityDictEnum.SAVAPAGE_SUPPORT.getWord(getLocale()),
                    CommunityDictEnum.MEMBERSHIP.getWord(getLocale()));
            break;

        case WRONG_COMMUNITY:
            signalColor = MarkupHelper.CSS_TXT_ERROR;
            txtStatus = localized("membership-status-wrong-product",
                    CommunityDictEnum.MEMBERSHIP.getWord(getLocale()),
                    memberCard.getProduct(),
                    CommunityDictEnum.SAVAPAGE_SUPPORT.getWord(getLocale()),
                    CommunityDictEnum.MEMBERSHIP.getWord(getLocale()));
            break;

        case WRONG_VERSION:
            signalColor = MarkupHelper.CSS_TXT_WARN;
            txtStatus = localized("membership-status-wrong-version",
                    CommunityDictEnum.MEMBERSHIP.getWord(getLocale()));
            break;

        case VALID:

            signalColor = MarkupHelper.CSS_TXT_COMMUNITY;

            if (memberCard.isVisitorCard()) {
                txtStatus = CommunityDictEnum.VISITOR.getWord(getLocale());
            } else {
                txtStatus = CommunityDictEnum.CARD_HOLDER.getWord(getLocale());
            }

            if (memberCard.getExpirationDate() != null) {
                validDaysLeft = memberCard.getDaysTillExpiry();
                validDays = localized("membership-valid-till-msg",
                        localizedDate(memberCard.getExpirationDate()),
                        validDaysLeft);
            }
            break;

        case EXCEEDED:
            signalColor = MarkupHelper.CSS_TXT_WARN;
            txtStatus = localized("membership-status-users-exceeded",
                    CommunityDictEnum.MEMBERSHIP.getWord(getLocale()));
            break;

        case EXPIRED:
            signalColor = MarkupHelper.CSS_TXT_WARN;
            txtStatus = localized("membership-status-expired",
                    CommunityDictEnum.MEMBERSHIP.getWord(getLocale()));
            validDaysLeft = memberCard.getDaysTillExpiry();
            validDays = localized("membership-expired-msg",
                    localizedDate(memberCard.getExpirationDate()),
                    validDaysLeft);
            break;

        case VISITOR_EDITION:
            signalColor = MarkupHelper.CSS_TXT_COMMUNITY;
            txtStatus = CommunityDictEnum.VISITING_GUEST.getWord(getLocale());
            break;

        case VISITOR:
            signalColor = MarkupHelper.CSS_TXT_COMMUNITY;
            txtStatus = localized("membership-status-visit",
                    CommunityDictEnum.VISITING_GUEST.getWord(getLocale()),
                    memberCard.getDaysLeftInVisitorPeriod(refDate));
            break;

        case VISITOR_EXPIRED:
            signalColor = MarkupHelper.CSS_TXT_WARN;

            txtStatus = localized("membership-status-visit-expired",
                    CommunityDictEnum.VISITING_GUEST.getWord(getLocale()),
                    localizedDate(DateUtils.addDays(new Date(), memberCard
                            .getDaysLeftInVisitorPeriod(refDate).intValue())));
            break;

        default:
            signalColor = MarkupHelper.CSS_TXT_ERROR;
            txtStatus = "???";
            break;
        }

        // -------------
        final String styleInfo = String.format("class=\"%s %s\"",
                MarkupHelper.CSS_TXT_WRAP, signalColor);

        final String styleInfoWarn = String.format("class=\"%s %s\"",
                MarkupHelper.CSS_TXT_WRAP, MarkupHelper.CSS_TXT_WARN);

        final String styleInfoValid = String.format("class=\"%s %s\"",
                MarkupHelper.CSS_TXT_WRAP, MarkupHelper.CSS_TXT_COMMUNITY);

        final String liFormat = "<li><h3>%s</h3><div %s>%s</div></li>";

        String htmlMembership = null;

        if (memberCard.hasMemberCardFile()) {

            //
            htmlMembership = String.format(liFormat,
                    CommunityDictEnum.MEMBER.getWord(getLocale()), styleInfo,
                    StringUtils.defaultString(
                            memberCard.getMemberOrganisation(), "-"));

            //
            htmlMembership += String.format(liFormat,
                    localized("membership-status"), styleInfo, txtStatus);

            //
            htmlMembership +=
                    String.format(liFormat, localized("membership-issuer"),
                            styleInfoValid, memberCard.getMembershipIssuer());

            //
            final String issueDate;

            if (memberCard.getMembershipIssueDate() != null) {
                issueDate = localizedDate(memberCard.getMembershipIssueDate());
            } else {
                issueDate = "";
            }

            htmlMembership +=
                    String.format(liFormat, localized("membership-issue-date"),
                            styleInfoValid, issueDate);

            //
            if (validDays != null) {
                final String styleWlk;
                if (validDaysLeft
                        .longValue() > MemberCard.DAYS_WARN_BEFORE_EXPIRE) {
                    styleWlk = styleInfo;
                } else {
                    styleWlk = styleInfoWarn;
                }
                htmlMembership += String.format(liFormat,
                        localized("membership-valid-till"), styleWlk,
                        validDays);
            }
            //
            htmlMembership +=
                    String.format(liFormat, localized("membership-version"),
                            styleInfo, memberCard.getMembershipVersion());

        } else {
            //
            htmlMembership = String.format(liFormat,
                    localized("membership-status"), styleInfo, txtStatus);

        }

        add(new Label("membership-details", htmlMembership)
                .setEscapeModelStrings(false));

        //
        labelWrk = new Label("membership-participants",
                helper.localizedNumber(memberCard.getMemberParticipants()));
        labelWrk.add(new AttributeModifier("class", signalColor));
        add(labelWrk);

        //
        final UserDao userDAO = ServiceContext.getDaoContext().getUserDao();

        if (signalColor.equals(MarkupHelper.CSS_TXT_COMMUNITY)) {
            signalColor = MarkupHelper.CSS_TXT_VALID;
        }

        labelWrk = new Label("membership-users",
                helper.localizedNumber(userDAO.countActiveUsers()));
        labelWrk.add(new AttributeModifier("class", signalColor));
        add(labelWrk);

        helper.encloseLabel("button-import-membercard",
                localized("button-import-membercard",
                        CommunityDictEnum.MEMBER_CARD.getWord(getLocale())),
                hasEditorAccess);
        //
        final String urlHelpDesk =
                CommunityDictEnum.SAVAPAGE_SUPPORT_URL.getWord();

        labelWrk = new Label("savapage-helpdesk-url", urlHelpDesk);
        labelWrk.add(new AttributeModifier("href", urlHelpDesk));
        add(labelWrk);

        //
        labelWrk = new Label("savapage-url",
                CommunityDictEnum.SAVAPAGE_WWW_DOT_ORG.getWord());
        labelWrk.add(new AttributeModifier("href",
                CommunityDictEnum.SAVAPAGE_WWW_DOT_ORG_URL.getWord()));
        add(labelWrk);

        //
        labelWrk = new Label("savapage-source-code-url",
                localized("source-code-link"));
        labelWrk.add(new AttributeModifier("href",
                CommunityDictEnum.COMMUNITY_SOURCE_CODE_URL
                        .getWord(getLocale())));
        add(labelWrk);

        //
        final PrinterDriverDownloadPanel downloadPanel =
                new PrinterDriverDownloadPanel("printerdriver-download-panel");
        add(downloadPanel);
        downloadPanel.populate();

    }
}
