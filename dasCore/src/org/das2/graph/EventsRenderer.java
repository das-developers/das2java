/*
 * EventsRenderer.java
 *
 * Created on December 13, 2005, 3:37 PM
 *
 *
 */

package org.das2.graph;

import java.awt.Shape;
import javax.swing.Icon;
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
import java.awt.geom.GeneralPath;
import javax.swing.ImageIcon;
import org.das2.dataset.DataSetUtil;
import org.das2.datum.Units;
import org.virbo.dataset.DDataSet;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.JoinDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;
import org.virbo.dsops.CoerceUtil;
import org.virbo.dsops.Ops;


/**
 * draw bars for the dataset.  This expects a QDataSet with the scheme:
 * Events[:] where DEPEND_0 is rank 2 bins dataset [:,"min,max"]
 * Note fill values in Events are under-implemented, we don't check validMin,validMax.
 * @author Jeremy
 */
public class EventsRenderer extends Renderer {
    public static final String PROP_COLOR = "color";

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

        QDataSet xmins;
        QDataSet xmaxs;
        if ( ds.rank()==1 && ds.property(QDataSet.DEPEND_0)==null ) {
            xmins= ds; //vap+inline:2010-002T03:50,2010-002T03:54,2010-002T03:56&RENDER_TYPE=eventsBar
            xmaxs= ds;
        } else if ( ds.rank()==1 && ds.property(QDataSet.DEPEND_0)!=null ) {
            xmins= (QDataSet)ds.property(QDataSet.DEPEND_0); //vap+inline:2010-002T03:50,2010-002T03:54,2010-002T03:56&RENDER_TYPE=eventsBar
            xmaxs= xmins;
        } else {
            xmins= SemanticOps.xtagsDataSet(ds);
            xmaxs= DataSetOps.unbundle( ds,1 );
        }

        Units u0= SemanticOps.getUnits(xmins);
        Units u1= SemanticOps.getUnits(xmaxs);

        if ( xmins.length()==0 ) {
            xrange=  DDataSet.wrap( new double[] {0,1}, u0 );

        } else {
            //TODO: probably the day/days containing would be better
            xrange= Ops.extent(xmins);
            if ( !u1.isConvertableTo(u0) && u1.isConvertableTo(u0.getOffsetUnits()) ) {
                xmaxs= Ops.add( xmins, xmaxs );
                xrange= Ops.extent(xmaxs,xrange);
            }

            if ( xrange.value(0)<xrange.value(1) ) {
                xrange= Ops.rescaleRange( xrange, -0.1, 1.1 );
            } else {
                QDataSet dx= DDataSet.wrap( new double[] {-1,1}, u0.getOffsetUnits() );
                xrange= Ops.add( xrange, dx );
            }
        }

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

    private Shape selectionArea;

    Shape selectionArea() {
        return selectionArea;
    }

    @Override
    public boolean acceptContext(int x, int y) {
        if ( selectionArea!=null ) {
            return selectionArea.contains( x, y );
        } else {
            return true;
        }
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
            if ( dr.width().value()== 0 ) {
                if ( d.toString().equals(dr.min().toString() ) ) {
                    return d.toString();
                } else {
                    return String.format( "%s!c%s", dr.min(), d ) ;
                }
            } else {
                return String.format( "%s (%s)!c%s", dr, sy, d ) ;
            }
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
    
    @Override
    protected void installRenderer() {
        MouseModule mm= getMouseModule();
        parent.getDasMouseInputAdapter().addMouseModule( mm );
        parent.getDasMouseInputAdapter().setPrimaryModule( mm );
    }
    
    private class DragRenderer extends LabelDragRenderer {
        DasPlot parent;
        DragRenderer( DasPlot parent ) {
            super( parent );
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
                    Units sxmaxunits= SemanticOps.getUnits( xmaxs );
                    if ( !sxmaxunits.isConvertableTo(sxunits) ) {
                        if ( sxmaxunits.isConvertableTo(sxunits.getOffsetUnits() ) ) {
                            sxmax= sxmin + sxmaxunits.convertDoubleTo( sxunits.getOffsetUnits(), sxmax );
                        } else {
                            sxmax= sxmin;
                        }
                    } else {
                        sxmax= sxmaxunits.convertDoubleTo( sxunits, sxmax );
                    }

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
            Color c0= getColor();
            Color c1= new Color( c0.getRed(), c0.getGreen(), c0.getBlue(), 128 );
            int irgb= c1.getRGB();
            
            colors= Ops.replicate( irgb, xmins.length() );

        } else {
            parent.postMessage( this, "dataset must be rank 1 or rank 2", DasPlot.WARNING, null, null );
            return null;
        }

        QDataSet ds= Ops.bundle( Ops.bundle( Ops.bundle( xmins, xmaxs ), colors ), msgs );

        return ds;

    }


    public void render(java.awt.Graphics g1, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {

        GeneralPath sa= new GeneralPath();

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

        if ( lastException!=null ) {
            renderException( g, xAxis, yAxis, lastException );
            
        } else {
            
            DasColumn column= xAxis.getColumn();
            DasRow row= parent.getRow();
            
            eventMap= new int[column.getWidth()];
            for ( int k=0; k<eventMap.length; k++ ) eventMap[k]= -1;
            
            QDataSet xds= xmins;
            Units xunits= SemanticOps.getUnits(xds);

            if ( vds.length()>0 ) {
                
                int ivds0= 0;
                int ivds1= xmins.length();

                for ( int i=ivds0; i<ivds1; i++ ) {
                    
                    int ixmin= (int)xAxis.transform( xmins.value(i),xunits);
                    int ixmax= (int)xAxis.transform( xmaxs.value(i),xunits);

                    int iwidth= Math.max( ixmax- ixmin, 1 );

                    if ( color!=null ) {
                        int irgb= (int)color.value(i);
                        int rr= ( irgb & 0xFF0000 ) >> 16;
                        int gg= ( irgb & 0x00FF00 ) >> 8;
                        int bb= ( irgb & 0x0000FF ) >> 0;
                        int aa= 128;
                        g.setColor( new Color( rr, gg, bb, aa ) );
                    }
                    
                    if ( column.getDMinimum() < ixmax || column.getDMaximum() > ixmin ) { // if any part is visible
                        if ( iwidth==0 ) iwidth=1;
                        sa.append( new Rectangle( ixmin-2, row.getDMinimum(), iwidth+4, row.getHeight() ), false );
                        g.fill( new Rectangle( ixmin, row.getDMinimum(), iwidth, row.getHeight() ) );
                        int im= ixmin-column.getDMinimum();
                        int em0= im-1;
                        int em1= im+iwidth+1;
                        for ( int k=em0; k<em1; k++ ) {
                            if ( k>=0 && k<eventMap.length ) eventMap[k]= i;
                        }
                        if ( this.showLabels ) {
                            DatumRange dr= new DatumRange( xmins.value(i), xmaxs.value(i), xunits );
                            Datum d= dr.min();
                            String text= textSpecifier.getText( dr, d );
                            g.drawString( text, ixmin+2, row.getDMinimum()+14 );
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

        selectionArea= sa;
        
    }

    @Override
    public Icon getListIcon() {
        return new ImageIcon(SpectrogramRenderer.class.getResource("/images/icons/eventsBar.png"));
    }
    
    private Color color= new Color(100,100,100,180); // note this alpha=180 is ignored
    
    public Color getColor() {
        return color;
    }
    
    public void setColor( Color color ) {
        Color old= this.color;
        this.color= new Color( color.getRed(), color.getGreen(), color.getBlue(), 180 );// note this alpha=180 is ignored
        propertyChangeSupport.firePropertyChange(  PROP_COLOR, old , color);
        super.invalidateParentCacheImage();
    }
    
    /**
     * true means draw the event label next to the bar.
     */
    protected boolean showLabels = false;
    public static final String PROP_SHOWLABELS = "showLabels";

    public boolean isShowLabels() {
        return showLabels;
    }

    public void setShowLabels(boolean showLabels) {
        boolean oldShowLabels = this.showLabels;
        this.showLabels = showLabels;
        parent.invalidateCacheImage();
        parent.repaint();
        propertyChangeSupport.firePropertyChange(PROP_SHOWLABELS, oldShowLabels, showLabels);
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
