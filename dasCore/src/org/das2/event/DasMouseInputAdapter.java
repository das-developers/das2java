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
package org.das2.event;

import java.util.logging.Level;
import org.das2.graph.DasColumn;
import org.das2.graph.DasRow;
import org.das2.system.DasLogger;
import org.das2.DasApplication;
import org.das2.graph.DasAxis;
import org.das2.graph.DasCanvas;
import org.das2.graph.DasCanvasComponent;
import org.das2.util.DasExceptionHandler;
import java.awt.*;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.event.*;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;
import org.das2.components.propertyeditor.Editable;
import org.das2.graph.DasColorBar;
import org.das2.util.LoggerManager;

/**
 * DasMouseInputAdapter delegates mouse and key events to mouse modules, which
 * do something with the events.  Also, mouse events are promoted to MouseDragEvents
 * which conveniently store information about the entire drag gesture.
 *
 * The base class of MouseModule has do-nothing stubs for KeyListener, MouseListener,
 * MouseMotionListener, and MouseWheelListener, which can be implemented if the
 * module wants to do something with these events.  Also MouseDragEvents will be
 * sent to the module as its DragRenderer has requested: after the mouse release,
 * during the drag, or when keys are pressed.
 *
 * The module will first receive the low-level events before receiving the MouseDragEvents.
 *
 * @author  jbf
 */
public class DasMouseInputAdapter extends MouseInputAdapter implements Editable, MouseWheelListener {

    private MouseModule primary = null;
    private MouseModule secondary = null;
    /*
     * array of active modules.  This will be removed, as the idea was
     * that a few modules could be used together simultaneously, but this implementation
     * only allows for one to be active at a time.
     */
    private ArrayList active = null;
    private boolean pinned = false;
    private final ArrayList modules;
    private final HashMap primaryActionButtonMap;
    private final HashMap secondaryActionButtonMap;
    protected JPopupMenu primaryPopup;
    protected JPopupMenu secondaryPopup;
    private JPanel pngFileNamePanel;
    private JTextField pngFileTextField;
    private JFileChooser pngFileChooser;
    JRadioButtonMenuItem primarySelectedItem;
    JRadioButtonMenuItem secondarySelectedItem;    // must be non-null, but may contain null elements
    Rectangle[] dirtyBoundsList;
    
    private static final Logger logger = LoggerManager.getLogger( "das2.gui.dmia" );
    
    /**
     * number of additional inserted popup menu items to the primary menu.
     */
    int numInserted;

    /**
     * number of additional inserted popup menu items to the secondary menu.
     * Components can be added to the primary menu, but not the secondary.
     */
    int numInsertedSecondary;

    protected ActionListener popupListener;
    protected DasCanvasComponent parent = null;
    private Point dSelectionStart;  // in DasCanvas device frame
    private Point dSelectionEnd; // in DasCanvas device frame
    private final MousePointSelectionEvent mousePointSelection;
    private int xOffset; // parent to canvas offset
    private int yOffset; // parent to canvas offset
    private int button = 0; // current depressed button
    private MouseMode mouseMode = MouseMode.idle;
    private boolean drawControlPoints = false;
    private DragRenderer resizeRenderer = null;
    private Point resizeStart = null;

    Rectangle dirtyBounds = null;
    private boolean hasFocus = false;
    private Point pressPosition;  // in the component frame
    private final boolean headless;

    public void setMenuLabel(String id) {
        primaryPopup.setLabel(id);
        secondaryPopup.setLabel(id);
    }

    /**
     * set the name of the menus to help with debugging
     * @param name 
     */
    public void resetName(String name) {
        if ( !this.headless ) {
            primaryPopup.setName("dmia_pop1_"+name);
            secondaryPopup.setName("dmia_pop2_"+name);
        }
    }

    private static final class MouseMode {

        String s;
        boolean resizeTop = false;
        boolean resizeBottom = false;
        boolean resizeRight = false;
        boolean resizeLeft = false;
        Point moveStart = null; // in the DasCanvas frame
        static final MouseMode idle = new MouseMode("idle");
        static final MouseMode resize = new MouseMode("resize");
        static final MouseMode move = new MouseMode("move");
        static final MouseMode moduleDrag = new MouseMode("moduleDrag");

        MouseMode(String s) {
            this.s = s;
        }
        @Override
        public String toString() {
            return s;
        }
    }

    private Feedback feedback;
    
    public static interface Feedback {
        public void setMessage( String message );
    }
    
    /** 
     * Create a DasMouseInputAdapter to handle mouse events for the component.
     * @param parent the component.
     */
    public DasMouseInputAdapter(DasCanvasComponent parent) {

        this.parent = parent;

        modules = new ArrayList();

        primaryActionButtonMap = new HashMap();
        secondaryActionButtonMap = new HashMap();

        this.headless = DasApplication.getDefaultApplication().isHeadless();
        if (!headless) {
            primaryPopup= new JPopupMenu();
            primaryPopup.setName("dmia_pop1_"+parent.getDasName());
            numInserted = createPopup(primaryPopup);
            secondaryPopup= new JPopupMenu();
            secondaryPopup.setName("dmia_pop2_"+parent.getDasName()); 
            numInsertedSecondary = createPopup(secondaryPopup);
            
        }

        active = null;

        mousePointSelection = new MousePointSelectionEvent(this, 0, 0);

        resizeRenderer = new BoxRenderer(parent,false);

        dirtyBoundsList = new Rectangle[0];
        this.feedback= new Feedback() {
            @Override
            public void setMessage(String message) {
                // do nothing by default.
            }
        };
    }

    public void setFeedback( Feedback f ) {
        this.feedback= f;
    }
    
    public void replaceMouseModule(MouseModule oldModule, MouseModule newModule) {
        JRadioButtonMenuItem j = (JRadioButtonMenuItem) primaryActionButtonMap.get(oldModule);
        primaryActionButtonMap.put(newModule, j);
        primaryActionButtonMap.remove(oldModule);
        secondaryActionButtonMap.put(newModule, secondaryActionButtonMap.get(oldModule));
        secondaryActionButtonMap.remove(oldModule);
        modules.remove(oldModule);
        modules.add(newModule);
    }
    
    public synchronized void removeMouseModule(MouseModule module) {
        JRadioButtonMenuItem j;
        j= (JRadioButtonMenuItem) primaryActionButtonMap.remove(module);
        if ( j!=null && !headless ) {
            if ( primaryPopup.isAncestorOf(j) ) {
                primaryPopup.remove(j);
                numInserted--;
            }
        }
        j= (JRadioButtonMenuItem) secondaryActionButtonMap.remove(module);
        if ( j!=null && !headless ) {
            if ( secondaryPopup.isAncestorOf(j) ) {
                secondaryPopup.remove(j);
                numInsertedSecondary--;
            }
        }
        modules.remove(module);
    }
    

    /**
     * add a mouse module to the list of available modules.  If a module with the same
     * label exists already, it will be replaced.
     * @param module the module
     */
    public synchronized void addMouseModule(MouseModule module) {

        if (headless) {
            DasLogger.getLogger(DasLogger.GUI_LOG).fine("not adding module since headless is true");

        } else {
            MouseModule preExisting = getModuleByLabel(module.getLabel());
            if (preExisting != null) {
                DasLogger.getLogger(DasLogger.GUI_LOG).log(Level.FINE, "Replacing mouse module {0}.", module.getLabel());
                replaceMouseModule(preExisting, module);

            } else {

                modules.add(module);

                String name = module.getLabel();

                JRadioButtonMenuItem primaryNewItem = new JRadioButtonMenuItem(name);
                JRadioButtonMenuItem secondaryNewItem = new JRadioButtonMenuItem(name);

                primaryNewItem.addActionListener(popupListener);
                primaryNewItem.setActionCommand("primary");
                secondaryNewItem.addActionListener(popupListener);
                secondaryNewItem.setActionCommand("secondary");

                primaryActionButtonMap.put(module, primaryNewItem);
                secondaryActionButtonMap.put(module, secondaryNewItem);

                try {
                    // insert the check box after the separator, and at the end of the actions list.
                    int i= numInserted + 1 + primaryActionButtonMap.size() - 1;
                    if ( i>primaryPopup.getComponentCount() ) {
                        i= primaryPopup.getComponentCount();
                    }
                    if ( i<0 ) {
                        logger.finer("here is that bug where numInserted is negative...");
                        i= 0;
                    }
                    primaryPopup.add(primaryNewItem, i );
                    i= numInsertedSecondary + 1 + secondaryActionButtonMap.size() - 1;
                    if ( i>secondaryPopup.getComponentCount() ) {
                        i= secondaryPopup.getComponentCount();
                    }
                    if ( i<0 ) {
                        logger.finer("here is that bug where numInsertedSecondary is negative...");
                        i= 0;
                    }
                    secondaryPopup.add(secondaryNewItem, i );
                } catch ( IllegalArgumentException ex ) {
                    logger.log( Level.SEVERE, ex.getMessage(), ex );
                }

            }
        }
    }

    /**
     * added so ColumnColumnConnector could delegate to DasPlot's adapter.
     * @return
     */
    public JPopupMenu getPrimaryPopupMenu() {
        return this.primaryPopup;
    }

    /**
     * allow clients to cancel, to take the same action as if cancel 
     * were pressed.
     */
    public void cancel() {
        Runnable run= new Runnable() {
            @Override
            public void run() {
                active = null;
                getGlassPane().setDragRenderer(null, null, null);
                parent.getCanvas().paintImmediately(0, 0, parent.getCanvas().getWidth(), parent.getCanvas().getHeight());
                feedback.setMessage("");
                refresh();        
            }
        };
        SwingUtilities.invokeLater(run);
    }
    
    public KeyAdapter getKeyAdapter() {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent ev) {
                logger.finest("keyPressed ");
                if (ev.getKeyCode() == KeyEvent.VK_ESCAPE && active != null) {
                    active = null;
                    getGlassPane().setDragRenderer(null, null, null);
                    parent.getCanvas().paintImmediately(0, 0, parent.getCanvas().getWidth(), parent.getCanvas().getHeight());
                    feedback.setMessage("");
                    refresh();
                    ev.consume();
                } else if (ev.getKeyCode() == KeyEvent.VK_SHIFT) {
                    drawControlPoints = true;
                    parent.repaint();
                } else if (ev.getKeyChar() == 'p') {
                    pinned = true;
                    ev.consume();
                    feedback.setMessage("pinned, will stay active until escape is pressed");
                } else if ( ev.getKeyCode()==KeyEvent.VK_UP || ev.getKeyCode()==KeyEvent.VK_DOWN
                    || ev.getKeyCode()==KeyEvent.VK_LEFT || ev.getKeyCode()==KeyEvent.VK_RIGHT ) {
                    try {
                        int dx= ev.getKeyCode()==KeyEvent.VK_LEFT ? -1 : ( ev.getKeyCode()==KeyEvent.VK_RIGHT ? 1 : 0 );
                        int dy= ev.getKeyCode()==KeyEvent.VK_UP ? -1 : ( ev.getKeyCode()==KeyEvent.VK_DOWN ? 1 : 0 );
                        Robot robot= new Robot();
                        Point p= MouseInfo.getPointerInfo().getLocation();
                        robot.mouseMove( p.x+dx, p.y+dy );
                    } catch (AWTException ex) {
                        logger.log(Level.SEVERE, null, ex);
                    }
                } else {
                    if (active == null) {
                        return;
                    }
                    for (Object active1 : active) {
                        ((MouseModule) active1).keyPressed(ev);
                    }
                }
            }
            @Override
            public void keyReleased(KeyEvent ev) {
                if (ev.getKeyCode() == KeyEvent.VK_SHIFT) {
                    drawControlPoints = false;
                    parent.repaint();
                }
                if (active == null) {
                    return;
                }
                for (Object active1 : active) {
                    ((MouseModule) active1).keyReleased(ev);
                }
            }
            @Override
            public void keyTyped(KeyEvent ev) {
                if (active == null) {
                    return;
                }
                for (Object active1 : active) {
                    ((MouseModule) active1).keyTyped(ev);
                }
            }
        };
    }

    public MouseModule getPrimaryModule() {
        ArrayList activ = new ArrayList();
        for (Object module : modules) {
            JRadioButtonMenuItem j = (JRadioButtonMenuItem) primaryActionButtonMap.get(module);
            if (j.isSelected()) {
                activ.add(module);
            }
        }
        return (MouseModule) activ.get(0); // at one time we allowed multiple modules at once.
    }

    public MouseModule getSecondaryModule() {
        ArrayList activ = new ArrayList();
        for (Object module : modules) {
            JRadioButtonMenuItem j = (JRadioButtonMenuItem) secondaryActionButtonMap.get(module);
            if (j.isSelected()) {
                activ.add(module);
            }
        }
        return (MouseModule) activ.get(0); // at one time we allowed multiple modules at once.
    }

    /**
     * set the primary module, the module receiving left-button events, to the
     * module provided.  If the module is not already loaded, implicitly addMouseModule
     * is called.
     * @param module the module
     */
    public void setPrimaryModule(MouseModule module) {
        if (headless) {
            return;
        }
        JRadioButtonMenuItem j = (JRadioButtonMenuItem) primaryActionButtonMap.get(module);
        if (j == null) {
            addMouseModule(module);
        }
        for (Iterator i = primaryActionButtonMap.entrySet().iterator(); i.hasNext();) {
            try {
                Object ii = ((Map.Entry) i.next()).getValue();
                ((JRadioButtonMenuItem) ii).setSelected(false);
            } catch (RuntimeException ex) {
                logger.log( Level.SEVERE, ex.getMessage(), ex );
                throw ex;
            }
        }

        j = (JRadioButtonMenuItem) primaryActionButtonMap.get(module);
        if (j != null) {
            j.setSelected(true);
        }
        primarySelectedItem = j;
        primary = module;
        parent.setCursor(primary.getCursor());
    }

    /**
     * set the secondary module, the module receiving middle-button events, to the
     * module provided.  If the module is not already loaded, implicitly addMouseModule
     * is called.
     * 
     * @param module
     */
    public void setSecondaryModule(MouseModule module) {
        if (headless) {
            return;
        }
        JRadioButtonMenuItem j = (JRadioButtonMenuItem) secondaryActionButtonMap.get(module);
        if (j == null) {
            addMouseModule(module);
        }
        for (Iterator i = secondaryActionButtonMap.entrySet().iterator(); i.hasNext();) {
            try {
                Object ii = ((Map.Entry) i.next()).getValue();
                ((JRadioButtonMenuItem) ii).setSelected(false);
            } catch (RuntimeException ex) {
                logger.log( Level.SEVERE, ex.getMessage(), ex );
                throw ex;
            }
        }

        j = (JRadioButtonMenuItem) secondaryActionButtonMap.get(module);
        if (j != null) {
            j.setSelected(true);
        }
        secondarySelectedItem = j;
        secondary = module;
    }

    /**
     * create the popup for the component.  This popup has three
     * sections:
     * <pre>1. component actions
     *2. mouse modules
     *3. canvas actions</pre>
     * The variable numInserted is the number of actions inserted, and
     * is used to calculate the position of inserted mouse modules.
     */
    private int createPopup(JPopupMenu popup) {
        
        synchronized (this) {
            popupListener = createPopupMenuListener();
        }

        Action[] componentActions = parent.getActions();
        for (Action componentAction : componentActions) {
            JMenuItem item = new JMenuItem();
            item.setAction(componentAction);
            popup.add(item);
        }
        int numInsert = componentActions.length;

        popup.addSeparator();
        // mouse modules go here
        popup.addSeparator();
                
        Action[] canvasActions = DasCanvas.getActions();
        for (Action canvasAction : canvasActions) {
            JMenuItem item = new JMenuItem();
            item.setAction(canvasAction);
            popup.add(item);
        }

        return numInsert;
    }

    private ActionListener createPopupMenuListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //DasMouseInputAdapter outer = DasMouseInputAdapter.this; // useful for debugging
                String command = e.getActionCommand();
                if (command.equals("properties")) {
                    parent.showProperties();
                } else if (command.equals("print")) {
                    Printable p = ((DasCanvas) parent.getParent()).getPrintable();
                    PrinterJob pj = PrinterJob.getPrinterJob();
                    pj.setPrintable(p);
                    if (pj.printDialog()) {
                        try {
                            pj.print();
                        } catch (PrinterException pe) {
                            Object[] message = {"Error printing", pe.getMessage()};
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
                        DasCanvas canvas = (DasCanvas) parent.getParent();
                        try {
                            canvas.writeToPng(pngFileTextField.getText());
                        } catch (java.io.IOException ioe) {
                            DasApplication.getDefaultApplication().getExceptionHandler().handle(ioe);
                        }
                    }
                } else if (command.equals("pngBrowse")) {
                    int choice = pngFileChooser.showDialog(parent, "Select File");
                    if (choice == JFileChooser.APPROVE_OPTION) {
                        pngFileTextField.setText(pngFileChooser.getSelectedFile().getPath());
                    }
                } else if (command.equals("close")) {
                } else if (command.equals("primary")) {
                    if (primarySelectedItem != null) {
                        primarySelectedItem.setSelected(false);
                    }
                    for (Object module : modules) {
                        JRadioButtonMenuItem j = (JRadioButtonMenuItem) primaryActionButtonMap.get(module);
                        if (j.isSelected()) {
                            primarySelectedItem = j;
                            break;
                        }
                    }
                    primarySelectedItem.setSelected(true); // for case when selection wasn't changed.
                } else if (command.equals("secondary")) {
                    if (secondarySelectedItem != null) {
                        secondarySelectedItem.setSelected(false);
                    }
                    for (Object module : modules) {
                        JRadioButtonMenuItem j = (JRadioButtonMenuItem) secondaryActionButtonMap.get(module);
                        if (j.isSelected()) {
                            secondarySelectedItem = j;
                            break;
                        }
                    }
                } else {
                    logger.log(Level.FINE, "{0}", command);
                }
            }
        };
    }

    /**
     * call the renderDrag method of the active module's dragRenderer.  This method
     * returns an array of Rectangles, or null, indicating the affected regions.
     * It's also permissable for a array element to be null.
     */
    private void renderSelection() {
        try {
            for (Object active1 : active) {
                DragRenderer dr = ((MouseModule) active1).getDragRenderer();
                getGlassPane().setDragRenderer(dr, dSelectionStart, dSelectionEnd);
            }
        } catch (RuntimeException e) {
            DasApplication.getDefaultApplication().getExceptionHandler().handle(e);
        }
    }

    /**
     * This attempts to redraw just the affected portions of parent.  Presently it
     * needs to call the parent's paintImmediately twice, because we don't know what
     * the dragRenderer's dirty bounds will be.
     */
    private synchronized void refresh() {
        if (dirtyBoundsList.length > 0) {
            Rectangle[] dd = new Rectangle[dirtyBoundsList.length];
            for (int i = 0; i < dd.length; i++) {
                if (dirtyBoundsList[i] != null) {
                    dd[i] = new Rectangle(dirtyBoundsList[i]);
                }
            }
            for (Rectangle dd1 : dd) {
                if (dd1 != null) {
                    //parent.getCanvas().paintImmediately(dd[i]);
                    parent.getCanvas().repaint(dd1);
                }
            }
            for (Rectangle dirtyBoundsList1 : dirtyBoundsList) {
                if (dirtyBoundsList1 != null) {
                    //parent.getCanvas().paintImmediately(dirtyBoundsList[i]);
                    parent.getCanvas().repaint(dirtyBoundsList1);
                }
            }
        } else {
            if (active != null) {
                //parent.getCanvas().paintImmediately(0, 0, parent.getCanvas().getWidth(), parent.getCanvas().getHeight());
                parent.getCanvas().repaint();
            }
        }
        if (active == null) {
            dirtyBoundsList = new Rectangle[0];
        }
    }

    /*
     * Paint the drag renderer on top of parent.  Graphics g1 should be in
     * the parent's coordinate frame.
     */
    public void paint(Graphics g1) {
        Graphics2D g = (Graphics2D) g1.create();

        DasCanvasComponent lparent= this.parent;
        if ( lparent==null ) return;
        g.translate(-lparent.getX(), -lparent.getY());

        if (active != null) {
            renderSelection();
        }
        if (hasFocus && hoverHighlite) {
            g.setColor(new Color(255, 0, 0, 10));
            g.setStroke(new BasicStroke(10));
            g.draw(lparent.getBounds());
            g.dispose();
            return;
        }
        if (hasFocus && drawControlPoints) {
            drawControlPoints(g,lparent);
        }
        g.dispose();
    }

    private void drawControlPoints(Graphics2D g, DasCanvasComponent parent ) {
        if (parent.getRow() != DasRow.NULL && parent.getColumn() != DasColumn.NULL) {
            int xLeft = parent.getColumn().getDMinimum();
            int xRight = parent.getColumn().getDMaximum();
            int xMid= ( xLeft + xRight ) / 2;
            int yTop = parent.getRow().getDMinimum();
            int yBottom = parent.getRow().getDMaximum();
            int yMid= (  yTop + yBottom ) / 2;
            
            Graphics2D gg = (Graphics2D) g.create();

            gg.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
            
            //gg.translate(-parent.getX(),-parent.getY());
            gg.setColor(new Color(0, 0, 0, 255));

            int ss = 9;
            gg.fillRect(xLeft + 1, yTop + 1, ss - 2, ss - 2);
            gg.fillRect(xRight - ss + 1, yTop + 1, ss - 2, ss - 2);
            gg.fillRect(xLeft + 1, yBottom - ss + 1, ss - 2, ss - 2);
            gg.fillRect(xRight - ss + 1, yBottom - ss + 1, ss - 2, ss - 2);

            gg.fillRect(xMid + 1 - ss/2, yTop + 1, ss - 2, ss - 2);
            gg.fillRect(xRight - ss + 1, yMid + 1 - ss/2, ss - 2, ss - 2);
            gg.fillRect(xMid + 1 - ss/2, yBottom - ss + 1, ss - 2, ss - 2);
            gg.fillRect(xLeft + 1, yMid - ss/2 + 1, ss - 2, ss - 2);

            gg.setColor(new Color(255, 255, 255, 100));
            gg.drawRect(xLeft, yTop, ss, ss);
            gg.drawRect(xRight - ss, yTop, ss, ss);
            gg.drawRect(xLeft, yBottom - ss, ss, ss);
            gg.drawRect(xRight - ss, yBottom - ss, ss, ss);
            gg.drawRect(xMid- ss/2, yTop + 1, ss, ss );
            gg.drawRect(xRight - ss, yMid - ss/2, ss, ss );
            gg.drawRect(xMid - ss/2, yBottom - ss, ss, ss );
            gg.drawRect(xLeft , yMid - ss/2, ss, ss );

            int rr = 4;
            g.setColor(new Color(255, 255, 255, 100));
            gg.fillOval(xMid - rr - 1, yMid - rr - 1, rr * 2 + 3, rr * 2 + 3);

            gg.setColor(new Color(0, 0, 0, 255));

            gg.drawOval(xMid - rr, yMid - rr, rr * 2, rr * 2);
            gg.fillOval(xMid - 1, yMid - 1, 3, 3);

            gg.dispose();
        }
    }

    private MouseMode activateMouseMode(MouseEvent e) {

        boolean xLeftSide = false;
        boolean xRightSide = false;
        boolean xMiddle = false;
        boolean yTopSide = false;
        boolean yBottomSide = false;
        boolean yMiddle = false;

        Point mousePoint = e.getPoint();
        mousePoint.translate(parent.getX(), parent.getY());// canvas coordinate system

        if (parent.getRow() != DasRow.NULL && parent.getColumn() != DasColumn.NULL) {
            int xLeft = parent.getColumn().getDMinimum();
            int xRight = parent.getColumn().getDMaximum();
            int yTop = parent.getRow().getDMinimum();
            int yBottom = parent.getRow().getDMaximum();
            int xmid = (xLeft + xRight) / 2;
            int ymid = (yTop + yBottom) / 2;

            xLeftSide = mousePoint.getX() < xLeft + 10;
            xRightSide = mousePoint.getX() > xRight - 10;
            xMiddle = Math.abs(mousePoint.getX() - xmid) < 4;
            yTopSide = (mousePoint.getY() < yTop + 10) && (mousePoint.getY() >= yTop);
            yBottomSide = mousePoint.getY() > (yBottom - 10);
            yMiddle = Math.abs(mousePoint.getY() - ymid) < 4;

        }

        MouseMode result = MouseMode.idle;
        Cursor cursor = new Cursor(Cursor.DEFAULT_CURSOR);

        if ( parent instanceof DasColorBar || !(parent instanceof DasAxis)) {
            if ((e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) == MouseEvent.SHIFT_DOWN_MASK) {
                if (xLeftSide) {
                    if (yTopSide) {
                        result = MouseMode.resize;
                        cursor = new Cursor(Cursor.NW_RESIZE_CURSOR);
                    } else if (yBottomSide) {
                        result = MouseMode.resize;
                        cursor = new Cursor(Cursor.SW_RESIZE_CURSOR);
                    } else if (yMiddle) {
                        result = MouseMode.resize;
                        cursor = new Cursor(Cursor.W_RESIZE_CURSOR);
                    }
                } else if (xRightSide) {
                    if (yTopSide) {
                        result = MouseMode.resize;
                        cursor = new Cursor(Cursor.NE_RESIZE_CURSOR);
                    } else if (yBottomSide) {
                        result = MouseMode.resize;
                        cursor = new Cursor(Cursor.SE_RESIZE_CURSOR);
                    } else if (yMiddle) {
                        result = MouseMode.resize;
                        cursor = new Cursor(Cursor.E_RESIZE_CURSOR);
                    }
                } else if (xMiddle && yMiddle) {
                    result = MouseMode.move;
                    cursor = new Cursor(Cursor.MOVE_CURSOR);
                } else if (xMiddle && yTopSide ) {
                    result = MouseMode.resize;
                    cursor = new Cursor(Cursor.N_RESIZE_CURSOR);
                } else if ( xMiddle && yBottomSide ) {
                    result = MouseMode.resize;
                    cursor = new Cursor(Cursor.S_RESIZE_CURSOR);
                }
            }

        }

        if (result == MouseMode.resize) {
            result.resizeBottom = yBottomSide;
            result.resizeTop = yTopSide;
            result.resizeRight = xRightSide;
            result.resizeLeft = xLeftSide;
        } else if (result == MouseMode.move) {
            result.moveStart = e.getPoint();
            result.moveStart.translate( parent.getX(), parent.getY());
        }

        if (result != mouseMode) {
            getGlassPane().setCursor(cursor);
        }
        return result;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        logger.finest("mouseMoved");
        Point l = parent.getLocation();
        xOffset = l.x;
        yOffset = l.y;

        boolean drawControlPoints0 = this.drawControlPoints;
        
        drawControlPoints = (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) == MouseEvent.SHIFT_DOWN_MASK;

        if (drawControlPoints0 != drawControlPoints) {
            parent.repaint();
        }

        MouseMode m;
        if ((m = activateMouseMode(e)) != null) {
            mouseMode = m;
        } else {
            mouseMode = MouseMode.idle;
        }
    }

    private void showPopup(JPopupMenu menu, MouseEvent ev) {
        logger.finest("showPopup");
        if ( menu != primaryPopup && menu != secondaryPopup) {
            throw new IllegalArgumentException("menu must be primary or secondary popup menu");
        }
        ButtonGroup bg= new ButtonGroup();
        for (Iterator i = modules.iterator(); i.hasNext();) { //TODO: it looks like this strange bit of code just sets the label.
            MouseModule mm = (MouseModule) i.next();
            JRadioButtonMenuItem j = (JRadioButtonMenuItem) primaryActionButtonMap.get(mm);
            j.setText(mm.getLabel());
            bg.add(j);
        }
        menu.show(ev.getComponent(), ev.getX(), ev.getY());
    }

    public void setPinned(boolean b) {
        pinned = b;
    }
    
    public boolean getPinned() {
        return pinned;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        logger.log(Level.FINE, "mousePressed {0} on {1}", new Object[] { mouseMode, parent } );
        if (pinned) {
            active = null;
            refresh();
        }
        pinned = false;
        Point l = parent.getLocation();
        parent.requestFocus();
        xOffset = l.x;
        yOffset = l.y;
        pressPosition = e.getPoint();

        Point cp = new Point(e.getPoint());
        cp.translate(xOffset, yOffset);
        if (!parent.acceptContext(cp.x, cp.y)) {
            return;
        }
        if (mouseMode == MouseMode.resize) {
            resizeStart = new Point(0, 0);
            if (mouseMode.resizeRight) {
                resizeStart.x = 0 + xOffset;
            } else if (mouseMode.resizeLeft) {
                resizeStart.x = parent.getWidth() + xOffset;
            } else {
                resizeStart.x = 0 + xOffset;
            }
            if (mouseMode.resizeTop) {
                resizeStart.y = parent.getHeight() + yOffset;
            } else if (mouseMode.resizeBottom) {
                resizeStart.y = 0 + yOffset;
            } else {
                resizeStart.y = 0 + yOffset;
            }

        } else if (mouseMode == MouseMode.move) {
            mouseMode.moveStart = e.getPoint();
            mouseMode.moveStart.translate(xOffset, yOffset);

        } else {
            if (active == null) {
                button = e.getButton();
                dSelectionStart = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), parent.getCanvas());
                dSelectionEnd = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), parent.getCanvas());

                if (e.isControlDown() || button == MouseEvent.BUTTON3) {
                    if (button == MouseEvent.BUTTON1 || button == MouseEvent.BUTTON3) {
                        showPopup(primaryPopup, e);
                    } else {
                        showPopup(secondaryPopup, e);
                    }
                } else {

                    active = new ArrayList();

                    if (button == MouseEvent.BUTTON1 || button == MouseEvent.BUTTON3) {
                        for (Object module : modules) {
                            JRadioButtonMenuItem j = (JRadioButtonMenuItem) primaryActionButtonMap.get(module);
                            if (j.isSelected()) {
                                active.add(module);
                            }
                        }
                    } else {
                        for (Object module : modules) {
                            JRadioButtonMenuItem j = (JRadioButtonMenuItem) secondaryActionButtonMap.get(module);
                            if (j.isSelected()) {
                                active.add(module);
                            }
                        }
                    }

                    Runnable run= new Runnable() {
                        @Override
                        public void run() {
                            ArrayList lactive= active;
                            if ( lactive==null || lactive.isEmpty() ) return;
                            MouseModule theone= ((MouseModule)lactive.get(0));
                            if ( theone==null ) return;
                            try {
                                // set the message based on whether the module overrides mouseRangeSelected
                                Method m= theone.getClass().getMethod("mouseRangeSelected",MouseDragEvent.class);
                                if ( m.equals(MouseModule.class.getMethod("mouseRangeSelected",MouseDragEvent.class)) ) {
                                    //feedback.setMessage("" + theone.getListLabel() );
                                } else {
                                    // it's going to do something when we release.
                                    String s= theone.getDirections();
                                    if ( s==null ) {
                                        s= theone.getLabel();
                                    }
                                    if ( !( s.startsWith(theone.getLabel()) ) ) {
                                        s= theone.getLabel()+": "+s;
                                    }
                                    feedback.setMessage( s + ", press escape to cancel" );                    
                                }
                            } catch (NoSuchMethodException ex) {
                                logger.log(Level.SEVERE, ex.getMessage(), ex);
                            } catch (SecurityException ex) {
                                logger.log(Level.SEVERE, ex.getMessage(), ex);
                            }
                            
                        }
                    };
                    SwingUtilities.invokeLater(run);

                    mouseMode = MouseMode.moduleDrag;

                    mousePointSelection.set(e.getX() + xOffset, e.getY() + yOffset);
                    for (Object active1 : active) {
                        MouseModule j = (MouseModule) active1;
                        j.mousePressed(e);
                        if (j.dragRenderer.isPointSelection()) {
                            mouseDragged(e);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        logger.log(Level.FINE, "mouseDragged {0} on {1}", new Object[] { mouseMode, parent } );
        if (mouseMode == MouseMode.resize) {
            Point p = e.getPoint();
            p.translate(parent.getX(), parent.getY());
            if ( !( mouseMode.resizeBottom || mouseMode.resizeTop ) ) {
                p.y= parent.getRow().getDMaximum();
            }
            if ( !( mouseMode.resizeRight || mouseMode.resizeLeft ) ) {
                p.x= parent.getColumn().getDMaximum();
            }
            getGlassPane().setDragRenderer(resizeRenderer, resizeStart, p);
            getGlassPane().repaint();

        } else if (mouseMode == MouseMode.move) {
            Point moveEnd = e.getPoint();
            moveEnd.translate(xOffset, yOffset);
            int dx = moveEnd.x - mouseMode.moveStart.x;
            int dy = moveEnd.y - mouseMode.moveStart.y;

            int xmin = parent.getColumn().getDMinimum();
            int xmax = parent.getColumn().getDMaximum();

            int ymin = parent.getRow().getDMinimum();
            int ymax = parent.getRow().getDMaximum();
            Point p1 = new Point(xmin + dx, ymin + dy);
            Point p2 = new Point(xmax + dx, ymax + dy);

            getGlassPane().setDragRenderer(resizeRenderer, p1, p2);
            getGlassPane().repaint();
        } else {
            if (active != null) {
                dSelectionEnd = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), parent.getCanvas());

                mousePointSelection.set((int) dSelectionEnd.getX(), (int) dSelectionEnd.getY());
                for (Object active1 : active) {
                    try {
                        MouseModule j = (MouseModule) active1;
                        if (j.dragRenderer.isPointSelection()) {
                            logger.finest("mousePointSelected");
                            j.mousePointSelected(mousePointSelection);
                        }
                        if (j.dragRenderer.isUpdatingDragSelection()) {
                            // Really it should be the DMM that indicates it wants updates...whoops...
                            MouseDragEvent de = j.dragRenderer.getMouseDragEvent(parent, dSelectionStart, dSelectionEnd, e.isShiftDown());
                            logger.finest("mouseRangeSelected");
                            j.mouseRangeSelected(de);
                        }
                        j.mouseDragged(e);
                    } catch (RuntimeException except) {
                        DasApplication.getDefaultApplication().getExceptionHandler().handle(except);
                    }
                }
                refresh();
            }
        }
    }

    private void performResize(MouseEvent e) {
        int dxLeft = parent.getColumn().getDMinimum();
        int dxRight = parent.getColumn().getDMaximum();
        int dyTop = parent.getRow().getDMinimum();
        int dyBottom = parent.getRow().getDMaximum();

        int dx = e.getX() + xOffset;
        int dy = e.getY() + yOffset;
        if (mouseMode.resizeRight) {
            dxRight = dx;
        } else if (mouseMode.resizeLeft) {
            dxLeft = dx;
        }
        if (mouseMode.resizeTop) {
            dyTop = dy;
        } else if (mouseMode.resizeBottom) {
            dyBottom = dy;
        }

        parent.getColumn().setDPosition(dxLeft, dxRight);
        parent.getRow().setDPosition(dyTop, dyBottom);

        xOffset += dx;
        yOffset += dy;

        parent.resize();
        getGlassPane().setDragRenderer(null, null, null);
        getGlassPane().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        logger.log(Level.FINE, "mouseReleased {0} on {1}", new Object[] { mouseMode, parent } );
        if (mouseMode == MouseMode.resize) {
            performResize(e);
            getGlassPane().setDragRenderer(null, null, null);
            parent.getCanvas().paintImmediately(0, 0, parent.getCanvas().getWidth(), parent.getCanvas().getHeight());
            refresh();
        } else if (mouseMode == MouseMode.move) {
            performMove(e);
            getGlassPane().setDragRenderer(null, null, null);
            parent.getCanvas().paintImmediately(0, 0, parent.getCanvas().getWidth(), parent.getCanvas().getHeight());
            refresh();

        } else {
            if (e.getButton() == button) {
                if (active != null) {
                    for (Object active1 : active) {
                        MouseModule j = (MouseModule) active1;
                        try {
                            MouseDragEvent de =
                                    j.dragRenderer.getMouseDragEvent(parent, dSelectionStart, dSelectionEnd, e.isShiftDown());
                            if ( de!=null ) {
                                j.mouseRangeSelected(de);
                            }
                            feedback.setMessage("" ); 
                        } catch (RuntimeException ex) {
                            DasApplication.getDefaultApplication().getExceptionHandler().handle(ex);
                        } finally {
                            button = 0;
                            try {
                                j.mouseReleased(e);
                            } catch (RuntimeException ex2) {
                                DasApplication.getDefaultApplication().getExceptionHandler().handle(ex2);
                            }
                        }
                    }
                    if (!pinned) {
                        active = null;
                        getGlassPane().setDragRenderer(null, null, null);
                        parent.getCanvas().paintImmediately(0, 0, parent.getCanvas().getWidth(), parent.getCanvas().getHeight());
                        refresh();
                    }
                }
            }
        }

    }

    public MouseModule getMouseModule(int i) {
        return (MouseModule) modules.get(i);
    }

    public MouseModule[] getMouseModules() {
        MouseModule[] result = new MouseModule[modules.size()];
        result= (MouseModule[]) modules.toArray(result);
        return result;
    }

    public String getPrimaryModuleByLabel() {
        MouseModule primary1 = getPrimaryModule();
        return primary1 == null ? "" : primary1.getLabel();
    }
    
    public void setPrimaryModuleByLabel(String label) {
        MouseModule mm = getModuleByLabel(label);
        if (mm != null) {
            setPrimaryModule(mm);
        }
    }

    public String getSecondaryModuleByLabel() {
        MouseModule secondary1 = getPrimaryModule();
        return secondary1 == null ? "" : secondary1.getLabel();
    }
    
    public void setSecondaryModuleByLabel(String label) {
        MouseModule mm = getModuleByLabel(label);
        if (mm != null) {
            setSecondaryModule(mm);
        }
    }

    /**
     * //TODO: check this
     * Setter for property mouseModules.
     * 
     * @param i the index
     * @param mouseModule the new mouseModule to use.
     */
    public void setMouseModule(int i, MouseModule mouseModule) {
        this.modules.set(i, mouseModule);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        hasFocus = true;
        if (e.isShiftDown()) {
            parent.repaint();
        }
        if (primary != null) {
            getGlassPane().setCursor(primary.getCursor());
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        hasFocus = false;
        if (e.isShiftDown()) {
            parent.repaint();
        }
        getGlassPane().setCursor(Cursor.getDefaultCursor());
    }

    /**
     * hack to provide way to get rid of "Dump Data".  
     * @param label string to search for.
     */
    public synchronized void removeMenuItem(String label) {
        if (headless) {
            return;
        }
        MenuElement[] ele = primaryPopup.getSubElements();
        int index = -1;
        for (int i = 0; i < numInserted; i++) {
            if (ele[i] instanceof JMenuItem) {
                if (((JMenuItem) ele[i]).getText().contains(label)) {
                    index = i;
                    break;
                }
            }
        }
        if (index != -1) {
            primaryPopup.remove(index);
            numInserted--;
            logger.log(Level.FINER, "numInserted: {0}", numInserted);
        }

    }

    public synchronized void addMenuItem(final Component b) {
        if (headless) {
            return;
        }
        if (numInserted == 0) {
            primaryPopup.insert(new JPopupMenu.Separator(), 0);
            numInserted++;
        }
        if ( b instanceof JPopupMenu ) {
            if ( numInserted>1 ) {
                primaryPopup.insert(new JPopupMenu.Separator(), 0);
                numInserted++;
            }
            JPopupMenu c= (JPopupMenu)b;

            for ( MenuElement me : c.getSubElements() ) {
                if ( me.getComponent() instanceof JRadioButtonMenuItem ) continue;   //TODO: kludge
                primaryPopup.insert( me.getComponent(), numInserted );
                numInserted++;
            }
        } else {
            primaryPopup.insert(b, numInserted);
            numInserted++;
        }

    }

    /**
     * return a menu with font to match LAF.
     * @param label
     * @return
     */
    public JMenu addMenu(String label) {
        JMenu result = new JMenu(label);
        addMenuItem(result);
        return result;
    }
    
    /**
     * return number of elements for diagnostic purposes.
     * @return 
     */
    public int getNumInserted() {
        return numInserted;
    }

    private DasCanvas.GlassPane getGlassPane() {
        DasCanvas.GlassPane r = (DasCanvas.GlassPane) ((DasCanvas) parent.getParent()).getGlassPane();
        if (r.isVisible() == false) {
            r.setVisible(true);
        }
        return r;
    }

    /**
     * remove the mouse module with the label.  
     * @param label the label (case-sensitive)
     * @return null if not found, or the module.
     */
    public MouseModule getModuleByLabel(java.lang.String label) {
        MouseModule result = null;
        for (Object module : modules) {
            if (label.equals(((MouseModule) module).getLabel())) {
                result = (MouseModule) module;
            }
        }
        return result;
    }
    /**
     * Draws a faint box around the border when the mouse enters the component,
     * to help indicate what's going on.
     */
    private boolean hoverHighlite = false;

    public boolean isHoverHighlite() {
        return this.hoverHighlite;
    }

    /**
     * glow the outline of the mouse area, for development.
     * @param value 
     */
    public void setHoverHighlite(boolean value) {
        this.hoverHighlite = value;
    }

    /**
     * returns the position of the last mouse press.  This is a hack so that
     * the mouse position can be obtained to get the context of the press.
     * The result point is in the parent's coordinate system.
     * @return the position of the mouse press.
     * @see #getMousePressPositionOnCanvas() 
     */
    public Point getMousePressPosition() {
        return this.pressPosition;
    }
    
    /**
     * return the position of the last mouse press, in the canvas coordinate 
     * frame.
     * @return the position of the mouse press in the canvas coordinate frame.
     * @see #getMousePressPosition() 
     */
    public Point getMousePressPositionOnCanvas() {
        Point r= this.pressPosition.getLocation(); // get a copy
        r.translate( this.parent.getX(), this.parent.getY() );
        return r;
    }

    private void performMove(MouseEvent e) {
        Point moveEnd = e.getPoint();
        moveEnd.translate(xOffset, yOffset);
        int dx = moveEnd.x - mouseMode.moveStart.x;
        int dy = moveEnd.y - mouseMode.moveStart.y;

        this.xOffset += dx;
        this.yOffset += dy;

        int min = parent.getColumn().getDMinimum();
        int max = parent.getColumn().getDMaximum();
        parent.getColumn().setDPosition(min + dx, max + dx);

        min = parent.getRow().getDMinimum();
        max = parent.getRow().getDMaximum();
        parent.getRow().setDPosition(min + dy, max + dy);
    }

    /**
     * the mouse wheel was turned so many units.  
     * Delegate this to the primary module, so that if it is set to "ZoomX"
     * then the mousewheel will be in just the X direction.
     * @param e the mouse wheel event
     */
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if ( secondary != null ) {
            secondary.mouseWheelMoved(e);
        }
    }
    
    /**
     * remove all references to mouse modules
     */
    public void releaseAll() {
        active= null;
        modules.clear();
        primary= null;
        secondary= null;
        primaryActionButtonMap.clear();
        secondaryActionButtonMap.clear();
        setFeedback(null);
        //parent=null;
    }

}
