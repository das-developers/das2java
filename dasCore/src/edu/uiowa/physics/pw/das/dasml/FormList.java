/* File: FormList.java
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
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.basic.BasicListUI;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;

public class FormList extends JList implements PropertyEditor.Editable, FormComponent {
    
    private String delimiter = " ";
    
    protected edu.uiowa.physics.pw.das.util.DnDSupport dndSupport;
    
    private String dasName;
    
    private boolean editable;
    
    public FormList(String name) {
        try {
            setDasName(name);
        }
        catch(edu.uiowa.physics.pw.das.DasNameException dne) {
            edu.uiowa.physics.pw.das.util.DasExceptionHandler.handle(dne);
        }
    }
    
    FormList(Element element, FormBase form)
        throws edu.uiowa.physics.pw.das.DasPropertyException, edu.uiowa.physics.pw.das.DasNameException,
        ParsedExpressionException {
        
        super(new OptionListModel());
        
        String name = element.getAttribute("name");
        String selectionMode = element.getAttribute("selectionMode");
        
        if (selectionMode.equals("single")) {
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        }
        else if (selectionMode.equals("multiple")) {
            setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        }

        setDelimiter(element.getAttribute("delimiter"));
        setEnabled(element.getAttribute("enabled").equals("true"));

        NodeList children = element.getChildNodes();
        int childCount = children.getLength();
        for (int i = 0; i < childCount; i++) {
            Node node = children.item(i);
            if (node instanceof Element && node.getNodeName().equals("option")) {
                processOptionElement((Element)node);
            }
        }
        setMinimumSize(getPreferredSize());
        setMaximumSize(getPreferredSize());
        try {
            setDasName(name);
        }
        catch(edu.uiowa.physics.pw.das.DasNameException dne) {
            edu.uiowa.physics.pw.das.util.DasExceptionHandler.handle(dne);
        }
    }
    
    private void processOptionElement(Element element) {
        ListOption option = new ListOption(element);
        ((OptionListModel)getModel()).list.add(option);
        boolean selected = element.getAttribute("selected").equals("true");
        if (selected) addSelectionInterval(getModel().getSize()-1, getModel().getSize()-1);
    }

    
    public void addMouseListener(MouseListener l) {
        if (l instanceof BasicListUI.MouseInputHandler) {
            l = new CtrlDownMouseInputListener((MouseInputListener)l);
        }
        super.addMouseListener(l);
    }
    
    public void addMouseMotionListener(MouseMotionListener l) {
        if (l instanceof BasicListUI.MouseInputHandler) {
            l = new CtrlDownMouseInputListener((MouseInputListener)l);
        }
        super.addMouseMotionListener(l);
    }
    
    public void addItem(ListOption o) {
        ((OptionListModel)getModel()).list.add(o);
    }
    
    public ListOption getItem(int index) {
        return (ListOption)((OptionListModel)getModel()).list.get(index);
    }
    
    public int getItemCount() {
        return ((OptionListModel)getModel()).list.size();
    }
    
    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }
    
    public String getDelimiter() {
        return delimiter;
    }
    
    public String getSelected() {
        Object[] o = getSelectedValues();
        if (o.length == 0) return "";
        String result = ((ListOption)o[0]).getValue();
        for (int i = 1; i < o.length; i++) {
            result = result + delimiter + ((ListOption)o[i]).getValue();
        }
        return result;
    }
    
    private static class CtrlDownMouseEvent extends MouseEvent {
        private static final int CTRL_YES = CTRL_MASK | CTRL_DOWN_MASK;
        private static final int SHIFT_NO = -(SHIFT_MASK | SHIFT_DOWN_MASK)-1;
        public CtrlDownMouseEvent(MouseEvent e) {
            super(e.getComponent(), e.getID(), e.getWhen(),
            (e.getModifiers() | CTRL_YES) & SHIFT_NO,
            e.getX(), e.getY(), e.getClickCount(),
            e.isPopupTrigger(), e.getButton());
        }
    }
    
    private static class CtrlDownMouseInputListener extends MouseInputAdapter {
        private MouseInputListener listener;
        public CtrlDownMouseInputListener(MouseInputListener listener) {
            this.listener = listener;
        }
        public void mousePressed(MouseEvent e) {
            listener.mousePressed(new CtrlDownMouseEvent(e));
        }
        public void mouseReleased(MouseEvent e) {
            listener.mouseReleased(new CtrlDownMouseEvent(e));
        }
        public void mouseDragged(MouseEvent e) {
            listener.mouseDragged(new CtrlDownMouseEvent(e));
        }
    }

    void setSelected(Object item, boolean b) {
        OptionListModel model = (OptionListModel)getModel();
        int index = 0;
        while (index < model.list.size() && model.list.get(index) != item) {
            index++;
        }
        if (model.list.get(index) != item) return;
        if (b) {
            addSelectionInterval(index, index);
        }
        else {
            removeSelectionInterval(index, index);
        }
    }
    
    
    public Element getDOMElement(Document document) {
        Element element = document.createElement("list");
        element.setAttribute("name", getDasName());
        element.setAttribute("delimiter", delimiter);
        element.setAttribute("enabled", (isEnabled() ? "true" : "false"));
        for (int index = 0; index < getItemCount(); index++) {
            element.appendChild(getItem(0).getDOMElement(document));
        }
        return element;
    }
    
    private static class OptionListModel extends AbstractListModel {
        
        List list = new ArrayList();
        
        /** Returns the value at the specified index.s
         * @param index the requested index
         * @return the value at <code>index</code>
         */
        public Object getElementAt(int index) {
            return list.get(index);
        }
        
        /**
         * Returns the length of the list.
         * @return the length of the list
         */
        public int getSize() {
            return list.size();
        }
        
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
    
}
