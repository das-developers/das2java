/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.graph;

import org.virbo.dataset.DDataSet;
import org.virbo.dataset.JoinDataSet;
import org.virbo.dsops.Ops;
import org.virbo.dataset.SemanticOps;
import org.virbo.dataset.QDataSet;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.das2.datum.Units;
import org.das2.util.monitor.ProgressMonitor;
import org.virbo.dataset.DataSetUtil;
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

    public final void setColorBar(DasColorBar colorBar) {
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

    public static QDataSet doAutorange(QDataSet tds) {

        QDataSet zdesc = Ops.extent( tds );

        QDataSet ads= SemanticOps.xtagsDataSet(tds);
        QDataSet rds= SemanticOps.ytagsDataSet(tds); // this is why they are semanticOps.  ytagsDataSet is just used for convenience even though this is not the y values.

        Units xunits= SemanticOps.getUnits(ads);
        Units yunits= SemanticOps.getUnits(rds);

        if ( yunits.isConvertableTo(Units.degrees) && !xunits.isConvertableTo(Units.degrees) ) { // swap em
            rds= SemanticOps.xtagsDataSet(tds);
            ads= SemanticOps.ytagsDataSet(tds);
            xunits= SemanticOps.getUnits(ads);
            yunits= SemanticOps.getUnits(rds);
        }

        QDataSet xdesc= DDataSet.wrap( new double[] { 0, Ops.extent(rds).value(1) } );
        QDataSet ydesc= xdesc;

        xdesc= Ops.rescaleRange( xdesc, -1.1, 1.1 );
        ydesc= Ops.rescaleRange( ydesc, -1.1, 1.1 );

        JoinDataSet bds= new JoinDataSet(2);
        bds.join(xdesc);
        bds.join(ydesc);
        bds.join(zdesc);

        return bds;

    }


    @Override
    public void render(Graphics g1, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {

        if (ds == null) {
            logger.fine("null data set");
            parent.postMessage(this, "no data set", DasPlot.INFO, null, null);
            return;
        }

        if ( !( SemanticOps.isTableDataSet(ds) ) ) {
            parent.postException( this, new IllegalArgumentException("expected Table: " +ds ) );
            return;
        }

        if ( !xAxis.getUnits().isConvertableTo( yAxis.getUnits() ) ) {
            parent.postException( this,
                    new IllegalArgumentException("x and y axes have different units, x="
                    +xAxis.getUnits() + " y="+yAxis.getUnits()  ) );
            return;
        }

        QDataSet tds= (QDataSet)ds;
        
        Graphics2D g= (Graphics2D)g1;
        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

        if ( tds==null ) return;
        if ( colorBar==null ) return;

        QDataSet ads= SemanticOps.xtagsDataSet(tds);
        QDataSet rds= SemanticOps.ytagsDataSet(tds); // this is why they are semanticOps.  ytagsDataSet is just used for convenience even though this is not the y values.

        Units xunits= SemanticOps.getUnits(ads);
        Units yunits= SemanticOps.getUnits(rds);

        if ( yunits.isConvertableTo(Units.degrees) && !xunits.isConvertableTo(Units.degrees) ) { // swap em
            rds= SemanticOps.xtagsDataSet(tds);
            ads= SemanticOps.ytagsDataSet(tds);
            xunits= SemanticOps.getUnits(ads);
            yunits= SemanticOps.getUnits(rds);
            tds= Ops.transpose(tds);
        }
        QDataSet wds= SemanticOps.weightsDataSet(tds);

        float[][] xx= new float[ tds.length()+1 ] [ tds.length(0)+1 ];
        float[][] yy= new float[ tds.length()+1 ] [ tds.length(0)+1 ];

        if ( SemanticOps.getUnits(ads)==Units.dimensionless ) {
            xunits= Units.radians;
        }
        Units zunits= SemanticOps.getUnits(tds);

        double da= ( ads.value(1) - ads.value(0) ) / 2;

        double x0= xAxis.transform(0,yunits);
        double y0= yAxis.transform(0,yunits);

        for ( int iflip=0; iflip<2; iflip++ ) {
            if ( !mirror && iflip==1 ) continue;
            for ( int j=0; j<rds.length()-1; j++ ) {
                double v1= rds.value( j ); // sure wish we'd been testing this so I'd know where the old code worked.
                double v2= rds.value( j+1 );
                double r0x= x0 - ( xAxis.transform(v1,yunits) ); // in
                double r0y= y0 - ( yAxis.transform(v1,yunits) ); // in
                double r1x= x0 - ( xAxis.transform(v2,yunits) ); // out
                double r1y= y0 - ( yAxis.transform(v2,yunits) ); // out
                for ( int i=0; i<ads.length(); i++ ) {
                    double a0= ads.value(i) - da;
                    double a1= ads.value(i) + da;
                    if ( iflip==1 ) {
                        a0= -a0;
                        a1= -a1;
                    }
                    if ( xunits==Units.degrees ) {
                        a0= Math.toRadians(a0);
                        a1= Math.toRadians(a1);
                    }
                    if ( originNorth ) {
                        yy[i][j]= (float) ( y0 - cos(a0) * r0y );
                        xx[i][j]= (float) ( x0 - sin(a0) * r0x );
                        yy[i][j+1]= (float) ( y0 - cos(a0) * r1y );
                        xx[i][j+1]= (float) ( x0 - sin(a0) * r1x );
                        yy[i+1][j]= (float) ( y0 - cos(a1) * r0y );
                        xx[i+1][j]= (float) ( x0 - sin(a1) * r0x );
                        yy[i+1][j+1]= (float) ( y0 - cos(a1) * r1y );
                        xx[i+1][j+1]= (float) ( x0 - sin(a1) * r1x );
                    } else {
                        xx[i][j]= (float) ( x0 + cos(a0) * r0x );
                        yy[i][j]= (float) ( y0 - sin(a0) * r0y );
                        xx[i][j+1]= (float) ( x0 + cos(a0) * r1x );
                        yy[i][j+1]= (float) ( y0 - sin(a0) * r1y );
                        xx[i+1][j]= (float) ( x0 + cos(a1) * r0x );
                        yy[i+1][j]= (float) ( y0 - sin(a1) * r0y );
                        xx[i+1][j+1]= (float) ( x0 + cos(a1) * r1x );
                        yy[i+1][j+1]= (float) ( y0 - sin(a1) * r1y );
                    }


                    if ( wds.value(i,j)>0 ) {
                        int zz= colorBar.rgbTransform( tds.value(i,j), zunits );
                        g.setColor( new Color(zz) );
                        GeneralPath gp= new GeneralPath( GeneralPath.WIND_NON_ZERO,6);
                        gp.moveTo( xx[i][j], yy[i][j] );
                        gp.lineTo( xx[i][j+1], yy[i][j+1] );
                        gp.lineTo( xx[i+1][j+1], yy[i+1][j+1] );
                        gp.lineTo( xx[i+1][j], yy[i+1][j] );
                        gp.lineTo( xx[i][j], yy[i][j] );

                        g.fill(gp);
                        g.draw(gp);

                    } else {
                        //g.setColor( Color.lightGray );
                    }


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
