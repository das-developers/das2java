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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.GeneralPath;
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
import org.das2.DasApplication;
import org.das2.DasProperties;
import org.das2.dataset.VectorUtil;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.InconvertibleUnitsException;
import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;
import org.das2.event.CrossHairMouseModule;
import org.das2.event.DasMouseInputAdapter;
import org.das2.event.LengthDragRenderer;
import org.das2.event.MouseModule;
import org.das2.system.DasLogger;
import org.das2.util.LoggerManager;
import org.das2.util.monitor.ProgressMonitor;
import org.virbo.dataset.ArrayDataSet;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.MutablePropertyDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;
import org.virbo.dsops.Ops;
import org.virbo.dsutil.DataSetBuilder;
import org.virbo.dsutil.Reduction;
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
 * @author  jbf
 */
public class SeriesRenderer extends Renderer {

    private DefaultPlotSymbol psym = DefaultPlotSymbol.CIRCLES;
    private float symSize = 3.0f; // radius in pixels

    private float lineWidth = 1.0f; // width in pixels

    private boolean histogram = false;
    private PsymConnector psymConnector = PsymConnector.SOLID;
    private FillStyle fillStyle = FillStyle.STYLE_FILL;
    //private int renderCount = 0;
    //private int updateImageCount = 0;
    private Color color = Color.BLACK;
    private long lastUpdateMillis;
    private boolean antiAliased = "on".equals(DasProperties.getInstance().get("antiAlias"));
    private int firstIndex=-1;/* the index of the first point drawn, nonzero when X is monotonic and we can clip. */
    private int lastIndex=-1;/* the non-inclusive index of the last point drawn. */
    private int firstIndex_v=-1;/* the index of the first point drawn that is visible, nonzero when X is monotonic and we can clip. */
    private int lastIndex_v=-1;/* the non-inclusive index of the last point that is visible. */
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
    Shape selectionArea;

    boolean haveValidColor= true;

    interface RenderElement {

        int render(Graphics2D g, DasAxis xAxis, DasAxis yAxis, QDataSet vds, ProgressMonitor mon);

        void update(DasAxis xAxis, DasAxis yAxis, QDataSet vds, ProgressMonitor mon);

        boolean acceptContext(Point2D.Double dp);
    }

    @Override
    public void setControl(String s) {
        super.setControl(s);
        setColor( getColorControl( CONTROL_KEY_COLOR, color ) );
        setFillColor( getColorControl( CONTROL_KEY_FILL_COLOR, fillColor ));
        setLineWidth( getDoubleControl( CONTROL_KEY_LINE_THICK, lineWidth ) );
        setSymSize( getDoubleControl( CONTROL_KEY_SYMBOL_SIZE, symSize ) );
    }

    @Override
    public String getControl() {
        Map<String,String> controls= new LinkedHashMap();
        controls.put( CONTROL_KEY_COLOR, setColorControl(color) );
        controls.put( CONTROL_KEY_FILL_COLOR, setColorControl(fillColor) );
        controls.put( CONTROL_KEY_LINE_THICK, String.valueOf(lineWidth) );
        controls.put( CONTROL_KEY_SYMBOL_SIZE, String.valueOf( symSize ) );
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

        double[] dpsymsPath; // store the location of the psyms here.

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

            QDataSet colorByDataSet=null;
            if ( colorByDataSetId != null && !colorByDataSetId.equals("")) {
                colorByDataSet = colorByDataSet( ds );
            }

            if (colorByDataSet != null) {
                for (int i = 0; i < count; i++) {
                    int icolor = colors[i];
                    if ( icolor>=0 ) {
                        g.drawImage(coloredPsyms[icolor], (int)dpsymsPath[i * 2] - cmx, (int)dpsymsPath[i * 2 + 1] - cmy, lparent);
                    }
                }
            } else {
                try {
                    for (int i = 0; i < count; i++) {
                        g.drawImage(psymImage, (int)dpsymsPath[i * 2] - cmx, (int)dpsymsPath[i * 2 + 1] - cmy, lparent);
                    }
                } catch ( ArrayIndexOutOfBoundsException ex ) {
                    logger.log( Level.WARNING, ex.getMessage(), ex );
                }
            }

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

            Color[] ccolors = null;
            if (colorByDataSet != null) {
                IndexColorModel icm = colorBar.getIndexColorModel();
                ccolors = new Color[icm.getMapSize()];
                for (int j = 0; j < icm.getMapSize(); j++) {
                    ccolors[j] = new Color(icm.getRGB(j));
                }
            }

            if (colorByDataSet != null) {
                for (int i = 0; i < count; i++) {
                    if ( colors[i]>=0 ) {
                        graphics.setColor(ccolors[colors[i]]);
                        psym.draw(graphics, dpsymsPath[i * 2], dpsymsPath[i * 2 + 1], fsymSize, fillStyle);
                    }
                }

            } else {
                for (int i = 0; i < count; i++) {
//                    psym.draw(graphics, ipsymsPath[i * 2], ipsymsPath[i * 2 + 1], fsymSize, fillStyle);
                    try {
                        psym.draw(graphics, dpsymsPath[i * 2], dpsymsPath[i * 2 + 1], fsymSize, fillStyle);
                    } catch ( ArrayIndexOutOfBoundsException ex ) {
                        logger.log( Level.WARNING, ex.getMessage(), ex );
                    }
                }
            }

            return count;

        }

        /**
         *
         * @param graphics
         * @param xAxis
         * @param yAxis
         * @param vds
         * @param mon
         * @return the number of points drawn, though possibly off screen.
         */
        @Override
        public synchronized int render(Graphics2D graphics, DasAxis xAxis, DasAxis yAxis, QDataSet vds, ProgressMonitor mon) {
            int i;
            if ( vds.rank()!=1 && !SemanticOps.isBundle(vds) ) {
                renderException( graphics, xAxis, yAxis, new IllegalArgumentException("dataset is not rank 1"));
            }
            DasPlot lparent= getParent();
            if ( lparent==null ) return 0;
            if (stampPsyms && !lparent.getCanvas().isPrintingThread()) {
                i = renderStamp(graphics, xAxis, yAxis, vds, mon);
            } else {
                i = renderDraw(graphics, xAxis, yAxis, vds, mon);
            }

            if ( haveValidColor==false ) {
                lparent.postMessage( SeriesRenderer.this, "no valid data to color plot symbols", Level.INFO, null, null );
            }

            return i;
        }

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
            
            Units cunits = null;
            if (colorByDataSet1 != null && fcolorBar!=null ) {
                cunits = SemanticOps.getUnits( colorByDataSet1 );
                if ( cunits.isConvertableTo(fcolorBar.getUnits()) ) {
                    cunits= fcolorBar.getUnits();
                }
            }

            double x, y;            
            double dx,dy;

            count= 0; // intermediate state
            dpsymsPath = new double[(lastIndex - firstIndex ) * 2];
            colors = new int[lastIndex - firstIndex + 2];

            int index = firstIndex;

            QDataSet vds;

            QDataSet xds = SemanticOps.xtagsDataSet(dataSet);
            vds= ytagsDataSet(dataSet);

            if ( xds.rank()==2 && xds.property( QDataSet.BINS_1 )!=null ) {
                xds= Ops.reduceMean(xds,1);
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
                    dpsymsPath[i * 2] = dx;
                    dpsymsPath[i * 2 + 1] = dy;
                    if ( wdsz!=null && colorByDataSet1 != null && fcolorBar!=null ) {
                        try {
                            if ( wdsz.value(index)>0 ) {
                                haveValidColor= true;
                                colors[i] = fcolorBar.indexColorTransform( colorByDataSet1.value(index), cunits);
                            } else {
                                colors[i] = -1;
                            }
                        } catch ( NullPointerException ex ) {
                            //System.err.println("here391");
                            logger.log( Level.WARNING, ex.getMessage(), ex );
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
            
            double[] p;
            
            //local copy for thread safety
            synchronized (this) {
                p= dpsymsPath;
            }
            
            if ( p == null ) {
                return false;
            }
            double rad = Math.max(symSize, 5);

            int np= p.length/2;
            for (int index = 0; index < np; index++) {
                int i = index;
                if (dp.distance(p[i * 2], p[i * 2 + 1]) < rad) {
                    return true;
                }
            }
            return false;
        }
    }

    private class ErrorBarRenderElement implements RenderElement {

        GeneralPath p;

        @Override
        public int render(Graphics2D g, DasAxis xAxis, DasAxis yAxis, QDataSet vds, ProgressMonitor mon) {
            GeneralPath lp= getPath();
            if (lp == null) {
                return 0;
            }
            g.draw(lp);
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

            QDataSet deltaPlusY = (QDataSet) vds.property( QDataSet.DELTA_PLUS );
            QDataSet deltaMinusY = (QDataSet) vds.property( QDataSet.DELTA_MINUS );

            GeneralPath lp;

            if (deltaPlusY == null) {
                p= null;
                return;
            }
            if (deltaMinusY == null) {
                p= null;
                return;
            }

            QDataSet up= Ops.add( vds, deltaPlusY );
            QDataSet dn= Ops.subtract( vds, deltaMinusY );
            
            QDataSet wup= Ops.valid(up);
            QDataSet wdn= Ops.valid(dn);
            
            QDataSet xds= SemanticOps.xtagsDataSet(dataSet);

            Units xunits = SemanticOps.getUnits(xds);
            Units yunits = SemanticOps.getUnits(vds);
            
            if ( unitsWarning ) yunits= yAxis.getUnits();
            if ( xunitsWarning ) xunits= xAxis.getUnits();

            lp = new GeneralPath();
            for (int i = firstIndex; i < lastIndex; i++) {
                double ix = xAxis.transform( xds.value(i), xunits );
                if ( wup.value(i)>0 && wdn.value(i)>0 ) {
                    double iym = yAxis.transform( dn.value(i), yunits );
                    double iyp = yAxis.transform( up.value(i), yunits );
                    lp.moveTo(ix, iym);
                    lp.lineTo(ix, iyp);
                }
            }
            
            p= lp;

        }

        @Override
        public boolean acceptContext(Point2D.Double dp) {
            GeneralPath gp= getPath();
            return gp != null && gp.contains(dp.x - 2, dp.y - 2, 5, 5);

        }
    }

    /**
     * isolate allow bad units logic.
     * @param d the datum.
     * @param u target units.
     * @return the double value.
     */
    private double doubleValue( Datum d, Units u ) {
        if ( d.getUnits().isConvertableTo(u) ) {
            return d.doubleValue(u);
        } else {
            try {
                return d.value();
            } catch ( IllegalArgumentException ex ) {
                throw new InconvertibleUnitsException(d.getUnits(),u);
            }
        }
    }

        private static void dumpPath( int width, int height, GeneralPath path1 ) {
            if ( path1!=null ) {
                try {
                    java.io.BufferedWriter w= new java.io.BufferedWriter( new java.io.FileWriter("/tmp/foo.jy") );
                    w.write( "from java.awt.geom import GeneralPath\nlp= GeneralPath()\n");
                    w.write( "h= " + height + "\n" );
                    w.write( "w= " + width + "\n" );
                    PathIterator it= path1.getPathIterator(null);
                    float [] seg= new float[6];
                    while ( !it.isDone() ) {
                        int r= it.currentSegment(seg);
                        if ( r==PathIterator.SEG_MOVETO ) {
                            w.write( String.format( "lp.moveTo( %9.2f, %9.4f )\n", seg[0], seg[1] ) );
                        } else {
                            w.write( String.format( "lp.lineTo( %9.2f, %9.4f )\n", seg[0], seg[1] ) );
                        }
                        it.next();
                    }
                    w.write( "from javax.swing import JPanel, JOptionPane\n" +
                            "from java.awt import Dimension, RenderingHints, Color\n" +
                            "\n" +
                            "class MyPanel( JPanel ):\n" +
                            "   def paintComponent( self, g ):\n" +
                            "       g.setColor(Color.WHITE)\n" +
                            "       g.fillRect(0,0,w,h)\n" + 
                            "       g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON )\n" +
                            "       g.setColor(Color.BLACK)\n" +
                            "       g.draw( lp )\n" +
                            "\n" +
                            "p= MyPanel()\n" +
                            "p.setMinimumSize( Dimension( w,h ) )\n" +
                            "p.setPreferredSize( Dimension( w,h ) )\n" +
                            "JOptionPane.showMessageDialog( None, p )" );
                    w.close();
                } catch ( java.io.IOException ex ) {
                    ex.printStackTrace();
                }
            }
        }     
    
    private class PsymConnectorRenderElement implements RenderElement {

        private GeneralPath path1;
   
        @Override
        public int render(Graphics2D g, DasAxis xAxis, DasAxis yAxis, QDataSet vds, ProgressMonitor mon) {
            logger.log(Level.FINE, "enter connector render" );
            if ( vds.rank()!=1 && !SemanticOps.isRank2Waveform(vds) ) {
                renderException( g, xAxis, yAxis, new IllegalArgumentException("dataset is not rank 1"));
            }
            GeneralPath lpath1= getPath();
            if (lpath1 == null) {
                return 0;
            }
            //dumpPath();
            psymConnector.draw(g, lpath1, (float)lineWidth);
            return 0;
        }

        private synchronized GeneralPath getPath() {
            return path1;
        }
        
        @Override
        public synchronized void update(DasAxis xAxis, DasAxis yAxis, QDataSet dataSet, ProgressMonitor mon) {
            logger.log(Level.FINE, "enter connector update" );
            QDataSet xds= SemanticOps.xtagsDataSet( dataSet );
            if ( xds.rank()==2 && xds.property( QDataSet.BINS_1 )!=null ) {
                xds= Ops.reduceMean(xds,1);
            }

            QDataSet vds= ytagsDataSet( dataSet );
            
            QDataSet wds= SemanticOps.weightsDataSet( vds );

            Units xUnits = SemanticOps.getUnits(xds);
            Units yUnits = SemanticOps.getUnits(vds);
            if ( unitsWarning ) yUnits= yAxis.getUnits();
            if ( xunitsWarning ) xUnits= yAxis.getUnits();

            Rectangle window= DasDevicePosition.toRectangle( yAxis.getRow(), xAxis.getColumn() );
            int buffer= (int)Math.ceil( Math.max( getLineWidth(),10 ) );

            DasPlot lparent= getParent();
            if ( lparent==null ) return;
            if ( lparent.isOverSize() ) {
                window= new Rectangle( window.x- window.width/3, window.y-buffer, 5 * window.width / 3, window.height + 2 * buffer );
                //TODO: there's a rectangle somewhere that is the preveiw.  Use this instead of assuming 1/3 on either side.
            } else {
                window= new Rectangle( window.x- buffer, window.y-buffer, window.width + 2*buffer, window.height + 2 * buffer );
            }

            if ( lastIndex-firstIndex==0 ) {
                this.path1= null;
                return;
            }

            long t0= System.currentTimeMillis();
            
            int pathLengthApprox= Math.max( 5, 110 * (lastIndex - firstIndex) / 100 );
            GeneralPath newPath = new GeneralPath(GeneralPath.WIND_NON_ZERO, pathLengthApprox );

            Datum sw = SemanticOps.guessXTagWidth( xds.trim(firstIndex,lastIndex), vds.trim(firstIndex,lastIndex) ); // TODO: this really shouldn't be here, since we calculate it once.
            double xSampleWidth;
            boolean logStep;
            if ( sw!=null) {
                if ( UnitsUtil.isRatiometric(sw.getUnits()) ) {
                    xSampleWidth = sw.doubleValue(Units.logERatio);
                    logStep= true;
                } else {
                    xSampleWidth = doubleValue( sw, xUnits.getOffsetUnits() );
                    logStep= false;
                }
            } else {
                xSampleWidth= 1e37; // something really big
                logStep= false;
            }


            /* fuzz the xSampleWidth */
            double xSampleWidthExact= xSampleWidth;
            xSampleWidth = xSampleWidth * 1.20;

            double x;
            double y;

            double x0; /* the last plottable point */
            //double y0; /* the last plottable point */

            double fx;
            double fy;
            double fx0;
            double fy0;
            boolean visible;  // true if this point can be seen
            boolean visible0; // true if the last point can be seen
            
            int index;

            index = firstIndex;
            x = (double) xds.value(index);
            y = (double) vds.value(index);

            // first point //
            logger.log(Level.FINE, "firstPoint moveTo,LineTo= {0},{1}", new Object[]{x, y});
            try {
                fx = xAxis.transform(x, xUnits);
            } catch ( InconvertibleUnitsException ex ) {
                xunitsWarning= true;
                return;
            }
            try {
                fy = yAxis.transform(y, yUnits);
            } catch ( InconvertibleUnitsException ex ) {
                unitsWarning= true;
                return;
            }

            visible0= window.contains(fx,fy);
            if (histogram) {
                double fx1 = midPoint( xAxis, x, xUnits, xSampleWidthExact, logStep, -0.5 );
                newPath.moveTo(fx1, fy);
                newPath.lineTo(fx, fy);
            } else {
                newPath.moveTo(fx, fy);
                newPath.lineTo(fx, fy);
            }

            x0 = x;
            //y0 = y;
            fx0 = fx;
            fy0 = fy;

            index++;

            // now loop through all of them. //
            boolean ignoreCadence= ! cadenceCheck;
            boolean isValid= false;

            for (; index < lastIndex; index++) {

                x = xds.value(index);
                y = vds.value(index);

                isValid = wds.value( index )>0 && xUnits.isValid(x);

                fx = xAxis.transform(x, xUnits);
                fy = yAxis.transform(y, yUnits);
                visible= isValid && window.intersectsLine( fx0,fy0, fx,fy );
                
                if (isValid) {
                    double step= logStep ? Math.log(x/x0) : x-x0;
                    if ( ignoreCadence || step < xSampleWidth) {
                        // draw connect-a-dot between last valid and here
                        if (histogram) {
                            double fx1 = (fx0 + fx) / 2;
                            newPath.lineTo(fx1, fy0);
                            newPath.lineTo(fx1, fy);
                            newPath.lineTo(fx, fy);
                        } else {
                            if ( visible ) {
                                if ( !visible0 ) {
                                    newPath.moveTo(fx0,fy0);
                                }
                                newPath.lineTo(fx, fy); // this is the typical path
                            }

                        }

                    } else {
                        // introduce break in line
                        if (histogram) {
                            double fx1 = xAxis.transform(x0 + xSampleWidthExact / 2, xUnits);
                            newPath.lineTo(fx1, fy0);
                            // there's a bug here if you pan around the dataset shown in SeriesBreakHist.java
                            fx1 = xAxis.transform(x - xSampleWidthExact / 2, xUnits);
                            newPath.moveTo(fx1, fy);
                            newPath.lineTo(fx, fy);

                        } else {
                            if ( visible ) {
                                newPath.moveTo(fx, fy);
                                newPath.lineTo(fx, fy);
                            }
                        }

                    } // else introduce break in line

                    x0 = x;
                    //y0 = y;
                    fx0 = fx;
                    fy0 = fy;
                    visible0 = visible;

                } else {
                    if (visible0) {
                        if ( histogram ) {
                            double fx1 = midPoint( xAxis, x0, xUnits, xSampleWidthExact, logStep, 0.5 );
                            newPath.lineTo(fx1, fy0);
                        } else {
                            newPath.moveTo(fx0, fy0); // place holder
                        }
                    }

                }

            } // for ( ; index < ixmax && lastIndex; index++ )

            if ( histogram ) {
                if ( isValid ) {
                    fx = xAxis.transform( x + xSampleWidthExact / 2, xUnits );
                    newPath.lineTo(fx, fy0);
                }
            }
            
            logger.fine( String.format("time to create general path (ms): "+ ( System.currentTimeMillis()-t0  ) ) );
            
            if (!histogram && simplifyPaths && colorByDataSetId.length()==0 ) {
                //j   System.err.println( "input: " );
                //j   System.err.println( GraphUtil.describe( newPath, true) );
                this.path1= new GeneralPath(GeneralPath.WIND_NON_ZERO, pathLengthApprox );
                //int count = GraphUtil.reducePath(newPath.getPathIterator(null), path1 );
                int count = GraphUtil.reducePath20140622(newPath.getPathIterator(null), path1, 1, 5 );
                logger.fine( String.format("reduce path in=%d  out=%d\n", lastIndex-firstIndex, count) );
            } else {
                this.path1 = newPath;
            }

            //dumpPath( getParent().getCanvas().getWidth(), getParent().getCanvas().getHeight(), path1 );  // dumps jython script showing problem.
            //GraphUtil.describe( path1, true );
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

    private double midPoint(DasAxis axis, double d1, Units units, double delta, boolean ratiometric, double alpha ) {
        double fx1;
        if (axis.isLog() && ratiometric ) {
            fx1 = (double) axis.transform( Math.exp( Math.log(d1) + delta * alpha ), units);
        } else {
            fx1 = (double) axis.transform( d1 + delta * alpha, units);
        }
        return fx1;
    }

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
//                write.printf( "%9d ", type );
//                if ( type==PathIterator.SEG_MOVETO) {
//                    for ( int i=0; i<6; i++ ) {
//                        write.printf( "%9.1f ", -99999.  );
//                    }
//                    write.println();
//                    write.printf( "%9d ", type );
//                }
//                for ( int i=0; i<6; i++ ) {
//                    write.printf( "%9.1f ", coords[i] );
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
        public int render(Graphics2D g, DasAxis xAxis, DasAxis yAxis, QDataSet vds, ProgressMonitor mon) {
            if ( fillToRefPath1==null ) {
                return 0;
            }
            //PathIterator it= fillToRefPath1.getPathIterator(null);
            //dumpPath(it);
                    
            g.setColor(fillColor);
            g.fill(fillToRefPath1);
            return 0;
        }

        @Override
        public void update(DasAxis xAxis, DasAxis yAxis, QDataSet dataSet, ProgressMonitor mon) {

            if ( lastIndex-firstIndex==0 ) {
                this.fillToRefPath1= null;
                return;
            }

            QDataSet xds = SemanticOps.xtagsDataSet(dataSet);
            QDataSet vds;
            vds = ytagsDataSet( ds );

            Units xUnits = SemanticOps.getUnits(xds);
            Units yUnits = SemanticOps.getUnits(vds);
            if ( unitsWarning ) yUnits= yAxis.getUnits();
            if ( xunitsWarning ) xUnits= xAxis.getUnits();

            QDataSet wds= SemanticOps.weightsDataSet( vds );

            int pathLengthApprox= Math.max( 5, 110 * (lastIndex - firstIndex) / 100 );
            GeneralPath fillPath = new GeneralPath(GeneralPath.WIND_NON_ZERO, pathLengthApprox );

            Datum sw = SemanticOps.guessXTagWidth( xds, dataSet.trim(firstIndex,lastIndex) );
            double xSampleWidth;
            boolean logStep;
            if ( sw!=null ) {
                if ( UnitsUtil.isRatiometric(sw.getUnits()) ) {
                    xSampleWidth = sw.doubleValue(Units.logERatio);
                    logStep= true;
                } else {
                    xSampleWidth = doubleValue( sw, xUnits.getOffsetUnits() );
                    logStep= false;
                }
            } else {
                xSampleWidth= 1e37;
                logStep= false;
            }


            /* fuzz the xSampleWidth */
            double xSampleWidthExact= xSampleWidth;
            xSampleWidth = xSampleWidth * 1.20;

            if (reference == null) {
                reference = yUnits.createDatum(yAxis.isLog() ? 1.0 : 0.0);
            }

            double yref = doubleValue( reference, yUnits );

            double x;
            double y;

            double x0; /* the last plottable point */
            //double y0; /* the last plottable point */

            double fyref = yAxis.transform(yref, yUnits);
            double fx;
            double fy;
            double fx0;
            double fy0;

            int index;

            index = firstIndex;
            x = xds.value(index);
            y = vds.value(index);

            // first point //
            fx = xAxis.transform(x, xUnits);
            fy = yAxis.transform(y, yUnits);
            if (histogram) {
                double fx1;
                fx1= midPoint( xAxis, x, xUnits, xSampleWidthExact, logStep, -0.5 );
                fillPath.moveTo(fx1-1, fyref); // doesn't line up, -1 is fudge
                fillPath.lineTo(fx1-1, fy);
                fillPath.lineTo(fx, fy);

            } else {
                fillPath.moveTo(fx, fyref);
                fillPath.lineTo(fx, fy);

            }

            x0 = x;
            //y0 = y;
            fx0 = fx;
            fy0 = fy;

            if (psymConnector != PsymConnector.NONE || fillToReference) {
                // now loop through all of them. //
                boolean ignoreCadence= ! cadenceCheck;
                for (; index < lastIndex; index++) {

                    x = xds.value(index);
                    y = vds.value(index);

                    final boolean isValid = wds.value( index )>0 && xUnits.isValid(x);

                    fx = xAxis.transform(x, xUnits);
                    fy = yAxis.transform(y, yUnits);

                    if (isValid) {
                        double step= logStep ? Math.log(x/x0) : x-x0;
                        if ( ignoreCadence || step < xSampleWidth) {
                            // draw connect-a-dot between last valid and here
                            if (histogram) {
                                double fx1 = (fx0 + fx) / 2; //sloppy with ratiometric spacing
                                fillPath.lineTo(fx1, fy0);
                                fillPath.lineTo(fx1, fy);
                                fillPath.lineTo(fx, fy);
                            } else {
                                fillPath.lineTo(fx, fy);
                            }

                        } else {
                            // introduce break in line
                            if (histogram) {
                                double fx1 = midPoint( xAxis, x0, xUnits, xSampleWidthExact, logStep, 0.5 );
                                fillPath.lineTo(fx1, fy0);
                                fillPath.lineTo(fx1, fyref);
                                fx1 = midPoint( xAxis, x, xUnits, xSampleWidthExact, logStep, -0.5 );
                                fillPath.moveTo(fx1, fyref);
                                fillPath.lineTo(fx1, fy);
                                fillPath.lineTo(fx, fy);

                            } else {
                                fillPath.lineTo(fx0, fyref);
                                fillPath.moveTo(fx, fyref);
                                fillPath.lineTo(fx, fy);
                            }

                        } // else introduce break in line

                        x0 = x;
                        //y0 = y;
                        fx0 = fx;
                        fy0 = fy;

                    }

                } // for ( ; index < ixmax && lastIndex; index++ )

            }

            if ( histogram ) {
                double fx1 = midPoint( xAxis, x0, xUnits, xSampleWidthExact, logStep, 0.5 );
                fillPath.lineTo(fx1, fy0);
                fillPath.lineTo(fx1, fyref);
            } else {
                fillPath.lineTo(fx0, fyref);
            }

            this.fillToRefPath1 = fillPath;

            if (simplifyPaths) {
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

    /**
     * updates the image of a psym that is stamped
     */
    private void updatePsym() {
        if ( !isActive() ) return;
        
        int sx = 6+(int) Math.ceil(symSize + 2 * lineWidth);
        int sy = 6+(int) Math.ceil(symSize + 2 * lineWidth);
        double dcmx, dcmy;
        dcmx = (lineWidth + (int) Math.ceil( ( symSize ) / 2)) +2 ;
        dcmy = (lineWidth + (int) Math.ceil( ( symSize ) / 2)) +2 ;
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
        
        int ixmax;
        int ixmin;
                
        QDataSet yds= dataSet;
        
        if ( xds.length()!=yds.length() ) {
            logger.fine("xds and yds have different lengths.  Assuming transitional case.");
            return;
            //throw new IllegalArgumentException("xds and yds are different lengths.");
        }
        
        if ( yds.rank()==2 ) {
            yds= DataSetOps.slice1( yds, 0 );
        }

        if ( xds.rank()==2 ) {
            xds= DataSetOps.slice1(xds,0); //BINS dataset
        }

        QDataSet wxds= SemanticOps.weightsDataSet( xds );

        QDataSet wds;
        wds= SemanticOps.weightsDataSet( yds );

        DasPlot lparent= getParent();
        if ( lparent==null ) return;

        dslen= xds.length();
        if ( SemanticOps.isMonotonic( xds )) {
            DatumRange visibleRange = xAxis.getDatumRange();
            Units xdsu= SemanticOps.getUnits(xds);
            if ( !visibleRange.getUnits().isConvertableTo( xdsu ) ) {
                visibleRange= new DatumRange( visibleRange.min().doubleValue(visibleRange.getUnits()), visibleRange.max().doubleValue(visibleRange.getUnits()), xdsu );
            }
            firstIndex_v = DataSetUtil.getPreviousIndex( xds, visibleRange.min());
            lastIndex_v = DataSetUtil.getNextIndex( xds, visibleRange.max()) + 1; // +1 is for exclusive.
            if (lparent.isOverSize()) {
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
        
        logger.log( Level.FINE, "ds: {0},  firstIndex={1} to lastIndex={2}", new Object[]{ String.valueOf(this.ds), this.firstIndex, this.lastIndex});
    }

    @Override
    public void setActive( boolean active ) {
        super.setActive(active);
        if ( active ) updatePsym();
    }

    /**
     * render the dataset by delegating to internal components such as the symbol connector and error bars.
     * @param g the graphics context.
     * @param xAxis the x axis
     * @param yAxis the y axis
     * @param mon a progress monitor
     */
    @Override
    public synchronized void render(Graphics g, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {

        DasPlot lparent= getParent();

        logger.log(Level.FINE, "enter {0}.render: {1}", new Object[]{id, String.valueOf(getDataSet()) });
        logger.log( Level.FINER, "ds: {0},  drawing indeces {1} to {2}", new Object[]{ String.valueOf(this.ds), this.firstIndex, this.lastIndex});
        
        if ( lparent==null ) return;
        if ( this.ds == null && lastException != null) {
            lparent.postException(this, lastException);
            return;
        }

        //study 20130214.  Why does http://sarahandjeremy.net/~jbf/geothermal20130116.vap sometimes fail to repaint?
        //if ( ds!=null ) {
        //    QDataSet xds1= SemanticOps.xtagsDataSet(ds);
        //    if ( xds1!=null ) {
        //        QDataSet xrange= Ops.extent(xds1);
        //        logger.log( Level.FINE, "{0} {1},  drawing de indeces {2} to {3}", new Object[]{ color, String.valueOf( xrange ), this.firstIndex, this.lastIndex});
        //        //end study 20130214.
        //    }
        //}

        //renderCount++;
        //reportCount();

        long timer0 = System.currentTimeMillis();

        QDataSet dataSet = getDataSet();

        if (dataSet == null) {
            DasLogger.getLogger(DasLogger.GRAPHICS_LOG).fine("null data set");
            lparent.postMessage(this, "no data set", DasPlot.INFO, null, null);
            return;
        }

        if (dataSet.rank() == 0) {
            DasLogger.getLogger(DasLogger.GRAPHICS_LOG).fine("rank 0 data set");
            lparent.postMessage(this, "rank 0 data set: "+dataSet.toString(), DasPlot.INFO, null, null);
            return;
        }

        if (dataSet.length() == 0) {
            DasLogger.getLogger(DasLogger.GRAPHICS_LOG).fine("empty data set");
            lparent.postMessage(this, "empty data set", DasPlot.INFO, null, null);
            return;
        }
        
        if ( dataSet.rank()!=1 && ! ( SemanticOps.isBundle(ds) || SemanticOps.isRank2Waveform(ds) ) ) {
            DasLogger.getLogger(DasLogger.GRAPHICS_LOG).fine("dataset is not rank 1 or a rank 2 waveform");
            lparent.postMessage(this, "dataset is not rank 1 or a rank 2 waveform", DasPlot.INFO, null, null);
            return;
        }

        if ( psym== DefaultPlotSymbol.NONE && psymConnector==PsymConnector.NONE ) {
            DasLogger.getLogger(DasLogger.GRAPHICS_LOG).fine("plot symbol and symbol connector are set to none");
            lparent.postMessage(this, "plot symbol and symbol connector are set to none", DasPlot.INFO, null, null);
            return;
        }

        QDataSet tds = null;
        QDataSet vds = null;
        boolean yaxisUnitsOkay;
        boolean xaxisUnitsOkay;

        QDataSet xds = SemanticOps.xtagsDataSet(dataSet);
        xaxisUnitsOkay = SemanticOps.getUnits(xds).isConvertableTo(xAxis.getUnits() );
        if ( !SemanticOps.isTableDataSet(dataSet) ) {
            vds= ytagsDataSet(ds);
            yaxisUnitsOkay = SemanticOps.getUnits(vds).isConvertableTo(yAxis.getUnits()); // Ha!  QDataSet makes the code the same

        } else {
            tds = (QDataSet) dataSet;
            yaxisUnitsOkay = SemanticOps.getUnits(tds).isConvertableTo(yAxis.getUnits());
        }

        boolean haveReportedUnitProblem= false;
        if ( !xaxisUnitsOkay && !yaxisUnitsOkay && ( vds!=null && SemanticOps.getUnits(xds)==SemanticOps.getUnits(vds) ) && xAxis.getUnits()==yAxis.getUnits() ) {
             if ( unitsWarning ) {
                //UnitsUtil.isRatioMeasurement( SemanticOps.getUnits(vds) ) && UnitsUtil.isRatioMeasurement( yAxis.getUnits() )
                lparent.postMessage( this, "axis units changed from \""+SemanticOps.getUnits(vds) + "\" to \"" + yAxis.getUnits() + "\"", DasPlot.INFO, null, null );
                haveReportedUnitProblem= true;
            } else {
                lparent.postMessage( this, "inconvertible axis units", DasPlot.INFO, null, null );
                return;
            }
        }

        if ( !haveReportedUnitProblem && !yaxisUnitsOkay ) {
            if ( unitsWarning ) {
                //UnitsUtil.isRatioMeasurement( SemanticOps.getUnits(vds) ) && UnitsUtil.isRatioMeasurement( yAxis.getUnits() )
                if ( vds!=null ) { // don't bother with the other mode.
                    lparent.postMessage( this, "yaxis units changed from \""+SemanticOps.getUnits(vds) + "\" to \"" + yAxis.getUnits() + "\"", DasPlot.INFO, null, null );
                }
            } else {
                lparent.postMessage( this, "inconvertible yaxis units", DasPlot.INFO, null, null );
                return;
            }
        }

        if ( !haveReportedUnitProblem && !xaxisUnitsOkay ) {
            if ( xunitsWarning ) {
                //UnitsUtil.isRatioMeasurement( SemanticOps.getUnits(vds) ) && UnitsUtil.isRatioMeasurement( yAxis.getUnits() )
                lparent.postMessage( this, "xaxis units changed from \""+SemanticOps.getUnits(xds) + "\" to \"" + xAxis.getUnits() + "\"", DasPlot.INFO, null, null );
            } else {
                lparent.postMessage( this, "inconvertible xaxis units", DasPlot.INFO, null, null );
                return;
            }
        }

        int messageCount= 0;

        logger.log(Level.FINER, "rendering points: {0}  {1}", new Object[]{lastIndex, firstIndex});
        if ( lastIndex == -1 ) {
            if ( messageCount++==0) {
                lparent.postMessage(SeriesRenderer.this, "need to update first/last", DasPlot.INFO, null, null);
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

        if (lastIndex == firstIndex) {
            if ( firstValidIndex==lastValidIndex ) {
                if ( messageCount++==0) lparent.postMessage(SeriesRenderer.this, "dataset contains no valid data", DasPlot.INFO, null, null);
            }
        }

        Graphics2D graphics = (Graphics2D) g.create();

        if (antiAliased) {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        } else {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        }
        
        mon.started();
        if (tds != null) {
            graphics.setColor(color);
            logger.log(Level.FINEST, "drawing psymConnector in {0}", color);

            int connectCount= psymConnectorElement.render(graphics, xAxis, yAxis, tds, mon.getSubtaskMonitor("psymConnectorElement.render")); // tds is only to check units

            logger.log(Level.FINEST, "connectCount: {0}", connectCount);
            errorElement.render(graphics, xAxis, yAxis, tds, mon.getSubtaskMonitor("errorElement.render"));

        } else {

            if (this.fillToReference) {
                fillElement.render(graphics, xAxis, yAxis, vds, mon.getSubtaskMonitor("fillElement.render"));
            }

            graphics.setColor(color);
            logger.log(Level.FINEST, "drawing psymConnector in {0}", color);

            int connectCount= psymConnectorElement.render(graphics, xAxis, yAxis, vds, mon.getSubtaskMonitor("psymConnectorElement.render")); // vds is only to check units
            logger.log(Level.FINEST, "connectCount: {0}", connectCount);
            errorElement.render(graphics, xAxis, yAxis, vds, mon.getSubtaskMonitor("errorElement.render"));

            int symCount;
            if (psym != DefaultPlotSymbol.NONE) {

                symCount= psymsElement.render(graphics, xAxis, yAxis, vds, mon.getSubtaskMonitor("psymsElement.render"));
                logger.log(Level.FINEST, "symCount: {0}", symCount);
                
            }
        }
        mon.finished();
        
        long milli = System.currentTimeMillis();
        long renderTime = (milli - timer0);
        double dppms = (lastIndex - firstIndex) / (double) renderTime;

        setRenderPointsPerMillisecond(dppms);

        logger.log(Level.FINER, "render: {0}ms total:{1} fps:{2} pts/ms:{3}", new Object[]{renderTime, milli - lastUpdateMillis, 1000. / (milli - lastUpdateMillis), dppms});
        lastUpdateMillis = milli;

        int ldataSetSizeLimit= getDataSetSizeLimit();
        
        if (dataSetClipped) {
            lparent.postMessage(this, "dataset clipped at " + ldataSetSizeLimit + " points", DasPlot.WARNING, null, null);
        }

        if (dataSetReduced) {
            lparent.postMessage(this, "dataset reduced because of size > " + ldataSetSizeLimit + " points", DasPlot.WARNING, null, null);
        }

        if ( ( lastIndex_v - firstIndex_v < 2 ) && dataSet.length()>1 ) { //TODO: single point would be helpful for digitizing.
            if ( messageCount++==0) {
                if ( lastIndex_v<2 ) {
                    lparent.postMessage(this, "data starts after range", DasPlot.INFO, null, null);
                } else if ( this.dslen - this.firstIndex_v < 2 ) {
                    lparent.postMessage(this, "data ends before range", DasPlot.INFO, null, null);
                } else {
                    lparent.postMessage(this, "fewer than two points visible", DasPlot.INFO, null, null);
                }
            }
        }

        graphics.dispose();
    }

    /**
     * reduce the dataset by coalescing points in the same location.
     * @param xAxis the current xaxis
     * @param yAxis the current yaxis
     * @param vds the dataset
     * @xlimit limit the resolution of the result to so many pixels
     * @ylimit limit the resolution of the result to so many pixels
     * @return reduced version of the dataset
     */
    private QDataSet doDataSetReduce( DasAxis xAxis, DasAxis yAxis, QDataSet vds, int xlimit, int ylimit ) {
        DatumRange xdr= xAxis.getDatumRange();
        QDataSet xxx= xAxis.isLog() ? Ops.exp10( Ops.linspace( Math.log10( xdr.min().doubleValue(xdr.getUnits()) ), Math.log10( xdr.max().doubleValue(xdr.getUnits()) ), xAxis.getDLength() ) ) :
                Ops.linspace( xdr.min().doubleValue(xdr.getUnits()), xdr.max().doubleValue(xdr.getUnits() ), Math.max( 2, xAxis.getDLength()/xlimit ) );
        MutablePropertyDataSet mxxx= DataSetOps.makePropertiesMutable(xxx);  // it is already
        mxxx.putProperty( QDataSet.UNITS, xdr.getUnits() );
        if ( xAxis.isLog() ) {
            mxxx.putProperty( QDataSet.SCALE_TYPE, QDataSet.VALUE_SCALE_TYPE_LOG ); //TODO: cheat
        }
        DatumRange ydr= yAxis.getDatumRange();
        QDataSet yyy= yAxis.isLog() ? Ops.exp10( Ops.linspace( Math.log10( ydr.min().doubleValue(ydr.getUnits()) ), Math.log10( ydr.max().doubleValue(ydr.getUnits()) ), yAxis.getDLength() ) ) :
                Ops.linspace( ydr.min().doubleValue(ydr.getUnits()), ydr.max().doubleValue(ydr.getUnits() ), Math.max( 2, yAxis.getDLength()/ylimit ) );
        MutablePropertyDataSet myyy= DataSetOps.makePropertiesMutable(yyy);  // it is already
        myyy.putProperty( QDataSet.UNITS, ydr.getUnits() );
        if ( yAxis.isLog() ) {
            myyy.putProperty( QDataSet.SCALE_TYPE, QDataSet.VALUE_SCALE_TYPE_LOG ); //TODO: cheat
        }
        long tt0= System.currentTimeMillis();
        QDataSet hds= Reduction.histogram2D( vds, mxxx, myyy );
        logger.log( Level.FINEST, "histogram2D: {0}", ( System.currentTimeMillis()-tt0 ));
        DataSetBuilder buildx= new DataSetBuilder(1,100);
        DataSetBuilder buildy= new DataSetBuilder(1,100);
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
        // logger.log( Level.FINEST, "build new 1-D dataset: {0}", ( System.currentTimeMillis()-tt0 )); // this takes a trivial (<3ms) amount of time.
        buildx.putProperty( QDataSet.UNITS, xdr.getUnits() );
        buildy.putProperty( QDataSet.UNITS, ydr.getUnits() );
        MutablePropertyDataSet mvds= DataSetOps.makePropertiesMutable( Ops.link( buildx.getDataSet(), buildy.getDataSet() ) );
        DataSetUtil.copyDimensionProperties( vds, mvds );        
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
        
        long t0= System.currentTimeMillis();
        logger.log(Level.FINE, "enter {0}.updatePlotImage: {1}", new Object[]{id, String.valueOf(getDataSet()) });

        super.incrementUpdateCount();

        QDataSet dataSet = getDataSet();
        selectionArea= null;

        if (dataSet == null ) {
            logger.fine("dataset was null");
            return;
        }

        if ( dataSet.rank() == 0) {
            logger.fine("rank 0 dataset");
            return;
        }

        if ( dataSet.length() == 0) {
            logger.fine("dataset was empty");
            return;
        }

        if ( !this.isActive() ) {
            return;
        }
        
        boolean plottable;

        QDataSet tds = null;
        QDataSet vds = null;

        QDataSet xds = SemanticOps.xtagsDataSet(dataSet);
        if ( !SemanticOps.isRank2Waveform(dataSet) ) {  // xtags are rank 2 bins can happen too
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
            unitsWarning= false;
            plottable = SemanticOps.getUnits(vds).isConvertableTo(yAxis.getUnits());
            if ( !plottable ) {
                if ( UnitsUtil.isRatioMeasurement( SemanticOps.getUnits(vds) ) && UnitsUtil.isRatioMeasurement( yAxis.getUnits() ) ) {
                    unitsWarning= true;
                }
            }

        } else {
            tds = (QDataSet) dataSet;
            plottable = SemanticOps.getUnits(tds).isConvertableTo(yAxis.getUnits());
            if ( !plottable ) {
                if ( UnitsUtil.isRatioMeasurement( SemanticOps.getUnits(tds) ) && UnitsUtil.isRatioMeasurement( yAxis.getUnits() ) ) {
                    unitsWarning= true;
                }
            }
        }

        plottable = SemanticOps.getUnits(xds).isConvertableTo(xAxis.getUnits());
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
        if (vds != null) {

            updateFirstLast(xAxis, yAxis, xds, vds );

            if ( dataSetReduced ) {
                logger.fine("reducing data that is bigger than dataSetSizeLimit");
                
                QDataSet mvds= doDataSetReduce( xAxis, yAxis, vds, 1, 1 );
                
                vds= mvds;
                xds= SemanticOps.xtagsDataSet(vds);

                updateFirstLast(xAxis, yAxis, xds, vds );  // we need to reset firstIndex, lastIndex

                logger.log( Level.FINE, "data reduced to {0} {1}", new Object[] { vds, Ops.extent(xds) } );
                logger.log(Level.FINE, "reduceDataSet complete {0}", System.currentTimeMillis()-t0 );                
            } else {
                logger.log(Level.FINE, "data not reduced");
            }
            
            if (fillToReference) {
                fillElement.update(xAxis, yAxis, vds, monitor.getSubtaskMonitor("fillElement.update"));
                logger.log(Level.FINE, "fillElement.update complete {0}", System.currentTimeMillis()-t0 );
            }

        } else if (tds != null) {
            
            LoggerManager.resetTimer("render waveform");
            
            updateFirstLast(xAxis, yAxis, xds, tds ); // minimal support assumes vert slice data is all valid or all invalid.
            LoggerManager.markTime("updateFirstLast");
            
            if ( SemanticOps.isRank2Waveform(dataSet) ) {
                QDataSet res= DataSetUtil.asDataSet( xAxis.getDatumRange().width().divide(xAxis.getWidth()) );
                vds= dataSet.trim( this.firstIndex, this.lastIndex );
                LoggerManager.markTime("trim");
            
                vds= Reduction.reducex( vds,res );  // waveform
                LoggerManager.markTime("reducex");
                if ( vds.rank()==2 ) {
                    vds= DataSetOps.flattenWaveform(vds);
                    LoggerManager.markTime("flatten");
                }
                xds= SemanticOps.xtagsDataSet(vds); 
                updateFirstLast(xAxis, yAxis, xds, vds );  // we need to reset firstIndex, lastIndex
                LoggerManager.markTime("updateFirstLast again");
            }
            logger.log(Level.FINE, "renderWaveform updateFirstLast complete {0}", System.currentTimeMillis()-t0 );
            
        } else {
            System.err.println("both tds and vds are null");
        }
        
        if (psymConnector != PsymConnector.NONE) {
            try {
                psymConnectorElement.update(xAxis, yAxis, vds, monitor.getSubtaskMonitor("psymConnectorElement.update"));
            } catch ( InconvertibleUnitsException ex ) {
                return;
            }
        }

        try {
            errorElement.update(xAxis, yAxis, vds, monitor.getSubtaskMonitor("errorElement.update"));
            if ( vds!=null && vds.rank()==1 && dataSet.rank()==2 && SemanticOps.isBundle(dataSet) ) {
                psymsElement.update(xAxis, yAxis, dataSet, monitor.getSubtaskMonitor("psymsElement.update"));     // color scatter
            } else {
                psymsElement.update(xAxis, yAxis, vds, monitor.getSubtaskMonitor("psymsElement.update"));
            }
            logger.log(Level.FINE, "psymsElement.update complete {0}", System.currentTimeMillis()-t0 );            
        } catch ( InconvertibleUnitsException ex ) {
            return;
        }

        selectionArea= calcSelectionArea( xAxis, yAxis, xds, vds );        
        logger.log(Level.FINE, "calcSelectionArea complete {0}", System.currentTimeMillis()-t0);  
        //if (getParent() != null) {
        //    getParent().repaint();
        //}

        logger.log(Level.FINE, "done updatePlotImage in {0} ms", (System.currentTimeMillis() - t0));
        
        long milli = System.currentTimeMillis();
        long renderTime = (milli - t0);
        double dppms = (lastIndex - firstIndex) / (double) renderTime;

        monitor.finished();
        setUpdatesPointsPerMillisecond(dppms);
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
        if ( this.unitsWarning ) {
            ArrayDataSet ds3= ArrayDataSet.copy(ds);
            ds3.putProperty( QDataSet.UNITS, yaxis.getUnits() );
            ds2= ds3;
        }
        if ( this.xunitsWarning ) {
            ArrayDataSet ds3= ArrayDataSet.copy(xds);
            ds3.putProperty( QDataSet.UNITS, yaxis.getUnits() );
            xds= ds3;
        }
        if ( ds2.rank()==2 ) {
            ds2= DataSetOps.slice1( ds2, 0 );
        }

        try {
            if ( this.psymConnector==PsymConnector.NONE || ds2.length()>10000 ) {
                QDataSet reduce= doDataSetReduce( xaxis, yaxis, ds2, 5, 5 );

                logger.fine( String.format( "reduce path in calcSelectionArea: %s\n", reduce ) );
                GeneralPath path = GraphUtil.getPath( xaxis, yaxis, SemanticOps.xtagsDataSet(reduce), reduce, GraphUtil.CONNECT_MODE_SCATTER, true );

                Shape s = new BasicStroke( Math.min(14,(float)getSymSize()+8.f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND ).createStrokedShape(path);
                return s;                
                
            } else {
                QDataSet reduce = VectorUtil.reduce2D(
                    xds, ds2,
                    firstIndex,
                    lastIndex,
                    widthx.divide(xaxis.getColumn().getWidth()/5),
                    widthy.divide(yaxis.getRow().getHeight()/5)
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
            logger.fine( String.format( "time to calcSelectionArea: %6.3f\n", ((System.currentTimeMillis()-t0)/1000.)) );
        }

        

    }

    @Override
    protected void installRenderer() {
        if (!DasApplication.getDefaultApplication().isHeadless()) {
            DasPlot lparent= getParent();
            if ( lparent==null ) throw new IllegalArgumentException("parent not set");
            DasMouseInputAdapter mouseAdapter = lparent.mouseAdapter;
            DasPlot p = lparent;
            mouseAdapter.addMouseModule(new MouseModule(p, new LengthDragRenderer(p, p.getXAxis(), p.getYAxis()), "Length"));

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
        return String.valueOf(this.getDataSetDescriptor());
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
               }
               update();
           }
        }
    };

    /**
     * Setter for property colorBar.
     * @param colorBar New value of property colorBar.
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
            DasPlot parent= getParent();
            if (parent != null && parent.getCanvas() != null) {
                parent.getCanvas().add(colorBar);
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

    public FillStyle getFillStyle() {
        return fillStyle;
    }

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

        if ((!accept) && errorElement.acceptContext(dp)) {
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
