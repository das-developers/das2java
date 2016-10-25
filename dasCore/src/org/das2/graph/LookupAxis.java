
package org.das2.graph;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumVector;
import org.das2.datum.DomainDivider;
import org.das2.datum.DomainDividerUtil;
import org.das2.datum.format.DatumFormatter;
import org.das2.util.GrannyTextRenderer;
import org.das2.util.LoggerManager;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;
import org.virbo.dsops.Ops;

/**
 * This is like a TCA, but is an axis which has ticks positioned by another axis.
 * @author jbf
 */
public class LookupAxis extends DasCanvasComponent {
    
    private static final Logger logger= LoggerManager.getLogger("org.das2.graph.lookupaxis" );
    /**
     * the width of the component.
     */
    int maxWidth;
    
    /**
     * the height of the component.
     */
    int maxHeight;
    
    DasAxis axis;
    
    QDataSet tt;
    QDataSet ff;
    
    
    public LookupAxis( DasAxis axis ) {
        this.maxWidth=100;
        this.axis= axis;
    }
    
    public void setDataSet( QDataSet yy ) {
        this.tt= SemanticOps.xtagsDataSet(yy);
        this.ff= yy;
    }

    public void setDataSet( QDataSet xx, QDataSet yy ) {
        this.tt= xx;
        this.ff= yy;
    }
    
    /**
     * set the axis.  This must be in units convertible to xx.
     * This must be a vertical axis with ticks on the left side, for now.
     * @param axis the axis.
     */
    public void setAxis( DasAxis axis ) {
        axis.addPropertyChangeListener( updateListener );
        this.axis= axis;
        
    }
    
    private final PropertyChangeListener updateListener= new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            LookupAxis.this.resize();
            LookupAxis.this.repaint();
        }
    };

    @Override
    protected void uninstallComponent() {
        if ( this.axis!=null ) {
            this.axis.removePropertyChangeListener(updateListener);
        }
    }
    
    
    private static QDataSet interpWow( Datum y, QDataSet xx, QDataSet yy ) {
        QDataSet zipzip= Ops.multiply( Ops.subtract(y, yy.trim(0,yy.length()-1) ), Ops.subtract( yy.trim(1,yy.length()), y ) );
        QDataSet r= Ops.where( Ops.ge( zipzip, 0 ) ); // find the points which bracket y.
        //QDataSet t1= Ops.subtract( y, DataSetOps.applyIndex( yy, 0, r, false ) );
        //QDataSet t0= DataSetOps.applyIndex( yy, Ops.add( r,1 ) );
        //QDataSet t2= Ops.subtract( DataSetOps.applyIndex( yy, Ops.add( r,1 ) ), DataSetOps.applyIndex( yy, r ) );
        QDataSet ff= Ops.divide( Ops.subtract( Ops.dataset(y),                            DataSetOps.applyIndex( yy,r ) ) , 
                                 Ops.subtract( DataSetOps.applyIndex( yy, Ops.add(r,1) ), DataSetOps.applyIndex( yy,r ) ) );
        return Ops.add( Ops.multiply( DataSetOps.applyIndex( xx,r ), Ops.subtract(1,ff) ), 
                        Ops.multiply( DataSetOps.applyIndex( xx,Ops.add(r,1)), ff ) );
    }
    
    @Override
    public void paintComponent( Graphics g ) {
        DatumVector ticks= axis.tickV.getMinorTicks();
        Datum fmin= Ops.datum(1e20);
        Datum fmax= Ops.datum(-1e20);

        if ( tt==null ) return;
        if ( ff==null ) return;

        if ( !SemanticOps.getUnits(tt).isConvertibleTo(ticks.getUnits() ) ) {
            tt= Ops.putProperty(tt, QDataSet.UNITS, ticks.getUnits() );
        }
        for ( int i=0; i<ticks.getLength(); i++ ) {
            Datum x= ticks.get(i);
            QDataSet f= Ops.findex(tt, Ops.dataset(x) );
            if ( f.value()>0 && f.value()<tt.length() ) {
                QDataSet ffint= Ops.interpolate(ff, f );
                if ( Ops.valid(ffint).value()!=0 ) {
                    fmin= Ops.datum( Ops.lesserOf( fmin, ffint ) );
                    fmax= Ops.datum( Ops.greaterOf( fmax, ffint ) );
                }
            }
        }
        if ( fmin.ge(fmax) ) {
            logger.fine("no ticks visible");
            return;
        }
        
        DomainDivider ytickvdd= DomainDividerUtil.getDomainDivider( Ops.datum(fmin),Ops.datum(fmax) );
        while (  ytickvdd.boundaryCount( fmin, fmax )<10 ) {
            DomainDivider dd= ytickvdd.finerDivider(false);
            if ( dd==null ) break;
            ytickvdd= dd;
        }
        while (  ytickvdd.boundaryCount( fmin, fmax )>20 ) {
            DomainDivider dd= ytickvdd.coarserDivider(false);
            if ( dd==null ) break;
            ytickvdd= dd;
        }

        ticks= ytickvdd.boundaries( fmin, fmax );

        g.setColor( Color.BLACK );
        int ascent= g.getFontMetrics().getAscent();
        int myY= this.getY();
        DatumRange dr= new DatumRange(fmin,fmax);
        DatumFormatter format= DomainDividerUtil.getDatumFormatter(ytickvdd,dr);

        maxWidth= 0;

        //draw the major ticks
        for ( int i=0; i<ticks.getLength(); i++ ) {
            Datum atick= ticks.get(i);
            QDataSet dds= interpWow(atick, tt, ff );
            int ix0= -999;
            for ( int j=0; j<dds.length(); j++ ) {
                QDataSet d = dds.slice(j);
                int ix = (int)axis.transform( d );
                if ( ix==ix0 ) continue;
                ix0= ix;
                g.drawLine( 0, ix-myY, 5, ix-myY );
                GrannyTextRenderer gtr= new GrannyTextRenderer( );
                gtr.setString( g, format.format( atick ) );
                gtr.draw( g, 5+3, ix+ascent/2-myY );
                int width0= (int)gtr.getWidth();
                if ( width0>maxWidth ) maxWidth=width0;
            }
        } 
        // draw the minor ticks
        ticks= ytickvdd.finerDivider( true ).boundaries( fmin, fmax );
        for (  int i=0; i<ticks.getLength(); i++ ) {
            g.setColor( Color.BLACK );
            myY= this.getY();
            Datum atick= ticks.get(i);
            QDataSet dds= interpWow(atick, tt, ff );
            int ix0= -999;
            for ( int j=0; j<dds.length(); j++ ) {
                QDataSet d = dds.slice(j);
                int ix = (int) axis.transform( d );
                if ( ix==ix0 ) continue;
                ix0= ix;
                g.drawLine( 0, ix-myY, 3, ix-myY );
            }
        }
    }
    
    @Override
    public void resize() {
        if ( getColumn()==null || getColumn().getParent()==null ) { 
             return;
        }
        if ( axis.isHorizontal() ) {
            int x= getColumn().getDMinimum();
            int y= getRow().getDMinimum();
            Rectangle rect= new Rectangle( x, y, getColumn().getWidth(), this.maxHeight );
            this.setBounds( rect );            
        } else {
            int x= getColumn().getDMaximum();
            int y= getRow().getDMinimum();
            Rectangle rect= new Rectangle( x, y, this.maxWidth, this.getRow().getHeight() );
            this.setBounds( rect );
        }
    }
}
