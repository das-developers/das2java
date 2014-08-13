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
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import java.util.LinkedHashMap;
import java.util.Map;
import org.virbo.dataset.ArrayDataSet;

/**
 * Draws a pitch angle distribution, which is a spectrogram wrapped around an origin.  Datasets must
 * be of the form Z[Angle,Radius].  The dataset Angle must be in radians (Units.radians or dimensionless), or 
 * have units Units.degrees.  Note, at one time it would guess the angles dimension, but this was unreliable and was
 * removed.
 * 
 * @author jbf
 */
public class PitchAngleDistributionRenderer extends Renderer {

    public PitchAngleDistributionRenderer( DasColorBar cb ) {
        setColorBar(cb);
    }

    /**
     * return true if the dataset can be interpreted as radian degrees from 0 to PI or from 0 to 2*PI.
     * @param ds any QDataSet.
     * @return the multiplier to make the dataset into radians, or null.
     */
    private static Double isAngleRange( QDataSet ds ) {
        Units u= SemanticOps.getUnits(ds);
        if ( u==Units.radians ) return 1.;
        if ( u==Units.deg || u==Units.degrees ) return Math.PI/180;
        QDataSet extent= Ops.extent(ds);
        double delta= extent.value(1)-extent.value(0);
        if ( u==Units.dimensionless && ( delta>160 && delta<181 || delta>320 && delta<362 ) ) {
            return Math.PI/180;
        } else if ( u==Units.dimensionless && ( delta>Math.PI*160/180 && delta<Math.PI*181/180 || delta>Math.PI*320/180 && delta<Math.PI*362/180 ) ) {
            return 1.;
        } else {
            return null;
        }
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
            if ( isAngleRange(xds)!=null ) return true;
            if ( isAngleRange(yds)!=null ) return true;
            return false;
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

        Units yunits= SemanticOps.getUnits(rds);

        if ( isAngleRange(rds)!=null && isAngleRange(ads)==null ) { // swap em
            rds= SemanticOps.xtagsDataSet(tds);
            //ads= SemanticOps.ytagsDataSet(tds); // not used
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

        Units yunits= SemanticOps.getUnits(rds);

        Double angleFactor= isAngleRange(ads);
        if ( isAngleRange(rds)!=null && angleFactor==null ) { // swap em
            rds= SemanticOps.xtagsDataSet(tds);
            ads= SemanticOps.ytagsDataSet(tds);
            yunits= SemanticOps.getUnits(rds);
            tds= Ops.transpose(tds);            
            angleFactor= isAngleRange(ads);
        }
        if ( angleFactor!=1. ) {
            ads= Ops.multiply(ads,angleFactor);
        }
        // assert all angles are now in radians.
        
        QDataSet wds= SemanticOps.weightsDataSet(tds);

        float[][] xx= new float[ tds.length()+1 ] [ tds.length(0)+1 ];
        float[][] yy= new float[ tds.length()+1 ] [ tds.length(0)+1 ];
        //float[][] cx1=new float[ tds.length()+1 ] [ tds.length(0)+1 ];
        //float[][] cy1=new float[ tds.length()+1 ] [ tds.length(0)+1 ];
        //float[][] cx2=new float[ tds.length()+1 ] [ tds.length(0)+1 ];
        //float[][] cy2=new float[ tds.length()+1 ] [ tds.length(0)+1 ];        
        
        Units zunits= SemanticOps.getUnits(tds);

        double amin= Double.NEGATIVE_INFINITY;
        double amax= Double.POSITIVE_INFINITY;
        double da= ( ads.value(1) - ads.value(0) ) / 2;
        QDataSet rangea= Ops.extent(ads);
        if ( rangea.value(1) - rangea.value(0) < Math.PI * 3 / 2 ) {
            amin= Math.PI * (int)( ads.value(1)  / 180 );
            amax= Math.PI * ( 1 + (int)( ads.value(ads.length()-2) / 180 ) );
        }
        
        ArrayDataSet damin= ArrayDataSet.copy(ads);
        ArrayDataSet damax= ArrayDataSet.copy(ads);
        for ( int i=0; i<damin.length(); i++ ) {
            if ( i==0 ) {
                damin.putValue( i, Math.max( amin, ads.value(i)-da ) );
                damax.putValue( i, ( ads.value(i+1) + ads.value(i) ) / 2 );
            } else if ( i<damin.length()-1 ) {
                damin.putValue( i, ( ads.value(i-1) + ads.value(i) ) / 2 );
                damax.putValue( i, ( ads.value(i+1) + ads.value(i) ) / 2 );
            } else {
                damin.putValue( i, ( ads.value(i-1) + ads.value(i) ) / 2 );
                damax.putValue( i, Math.min( amax, ads.value(i)+da ) );
            }
        }
        
        double x0= xAxis.transform(0,yunits);
        double y0= yAxis.transform(0,yunits);

        //boolean useBelzier= true;
        for ( int iflip=0; iflip<2; iflip++ ) {
            if ( !mirror && iflip==1 ) continue;
            for ( int j=0; j<rds.length()-1; j++ ) {
                double v1= rds.value( j ); // sure wish we'd been testing this so I'd know where the old code worked.
                double v2= rds.value( j+1 );
                double r0x= ( xAxis.transform(v1,yunits) ) - x0; // inner ring radius at y=0
                double r0y= y0 - ( yAxis.transform(v1,yunits) ); // inner ring radius at x=0, equal to r0x when isotropic (round)
                double r1x= ( xAxis.transform(v2,yunits) ) - x0; // outer ring radius at y=0
                double r1y= y0 - ( yAxis.transform(v2,yunits) ); // outer ring radius at x=0, equal to r1x when isotropic (round)
                //double r= Math.sqrt( r1y*r1y + r1x*r1x );
                
                for ( int i=0; i<ads.length(); i++ ) {
                    double a0= damin.value(i);
                    double a1= damax.value(i);
                    if ( iflip==1 ) {
                        a0= -a0;
                        a1= -a1;
                    }                    
                    if ( originNorth ) {
                        yy[i][j]= (float) ( y0 - cos(a0) * r0y );
                        xx[i][j]= (float) ( x0 - sin(a0) * r0x );
                        yy[i][j+1]= (float) ( y0 - cos(a0) * r1y ); // outer
                        xx[i][j+1]= (float) ( x0 - sin(a0) * r1x ); // outer
                        yy[i+1][j]= (float) ( y0 - cos(a1) * r0y );
                        xx[i+1][j]= (float) ( x0 - sin(a1) * r0x );
                        yy[i+1][j+1]= (float) ( y0 - cos(a1) * r1y ); // outer
                        xx[i+1][j+1]= (float) ( x0 - sin(a1) * r1x ); // outer
//                        if ( useBelzier ) {
//                            double theta= a1-a0;
//                            double epsilon= 2 * sin(theta/2)* r / ( 1 + 2 * cos(theta/2) );  //http://www.tsplines.com/resources/class_notes/Bezier_curves.pdf, page 15
//                            cx1[i][j]= xx[i][j+1] + cos(  ) // Not completed!
//                        }
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
                        //gp.curveTo( 0,0, 0,0, xx[i+1][j+1], yy[i+1][j+1] );
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

    @Override
    protected void installRenderer() {
        DasPlot parent= getParent();
        if (parent != null && parent.getCanvas() != null) {
            if (colorBar != null) {
                parent.getCanvas().add(colorBar, parent.getRow(), colorBar.getColumn());
            }
        }
    }

    @Override
    protected void uninstallRenderer() {
//        if (colorBar != null && colorBar.getCanvas() != null) {
//            colorBar.getCanvas().remove(colorBar);
//        }
    }

    @Override
    public String getControl() {
        Map<String,String> controls= new LinkedHashMap();
        controls.put( "mirror", setBooleanControl( mirror ) );
        controls.put( "originNorth", setBooleanControl( originNorth ) );
        return Renderer.formatControl(controls);
    }
    

    
    @Override
    public void setControl(String s) {
        super.setControl(s);
        this.mirror= getBooleanControl( "mirror", false );
        this.originNorth= getBooleanControl("originNorth", false );
    }    

    /**
     * if true, then angle=0 is in the positive Y direction, otherwise
     * it is in the positive x direction
     */
    public static final String PROP_ORIGINNORTH = "originNorth";
    protected boolean originNorth = false; // see setControl, which must also be false.

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
    protected boolean mirror = false; // see setControl, which must also be false.
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
