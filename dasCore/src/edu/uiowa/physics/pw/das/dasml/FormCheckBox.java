/* File: FormCheckBox.java
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

import edu.uiowa.physics.pw.das.components.PropertyEditor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.*;

/**
 * This class is provided to override the Java Beans properties of
 * the JCheckBox class.
 *
 * @author Edward West
 */
public class FormCheckBox extends JCheckBox implements PropertyEditor.Editable, FormComponent {

    CommandAction commandAction;
    
    CommandBlock commandBlock;
    
    private String dasName;
    
    protected edu.uiowa.physics.pw.das.util.DnDSupport dndSupport;
    
    private boolean editable;
    
    public FormCheckBox(String name, String label) {
        super(label);
        if (name == null) {
            name = "checkbox_" + Integer.toHexString(System.identityHashCode(this));
        }
        try {
            setDasName(name);
        }
        catch(edu.uiowa.physics.pw.das.DasNameException dne) {
            edu.uiowa.physics.pw.das.util.DasExceptionHandler.handle(dne);
        }
    }
    
    FormCheckBox(Element element, FormBase form)
        throws edu.uiowa.physics.pw.das.DasPropertyException, ParsedExpressionException,
        edu.uiowa.physics.pw.das.DasNameException {
        
        String name = element.getAttribute("name");
        String label = element.getAttribute("label");
        boolean enabled = element.getAttribute("enabled").equals("true");
        boolean selected = element.getAttribute("selected").equals("true");
        
        setText(label);
        setEnabled(enabled);
        setSelected(selected);
        
        try {
            setDasName(name);
        }
        catch(edu.uiowa.physics.pw.das.DasNameException dne) {
            edu.uiowa.physics.pw.das.util.DasExceptionHandler.handle(dne);
        }
        
        NodeList children = element.getChildNodes();
        int childCount = children.getLength();
        for (int index = 0; index < childCount; index++) {
            Node node = children.item(index);
            if (node instanceof Element && node.getNodeName().equals("action")) {
                commandBlock = new CommandBlock((Element)node, form);
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
        Element element = document.createElement("checkbox");
        element.setAttribute("name", getDasName());
        element.setAttribute("enabled", String.valueOf(isEnabled()));
        element.setAttribute("selected", String.valueOf(isSelected()));
        element.setAttribute("label", getText());
        if (commandBlock != null) {
            Element commandElement = document.createElement("action");
            commandBlock.appendDOMElements(element);
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
        edu.uiowa.physics.pw.das.NameContext.getDefaultNameContext().put(name, this);
        if (oldName != null) {
            edu.uiowa.physics.pw.das.NameContext.getDefaultNameContext().remove(oldName);
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
    
}
