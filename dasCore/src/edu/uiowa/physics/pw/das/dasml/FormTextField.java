/* File: FormTextField.java
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

import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.components.PropertyEditor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import javax.swing.*;

/**
 * A subclass of JTextField to override the default Beans properties of
 * a JTextField
 *
 * @author Edward West
 */
public class FormTextField extends JTextField implements PropertyEditor.Editable, FormComponent {
    
    private String dasName;
    
    protected edu.uiowa.physics.pw.das.util.DnDSupport dndSupport;
    
    private boolean editingMode;
    
    public FormTextField(String name) {
        super(10);
        if (name == null) {
            name="textfield_" + Integer.toHexString(System.identityHashCode(this));
        }
        try {
            setDasName(name);
        }
        catch (edu.uiowa.physics.pw.das.DasNameException dne) {
            edu.uiowa.physics.pw.das.util.DasExceptionHandler.handle(dne);
        }
        setMinimumSize(getPreferredSize());
        setMaximumSize(getPreferredSize());
    }
    
    public FormTextField(Element element, FormBase form)
        throws edu.uiowa.physics.pw.das.DasPropertyException, edu.uiowa.physics.pw.das.DasNameException,
        ParsedExpressionException {

        String name = element.getAttribute("name");

        int length = Integer.parseInt(element.getAttribute("length"));
        setColumns(length);
        setMinimumSize(getPreferredSize());
        setMaximumSize(getPreferredSize());

        NodeList children = element.getChildNodes();
        int childCount = children.getLength();
        for (int i = 0; i < childCount; i++) {
            if (children.item(i) instanceof Text) {
                Text text = (Text)children.item(i);
                setText(text.getData());
                break;
            }
        }
        
        try {
            setDasName(name);
        }
        catch (edu.uiowa.physics.pw.das.DasNameException dne) {
            edu.uiowa.physics.pw.das.util.DasExceptionHandler.handle(dne);
        }
    }
    
    public Element getDOMElement(Document document) {
        Element element = document.createElement("textfield");
        Text text = document.createTextNode(getText());
        element.appendChild(text);
        element.setAttribute("name", getDasName());
        element.setAttribute("enabled", (isEnabled() ? "true" : "false"));
        element.setAttribute("length", Integer.toString(getColumns()));
        return element;
    }
    
    public FormBase getForm() {
        FormComponent parent = (FormComponent)getParent();
        if (parent == null) {
            return null;
        }
        return parent.getForm();
    }
    
    public edu.uiowa.physics.pw.das.util.DnDSupport getDnDSupport() {
        if (dndSupport == null) {
            dndSupport = new DefaultComponentDnDSupport(this);
        }
        return dndSupport;
    }
    
    public boolean startDrag(int x, int y, int action, java.awt.event.MouseEvent evt) {
        return false;
    }
    
    public void setEditingMode(boolean b) { editingMode = b; }
    public boolean getEditingMode() { return editingMode; }
    
    public String getDasName() {
        return dasName;
    }
    
    public void setDasName(String name) throws edu.uiowa.physics.pw.das.DasNameException {
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
    }

    public DasApplication getDasApplication() {
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
    }
    
}
