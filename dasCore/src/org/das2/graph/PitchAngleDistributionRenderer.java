/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.graph;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.das2.dataset.TableDataSet;
import org.das2.datum.Units;
import org.das2.util.monitor.ProgressMonitor;
import static java.lang.Math.*;

/**
 *
 * @author jbf
 */
public class PitchAngleDistributionRenderer extends Renderer {

    public PitchAngleDistributionRenderer( DasColorBar cb ) {
        setColorBar(cb);
    }
    
    DasColorBar cb;

    protected DasColorBar colorBar = null;
    public static final String PROP_COLORBAR = "colorBar";

    public DasColorBar getColorBar() {
        return colorBar;
    }

    PropertyChangeListener rebinListener= new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent e) {
            update();
            refreshImage();
        }
    };

    public void setColorBar(DasColorBar colorBar) {
        DasColorBar oldColorBar = this.colorBar;
        if ( this.colorBar!=null ) {
            colorBar.removePropertyChangeListener("dataMinimum", rebinListener);
            colorBar.removePropertyChangeListener("dataMaximum", rebinListener);
            colorBar.removePropertyChangeListener("log", rebinListener);
            colorBar.removePropertyChangeListener(DasColorBar.PROPERTY_TYPE, rebinListener);
            colorBar.removePropertyChangeListener(DasColorBar.PROPERTY_FILL_COLOR, rebinListener);

        }
        this.colorBar = colorBar;
        if (this.colorBar != null) {
            colorBar.addPropertyChangeListener("dataMinimum", rebinListener);
            colorBar.addPropertyChangeListener("dataMaximum", rebinListener);
            colorBar.addPropertyChangeListener("log", rebinListener);
            colorBar.addPropertyChangeListener(DasColorBar.PROPERTY_TYPE, rebinListener);
            colorBar.addPropertyChangeListener(DasColorBar.PROPERTY_FILL_COLOR, rebinListener);
        }
        propertyChangeSupport.firePropertyChange(PROP_COLORBAR, oldColorBar, colorBar);
    }

    @Override
    public void render(Graphics g1, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {
        TableDataSet tds= (TableDataSet)ds;
        Graphics2D g= (Graphics2D)g1;
        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

        if ( tds==null ) return;
        if ( colorBar==null ) return;
        
        float[][] xx= new float[ tds.getXLength()+1 ] [ tds.getYLength(0)+1 ];
        float[][] yy= new float[ tds.getXLength()+1 ] [ tds.getYLength(0)+1 ];

        Units xunits= Units.radians;  // should be rad
        if ( ds.getXUnits()==Units.dimensionless ) {
            xunits= Units.dimensionless;
        }
        Units yunits= Units.dimensionless;  // should be eV
        Units zunits= tds.getZUnits();

        //TODO: handle log energy, etc, by converting to linear axis.

        double da= ( tds.getXTagDouble(1,xunits) - tds.getXTagDouble(0,xunits) ) / 2;

        double x0= xAxis.transform(0,yunits);
        double y0= yAxis.transform(0,yunits);

        for ( int iflip=0; iflip<2; iflip++ ) {
            if ( !mirror && iflip==1 ) continue;
            for ( int j=0; j<tds.getYLength(0)-1; j++ ) {
                double v1= tds.getYTagDouble(0, j,yunits);
                double v2= tds.getYTagDouble(0, j+1,yunits);
                double r0= y0 - ( yAxis.transform(v1,yunits) ); // in
                double r1= y0 - ( yAxis.transform(v2,yunits) ); // out
                for ( int i=0; i<tds.getXLength(); i++ ) {
                    double a0= tds.getXTagDouble(i,xunits) - da;
                    double a1= tds.getXTagDouble(i,xunits) + da;
                    if ( iflip==1 ) {
                        a0= -a0;
                        a1= -a1;
                    }
                    if ( originNorth ) {
                        yy[i][j]= (float) ( y0 - cos(a0) * r0 );
                        xx[i][j]= (float) ( x0 - sin(a0) * r0 );
                        yy[i][j+1]= (float) ( y0 - cos(a0) * r1 );
                        xx[i][j+1]= (float) ( x0 - sin(a0) * r1 );
                        yy[i+1][j]= (float) ( y0 - cos(a1) * r0 );
                        xx[i+1][j]= (float) ( x0 - sin(a1) * r0 );
                        yy[i+1][j+1]= (float) ( y0 - cos(a1) * r1 );
                        xx[i+1][j+1]= (float) ( x0 - sin(a1) * r1 );
                    } else {
                        xx[i][j]= (float) ( x0 + cos(a0) * r0 );
                        yy[i][j]= (float) ( y0 - sin(a0) * r0 );
                        xx[i][j+1]= (float) ( x0 + cos(a0) * r1 );
                        yy[i][j+1]= (float) ( y0 - sin(a0) * r1 );
                        xx[i+1][j]= (float) ( x0 + cos(a1) * r0 );
                        yy[i+1][j]= (float) ( y0 - sin(a1) * r0 );
                        xx[i+1][j+1]= (float) ( x0 + cos(a1) * r1 );
                        yy[i+1][j+1]= (float) ( y0 - sin(a1) * r1 );
                    }
                    int zz= colorBar.rgbTransform( tds.getDouble(i, j, zunits ), zunits );
                    //int[] x= new int [] {(int) xx[i][j], (int)xx[i][j+1], (int)xx[i+1][j+1], (int)xx[i+1][j], (int)xx[i][j] };
                    //int[] y= new int [] { (int)yy[i][j], (int)yy[i][j+1], (int)yy[i+1][j+1], (int)yy[i+1][j], (int)yy[i][j] };
                    //int[] x= new int [] {(int) xx[i][j], (int)xx[i][j+1], (int)xx[i+1][j+1], (int)xx[i+1][j],};
                    //int[] y= new int [] { (int)yy[i][j], (int)yy[i][j+1], (int)yy[i+1][j+1], (int)yy[i+1][j]};
                    //Polygon p= new Polygon( x, y, 4 );
                    g.setColor( new Color(zz) );
                    //g.fillPolygon(p);

                    GeneralPath gp= new GeneralPath( GeneralPath.WIND_NON_ZERO,6);
                    gp.moveTo( xx[i][j], yy[i][j] );
                    gp.lineTo( xx[i][j+1], yy[i][j+1] );
                    gp.lineTo( xx[i+1][j+1], yy[i+1][j+1] );
                    gp.lineTo( xx[i+1][j], yy[i+1][j] );
                    gp.lineTo( xx[i][j], yy[i][j] );

                    g.fill(gp);
                    g.draw(gp);

                }
            }
        }

    }

    protected void installRenderer() {
        if (parent != null && parent.getCanvas() != null) {
            if (colorBar != null) {
                if (colorBar.getColumn() == DasColumn.NULL) {
                    DasColumn column = parent.getColumn();
                    colorBar.setColumn(new DasColumn(null, column, 1.0, 1.0, 1, 2, 0, 0));
                }
                parent.getCanvas().add(colorBar, parent.getRow(), colorBar.getColumn());
            }
        }
    }

    protected void uninstallRenderer() {
        if (colorBar != null && colorBar.getCanvas() != null) {
            colorBar.getCanvas().remove(colorBar);
        }
    }

    /**
     * if true, then angle=0 is in the positive Y direction, otherwise
     * it is in the positive x direction
     */
    public static final String PROP_ORIGINNORTH = "originNorth";
    protected boolean originNorth = false;

    public boolean isOriginNorth() {
        return originNorth;
    }

    public void setOriginNorth(boolean originNorth) {
        boolean oldOriginNorth = this.originNorth;
        this.originNorth = originNorth;
        propertyChangeSupport.firePropertyChange(PROP_ORIGINNORTH, oldOriginNorth, originNorth);
        update();
    }

    /**
     * if true, then mirror the image about angle=0.
     */
    protected boolean mirror = false;
    public static final String PROP_MIRROR = "mirror";

    public boolean isMirror() {
        return mirror;
    }

    public void setMirror(boolean mirror) {
        boolean oldMirror = this.mirror;
        this.mirror = mirror;
        propertyChangeSupport.firePropertyChange(PROP_MIRROR, oldMirror, mirror);
        update();
    }


}
