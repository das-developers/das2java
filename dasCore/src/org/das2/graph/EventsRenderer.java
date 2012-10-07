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
import org.das2.datum.Units;
import org.das2.util.GrannyTextRenderer;
import org.virbo.dataset.DDataSet;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.IDataSet;
import org.virbo.dataset.JoinDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.RankZeroDataSet;
import org.virbo.dataset.SemanticOps;
import org.virbo.dataset.WritableDataSet;
import org.virbo.dsops.Ops;
import org.virbo.dsutil.DataSetBuilder;


/**
 * Draw colored horizontal bars for the dataset, marking events datasets or modes of the data.  This expects
 * a QDataSet with the canonical scheme:
 *    Events[:,BUNDLE_1=4] where the columns are:
 *      BUNDLE_1=startTime,stopTime,Color,Message
 *    startTime,stopTime are in some time location unit.  stopTime may also be an offset from startTime (e.g. seconds)
 *    Color is an int, that is either 0xRRGGBB or 0xAARRGGBB.
 *    Message is any datum, so typically an enumeration unit is used.
 * Note this also contains systems for coloring data in old schemes, such as the colorSpecifier interface and textSpecifier.
 * These should not be used when a dataset will be sufficient.
 *
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
        
    public EventsRenderer( ) {
        super();
    }

    private Shape selectionArea;

    Shape selectionArea() {
        return selectionArea==null ? SelectionUtil.NULL : selectionArea;
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
                    return String.format( "%s", d ) ; // don't indicate time if it's zero width, because this is redundant and clutter.
                }
            } else {
                return String.format( "%s (%s)!c%s", dr, sy, d ) ;
            }
        }
    };
    
    
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

            QDataSet ds= cds;
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

                    if ( sxmax<sxmin ) {
                        setLabel( "Error, sxmax<sxmin");
                    } else {
                        String ss= "";
                        DatumRange dr= new DatumRange( sxmin, sxmax, sxunits );
                        try {
                            Datum sz= zunits.createDatum( msgs.value(i) );
                            ss= textSpecifier.getText( dr, sz );
                        } catch ( RuntimeException ex ) {
                            ss= "" + dr + " fill";
                        }
                        setLabel( ss );
                    }
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
     * canonical dataset containing rank2 bundle of start,stop,color,text.
     */
    private QDataSet cds=null;
    
    /**
     * make the canonical dataset smaller by combining adjacent records.  This is introduced
     * because we now support event datasets with a regular cadence.
     * @param vds
     * @return
     */
    private QDataSet coalesce( QDataSet vds ) {
        QDataSet bds= (QDataSet) vds.property(QDataSet.BUNDLE_1);
        DataSetBuilder build= new DataSetBuilder(2,100,4);
        DDataSet v= DDataSet.createRank1(4);

        QDataSet dep0= DataSetOps.unbundle( vds,0 );

        double tlim= 1e-31;
        RankZeroDataSet cad= org.virbo.dataset.DataSetUtil.guessCadenceNew( dep0, null );
        if ( cad!=null ) {
            tlim= cad.value()/100;
        }

        int count=0;

        if ( vds.length()==0 ) {
            return vds;
        }

        v.putValue( 0,vds.value(0,0) );
        v.putValue( 1,vds.value(0,1) );
        v.putValue( 2,vds.value(0,2) );
        v.putValue( 3,vds.value(0,3) );

        for ( int i=1; i<vds.length(); i++ ) {
            if ( Math.abs( vds.value(i,0)-vds.value(i-1,1) ) > tlim        // they don't connect
                    || vds.value(i,3)!=vds.value(i-1,3)                    // the message changed
                    || Math.abs( vds.value(i,2)-vds.value(i-1,2) ) > 1e-31 // the color changed
                    ) {
                build.putValues( -1, v, 4 );
                build.nextRecord();
                v.putValue( 0,vds.value(i,0) );
                v.putValue( 1,vds.value(i,1) );
                v.putValue( 2,vds.value(i,2) );
                v.putValue( 3,vds.value(i,3) );
                count=1;
            } else {
                v.putValue( 1,vds.value(i,1) );
                count++;
            }
        }
        build.putValues( -1, v, 4 );
        build.putProperty( QDataSet.BUNDLE_1, bds );
        return build.getDataSet();
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

        if ( vds==null ) {
            return null;
        }
        
        if ( vds.rank()==2 ) {
            QDataSet dep0= (QDataSet) vds.property(QDataSet.DEPEND_0);
            if ( dep0==null ) {
                xmins= DataSetOps.unbundle( vds,0 );
                xmaxs= DataSetOps.unbundle( vds,1 );

                if ( vds.length(0)>3 ) {
                    colors= DataSetOps.unbundle( vds,2 );
                } else {
                    colors= Ops.replicate( getColor().getRGB(), xmins.length() );
                }
                
            } else if ( dep0.rank()==2 ) {
                if ( SemanticOps.isBins(dep0) ) {
                    xmins= DataSetOps.slice1( dep0, 0 );
                    xmaxs= DataSetOps.slice1( dep0, 1 );
                    colors= Ops.replicate( 0x808080, xmins.length() );
                    Units u0= SemanticOps.getUnits(xmins );
                    Units u1= SemanticOps.getUnits(xmaxs );
                    if ( !u1.isConvertableTo(u0) && u1.isConvertableTo(u0.getOffsetUnits()) ) {
                        xmaxs= Ops.add( xmins, xmaxs );
                    }
                } else {
                    parent.postMessage( this, "DEPEND_0 is rank 2 but not bins", DasPlot.WARNING, null, null );
                    return null;
                }
                
            } else  if ( dep0.rank() == 1 ) {
                Datum width= SemanticOps.guessXTagWidth( dep0, null ).divide(2);
                xmins= Ops.subtract( dep0, org.virbo.dataset.DataSetUtil.asDataSet(width) );
                xmaxs= Ops.add( dep0, org.virbo.dataset.DataSetUtil.asDataSet(width) );
                colors= Ops.replicate( getColor().getRGB(), xmins.length() );

            } else {
                parent.postMessage( this, "rank 2 dataset must have dep0 of rank 1 or rank 2 bins", DasPlot.WARNING, null, null );
                return null;
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
                Datum width= SemanticOps.guessXTagWidth( dep0, null ).divide(2);
                xmins= Ops.subtract( dep0, org.virbo.dataset.DataSetUtil.asDataSet(width) );
                xmaxs= Ops.add( dep0, org.virbo.dataset.DataSetUtil.asDataSet(width) );
                msgs= vds;
            } else {
                parent.postMessage( this, "dataset is not correct form", DasPlot.WARNING, null, null );
                return null;
            }
            Color c0= getColor();
            Color c1= new Color( c0.getRed(), c0.getGreen(), c0.getBlue(), c0.getAlpha()==255 ? 128 : c0.getAlpha() );
            int irgb= c1.getRGB();
            
            colors= Ops.replicate( irgb, xmins.length() );

        } else {
            parent.postMessage( this, "dataset must be rank 1 or rank 2", DasPlot.WARNING, null, null );
            return null;
        }

        if ( this.colorSpecifier!=null ) {
            Units u= SemanticOps.getUnits(msgs);
            WritableDataSet wds= IDataSet.copy(colors);
            for ( int i=0; i<msgs.length(); i++ ) {
                wds.putValue( i, colorSpecifier.getColor( Datum.create( msgs.value(i), u ) ).getRGB() );
            }
            colors= wds;
        }

        Units u0= SemanticOps.getUnits( xmins );
        Units u1= SemanticOps.getUnits( xmaxs );

        if ( u1.isConvertableTo( u0.getOffsetUnits() ) && !u1.isConvertableTo(u0) ) { // maxes are dt instead of stopt.
            xmaxs= Ops.add( xmins, xmaxs );
        }

        QDataSet ds= Ops.bundle( Ops.bundle( Ops.bundle( xmins, xmaxs ), colors ), msgs );

        ds= coalesce(ds);
        
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

        if ( cds==null ) {
            // a message should be posted by makeCanonical
            return;
        }

        QDataSet xmins= DataSetOps.unbundle( cds,0 );
        QDataSet xmaxs= DataSetOps.unbundle( cds,1 );
        QDataSet msgs= DataSetOps.unbundle( cds,3 );
        Units eu= SemanticOps.getUnits( msgs );
        QDataSet lcolor= DataSetOps.unbundle( cds,2 );

        long t0= System.currentTimeMillis();

        if ( lastException!=null ) {
            renderException( g, xAxis, yAxis, lastException );
            
        } else {
            
            DasColumn column= xAxis.getColumn();
            DasRow row= parent.getRow();
            
            eventMap= new int[column.getWidth()];
            for ( int k=0; k<eventMap.length; k++ ) eventMap[k]= -1;
            
            QDataSet xds= xmins;
            Units xunits= SemanticOps.getUnits(xds);

            if ( cds.length()>0 ) {
                
                int ivds0= 0;
                int ivds1= xmins.length();

                GrannyTextRenderer gtr= new GrannyTextRenderer();

                int ixmax0= -999;

                for ( int i=ivds0; i<ivds1; i++ ) {

                    long dt= System.currentTimeMillis()-t0;
                    if ( i%10==0 && dt > 300 ) {
                        parent.postMessage( this, "renderer ran out of time, dataset truncated", DasPlot.WARNING, null, null);
                        break;
                    }

                    int ixmin= (int)xAxis.transform( xmins.value(i),xunits);
                    int ixmax= (int)xAxis.transform( xmaxs.value(i),xunits);

                    int iwidth= Math.max( ixmax- ixmin, 1 ); 

                    if ( lcolor!=null ) {
                        int irgb= (int)lcolor.value(i);
                        int rr= ( irgb & 0xFF0000 ) >> 16;
                        int gg= ( irgb & 0x00FF00 ) >> 8;
                        int bb= ( irgb & 0x0000FF );
                        int aa= ( irgb >> 24 & 0xFF );
                        if ( aa>0 ) {
                            g.setColor( new Color( rr, gg, bb, aa ) );
                        } else {
                            g.setColor( new Color( rr, gg, bb ) );
                        }
                    }
                    
                    if ( column.getDMinimum() < ixmax || column.getDMaximum() > ixmin ) { // if any part is visible
                        if ( iwidth==0 ) iwidth=1;
                        if ( dt<100 ) sa.append( new Rectangle( ixmin-2, row.getDMinimum(), iwidth+4, row.getHeight() ), false );
                        g.fill( new Rectangle( ixmin, row.getDMinimum(), iwidth, row.getHeight() ) );
                        int im= ixmin-column.getDMinimum();
                        int em0= im-1;
                        int em1= im+iwidth+1;
                        for ( int k=em0; k<em1; k++ ) {
                            if ( k>=0 && k<eventMap.length ) eventMap[k]= i;
                        }
                        if ( this.showLabels ) {
                            DatumRange dr= new DatumRange( xmins.value(i), xmaxs.value(i), xunits );
                            Datum d= eu.createDatum( msgs.value(i) );
                            String text= textSpecifier.getText( dr, d );
                            gtr.setString(g1,text);
                            gtr.draw( g1, ixmin+2, row.getDMinimum()+(int)gtr.getAscent() );
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

            long dt= System.currentTimeMillis()-t0;
            if ( dt>100 ) {
                sa= new GeneralPath();
                sa.append( org.das2.graph.DasDevicePosition.toRectangle( row, column ), false );
            }
        }
        g.dispose();

        selectionArea= sa;
        
    }

    @Override
    public void setDataSet(QDataSet ds) {
        super.setDataSet(ds);
        cds= makeCanonical(ds);
    }


    @Override
    public Icon getListIcon() {
        return new ImageIcon(SpectrogramRenderer.class.getResource("/images/icons/eventsBar.png"));
    }

    @Override
    public void setControl(String s) {
        super.setControl(s);
        this.setShowLabels( getBooleanControl( "showLabels", isShowLabels() ) );
    }

    private Color color= new Color(100,100,100); 
    
    public Color getColor() {
        return color;
    }
    
    /**
     * set the color to use when the data doesn't specify a color.  If an alpha channel is specified, then
     * this alpha value is used, otherwise 80% is used.
     * @param color
     */
    public void setColor( Color color ) {
        Color old= this.color;
        this.color= color;
        cds= makeCanonical(getDataSet());
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
        if ( parent!=null ) {
            parent.invalidateCacheImage();
            parent.repaint();
        }
        propertyChangeSupport.firePropertyChange(PROP_SHOWLABELS, oldShowLabels, showLabels);
    }

    public static final String PROP_COLOR_SPECIFIER= "colorSpecifier";

    private ColorSpecifier colorSpecifier=null;

    /**
     * set this to be an object implementing ColorSpecifier interface, if more than
     * one color is to be used when drawing the bars.  Setting this to null will
     * restore the initial behavior of drawing all bars in one color (or with rank 2 bundle containing color).
     */
    public void setColorSpecifier( ColorSpecifier spec ) {
        Object old= this.colorSpecifier;
        this.colorSpecifier= spec;
        cds= makeCanonical(ds);
        propertyChangeSupport.firePropertyChange( PROP_COLOR_SPECIFIER, old , spec );
        super.invalidateParentCacheImage();
    }

    public ColorSpecifier getColorSpecifier( ) {
        return this.colorSpecifier;
    }

    /**
     * Old TextSpecifier provided an alternate means to get text for any datum, allowing the user to avoid use of EnumerationUnits.
     * This is currently not used in this version of the library.
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
