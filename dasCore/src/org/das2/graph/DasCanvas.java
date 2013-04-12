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

import java.util.logging.Level;
import org.das2.DasApplication;
import org.das2.DasNameException;
import org.das2.DasProperties;
import org.das2.event.DragRenderer;
import org.das2.graph.dnd.TransferableCanvasComponent;
import org.das2.system.DasLogger;
import org.das2.system.RequestProcessor;
import org.das2.util.AboutUtil;
import org.das2.util.DasExceptionHandler;
import org.das2.util.DasPNGConstants;
import org.das2.util.DasPNGEncoder;
import org.das2.system.EventQueueBlocker_1;
import org.das2.util.awt.GraphicsOutput;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
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
import java.awt.Toolkit;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
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
import javax.swing.LookAndFeel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import javax.swing.filechooser.FileFilter;
import org.das2.components.propertyeditor.Editable;
import org.das2.components.propertyeditor.PropertyEditor;
import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;
import org.das2.event.DasUpdateEvent;
import org.das2.system.ChangesSupport;

/** Canvas for das2 graphics.  The DasCanvas contains any number of DasCanvasComponents such as axes, plots, colorbars, etc.
 * @author eew
 */
public class DasCanvas extends JLayeredPane implements Printable, Editable, Scrollable {

    /** Default drawing layer of the JLayeredPane */
    public static final Integer DEFAULT_LAYER = JLayeredPane.DEFAULT_LAYER;
    /** Z-Layer for drawing the plot.  */
    public static final Integer PLOT_LAYER = 300;
    /** Z-Layer for vertical axis.  Presently lower than the horizontal axis, presumably to remove ambiguity */
    public static final Integer VERTICAL_AXIS_LAYER = 400;
    /** Z-Layer */
    public static final Integer HORIZONTAL_AXIS_LAYER = 500;
    /** Z-Layer */
    public static final Integer AXIS_LAYER = VERTICAL_AXIS_LAYER;
    /** Z-Layer */
    public static final Integer ANNOTATION_LAYER = 1000;
    /** Z-Layer */
    public static final Integer GLASS_PANE_LAYER = 30000;
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

    /**
     * return the canvas that has the focus.
     * @return
     */
    public static DasCanvas getFocusCanvas() {
        return CanvasAction.currentCanvas;
    }

    private List<Painter> topDecorators= new LinkedList<Painter>();
    private List<Painter> bottomDecorators= new LinkedList<Painter>();

    /**
     * Java6 has paintingForPrint, use this for now.
     */
    boolean lpaintingForPrint= false;
    

    /* Canvas actions */
    protected static abstract class CanvasAction extends AbstractAction {

        protected static DasCanvas currentCanvas;

        CanvasAction(String label) {
            super(label);
        }
    }

    private static FileFilter getFileNameExtensionFilter(final String description, final String ext) {
        return new FileFilter() {

            public boolean accept(File f) {
                if ( f.toString()==null ) return false;
                return f.isDirectory() || f.toString().endsWith(ext);
            }

            public String getDescription() {
                return description;
            }
        };
    }
    private static File currentFile;
    public static final Action SAVE_AS_PNG_ACTION = new CanvasAction("Save as PNG") {

        public void actionPerformed(ActionEvent e) {
            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Write to PNG");
            fileChooser.setFileFilter(getFileNameExtensionFilter("png files", "png"));
            Preferences prefs = Preferences.userNodeForPackage(DasCanvas.class);
            String savedir = prefs.get("savedir", null);
            if (savedir != null)
                fileChooser.setCurrentDirectory(new File(savedir));
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

        public void actionPerformed(ActionEvent e) {
            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.setApproveButtonText("Select File");
            fileChooser.setDialogTitle("Write to SVG");
            fileChooser.setFileFilter(getFileNameExtensionFilter("svg files", "svg"));
            Preferences prefs = Preferences.userNodeForPackage(DasCanvas.class);
            String savedir = prefs.get("savedir", null);
            if (savedir != null)
                fileChooser.setCurrentDirectory(new File(savedir));
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

        public void actionPerformed(ActionEvent e) {
            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.setApproveButtonText("Select File");
            fileChooser.setDialogTitle("Write to PDF");
            fileChooser.setFileFilter(getFileNameExtensionFilter("pdf files", "pdf"));
            Preferences prefs = Preferences.userNodeForPackage(DasCanvas.class);
            String savedir = prefs.get("savedir", null);
            if (savedir != null)
                fileChooser.setCurrentDirectory(new File(savedir));
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

        public void actionPerformed(ActionEvent e) {
            org.das2.DasProperties.showEditor();
        }
    };
    public static final Action PRINT_ACTION = new CanvasAction("Print...") {

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

    /**
     * returns a list of all the rows and columns on the canvas.
     * @return
     */
    public List devicePositionList() {
        return Collections.unmodifiableList(devicePositionList);
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
    }

    @Override
    public void invalidate() {
        super.invalidate();
    }
    public static final Action ABOUT_ACTION = new CanvasAction("About") {

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

    private static boolean disableActions = false;

    public static void setDisableActions(boolean val) {
        disableActions = val;
    }

    public static Action[] getActions() {
        if ( disableActions ) {
            return new Action[0];
        } else {
            return new Action[]{
                    ABOUT_ACTION,
                    REFRESH_ACTION,
                    EDIT_DAS_PROPERTIES_ACTION,
                    PRINT_ACTION,
                    SAVE_AS_PNG_ACTION,
                    SAVE_AS_SVG_ACTION,
                    SAVE_AS_PDF_ACTION,};
        }
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
    ChangesSupport stateSupport;
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
        stateSupport = new ChangesSupport(null, this);
        stateSupport.addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent e) {
                if (Boolean.FALSE.equals(e.getNewValue())) {
                    setOpaque(true);
                    repaint();
                } else {
                    setOpaque(false);
                }
            }
        });
    }

    public void addTopDecorator(Painter painter) {
        this.topDecorators.add( painter );
        repaint();
    }
    public void removeTopDecorator(Painter painter) {
        this.topDecorators.remove( painter );
        repaint();
    }
    public void removeTopDecorators() {
        this.topDecorators.clear( );
        repaint();
    }

    public boolean hasTopDecorators() {
        return ! this.topDecorators.isEmpty();
    }

    public void addBottomDecorator(Painter painter) {
        this.bottomDecorators.add( painter );
        repaint();
    }
    public void removeBottomDecorator(Painter painter) {
        this.bottomDecorators.remove( painter );
        repaint();
    }
    public void removeBottomDecorators() {
        this.bottomDecorators.clear( );
        repaint();
    }

    public boolean hasBottomDecorators() {
        return ! this.bottomDecorators.isEmpty();
    }
    
    private MouseInputAdapter createMouseInputAdapter() {
        return new MouseInputAdapter() {

            public void mousePressed(MouseEvent e) {
                CanvasAction.currentCanvas = DasCanvas.this;
                if ( e.isPopupTrigger() )
                    popup.show(DasCanvas.this, e.getX(), e.getY());
            }

            public void mouseReleased(MouseEvent e) {
                CanvasAction.currentCanvas = DasCanvas.this;
                if ( e.isPopupTrigger() )
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
        if ( !disableActions ) {
            for (int iaction = 0; iaction < actions.length; iaction++) {
                JMenuItem item = new JMenuItem();
                item.setAction(actions[iaction]);
                popup.add(item);
            }
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
    private final Object displayLockObject = new String("DISPLAY_LOCK_OBJECT");//findbugs DM_STRING_CTOR okay

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
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    /** paints the canvas itself.  If printing, stamps the date on it as well.
     * @param gl the Graphics object
     */
    protected void paintComponent(Graphics g1) {
        logger.finest("entering DasCanvas.paintComponent");

        if (stateSupport.isValueAdjusting()) {
            logger.finest("value is adjusting, returning");
            return;
        }

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

        for ( Painter p : bottomDecorators ) {
            try {
                Graphics2D g2= (Graphics2D) g.create(); // create a graphics object in case they reset colors, etc.
                p.paint(g2);
            } catch ( Exception ex ) {
                g.drawString( "bottomDecorator causes exception: "+ex.toString(), 20, 20 );
                ex.printStackTrace();
            }
        }

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
                g.drawString(s, getWidth() - width - 2 * height, getHeight() - 2 * height);
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

        public Dimension minimumLayoutSize(Container target) {
            return new Dimension(0, 0);
        }

        public Dimension preferredLayoutSize(Container target) {
            return new Dimension(400, 300);
        }

        public void addLayoutComponent(String name, Component comp) {
        }

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

    /**
     * Java1.6 has this function native
     * @return
     */
    public boolean lisPaintingForPrint() {
        return this.lpaintingForPrint;
    }

    @Override
    public void print(Graphics g1) {
        synchronized (this) {
            if (printingThreads == null) {
                printingThreads = new HashSet();
            }
            printingThreads.add(Thread.currentThread());
        }
        try {
            setOpaque(false);

            Graphics2D g= (Graphics2D)g1;
            // if svg
            g.setColor( this.getBackground() );
            g.fillRect(0,0,getWidth(),getHeight());
            g.setColor( this.getForeground() );
            g.setBackground( this.getBackground() );

            //logger.fine("*** print graphics: " + g);
            //logger.fine("*** print graphics clip: " + g.getClip());

            //for (int i = 0; i < getComponentCount(); i++) {
              //  Component c = getComponent(i);
                //if (c instanceof DasPlot) {
                //    DasPlot p = (DasPlot) c;
                //logger.fine("    DasPlot.isDirty()=" + p.isDirty());
                //logger.fine("    DasPlot.getBounds()=" + p.getBounds());
                //System.err.println("    DasPlot.isDirty()=" + p.isDirty());
                //System.err.println("    DasPlot.getBounds()=" + p.getBounds());
               // }
            //}
            lpaintingForPrint= true;

            super.print(g);

            lpaintingForPrint= false;
            
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
     * encode the plot in a JSON fragment.
     * @param plot the plot to describe.
     * @param indent indent new lines this amount
     * @param isInList is in list, so append a comma after the y_axis.
     * @return
     */
    private String getJSONForPlot( DasPlot plot, String indent, boolean isInList ) {
        StringBuilder json= new StringBuilder();
        json.append( String.format( "%s\"title\":\"%s\", \n", indent, plot.getTitle().replaceAll("\"", "\\\"") ) );
        DasAxis axis= plot.getXAxis();
        String minstr= UnitsUtil.isTimeLocation( axis.getDataMinimum().getUnits() ) ?
            String.format( "\"%s\"", axis.getDataMinimum().toString() ) :
            String.valueOf( axis.getDataMinimum( axis.getUnits() ) );
        String maxstr= UnitsUtil.isTimeLocation( axis.getDataMaximum().getUnits() ) ?
            String.format( "\"%s\"", axis.getDataMaximum().toString() ) :
            String.valueOf( axis.getDataMaximum( axis.getUnits() ) );
        String unitsstr= UnitsUtil.isTimeLocation( axis.getDataMinimum().getUnits() ) ? "UTC" : axis.getDataMinimum().getUnits().toString();
        json.append( String.format( "%s\"x_axis\": { \"label\":\"%s\", \"min\":%s, \"max\":%s, \"minpixel\":%d, \"maxpixel\":%d, \"type\":\"%s\", \"units\":\"%s\" },\n",
                indent,
                plot.getXAxis().getLabel().replaceAll("\"", "\\\"") ,
                minstr, maxstr, (int)plot.getColumn().getDMinimum(), (int)plot.getColumn().getDMaximum(),
                plot.getXAxis().isLog() ? "log" : "lin",
                unitsstr ) );
        axis= plot.getYAxis();
        minstr= UnitsUtil.isTimeLocation( axis.getDataMinimum().getUnits() ) ?
            String.format( "'%s'", axis.getDataMinimum().toString() ) :
            String.valueOf( axis.getDataMinimum( axis.getUnits() ) );
        maxstr= UnitsUtil.isTimeLocation( axis.getDataMaximum().getUnits() ) ?
            String.format( "'%s'", axis.getDataMaximum().toString() ) :
            String.valueOf( axis.getDataMaximum( axis.getUnits() ) );
        unitsstr= UnitsUtil.isTimeLocation( axis.getDataMinimum().getUnits() ) ? "UTC" : axis.getDataMinimum().getUnits().toString();
        json.append( String.format( "%s\"y_axis\": { \"label\":\"%s\", \"min\":%s, \"max\":%s, \"minpixel\":%d, \"maxpixel\":%d, \"type\":\"%s\", \"units\":\"%s\" }%s\n",
                indent,
                plot.getYAxis().getLabel().replaceAll("\"", "\\\"") ,
                minstr, maxstr, (int)plot.getRow().getDMinimum(), (int)plot.getRow().getDMaximum(),
                plot.getYAxis().isLog() ? "log" : "lin",
                unitsstr, isInList ? "," : "" ) );
        return json.toString();
    }

    /**
     * returns JSON code that can be used to get plot positions and axes.
     * @return
     */
    public String getImageMetadata() {
        List<DasPlot> plots= new ArrayList();
        DasPlot plot= null;
        int plotHeight= 0;
        for ( Component c: this.getCanvasComponents() ) {
            if ( c instanceof DasPlot ) {
                plots.add( (DasPlot)c );
                if ( ((DasPlot)c).getRow().getHeight()>plotHeight ) {
                    plot= ((DasPlot)c);
                    plotHeight= ((DasPlot)c).getRow().getHeight();
                }
            }
        }

        if ( plot==null ) return "{ \"number_of_plots_on_canvas\":0 }";

        StringBuilder json= new StringBuilder();

        json.append("{\n");
        json.append( String.format("  \"size\":[%d,%d],\n", this.getWidth(),this.getHeight() ) );
        String json1= getJSONForPlot( plot, "  ", plots.size()>1 );
        json.append( json1 );
        if ( plots.size()>1 ) {
            json.append("  \"plots\": [\n");
            DasPlot lastPlot= plots.get( plots.size()-1 );
            for ( DasPlot p: plots ) {
                json.append( "  {\n");
                json.append( getJSONForPlot( p, "    ", false ) );
                json.append( "  }");
                if ( p!=lastPlot ) json.append(",");
                json.append( "\n" );
            }
            json.append(" ]");
        }
        json.append("}" );
        return json.toString();

    }

    /**
     * uses getImage to get an image of the canvas and encodes it
     * as a png.
     *
     * Note this now puts in a JSON representation of plot locations in the "plotInfo" tag.  The plotInfo
     * tag will contain:
     *   "number_of_plots_on_canvas:0"   or
     *   "size" "title" "x_axis" and "y_axis"  or
     *   "size" "title" "x_axis" "y_axis" and "plots" when there is more than one plot.  The tallest plot is used as the default.
     *
     *
     * TODO: this should take an output stream, and then a helper class
     * manages the file. (e.g. web servers)
     *
     * @param filename the specified filename
     * @throws IOException if there is an error opening the file for writing
     */
    public void writeToPng(String filename) throws IOException {

        final FileOutputStream out = new FileOutputStream(filename);

        logger.fine("Enter writeToPng");

        int w= getWidth();
        int h= getHeight();

        if (h==0 || w==0) {
            Dimension p = getPreferredSize();
            w=(int) p.getWidth();
            h=(int) p.getHeight();
        }
        Image image = getImage(w,h);

        DasPNGEncoder encoder = new DasPNGEncoder();
        encoder.addText(DasPNGConstants.KEYWORD_CREATION_TIME, new Date().toString());
        encoder.addText("plotInfo", getImageMetadata() );
        try {
            logger.fine("Encoding image into png");
            encoder.write((BufferedImage) image, out);
            logger.log(Level.FINE, "write png file {0}", filename);
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
            DasLogger.getLogger(DasLogger.GRAPHICS_LOG).log(Level.FINE, "write pdf file {0}", filename);
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
        out.close();
    }

    /**
     * @param filename the specified filename
     * @throws IOException if there is an error opening the file for writing
     */
    public void writeToSVG(String filename) throws IOException {
        try {
            writeToGraphicsOutput(filename, "org.das2.util.awt.SvgGraphicsOutput");
            DasLogger.getLogger(DasLogger.GRAPHICS_LOG).log(Level.FINE, "write svg file {0}", filename);
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
        DasCanvasComponent[] cc = this.getCanvasComponents();
        boolean result = false;
        for (int i = 0; i < cc.length; i++) {
            result = result | cc[i].isDirty();
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

        System.err.println("==Dump Future Events==");
        EventQueueBlocker_1.dumpEventQueue( System.err );
        System.err.println("======================");
        
        
        EventQueue eventQueue= Toolkit.getDefaultToolkit().getSystemEventQueue();
        while ( eventQueue.peekEvent( DasUpdateEvent.DAS_UPDATE_EVENT_ID )!=null ) {
            try {
                Thread.sleep(100);
                System.err.println("==Dump Future Events...==");
                EventQueueBlocker_1.dumpEventQueue( System.err );
                System.err.println("=========================");
                
            }catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
                
        logger.fine("canvas is idle");
        /* should be in static state */

    }

    /**
     * return a list of pending changes.
     * @see waitUntilIdle
     * @param result
     */
    public void pendingChanges( Map<Object,Object> result ) {
        stateSupport.pendingChanges(result);
        if ( Toolkit.getDefaultToolkit().getSystemEventQueue().peekEvent(DasUpdateEvent.DAS_UPDATE_EVENT_ID) != null ) {
            result.put( "dasUpdate", "eventQueueContainsUpdateEvents");
            //TODO: it would be better to check for the eventQueueBlocker object as well.
        }
    }

    /**
     * introduced as a kludgy way for clients to force the canvas to resize all of its components.
     * validate or revalidate should probably do this.
     */
    public void resizeAllComponents() {
        for ( int i=0; i<devicePositionList.size(); i++ ) {
            ((DasDevicePosition)devicePositionList.get(i)).revalidate();
        }
        for (int i = 0; i < getComponentCount(); i++) {
            Component c = getComponent(i);
            if (c instanceof DasCanvasComponent) {
                ((DasCanvasComponent) c).resize();
            }
        }
    }

    /**
     * process all pending operations and make sure we're repainted.  See PlotCommand in Autoplot.
     */
    public void waitUntilValid() {
        for (int i = 0; i < getComponentCount(); i++) {
            Component c = getComponent(i);
            if (c instanceof DasPlot) {
                ((DasPlot) c).invalidateCacheImage();
            }
        }
        if ( ! this.isShowing() || "true".equals(DasApplication.getProperty("java.awt.headless", "false")) ) {
            this.addNotify();
            logger.log(Level.FINER, "setSize({0})", getPreferredSize());
            this.setSize(getPreferredSize());
            logger.finer("validate()");
            this.validate();

            resizeAllComponents();
        } else {
            resizeAllComponents();
        }
        try {
            waitUntilIdle();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
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
    public void prepareForOutput(int width, int height) {
        if (SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("dasCanvas.prepareForOutput must not be called from event queue!");
        setPreferredWidth(width);
        setPreferredHeight(height);

        if ( ! this.isShowing() || "true".equals(DasApplication.getProperty("java.awt.headless", "false")) ) {
            this.addNotify();
            logger.log(Level.FINER, "setSize({0})", getPreferredSize());
            this.setSize(getPreferredSize());
            logger.finer("validate()");
            this.validate();

            resizeAllComponents();
        }
        try {
            waitUntilIdle();
        } catch (InterruptedException ex) {
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

        long t0= System.currentTimeMillis();

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

        logger.log(Level.FINE, "time to getImage: {0}ms", (System.currentTimeMillis() - t0));
        return image;
    }

    /**
     * Creates a BufferedImage by blocking until the image is ready.  This
     * includes waiting for datasets to load, etc.  Works by submitting
     * an invokeAfter request to the RequestProcessor that calls
     * {@link #writeToImageImmediately(Image)}.
     *
     * Note, this calls writeToImageImmediatelyNonPrint, which avoids the usual overhead of
     * revalidating the DasPlot elements we normally do when printing to a new device.
     *
     * @param width
     * @param height
     * @return
     */
    public Image getImageNonPrint(int width, int height) {

        long t0= System.currentTimeMillis();

        String msg = "dasCanvas.getImage(" + width + "," + height + ")";
        logger.fine(msg);

        prepareForOutput(width, height);

        final Image image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        if (SwingUtilities.isEventDispatchThread()) {
            writeToImageImmediatelyNonPrint(image);
        } else {
            Runnable run = new Runnable() {

                public void run() {
                    logger.fine("writeToImageImmediately");
                    writeToImageImmediatelyNonPrint(image);
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

        logger.log(Level.FINE, "time to getImageNonPaint: {0}ms", (System.currentTimeMillis() - t0));
        return image;
    }

    /** TODO
     * @param image
     */
    public void writeToImageImmediately(Image image) {
        Graphics2D graphics;
        try {
            synchronized (displayLockObject) {
                if (displayLockCount != 0) {
                    displayLockObject.wait();
                }
            }
        } catch (InterruptedException ex) {
        }
        graphics = (Graphics2D) image.getGraphics();
        graphics.setColor(this.getBackground());
        graphics.fillRect(0, 0, image.getWidth(this), image.getHeight(this));
        graphics.setColor(this.getForeground());
        graphics.setBackground(this.getBackground());
        print(graphics);
    }

    /**
     * This by passes the normal print method used in writeToImageImmedately, which sets the printing flags which
     * tell the components, like DasPlot, to fully reset.  This was introduced so that Autoplot could get thumbnails
     * and an image of the canvas for its layout tab without having to reset.
     */
     public void writeToImageImmediatelyNonPrint( Image image ) {
        long t0= System.currentTimeMillis();
        Graphics2D graphics;
        try {
            synchronized (displayLockObject) {
                if (displayLockCount != 0) {
                    displayLockObject.wait();
                }
            }
        } catch (InterruptedException ex) {
        }
        graphics = (Graphics2D) image.getGraphics();

        // code from print method.  Here we do the print stuff, but don't turn on the printing flag.
            setOpaque(false);

            Graphics2D g= (Graphics2D)graphics;
            // if svg
            g.setColor( this.getBackground() );
            g.fillRect(0,0,getWidth(),getHeight());
            g.setColor( this.getForeground() );
            g.setBackground( this.getBackground() );

            // avoid calling the overrided print method, which turns on flags for printing.
            super.print(g);

        logger.log(Level.FINE, "time to writeToImageImmediatelyNonPaint: {0}ms", (System.currentTimeMillis() - t0));
    }

    private PropertyChangeListener repaintListener= new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
            repaint();
        }
    };

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

        row.addPropertyChangeListener(repaintListener);
        column.addPropertyChangeListener(repaintListener);
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

    public static final String PROP_BASEFONT= "baseFont";

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
        logger.fine("setBaseFont("+font+")");
        Font oldFont = getFont();
        Font oldBaseFont= baseFont;
        this.baseFont = font;
        if ( scaleFonts ) {
            setFont(getFontForSize(getWidth(), getHeight()));
        } else {
            setFont( font );
        }
        firePropertyChange("font", oldFont, getFont()); //TODO: really?
        firePropertyChange("baseFont", oldBaseFont, this.baseFont );
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
            for ( Painter p : getCanvas().topDecorators ) {
                try {
                    long t0= System.currentTimeMillis();
                    Graphics2D g22= (Graphics2D) g2.create(); // create a graphics object in case they reset colors, etc.
                    p.paint(g22);
                    long dt= System.currentTimeMillis()-t0;
                    if (  dt > 120 ) { // warn if painters are taking more than 120 ms to paint.
                        System.err.println("painter is taking too long to paint ("+dt+" ms): "+p );
                    }
                } catch ( Exception ex ) {
                    g.drawString( "topDecorator causes exception: "+ex.toString(), 20, 20 );
                    ex.printStackTrace();
                }
            }
            g2.dispose();
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


    /** TODO
     * @return
     */
    public boolean getEditingMode() {
        return editable;
    }

    /** TODO
     * @param b
     */
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
    public String getDasName() {
        return dasName;
    }

    /** TODO
     * @param name
     * @throws DasNameException
     */
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

    public DasApplication getDasApplication() {
        return getApplication();
    }


    public DasCanvasComponent getCanvasComponents(int index) {
        return (DasCanvasComponent) getComponent(index + 1);
    }

    public DasCanvasComponent[] getCanvasComponents() {
        Component[] cc= getComponents();
        int n = cc.length - 1;
        DasCanvasComponent[] result = new DasCanvasComponent[n];
        for (int i = 0; i < n; i++) {
            result[i] = (DasCanvasComponent) cc[i+1];
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

        final void refresh() {
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

        @Override
        public boolean equals(Object o) {
            if (o instanceof HotLine) {
                HotLine h = (HotLine) o;
                return h.devicePosition == devicePosition && h.minOrMax == minOrMax;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return minOrMax * devicePosition.hashCode();
        }

        @Override
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

        public void propertyChange(PropertyChangeEvent e) {
            if (e.getSource() == row) {
                rc.y = (int) Math.floor(row.getDMinimum() + 0.5);
                rc.height = (int) Math.floor(row.getDMaximum() + 0.5) - rc.y;
            } else {
                rc.x = (int) Math.floor(column.getDMinimum() + 0.5);
                rc.width = (int) Math.floor(column.getDMaximum() + 0.5) - rc.x;
            }
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 79 * hash + (this.row != null ? this.row.hashCode() : 0);
            hash = 79 * hash + (this.column != null ? this.column.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Cell) {
                Cell box = (Cell) o;
                return box.row == row && box.column == column;
            }
            return false;
        }

        @Override
        public String toString() {
            return "{" + row.getDasName() + " x " + column.getDasName() + ": " + rc.toString() + "}";
        }

        /** get the bounds
         * @return
         */
        public Rectangle getCellBounds() {
            return new Rectangle(rc);
        }

        /** get the bounds
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

        /** get the Row
         * @return
         */
        public DasRow getRow() {
            return row;
        }

        /** get the Column
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

    /**
     * set the current canvas;
     */
    public void makeCurrent() {
        CanvasAction.currentCanvas= this;
    }

    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

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

    public boolean getScrollableTracksViewportWidth() {
        return fitted;
    }

    public boolean getScrollableTracksViewportHeight() {
        return fitted;
    }

    /**
     * indicate to the canvas that a change will be made soon. 
     * For example, the canvas should wait for the change to be performed before creating an image.
     * @see ChangesSupport
     * @param client the client registering the change
     * @param lockObject an object identifying the change
     */
    public void registerPendingChange(Object client, Object lockObject) {
        stateSupport.registerPendingChange(client, lockObject);
    }

    /**
     * indicate to the canvas that a change is being performed.
     * @see ChangesSupport
     * @param client the client registering the change
     * @param lockObject an object identifying the change
     */
    public void performingChange(Object client, Object lockObject) {
        stateSupport.performingChange(client, lockObject);
    }

    /**
     * indicate to the canvas that a change is now complete.
     * @see ChangesSupport
     * @param client the client registering the change
     * @param lockObject an object identifying the change
     */
    public void changePerformed(Object client, Object lockObject) {
        stateSupport.changePerformed(client, lockObject);
    }

    /**
     * returns true if there are changes pending.
     * @see ChangesSupport
     * @return
     */
    public boolean isPendingChanges() {
        return stateSupport.isPendingChanges();
    }

    /**
     * access the lock for an atomic operation. 
     * @see ChangesSupport
     * @return
     */
    public Lock mutatorLock() {
        return stateSupport.mutatorLock();
    }

    /**
     * returns true if an operation is being performed that should be treated as atomic.
     * @see ChangesSupport
     * @return
     */
    public boolean isValueAdjusting() {
        return stateSupport.isValueAdjusting();
    }
}
