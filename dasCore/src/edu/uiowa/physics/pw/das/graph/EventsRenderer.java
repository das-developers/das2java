/*
 * EventsRenderer.java
 *
 * Created on December 13, 2005, 3:37 PM
 *
 *
 */

package edu.uiowa.physics.pw.das.graph;

import org.das2.dataset.DataSetDescriptor;
import org.das2.dataset.VectorDataSet;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumUtil;
import org.das2.datum.UnitsConverter;
import org.das2.event.LabelDragRenderer;
import org.das2.event.MouseModule;
import org.das2.system.DasLogger;
import org.das2.util.monitor.ProgressMonitor;
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
    
    private EventsRenderer.ColorSpecifier colorSpecifier=null;
    
    public EventsRenderer( DataSetDescriptor dsd ) {
        super(dsd);
    }
    
    public EventsRenderer( ) {
        super();
    }
    
    public interface ColorSpecifier {
        /**
         * returns a color for the given datum.  null may be returned, indicating the
         * default color should be used.
         */
        Color getColor( Datum d );
    }
    
    public interface TextSpecifier {
        /**
         * returns the text for the given datum.  null may be returned, indicating the
         * default String.valueOf(d) should be used.
         * @param range the range of the event
         * @param d the Datum associated with the range
         */
        String getText( DatumRange range, Datum d );
    }
    
    public static final TextSpecifier DEFAULT_TEXT_SPECIFIER= new TextSpecifier() {
        public String getText( DatumRange dr, Datum d ) {
            Datum sy= DatumUtil.asOrderOneUnits( dr.width() );
            return ""+dr+" ("+sy+")!c"+d ;
        }
    };
    
    /**
     * set this to be an object implementing ColorSpecifier interface, if more than
     * one color is to be used when drawing the bars.  Setting this to null will
     * restore the initial behavior of drawing all bars in one color.
     */
    public void setColorSpecifier( ColorSpecifier spec ) {
        this.colorSpecifier= spec;
    }
    
    public ColorSpecifier getColorSpecifier( ) {
        return this.colorSpecifier;
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
        @Override
        public Rectangle[] renderDrag( Graphics g, Point p1, Point p2 ) {
            VectorDataSet vds= (VectorDataSet)getDataSet();
            
            if ( vds==null ) return new Rectangle[0];
            if ( vds.getXLength()==0 ) return new Rectangle[0];
            
            int ix= (int)p2.getX() - parent.getColumn().getDMinimum();
            
            if ( ix<0 || ix >= eventMap.length ) {
                setLabel(null);
            } else {
                int i= eventMap[ix];
                if ( i>=0 ) {
                    Datum sx= vds.getXTagDatum(i);
                    Datum sz= vds.getDatum(i);
                    VectorDataSet widthsDs= (VectorDataSet)vds.getPlanarView(widthPlaneId);
                    DatumRange dr= new DatumRange( sx, sx.add(widthsDs.getDatum(i)) );
                    setLabel( textSpecifier.getText( dr, sz ) );
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
    
    public void render(java.awt.Graphics g1, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {
        
        VectorDataSet vds= (VectorDataSet)getDataSet();
        if (vds == null || vds.getXLength() == 0) {
            DasLogger.getLogger(DasLogger.GRAPHICS_LOG).fine("null data set");
            return;
        }
        
        Graphics2D g= ( Graphics2D ) g1.create();
        
        g.setColor(color);
        
        if ( vds==null && lastException!=null ) {
            renderException( g, xAxis, yAxis, lastException );
            
        } else {
            VectorDataSet widthsDs= (VectorDataSet)vds.getPlanarView(widthPlaneId);
            if ( widthsDs==null ) {
                throw new IllegalArgumentException("no width plane named \""+widthPlaneId+"\" found");
            }
            
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
                    
                    if ( colorSpecifier!=null ) {
                        Datum sz= vds.getDatum(i);
                        g.setColor( colorSpecifier.getColor(sz) );
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
    
    private Color color= new Color(100,100,100,180);
    
    public Color getColor() {
        return color;
    }
    
    public void setColor( Color color ) {
        this.color= new Color( color.getRed(), color.getGreen(), color.getBlue(), 180 );
        super.invalidateParentCacheImage();
    }
    
    private String widthPlaneId="xTagWidth";
    public void setWidthPlaneId( String id ) {
        this.widthPlaneId= id;
    }
    public String getWidthPlaneId( ) {
        return this.widthPlaneId;
    }
    
    /**
     * Holds value of property textSpecifier.
     */
    private TextSpecifier textSpecifier= DEFAULT_TEXT_SPECIFIER;
    
    /**
     * Getter for property textSpecifier.
     * @return Value of property textSpecifier.
     */
    public TextSpecifier getTextSpecifier() {
        return this.textSpecifier;
    }
    
    /**
     * Setter for property textSpecifier.
     * @param textSpecifier New value of property textSpecifier.
     */
    public void setTextSpecifier(TextSpecifier textSpecifier) {
        TextSpecifier oldTextSpecifier = this.textSpecifier;
        this.textSpecifier = textSpecifier;
        propertyChangeSupport.firePropertyChange("textSpecifier", oldTextSpecifier, textSpecifier);
    }
    
}
