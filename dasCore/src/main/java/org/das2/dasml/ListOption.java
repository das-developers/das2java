/* File: ListOption.java
 * Copyright (C) 2002-2003 The University of Iowa
 * Created by: Jeremy Faden <jbf@space.physics.uiowa.edu>
 *             Jessica Swanner <jessica@space.physics.uiowa.edu>
 *             Edward E. West <eew@space.physics.uiowa.edu>
 *
 * This file is part of the das2 library.
 *
 * das2 is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.das2.dasml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.awt.*;

public class ListOption {
    private String label;
    private String value;
    private boolean selected;
    
    ItemSelectable selectable;
    
    public ListOption(FormBase form, String label, String value) {
        this.label = label;
        this.value = value;
    }
    
    ListOption(Element element) {
        label = element.getAttribute("label");
        value = element.getAttribute("value");
    }
    
    ListOption(String label, String value) {
        this.label = label;
        this.value = value;
    }
    
    public String toString() {
        return getLabel();
    }
    
    public String getLabel() {
        return label;
    }
    
    public String getValue() {
        return value;
    }
    
    public void setLabel(String label) {
        this.label = label;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
    
    public boolean isSelected() {
        if (selectable != null) {
            Object[] items = selectable.getSelectedObjects();
            for (int index = 0; index < items.length; index++) {
                if (items[index] == this) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public void setSelected(boolean b) {
        if (selectable instanceof FormChoice) {
            if (b) {
                FormChoice choice = (FormChoice)selectable;
                choice.setSelectedItem(this);
            }
        }
        else if (selectable instanceof FormList) {
            FormList list = (FormList)selectable;
            list.setSelected(this, b);
        }
    }
    
    public Element getDOMElement(Document document) {
        Element element = document.createElement("option");
        element.setAttribute("label", label);
        element.setAttribute("value", value);
        element.setAttribute("selected", String.valueOf(isSelected()));
        return element;
    }
    
}

