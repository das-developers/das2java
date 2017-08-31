
package org.das2.graph;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.das2.DasApplication;
import org.das2.DasException;
import org.das2.datum.Units;
import org.das2.event.CrossHairMouseModule;
import org.das2.event.DasMouseInputAdapter;
import org.das2.event.MouseModule;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.qds.DDataSet;
import org.das2.qds.JoinDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.ops.Ops;

/**
 * Renders RBG images stored in a QDataSet[m,n,3], etc.
 * @author jbf
 */
public class RGBImageRenderer extends Renderer {

    BufferedImage image=null;

    /**
     * the bounds of the last rendering
     */
    Rectangle rect= null;
    
    @Override
    public void setControl(String s) {
        super.setControl(s);
        this.nearestNeighborInterpolation= getBooleanControl( RGBImageRenderer.PROP_NEARESTNEIGHBORINTERPOLATION, nearestNeighborInterpolation );
    }
    
    @Override
    public String getControl() {
        Map<String,String> controls= new LinkedHashMap();
        controls.put( RGBImageRenderer.PROP_NEARESTNEIGHBORINTERPOLATION, encodeBooleanControl( nearestNeighborInterpolation ) );
        return Renderer.formatControl(controls);
    }
    
    @Override
    public void render(Graphics g, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {
        QDataSet lds= getDataSet();
        QDataSet dep0;
        QDataSet dep1;

        if ( lds==null || lds.length() == 0) {
            getParent().postMessage( this, "null data set", Level.INFO, null, null );
            return;
        }
        
        if ( image==null ) {
            getParent().postException( this, lastException );
        }
        
        BufferedImage im= image; // make local copy for thread safety
        if ( im==null ) return; // transitional state

        dep0= (QDataSet)lds.property(QDataSet.DEPEND_0);
        dep1= (QDataSet)lds.property(QDataSet.DEPEND_1);

        if ( dep0==null ) dep0= Ops.dindgen(im.getWidth());
        if ( dep1==null ) dep1= Ops.dindgen(im.getHeight());
        
        {
            int n= dep0.length();
            double dx1= ( dep0.value(1)- dep0.value(0) );
            double dx2= ( dep0.value(n-1)- dep0.value(n-2) );
            boolean xlog= (dx2/dx1)>10.;
            if ( xAxis.isLog()!=xlog ) {
                getParent().postMessage( this, "xaxis must be " + ( xlog ? "log" : "linear" ) + ", for this image",  Level.INFO, null, null );
                return;
            }
        }
        {
            int n= dep1.length();
            double dy1= ( dep1.value(1)- dep1.value(0) );
            double dy2= ( dep1.value(n-1)- dep1.value(n-2) );
            boolean ylog= (dy2/dy1)>10.;
            if ( yAxis.isLog()!=ylog ) {
                getParent().postMessage( this, "yaxis must be " + ( ylog ? "log" : "linear" )+ ", for this image",  Level.INFO, null, null );
                return;
            }
        }        

        Units xunits= SemanticOps.getUnits(dep0);
        Units yunits= SemanticOps.getUnits(dep1);

        int h= im.getHeight();
        int w= im.getWidth();
        
        double dx0= dep0.value(1)-dep0.value(0);
        double dy0= dep1.value(1)-dep1.value(0);
        int ix0;
        int ix1; // inclusive
        int iy0;
        int iy1; // inclusive
        
        double dx= dep0.value(1)-dep0.value(0);
        
        int x0;
        if ( true ) { //if ( x0==-10000 ) {
            if ( dx>0 ) {
                ix0= (int)( Math.floor( Ops.findex( dep0, xAxis.invTransform( 0. ) ).value() ) );
            } else {
                ix0= (int)( Math.floor( Ops.findex( Ops.multiply(-1,dep0), xAxis.invTransform( 0. ).multiply(-1) ).value() ) );
            }
            ix0= Math.max( 0, ix0 );
            ix0= Math.min( w-1, ix0 );
            x0= (int)xAxis.transform( dep0.value(ix0) - dx0/2, xunits);
        }
        
        double dy= dep1.value(1)-dep1.value(0);
        int y0;
        if ( true ) { //y0==10000 ) {
            if ( dy>0 ) {
                iy0= (int)( Math.floor( Ops.findex( dep1, yAxis.invTransform( yAxis.getHeight()+yAxis.getY() ) ).value() ) );
            } else {
                iy0= (int)( Math.floor( Ops.findex( Ops.multiply(-1,dep1), yAxis.invTransform( yAxis.getHeight()+yAxis.getY() ).multiply(-1) ).value() ) );
            }
            iy0= Math.max( 0, iy0 );
            iy0= Math.min( h-1, iy0 );
            y0= (int)yAxis.transform( dep1.value(iy0) - dy0/2, yunits);
        }
        
        int x1;
        if ( true ) { //if ( x1==10000 ) {
            if ( dx>0 ) {
                ix1= (int)( Math.ceil( Ops.findex( dep0, xAxis.invTransform( xAxis.getWidth()+xAxis.getX() ) ).value() ) );
            } else {
                ix1= (int)( Math.ceil( Ops.findex( Ops.multiply(-1,dep0), xAxis.invTransform( xAxis.getWidth()+xAxis.getX() ).multiply(-1) ).value() ) );
            }
            ix1= Math.max( 0, ix1 );
            ix1= Math.min( w-1, ix1 );
            x1= (int)xAxis.transform( dep0.value(ix1) + dx0/2, xunits);
        }
        
        int y1;
        if ( true ) { //if ( y1==-10000 ) {
            if ( dy>0 ) {
                iy1= (int)( Math.ceil( Ops.findex( dep1, yAxis.invTransform( 0 ) ).value() ) );
            } else {
                iy1= (int)( Math.floor( Ops.findex( Ops.multiply(-1,dep1), yAxis.invTransform( 0. ).multiply(-1) ).value() ) );
            }
            iy1= Math.max( 0, iy1 );
            iy1= Math.min( h-1, iy1 );
            y1= (int)yAxis.transform( dep1.value(iy1) + dy0/2, yunits);
        }
        if ( nearestNeighborInterpolation ) {
            ((Graphics2D)g).setRenderingHint( RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR );
        }
        ix1= ix1+1;  // now exclusive
        iy1= iy1+1;
        
        if ( ix0==ix1 ) {
            getParent().postMessage( this, "image is off screen",  Level.INFO, null, null );
        }
        if ( iy0==iy1 ) {
            getParent().postMessage( this, "image is off screen",  Level.INFO, null, null );
        }
        
        if ( ix0>0 || ix1<w || iy0>0 || iy1<h ) {
            if ( ix0<ix1 ) {
                if ( iy0<iy1 ) {
                    im= im.getSubimage( ix0, iy0, ix1-ix0, iy1-iy0 );
                } else {
                    im= im.getSubimage( ix0, iy1, ix1-ix0, iy0-iy1 );
                    AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
                    tx.translate(0,-im.getHeight(null));
                    AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
                    im = op.filter(im, null);
                }
            } else {
                if ( iy0<iy1 ) {
                    im= im.getSubimage( ix1, iy0, ix0-ix1, iy1-iy0 );
                    AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
                    tx.translate(-im.getWidth(null), 0);
                    AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
                    im = op.filter(im, null);
                } else {
                    im= im.getSubimage( ix1, iy1, ix0-ix1, iy0-iy1 );
                    AffineTransform tx = AffineTransform.getScaleInstance(-1, -1);
                    tx.translate(-im.getWidth(null), -im.getHeight(null) );
                    AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
                    im = op.filter(im, null);
                }
            }
        }
        
        g.drawImage( im, x0, y0, x1-x0, y1-y0, null );
        rect= new Rectangle( x0, y1, x1-x0, y0-y1 );
    }

    private boolean nearestNeighborInterpolation = false;

    public static final String PROP_NEARESTNEIGHBORINTERPOLATION = "nearestNeighborInterpolation";

    public boolean isNearestNeighborInterpolation() {
        return nearestNeighborInterpolation;
    }

    public void setNearestNeighborInterpolation(boolean nearestNeighborInterpolation) {
        boolean oldNearestNeighborInterpolation = this.nearestNeighborInterpolation;
        this.nearestNeighborInterpolation = nearestNeighborInterpolation;
        update();
        propertyChangeSupport.firePropertyChange(PROP_NEARESTNEIGHBORINTERPOLATION, oldNearestNeighborInterpolation, nearestNeighborInterpolation);
    }

    @Override
    public void setDataSet(QDataSet ds) {
        super.setDataSet(ds);
        image= null;
    }

    @Override
    protected void installRenderer() {
        super.installRenderer(); //To change body of generated methods, choose Tools | Templates.
        DasPlot parent= getParent();
        if (!"true".equals(DasApplication.getProperty("java.awt.headless", "false"))) {
            DasMouseInputAdapter mouseAdapter = parent.mouseAdapter;

            MouseModule ch = new CrossHairMouseModule(parent, this, parent.getXAxis(), parent.getYAxis());
            mouseAdapter.addMouseModule(ch);

        }
    }

    @Override
    protected void uninstallRenderer() {
        //TODO: this leaves the mouse module, and it really should be removed.  SpectrogramRenderer too...
        super.uninstallRenderer();
    }

    
    
    /**
     * this actually can take a little while, I discovered when playing with the wave-at-cassini image.
     */
    @Override
    public void updatePlotImage(DasAxis xAxis, DasAxis yAxis, ProgressMonitor monitor) throws DasException {
        monitor.started();
        monitor.setProgressMessage("creating image from RGB data");
        if ( ds==null ) {
            image=null;
        } else {
            if ( image==null ) {
                try {
                    image= getImage(ds,xAxis,yAxis);
                    setLastException(null);
                } catch ( IllegalArgumentException ex ) {
                    setLastException(ex);
                    return;
                }
            }
        }
        monitor.finished();
    }

    
    private BufferedImage getImage(QDataSet ds, DasAxis xAxis, DasAxis yAxis ) throws IllegalArgumentException {
        // ds should be a rank 2 gray scale of Width x Height values, valued from 0-255,
        // or a rank 3 color image 3,W,H.
        int imageType = -19999;
        int w;
        int h;

        w = ds.length();
        h = ds.length(0);

        BufferedImage im;
        switch (ds.rank()) {
            case 2:
                imageType = BufferedImage.TYPE_BYTE_GRAY;
                break;
            case 3:
                if (ds.length(0,0) == 3) {
                    QDataSet dep2 = (QDataSet) ds.property(QDataSet.DEPEND_2);
                    imageType = BufferedImage.TYPE_INT_RGB; // default
                    if (dep2 != null) {
                        String s0 = org.das2.qds.DataSetUtil.asDatum(dep2.slice(0)).toString().toLowerCase();
                        String s1 = org.das2.qds.DataSetUtil.asDatum(dep2.slice(1)).toString().toLowerCase();
                        String s2 = org.das2.qds.DataSetUtil.asDatum(dep2.slice(2)).toString().toLowerCase();
                        if (s0.startsWith("r") && s1.startsWith("g") && s2.startsWith("b")) {
                            imageType = BufferedImage.TYPE_INT_RGB;
                        } else if (s0.startsWith("b") && s1.startsWith("g") && s2.startsWith("r")) {
                            imageType = BufferedImage.TYPE_INT_BGR;
                        }
                    }
                } else if (ds.length(0,0) == 4) {
                    QDataSet dep2 = (QDataSet) ds.property(QDataSet.DEPEND_2);
                    imageType = BufferedImage.TYPE_INT_ARGB;
                    if (dep2 != null) {
                        String s0 = org.das2.qds.DataSetUtil.asDatum(dep2.slice(0)).toString().toLowerCase();
                        String s1 = org.das2.qds.DataSetUtil.asDatum(dep2.slice(1)).toString().toLowerCase();
                        String s2 = org.das2.qds.DataSetUtil.asDatum(dep2.slice(2)).toString().toLowerCase();
                        String s3 = org.das2.qds.DataSetUtil.asDatum(dep2.slice(3)).toString().toLowerCase();
                        if (s0.startsWith("a") && s1.startsWith("r") && s2.startsWith("g") && s3.startsWith("b")) {
                            imageType = BufferedImage.TYPE_INT_ARGB;
                        } else if ( s0.startsWith("a") && s1.startsWith("b") && s2.startsWith("g") && s3.startsWith("r") ) {
                            imageType = BufferedImage.TYPE_4BYTE_ABGR;
                        }
                    }
                }   break;
            default:
                throw new IllegalArgumentException("DataSet must be rank 2 or rank 3: "+ds );
        }
        if (imageType == -19999) {
            throw new IllegalArgumentException("DataSet must be ds[w,h] ds[w,h,3] or ds[w,h,4] and be RGB, BGR, or ARGB.  Default is RBG");
        } else {
            im = new BufferedImage(w, h, imageType);
        }
        if (ds.rank() == 2) {
            for (int i = 0; i < w; i++) {
                for (int j = 0; j < h; j++) {
                    im.setRGB(i, j, 65536 * (int) ds.value(i,j) +  256 * (int) ds.value(i,j) + (int) ds.value(i,j));
                }
            }
        } else if (ds.rank() == 3) {
            for (int i = 0; i < w; i++) {
                for (int j = 0; j < h; j++) {
                    int v;
                    switch (ds.length(0,0)) {
                        case 4:
                            v = 16777216 * (int) ds.value(i,j,0) + 65536 * (int) ds.value(i,j,1) + 256 * (int) ds.value(i,j,2) + (int) ds.value(i,j,3);
                            break;
                        case 3:
                            v = 65536 * (int) ds.value(i,j,0) + 256 * (int) ds.value(i,j,1) + (int) ds.value(i,j,2);
                            break;
                        default:
                            throw new IllegalArgumentException("ds.length=" + ds.length());
                    }
                    im.setRGB(i, j, v);
                }
            }
        }

        return im;
    }

    /**
     * accepts either rank2 data with grey scale 0-255, or rank3 (w,h,3 or 4)
     * @param ds the dataset
     * @return true if the dataset is useful.
     */
    public static boolean acceptsData( QDataSet ds ) {
        switch (ds.rank()) {
            case 2:
                return !SemanticOps.isBundle(ds);
            case 3:
                return ds.length(0,0)>2 && ds.length(0,0)<5;
            default:
                return false;
        }
    }

    @Override
    public boolean acceptContext(int x, int y) {
        return rect!=null && rect.contains(x,y);
    }
    
    public Shape selectionArea() {
        //GeneralPath gp= new GeneralPath();
        //gp.append( new BasicStroke( Math.min(14,1.f+8.f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND ).createStrokedShape(rect), true );
        if ( rect==null ) {
            return SelectionUtil.NULL;
        } else {
            return rect;
        }
    }

    @Override
    public Icon getListIcon() {
        BufferedImage result= new BufferedImage(16,16,BufferedImage.TYPE_INT_RGB);
        
        Graphics2D g= (Graphics2D) result.getGraphics();
        
        g.drawImage( image, 0,0, 16,16, null ); // TODO: preserve aspect ratio, pick representative region. 
        return new ImageIcon( result );
    }
    
    
    /**
     * autorange on the data, returning a rank 2 bounds for the dataset.
     * @param ds the dataset
     * @return a bounding box 
     * @see org.das2.qds.examples.Schemes#boundingBox() 
     */
    public static QDataSet doAutorange( QDataSet ds ) {

        QDataSet xrange;
        QDataSet yrange;

        if ( ds.rank()==2 || ds.rank()==3 ) {
            QDataSet xx= (QDataSet)ds.property( QDataSet.DEPEND_0 );
            QDataSet yy= (QDataSet)ds.property( QDataSet.DEPEND_1 );
            if ( xx!=null ) {
                xrange= Ops.extent(xx);
            } else {
                xrange= DDataSet.wrap( new double[] { 0, ds.length() } );
            }
            if ( yy!=null ) {
                yrange= Ops.extent(yy);
            } else {
                yrange= DDataSet.wrap( new double[] { 0, ds.length(0) } );
            }
        } else {
            throw new IllegalArgumentException("dataset should be rank 2 or rank 3: "+ds );
        }

        JoinDataSet bds= new JoinDataSet(2);
        bds.join(xrange);
        bds.join(yrange);

        return bds;

    }
}
