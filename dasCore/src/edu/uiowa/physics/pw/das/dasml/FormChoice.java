/* File: FormChoice.java
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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.*;

/**
 * Drop down list for making single selections.
 */
public class FormChoice extends JComboBox implements PropertyEditor.Editable, FormComponent, OptionList {
    
    protected edu.uiowa.physics.pw.das.util.DnDSupport dndSupport;
    
    private boolean editable;
    
    private String dasName;
    
    public FormChoice(String name) {
        if (name == null) {
            name = "choice_" + Integer.toString(System.identityHashCode(this));
        }
        try {
            setDasName(name);
        }
        catch(edu.uiowa.physics.pw.das.DasNameException dne) {
            edu.uiowa.physics.pw.das.util.DasExceptionHandler.handle(dne);
        }
    }
    
    FormChoice(Element element, FormBase form)
        throws edu.uiowa.physics.pw.das.DasPropertyException, edu.uiowa.physics.pw.das.DasNameException,
        ParsedExpressionException {
        
        super();
        
        String name = element.getAttribute("name");
        
        NodeList children = element.getChildNodes();
        int childCount = children.getLength();
        for (int i = 0; i < childCount; i++) {
            Node node = children.item(i);
            if (node instanceof Element && node.getNodeName().equals("option")) {
                processOptionElement((Element)node);
            }
            else if (node instanceof Element && node.getNodeName().equals("action")) {
                CommandBlock cb = new CommandBlock((Element)node, form);
                CommandAction action = new CommandAction(cb);
                addActionListener(action);
            }
        }
        try {
            setDasName(name);
        }
        catch (edu.uiowa.physics.pw.das.DasNameException dne) {
            edu.uiowa.physics.pw.das.util.DasExceptionHandler.handle(dne);
        }
    }
    
    public java.awt.Dimension getMinimumSize() {
        return getPreferredSize();
    }
    
    public java.awt.Dimension getMaximumSize() {
        return getPreferredSize();
    }
    
    private void processOptionElement(Element element) {
        boolean selected = element.getAttribute("selected").equals("true");
        ListOption option = new ListOption(element);
        addItem(option);
        if (selected) setSelectedItem(option);
    }

    public String getSelectedValue() {
        ListOption selected = (ListOption)getSelectedItem();
        if (selected == null) {
            return null;
        }
        return selected.getValue();
    }
    
    public void addOption(ListOption option) {
        addItem(option);
    }
    
    public Element getDOMElement(Document document) {
        Element element = document.createElement("choice");
        element.setAttribute("name", getDasName());
        element.setAttribute("enabled", String.valueOf(isEnabled()));
        for (int index = 0; index < getItemCount(); index++) {
            ListOption option = (ListOption)getItemAt(index);
            element.appendChild(option.getDOMElement(document));
        }
        return element;
    }
    
    public ListOption[] getOptions() {
        ListModel model = getModel();
        ListOption[] options = new ListOption[model.getSize()];
        for (int index = 0; index < options.length; index++) {
            options[index] = (ListOption)model.getElementAt(index);
        }
        return options;
    }
    
    public void setOptions(ListOption[] options) {
        setModel(new DefaultComboBoxModel(options));
        if (options.length == 0) {
            setSelectedItem(null);
        }
        else {
            setSelectedItem(options[0]);
            for (int index = 0; index < options.length; index++) {
                if (options[index].isSelected()) {
                    setSelectedItem(options[index]);
                }
            }
        }
    }
    
    public Object getPrototypeDisplayValue() {
        if (this.getItemCount() == 0) {
            return "XXXXXXXXXXXX";
        }
        return null;
    }
    
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
    
    public boolean getEditingMode() {
        return editable;
    }
    
    public void setEditingMode(boolean b) {
        editable = b;
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
