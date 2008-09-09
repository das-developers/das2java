/* File: FormContainer.java
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
import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.components.propertyeditor.Editable;
import edu.uiowa.physics.pw.das.graph.DasCanvas;
import edu.uiowa.physics.pw.das.graph.dnd.TransferableCanvas;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.List;
import org.das2.DasNameException;

/**
 * A subclass of JPanel to override the default Beans properties of
 * a JPanel
 *
 * @author  Edward West
 */
public abstract class FormContainer extends JPanel implements Editable, FormComponent {
    
    float horizontalComponentAlignment = JComponent.LEFT_ALIGNMENT;
    final float verticalComponentAlignment = JComponent.TOP_ALIGNMENT;
    
    boolean onHover = false;
    org.das2.util.DnDSupport dndSupport;
    List flavorList;
    int dropPosition;
    boolean editable;
    
    /**
     * The axis along which child components will be laid out.
     */
    private int axis = BoxLayout.X_AXIS;
    
    /**
     * The titled displayed along the panel border.
     */
    private String borderTitle = "";
    
    /**
     * If true, the panel has an etched border.
     */
    private boolean hasBorder = false;

    /**
     * Empty constructor for use with super classes.
     */
    protected FormContainer() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setBorder(new NoBorder());
    }
    
    /**
     * Returns <code>true</code> if this panel has a border
     */
    public boolean hasBorder() {
        return hasBorder;
    }
    
    /**
     * If the given boolean is <code>true</code> then the panel will be
     * given a border, if is does not already have one.  If the given
     * boolean is <code>false</code> then the panel's border will be
     * removed, if it has one.
     */
    public void setHasBorder(boolean b) {
        if (hasBorder != b) {
            hasBorder = b;
            if (hasBorder) {
                EtchedBorder etchedBorder = new EtchedBorder();
                TitledBorder titledBorder = new TitledBorder(etchedBorder, borderTitle, TitledBorder.LEFT, TitledBorder.TOP);
                setBorder(titledBorder);
            }
            else {
                setBorder(new NoBorder());
            }
        }
    }
    
    /**
     * Returns the title that is displayed along the top of this panels border
     * (if it has one).
     */
    public String getBorderTitle() {
        return borderTitle;
    }
    
    /**
     * Set the title that is displayed along the top of this panels border
     * (if it has one) to the given String.
     */
    public void setBorderTitle(String s) {
        if (!s.equals(borderTitle)) {
            borderTitle = s;
            if (hasBorder) {
                EtchedBorder etchedBorder = new EtchedBorder();
                TitledBorder titledBorder = new TitledBorder(etchedBorder, borderTitle, TitledBorder.LEFT, TitledBorder.TOP);
                setBorder(titledBorder);
            }
        }
    }
    
    /** Adds the specified component to this container at the specified
     * index. This method also notifies the layout manager to add
     * the component to this container's layout using the specified
     * constraints object via the <code>addLayoutComponent</code>
     * method.  The constraints are
     * defined by the particular layout manager being used.  For
     * example, the <code>BorderLayout</code> class defines five
     * constraints: <code>BorderLayout.NORTH</code>,
     * <code>BorderLayout.SOUTH</code>, <code>BorderLayout.EAST</code>,
     * <code>BorderLayout.WEST</code>, and <code>BorderLayout.CENTER</code>.
     *
     * <p>Note that if the component already exists
     * in this container or a child of this container,
     * it is removed from that container before
     * being added to this container.
     * <p>
     * This is the method to override if a program needs to track
     * every add request to a container as all other add methods defer
     * to this one. An overriding method should
     * usually include a call to the superclass's version of the method:
     * <p>
     * <blockquote>
     * <code>super.addImpl(comp, constraints, index)</code>
     * </blockquote>
     * <p>
     * @param     comp       the component to be added
     * @param     constraints an object expressing layout constraints
     *                 for this component
     * @param     index the position in the container's list at which to
     *                 insert the component, where <code>-1</code>
     *                 means append to the end
     * @exception IllegalArgumentException if <code>index</code> is invalid
     * @exception IllegalArgumentException if adding the container's parent
     * 			to itself
     * @exception IllegalArgumentException if adding a window to a container
     * @see       java.awt.Container#add(Component)
     * @see       java.awt.Container#add(Component, int)
     * @see       java.awt.Container#add(Component, java.lang.Object)
     * @see       java.awt.LayoutManager
     * @see       java.awt.LayoutManager2
     */
    protected void addImpl(Component comp, Object constraints, int index) {
        if (comp instanceof JComponent) {
            ((JComponent)comp).setAlignmentX(horizontalComponentAlignment);
            ((JComponent)comp).setAlignmentY(verticalComponentAlignment);
        }
        super.addImpl(comp, constraints, index);
        if (comp instanceof FormComponent) {
            FormComponent fc = (FormComponent)comp;
            org.das2.util.DnDSupport childDnDSupport = fc.getDnDSupport();
            if (childDnDSupport != null) {
                childDnDSupport.setParent(dndSupport);
            }
            fc.setEditingMode(getEditingMode());
        }
        packFormWindowAnscestor();
    }
    
    private void packFormWindowAnscestor() {
        if (isDisplayable()) {
            if (this instanceof FormWindow) {
                ((FormWindow)this).pack();
            }
            else {
                FormWindow fw = (FormWindow)SwingUtilities.getAncestorOfClass(FormWindow.class, this);
                if (fw != null) {
                    fw.pack();
                }
            }
        }
    }
    
    public void removeAll() {
        int ncomponents = getComponentCount();
        for (int index = ncomponents-1; index >= 0; index--) {
            remove(index);
        }
    }
    
    public void remove(int index) {
        super.remove(index);
        packFormWindowAnscestor();
    }
    
    public void remove(Component c) {
        super.remove(c);
        packFormWindowAnscestor();
    }
    
    public void setDirection(Orientation direction) {
        if (direction == Orientation.HORIZONTAL) {
            if (axis != BoxLayout.X_AXIS) {
                axis = BoxLayout.X_AXIS;
                setLayout(new BoxLayout(this, axis));
                getForm().validate();
            }
        }
        else if (axis != BoxLayout.Y_AXIS) {
            axis = BoxLayout.Y_AXIS;
            setLayout(new BoxLayout(this, axis));
            revalidate();
        }
    }
    
    public Orientation getDirection() {
        if (axis == BoxLayout.X_AXIS) return Orientation.HORIZONTAL;
        if (axis == BoxLayout.Y_AXIS) return Orientation.VERTICAL;
        throw new AssertionError("Invalid value for axis");
    }
    
    /**
     * Returns the FormBase object this component is associated with, or null
     */
    public FormBase getForm() {
        FormComponent parent = (FormComponent)getParent();
        if (parent == null) {
            return null;
        }
        return parent.getForm();
    }
    
    /**
     * Returns true if this component is in an editing state.
     * @return true if this component is in an editing state.
     */
    public boolean getEditingMode() {
        return editable;
    }
    
    public void paint(Graphics g) {
        super.paint(g);
        if (onHover) {
            Graphics2D g2 = (Graphics2D)g.create();
            Stroke thick = new BasicStroke(3.0f);
            g2.setStroke(thick);
            g2.setPaint(Color.GRAY);
            g2.drawRect(1, 1, getWidth() - 2, getHeight() - 2);
            g2.setPaint(Color.ORANGE);
            if (getDirection() == Orientation.HORIZONTAL) {
                g2.drawLine(dropPosition, 4, dropPosition, getHeight() - 4);
            }
            else {
                g2.drawLine(4, dropPosition, getHeight() - 4, dropPosition);
            }
            g2.dispose();
        }
    }
    
    public void setEditingMode(boolean b) {
        if (editable == b) return;
        editable = b;
        int componentCount = getComponentCount();
        for (int i = 0; i < componentCount; i++) {
            if (getComponent(i) instanceof FormComponent) {
                ((FormComponent)getComponent(i)).setEditingMode(b);
            }
        }
    }
    
    private int[] getInsertionPositions() {
        int componentCount = getComponentCount();
        int[] positions = new int[componentCount + 1];
        if (getDirection() == Orientation.HORIZONTAL) {
            positions[0] = 2;
            for (int i = 1; i <= componentCount; i++) {
                Component c = getComponent(i - 1);
                positions[i] = c.getX() + c.getWidth() + 1;
            }
        }
        else {
            positions[0] = 2;
            for (int i = 1; i <= componentCount; i++) {
                Component c = getComponent(i - 1);
                positions[i] = c.getY() + c.getHeight() + 1;
            }
        }
        return positions;
    }
    
    private int getInsertionPosition(int p) {
        int[] positions = getInsertionPositions();
        int insertionPosition = 0;
        int dp = Integer.MAX_VALUE;
        for (int i = 0; i < positions.length; i++) {
            int delta = Math.abs(p - positions[i]);
            if (delta < dp) {
                dp = delta;
                insertionPosition = positions[i];
            }
        }
        return insertionPosition;
    }
    
    private int getInsertionIndex(int p) {
        int[] positions = getInsertionPositions();
        int insertionIndex = 0;
        int dp = Integer.MAX_VALUE;
        for (int i = 0; i < positions.length; i++) {
            int delta = Math.abs(p - positions[i]);
            if (delta < dp) {
                dp = delta;
                insertionIndex = i;
            }
        }
        return insertionIndex;
    }
    
    public boolean startDrag(int x, int y, int action, java.awt.event.MouseEvent evt) {
        for (int i = 0; i < getComponentCount(); i++) {
            if (getComponent(i).getBounds().contains(x, y)) {
                dndSupport.startDrag(x, y, action, evt);
                return true;
            }
        }
        return false;
    }
    
    public Dimension getPreferredSize() {
        if (getComponentCount() == 0) {
            return new Dimension(100, 100);
        }
        else {
            return super.getPreferredSize();
        }
    }
    
    public Dimension getMinimumSize() {
        if (getComponentCount() == 0) {
            return new Dimension(100, 100);
        }
        else {
            return super.getMinimumSize();
        }
    }
    
    public Dimension getMaximumSize() {
        Dimension pref = getPreferredSize();
        Dimension max = super.getMaximumSize();
        max.width = Math.max(max.width, 100);
        max.height = pref.height;
        return max;
    }
    
    public String getDasName() {
        return null;
    }
    
    public void setDasName(String name) throws org.das2.DasNameException {
        throw new org.das2.DasNameException();
    }
    
    public void deregisterComponent() {
        for (int index = 0; index < getComponentCount(); index++) {
            Component c = getComponent(index);
            if (c instanceof FormComponent) {
                ((FormComponent)c).deregisterComponent();
            }
        }
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
        try {
            for (int index = 0; index < getComponentCount(); index++) {
                Component c = getComponent(index);
                if (c instanceof FormComponent) {
                    ((FormComponent)c).registerComponent();
                }
            }
        }
        catch (DasNameException dne) {
            deregisterComponent();
            throw dne;
        }
    }
    
    class NoBorder extends EmptyBorder {
        
        Color color = Color.GRAY;
        
        NoBorder() {
            super(5, 5, 5, 5);
        }
        
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            if (editable) {
                g.setColor(color);
                g.drawRect(x + 2, y + 2, width - 4, height - 4);
            }
        }
    }
    
    protected class ContainerDnDSupport extends org.das2.util.DnDSupport {
        
        ContainerDnDSupport(org.das2.util.DnDSupport parent) {
            super(FormContainer.this, java.awt.dnd.DnDConstants.ACTION_COPY_OR_MOVE, parent);
        }
        
        protected int canAccept(DataFlavor[] flavors, int x, int y, int action) {
            if (flavorList != null && getEditingMode()) {
                for (int i = 0; i < flavors.length; i++) {
                    if (flavorList.contains(flavors[i])) {
                        onHover = true;
                        if (getDirection() == Orientation.HORIZONTAL) {
                            dropPosition = getInsertionPosition(x);
                        }
                        else {
                            dropPosition = getInsertionPosition(y);
                        }
                        repaint();
                        return action;
                    }
                }
            }
            return -1;
        }
        
        protected void done() {
            onHover = false;
            repaint();
        }
        
        protected boolean importData(Transferable t, int x, int y, int action) {
            boolean success = false;
            try {
                int insertionIndex;
                if (getDirection() == Orientation.HORIZONTAL) {
                    insertionIndex = getInsertionIndex(x);
                }
                else {
                    insertionIndex = getInsertionIndex(y);
                }
                Component c = null;
                if (t.isDataFlavorSupported(TransferableFormComponent.COMPONENT_FLAVOR)) {
                    c = (Component)t.getTransferData(TransferableFormComponent.COMPONENT_FLAVOR);
                }
                else if (t.isDataFlavorSupported(TransferableCanvas.CANVAS_FLAVOR)) {
                    c = (Component)t.getTransferData(TransferableCanvas.CANVAS_FLAVOR);
                }
                else if (t.isDataFlavorSupported(TransferableFormComponent.DASML_FRAGMENT_FLAVOR)) {
                    c = getComponentFromDasMLFragment((String)t.getTransferData(TransferableFormComponent.DASML_FRAGMENT_FLAVOR));
                }
                if (c != null) {
                    if (!(c instanceof FormTab) && !(c instanceof FormWindow)) {
                        if (c != FormContainer.this && !SwingUtilities.isDescendingFrom(FormContainer.this, c)) {
                            if (c.getParent() == FormContainer.this) {
                                int cIndex = -1;
                                int componentCount = getComponentCount();
                                for (int i = 0; i < componentCount; i++) {
                                    if (getComponent(i) == c) {
                                        cIndex = i;
                                        break;
                                    }
                                }
                                if (insertionIndex > cIndex) {
                                    insertionIndex--;
                                }
                                remove(cIndex);
                                add(c, insertionIndex);
                                success = true;
                            }
                            else {
                                add(c, insertionIndex);
                                success = true;
                            }
                        }
                    }
                }
            }
            catch (UnsupportedFlavorException ufe) {
            }
            catch (IOException ioe) {
            }
            if (success) {
                revalidate();
            }
            return success;
        }
        
        private Component getComponentFromDasMLFragment(String dasML) {
            try {
                Document document = FormBase.parseDasML(new java.io.StringReader(dasML), null);
                Element element = document.getDocumentElement();
                String tag = element.getTagName();
                if (tag.equals("panel")) {
                    return new FormPanel(element, getForm());
                }
                else if (tag.equals("radiobutton")) {
                    return new FormRadioButton(element, getForm());
                }
                else if (tag.equals("textfield")) {
                    return new FormTextField(element, getForm());
                }
                else if (tag.equals("text")) {
                    return new FormText(element);
                }
                else if (tag.equals("button")) {
                    return new FormButton(element, getForm());
                }
                else if (tag.equals("checkbox")) {
                    return new FormCheckBox(element, getForm());
                }
                else if (tag.equals("buttongroup")) {
                    return new FormRadioButtonGroup(element, getForm());
                }
                else if (tag.equals("canvas")) {
                    return DasCanvas.processCanvasElement(element, getForm());
                }
                else if (tag.equals("choice")) {
                    return new FormChoice(element, getForm());
                }
            }
            catch (javax.xml.parsers.ParserConfigurationException pce) {
                throw new RuntimeException(pce);
            }
            catch (org.xml.sax.SAXException se) {
                throw new RuntimeException(se);
            }

            catch (org.das2.DasException de) {
                org.das2.util.DasExceptionHandler.handle(de);
            }            catch (org.das2.dasml.ParsedExpressionException pee) {
                org.das2.util.DasExceptionHandler.handle(pee);
            }
            catch (IOException ioe) {
                org.das2.util.DasExceptionHandler.handle(ioe);
            }
            catch ( java.text.ParseException ex ) {
                org.das2.util.DasExceptionHandler.handle(ex);
            }
            return null;
        }
        
        protected Transferable getTransferable(int x, int y, int action) {
            for (int i = 0; i < getComponentCount(); i++) {
                Component c = getComponent(i);
                if (c.getBounds().contains(x, y)) {
                    if (c instanceof DasCanvas) {
                        return new TransferableCanvas((DasCanvas)c);
                    }
                    else if (c instanceof FormPanel) {
                        return new TransferableFormComponent((FormPanel)c);
                    }
                    else if (c instanceof FormText) {
                        return new TransferableFormComponent((FormText)c);
                    }
                    else if (c instanceof FormTextField) {
                        return new TransferableFormComponent((FormTextField)c);
                    }
                    else if (c instanceof FormButton) {
                        return new TransferableFormComponent((FormButton)c);
                    }
                    else if (c instanceof FormCheckBox) {
                        return new TransferableFormComponent((FormCheckBox)c);
                    }
                    else if (c instanceof FormRadioButtonGroup) {
                        return new TransferableFormComponent((FormRadioButtonGroup)c);
                    }
                    else if (c instanceof FormRadioButton) {
                        return new TransferableFormComponent((FormRadioButton)c);
                    }
                    else if (c instanceof FormTab) {
                        return new TransferableFormComponent((FormTab)c);
                    }
                    else if (c instanceof FormChoice) {
                        return new TransferableFormComponent((FormChoice)c);
                    }
                    else if (c instanceof FormList) {
                        return new TransferableFormComponent((FormList)c);
                    }
                }
            }
            return null;
        }
        
        protected void exportDone(Transferable t, int action) {
        }
        
    }
}
