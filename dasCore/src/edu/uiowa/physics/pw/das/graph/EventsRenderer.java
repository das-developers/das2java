/*
 * EventsRenderer.java
 *
 * Created on December 13, 2005, 3:37 PM
 *
 *
 */

package edu.uiowa.physics.pw.das.graph;

import edu.uiowa.physics.pw.das.dataset.DataSetDescriptor;
import edu.uiowa.physics.pw.das.dataset.VectorDataSet;
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.datum.DatumRange;
import edu.uiowa.physics.pw.das.datum.DatumUtil;
import edu.uiowa.physics.pw.das.datum.UnitsConverter;
import edu.uiowa.physics.pw.das.event.LabelDragRenderer;
import edu.uiowa.physics.pw.das.event.MouseModule;
import edu.uiowa.physics.pw.das.system.DasLogger;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;


/**
 *
 * @author Jeremy
 */
public class EventsRenderer extends Renderer {
    
    int[] eventMap;
    
    public EventsRenderer( DataSetDescriptor dsd ) {
        super(dsd);
    }
    
    public EventsRenderer( ) {
        super();
    }
    
    protected org.w3c.dom.Element getDOMElement(org.w3c.dom.Document document) {
        return null;
    }
    
    protected void installRenderer() {
        MouseModule mm= getMouseModule();
        parent.getMouseAdapter().addMouseModule( mm );
        parent.getMouseAdapter().setPrimaryModule( mm );
    }
    
    private class DragRenderer extends LabelDragRenderer {
        DasAxis xaxis, yaxis;
        DasPlot parent;        
        DragRenderer( DasPlot parent ) {
            super( parent );
            this.xaxis= parent.getXAxis();
            this.yaxis= parent.getYAxis();
            this.parent= parent;
            this.setTooltip(true);
        }
        public Rectangle[] renderDrag( Graphics g, Point p1, Point p2 ) {
            VectorDataSet vds= (VectorDataSet)getDataSet();
            
            if ( vds==null ) return null;
            if ( vds.getXLength()==0 ) return null;
            
            if ( p2.getX()<0 || p2.getX() >= eventMap.length ) {
                setLabel(null);
            } else {
                int i= eventMap[(int)p2.getX()];
                if ( i>=0 ) {
                    Datum sx= vds.getXTagDatum(i);
                    Datum sz= vds.getDatum(i);
                    VectorDataSet widthsDs= (VectorDataSet)vds.getPlanarView("xTagWidth");
                    Datum sy= DatumUtil.asOrderOneUnits( widthsDs.getDatum(i) );
                    DatumRange dr= new DatumRange( sx, sx.add(sy) );
                    //setLabel(""+sx+" "+sy+"!c"+sz );
                    setLabel(""+dr+" ("+sy+")!c"+sz );
                } else {
                    setLabel(null);
                }
            }
            
            return super.renderDrag( g, p1, p2 );
        }
        
    }
    
    
    
    private MouseModule getMouseModule() {
        return new MouseModule( parent, new DragRenderer(parent), "event lookup" );
    }
    
    public void render(java.awt.Graphics g1, DasAxis xAxis, DasAxis yAxis) {
        
        VectorDataSet vds= (VectorDataSet)getDataSet();
        if (vds == null || vds.getXLength() == 0) {
            DasLogger.getLogger(DasLogger.GRAPHICS_LOG).fine("null data set");
            return;
        }
        
        Graphics2D g= ( Graphics2D ) g1.create();
        
        g.setColor(new Color(100,100,100,180));
        
        if ( vds==null && lastException!=null ) {
            renderException( g, xAxis, yAxis, lastException );
            
        } else {
            if ( vds.getPlanarView("xTagWidth")==null ) {
                throw new IllegalArgumentException("no xTagWidth plane found.");
            }
            VectorDataSet widthsDs= (VectorDataSet)vds.getPlanarView("xTagWidth");
            
            DasColumn column= xAxis.getColumn();
            DasRow row= parent.getRow();
            
            eventMap= new int[column.getWidth()];
            for ( int k=0; k<eventMap.length; k++ ) eventMap[k]= -1;
            
            if ( vds.getXLength()>0 ) {
                UnitsConverter uc=  UnitsConverter.getConverter( widthsDs.getYUnits(), xAxis.getUnits().getOffsetUnits() );
                
                int ivds0= 0;
                int ivds1= vds.getXLength();
                for ( int i=ivds0; i<ivds1; i++ ) {
                    Datum x= vds.getXTagDatum(i);
                    int ix= (int)xAxis.transform(x);
                    int iwidth;
                    if ( uc!=null ) {
                        Datum y= widthsDs.getDatum(i);
                        iwidth= (int)xAxis.transform( x.add( y ) ) - ix;
                    } else {
                        iwidth= 1;
                    }
                    if ( column.getDMinimum() < ( ix+iwidth ) ||
                            column.getDMaximum() > (ix) ) {
                        if ( iwidth==0 ) iwidth=1;
                        g.fill( new Rectangle( ix, row.getDMinimum(), iwidth, row.getHeight() ) );
                        int im= ix-column.getDMinimum();
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
        }
        g.dispose();
        
    }
    
    protected void uninstallRenderer() {
    }
    
}
