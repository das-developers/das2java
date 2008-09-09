/* File: FormWindow.java
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
import org.das2.DasPropertyException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.*;
import java.awt.*;
import org.das2.components.propertyeditor.Editable;
import org.das2.util.DasExceptionHandler;

/**
 * @author  eew
 */
public class FormWindow extends FormContainer implements Editable, FormComponent {
    
    FormBase form;
    
    JDialog dialog;
    
    JInternalFrame internalFrame;
    
    String title = "";
    
    int windowWidth = -1;
    
    int windowHeight = -1;
    
    boolean shouldBeVisible = false;
    
    private String dasName;
    
    {
        flavorList = java.util.Arrays.asList(new java.awt.datatransfer.DataFlavor[]{
            TransferableFormComponent.BUTTONGROUP_FLAVOR,
            TransferableFormComponent.BUTTON_FLAVOR,
            TransferableFormComponent.CHECKBOX_FLAVOR,
            TransferableFormComponent.CHOICE_FLAVOR,
            //TransferableFormComponent.LIST_FLAVOR,
            TransferableFormComponent.PANEL_FLAVOR,
            TransferableFormComponent.TEXTFIELD_FLAVOR,
            TransferableFormComponent.TEXT_FLAVOR,
            edu.uiowa.physics.pw.das.graph.dnd.TransferableCanvas.CANVAS_FLAVOR
        });
        setLayout(new BorderLayout());
    }
    
    public FormWindow(String name, String title) {
        this(name, title, -1, -1);
    }
    
    public FormWindow(String name, String title, int width, int height) {
        this.title = title;
        this.windowWidth = width;
        this.windowHeight = height;
        if (name == null) {
            name = "window_" + Integer.toHexString(System.identityHashCode(this));
        }
        try {
            setDasName(name);
        }
        catch (org.das2.DasNameException dne) {
            org.das2.util.DasExceptionHandler.handle(dne);
        }
        dndSupport = new ContainerDnDSupport(null);
    }
    
    /** Creates a new instance of FormWindow */
    FormWindow(Element element, FormBase form)
    throws DasException,
    ParsedExpressionException, org.xml.sax.SAXException {
        
        this.form = form;
        
        String name = element.getAttribute("name");
        String title = element.getAttribute("title");
        String alignment = element.getAttribute("alignment");
        int width = Integer.parseInt(element.getAttribute("width"));
        int height = Integer.parseInt(element.getAttribute("height"));
        java.awt.Point location = parsePoint(element.getAttribute("location"));
        boolean visible = element.getAttribute("visible").equals("true");
        
        NodeList children = element.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node node = children.item(index);
            if (node instanceof Element && node.getNodeName().equals("panel")) {
                try {
                    FormPanel panel = new FormPanel((Element)node, form);
                    add(panel);
                } catch ( java.text.ParseException ex ) {
                    DasExceptionHandler.handle(ex);
                }
            }
            else if (node instanceof Element && node.getNodeName().equals("canvas")) {
                try {
                    edu.uiowa.physics.pw.das.graph.DasCanvas canvas = edu.uiowa.physics.pw.das.graph.DasCanvas.processCanvasElement((Element)node, form);
                    add(canvas);
                } catch ( java.text.ParseException ex ) {
                    DasExceptionHandler.handle(ex);
                }
            }
        }
        
        setTitle(title);
        windowWidth = width;
        windowHeight = height;
        setWindowVisible(visible);
        try {
            setDasName(name);
        }
        catch (org.das2.DasNameException dne) {
            org.das2.util.DasExceptionHandler.handle(dne);
        }
        dndSupport = new ContainerDnDSupport(null);
    }
    
    private static java.awt.Point parsePoint(String str) {
        int commaIndex = str.indexOf(',');
        return new java.awt.Point(Integer.parseInt(str.substring(1, commaIndex)),
        Integer.parseInt(str.substring(commaIndex+1, str.length()-1)));
    }
    
    public FormBase getForm() {
        return form;
    }
    
    public Element getDOMElement(Document document) {
        Element element = document.createElement("window");
        element.setAttribute("name", getDasName());
        element.setAttribute("width", String.valueOf(getWidth()));
        element.setAttribute("height", String.valueOf(getHeight()));
        element.setAttribute("title", title);
        element.setAttribute("visible", String.valueOf(isVisible()));
        if (getComponentCount() > 0) {
            Component comp = getComponent(0);
            if (comp instanceof FormComponent) {
                FormComponent child = (FormComponent)comp;
                element.appendChild(child.getDOMElement(document));
            }
            else if (comp instanceof edu.uiowa.physics.pw.das.graph.DasCanvas) {
                edu.uiowa.physics.pw.das.graph.DasCanvas child = (edu.uiowa.physics.pw.das.graph.DasCanvas)comp;
                element.appendChild(child.getDOMElement(document));
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
    
    public boolean isWindowVisible() {
        if (getEditingMode()) {
            return shouldBeVisible;
        }
        else {
            return (dialog == null ? false : dialog.isVisible());
        }
    }
    
    public void setWindowVisible(boolean b) {
        boolean oldValue = isWindowVisible();
        if (oldValue == b) {
            return;
        }
        shouldBeVisible = b;
        if (!getEditingMode() && b) {
            if (dialog == null) {
                initDialog();
            }
            dialog.setVisible(b);
        }
        firePropertyChange("visible", oldValue, b);
    }
    
    public void setEditingMode(boolean b) {
        if (getEditingMode() == b) {
            return;
        }
        if (b) {
            if (dialog != null) {
                shouldBeVisible = dialog.isVisible();
                dialog.setVisible(false);
            }
            else {
                shouldBeVisible = false;
            }
            maybeInitializeInternalFrame();
            internalFrame.setContentPane(this);
        }
        else {
            if (dialog != null) {
                dialog.setContentPane(this);
                dialog.pack();
                dialog.setVisible(shouldBeVisible);
            }
            else if (shouldBeVisible) {
                initDialog();
                dialog.pack();
                dialog.setVisible(shouldBeVisible);
            }
        }
        super.setEditingMode(b);
    }
    
    public Dimension getPreferredSize() {
        if (windowWidth == -1 || windowHeight == -1) {
            return super.getPreferredSize();
        }
        else {
            return new Dimension(windowWidth, windowHeight);
        }
    }
    
    /** Getter for property title.
     * @return Value of property title.
     *
     */
    public String getTitle() {
        return title;
    }
    
    /** Setter for property title.
     * @param title New value of property title.
     *
     */
    public void setTitle(String title) {
        if (this.title == title || (this.title != null && this.title.equals(title))) {
            return;
        }
        String oldValue = this.title;
        this.title = title;
        if (getEditingMode() && internalFrame != null) {
            internalFrame.setTitle(title);
        }
        firePropertyChange("title", oldValue, title);
    }
    
    public void pack() {
        if (getEditingMode()) {
            maybeInitializeInternalFrame();
            internalFrame.pack();
        }
        else {
            if (dialog == null) {
                initDialog();
            }
            dialog.pack();
        }
    }
    
    public Dimension getWindowSize() {
        return new Dimension(windowWidth, windowHeight);
    }
    
    public void setWindowSize(int width, int height) {
        int oldWidth = windowWidth;
        int oldHeight = windowHeight;
        if (width != windowWidth) {
            windowWidth = width;
            firePropertyChange("width", oldWidth, width);
        }
        if (height != windowHeight) {
            windowHeight = height;
            firePropertyChange("height", oldHeight, height);
        }
        if (height != windowHeight || width != windowWidth) {
            pack();
        }
    }
    
    public void setWindowWidth(int width) {
        setWindowSize(width, windowHeight);
    }
    
    public int getWindowWidth() {
        return windowWidth;
    }
    
    public void setWindowHeight(int height) {
        setWindowSize(windowWidth, height);
    }
    
    public int getWindowHeight() {
        return windowHeight;
    }
    
    private void initDialog() {
        Window w = SwingUtilities.getWindowAncestor(form);
        if (w instanceof Frame) {
            dialog = new JDialog((Frame)w);
        }
        else if (w instanceof Dialog) {
            dialog = new JDialog((Dialog)w);
        }
        else {
            dialog = new JDialog();
        }
        dialog.setContentPane(this);
        dialog.setTitle(title);
    }
    
    private void maybeInitializeInternalFrame() {
        if (internalFrame == null) {
            internalFrame = new InternalFrame();
            internalFrame.setTitle(title);
            internalFrame.setVisible(true);
            internalFrame.setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);
            if (getEditingMode()) {
                internalFrame.setContentPane(this);
            }
            internalFrame.pack();
        }
    }
    
    public org.das2.util.DnDSupport getDnDSupport() {
        if (dndSupport == null) {
            dndSupport = new ContainerDnDSupport(null);
        }
        return dndSupport;
    }
    
    protected void addImpl(Component c, Object constraints, int index) {
        if (getComponentCount() >= 1) throw new IllegalArgumentException("Only one component allowed");
        super.addImpl(c, constraints, index);
        if (c instanceof JComponent) {
            ((JComponent)c).setAlignmentY(JComponent.TOP_ALIGNMENT);
        }
    }
    
    JInternalFrame getInternalFrame() {
        if (getEditingMode()) {
            maybeInitializeInternalFrame();
            return internalFrame;
        }
        return null;
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
    
    public DasApplication getDasApplication() {
        if (form != null) {
            return form.getDasApplication();
        }
        else {
            return null;
        }
    }
    
    public void registerComponent() throws org.das2.DasException {
        DasApplication app = getDasApplication();
        if (app != null) {
            NameContext nc = app.getNameContext();
            nc.put(getDasName(), this);
        }
        super.registerComponent();
    }
    
    public class InternalFrame extends JInternalFrame {
        InternalFrame() {
            super(null, true, false, false, false);
        }
        public FormWindow getWindow() {
            return FormWindow.this;
        }
    }
    
}
