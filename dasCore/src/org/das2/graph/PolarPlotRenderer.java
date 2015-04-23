package org.das2.graph;

import java.awt.BasicStroke;
import javax.swing.ImageIcon;
import javax.swing.Icon;
import org.virbo.dataset.DDataSet;
import org.virbo.dataset.JoinDataSet;
import org.virbo.dsops.Ops;
import org.virbo.dataset.SemanticOps;
import org.virbo.dataset.QDataSet;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.das2.datum.Units;
import org.das2.util.monitor.ProgressMonitor;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import java.util.LinkedHashMap;
import java.util.Map;
import org.das2.datum.Datum;
import org.das2.datum.DatumVector;
import org.das2.datum.format.DatumFormatter;
import org.virbo.dataset.ArrayDataSet;

/**
 * Draws a pitch angle distribution, which is a spectrogram wrapped around an origin.  Datasets must
 * be of the form Z[Angle,Radius].  The dataset Angle must be in radians (Units.radians or dimensionless), or 
 * have units Units.degrees.  Note, at one time it would guess the angles dimension, but this was unreliable and was
 * removed.
 * 
 * @author jbf
 */
public class PolarPlotRenderer extends Renderer {

    public PolarPlotRenderer( DasColorBar cb ) {
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
        } else if ( ds.rank()==1 ) {
            return true;
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
   
    private static QDataSet doAutorangeRank1(QDataSet rds ) {
        
        Units yunits= SemanticOps.getUnits(rds);
        
        ArrayDataSet xdesc= DDataSet.wrap( new double[] { 0, Ops.extent(rds).value(1) }, yunits );
        ArrayDataSet ydesc= xdesc;

        xdesc= ArrayDataSet.maybeCopy( Ops.rescaleRangeLogLin( xdesc, -1.1, 1.1 ) );
        ydesc= ArrayDataSet.maybeCopy( Ops.rescaleRangeLogLin( ydesc, -1.1, 1.1 ) );
        
        JoinDataSet bds= new JoinDataSet(2);
        bds.join(xdesc);
        bds.join(ydesc);

        return bds;
    }
    
    public static QDataSet doAutorange(QDataSet tds) {

        if ( tds.rank()==1 ) {
            return doAutorangeRank1(tds);
        }
        
        QDataSet zdesc = Ops.extent( tds );
        if ( zdesc.value(0)==zdesc.value(1) ) {
            if ( zdesc.value(0)>0 ) {
                zdesc= DDataSet.wrap( new double[] { 0, zdesc.value(1) } );
                zdesc= Ops.putProperty( zdesc, QDataSet.UNITS, tds.property(QDataSet.UNITS) );
            } else {
                zdesc= DDataSet.wrap( new double[] { 0, 1 } );
                zdesc= Ops.putProperty( zdesc, QDataSet.UNITS, tds.property(QDataSet.UNITS) );
            }
        }
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

        JoinDataSet bds= new JoinDataSet(2);
        bds.join(xdesc);
        bds.join(ydesc);
        bds.join(zdesc);

        return bds;

    }

    private void renderRank1( Graphics g1, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {

        Graphics2D g= (Graphics2D)g1;
        
        QDataSet ads= SemanticOps.xtagsDataSet(ds);
        QDataSet rds= SemanticOps.ytagsDataSet(ds); // this is why they are semanticOps.  ytagsDataSet is just used for convenience even though this is not the y values.

        Double angleFactor= isAngleRange(ads);
        
        double x= rds.value(0) * cos( ads.value(0) * angleFactor );
        double y= rds.value(0) * sin( ads.value(0) * angleFactor );
        
        GeneralPath gp= new GeneralPath();
        
        Units xunits= xAxis.getUnits();
        Units yunits= yAxis.getUnits();
        
        gp.moveTo( xAxis.transform( x, xunits ), yAxis.transform( y, yunits ) );
        
        for ( int i=1; i<ads.length(); i++ ) {
            x= rds.value(i) * cos( ads.value(i) * angleFactor );
            y= rds.value(i) * sin( ads.value(i) * angleFactor );
        
            gp.lineTo( xAxis.transform( x, xunits ), yAxis.transform( y, yunits ) );
        }
        
        g.setColor( getColorControl("color",Color.black) );
        g.draw(gp);
        
    }
    
    private void renderRank2( Graphics2D g, DasAxis xAxis, DasAxis yAxis, ProgressMonitor monitor ) {
        
        QDataSet tds= (QDataSet)ds;
        
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
        if ( angleFactor==null ) {
            throw new IllegalArgumentException("neither dimension appears to be angles");
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

                    if ( clockwise ) {
                        a0= -a0;
                        a1= -a1;
                    }
                    
                    if ( iflip==1 ) {
                        a0= -a0;
                        a1= -a1;
                    }                    
                    
                    if ( origin.length()>0 ) {
                        if ( origin.equalsIgnoreCase("N") ) {
                            a0= a0+Math.PI/2;
                            a1= a1+Math.PI/2;
                        } else if ( origin.equalsIgnoreCase("E") ) {
                            
                        } else if ( origin.equalsIgnoreCase("S") ) {
                            a0= a0-Math.PI/2;
                            a1= a1-Math.PI/2;
                        } else if ( origin.equalsIgnoreCase("W") ) {
                            a0= a0+Math.PI;
                            a1= a1+Math.PI;                            
                        } 
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
    public void render(Graphics g1, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {

        QDataSet tds= (QDataSet)ds;
        Graphics2D g= (Graphics2D)g1;
                
        DasPlot parent= getParent();
        if (tds == null) {
            logger.fine("null data set");
            parent.postMessage(this, "no data set", DasPlot.INFO, null, null);
            return;
        }

        if ( tds.rank()==1 ) {
            renderRank1( g1, xAxis, yAxis, mon );
            
        } else {
        
            if ( !( SemanticOps.isTableDataSet(tds) ) ) {
                parent.postException( this, new IllegalArgumentException("expected Table: " +tds ) );
                return;
            }

            if ( !xAxis.getUnits().isConvertibleTo( yAxis.getUnits() ) ) {
                parent.postException( this,
                        new IllegalArgumentException("x and y axes have different units, x="
                        +xAxis.getUnits() + " y="+yAxis.getUnits()  ) );
                return;
            }

            g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

            renderRank2( g, xAxis, yAxis, mon );

        } 
        
        if ( drawPolarAxes ) {
            
            Units yunits= yAxis.getUnits();
            
            double x0= xAxis.transform(0,yunits);
            double y0= yAxis.transform(0,yunits);
        
            Font f0= g.getFont();
            g.setFont( f0.deriveFont( f0.getSize()/2.f ) );
            g.setColor( xAxis.getForeground() );
            g.setStroke( new BasicStroke(0.4f) );
            int dx= f0.getSize()/6;
            DatumFormatter df= xAxis.getDatumFormatter();
            TickVDescriptor rr= xAxis.getTickV();
            DatumVector ticks= rr.tickV;
            Units u= ticks.getUnits();
            for ( int i=0; i<ticks.getLength(); i++ ) {
                Datum t= ticks.get(i);
                if ( t.doubleValue(ticks.getUnits())>0 ) {
                    double xc0= xAxis.transform(t.multiply(-1));
                    double yc0= yAxis.transform(t);
                    double xc1= xAxis.transform(t);
                    double yc1= yAxis.transform(t.multiply(-1));
                    g.drawOval((int)xc0,(int)yc0,(int)(xc1-xc0),(int)(yc1-yc0));
                    if ( !xAxis.isVisible() ) {
                        g.drawString(df.format(t,xAxis.getUnits()), (int)xc1+dx, (int)y0-dx );
                        g.drawString(df.format(t,xAxis.getUnits()), (int)xc0+dx, (int)y0-dx );
                    }
                }
            }
            
            ticks= rr.minorTickV;
            for ( int i=0; i<ticks.getLength(); i++ ) {
                Datum t= ticks.get(i);
                if ( t.doubleValue(ticks.getUnits())>0 ) {
                    double xc0= xAxis.transform(t.multiply(-1));
                    double yc0= yAxis.transform(t);
                    double xc1= xAxis.transform(t);
                    double yc1= yAxis.transform(t.multiply(-1));
                    g.drawLine( (int)xc0,(int)y0-1,(int)(xc0),(int)(y0+1) );
                    g.drawLine( (int)xc1,(int)y0-1,(int)(xc1),(int)(y0+1) );
                    g.drawLine( (int)x0-1,(int)yc0,(int)(x0+1),(int)(yc0) );
                    g.drawLine( (int)x0-1,(int)yc1,(int)(x0+1),(int)(yc1) );
                }
            }
            g.setFont(f0);
            Datum rmax= ticks.get(0).abs();
            Datum rmax1=ticks.get(ticks.getLength()-1);
            if ( rmax.lt(rmax1) ) rmax= rmax1;
            for ( int i=0; i<360; i+=30 ) {
                double xr0= xAxis.transform(0,u);
                double yr0= yAxis.transform(0,u);
                double xr1= xAxis.transform(rmax.value()*Math.cos(i*Math.PI/180),u);
                double yr1= yAxis.transform(rmax.value()*Math.sin(i*Math.PI/180),u);
                g.drawLine((int)xr0,(int)yr0,(int)xr1,(int)yr1);
            }
            if ( originNorth ) {
                Point2D p1= new Point2D.Double( xAxis.transform(0,u), yAxis.transform(0,u) );
                Point2D p0= new Point2D.Double( xAxis.transform(0,u), yAxis.getRow().getDMinimum() );
                Arrow.paintArrow( g, p0, p1, 10.0, Arrow.HeadStyle.DRAFTING );                
            } else if ( origin.length()>0 ) {
                if ( origin.equalsIgnoreCase("N") ) {
                    Point2D p1= new Point2D.Double( xAxis.transform(0,u), yAxis.transform(0,u) );
                    Point2D p0= new Point2D.Double( xAxis.transform(0,u), yAxis.getRow().getDMinimum() );
                    Arrow.paintArrow( g, p0, p1, 10.0, Arrow.HeadStyle.DRAFTING );              
                } else if ( origin.equalsIgnoreCase("S") ) {
                    Point2D p1= new Point2D.Double( xAxis.transform(0,u), yAxis.transform(0,u) );
                    Point2D p0= new Point2D.Double( xAxis.transform(0,u), yAxis.getRow().getDMaximum() );
                    Arrow.paintArrow( g, p0, p1, 10.0, Arrow.HeadStyle.DRAFTING );              
                } else if ( origin.equalsIgnoreCase("W") ) {
                    Point2D p1= new Point2D.Double( xAxis.transform(0,u), yAxis.transform(0,u) );
                    Point2D p0= new Point2D.Double( xAxis.getColumn().getDMinimum(), yAxis.transform(0,u) );
                    Arrow.paintArrow( g, p0, p1, 10.0, Arrow.HeadStyle.DRAFTING );                                  
                } else if ( origin.equalsIgnoreCase("E") ) {
                    Point2D p1= new Point2D.Double( xAxis.transform(0,u), yAxis.transform(0,u) );
                    Point2D p0= new Point2D.Double( xAxis.getColumn().getDMaximum(), yAxis.transform(0,u) );
                    Arrow.paintArrow( g, p0, p1, 10.0, Arrow.HeadStyle.DRAFTING );             
                }
            } else {
                Point2D p1= new Point2D.Double( xAxis.transform(0,u), yAxis.transform(0,u) );
                Point2D p0= new Point2D.Double( xAxis.getColumn().getDMaximum(), yAxis.transform(0,u) );
                Arrow.paintArrow( g, p0, p1, 10.0, Arrow.HeadStyle.DRAFTING );
            }
            if ( !xAxis.isVisible() ) {
                
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
        controls.put( "mirror", encodeBooleanControl( mirror ) );
        controls.put( "originNorth", encodeBooleanControl( originNorth ) );
        controls.put( "drawPolarAxes", encodeBooleanControl( drawPolarAxes ) );
        if ( origin.length()>0 && origin.equalsIgnoreCase("E") ) {
            controls.put( "origin", origin );
            controls.remove("originNorth");
        }
        if ( clockwise ) controls.put("clockwise", "T" );
        return Renderer.formatControl(controls);
    }
    

    
    @Override
    public void setControl(String s) {
        super.setControl(s);
        this.mirror= getBooleanControl( "mirror", false );
        this.originNorth= getBooleanControl("originNorth", false );
        this.drawPolarAxes= getBooleanControl("drawPolarAxes",false );
        this.origin= getControl("origin","");
        this.clockwise= getBooleanControl("clockwise",false);
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
     * true means increasing angle goes in the clockwise direction.
     */
    private boolean clockwise = false;
    
    public static final String PROP_CLOCKWISE = "clockwise";

    /**
     * true if increasing angle corresponds to clockwise when not mirror.
     * @return true if increasing angle corresponds to clockwise
     */
    public boolean isClockwise() {
        return clockwise;
    }

    /**
     * true if increasing angle corresponds to clockwise when not mirror.
     * @param clockwise true if increasing angle corresponds to clockwise
     */
    public void setClockwise(boolean clockwise) {
        boolean oldClockwise = this.clockwise;
        this.clockwise = clockwise;
        propertyChangeSupport.firePropertyChange(PROP_CLOCKWISE, oldClockwise, clockwise);
        update();
    }

    /**
     * One of "", "N", "S", "E", "W"
     */
    public static final String PROP_ORIGIN = "origin";
    
    protected String origin = ""; // see setControl.

    public String getOrigin() {
        return origin;
    }

    public void setOrigin( String origin ) {
        String oldOrigin = this.origin;
        this.origin = origin;
        propertyChangeSupport.firePropertyChange(PROP_ORIGIN, oldOrigin, origin);
        update();
    }    
    
    /**
     * if true, then draw circular axes.
     */
    private boolean drawPolarAxes = false;
    
    public static final String PROP_DRAWPOLARAXES = "drawPolarAxes";

    public boolean isDrawPolarAxes() {
        return drawPolarAxes;
    }

    public void setDrawPolarAxes(boolean drawPolarAxes) {
        boolean oldDrawPolarAxes = this.drawPolarAxes;
        this.drawPolarAxes = drawPolarAxes;
        propertyChangeSupport.firePropertyChange(PROP_DRAWPOLARAXES, oldDrawPolarAxes, drawPolarAxes);
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
