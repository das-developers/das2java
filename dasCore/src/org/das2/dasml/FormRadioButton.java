/* File: FormRadioButton.java
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

import org.das2.NameContext;
import org.das2.DasApplication;
import org.das2.DasException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.swing.*;
import org.das2.DasPropertyException;
import org.das2.components.propertyeditor.Editable;

public class FormRadioButton extends JRadioButton implements Editable, FormComponent {
    
    private String dasName;
    
    private String value;
    
    protected org.das2.util.DnDSupport dndSupport;
    
    private boolean editable;
    
    public FormRadioButton(String name, String label) {
        super(label);
        if (name == null) {
            name = "radiobutton_" + Integer.toHexString(System.identityHashCode(this));
        }
        try {
            setDasName(name);
        }
        catch (org.das2.DasNameException dne) {
            org.das2.util.DasExceptionHandler.handle(dne);
        }
    }
    
    FormRadioButton(Element element, FormBase form)
        throws org.das2.DasPropertyException, ParsedExpressionException,
        org.das2.DasNameException {

        String name = element.getAttribute("name");
        String label = element.getAttribute("label");
        boolean selected = element.getAttribute("selected").equals("true");
        boolean enabled = element.getAttribute("enabled").equals("true");
        
        setText(label);
        setSelected(selected);
        setEnabled(enabled);
        
        if (!name.equals("")) {
            try {
                setDasName(name);
            }
            catch (org.das2.DasNameException dne) {
                org.das2.util.DasExceptionHandler.handle(dne);
            }
        }
        
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
    
    public Element getDOMElement(Document document) {
        Element element = document.createElement("radiobutton");
        element.setAttribute("name", getDasName());
        element.setAttribute("selected", String.valueOf(isSelected()));
        element.setAttribute("enabled", String.valueOf(isEnabled()));
        element.setAttribute("label", getText());
        return element;
    }
    
    public FormBase getForm() {
        FormComponent parent = (FormComponent)getParent();
        if (parent == null) {
            return null;
        }
        return parent.getForm();
    }
    
    public boolean getEditingMode() {
        return editable;
    }
    
    public void setEditingMode(boolean b) {
        editable = b;
    }
    
    public org.das2.util.DnDSupport getDnDSupport() {
        if (dndSupport == null) {
            dndSupport = new DefaultComponentDnDSupport(this);
        }
        return dndSupport;
    }
    
    public boolean startDrag(int x, int y, int action, java.awt.event.MouseEvent evt) {
        return false;
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
