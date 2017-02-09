/* File: DasCanvas.java
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
package org.das2.graph;

import org.das2.DasApplication;
import org.das2.DasException;
import org.das2.DasNameException;
import org.das2.DasProperties;
import org.das2.DasPropertyException;
import org.das2.NameContext;
import org.das2.dasml.FormBase;
import org.das2.dasml.FormComponent;
import org.das2.dasml.ParsedExpressionException;
import org.das2.event.DragRenderer;
import org.das2.graph.dnd.TransferableCanvasComponent;
import org.das2.system.DasLogger;
import org.das2.system.RequestProcessor;
import org.das2.util.AboutUtil;
import org.das2.util.DasExceptionHandler;
import org.das2.util.DasPNGConstants;
import org.das2.util.DasPNGEncoder;
import org.das2.util.awt.EventQueueBlocker_1;
import org.das2.util.awt.GraphicsOutput;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLayeredPane;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.LookAndFeel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import javax.swing.filechooser.FileFilter;
import org.das2.components.propertyeditor.Editable;
import org.das2.components.propertyeditor.PropertyEditor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Canvas for das2 graphics.  The DasCanvas contains any number of DasCanvasComponents 
 * such as axes, plots, colorbars, etc.
 *
 * A basic hierarchy of ownership is:
 *<pre>
 * DasCanvas
 *  |
 *  +- {@link DasDevicePosition} (2-N objects)
 *  |   |
 *  |   +- {@link DasDevicePosition} (sub positions, 0-N objects)
 *  |
 *  +- {@link DasCanvasComponent} (0-N objects)
 *</pre>
 * For example: For a 2-D line plot the basic components are:
 *<pre>
 * DasCavas
 *  |
 *  +- {@link DasRow} (a DasDevicePosition)
 *  |
 *  +- {@link DasColumn} (a DasDevicePosition)
 *  |
 *  +- {@link DasAxis} (a DasCanvasComponent)
 *  |
 *  +- {@link DasAxis} (a DasCanvasComponent)
 *  |
 *  +- {@link DasPlot} (a DasCanvasComponent)
 *     |
 *     +- {@link SymbolLineRenderer} (Helper for DasPlot, implements a data painting style.)
 *</pre>
 * Note that {@link DasPlot}s don't know how to paint data, that job is delegated to one or
 * more {@link Renderer} objects.  {@link SymbolLineRenderer}s know how to draw simple line
 * plots.
 *
 * @see DasColumn
 * @see DasRow
 * @see DasAxis
 * @see DasPlot
 * @see Renderer
 * @author eew
 */
public class DasCanvas extends JLayeredPane implements Printable, Editable, FormComponent, Scrollable {

    /** Default drawing layer of the JLayeredPane */
    public static final Integer DEFAULT_LAYER = JLayeredPane.DEFAULT_LAYER;
    /** Z-Layer for drawing the plot.  */
    public static final Integer PLOT_LAYER = new Integer(300);
    /** Z-Layer for vertical axis.  Presently lower than the horizontal axis, presumably to remove ambiguity */
    public static final Integer VERTICAL_AXIS_LAYER = new Integer(400);
    /** Z-Layer */
    public static final Integer HORIZONTAL_AXIS_LAYER = new Integer(500);
    /** Z-Layer */
    public static final Integer AXIS_LAYER = VERTICAL_AXIS_LAYER;
    /** Z-Layer */
    public static final Integer ANNOTATION_LAYER = new Integer(1000);
    /** Z-Layer */
    public static final Integer GLASS_PANE_LAYER = new Integer(30000);
    private static final Paint PAINT_ROW = new Color(0xff, 0xb2, 0xb2, 0x92);
    private static final Paint PAINT_COLUMN = new Color(0xb2, 0xb2, 0xff, 0x92);
    private static final Paint PAINT_SELECTION = Color.GRAY;
    private static final Stroke STROKE_DASHED;
    

    static {
        float thick = 3.0f;
        int cap = BasicStroke.CAP_SQUARE;
        int join = BasicStroke.JOIN_MITER;
        float[] dash = new float[]{thick * 4.0f, thick * 4.0f};
        STROKE_DASHED = new BasicStroke(thick, cap, join, thick, dash, 0.0f);
    }

    /* Canvas actions */
    protected static abstract class CanvasAction extends AbstractAction {

        protected static DasCanvas currentCanvas;

        CanvasAction(String label) {
            super(label);
        }
    }

    private static FileFilter getFileNameExtensionFilter(final String description, final String ext) {
        return new FileFilter() {

				@Override
            public boolean accept(File f) {
                return f.isDirectory() || f.toString().endsWith(ext);
            }

				@Override
            public String getDescription() {
                return description;
            }
        };
    }
    private static File currentFile;
    public static final Action SAVE_AS_PNG_ACTION = new CanvasAction("Save as PNG") {

		  @Override
        public void actionPerformed(ActionEvent e) {
            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Write to PNG");
            fileChooser.setFileFilter(getFileNameExtensionFilter("png files", "png"));
            Preferences prefs = Preferences.userNodeForPackage(DasCanvas.class);
            String savedir = prefs.get("savedir", null);
            if (savedir != null) fileChooser.setCurrentDirectory(new File(savedir));
            if (currentFile != null) fileChooser.setSelectedFile(currentFile);
            int choice = fileChooser.showSaveDialog(currentCanvas);
            if (choice == JFileChooser.APPROVE_OPTION) {
                final DasCanvas canvas = currentCanvas;
                String fname = fileChooser.getSelectedFile().toString();
                if (!fname.toLowerCase().endsWith(".png")) fname += ".png";
                final String ffname = fname;
                prefs.put("savedir", new File(ffname).getParent());
                currentFile = new File(ffname.substring(0, ffname.length() - 4));
                Runnable run = new Runnable() {

                    public void run() {
                        try {
                            canvas.writeToPng(ffname);
                        } catch (java.io.IOException ioe) {
                            org.das2.util.DasExceptionHandler.handle(ioe);
                        }
                    }
                };
                new Thread(run, "writePng").start();
            }
        }
    };
    public static final Action SAVE_AS_SVG_ACTION = new CanvasAction("Save as SVG") {

		  @Override
        public void actionPerformed(ActionEvent e) {
            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.setApproveButtonText("Select File");
            fileChooser.setDialogTitle("Write to SVG");
            fileChooser.setFileFilter(getFileNameExtensionFilter("svg files", "svg"));
            Preferences prefs = Preferences.userNodeForPackage(DasCanvas.class);
            String savedir = prefs.get("savedir", null);
            if (savedir != null) fileChooser.setCurrentDirectory(new File(savedir));
            if (currentFile != null) fileChooser.setSelectedFile(currentFile);
            int choice = fileChooser.showSaveDialog(currentCanvas);
            if (choice == JFileChooser.APPROVE_OPTION) {
                final DasCanvas canvas = currentCanvas;
                String fname = fileChooser.getSelectedFile().toString();
                if (!fname.toLowerCase().endsWith(".svg")) fname += ".svg";
                final String ffname = fname;
                prefs.put("savedir", new File(ffname).getParent());
                currentFile = new File(ffname.substring(0, ffname.length() - 4));
                Runnable run = new Runnable() {

                    public void run() {
                        try {
                            canvas.writeToSVG(ffname);
                        } catch (java.io.IOException ioe) {
                            org.das2.util.DasExceptionHandler.handle(ioe);
                        }
                    }
                };
                new Thread(run, "writeSvg").start();
            }
        }
    };
    public static final Action SAVE_AS_PDF_ACTION = new CanvasAction("Save as PDF") {

		  @Override
        public void actionPerformed(ActionEvent e) {
            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.setApproveButtonText("Select File");
            fileChooser.setDialogTitle("Write to PDF");
            fileChooser.setFileFilter(getFileNameExtensionFilter("pdf files", "pdf"));
            Preferences prefs = Preferences.userNodeForPackage(DasCanvas.class);
            String savedir = prefs.get("savedir", null);
            if (savedir != null) fileChooser.setCurrentDirectory(new File(savedir));
            if (currentFile != null) fileChooser.setSelectedFile(currentFile);
            int choice = fileChooser.showDialog(currentCanvas, "Select File");
            if (choice == JFileChooser.APPROVE_OPTION) {
                final DasCanvas canvas = currentCanvas;
                String fname = fileChooser.getSelectedFile().toString();
                if (!fname.toLowerCase().endsWith(".pdf")) fname += ".pdf";
                final String ffname = fname;
                prefs.put("savedir", new File(ffname).getParent());
                currentFile = new File(ffname.substring(0, ffname.length() - 4));
                Runnable run = new Runnable() {

                    public void run() {
                        try {
                            canvas.writeToPDF(ffname);
                        } catch (java.io.IOException ioe) {
                            org.das2.util.DasExceptionHandler.handle(ioe);
                        }
                    }
                };
                new Thread(run, "writePdf").start();
            }
        }
    };
    public static final Action EDIT_DAS_PROPERTIES_ACTION = new AbstractAction("DAS Properties") {

		  @Override
        public void actionPerformed(ActionEvent e) {
            org.das2.DasProperties.showEditor();
        }
    };
    public static final Action PRINT_ACTION = new CanvasAction("Print...") {

		  @Override
        public void actionPerformed(ActionEvent e) {
            Printable p = currentCanvas.getPrintable();
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
        }
    };
    public static final Action REFRESH_ACTION = new CanvasAction("Refresh") {

        public void actionPerformed(ActionEvent e) {
            DasCanvasComponent[] comps = currentCanvas.getCanvasComponents();
            for (int i = 0; i < comps.length; i++) {
                comps[i].update();
            }
        }
    };

    public static final Action ABOUT_ACTION = new CanvasAction("About") {

		  @Override
        public void actionPerformed(ActionEvent e) {
            String aboutContent = AboutUtil.getAboutHtml();

            JOptionPane.showConfirmDialog(currentCanvas, aboutContent, "about das2", JOptionPane.PLAIN_MESSAGE);
        }
    };
    public final Action PROPERTIES_ACTION = new CanvasAction("properties") {

        public void actionPerformed(ActionEvent e) {
            PropertyEditor editor = new PropertyEditor(DasCanvas.this);
            editor.showDialog(DasCanvas.this);
        }
    };

    public static Action[] getActions() {
        return new Action[]{
                    ABOUT_ACTION,
                    REFRESH_ACTION,
                    EDIT_DAS_PROPERTIES_ACTION,
                    PRINT_ACTION,
                    SAVE_AS_PNG_ACTION,
                    SAVE_AS_SVG_ACTION,
                    SAVE_AS_PDF_ACTION,
                };
    }
    private DasApplication application;
    private static final Logger logger = DasLogger.getLogger(DasLogger.GRAPHICS_LOG);
    private final GlassPane glassPane;
    private String dasName;
    private JPopupMenu popup;
    private boolean editable;
    private int printing = 0;
    List devicePositionList = new ArrayList();
    org.das2.util.DnDSupport dndSupport;
    DasCanvasStateSupport stateSupport;
    /** The set of Threads that are currently printing this canvas.
     * This set is used to determine of certain operations that are only
     * appropriate in printing situations should occur.
     */
    private Set printingThreads;

    /** Creates a new instance of DasCanvas
     * TODO
     */
    public DasCanvas() {
        LookAndFeel.installColorsAndFont(this, "Panel.background", "Panel.foreground", "Panel.font");
        application = DasApplication.getDefaultApplication();
        String name = application.suggestNameFor(this);
        setName(name);
        setOpaque(true);
        setLayout(new RowColumnLayout());
        addComponentListener(createResizeListener());
        setBackground(Color.white);
        setPreferredSize(new Dimension(400, 300));
        this.setDoubleBuffered(true);
        glassPane = new GlassPane();
        add(glassPane, GLASS_PANE_LAYER);
        if (!application.isHeadless()) {
            popup = createPopupMenu();
            this.addMouseListener(createMouseInputAdapter());

            try {
                dndSupport = new CanvasDnDSupport();
            } catch (SecurityException ex) {
                dndSupport = new CanvasDnDSupport();
            }
        }
        CanvasAction.currentCanvas = this;
        stateSupport = new DasCanvasStateSupport(this);
    }

    private MouseInputAdapter createMouseInputAdapter() {
        return new MouseInputAdapter() {

            public void mousePressed(MouseEvent e) {
                Point primaryPopupLocation = e.getPoint();
                CanvasAction.currentCanvas = DasCanvas.this;
                if (SwingUtilities.isRightMouseButton(e))
                    popup.show(DasCanvas.this, e.getX(), e.getY());
            }
        };
    }

    private JPopupMenu createPopupMenu() {
        JPopupMenu popup = new JPopupMenu();

        JMenuItem props = new JMenuItem(PROPERTIES_ACTION);
        popup.add(props);

        popup.addSeparator();

        Action[] actions = getActions();
        for (int iaction = 0; iaction < actions.length; iaction++) {
            JMenuItem item = new JMenuItem();
            item.setAction(actions[iaction]);
            popup.add(item);
        }

        popup.addSeparator();

        JMenuItem close = new JMenuItem("close");
        close.setToolTipText("close this popup");
        popup.add(close);

        return popup;
    }

    /** returns the GlassPane above all other components. This is used for drawing dragRenderers, etc.
     * @return
     */
    public Component getGlassPane() {
        return glassPane;
    }

    /** TODO
     * @return
     */
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
        //if (displayLockCount == 1) {
        //    glassPane.setBlocking(true);
        //}
        }
    }

    /**
     * Frees the lock the specified object has requested on the display.
     * The display will not be freed until all locks have been freed.
     *
     * @param o the object releasing its lock on the display
     * @see #lockDisplay(Object)
     */
    synchronized void freeDisplay(Object o) {
        synchronized (displayLockObject) {
            displayLockCount--;
            if (displayLockCount == 0) {
                //    glassPane.setBlocking(false);
                displayLockObject.notifyAll();
            }
        }
    }

    /** Creates a new instance of DasCanvas with the specified width and height
     * TODO
     * @param width The width of the DasCanvas
     * @param height The height of the DasCanvas
     */
    public DasCanvas(int width, int height) {
        this();
        setPreferredSize(new Dimension(width, height));
    }

    public DasApplication getApplication() {
        return application;
    }

    public void setApplication(DasApplication application) {
        this.application = application;
    }

    /** simply returns getPreferredSize().
     * @return getPreferredSize()
     */
	 @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    /** paints the canvas itself.  If printing, stamps the date on it as well.
     * @param gl the Graphics object
     */
	 @Override
    protected void paintComponent(Graphics g1) {
        logger.fine("entering DasCanvas.paintComponent");

        Graphics2D g = (Graphics2D) g1;

        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                textAntiAlias ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                antiAlias ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);


        if (!(isPrintingThread() && getBackground().equals(Color.WHITE))) {
            g.setColor(getBackground());
            //g.fillRect(0, 0, getWidth(), getHeight());
            Graphics2D g2 = g;
            g2.fill(g2.getClipBounds());
        }
        g.setColor(getForeground());
        if (isPrintingThread()) {
            int width, height;
            Date now;
            SimpleDateFormat dateFormat;
            Font font, oldFont;
            FontMetrics metrics;
            String s;

            if (!printingTag.equals("")) {
                now = new Date();
                dateFormat = new SimpleDateFormat(printingTag);
                s = dateFormat.format(now);

                oldFont = g.getFont();
                font = oldFont.deriveFont((float) oldFont.getSize() / 2);
                metrics = g.getFontMetrics(font);
                width = metrics.stringWidth(s);
                height = metrics.getHeight();

                g.setFont(font);
                g.drawString(s, getWidth() - width, getHeight() -height );
                g.setFont(oldFont);
            }
        }
    }

    /** Prints the canvas, scaling and possibly rotating it to improve fit.
     * @param printGraphics the Graphics object.
     * @param format the PageFormat object.
     * @param pageIndex should be 0, since the image will be on one page.
     * @return Printable.PAGE_EXISTS or Printable.NO_SUCH_PAGE
     */
	 @Override
    public int print(Graphics printGraphics, PageFormat format, int pageIndex) {

        if (pageIndex != 0) return NO_SUCH_PAGE;

        Graphics2D g2 = (Graphics2D) printGraphics;
        double canvasWidth = (double) getWidth();
        double canvasHeight = (double) getHeight();
        double printableWidth = format.getImageableWidth();
        double printableHeight = format.getImageableHeight();

        g2.translate(format.getImageableX(), format.getImageableY());

        double canvasMax = Math.max(canvasWidth, canvasHeight);
        double canvasMin = Math.min(canvasWidth, canvasHeight);
        double printableMax = Math.max(printableWidth, printableHeight);
        double printableMin = Math.min(printableWidth, printableHeight);

        double maxScaleFactor = printableMax / canvasMax;
        double minScaleFactor = printableMin / canvasMin;
        double scaleFactor = Math.min(maxScaleFactor, minScaleFactor);
        g2.scale(scaleFactor, scaleFactor);

        if ((canvasWidth == canvasMax) ^ (printableWidth == printableMax)) {
            g2.rotate(Math.PI / 2.0);
            g2.translate(0.0, -canvasHeight);
        }

        print(g2);

        return PAGE_EXISTS;

    }

    /**
     * Layout manager for managing the Row, Column layout implemented by swing.
     * This will probably change in the future when we move away from using
     * swing to handle the DasCanvasComponents.
     */
    protected static class RowColumnLayout implements LayoutManager {

		  @Override
        public void layoutContainer(Container target) {
            synchronized (target.getTreeLock()) {
                int count = target.getComponentCount();
                for (int i = 0; i < count; i++) {
                    Component c = target.getComponent(i);
                    if (c instanceof DasCanvasComponent) {
                        ((DasCanvasComponent) c).update();
                    } else if (c == ((DasCanvas) target).glassPane) {
                        Dimension size = target.getSize();
                        c.setBounds(0, 0, size.width, size.height);
                    }
                }
            }
        }

		  @Override
        public Dimension minimumLayoutSize(Container target) {
            return new Dimension(0, 0);
        }

		  @Override
        public Dimension preferredLayoutSize(Container target) {
            return new Dimension(400, 300);
        }

		  @Override
        public void addLayoutComponent(String name, Component comp) {
        }

		  @Override
        public void removeLayoutComponent(Component comp) {
        }
    }

    /**
     * @return true if the current thread is registered as the one printing this component.
     */
    protected final boolean isPrintingThread() {
        synchronized (this) {
            return printingThreads == null ? false : printingThreads.contains(Thread.currentThread());
        }
    }

	 @Override
    public void print(Graphics g) {
        synchronized (this) {
            if (printingThreads == null) {
                printingThreads = new HashSet();
            }
            printingThreads.add(Thread.currentThread());
        }
        try {
            setOpaque(false);
            logger.fine("*** print graphics: " + g);
            logger.fine("*** print graphics clip: " + g.getClip());

            for (int i = 0; i < getComponentCount(); i++) {
                Component c = getComponent(i);
                if (c instanceof DasPlot) {
                    DasPlot p = (DasPlot) c;
                    logger.fine("    DasPlot.isDirty()=" + p.isDirty());
                    logger.fine("    DasPlot.getBounds()=" + p.getBounds());
                    /* System.err.println("    DasPlot.isDirty()=" + p.isDirty());
                    System.err.println("    DasPlot.getBounds()=" + p.getBounds()); */
                }
            }
            super.print(g);
        } finally {
            setOpaque(true);
            synchronized (this) {
                printingThreads.remove(Thread.currentThread());
            }
        }
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
     * uses getImage to get an image of the canvas and encodes it
     * as a png.
     *
     * TODO: this should take an output stream, and then a helper class
     * manages the file. (e.g. web servers)
     *
     * @param filename the specified filename
     * @throws IOException if there is an error opening the file for writing
     */
    public void writeToPng(String filename) throws IOException {
		writeToPng(filename, Collections.EMPTY_MAP);
	}

    public void writeToPng(String filename, Map<String,String> txt) throws IOException {

        final FileOutputStream out = new FileOutputStream(filename);

        logger.fine("Enter writeToPng");

        Image image = getImage(getWidth(), getHeight());

        DasPNGEncoder encoder = new DasPNGEncoder();
        encoder.addText(DasPNGConstants.KEYWORD_CREATION_TIME, new Date().toString());
		for (String key : txt.keySet()) {
			encoder.addText(key, txt.get(key));
		}
        try {
            logger.fine("Encoding image into png");
            encoder.write((BufferedImage) image, out);
            logger.fine("write png file " + filename);
        } catch (IOException ioe) {
        } finally {
            try {
                out.close();
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }

    }

    public void writeToPDF(String filename) throws IOException {
        try {
            writeToGraphicsOutput(filename, "org.das2.util.awt.PdfGraphicsOutput");
            DasLogger.getLogger(DasLogger.GRAPHICS_LOG).fine("write pdf file " + filename);
        } catch (NoClassDefFoundError cnfe) {
            DasExceptionHandler.handle(new RuntimeException("PDF output is not available", cnfe));
        } catch (ClassNotFoundException cnfe) {
            DasExceptionHandler.handle(new RuntimeException("PDF output is not available", cnfe));
        } catch (InstantiationException ie) {
            DasExceptionHandler.handleUncaught(ie);
        } catch (IllegalAccessException iae) {
            DasExceptionHandler.handleUncaught(iae);
        }
    }

    /**
     * write to various graphics devices such as png, pdf and svg.  This handles the synchronization and
     * parameter settings.
     * @param out OutputStream to receive the data
     * @param go GraphicsOutput object.
     */
    public void writeToGraphicsOutput(OutputStream out, GraphicsOutput go) throws IOException, IllegalAccessException {
        go.setOutputStream(out);
        go.setSize(getWidth(), getHeight());
        go.start();
        print(go.getGraphics());
        go.finish();
    }

    public void writeToGraphicsOutput(String filename, String graphicsOutput)
            throws IOException, ClassNotFoundException,
            InstantiationException, IllegalAccessException {
        FileOutputStream out = new FileOutputStream(filename);
        Class goClass = Class.forName(graphicsOutput);
        GraphicsOutput go = (GraphicsOutput) goClass.newInstance();
        writeToGraphicsOutput(out, go);

    }

    /**
     * @param filename the specified filename
     * @throws IOException if there is an error opening the file for writing
     */
    public void writeToSVG(String filename) throws IOException {
        try {
            writeToGraphicsOutput(filename, "org.das2.util.awt.SvgGraphicsOutput");
            DasLogger.getLogger(DasLogger.GRAPHICS_LOG).fine("write svg file " + filename);
        } catch (ClassNotFoundException cnfe) {
            DasExceptionHandler.handle(new RuntimeException("SVG output is not available", cnfe));
        } catch (InstantiationException ie) {
            DasExceptionHandler.handleUncaught(ie);
        } catch (IllegalAccessException iae) {
            DasExceptionHandler.handleUncaught(iae);
        }
    }

    /**
     * returns true if work needs to be done to make the canvas clean.  This checks each component's
     * isDirty.
     * 
     * @return
     */
    public boolean isDirty() {
        DasCanvasComponent[] cc= this.getCanvasComponents();
        boolean result= false;
        for ( int i=0; i<cc.length; i++ ) {
             result= result | cc[i].isDirty();
        }
        return result;
    }

    /**
     * Blocks the caller's thread until all events have been dispatched from the awt event thread, and
     * then waits for the RequestProcessor to finish all tasks with this canvas as the lock object.
     */
    public void waitUntilIdle() throws InterruptedException {

        String msg = "dasCanvas.waitUntilIdle";

        logger.fine(msg);

        final Object lockObject = new Object();

        /* wait for all the events on the awt event thread to process */
        EventQueueBlocker_1.clearEventQueue();
        logger.finer("pending events processed");

        /* wait for all the RequestProcessor to complete */
        Runnable request = new Runnable() {

            public void run() {
                synchronized (lockObject) {
                    lockObject.notifyAll();
                }
            }
        };

        try {
            synchronized (lockObject) {
                logger.finer("submitting invokeAfter to RequestProcessor to block until all tasks are complete");
                RequestProcessor.invokeAfter(request, this);
                lockObject.wait();
                logger.finer("requestProcessor.invokeAfter task complete");
            }
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }

        /* wait for all the post data-load stuff to clear */
        EventQueueBlocker_1.clearEventQueue();
        logger.finer("post data-load pending events processed");

        /** wait for registered pending changes.  TODO: this is cheesy */
        if (stateSupport.isPendingChanges()) {
            logger.finer("waiting for pending changes");
            while (stateSupport.isPendingChanges()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
            waitUntilIdle();
        }

        logger.fine("canvas is idle");
        /* should be in static state */
        return;
    }

    /**
     * introduced as a kludgy way for clients to force the canvas to resize all of its components.
     * validate or revalidate should probably do this.
     */
    public void resizeAllComponents() {
        for (int i = 0; i < getComponentCount(); i++) {
            Component c = getComponent(i);
            if (c instanceof DasCanvasComponent) {
                ((DasCanvasComponent) c).resize();
            }
        }
    }

    /**
     * resets the width and height, then waits for all update
     * messages to be processed.  In headless mode,
     * the gui components are validated.
     * This must not be called from the event queue, because
     * it uses eventQueueBlocker!
     *
     * @param width the width of the output in pixels.
     * @param height the width of the output in pixels.
     *
     * @throws IllegalStateException if called from the event queue.
     */
    public void prepareForOutput(final int width, final int height) {
        if (SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("dasCanvas.prepareForOutput must not be called from event queue!");

        try {
			SwingUtilities.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					setPreferredWidth(width);
					setPreferredHeight(height);

					if ("true".equals(DasApplication.getProperty("java.awt.headless", "false"))) {
						DasCanvas.this.addNotify();
						logger.finer("setSize(" + getPreferredSize() + ")");
						DasCanvas.this.setSize(getPreferredSize());
						logger.finer("validate()");
						DasCanvas.this.validate();

						resizeAllComponents();
					}
				}
			});
            waitUntilIdle();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
		catch (InvocationTargetException ex) {
			throw new RuntimeException(ex);
		}

    }

    /** 
     * Creates a BufferedImage by blocking until the image is ready.  This
     * includes waiting for datasets to load, etc.  Works by submitting
     * an invokeAfter request to the RequestProcessor that calls
     * {@link #writeToImageImmediately(Image)}.
     * 
     * @param width
     * @param height
     * @return
     */
    public Image getImage(int width, int height) {
        String msg = "dasCanvas.getImage(" + width + "," + height + ")";
        logger.fine(msg);

        prepareForOutput(width, height);

        final Image image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        if (SwingUtilities.isEventDispatchThread()) {
            writeToImageImmediately(image);
        } else {
            Runnable run = new Runnable() {

                public void run() {
                    logger.fine("writeToImageImmediately");
                    writeToImageImmediately(image);
                }
            };
            try {
                SwingUtilities.invokeAndWait(run);
            } catch (InvocationTargetException ex) {
                application.getExceptionHandler().handle(ex);
            } catch (InterruptedException ex) {
                application.getExceptionHandler().handle(ex);
            }
        }

        return image;
    }

    /** TODO
     * @param image
     */
    protected void writeToImageImmediately(Image image) {
        Graphics graphics;
        try {
            synchronized (displayLockObject) {
                if (displayLockCount != 0) {
                    displayLockObject.wait();
                }
            }
        } catch (InterruptedException ex) {
        }
        graphics = image.getGraphics();
        graphics.setColor(this.getBackground());
        graphics.fillRect(0, 0, image.getWidth(this), image.getHeight(this));
        graphics.setColor(this.getForeground());
        print(graphics);
    }

    /** This methods adds the specified <code>DasCanvasComponent</code> to this canvas.
     * @param c the component to be added to this canvas
     * Note that the canvas will need to be revalidated after the component
     * is added.
     * @param row DasRow specifying the layout of the component.
     * @param column  DasColumn specifying the layout of the component.
     */
    public void add(DasCanvasComponent c, DasRow row, DasColumn column) {
        if (c.getRow() == DasRow.NULL || c.getRow().getParent() != this) {
            c.setRow(row);
        }
        if (c.getColumn() == DasColumn.NULL || c.getColumn().getParent() != this) {
            c.setColumn(column);
        }
        add(c);
        PropertyChangeListener positionListener = new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                repaint();
            }
        };

		  if(row != null)
		     row.addPropertyChangeListener(positionListener);
		  
		  if(column != null)
		     column.addPropertyChangeListener(positionListener);
    }

    /** TODO
     * @param comp
     * @param constraints
     * @param index
     */
	 @Override
    protected void addImpl(Component comp, Object constraints, int index) {
        if (comp == null) {
            org.das2.util.DasDie.println("NULL COMPONENT");
            Thread.dumpStack();
            return;
        }
        if (index < 0) index = 0;

        Integer layer = (Integer) ((JComponent) comp).getClientProperty(LAYER_PROPERTY);
        if (layer == null) {
            if (comp instanceof DasPlot) {
                ((DasPlot) comp).putClientProperty(LAYER_PROPERTY, PLOT_LAYER);
            } else if (comp instanceof DasAxis) {
                ((DasAxis) comp).putClientProperty(LAYER_PROPERTY, AXIS_LAYER);
            } else if (comp instanceof Legend) {
                ((Legend) comp).putClientProperty(LAYER_PROPERTY, AXIS_LAYER);
            } else if (comp instanceof DasAnnotation) {
                ((DasAnnotation) comp).putClientProperty(LAYER_PROPERTY, ANNOTATION_LAYER);
            } else if (comp instanceof JComponent) {
                ((JComponent) comp).putClientProperty(LAYER_PROPERTY, DEFAULT_LAYER);
            }
        }
        super.addImpl(comp, constraints, index);
        if (comp instanceof DasCanvasComponent) {
            ((DasCanvasComponent) comp).installComponent();
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
        if (getParent() != null) ((JComponent) getParent()).revalidate();
    }

    /** Sets the preferred height of the canvas to the specified height.
     *
     * @param height the specified height
     */
    public void setPreferredHeight(int height) {
        Dimension pref = getPreferredSize();
        pref.height = height;
        setPreferredSize(pref);
        if (getParent() != null) ((JComponent) getParent()).revalidate();
    }


	 protected boolean scaleFonts = true;

    /**
     * The font used should be the base font scaled based on the canvas size.
     * If this is false, then the canvas font is simply the base font.
     */
    public static final String PROP_SCALEFONTS = "scaleFonts";

    public boolean isScaleFonts() {
        return scaleFonts;
    }

    public void setScaleFonts(boolean scaleFonts) {
        boolean oldScaleFonts = this.scaleFonts;
        this.scaleFonts = scaleFonts;
        setBaseFont(getBaseFont());
        firePropertyChange(PROP_SCALEFONTS, oldScaleFonts, scaleFonts);
    }

    private Font baseFont = null;

    /** TODO
     * @return
     */
    public Font getBaseFont() {
        if (baseFont == null) {
            baseFont = getFont();
        }
        return this.baseFont;
    }

    /**
     * The base font is the font from which all other fonts should be derived.  When the
     * canvas is resized, the base font size is scaled.
     * @param font the font used to derive all other fonts.
     */
    public void setBaseFont(Font font) {
        Font oldFont = getFont();
        this.baseFont = font;
		  if ( scaleFonts ) {
            setFont(getFontForSize(getWidth(), getHeight()));
        } else {
            setFont( font );
        }
        firePropertyChange("font", oldFont, getFont()); //TODO: really?
        repaint();
    }
    private static final int R_1024_X_768 = 1024 * 768;
    private static final int R_800_X_600 = 800 * 600;
    private static final int R_640_X_480 = 640 * 480;
    private static final int R_320_X_240 = 320 * 240;

    private Font getFontForSize(int width, int height) {
        int area = width * height;
        Font f;

        float baseFontSize = getBaseFont().getSize2D();

        if (area >= (R_1024_X_768 - R_800_X_600) / 2 + R_800_X_600) {
            f = getBaseFont().deriveFont(baseFontSize / 12f * 18f); //new Font("Serif", Font.PLAIN, 18);
        } else if (area >= (R_800_X_600 - R_640_X_480) / 2 + R_640_X_480) {
            f = getBaseFont().deriveFont(baseFontSize / 12f * 14f); //new Font("Serif", Font.PLAIN, 14);
        } else if (area >= (R_640_X_480 - R_320_X_240) / 2 + R_320_X_240) {
            f = getBaseFont().deriveFont(baseFontSize / 12f * 12f); //new Font("Serif", Font.PLAIN, 12);
        } else if (area >= (R_320_X_240) / 2) {
            f = getBaseFont().deriveFont(baseFontSize / 12f * 8f); //new Font("Serif", Font.PLAIN, 8);
        } else {
            f = getBaseFont().deriveFont(baseFontSize / 12f * 6f); //new Font("Serif", Font.PLAIN, 6);
        }
        return f;
    }

    private ComponentListener createResizeListener() {
        return new ComponentAdapter() {

			@Override
            public void componentResized(ComponentEvent e) {
                Font aFont;
                if ( scaleFonts ) {
                    aFont= getFontForSize(getWidth(), getHeight());
                } else {
                    aFont= getBaseFont();
                }
                if (!aFont.equals(getFont())) {
                    setFont(aFont);
                }
            }
        };
    }

    /** TODO
     * @return
     * @param document
     */
	 @Override
    public Element getDOMElement(Document document) {
        Element element = document.createElement("canvas");
        Dimension size = getPreferredSize();
        element.setAttribute("name", getDasName());
        element.setAttribute("width", Integer.toString(size.width));
        element.setAttribute("height", Integer.toString(size.height));

        for (int index = 0; index < devicePositionList.size(); index++) {
            Object obj = devicePositionList.get(index);
            if (obj instanceof DasRow) {
                DasRow row = (DasRow) obj;
                element.appendChild(row.getDOMElement(document));
            } else if (obj instanceof DasColumn) {
                DasColumn column = (DasColumn) obj;
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
                DasAxis axis = (DasAxis) components[index];
                elementMap.put(axis.getDasName(), axis.getDOMElement(document));
            }
        }
        for (int index = 0; index < components.length; index++) {
            if (components[index] instanceof DasColorBar) {
                DasColorBar colorbar = (DasColorBar) components[index];
                elementMap.put(colorbar.getDasName(), colorbar.getDOMElement(document));
            }
        }
        for (int index = 0; index < components.length; index++) {
            if (components[index] instanceof DasPlot) {
                DasPlot plot = (DasPlot) components[index];
                elementMap.remove(plot.getXAxis().getDasName());
                elementMap.remove(plot.getYAxis().getDasName());
                Renderer[] renderers = plot.getRenderers();
                for (int i = 0; i < renderers.length; i++) {
                    if (renderers[i] instanceof SpectrogramRenderer) {
                        SpectrogramRenderer spectrogram = (SpectrogramRenderer) renderers[i];
                        elementMap.remove(spectrogram.getColorBar().getDasName());
                    }
                }
                elementMap.put(plot.getDasName(), plot.getDOMElement(document));
            }
        }

        for (Iterator iterator = elementMap.values().iterator(); iterator.hasNext();) {
            Element e = (Element) iterator.next();
            if (e != null) {
                element.appendChild(e);
            }
        }
        return element;
    }

    /** Process a <code>&lt;canvas&gt;</code> element.
     *
     * @param form
     * @param element The DOM tree node that represents the element
     * @throws DasPropertyException
     * @throws DasNameException
     * @throws ParsedExpressionException
     * @return
     */
    public static DasCanvas processCanvasElement(Element element, FormBase form)
            throws DasPropertyException, DasNameException, DasException, ParsedExpressionException, java.text.ParseException {
        try {
            Logger log = DasLogger.getLogger(DasLogger.DASML_LOG);

            String name = element.getAttribute("name");
            int width = Integer.parseInt(element.getAttribute("width"));
            int height = Integer.parseInt(element.getAttribute("height"));

            DasApplication app = form.getDasApplication();
            NameContext nc = app.getNameContext();

            DasCanvas canvas = new DasCanvas(width, height);

            NodeList children = element.getChildNodes();
            int childCount = children.getLength();
            for (int index = 0; index < childCount; index++) {
                Node node = children.item(index);
                log.fine("node=" + node.getNodeName());
                if (node instanceof Element) {
                    String tagName = node.getNodeName();
                    if (tagName.equals("row")) {
                        DasRow row = DasRow.processRowElement((Element) node, canvas, form);
                    } else if (tagName.equals("column")) {
                        DasColumn column = DasColumn.processColumnElement((Element) node, canvas, form);
                    } else if (tagName.equals("axis")) {
                        DasAxis axis = DasAxis.processAxisElement((Element) node, form);
                        canvas.add(axis);
                    } else if (tagName.equals("timeaxis")) {
                        DasAxis timeaxis = DasAxis.processTimeaxisElement((Element) node, form);
                        canvas.add(timeaxis);
                    } else if (tagName.equals("attachedaxis")) {
                        DasAxis attachedaxis = DasAxis.processAttachedaxisElement((Element) node, form);
                        canvas.add(attachedaxis);
                    } else if (tagName.equals("colorbar")) {
                        DasColorBar colorbar = DasColorBar.processColorbarElement((Element) node, form);
                        canvas.add(colorbar);
                    } else if (tagName.equals("plot")) {
                        DasPlot plot = DasPlot.processPlotElement((Element) node, form);
                        canvas.add(plot);
                    } else if (tagName.equals("spectrogram")) {
                        DasPlot plot = DasPlot.processPlotElement((Element) node, form);
                        canvas.add(plot);
                    }

                }
            }
            canvas.setDasName(name);
            nc.put(name, canvas);

            return canvas;
        } catch (org.das2.DasPropertyException dpe) {
            if (!element.getAttribute("name").equals("")) {
                dpe.setObjectName(element.getAttribute("name"));
            }
            throw dpe;
        }
    }

    /**
     * @param name
     * @param width
     * @param height
     * @return DasCanvas with a name.
     */
    public static DasCanvas createFormCanvas(String name, int width, int height) {
        DasCanvas canvas = new DasCanvas(width, height);
        if (name == null) {
            name = "canvas_" + Integer.toHexString(System.identityHashCode(canvas));
        }
        try {
            canvas.setDasName(name);
        } catch (org.das2.DasNameException dne) {
            org.das2.util.DasExceptionHandler.handle(dne);
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
                DasCanvasComponent cc = (DasCanvasComponent) c;
                if (cc.getActiveRegion().contains((double) x, (double) y)) {
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
	 @Override
    public void remove(int index) {
        Component comp = this.getComponent(index);
        super.remove(index);
        if (comp instanceof DasCanvasComponent) {
            ((DasCanvasComponent) comp).uninstallComponent();
        }
    }

    private class CanvasDnDSupport extends org.das2.util.DnDSupport {

        CanvasDnDSupport() {
            super(DasCanvas.this, DnDConstants.ACTION_COPY_OR_MOVE, null);
        }
        private List acceptList = Arrays.asList(new DataFlavor[]{
                    org.das2.graph.dnd.TransferableCanvasComponent.PLOT_FLAVOR,
                    org.das2.graph.dnd.TransferableCanvasComponent.AXIS_FLAVOR,
                    org.das2.graph.dnd.TransferableCanvasComponent.COLORBAR_FLAVOR
                });

        private Rectangle getAxisRectangle(Rectangle rc, Rectangle t, int x, int y) {
            if (t == null) {
                t = new Rectangle();
            }
            int o = getAxisOrientation(rc, x, y);
            switch (o) {
                case DasAxis.TOP:
                    t.width = rc.width;
                    t.height = 3 * getFont().getSize();
                    t.x = rc.x;
                    t.y = rc.y - t.height;
                    break;
                case DasAxis.RIGHT:
                    t.width = 3 * getFont().getSize();
                    t.height = rc.height;
                    t.x = rc.x + rc.width;
                    t.y = rc.y;
                    break;
                case DasAxis.LEFT:
                    t.width = 3 * getFont().getSize();
                    t.height = rc.height;
                    t.x = rc.x - t.width;
                    t.y = rc.y;
                    break;
                case DasAxis.BOTTOM:
                    t.width = rc.width;
                    t.height = 3 * getFont().getSize();
                    t.x = rc.x;
                    t.y = rc.y + rc.height;
                    break;
                default:
                    throw new RuntimeException("invalid orientation: " + o);
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

		  @Override
        protected int canAccept(DataFlavor[] flavors, int x, int y, int action) {
            glassPane.setAccepting(true);
            List flavorList = java.util.Arrays.asList(flavors);
            Cell cell = getCellAt(x, y);
            Rectangle cellBounds = (cell == null ? null : cell.getCellBounds());
            Rectangle target = glassPane.target;
            if (flavorList.contains(TransferableCanvasComponent.COLORBAR_FLAVOR)) {
                return action;
            } else if (flavorList.contains(TransferableCanvasComponent.AXIS_FLAVOR)) {
                if (target != cellBounds && (target == null || !target.equals(cellBounds))) {
                    if (target != null) {
                        glassPane.repaint(target.x - 1, target.y - 1, target.width + 2, target.height + 2);
                    }
                    if (cellBounds != null) {
                        target = glassPane.target = getAxisRectangle(cellBounds, target, x, y);
                        glassPane.repaint(target.x - 1, target.y - 1, target.width + 2, target.height + 2);
                    } else {
                        glassPane.target = null;
                    }
                }
                return action;
            } else if (flavorList.contains(TransferableCanvasComponent.CANVAS_COMPONENT_FLAVOR)) {
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

		  @Override
        protected void done() {
            glassPane.setAccepting(false);
            if (glassPane.target != null) {
                Rectangle target = glassPane.target;
                glassPane.target = null;
                glassPane.repaint(target.x - 1, target.y - 1, target.width + 2, target.height + 2);
            }
        }

		  @Override
        protected boolean importData(Transferable t, int x, int y, int action) {
            boolean success = false;
            try {
                if (t.isDataFlavorSupported(TransferableCanvasComponent.COLORBAR_FLAVOR)) {
                    Cell c = getCellAt(x, y);
                    if (c != null) {
                        DasCanvasComponent comp = (DasCanvasComponent) t.getTransferData(TransferableCanvasComponent.CANVAS_COMPONENT_FLAVOR);
                        comp.setRow(c.getRow());
                        comp.setColumn(c.getColumn());
                        add(comp);
                        revalidate();
                        success = true;
                    }
                } else if (t.isDataFlavorSupported(TransferableCanvasComponent.AXIS_FLAVOR)) {
                    Cell c = getCellAt(x, y);
                    if (c != null) {
                        DasAxis axis = (DasAxis) t.getTransferData(TransferableCanvasComponent.AXIS_FLAVOR);
                        axis.setRow(c.getRow());
                        axis.setColumn(c.getColumn());
                        Rectangle cellBounds = c.getCellBounds();
                        int orientation = getAxisOrientation(cellBounds, x, y);
                        axis.setOrientation(orientation);
                        add(axis);
                        revalidate();
                        success = true;
                    }
                } else if (t.isDataFlavorSupported(TransferableCanvasComponent.CANVAS_COMPONENT_FLAVOR)) {
                    Cell c = getCellAt(x, y);
                    if (c != null) {
                        DasCanvasComponent comp = (DasCanvasComponent) t.getTransferData(TransferableCanvasComponent.CANVAS_COMPONENT_FLAVOR);
                        comp.setRow(c.getRow());
                        comp.setColumn(c.getColumn());
                        add(comp);
                        revalidate();
                        success = true;
                    }
                }
            } catch (UnsupportedFlavorException ufe) {
            } catch (IOException ioe) {
            }
            return success;
        }

		  @Override
        protected Transferable getTransferable(int x, int y, int action) {
            DasCanvasComponent component = DasCanvas.this.getCanvasComponentAt(x, y);
            if (component instanceof DasColorBar) {
                return new TransferableCanvasComponent((DasColorBar) component);
            } else if (component instanceof DasAxis) {
                return new TransferableCanvasComponent((DasAxis) component);
            } else if (component instanceof DasPlot) {
                return new TransferableCanvasComponent((DasPlot) component);
            } else {
                return null;
            }
        }

		  @Override
        protected void exportDone(Transferable t, int action) {
        }
    }

    /**
     * JPanel that lives above all other components, and is capable of blocking keyboard and mouse input from
     * all components underneath.
     */
    public static class GlassPane extends JPanel implements MouseInputListener, KeyListener {

        boolean blocking = false;
        boolean accepting = false;
        Rectangle target;
        DragRenderer dragRenderer = null;
        Point p1 = null, p2 = null;

        public GlassPane() {
            setOpaque(false);
            setLayout(null);
        }

        DasCanvas getCanvas() {
            return (DasCanvas) getParent();
        }

        void setBlocking(boolean b) {
            if (b != blocking) {
                blocking = b;
                if (b) {
                    addMouseListener(this);
                    addMouseMotionListener(this);
                } else {
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

        public void setDragRenderer(DragRenderer r, Point p1, Point p2) {
            this.dragRenderer = r;
            this.p1 = p1;
            this.p2 = p2;
        }

		  @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            if (blocking) {
                paintLoading(g2);
            }
            if (((DasCanvas) getParent()).getEditingMode()) {
                paintRowColumn(g2);
            }
            if (accepting && target != null) {
                paintDnDTarget(g2);
            }
            if (dragRenderer != null) {
                dragRenderer.renderDrag(g2, p1, p2);
            }
        }

        private void paintRowColumn(Graphics2D g2) {
            g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                    RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
            DasCanvas canvas = getCanvas();
            for (Iterator i = canvas.devicePositionList.iterator(); i.hasNext();) {
                DasDevicePosition d = (DasDevicePosition) i.next();
                double minimum = d.getMinimum();
                double maximum = d.getMaximum();
                int cWidth = canvas.getWidth();
                int cHeight = canvas.getHeight();
                int x, y, width, height;
                Paint paint;
                if (d instanceof DasRow) {
                    x = 0;
                    width = cWidth;
                    y = (int) Math.floor(minimum * cHeight + 0.5);
                    height = (int) Math.floor(maximum * cHeight + 0.5) - y;
                    paint = PAINT_ROW;
                } else {
                    x = (int) Math.floor(minimum * cWidth + 0.5);
                    width = (int) Math.floor(maximum * cWidth + 0.5) - x;
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
            } else {
                g2.fillRect(rect.x, rect.y, rect.width, rect.height);
            }
        }

		  @Override
        public void mouseClicked(MouseEvent e) {
        }

		  @Override
        public void mouseDragged(MouseEvent e) {
        }

		  @Override
        public void mouseEntered(MouseEvent e) {
        }

		  @Override
        public void mouseExited(MouseEvent e) {
        }

		  @Override
        public void mouseMoved(MouseEvent e) {
        }

		  @Override
        public void mousePressed(MouseEvent e) {
        }

		  @Override
        public void mouseReleased(MouseEvent e) {
        }

        /** Invoked when a key has been pressed.
         * See the class description for {@link KeyEvent} for a definition of
         * a key pressed event.
         */
		  @Override
        public void keyPressed(KeyEvent e) {
        }

        /** Invoked when a key has been released.
         * See the class description for {@link KeyEvent} for a definition of
         * a key released event.
         */
		  @Override
        public void keyReleased(KeyEvent e) {
        }

        /** Invoked when a key has been typed.
         * See the class description for {@link KeyEvent} for a definition of
         * a key typed event.
         */
		  @Override
        public void keyTyped(KeyEvent e) {
        }
    }
    private HashSet horizontalLineSet = new HashSet();
    private HashSet verticalLineSet = new HashSet();
    private HashSet cellSet = new HashSet();

    /** TODO
     * @param x
     * @param y
     * @return
     */
    public HotLine getLineAt(int x, int y) {
        Iterator iterator = horizontalLineSet.iterator();
        while (iterator.hasNext()) {
            HotLine line = (HotLine) iterator.next();
            if (y >= line.position - 1 && y <= line.position + 1) {
                return line;
            }
        }
        iterator = verticalLineSet.iterator();
        while (iterator.hasNext()) {
            HotLine line = (HotLine) iterator.next();
            if (x >= line.position - 1 && x <= line.position + 1) {
                return line;
            }
        }
        return null;
    }

    /** TODO
     * @param x
     * @param y
     * @return
     */
    public Cell getCellAt(int x, int y) {
        Cell best = null;
        Point bestCenter = null;
        Point boxCenter = null;
        Iterator iterator = cellSet.iterator();
        while (iterator.hasNext()) {
            Cell box = (Cell) iterator.next();
            Rectangle rc = box.rc;
            if (rc.contains(x, y)) {
                if (best == null) {
                    best = box;
                } else {
                    if (bestCenter == null) {
                        bestCenter = new Point();
                        boxCenter = new Point();
                    }
                    if (best.rc.contains(rc)) {
                        best = box;
                    } else {
                        bestCenter.setLocation(best.rc.x + best.rc.width / 2, best.rc.y + best.rc.height / 2);
                        boxCenter.setLocation(rc.x + rc.width / 2, rc.y + rc.height / 2);
                        int bestDistance = distanceSquared(x, y, bestCenter.x, bestCenter.y);
                        int boxDistance = distanceSquared(x, y, boxCenter.x, boxCenter.y);
                        if (boxDistance < bestDistance) {
                            best = box;
                        } else if (boxDistance == bestDistance) {
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
        return (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);
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
            addRow((DasRow) position);
        } else if (position instanceof DasColumn) {
            addColumn((DasColumn) position);
        }
    }

    private void addRow(DasRow row) {
        HotLine min = new HotLine(row, HotLine.MIN);
        HotLine max = new HotLine(row, HotLine.MAX);
        horizontalLineSet.add(min);
        horizontalLineSet.add(max);
        Iterator iterator = devicePositionList.iterator();
        while (iterator.hasNext()) {
            DasDevicePosition position = (DasDevicePosition) iterator.next();
            if (position instanceof DasColumn) {
                DasColumn column = (DasColumn) position;
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
            DasDevicePosition position = (DasDevicePosition) iterator.next();
            if (position instanceof DasRow) {
                DasRow row = (DasRow) position;
                cellSet.add(new Cell(row, column));
            }
        }
    }

    /** TODO
     * @param position
     */
    public void removepwDevicePosition(DasDevicePosition position) {

        devicePositionList.remove(position);
        if (position instanceof DasRow) {
            removeRow((DasRow) position);
        } else if (position instanceof DasColumn) {
            removeColumn((DasColumn) position);
        }
    }

    private void removeRow(DasRow row) {
        for (Iterator i = horizontalLineSet.iterator(); i.hasNext();) {
            HotLine line = (HotLine) i.next();
            if (line.devicePosition == row) {
                i.remove();
            }
        }
        for (Iterator i = cellSet.iterator(); i.hasNext();) {
            Cell cell = (Cell) i.next();
            if (cell.row == row) {
                i.remove();
            }
        }
    }

    private void removeColumn(DasColumn column) {
        for (Iterator i = verticalLineSet.iterator(); i.hasNext();) {
            HotLine line = (HotLine) i.next();
            if (line.devicePosition == column) {
                i.remove();
            }
        }
        for (Iterator i = cellSet.iterator(); i.hasNext();) {
            Cell cell = (Cell) i.next();
            if (cell.column == column) {
                i.remove();
            }
        }
    }

    /** TODO
     * @return
     */
	 @Override
    public FormBase getForm() {
        Component parent = getParent();
        if (parent instanceof FormComponent) {
            return ((FormComponent) parent).getForm();
        }
        return null;
    }

    /** TODO
     * @return
     */
	 @Override
    public boolean getEditingMode() {
        return editable;
    }

    /** TODO
     * @param b
     */
	 @Override
    public void setEditingMode(boolean b) {
        if (editable == b) {
            return;
        }
        editable = b;
        revalidate();
    }

    /** TODO
     * @return
     */
	 @Override
    public org.das2.util.DnDSupport getDnDSupport() {
        return dndSupport;
    }

    /** TODO
     * @param x
     * @param y
     * @param action
     * @param evt
     * @return
     */
	 @Override
    public boolean startDrag(int x, int y, int action, java.awt.event.MouseEvent evt) {
        for (int i = 0; i < getComponentCount(); i++) {
            if (getComponent(i).getBounds().contains(x, y)) {
                dndSupport.startDrag(x, y, action, evt);
                return true;
            }
        }
        return false;
    }

    /** TODO
     * @return
     */
	 @Override
    public String getDasName() {
        return dasName;
    }

    /** TODO
     * @param name
     * @throws DasNameException
     */
	 @Override
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

	 @Override
    public void deregisterComponent() {
        DasApplication app = getDasApplication();
        if (app != null) {
            NameContext nc = app.getNameContext();
            for (Iterator i = devicePositionList.iterator(); i.hasNext();) {
                DasDevicePosition dp = (DasDevicePosition) i.next();
                try {
                    if (nc.get(dp.getDasName()) == dp) {
                        nc.remove(dp.getDasName());
                    }
                } catch (DasPropertyException dpe) {
                    //This exception would only occur due to some invalid state.
                    //So, wrap it and toss it.
                    IllegalStateException se = new IllegalStateException(dpe.toString());
                    se.initCause(dpe);
                    throw se;
                } catch (java.lang.reflect.InvocationTargetException ite) {
                    //This exception would only occur due to some invalid state.
                    //So, wrap it and toss it.
                    IllegalStateException se = new IllegalStateException(ite.toString());
                    se.initCause(ite);
                    throw se;
                }
            }
            for (int index = 0; index < getComponentCount(); index++) {
                Component c = getComponent(index);
                if (c instanceof DasCanvasComponent) {
                    DasCanvasComponent cc = (DasCanvasComponent) c;
                    try {
                        if (nc.get(cc.getDasName()) == cc) {
                            nc.remove(cc.getDasName());
                        }
                    } catch (DasPropertyException dpe) {
                        //This exception would only occur due to some invalid state.
                        //So, wrap it and toss it.
                        IllegalStateException se = new IllegalStateException(dpe.toString());
                        se.initCause(dpe);
                        throw se;
                    } catch (java.lang.reflect.InvocationTargetException ite) {
                        //This exception would only occur due to some invalid state.
                        //So, wrap it and toss it.
                        IllegalStateException se = new IllegalStateException(ite.toString());
                        se.initCause(ite);
                        throw se;
                    }
                }
            }
            try {
                if (nc.get(getDasName()) == this) {
                    nc.remove(getDasName());
                }
            } catch (DasPropertyException dpe) {
                //This exception would only occur due to some invalid state.
                //So, wrap it and toss it.
                IllegalStateException se = new IllegalStateException(dpe.toString());
                se.initCause(dpe);
                throw se;
            } catch (java.lang.reflect.InvocationTargetException ite) {
                //This exception would only occur due to some invalid state.
                //So, wrap it and toss it.
                IllegalStateException se = new IllegalStateException(ite.toString());
                se.initCause(ite);
                throw se;
            }
        }
    }

	 @Override
    public DasApplication getDasApplication() {
        Container p = getParent();
        if (p instanceof FormComponent) {
            return ((FormComponent) p).getDasApplication();
        } else {
            return null;
        }
    }

	 @Override
    public void registerComponent() throws org.das2.DasException {
        try {
            DasApplication app = getDasApplication();
            if (app != null) {
                NameContext nc = app.getNameContext();
                for (Iterator i = devicePositionList.iterator(); i.hasNext();) {
                    DasDevicePosition dp = (DasDevicePosition) i.next();
                    nc.put(dp.getDasName(), dp);
                }
                for (int index = 0; index < getComponentCount(); index++) {
                    Component c = getComponent(index);
                    if (c instanceof DasCanvasComponent) {
                        DasCanvasComponent cc = (DasCanvasComponent) c;
                        nc.put(cc.getDasName(), cc);
                    }
                }
                nc.put(getDasName(), this);
            }
        } catch (DasNameException dne) {
            deregisterComponent();
            throw dne;
        }
    }

	 /** Support reloading and refreshing all data on the canvas
	  */
	public void reload(){
		int nLen = getComponentCount();
		Component cmp;
		for(int i = 0; i < nLen; i++){
			cmp = getComponent(i);
			if( cmp instanceof DasCanvasComponent)
				((DasCanvasComponent)cmp).reload();
		}
	}

    public DasCanvasComponent getCanvasComponents(int index) {
        return (DasCanvasComponent) getComponent(index + 1);
    }

    public DasCanvasComponent[] getCanvasComponents() {
        int n = getComponentCount() - 1;
        DasCanvasComponent[] result = new DasCanvasComponent[n];
        for (int i = 0; i < n; i++) {
            result[i] = getCanvasComponents(i);
        }
        return result;
    }

	@Override
    public String toString() {
        return "[DasCanvas " + this.getWidth() + "x" + this.getHeight() + " " + this.getDasName() + "]";
    }

    /** TODO */
    public static class HotLine implements PropertyChangeListener {

        /** TODO */
        public static final int MIN = -1;
        /** TODO */
        public static final int NONE = 0;
        /** TODO */
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
                    ? (int) Math.floor(devicePosition.getDMinimum() + 0.5)
                    : (int) Math.floor(devicePosition.getDMaximum() + 0.5));
        }

        /** TODO
         * @param e
         */
        public void propertyChange(PropertyChangeEvent e) {
            refresh();
        }

        /** TODO
         * @param o
         * @return
         */
        public boolean equals(Object o) {
            if (o instanceof HotLine) {
                HotLine h = (HotLine) o;
                return h.devicePosition == devicePosition && h.minOrMax == minOrMax;
            }
            return false;
        }

        /** TODO
         * @return
         */
        public int hashCode() {
            return minOrMax * devicePosition.hashCode();
        }

        /** TODO
         * @return
         */
        public String toString() {
            return "{" + devicePosition.getDasName() + (minOrMax == MIN ? ", MIN, " : ", MAX, ") + position + "}";
        }

        /** TODO
         * @return
         */
        public DasDevicePosition getDevicePosition() {
            return devicePosition;
        }

        /** TODO
         * @return
         */
        public int getMinOrMax() {
            return minOrMax;
        }
    }

    /** TODO */
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
            rc.x = (int) Math.floor(column.getDMinimum() + 0.5);
            rc.y = (int) Math.floor(row.getDMinimum() + 0.5);
            rc.width = (int) Math.floor(column.getDMaximum() + 0.5) - rc.x;
            rc.height = (int) Math.floor(row.getDMaximum() + 0.5) - rc.y;
        }

        /** TODO
         * @param e
         */
		  @Override
        public void propertyChange(PropertyChangeEvent e) {
            if (e.getSource() == row) {
                rc.y = (int) Math.floor(row.getDMinimum() + 0.5);
                rc.height = (int) Math.floor(row.getDMaximum() + 0.5) - rc.y;
            } else {
                rc.x = (int) Math.floor(column.getDMinimum() + 0.5);
                rc.width = (int) Math.floor(column.getDMaximum() + 0.5) - rc.x;
            }
        }

        /** TODO
         * @param o
         * @return
         */
		  @Override
        public boolean equals(Object o) {
            if (o instanceof Cell) {
                Cell box = (Cell) o;
                return box.row == row && box.column == column;
            }
            return false;
        }

        /** TODO
         * @return
         */
		  @Override
        public String toString() {
            return "{" + row.getDasName() + " x " + column.getDasName() + ": " + rc.toString() + "}";
        }

        /** TODO
         * @return
         */
        public Rectangle getCellBounds() {
            return new Rectangle(rc);
        }

        /** TODO
         * @param r
         * @return
         */
        public Rectangle getCellBounds(Rectangle r) {
            if (r == null) {
                return getCellBounds();
            }
            r.setBounds(rc);
            return r;
        }

        /** TODO
         * @return
         */
        public DasRow getRow() {
            return row;
        }

        /** TODO
         * @return
         */
        public DasColumn getColumn() {
            return column;
        }
    }
    /**
     * printingTag is the DateFormat string to use to tag printed images.
     */
    private String printingTag = "'UIOWA 'yyyyMMdd";

    /**
     * printingTag is the DateFormat string to use to tag printed images.
     * @return Value of property printingTag.
     */
    public String getPrintingTag() {
        return this.printingTag;
    }

    /**
     * printingTag is the DateFormat string to use to tag printed images.
     * @param printingTag New value of property printingTag.
     */
    public void setPrintingTag(String printingTag) {
        String old = this.printingTag;
        this.printingTag = printingTag;
        firePropertyChange("printingTag", old, printingTag);
    }
    /**
     * Holds value of property textAntiAlias.
     */
    private boolean textAntiAlias = true;

    /**
     * Getter for property textAntiAlias.
     * @return Value of property textAntiAlias.
     */
    public boolean isTextAntiAlias() {
        return this.textAntiAlias;
    }

    /**
     * Setter for property textAntiAlias.
     * @param textAntiAlias New value of property textAntiAlias.
     */
    public void setTextAntiAlias(boolean textAntiAlias) {
        boolean old = this.textAntiAlias;
        this.textAntiAlias = textAntiAlias;
        firePropertyChange("textAntiAlias", old, textAntiAlias);
    }
    /**
     * Holds value of property antiAlias.
     */
    private boolean antiAlias = "on".equals(DasProperties.getInstance().get("antiAlias"));

    /**
     * Getter for property antiAlias.
     * @return Value of property antiAlias.
     */
    public boolean isAntiAlias() {
        return this.antiAlias;
    }

    /**
     * Setter for property antiAlias.
     * @param antiAlias New value of property antiAlias.
     */
    public void setAntiAlias(boolean antiAlias) {
        boolean old = this.antiAlias;
        this.antiAlias = antiAlias;
        firePropertyChange("antiAlias", old, antiAlias);
    }
    private boolean fitted;

    /**
     * If true, and the canvas was added to a scrollpane, the canvas
     * will size itself to fit within the scrollpane.
     * 
     * @return value of fitted property
     */
    public boolean isFitted() {
        return fitted;
    }

    public void setFitted(boolean fitted) {
        boolean oldValue = this.fitted;
        this.fitted = fitted;
        firePropertyChange("fitted", oldValue, fitted);
        revalidate();
    }

	 @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

	 @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        switch (orientation) {
            case SwingConstants.HORIZONTAL:
                return visibleRect.width / 10;
            case SwingConstants.VERTICAL:
                return visibleRect.height / 10;
            default:
                return 10;
        }
    }

	 @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        switch (orientation) {
            case SwingConstants.HORIZONTAL:
                return visibleRect.width;
            case SwingConstants.VERTICAL:
                return visibleRect.height;
            default:
                return 10;
        }
    }

	 @Override
    public boolean getScrollableTracksViewportWidth() {
        return fitted;
    }

	 @Override
    public boolean getScrollableTracksViewportHeight() {
        return fitted;
    }

    public void registerPendingChange(Object client, Object lockObject) {
        stateSupport.registerPendingChange(client, lockObject);
    }

    public void performingChange(Object client, Object lockObject) {
        stateSupport.performingChange(client, lockObject);
    }

    public void changePerformed(Object client, Object lockObject) {
        stateSupport.changePerformed(client, lockObject);
    }

    public boolean isPendingChanges() {
        return stateSupport.isPendingChanges();
    }
}
