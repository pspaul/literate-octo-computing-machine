/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
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

import java.text.MessageFormat;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.savapage.core.SpException;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.community.MemberCard;
import org.savapage.server.pages.MarkupHelper;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class MembershipMsg extends WebPage {

    private static final long serialVersionUID = 1L;

    /**
     *
     */
    public MembershipMsg() {

        add(new Label("title", localized("title",
                CommunityDictEnum.MEMBERSHIP.getWord())));

        final MemberCard lic = MemberCard.instance();

        try {

            final String txtStatus;

            switch (lic.getStatus()) {

            case WRONG_MODULE:
                txtStatus =
                        localized("membership-status-wrong-module",
                                CommunityDictEnum.MEMBERSHIP.getWord(),
                                CommunityDictEnum.SAVAPAGE_SUPPORT.getWord(),
                                CommunityDictEnum.MEMBERSHIP.getWord());
                break;

            case WRONG_COMMUNITY:
                txtStatus =
                        localized("membership-status-wrong-product",
                                CommunityDictEnum.MEMBERSHIP.getWord(),
                                lic.getProduct(),
                                CommunityDictEnum.SAVAPAGE_SUPPORT.getWord(),
                                CommunityDictEnum.MEMBERSHIP.getWord());
                break;

            case WRONG_VERSION:
                txtStatus =
                        localized("membership-status-wrong-version",
                                CommunityDictEnum.MEMBERSHIP.getWord());
                break;

            case EXCEEDED:
                txtStatus =
                        localized("membership-status-users-exceeded",
                                CommunityDictEnum.MEMBERSHIP.getWord());
                break;

            case EXPIRED:
                txtStatus =
                        localized("membership-status-expired",
                                CommunityDictEnum.MEMBERSHIP.getWord());
                break;

            case VISITOR_EXPIRED:
                txtStatus = localized("membership-status-visit-expired");
                break;

            default:
                txtStatus = "???";
                break;
            }

            /*
             *
             */
            Label labelWrk = new Label("membership-status", txtStatus);
            labelWrk.add(new AttributeAppender("class", String.format(" %s",
                    MarkupHelper.CSS_TXT_ERROR)));
            add(labelWrk);

            //
            add(new Label("membership-msg", localized("membership-msg",
                    CommunityDictEnum.MEMBERSHIP.getWord(),
                    CommunityDictEnum.MEMBERSHIP.getWord(),
                    CommunityDictEnum.MEMBER_CARD.getWord())));

        } catch (Exception e) {
            throw new SpException(e);
        }
    }

    /**
     * Localizes and format a string with placeholder arguments.
     *
     * @param key
     *            The key from the XML resource file
     * @param objects
     *            The values to fill the placeholders
     * @return The localized string.
     */
    protected final String localized(final String key, final Object... objects) {
        return MessageFormat.format(getLocalizer().getString(key, this),
                objects);
    }

}
