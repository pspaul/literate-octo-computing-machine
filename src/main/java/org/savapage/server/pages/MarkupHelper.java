/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
 * Authors: Rijk Ravestein.
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
package org.savapage.server.pages;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.Localizer;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;

/**
 * Helper methods for a {@link MarkupContainer}.
 * <p>
 * TODO: All helper methods from {@link AbstractPage} should be moved here.
 * </p>
 * <p>
 * INVARIANT: CSS_* constants MUST match CSS classes in jquery.savapage.css
 * </p>
 *
 * @author Datraverse B.V.
 *
 */
public class MarkupHelper {

    public static final String CSS_AMOUNT_MIN = "sp-amount-min";

    public static final String CSS_TXT_ERROR = "sp-txt-error";
    public static final String CSS_TXT_WARN = "sp-txt-warn";
    public static final String CSS_TXT_INFO = "sp-txt-info";
    public static final String CSS_TXT_VALID = "sp-txt-valid";

    public static final String CSS_TXT_COMMUNITY= "sp-txt-community";

    public static final String CSS_TXT_WRAP = "sp-txt-wrap";

    public static final String CSS_TXT_INTERNAL_USER = "sp-txt-internal-user";

    public static final String CSS_PRINT_IN_QUEUE = "sp-print-in-queue";
    public static final String CSS_PRINT_OUT_PRINTER = "sp-print-out-printer";
    public static final String CSS_PRINT_OUT_PDF = "sp-print-out-pdf";

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
        this.dfLongDate =
                DateFormat.getDateInstance(DateFormat.LONG, container
                        .getSession().getLocale());
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
    public final Localizer getLocalizer() {
        return container.getApplication().getResourceSettings().getLocalizer();
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
    public final String localized(final String key, final Object... objects) {
        return MessageFormat.format(getLocalizer().getString(key, container),
                objects);
    }

    /**
     * Gives the localized string for a key.
     *
     * @param key
     *            The key from the XML resource file
     * @return The localized string.
     */
    protected final String localized(final String key) {
        return getLocalizer().getString(key, container);
    }

    /**
     * Gets as localized string of a Number. The locale of the current session
     * is used.
     *
     * @param number
     * @return The localized string.
     */
    public final String localizedNumber(final long number) {
        return fmNumber.format(number);
    }

    /**
     * Gets as localized (long) date string of a Date. The locale of the current
     * session is used.
     *
     * @param date
     *            The date.
     * @return The localized date string.
     */
    public final String localizedDate(final Date date) {
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
    public final String localizedNumber(final double number,
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
     */
    public void labelledCheckbox(final String wicketId, final String attrIdFor,
            final boolean checked) {
        tagCheckbox(wicketId, attrIdFor, checked);
        tagLabel(wicketId + "-label", wicketId, attrIdFor);
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
     */
    public void tagCheckbox(final String wicketId, final String htmlId,
            final boolean checked) {
        add(createCheckbox(wicketId, htmlId, checked));
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
     */
    public static Label createCheckbox(final String wicketId,
            final String htmlId, final boolean checked) {
        Label labelWrk = new Label(wicketId, "");
        labelWrk.add(new AttributeModifier("id", htmlId));
        if (checked) {
            labelWrk.add(new AttributeModifier("checked", "checked"));
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
     */
    public void tagRadio(final String wicketId, final String attrValue,
            final boolean checked) {
        tagRadio(wicketId, null, null, attrValue, checked);
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
     */
    public void tagRadio(final String wicketId, final String attrName,
            final String attrId, final String attrValue, final boolean checked) {

        Label labelWrk = new Label(wicketId);

        labelWrk.add(new AttributeModifier("value", attrValue));

        if (StringUtils.isNotBlank(attrId)) {
            labelWrk.add(new AttributeModifier("id", attrId));
        }

        if (StringUtils.isNotBlank(attrName)) {
            labelWrk.add(new AttributeModifier("name", attrName));
        }

        if (checked) {
            labelWrk.add(new AttributeModifier("checked", "checked"));
        }

        add(labelWrk);
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
     */
    public void tagLabel(final String wicketId, final String localizerKey,
            final String attrFor) {
        Label labelWrk =
                new Label(wicketId, getLocalizer().getString(localizerKey,
                        container));
        labelWrk.add(new AttributeModifier("for", attrFor));
        add(labelWrk);
    }

    /**
     * Adds a text label.
     *
     * @param wicketId
     *            The {@code wicket:id} of the HTML entity.
     * @param text
     *            The value of the HTML 'value' attribute.
     */
    public final void addLabel(final String wicketId, final String text) {
        Label labelWrk = new Label(wicketId, text);
        add(labelWrk);
    }

    /**
     * Adds input of type text.
     *
     * @param wicketId
     *            The {@code wicket:id} of the HTML entity.
     * @param value
     *            The value of the HTML 'value' attribute.
     */
    public final void addTextInput(final String wicketId, final String value) {
        Label labelWrk = new Label(wicketId);
        labelWrk.add(new AttributeModifier("value", value));
        add(labelWrk);
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
     */
    public final void addModifyLabelAttr(final String wicketId,
            final String attribute, final String value) {
        Label labelWrk = new Label(wicketId);
        labelWrk.add(new AttributeModifier(attribute, value));
        add(labelWrk);
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
     */
    public final void addModifyLabelAttr(final String wicketId,
            final String label, final String attribute, final String value) {
        Label labelWrk = new Label(wicketId, label);
        labelWrk.add(new AttributeModifier(attribute, value));
        add(labelWrk);
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
     */
    public final void addAppendLabelAttr(final String wicketId,
            final String attribute, final String value) {
        Label labelWrk = new Label(wicketId);
        labelWrk.add(new AttributeAppender(attribute, " " + value.trim()));
        add(labelWrk);
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
     */
    public final void addAppendLabelAttr(final String wicketId,
            final String label, final String attribute, final String value) {
        Label labelWrk = new Label(wicketId, label);
        labelWrk.add(new AttributeAppender(attribute, " " + value.trim()));
        add(labelWrk);
    }

    /**
     * Discloses a label.
     *
     * @param wicketId
     *            The {@code wicket:id} of the HTML entity.
     */
    public final void discloseLabel(final String wicketId) {
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
     * @return The label
     */
    public final Label encloseLabel(final String wicketId, final String value,
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
     *            The label value;
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

}
