
package org.das2.graph;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
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
import org.virbo.dataset.DDataSet;
import org.virbo.dataset.JoinDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;
import org.virbo.dsops.Ops;

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
            boolean xlog= dx1<dx2/10;
            if ( xAxis.isLog()!=xlog ) {
                getParent().postMessage( this, "xaxis must be " + ( xlog ? "log" : "linear" ) + ", for this image",  Level.INFO, null, null );
                return;
            }
        }
        {
            int n= dep1.length();
            double dy1= ( dep1.value(1)- dep1.value(0) );
            double dy2= ( dep1.value(n-1)- dep1.value(n-2) );
            boolean ylog= dy1<dy2/10;
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
        int x0= (int)xAxis.transform( dep0.value(0) - dx0/2, xunits);
        int y0= (int)yAxis.transform( dep1.value(0) - dy0/2, yunits);
        int x1= (int)xAxis.transform( dep0.value(w-1) + dx0/2, xunits );
        int y1= (int)yAxis.transform( dep1.value(h-1) + dy0/2, yunits );
        if ( nearestNeighborInterpolation ) {
            ((Graphics2D)g).setRenderingHint( RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR );
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
        if (ds.rank() == 2) {
            imageType = BufferedImage.TYPE_BYTE_GRAY;
        } else if (ds.rank() == 3) {
            if (ds.length(0,0) == 3) {
                QDataSet dep2 = (QDataSet) ds.property(QDataSet.DEPEND_2);
                imageType = BufferedImage.TYPE_INT_RGB; // default
                if (dep2 != null) {
                    String s0 = org.virbo.dataset.DataSetUtil.asDatum(dep2.slice(0)).toString().toLowerCase();
                    String s1 = org.virbo.dataset.DataSetUtil.asDatum(dep2.slice(1)).toString().toLowerCase();
                    String s2 = org.virbo.dataset.DataSetUtil.asDatum(dep2.slice(2)).toString().toLowerCase();
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
                    String s0 = org.virbo.dataset.DataSetUtil.asDatum(dep2.slice(0)).toString().toLowerCase();
                    String s1 = org.virbo.dataset.DataSetUtil.asDatum(dep2.slice(1)).toString().toLowerCase();
                    String s2 = org.virbo.dataset.DataSetUtil.asDatum(dep2.slice(2)).toString().toLowerCase();
                    String s3 = org.virbo.dataset.DataSetUtil.asDatum(dep2.slice(3)).toString().toLowerCase();
                    if (s0.startsWith("a") && s1.startsWith("r") && s2.startsWith("g") && s3.startsWith("b")) {
                        imageType = BufferedImage.TYPE_INT_ARGB;
                    } else if ( s0.startsWith("a") && s1.startsWith("b") && s2.startsWith("g") && s3.startsWith("r") ) {
                        imageType = BufferedImage.TYPE_4BYTE_ABGR;
                    }
                }
            }
        } else {
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
                    if (ds.length(0,0) == 4) {
                        v = 16777216 * (int) ds.value(i,j,0) + 65536 * (int) ds.value(i,j,1) + 256 * (int) ds.value(i,j,2) + (int) ds.value(i,j,3);
                    } else if (ds.length(0,0) == 3) {
                        v = 65536 * (int) ds.value(i,j,0) + 256 * (int) ds.value(i,j,1) + (int) ds.value(i,j,2);
                    } else {
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
        if ( ds.rank()==2 ) {
            return !SemanticOps.isBundle(ds);
        } else if ( ds.rank()==3 ) {
            return ds.length(0,0)>2 && ds.length(0,0)<5;
        } else {
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
        return rect;
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
     * @see org.virbo.dataset.examples.Schemes#boundingBox() 
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
