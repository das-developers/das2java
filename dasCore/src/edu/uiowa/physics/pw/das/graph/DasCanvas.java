/* File: DasCanvas.java
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

package edu.uiowa.physics.pw.das.graph;

import edu.uiowa.physics.pw.das.components.PropertyEditor;
import edu.uiowa.physics.pw.das.dasml.FormBase;
import edu.uiowa.physics.pw.das.dasml.FormComponent;
import edu.uiowa.physics.pw.das.dasml.ParsedExpressionException;
import edu.uiowa.physics.pw.das.graph.dnd.TransferableCanvasComponent;
import edu.uiowa.physics.pw.das.util.DasPNGConstants;
import edu.uiowa.physics.pw.das.util.DasPNGEncoder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 *
 * @author  eew
 */
public class DasCanvas extends JLayeredPane implements Printable, PropertyEditor.Editable, FormComponent {
    
    public static final Integer DEFAULT_LAYER = JLayeredPane.DEFAULT_LAYER;
    public static final Integer PLOT_LAYER = new Integer(300);
    public static final Integer VERTICAL_AXIS_LAYER = new Integer(400);
    public static final Integer HORIZONTAL_AXIS_LAYER = new Integer(500);
    public static final Integer AXIS_LAYER = VERTICAL_AXIS_LAYER;
    public static final Integer ANNOTATION_LAYER = new Integer(1000);
    public static final Integer GLASS_PANE_LAYER = new Integer(30000);
    
    private static final Paint PAINT_ROW = new Color(0xff, 0xb2, 0xb2, 0x92);
    private static final Paint PAINT_COLUMN = new Color(0xb2, 0xb2, 0xff, 0x92);
    private static final Paint PAINT_SELECTION = Color.GRAY;
    
    private static final Stroke STROKE_DASHED;
    static {
        float thick = 3.0f;
        int cap = BasicStroke.CAP_SQUARE;
        int join = BasicStroke.JOIN_MITER;
        float[] dash = new float[] {thick * 4.0f, thick * 4.0f};
        STROKE_DASHED = new BasicStroke(thick, cap, join, thick, dash, 0.0f);
    }
    
    private final GlassPane glassPane;
    
    private String dasName;
    
    private JPopupMenu popup;
    
    private boolean editable;
    
    List devicePositionList = new ArrayList();
    
    edu.uiowa.physics.pw.das.util.DnDSupport dndSupport;
    
    /** Creates a new instance of DasCanvas */
    public DasCanvas() {
        LookAndFeel.installColorsAndFont(this, "Panel.background", "Panel.foreground", "Panel.font");
        setOpaque(true);
        setLayout(new RowColumnLayout());
        addComponentListener(createResizeListener());
        setBackground(Color.white);
        this.setDoubleBuffered(true);
        glassPane = new GlassPane();
        add(glassPane, GLASS_PANE_LAYER);
        
        popup= createPopupMenu();
        this.addMouseListener(createMouseInputAdapter());
        
        try {
            if (!"true".equals(System.getProperty("java.awt.headless"))) {
                dndSupport = new CanvasDnDSupport();
            }
        } catch ( SecurityException ex ) {
            dndSupport = new CanvasDnDSupport();
        }
    }
    
    private MouseInputAdapter createMouseInputAdapter() {
        return new MouseInputAdapter() {
            public void mousePressed(MouseEvent e) {
                Point primaryPopupLocation= e.getPoint();
                popup.show( DasCanvas.this, e.getX(), e.getY());
            }
        };
    }
    
    private ActionListener createPopupMenuListener() {
        return new ActionListener() {
            private JFileChooser pngFileChooser;
            private JPanel pngFileNamePanel;
            private JTextField pngFileTextField;
            
            public void actionPerformed(ActionEvent e) {
                String command = e.getActionCommand();
                if (command.equals("Das Properties")) {
                    edu.uiowa.physics.pw.das.DasProperties.showEditor();
                }
                else if (command.equals("print")) {
                    Printable p = DasCanvas.this.getPrintable();
                    PrinterJob pj = PrinterJob.getPrinterJob();
                    pj.setPrintable(p);
                    if (pj.printDialog()) {
                        try {
                            pj.print();
                        }
                        catch (PrinterException pe) {
                            Object[] message = {"Error printing", pe.getMessage() };
                            JOptionPane.showMessageDialog(null, message, "ERROR", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
                else if (command.equals("toPng")) {
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
                    String[] options = {"Save as PNG", "Cancel"};
                    int choice = JOptionPane.showOptionDialog(DasCanvas.this,
                    pngFileNamePanel,
                    "Write to PNG",
                    0,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    "Ok");
                    if (choice == 0) {
                        DasCanvas canvas = DasCanvas.this;
                        try {
                            canvas.writeToPng(pngFileTextField.getText());
                        }
                        catch (java.io.IOException ioe) {
                            edu.uiowa.physics.pw.das.util.DasExceptionHandler.handle(ioe);
                        }
                    }
                }
                else if (command.equals("pngBrowse")) {
                    int choice = pngFileChooser.showDialog(DasCanvas.this, "Select File");
                    if (choice == JFileChooser.APPROVE_OPTION) {
                        pngFileTextField.setText(pngFileChooser.getSelectedFile().getPath());
                    }
                } else {
                    edu.uiowa.physics.pw.das.util.DasDie.println(""+command);
                }
            }
        };
    }
    
    private JPopupMenu createPopupMenu() {
        JPopupMenu popup= new JPopupMenu();
        
        ActionListener popupListener = createPopupMenuListener();
        
        JMenuItem properties = new JMenuItem("Das Properties");
        properties.addActionListener(popupListener);
        properties.setToolTipText("edit global properties");
        popup.add(properties);
        popup.addSeparator();
        JMenuItem print = new JMenuItem("print...");
        print.setActionCommand("print");
        print.addActionListener(popupListener);
        print.setToolTipText("print entire canvas");
        popup.add(print);
        JMenuItem toPng = new JMenuItem("save as PNG...");
        toPng.setActionCommand("toPng");
        toPng.setToolTipText("save canvas to png image file");
        toPng.addActionListener(popupListener);
        popup.add(toPng);
        popup.addSeparator();
        JMenuItem x = new JMenuItem("close");
        x.addActionListener(popupListener);
        x.setToolTipText("close this popup");
        popup.add(x);
        
        return popup;
    }
    
    public Component getGlassPane() {
        return glassPane;
    }
    
    public List getDevicePositionList() {
        return Collections.unmodifiableList(devicePositionList);
    }
    
    private int displayLockCount = 0;
    private Object displayLockObject = new String("DISPLAY_LOCK_OBJECT");
    
    /**
     * Lock the display for this canvas.  All Mouse and Key events are captured
     * and swallowed by the glass pane.
     *
     * @param o the Object requesting the lock.
     * @see #freeDisplay(Object);
     */
    synchronized void lockDisplay(Object o) {
        synchronized (displayLockObject) {
            displayLockCount++;
            if (displayLockCount == 1) {
                glassPane.setBlocking(true);
            }
        }
    }
    
    /**
     * Frees the lock the specified object has requested on the display.
     * The display will not be freed until all locks have been freed.
     *
     * @param o the object releasing it's lock on the display
     * @see #lockDisplay(Object)
     */
    synchronized void freeDisplay(Object o) {
        synchronized (displayLockObject) {
            displayLockCount--;
            if (displayLockCount == 0) {
                glassPane.setBlocking(false);
                displayLockObject.notifyAll();
            }
        }
    }
    
    /** Creates a new instance of DasCanvas with the specified width and height
     *
     * @param width The width of the DasCanvas
     * @param height The height of the DasCanvas
     */
    public DasCanvas(int width, int height) {
        this();
        setPreferredSize(new Dimension(width, height));
    }
    
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }
    
    protected void paintComponent(Graphics g) {
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
    }
    
    public int print(Graphics printGraphics, PageFormat format, int pageIndex) {
        
        if (pageIndex != 0) return NO_SUCH_PAGE;
        
        Graphics2D g2 = (Graphics2D)printGraphics;
        double canvasWidth = (double)getWidth();
        double canvasHeight = (double)getHeight();
        double printableWidth = format.getImageableWidth();
        double printableHeight = format.getImageableHeight();
        
        g2.translate(format.getImageableX(), format.getImageableY());
        
        double canvasMax = Math.max(canvasWidth, canvasHeight);
        double canvasMin = Math.min(canvasWidth, canvasHeight);
        double printableMax = Math.max(printableWidth, printableHeight);
        double printableMin = Math.min(printableWidth, printableHeight);
        
        double maxScaleFactor = printableMax/canvasMax;
        double minScaleFactor = printableMin/canvasMin;
        double scaleFactor = Math.min(maxScaleFactor, minScaleFactor);
        g2.scale(scaleFactor, scaleFactor);
        
        if ((canvasWidth==canvasMax)^(printableWidth==printableMax)) {
            g2.rotate(Math.PI/2.0);
            g2.translate(0.0, -canvasHeight);
        }
        
        print(g2);
        
        return PAGE_EXISTS;
        
    }
    
    protected static class RowColumnLayout implements LayoutManager {
        public void layoutContainer(Container target) {
            synchronized (target.getTreeLock()) {
                int count = target.getComponentCount();
                for (int i = 0; i < count; i++) {
                    Component c = target.getComponent(i);
                    if (c instanceof DasCanvasComponent) {
                        ((DasCanvasComponent)c).update();
                    }
                    else if (c == ((DasCanvas)target).glassPane) {
                        Dimension size = target.getSize();
                        c.setBounds(0, 0, size.width, size.height);
                    }
                }
            }
        }
        
        public Dimension minimumLayoutSize(Container target) {
            synchronized (target.getTreeLock()) {
                int count = target.getComponentCount();
                Rectangle r = new Rectangle(0, 0, 0, 0);
                for (int i = 0; i < count; i++) {
                    r.add(target.getComponent(i).getBounds());
                }
                return new Dimension(r.width, r.height);
            }
        }
        
        public Dimension preferredLayoutSize(Container target) {
            return minimumLayoutSize(target);
        }
        
        public void addLayoutComponent(String name, Component comp){}
        public void removeLayoutComponent(Component comp){}
    }
    
    
    /** Returns an instance of <code>java.awt.print.Printable</code> that can
     * be used to render this canvas to a printer.  The current implementation
     * returns a reference to this canvas.  This method is provided so that in
     * the future, the canvas can delegate it's printing to another object.
     * @return a <code>Printable</code> instance for rendering this component.
     */
    public Printable getPrintable() {
        return this;
    }
    
    /**
     * Queues an event to the AWT Event Queue that will invoke
     * {@link #writeToPngImmediately(java.lang.String)} when it
     * is processed
     *
     * @param filename the specified filename
     * @throws IOException if there is an error opening the file for writing
     */
    public void writeToPng(String filename) throws IOException {
        final FileOutputStream out = new FileOutputStream(filename);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                writeToPngImmediately(out);
            }
        });
    }
    
    /**
     * Writes the contents of this canvas to the specified
     * FileOutputStream.
     *
     * @param out the specified FileOutputStream
     */
    protected void writeToPngImmediately(FileOutputStream out) {
        try {
            synchronized(displayLockObject) {
                if (displayLockCount != 0) {
                    displayLockObject.wait();
                }
            }
        }
        catch (InterruptedException ie) {
        }
        BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        print(image.getGraphics());
        DasPNGEncoder encoder = new DasPNGEncoder();
        encoder.addText(DasPNGConstants.KEYWORD_CREATION_TIME, new Date().toString());
        try {
            encoder.write(image, out);
        }
        catch (IOException ioe) {}
        finally {
            try { out.close(); } catch (IOException ioe) {}
        }
    }
    
    public Image getImage(int width, int height) {
        setPreferredWidth(width);
        setPreferredHeight(height);
        
        if ( "true".equals(System.getProperty("java.awt.headless"))) {
            this.addNotify();
            this.setSize(getPreferredSize());
            this.validate();
        }
        final Image image= new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        
        if ( EventQueue.isDispatchThread() ) {
            writeToImageImmediately(image);
        } else {
            try {
                synchronized(image) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            writeToImageImmediately(image);
                            synchronized(image) {
                                image.notifyAll();
                            }
                        }
                    });
                    image.wait();
                }
            }
            catch ( InterruptedException ex ) {
            }            
        }
        return image;
    }

    protected void writeToImageImmediately(Image image) {
        try {
            synchronized(displayLockObject) {
                if (displayLockCount != 0) {
                    displayLockObject.wait();
                }
            }
        } catch ( InterruptedException ex ) {
        }
        print(image.getGraphics());
        
    }
    
    /** This methods adds the specified <code>DasCanvasComponent</code> to this canvas.
     * @param c the component to be added to this canvas
     */
    public void addCanvasComponent(DasCanvasComponent c) {
        add(c);
    }
    
    protected void addImpl(Component comp, Object constraints, int index) {
        if (comp == null) {
            edu.uiowa.physics.pw.das.util.DasDie.println("NULL COMPONENT");
            Thread.dumpStack();
            return;
        }
        if (index < 0) index = 0;
        if (comp instanceof DasPlot) {
            ((DasPlot)comp).putClientProperty(LAYER_PROPERTY, PLOT_LAYER);
        }
        else if (comp instanceof DasAxis) {
            ((DasAxis)comp).putClientProperty(LAYER_PROPERTY, AXIS_LAYER);
        }
        //            else if (comp instanceof DasAnnotation) {
        //                ((DasLabel)comp).putClientProperty(LAYER_PROPERTY, ANNOTATION_LAYER);
        //            }
        else if (comp instanceof JComponent) {
            ((JComponent)comp).putClientProperty(LAYER_PROPERTY, DEFAULT_LAYER);
        }
        super.addImpl(comp, constraints, index);
        if (comp instanceof DasCanvasComponent) {
            ((DasCanvasComponent)comp).installComponent();
        }
    }
    
    /**
     * Sets the preferred width of the canvas to the specified width.
     *
     * @param width the specified width.
     */
    public void setPreferredWidth(int width) {
        Dimension pref = getPreferredSize();
        pref.width = width;
        setPreferredSize(pref);
    }
    
    /**
     * Sets the preferred height of the canvas to the specified height.
     *
     * @param height the specified height
     * @author Edward West
     */
    public void setPreferredHeight(int height) {
        Dimension pref = getPreferredSize();
        pref.height = height;
        setPreferredSize(pref);
    }
    
    public Font getBaseFont() {
        return getFont();
    }
    
    private static final int R_1024_X_768 = 1024 * 768;
    private static final int R_800_X_600 = 800 * 600;
    private static final int R_640_X_480 = 640 * 480;
    private static final int R_320_X_240 = 320 * 240;
    private ComponentListener createResizeListener() {
        return new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                int width = getWidth();
                int height = getHeight();
                int area = width*height;
                Font f;
                if (area >= (R_1024_X_768 - R_800_X_600) / 2 + R_800_X_600) {
                    f = getFont().deriveFont(18f); //new Font("Serif", Font.PLAIN, 18);
                }
                else if (area >= (R_800_X_600 - R_640_X_480) / 2 + R_640_X_480) {
                    f = getFont().deriveFont(14f); //new Font("Serif", Font.PLAIN, 14);
                }
                else if (area >= (R_640_X_480 - R_320_X_240) / 2 + R_320_X_240) {
                    f = getFont().deriveFont(12f); //new Font("Serif", Font.PLAIN, 12);
                }
                else if (area >= (R_320_X_240) / 2) {
                    f = getFont().deriveFont(8f); //new Font("Serif", Font.PLAIN, 8);
                }
                else {
                    f = getFont().deriveFont(6f); //new Font("Serif", Font.PLAIN, 6);
                }
                setFont(f);
            }
        };
    }
    
    public Element getDOMElement(Document document) {
        Element element = document.createElement("canvas");
        Dimension size = getPreferredSize();
        element.setAttribute("name", getDasName());
        element.setAttribute("width", Integer.toString(size.width));
        element.setAttribute("height", Integer.toString(size.height));
        
        for (int index = 0; index < devicePositionList.size(); index ++) {
            Object obj = devicePositionList.get(index);
            if (obj instanceof DasRow) {
                DasRow row = (DasRow)obj;
                element.appendChild(row.getDOMElement(document));
            }
            else if (obj instanceof DasColumn) {
                DasColumn column = (DasColumn)obj;
                element.appendChild(column.getDOMElement(document));
            }
        }
        
        Component[] components = getComponents();
        Map elementMap = new LinkedHashMap();
        
        //THREE PASS ALGORITHM.
        //1.  Process all DasAxis components.
        //    Add all <axis>, <timeaxis>, <attachedaxis> elements to elementList.
        //2.  Process all DasColorBar components.
        //    Remove all <axis> elements that correspond to axis property of colorbars.
        //    Add all <colorbar> elements to elementList.
        //3.  Process all DasSpectrogramPlot and DasPlot components.
        //    Remove all <axis>, <attachedaxis>, <timeaxis>, and <colorbar> elements
        //        that correspond to xAxis, yAxis, and colorbar properties of
        //        plots spectrograms and spectrogram renderers.
        //    Add all <plot> <spectrogram> elements to elementList.
        
        for (int index = 0; index < components.length; index++) {
            if (components[index] instanceof DasAxis) {
                DasAxis axis = (DasAxis)components[index];
                //if (!axis.isOppositeAxis()) {
                //    elementMap.put(axis.getDasName(), axis.getDOMElement(document));
                //}
            }
        }
        for (int index = 0; index < components.length; index++) {
            if (components[index] instanceof DasColorBar) {
                DasColorBar colorbar = (DasColorBar)components[index];
                elementMap.put(colorbar.getDasName(), colorbar.getDOMElement(document));
            }
        }
        for (int index = 0; index < components.length; index++) {
            if (components[index] instanceof DasSpectrogramPlot) {
                DasSpectrogramPlot plot = (DasSpectrogramPlot)components[index];
                elementMap.remove(plot.getXAxis().getDasName());
                elementMap.remove(plot.getYAxis().getDasName());
                elementMap.remove(plot.getColorBar().getDasName());
                elementMap.put(plot.getDasName(), plot.getDOMElement(document));
            }
            else if (components[index] instanceof DasPlot) {
                DasPlot plot = (DasPlot)components[index];
                elementMap.remove(plot.getXAxis().getDasName());
                elementMap.remove(plot.getYAxis().getDasName());
                Renderer[] renderers = plot.getRenderers();
                for (int i = 0; i < renderers.length; i++) {
                    if (renderers[i] instanceof SpectrogramRenderer) {
                        SpectrogramRenderer spectrogram = (SpectrogramRenderer)renderers[i];
                        elementMap.remove(spectrogram.getColorBar().getDasName());
                    }
                }
                elementMap.put(plot.getDasName(), plot.getDOMElement(document));
            }
        }
        
        for (Iterator iterator = elementMap.values().iterator(); iterator.hasNext();) {
            Element e = (Element)iterator.next();
            if (e != null) {
                element.appendChild(e);
            }
        }
        return element;
    }
    
    /** Process a <code>&lt;canvas&gt;</code> element.
     *
     * @param element The DOM tree node that represents the element
     */
    public static DasCanvas processCanvasElement(Element element, FormBase form)
    throws edu.uiowa.physics.pw.das.DasPropertyException, edu.uiowa.physics.pw.das.DasNameException, ParsedExpressionException {
        try {
            
            String name = element.getAttribute("name");
            int width = Integer.parseInt(element.getAttribute("width"));
            int height = Integer.parseInt(element.getAttribute("height"));
            
            DasCanvas canvas = new DasCanvas(width, height);
            
            NodeList children = element.getChildNodes();
            int childCount = children.getLength();
            for (int index = 0; index < childCount; index++) {
                Node node = children.item(index);
                if (node instanceof Element) {
                    String tagName = node.getNodeName();
                    if (tagName.equals("row")) {
                        DasRow row = DasRow.processRowElement((Element)node, canvas, form);
                    }
                    else if (tagName.equals("column")) {
                        DasColumn column
                        = DasColumn.processColumnElement((Element)node, canvas, form);
                    }
                    else if (tagName.equals("axis")) {
                        DasAxis axis
                        = DasAxis.processAxisElement((Element)node, null, null, form);
                        canvas.add(axis);
                    }
                    else if (tagName.equals("timeaxis")) {
                        DasTimeAxis timeaxis
                        = DasTimeAxis.processTimeaxisElement((Element)node, null, null, form);
                        canvas.add(timeaxis);
                    }
                    else if (tagName.equals("attachedaxis")) {
                        DasAxis attachedaxis
                        = DasAxis.processAttachedaxisElement((Element)node,
                        null, null, form);
                        canvas.add(attachedaxis);
                    }
                    else if (tagName.equals("colorbar")) {
                        DasColorBar colorbar
                        = DasColorBar.processColorbarElement((Element)node, null, null, form);
                        canvas.add(colorbar);
                    }
                    else if (tagName.equals("spectrogram")) {
                        DasSpectrogramPlot spectrogram
                        = DasSpectrogramPlot.processSpectrogramElement((Element)node, form);
                        canvas.add(spectrogram);
                    }
                    else if (tagName.equals("plot")) {
                        DasPlot plot = DasPlot.processPlotElement((Element)node, form);
                        canvas.add(plot);
                    }
                }
            }
            try {
                canvas.setDasName(name);
            }
            catch (edu.uiowa.physics.pw.das.DasNameException dne) {
                edu.uiowa.physics.pw.das.util.DasExceptionHandler.handle(dne);
            }
            
            return canvas;
        }
        catch (edu.uiowa.physics.pw.das.DasPropertyException dpe) {
            if (!element.getAttribute("name").equals("")) {
                dpe.setObjectName(element.getAttribute("name"));
            }
            throw dpe;
        }
    }
    
    
    
    public static DasCanvas createFormCanvas(String name, int width, int height) {
        DasCanvas canvas = new DasCanvas(width, height);
        if (name == null) {
            name = "canvas_" + Integer.toHexString(System.identityHashCode(canvas));
        }
        try {
            canvas.setDasName(name);
        }
        catch (edu.uiowa.physics.pw.das.DasNameException dne) {
            edu.uiowa.physics.pw.das.util.DasExceptionHandler.handle(dne);
        }
        return canvas;
    }
    
    /** Returns the DasCanvasComponent that contains the (x, y) location.
     * If there is no component at that location, this method
     * returns <CODE>null</CODE>
     * @param x the x coordinate
     * @param y the y coordinate
     * @return the component at the specified point, or null
     */
    public DasCanvasComponent getCanvasComponentAt(int x, int y) {
        Component[] components = getComponents();
        for (int index = 1; index < components.length; index++) {
            Component c = components[index];
            if (c instanceof DasCanvasComponent) {
                DasCanvasComponent cc = (DasCanvasComponent)c;
                if (cc.getActiveRegion().contains((double)x, (double)y)) {
                    return cc;
                }
            }
        }
        return null;
    }
    
    /**
     * Removes the component, specified by <code>index</code>,
     * from this container.
     * @param     index   the index of the component to be removed.
     */
    public void remove(int index) {
        Component comp = this.getComponent(index);
        super.remove(index);
        if (comp instanceof DasCanvasComponent) {
            ((DasCanvasComponent)comp).uninstallComponent();
        }
    }
    
    private class CanvasDnDSupport extends edu.uiowa.physics.pw.das.util.DnDSupport {
        
        CanvasDnDSupport() {
            super(DasCanvas.this, DnDConstants.ACTION_COPY_OR_MOVE, null);
        }
        
        private List acceptList = Arrays.asList(new DataFlavor[]{
            edu.uiowa.physics.pw.das.graph.dnd.TransferableCanvasComponent.PLOT_FLAVOR,
            edu.uiowa.physics.pw.das.graph.dnd.TransferableCanvasComponent.AXIS_FLAVOR,
            edu.uiowa.physics.pw.das.graph.dnd.TransferableCanvasComponent.COLORBAR_FLAVOR
        });
        
        private Rectangle getAxisRectangle(Rectangle rc, Rectangle t, int x, int y) {
            if (t == null) {
                t = new Rectangle();
            }
            int o = getAxisOrientation(rc, x, y);
            switch (o) {
                case DasAxis.TOP:
                    t.width = rc.width;
                    t.height = 3*getFont().getSize();
                    t.x = rc.x;
                    t.y = rc.y - t.height;
                    break;
                case DasAxis.RIGHT:
                    t.width = 3*getFont().getSize();
                    t.height = rc.height;
                    t.x = rc.x + rc.width;
                    t.y = rc.y;
                    break;
                case DasAxis.LEFT:
                    t.width = 3*getFont().getSize();
                    t.height = rc.height;
                    t.x = rc.x - t.width;
                    t.y = rc.y;
                    break;
                case DasAxis.BOTTOM:
                    t.width = rc.width;
                    t.height = 3*getFont().getSize();
                    t.x = rc.x;
                    t.y = rc.y + rc.height;
                    break;
                default: throw new RuntimeException("invalid orientation: " + o);
            }
            return t;
        }
        
        private int getAxisOrientation(Rectangle rc, int x, int y) {
            int nx = (x - rc.x) * rc.height;
            int ny = (y - rc.y) * rc.width;
            int a = rc.width * rc.height;
            boolean b = nx + ny < a;
            return (nx > ny ? (b ? DasAxis.TOP : DasAxis.RIGHT) : (b ? DasAxis.LEFT : DasAxis.BOTTOM));
        }
        
        protected int canAccept(DataFlavor[] flavors, int x, int y, int action) {
            glassPane.setAccepting(true);
            List flavorList = java.util.Arrays.asList(flavors);
            Cell cell = getCellAt(x, y);
            Rectangle cellBounds = (cell == null ? null : cell.getCellBounds());
            Rectangle target = glassPane.target;
            if (flavorList.contains(TransferableCanvasComponent.COLORBAR_FLAVOR)) {
                return action;
            }
            else if (flavorList.contains(TransferableCanvasComponent.AXIS_FLAVOR)) {
                if (target != cellBounds && (target == null || !target.equals(cellBounds))) {
                    if (target != null) {
                        glassPane.repaint(target.x - 1, target.y - 1, target.width + 2, target.height + 2);
                    }
                    if (cellBounds != null) {
                        target = glassPane.target = getAxisRectangle(cellBounds, target, x, y);
                        glassPane.repaint(target.x - 1, target.y - 1, target.width + 2, target.height + 2);
                    }
                    else {
                        glassPane.target = null;
                    }
                }
                return action;
            }
            else if (flavorList.contains(TransferableCanvasComponent.CANVAS_COMPONENT_FLAVOR)) {
                if (target != cellBounds && (target == null || !target.equals(cellBounds))) {
                    if (target != null) {
                        glassPane.repaint(target.x - 1, target.y - 1, target.width + 2, target.height + 2);
                    }
                    target = glassPane.target = cellBounds;
                    if (cellBounds != null) {
                        glassPane.repaint(target.x - 1, target.y - 1, target.width + 2, target.height + 2);
                    }
                }
                return action;
            }
            return -1;
        }
        
        protected void done() {
            glassPane.setAccepting(false);
            if (glassPane.target != null) {
                Rectangle target = glassPane.target;
                glassPane.target = null;
                glassPane.repaint(target.x - 1, target.y - 1, target.width + 2, target.height + 2);
            }
        }
        
        protected boolean importData(Transferable t, int x, int y, int action) {
            boolean success = false;
            try {
                if (t.isDataFlavorSupported(TransferableCanvasComponent.COLORBAR_FLAVOR)) {
                    Cell c = getCellAt(x, y);
                    if (c != null) {
                        DasCanvasComponent comp = (DasCanvasComponent)t.getTransferData(TransferableCanvasComponent.CANVAS_COMPONENT_FLAVOR);
                        comp.setRow(c.getRow());
                        comp.setColumn(c.getColumn());
                        add(comp);
                        revalidate();
                        success = true;
                    }
                }
                else if (t.isDataFlavorSupported(TransferableCanvasComponent.AXIS_FLAVOR)) {
                    Cell c = getCellAt(x, y);
                    if (c != null) {
                        DasAxis axis = (DasAxis)t.getTransferData(TransferableCanvasComponent.AXIS_FLAVOR);
                        axis.setRow(c.getRow());
                        axis.setColumn(c.getColumn());
                        Rectangle cellBounds = c.getCellBounds();
                        int orientation = getAxisOrientation(cellBounds, x, y);
                        axis.setOrientation(orientation);
                        add(axis);
                        revalidate();
                        success = true;
                    }
                }
                else if (t.isDataFlavorSupported(TransferableCanvasComponent.CANVAS_COMPONENT_FLAVOR)) {
                    Cell c = getCellAt(x, y);
                    if (c != null) {
                        DasCanvasComponent comp = (DasCanvasComponent)t.getTransferData(TransferableCanvasComponent.CANVAS_COMPONENT_FLAVOR);
                        comp.setRow(c.getRow());
                        comp.setColumn(c.getColumn());
                        add(comp);
                        revalidate();
                        success = true;
                    }
                }
            }
            catch (UnsupportedFlavorException ufe) {
            }
            catch (IOException ioe) {
            }
            return success;
        }
        
        protected Transferable getTransferable(int x, int y, int action) {
            DasCanvasComponent component = DasCanvas.this.getCanvasComponentAt(x,y);
            if (component instanceof DasColorBar) {
                return new TransferableCanvasComponent((DasColorBar)component);
            }
            else if (component instanceof DasAxis) {
                return new TransferableCanvasComponent((DasAxis)component);
            }
            else if (component instanceof DasPlot) {
                return new TransferableCanvasComponent((DasPlot)component);
            }
            else {
                return null;
            }
        }
        
        protected void exportDone(Transferable t, int action) {
        }
        
    }
    
    private static class GlassPane extends JPanel implements MouseInputListener, KeyListener {
        
        boolean blocking = false;
        boolean accepting = false;
        Rectangle target;
        
        public GlassPane() {
            setOpaque(false);
            setLayout(null);
        }
        
        DasCanvas getCanvas() {
            return (DasCanvas)getParent();
        }
        
        void setBlocking(boolean b) {
            if (b != blocking) {
                blocking = b;
                if (b) {
                    addMouseListener(this);
                    addMouseMotionListener(this);
                }
                else {
                    removeMouseListener(this);
                    removeMouseMotionListener(this);
                }
                repaint();
            }
        }
        
        void setAccepting(boolean b) {
            if (b != accepting) {
                accepting = b;
                repaint();
            }
        }
        
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D)g.create();
            if (blocking) {
                paintLoading(g2);
            }
            if (((DasCanvas)getParent()).getEditingMode()) {
                paintRowColumn(g2);
            }
            if (accepting && target != null) {
                paintDnDTarget(g2);
            }
        }
        
        private void paintRowColumn(Graphics2D g2) {
            g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
            RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
            DasCanvas canvas = getCanvas();
            for (Iterator i = canvas.devicePositionList.iterator(); i.hasNext();) {
                DasDevicePosition d = (DasDevicePosition)i.next();
                double minimum = d.getMinimum();
                double maximum = d.getMaximum();
                int cWidth = canvas.getWidth();
                int cHeight = canvas.getHeight();
                int x, y, width, height;
                Paint paint;
                if (d instanceof DasRow) {
                    x = 0;
                    width = cWidth;
                    y = (int)Math.floor(minimum * cHeight + 0.5);
                    height = (int)Math.floor(maximum * cHeight + 0.5) - y;
                    paint = PAINT_ROW;
                }
                else {
                    x = (int)Math.floor(minimum * cWidth + 0.5);
                    width = (int)Math.floor(maximum * cWidth + 0.5) - x;
                    y = 0;
                    height = cHeight;
                    paint = PAINT_COLUMN;
                }
                g2.setPaint(paint);
                g2.fillRect(x, y, width, height);
            }
        }
        
        private void paintDnDTarget(Graphics2D g2) {
            g2.setStroke(STROKE_DASHED);
            g2.setPaint(PAINT_SELECTION);
            g2.drawRect(target.x + 1, target.y + 1, target.width - 2, target.height - 2);
        }
        
        private void paintLoading(Graphics2D g2) {
            g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
            RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
            g2.setColor(new Color(0xdcFFFFFF, true));
            Rectangle rect = g2.getClipBounds();
            if (rect == null) {
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            else {
                g2.fillRect(rect.x, rect.y, rect.width, rect.height);
            }
        }
        
        public void mouseClicked(MouseEvent e) {
        }
        
        public void mouseDragged(MouseEvent e) {
        }
        
        public void mouseEntered(MouseEvent e) {
        }
        
        public void mouseExited(MouseEvent e) {
        }
        
        public void mouseMoved(MouseEvent e) {
        }
        
        public void mousePressed(MouseEvent e) {
        }
        
        public void mouseReleased(MouseEvent e) {
        }
        
        /** Invoked when a key has been pressed.
         * See the class description for {@link KeyEvent} for a definition of
         * a key pressed event.
         */
        public void keyPressed(KeyEvent e) {
        }
        
        /** Invoked when a key has been released.
         * See the class description for {@link KeyEvent} for a definition of
         * a key released event.
         */
        public void keyReleased(KeyEvent e) {
        }
        
        /** Invoked when a key has been typed.
         * See the class description for {@link KeyEvent} for a definition of
         * a key typed event.
         */
        public void keyTyped(KeyEvent e) {
        }
        
    }
    
    private HashSet horizontalLineSet = new HashSet();
    private HashSet verticalLineSet = new HashSet();
    private HashSet cellSet = new HashSet();
    
    public HotLine getLineAt(int x, int y) {
        Iterator iterator = horizontalLineSet.iterator();
        while (iterator.hasNext()) {
            HotLine line = (HotLine)iterator.next();
            if (y >= line.position - 1 && y <= line.position + 1) {
                return line;
            }
        }
        iterator = verticalLineSet.iterator();
        while (iterator.hasNext()) {
            HotLine line = (HotLine)iterator.next();
            if (x >= line.position - 1 && x <= line.position + 1) {
                return line;
            }
        }
        return null;
    }
    
    public Cell getCellAt(int x, int y) {
        Cell best = null;
        Point bestCenter = null;
        Point boxCenter = null;
        Iterator iterator = cellSet.iterator();
        while (iterator.hasNext()) {
            Cell box = (Cell)iterator.next();
            Rectangle rc = box.rc;
            if (rc.contains(x, y)) {
                if (best == null) {
                    best = box;
                }
                else {
                    if (bestCenter == null) {
                        bestCenter = new Point();
                        boxCenter = new Point();
                    }
                    if (best.rc.contains(rc)) {
                        best = box;
                    }
                    else {
                        bestCenter.setLocation(best.rc.x + best.rc.width/2, best.rc.y + best.rc.height/2);
                        boxCenter.setLocation(rc.x + rc.width/2, rc.y + rc.height/2);
                        int bestDistance = distanceSquared(x, y, bestCenter.x, bestCenter.y);
                        int boxDistance = distanceSquared(x, y, boxCenter.x, boxCenter.y);
                        if (boxDistance < bestDistance) {
                            best = box;
                        }
                        else if (boxDistance == bestDistance) {
                            if (rc.width * rc.height < best.rc.width * best.rc.height) {
                                best = box;
                            }
                        }
                    }
                }
            }
        }
        return best;
    }
    
    private static int distanceSquared(int x1, int y1, int x2, int y2) {
        return (x2 - x1)*(x2 - x1) + (y2 - y1)*(y2 - y1);
    }
    
    /**
     * This method should only be called in a constructor defined in the
     * DasDevicePosition class.
     *
     * @param position the DasDevicePosition object calling this method.
     */
    void addDevicePosition(DasDevicePosition position) {
        devicePositionList.add(position);
        if (position instanceof DasRow) {
            addRow((DasRow)position);
        }
        else if (position instanceof DasColumn) {
            addColumn((DasColumn)position);
        }
    }
    
    private void addRow(DasRow row) {
        HotLine min = new HotLine(row, HotLine.MIN);
        HotLine max = new HotLine(row, HotLine.MAX);
        horizontalLineSet.add(min);
        horizontalLineSet.add(max);
        Iterator iterator = devicePositionList.iterator();
        while (iterator.hasNext()) {
            DasDevicePosition position = (DasDevicePosition)iterator.next();
            if (position instanceof DasColumn) {
                DasColumn column = (DasColumn)position;
                cellSet.add(new Cell(row, column));
            }
        }
    }
    
    private void addColumn(DasColumn column) {
        HotLine min = new HotLine(column, HotLine.MIN);
        HotLine max = new HotLine(column, HotLine.MAX);
        verticalLineSet.add(min);
        verticalLineSet.add(max);
        Iterator iterator = devicePositionList.iterator();
        while (iterator.hasNext()) {
            DasDevicePosition position = (DasDevicePosition)iterator.next();
            if (position instanceof DasRow) {
                DasRow row = (DasRow)position;
                cellSet.add(new Cell(row, column));
            }
        }
    }
    
    public void removepwDevicePosition(DasDevicePosition position) {
        devicePositionList.remove(position);
        if (position instanceof DasRow) {
            removeRow((DasRow)position);
        }
        else if (position instanceof DasColumn) {
            removeColumn((DasColumn)position);
        }
    }
    
    private void removeRow(DasRow row) {
        for(Iterator i = horizontalLineSet.iterator(); i.hasNext();) {
            HotLine line = (HotLine)i.next();
            if (line.devicePosition == row) {
                i.remove();
            }
        }
        for (Iterator i = cellSet.iterator(); i.hasNext();) {
            Cell cell = (Cell)i.next();
            if (cell.row == row) {
                i.remove();
            }
        }
    }

    private void removeColumn(DasColumn column) {
        for(Iterator i = verticalLineSet.iterator(); i.hasNext();) {
            HotLine line = (HotLine)i.next();
            if (line.devicePosition == column) {
                i.remove();
            }
        }
        for (Iterator i = cellSet.iterator(); i.hasNext();) {
            Cell cell = (Cell)i.next();
            if (cell.column == column) {
                i.remove();
            }
        }
    }
    
    public FormBase getForm() {
        Component parent = getParent();
        if (parent instanceof FormComponent) {
            return ((FormComponent)parent).getForm();
        }
        return null;
    }
    
    public boolean getEditingMode() {
        return editable;
    }
    
    public void setEditingMode(boolean b) {
        if (editable == b) {
            return;
        }
        editable = b;
        revalidate();
    }
    
    public edu.uiowa.physics.pw.das.util.DnDSupport getDnDSupport() {
        return dndSupport;
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
    
    
    public static class HotLine implements PropertyChangeListener {
        
        public static final int MIN = -1;
        public static final int NONE = 0;
        public static final int MAX = 1;
        
        int position;
        DasDevicePosition devicePosition;
        int minOrMax;
        HotLine(DasDevicePosition devicePosition, int minOrMax) {
            this.devicePosition = devicePosition;
            this.minOrMax = minOrMax;
            refresh();
            devicePosition.addPropertyChangeListener((minOrMax == MIN ? "dMinimum" : "dMaximum"), this);
        }
        void refresh() {
            position = (minOrMax == MIN
            ? (int)Math.floor(devicePosition.getDMinimum() + 0.5)
            : (int)Math.floor(devicePosition.getDMaximum() + 0.5));
        }
        public void propertyChange(PropertyChangeEvent e) {
            refresh();
        }
        public boolean equals(Object o) {
            if (o instanceof HotLine) {
                HotLine h = (HotLine)o;
                return h.devicePosition == devicePosition && h.minOrMax == minOrMax;
            }
            return false;
        }
        public int hashCode() {
            return minOrMax * devicePosition.hashCode();
        }
        public String toString() {
            return "{" + devicePosition.getDasName()
            + (minOrMax == MIN ? ", MIN, " : ", MAX, ") + position + "}";
        }
        
        public DasDevicePosition getDevicePosition() {
            return devicePosition;
        }
        
        public int getMinOrMax() {
            return minOrMax;
        }
    }
    
    public static class Cell implements PropertyChangeListener {
        
        Rectangle rc;
        DasRow row;
        DasColumn column;
        
        Cell(DasRow row, DasColumn column) {
            this.row = row;
            this.column = column;
            rc = new Rectangle();
            row.addPropertyChangeListener("dMinimum", this);
            row.addPropertyChangeListener("dMaximum", this);
            column.addPropertyChangeListener("dMinimum", this);
            column.addPropertyChangeListener("dMaximum", this);
            rc.x = (int)Math.floor(column.getDMinimum() + 0.5);
            rc.y = (int)Math.floor(row.getDMinimum() + 0.5);
            rc.width = (int)Math.floor(column.getDMaximum() + 0.5) - rc.x;
            rc.height = (int)Math.floor(row.getDMaximum() + 0.5) - rc.y;
        }
        
        public void propertyChange(PropertyChangeEvent e) {
            if (e.getSource() == row) {
                rc.y = (int)Math.floor(row.getDMinimum() + 0.5);
                rc.height = (int)Math.floor(row.getDMaximum() + 0.5) - rc.y;
            }
            else {
                rc.x = (int)Math.floor(column.getDMinimum() + 0.5);
                rc.width = (int)Math.floor(column.getDMaximum() + 0.5) - rc.x;
            }
        }
        
        public boolean equals(Object o) {
            if (o instanceof Cell) {
                Cell box = (Cell)o;
                return box.row == row && box.column == column;
            }
            return false;
        }
        
        public String toString() {
            return "{" + row.getDasName() + " x " + column.getDasName() + ": " + rc.toString() + "}";
        }
        
        public Rectangle getCellBounds() {
            return new Rectangle(rc);
        }
        
        public Rectangle getCellBounds(Rectangle r) {
            if (r == null) {
                return getCellBounds();
            }
            r.setBounds(rc);
            return r;
        }
        
        public DasRow getRow() {
            return row;
        }
        
        public DasColumn getColumn() {
            return column;
        }
        
    }
    
}
