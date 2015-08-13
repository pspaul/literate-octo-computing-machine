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
import org.savapage.core.SpException;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.community.MemberCard;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.UserDao;
import org.savapage.core.doc.OfficeToPdf;
import org.savapage.core.doc.XpsToPdf;
import org.savapage.core.jpa.tools.DbVersionInfo;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.system.SystemInfo;
import org.savapage.core.system.SystemInfo.SysctlEnum;
import org.savapage.core.util.NumberUtil;
import org.savapage.server.pages.MarkupHelper;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class About extends AbstractAdminPage {

    /**
     * .
     */
    private static final ProxyPrintService PROXY_PRINT_SERVICE = ServiceContext
            .getServiceFactory().getProxyPrintService();

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Override
    protected final boolean needMembership() {
        return false;
    }

    /**
     *
     */
    public About() {

        try {
            handlePage();
        } catch (Exception e) {
            throw new SpException(e);
        }
    }

    /**
     * @throws Exception
     *
     */
    @SuppressWarnings("serial")
    private void handlePage() throws Exception {

        final MemberCard memberCard = MemberCard.instance();

        Label labelWrk;

        //
        add(new Label("version-build", ConfigManager.getAppVersionBuild()));

        //
        add(new Label("version-date",
                localizedDateTime(org.savapage.core.VersionInfo.getBuildDate())));

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
        add(new Label("app-license-name", CommunityDictEnum.SAVAPAGE.getWord()));
        add(new Label("current-year", String.valueOf(Calendar.getInstance()
                .get(Calendar.YEAR))));

        //
        //
        labelWrk =
                new Label("app-copyright-owner-url",
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

        add(new Label("jre-available-processors", Integer.valueOf(Runtime
                .getRuntime().availableProcessors())));

        add(new Label("jre-max-memory", NumberUtil.humanReadableByteCount(
                Runtime.getRuntime().maxMemory(), true)));

        add(new Label("jre-os-name", System.getProperty("os.name")));
        add(new Label("jre-os-version", System.getProperty("os.version")));
        add(new Label("jre-os-arch", System.getProperty("os.arch")));

        // ---------- CUPS
        colorInstalled = null;
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
        if (colorInstalled != null) {
            labelWrk.add(new AttributeModifier("class", colorInstalled));
        }
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
        version = OfficeToPdf.getLibreOfficeVersion();
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
                item.add(new Label("sysctl-value", SystemInfo.getSysctl(sysctl)));
            }
        });

        //
        add(new Label("cat-membership", CommunityDictEnum.COMMUNITY.getWord()));

        // ---------
        String validDays = null;
        String txtStatus = null;
        String signalColor = null;

        final Date refDate = ServiceContext.getTransactionDate();

        switch (memberCard.getStatus()) {

        case WRONG_MODULE:
            signalColor = MarkupHelper.CSS_TXT_ERROR;
            txtStatus =
                    localized("membership-status-wrong-module",
                            CommunityDictEnum.MEMBERSHIP.getWord(),
                            CommunityDictEnum.SAVAPAGE_SUPPORT.getWord(),
                            CommunityDictEnum.MEMBERSHIP.getWord());
            break;

        case WRONG_COMMUNITY:
            signalColor = MarkupHelper.CSS_TXT_ERROR;
            txtStatus =
                    localized("membership-status-wrong-product",
                            CommunityDictEnum.MEMBERSHIP.getWord(),
                            memberCard.getProduct(),
                            CommunityDictEnum.SAVAPAGE_SUPPORT.getWord(),
                            CommunityDictEnum.MEMBERSHIP.getWord());
            break;

        case WRONG_VERSION:
            signalColor = MarkupHelper.CSS_TXT_WARN;
            txtStatus =
                    localized("membership-status-wrong-version",
                            CommunityDictEnum.MEMBERSHIP.getWord());
            break;

        case WRONG_VERSION_WITH_GRACE:
            signalColor = MarkupHelper.CSS_TXT_WARN;
            txtStatus =
                    localized("membership-status-wrong-version-grace-period",
                            CommunityDictEnum.MEMBERSHIP.getWord(),
                            memberCard.getDaysLeftInVisitorPeriod(refDate),
                            CommunityDictEnum.MEMBERSHIP.getWord());
            break;

        case VALID:

            signalColor = MarkupHelper.CSS_TXT_COMMUNITY;

            if (memberCard.isVisitorCard()) {
                txtStatus = CommunityDictEnum.VISITOR.getWord();
            } else {
                txtStatus = CommunityDictEnum.FELLOW.getWord();
            }

            if (memberCard.getExpirationDate() != null) {
                validDays =
                        localized("membership-valid-till-msg",
                                localizedDate(memberCard.getExpirationDate()),
                                memberCard.getDaysTillExpiry());
            }
            break;

        case EXCEEDED:
            signalColor = MarkupHelper.CSS_TXT_WARN;
            txtStatus =
                    localized("membership-status-users-exceeded",
                            CommunityDictEnum.MEMBERSHIP.getWord());
            break;

        case EXPIRED:
            signalColor = MarkupHelper.CSS_TXT_WARN;
            txtStatus =
                    localized("membership-status-expired",
                            CommunityDictEnum.MEMBERSHIP.getWord());
            validDays =
                    localized("membership-expired-msg",
                            localizedDate(memberCard.getExpirationDate()),
                            memberCard.getDaysTillExpiry());
            break;

        case VISITOR_EDITION:
            signalColor = MarkupHelper.CSS_TXT_COMMUNITY;
            txtStatus = CommunityDictEnum.VISITING_GUEST.getWord();
            break;

        case VISITOR:
            signalColor = MarkupHelper.CSS_TXT_COMMUNITY;
            txtStatus =
                    localized("membership-status-visit",
                            CommunityDictEnum.VISITING_GUEST.getWord(),
                            memberCard.getDaysLeftInVisitorPeriod(refDate));
            break;

        case VISITOR_EXPIRED:
            signalColor = MarkupHelper.CSS_TXT_WARN;

            txtStatus =
                    localized("membership-status-visit-expired",
                            CommunityDictEnum.VISITING_GUEST.getWord(),
                            localizedDate(DateUtils.addDays(
                                    new Date(),
                                    memberCard.getDaysLeftInVisitorPeriod(
                                            refDate).intValue())));
            break;

        default:
            signalColor = MarkupHelper.CSS_TXT_ERROR;
            txtStatus = "???";
            break;
        }

        // -------------
        final String styleInfo =
                String.format("class=\"%s %s\"", MarkupHelper.CSS_TXT_WRAP,
                        signalColor);

        final String styleInfoValid =
                String.format("class=\"%s %s\"", MarkupHelper.CSS_TXT_WRAP,
                        MarkupHelper.CSS_TXT_COMMUNITY);

        final String liFormat = "<li><h3>%s</h3><div %s>%s</div></li>";

        String htmlMembership = null;

        if (memberCard.hasMemberCardFile()) {

            //
            htmlMembership =
                    String.format(
                            liFormat,
                            CommunityDictEnum.MEMBER.getWord(),
                            styleInfo,
                            StringUtils.defaultString(
                                    memberCard.getMemberOrganisation(), "-"));

            //
            htmlMembership +=
                    String.format(liFormat, localized("membership-status"),
                            styleInfo, txtStatus);

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
                htmlMembership +=
                        String.format(liFormat,
                                localized("membership-valid-till"), styleInfo,
                                validDays);
            }
            //
            htmlMembership +=
                    String.format(liFormat, localized("membership-version"),
                            styleInfo, memberCard.getMembershipVersion());

        } else {
            //
            htmlMembership =
                    String.format(liFormat, localized("membership-status"),
                            styleInfo, txtStatus);

        }

        add(new Label("membership-details", htmlMembership)
                .setEscapeModelStrings(false));

        //
        labelWrk =
                new Label("membership-participants",
                        localizedNumber(memberCard.getMemberParticipants()));
        labelWrk.add(new AttributeModifier("class", signalColor));
        add(labelWrk);

        //
        final UserDao userDAO = ServiceContext.getDaoContext().getUserDao();

        if (signalColor.equals(MarkupHelper.CSS_TXT_COMMUNITY)) {
            signalColor = MarkupHelper.CSS_TXT_VALID;
        }

        labelWrk =
                new Label("membership-users",
                        localizedNumber(userDAO.countActiveUsers()));
        labelWrk.add(new AttributeModifier("class", signalColor));
        add(labelWrk);

        add(new Label("button-import-membercard", localized(
                "button-import-membercard",
                CommunityDictEnum.MEMBER_CARD.getWord())));

        //
        final String urlHelpDesk =
                ConfigManager.instance().getConfigValue(
                        Key.COMMUNITY_HELPDESK_URL);

        labelWrk = new Label("savapage-helpdesk-url", urlHelpDesk);
        labelWrk.add(new AttributeModifier("href", urlHelpDesk));
        add(labelWrk);

        //
        labelWrk =
                new Label("savapage-source-code-url",
                        localized("source-code-link"));
        labelWrk.add(new AttributeModifier("href",
                CommunityDictEnum.COMMUNITY_SOURCE_CODE_URL.getWord()));
        add(labelWrk);

    }
}
