/*
 * DasEventsIndicator.java
 *
 * Created on April 6, 2004, 10:39 AM
 */

package edu.uiowa.physics.pw.das.graph;

import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.graph.*;
import edu.uiowa.physics.pw.das.util.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 *
 * @author  Jeremy
 */
public class DasEventsIndicator extends DasCanvasComponent {
    DasAxis axis;
    DataSetDescriptor dsd;
    VectorDataSet vds;
    String planeId; /* name of the plane to get the event type */
    int[] eventMap;
    
    /** Creates a new instance of DasEventsIndicator */
    public DasEventsIndicator( DataSetDescriptor dsd, DasAxis axis, String planeId ) {
        if ( !axis.isHorizontal() ) {
            throw new IllegalArgumentException( "Axis must be horizontal");
        }
        this.axis= axis;
        this.dsd= dsd;
        axis.addPropertyChangeListener("dataMinimum", getPropertyChangeListener());
        axis.addPropertyChangeListener("dataMaximum", getPropertyChangeListener());
        axis.addPropertyChangeListener("log", getPropertyChangeListener());
        MouseInputAdapter ma= new MyMouseAdapter(this,planeId);
        addMouseListener( ma );
        addMouseMotionListener( ma );
    }
    
    private PropertyChangeListener getPropertyChangeListener() {
        return new PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent e) {
                markDirty();
                update();
            }
        };
    }
    
    class MyMouseAdapter extends MouseInputAdapter {
        DasEventsIndicator parent;
        String planeId;
        
        MyMouseAdapter( DasEventsIndicator parent, String planeId ) {
            this.parent= parent;
            this.planeId= planeId;
        }
        
        public void mouseClicked(MouseEvent e) {
        }
        
        public void mouseMoved(MouseEvent e) {
            
            if ( parent.vds==null ) return;
            if ( parent.vds.getXLength()==0 ) return;
            
            int ix= e.getX()+parent.getX();
            Datum x= parent.axis.invTransform(ix);
            
            Datum axisRes= parent.axis.invTransform(ix+2).subtract(x); /* two-pixel resolution */
            int i= eventMap[e.getX()];
            Datum sx= parent.vds.getXTagDatum(i);
            Datum sy= parent.vds.getDatum(i);
            if ( parent.vds.getPlanarView(planeId)==null ) {
                throw new IllegalArgumentException("planeId is not found in dataset");
            }
            Datum sz= ((VectorDataSet)parent.vds.getPlanarView(planeId)).getDatum(i);
            if ( sx.subtract(axisRes).le(x) && x.le(sx.add(sy).add(axisRes)) ) {
                parent.setToolTipText(""+sx+" "+sy+" "+sz);
            } else {
                parent.setToolTipText(null);
            }
        }
        
    }
    
    public void paint(java.awt.Graphics g1) {
        Graphics2D g= ( Graphics2D ) g1;
        g.translate(-getX(), -getY());
        g.setColor(Color.white);
        g.fill(getBounds());
        g.setColor(Color.DARK_GRAY);
        g.draw(new Rectangle(getX(),getY(),getWidth()-1,getHeight()-1));
        
        g.setColor(new Color(100,100,100,180));
        
        eventMap= new int[getWidth()];
        
        try {
            vds= (VectorDataSet)dsd.getDataSet( axis.getDataMinimum(), axis.getDataMaximum(), null, null );
            
            if ( vds.getXLength()>0 ) {                
                UnitsConverter uc=  UnitsConverter.getConverter( vds.getYUnits(), axis.getUnits().getOffsetUnits() );
                
                int ivds0= 0;
                int ivds1= vds.getXLength();
                for ( int i=ivds0; i<ivds1; i++ ) {
                    Datum x= vds.getXTagDatum(i);
                    int ix= axis.transform(x);
                    int iwidth;
                    if ( uc!=null ) {
                        Datum y= vds.getDatum(i);
                        iwidth= axis.transform( x.add( y ) ) - ix;
                    } else {
                        iwidth= 1;
                    }
                    if ( iwidth==0 ) iwidth=1;
                    g.fill( new Rectangle( ix, getY(), iwidth, getHeight() ) ); {
                        int em0= ix-getX()-1;
                        if (em0<0) em0=0;
                        if (em0>=eventMap.length) em0= eventMap.length-1;
                        int em1= ix-getX()+iwidth+1;
                        if (em1<0) em1=0;
                        if (em1>=eventMap.length) em1= eventMap.length-1;
                        for ( int k= em0; k<em1; k++ ) {
                            eventMap[k]= i;
                        }
                    }
                }
            }
        } catch ( DasException e ) {
            g.drawString( "exception: "+e.getMessage(), getX(), getY()+getHeight() );
        }        
    }
    
    public void setDataSetDescriptor( DataSetDescriptor dsd ) {
        this.dsd= dsd;
        markDirty();
        update();
    }
    
}
