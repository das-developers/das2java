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
import edu.uiowa.physics.pw.das.graph.*;
import edu.uiowa.physics.pw.das.graph.DasAxis;
import edu.uiowa.physics.pw.das.graph.DasCanvas;
import edu.uiowa.physics.pw.das.graph.DasCanvasComponent;
import edu.uiowa.physics.pw.das.system.*;
import edu.uiowa.physics.pw.das.util.DasExceptionHandler;
import java.awt.*;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.event.*;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.*;
import java.util.logging.Logger;

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
    private Vector active = null;
    private boolean pinned = false;
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
    JCheckBoxMenuItem secondarySelectedItem;    // must be non-null, but may contain null elements
    Rectangle[] dirtyBoundsList;
    Logger log = DasLogger.getLogger(DasLogger.GUI_LOG);
    /**
     * number of additional inserted popup menu items.
     */
    int numInserted;
    protected ActionListener popupListener;
    protected DasCanvasComponent parent = null;
    //private Point selectionStart;   // in component frame
    //private Point selectionEnd;     // in component frame
    private Point dSelectionStart;  // in DasCanvas device frame
    private Point dSelectionEnd; // in DasCanvas device frame
    private MousePointSelectionEvent mousePointSelection;
    private int xOffset; // parent to canvas offset
    private int yOffset; // parent to canvas offset
    private int button = 0; // current depressed button
    private MouseMode mouseMode = MouseMode.idle;
    private boolean drawControlPoints = false;
    private DragRenderer resizeRenderer = null;
    private Point resizeStart = null;
    /*
     *this will be removed, and the component can add its own popup buttons.
     */
    Vector hotSpots = null;
    Rectangle dirtyBounds = null;
    private boolean hasFocus = false;
    private Point pressPosition;  // in the component frame
    private boolean headless;

    private static class MouseMode {

        String s;
        boolean resizeTop = false;
        boolean resizeBottom = false;
        boolean resizeRight = false;
        boolean resizeLeft = false;
        Point moveStart = null; // in the DasCanvas frame
        static MouseMode idle = new MouseMode("idle");
        static MouseMode resize = new MouseMode("resize");
        static MouseMode move = new MouseMode("move");
        static MouseMode moduleDrag = new MouseMode("moduleDrag");

        MouseMode(String s) {
            this.s = s;
        }

        public String toString() {
            return s;
        }
    }

    /** Creates a new instance of dasMouseInputAdapter */
    public DasMouseInputAdapter(DasCanvasComponent parent) {

        this.parent = parent;

        modules = new Vector();

        primaryActionButtonMap = new HashMap();
        secondaryActionButtonMap = new HashMap();

        numInserted = 0;

        this.headless = DasApplication.getDefaultApplication().isHeadless();
        if (!headless) {
            primaryPopup = createPopup();
            secondaryPopup = createPopup();
        }

        active = null;

        mousePointSelection = new MousePointSelectionEvent(this, 0, 0);

        resizeRenderer = new BoxRenderer(parent);


        dirtyBoundsList = new Rectangle[0];
    }

    public void replaceMouseModule(MouseModule oldModule, MouseModule newModule) {
        JCheckBoxMenuItem j = (JCheckBoxMenuItem) primaryActionButtonMap.get(oldModule);
        primaryActionButtonMap.put(newModule, j);
        primaryActionButtonMap.remove(oldModule);
        secondaryActionButtonMap.put(newModule, secondaryActionButtonMap.get(oldModule));
        secondaryActionButtonMap.remove(oldModule);
        modules.removeElement(oldModule);
        modules.addElement(newModule);
    }

    /**
     * add a mouse module to the list of available modules.  If a module with the same
     * label exists already, it will be replaced.
     */
    public void addMouseModule(MouseModule module) {

        if (headless) {
            DasLogger.getLogger(DasLogger.GUI_LOG).info("not adding module since headless is true");

        } else {
            MouseModule preExisting = getModuleByLabel(module.getLabel());
            if (preExisting != null) {
                DasLogger.getLogger(DasLogger.GUI_LOG).info("Replacing mouse module " + module.getLabel() + ".");
                replaceMouseModule(preExisting, module);

            } else {

                modules.add(module);

                String name = module.getLabel();

                JCheckBoxMenuItem primaryNewItem = new JCheckBoxMenuItem(name);
                JCheckBoxMenuItem secondaryNewItem = new JCheckBoxMenuItem(name);

                primaryNewItem.addActionListener(popupListener);
                primaryNewItem.setActionCommand("primary");
                secondaryNewItem.addActionListener(popupListener);
                secondaryNewItem.setActionCommand("secondary");

                primaryActionButtonMap.put(module, primaryNewItem);
                secondaryActionButtonMap.put(module, secondaryNewItem);

                // insert the check box after the separator, and at the end of the actions list.
                primaryPopup.add(primaryNewItem, numInserted + 1 + primaryActionButtonMap.size() - 1);
                secondaryPopup.add(secondaryNewItem, numInserted + 1 + secondaryActionButtonMap.size() - 1);

            }
        }
    }

    public KeyAdapter getKeyAdapter() {
        return new KeyAdapter() {

            public void keyPressed(KeyEvent ev) {
                log.finest("keyPressed ");
                if (ev.getKeyCode() == KeyEvent.VK_ESCAPE & active != null) {
                    active = null;
                    getGlassPane().setDragRenderer(null, null, null);
                    parent.getCanvas().paintImmediately(0, 0, parent.getCanvas().getWidth(), parent.getCanvas().getHeight());
                    refresh();
                    ev.consume();
                } else if (ev.getKeyCode() == KeyEvent.VK_SHIFT) {
                    drawControlPoints = true;
                    parent.repaint();
                } else if (ev.getKeyChar() == 'p') {
                    pinned = true;
                    ev.consume();
                } else {
                    if (active == null) {
                        return;
                    }
                    for (int i = 0; i < active.size(); i++) {
                        ((MouseModule) active.get(i)).keyPressed(ev);
                    }
                }
            }

            public void keyReleased(KeyEvent ev) {
                if (ev.getKeyCode() == KeyEvent.VK_SHIFT) {
                    drawControlPoints = false;
                    parent.repaint();
                }
                if (active == null) {
                    return;
                }
                for (int i = 0; i < active.size(); i++) {
                    ((MouseModule) active.get(i)).keyReleased(ev);
                }
            }

            public void keyTyped(KeyEvent ev) {
                if (active == null) {
                    return;
                }
                for (int i = 0; i < active.size(); i++) {
                    ((MouseModule) active.get(i)).keyTyped(ev);
                }
            }
        };
    }

    public MouseModule getPrimaryModule() {
        ArrayList activ = new ArrayList();
        for (int i = 0; i < modules.size(); i++) {
            JCheckBoxMenuItem j = (JCheckBoxMenuItem) primaryActionButtonMap.get(modules.get(i));
            if (j.isSelected()) {
                activ.add(modules.get(i));
            }
        }
        return (MouseModule) activ.get(0); // at one time we allowed multiple modules at once.
    }

    public MouseModule getSecondaryModule() {
        ArrayList activ = new ArrayList();
        for (int i = 0; i < modules.size(); i++) {
            JCheckBoxMenuItem j = (JCheckBoxMenuItem) secondaryActionButtonMap.get(modules.get(i));
            if (j.isSelected()) {
                activ.add(modules.get(i));
            }
        }
        return (MouseModule) activ.get(0); // at one time we allowed multiple modules at once.
    }

    /**
     * set the primary module, the module receiving left-button events, to the
     * module provided.  If the module is not already loaded, implicitly addMouseModule
     * is called.
     */
    public void setPrimaryModule(MouseModule module) {
        if (headless) {
            return;
        }
        JCheckBoxMenuItem j = (JCheckBoxMenuItem) primaryActionButtonMap.get(module);
        if (j == null) {
            addMouseModule(module);
        }
        for (Iterator i = primaryActionButtonMap.entrySet().iterator(); i.hasNext();) {
            try {
                Object ii = ((Map.Entry) i.next()).getValue();
                ((JCheckBoxMenuItem) ii).setSelected(false);
            } catch (RuntimeException e) {
                e.printStackTrace();
                throw e;
            }
        }

        j = (JCheckBoxMenuItem) primaryActionButtonMap.get(module);
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
     */
    public void setSecondaryModule(MouseModule module) {
        if (headless) {
            return;
        }
        JCheckBoxMenuItem j = (JCheckBoxMenuItem) secondaryActionButtonMap.get(module);
        if (j == null) {
            addMouseModule(module);
        }
        for (Iterator i = secondaryActionButtonMap.entrySet().iterator(); i.hasNext();) {
            try {
                Object ii = ((Map.Entry) i.next()).getValue();
                ((JCheckBoxMenuItem) ii).setSelected(false);
            } catch (RuntimeException e) {
                e.printStackTrace();
                throw e;
            }
        }

        j = (JCheckBoxMenuItem) secondaryActionButtonMap.get(module);
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
    private JPopupMenu createPopup() {
        JPopupMenu popup = new JPopupMenu();
        popupListener = createPopupMenuListener();

        Action[] componentActions = parent.getActions();
        for (int iaction = 0; iaction < componentActions.length; iaction++) {
            JMenuItem item = new JMenuItem();
            item.setAction(componentActions[iaction]);
            popup.add(item);
        }
        numInserted = componentActions.length;

        popup.addSeparator();
        // mouse modules go here
        popup.addSeparator();

        Action[] canvasActions = DasCanvas.getActions();
        for (int iaction = 0; iaction < canvasActions.length; iaction++) {
            JMenuItem item = new JMenuItem();
            item.setAction(canvasActions[iaction]);
            popup.add(item);
        }

        return popup;
    }

    private ActionListener createPopupMenuListener() {
        return new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                DasMouseInputAdapter outer = DasMouseInputAdapter.this; // useful for debugging
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
                    if (primarySelectedItem != null) {
                        primarySelectedItem.setSelected(false);
                    }
                    for (int i = 0; i < modules.size(); i++) {
                        JCheckBoxMenuItem j = (JCheckBoxMenuItem) primaryActionButtonMap.get(modules.get(i));
                        if (j.isSelected()) {
                            primarySelectedItem = j;
                            break;
                        }
                    }
                    primarySelectedItem.setSelected(true); // for case when selection wasn't changed.
                //primaryPopup.show( parent, l.x, l.y );
                } else if (command.equals("secondary")) {
                    if (secondarySelectedItem != null) {
                        secondarySelectedItem.setSelected(false);
                    }
                    Point l = secondaryPopupLocation;
                    for (int i = 0; i < modules.size(); i++) {
                        JCheckBoxMenuItem j = (JCheckBoxMenuItem) secondaryActionButtonMap.get(modules.get(i));
                        if (j.isSelected()) {
                            secondarySelectedItem = j;
                            break;
                        }
                    }
                //secondaryPopup.show( parent, l.x, l.y );
                } else {
                    edu.uiowa.physics.pw.das.util.DasDie.println("" + command);
                }
            }
        };
    }

    /**
     * call the renderDrag method of the active module's dragRenderer.  This method
     * returns an array of Rectangles, or null, indicating the affected regions.
     * It's also permisable for a array element to be null.
     */
    private void renderSelection(Graphics2D g2d) {
        try {
            //DasCanvas canvas = parent.getCanvas();
            //selectionStart = SwingUtilities.convertPoint(canvas, dSelectionStart, parent);
            //selectionEnd = SwingUtilities.convertPoint(canvas, dSelectionEnd, parent);

            for (int i = 0; i < active.size(); i++) {
                DragRenderer dr = ((MouseModule) active.get(i)).getDragRenderer();

                //Rectangle[] dd = dr.renderDrag( getGlassPane().getGraphics(), dSelectionStart, dSelectionEnd);
                //dirtyBoundsList = new Rectangle[dd.length];
                //for (i = 0; i < dd.length; i++) {
                //    dirtyBoundsList[i] = new Rectangle(dd[i]);
                //}
                getGlassPane().setDragRenderer(dr, dSelectionStart, dSelectionEnd);
            }
        } catch (RuntimeException e) {
            DasExceptionHandler.handle(e);
        }
    }

    /* This attempts to redraw just the affected portions of parent.  Presently it
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
            for (int i = 0; i < dd.length; i++) {
                if (dd[i] != null) {
                    parent.getCanvas().paintImmediately(dd[i]);
                }
            }
            for (int i = 0; i < dirtyBoundsList.length; i++) {
                if (dirtyBoundsList[i] != null) {
                    parent.getCanvas().paintImmediately(dirtyBoundsList[i]);
                }
            }
        } else {
            if (active != null) {
                parent.getCanvas().paintImmediately(0, 0, parent.getCanvas().getWidth(), parent.getCanvas().getHeight());
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
        //g= (Graphics2D)getGlassPane().getGraphics();
        //g.translate(parent.getX(),parent.getY());

        g.translate(-parent.getX(), -parent.getY());

        if (active != null) {
            renderSelection(g);
        }
        if (hasFocus && hoverHighlite) {
            g.setColor(new Color(255, 0, 0, 10));
            g.setStroke(new BasicStroke(10));
            g.draw(parent.getBounds());
            return;
        }
        if (hasFocus && drawControlPoints) {
            drawControlPoints(g);
        }
    }

    private void drawControlPoints(Graphics2D g) {
        if (parent.getRow() != DasRow.NULL && parent.getColumn() != DasColumn.NULL) {
            int xLeft = parent.getColumn().getDMinimum();
            int xRight = parent.getColumn().getDMaximum();
            int yTop = parent.getRow().getDMinimum();
            int yBottom = parent.getRow().getDMaximum();

            Graphics2D gg = (Graphics2D) g.create();

            //gg.translate(-parent.getX(),-parent.getY());
            gg.setColor(new Color(0, 0, 0, 255));

            int ss = 9;
            gg.fillRect(xLeft + 1, yTop + 1, ss - 2, ss - 2);
            gg.fillRect(xRight - ss + 1, yTop + 1, ss - 2, ss - 2);
            gg.fillRect(xLeft + 1, yBottom - ss + 1, ss - 2, ss - 2);
            gg.fillRect(xRight - ss + 1, yBottom - ss + 1, ss - 2, ss - 2);

            gg.setColor(new Color(255, 255, 255, 100));
            gg.drawRect(xLeft, yTop, ss, ss);
            gg.drawRect(xRight - ss, yTop, ss, ss);
            gg.drawRect(xLeft, yBottom - ss, ss, ss);
            gg.drawRect(xRight - ss, yBottom - ss, ss, ss);

            int xmid = (xLeft + xRight) / 2;
            int ymid = (yTop + yBottom) / 2;

            int rr = 4;
            g.setColor(new Color(255, 255, 255, 100));
            gg.fillOval(xmid - rr - 1, ymid - rr - 1, rr * 2 + 3, rr * 2 + 3);

            gg.setColor(new Color(0, 0, 0, 255));

            gg.drawOval(xmid - rr, ymid - rr, rr * 2, rr * 2);
            gg.fillOval(xmid - 1, ymid - 1, 3, 3);

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

        if (!(parent instanceof DasAxis)) {
            if ((e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) == MouseEvent.SHIFT_DOWN_MASK) {
                if (xLeftSide) {
                    if (yTopSide) {
                        result = MouseMode.resize;
                        cursor = new Cursor(Cursor.NW_RESIZE_CURSOR);
                    } else if (yBottomSide) {
                        result = MouseMode.resize;
                        cursor = new Cursor(Cursor.SW_RESIZE_CURSOR);
                    }
                } else if (xRightSide) {
                    if (yTopSide) {
                        result = MouseMode.resize;
                        cursor = new Cursor(Cursor.NE_RESIZE_CURSOR);
                    } else if (yBottomSide) {
                        result = MouseMode.resize;
                        cursor = new Cursor(Cursor.SE_RESIZE_CURSOR);
                    }
                } else if (xMiddle && yMiddle) {
                    result = MouseMode.move;
                    cursor = new Cursor(Cursor.MOVE_CURSOR);
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
            result.moveStart.translate(-parent.getX(), -parent.getY());
        }

        if (result != mouseMode) {
            getGlassPane().setCursor(cursor);
        }
        return result;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        log.finest("mouseMoved");
        Point l = parent.getLocation();
        xOffset = l.x;
        yOffset = l.y;

        boolean drawControlPoints0 = this.drawControlPoints;
        if ((e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) == MouseEvent.SHIFT_DOWN_MASK) {
            drawControlPoints = true;
        } else {
            drawControlPoints = false;
        }

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
        log.finest("showPopup");
        HashMap map = null;
        if (menu == primaryPopup) {
            map = primaryActionButtonMap;
        } else if (menu == secondaryPopup) {
            map = secondaryActionButtonMap;
        } else {
            throw new IllegalArgumentException("menu must be primary or secondary popup menu");
        }
        for (Iterator i = modules.iterator(); i.hasNext();) {
            MouseModule mm = (MouseModule) i.next();
            JCheckBoxMenuItem j = (JCheckBoxMenuItem) primaryActionButtonMap.get(mm);
            j.setText(mm.getLabel());
        }
        menu.show(ev.getComponent(), ev.getX(), ev.getY());
    }

    public void setPinned(boolean b) {
        pinned = b;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        log.finer("mousePressed " + mouseMode);
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

	Point cp= new Point( e.getPoint() );
	cp.translate(xOffset, yOffset );
	if ( !parent.acceptContext(cp.x, cp.y ) ) {
	    return;
	}
        if (mouseMode == MouseMode.resize) {
            resizeStart = new Point(0, 0);
            if (mouseMode.resizeRight) {
                resizeStart.x = 0 + xOffset;
            } else if (mouseMode.resizeLeft) {
                resizeStart.x = parent.getWidth() + xOffset;
            }
            if (mouseMode.resizeTop) {
                resizeStart.y = parent.getHeight() + yOffset;
            } else if (mouseMode.resizeBottom) {
                resizeStart.y = 0 + yOffset;
            }

        } else if (mouseMode == MouseMode.move) {
            mouseMode.moveStart = e.getPoint();
            mouseMode.moveStart.translate(xOffset, yOffset);

        } else {
            if (active == null) {
                button = e.getButton();
                //selectionStart = e.getPoint();
                dSelectionStart = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), parent.getCanvas());
                //selectionEnd = e.getPoint();
                dSelectionEnd = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), parent.getCanvas());
                //graphics = (Graphics2D) parent.getGraphics();

                if (e.isControlDown() || button == MouseEvent.BUTTON3) {
                    if (button == MouseEvent.BUTTON1 || button == MouseEvent.BUTTON3) {
                        showPopup(primaryPopup, e);
                    } else {
                        showPopup(secondaryPopup, e);
                    }
                } else {

                    active = new Vector();

                    if (button == MouseEvent.BUTTON1 || button == MouseEvent.BUTTON3) {
                        for (int i = 0; i < modules.size(); i++) {
                            JCheckBoxMenuItem j = (JCheckBoxMenuItem) primaryActionButtonMap.get(modules.get(i));
                            if (j.isSelected()) {
                                active.add(modules.get(i));
                            }
                        }
                    } else {
                        for (int i = 0; i < modules.size(); i++) {
                            JCheckBoxMenuItem j = (JCheckBoxMenuItem) secondaryActionButtonMap.get(modules.get(i));
                            if (j.isSelected()) {
                                active.add(modules.get(i));
                            }
                        }
                    }

                    mouseMode = MouseMode.moduleDrag;

                    mousePointSelection.set(e.getX() + xOffset, e.getY() + yOffset);
                    for (int i = 0; i < active.size(); i++) {
                        MouseModule j = (MouseModule) active.get(i);
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
        log.finest("mouseDragged in " + mouseMode);
        if (mouseMode == MouseMode.resize) {
            Point p = e.getPoint();
            p.translate(parent.getX(), parent.getY());
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
        //resizeRenderer.clear(graphics);
        //resizeRenderer.renderDrag(graphics, p1, p2);
        } else {
            if (active != null) {
                //clearSelection(graphics);
                //selectionEnd = e.getPoint();
                dSelectionEnd = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), parent.getCanvas());

                mousePointSelection.set((int) dSelectionEnd.getX(), (int) dSelectionEnd.getY());
                for (int i = 0; i < active.size(); i++) {
                    try {
                        MouseModule j = (MouseModule) active.get(i);
                        if (j.dragRenderer.isPointSelection()) {
                            log.finest("mousePointSelected");
                            j.mousePointSelected(mousePointSelection);
                        }
                        if (j.dragRenderer.isUpdatingDragSelection()) {
                            // Really it should be the DMM that indicates it wants updates...whoops...
                            MouseDragEvent de = j.dragRenderer.getMouseDragEvent(parent, dSelectionStart, dSelectionEnd, e.isShiftDown());
                            log.finest("mouseRangeSelected");
                            j.mouseRangeSelected(de);
                        }
                        j.mouseDragged(e);
                    } catch (RuntimeException except) {
                        DasExceptionHandler.handle(except);
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

    public void mouseReleased(MouseEvent e) {
        log.finest("mouseReleased");
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
                    //clearSelection(graphics);
                    int x = e.getX();
                    int y = e.getY();
                    for (int i = 0; i < active.size(); i++) {
                        MouseModule j = (MouseModule) active.get(i);
                        try {
                            MouseDragEvent de =
                                    j.dragRenderer.getMouseDragEvent(parent, dSelectionStart, dSelectionEnd, e.isShiftDown());
                            j.mouseRangeSelected(de);
                        } catch (RuntimeException ex) {
                            DasExceptionHandler.handle(ex);
                        } finally {
                            button = 0;
                            try {
                                j.mouseReleased(e);
                            } catch (RuntimeException ex2) {
                                DasExceptionHandler.handle(ex2);
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

    public void removeMouseModule(MouseModule module) {
        // not implemented yet
    }

    /**
     * Getter for property mouseModules.
     * @return Value of property mouseModules.
     */
    public MouseModule getMouseModule(int i) {
        return (MouseModule) modules.get(i);
    }

    public MouseModule[] getMouseModules() {
        MouseModule[] result = new MouseModule[modules.size()];
        modules.copyInto(result);
        return result;
    }

    public String getPrimaryModuleLabel() {
        MouseModule primary = getPrimaryModule();
        return primary == null ? "" : primary.getLabel();
    }

    public void setPrimaryModuleByLabel(String label) {
        MouseModule mm = getModuleByLabel(label);
        if (mm != null) {
            setPrimaryModule(mm);
        }
    }

    public String getSecondaryModuleLabel() {
        MouseModule secondary = getPrimaryModule();
        return secondary == null ? "" : secondary.getLabel();
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
     * @param mouseModule the new mouseModule to use.
     */
    public void setMouseModule(int i, MouseModule mouseModule) {
        this.modules.set(i, mouseModule);
    }

    public void mouseEntered(MouseEvent e) {
        hasFocus = true;
        if (e.isShiftDown()) {
            parent.repaint();
        }
        if (primary != null) {
            getGlassPane().setCursor(primary.getCursor());
        }
    }

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
    public void removeMenuItem(String label) {
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
        }

    }

    public void addMenuItem( final Component b ) {
        //SwingUtilities.invokeLater(new Runnable() {
        //    public void run() {
                if (DasMouseInputAdapter.this.headless) {
                    return;
                }
                if (numInserted == 0) {
                    primaryPopup.insert(new JPopupMenu.Separator(), 0);
                }
                primaryPopup.insert(b, numInserted);
                numInserted++;
        //    }
        //});

    }

    /**
     * return a menu with font to match LAF.
     * @param label
     * @return
     */
    public JMenu addMenu( String label ) {
        JMenu result= new JMenu(label);
        //result.setFont(primaryPopup.getFont());
        addMenuItem(result);
        return result;
    }

    private DasCanvas.GlassPane getGlassPane() {
        DasCanvas.GlassPane r= (DasCanvas.GlassPane) ((DasCanvas) parent.getParent()).getGlassPane();
        if ( r.isVisible()==false ) r.setVisible(true);
        return r;
    }

    public MouseModule getModuleByLabel(java.lang.String label) {
        MouseModule result = null;
        for (int i = 0; i < modules.size(); i++) {
            if (label.equals(((MouseModule) modules.get(i)).getLabel())) {
                result = (MouseModule) modules.get(i);
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

    public void setHoverHighlite(boolean value) {
        this.hoverHighlite = value;
    }

    /**
     * returns the position of the last mouse press.  This is a hack so that
     * the mouse position can be obtained to get the context of the press.
     * The result point is in the parent's coordinate system.
     */
    public Point getMousePressPosition() {
        return this.pressPosition;
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

        min =
                parent.getRow().getDMinimum();
        max =
                parent.getRow().getDMaximum();
        parent.getRow().setDPosition(min + dy, max + dy);
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        if (secondary != null) {
            secondary.mouseWheelMoved(e);
        }
    }
}
