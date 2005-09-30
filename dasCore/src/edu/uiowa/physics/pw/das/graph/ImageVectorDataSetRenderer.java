/*
 * ImageVectorDataSetRenderer.java
 *
 * This renderer can handle vector data sets with tens of thousands of points
 * by histogramming the points and then creating a greyscale spectrogram of
 * the histogram.  The property "saturationHitCount" defines the number of pixel
 * hits that will make the pixel black.  In the future, this may be modified to
 * support color, alpha channel, and connected psyms.
 *
 * Created on April 14, 2005, 8:45 PM
 */

package edu.uiowa.physics.pw.das.graph;

import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.system.DasLogger;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.util.logging.Logger;

/**
 *
 * @author Jeremy
 */
public class ImageVectorDataSetRenderer extends Renderer {
    
    GeneralPath path;
    
    //SymbolLineRenderer highResRenderer;
    Datum xTagWidth;
    
    BufferedImage plotImage;
    DatumRange imageXRange;
    DatumRange imageYRange;
    
    SymbolLineRenderer symbolLineRenderer;
    
    /** Creates a new instance of LotsaPointsRenderer */
    public ImageVectorDataSetRenderer( DataSetDescriptor dsd ) {
        super(dsd);
        symbolLineRenderer= new SymbolLineRenderer((DataSet)null);
        //highResRenderer= new SymbolLineRenderer(dsd);
        //highResRenderer.setPsym(Psym.NONE);
        //highResRenderer.setPsymConnector(PsymConnector.SOLID);
        //        xTagWidth= (Datum)dsd.getProperty("xTagWidth");
    }
    
    protected org.w3c.dom.Element getDOMElement(org.w3c.dom.Document document) {
        return null;
    }
    
    private void renderGhostly( java.awt.Graphics g1, DasAxis xAxis, DasAxis yAxis ) {
        Graphics2D g2= (Graphics2D)g1.create();
        
        AffineTransform at= getAffineTransform( xAxis, yAxis );
        
        if ( at==null ) {
            return; // TODO: consider throwing exception
        }
        
        if (getDataSet()==null && lastException!=null ) {
            renderException(g2,xAxis,yAxis,lastException);
        } else if (plotImage!=null) {
            Point2D p;

            try {
                g2.transform(at);
                //g2.setRenderingHint( RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED );
                g2.setRenderingHint( RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR );
                p= new Point2D.Double( xAxis.transform(imageXRange.min()), yAxis.transform(imageYRange.max()) );
                p= at.inverseTransform( p, p );
            } catch ( NoninvertibleTransformException e ) {
                return;
            }

            g2.drawImage( plotImage,(int)(p.getX()+0.5),(int)(p.getY()+0.5), getParent() );
            
        }
        g2.dispose();
    }
    
    
    public void render(java.awt.Graphics g1, DasAxis xAxis, DasAxis yAxis) {
        renderGhostly( g1, xAxis, yAxis );        
    }
    
    private void ghostlyImage2( DasAxis xAxis, DasAxis yAxis, VectorDataSet ds ) {
        int ny= yAxis.getRow().getHeight();
        int nx= xAxis.getColumn().getWidth();
        
        Logger log= DasLogger.getLogger( DasLogger.GRAPHICS_LOG );
        
        log.fine( "create Image" );
        BufferedImage image= new BufferedImage( nx, ny, BufferedImage.TYPE_INT_ARGB );
        Graphics2D g= (Graphics2D)image.getGraphics();
        
        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        
        g.setColor(Color.black);
        g.setStroke( new BasicStroke( 1.f/saturationHitCount ) );
        
        g.translate( -xAxis.getColumn().getDMinimum(), -yAxis.getRow().getDMinimum() );
        
        DatumRange visibleRange= xAxis.getDatumRange();
        //if ( isOverloading() ) visibleRange= visibleRange.rescale(-1,2);
        
        int firstIndex= DataSetUtil.getPreviousColumn( ds, visibleRange.min() );
        int lastIndex= DataSetUtil.getNextColumn( ds, visibleRange.max() );
        
        final int STATE_LINETO= -991;
        final int STATE_MOVETO= -992;
        
        int state= STATE_MOVETO;
        
        
        // TODO: data breaks
        int ix0=0, iy0=0;
        for ( int i=firstIndex; i<=lastIndex; i++ ) {
            int iy= (int)yAxis.transform( ds.getDatum(i) );
            int ix= (int)xAxis.transform( ds.getXTagDatum(i) );
            switch( state ) {
                case STATE_MOVETO:
                    g.fillRect( ix, iy, 1, 1 ); 
                    ix0= ix; iy0=iy; break;
                case STATE_LINETO:
                    g.draw( new Line2D.Float( ix0, iy0, ix, iy ) );
                    g.fillRect( ix, iy, 1, 1 );
                    ix0= ix; iy0=iy;
                    break;
            }
            state= STATE_LINETO;
        }
        
        log.fine( "done" );
        plotImage= image;
        imageXRange= xAxis.getDatumRange();
        imageYRange= yAxis.getDatumRange();
    }
    
    private TableDataSet histogram( RebinDescriptor ddx, RebinDescriptor ddy, VectorDataSet ds ) {
        ddx.setOutOfBoundsAction(RebinDescriptor.MINUSONE);
        ddy.setOutOfBoundsAction(RebinDescriptor.MINUSONE);
        WritableTableDataSet tds= WritableTableDataSet.newSimple( ddx.numberOfBins(), ddx.getUnits(),
                ddy.numberOfBins(), ddy.getUnits(), Units.dimensionless );
        Units xunits= ddx.getUnits();
        Units yunits= ddy.getUnits();
        Units zunits= Units.dimensionless;
        
        int i= DataSetUtil.getPreviousColumn( ds, ddx.binStart(0) );
        int n= DataSetUtil.getNextColumn( ds, ddx.binStop(ddx.numberOfBins()-1) );
        for ( ; i<=n; i++ ) {
            int ix= ddx.whichBin( ds.getXTagDouble( i, xunits ), xunits );
            int iy= ddy.whichBin( ds.getDouble( i, yunits ), yunits );
            if ( ix!=-1 && iy!=-1 ) {
                double d= tds.getDouble(ix,iy, zunits );
                tds.setDouble( ix, iy, d + 1, zunits );
            }
        }
        return tds;
    }
    
    
    private void ghostlyImage( DasAxis xAxis, DasAxis yAxis, VectorDataSet ds ) {
        RebinDescriptor ddx;
        
        DatumRange xrange=xAxis.getDatumRange();
       /* if ( isOverloading() ) {
            xrange= xrange.rescale(-1,2);
            ddx= new RebinDescriptor(
                    xrange.min(), xrange.max(),
                    xAxis.getColumn().getWidth()*3,
                    xAxis.isLog());
        } else { */
        ddx= new RebinDescriptor(
                xrange.min(), xrange.max(),
                xAxis.getColumn().getWidth(),
                xAxis.isLog());
        /* } */
        
        DatumRange yrange= yAxis.getDatumRange();
        RebinDescriptor ddy = new RebinDescriptor(
                yrange.min(), yrange.max(),
                yAxis.getRow().getHeight(),
                yAxis.isLog());
        
        TableDataSet hist= histogram( ddx, ddy, ds );
        //WritableTableDataSet whist= (WritableTableDataSet)hist;
        
       /* double histMax= TableUtil.tableMax(hist, Units.dimensionless);
        for ( int i=0; i<whist.getXLength(); i++ ) {
            for ( int j=0, n=whist.getYLength(0); j<n; j++ ) {
                double d= whist.getDouble( i, j, Units.dimensionless );
                if ( d > 0 && d < histMax*floorFactor )
                    whist.setDouble( i,j, histMax*floorFactor, Units.dimensionless );
            }
        }  */
        
        
        int h= ddy.numberOfBins();
        int w= ddx.numberOfBins();
        
        int[] raster= new int[ h*w ];
        
        for (int i=0; i<w; i++) {
            for (int j=0; j<h; j++) {
                int index= (i-0) + ( h - j - 1 ) * w;
                int alpha= 255 - ( 256 * (int)hist.getDouble(i,j,Units.dimensionless) / saturationHitCount );
                if ( alpha < 0 ) alpha= 0;
                
                int icolor= ( alpha << 16 ) + ( alpha << 8 ) + alpha;
                raster[index]=icolor;
            }
        }
        
        plotImage= new BufferedImage( w, h, BufferedImage.TYPE_INT_RGB );
        WritableRaster r= plotImage.getRaster();
        r.setDataElements( 0,0,w,h,raster);
        
        imageXRange= xrange;
        imageYRange= yrange;
    }    
    
    public void updatePlotImage(DasAxis xAxis, DasAxis yAxis, edu.uiowa.physics.pw.das.util.DasProgressMonitor monitor) throws DasException {
        super.updatePlotImage( xAxis, yAxis, monitor );
        
        long t0= System.currentTimeMillis();
        VectorDataSet ds= (VectorDataSet)getDataSet();
        if ( ds==null ) return;
                
        DatumRange visibleRange= xAxis.getDatumRange();        
        int firstIndex= DataSetUtil.getPreviousColumn( ds, visibleRange.min() );
        int lastIndex= DataSetUtil.getNextColumn( ds, visibleRange.max() );
                
        if ( ( lastIndex-firstIndex ) > 20 * xAxis.getColumn().getWidth() ) {
            logger.info("rendering with histogram");
            ghostlyImage( xAxis, yAxis, ds );
        } else {
            logger.info("rendinging with lines");
            ghostlyImage2( xAxis, yAxis, ds );
        }
        logger.info( "done updatePlotImage" );
        
    }
    
    int saturationHitCount= 5;
    
    public void setSaturationHitCount( int d ) {
        if ( d>10 ) d=10;
        this.saturationHitCount= d;
        this.update();
    }
    
    public int getSaturationHitCount( ) {
        return this.saturationHitCount;
    }
    
    protected void uninstallRenderer() {
    }
    
    protected void installRenderer() {
    }
    
}
