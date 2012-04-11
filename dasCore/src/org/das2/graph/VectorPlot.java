/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.graph;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import org.das2.datum.Units;
import org.das2.util.monitor.ProgressMonitor;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;

/**
 *
 * @author jbf
 */
public class VectorPlot extends Renderer {

    @Override
    public void render(Graphics g1, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {

        Graphics2D g= (Graphics2D)g1;

        QDataSet ds= getDataSet();

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

        double scale= 1;

        for ( int i=0; i<ds.length(); i++ ) {
            double ix= xAxis.transform( x.value(i), xunits );
            double iy= yAxis.transform( y.value(i), yunits );

            double idx= xAxis.transform( x.value(i) + dx.value(i) * scale, xunits );
            double idy= yAxis.transform( y.value(i) + dy.value(i) * scale, yunits );

            Arrow.paintArrow( g, new Point((int)idx,(int)idy),  new Point((int)ix,(int)iy), 10., Arrow.HeadStyle.DRAFTING );
            
        }

    }

}
