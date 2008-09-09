/* File: FormPanel.java
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

import edu.uiowa.physics.pw.das.graph.DasCanvas;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import org.das2.components.propertyeditor.Editable;

/**
 * A subclass of JPanel to override the default Beans properties of
 * a JPanel
 *
 * @author  Edward West
 */
public class FormPanel extends FormContainer implements Editable, FormComponent {

    /** Initializer for flavorList */
    {
        DataFlavor[] flavors = {
            TransferableFormComponent.BUTTONGROUP_FLAVOR,
            TransferableFormComponent.BUTTON_FLAVOR,
            TransferableFormComponent.CHECKBOX_FLAVOR,
            TransferableFormComponent.CHOICE_FLAVOR,
            //TransferableFormComponent.LIST_FLAVOR,
            TransferableFormComponent.PANEL_FLAVOR,
            TransferableFormComponent.TEXTFIELD_FLAVOR,
            TransferableFormComponent.TEXT_FLAVOR,
            edu.uiowa.physics.pw.das.graph.dnd.TransferableCanvas.CANVAS_FLAVOR
        };
        flavorList = java.util.Arrays.asList(flavors);
    }
    
    
    /**
     * Empty constructor for use with super classes.
     */
    public FormPanel() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setBorder(new NoBorder());
    }
    
    /**
     * Constructs a FormPanel object associated with the given
     * FormBase instance and initialized with the values in the given
     * Element instance.
     */
    FormPanel(Element element, FormBase form)
        throws org.das2.DasException,
        ParsedExpressionException, org.xml.sax.SAXException, java.text.ParseException {
        
        super();
        
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
            layout = new BoxLayout(this, BoxLayout.X_AXIS);
        }
        else {
            layout = new BoxLayout(this, BoxLayout.Y_AXIS);
        }
        setLayout(layout);

        NodeList children = element.getChildNodes();
        int length = children.getLength();
        for (int index = 0; index < length; index++) {
            Node node = children.item(index);
            if (node instanceof Element) {
                String tagName = node.getNodeName();
                if (tagName.equals("panel")) {
                    FormPanel p = new FormPanel((Element)node, form);
                    add(p);
                }
                else if (tagName.equals("text") || tagName.equals("info")) {
                    FormText text = new FormText((Element)node);
                    add(text);
                }
                else if (tagName.equals("textfield")) {
                    FormTextField textfield = new FormTextField((Element)node, form);
                    add(textfield);
                }
                else if (tagName.equals("button")) {
                    FormButton button = new FormButton((Element)node, form);
                    add(button);
                }
                else if (tagName.equals("checkbox")) {
                    FormCheckBox checkbox = new FormCheckBox((Element)node, form);
                    add(checkbox);
                }
                else if (tagName.equals("list")) {
                    FormList list = new FormList((Element)node, form);
                    add(list);
                }
                else if (tagName.equals("choice")) {
                    FormChoice choice = new FormChoice((Element)node, form);
                    add(choice);
                }
                else if (tagName.equals("glue")) {
                    add(form.processGlueElement((Element)node));
                }
                else if (tagName.equals("buttongroup")) {
                    add(new FormRadioButtonGroup((Element)node, form));
                }
                else if (tagName.equals("canvas")) {
                    DasCanvas canvas = DasCanvas.processCanvasElement((Element)node, form);
                    add(canvas);
                }
                else {
                    //DO NOTHING RIGHT NOW
                }
            }
            else {
                //TODO: do some sort of error handling here.
            }
        }

        setHasBorder(element.getAttribute("border").equals("true"));
        setBorderTitle(element.getAttribute("border-title"));
    }

    public Element getDOMElement(Document document) {
        Element element = document.createElement("panel");
        element.setAttribute("border", Boolean.toString(hasBorder()));
        element.setAttribute("border-title", getBorderTitle());
        for (int index = 0; index < getComponentCount(); index++) {
            Component comp = getComponent(index);
            if (comp instanceof FormComponent) {
                FormComponent formComponent = (FormComponent)comp;
                Element child = formComponent.getDOMElement(document);
                element.appendChild(child);
            }
            else if (comp instanceof DasCanvas) {
                DasCanvas canvas = (DasCanvas)comp;
                Element child = canvas.getDOMElement(document);
                element.appendChild(child);
            }
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
