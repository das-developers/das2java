/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.graph;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import org.das2.datum.Units;
import org.das2.system.DasLogger;
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

    @Override
    public void render(Graphics g, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {
        QDataSet ds= getDataSet();
        QDataSet dep0;
        QDataSet dep1;

        if ( ds==null || ds.length() == 0) {
            DasLogger.getLogger(DasLogger.GRAPHICS_LOG).fine("null data set");
            return;
        }

        BufferedImage im= image; // make local copy for thread safety
        if ( im==null ) return; // transitional state

        dep0= (QDataSet)ds.property(QDataSet.DEPEND_0);
        dep1= (QDataSet)ds.property(QDataSet.DEPEND_1);

        if ( dep0==null ) dep0= Ops.dindgen(im.getWidth());
        if ( dep1==null ) dep1= Ops.dindgen(im.getHeight());
        
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
        g.drawImage( im, x0, y0, x1-x0, y1-y0, null );
        
    }

    @Override
    public void setDataSet(QDataSet ds) {
        super.setDataSet(ds);
        if ( ds==null ) {
            image=null;
        } else {
            image= getImage(ds);
        }
        refresh();
    }

    
    private BufferedImage getImage(QDataSet ds) throws IllegalArgumentException {
        // ds should be a rank 2 gray scale of Width x Height values, valued from 0-255,
        // or a rank 3 color image 3,W,H.
        int imageType = -19999;
        int w = 0;
        int h = 0;

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
     * autorange on the data, returning a rank 2 bounds for the dataset.
     *
     * @param fillDs
     * @return
     */
    public static QDataSet doAutorange( QDataSet ds ) {

        QDataSet xds;
        QDataSet yds;

        QDataSet xrange;
        QDataSet yrange;

        if ( ds.rank()==2 ) {
            xrange= DDataSet.wrap( new double[] { 0, ds.length() } );
            yrange= DDataSet.wrap( new double[] { 0, ds.length(0) } );
        } else if ( ds.rank()==3 ) {
            xrange= DDataSet.wrap( new double[] { 0, ds.length() } );
            yrange= DDataSet.wrap( new double[] { 0, ds.length(0) } );
        } else {
            throw new IllegalArgumentException("dataset should be rank 2 or rank 3: "+ds );
        }

        JoinDataSet bds= new JoinDataSet(2);
        bds.join(xrange);
        bds.join(yrange);

        return bds;

    }
}
