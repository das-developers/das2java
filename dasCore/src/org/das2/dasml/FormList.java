/* File: FormList.java
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
import org.das2.DasPropertyException;
import org.das2.components.propertyeditor.Editable;

public final class FormList extends JList implements Editable, FormComponent {
    
    private String delimiter = " ";
    
    protected org.das2.util.DnDSupport dndSupport;
    
    private String dasName;
    
    private boolean editable;
    
    public FormList(String name) {
        try {
            setDasName(name);
        }
        catch(org.das2.DasNameException dne) {
            org.das2.util.DasExceptionHandler.handle(dne);
        }
    }
    
    FormList(Element element, FormBase form)
        throws  org.das2.DasPropertyException,org.das2.DasNameException, org.das2.DasException ,
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
        catch(org.das2.DasNameException dne) {
            org.das2.util.DasExceptionHandler.handle(dne);
        }
    }
    
    private void processOptionElement(Element element) {
        ListOption option = new ListOption(element);
        ((OptionListModel)getModel()).list.add(option);
        boolean selected = element.getAttribute("selected").equals("true");
        if (selected) addSelectionInterval(getModel().getSize()-1, getModel().getSize()-1);
    }

    
    @Override
    public void addMouseListener(MouseListener l) {
        if (l instanceof BasicListUI.MouseInputHandler) {
            l = new CtrlDownMouseInputListener((MouseInputListener)l);
        }
        super.addMouseListener(l);
    }
    
    @Override
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
        int[] ii= getSelectedIndices();
        if ( ii.length==0 ) {
            return "";
        }
        StringBuilder result= new StringBuilder( ((ListOption)getItem(ii[0])).getValue() );
        for (int i = 1; i < ii.length; i++) {
            result.append( delimiter ). append( ((ListOption)getItem(ii[i])).getValue() );
        }
        return result.toString();
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
        @Override
        public void mousePressed(MouseEvent e) {
            listener.mousePressed(new CtrlDownMouseEvent(e));
        }
        @Override
        public void mouseReleased(MouseEvent e) {
            listener.mouseReleased(new CtrlDownMouseEvent(e));
        }
        @Override
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
                throw new IllegalStateException(dpe);
            }
            catch (java.lang.reflect.InvocationTargetException ite) {
                throw new IllegalStateException(ite);
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
