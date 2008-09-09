/* File: Toolbox.java
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

package org.das2.components;

import org.das2.dasml.FormTab;
import org.das2.dasml.FormChoice;
import org.das2.dasml.FormList;
import org.das2.dasml.FormPanel;
import org.das2.dasml.FormText;
import org.das2.dasml.FormRadioButtonGroup;
import org.das2.dasml.FormRadioButton;
import org.das2.dasml.FormWindow;
import org.das2.dasml.FormTextField;
import org.das2.dasml.FormCheckBox;
import org.das2.dasml.TransferableFormComponent;
import org.das2.dasml.FormButton;
import edu.uiowa.physics.pw.das.graph.*;
import edu.uiowa.physics.pw.das.graph.dnd.TransferableCanvas;
import edu.uiowa.physics.pw.das.graph.dnd.TransferableCanvasComponent;
import edu.uiowa.physics.pw.das.graph.dnd.TransferableRenderer;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.URL;

/**
 * A component that allows the user to create new objects and pass them to
 * components using the drag and drop interface.  The objects that can be
 * created are represented by icons.  The icons are grouped by tabs for each\
 * group.
 *
 * @author  Edward West
 */
public class Toolbox extends JTabbedPane {
    
    
    
    
    /** Image used to combine with Icons to create a pointer */
    private static Image pointerOverlay;
    
    
    
    
    /** Dummy component used for loading images and creating pointer images */
    private static Component dummy = new Component(){};
    
    
    
    
    /** initialize comp and load pointer image */
    static {
        /** get image from jar resource */
        Class cl = ToolComponent.class;
        URL pointerURL = cl.getResource("/images/toolbox/dragpointer.gif");
        Image image = Toolkit.getDefaultToolkit().getImage(pointerURL);
        /** Make sure the whole image is loaded */
        MediaTracker tracker = new MediaTracker(dummy);
        tracker.addImage(image, 0);
        try {
            tracker.waitForAll();
        }
        catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }
        pointerOverlay = image;
    }
    
    
    
    
    /** Creates a new instance of Toolbox */
    public Toolbox() {
        initializeFormToolComponent();
        initializeGraphToolComponent();
    }
    
    
    
    /** initializes the ToolComponent containing icons for form elements */
    private void initializeFormToolComponent() {
        String[] ids = {
            "form tab",
            "window",
            "panel",
            "static text",
            "text field",
            "button",
            "check box",
            "button group",
            "radio button",
            "choice",
            "list"
        };
        Class c = Toolbox.class;
        Icon[] icons = {
            new ImageIcon(c.getResource("/images/toolbox/tab.gif")),
            new ImageIcon(c.getResource("/images/toolbox/window.gif")),
            new ImageIcon(c.getResource("/images/toolbox/panel.gif")),
            new ImageIcon(c.getResource("/images/toolbox/text.gif")),
            new ImageIcon(c.getResource("/images/toolbox/textfield.gif")),
            new ImageIcon(c.getResource("/images/toolbox/button.gif")),
            new ImageIcon(c.getResource("/images/toolbox/checkbox.gif")),
            new ImageIcon(c.getResource("/images/toolbox/buttongroup.gif")),
            new ImageIcon(c.getResource("/images/toolbox/radiobutton.gif")),
            new ImageIcon(c.getResource("/images/toolbox/choice.gif")),
            //new ImageIcon(l.getResource("/images/toolbox/list.png")),
        };
        
        ToolComponent tc = new ToolComponent(ids, icons, 4);
        add("Form", tc);
    }
    
    
    
    
    /** initializes the ToolComponent containing icons for graph elements */
    private void initializeGraphToolComponent() {
        String[] ids = {
            "canvas",
            "plot",
            "axis",
            "time axis",
            "spectrogram renderer",
            "line plot renderer",
            "spectrogram plot"
        };
        Class c = Toolbox.class;
        Icon[] icons = {
            new ImageIcon(c.getResource("/images/toolbox/canvas.gif")),
            new ImageIcon(c.getResource("/images/toolbox/plot.gif")),
            new ImageIcon(c.getResource("/images/toolbox/axis.gif")),
            new ImageIcon(c.getResource("/images/toolbox/taxis.gif")),
            new ImageIcon(c.getResource("/images/toolbox/spectrogram.gif")),
            new ImageIcon(c.getResource("/images/toolbox/line.gif")),
            new ImageIcon(c.getResource("/images/toolbox/spectrogram_plot.gif"))
        };
        ToolComponent tc = new ToolComponent(ids, icons, 4);
        add("Graph", tc);
    }
    
    
    
    
    
    /** Creates a transferable based on the String parameter */
    private static Transferable createTransferable(String id) {
            if (id.equals("form tab")) {
                return new TransferableFormComponent(
                    new FormTab(null, "label"));
            }
            else if (id.equals("window")) {
                return new TransferableFormComponent(
                    new FormWindow(null, "title", 640, 480));
            }
            else if (id.equals("panel")) {
                return new TransferableFormComponent(new FormPanel());
            }
            else if (id.equals("static text")) {
                return new TransferableFormComponent(new FormText());
            }
            else if (id.equals("text field")) {
                return new TransferableFormComponent(new FormTextField(null));
            }
            else if (id.equals("button")) {
                return new TransferableFormComponent(
                    new FormButton(null, "label"));
            }
            else if (id.equals("check box")) {
                return new TransferableFormComponent(
                    new FormCheckBox(null, "label"));
            }
            else if (id.equals("button group")) {
                return new TransferableFormComponent(
                    new FormRadioButtonGroup());
            }
            else if (id.equals("radio button")) {
                return new TransferableFormComponent(
                    new FormRadioButton(null, "label"));
            }
            else if (id.equals("choice")) {
                return new TransferableFormComponent(new FormChoice(null));
            }
            else if (id.equals("list")) {
                return new TransferableFormComponent(new FormList(null));
            }
            else if (id.equals("canvas")) {
                return new TransferableCanvas(
                    DasCanvas.createFormCanvas(null, 640, 480));
            }
            else if (id.equals("plot")) {
                return new TransferableCanvasComponent(
                    DasPlot.createNamedPlot(null));
            }
            else if (id.equals("axis")) {
                return new TransferableCanvasComponent(
                    DasAxis.createNamedAxis(null));
            }
            else if (id.equals("time axis")) {
                return new TransferableCanvasComponent(
                    DasAxis.createNamedAxis(null));
            }
            else if (id.equals("spectrogram renderer")) {
                DasColorBar cb = DasColorBar.createNamedColorBar(null);
                return new TransferableRenderer(
                    new SpectrogramRenderer(null, cb));
            }
            else if (id.equals("line plot renderer")) {
                return new TransferableRenderer(
                    new SymbolLineRenderer());
            }
            else if (id.equals("spectrogram plot")) {
                DasPlot plot = DasPlot.createNamedPlot(null);
                DasColorBar colorBar = DasColorBar.createNamedColorBar(
                    plot.getDasName() + "_colorbar");
                SpectrogramRenderer renderer
                    = new SpectrogramRenderer(null, colorBar);
                plot.addRenderer(renderer);
                return new TransferableCanvasComponent(plot);
            }
            else {
                throw new IllegalArgumentException(id);
            }
    }
    
    
    
    /** Creates an array of cursors to be used for drag and drop operations */
    private static Cursor[] getCursors(String[] ids, Icon[] icons) {
        Cursor[] cursors = new Cursor[icons.length];
        Point origin = new Point(0, 0);
        for (int i = 0; i < cursors.length; i++) {
            int width = icons[i].getIconWidth();
            int height = icons[i].getIconHeight();
            BufferedImage cimage = new BufferedImage(32, 32,
                BufferedImage.TYPE_INT_ARGB);
            Graphics g = cimage.getGraphics();
            icons[i].paintIcon(dummy, g, 8, 8);
            g.drawImage(pointerOverlay, 0, 0, dummy);
            cursors[i] =Toolkit.getDefaultToolkit().createCustomCursor(
                cimage, origin, ids[i]);
        }
        return cursors;
    }
    
    
    
    /** DragGesture and DragSourceListener implementation used to initiate drag
     * and drop operations for ToolboxComponents.
     */
    private static class ToolboxDragGestureListener
        implements DragGestureListener, DragSourceListener {
        
        /** A <code>DragGestureRecognizer</code> has detected
         * a platform-dependent drag initiating gesture and
         * is notifying this listener
         * in order for it to initiate the action for the user.
         * <P>
         * @param dge the <code>DragGestureEvent</code> describing
         * the gesture that has just occurred
         */
        public void dragGestureRecognized(DragGestureEvent dge) {
            ToolComponent tc = (ToolComponent)dge.getComponent();
            int index = tc.selectedIndex;
            if (index >= 0) {
                Cursor dragCursor = tc.cursors[index];
                Transferable t = createTransferable(tc.ids[index]);
                dge.startDrag(dragCursor, t, this);
            }
        }
        
        /** This method is invoked to signify that the Drag and Drop
         * operation is complete. The getDropSuccess() method of
         * the <code>DragSourceDropEvent</code> can be used to
         * determine the termination state. The getDropAction() method
         * returns the operation that the drop site selected
         * to apply to the Drop operation. Once this method is complete, the
         * current <code>DragSourceContext</code> and
         * associated resources become invalid.
         *
         * @param dsde the <code>DragSourceDropEvent</code>
         */
        public void dragDropEnd(DragSourceDropEvent dsde) {
        }
        
        /** Called as the cursor's hotspot enters a platform-dependent drop site.
         * This method is invoked when all the following conditions are true:
         * <UL>
         * <LI>The cursor's hotspot enters the operable part of a platform-
         * dependent drop site.
         * <LI>The drop site is active.
         * <LI>The drop site accepts the drag.
         * </UL>
         *
         * @param dsde the <code>DragSourceDragEvent</code>
         */
        public void dragEnter(DragSourceDragEvent dsde) {
        }
        
        /** Called as the cursor's hotspot exits a platform-dependent drop site.
         * This method is invoked when any of the following conditions are true:
         * <UL>
         * <LI>The cursor's hotspot no longer intersects the operable part
         * of the drop site associated with the previous dragEnter() invocation.
         * </UL>
         * OR
         * <UL>
         * <LI>The drop site associated with the previous dragEnter() invocation
         * is no longer active.
         * </UL>
         * OR
         * <UL>
         * <LI> The current drop site has rejected the drag.
         * </UL>
         *
         * @param dse the <code>DragSourceEvent</code>
         */
        public void dragExit(DragSourceEvent dse) {
        }
        
        /** Called as the cursor's hotspot moves over a platform-dependent drop site.
         * This method is invoked when all the following conditions are true:
         * <UL>
         * <LI>The cursor's hotspot has moved, but still intersects the
         * operable part of the drop site associated with the previous
         * dragEnter() invocation.
         * <LI>The drop site is still active.
         * <LI>The drop site accepts the drag.
         * </UL>
         *
         * @param dsde the <code>DragSourceDragEvent</code>
         */
        public void dragOver(DragSourceDragEvent dsde) {
        }
        
        /** Called when the user has modified the drop gesture.
         * This method is invoked when the state of the input
         * device(s) that the user is interacting with changes.
         * Such devices are typically the mouse buttons or keyboard
         * modifiers that the user is interacting with.
         *
         * @param dsde the <code>DragSourceDragEvent</code>
         */
        public void dropActionChanged(DragSourceDragEvent dsde) {
        }
        
    }
    
    
    
    /** Displays a group of icons for the Toolbox component. */
    private static class ToolComponent extends JComponent {
        
        /** array of icons for this group */
        private Icon[] icons;
        
        
        
        /** array of ids for this group */
        private String[] ids;
        
        
        
        /** array of drag cursors for this group */
        private Cursor[] cursors;
        
        
        
        
        /** maximum number of icons per row */
        private int width;
        
        
        
        
        /** number of rows of icons */
        private int height;
        
        
        
        
        /** The index of the last icon that that was the recipient of a
         * mousePressed event
         */
        private int selectedIndex = -1;
        
        
        
        /** Create a new ToolComponent */
        private ToolComponent(String[] ids, Icon [] icons, int w) {
            this.ids = ids;
            this.icons = icons;
            this.cursors = getCursors(ids, icons);
            this.width = w;
            this.height = (int)Math.ceil((double)icons.length / (double)w);
            setBackground(Color.WHITE);
            setForeground(Color.BLACK);
            setTransferHandler(null);
            DragSource dragSource = DragSource.getDefaultDragSource();
            dragSource.createDefaultDragGestureRecognizer(this,
                DnDConstants.ACTION_COPY, new ToolboxDragGestureListener());
            addMouseListener(new InputListener());
            ToolTipManager.sharedInstance().registerComponent(this);
        }//public ToolComponent
        
        protected void paintComponent(Graphics g){
            Rectangle clip = g.getClipBounds();
            g.setColor(getBackground());
            if (clip == null) {
                g.fillRect(0, 0, getWidth(), getHeight());
            }
            else {
                g.fillRect(clip.x, clip.y, clip.width, clip.height);
            }
            g.setColor(getForeground());
            for(int index = 0; index < icons.length; index++) {
                int i = index % width;
                int j = index / width;
                int x = i * 32 + 7;
                int y = j * 32 + 7;
                icons[index].paintIcon(this,g, x, y);
                g.drawRect(x - 1, y - 1, 24, 24);
            }
        }//public void paintComponent(Graphics g)

        public Dimension getPreferredSize() {
            int w = width * 32 + 6;
            int h = height * 32 + 6;
            return new Dimension(w, h);
        }

        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
        }
        
        private int positionToIndex(int x, int y) {
            for (int index = 0; index < icons.length; index++) {
                int i = index % width;
                int j = index / width;
                int xi = i * 32 + 7;
                int yi = j * 32 + 7;
                if (x >= xi && x < xi + 24 && y >= yi && y < yi + 24) {
                    return index;
                }
            }
            return -1;
        }
        
        public String getToolTipText(MouseEvent event) {
            int index = positionToIndex(event.getX(), event.getY());
            if (index == -1) {
                return null;
            }
            else {
                return ids[index];
            }
        }
        
    }
    
    /** A listener to listen for mousePressed events on a ToolComponent */
    private static class InputListener extends MouseInputAdapter {
        public void mousePressed(MouseEvent e) {
            ToolComponent tc = (ToolComponent)e.getComponent();
            tc.selectedIndex = tc.positionToIndex(e.getX(), e.getY());
        }
    }
    
    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.getContentPane().add(new Toolbox(), "Center");
        frame.getContentPane().add(new JTextArea(10, 10), "West");
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
    
}
