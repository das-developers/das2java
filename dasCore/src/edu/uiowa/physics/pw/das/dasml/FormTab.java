/* File: FormTab.java
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

package edu.uiowa.physics.pw.das.dasml;

import org.das2.NameContext;
import org.das2.DasApplication;
import org.das2.DasException;
import org.das2.DasPropertyException;
import edu.uiowa.physics.pw.das.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.*;
import org.das2.util.DasExceptionHandler;

/**
 *
 * @author  eew
 */
public class FormTab extends FormContainer {
    
    private String label;
    
    private String dasName;
    
    /** Initializer for flavorList */
    {
        java.awt.datatransfer.DataFlavor[] flavors = {
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
    
    /** Creates a new instance of Form */
    public FormTab(String name, String label) {
        super();
        setDirection(Orientation.VERTICAL);
        if (name == null) {
            name = "form_" + Integer.toHexString(System.identityHashCode(this));
        }
        try {
            setDasName(name);
        }
        catch (org.das2.DasNameException dne) {
            org.das2.util.DasExceptionHandler.handle(dne);
        }
        this.label = label;
        dndSupport = new ContainerDnDSupport(null);
    }
    
    FormTab(Element element, FormBase form)
        throws DasException,
        ParsedExpressionException, org.xml.sax.SAXException {

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
        String name = element.getAttribute("name");
        String label = element.getAttribute("label");
        setDirection(Orientation.VERTICAL);
        try {
            setDasName(name);
        }
        catch (org.das2.DasNameException dne) {
            org.das2.util.DasExceptionHandler.handle(dne);
        }
        if (label.equals("")) {
            setLabel(name);
        }
        else {
            setLabel(label);
        }
        
        NodeList children = element.getChildNodes();
        int length = children.getLength();
        for (int index = 0; index < length; index++) {
            Node node = children.item(index);
            if (node instanceof Element) {
                String tagName = node.getNodeName();
                if (tagName.equals("panel")) {
                    try {
                        FormPanel p = new FormPanel((Element)node, form);
                        add(p);
                    } catch ( java.text.ParseException ex ) {
                        DasExceptionHandler.handle(ex);
                    }
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
                    try {
                        edu.uiowa.physics.pw.das.graph.DasCanvas canvas = edu.uiowa.physics.pw.das.graph.DasCanvas.processCanvasElement((Element)node, form);
                        canvas.setAlignmentX(horizontalComponentAlignment);
                        add(canvas);
                    }

                    catch (org.das2.DasException dne) {
                        DasExceptionHandler.handle(dne);
                    }                    catch ( java.text.ParseException ex ) {
                        DasExceptionHandler.handle(ex);
                    }
                }
                else {
                    //DO NOTHING RIGHT NOW
                }
            }
            else {
                //TODO: do some sort of error handling here.
            }
        }
        dndSupport = new ContainerDnDSupport(null);
    }
    
    public String getLabel() {
        return label;
    }
    
    public void setLabel(String label) {
        this.label = label;
        FormBase form = getForm();
        if (form != null) {
            form.setTitleAt(getForm().indexOfComponent(this), label);
        }
    }
    
    public Element getDOMElement(Document document) {
        Element element = document.createElement("form");
        element.setAttribute("name", getDasName());
        element.setAttribute("label", getLabel());
        for (int index = 0; index < getComponentCount(); index++) {
            java.awt.Component comp = getComponent(index);
            if (comp instanceof FormComponent) {
                FormComponent formComponent = (FormComponent)comp;
                Element child = formComponent.getDOMElement(document);
                element.appendChild(child);
            }
            else if (comp instanceof edu.uiowa.physics.pw.das.graph.DasCanvas) {
                edu.uiowa.physics.pw.das.graph.DasCanvas canvas = (edu.uiowa.physics.pw.das.graph.DasCanvas)comp;
                Element child = canvas.getDOMElement(document);
                element.appendChild(child);
            }
        }
        return element;
    }
    
    public String getDasName() {
        return dasName;
    }
    
    public void setDasName(String name) throws org.das2.DasNameException {
        if (name.equals(dasName)) {
            return;
        }
        String oldName = dasName;
        dasName = name;
        DasApplication app = getDasApplication();
        if (app != null) {
            app.getNameContext().put(name, this);
            if (oldName != null) {
                app.getNameContext().remove(oldName);
            }
        }
        this.firePropertyChange("name", oldName, name);
    }
    
    public org.das2.util.DnDSupport getDnDSupport() {
        if (dndSupport == null) {
            dndSupport = new ContainerDnDSupport(null);
        }
        return dndSupport;
    }
    
    public void deregisterComponent() {
        DasApplication app = getDasApplication();
        if (app != null) {
            NameContext nc = app.getNameContext();
            try {
                if (nc.get(getDasName()) == this) {
                    nc.remove(getDasName());
                }
            }
            catch (DasPropertyException dpe) {
                //This exception would only occur due to some invalid state.
                //So, wrap it and toss it.
                IllegalStateException se = new IllegalStateException(dpe.toString());
                se.initCause(dpe);
                throw se;
            }
            catch (java.lang.reflect.InvocationTargetException ite) {
                //This exception would only occur due to some invalid state.
                //So, wrap it and toss it.
                IllegalStateException se = new IllegalStateException(ite.toString());
                se.initCause(ite);
                throw se;
            }
        }
        super.deregisterComponent();
    }
    
    public final DasApplication getDasApplication() {
        java.awt.Container p = getParent();
        if (p instanceof FormComponent) {
            return ((FormComponent)p).getDasApplication();
        }
        else {
            return null;
        }
    }
    
    public void registerComponent() throws DasException {
        DasApplication app = getDasApplication();
        if (app != null) {
            NameContext nc = app.getNameContext();
            nc.put(getDasName(), this);
        }
        super.registerComponent();
    }
    
}
