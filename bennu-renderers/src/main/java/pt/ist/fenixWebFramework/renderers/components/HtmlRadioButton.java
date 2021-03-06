/**
 * Copyright © 2008 Instituto Superior Técnico
 *
 * This file is part of Bennu Renderers Framework.
 *
 * Bennu Renderers Framework is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bennu Renderers Framework is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Bennu Renderers Framework.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixWebFramework.renderers.components;

import javax.servlet.jsp.PageContext;

import pt.ist.fenixWebFramework.renderers.components.tags.HtmlTag;

public class HtmlRadioButton extends HtmlInputComponent {

    private String text;

    private boolean checked;

    public HtmlRadioButton() {
        super("radio");

        this.checked = false;
    }

    public HtmlRadioButton(boolean checked) {
        this();

        this.checked = checked;
    }

    public HtmlRadioButton(String text) {
        this();

        this.text = text;
    }

    public HtmlRadioButton(String text, boolean checked) {
        this();

        this.text = text;
        this.checked = checked;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getUserValue() {
        return super.getValue();
    }

    public void setUserValue(String userValue) {
        super.setValue(userValue == null ? "" : userValue);
    }

    @Override
    public void setValue(String value) {
        setChecked(String.valueOf(getUserValue()).equals(value));
    }

    @Override
    public HtmlTag getOwnTag(PageContext context) {
        HtmlTag tag = super.getOwnTag(context);

        if (this.checked) {
            tag.setAttribute("checked", this.checked);
        }

        if (this.text == null) {
            return tag;
        }

        HtmlTag span = new HtmlTag("span");

        span.addChild(tag);
        span.addChild(new HtmlTag(null, this.text));

        return span;
    }
}
