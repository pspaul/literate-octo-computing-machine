/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Authors: Rijk Ravestein.
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

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.Localizer;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.TransparentWebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.savapage.core.dao.enums.AppLogLevelEnum;
import org.savapage.core.dao.enums.ExternalSupplierStatusEnum;
import org.savapage.core.i18n.AdjectiveEnum;
import org.savapage.core.i18n.AdverbEnum;
import org.savapage.core.i18n.LabelEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.i18n.PhraseEnum;
import org.savapage.core.i18n.PrepositionEnum;
import org.savapage.core.i18n.PrintOutNounEnum;
import org.savapage.core.i18n.PrintOutVerbEnum;
import org.savapage.core.ipp.IppJobStateEnum;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.server.WebApp;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.helpers.HtmlTooltipEnum;

/**
 * Helper methods for a {@link MarkupContainer}.
 * <p>
 * TODO: All helper methods from {@link AbstractPage} should be moved here.
 * </p>
 * <p>
 * INVARIANT: CSS_* constants MUST match CSS classes in jquery.savapage.css
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public final class MarkupHelper {

    public static final String ATTR_ACCEPT = "accept";
    public static final String ATTR_ALIGN = "align";
    public static final String ATTR_CHECKED = "checked";
    public static final String ATTR_SELECTED = "selected";
    public static final String ATTR_DISABLED = "disabled";
    public static final String ATTR_ID = "id";
    public static final String ATTR_FOR = "for";
    public static final String ATTR_TITLE = "title";
    public static final String ATTR_SRC = "src";
    public static final String ATTR_CLASS = "class";
    public static final String ATTR_STYLE = "style";
    public static final String ATTR_HREF = "href";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_VALUE = "value";
    public static final String ATTR_PLACEHOLDER = "placeholder";
    public static final String ATTR_SLIDER_MIN = "min";
    public static final String ATTR_SLIDER_MAX = "max";
    public static final String ATTR_MAXLENGTH = "maxlength";
    public static final String ATTR_TABINDEX = "tabindex";

    public static final String ATTR_DATA_JQM_DATA_ON_TEXT = "data-on-text";
    public static final String ATTR_DATA_JQM_DATA_OFF_TEXT = "data-off-text";

    public static final String ATTR_DATA_INPUT = "data-input";

    public static final String ATTR_DATA_SAVAPAGE = "data-savapage";
    public static final String ATTR_DATA_SAVAPAGE_KEY = "data-savapage-key";
    public static final String ATTR_DATA_SAVAPAGE_TYPE = "data-savapage-type";

    public static final String CSS_AMOUNT_MIN = "sp-amount-min";

    public static final String CSS_TXT_ERROR = "sp-txt-error";
    public static final String CSS_TXT_WARN = "sp-txt-warn";
    public static final String CSS_TXT_INFO = "sp-txt-info";
    public static final String CSS_TXT_VALID = "sp-txt-valid";

    public static final String CSS_TXT_COMMUNITY = "sp-txt-community";

    public static final String CSS_TXT_WRAP = "sp-txt-wrap";

    public static final String CSS_TXT_INTERNAL_USER = "sp-txt-internal-user";

    public static final String CSS_PRINT_IN_QUEUE = "sp-print-in-queue";
    public static final String CSS_PRINT_OUT_PRINTER = "sp-print-out-printer";
    public static final String CSS_PRINT_OUT_PDF = "sp-print-out-pdf";

    public static final String CSS_DISABLED = "sp-disabled";
    public static final String CSS_INVISIBLE = "sp-invisible";

    public static final String CSS_PREFERRED_ICON_ON =
            "ui-icon-mini-preferred-on";
    public static final String CSS_PREFERRED_ICON_OFF =
            "ui-icon-mini-preferred-off";

    /**
     * HTML entity for ⦦ : OBLIQUE ANGLE OPENING UP).
     */
    public static final String HTML_ENT_OBL_ANGLE_OPENING_UP = "&#x29A6;";

    /**
     * Image path for unkown Account.
     */
    private static final String IMG_PATH_ACCOUNT_UNKNOWN =
            WebApp.PATH_IMAGES_FAMFAM + "/cross.png";
    /**
     * Image path for Personal Account.
     */
    private static final String IMG_PATH_ACCOUNT_PERSONAL =
            WebApp.PATH_IMAGES_FAMFAM + "/user_gray.png";
    /**
     * Image path for Group Account.
     */
    private static final String IMG_PATH_ACCOUNT_GROUP =
            WebApp.PATH_IMAGES_FAMFAM + "/group.png";
    /**
     * Image path for Shared Account.
     */
    private static final String IMG_PATH_ACCOUNT_SHARED =
            WebApp.PATH_IMAGES_FAMFAM + "/tag_green.png";

    /**
     * Image path for User Roles.
     */
    public static final String IMG_PATH_USER_ROLES =
            WebApp.PATH_IMAGES_FAMFAM + "/script.png";

    /**
     * Image path for enabled User Roles.
     */
    public static final String IMG_PATH_USER_ROLES_ENABLED =
            WebApp.PATH_IMAGES_FAMFAM + "/script_add.png";

    /**
     * Image path for disabled User Roles.
     */
    public static final String IMG_PATH_USER_ROLES_DISABLED =
            WebApp.PATH_IMAGES_FAMFAM + "/script_delete.png";

    /**
     * Image path for User Privileges.
     */
    public static final String IMG_PATH_USER_PRIVILEGES =
            WebApp.PATH_IMAGES_FAMFAM + "/shield.png";

    /**
     * Image path for enabled User Privileges.
     */
    public static final String IMG_PATH_USER_PRIVILEGES_ENABLED =
            WebApp.PATH_IMAGES_FAMFAM + "/shield_add.png";

    /**
     * Image path for disabled User Privileges.
     */
    public static final String IMG_PATH_USER_PRIVILEGES_DISABLED =
            WebApp.PATH_IMAGES_FAMFAM + "/shield_delete.png";

    /**
     * Image path for Admin Privileges.
     */
    public static final String IMG_PATH_ADMIN_PRIVILEGES =
            WebApp.PATH_IMAGES_FAMFAM + "/cog.png";

    /**
     * Image path for enabled Admin Privileges.
     */
    public static final String IMG_PATH_ADMIN_PRIVILEGES_ENABLED =
            WebApp.PATH_IMAGES_FAMFAM + "/cog_add.png";

    /**
     * Image path for disabled Admin Privileges.
     */
    public static final String IMG_PATH_ADMIN_PRIVILEGES_DISABLED =
            WebApp.PATH_IMAGES_FAMFAM + "/cog_delete.png";

    /**
     * HTML img tag for Personal Account.
     */
    public static final String HTML_IMG_ACCOUNT_PERSONAL =
            "<img src=\"" + IMG_PATH_ACCOUNT_PERSONAL + "\"/>";
    /**
     * HTML img tag for Group Account.
     */
    public static final String HTML_IMG_ACCOUNT_GROUP =
            "<img src=\"" + IMG_PATH_ACCOUNT_GROUP + "\"/>";
    /**
     * HTML img tag for Shared Account.
     */
    public static final String HTML_IMG_ACCOUNT_SHARED =
            "<img src=\"" + IMG_PATH_ACCOUNT_SHARED + "\"/>";
    /**
     *
     */
    private final MarkupContainer container;

    /**
     *
     */
    private final NumberFormat fmNumber;

    private final DateFormat dfLongDate;

    /**
     *
     * @param container
     */
    public MarkupHelper(MarkupContainer container) {
        this.container = container;
        this.fmNumber =
                NumberFormat.getInstance(container.getSession().getLocale());
        this.dfLongDate = DateFormat.getDateInstance(DateFormat.LONG,
                container.getSession().getLocale());
    }

    /**
     *
     * @param components
     */
    private void add(Component... components) {
        container.add(components);
    }

    /**
     * Convenience method to provide easy access to the localizer object within
     * any component.
     *
     * @return The localizer object
     */
    public Localizer getLocalizer() {
        return container.getApplication().getResourceSettings().getLocalizer();
    }

    /**
     * Gives the localized string for a verb.
     *
     * @param verb
     *            The verb.
     * @return The localized string.
     */
    public String localized(final PrintOutVerbEnum verb) {
        return verb.uiText(container.getLocale());
    }

    /**
     * Gives the localized string for a noun.
     *
     * @param noun
     *            The noun.
     * @return The localized string.
     */
    public String localized(final PrintOutNounEnum noun) {
        return noun.uiText(container.getLocale());
    }

    /**
     * Gives the localized string for a noun.
     *
     * @param noun
     *            The noun.
     * @param plural
     *            {@code true} when plural form.
     * @return The localized string.
     */
    public String localized(final PrintOutNounEnum noun, final boolean plural) {
        return noun.uiText(container.getLocale(), plural);
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
    public String localized(final String key, final Object... objects) {
        return MessageFormat.format(getLocalizer().getString(key, container),
                objects);
    }

    /**
     * Gets as localized string of a Number. The locale of the current session
     * is used.
     *
     * @param number
     *            Number value.
     * @return The localized string.
     */
    public String localizedNumber(final long number) {
        return fmNumber.format(number);
    }

    /**
     * Gets as localized string of a Number. The locale of the current session
     * is used.
     *
     * @param number
     *            Number value.
     * @return The localized string or "&nbsp;" for zero number. .
     */
    public String localizedNumberOrSpace(final long number) {
        if (number == 0) {
            return "&nbsp;";
        }
        return this.localizedNumber(number);
    }

    /**
     * Gets as localized (long) date string of a Date. The locale of the current
     * session is used.
     *
     * @param date
     *            The date.
     * @return The localized date string.
     */
    public String localizedDate(final Date date) {
        return dfLongDate.format(date);
    }

    /**
     * Gets a localized string of a Double. The locale of the current session is
     * used.
     *
     * @param number
     *            The double.
     * @param maxFractionDigits
     *            Number of digits for the fraction.
     * @return The localized string.
     */
    public String localizedNumber(final double number,
            final int maxFractionDigits) {
        NumberFormat fm =
                NumberFormat.getInstance(container.getSession().getLocale());
        fm.setMaximumFractionDigits(maxFractionDigits);
        return fm.format(number);
    }

    /**
     * Adds a localized label/input checkbox.
     *
     * @param wicketId
     *            The {@code wicket:id} of the {@code <input>} part. The
     *            {@code <label>} part must have the same {@code wicket:id} with
     *            the {@code-label} suffix appended.
     *
     * @param attrIdFor
     *            The value of the HTML 'id' attribute of the {@code <input>}
     *            part, and the 'for' attribute of the {@code <label>} part.
     * @param checked
     *            {@code true} if the checkbox must be checked.
     * @return The added checkbox.
     */
    public Label labelledCheckbox(final String wicketId, final String attrIdFor,
            final boolean checked) {
        tagLabel(wicketId + "-label", wicketId, attrIdFor);
        return addCheckbox(wicketId, attrIdFor, checked);
    }

    /**
     * Adds a checkbox.
     *
     * @param wicketId
     *            The {@code wicket:id} of the {@code <input>} part.
     *
     * @param htmlId
     *            The HTML 'id' of the {@code <input>} part.
     * @param checked
     *            {@code true} if the checkbox must be checked.
     * @return The added checkbox.
     */
    public Label addCheckbox(final String wicketId, final String htmlId,
            final boolean checked) {
        final Label label = createCheckbox(wicketId, htmlId, checked);
        add(label);
        return label;
    }

    /**
     * Adds a checkbox.
     *
     * @param wicketId
     *            The {@code wicket:id} of the {@code <input>} part.
     * @param checked
     *            {@code true} if the checkbox must be checked.
     * @return The added checkbox.
     */
    public Label addCheckbox(final String wicketId, final boolean checked) {

        final Label label = new Label(wicketId, "");

        if (checked) {
            label.add(new AttributeModifier(ATTR_CHECKED, "checked"));
        }

        add(label);
        return label;
    }

    /**
     * Adds a button.
     *
     * @param wicketId
     *            The {@code wicket:id} of the {@code <input>} part.
     * @param button
     *            The {@link HtmlButtonEnum}.
     * @return The added button.
     */
    public Label addButton(final String wicketId, final HtmlButtonEnum button) {
        final Label label =
                new Label(wicketId, button.uiText(this.container.getLocale()));
        add(label);
        return label;
    }

    /**
     * Creates a checkbox (without adding it to the container).
     *
     * @param wicketId
     *            The {@code wicket:id} of the {@code <input>} part.
     *
     * @param htmlId
     *            The HTML 'id' of the {@code <input>} part.
     * @param checked
     *            {@code true} if the checkbox must be checked.
     * @return The added checkbox.
     */
    public static Label createCheckbox(final String wicketId,
            final String htmlId, final boolean checked) {

        final Label labelWrk = new Label(wicketId, "");

        modifyLabelAttr(labelWrk, "id", htmlId);

        if (checked) {
            labelWrk.add(new AttributeModifier(ATTR_CHECKED, "checked"));
        }
        return labelWrk;
    }

    /**
     * Adds a radio button.
     *
     * @param wicketId
     *            The {@code wicket:id} for this radio group item.
     * @param attrValue
     *            The value of the HTML 'value' attribute of this radio button.
     * @param checked
     *            {@code true} when radio button must be checked.
     * @return The added radio button.
     */
    public Label tagRadio(final String wicketId, final String attrValue,
            final boolean checked) {
        return tagRadio(wicketId, null, null, attrValue, checked);
    }

    /**
     * Adds a radio button.
     *
     * @param wicketId
     *            The {@code wicket:id} for this radio group item.
     * @param attrName
     *            The value of the HTML 'name' attribute of this radio button.
     * @param attrId
     *            The value of the HTML 'id' attribute of this radio button.
     * @param attrValue
     *            The value of the HTML 'value' attribute of this radio button.
     * @param checked
     *            {@code true} when radio button must be checked.
     * @return The added {@link Label}.
     */
    public Label tagRadio(final String wicketId, final String attrName,
            final String attrId, final String attrValue,
            final boolean checked) {

        final Label labelWrk = new Label(wicketId);

        labelWrk.add(new AttributeModifier("value", attrValue));

        if (StringUtils.isNotBlank(attrId)) {
            labelWrk.add(new AttributeModifier("id", attrId));
        }

        if (StringUtils.isNotBlank(attrName)) {
            labelWrk.add(new AttributeModifier("name", attrName));
        }

        if (checked) {
            labelWrk.add(new AttributeModifier(ATTR_CHECKED, "checked"));
        }

        add(labelWrk);
        return labelWrk;
    }

    /**
     * Adds a localized label.
     *
     * @param wicketId
     *            The {@code wicket:id} of the HTML entity.
     * @param localizerKey
     *            The localizer key of the label text as used in
     *            {@link #getLocalizer()}.
     * @param attrFor
     *            The value of the HTML 'for' attribute.
     * @return The added {@link Label}.
     */
    public Label tagLabel(final String wicketId, final String localizerKey,
            final String attrFor) {
        final Label labelWrk = new Label(wicketId,
                getLocalizer().getString(localizerKey, container));
        labelWrk.add(new AttributeModifier("for", attrFor));
        add(labelWrk);
        return labelWrk;
    }

    /**
     * Adds a text label.
     *
     * @param wicketId
     *            The {@code wicket:id} of the HTML entity.
     * @param text
     *            The value of the HTML 'value' attribute.
     * @return The added {@link Label}.
     */
    public Label addLabel(final String wicketId, final String text) {
        final Label labelWrk = new Label(wicketId, text);
        add(labelWrk);
        return labelWrk;
    }

    /**
     * Adds a Flipswitch.
     *
     * @param wicketId
     *            The {@code wicket:id} of the HTML entity.
     * @param onText
     *            The On text.
     * @param offText
     *            The Off text.
     * @return The added {@link Label}.
     */
    public Label addFlipswitch(final String wicketId, final String onText,
            final String offText) {
        return modifyLabelAttr(addModifyLabelAttr(wicketId,
                ATTR_DATA_JQM_DATA_ON_TEXT, onText),
                ATTR_DATA_JQM_DATA_OFF_TEXT, offText);
    }

    /**
     * Adds a noun label.
     *
     * @param wicketId
     *            The {@code wicket:id} of the HTML entity.
     * @param noun
     *            The {@link NounEnum}.
     * @return The added {@link Label}.
     */
    public Label addLabel(final String wicketId, final NounEnum noun) {
        final Label label =
                new Label(wicketId, noun.uiText(this.container.getLocale()));
        add(label);
        return label;
    }

    /**
     * Adds a Adjective label.
     *
     * @param wicketId
     *            The {@code wicket:id} of the HTML entity.
     * @param adjective
     *            The {@link AdjectiveEnum}.
     * @return The added {@link Label}.
     */
    public Label addLabel(final String wicketId,
            final AdjectiveEnum adjective) {
        final Label label = new Label(wicketId,
                adjective.uiText(this.container.getLocale()));
        add(label);
        return label;
    }

    /**
     * Adds a phrase label.
     *
     * @param wicketId
     *            The {@code wicket:id} of the HTML entity.
     * @param phrase
     *            The {@link PhraseEnum}.
     * @return The added {@link Label}.
     */
    public Label addLabel(final String wicketId, final PhraseEnum phrase) {
        final Label label =
                new Label(wicketId, phrase.uiText(this.container.getLocale()));
        add(label);
        return label;
    }

    /**
     * Adds a adverb label.
     *
     * @param wicketId
     *            The {@code wicket:id} of the HTML entity.
     * @param adverb
     *            The {@link AdverbEnum}.
     * @return The added {@link Label}.
     */
    public Label addLabel(final String wicketId, final AdverbEnum adverb) {
        final Label label =
                new Label(wicketId, adverb.uiText(this.container.getLocale()));
        add(label);
        return label;
    }

    /**
     * Adds a label.
     *
     * @param wicketId
     *            The {@code wicket:id} of the HTML entity.
     * @param noun
     *            The {@link NounEnum}.
     * @return The added {@link Label}.
     */
    public Label addLabel(final String wicketId, final LabelEnum noun) {
        final Label label =
                new Label(wicketId, noun.uiText(this.container.getLocale()));
        add(label);
        return label;
    }

    /**
     * Adds a preposition label.
     *
     * @param wicketId
     *            The {@code wicket:id} of the HTML entity.
     * @param prepos
     *            The {@link PrepositionEnum}.
     * @return The added {@link Label}.
     */
    public Label addLabel(final String wicketId, final PrepositionEnum prepos) {
        final Label label =
                new Label(wicketId, prepos.uiText(this.container.getLocale()));
        add(label);
        return label;
    }

    /**
     * Adds a noun label.
     *
     * @param wicketId
     *            The {@code wicket:id} of the HTML entity.
     * @param noun
     *            The {@link PrintOutNounEnum}.
     * @return The added {@link Label}.
     */
    public Label addLabel(final String wicketId, final PrintOutNounEnum noun) {
        final Label label =
                new Label(wicketId, noun.uiText(this.container.getLocale()));
        add(label);
        return label;
    }

    /**
     * Adds input of type text.
     *
     * @param wicketId
     *            The {@code wicket:id} of the HTML entity.
     * @param value
     *            The value of the HTML 'value' attribute.
     * @return The added {@link Label}.
     */
    public Label addTextInput(final String wicketId, final String value) {
        final Label labelWrk = new Label(wicketId);
        labelWrk.add(new AttributeModifier("value", value));
        add(labelWrk);
        return labelWrk;
    }

    /**
     * Adds input of type text.
     *
     * @param wicketId
     *            The {@code wicket:id} of the HTML entity.
     * @param value
     *            The {@link HtmlButtonEnum}. Its text is used as HTML 'value'
     *            attribute.
     * @return The added {@link Label}.
     */
    public Label addTextInput(final String wicketId,
            final HtmlButtonEnum value) {
        return this.addTextInput(wicketId,
                value.uiText(this.container.getLocale()));
    }

    /**
     * Adds a label with a modified attribute value.
     *
     * @param wicketId
     *            The {@code wicket:id} of the HTML entity.
     * @param attribute
     *            The name of the attribute
     * @param value
     *            The value of the attribute.
     * @return The added {@link Label}.
     */
    public Label addModifyLabelAttr(final String wicketId,
            final String attribute, final String value) {
        final Label labelWrk = new Label(wicketId, "");
        modifyLabelAttr(labelWrk, attribute, value);
        add(labelWrk);
        return labelWrk;
    }

    /**
     * Adds an IMG label with a modified "src" attribute value containing a
     * base64 encoded PNG image.
     *
     * @param wicketId
     *            The {@code wicket:id} of the HTML entity.
     * @param base64
     *            The base64 encoded PNG image.
     * @return The added {@link Label}.
     */
    public Label addModifyImagePngBase64Attr(final String wicketId,
            final String base64) {
        final Label labelWrk = new Label(wicketId, "");
        modifyLabelAttr(labelWrk, ATTR_SRC,
                "data:image/png;base64,".concat(base64));
        add(labelWrk);
        return labelWrk;
    }

    /**
     * Modifies an attribute value of a {@link Label}.
     *
     * @param label
     *            The {@link Label}.
     * @param attribute
     *            The name of the attribute
     * @param value
     *            The value of the attribute.
     * @return The modified {@link Label}.
     */
    public static Label modifyLabelAttr(final Label label,
            final String attribute, final String value) {
        label.add(new AttributeModifier(attribute, value));
        return label;
    }

    /**
     * Modifies an attribute value of a {@link Component}.
     *
     * @param component
     *            The {@link Component}.
     * @param attribute
     *            The name of the attribute
     * @param value
     *            The value of the attribute.
     * @return The modified {@link Component}.
     */
    public static Component modifyComponentAttr(final Component component,
            final String attribute, final String value) {
        component.add(new AttributeModifier(attribute, value));
        return component;
    }

    /**
     * Sets the on/off texts for a jQuery Mobile Flipswitch.
     *
     * @param label
     *            The flipswitch {@link Label}.
     * @param locale
     *            {@link Locale}.
     * @return label.
     */
    public static Label setFlipswitchOnOffText(final Label label,
            final Locale locale) {
        modifyLabelAttr(label, "data-on-text", AdverbEnum.ON.uiText(locale));
        modifyLabelAttr(label, "data-off-text", AdverbEnum.OFF.uiText(locale));
        return label;
    }

    /**
     * Adds a label with a modified attribute value.
     *
     * @param wicketId
     *            The {@code wicket:id} of the HTML entity.
     * @param label
     *            The label value.
     * @param attribute
     *            The name of the attribute
     * @param value
     *            The value of the attribute.
     * @return The added {@link Label}.
     */
    public Label addModifyLabelAttr(final String wicketId, final String label,
            final String attribute, final String value) {
        final Label labelWrk = new Label(wicketId, label);
        modifyLabelAttr(labelWrk, attribute, value);
        add(labelWrk);
        return labelWrk;
    }

    /**
     * Adds a label with an appended attribute value.
     *
     * @param wicketId
     *            The {@code wicket:id} of the HTML entity.
     * @param attribute
     *            The name of the attribute
     * @param value
     *            The value of the attribute.
     * @return The added {@link Label}.
     */
    public Label addAppendLabelAttr(final String wicketId,
            final String attribute, final String value) {
        final Label labelWrk = new Label(wicketId, "");
        add(appendLabelAttr(labelWrk, attribute, value));
        return labelWrk;
    }

    /**
     * Appends a value to a label attribute.
     *
     * @param label
     *            The {@link Label}.
     * @param attribute
     *            The name of the attribute
     * @param value
     *            The value of the attribute.
     * @return The {@link Label}.
     */
    public static Label appendLabelAttr(final Label label,
            final String attribute, final String value) {
        label.add(new AttributeAppender(attribute,
                String.format(" %s", value.trim())));
        return label;
    }

    /**
     * Adds a {@link #ATTR_TITLE} (tooltip) to a {@ Label}.
     *
     * @param label
     *            The {@link Label}.
     * @param id
     *            Wicket ID of localized tooltip string.
     * @return The {@link Label}.
     */
    public Label addTooltip(final Label label, final String id) {
        return modifyLabelAttr(label, ATTR_TITLE, this.localized(id));
    }

    /**
     * Adds a {@link #ATTR_TITLE} (tooltip) to a {@link Component}.
     *
     * @param component
     *            The {@link Label}.
     * @param id
     *            Wicket ID of localized tooltip string.
     * @return The {@link Component}.
     */
    public Component addTooltip(final Component component, final String id) {
        return modifyComponentAttr(component, ATTR_TITLE, this.localized(id));
    }

    /**
     * Adds a {@link #ATTR_TITLE} (tooltip) to a {@ Label}.
     *
     * @param label
     *            The {@link Label}.
     * @param tooltip
     *            The {@link HtmlTooltipEnum}.
     * @return The {@link Label}.
     */
    public Label addTooltip(final Label label, final HtmlTooltipEnum tooltip) {
        return modifyLabelAttr(label, ATTR_TITLE,
                tooltip.uiText(this.container.getLocale()));
    }

    /**
     * Appends a value to a component attribute.
     *
     * @param component
     *            The {@link Component}.
     * @param attribute
     *            The name of the attribute
     * @param value
     *            The value of the attribute.
     * @return The {@link Component}.
     */
    public static Component appendComponentAttr(final Component component,
            final String attribute, final String value) {
        component.add(new AttributeAppender(attribute,
                String.format(" %s", value.trim())));
        return component;
    }

    /**
     * Adds a transparant Wicket component and appends HTML attribute value.
     *
     * @param wid
     *            Wicket ID.
     * @param attr
     *            HTML attribute.
     * @param value
     *            HTML attribute value.
     * @return {@link Component}.
     */
    public Component addTransparentAppendAttr(final String wid,
            final String attr, final String value) {
        return appendComponentAttr(this.addTransparant(wid), attr, value);
    }

    /**
     * Adds a transparant Wicket component and modifies HTML "title" attribute.
     *
     * @param wid
     *            Wicket ID.
     * @param value
     *            HTML "title" attribute value.
     * @return {@link Component}.
     */
    public Component addTransparentWithAttrTitle(final String wid,
            final String value) {
        return modifyComponentAttr(this.addTransparant(wid),
                MarkupHelper.ATTR_TITLE, value);
    }

    /**
     * Adds a transparant Wicket component and modifies HTML "title" attribute.
     *
     * @param wid
     *            Wicket ID.
     * @param value
     *            HTML "title" attribute value.
     * @return {@link Component}.
     */
    public Component addTransparentWithAttrTitle(final String wid,
            final HtmlButtonEnum value) {
        return modifyComponentAttr(this.addTransparant(wid),
                MarkupHelper.ATTR_TITLE,
                value.uiText(this.container.getLocale()));
    }

    /**
     * Adds a transparant Wicket component and modifies HTML "title" attribute.
     *
     * @param wid
     *            Wicket ID.
     * @param value
     *            HTML "title" attribute value.
     * @return {@link Component}.
     */
    public Component addTransparentWithAttrTitle(final String wid,
            final NounEnum value) {
        return modifyComponentAttr(this.addTransparant(wid),
                MarkupHelper.ATTR_TITLE,
                value.uiText(this.container.getLocale()));
    }

    /**
     * Adds a label with an appended attribute value.
     *
     * @param wicketId
     *            The {@code wicket:id} of the HTML entity.
     * @param label
     *            The label value.
     * @param attribute
     *            The name of the attribute
     * @param value
     *            The value of the attribute.
     * @return The added {@link Label}.
     */
    public Label addAppendLabelAttr(final String wicketId, final String label,
            final String attribute, final String value) {
        Label labelWrk = new Label(wicketId, label);
        labelWrk.add(new AttributeAppender(attribute, " " + value.trim()));
        add(labelWrk);
        return labelWrk;
    }

    /**
     * Discloses a label.
     *
     * @param wicketId
     *            The {@code wicket:id} of the HTML entity.
     */
    public void discloseLabel(final String wicketId) {
        encloseLabel(wicketId, null, false);
    }

    /**
     * Adds an enclosed label to the {@link MarkupContainer}.
     *
     * @param wicketId
     *            The {@code wicket:id} of the HTML entity.
     * @param value
     *            The label value;
     * @param enclose
     *            {@code true} when label should be enclosed.
     * @return The enclosed {@link Label}.
     */
    public Label encloseLabel(final String wicketId, final String value,
            final boolean enclose) {

        final Label label = createEncloseLabel(wicketId, value, enclose);
        add(label);
        return label;
    }

    /**
     * Creates an enclose label.
     *
     * @param wicketId
     *            The {@code wicket:id} of the HTML entity.
     * @param value
     *            The label value.
     * @param enclose
     *            {@code true} when label should be enclosed.
     * @return The label
     */
    public static Label createEncloseLabel(final String wicketId,
            final String value, final boolean enclose) {

        return new Label(wicketId, value) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return enclose;
            }
        };
    }

    /**
     * Creates a transparent component.
     *
     * @param wicketId
     *            The {@code wicket:id} of the HTML entity.
     * @return The component.
     */
    private static Component createTransparent(final String wicketId) {
        return new TransparentWebMarkupContainer(wicketId);
    }

    /**
     * Adds a transparent component.
     *
     * @param wicketId
     *            The {@code wicket:id} of the HTML entity.
     * @return The component.
     */
    public Component addTransparant(final String wicketId) {
        final Component component = createTransparent(wicketId);
        add(component);
        return component;
    }

    /**
     * Adds a disabled transparent component.
     *
     * @param wicketId
     *            The {@code wicket:id} of the HTML entity.
     * @param disabled
     *            {@code true} when component should be disabled.
     * @return The component.
     */
    public Component addTransparantDisabled(final String wicketId,
            final boolean disabled) {

        final Component component = createTransparent(wicketId);
        if (disabled) {
            component.add(new AttributeAppender(MarkupHelper.ATTR_CLASS,
                    CSS_DISABLED));
        }
        add(component);
        return component;
    }

    /**
     * Adds a visible transparent component.
     *
     * @param wicketId
     *            The {@code wicket:id} of the HTML entity.
     * @param invisible
     *            {@code true} when component should be invisible.
     * @return The component.
     */
    public Component addTransparantInvisible(final String wicketId,
            final boolean invisible) {
        final Component component = createTransparent(wicketId);
        if (invisible) {
            component.add(new AttributeAppender(MarkupHelper.ATTR_CLASS,
                    CSS_INVISIBLE));
        }
        add(component);
        return component;
    }

    /**
     * Gets CSS_TXT_* class of enum value.
     *
     * @param status
     *            The {@link ExternalSupplierStatusEnum}.
     * @return the CSS_TXT_* class of the enum value.
     */
    public static String
            getCssTxtClass(final ExternalSupplierStatusEnum status) {

        switch (status) {

        case COMPLETED:
            return CSS_TXT_VALID;

        case PENDING:
        case PENDING_COMPLETE:
        case PENDING_EXT:
            return CSS_TXT_WARN;

        case CANCELLED:
        case PENDING_CANCEL:
        case EXPIRED:
        case ERROR:
        default:
            return CSS_TXT_ERROR;
        }

    }

    /**
     * Gets CSS_TXT_* class of enum value.
     *
     * @param state
     *            The {@link IppJobStateEnum}.
     * @return the CSS_TXT_* class of the enum value.
     */
    public static String getCssTxtClass(final IppJobStateEnum state) {
        switch (state) {
        case IPP_JOB_ABORTED:
        case IPP_JOB_STOPPED:
        case IPP_JOB_CANCELED:
            return CSS_TXT_ERROR;

        case IPP_JOB_COMPLETED:
            return CSS_TXT_VALID;

        case IPP_JOB_HELD:
        case IPP_JOB_PENDING:
        case IPP_JOB_PROCESSING:
        case IPP_JOB_UNKNOWN:
            return CSS_TXT_WARN;

        default:
            return "";
        }
    }

    /**
     * Gets the URL image path of {@link AccountTypeEnum}.
     *
     * @param accountType
     *            The enum value.
     * @return The {@code /} prefixed URL path.
     */
    public static String getImgUrlPath(final AccountTypeEnum accountType) {

        if (accountType == null) {
            return IMG_PATH_ACCOUNT_UNKNOWN;
        } else {
            switch (accountType) {
            case GROUP:
                return IMG_PATH_ACCOUNT_GROUP;
            case SHARED:
                return IMG_PATH_ACCOUNT_SHARED;
            default:
                return IMG_PATH_ACCOUNT_PERSONAL;
            }
        }
    }

    /**
     * @param level
     *            The log level.
     * @return The CSS_TXT-* class.
     */
    public static String getCssTxtClass(final AppLogLevelEnum level) {
        switch (level) {
        case ERROR:
            return MarkupHelper.CSS_TXT_ERROR;
        case WARN:
            return MarkupHelper.CSS_TXT_WARN;
        default:
            return MarkupHelper.CSS_TXT_INFO;
        }
    }

}
