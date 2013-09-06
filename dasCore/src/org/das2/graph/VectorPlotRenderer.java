/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.graph;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.logging.Level;
import org.das2.datum.Units;
import org.das2.util.monitor.ProgressMonitor;
import org.virbo.dataset.DDataSet;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.JoinDataSet;
import org.virbo.dataset.MutablePropertyDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;
import org.virbo.dsops.Ops;

/**
 *
 * @author jbf
 */
public class VectorPlotRenderer extends Renderer {

    public static boolean acceptsData( QDataSet ds ) {
        if ( ds.rank()!=2 ) {
            return false;
        } else {
            return ds.length()==0 || ds.length(0)==4;
        }
    }

    /**
     * autorange on the data, returning a rank 2 bounds for the dataset.
     *
     * @param fillDs
     * @return
     */
    public static QDataSet doAutorange( QDataSet ds ) {
        
        QDataSet xrange= doRange( DataSetOps.unbundle(ds,0), DataSetOps.unbundle(ds,2) );
        QDataSet yrange= doRange( DataSetOps.unbundle(ds,1), DataSetOps.unbundle(ds,3) );

        JoinDataSet bds= new JoinDataSet(2);
        bds.join(xrange);
        bds.join(yrange);

        return bds;

    }

    private static QDataSet doRange( QDataSet xds, QDataSet dxds ) {

        double scale= 1.0; 

        QDataSet xrange= Ops.extent(xds);

        MutablePropertyDataSet vxds= (MutablePropertyDataSet)Ops.multiply(dxds,DataSetUtil.asDataSet(scale));
        vxds.putProperty(QDataSet.UNITS, xds.property( QDataSet.UNITS ) );
        xrange= Ops.extent( Ops.add(xds,vxds), xrange );

        if ( xrange.value(1)==xrange.value(0) ) {
            if ( !"log".equals( xrange.property(QDataSet.SCALE_TYPE)) ) {
                xrange= DDataSet.wrap( new double[] { xrange.value(0)-1, xrange.value(1)+1 } ).setUnits( SemanticOps.getUnits(xrange) );
            } else {
                xrange= DDataSet.wrap( new double[] { xrange.value(0)/10, xrange.value(1)*10 } ).setUnits( SemanticOps.getUnits(xrange) );
            }
        }
        xrange= Ops.rescaleRangeLogLin( xrange, -0.1, 1.1 );
        xrange= Ops.rescaleRangeLogLin( xrange, -0.1, 1.1 );
        return xrange;
    }

    protected double scale = 1.0;
    public static final String PROP_SCALE = "scale";

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        double oldScale = this.scale;
        this.scale = scale;
        refresh();
        propertyChangeSupport.firePropertyChange(PROP_SCALE, oldScale, scale);
    }

    @Override
    public void render(Graphics g1, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {

        Graphics2D g= (Graphics2D)g1;

        QDataSet ds= getDataSet();

        if ( ds==null ) {
            if ( getLastException()!=null ) {
                renderException(g, xAxis, yAxis, lastException);
            } else {
                parent.postMessage(this, "no data set", DasPlot.INFO, null, null);
            }
            return;
        }

        if ( ds.length()==0 ) {
            parent.postException( this, new IllegalArgumentException("no data to plot") );
            return;
        }

        if ( ds.rank()!=2 || ds.length(0)!=4 ) {
            parent.postException( this, new IllegalArgumentException("plot must be rank 2 and be of the form ds[:,4]") );
            return;
        }

        QDataSet x= DataSetOps.unbundle(ds,0);
        QDataSet y= DataSetOps.unbundle(ds,1);
        QDataSet dx= DataSetOps.unbundle(ds,2);
        QDataSet dy= DataSetOps.unbundle(ds,3);

        Units xunits= SemanticOps.getUnits( x );
        Units yunits= SemanticOps.getUnits( y );

        QDataSet wdx= SemanticOps.weightsDataSet(dx);
        QDataSet wdy= SemanticOps.weightsDataSet(dy);
        QDataSet wx= SemanticOps.weightsDataSet(x);
        QDataSet wy= SemanticOps.weightsDataSet(y);

        for ( int i=0; i<ds.length(); i++ ) {
            if ( wdx.value(i) * wdy.value(i) * wx.value(i) * wy.value(i)==0 ) continue;
            
            double ix= xAxis.transform( x.value(i), xunits );
            double iy= yAxis.transform( y.value(i), yunits );

            double idx= xAxis.transform( x.value(i) + dx.value(i) * scale, xunits );
            double idy= yAxis.transform( y.value(i) + dy.value(i) * scale, yunits );

            Arrow.paintArrow( g, new Point((int)idx,(int)idy),  new Point((int)ix,(int)iy), 10., Arrow.HeadStyle.DRAFTING );
            
        }

    }

}
