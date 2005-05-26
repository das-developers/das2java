/* File: DasMouseInputAdapter.java
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

package edu.uiowa.physics.pw.das.event;

import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.components.propertyeditor.Editable;
import edu.uiowa.physics.pw.das.components.propertyeditor.PropertyEditor;
import edu.uiowa.physics.pw.das.graph.*;
import edu.uiowa.physics.pw.das.graph.DasAxis;
import edu.uiowa.physics.pw.das.graph.DasCanvas;
import edu.uiowa.physics.pw.das.graph.DasCanvasComponent;
import java.awt.*;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.event.*;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.*;


/**
 *
 * @author  jbf
 */
public class DasMouseInputAdapter extends MouseInputAdapter implements Editable {
    
    private MouseModule primary=null;
    private MouseModule secondary=null;
    private MouseModule tertiary=null;
    
    private Vector active=null; // array of active modules    
    
    private Vector modules;
    
    private HashMap primaryActionButtonMap;
    private HashMap secondaryActionButtonMap;
    
    protected JPopupMenu primaryPopup;
    protected JPopupMenu secondaryPopup;
    private Point primaryPopupLocation;
    private Point secondaryPopupLocation;
    
    private JPanel pngFileNamePanel;
    private JTextField pngFileTextField;
    private JFileChooser pngFileChooser;
    
    JCheckBoxMenuItem primarySelectedItem;
    JCheckBoxMenuItem secondarySelectedItem;
    
    Rectangle[] dirtyBoundses;
    
    int numInserted;
    
    protected ActionListener popupListener;
    
    protected DasCanvasComponent parent=null;
    
    private Point selectionStart;   // in component frame
    private Point selectionEnd;     // in component frame
    private Point dSelectionStart;  // in DasCanvas device frame
    private Point dSelectionEnd; // in DasCanvas device frame
    private Graphics2D graphics;
    
    private MousePointSelectionEvent mousePointSelection;
    private int xOffset;
    private int yOffset;
    
    private int button=0; // current depressed button
    
    private MouseMode mouseMode= MouseMode.idle;
    
    private DragRenderer resizeRenderer= null;
    private Point resizeStart= null;
    
    Vector hotSpots = null;
    Rectangle dirtyBounds= null;
    
    private static class MouseMode {
        String s;
        boolean resizeTop= false;
        boolean resizeBottom= false;
        boolean resizeRight= false;
        boolean resizeLeft= false;
        static MouseMode idle= new MouseMode("idle");
        static MouseMode resize= new MouseMode("resize");
        static MouseMode moduleDrag= new MouseMode("moduleDrag");
        static MouseMode hotSpot = new MouseMode("hotSpot");
        
        MouseMode(String s) { this.s= s; }
        public String toString() { return s; }
    }
    
    /** Creates a new instance of dasMouseInputAdapter */
    public DasMouseInputAdapter(DasCanvasComponent parent) {
        
        this.parent= parent;
        
        modules= new Vector();
        
        primaryActionButtonMap= new HashMap();
        secondaryActionButtonMap= new HashMap();
        
        if ( ! DasApplication.getDefaultApplication().isHeadless() ) {
            primaryPopup= createPopup();
            secondaryPopup= createPopup();
        }
        
        active= null;
        
        mousePointSelection= new MousePointSelectionEvent(this,0,0);
        
        resizeRenderer= new BoxRenderer(parent);
        
        numInserted= 0; // number of additional inserted items
    }
    
    public void replaceMouseModule( MouseModule oldModule, MouseModule newModule ) {
        JCheckBoxMenuItem j= (JCheckBoxMenuItem)primaryActionButtonMap.get(oldModule);
        primaryActionButtonMap.put(newModule,j);
        primaryActionButtonMap.remove(oldModule);
        secondaryActionButtonMap.put(newModule,secondaryActionButtonMap.get(oldModule));
        secondaryActionButtonMap.remove(oldModule);
        modules.removeElement(oldModule);
        modules.addElement(newModule);
    }
    
    public void addMouseModule(MouseModule module) {
        
        if ( DasApplication.getDefaultApplication().isHeadless() ) {
            DasApplication.getDefaultApplication().getLogger().fine("not adding module since headless is true");
        } else {
            MouseModule preExisting= getModuleByLabel(module.getLabel());
            if (preExisting!=null) {
                edu.uiowa.physics.pw.das.util.DasDie.println(edu.uiowa.physics.pw.das.util.DasDie.INFORM,"Replacing mouse module "+module.getLabel()+".");
                replaceMouseModule(preExisting,module);
                
            } else {
                
                modules.add(module);
                
                String name= module.getLabel();
                
                //        popup.setVisible(false);
                
                JCheckBoxMenuItem primaryNewItem = new JCheckBoxMenuItem(name);
                JCheckBoxMenuItem secondaryNewItem = new JCheckBoxMenuItem(name);
                
                primaryNewItem.addActionListener(popupListener);
                primaryNewItem.setActionCommand("primary");
                secondaryNewItem.addActionListener(popupListener);
                secondaryNewItem.setActionCommand("secondary");
                
                primaryActionButtonMap.put(module,primaryNewItem);
                secondaryActionButtonMap.put(module,secondaryNewItem);
                
                primaryPopup.add( primaryNewItem, primaryActionButtonMap.size()-1 );
                secondaryPopup.add( secondaryNewItem, secondaryActionButtonMap.size()-1 );
                
            }
        }
    }
    
    public KeyAdapter getKeyAdapter() {
        return new KeyAdapter() {
            public void keyPressed( KeyEvent ev ) {
                if ( ev.getKeyCode()==27 & active!=null ) {
                    active=null;
                    refresh();
                }
            }
        };
    }
    
    public void setPrimaryModule(MouseModule module) {
        
        for ( Iterator i= primaryActionButtonMap.entrySet().iterator(); i.hasNext(); ) {
            try {
                Object ii= ((Map.Entry)i.next()).getValue();
                ((JCheckBoxMenuItem)ii).setSelected(false);
            } catch ( RuntimeException e ) {
                e.printStackTrace();
                throw e;
            }
        }
        JCheckBoxMenuItem j= (JCheckBoxMenuItem)primaryActionButtonMap.get(module);
        if (j!=null) {
            j.setSelected(true);
        }
        primarySelectedItem= j;
        primary= module;
        parent.setCursor(primary.getCursor());
    }
    
    public void setSecondaryModule(MouseModule module) {
        for ( Iterator i= secondaryActionButtonMap.entrySet().iterator(); i.hasNext(); ) {
            try {
                Object ii= ((Map.Entry)i.next()).getValue();
                ((JCheckBoxMenuItem)ii).setSelected(false);
            } catch ( RuntimeException e ) {
                e.printStackTrace();
                throw e;
            }
        }
        JCheckBoxMenuItem j= (JCheckBoxMenuItem)secondaryActionButtonMap.get(module);
        if (j!=null) {
            j.setSelected(true);
        }
        secondarySelectedItem= j;
        secondary= module;
    }
    
    public void setTertiaryModule(MouseModule module) {
        tertiary= module;
    }
    
    
    private JPopupMenu createPopup() {
        JPopupMenu popup = new JPopupMenu();
        popupListener = createPopupMenuListener();
        
        Action[] componentActions = parent.getActions();
        for (int iaction = 0; iaction < componentActions.length; iaction++) {
            JMenuItem item = new JMenuItem();
            item.setAction(componentActions[iaction]);
            popup.add(item);
        }
        
        popup.addSeparator();
        
        Action[] canvasActions = DasCanvas.getActions();
        for (int iaction = 0; iaction < canvasActions.length; iaction++) {
            JMenuItem item = new JMenuItem();
            item.setAction(canvasActions[iaction]);
            popup.add(item);
        }
        
        popup.addSeparator();
        
        return popup;
    }
    
    private ActionListener createPopupMenuListener() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                DasMouseInputAdapter outer= DasMouseInputAdapter.this; // useful for debugging
                String command = e.getActionCommand();
                if (command.equals("properties")) {
                    parent.showProperties();
                } else if (command.equals("print")) {
                    Printable p = ((DasCanvas)parent.getParent()).getPrintable();
                    PrinterJob pj = PrinterJob.getPrinterJob();
                    pj.setPrintable(p);
                    if (pj.printDialog()) {
                        try {
                            pj.print();
                        } catch (PrinterException pe) {
                            Object[] message = {"Error printing", pe.getMessage() };
                            JOptionPane.showMessageDialog(null, message, "ERROR", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                } else if (command.equals("toPng")) {
                    if (pngFileNamePanel == null) {
                        pngFileNamePanel = new JPanel();
                        pngFileNamePanel.setLayout(new BoxLayout(pngFileNamePanel, BoxLayout.X_AXIS));
                        pngFileTextField = new JTextField(32);
                        pngFileTextField.setMaximumSize(pngFileTextField.getPreferredSize());
                        pngFileChooser = new JFileChooser();
                        pngFileChooser.setApproveButtonText("Select File");
                        pngFileChooser.setDialogTitle("Write to PNG");
                        JButton b = new JButton("Browse");
                        b.setActionCommand("pngBrowse");
                        b.addActionListener(this);
                        pngFileNamePanel.add(pngFileTextField);
                        pngFileNamePanel.add(b);
                    }
                    pngFileTextField.setText(pngFileChooser.getCurrentDirectory().getPath());
                    String[] options = {"Write to PNG", "Cancel"};
                    int choice = JOptionPane.showOptionDialog(parent,
                            pngFileNamePanel,
                            "Write to PNG",
                            0,
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            options,
                            "Ok");
                    if (choice == 0) {
                        DasCanvas canvas = (DasCanvas)parent.getParent();
                        try {
                            canvas.writeToPng(pngFileTextField.getText());
                        } catch (java.io.IOException ioe) {
                            edu.uiowa.physics.pw.das.util.DasExceptionHandler.handle(ioe);
                        }
                    }
                } else if (command.equals("pngBrowse")) {
                    int choice = pngFileChooser.showDialog(parent, "Select File");
                    if (choice == JFileChooser.APPROVE_OPTION) {
                        pngFileTextField.setText(pngFileChooser.getSelectedFile().getPath());
                    }
                } else if (command.equals("close")) {
                } else if (command.equals("primary")) {
                    if (primarySelectedItem!=null) primarySelectedItem.setSelected(false);
                    for (int i=0; i<modules.size(); i++) {
                        JCheckBoxMenuItem j= (JCheckBoxMenuItem)primaryActionButtonMap.get(modules.get(i));
                        if (j.isSelected()) {
                            primarySelectedItem=j;
                            break;
                        }
                    }
                    primarySelectedItem.setSelected(true); // for case when selection wasn't changed.
                    //primaryPopup.show( parent, l.x, l.y );
                } else if (command.equals("secondary")) {
                    if (secondarySelectedItem!=null) secondarySelectedItem.setSelected(false);
                    Point l= secondaryPopupLocation;
                    for (int i=0; i<modules.size(); i++) {
                        JCheckBoxMenuItem j= (JCheckBoxMenuItem)secondaryActionButtonMap.get(modules.get(i));
                        if (j.isSelected()) {
                            secondarySelectedItem=j;
                            break;
                        }
                    }
                    //secondaryPopup.show( parent, l.x, l.y );
                } else {
                    edu.uiowa.physics.pw.das.util.DasDie.println(""+command);
                }
            }
        };
    }
    
    private void renderSelection(Graphics2D g) {
        if ( active==null ) {
            return;
        }
        for (int i=0; i<active.size(); i++) {
            DasCanvas canvas = parent.getCanvas();
            selectionStart = SwingUtilities.convertPoint(canvas, dSelectionStart, parent);
            selectionEnd = SwingUtilities.convertPoint(canvas, dSelectionEnd, parent);
            DragRenderer dr= ((MouseModule)active.get(i)).getDragRenderer();
            Rectangle[] dd= dr.renderDrag(g,selectionStart,selectionEnd);
            dirtyBoundses= new Rectangle[dd.length];
            for ( i=0; i<dd.length; i++ ) {
                dirtyBoundses[i]= new Rectangle( dd[i] );
            }
        }
    }
    
    /* This attempts to redraw just the affected portions of parent.  Presently it
     * needs to call the parent's paintImmediately twice, because we don't know what
     * the dragRenderer's dirty bounds will be.
     */
    private synchronized void refresh() {
        if ( dirtyBoundses!=null ) {                                
            Rectangle[] dd= new Rectangle[dirtyBoundses.length];
            for ( int i=0; i<dd.length; i++ ) {                
                dd[i]= new Rectangle( dirtyBoundses[i] );
            }
            for ( int i=0; i<dd.length; i++ ) {                                
                parent.paintImmediately( dd[i] );
            }
            for ( int i=0; i<dirtyBoundses.length; i++ ) {                                
                parent.paintImmediately( dirtyBoundses[i] );                
            }            
        } else {        
            if ( active!=null ) {
                parent.paintImmediately( 0, 0, parent.getWidth(), parent.getHeight() );
            }
        }
        if ( active==null ) {
            dirtyBoundses=null;
        }
    }
    
    /*
     * Paint the drag renderer on top of parent.
     */
    public void paint( Graphics g ) {
        renderSelection( (Graphics2D)g );
    }
    
    private MouseMode activateMouseMode(MouseEvent e) {
        
        boolean xLeftSide = false;
        boolean xRightSide = false;
        boolean yTopSide = false;
        boolean yBottomSide = false;
        
        if (parent.getRow() != DasRow.NULL && parent.getColumn() != DasColumn.NULL ) {
            int xLeft= parent.getColumn().getDMinimum()-xOffset;
            int xRight= parent.getColumn().getDMaximum()-xOffset;
            int yTop= parent.getRow().getDMinimum()-yOffset;
            int yBottom= parent.getRow().getDMaximum()-yOffset;
            
            xLeftSide= e.getX()<xLeft+10;
            xRightSide= e.getX()>xRight-10;
            yTopSide= (e.getY()<yTop+10) && (e.getY()>=yTop);
            yBottomSide= e.getY()>(yBottom-10);
        }
        
        MouseMode result= MouseMode.idle;
        Cursor cursor= new Cursor(Cursor.DEFAULT_CURSOR);
        
        if ( !(parent instanceof DasAxis) ) {
            if ( xLeftSide ) {
                if ( yTopSide ) {
                    result= MouseMode.resize;
                    cursor= new Cursor(Cursor.NW_RESIZE_CURSOR);
                } else if ( yBottomSide ) {
                    result= MouseMode.resize;
                    cursor= new Cursor(Cursor.SW_RESIZE_CURSOR);
                }
            } else if ( xRightSide ) {
                if ( yTopSide ) {
                    result= MouseMode.resize;
                    cursor= new Cursor(Cursor.NE_RESIZE_CURSOR);
                } else if  ( yBottomSide ) {
                    result= MouseMode.resize;
                    cursor= new Cursor(Cursor.SE_RESIZE_CURSOR);
                }
            }
        }
        
        Shape hotSpotShape=null;
        if ( hotSpots!=null ) {
            Vector v= hotSpots;
            for (int i=0; i<v.size(); i++) {
                Shape s= (Shape) v.get(i);
                if (s.contains(e.getX(),e.getY())){
                    cursor= new Cursor(Cursor.HAND_CURSOR);
                    result= MouseMode.hotSpot;
                    hotSpotShape= s;
                };
            }
        }
        
        if (result==MouseMode.resize) {
            result.resizeBottom= yBottomSide;
            result.resizeTop= yTopSide;
            result.resizeRight= xRightSide;
            result.resizeLeft= xLeftSide;
        }
        if (result!=mouseMode) {
            getGlassPane().setCursor(cursor);
            if (mouseMode==MouseMode.hotSpot && result!=MouseMode.hotSpot) {
                parent.repaint(dirtyBounds);
                dirtyBounds= null;
            } else if ( result==MouseMode.hotSpot && mouseMode!=MouseMode.hotSpot) {
                Graphics2D g= (Graphics2D)parent.getGraphics();
                g.setColor( new Color(0,200,255,50) );
                g.fill(hotSpotShape);
                dirtyBounds= hotSpotShape.getBounds();
            }
        }
        return result;
    }
    
    public void mouseMoved(MouseEvent e) {
        Point l= parent.getLocation();
        xOffset= l.x;
        yOffset= l.y;
        
        MouseMode m;
        if ((m=activateMouseMode(e))!=null) {
            mouseMode= m;
        } else {
            mouseMode= MouseMode.idle;
        }
    }
    
    private void showPopup( JPopupMenu menu, Point p ) {
        HashMap map=null;
        if ( menu==primaryPopup ) {
            map= primaryActionButtonMap;
        } else if ( menu==secondaryPopup ) {
            map= secondaryActionButtonMap;
        } else {
            throw new IllegalArgumentException( "menu must be primary or secondary popup menu" );
        }
        for ( Iterator i= modules.iterator(); i.hasNext(); ) {
            MouseModule mm= (MouseModule)i.next();
            JCheckBoxMenuItem j= (JCheckBoxMenuItem)primaryActionButtonMap.get(mm);
            j.setText(mm.getLabel());
        }
        menu.show( parent, p.x, p.y);
    }
    
    public void mousePressed(MouseEvent e) {
        Point l= parent.getLocation();
        parent.requestFocus();
        xOffset= l.x;
        yOffset= l.y;
        if (mouseMode==MouseMode.resize) {
            resizeStart= new Point(0,0);
            graphics= (Graphics2D) getGlassPane().getGraphics();
            graphics.translate(parent.getX(),parent.getY());
            if (mouseMode.resizeRight) {
                resizeStart.x=  0;
            } else if (mouseMode.resizeLeft) {
                resizeStart.x=  parent.getWidth();
            }
            if (mouseMode.resizeTop) {
                resizeStart.y=  parent.getHeight();
            } else if (mouseMode.resizeBottom) {
                resizeStart.y=  0;
            }
        } else if ( mouseMode==MouseMode.hotSpot ) {
            Vector v= hotSpots;
            for (int i=0; i<v.size(); i++) {
                if (((Shape) v.get(i)).contains(e.getX(),e.getY())) {
                    primary.hotSpotPressed((Shape)v.get(i));
                }
            }
        } else {
            if (active==null) {
                button = e.getButton();
                selectionStart= e.getPoint();
                dSelectionStart= SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), parent.getCanvas());
                selectionEnd= e.getPoint();
                dSelectionEnd= SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), parent.getCanvas());
                graphics= (Graphics2D) parent.getGraphics();
                
                if ( e.isControlDown() || button==MouseEvent.BUTTON3 ) {
                    if (button==MouseEvent.BUTTON1 || button==MouseEvent.BUTTON3 ) {
                        showPopup( primaryPopup, e.getPoint() );
                    } else {
                        showPopup( secondaryPopup, e.getPoint() );
                    }
                } else {
                    
                    active= new Vector();
                    
                    if ( button==MouseEvent.BUTTON1 || button==MouseEvent.BUTTON3 ) {
                        for (int i=0; i< modules.size(); i++) {
                            JCheckBoxMenuItem j= (JCheckBoxMenuItem)primaryActionButtonMap.get(modules.get(i));
                            if (j.isSelected()) active.add(modules.get(i));
                        }
                    } else {
                        for (int i=0; i< modules.size(); i++) {
                            JCheckBoxMenuItem j= (JCheckBoxMenuItem)secondaryActionButtonMap.get(modules.get(i));
                            if (j.isSelected()) active.add(modules.get(i));
                        }
                    }                                        
                    
                    mouseMode= MouseMode.moduleDrag;
                    
                    mousePointSelection.set(e.getX()+xOffset,e.getY()+yOffset);
                    for (int i=0; i<active.size(); i++) {
                        MouseModule j= (MouseModule)active.get(i);
                        j.mousePressed(e);
                        if (j.dragRenderer.isPointSelection()) {
                            mouseDragged(e);
                        }
                    }
                }
            }
        }
    }
    
    public void mouseDragged(MouseEvent e) {
        if (mouseMode==MouseMode.resize) {
            resizeRenderer.clear(graphics);
            resizeRenderer.renderDrag(graphics,resizeStart,e.getPoint());
        } else {
            if (active!=null) {
                //clearSelection(graphics);
                selectionEnd= e.getPoint();
                dSelectionEnd= SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), parent.getCanvas());
                
                mousePointSelection.set((int)dSelectionEnd.getX(),(int)dSelectionEnd.getY());
                for (int i=0; i<active.size(); i++) {
                    MouseModule j= (MouseModule)active.get(i);
                    if (j.dragRenderer.isPointSelection()) {
                        j.mousePointSelected(mousePointSelection);
                    }
                    if (j.dragRenderer.isUpdatingDragSelection()) {
                        // Really it should be the DMM that indicates it wants updates...whoops...
                        MouseDragEvent de= j.dragRenderer.getMouseDragEvent(parent,dSelectionStart,dSelectionEnd,e.isShiftDown());
                        j.mouseRangeSelected(de);
                    }                    
                    j.mouseDragged(e);
                }
                if ( dSelectionStart.distance(dSelectionEnd)>30 && dirtyBoundses.length>0 ) {
                    System.out.println(dirtyBoundses[0]);
                }
                refresh();
            }
        }
    }
    
    private void performResize(MouseEvent e) {
        DasCanvas canvas= (DasCanvas)parent.getParent();
        int dxLeft= parent.getColumn().getDMinimum();
        int dxRight= parent.getColumn().getDMaximum();
        int dyTop= parent.getRow().getDMinimum();
        int dyBottom= parent.getRow().getDMaximum();
        
        int dx= e.getX()+xOffset;
        int dy= e.getY()+yOffset;
        if (mouseMode.resizeRight) {
            dxRight= dx;
        } else if (mouseMode.resizeLeft) {
            dxLeft= dx;
        }
        if (mouseMode.resizeTop) {
            dyTop= dy;
        } else if (mouseMode.resizeBottom) {
            dyBottom= dy;
        }
        
        parent.getColumn().setDPosition(dxLeft,dxRight);
        parent.getRow().setDPosition(dyTop,dyBottom);
        
        parent.resize();
        getGlassPane().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }
    
    public void mouseReleased(MouseEvent e) {
        if (mouseMode==MouseMode.resize) {
            performResize(e);
        } else {
            if (e.getButton()==button) {
                if (active!=null) {
                    //clearSelection(graphics);
                    int x= e.getX();
                    int y= e.getY();
                    for (int i=0; i<active.size(); i++) {
                        MouseModule j= (MouseModule)active.get(i);
                        try {
                            MouseDragEvent de=
                                    j.dragRenderer.getMouseDragEvent(parent,dSelectionStart,dSelectionEnd,e.isShiftDown());
                            j.mouseRangeSelected(de);
                        } finally {
                            button=0;
                            j.mouseReleased(e);
                        }
                    }
                    active= null;
                    refresh();
                }
            }
        }
    }
    
    public void removeMouseModule(MouseModule module) {
        // not implemented yet
    }
    
    /**
     * Getter for property mouseModules.
     * @return Value of property mouseModules.
     */
    public MouseModule getMouseModule(int i) {
        return (MouseModule)modules.get(i);
    }
    
    public MouseModule[] getMouseModules() {
        MouseModule[] result= new MouseModule[modules.size()];
        modules.copyInto(result);
        return result;
    }
    
    /**
     * Setter for property mouseModules.
     * @param mouseModules New value of property mouseModules.
     */
    public void setMouseModule( int i, MouseModule mouseModule ) {
        this.modules.set(i,mouseModule);
    }
    
    public void mouseEntered(MouseEvent e) {
        if (primary!=null) {
            hotSpots= primary.getHotSpots();
            getGlassPane().setCursor(primary.getCursor());
        }
    }
    
    public void mouseExited(MouseEvent e) {
        if (mouseMode==MouseMode.hotSpot) {
            parent.repaint(dirtyBounds);
            mouseMode= MouseMode.idle;
        }
        getGlassPane().setCursor(Cursor.getDefaultCursor());
    }
    
    public void addMenuItem(Component b) {
        if (numInserted==0) {
            primaryPopup.insert(new JPopupMenu.Separator(),0);
        }
        primaryPopup.insert(b,numInserted);
        numInserted++;
    }
    
    public Component getGlassPane() {
        return ((DasCanvas)parent.getParent()).getGlassPane();
    }
    
    public MouseModule getModuleByLabel(java.lang.String label) {
        MouseModule result=null;
        for (int i=0; i<modules.size(); i++) {
            if (label.equals(((MouseModule)modules.get(i)).getLabel())) {
                result= (MouseModule)modules.get(i);
            }
        }
        return result;
    }
    
}
