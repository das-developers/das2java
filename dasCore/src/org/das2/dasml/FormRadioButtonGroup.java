/* File: FormRadioButtonGroup.java
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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import org.das2.components.propertyeditor.Editable;

public class FormRadioButtonGroup extends FormContainer implements Editable, FormComponent {
    
    /**
     * The button group the radio buttons will be associated with.
     */
    private ButtonGroup group;
    
    /** Initializer for flavorList */
    {
        DataFlavor[] flavors = {
            TransferableFormComponent.RADIOBUTTON_FLAVOR
        };
        flavorList = java.util.Arrays.asList(flavors);
    }
    
    public FormRadioButtonGroup() {
        setDirection(Orientation.HORIZONTAL);
        setHasBorder(false);
        group = new ButtonGroup();
    }
    
    FormRadioButtonGroup(Element element, FormBase form)
        throws org.das2.DasPropertyException, ParsedExpressionException,
        org.das2.DasNameException, org.xml.sax.SAXException {
           
        group = new ButtonGroup();
        
        String alignment = element.getAttribute("alignment");
        if (alignment.equals("left")) {
            horizontalComponentAlignment = JComponent.LEFT_ALIGNMENT;
        }
        else if (alignment.equals("right")) {
            horizontalComponentAlignment = JComponent.RIGHT_ALIGNMENT;
        }
        else {
            horizontalComponentAlignment = JComponent.CENTER_ALIGNMENT;
        }

        String direction = element.getAttribute("direction");
        BoxLayout layout;
        if (direction.equals("horizontal")) {
            setDirection(Orientation.HORIZONTAL);
        }
        else {
            setDirection(Orientation.VERTICAL);
        }

        NodeList children = element.getChildNodes();
        int length = children.getLength();
        for (int index = 0; index < length; index++) {
            Node node = children.item(index);
            if (node instanceof Element) {
                String tagName = node.getNodeName();
                if (tagName.equals("radiobutton")) {
                    FormRadioButton radiobutton = new FormRadioButton((Element)node, form);
                    radiobutton.setAlignmentX(horizontalComponentAlignment);
                    add(radiobutton);
                }
            }
        }

        setHasBorder(element.getAttribute("border").equals("true"));
        setBorderTitle(element.getAttribute("border-title"));
    }
    
    protected void addImpl(Component comp, Object constraints, int index) {
        if (comp instanceof FormRadioButton) {
            FormRadioButton radiobutton = (FormRadioButton)comp;
            super.addImpl(radiobutton, constraints, index);
            group.add(radiobutton);
        }
        else {
            throw new IllegalArgumentException("Only FormRadioButton instances allowed");
        }
    }
    
    public Element getDOMElement(Document document) {
        Element element = document.createElement("group");
        for (int index = 0; index < getComponentCount(); index++) {
            FormComponent comp = (FormComponent)getComponent(index);
            Element child = comp.getDOMElement(document);
            element.appendChild(child);
        }
        return element;
    }
    
    public org.das2.util.DnDSupport getDnDSupport() {
        if (dndSupport == null) {
            dndSupport = new ContainerDnDSupport(null);
        }
        return dndSupport;
    }
    
}
