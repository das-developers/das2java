/*
 * EventsRenderer.java
 *
 * Created on December 13, 2005, 3:37 PM
 *
 *
 */

package org.das2.graph;

import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumUtil;
import org.das2.event.LabelDragRenderer;
import org.das2.event.MouseModule;
import org.das2.system.DasLogger;
import org.das2.util.monitor.ProgressMonitor;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import org.das2.datum.Units;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;


/**
 * draw bars for the dataset.  This expects a QDataSet with the scheme:
 * Events[:] where DEPEND_0 is rank 2 bins dataset [:,"min,max"]
 * Note fill values in Events are under-implemented, we don't check validMin,validMax.
 * @author Jeremy
 */
public class EventsRenderer extends Renderer {
    
    int[] eventMap;
    
    private EventsRenderer.ColorSpecifier colorSpecifier=null;
    
    
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
        parent.getDasMouseInputAdapter().addMouseModule( mm );
        parent.getDasMouseInputAdapter().setPrimaryModule( mm );
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
            QDataSet vds= (QDataSet)getDataSet();
            QDataSet xds= SemanticOps.xtagsDataSet(vds); // should be rank 2
            if ( vds==null ) return new Rectangle[0];
            if ( vds.length()==0 ) return new Rectangle[0];
            
            int ix= (int)p2.getX() - parent.getColumn().getDMinimum();
            
            if ( ix<0 || ix >= eventMap.length ) {
                setLabel(null);
            } else {
                int i= eventMap[ix];
                if ( i>=0 ) {
                    double sxmin= xds.value(i,0);
                    double sxmax= xds.value(i,1);
                    Units sxunits= SemanticOps.getUnits(xds);
                    Units zunits= SemanticOps.getUnits(ds);

                    Datum sz= zunits.createDatum( vds.value(i) ); //TODO: validMin, validMax
                    DatumRange dr= new DatumRange( sxmin, sxmax, sxunits );
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
        
        QDataSet vds= (QDataSet)getDataSet();
        if (vds == null || vds.length() == 0) {
            DasLogger.getLogger(DasLogger.GRAPHICS_LOG).fine("null data set");
            return;
        }
        
        Graphics2D g= ( Graphics2D ) g1.create();
        
        g.setColor(color);
        
        if ( vds==null && lastException!=null ) {
            renderException( g, xAxis, yAxis, lastException );
            
        } else {
            
            DasColumn column= xAxis.getColumn();
            DasRow row= parent.getRow();
            
            eventMap= new int[column.getWidth()];
            for ( int k=0; k<eventMap.length; k++ ) eventMap[k]= -1;
            
            QDataSet xds= SemanticOps.xtagsDataSet(ds);
            Units xunits= SemanticOps.getUnits(xds);
            Units units= SemanticOps.getUnits(ds);

            if ( vds.length()>0 ) {
                
                int ivds0= 0;
                int ivds1= vds.length();

                for ( int i=ivds0; i<ivds1; i++ ) {
                    
                    double xmin= vds.value(i,0);
                    int ixmin= (int)xAxis.transform( vds.value(i,0),xunits);
                    int ixmax= (int)xAxis.transform( vds.value(i,1),xunits);

                    int iwidth= Math.max( ixmax- ixmin, 1 );

                    if ( colorSpecifier!=null ) {
                        Datum sz= units.createDatum( vds.value(i) );
                        g.setColor( colorSpecifier.getColor(sz) );
                    }
                    
                    if ( column.getDMinimum() < ixmax || column.getDMaximum() > ixmin ) { // if any part is visible
                        if ( iwidth==0 ) iwidth=1;
                        g.fill( new Rectangle( ixmin, row.getDMinimum(), ixmax-ixmin, row.getHeight() ) );
                        int im= ixmin-column.getDMinimum();
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
