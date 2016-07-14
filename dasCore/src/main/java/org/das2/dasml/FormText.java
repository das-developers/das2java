/* File: FormText.java
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

import org.das2.DasApplication;
import org.das2.DasException;
import org.w3c.dom.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import org.das2.components.propertyeditor.Editable;

/**
 *
 * @author  eew
 */
public class FormText extends JTextArea implements Editable, FormComponent {
    
    protected org.das2.util.DnDSupport dndSupport;
    
    private boolean editingMode;
    
    public FormText() {
        super("");
        super.setEditable(false);
        setOpaque(false);
        setBorder(new EmptyBorder(5,5,5,5));
    }
    
    FormText(Element element) {
        this();
        Text text = getElementContent(element);
        if (text != null) {
            setText(text.getData());
        }
    }
    
    private static Text getElementContent(Element element) {
        NodeList children = element.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node node = children.item(index);
            if (node instanceof Text) {
                return (Text)node;
            }
        }
        return null;
    }
   
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }
    
    public Element getDOMElement(Document document) {
        Element element = document.createElement("text");
        Text text = document.createTextNode(getText());
        element.appendChild(text);
        return element;
    }
    
    public FormBase getForm() {
        FormComponent parent = (FormComponent)getParent();
        if (parent == null) {
            return null;
        }
        return parent.getForm();
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
    
    public void setEditingMode(boolean b) { editingMode = b; }
    
    public boolean getEditingMode() { return editingMode; }
    
    public String getDasName() {
        return null;
    }
    
    public void setDasName(String name) throws org.das2.DasNameException {
        throw new org.das2.DasNameException();
    }
    
    public void deregisterComponent() {
    }
    
    public DasApplication getDasApplication() {
        Container p = getParent();
        if (p instanceof FormComponent) {
            return ((FormComponent)p).getDasApplication();
        }
        else {
            return null;
        }
    }
    
    public void registerComponent() throws DasException {
    }
    
}
