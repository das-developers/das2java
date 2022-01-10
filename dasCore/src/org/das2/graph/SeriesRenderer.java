/* File: SeriesRenderer.java
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import org.das2.DasApplication;
import org.das2.DasProperties;
import org.das2.dataset.VectorUtil;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.InconvertibleUnitsException;
import org.das2.datum.Units;
import org.das2.datum.UnitsConverter;
import org.das2.datum.UnitsUtil;
import org.das2.event.CrossHairMouseModule;
import org.das2.event.DasMouseInputAdapter;
import org.das2.event.LengthDragRenderer;
import org.das2.event.LengthMouseModule;
import org.das2.event.MouseModule;
import org.das2.system.DasLogger;
import org.das2.util.LoggerManager;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.qds.ArrayDataSet;
import org.das2.qds.DataSetOps;
import org.das2.qds.DataSetUtil;
import org.das2.qds.MutablePropertyDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.examples.Schemes;
import org.das2.qds.ops.Ops;
import org.das2.qds.util.DataSetBuilder;
import org.das2.qds.util.Reduction;
import org.das2.util.monitor.NullProgressMonitor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * SeriesRender is a high-performance replacement for the SymbolLineRenderer.
 * The SymbolLineRenderer is limited to about 30,000 points, beyond which 
 * contracts for speed start breaking, degrading usability.  The goal of the
 * SeriesRenderer is to plot 1,000,000 points without breaking the contracts.
 * 
 * It should be said that five years after its introduction that it's still quite limited.
 * 
 * The SeriesRenderer has a few additional features, such as error bars and 
 * fill-to-reference.  These are implemented as "render elements" so that work 
 * is encapsulated.
 * 
 * @author  jbf
 */
public class SeriesRenderer extends Renderer {

    private DefaultPlotSymbol psym = DefaultPlotSymbol.CIRCLES;
    private float symSize = 3.0f; // radius in pixels

    private float lineWidth = 1.0f; // width in pixels

    private boolean histogram = false;
    private PsymConnector psymConnector = PsymConnector.SOLID;
    private FillStyle fillStyle = FillStyle.STYLE_SOLID;
    //private int renderCount = 0;
    //private int updateImageCount = 0;
    private Color color = Color.BLACK;
    private long lastUpdateMillis;
    private boolean antiAliased = "on".equals(DasProperties.getInstance().get("antiAlias"));
    
    /**
     * background thick paints a thicker background color under for contrast.
     */
    public static final String CONTROL_KEY_BACKGROUND_THICK= "backgroundThick";
    
    /**
     * fill style is whether the symbol is filled in or not.
     */
    public static final String CONTROL_KEY_FILL_STYLE="fillStyle";
    
    /**
     * the index of the first point drawn, nonzero when X is monotonic and we can clip. 
     */
    private int firstIndex=-1;
    /**
     * the non-inclusive index of the last point drawn. 
     */
    private int lastIndex=-1;
    
    private boolean dataIsMonotonic= false;
    /**
     * total number of points plotted
     */
    private int numberOfPoints;
    
    /**
     * the index of the first point drawn that is visible, nonzero when X is monotonic and we can clip. 
     */
    private int firstIndex_v=-1;
    /**
     * the non-inclusive index of the last point that is visible. 
     */
    private int lastIndex_v=-1;
    private int dslen=-1; // length of dataset, compare to firstIndex_v.

    boolean unitsWarning= false; // true indicates we've warned the user that we're ignoring units.
    boolean xunitsWarning= false;

    private static final Logger logger = LoggerManager.getLogger("das2.graphics.renderer.series");
    /**
     * indicates the dataset was clipped by dataSetSizeLimit 
     */
    private boolean dataSetClipped;

    /**
     * the dataset was reduced 
     */
    private boolean dataSetReduced;
    
    public SeriesRenderer() {
        super();
        updatePsym();
    }
    Image psymImage;
    Image[] coloredPsyms;
    int cmx, cmy;
    FillRenderElement fillElement = new FillRenderElement();
    ErrorBarRenderElement errorElement = new ErrorBarRenderElement();
    PsymConnectorRenderElement psymConnectorElement = new PsymConnectorRenderElement();
    PsymRenderElement psymsElement = new PsymRenderElement();

    QDataSet xds;
    QDataSet yds;
    QDataSet zds;
    
    @Override
    public void setDataSet(QDataSet ds) {
        if ( ds==null || ds.rank()>0 ) {
            super.setDataSet(ds);
        } else {
            QDataSet xtags= getXTags(ds);
            QDataSet d= Ops.bundle(xtags,ds);
            QDataSet bds= (QDataSet) d.property(QDataSet.BUNDLE_0);
            QDataSet j= Ops.join( null, d );
            j= Ops.putProperty( j, QDataSet.BUNDLE_1, bds ); // See https://sourceforge.net/p/autoplot/bugs/2244/
            super.setDataSet( j );
        }
        if ( ds==null ) {
            xds= null;
            yds= null;
            zds= null;
        } else {
            xds= getXTags(ds);
            yds= ytagsDataSet(ds);
            zds= colorByDataSet(ds);
        }
    }
    
    
    /**
     * if true and the dataset contains limits information (warning range, nominal range), show these ranges.
     */
    private boolean showLimits = true;

    public static final String PROP_SHOWLIMITS = "showLimits";

    public boolean isShowLimits() {
        return showLimits;
    }

    /**
     * if the dataset contains metadata describing nominal and warning ranges, display them.  Currently
     * this is found in CDF metadata, but should become part of QDataSet.
     * @param showLimits 
     */
    public void setShowLimits(boolean showLimits) {
        boolean oldShowLimits = this.showLimits;
        this.showLimits = showLimits;
        updateCacheImage();
        propertyChangeSupport.firePropertyChange(PROP_SHOWLIMITS, oldShowLimits, showLimits);
    }

    /**
     * the selectionArea, which can be null.
     */
    Shape selectionArea; 

    boolean haveValidColor= true;

    interface RenderElement {
        
        /**
         * render a background on which the element is to be rendered.  All
         * backgrounds are drawn first, then all elements.
         * @param g 
         */
        void renderBackground( Graphics2D g );
        
        int render(Graphics2D g, DasAxis xAxis, DasAxis yAxis, QDataSet vds, ProgressMonitor mon);

        void update(DasAxis xAxis, DasAxis yAxis, QDataSet vds, ProgressMonitor mon);

        boolean acceptContext(Point2D.Double dp);
    }

    @Override
    public void setControl(String s) {
        super.setControl(s);
        setColor( getColorControl( CONTROL_KEY_COLOR, color ) );
        setFillColor( getColorControl( CONTROL_KEY_FILL_COLOR, fillColor ));
        setFillDirection( getControl( CONTROL_KEY_FILL_DIRECTION, "both" ) );
        setLineWidth( getDoubleControl( CONTROL_KEY_LINE_THICK, lineWidth ) );
        setSymSize( getDoubleControl( CONTROL_KEY_SYMBOL_SIZE, symSize ) );
        setPsym( decodePlotSymbolControl( getControl( CONTROL_KEY_SYMBOL, encodePlotSymbolControl(psym) ), psym ) );
        setDrawError( getBooleanControl( CONTROL_KEY_DRAW_ERROR, drawError ) );
        setBackgroundThick( getControl( CONTROL_KEY_BACKGROUND_THICK, backgroundThick ) );
        setFillStyle( decodeFillStyle( getControl( CONTROL_KEY_FILL_STYLE, encodeFillStyle(fillStyle) ), fillStyle ) );
    }

    @Override
    public String getControl() {
        Map<String,String> controls= new LinkedHashMap();
        controls.put( CONTROL_KEY_COLOR, encodeColorControl(color) );
        controls.put( CONTROL_KEY_FILL_COLOR, encodeColorControl(fillColor) );
        controls.put( CONTROL_KEY_FILL_DIRECTION, String.valueOf(fillDirection) );
        controls.put( CONTROL_KEY_LINE_THICK, String.valueOf(lineWidth) );
        controls.put( CONTROL_KEY_SYMBOL_SIZE, String.valueOf( symSize ) );
        controls.put( CONTROL_KEY_SYMBOL, encodePlotSymbolControl(psym) );
        controls.put( CONTROL_KEY_DRAW_ERROR, encodeBooleanControl( drawError ) );
        controls.put( CONTROL_KEY_BACKGROUND_THICK, backgroundThick );
        controls.put( CONTROL_KEY_FILL_STYLE, encodeFillStyle(fillStyle) );
        return formatControl(controls);
    }
    
    
    private QDataSet ytagsDataSet( QDataSet ds ) {
        QDataSet vds;
        if ( ds.rank()==2 && SemanticOps.isBundle(ds) ) {
            vds= SemanticOps.ytagsDataSet(ds);
        } else if ( ds.rank()==2 ) {
            getParent().postMessage(this, "dataset is rank 2 and not a bundle", DasPlot.INFO, null, null);
            return null;
        } else {
            vds = (QDataSet) ds;
        }
        return vds;
    }
    /**
     * return the dataset for colors, or null if one is not identified.  This defines a scheme,
     * that the last column of a bundle is the color.
     * @param ds
     * @return
     */
    private QDataSet colorByDataSet( QDataSet ds ) {
        QDataSet colorByDataSet1=null;
        if ( this.colorByDataSetId.length()>0 ) {
            if ( colorByDataSetId.equals(QDataSet.PLANE_0) ) {
                colorByDataSet1= (QDataSet) ds.property(QDataSet.PLANE_0); // maybe they really meant PLANE_0.
                if ( colorByDataSet1==null && ds.rank()==2 ) {
                    colorByDataSet1= DataSetOps.unbundleDefaultDataSet( ds );
                }
                if ( colorByDataSet1!=null && colorByDataSet1.rank()!=1 ) {
                    colorByDataSet1= null;
                }
            } else {
                if ( ds.rank()==2 ) {
                    colorByDataSet1= DataSetOps.unbundle( ds, colorByDataSetId );
                }
            }
        }
        return colorByDataSet1;
    }



    private class PsymRenderElement implements RenderElement {

        int[] colors; // store the color index  of each psym

        double[] dpsymsPathX; // store the location of the psyms here.
        double[] dpsymsPathY; // store the location of the psyms here.
        
        int count; // the number of points to plot


        /**
         * render the psyms by stamping an image at the psym location.  The intent is to
         * provide fast rendering by reducing fidelity.
         * @return the number of points drawn, though possibly off screen.
         * On 20080206, this was measured to run at 320pts/millisecond for FillStyle.FILL
         * On 20080206, this was measured to run at 300pts/millisecond in FillStyle.OUTLINE
         */
        private int renderStamp(Graphics2D g, DasAxis xAxis, DasAxis yAxis, QDataSet vds, ProgressMonitor mon) {

            DasPlot lparent= getParent();
            if ( lparent==null ) return 0;

            long t0= System.currentTimeMillis();
            logger.log( Level.FINE, "enter PsymRenderElement.renderStamp" );
            
            QDataSet colorByDataSet=null;
            if ( colorByDataSetId != null && !colorByDataSetId.equals("")) {
                colorByDataSet = colorByDataSet( ds );
            }

            if (colorByDataSet != null) {
                for (int i = 0; i < count; i++) {
                    int icolor = colors[i];
                    if ( icolor>=0 ) {
                        g.drawImage(coloredPsyms[icolor], (int)dpsymsPathX[i] - cmx, (int)dpsymsPathY[i] - cmy, lparent);
                    }
                }
            } else {
                try {
                    for (int i = 0; i < count; i++) {
                        g.drawImage(psymImage, (int)dpsymsPathX[i] - cmx, (int)dpsymsPathY[i] - cmy, lparent);
                    }
                } catch ( ArrayIndexOutOfBoundsException ex ) {
                    logger.log( Level.WARNING, ex.getMessage(), ex );
                }
            }

            logger.log(Level.FINE, "done PsymRenderElement.renderStamp ({0}ms)", ( System.currentTimeMillis()-t0  ) );
            return count;

        }

        /**
         * Render the psyms individually.  This is the highest fidelity rendering, and
         * should be used in printing.
         * @return the number of points drawn, though possibly off screen.
         * On 20080206, this was measured to run at 45pts/millisecond in FillStyle.FILL
         * On 20080206, this was measured to run at 9pts/millisecond in FillStyle.OUTLINE
         */
        private int renderDraw(Graphics2D graphics, DasAxis xAxis, DasAxis yAxis, QDataSet dataSet, ProgressMonitor mon) {

            logger.log( Level.FINE, "enter PsymRenderElement.renderDraw" );
            long t0= System.currentTimeMillis();
            
            float fsymSize = symSize;

            QDataSet colorByDataSet=null;
            if ( colorByDataSetId != null && !colorByDataSetId.equals("")) {
                colorByDataSet = colorByDataSet(ds);
                if ( colorByDataSet!=null ) {
                    if ( colorByDataSet.length()!=dataSet.length()) {
                        throw new IllegalArgumentException("colorByDataSet and dataSet do not have same length");
                    }
                } else {
                    System.err.println("why is colorByDataSetId set?");
                }
            }

            graphics.setStroke(new BasicStroke((float) lineWidth));

            boolean rgbColor= false;
            Color[] ccolors = null;
            if ( colorByDataSet != null ) {
                rgbColor= Units.rgbColor.equals( colorByDataSet.property(QDataSet.UNITS) );
                IndexColorModel icm = colorBar.getIndexColorModel();
                ccolors = new Color[icm.getMapSize()];
                for (int j = 0; j < icm.getMapSize(); j++) {               
                    ccolors[j] = new Color(icm.getRGB(j));
                }
            }

            if (colorByDataSet != null) {
                if ( rgbColor ) {
                    for (int i = 0; i < count; i++) {
                        if ( colors[i]>=0 ) {
                            graphics.setColor( new Color(colors[i]) );
                            psym.draw(graphics, dpsymsPathX[i], dpsymsPathY[i], fsymSize, fillStyle);
                        }
                    }
                    
                } else {
                    for (int i = 0; i < count; i++) {
                        if ( colors[i]>=0 ) {
                            graphics.setColor(ccolors[colors[i]]);
                            psym.draw(graphics, dpsymsPathX[i], dpsymsPathY[i], fsymSize, fillStyle);
                        }
                    }
                }

            } else {
                for (int i = 0; i < count; i++) {
//                    psym.draw(graphics, ipsymsPath[i * 2], ipsymsPath[i * 2 + 1], fsymSize, fillStyle);
                    try {
                        psym.draw(graphics, dpsymsPathX[i], dpsymsPathY[i], fsymSize, fillStyle);
                    } catch ( ArrayIndexOutOfBoundsException ex ) {
                        logger.log( Level.WARNING, ex.getMessage(), ex );
                    }
                }
            }

            logger.log(Level.FINE, "done PsymRenderElement.renderDraw ({0}ms)", ( System.currentTimeMillis()-t0  ) );
            
            return count;

        }

        @Override
        public synchronized void renderBackground( Graphics2D graphics ) {
            Color color0= graphics.getColor();
            Color backgroundColor= graphics.getBackground();
            graphics.setColor(backgroundColor);

            double backLineWidth= GraphUtil.parseLayoutLength(backgroundThick, lineWidth, symSize );
            graphics.setStroke(new BasicStroke((float) backLineWidth));
            double backWidth= GraphUtil.parseLayoutLength(backgroundThick, symSize, symSize );
            for (int i1 = 0; i1 < count; i1++) {
                psym.draw(graphics, dpsymsPathX[i1], dpsymsPathY[i1], (float)backWidth, fillStyle );
            }

            graphics.setBackground(backgroundColor);
            graphics.setColor(color0);   
        }
        
        //PsymRendererElement
        @Override
        public synchronized int render(Graphics2D graphics, DasAxis xAxis, DasAxis yAxis, QDataSet vds, ProgressMonitor mon) {
            int i;
            if ( vds.rank()>1 && !SemanticOps.isBundle(vds) && !SemanticOps.isRank2Waveform(vds) ) {
                renderException( graphics, xAxis, yAxis, new IllegalArgumentException("dataset is not rank 1"));
            }
            DasPlot lparent= getParent();
            if ( lparent==null ) return 0;
            
            QDataSet colorByDataSet = colorByDataSet(ds);
            logger.log(Level.FINER, "colorByDataSet: {0}", colorByDataSet);
            boolean rgbColor= colorByDataSet!=null && Units.rgbColor.equals( colorByDataSet.property(QDataSet.UNITS) );
        
            if ( stampPsyms && !rgbColor && !lparent.getCanvas().isPrintingThread()) {
                i = renderStamp(graphics, xAxis, yAxis, vds, mon);
            } else {
                i = renderDraw(graphics, xAxis, yAxis, vds, mon);
            }

            if ( haveValidColor==false ) {
                lparent.postMessage( SeriesRenderer.this, "no valid data to color plot symbols", Level.INFO, null, null );
            }
            
            DasColorBar lcolorBar= colorBar;
            if (colorByDataSet != null && lcolorBar!=null ) {
                Units zunits= SemanticOps.getUnits( colorByDataSet );
                if ( !zunits.isConvertibleTo( lcolorBar.getUnits() ) ) {
                    lparent.postMessage( SeriesRenderer.this, "colorbar units do not match, data units are \""+zunits+"\"", Level.INFO, null, null );
                }
            }

            return i;
        }

        //PsymRendererElement
        @Override
        public synchronized void update(DasAxis xAxis, DasAxis yAxis, QDataSet dataSet, ProgressMonitor mon) {

            QDataSet colorByDataSet1 = colorByDataSet(dataSet);
            QDataSet wdsz= null;
            if ( colorByDataSet1!=null ) {
                wdsz= SemanticOps.weightsDataSet(colorByDataSet1);
                haveValidColor= false;
            } else {
                haveValidColor= true; // just so we don't show a message.
            }

            DasColorBar fcolorBar= colorBar;
            
            Units zunits = null;
            if (colorByDataSet1 != null && fcolorBar!=null ) {
                zunits= SemanticOps.getUnits( colorByDataSet1 );
                if ( !zunits.isConvertibleTo( fcolorBar.getUnits() ) ) {
                    zunits= fcolorBar.getUnits(); // we will post warning later
                }
            }

            double x, y;            
            double dx,dy;

            count= 0; // intermediate state
            dpsymsPathX = new double[(lastIndex - firstIndex ) ];
            dpsymsPathY = new double[(lastIndex - firstIndex ) ];
            colors = new int[lastIndex - firstIndex + 2];

            int index = firstIndex;

            QDataSet vds;

            QDataSet xds = SemanticOps.xtagsDataSet(dataSet);
            vds= ytagsDataSet(dataSet);

            if ( xds.rank()==2 && xds.property( QDataSet.BINS_1 )!=null ) {
                xds= Ops.reduceMean(xds,1);
            }
            
            if ( dataSet.rank()==2 && xds.length()!=vds.length() ) {
                return;
            }
                
            Units xUnits= SemanticOps.getUnits(xds);
            Units yUnits = SemanticOps.getUnits(vds);
            if ( unitsWarning ) yUnits= yAxis.getUnits();
            if ( xunitsWarning ) xUnits= xAxis.getUnits();
            
            double dx0=-99, dy0=-99; //last point.

            QDataSet wds= SemanticOps.weightsDataSet(vds);

            int buffer= (int)Math.ceil( Math.max( 20, getSymSize() ) );
            Rectangle window= DasDevicePosition.toRectangle( yAxis.getRow(), xAxis.getColumn() );
            window= new Rectangle( window.x-buffer, window.y-buffer, window.width+2*buffer, window.height+2*buffer );
            DasPlot lparent= getParent();
            if ( lparent==null ) return;
            if ( lparent.isOverSize() ) {
                window= new Rectangle( window.x- window.width/3, window.y-buffer, 5 * window.width / 3, window.height + 2 * buffer );
                //TODO: there's a rectangle somewhere that is the preveiw.  Use this instead of assuming 1/3 on either side.
            } else {
                window= new Rectangle( window.x- buffer, window.y-buffer, window.width + 2*buffer, window.height + 2 * buffer );
            }
            
            boolean rgbColor= colorByDataSet1!=null && Units.rgbColor.equals( zunits );
            int i = 0;
            for (; index < lastIndex; index++) {
                x = xds.value(index);
                y = vds.value(index);
                
                final boolean isValid = wds.value(index)>0 && xUnits.isValid(x);

                dx = xAxis.transform(x, xUnits);
                dy = yAxis.transform(y, yUnits);

                if (isValid && window.contains(dx,dy) ) {
                    if ( simplifyPaths ) {
                        if ( dx==dx0 && dy==dy0 ) continue;
                    }
                    dpsymsPathX[i] = dx;
                    dpsymsPathY[i] = dy;
                    if ( wdsz!=null && colorByDataSet1 != null && fcolorBar!=null ) {
                        try {
                            if ( wdsz.value(index)>0 ) {
                                haveValidColor= true;
                                if ( rgbColor ) {
                                    colors[i] = (int)colorByDataSet1.value(index);
                                } else {
                                    colors[i] = fcolorBar.indexColorTransform( colorByDataSet1.value(index), zunits );
                                }
                            } else {
                                colors[i] = -1;
                            }
                        } catch ( NullPointerException ex ) {
                            //System.err.println("here391");
                            logger.log( Level.WARNING, ex.getMessage(), ex );
                        }
                    } else if ( wdsz!=null && rgbColor ) {
                        if ( wdsz.value(index)>0 ) {
                            haveValidColor= true;
                            assert colorByDataSet1!=null;
                            colors[i] = (int)colorByDataSet1.value(index);
                        } else {
                            colors[i] = -1;
                        }
                    }
                    i++;
                }
                dx0= dx;
                dy0= dy;

            }

            count = i;
            if ( count==0 ) haveValidColor= true; // don't show this warning when all the points are off the page.

        }

        @Override
        public boolean acceptContext(Point2D.Double dp) {
            
            double[] px;
            double[] py;
            
            //local copy for thread safety
            synchronized (this) {
                px= dpsymsPathX;
                py= dpsymsPathY;
            }
            
            if ( px == null ) {
                return false;
            }
            double rad = Math.max(symSize, 5);

            int np= px.length;
            for (int index = 0; index < np; index++) {
                int i = index;
                if (dp.distance(px[i], py[i]) < rad) {
                    return true;
                }
            }
            return false;
        }
    }

    private class ErrorBarRenderElement implements RenderElement {

        GeneralPath p;

        @Override
        public void renderBackground(Graphics2D g) {
            // do nothing for now.
        }
        
        @Override
        public int render(Graphics2D g, DasAxis xAxis, DasAxis yAxis, QDataSet vds, ProgressMonitor mon) {
            GeneralPath lp= getPath();
            if (lp == null) {
                return 0;
            }
            Rectangle b= lp.getBounds();
            if ( b.height==0 ) b.height=1; // horizontal points
            if ( b.width==0 ) b.width=1; //vertical points            
            Rectangle canvasRect= DasDevicePosition.toRectangle( yAxis.getRow(), xAxis.getColumn() );
            if ( !b.intersects(canvasRect) ) {
                logger.log(Level.FINE, "all data is off-page" );
                return 0;
            }
            g.setStroke(new BasicStroke((float) lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND ));
            if ( errorBarType==ErrorBarType.SHADE ) {
                g.setColor( fillColor );
                g.fill(lp);
            } else {
                g.draw(lp);
            }
            return lastIndex - firstIndex;
        }

        private synchronized GeneralPath getPath() {
            return p;
        }
        
        @Override
        public synchronized void update(DasAxis xAxis, DasAxis yAxis, QDataSet dataSet, ProgressMonitor mon) {

            QDataSet vds= ytagsDataSet(dataSet);
            if ( vds==null ) {
                p= null;
                return;
            }
            
            QDataSet xds= SemanticOps.xtagsDataSet(dataSet);

            QDataSet[] binMinMax= getMinMax( vds );
            QDataSet binMax=binMinMax[1];
            QDataSet binMin=binMinMax[0];
            
            GeneralPath lp;

            Units xunits = SemanticOps.getUnits(xds);
            Units yunits = SemanticOps.getUnits(vds);
            
            if ( unitsWarning ) yunits= yAxis.getUnits();
            if ( xunitsWarning ) xunits= xAxis.getUnits();

            lp = null;
            
            Point2D.Double returnTo= null;
            
            if ( binMax!=null && binMin!=null ) {
                try {
                    lp = new GeneralPath();
                    QDataSet p1= binMax;
                    QDataSet p2= binMin;
                    QDataSet w1= Ops.valid(p2);
                    QDataSet w2= Ops.valid(p1);
                    boolean penUp= true;
                    if ( firstIndex==-1 ) return; // test140/7822
                    for (int i = firstIndex; i < lastIndex; i++) {
                        double ix = xAxis.transform( xds.value(i), xunits );
                        if ( w1.value(i)>0 && w2.value(i)>0 ) {
                            double iym = yAxis.transform( p1.value(i), yunits );
                            double iyp = yAxis.transform( p2.value(i), yunits );
                            switch (errorBarType) {
                                case BAR:
                                    lp.moveTo(ix, iym);
                                    lp.lineTo(ix, iyp);
                                    break;
                                case SERIF_BAR:
                                    lp.moveTo(ix-2, iym);
                                    lp.lineTo(ix+2, iym);
                                    lp.moveTo(ix, iym);
                                    lp.lineTo(ix, iyp);
                                    lp.moveTo(ix-2, iyp);
                                    lp.lineTo(ix+2, iyp);
                                    break;
                                case SHADE:
                                    if ( penUp ) {
                                        lp.moveTo( ix,iym );
                                        returnTo= new Point2D.Double(ix,iym);
                                        penUp= false;
                                    } else {
                                        lp.lineTo(ix, iym);
                                    }
                                default:
                                    logger.log(Level.INFO, "unsupported ErrorBarType: {0}", errorBarType);
                                    break;
                            }
                        }
                    }
                    if ( errorBarType==ErrorBarType.SHADE ) {
                        for (int i = lastIndex-1; i >= firstIndex; i--) {
                            double ix = xAxis.transform( xds.value(i), xunits );
                            double iyp = yAxis.transform( p2.value(i), yunits );
                            if ( penUp ) {
                                lp.moveTo( ix,iyp );
                                penUp= false;
                            } else {
                                lp.lineTo(ix, iyp);
                            }
                        }
                        if ( returnTo!=null ) {
                            lp.lineTo( returnTo.x, returnTo.y );
                        }
                    }
                } catch ( IllegalArgumentException ex ) {
                    if ( getParent()!=null ) getParent().postException(SeriesRenderer.this,ex);
                }
            }
            
            binMinMax= getMinMax( xds );
            QDataSet xbinMax=binMinMax[1];
            QDataSet xbinMin=binMinMax[0];
            
            if ( xbinMax!=null && xbinMin!=null ) {
                try {
                    if ( lp==null ) lp= new GeneralPath();
                    QDataSet p1= xbinMin;
                    QDataSet p2= xbinMax;
                    QDataSet w1= Ops.valid(p1);
                    QDataSet w2= Ops.valid(p2);
                    for (int i = firstIndex; i < lastIndex; i++) {
                        double iy = yAxis.transform( vds.value(i), yunits );
                        if ( w1.value(i)>0 && w2.value(i)>0 ) {
                            double ixm = xAxis.transform( p1.value(i), xunits );
                            double ixp = xAxis.transform( p2.value(i), xunits );
                            switch (errorBarType) {
                                case BAR:
                                    lp.moveTo(ixm, iy);
                                    lp.lineTo(ixp, iy);
                                    break;
                                case SERIF_BAR:
                                    lp.moveTo(ixm, iy-2);
                                    lp.lineTo(ixm, iy+2);
                                    lp.moveTo(ixm, iy);
                                    lp.lineTo(ixp, iy);
                                    lp.moveTo(ixp, iy-2);
                                    lp.lineTo(ixp, iy+2);
                                    break;
                                case SHADE:
                                    logger.log(Level.INFO, "SHADE does not support x error bars" );
                                    break;
                                default:
                                    logger.log(Level.INFO, "unsupported ErrorBarType: {0}", errorBarType);
                                    break;
                            }

                        }
                    }
                } catch ( IllegalArgumentException ex ) {
                    if ( getParent()!=null ) getParent().postException(SeriesRenderer.this,ex);
                }
            }
            
            p= lp;

        }

        @Override
        public boolean acceptContext(Point2D.Double dp) {
            GeneralPath gp= getPath();
            return gp != null && gp.contains(dp.x - 2, dp.y - 2, 5, 5);

        }

        /**
         * return the min and the max extent of each data point, or null.
         * @param vds
         * @return the extent of each data point in a two-element QDataSet array, or [ null,null ].
         */
        private QDataSet[] getMinMax(QDataSet vds) {
            QDataSet binMin=null;
            QDataSet binMax=null;
            
            QDataSet deltaPlusY = (QDataSet) vds.property( QDataSet.DELTA_PLUS );
            QDataSet deltaMinusY = (QDataSet) vds.property( QDataSet.DELTA_MINUS );
            
            if ( deltaPlusY==null ) {
                QDataSet m= (QDataSet) vds.property(QDataSet.BIN_PLUS);
                if ( m!=null ) deltaPlusY= m;
                m= (QDataSet) vds.property(QDataSet.BIN_MINUS);
                if ( m!=null ) deltaMinusY= m;
            }
            if ( deltaPlusY==null ) {
                QDataSet m= (QDataSet) vds.property(QDataSet.BIN_MAX);
                if ( m!=null ) binMax= m;
                m= (QDataSet) vds.property(QDataSet.BIN_MIN);
                if ( m!=null ) binMin= m;
            } else {
                binMax= Ops.add( vds, deltaPlusY );
                binMin= Ops.subtract( vds, deltaMinusY );
            } 
            
            return new QDataSet[] { binMin, binMax } ;
        }
    }

    /**
     * isolate allow bad units logic.
     * @param d the datum.
     * @param u target units.
     * @return the double value.
     */
    private double doubleValue( Datum d, Units u ) {
        if ( d.getUnits().isConvertibleTo(u) ) {
            return d.doubleValue(u);
        } else {
            try {
                return d.value();
            } catch ( IllegalArgumentException ex ) {
                throw new InconvertibleUnitsException(d.getUnits(),u);
            }
        }
    } 
    
    private DataGeneralPathBuilder getPathBuilderForData( DasAxis xAxis, DasAxis yAxis, QDataSet xds, QDataSet vds ) {
        DataGeneralPathBuilder pathBuilder= new DataGeneralPathBuilder(xAxis,yAxis);

        Datum sw = null;
        try {// TODO: this really shouldn't be here, since we calculate it once.
            sw= getCadence( xds, vds, firstIndex, lastIndex );
            // Note it uses a cached value that runs along with the data.
        } catch ( IllegalArgumentException ex ) {
            logger.log( Level.WARNING, null, ex );
        }
        double xSampleWidth;
        boolean logStep;
        if ( sw!=null) {
            if ( UnitsUtil.isRatiometric(sw.getUnits()) ) {
                xSampleWidth = sw.doubleValue(Units.logERatio);
                logStep= true;
            } else {
                Units xUnits = SemanticOps.getUnits(xds);
                xSampleWidth = doubleValue( sw, xUnits.getOffsetUnits() );
                logStep= false;
            }
            //double-check cadence
            int cadenceGapCount= 0;
            double xSampleWidthFudge= xSampleWidth*1.20;
            if ( logStep ) {
                for ( int i=1; i<xds.length(); i++ ) {
                    if ( Math.log( xds.value(i) / xds.value(i-1) ) > xSampleWidthFudge ) cadenceGapCount++;
                }
            } else {
                for ( int i=1; i<xds.length(); i++ ) {
                    if ( xds.value(i)-xds.value(i-1) > xSampleWidthFudge ) cadenceGapCount++;
                }
            }

            if ( cadenceGapCount>(vds.length()/2) || !cadenceCheck ) {
                pathBuilder.setCadence( null );
            } else {
                pathBuilder.setCadence( sw );
            }
        } 
        return pathBuilder;
    }
    
    
    private class PsymConnectorRenderElement implements RenderElement {

        private GeneralPath path1;
        private boolean pathWasReduced= true; // true if the path was reduced
   
        @Override
        public void renderBackground( Graphics2D g ) {
            GeneralPath lpath1= getPath();
            if (lpath1 == null) {
                return;
            }
            Color color0= g.getColor();
            Color backgroundColor= g.getBackground();
            g.setColor(backgroundColor);
            double backWidth= GraphUtil.parseLayoutLength(backgroundThick, lineWidth, symSize );
            PsymConnector.SOLID.draw(g, lpath1, (float)backWidth);
            g.setBackground(backgroundColor);
            g.setColor(color0);
        }
        
        @Override
        public int render(Graphics2D g, DasAxis xAxis, DasAxis yAxis, QDataSet vds, ProgressMonitor mon) {
            //if ( pathWasReduced && !dataIsMonotonic ) return 0;
            long t0= System.currentTimeMillis();
            final boolean debug= false;
            if ( debug ) {
                Color color0= g.getColor();
                g.setColor( Color.LIGHT_GRAY );
                for ( int i=0; i<1000; i+=100 ) { // draw grid
                    g.drawLine(i, 0, i, 1000);
                    g.drawLine(0, i, 1000, i );
                }
                g.setColor(color0);
            }
            logger.log(Level.FINE, "enter connector render" );
            logger.log(Level.FINER, "path was reduced: {0}", pathWasReduced);

            GeneralPath lpath1= getPath();
            if (lpath1 == null) {
                return 0;
            }
            Rectangle b= lpath1.getBounds();
            if ( b.height==0 ) b.height=1; // horizontal points
            if ( b.width==0 ) b.width=1; //vertical points

            Rectangle canvasRect= DasDevicePosition.toRectangle( yAxis.getRow(), xAxis.getColumn() );
            long t= ( System.currentTimeMillis()-t0  );
            if ( !b.intersects(canvasRect) ) {
                logger.log(Level.FINE, "all data is off-page ({0}ms)", ( t-t0  ) );
                return 0;
            }
                    
            //dumpPath();
            psymConnector.draw(g, lpath1, (float)lineWidth);
            logger.log(Level.FINE, "done connector render ({0}ms)", ( System.currentTimeMillis()-t0  ) );
            return 0;
        }

        private synchronized GeneralPath getPath() {
            return path1;
        }
        
        //PSymConnectorRendererElement
        @Override
        public synchronized void update(DasAxis xAxis, DasAxis yAxis, QDataSet dataSet, ProgressMonitor mon) {
            logger.log(Level.FINE, "enter connector update" );
            
            if ( dataSet.rank()==0 ) return;
            QDataSet xds= SemanticOps.xtagsDataSet( dataSet );
            if ( xds.rank()==2 && xds.property( QDataSet.BINS_1 )!=null ) {
                xds= Ops.reduceMean(xds,1);
            }

            QDataSet vds= ytagsDataSet( dataSet );
            
            QDataSet wds= SemanticOps.weightsDataSet( vds );

            Units xUnits = SemanticOps.getUnits(xds);
            Units yUnits = SemanticOps.getUnits(vds);
            Units xaxisUnits= xAxis.getUnits();
            Units yaxisUnits= yAxis.getUnits();
            if ( !yUnits.isConvertibleTo(yaxisUnits) ) {
                yUnits= yAxis.getUnits();
                unitsWarning= true;
            }
            if ( !xUnits.isConvertibleTo(xaxisUnits) ) {
                xUnits= xAxis.getUnits();
                unitsWarning= true;
            }

            if ( vds.rank()==0 ) {
                GeneralPath gp= new GeneralPath();
                gp.moveTo( xAxis.transform( xAxis.getDataMinimum() ), yAxis.transform( DataSetUtil.asDatum(vds) ) );
                gp.lineTo( xAxis.transform( xAxis.getDataMaximum() ), yAxis.transform( DataSetUtil.asDatum(vds) ) );
                this.path1 = gp;
                return;
            }
            
            if ( lastIndex-firstIndex==0 ) {
                this.path1= null;
                return;
            }

            long t0= System.currentTimeMillis();
            
            DataGeneralPathBuilder pathBuilder= getPathBuilderForData( xAxis, yAxis, xds, vds );
            pathBuilder.setHistogramMode(histogram);
                
            /* fuzz the xSampleWidth */
            double xSampleWidthExact= pathBuilder.getCadenceDouble();
            
            double x;
            double y;

            //double y0; /* the last plottable point */

            UnitsConverter xuc= xUnits.getConverter(xaxisUnits);
            UnitsConverter yuc= yUnits.getConverter(yaxisUnits);
            
            int index;

            index = firstIndex;
            x = xuc.convert( (double) xds.value(index) );
            y = yuc.convert( (double) vds.value(index) );

            pathBuilder.addDataPoint( true, x, y );

            index++;

            // now loop through all of them. //
            //boolean ignoreCadence= ! cadenceCheck;
            boolean isValid;
            
            //poes_n17_20041228.cdf?P1_90[0:300] contains fill records between 
            //each measurement. Test for this.
            int invalidInterleaveCount= 0;
            for ( int i=1; i<xds.length(); i++ ) {
                if ( wds.value(i-1)>0 && wds.value(i)==0 ) invalidInterleaveCount++;
            }
            boolean notInvalidInterleave= invalidInterleaveCount<(wds.length()/3);
                                   
            for (; index < lastIndex; index++) {

                x = xuc.convert( xds.value(index) );
                y = yuc.convert( vds.value(index) );

                isValid = wds.value( index )>0;

                if ( isValid || notInvalidInterleave ) {
                    pathBuilder.addDataPoint( isValid, x, y );
                }
                                
            } // for ( ; index < ixmax && lastIndex; index++ )
            
            logger.log(Level.FINE, "done create general path ({0}ms)", ( System.currentTimeMillis()-t0  ));
            
            pathBuilder.finishThought();
            boolean allowSimplify= xSampleWidthExact<1e37;
            if (!histogram && simplifyPaths && allowSimplify && colorByDataSetId.length()==0 ) {
                int pathLengthApprox= Math.max( 5, 110 * (lastIndex - firstIndex) / 100 );
                this.path1= new GeneralPath(GeneralPath.WIND_NON_ZERO, pathLengthApprox );
                int count = GraphUtil.reducePath20140622(pathBuilder.getPathIterator(), path1, 1, 5 );
                if ( additionalClip ) {
                    GeneralPath path2= new GeneralPath(GeneralPath.WIND_NON_ZERO, pathLengthApprox );
                    int x2= GraphUtil.clipPath( path1.getPathIterator(null), path2, GraphUtil.shrinkRectangle( getParent().getAxisClip(), 90 ) );
                    logger.log(Level.FINE, "additionalClip: {0}", x2);
                    path1= path2;
                }
                pathWasReduced= true;
                logger.fine( String.format("reduce path in=%d  out=%d\n", lastIndex-firstIndex, count) );
            } else {
                //this.path1 = newPath;
                this.path1 = pathBuilder.getGeneralPath();
                pathWasReduced= false;
            }

            //dumpPath( getParent().getCanvas().getWidth(), getParent().getCanvas().getHeight(), path1 );  // dumps jython script showing problem.
            //GraphUtil.describe( path1, true );
            logger.log(Level.FINE, "done connector update ({0}ms)", ( System.currentTimeMillis()-t0  ));
        }

        @Override
        public boolean acceptContext(Point2D.Double dp) {
            GeneralPath gp= getPath();
            if ( gp==null ) return false;
            Rectangle2D hitbox = new Rectangle2D.Double(dp.x-5, dp.y-5, 10f, 10f);
            double[] coords = new double[6];
            PathIterator it = gp.getPathIterator(null);
            it.currentSegment(coords);

            double x1 = coords[0];
            double y1 = coords[1];
            it.next();

            while (!it.isDone()) {
                int segType = it.currentSegment(coords);
                // don't test against SEG_MOVETO; we shouldn't have any others
                if (segType == PathIterator.SEG_LINETO) {
                    if(hitbox.intersectsLine(x1, y1, coords[0], coords[1])) return true;
                }
                x1 = coords[0];
                y1 = coords[1];
                it.next();
            }
            return false;
        }
    }

    private double midPointData(DasAxis axis, double d1, Units units, double delta, boolean ratiometric, double alpha ) {
        double fx1;
        if (axis.isLog() && ratiometric ) {
            fx1 = Math.exp( Math.log(d1) + delta * alpha );
        } else {
            fx1 = d1 + delta * alpha;
        }
        return fx1;
    }
    
    private double midPointData( DasAxis axis, double d1, double d2, boolean ratiometric ) {
        double fx1;
        if (axis.isLog() && ratiometric ) {
            fx1 = Math.exp( ( Math.log(d1) + Math.log(d2) ) / 2 );
        } else {
            fx1 = ( d1 + d2 ) / 2;
        }
        return fx1;
    }
    

//        /* dumps a jython script which draws the path See https://sourceforge.net/p/autoplot/bugs/1215/ */
//        private static void dumpPath( int width, int height, GeneralPath path1 ) {
//            if ( path1!=null ) {
//                try {
//                    java.io.BufferedWriter w= new java.io.BufferedWriter( new java.io.FileWriter("/tmp/foo.jy") );
//                    w.write( "from java.awt.geom import GeneralPath\nlp= GeneralPath()\n");
//                    w.write( "h= " + height + "\n" );
//                    w.write( "w= " + width + "\n" );
//                    PathIterator it= path1.getPathIterator(null);
//                    float [] seg= new float[6];
//                    while ( !it.isDone() ) {
//                        int r= it.currentSegment(seg);
//                        if ( r==PathIterator.SEG_MOVETO ) {
//                            w.write( String.format( "lp.moveTo( %9.2f, %9.4f )\n", seg[0], seg[1] ) );
//                        } else {
//                            w.write( String.format( "lp.lineTo( %9.2f, %9.4f )\n", seg[0], seg[1] ) );
//                        }
//                        it.next();
//                    }
//                    w.write( "from javax.swing import JPanel, JOptionPane\n" +
//                            "from java.awt import Dimension, RenderingHints, Color\n" +
//                            "\n" +
//                            "class MyPanel( JPanel ):\n" +
//                            "   def paintComponent( self, g ):\n" +
//                            "       g.setColor(Color.WHITE)\n" +
//                            "       g.fillRect(0,0,w,h)\n" + 
//                            "       g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON )\n" +
//                            "       g.setColor(Color.BLACK)\n" +
//                            "       g.draw( lp )\n" +
//                            "\n" +
//                            "p= MyPanel()\n" +
//                            "p.setMinimumSize( Dimension( w,h ) )\n" +
//                            "p.setPreferredSize( Dimension( w,h ) )\n" +
//                            "JOptionPane.showMessageDialog( None, p )" );
//                    w.close();
//                } catch ( java.io.IOException ex ) {
//                    ex.printStackTrace();
//                }
//            }
//        }    
    
//    /**
//     * for debugging, dumps the path iterator out to a file.
//     * @param it 
//     */
//    private void dumpPath(PathIterator it) {
//        PrintWriter write = null;
//        try {
//            write = new PrintWriter( new FileWriter("/tmp/foo."+id+".txt") );
//            
//            while ( !it.isDone() ) {
//                float[] coords= new float[6];
//                
//                int type= it.currentSegment(coords);
//                write.printf( "%10d ", type );
//                if ( type==PathIterator.SEG_MOVETO) {
//                    for ( int i=0; i<6; i++ ) {
//                        write.printf( "%10.2f ", -99999.  );
//                    }
//                    write.println();
//                    write.printf( "%10d ", type );
//                }
//                for ( int i=0; i<6; i++ ) {
//                    write.printf( "%10.2f ", coords[i] );
//                }
//                write.println();
//                it.next();
//            }
//        } catch (IOException ex) {
//            Logger.getLogger(SeriesRenderer.class.getName()).log(Level.SEVERE, null, ex);
//        } finally {
//            write.close();
//        }
//    }
    
    class FillRenderElement implements RenderElement {

        private GeneralPath fillToRefPath1;

        @Override
        public void renderBackground(Graphics2D g) {
            // do nothing for now.
        }
        
        @Override
        public int render(Graphics2D g, DasAxis xAxis, DasAxis yAxis, QDataSet vds, ProgressMonitor mon) {
            if ( fillToRefPath1==null ) {
                return 0;
            }
            
            Rectangle b= fillToRefPath1.getBounds();
            if ( b.height==0 ) b.height=1; // horizontal points
            if ( b.width==0 ) b.width=1; //vertical points
            Rectangle canvasRect= DasDevicePosition.toRectangle( yAxis.getRow(), xAxis.getColumn() );
            if ( !b.intersects(canvasRect) ) {
                logger.log(Level.FINE, "all data is off-page" );
                return 0;
            }
            //g.setRenderingHint( RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE );
            //PathIterator it= fillToRefPath1.getPathIterator(null);
            //dumpPath(it);

            if ( fillColor.getAlpha()==0 ) {
                double y;
                try {
                    y= yAxis.transform( reference );
                } catch ( InconvertibleUnitsException ex ) {
                    y= yAxis.transform( reference.value(), yAxis.getUnits() );
                }
                DasColumn column= xAxis.getColumn();
                g.draw( new java.awt.geom.Line2D.Double( (double)column.getDMinimum(), y, (double)column.getDMaximum(), y ) );
            } else {
                g.setColor(fillColor);
                g.fill(fillToRefPath1);
                g.draw(fillToRefPath1);
            }
            return 0;
        }

        @Override
        public void update(DasAxis xAxis, DasAxis yAxis, QDataSet dataSet, ProgressMonitor mon) {

            if ( lastIndex-firstIndex==0 ) {
                this.fillToRefPath1= null;
                return;
            }

            QDataSet xds = SemanticOps.xtagsDataSet(dataSet);
            QDataSet vds = ytagsDataSet( dataSet );
            QDataSet wds = SemanticOps.weightsDataSet(vds);
            
            Units xUnits = SemanticOps.getUnits(xds);
            Units yUnits = SemanticOps.getUnits(vds);
            
            synchronized(SeriesRenderer.this) {
                if ( unitsWarning ) yUnits= yAxis.getUnits();
                if ( xunitsWarning ) xUnits= xAxis.getUnits();
            }
            
            
            boolean above= fillDirection.equals("above") || fillDirection.equals("both");
            boolean below= fillDirection.equals("below") || fillDirection.equals("both");
            
            double x, y;
            
            //  NEW CODE TO MATCH CONNECTOR CODE
            Units xaxisUnits= xAxis.getUnits();
            Units yaxisUnits= yAxis.getUnits();
            if ( !yUnits.isConvertibleTo(yaxisUnits) ) {
                yUnits= yAxis.getUnits();
                synchronized(SeriesRenderer.this){
                    unitsWarning= true;
                }
            }
            if ( !xUnits.isConvertibleTo(xaxisUnits) ) {
                xUnits= xAxis.getUnits();
                synchronized(SeriesRenderer.this){
                    xunitsWarning= true;
                }
            }
            
            DataGeneralPathBuilder pathBuilder= getPathBuilderForData( xAxis, yAxis, xds, vds );
            String pathBuilderName= ""; // "fillgp" for debugging
            pathBuilder.setName(pathBuilderName); // for debugging.
            
            double xSampleWidthExact= pathBuilder.getCadenceDouble();
            //boolean logStep= pathBuilder.isCadenceRatiometric();
            
            pathBuilder.setHistogramMode(histogram);
            
            //double y0; /* the last plottable point */

            UnitsConverter xuc= xUnits.getConverter(xAxis.getUnits());
            UnitsConverter yuc= yUnits.getConverter(yAxis.getUnits());
            
            if (reference == null) {
                reference = yUnits.createDatum(yAxis.isLog() ? 1.0 : 0.0);
            }

            double yref = doubleValue( reference, yUnits );
            
            int index;

            index = firstIndex;
            x = xuc.convert( (double) xds.value(index) );
            y = yuc.convert( (double) vds.value(index) );

            pathBuilder.addDataPoint( true, x, yref );

            double ireferenceY= yAxis.transform( yref, yUnits );
                
            if ( pathBuilderName.length()>0 ) System.err.println( String.format( "ireferenceY=%.2f", ireferenceY) );

            pathBuilder.setHistogramFillFlag();
            pathBuilder.addDataPoint( true, x, y );

            index++;
            
            // now loop through all of them. //
            //boolean ignoreCadence= ! cadenceCheck;
            boolean isValid;
            
            //poes_n17_20041228.cdf?P1_90[0:300] contains fill records between 
            //each measurement. Test for this.
            int invalidInterleaveCount= 0;
            for ( int i=1; i<xds.length(); i++ ) {
                if ( wds.value(i-1)>0 && wds.value(i)==0 ) invalidInterleaveCount++;
            }
            boolean notInvalidInterleave= invalidInterleaveCount<(wds.length()/3);
            
            boolean lastIsValid= true; // last state of the valid
            double lastX=x;
            
            if ( pathBuilderName.length()>0 ) System.err.println("# here invalid");
            
            for (; index < lastIndex; index++) {

                if ( pathBuilderName.length()>0 ) System.err.println( String.format( "# x=%.2f, y=%.2f",xds.value(index), vds.value(index) ) );
                x = xuc.convert( xds.value(index) );
                y = yuc.convert( vds.value(index) );

                isValid = wds.value( index )>0;

                if ( Math.abs( x - lastX ) > (xSampleWidthExact*1.2) ) {
                    pathBuilder.finishThought();
                    Point2D last= pathBuilder.getPenPosition();
                    pathBuilder.insertLineTo( last.getX(), ireferenceY );
                    lastIsValid= false;
                }
                
                if ( isValid || notInvalidInterleave ) {
                    if ( !lastIsValid ) {
                        pathBuilder.addDataPoint( isValid, x, yref );
                        Point2D last= pathBuilder.getPenPosition();
                        if ( isValid ) {
                            pathBuilder.insertLineTo( last.getX(), yAxis.transform( pathBuilder.getYUnits().createDatum(y) ) );
                            pathBuilder.setHistogramFillFlag();
                        }
                        pathBuilder.addDataPoint( isValid, x, y );
                    } else {
                        pathBuilder.addDataPoint( isValid, x, y );
                    }
                }
                
                if ( !isValid && lastIsValid ) {
                    Point2D last= pathBuilder.getPenPosition();
                    pathBuilder.insertLineTo( last.getX(), ireferenceY );
                    lastIsValid= false;
                } else {
                    if ( isValid ) {
                        lastIsValid= true;
                    }
                }
                
                lastX= x;
                
            } // for ( ; index < ixmax && lastIndex; index++ )
            
//            for (; index < lastIndex; index++) {
//
//                x = xuc.convert( xds.value(index) );
//                y = yuc.convert( vds.value(index) );
//
//                isValid = wds.value( index )>0;
//
//                if ( isValid || notInvalidInterleave ) {
//
//                    if ( isValid ) {
//                        if ( !lastIsValid ) {
//                            pathBuilder.addDataPoint( isValid, x, yref );
//                        }
//                        pathBuilder.addDataPoint( isValid, x, y );
//                    } else {
//                        if ( lastIsValid ) {
//                            Point2D p= pathBuilder.getLastDrawnPoint();
//                            pathBuilder.addDataPoint( true, lastX, yref );
//                            
//                        }
//                        pathBuilder.addDataPoint( isValid, x, y );
//                    }
//                        
//                }
//                lastIsValid= isValid;
//                lastX= x;
//                
//                                
//            } // for ( ; index < ixmax && lastIndex; index++ )
            
            // return to the reference line
            pathBuilder.finishThought();
            pathBuilder.insertLineTo( pathBuilder.getPenPosition().getX(), ireferenceY );
            GeneralPath fillPath= pathBuilder.getGeneralPath();
            
            //  END, NEW CODE TO MATCH CONNECTOR CODE
            
            double fyref= yAxis.transform(yref, yUnits);
            if ( !above ) {
                Area a= new Area(fillPath);
                Rectangle2D.Double rect= new Rectangle2D.Double( 0, fyref, getParent().getColumn().getDMaximum(), getParent().getRow().getHeight() ); 
                a.intersect( new Area( rect ) );
                GeneralPath p= new GeneralPath(a);
                fillPath= p;
            } 
            
            if ( !below ) {
                Area a= new Area(fillPath);
                Rectangle2D.Double rect= new Rectangle2D.Double( 0, 0, getParent().getColumn().getDMaximum(), fyref ); 
                a.intersect( new Area( rect ) );
                GeneralPath p= new GeneralPath(a);
                fillPath= p;
            }

            this.fillToRefPath1 = fillPath;
            
            boolean allowSimplify= (lastIndex-firstIndex)>SIMPLIFY_PATHS_MIN_LIMIT && xSampleWidthExact<1e37;
            if ( !histogram && simplifyPaths && allowSimplify && colorByDataSetId.length()==0 ) {
                int pathLengthApprox= Math.max( 5, 110 * (lastIndex - firstIndex) / 100 );
                GeneralPath newPath= new GeneralPath(GeneralPath.WIND_NON_ZERO, pathLengthApprox );
                int count= GraphUtil.reducePath(fillToRefPath1.getPathIterator(null), newPath );
                fillToRefPath1= newPath;
                logger.fine( String.format("reduce path(fill) in=%d  out=%d\n", lastIndex-firstIndex, count ) );
            }

        }

        @Override
        public boolean acceptContext(Point2D.Double dp) {
            return fillToRefPath1 != null && fillToRefPath1.contains(dp);
        }
    }
    
    private static final int SIMPLIFY_PATHS_MIN_LIMIT = 1000;

    QDataSet xdsc, ydsc;
    int firstIndexc, lastIndexc;
    Datum cadencec;
    
    /**
     * get the cadence for the data, caching it for a particular input.
     */
    private Datum getCadence(QDataSet xds, QDataSet yds, int firstIndex, int lastIndex) {
        if ( xds==xdsc && yds==ydsc && firstIndex==firstIndexc && lastIndex==lastIndexc ) {
            if ( cadencec!=null ) {
                logger.finer("cache hit avoids recalculating cadence");
                return cadencec;
            }
        }
        logger.finer("cache miss means we must recalculate cadence");
        MutablePropertyDataSet xds1= Ops.copy(xds.trim(firstIndex,lastIndex));
        xds1.putProperty(QDataSet.CADENCE,null);
        cadencec= SemanticOps.guessXTagWidth( xds1, yds.trim(firstIndex,lastIndex) );
        xdsc= xds;
        ydsc= yds;
        firstIndexc= firstIndex;
        lastIndexc= lastIndex;
        return cadencec;
    }

    /**
     * updates the image of a psym that is stamped
     */
    private void updatePsym() {
        if ( !isActive() ) return;
        
        int sx = 6+(int) Math.ceil(symSize + 2 * lineWidth);
        int sy = 6+(int) Math.ceil(symSize + 2 * lineWidth);
        double dcmx, dcmy;
        if ( fillStyle==FillStyle.STYLE_OUTLINE ) {
            dcmx = (lineWidth + (int) Math.ceil( ( symSize ) / 2)) +2 ;
            dcmy = (lineWidth + (int) Math.ceil( ( symSize ) / 2)) +2 ;
        } else {
            dcmx = ( (int) Math.ceil( ( symSize ) / 2)) +2.5 ;
            dcmy = ( (int) Math.ceil( ( symSize ) / 2)) +2.5 ;
        }
        BufferedImage image = new BufferedImage(sx, sy, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) image.getGraphics();

        Object rendering = antiAliased ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, rendering);
        g.setColor(color);

        DasPlot lparent= getParent();
        if ( lparent==null ) return;

        g.setBackground(lparent.getBackground());
        
        g.setStroke(new BasicStroke((float) lineWidth));

        psym.draw(g, dcmx, dcmy, symSize, fillStyle);
        psymImage = image;

        DasColorBar lcolorBar=  this.colorBar;
        if (  colorByDataSetId != null && !colorByDataSetId.equals("") && lcolorBar!=null ) {
            initColoredPsyms(sx, sy, image, g, lparent, lcolorBar, rendering, dcmx, dcmy);
        }
        
        cmx = (int) dcmx;
        cmy = (int) dcmy;

        update();
    }

    private void initColoredPsyms(int sx, int sy, BufferedImage image, Graphics2D g, DasPlot lparent, DasColorBar lcolorBar, Object rendering, double dcmx, double dcmy) {
        IndexColorModel model = lcolorBar.getIndexColorModel();
        coloredPsyms = new Image[model.getMapSize()];
        for (int i = 0; i < model.getMapSize(); i++) {
            Color c = new Color(model.getRGB(i));
            image = new BufferedImage(sx, sy, BufferedImage.TYPE_INT_ARGB);
            g = (Graphics2D) image.getGraphics();
            g.setBackground(lparent.getBackground());
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, rendering);
            g.setColor(c);
            g.setStroke(new BasicStroke((float) lineWidth));
            psym.draw(g, dcmx, dcmy, symSize, this.fillStyle);
            coloredPsyms[i] = image;
        }
    }

   // private void reportCount() {
        //if ( renderCount % 100 ==0 ) {
        //System.err.println("  updates: "+updateImageCount+"   renders: "+renderCount );
        //new Throwable("").printStackTrace();
        //}
  //  }

    /**
     * updates firstIndex and lastIndex that point to the part of
     * the data that is plottable.  The plottable part is the part that
     * might be visible while limiting the number of plotted points.
     */
    private synchronized void updateFirstLast(DasAxis xAxis, DasAxis yAxis, QDataSet xds, QDataSet dataSet ) {
        
        long t0= System.currentTimeMillis();
        
        int ixmax;
        int ixmin;
                
        firstIndex= -1;
        
        QDataSet yds= dataSet;
        
        if ( xds.length()!=yds.length() ) {
            logger.fine("xds and yds have different lengths.  Assuming transitional case.");
            firstIndex_v= 0;
            lastIndex_v= xds.length();
            firstIndex= 0;
            lastIndex= xds.length();
            return;
            //throw new IllegalArgumentException("xds and yds are different lengths.");
        }
        
        if ( yds.rank()==2 ) {
            yds= DataSetOps.slice1( yds, 0 );
        }

        if ( yds.rank()==3 ) {
           firstIndex= 0;
           lastIndex= xds.length();
           dataIsMonotonic= false;
           return;
        }
        
        if ( xds.rank()==2 && SemanticOps.isRank3JoinOfRank2Waveform(dataSet) ) {
            xds= DataSetOps.slice1(xds,0); //BINS dataset
        }

        QDataSet wxds= SemanticOps.weightsDataSet( xds );

        QDataSet wds;
        wds= SemanticOps.weightsDataSet( yds );

        DasPlot lparent= getParent();

        dslen= xds.length();
        dataIsMonotonic= SemanticOps.isMonotonic( xds );
        if ( dataIsMonotonic ) {
            
            DatumRange visibleRange = xAxis.getDatumRange();
            Units xdsu= SemanticOps.getUnits(xds);
            if ( !visibleRange.getUnits().isConvertibleTo( xdsu ) ) {
                visibleRange= new DatumRange( visibleRange.min().doubleValue(visibleRange.getUnits()), visibleRange.max().doubleValue(visibleRange.getUnits()), xdsu );
            }
            firstIndex_v = DataSetUtil.getPreviousIndex( xds, visibleRange.min());
            lastIndex_v = DataSetUtil.getNextIndex( xds, visibleRange.max()) + 1; // +1 is for exclusive.
            if ( lparent!=null && lparent.isOverSize()) {
                Rectangle plotBounds = lparent.getUpdateImageBounds();
                if ( plotBounds!=null ) {
                    visibleRange = xAxis.invTransform( plotBounds.x, plotBounds.x + plotBounds.width );
                }
                try {
                    ixmin = DataSetUtil.getPreviousIndex( xds, visibleRange.min());
                    ixmax = DataSetUtil.getNextIndex( xds, visibleRange.max()) + 1; // +1 is for exclusive.
                } catch ( IllegalArgumentException ex ) {
                    ixmin = firstIndex_v;
                    ixmax = lastIndex_v;
                }

            } else {
                ixmin = firstIndex_v;
                ixmax = lastIndex_v;
            }

        } else {

            ixmin = 0;
            ixmax = xds.length();
            firstIndex_v= ixmin;
            lastIndex_v= ixmax;
        }

        int index;

        // find the first valid point, set x0, y0 //
        for (index = ixmin; index < ixmax; index++) {
            final boolean isValid = wds.value(index)>0 && wxds.value(index)>0 ;
            if (isValid) {
                firstIndex = index;  // TODO: what if no valid points?
                index++;
                break;
            }
        }

        if ( firstIndex==-1 ) { // no valid points
            lastIndex= ixmax;
            firstIndex= ixmax;
        }

        // find the last valid point, minding the dataSetSizeLimit
        int pointsPlotted = 0;
        for (index = firstIndex; index < ixmax && pointsPlotted < dataSetSizeLimit; index++) {
            final boolean isValid = wds.value(index)>0 && wxds.value(index)>0;
            if (isValid) {
                pointsPlotted++;
            }
        }

        //if (index < ixmax && pointsPlotted == dataSetSizeLimit) {
        //    dataSetClipped = true;
        //}
        
        if (index < ixmax && pointsPlotted == dataSetSizeLimit) {
            dataSetReduced = true;
            lastIndex= ixmax;
        } else {
            lastIndex = index;
        }
        
        if ( firstIndex==lastIndex && lastIndex==xds.length() ) {
            logger.info("all data removed in firstIndex/lastIndex");
        } else {
            logger.fine("some data found in firstIndex/lastIndex");
        }
        
        logger.log( Level.FINE, "updateFirstLast ds: {0},  firstIndex={1} to lastIndex={2} in {3}ms", new Object[]{ String.valueOf(this.ds), this.firstIndex, this.lastIndex, System.currentTimeMillis()-t0 });
    }

    @Override
    public void setActive( boolean active ) {
        super.setActive(active);
        if ( active ) updatePsym();
    }
    
    private boolean additionalClip = false;

    public static final String PROP_ADDITIONALCLIP = "additionalClip";

    public boolean isAdditionalClip() {
        return additionalClip;
    }

    public void setAdditionalClip(boolean additionalClip) {
        boolean oldAdditionalClip = this.additionalClip;
        this.additionalClip = additionalClip;
        propertyChangeSupport.firePropertyChange(PROP_ADDITIONALCLIP, oldAdditionalClip, additionalClip);
    }


    /**
     * render the dataset by delegating to internal components such as the symbol connector and error bars.
     * @param g the graphics context.
     * @param xAxis the x axis
     * @param yAxis the y axis
     */
    @Override
    public synchronized void render(Graphics2D g, DasAxis xAxis, DasAxis yAxis) {

        ProgressMonitor monitor= new NullProgressMonitor();
        
        DasPlot lparent= getParent();

        logger.log(Level.FINE, "enter {0}.render: {1}", new Object[]{id, String.valueOf(getDataSet()) });
        logger.log( Level.FINER, "ds: {0},  drawing indeces {1} to {2}", new Object[]{ String.valueOf(this.ds), this.firstIndex, this.lastIndex});
        
        if ( this.ds == null && lastException != null) {
            if ( lparent!=null ) lparent.postException(this, lastException);
            return;
        }
        
        if ( renderException!=null ) {
            if ( lparent!=null ) lparent.postException(this, renderException);
            return;
        }

        //renderCount++;
        //reportCount();

        long timer0 = System.currentTimeMillis();

        QDataSet dataSet = getDataSet();

        if (dataSet == null) {
            DasLogger.getLogger(DasLogger.GRAPHICS_LOG).fine("null data set");
            if ( lparent!=null ) lparent.postMessage(this, "no data set", DasPlot.INFO, null, null);
            return;
        }

        if (dataSet.rank() == 0) {
            DasLogger.getLogger(DasLogger.GRAPHICS_LOG).fine("rank 0 data set");
            //if ( lparent!=null ) lparent.postMessage(this, "rank 0 data set: "+dataSet.toString(), DasPlot.INFO, null, null);
            //return;
        } else {
            if (dataSet.length() == 0) {
                DasLogger.getLogger(DasLogger.GRAPHICS_LOG).fine("empty data set");
                if ( lparent!=null ) lparent.postMessage(this, "empty data set", DasPlot.INFO, null, null);
                return;
            }
        
            if ( dataSet.rank()!=1 && ! ( SemanticOps.isBundle(ds) || SemanticOps.isRank2Waveform(ds) || SemanticOps.isRank3JoinOfRank2Waveform(ds) ) ) {
                DasLogger.getLogger(DasLogger.GRAPHICS_LOG).fine("dataset is not rank 1 or a rank 2 waveform");
                if ( lparent!=null ) lparent.postMessage(this, "dataset is not rank 1 or a rank 2 waveform", DasPlot.INFO, null, null);
                return;
            }
        }

        if ( psym== DefaultPlotSymbol.NONE && psymConnector==PsymConnector.NONE && !drawError ) {
            DasLogger.getLogger(DasLogger.GRAPHICS_LOG).fine("plot symbol and symbol connector are set to none");
            if ( lparent!=null ) lparent.postMessage(this, "plot symbol and symbol connector are set to none", DasPlot.INFO, null, null);
            //return;
        }

        if ( lparent!=null ) {
            boolean foreBackSameColor= true;
            if ( !color.equals( lparent.getBackground() ) ) foreBackSameColor= false;
            if ( fillToReference &&  !color.equals( lparent.getBackground() ) ) foreBackSameColor= false;
            if ( lparent.getRenderers().length>1 ) foreBackSameColor= false; // weak test but better than nothing.
            if ( foreBackSameColor ) {
                DasLogger.getLogger(DasLogger.GRAPHICS_LOG).fine("foreground and background colors are the same");
                lparent.postMessage(this, "foreground and background colors are the same", DasPlot.INFO, null, null);
            }
        }
        
        QDataSet tds = null;
        QDataSet vds = null;
        boolean yaxisUnitsOkay;
        boolean xaxisUnitsOkay;
        
        Units yunits;
        
        QDataSet xds = getXTags(dataSet);
        xaxisUnitsOkay = SemanticOps.getUnits(xds).isConvertibleTo(xAxis.getUnits() );
        if ( !SemanticOps.isTableDataSet(dataSet) ) {
            vds= ytagsDataSet(ds);
            yunits= SemanticOps.getUnits(vds);
            yaxisUnitsOkay = yunits.isConvertibleTo(yAxis.getUnits()); // Ha!  QDataSet makes the code the same

        } else {
            tds = (QDataSet) dataSet;
            yunits= SemanticOps.getUnits(tds);
            yaxisUnitsOkay = SemanticOps.getUnits(tds).isConvertibleTo(yAxis.getUnits());
        }

        boolean haveReportedUnitProblem= false;
        if ( !xaxisUnitsOkay && !yaxisUnitsOkay && ( vds!=null && SemanticOps.getUnits(xds)==SemanticOps.getUnits(vds) ) && xAxis.getUnits()==yAxis.getUnits() ) {
             if ( unitsWarning ) {
                //UnitsUtil.isRatioMeasurement( SemanticOps.getUnits(vds) ) && UnitsUtil.isRatioMeasurement( yAxis.getUnits() )
                if ( lparent!=null ) lparent.postMessage( this, "axis units changed from \""+SemanticOps.getUnits(vds) + "\" to \"" + yAxis.getUnits() + "\"", DasPlot.INFO, null, null );
                haveReportedUnitProblem= true;
            } else {
                if ( lparent!=null ) lparent.postMessage( this, "inconvertible axis units", DasPlot.INFO, null, null );
                return;
            }
        }

        if ( !haveReportedUnitProblem && !yaxisUnitsOkay ) {
            if ( unitsWarning ) {
                if ( vds!=null ) { // don't bother with the other mode.
                    if ( yAxis.getUnits()==Units.dimensionless ) {
                        logger.log(Level.FINE, "data units \"{0}\" plotted on dimensionless axis", SemanticOps.getUnits(vds));
                    } else {
                        if ( lparent!=null ) lparent.postMessage( this, "yaxis units changed from \""+SemanticOps.getUnits(vds) + "\" to \"" + yAxis.getUnits() + "\"", DasPlot.INFO, null, null );
                    }
                }
            } else {
                if ( lparent!=null ) lparent.postMessage( this, "inconvertible yaxis units", DasPlot.INFO, null, null );
                return;
            }
        }

        if ( !haveReportedUnitProblem && !xaxisUnitsOkay ) {
            if ( xunitsWarning ) {
                if ( xAxis.getUnits()==Units.dimensionless ) {
                    logger.log(Level.FINE, "data units \"{0}\" plotted on dimensionless axis", SemanticOps.getUnits(xds));
                } else {
                    if ( lparent!=null ) lparent.postMessage( this, "xaxis units changed from \""+SemanticOps.getUnits(xds) + "\" to \"" + xAxis.getUnits() + "\"", DasPlot.INFO, null, null );
                }
            } else {
                if ( lparent!=null ) lparent.postMessage( this, "inconvertible xaxis units", DasPlot.INFO, null, null );
                return;
            }
        }

        int messageCount= 0;

        logger.log(Level.FINER, "rendering points: ds[{0}:{1}]", new Object[]{firstIndex,lastIndex});
        if ( dataSet.rank()>0 && lastIndex == -1 ) {
            if ( messageCount++==0) {
                if ( lparent!=null ) lparent.postMessage(SeriesRenderer.this, "need to update first/last", DasPlot.INFO, null, null);
            }
            update(); //DANGER: this kludge is not well tested, and may cause problems.  It should be the case that another
                      // update is posted that will resolve this problem, but somehow it's not happening when Autoplot adds a
                      // bunch of panels.
            //System.err.println("need to update first/last bit");
            javax.swing.Timer t= new javax.swing.Timer( 200, new ActionListener() {
                @Override
                public void actionPerformed( ActionEvent e ) {
                    update();
                }
            } );
            t.setRepeats(false);
            t.restart();
        }

        if ( lparent!=null ) {
            if (lastIndex == firstIndex && dataSet.rank()>0 ) {
                if ( firstValidIndex==lastValidIndex ) {
                    if ( !dataSetReduced ) {
                        if ( messageCount++==0) lparent.postMessage(SeriesRenderer.this, "dataset contains no valid data", DasPlot.INFO, null, null);
                    }
                }
            }
        }

        Graphics2D graphics = g;

        if (antiAliased) {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        } else {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        }
        
        monitor.started();
        
        boolean drawBackground= backgroundThick.length()>0;
                
        if ( SemanticOps.isRank2Waveform(dataSet) ) {
            
            if ( drawBackground ) { 
                psymConnectorElement.renderBackground(graphics);
                if ( drawError ) {
                    errorElement.renderBackground(graphics);
                }
            }
            
            graphics.setColor(color);
            logger.log(Level.FINEST, "drawing psymConnector in {0}", color);

            int connectCount= psymConnectorElement.render((Graphics2D)graphics.create(), xAxis, yAxis, dataSet, monitor.getSubtaskMonitor("psymConnectorElement.render")); // vds is only to check units
            logger.log(Level.FINEST, "connectCount: {0}", connectCount);

            if ( drawError ) { // error bars show the extent of the waveform
                errorElement.render((Graphics2D)graphics.create(), xAxis, yAxis, dataSet, monitor.getSubtaskMonitor("errorElement.render"));
            }

            int symCount;
            if (psym != DefaultPlotSymbol.NONE) {

                symCount= psymsElement.render((Graphics2D)graphics.create(), xAxis, yAxis, dataSet, monitor.getSubtaskMonitor("psymsElement.render"));
                logger.log(Level.FINEST, "symCount: {0}", symCount);
                
            }
            
        } else if ( dataSet.rank()==2 && dataSet.length(0)==3 && !SemanticOps.isRank2Waveform(dataSet) ) {
            // TODO: Copy of code below!
            
            QDataSet xx= SemanticOps.xtagsDataSet(dataSet);
            QDataSet yy= SemanticOps.ytagsDataSet(dataSet);
            QDataSet zz= Ops.slice1( dataSet, 2);
            
            QDataSet theDs= Ops.link( xx, yy );
            theDs= Ops.putProperty( theDs, QDataSet.PLANE_0, zz );
            
            if ( drawBackground ) {
                fillElement.renderBackground(g);
                psymConnectorElement.renderBackground(graphics);
                if ( drawError ) {
                    errorElement.renderBackground(graphics);
                }
                if (psym != DefaultPlotSymbol.NONE) {
                    psymsElement.renderBackground( graphics );
                }
            }
            
            if (this.fillToReference) {
                fillElement.render( (Graphics2D)graphics.create(), xAxis, yAxis, theDs, monitor.getSubtaskMonitor("fillElement.render"));
            }

            graphics.setColor(color);
            logger.log(Level.FINEST, "drawing psymConnector in {0}", color);
            
            if ( drawError ) { // error bars
                errorElement.render( (Graphics2D)graphics.create(), xAxis, yAxis, theDs, monitor.getSubtaskMonitor("errorElement.render"));
            }
            
            int connectCount= psymConnectorElement.render(graphics, xAxis, yAxis, theDs, monitor.getSubtaskMonitor("psymConnectorElement.render")); // vds is only to check units
            logger.log(Level.FINEST, "connectCount: {0}", connectCount);

            int symCount;
            if (psym != DefaultPlotSymbol.NONE) {
                            
                symCount= psymsElement.render( (Graphics2D)graphics.create(), xAxis, yAxis, theDs, monitor.getSubtaskMonitor("psymsElement.render"));
                logger.log(Level.FINEST, "symCount: {0}", symCount);
                
            }
            
        } else if (tds != null ) {
            
            if ( drawBackground ) { 
                psymConnectorElement.renderBackground(graphics);
                if ( drawError ) {
                    errorElement.renderBackground(graphics);
                }
            }
            
            graphics.setColor(color);
            logger.log(Level.FINEST, "drawing psymConnector in {0}", color);

            int connectCount= psymConnectorElement.render( (Graphics2D)graphics.create(), xAxis, yAxis, tds, monitor.getSubtaskMonitor("psymConnectorElement.render")); // tds is only to check units

            logger.log(Level.FINEST, "connectCount: {0}", connectCount);
            
            if ( drawError ) { // error bars
                errorElement.render( (Graphics2D)graphics.create(), xAxis, yAxis, tds, monitor.getSubtaskMonitor("errorElement.render"));
            }

        } else {
            
            if ( drawBackground ) {
                fillElement.renderBackground(g);
                psymConnectorElement.renderBackground(graphics);
                if ( drawError ) {
                    errorElement.renderBackground(graphics);
                }
                if (psym != DefaultPlotSymbol.NONE) {
                    psymsElement.renderBackground( graphics );
                }
            }
            
            if (this.fillToReference) {
                fillElement.render( (Graphics2D)graphics.create(), xAxis, yAxis, vds, monitor.getSubtaskMonitor("fillElement.render"));
            }

            graphics.setColor(color);
            logger.log(Level.FINEST, "drawing psymConnector in {0}", color);
            
            if ( drawError ) { // error bars
                errorElement.render( (Graphics2D)graphics.create(), xAxis, yAxis, vds, monitor.getSubtaskMonitor("errorElement.render"));
            }
            
            int connectCount= psymConnectorElement.render(graphics, xAxis, yAxis, vds, monitor.getSubtaskMonitor("psymConnectorElement.render")); // vds is only to check units
            logger.log(Level.FINEST, "connectCount: {0}", connectCount);

            int symCount;
            if (psym != DefaultPlotSymbol.NONE) {
                            
                symCount= psymsElement.render( (Graphics2D)graphics.create(), xAxis, yAxis, vds, monitor.getSubtaskMonitor("psymsElement.render"));
                logger.log(Level.FINEST, "symCount: {0}", symCount);
                
            }
        }
        
        // Peek in the METADATA to see if there is LIMITS_WARN_MIN and other flags.
        drawLimits(graphics, yAxis, yunits);
        
        monitor.finished();
        
        long milli = System.currentTimeMillis();
        long renderTime = (milli - timer0);
        double dppms = (lastIndex - firstIndex) / (double) renderTime;

        addToStats( numberOfPoints, renderTime, 'r' );
        setRenderPointsPerMillisecond(dppms);

        logger.log(Level.FINE, "render: {0}ms total:{1} fps:{2} pts/ms:{3}", new Object[]{renderTime, milli - lastUpdateMillis, 1000. / (milli - lastUpdateMillis), dppms});
        lastUpdateMillis = milli;

        int ldataSetSizeLimit= getDataSetSizeLimit();
        
        if ( lparent!=null ) {
            if (dataSetClipped) {
                lparent.postMessage(this, "dataset clipped at " + ldataSetSizeLimit + " points", DasPlot.WARNING, null, null);
            }

            if ( !dataSetReduced ) {
                if ( ( lastIndex_v - firstIndex_v < 2 ) && dataSet.rank()>0 && dataSet.length()>1 ) { //TODO: single point would be helpful for digitizing.
                    if ( messageCount++==0) {
                        if ( lastIndex_v<2 ) {
                            if ( firstValidIndex==lastValidIndex ) {
                                //sftp://papco.org/home/jbf/ct/autoplot/data.backup/examples/d2s/dataOutOfRange.das2Stream
                                lparent.postMessage(this, "dataset contains no plottable data", DasPlot.INFO, null, null);
                            } else {
                                lparent.postMessage(this, "data starts after range", DasPlot.INFO, null, null);
                            }
                        } else if ( this.dslen - this.firstIndex_v < 2 ) {
                            lparent.postMessage(this, "data ends before range", DasPlot.INFO, null, null);
                        } else {
                            lparent.postMessage(this, "fewer than two points visible", DasPlot.INFO, null, null);
                        }
                    }
                }
            } else {
                if ( ( lastIndex_v - firstIndex_v < 1 ) && dataSet.rank()>0 && dataSet.length()>1 ) { 
                    lparent.postMessage(this, "no data is visible", DasPlot.INFO, null, null);
                }
            }
        }

        graphics.dispose();
    }

    /**
     * Look in metadata for LIMITS_WARN_MIN and other annotations.
     * @param graphics
     * @param yAxis
     * @param yunits 
     */
    private void drawLimits(Graphics2D graphics, DasAxis yAxis, Units yunits) {
        Map<String,Object> meta;
        meta= (Map<String,Object>) this.ds.property(QDataSet.METADATA);
        
        if ( meta!=null && showLimits ) {
            Number d;
            DasColumn col= getParent().getColumn();
            Graphics2D graphics1= (Graphics2D)graphics.create();
            d= getKey( meta, "LIMITS_WARN_MIN", Number.class );
            if ( d!=null ) {
                double iy= yAxis.transform( d.doubleValue(), yunits );
                Line2D.Double l= new Line2D.Double( col.getDMinimum(), iy, col.getDMaximum(), iy );
                graphics1.setColor( Color.RED );
                graphics1.setStroke( PsymConnector.DASHES.getStroke(1.0f) );
                graphics1.draw(l);
            }
            d= getKey( meta, "LIMITS_WARN_MAX", Number.class );
            if ( d!=null ) {
                double iy= yAxis.transform( d.doubleValue(), yunits );
                Line2D.Double l= new Line2D.Double( col.getDMinimum(), iy, col.getDMaximum(), iy );
                graphics1.setColor( Color.RED );
                graphics1.setStroke( PsymConnector.DASHES.getStroke(1.0f) );
                graphics1.draw(l);
            }
            d= getKey( meta, "LIMITS_NOMINAL_MIN", Number.class );
            if ( d!=null ) {
                double iy= yAxis.transform( d.doubleValue(), yunits );
                Line2D.Double l= new Line2D.Double( col.getDMinimum(), iy, col.getDMaximum(), iy );
                graphics1.setColor( Color.YELLOW );
                graphics1.setStroke( PsymConnector.DASHES.getStroke(1.0f) );
                graphics1.draw(l);
            }
            d= getKey( meta, "LIMITS_NOMINAL_MAX", Number.class );
            if ( d!=null ) {
                double iy= yAxis.transform( d.doubleValue(), yunits );
                Line2D.Double l= new Line2D.Double( col.getDMinimum(), iy, col.getDMaximum(), iy );
                graphics1.setColor( Color.YELLOW );
                graphics1.setStroke( PsymConnector.DASHES.getStroke(1.0f) );
                graphics1.draw(l);
            }
        }
    }

    private static <T> T getKey( Map<String,Object> meta, String key, Class<T> type ) {
        Object o= meta.get(key);
        if ( o==null || !type.isInstance(o) ) {
            return null;
        } else {
            return type.cast(o);
        }
    }
    
    /**
     * reduce the dataset by coalescing points in the same location.
     * @param xAxis the current xaxis
     * @param yAxis the current yaxis
     * @param vds the dataset
     * @xlimit limit the resolution of the result to so many pixels (/2)
     * @ylimit limit the resolution of the result to so many pixels (/2)
     * @return reduced version of the dataset
     */
    private QDataSet doDataSetReduce( DasAxis xAxis, DasAxis yAxis, QDataSet vds, int xlimit, int ylimit ) {
        DatumRange xdr= xAxis.getDatumRange();
        QDataSet xxx;
        if ( xAxis.isLog() ) {
            final int nstep= Math.max( 2, 2*xAxis.getDLength() );
            final double min = Math.log10( xdr.min().doubleValue(xdr.getUnits()) );
            final double max = Math.log10( xdr.max().doubleValue(xdr.getUnits()) );
            xxx= Ops.exp10( Ops.linspace( min, max, nstep ) );
        } else {
            final int nstep= Math.max( 2, 2*xAxis.getDLength()/xlimit );
            final double min = xdr.min().doubleValue(xdr.getUnits());
            final double max = xdr.max().doubleValue(xdr.getUnits() );
            xxx= Ops.linspace( min, max, nstep );
        }
        MutablePropertyDataSet mxxx= DataSetOps.makePropertiesMutable(xxx);  // it is already
        mxxx.putProperty( QDataSet.UNITS, xdr.getUnits() );
        if ( xAxis.isLog() ) {
            mxxx.putProperty( QDataSet.SCALE_TYPE, QDataSet.VALUE_SCALE_TYPE_LOG ); //TODO: cheat
        }
        DatumRange ydr= yAxis.getDatumRange();
        QDataSet yyy;
        if ( yAxis.isLog() ) {
            final int nstep= Math.max( 2, 2*yAxis.getDLength() );
            final double min= Math.log10( ydr.min().doubleValue(ydr.getUnits()) );
            final double max = Math.log10( ydr.max().doubleValue(ydr.getUnits()) );
            yyy= Ops.exp10( Ops.linspace(min, max, nstep ) );
        } else {
            final int nstep= Math.max( 2, 2*yAxis.getDLength()/ylimit );
            final double min = ydr.min().doubleValue(ydr.getUnits());
            final double max = ydr.max().doubleValue(ydr.getUnits() );
            yyy= Ops.linspace( min, max, nstep );
        }
        MutablePropertyDataSet myyy= DataSetOps.makePropertiesMutable(yyy);  // it is already
        myyy.putProperty( QDataSet.UNITS, ydr.getUnits() );
        if ( yAxis.isLog() ) {
            myyy.putProperty( QDataSet.SCALE_TYPE, QDataSet.VALUE_SCALE_TYPE_LOG ); //TODO: cheat
        }
        long tt0= System.currentTimeMillis();
        if ( mxxx.length()<2 || myyy.length()<2 ) {
            logger.warning("that strange case where Kris saw  rte_1852410924");
            return vds;
        }
        QDataSet hds;
        try {
            hds = Reduction.histogram2D( vds, mxxx, myyy );
        } catch ( InconvertibleUnitsException u ) {
            if ( !SemanticOps.getUnits(myyy).isConvertibleTo(SemanticOps.getUnits(vds) ) ) {
                if ( SemanticOps.getUnits(myyy)==Units.dimensionless || SemanticOps.getUnits(vds)==Units.dimensionless ) {
                    myyy.putProperty( QDataSet.UNITS, SemanticOps.getUnits(vds) );
                }
            }
            if ( vds.property(QDataSet.DEPEND_0)!=null ) {
                Units dsxunits= SemanticOps.getUnits( (QDataSet)vds.property(QDataSet.DEPEND_0));
                if ( !SemanticOps.getUnits(mxxx).isConvertibleTo(dsxunits) )  {
                    if ( SemanticOps.getUnits(mxxx)==Units.dimensionless || dsxunits==Units.dimensionless ) {
                        mxxx.putProperty( QDataSet.UNITS, dsxunits );
                    }
                }
            }
            hds = Reduction.histogram2D( vds, mxxx, myyy );
        }
        QDataSet colors= colorByDataSet(vds);
        if ( colors!=null ) {
            colors= Reduction.lastPointAt2D( colors, SemanticOps.xtagsDataSet(vds), SemanticOps.ytagsDataSet(vds), mxxx, myyy );
        }
        logger.log( Level.FINEST, "done histogram2D ({0}ms)", ( System.currentTimeMillis()-tt0 ));
        DataSetBuilder buildx= new DataSetBuilder(1,100);
        DataSetBuilder buildy= new DataSetBuilder(1,100);
        DataSetBuilder buildc= null;
        if ( colors!=null ) {
            buildc= new DataSetBuilder(1,100);
            buildc.putProperty( QDataSet.UNITS,colors.property(QDataSet.UNITS) );
            for ( int ii=0; ii<hds.length(); ii++ ) {                
                for ( int jj=0; jj<hds.length(0); jj++ ) {
                    if ( hds.value(ii,jj)>0 ) {
                        buildx.nextRecord(xxx.value(ii));
                        buildy.nextRecord(yyy.value(jj));
                        buildc.nextRecord(colors.value(ii,jj));
                    }
                }
            }            
        } else {
            for ( int ii=0; ii<hds.length(); ii++ ) {                
                for ( int jj=0; jj<hds.length(0); jj++ ) {
                    if ( hds.value(ii,jj)>0 ) {
                        buildx.putValue(-1,xxx.value(ii));
                        buildy.putValue(-1,yyy.value(jj));
                        buildx.nextRecord(); 
                        buildy.nextRecord();
                    }
                }
            }
        }
        // logger.log( Level.FINEST, "build new 1-D dataset: {0}", ( System.currentTimeMillis()-tt0 )); // this takes a trivial (<3ms) amount of time.
        buildx.putProperty( QDataSet.UNITS, xdr.getUnits() );
        buildy.putProperty( QDataSet.UNITS, ydr.getUnits() );
        MutablePropertyDataSet mvds= DataSetOps.makePropertiesMutable( Ops.link( buildx.getDataSet(), buildy.getDataSet() ) );
        Map<String,Object> p= DataSetUtil.getDimensionProperties(vds,null);
        p.remove( QDataSet.VALID_MIN );
        p.remove( QDataSet.VALID_MAX );
        p.remove( QDataSet.FILL_VALUE );
        p.remove( QDataSet.UNITS );
        DataSetUtil.putProperties( p, mvds );
        if ( buildc!=null ) {
            mvds.putProperty(QDataSet.PLANE_0,buildc.getDataSet());
        }
        return mvds;
    }

    /**
     * Do the same as updatePlotImage, but use AffineTransform to implement axis transform.  This is the
     * main updatePlotImage method, which will delegate to internal components of the plot, such as connector
     * and error bar elements.
     * @param xAxis the x axis
     * @param yAxis the y axis
     * @param monitor progress monitor is used to provide feedback
     */
    @Override
    public synchronized void updatePlotImage(DasAxis xAxis, DasAxis yAxis, ProgressMonitor monitor) {
        
        if ( monitor==null ) monitor= new NullProgressMonitor();
        
        long t0= System.currentTimeMillis();
        logger.log(Level.FINE, "enter {0}.updatePlotImage: {1}", new Object[]{id, String.valueOf(getDataSet()) });

        super.incrementUpdateCount();

        QDataSet dataSet = getDataSet();
        selectionArea= SelectionUtil.NULL;

        if (dataSet == null ) {
            logger.fine("dataset was null");
            return;
        }

        if ( dataSet.rank() == 0) {
            logger.fine("rank 0 dataset will not work with older Autoplots");
        } else {
            if ( dataSet.length() == 0) {
                logger.fine("dataset was empty");
                return;
            }
        }

        if ( !this.isActive() ) {
            return;
        }
        
        boolean plottable;

        QDataSet tds = null;
        QDataSet vds = null;

        QDataSet xds = getXTags(dataSet);

        Units yunits;
        
        if ( dataSet.rank()>0 && dataSet.rank()<3 && !SemanticOps.isRank2Waveform(dataSet) ) {  // xtags are rank 2 bins can happen too
            vds= ytagsDataSet(ds);
            if ( vds==null ) {
                logger.fine("dataset is not rank 1 or a rank 2 waveform");
                return;
            }
            if ( xds.rank()!=1 ) {
                logger.fine("dataset xtags are not rank 1.");
                return;
            }
            if ( vds.rank()!=1 ) {
                logger.fine("dataset is rank 2 and not a bundle.");
                return;
            }
            if ( ds.rank()!=1 && !SemanticOps.isBundle(ds) ) {
                logger.fine("dataset is rank 2 and not a bundle");
                return;
            }
            if ( vds.length()!=ds.length() ) {
                logger.fine("dataset is rank 2 and will cause problems");
                return;
            }
            unitsWarning= false;
            yunits=  SemanticOps.getUnits(vds);
            plottable = yunits.isConvertibleTo(yAxis.getUnits()) || 
                    ( yAxis.getUnits()==Units.dimensionless && UnitsUtil.isRatioMeasurement( yunits ) );
            if ( !plottable ) {
                if ( UnitsUtil.isRatioMeasurement( yunits ) && UnitsUtil.isRatioMeasurement( yAxis.getUnits() ) ) {
                    unitsWarning= true;
                }
            } else {
                if (  !yunits.isConvertibleTo(yAxis.getUnits()) && 
                    ( yAxis.getUnits()==Units.dimensionless && UnitsUtil.isRatioMeasurement( yunits ) ) ) {
                    unitsWarning= true;
                }
            }

        } else if (SemanticOps.isRank2Waveform(dataSet)) {
            tds = (QDataSet) dataSet;
            yunits= SemanticOps.getUnits(tds);
            plottable = yunits.isConvertibleTo(yAxis.getUnits()) || 
                    ( yAxis.getUnits()==Units.dimensionless && UnitsUtil.isRatioMeasurement( yunits ) );
            if ( !plottable ) {
                if ( UnitsUtil.isRatioMeasurement( yunits ) && UnitsUtil.isRatioMeasurement( yAxis.getUnits() ) ) {
                    unitsWarning= true;
                }
            }
        } else if ( SemanticOps.isRank3JoinOfRank2Waveform(dataSet) ) {
            tds = (QDataSet) dataSet;
            yunits= SemanticOps.getUnits(tds);            
            plottable = yunits.isConvertibleTo(yAxis.getUnits()) || 
                    ( yAxis.getUnits()==Units.dimensionless && UnitsUtil.isRatioMeasurement( yunits ) );
            if ( !plottable ) {
                if ( UnitsUtil.isRatioMeasurement( yunits ) && UnitsUtil.isRatioMeasurement( yAxis.getUnits() ) ) {
                    unitsWarning= true;
                }
            }
        } 

        plottable = SemanticOps.getUnits(xds).isConvertibleTo(xAxis.getUnits());
        xunitsWarning= false;
        if ( !plottable ) {
            if ( UnitsUtil.isRatioMeasurement( SemanticOps.getUnits(xds) ) && UnitsUtil.isRatioMeasurement( xAxis.getUnits() ) ) {
                plottable= true; // we'll provide a warning
                xunitsWarning= true;
            }
        }

        if (!plottable) {
            return;
        }

        dataSetClipped = false;  // disabled while experimenting with dataSetReduced.
        dataSetReduced = false;

        firstIndex= -1;
        lastIndex= -1;
        
        monitor.started();
        
        try {
            if (vds != null) {

                try {
                    updateFirstLast(xAxis, yAxis, xds, vds );
                } catch ( IllegalArgumentException ex ) { // InconvertibleUnitsException, data all fill.
                    renderException= ex;
                    logger.log( Level.INFO, ex.getMessage(), ex );
                    return;
                }
                numberOfPoints= lastIndex-firstIndex;
                if ( Schemes.isBundleDataSet(ds) ) {
                    dataSetReduced= false;
                }
                if ( dataSetReduced ) {
                    logger.fine("reducing data that is bigger than dataSetSizeLimit");

                    QDataSet mvds;
                    try {
                        mvds= doDataSetReduce( xAxis, yAxis, vds, 1, 1 );
                    } catch ( InconvertibleUnitsException ex ) {
                        logger.warning("InconvertibleUnitsException");
                        logger.log( Level.INFO, ex.getMessage(), ex );
                        return;
                    }

                    vds= mvds;
                    xds= SemanticOps.xtagsDataSet(vds);

                    updateFirstLast(xAxis, yAxis, xds, vds );  // we need to reset firstIndex, lastIndex

                    logger.log( Level.FINER, "data reduced to {0} {1}", new Object[] { vds, Ops.extent(xds) } );
                    logger.log( Level.FINER, "reduceDataSet complete ({0}ms)", System.currentTimeMillis()-t0 );                
                } else {
                    logger.log( Level.FINER, "data not reduced");
                }

                if (fillToReference) {
                    fillElement.update(xAxis, yAxis, vds, monitor.getSubtaskMonitor("fillElement.update"));
                    logger.log( Level.FINER, "fillElement.update complete ({0}ms)", System.currentTimeMillis()-t0 );
                }

            } else if (tds != null) {

                LoggerManager.resetTimer("render waveform");

                updateFirstLast(xAxis, yAxis, xds, tds ); // minimal support assumes vert slice data is all valid or all invalid.
                LoggerManager.markTime("updateFirstLast");

                if ( SemanticOps.isRank2Waveform(dataSet) ) {
                    QDataSet res= DataSetUtil.asDataSet( xAxis.getDatumRange().width().divide(xAxis.getWidth()) );
                    vds= dataSet.trim( this.firstIndex, this.lastIndex );
                    LoggerManager.markTime("trim");

                    QDataSet dep1= (QDataSet) vds.property(QDataSet.DEPEND_1);
                    if ( dep1.rank()==1 ) {
                        vds= Reduction.reducex( vds,res );  // waveform
                    }
                    LoggerManager.markTime("reducex");
                    if ( vds.rank()==2 ) {
                        vds= DataSetOps.flattenWaveform(vds);
                        LoggerManager.markTime("flatten");
                    }
                    xds= SemanticOps.xtagsDataSet(vds); 
                    updateFirstLast(xAxis, yAxis, xds, vds );  // we need to reset firstIndex, lastIndex
                    LoggerManager.markTime("updateFirstLast again");
                } else if ( SemanticOps.isRank3JoinOfRank2Waveform(dataSet) ) { // we shall flatten the whole thing
                    QDataSet res= DataSetUtil.asDataSet( xAxis.getDatumRange().width().divide(xAxis.getWidth()) );
                    Units dsxu= SemanticOps.getUnits( (QDataSet)dataSet.slice(0).property(QDataSet.DEPEND_0 ) );
                    if ( !SemanticOps.getUnits(res).isConvertibleTo(dsxu.getOffsetUnits())  ) {
                        res= Ops.putProperty( res, QDataSet.UNITS, dsxu.getOffsetUnits() );
                    }
                    vds= null;
                    for ( int k=this.firstIndex; k< this.lastIndex; k++ ) {
                        boolean xmono= true;
                        QDataSet ds1= dataSet.slice(k);
                        QDataSet xds1= (QDataSet)ds1.property(QDataSet.DEPEND_0);
                        int firstIndex1 = xmono ? DataSetUtil.getPreviousIndex( xds1, xAxis.getDatumRange().min() ) : 0;
                        int lastIndex1 = xmono ? DataSetUtil.getNextIndex( xds1, xAxis.getDatumRange().max() ) : ds1.length();
                        if ( firstIndex1==lastIndex1 && lastIndex1==ds1.length()-1 ) {
                            lastIndex1= ds1.length();
                        }
                        if ( firstIndex1==lastIndex1 ) {
                            continue;
                        }
                        ds1= ds1.trim(firstIndex1,lastIndex1);
                        LoggerManager.markTime("trim");
                        QDataSet vds1= Reduction.reducex( ds1,res );  // waveform
                        LoggerManager.markTime("reducex");
                        if ( SemanticOps.isRank2Waveform(vds1) ) {
                            vds1= DataSetOps.flattenWaveform(vds1);
                            LoggerManager.markTime("flatten");
                        } else if ( vds1.rank()==2 ) {
                            continue;
                        }
                        if ( vds==null ) {
                            vds= vds1;
                        } else {
                            vds= Ops.append( vds,vds1 );
                        }
                    }
                    if ( vds==null ) {
                        getParent().postMessage( this, "first point of waveform package is not visible", Level.WARNING, null, null );
                        return;
                    }
                    xds= SemanticOps.xtagsDataSet(vds); 
                    updateFirstLast(xAxis, yAxis, xds, vds );  // we need to reset firstIndex, lastIndex
                    LoggerManager.markTime("updateFirstLast again");
                }
                logger.log(Level.FINER, "renderWaveform updateFirstLast complete ({0}ms)", System.currentTimeMillis()-t0 );
            } else if ( ds.rank()==0 ) {
                
            } else {
                System.err.println("both tds and vds are null");
            }

            logger.log( Level.FINER, "updatePlotImage uses subset from firstIndex, lastIndex: {0}, {1} ({2} points})", new Object[]{ firstIndex, lastIndex, lastIndex-firstIndex } );
            
            if (psymConnector != PsymConnector.NONE) {
                try {
                    if ( vds!=null && vds.rank()==1 && dataSet.rank()==2 && SemanticOps.isBundle(dataSet) ) {
                        psymConnectorElement.update(xAxis, yAxis, dataSet, monitor.getSubtaskMonitor("psymConnectorElement.update")); 
                    } else if ( dataSet.rank()==0 ) {
                        psymConnectorElement.update(xAxis, yAxis, dataSet, monitor.getSubtaskMonitor("psymConnectorElement.update")); 
                    } else {
                        psymConnectorElement.update(xAxis, yAxis, vds, monitor.getSubtaskMonitor("psymConnectorElement.update"));
                    }
                } catch ( InconvertibleUnitsException ex ) {
                    return;
                }
            }

            try {
                if ( dataSet.rank()==0 ) {
                    
                } else {
                    errorElement.update(xAxis, yAxis, vds, monitor.getSubtaskMonitor("errorElement.update"));
                    if ( vds!=null && vds.rank()==1 && dataSet.rank()==2 && SemanticOps.isBundle(dataSet) ) {
                        psymsElement.update(xAxis, yAxis, dataSet, monitor.getSubtaskMonitor("psymsElement.update"));     // color scatter
                    } else {
                        psymsElement.update(xAxis, yAxis, vds, monitor.getSubtaskMonitor("psymsElement.update"));
                    }
                    logger.log(Level.FINER, "psymsElement.update complete ({0}ms)", System.currentTimeMillis()-t0 );            
                }
            } catch ( InconvertibleUnitsException ex ) {
                return;
            }

            if ( vds!=null ) {
                if ( vds.rank()!=1 ) {
                    return; // transitional case.
                }
                if ( firstIndex==0 && lastIndex==xds.length() ) {
                    selectionArea= calcSelectionArea( xAxis, yAxis, xds, vds );
                } else if ( firstIndex==-1 && lastIndex==-1 ) {
                    selectionArea= SelectionUtil.NULL;
                } else {
                    selectionArea= calcSelectionArea( xAxis, yAxis, xds.trim(firstIndex,lastIndex), vds.trim(firstIndex,lastIndex) );        
                }
            }
            
            logger.log(Level.FINER, "calcSelectionArea complete ({0}ms)", System.currentTimeMillis()-t0);  
            //if (getParent() != null) {
            //    getParent().repaint();
            //}

        } finally {
            monitor.finished();
        }

        long milli = System.currentTimeMillis();
        long renderTime = (milli - t0);
        double dppms = (lastIndex - firstIndex) / (double) renderTime;

        addToStats( numberOfPoints, renderTime, 'u' );
        
        logger.log(Level.FINE, "done updatePlotImage ({0}ms)", renderTime );
        
        setUpdatesPointsPerMillisecond(dppms);
    }

    /**
     * return the xtags or CONTEXT_0 for rank 0.
     * @param dataSet
     * @return 
     */
    private QDataSet getXTags(QDataSet dataSet) {
        QDataSet xds;
        if ( dataSet.rank()==0 ) {
            xds= (QDataSet)dataSet.property(QDataSet.CONTEXT_0);
            if ( xds==null ) {
                xds= DataSetUtil.asDataSet(0);
            } else if ( xds.rank()==1 ) { // take the first one.
                xds= xds.slice(0);
            }
        } else {
            xds= SemanticOps.xtagsDataSet(dataSet);
        }
        return xds;
    }

    /**
     * calculate a selection area that is used to indicate selection and is a target for mouse clicks.
     * When a dataset is longer than 10000 points, the connecting lines are not included in the Shape.
     * @param xaxis the xaxis
     * @param yaxis the yaxis
     * @param xds the x values
     * @param ds the y values
     * @return a Shape that can be rendered within 100 millis.
     */
    private Shape calcSelectionArea( DasAxis xaxis, DasAxis yaxis, QDataSet xds, QDataSet ds ) {
        
        long t0= System.currentTimeMillis();

        boolean _unitsWarning;
        boolean _xunitsWarning;
        synchronized (this) {
            _unitsWarning= this.unitsWarning;
            _xunitsWarning= this.xunitsWarning;
        }
        
        Datum widthx;
        if (xaxis.isLog()) {
            widthx = Units.logERatio.createDatum(Math.log(xaxis.getDataMaximum(xaxis.getUnits()) - xaxis.getDataMinimum(xaxis.getUnits())));
        } else {
            widthx = xaxis.getDatumRange().width();
        }
        Datum widthy;
        if (yaxis.isLog()) {
            widthy = Units.logERatio.createDatum(Math.log(yaxis.getDataMaximum(yaxis.getUnits()) - yaxis.getDataMinimum(yaxis.getUnits())));
        } else {
            widthy = yaxis.getDatumRange().width();
        }

        if ( xaxis.getColumn().getWidth()==0 || yaxis.getRow().getHeight()==0 ) return null;
        
        QDataSet ds2= ds;
        if ( _unitsWarning ) {
            ArrayDataSet ds3= ArrayDataSet.copy(ds);
            ds3.putProperty( QDataSet.UNITS, yaxis.getUnits() );
            ds2= ds3;
        }
        if ( _xunitsWarning ) {
            ArrayDataSet ds3= ArrayDataSet.copy(xds);
            ds3.putProperty( QDataSet.UNITS, xaxis.getUnits() );
            xds= ds3;
        }
        if ( ds2.rank()==2 ) {
            ds2= DataSetOps.slice1( ds2, 0 );
        }

        try {
            if ( this.psymConnector==PsymConnector.NONE || ds2.length()>10000 ) {
                if ( ds2.property(QDataSet.DEPEND_0)==null ) {
                    ds2= Ops.putProperty( ds2, QDataSet.DEPEND_0, xds );
                }
                QDataSet reduce= doDataSetReduce( xaxis, yaxis, ds2, 5, 5 );

                logger.fine( String.format( "reduce path in calcSelectionArea: %s\n", reduce ) );
                GeneralPath path = GraphUtil.getPath( xaxis, yaxis, SemanticOps.xtagsDataSet(reduce), reduce, GraphUtil.CONNECT_MODE_SCATTER, true );

                Shape s = new BasicStroke( Math.min(14,(float)getSymSize()+8.f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND ).createStrokedShape(path);
                return s;                
                
            } else {
                
                if ( xds.length()!=ds2.length() ) {
                    return SelectionUtil.NULL;
                }
                
                QDataSet reduce = VectorUtil.reduce2D(
                    xds, ds2,
                    0,
                    xds.length(),
                    widthx.divide(xaxis.getColumn().getWidth()/5.),
                    widthy.divide(yaxis.getRow().getHeight()/5.)
                    );

                logger.fine( String.format( "reduce path in calcSelectionArea: %s\n", reduce ) );
                GeneralPath path = GraphUtil.getPath( xaxis, yaxis, SemanticOps.xtagsDataSet(reduce), reduce, histogram ? GraphUtil.CONNECT_MODE_HISTOGRAM : GraphUtil.CONNECT_MODE_SERIES, true );

                Shape s = new BasicStroke( Math.min(14,(float)getSymSize()+8.f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND ).createStrokedShape(path);
                return s;
            }

        } catch ( InconvertibleUnitsException ex ) {
            logger.fine("failed to convert units in calcSelectionArea");
            return SelectionUtil.NULL; // transient state, hopefully...

        } finally {
            logger.log(Level.FINE, "done calcSelectionArea ({0}ms)", System.currentTimeMillis()-t0 );
        }

        

    }

    @Override
    protected void installRenderer() {
        if (!DasApplication.getDefaultApplication().isHeadless()) {
            DasPlot lparent= getParent();
            if ( lparent==null ) throw new IllegalArgumentException("parent not set");
            DasMouseInputAdapter mouseAdapter = lparent.mouseAdapter;
            DasPlot p = lparent;
            mouseAdapter.addMouseModule(new LengthMouseModule(p, new LengthDragRenderer(p, p.getXAxis(), p.getYAxis()), "Length"));

            MouseModule ch = new CrossHairMouseModule(lparent, this, lparent.getXAxis(), lparent.getYAxis());
            mouseAdapter.addMouseModule(ch);
        }

        updatePsym();
    }

    @Override
    protected void uninstallRenderer() {
        //There's a bug here, that if multiple SeriesRenderers are drawing on a plot, then we want to leave the mouseModules.
        //We can't do that right now...
    }

    public Element getDOMElement(Document document) {
        return null;
    }

    /**
     * get an Icon representing the trace.  This will be an ImageIcon.
     * TODO: cache the result to support use in legend.
     * @return
     */
    @Override
    public javax.swing.Icon getListIcon() {
        Image i = new BufferedImage(15, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) i.getGraphics();
        drawListIcon( g, 0, 0 );
        return new ImageIcon(i);
    }

    @Override
    public void drawListIcon(Graphics2D g1, int x, int y) {

        Graphics2D g= (Graphics2D) g1.create( x, y, 16, 16 );

        g.setRenderingHints(DasProperties.getRenderingHints());

        DasPlot lparent= getParent();
        if ( lparent!=null ) g.setBackground(lparent.getBackground());

        // leave transparent if not white
        if (color.equals(Color.white)) {
            g.setColor(Color.GRAY);
        } else {
            g.setColor(new Color(0, 0, 0, 0));
        }

        g.fillRect(0, 0, 15, 10);

        if (fillToReference) {
            g.setColor(fillColor);
            Polygon p = new Polygon(new int[]{2, 13, 13, 2}, new int[]{3, 7, 10, 10}, 4);
            g.fillPolygon(p);
        }

        g.setColor(color);
        Stroke stroke0 = g.getStroke();
        getPsymConnector().drawLine(g, 2, 3, 13, 7, 1.5f);
        g.setStroke(stroke0);
        // psym.draw(g, 7, 5, 3.f, fillStyle);
        // psym.draw(g, 7, 5, 8.f, fillStyle);  // Bigger dot for summary plot (HACK)
        psym.draw(g, 7, 5, listIconSymSize, fillStyle);  // the size of this is now settable
    }

    private float listIconSymSize = 3.f;
    public void setListIconSymSize(float newSize) {
       listIconSymSize = newSize;
       refreshRender();
    }

    @Override
    public String getListLabel() {
        return "series";
    }

    /**
     * trigger render, but not updatePlotImage.
     */
    private void refreshRender() {
        DasPlot lparent= getParent();
        if ( lparent!=null ) {
            lparent.invalidateCacheImage();
            lparent.repaint();
        }
    }
    
// ------- Begin Properties ---------------------------------------------  //
    public PsymConnector getPsymConnector() {
        return psymConnector;
    }

    public void setPsymConnector(PsymConnector p) {
        PsymConnector old = this.psymConnector;
        if (!p.equals(psymConnector)) {
            psymConnector = p;
            updateCacheImage();
            propertyChangeSupport.firePropertyChange("psymConnector", old, p);
        }

    }

    /** Getter for property psym.
     * @return Value of property psym.
     */
    public PlotSymbol getPsym() {
        return this.psym;
    }

    /** Setter for property psym.
     * @param psym New value of property psym.
     */
    public void setPsym(PlotSymbol psym) {
        if (psym == null) {
            throw new NullPointerException("psym cannot be null");
        }

        if (psym != this.psym) {
            Object oldValue = this.psym;
            this.psym = (DefaultPlotSymbol) psym;
            updatePsym();
            refreshRender();
            propertyChangeSupport.firePropertyChange("psym", oldValue, psym);
        }

    }

    /**
     * true if error bars should be drawn when available.
     */
    private boolean drawError = true;

    public static final String PROP_DRAWERROR = "drawError";

    public boolean isDrawError() {
        return drawError;
    }

    public void setDrawError(boolean drawError) {
        boolean oldDrawError = this.drawError;
        this.drawError = drawError;
        if ( oldDrawError!=drawError ) {
            update();
        }
        propertyChangeSupport.firePropertyChange(PROP_DRAWERROR, oldDrawError, drawError);
    }

    /** Getter for property symsize.
     * @return Value of property symsize.
     */
    public double getSymSize() {
        return this.symSize;
    }

    /** Setter for property symsize.
     * @param symSize New value of property symsize.
     */
    public void setSymSize(double symSize) {
        float old = this.symSize;
        if (this.symSize != symSize) {
            this.symSize =(float)symSize;
            setPsym(this.psym);
            updatePsym();
            refreshRender();
            propertyChangeSupport.firePropertyChange("symSize", old, symSize );
        }

    }

    /** Getter for property color.
     * @return Value of property color.
     */
    public Color getColor() {
        return color;
    }

    /** Setter for property color.
     * @param color New value of property color.
     */
    public void setColor(Color color) {
        if (color == null) {
            throw new IllegalArgumentException("null color");
        }
        Color old = this.color;
        if (!this.color.equals(color)) {
            this.color = color;
            updatePsym();
            refreshRender();
            propertyChangeSupport.firePropertyChange("color", old, color);
        }

    }

    public double getLineWidth() {
        return lineWidth;
    }

    /**
     * set the width of the connecting lines.
     * @param f 
     */
    public void setLineWidth(double f) {
        double old = this.lineWidth;
        if (this.lineWidth != f) {
            lineWidth = (float)f;
            updatePsym();
            refreshRender();
            propertyChangeSupport.firePropertyChange("lineWidth", old, f );
        }

    }

    private String backgroundThick = "";

    public static final String PROP_BACKGROUNDWIDTH = "backgroundWidth";

    public String getBackgroundThick() {
        return backgroundThick;
    }

    /**
     * set the width of background lines, for example "2em" is twice the 
     * thickness of the line.
     * 
     * @param backgroundThick 
     */
    public void setBackgroundThick(String backgroundThick) {
        String oldBackgroundWidth = this.backgroundThick;
        this.backgroundThick = backgroundThick;
        if ( !oldBackgroundWidth.equals(backgroundThick) ) {
            updatePsym();
            refreshRender();
        }
        propertyChangeSupport.firePropertyChange(PROP_BACKGROUNDWIDTH, oldBackgroundWidth, backgroundThick);
    }

    /** Getter for property antiAliased.
     * @return Value of property antiAliased.
     *
     */
    public boolean isAntiAliased() {
        return this.antiAliased;
    }

    /** Setter for property antiAliased.
     * @param antiAliased New value of property antiAliased.
     *
     */
    public void setAntiAliased(boolean antiAliased) {
        boolean old = this.antiAliased;
        this.antiAliased = antiAliased;
        updatePsym();
        updateCacheImage();
        propertyChangeSupport.firePropertyChange("antiAliased", old, antiAliased);
    }

    public boolean isHistogram() {
        return histogram;
    }

    public void setHistogram(final boolean b) {
        boolean old = b;
        if (b != histogram) {
            histogram = b;
            updateCacheImage();
            propertyChangeSupport.firePropertyChange("histogram", old, antiAliased);
        }

    }

    /**
     * Holds value of property fillColor.
     */
    private Color fillColor = Color.lightGray;

    /**
     * Getter for property fillReference.
     * @return Value of property fillReference.
     */
    public Color getFillColor() {
        return this.fillColor;
    }

    
    /**
     * Setter for property fillReference.
     * @param color the color
     */
    public void setFillColor(Color color) {
        Color old = this.fillColor;
        if (!this.fillColor.equals(color)) {
            this.fillColor = color;
            update();
            propertyChangeSupport.firePropertyChange("fillColor", old, color);
        }
    }
    
    /**
     * One of ""
     */
    private String fillDirection = "both";

    public static final String PROP_FILLDIRECTION = "fillDirection";

    public String getFillDirection() {
        return fillDirection;
    }

    public void setFillDirection(String fillDirection) {
        String oldFillDirection = this.fillDirection;
        this.fillDirection = fillDirection;
        update();
        propertyChangeSupport.firePropertyChange(PROP_FILLDIRECTION, oldFillDirection, fillDirection);
    }

    /**
     * Holds value of property colorByDataSetId.
     */
    private String colorByDataSetId = "";

    /**
     * Getter for property colorByDataSetId.
     * @return Value of property colorByDataSetId.
     */
    public String getColorByDataSetId() {
        return this.colorByDataSetId;
    }

    /**
     * The dataset plane to use to get colors.  If this is null or "", then
     * no coloring is done. (Note the default plane cannot be used to color.)
     * @param colorByDataSetId New value of property colorByDataSetId.
     */
    public void setColorByDataSetId(String colorByDataSetId) {
        String oldVal = this.colorByDataSetId;
        this.colorByDataSetId = colorByDataSetId;
        update();
        if ( !colorByDataSetId.equals("") ) updatePsym();
        propertyChangeSupport.firePropertyChange("colorByDataSetId", oldVal, colorByDataSetId);
    }

    PropertyChangeListener colorBarListener= new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
           if (colorByDataSetId != null && !colorByDataSetId.equals("")) {
               if ( evt.getPropertyName().equals(DasColorBar.PROPERTY_TYPE) ) {
                   updatePsym();
                   update();
               } else if ( evt.getPropertyName().equals(DasAxis.PROPERTY_DATUMRANGE) ) {
                   update();
               } else if ( evt.getPropertyName().equals(DasAxis.PROP_LOG) ) {
                   update();
               }
           }
        }
    };

    /**
     * Setter for property colorBar.
     * @param cb New value of property colorBar.
     */
    @Override
    public void setColorBar(DasColorBar cb) {
        if ( this.colorBar == cb) {
            return;
        }
        if (colorBar != null) {
            colorBar.removePropertyChangeListener( colorBarListener );
        }
        super.setColorBar(cb);
        if (colorBar != null) {
            final DasPlot parent= getParent();
            if (parent != null && parent.getCanvas() != null) {
                final DasCanvas dasCanvas= parent.getCanvas();
                Runnable run= new Runnable() {
                    @Override
                    public void run() {
                        dasCanvas.add(colorBar);
                    }
                };
                if ( SwingUtilities.isEventDispatchThread() ) {
                    run.run();
                } else {
                    SwingUtilities.invokeLater(run);
                }
            }
            colorBar.addPropertyChangeListener( colorBarListener );        
        }
        updateCacheImage();
        updatePsym();
    }
    /**
     * Holds value of property fillToReference.
     */
    private boolean fillToReference;

    /**
     * Getter for property fillToReference.
     * @return Value of property fillToReference.
     */
    public boolean isFillToReference() {
        return this.fillToReference;
    }

    /**
     * Setter for property fillToReference.
     * @param fillToReference New value of property fillToReference.
     */
    public void setFillToReference(boolean fillToReference) {
        boolean old = this.fillToReference;
        if (this.fillToReference != fillToReference) {
            this.fillToReference = fillToReference;
            update();
            propertyChangeSupport.firePropertyChange("fillToReference", old, fillToReference);
        }

    }
    /**
     * Holds value of property reference.
     */
    private Datum reference = Units.dimensionless.createDatum(0);

    /**
     * Getter for property reference.
     * @return Value of property reference.
     */
    public Datum getReference() {
        return this.reference;
    }

    /**
     * Setter for property reference.
     * @param reference New value of property reference.
     */
    public void setReference(Datum reference) {
        Datum old = this.reference;
        if (!this.reference.equals(reference)) {
            this.reference = reference;
            updateCacheImage();
            propertyChangeSupport.firePropertyChange("reference", old, reference);
        }

    }
    
    private ErrorBarType errorBarType = ErrorBarType.BAR;

    public static final String PROP_ERRORBARTYPE = "errorBarType";

    public ErrorBarType getErrorBarType() {
        return errorBarType;
    }

    public void setErrorBarType(ErrorBarType errorBarType) {
        ErrorBarType oldErrorBarType = this.errorBarType;
        this.errorBarType = errorBarType;
        updateCacheImage();
        propertyChangeSupport.firePropertyChange(PROP_ERRORBARTYPE, oldErrorBarType, errorBarType);
    }


    /**
     * Holds value of property resetDebugCounters.
     */
    private boolean resetDebugCounters;

    /**
     * Getter for property resetDebugCounters.
     * @return Value of property resetDebugCounters.
     */
    public boolean isResetDebugCounters() {
        return this.resetDebugCounters;
    }

    /**
     * Setter for property resetDebugCounters.
     * @param resetDebugCounters New value of property resetDebugCounters.
     */
    public void setResetDebugCounters(boolean resetDebugCounters) {
        if (resetDebugCounters) {
            //renderCount = 0;
            //updateImageCount = 0;
            update();
        }

    }
    /**
     * Holds value of property simplifyPaths.
     */
    private boolean simplifyPaths = true;

    /**
     * Getter for property simplifyPaths.
     * @return Value of property simplifyPaths.
     */
    public boolean isSimplifyPaths() {
        return this.simplifyPaths;
    }

    /**
     * Setter for property simplifyPaths.
     * @param simplifyPaths New value of property simplifyPaths.
     */
    public void setSimplifyPaths(boolean simplifyPaths) {
        this.simplifyPaths = simplifyPaths;
        updateCacheImage();
    }
    private boolean stampPsyms = true;
    public static final String PROP_STAMPPSYMS = "stampPsyms";

    public boolean isStampPsyms() {
        return this.stampPsyms;
    }

    public void setStampPsyms(boolean newstampPsyms) {
        boolean oldstampPsyms = stampPsyms;
        this.stampPsyms = newstampPsyms;
        propertyChangeSupport.firePropertyChange(PROP_STAMPPSYMS, oldstampPsyms, newstampPsyms);
        updateCacheImage();
    }

    /**
     * how each plot symbol is filled.
     * @return 
     */
    public FillStyle getFillStyle() {
        return fillStyle;
    }

    /**
     * how each plot symbol is filled.
     * @param fillStyle 
     */
    public void setFillStyle(FillStyle fillStyle) {
        this.fillStyle = fillStyle;
        updatePsym();
        updateCacheImage();
    }

    /**
     * like accept context, but provides a shape to indicate selection.  This
     * should be roughly the same as the locus of points where acceptContext is
     * true.
     * @return
     */
    public synchronized Shape selectionArea() {
        return selectionArea==null ? SelectionUtil.NULL : selectionArea;
    }

    @Override
    public boolean acceptContext(int x, int y) {
        boolean accept = false;

        Point2D.Double dp = new Point2D.Double(x, y);

        if (this.fillToReference && fillElement.acceptContext(dp)) {
            accept = true;
        }

        if ((!accept) && psymConnectorElement.acceptContext(dp)) {
            accept = true;
        }

        if ((!accept) && psymsElement.acceptContext(dp)) {
            accept = true;
        }

        if ((!accept) && drawError && errorElement.acceptContext(dp)) {
            accept = true;
        }

        return accept;
    }
    /**
     * property dataSetSizeLimit is the maximum number of points that will be rendered.
     * This is introduced because large datasets cause java2D plotting to fail.
     * When the size limit is reached, the data is clipped and a message is displayed.
     */
    private int dataSetSizeLimit = 200000;

    /**
     * Getter for property dataSetSizeLimit.
     * @return Value of property dataSetSizeLimit.
     */
    public synchronized int getDataSetSizeLimit() {
        return this.dataSetSizeLimit;
    }

    /**
     * Setter for property dataSetSizeLimit.
     * @param dataSetSizeLimit New value of property dataSetSizeLimit.
     */
    public synchronized void setDataSetSizeLimit(int dataSetSizeLimit) {
        int oldDataSetSizeLimit = this.dataSetSizeLimit;
        this.dataSetSizeLimit = dataSetSizeLimit;
        updateCacheImage();
        propertyChangeSupport.firePropertyChange("dataSetSizeLimit", oldDataSetSizeLimit, dataSetSizeLimit );
    }
    private double updatesPointsPerMillisecond;
    public static final String PROP_UPDATESPOINTSPERMILLISECOND = "updatesPointsPerMillisecond";

    public double getUpdatesPointsPerMillisecond() {
        return this.updatesPointsPerMillisecond;
    }

    public void setUpdatesPointsPerMillisecond(double newupdatesPointsPerMillisecond) {
        //double oldupdatesPointsPerMillisecond = updatesPointsPerMillisecond;
        this.updatesPointsPerMillisecond = newupdatesPointsPerMillisecond;
    //propertyChangeSupport.firePropertyChange(PROP_UPDATESPOINTSPERMILLISECOND, oldupdatesPointsPerMillisecond, newupdatesPointsPerMillisecond);
    }
    private double renderPointsPerMillisecond;
    public static final String PROP_RENDERPOINTSPERMILLISECOND = "renderPointsPerMillisecond";

    public double getRenderPointsPerMillisecond() {
        return this.renderPointsPerMillisecond;
    }

    public void setRenderPointsPerMillisecond(double newrenderPointsPerMillisecond) {
        //double oldrenderPointsPerMillisecond = renderPointsPerMillisecond;
        this.renderPointsPerMillisecond = newrenderPointsPerMillisecond;
    //propertyChangeSupport.firePropertyChange(PROP_RENDERPOINTSPERMILLISECOND, oldrenderPointsPerMillisecond, newrenderPointsPerMillisecond);
    }

    public int getFirstIndex() {
        return this.firstIndex;
    }

    public int getLastIndex() {
        return this.lastIndex;
    }

    protected boolean cadenceCheck = true;
    public static final String PROP_CADENCECHECK = "cadenceCheck";

    public boolean isCadenceCheck() {
        return cadenceCheck;
    }

    /**
     * If true, then use a cadence estimate to determine and indicate data gaps.
     * @param cadenceCheck
     */
    public void setCadenceCheck(boolean cadenceCheck) {
        boolean oldCadenceCheck = this.cadenceCheck;
        this.cadenceCheck = cadenceCheck;
        updateCacheImage();
        propertyChangeSupport.firePropertyChange(PROP_CADENCECHECK, oldCadenceCheck, cadenceCheck);
    }

    @Override
    public boolean acceptsDataSet(QDataSet dataSet) {
        QDataSet ds1= dataSet;
        //QDataSet vds, tds;
        boolean plottable;
        if ( !SemanticOps.isTableDataSet(dataSet) ) { 
            if ( ds1.rank()==2 && SemanticOps.isBundle(ds1) ) {
            //    vds = DataSetOps.unbundleDefaultDataSet( ds );
            } else if ( ds1.rank()!=1 ) {
                logger.fine("dataset rank error");
                return false;
            }  else {
            //    vds = (QDataSet) dataSet;
            }

            unitsWarning= false;
            plottable = true;

        } else { // is table data set
            plottable = true;
        }

        return plottable;
    }


}
