/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.graph;

import javax.swing.ImageIcon;
import javax.swing.Icon;
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
import static java.lang.Math.*;
import org.virbo.dataset.ArrayDataSet;

/**
 *
 * @author jbf
 */
public class PitchAngleDistributionRenderer extends Renderer {

    public PitchAngleDistributionRenderer( DasColorBar cb ) {
        setColorBar(cb);
    }

    /**
     * accepts data that is rank 2 and not a timeseries.  Angles
     * may be in radians or in Units.degrees.  
     * ds[Energy,Pitch] or ds[Pitch,Energy] where Pitch is in:
     *   Units.degrees, Units.radians, or dimensionless and -2*PI to 2*PI.
     * @param ds
     * @return
     */
    public static boolean acceptsData( QDataSet ds ) {
        if ( ds.rank()==2 ) {
            if ( SemanticOps.isTimeSeries(ds) ) return false;
            if ( SemanticOps.isBundle(ds) ) return false;
            QDataSet yds= SemanticOps.ytagsDataSet(ds);
            QDataSet xds= SemanticOps.xtagsDataSet(ds);
            if ( SemanticOps.getUnits(yds)==Units.degrees || SemanticOps.getUnits(xds)==Units.degrees || SemanticOps.getUnits(yds)==Units.radians || SemanticOps.getUnits(xds)==Units.radians ) {
                return true;
            } else {
                QDataSet extent= Ops.extent(yds);
                if ( extent.value(0)<-2*PI || extent.value(1)>2*PI ) {
                    return false;
                } else {
                    return true;
                }
            }
        } else {
            return false;
        }
    }

    PropertyChangeListener rebinListener= new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent e) {
            update();
            updateCacheImage();
        }
    };

    @Override
    public Icon getListIcon() {
        return new ImageIcon(SpectrogramRenderer.class.getResource("/images/icons/pitchAngleDistribution.png"));
    }


    @Override
    public final void setColorBar(DasColorBar colorBar) {
        DasColorBar oldColorBar = this.colorBar;
        if ( this.colorBar!=null ) {
            this.colorBar.removePropertyChangeListener("dataMinimum", rebinListener);
            this.colorBar.removePropertyChangeListener("dataMaximum", rebinListener);
            this.colorBar.removePropertyChangeListener("log", rebinListener);
            this.colorBar.removePropertyChangeListener(DasColorBar.PROPERTY_TYPE, rebinListener);
            this.colorBar.removePropertyChangeListener(DasColorBar.PROPERTY_FILL_COLOR, rebinListener);

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
        zdesc= Ops.putProperty( zdesc, QDataSet.SCALE_TYPE, tds.property(QDataSet.SCALE_TYPE ) );

        QDataSet ads= SemanticOps.xtagsDataSet(tds);
        QDataSet rds= SemanticOps.ytagsDataSet(tds); // this is why they are semanticOps.  ytagsDataSet is just used for convenience even though this is not the y values.

        Units xunits= SemanticOps.getUnits(ads);
        Units yunits= SemanticOps.getUnits(rds);

        if ( yunits.isConvertableTo(Units.degrees) && !xunits.isConvertableTo(Units.degrees) ) { // swap em
            rds= SemanticOps.xtagsDataSet(tds);
        }

        ArrayDataSet xdesc= DDataSet.wrap( new double[] { 0, Ops.extent(rds).value(1) }, yunits );
        ArrayDataSet ydesc= xdesc;

        xdesc= ArrayDataSet.maybeCopy( Ops.rescaleRangeLogLin( xdesc, -1.1, 1.1 ) );
        ydesc= ArrayDataSet.maybeCopy( Ops.rescaleRangeLogLin( ydesc, -1.1, 1.1 ) );

        String l= rds.property(QDataSet.LABEL)==null ? "(Parallel)" : String.format( "%s (Parallel)", rds.property(QDataSet.LABEL));
        xdesc.putProperty( QDataSet.LABEL, l ) ;
        l= rds.property(QDataSet.LABEL)==null ? "(Perp)" : String.format( "%s (Perp)", rds.property(QDataSet.LABEL));
        ydesc.putProperty( QDataSet.LABEL, l ) ;
        
        JoinDataSet bds= new JoinDataSet(2);
        bds.join(xdesc);
        bds.join(ydesc);
        bds.join(zdesc);

        return bds;

    }


    @Override
    public void render(Graphics g1, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {

        QDataSet tds= (QDataSet)ds;
        DasPlot parent= getParent();
        if (tds == null) {
            logger.fine("null data set");
            parent.postMessage(this, "no data set", DasPlot.INFO, null, null);
            return;
        }

        if ( !( SemanticOps.isTableDataSet(tds) ) ) {
            parent.postException( this, new IllegalArgumentException("expected Table: " +tds ) );
            return;
        }

        if ( !xAxis.getUnits().isConvertableTo( yAxis.getUnits() ) ) {
            parent.postException( this,
                    new IllegalArgumentException("x and y axes have different units, x="
                    +xAxis.getUnits() + " y="+yAxis.getUnits()  ) );
            return;
        }
        
        Graphics2D g= (Graphics2D)g1;
        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

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

        ArrayDataSet damin= ArrayDataSet.copy(ads);
        ArrayDataSet damax= ArrayDataSet.copy(ads);
        for ( int i=0; i<damin.length(); i++ ) {
            if ( i==0 ) {
                damin.putValue( i, Math.max( 0., ads.value(i)-da) );
                damax.putValue( i, ( ads.value(i+1) + ads.value(i) ) / 2 );
            } else if ( i<damin.length()-1 ) {
                damin.putValue( i, ( ads.value(i-1) + ads.value(i) ) / 2 );
                damax.putValue( i, ( ads.value(i+1) + ads.value(i) ) / 2 );
            } else {
                damin.putValue( i, ( ads.value(i-1) + ads.value(i) ) / 2 );
                damax.putValue( i, Math.min( 180, ads.value(i)+da) );
            }
        }
        
        double x0= xAxis.transform(0,yunits);
        double y0= yAxis.transform(0,yunits);

        for ( int iflip=0; iflip<2; iflip++ ) {
            if ( !mirror && iflip==1 ) continue;
            for ( int j=0; j<rds.length()-1; j++ ) {
                double v1= rds.value( j ); // sure wish we'd been testing this so I'd know where the old code worked.
                double v2= rds.value( j+1 );
                double r0x= ( xAxis.transform(v1,yunits) ) - x0; // inner ring radius at y=0
                double r0y= y0 - ( yAxis.transform(v1,yunits) ); // inner ring radius at x=0, equal to r0x when isotropic (round)
                double r1x= ( xAxis.transform(v2,yunits) ) - x0; // outer ring radius at y=0
                double r1y= y0 - ( yAxis.transform(v2,yunits) ); // outer ring radius at x=0, equal to r1x when isotropic (round)
                
                for ( int i=0; i<ads.length(); i++ ) {
                    double a0= damin.value(i);
                    double a1= damax.value(i);
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
        DasPlot parent= getParent();
        if (parent != null && parent.getCanvas() != null) {
            if (colorBar != null) {
                parent.getCanvas().add(colorBar, parent.getRow(), colorBar.getColumn());
            }
        }
    }

    protected void uninstallRenderer() {
//        if (colorBar != null && colorBar.getCanvas() != null) {
//            colorBar.getCanvas().remove(colorBar);
//        }
    }
    

    @Override
    public void setControl(String s) {
        super.setControl(s);
        this.mirror= getBooleanControl( "mirror", mirror );
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
