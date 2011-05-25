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
import org.virbo.dataset.BundleDataSet;
import org.virbo.dataset.DDataSet;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.JoinDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;
import org.virbo.dsops.Ops;
import test.graph.PlotDemo;


/**
 * draw bars for the dataset.  This expects a QDataSet with the scheme:
 * Events[:] where DEPEND_0 is rank 2 bins dataset [:,"min,max"]
 * Note fill values in Events are under-implemented, we don't check validMin,validMax.
 * @author Jeremy
 */
public class EventsRenderer extends Renderer {

    /**
     * return bounding cube 
     * @param ds
     * @return
     */
    public static QDataSet doAutorange(QDataSet ds) {

        QDataSet xrange;
        DDataSet yrange;
        yrange= DDataSet.createRank1(2);

        yrange.putValue(0,0);
        yrange.putValue(1,10);

        QDataSet xds= SemanticOps.xtagsDataSet(ds);

        xrange= Ops.extent(xds);

        xrange= Ops.rescaleRange( xrange, -0.1, 1.1 );

        JoinDataSet bds= new JoinDataSet(2);
        bds.join(xrange);
        bds.join(yrange);

        return bds;
        
    }
    
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

            if ( vds==null ) return new Rectangle[0];
            if ( vds.length()==0 ) return new Rectangle[0];

            QDataSet ds= makeCanonical(vds);
            QDataSet xmins= DataSetOps.unbundle( ds,0 );
            QDataSet xmaxs= DataSetOps.unbundle( ds,1 );
            QDataSet msgs= DataSetOps.unbundle(ds,ds.length(0)-1);

            int ix= (int)p2.getX() - parent.getColumn().getDMinimum();
            
            if ( ix<0 || ix >= eventMap.length ) {
                setLabel(null);
            } else {
                int i= eventMap[ix];
                if ( i>=0 ) {
                    double sxmin= xmins.value(i);
                    double sxmax= xmaxs.value(i);
                    Units sxunits= SemanticOps.getUnits(xmins);
                    Units zunits= SemanticOps.getUnits(msgs);

                    Datum sz= zunits.createDatum( msgs.value(i) );
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
    
    /**
     * make canonical rank 2 bundle dataset of min,max,color,text
     * @param vds
     * @return
     */
    private QDataSet makeCanonical( QDataSet vds ) {

        QDataSet xmins;
        QDataSet xmaxs;
        QDataSet colors;
        QDataSet msgs;

        if ( vds.rank()==2 ) {
            xmins= DataSetOps.unbundle( vds,0 );
            xmaxs= DataSetOps.unbundle( vds,1 );
            Units u0= SemanticOps.getUnits(xmins );
            Units u1= SemanticOps.getUnits(xmaxs );
            if ( !u1.isConvertableTo(u0) && u1.isConvertableTo(u0.getOffsetUnits()) ) {
                xmaxs= Ops.add( xmins, xmaxs );
            }
            if ( vds.length(0)>3 ) {
                colors= DataSetOps.unbundle( vds,2 );
            } else {
                colors= Ops.replicate( 0x808080, xmins.length() );
            }
            msgs= DataSetOps.unbundle( vds, vds.length(0)-1 );
            
        } else if ( vds.rank()==1 ) {
            QDataSet dep0= (QDataSet) vds.property(QDataSet.DEPEND_0);
            if ( dep0==null ) {
                xmins= vds;
                xmaxs= vds;
                msgs= vds;
            } else if ( dep0.rank() == 2  ) {
                if ( SemanticOps.isBins(dep0) ) {
                    xmins= DataSetOps.slice1( dep0, 0 );
                    xmaxs= DataSetOps.slice1( dep0, 1 );
                    Units u0= SemanticOps.getUnits(xmins );
                    Units u1= SemanticOps.getUnits(xmaxs );
                    if ( !u1.isConvertableTo(u0) && u1.isConvertableTo(u0.getOffsetUnits()) ) {
                        xmaxs= Ops.add( xmins, xmaxs );
                    }
                    msgs= vds;
                } else {
                    parent.postMessage( this, "DEPEND_0 is rank 2 but not bins", DasPlot.WARNING, null, null );
                    return null;
                }
            } else if ( dep0.rank() == 1 ) {
                xmins= dep0;
                xmaxs= xmins;
                msgs= vds;
            } else {
                parent.postMessage( this, "dataset is not correct form", DasPlot.WARNING, null, null );
                return null;
            }
            colors= Ops.replicate( 0x808080, xmins.length() );
        } else {
            parent.postMessage( this, "dataset must be rank 1 or rank 2", DasPlot.WARNING, null, null );
            return null;
        }

        QDataSet ds= Ops.bundle( Ops.bundle( Ops.bundle( xmins, xmaxs ), colors ), msgs );

        return ds;

    }


    public void render(java.awt.Graphics g1, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {

        QDataSet vds= (QDataSet)getDataSet();
        if (vds == null || vds.length() == 0) {
            DasLogger.getLogger(DasLogger.GRAPHICS_LOG).fine("null data set");
            return;
        }
        
        Graphics2D g= ( Graphics2D ) g1.create();
        
        g.setColor(color);

        QDataSet ds= makeCanonical(vds);
        if ( ds==null ) {
            // a message should be posted by makeCanonical
            return;
        }

        QDataSet xmins= DataSetOps.unbundle( ds,0 );
        QDataSet xmaxs= DataSetOps.unbundle( ds,1 );
        QDataSet color= ds.length(0)>3 ? DataSetOps.unbundle( ds,2 ) : null;
        QDataSet msgs= DataSetOps.unbundle(ds,ds.length(0)-1);

        if ( vds==null && lastException!=null ) {
            renderException( g, xAxis, yAxis, lastException );
            
        } else {
            
            DasColumn column= xAxis.getColumn();
            DasRow row= parent.getRow();
            
            eventMap= new int[column.getWidth()];
            for ( int k=0; k<eventMap.length; k++ ) eventMap[k]= -1;
            
            QDataSet xds= xmins;
            Units xunits= SemanticOps.getUnits(xds);
            Units units= SemanticOps.getUnits(msgs);

            if ( vds.length()>0 ) {
                
                int ivds0= 0;
                int ivds1= xmins.length();

                for ( int i=ivds0; i<ivds1; i++ ) {
                    
                    int ixmin= (int)xAxis.transform( xmins.value(i),xunits);
                    int ixmax= (int)xAxis.transform( xmaxs.value(i),xunits);

                    int iwidth= Math.max( ixmax- ixmin, 1 );

                    if ( color!=null ) {
                        int rr= ( (int)color.value(i) & 0xFF0000 ) >> 16;
                        int gg= ( (int)color.value(i) & 0x00FF00 ) >> 8;
                        int bb= ( (int)color.value(i) & 0x0000FF ) >> 0;
                        int aa= 128;
                        g.setColor( new Color( rr, gg, bb, aa ) );
                    }
                    
                    if ( column.getDMinimum() < ixmax || column.getDMaximum() > ixmin ) { // if any part is visible
                        if ( iwidth==0 ) iwidth=1;
                        g.fill( new Rectangle( ixmin, row.getDMinimum(), iwidth, row.getHeight() ) );
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
