/* File: Renderer.java
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

import org.das2.util.ColorUtil;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.util.logging.Level;
import org.das2.dataset.NoDataInIntervalException;
import org.das2.dataset.DataSetConsumer;
import org.das2.dataset.DataSetDescriptor;
import org.das2.dataset.VectorUtil;
import org.das2.dataset.TableDataSet;
import org.das2.dataset.TableUtil;
import org.das2.dataset.VectorDataSet;
import org.das2.DasApplication;
import org.das2.DasException;
import org.das2.graph.DasAxis.Memento;
import java.beans.PropertyChangeListener;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.components.propertyeditor.Editable;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import org.das2.components.propertyeditor.Displayable;
import org.das2.dataset.DataSetAdapter;
import org.das2.datum.Datum;
import org.das2.util.LoggerManager;
import org.das2.qds.DataSetUtil;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;

public abstract class Renderer implements DataSetConsumer, Editable, Displayable {

    protected static final Logger logger= LoggerManager.getLogger("das2.graphics.renderer");
    /**
     * identifies the dataset (in the DataSetDescriptor sense) being plotted
     * by the Renderer.  May be null if no such identifier exists.  See
     * DataSetDescriptor.create( String id ).
     */
    String dataSetId;
    /**
     * The dataset that is being plotted by the Renderer.
     */
    protected QDataSet ds;
    /**
     * Memento for x axis state last time updatePlotImage was called.
     */
    private DasAxis.Memento xmemento;
    /**
     * Memento for y axis state last time updatePlotImage was called.
     */
    private DasAxis.Memento ymemento;
    /**
     * plot containing this renderer
     */
    private DasPlot parent;
    //DasPlot2 parent2;
    /**
     * the responsibility of keeping a relevant dataset loaded.  Can be null
     * if a loading mechanism is not used.  The DataLoader will be calling
     * setDataSet and setException.
     */
    DataLoader loader;
    /**
     * When a dataset cannot be loaded, the exception causing the failure
     * will be rendered instead.
     */
    protected Exception lastException;
    /**
     * This is the exception to be rendered.  This is so if an exception occurs during drawing, then this will be drawn instead.
     */
    protected Exception renderException;

    /**
     * keep track of first and last valid points of the dataset to simplify
     * subclass code and allow them to check if there are any valid points.
     */
    protected int firstValidIndex=-1;
    protected int lastValidIndex=-1;

    private static final String PROPERTY_ACTIVE = "active";
    private static final String PROPERTY_DATASET = "dataSet";

    protected Renderer(DataSetDescriptor dsd) {
        this.loader = new XAxisDataLoader(this, dsd);
    }

    protected Renderer(QDataSet ds) {
        this.ds = ds;
        this.loader = null;
    }

    protected Renderer() {
        this((DataSetDescriptor) null);
    }

    public DasPlot getParent() {
        return this.parent;
    }

    public void setParent( DasPlot parent ) {
        this.parent= parent;
    }
    
    public Memento getXmemento() {
        return xmemento;
    }

    public Memento getYmemento() {
        return ymemento;
    }

    public static boolean isTableDataSet( QDataSet ds ) {
        return SemanticOps.isTableDataSet(ds);
    }

    protected Set<String> needWorkMarkers= Collections.synchronizedSet( new HashSet<String>() );
    
    protected final String MARKER_DATASET= "dataset";
    protected final String MARKER_X_AXIS_RANGE= "xaxisRange";
    protected final String MARKER_Y_AXIS_RANGE= "yaxisRange";
    
    /**
     * find the first and last valid data points.  This is an inexpensive
     * calculation which is only done when the dataset changes.  It improves
     * update and render codes by allowing them to skip initial fill data
     * and more accurately report the presence of off-screen valid data.
     * preconditions: setDataSet is called with null or non-null dataset.
     * postconditions: firstValid and lastValid are set.  In the case of a
     * null dataset, firstValid and lastValid are set to 0.
     */
    private void updateFirstLastValid() {
        if ( ds==null || ds.rank()==0 ) {
            firstValidIndex=0;
            lastValidIndex=0;
        } else {
            if ( SemanticOps.isTableDataSet(ds) ) {
                firstValidIndex= 0;
                lastValidIndex= ds.length();
            } else if ( SemanticOps.isSimpleBundleDataSet(ds) ) {
                firstValidIndex= 0;
                lastValidIndex= ds.length();
            } else {
                firstValidIndex= -1;
                lastValidIndex= -1;
                QDataSet wds= DataSetUtil.weightsDataSet(ds);
                if ( wds.rank()==1 ) {
                    for ( int i=0; firstValidIndex==-1 && i<ds.length(); i++ ) {
                        if ( wds.value(i)>0 ) firstValidIndex=i;
                    }
                    for ( int i=ds.length()-1; lastValidIndex==-1 && i>=0; i-- ) {
                        if ( wds.value(i)>0 ) lastValidIndex=i+1;
                    }
                } else {
                    firstValidIndex= 0;
                    lastValidIndex= wds.length();
                }
            }
        }
    }

    protected void invalidateParentCacheImage() {
        DasPlot lparent= parent;
        if (lparent != null) lparent.invalidateCacheImage();
    }

    /**
     * returns the current dataset being displayed.
     * @return 
     */
    public QDataSet getDataSet() {
        return this.ds;
    }

    /**
     * Renderers should use this internally instead of getDataSet() to support
     * subclasses preprocessing datasets
     * @return 
     */
    protected QDataSet getInternalDataSet(){
        return getDataSet();
    }
    /**
     * return the data for DataSetConsumer, which might be rebinned.
     * @return 
     */
    @Override
    public QDataSet getConsumedDataSet() {
        return this.ds;
    }

    private boolean dumpDataSet;

    /** Getter for property dumpDataSet.
     * @return Value of property dumpDataSet.
     *
     */
    public boolean isDumpDataSet() {
        return this.dumpDataSet;
    }

    /** Setter for property dumpDataSet setting this to
     * true causes the dataSet to be dumped.
     * @param dumpDataSet New value of property dumpDataSet.
     *
     */
    public void setDumpDataSet(boolean dumpDataSet) {
        this.dumpDataSet = dumpDataSet;
        if (dumpDataSet == true) {
            try {
                if (ds == null) {
                    setDumpDataSet(false);
                    throw new DasException("data set is null");
                } else {
                    JFileChooser chooser = new JFileChooser();
                    int xx = chooser.showSaveDialog(this.getParent());
                    if (xx == JFileChooser.APPROVE_OPTION) {
                        File file = chooser.getSelectedFile();
                        if ( isTableDataSet(ds) ) {
                            TableUtil.dumpToAsciiStream( (TableDataSet) DataSetAdapter.createLegacyDataSet(ds), new FileOutputStream(file));
                        } else if (ds instanceof VectorDataSet) {
                            VectorUtil.dumpToAsciiStream((VectorDataSet) DataSetAdapter.createLegacyDataSet(ds), new FileOutputStream(file));
                        } else {
                            throw new DasException("don't know how to serialize data set: " + ds);
                        }
                    }
                    setDumpDataSet(false);
                }
            } catch (HeadlessException | FileNotFoundException | DasException e) {
                DasApplication.getDefaultApplication().getExceptionHandler().handle(e);
            }
            this.dumpDataSet = dumpDataSet;
        }
    }

    protected Painter topDecorator = null;

    public static final String PROP_TOPDECORATOR = "topDecorator";

    public Painter getTopDecorator() {
        return topDecorator;
    }

    /**
     * add additional painting code to the renderer, which is called after 
     * the renderer is called.
     * @param topDecorator the Painter to call, or null to clear.
     */
    public void setTopDecorator(Painter topDecorator) {
        Painter oldTopDecorator = this.topDecorator;
        this.topDecorator = topDecorator;
        updateCacheImage();
        propertyChangeSupport.firePropertyChange(PROP_TOPDECORATOR, oldTopDecorator, topDecorator);
    }

    /**
     * TODO: what is the difference between lastException and exception?
     * @param e 
     */
    public void setLastException(Exception e) {
        this.lastException = e;
        this.renderException = lastException;
    }

    public Exception getLastException() {
        return this.lastException;
    }

    /**
     * return true if the dataset appears to be in a scheme accepted by this renderer.  This should assume that axis
     * units can be reset, etc.
     * @param ds
     * @return true if the dataset appears to be acceptable.
     */
    public boolean acceptsDataSet( QDataSet ds ) {
        return true;
    }
    /**
     * Set the dataset to be plotted.  Different renderers accept QDataSets with
     * different schemes.  For example SeriesRenderer takes:
     *   ds[t]   rank 1 dataset with rank 1 DEPEND_0  or
     *   ds[t,n] rank 2 bundle dataset with X in ds[t,0] and Y in ds[t,n-1]
     * and SpectrogramRenderer takes:
     *   ds[t,y] rank 2 table dataset
     *   ds[n,t,y]  rank 3 dataset with the first dimension join
     * See each renderer's documentation for the schemes it takes.
     * Note the lastException property is cleared, even when the dataset is null.
     * @param ds
     */
    public void setDataSet(QDataSet ds) {
        logger.log(Level.FINE, "Renderer.setDataSet {0}: {1}", new Object[]{id, String.valueOf(ds) });

        QDataSet oldDs = this.ds;
        boolean update= lastException!=null || oldDs!=ds ;
        
        this.lastException = null;

        if ( update ) {
            synchronized(this) {
                updateFirstLastValid();
                this.ds = ds;
            }
            needWorkMarkers.add( MARKER_DATASET );
            //refresh();
            update();
            invalidateParentCacheImage();
            propertyChangeSupport.firePropertyChange(PROPERTY_DATASET, oldDs, ds);
        }
    }

    /**
     * set the exception to be rendered instead of the dataset.
     * @param e
     */
    public void setException(Exception e) {
        logger.log(Level.FINE, "Renderer.setException {0}: {1}", new Object[] { id, String.valueOf(e) });
        Exception oldException = this.lastException;
        this.lastException = e;
        this.renderException = lastException;
        if ( parent != null && oldException != e) {
            //parent.markDirty();
            //parent.update();
            update();
            //refresh();
            invalidateParentCacheImage();
        }
    //refresh();
    }

    public void setDataSetID(String id) throws org.das2.DasException {
        if (id == null) throw new NullPointerException("Null dataPath not allowed");
        if (id.equals("")) {
            setDataSetDescriptor(null);
            return;
        }
        try {
            DataSetDescriptor dsd = DataSetDescriptor.create(id);
            setDataSetDescriptor(dsd);
        } catch (DasException ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    public String getDataSetID() {
        if (getDataSetDescriptor() == null) {
            return "";
        } else {
            return getDataSetDescriptor().getDataSetID();
        }
    }


    /**
     * allocate a bunch of canonical properties.  See http://autoplot.org/developer.guessRenderType#Proposed_extensions
     */
    public static final String CONTROL_KEY_COLOR= "color";
    public static final String CONTROL_KEY_FILL_COLOR= "fillColor";
    public static final String CONTROL_KEY_FILL_DIRECTION= "fillDirection"; // "above" "below" "none" "both"
    
    public static final String CONTROL_KEY_COLOR_TABLE= "colorTable";
    public static final String CONTROL_KEY_LINE_THICK= "lineThick";
    public static final String CONTROL_KEY_LINE_STYLE= "lineStyle";
    public static final String CONTROL_KEY_SYMBOL= "symbol";
    public static final String CONTROL_KEY_SYMBOL_SIZE= "symbolSize";
    
    /**
     * font size relative to the parent, so "" or "1em" is the same size.
     */
    public static final String CONTROL_KEY_FONT_SIZE= "fontSize";
    
    public static final String CONTROL_KEY_REFERENCE= "reference";
    public static final String CONTROL_KEY_DRAW_ERROR= "drawError";
    
    public static final String PROP_CONTROL= "control";

    /**
     * generic control string, that is handled by the renderer.  In general, this should be
     * a ampersand-delimited string of name=value pairs.  This may return values that
     * are represented as a separate control, such as color.
     * {@code fill=red,above,5.0;grey,below,0.0&ref=2.5}
     * (Note these are example controls which are not implemented.)
     */
    protected String control="";

    private Map<String,String> controls= Collections.emptyMap();

    /**
     * set the control string which contains a number of properties.  These are defined for
     * each renderer, but they should try to be consistent.  See http://autoplot.org/developer.guessRenderType#Proposed_extensions
     * When overriding this, be sure to call super.
     * @see #CONTROL_KEY_COLOR etc
     * @param s the control
     */
    public void setControl( String s ) {
        String oldValue= this.control;
        this.control= s;
        if ( oldValue==s || ( oldValue != null && oldValue.equals(s) ) ) {
	    return;
	}
        this.controls= parseControl(s);
        update();
        propertyChangeSupport.firePropertyChange(PROP_CONTROL, oldValue, control );
    }

    /**
     * get the string which summarizes the state of the renderer.  These allow a compact string to contain the renderer settings.
     * This should be an ampersand-delimited list of name=value pairs.
     * @return 
     */
    public String getControl() {
        return this.control;
    }


    /**
     * convenient and official location for method that formats control string.
     * @param c
     * @return
     */
    public static String formatControl( Map<String,String> c ) {
        StringBuilder result= new StringBuilder(50);
        
        String ampstr= "&";
        for ( Entry<String,String> ee: c.entrySet() ) {
            if ( ee.getKey().contains("&") ) throw new IllegalArgumentException("keys must be java identifiers");
            if ( ee.getValue().contains("&") ) ampstr= "&amp;";
        }
        boolean amp= false;
        for ( Entry<String,String> ee: c.entrySet() ) {
            if ( amp ) result.append(ampstr); else amp=true;
            result.append( ee.getKey() ) .append("=").append(ee.getValue() );
        }
        return result.toString();
    }

    /**
     * convenient and official location for method that parses control string.
     * This will split on ampersand, and when no ampersands are found then it will
     * try semicolons.  This is to support embedding the control string in 
     * other control strings (like Autoplot URIs) which use ampersands.
     * @param c the control string or null.
     * @return the control string, parsed.
     */
    public static Map<String,String> parseControl( String c ) {
        Map<String,String> result= new LinkedHashMap();
        if ( c==null ) {
            return result;
        }
        String ampstr= "&";
        if ( c.contains("&amp;") ) {
            ampstr= "&amp;";
        }
        if ( c.trim().length()==0 ) return result;
        String[] ss= c.split(ampstr);
        if ( ss.length==1 ) {
            ss= c.split(";");
        }
        for ( int i=0; i<ss.length; i++ ) {
            if ( ss[i].trim().length()==0 ) continue;
            String[] ss2= ss[i].split("=",2);
            if ( ss2.length==1 ) {
                result.put( ss2[0].trim(), "T" ); // true
            } else {
                String k= ss2[0].trim();
                String v= ss2[1].trim();
                result.put( k,v );
            }
        }
        return result;
    }

    /**
     * Get the control.  This provides an easy way for renderers to have controls in a compact string.
     * @param key the key name.
     * @param deft the default value.
     * @return the string value of the control.
     * @see #getDoubleControl(java.lang.String, double) 
     * @see #getBooleanControl(java.lang.String, boolean) 
     */
    public String getControl( String key, String deft ) {
        if ( this.control.trim().length()==0 ) return deft;
        String v= controls.get(key);
        if ( v!=null ) return v; else return deft;
    }

    /**
     * return true if the control is specified.
     * @param key the key name
     * @return true if the control is specified.
     */
    public boolean hasControl( String key ) {
        if ( this.control.trim().length()==0 ) return false;
        return controls.containsKey(key);
    }
    
    /**
     * get the boolean control.
     * @param key the key name.
     * @param deft the default value.
     * @return the boolean value, where "T" is true, false otherwise; or the default when the value is not found.
     */
    public boolean getBooleanControl( String key, boolean deft ) {
        String v= controls.get(key);
        if ( v!=null ) return v.equalsIgnoreCase("T"); else return deft;
    }

    /**
     * return the encoding for the boolean value.
     * @param v the boolean value.
     * @return "T" or "F"
     */
    public static String encodeBooleanControl( boolean v ) {
        return v ? "T" : "F";
    }
    
    /**
     * get the double control.
     * @param key the key name.
     * @param deft the default value.
     * @return the double, parsed with Double.parseDouble; or the default when the value is not found.
     */
    public double getDoubleControl( String key, double deft ) {
        String v= controls.get(key);
        if ( v!=null ) {
            try {
                return Double.parseDouble(v);
            } catch ( NumberFormatException ex ) {
                logger.log( Level.WARNING, "Unable to parse as double: {0}", key);
                return deft;
            }
        } else {
            return deft;
        }
    }

    /**
     * get the integer control.
     * @param key the key name.
     * @param deft the default value.
     * @return the int, parsed with Integer.parseInt; or the default when the value is not found.
     */
    public int getIntegerControl( String key, int deft ) {
        String v= controls.get(key);
        if ( v!=null ) {
            try {
                return Integer.parseInt(v);
            } catch ( NumberFormatException ex ) {
                logger.log( Level.WARNING, "Unable to parse as int: {0}", key);
                return deft;
            }
        } else {
            return deft;
        }
    }

    /**
     * get the double array control.  These should be encoded on a string
     * with commas delimiting values.
     * @param key the key name.
     * @param deft the default value.
     * @return the double array, each element parsed with Double.parseDouble or the default when the value is not found.
     */
    public double[] getDoubleArrayControl( String key, double[] deft ) {
        String v= controls.get(key);
        if ( v!=null ) {
            try {
                String[] ss= v.split(",");
                double[] result= new double[ss.length];
                for ( int i=0; i<ss.length; i++ ) {
                    result[i]= Double.parseDouble(ss[i]);
                }
                return result;
            } catch ( NumberFormatException ex ) {
                logger.log( Level.WARNING, "Unable to parse as double array: {0}", key);
                return deft;
            }
        } else {
            return deft;
        }
    }

    /**
     * get the Datum control. 
     * @param key the key name.
     * @param deft the default value, which also provides the units.
     * @return the Datum or the default when the value is not found.
     */
    public Datum getDatumControl( String key, Datum deft ) {
        String v= controls.get(key);
        if ( v!=null ) {
            try {
                return deft.getUnits().parse(key);
            } catch ( ParseException ex ) {
                logger.log( Level.WARNING, "Unable to parse as datum: {0}", key);
                return deft;
            }
        } else {
            return deft;
        }
    }

    /**
     * get the Color control.  
     * @param key the key name.
     * @param deft the default value
     * @return the Color or the default when the value is not found.
     * @see ColorUtil#decodeColor(java.lang.String) 
     */
    public Color getColorControl( String key, Color deft ) {
        String v= this.controls.get(key);
        if ( v!=null ) {
            try { 
                return ColorUtil.decodeColor(v);
            } catch ( NumberFormatException ex ) {
                logger.log( Level.WARNING, "Unable to parse as color: {0}", v);
                return deft;
            }
        } else {
            return deft;
        }
    }
    
    /**
     * encode the Color control.  
     * @param color
     * @return the color encoded as a string.
     * @see ColorUtil#encodeColor(java.awt.Color) 
     */
    public static String encodeColorControl( Color color ) {
        return ColorUtil.encodeColor(color);
    }
    
    /**
     * encode the plot symbol as a string, such as:
     * none, cicles, triangles, cross, ex, star, diamond, box
     * @param psym the plot symbol.
     * @return the string encoding.
     */
    public static String encodePlotSymbolControl( PlotSymbol psym ) {
        return psym.toString().toLowerCase();
    }
    
    /**
     * decode the string into a plot symbol.
     * @param s the symbol name, such as none, circles, triangles, cross, ex, star, diamond, box
     * @param deflt the symbol to use when the value is not parsed.
     * @return the parsed value.
     */
    public static PlotSymbol decodePlotSymbolControl( String s, PlotSymbol deflt ) {
        s= s.toUpperCase();
        switch (s) {
            case "NONE":
                return DefaultPlotSymbol.NONE;
            case "CIRCLES":
                return DefaultPlotSymbol.CIRCLES;
            case "TRIANGLES":
                return DefaultPlotSymbol.TRIANGLES;
            case "CROSS":
                return DefaultPlotSymbol.CROSS;
            case "EX":
                return DefaultPlotSymbol.EX;
            case "STAR":
                return DefaultPlotSymbol.STAR;
            case "DIAMOND":
                return DefaultPlotSymbol.DIAMOND;
            case "BOX":
                return DefaultPlotSymbol.BOX;
            case "TRIANGLESEAST":
                return DefaultPlotSymbol.TRIANGLES_EAST;
            case "TRIANGLESNORTH":
                return DefaultPlotSymbol.TRIANGLES_NORTH;
            case "TRIANGLESWEST":
                return DefaultPlotSymbol.TRIANGLES_WEST;
            case "TRIANGLESSOUTH":
                return DefaultPlotSymbol.TRIANGLES_SOUTH;
            default:
                logger.log(Level.FINE, "unable to parse symbol: {0}", deflt);
                return deflt;
        }
    }
    
    /*
     * returns the AffineTransform to transform data from the last updatePlotImage call
     * axes (if super.updatePlotImage was called), or null if the transform is not possible.
     *
     * @deprecated DasPlot handles the affine transform and previews now.
     */
    protected AffineTransform getAffineTransform(DasAxis xAxis, DasAxis yAxis) {
        if (xmemento == null) {
            logger.fine("unable to calculate AT, because old transform is not defined.");
            return null;
        } else {
            AffineTransform at = new AffineTransform();
            at = xAxis.getAffineTransform(xmemento, at);
            at = yAxis.getAffineTransform(ymemento, at);
            return at;
        }
    }

    /** Render is called whenever the image needs to be refreshed or the content
     * has changed.  This operation should occur with an animation-interactive
     * time scale, and an image should be cached when this is not possible.  The graphics
     * object will have its origin at the upper-left corner of the screen.
     * @param g the graphics context in the canvas reference frame.
     * @param xAxis the axis relating x data coordinates to horizontal pixel coordinates
     * @param yAxis the axis relating y data coordinates to horizontal pixel coordinates
     * @param mon a monitor for the operation.  Note the render operation should 
     *    be fast (&lt;300ms).
     */
    public abstract void render(Graphics g, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon);

    /**
     * Returns true if the render will accept the context for a point.  
     * That is, the renderer affected that point, or nearby points.  This is 
     * used currently to provide a way for the operator to click on a plot and 
     * directly edit the renderer which drew the pixel.
     *
     * @param x the x coordinate in the canvas coordinate system.  
     * @param y the y coordinate in the canvas coordinate system.  
     * @return true if the renderer will accept the context.
     */
    public boolean acceptContext(int x, int y) {
        return false;
    }

    /**
     * render the exception on the graphics context.  Presently this just
     * hands off the exception to the parent plot's postMessage method so
     * it will appear in a message bubble.
     * @param g the graphics context in the canvas reference frame
     * @param xAxis the axis relating x data coordinates to horizontal pixel coordinates
     * @param yAxis the axis relating y data coordinates to horizontal pixel coordinates
     * @param e the exception.
     */
    protected void renderException(Graphics g, DasAxis xAxis, DasAxis yAxis, Exception e) {

        String s;
        String message;

        if (e instanceof NoDataInIntervalException) {
            s = "no data in interval";
            message = e.getMessage();
        } else {
            s = e.getMessage();
            message = "";
            if (s == null || s.length() < 10) {
                s = e.toString();
            }
        }

        if (!message.equals("")) {
            s += ":!c" + message;
        }
        DasPlot lparent= parent;
        if ( lparent!=null ) lparent.postMessage(this, s, DasPlot.SEVERE, null, null);

    }

    /** updatePlotImage is called once the expensive operation of loading
     * the data is completed.  This operation should occur on an interactive
     * time scale.  This is an opportunity to create a cache
     * image of the data with the current axis state, when the render
     * operation cannot operate on an animation interactive time scale.
     * Also, several Renders can be updating at once on separate threads, while
     * the render methods must be called sequentially.
     * 
     * Only Renderer should call this method!  (This should be protected then, but
     * this is not possible because of exiting use.  TODO: introduce "revalidate"
     * to replace this operation.)
     * 
     * @param xAxis the axis relating x data coordinates to horizontal pixel coordinates
     * @param yAxis the axis relating y data coordinates to horizontal pixel coordinates
     * @param monitor a monitor for the operation.  Note the updatePlotImage operation should 
     *    be fast (&lt;1000ms).
     * @throws org.das2.DasException
     */
    public void updatePlotImage(DasAxis xAxis, DasAxis yAxis, ProgressMonitor monitor) throws DasException {
    }

    /**
     * refresh, but only if the parent has been set.
     * @deprecated call refresh which also checks the parent.
     */
    protected void refreshImage() {
        if (getParent() != null) {
   //         refresh();
        }
    }

    /**
     * Something has changed with the Render, and the plot should come back
     * to allow this render to repaint.  Its cacheImage is invalidated and a
     * repaint is posted on the event thread.
     */
    public void update() {
        logger.log(Level.FINE, "Renderer.update {0}", id);
        DasPlot lparent= parent;
        if ( lparent==null ) {
            logger.fine("update but parent was null");
            return;
        }
        lparent.repaint();
        this.xmemento= null;
        this.ymemento= null;
        java.awt.EventQueue eventQueue =
                Toolkit.getDefaultToolkit().getSystemEventQueue();
        DasRendererUpdateEvent drue = new DasRendererUpdateEvent(lparent, this);
        eventQueue.postEvent(drue);
    }

    /**
     * The cacheImage is invalidated and updateEvent posted on the event thread
     * by calling update.
     */
    public void updateCacheImage() {
        DasPlot lparent= parent;
        if ( lparent==null ) {
            logger.fine("update but parent was null");
            return;
        }
        lparent.invalidateCacheImageNoUpdate();
        update();
    }
    
    /**
     * updateImmediately is called from DasPlot when it gets an update event from the
     * AWT Event thread.  This should trigger a data load and eventually a refresh to
     * render the dataset.
     */
    protected void updateImmediately() {
        logger.entering( "org.das.graph.Renderer", "updateImmediately");
        DasPlot lparent= parent;
        if (lparent == null || !lparent.isDisplayable()) {
            logger.exiting("org.das.graph.Renderer", "updateImmediately");
            return;
        }

        // If there's a loader, then tell him he might want to load new data.
        if (loader != null) {
            loader.update();
        }

        logger.exiting("org.das.graph.Renderer", "updateImmediately");
        
        // The parent has already used an AffineTransform to preview the image, but
        // we might as well re-render using the dataset we have.
     //   refresh();
       // lparent.invalidateCacheImageNoUpdate();
    }

    /**
     * if we were asked to refresh, but couldn't because we were on the event 
     * thread, this is called from a different thread.
     * 
     * THIS IS NOT USED PRESENTLY.
     */
    private void refreshImmediately() {
        logger.log(Level.FINE, "update plot image for {0}", id);
        DasPlot lparent= parent;
        if ( lparent==null ) return;
        try {
            final ProgressMonitor progressPanel = DasApplication.getDefaultApplication().getMonitorFactory().getMonitor(parent, "Rebinning data set", "updatePlotImage");
            DasAxis lxaxis= lparent.getXAxis();
            DasAxis lyaxis= lparent.getYAxis();
            updatePlotImage( lxaxis, lyaxis, progressPanel);
            xmemento = lxaxis.getMemento();
            ymemento = lyaxis.getMemento();
            renderException = null;
        } catch (DasException de) {
            // TODO: there's a problem here, that the Renderer can set its own exception and dataset.  This needs to be addressed, or handled as an invalid state.
            logger.log(Level.WARNING, de.getMessage(), de);
            //ds = null;
            renderException = de;
        } catch (RuntimeException re) {
            logger.log(Level.WARNING, re.getMessage(), re);
            renderException = re;
            lparent.invalidateCacheImage();
            throw re;
        } finally {
            // this code used to call finished() on the progressPanel
        }

        logger.fine("invalidate parent cacheImage and repaint");

        lparent.invalidateCacheImage();
        
    }
    
    /**
     * recalculate the plot image and repaint.  The dataset or exception have
     * been updated, or the axes have changed, so we need to perform updatePlotImage
     * to do the expensive parts of rendering.
     * 
     * This is only called from the resize method of DasPlot.
     */
    protected void refresh() {
        //System.err.println("in refresh...");
        if (!isActive()) return;
        DasPlot lparent= parent;
        logger.fine("entering Renderer.refresh");
        if (lparent == null) {
            logger.fine("null parent in refresh");
            return;
        }
        if (!lparent.isDisplayable()) {
            logger.fine("parent not displayable");
            return;
        }

        Runnable run = new Runnable() {  
            @Override
            public void run() {
                refreshImmediately();
            }
        };

        boolean async = true;  // updating is done on the event thread...
        if (EventQueue.isDispatchThread()) {
            if (async) {
                new Thread(run, "updatePlotImage").start();
            } else {
                run.run();
            }
        } else {
            run.run();
        }
    }

    public void setDataSetDescriptor(DataSetDescriptor dsd) {
        if (loader == null) {
            logger.warning("installing loader--danger!");
            loader = new XAxisDataLoader(this, dsd);
        }
        if (loader instanceof XAxisDataLoader) {
            ((XAxisDataLoader) loader).setDataSetDescriptor(dsd);
            DasPlot lparent= parent;
            if (lparent != null) {
                lparent.markDirty("dataSetDescriptor");
                lparent.update();
            }
            this.ds = null;
        } else {
            throw new RuntimeException("loader is not based on DataSetDescriptor");
        }

    }

    public DataLoader getDataLoader() {
        return this.loader;
    }

    public void setDataSetLoader(DataLoader loader) {
        this.loader = loader;
        if (loader != null) loader.update();
    }

    public DataSetDescriptor getDataSetDescriptor() {
        if (loader == null) {
            return null;
        } else {
            if (this.loader instanceof XAxisDataLoader) {
                return ((XAxisDataLoader) loader).getDataSetDescriptor();
            } else {
                return null;
            }
        }
    }

    protected void installRenderer() {
        // override me
    }

    protected void uninstallRenderer() {
        // override me
    }

    // old property overloading was removed because it is no longer used.

    /**
     * display the renderer.  This is allows a renderer to be disabled without removing it from the application.
     */
    public static final String PROP_ACTIVE = "active";

    private boolean active = true;

    /**
     * true when the renderer should be drawn.  
     * @return true when the renderer should be drawn.
     */
    public boolean isActive() {
        return this.active;
    }

    /**
     * set the active property, when false the renderer will not be drawn.
     * This is allows a renderer to be 
     * disabled without removing it from the application.
     * @param active false if the renderer should not be drawn.
     */
    public void setActive(boolean active) {
        boolean oldValue = this.active;
        this.active = active;
        if ( oldValue!=active ) update();
        propertyChangeSupport.firePropertyChange(PROPERTY_ACTIVE, oldValue, active);
    }

     /**
     * If non-null and non-zero-length, use this label to describe the renderer
     * in the plot's legend.
     */
    public static final String PROP_LEGENDLABEL = "legendLabel";

    protected String legendLabel = "";

    /**
     * get the label to describe the renderer in the plot's legend.   
     * If zero-length, then the legend label should be hidden.
     * @return the label to describe the renderer
     */
    public String getLegendLabel() {
        return legendLabel;
    }

    /**
     * set the label to describe the renderer in the plot's legend.  
     * If zero-length, then the legend label should be hidden.
     * @param legendLabel  the label to describe the renderer
     */
    public void setLegendLabel(String legendLabel) {
        String oldLegendLabel = this.legendLabel;
        this.legendLabel = legendLabel;
        propertyChangeSupport.firePropertyChange(PROP_LEGENDLABEL, oldLegendLabel, legendLabel);
        updateCacheImage();
    }

    protected boolean drawLegendLabel = false;
    
    /**
     * true if the legend label should be drawn.
     */
    public static final String PROP_DRAWLEGENDLABEL = "drawLegendLabel";

    /**
     * get the switch used to turn off legend label.  This allows the label
     * to be hidden without loosing the information it provides.
     * @return true if the legend label should be drawn
     */
    public boolean isDrawLegendLabel() {
        return drawLegendLabel;
    }

    /**
     * set the switch used to turn off legend label.  This allows the label
     * to be hidden without loosing the information it provides.
     * @param drawLegendLabel true if the legend label should be drawn
     */
    public void setDrawLegendLabel(boolean drawLegendLabel) {
        boolean oldDrawLegendLabel = this.drawLegendLabel;
        this.drawLegendLabel = drawLegendLabel;
        propertyChangeSupport.firePropertyChange(PROP_DRAWLEGENDLABEL, oldDrawLegendLabel, drawLegendLabel);
        updateCacheImage();
    }

    protected String id = "rend";
    public static final String PROP_ID = "id";

    public String getId() {
        return id;
    }

    public void setId(String id) {
        String oldId = this.id;
        this.id = id;
        propertyChangeSupport.firePropertyChange(PROP_ID, oldId, id);
    }

    @Override
    public void drawListIcon( Graphics2D g, int x, int y ) {
        ImageIcon icon= (ImageIcon) getListIcon();
        g.drawImage(icon.getImage(), x, y, null);
    }

    @Override
    public Icon getListIcon() {
        return new ImageIcon( new BufferedImage( 16, 16, BufferedImage.TYPE_INT_ARGB ) );
    }

    @Override
    public String getListLabel() {
        StringBuilder l= new StringBuilder( getLegendLabel() );
        if ( this.getDataSetDescriptor()!=null ) {
            if ( l.length()>0 ) {
                l.append(" (").append(this.getDataSetDescriptor()).append( ")");
            }
        }
        if ( l.length()==0 ) {
            l.append( this.getClass().getName() );
        }
        return l.toString();
    }

    public static final String PROP_COLORBAR = "colorBar";
    protected DasColorBar colorBar;

    /**
     * set a colorbar for the renderer.  By default, the renderer simply ignores 
     * the colorbar, but instances may introduce special handling.
     * WARNING: some subclasses override this, but do not call super.setColorBar.
     * @param cb a colorbar
     */
    public void setColorBar( DasColorBar cb ) {
        this.colorBar= cb;
    }

    /**
     * get the colorbar for the renderer.  
     * @return the colorbar for the renderer.  
     */
    public DasColorBar getColorBar() {
        return colorBar;
    }

    /**
     * handle the fontSize property, which has values like "1em" and "7px"
     * @param f the parent font.
     * @param fontSize fontSize property, for example "1em" and "7px"
     * @return the relative font.
     */
    public static Font setUpFont( Font f, String fontSize ) {
        if ( fontSize!=null && fontSize.length()>0 && !fontSize.equals("1em") ) {
            try {
                double[] size= DasDevicePosition.parseLayoutStr(fontSize);
                double s= f.getSize2D() * size[0]/100 + f.getSize2D() * size[1] + size[2];
                f= f.deriveFont((float)s);
            } catch ( ParseException ex ) {
                logger.log( Level.WARNING, ex.getMessage(), ex );
            }
            return f;
        } else {
            return f;
        }
    }
    
    /**
     * handle the fontSize property, which has values like "1em" and "7px"
     * @param g1
     * @param fontSize fontSize property, for example "1em" and "7px"
     */
    protected void setUpFont( Graphics g1, String fontSize ) {
        Font f0= getParent().getFont();
        if ( f0==null ) return;
        Font f= setUpFont( f0, fontSize );
        if ( !f.equals(f0) ) {
            g1.setFont(f);
        }
    }
    
    private int renderCount=0;
    private int updateCount=0;
    
    protected synchronized void incrementRenderCount() {
        renderCount++;
    }
    
    /**
     * return the number of times render has been called since the last reset.
     * @return number of times render has been called since the last reset.
     */
    public int getRenderCount() {
        return renderCount;
    }
    
    protected synchronized void incrementUpdateCount() {
        updateCount++;
    }
    
    /**
     * return the number of times updatePlotImage has been called since the last reset.
     * @return the number of times updatePlotImage has been called since the last reset.
     */
    public int getUpdateCount() {
        return updateCount;
    }
    
    /**
     * reset the counters.
     */
    public synchronized void resetCounters() {
        renderCount=0;
        updateCount=0;
    }
    
    private String recordFile = "";

    private PrintStream recordStream=null;
    
    public static final String PROP_RECORDFILE = "recordFile";

    public String getStatsFile() {
        return recordFile;
    }

    /**
     * name of the file where rendering speed is recorded.
     * @param recordFile 
     */
    public void setStatsFile(String recordFile) {
        String oldRecordFile = this.recordFile;
        if ( oldRecordFile!=null && !oldRecordFile.equals(recordFile ) ) {
            if ( recordStream!=null ) {
                recordStream.close();
            }
        }
        this.recordFile = recordFile;
        try {
            resetCounters();
            recordStream= new PrintStream( recordFile );
            recordStream.println("updates, renders, numberOfPoints, seconds, type");
        } catch (FileNotFoundException ex) {            
            logger.log(Level.SEVERE, null, ex);
        }
        propertyChangeSupport.firePropertyChange(PROP_RECORDFILE, oldRecordFile, recordFile);
    }

    /**
     * record the stat to a file.
     * @param numberOfPoints
     * @param millis milliseconds
     * @param t 'r' for rendering, 'u' for updatePlotImage
     */
    protected void addToStats( int numberOfPoints, long millis, char t ) {
        if ( recordStream!=null ) {
            recordStream.format( "%d, %d, %d, %.3f, %c\n", updateCount, renderCount, numberOfPoints, millis/1000., t );
        }
    }
    
    /**
     * Utility field used by bound properties.
     */
    protected java.beans.PropertyChangeSupport propertyChangeSupport = new java.beans.PropertyChangeSupport(this);

    /**
     * Adds a PropertyChangeListener to the listener list.
     * @param l The listener to add.
     */
    public void addPropertyChangeListener(java.beans.PropertyChangeListener l) {
        //System.err.println(""+this+" addPCL <>" );
        propertyChangeSupport.addPropertyChangeListener(l);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        //System.err.println(""+this+" addPCL "+propertyName);
        propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
    }

    /**
     * Removes a PropertyChangeListener from the listener list.
     * @param l The listener to remove.
     */
    public void removePropertyChangeListener(java.beans.PropertyChangeListener l) {
        //System.err.println(""+this+" removePCL <>" );
        propertyChangeSupport.removePropertyChangeListener(l);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        //System.err.println(""+this+" removePCL "+propertyName);
        propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
    }

}
