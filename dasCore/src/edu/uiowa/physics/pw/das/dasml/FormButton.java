/* File: FormButton.java
 * Copyright (C) 2002-2003 University of Iowa
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
 * This class is provided to override the Java Beans properties of
 * the JButton class.
 *
 * @author Edward West
 */
public class FormButton extends JButton implements PropertyEditor.Editable, FormComponent {

    CommandAction commandAction;
    
    CommandBlock commandBlock;
    
    private boolean editable;
    
    private String dasName;
    
    protected edu.uiowa.physics.pw.das.util.DnDSupport dndSupport;
    
    public FormButton(String name, String label) {
        super(label);
        if (name == null) {
            name = "button_" + Integer.toHexString(System.identityHashCode(this));
        }
        try {
            setDasName(name);
        }
        catch (edu.uiowa.physics.pw.das.DasNameException dne) {
            edu.uiowa.physics.pw.das.util.DasExceptionHandler.handle(dne);
        }
    }
    
    /** Creates a new instance of FormButton */
    FormButton(Element element, FormBase form)
        throws edu.uiowa.physics.pw.das.DasPropertyException, ParsedExpressionException {

        String name = element.getAttribute("name");
        String label = element.getAttribute("label");
        boolean enabled = element.getAttribute("enabled").equals("true");
        
        setText(label);
        setEnabled(enabled);
        
        if (!name.equals("")) {
            try {
                setDasName(name);
            }
            catch (edu.uiowa.physics.pw.das.DasNameException dne) {
                edu.uiowa.physics.pw.das.util.DasExceptionHandler.handle(dne);
            }
        }
        
        NodeList children = element.getChildNodes();
        int childCount = children.getLength();
        for (int index = 0; index < childCount; index++) {
            Node node = children.item(index);
            if (node instanceof Element && node.getNodeName().equals("action")) {
                Element actionElement = (Element)node;
                commandBlock = new CommandBlock(actionElement, form);
                commandAction = new CommandAction(commandBlock);
                addActionListener(commandAction);
            }
        }
    }
    
    public CommandBlock getFormAction() {
        return commandBlock;
    }
    
    public void setFormAction(CommandBlock cb) {
        if (cb == commandBlock) {
            return;
        }
        if (commandBlock != null) {
            removeActionListener(commandAction);
        }
        if (cb == null) {
            commandAction = null;
            commandBlock = null;
        }
        else {
            commandBlock = cb;
            commandAction = new CommandAction(commandBlock);
            addActionListener(commandAction);
        }
    }
 
    public Element getDOMElement(Document document) {
        Element element = document.createElement("button");
        element.setAttribute("name", getDasName());
        element.setAttribute("label", getText());
        element.setAttribute("enabled", String.valueOf(isEnabled()));
        if (commandBlock != null) {
            Element actionElement = document.createElement("action");
            commandBlock.appendDOMElements(actionElement);
            element.appendChild(actionElement);
        }
        return element;
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
    
    public boolean getEditingMode() {
        return editable;
    }
    
    public void setEditingMode(boolean b) {
        editable = b;
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
