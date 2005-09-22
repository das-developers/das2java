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
import edu.uiowa.physics.pw.apps.auralization.*;
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
        
        // TODO: This seems to work as long as the upper-left hand corner of the image doesn't move!!!
        //   works fine for resize of lower-right and range changes.
        AffineTransform at= getAffineTransform( xAxis, yAxis );
        
        if ( at==null ) {
            return; // TODO: consider throwing exception
        }
        
        if (getDataSet()==null && lastException!=null ) {
            renderException(g2,xAxis,yAxis,lastException);
        } else if (plotImage!=null) {
            Point2D p;
            //  if ( !at.isIdentity() ) {
            try {
                g2.transform(at);
                //g2.setRenderingHint( RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED );
                g2.setRenderingHint( RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR );
                p= new Point2D.Double( xAxis.transform(imageXRange.min()), yAxis.transform(imageYRange.max()) );
                p= at.inverseTransform( p, p );
            } catch ( NoninvertibleTransformException e ) {
                return;
            }
            //  } else {
            //      p= new Point2D.Float( xAxis.getColumn().getDMinimum(), yAxis.getRow().getDMinimum() );
            //  }
            
            g2.drawImage( plotImage,(int)(p.getX()+0.5),(int)(p.getY()+0.5), getParent() );
            
        }
        g2.dispose();
    }
    
    
    public void render(java.awt.Graphics g1, DasAxis xAxis, DasAxis yAxis) {
        renderGhostly( g1, xAxis, yAxis );
        /* long t0= System.currentTimeMillis();
        Graphics2D g= (Graphics2D)g1.create();
         
        AffineTransform at= getAffineTransform(xAxis,yAxis);
        if ( at==null ) {
            return;
        }
         
        g.transform(at);
         
        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        Stroke s= new BasicStroke( 1.f );
         
        if (path != null) {
            g.draw(path);
        }
        g.dispose();
         
        System.err.println( "time to render: "+( System.currentTimeMillis()-t0 ) );*/
        
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
        g.setStroke( new BasicStroke( .1f ) );
        
        g.translate( -xAxis.getColumn().getDMinimum(), -yAxis.getRow().getDMinimum() );
        
        log.fine("calculate path");
        //GeneralPath path= calcPathConnect( xAxis, yAxis, ds );
        
        log.fine( "draw path" );
        //g.draw(path);
        
        
        DatumRange visibleRange= xAxis.getDatumRange();
        //if ( isOverloading() ) visibleRange= visibleRange.rescale(-1,2);
        
        int firstIndex= DataSetUtil.getPreviousColumn( ds, visibleRange.min() );
        int lastIndex= DataSetUtil.getNextColumn( ds, visibleRange.max() );
        
        final int STATE_LINETO= -991;
        final int STATE_MOVETO= -992;
        
        int state= STATE_MOVETO;
        
        int ix0=0, iy0=0;
        for ( int i=firstIndex; i<=lastIndex; i++ ) {
            int iy= (int)yAxis.transform( ds.getDatum(i) );
            int ix= (int)xAxis.transform( ds.getXTagDatum(i) );
            switch( state ) {
                case STATE_MOVETO:
                    ix0= ix; iy0=iy; break;
                case STATE_LINETO:
                    g.draw( new Line2D.Float( ix0, iy0, ix, iy ) );
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
        if ( isOverloading() ) {
            xrange= xrange.rescale(-1,2);
            ddx= new RebinDescriptor(
                    xrange.min(), xrange.max(),
                    xAxis.getColumn().getWidth()*3,
                    xAxis.isLog());
        } else {
            ddx= new RebinDescriptor(
                    xrange.min(), xrange.max(),
                    xAxis.getColumn().getWidth(),
                    xAxis.isLog());
        }
        
        DatumRange yrange= yAxis.getDatumRange();
        RebinDescriptor ddy = new RebinDescriptor(
                yrange.min(), yrange.max(),
                yAxis.getRow().getHeight(),
                yAxis.isLog());
        
        TableDataSet hist= histogram( ddx, ddy, ds );
        WritableTableDataSet whist= (WritableTableDataSet)hist;
        
       /* double histMax= TableUtil.tableMax(hist, Units.dimensionless);
        for ( int i=0; i<whist.getXLength(); i++ ) {
            for ( int j=0, n=whist.getYLength(0); j<n; j++ ) {
                double d= whist.getDouble( i, j, Units.dimensionless );
                if ( d > 0 && d < histMax*floorFactor )
                    whist.setDouble( i,j, histMax*floorFactor, Units.dimensionless );
            }
        }  */
        
        DasColorBar cb= new DasColorBar( Units.dimensionless.createDatum(0.),Units.dimensionless.createDatum(saturationHitCount), false );
        cb.setType( DasColorBar.Type.GRAYSCALE );
        
        byte[] raster= SpectrogramRenderer.transformSimpleTableDataSet( whist, cb );
        
        int h= ddy.numberOfBins();
        int w= ddx.numberOfBins();
        
        IndexColorModel model= cb.getIndexColorModel();
        plotImage= new BufferedImage( w, h, BufferedImage.TYPE_BYTE_INDEXED, model );
        
        WritableRaster r= plotImage.getRaster();        
        r.setDataElements( 0,0,w,h,raster);                
        
        imageXRange= xrange;
        imageYRange= yrange;
    }
    
    private GeneralPath calcPathConnect( DasAxis xAxis, DasAxis yAxis, VectorDataSet ds ) {
        
        final int STATE_LINETO= -991;
        final int STATE_MOVETO= -992;
        
        int state= STATE_MOVETO;
        
        DatumRange visibleRange= xAxis.getDatumRange();
        if ( isOverloading() ) visibleRange= visibleRange.rescale(-1,2);
        
        int firstIndex= DataSetUtil.getPreviousColumn( ds, visibleRange.min() );
        int lastIndex= DataSetUtil.getNextColumn( ds, visibleRange.max() );
        
        GeneralPath newPath = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 110 * ( lastIndex - firstIndex ) / 100 );
        
        for ( int i=firstIndex; i<=lastIndex; i++ ) {
            int iy= (int)yAxis.transform( ds.getDatum(i) );
            int ix= (int)xAxis.transform( ds.getXTagDatum(i) );
            switch( state ) {
                case STATE_MOVETO: newPath.moveTo( ix, iy );
                case STATE_LINETO: newPath.lineTo( ix, iy );
            }
            state= STATE_LINETO;
        }
        return newPath;
    }
    
    private GeneralPath calcPathMinMax( DasAxis xAxis, DasAxis yAxis, VectorDataSet ds ) throws DasException {
        DataSetRebinner rebinner= new RangeRebinner();
        
        DatumRange visibleRange= xAxis.getDatumRange();
        if ( isOverloading() ) visibleRange= visibleRange.rescale(-1,2);
        
        RebinDescriptor xRebinDescriptor = new RebinDescriptor(
                visibleRange.min(), visibleRange.max(),
                xAxis.getDLength()*3,
                xAxis.isLog());
        
        DataSet renderDs= rebinner.rebin( ds, xRebinDescriptor, null );
        
        if ( renderDs.getXLength()==0 ) {
            return null;
            
        } else {
            VectorDataSet minDs= (VectorDataSet)renderDs.getPlanarView("min");
            VectorDataSet maxDs= (VectorDataSet)renderDs.getPlanarView("max");
            Units yUnits= minDs.getYUnits();
            Units xUnits= minDs.getXUnits();
            
            GeneralPath newPath = new GeneralPath();
            
            for ( int i=0; i<renderDs.getXLength(); i++ ) {
                int iy0= (int)yAxis.transform( minDs.getDatum(i) );
                int iy1= (int)yAxis.transform( maxDs.getDatum(i) );
                int x= (int)xAxis.transform( minDs.getXTagDatum(i) );
                newPath.moveTo( x, iy0 );
                newPath.lineTo( x, iy1 );
            }
            
            return newPath;
        }
    }
    
    public void updatePlotImage(DasAxis xAxis, DasAxis yAxis, edu.uiowa.physics.pw.das.util.DasProgressMonitor monitor) throws DasException {
        super.updatePlotImage( xAxis, yAxis, monitor );
        
        long t0= System.currentTimeMillis();
        VectorDataSet ds= (VectorDataSet)getDataSet();
        if ( ds==null ) return;
        
        //ghostlyImage2( xAxis, yAxis, ds );
        ghostlyImage( xAxis, yAxis, ds );
        /*
        Datum pixelWidth= xAxis.invTransform(1).subtract(xAxis.invTransform(0));
         
        long t1= System.currentTimeMillis();
         
        if ( xTagWidth==null ) xTagWidth= DataSetUtil.guessXTagWidth( ds );
        System.err.println( "time to guess: "+( System.currentTimeMillis()-t1 ) );
         
        double samplesPerPixel= pixelWidth.divide( xTagWidth ).doubleValue( Units.dimensionless );
        if ( samplesPerPixel > 10 ) {
            this.path= calcPathMinMax( xAxis, yAxis, ds );
        } else {
            this.path= calcPathConnect( xAxis, yAxis, ds );
        }
        System.err.println( "time to update: "+( System.currentTimeMillis()-t0 ) );*/
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
