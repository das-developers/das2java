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
public class DasEventsIndicator extends DasCanvasComponent implements DataSetUpdateListener {
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
        dsd.addDataSetUpdateListener(this);
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
            
            int i= eventMap[e.getX()];
            if ( i>=0 ) {
                Datum sx= parent.vds.getXTagDatum(i);
                Datum sz= parent.vds.getDatum(i);
                VectorDataSet widthsDs= (VectorDataSet)vds.getPlanarView("xTagWidth");
                Datum sy= widthsDs.getDatum(i);
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
            if ( vds.getPlanarView("xTagWidth")==null ) {
                throw new IllegalArgumentException("no xTagWidth plane found.");
            }
            VectorDataSet widthsDs= (VectorDataSet)vds.getPlanarView("xTagWidth");
            for ( int k=0; k<eventMap.length; k++ ) eventMap[k]= -1;
            if ( vds.getXLength()>0 ) {
                UnitsConverter uc=  UnitsConverter.getConverter( widthsDs.getYUnits(), axis.getUnits().getOffsetUnits() );
                
                int ivds0= 0;
                int ivds1= vds.getXLength();
                for ( int i=ivds0; i<ivds1; i++ ) {
                    Datum x= vds.getXTagDatum(i);
                    int ix= (int)axis.transform(x);
                    int iwidth;
                    if ( uc!=null ) {
                        Datum y= widthsDs.getDatum(i);
                        iwidth= (int)axis.transform( x.add( y ) ) - ix;
                    } else {
                        iwidth= 1;
                    }
                    if ( getColumn().getDMinimum() < ( ix+iwidth ) || 
                        getColumn().getDMaximum() > (ix) ) {
                        if ( iwidth==0 ) iwidth=1;
                        g.fill( new Rectangle( ix, getY(), iwidth, getHeight() ) );
                        int im= ix-getX();
                        int em0= im-1;
                        int em1= im+iwidth+1;
                        for ( int k=em0; k<em1; k++ ) {
                            if ( k>=0 && k<eventMap.length ) eventMap[k]= i;
                        }
                    }
                }
                for ( int k1=1; k1<=2; k1++ ) { /* add fuzziness using Larry's algorithm */
                    for ( int k2=-1; k2<=1; k2+=2 ) {
                        int em0= ( k2==1 ) ? 0 : eventMap.length-1;
                        int em1= ( k2==1 ) ? eventMap.length-k1 : k1;
                        for ( int k=em0; k!=em1; k+=k2) {
                            if ( eventMap[k]==-1 ) eventMap[k]= eventMap[k+k2];
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
    
    public void dataSetUpdated(DataSetUpdateEvent e) {
        markDirty();
        update();
    }
    
}
