/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.graph;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;
import org.das2.dataset.TableDataSet;
import org.das2.datum.Units;
import org.das2.util.monitor.ProgressMonitor;
import static java.lang.Math.*;

/**
 *
 * @author jbf
 */
public class PitchAngleDistributionRenderer extends Renderer {

    DasColorBar cb;

    protected DasColorBar colorBar = null;
    public static final String PROP_COLORBAR = "colorBar";

    public DasColorBar getColorBar() {
        return colorBar;
    }

    public void setColorBar(DasColorBar colorBar) {
        DasColorBar oldColorBar = this.colorBar;
        this.colorBar = colorBar;
        propertyChangeSupport.firePropertyChange(PROP_COLORBAR, oldColorBar, colorBar);
    }

    @Override
    public void render(Graphics g, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {
        TableDataSet tds= (TableDataSet)ds;

        double radMin=0;

        double[][] xx= new double[ tds.getXLength()+1 ] [ tds.getYLength(0)+1 ];
        double[][] yy= new double[ tds.getXLength()+1 ] [ tds.getYLength(0)+1 ];


        Units xunits= Units.dimensionless;  // should be rad
        Units yunits= Units.dimensionless;  // should be eV
        Units zunits= tds.getZUnits();

        double da= tds.getXTagDouble(1,xunits) - tds.getXTagDouble(0,xunits);

        double x0= xAxis.transform(0,Units.dimensionless);
        double y0= yAxis.transform(0,Units.dimensionless);

        for ( int j=0; j<tds.getYLength(0); j++ ) {
            double v1= tds.getYTagDouble(0, j,yunits);
            double dy= Math.abs( yAxis.transform(v1,yunits) - y0 );
            for ( int i=0; i<tds.getXLength(); i++ ) {
                double a1= tds.getXTagDouble(i,xunits);
                xx[i][j]= x0 + sin(a1-da) * dy;
                yy[i][j]= y0 + cos(a1-da) * dy;
                xx[i+1][j+1]= x0 + sin(a1+da) * dy;
                yy[i+1][j+1]= y0 + cos(a1+da) * dy;
                int zz= colorBar.indexColorTransform( tds.getDouble(i, j, zunits ), zunits );
                int[] x= new int [] {(int) xx[i][j], (int)xx[i][j], (int)xx[i+1][j], (int)xx[i+1][j], (int)xx[i][j] };
                int[] y= new int [] { (int)yy[i][j], (int)yy[i][j+1], (int)yy[i][j+1], (int)yy[i][j], (int)yy[i][j] };
                Polygon p= new Polygon( x, y, 5 );
                g.setColor( new Color(zz) );
                g.fillPolygon(p);

            }
        }

    }

}
