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
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;
import static org.das2.graph.Renderer.encodeBooleanControl;
import org.das2.util.GrannyTextRenderer;
import org.das2.qds.DDataSet;
import org.das2.qds.DataSetOps;
import org.das2.qds.DataSetUtil;
import org.das2.qds.IDataSet;
import org.das2.qds.JoinDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.RankZeroDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.TagGenDataSet;
import org.das2.qds.WritableDataSet;
import org.das2.qds.ops.Ops;
import org.das2.qds.util.DataSetBuilder;
import org.das2.util.LoggerManager;


/**
 * Draw colored horizontal bars for the dataset, marking events datasets or modes of the data.  This expects
 * a QDataSet with the canonical scheme:
 *<blockquote><pre><small>{@code
 *    Events[:,BUNDLE_1=4] where the columns are:
 *      BUNDLE_1=startTime,stopTime,Color,Message
 *    startTime,stopTime are in some time location unit.  stopTime may also be an offset from startTime (e.g. seconds)
 *    Color is an int, that is either 0xRRGGBB or 0xAARRGGBB.
 *    Message is any datum, so typically an enumeration unit is used.
 *}</small></pre></blockquote>
 * Note this also contains systems for coloring data in old schemes, such as the colorSpecifier interface and textSpecifier.
 * These should not be used when a dataset will be sufficient.
 *
 * @see org.das2.qds.examples.Schemes#eventsList() 
 * @author Jeremy
 */
public class EventsRenderer extends Renderer {
    
    public static final String PROP_COLOR = "color";

    /**
     * if true, then only then eventsMap is used, otherwise we look though all the events for hits.
     */
    private boolean useOnlyEventsMap= false;
    
    private static Logger logger= LoggerManager.getLogger("das2.graphics.renderer.events");
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
        } else if ( ds.rank()==0 ) {
            xmins= Ops.join( null, ds );
            xmaxs= xmins;
        } else {
            xmins= SemanticOps.xtagsDataSet(ds);
            if ( ds.length(0)>1 ) {
                xmaxs= DataSetOps.unbundle( ds,1 );
            } else {
                xmaxs= xmins;
            }
        }

        Units u0= SemanticOps.getUnits(xmins);
        Units u1= SemanticOps.getUnits(xmaxs);

        if ( xmins.length()==0 ) {
            xrange=  DDataSet.wrap( new double[] {0,1}, u0 );

        } else {
            if ( UnitsUtil.isIntervalOrRatioMeasurement(u1) ) {
                //TODO: probably the day/days containing would be better
                xrange= Ops.extent(xmins);
                if ( !u1.isConvertibleTo(u0) && u1.isConvertibleTo(u0.getOffsetUnits()) ) {
                    xmaxs= Ops.add( xmins, xmaxs );
                    xrange= Ops.extent(xmaxs,xrange);
                } else {
                    xrange= Ops.extent(xmaxs,xrange);
                }
            } else {
                xrange= DDataSet.createRank1(2);
                ((DDataSet)xrange).putValue(0,0);
                ((DDataSet)xrange).putValue(1,10);
            }
            
            if ( xrange.value(0)<xrange.value(1) ) {
                xrange= Ops.rescaleRangeLogLin(xrange, -0.1, 1.1 );
            } else {
                if ( UnitsUtil.isTimeLocation(u0) ) {
                    QDataSet dx= DDataSet.wrap( new double[] {-0.5,0.5}, Units.hours ); // otherwise you get a one microsecond wide plot
                    xrange= Ops.add( xrange, dx );
                } else {
                    QDataSet dx= DDataSet.wrap( new double[] {-1,1}, u0.getOffsetUnits() );
                    xrange= Ops.add( xrange, dx );
                }
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
            return false;
        }
    }


    public interface ColorSpecifier {
        /**
         * returns a color for the given datum.  null may be returned, indicating the
         * default color should be used.
         * @param d the Datum to represent.
         * @return color for the Datum.
         */
        Color getColor( Datum d );
    }
    
    public interface TextSpecifier {
        /**
         * returns the text for the given datum.  null may be returned, indicating the
         * default String.valueOf(d) should be used.
         * @param range the range of the event
         * @param d the Datum associated with the range
         * @return the string for the datum and range shown in a popup.
         */
        String getText( DatumRange range, Datum d );
    }
    
    public static final TextSpecifier DEFAULT_TEXT_SPECIFIER= new TextSpecifier() {
        @Override
        public String getText( DatumRange dr, Datum d ) {
            Datum sy= DatumUtil.asOrderOneUnits( dr.width() );
            if ( dr.width().value()== 0 ) {
                if ( d.toString().equals(dr.min().toString() ) ) {
                    return d.toString();
                } else {
                    return String.format( "%s", d ) ; // don't indicate time if it's zero width, because this is redundant and clutter.
                }
            } else {
                String ssy= sy.toString().trim();
                return String.format( "%s (%s)!c%s", dr, ssy, d ) ;
            }
        }
    };
    
    
    @Override
    protected void installRenderer() {
        MouseModule mm= getMouseModule();
        DasPlot parent= getParent();
        parent.getDasMouseInputAdapter().addMouseModule( mm );
        parent.getDasMouseInputAdapter().setPrimaryModule( mm );
        super.installRenderer();
    }

    @Override
    protected void uninstallRenderer() {
        MouseModule mm= getMouseModule();
        DasPlot parent= getParent();
        parent.getDasMouseInputAdapter().removeMouseModule(mm);
        super.uninstallRenderer(); 
    }
    
    
    private class EventLabelDragRenderer extends LabelDragRenderer {
        DasPlot parent;
        EventLabelDragRenderer( DasPlot parent ) {
            super( parent );
            this.parent= parent;
            this.setTooltip(true);
        }
        @Override
        public Rectangle[] renderDrag( Graphics g, Point p1, Point p2 ) {
            QDataSet vds= (QDataSet)getDataSet();

            if ( vds==null ) return new Rectangle[0];
            if ( vds.rank()==0 ) {
                return new Rectangle[0];
            }
            
            if ( vds.length()==0 ) return new Rectangle[0];

            QDataSet ds= cds;
            if ( ds==null ) return new Rectangle[0];
            
            QDataSet xmins= DataSetOps.unbundle( ds,0 );
            QDataSet xmaxs= DataSetOps.unbundle( ds,1 );
            QDataSet msgs= DataSetOps.unbundle(ds,ds.length(0)-1);

            int ix= (int)p2.getX() - parent.getColumn().getDMinimum();

            Datum px= parent.getXAxis().invTransform( p2.getX() );

            if ( ix<0 || eventMap==null || ix >= eventMap.length ) {
                setLabel(null);
            } else {
                Units sxunits= SemanticOps.getUnits(xmins);
                Units zunits= SemanticOps.getUnits(msgs);
                Units sxmaxunits= SemanticOps.getUnits( xmaxs );

                List<Integer> ii= new ArrayList();
                if ( useOnlyEventsMap==true && eventMap[ix]>-1 ) {
                    ii.add( eventMap[ix] );
                } else {
                    if ( eventMap[ix]>-1 ) ii.add( eventMap[ix] );
                    for ( int i=0; i<xmaxs.length(); i++ ) {
                        double sxmin= xmins.value(i);
                        double sxmax= xmaxs.value(i);
                        if ( !sxmaxunits.isConvertibleTo(sxunits) ) {
                            if ( sxmaxunits.isConvertibleTo(sxunits.getOffsetUnits() ) ) {
                                sxmax= sxmin + sxmaxunits.convertDoubleTo( sxunits.getOffsetUnits(), sxmax );
                            } else {
                                sxmax= sxmin;
                            }
                        } else {
                            sxmax= sxmaxunits.convertDoubleTo( sxunits, sxmax );
                        }

                        if ( sxmax<sxmin ) {
                            setLabel( "Error, sxmax<sxmin: "+ Datum.create( sxmax,sxunits)+" < "+ Datum.create(sxmin,sxmaxunits) );
                        } else {                            
                            DatumRange dr= new DatumRange( sxmin, sxmax, sxunits );
                            if ( !dr.getUnits().isConvertibleTo(px.getUnits()) )  {
                                logger.fine("inconvertible units");
                                return new Rectangle[0];
                            }
                            if ( dr.contains(px) ) {
                                if ( !ii.contains(i) )  ii.add(i);
                            }
                        }
                    }
                }

                if ( ii.size()>0 ) {
                    StringBuilder sb= new StringBuilder();
                    int count= 0;
                    for ( Integer ii1 : ii ) {
                        int i = ii1;
                        double sxmin= xmins.value(i);
                        double sxmax= xmaxs.value(i);
                        if ( !sxmaxunits.isConvertibleTo(sxunits) ) {
                            if ( sxmaxunits.isConvertibleTo(sxunits.getOffsetUnits() ) ) {
                                sxmax= sxmin + sxmaxunits.convertDoubleTo( sxunits.getOffsetUnits(), sxmax );
                            } else {
                                sxmax= sxmin;
                            }
                        } else {
                            sxmax= sxmaxunits.convertDoubleTo( sxunits, sxmax );
                        }
                        if ( sxmax<sxmin ) {
                            String ss;
                            DatumRange dr= new DatumRange( sxmin, sxmin, sxunits );
                            try {
                                Datum sz= zunits.createDatum( msgs.value(i) );
                                ss= textSpecifier.getText( dr, sz );
                            } catch ( RuntimeException ex ) {
                                ss= "" + dr + " fill";
                            }
                            sb.append(ss).append( "!c");
                        } else {
                            String ss;
                            DatumRange dr= new DatumRange( sxmin, sxmax, sxunits );
                            try {
                                Datum sz= zunits.createDatum( msgs.value(i) );
                                ss= textSpecifier.getText( dr, sz );
                            } catch ( RuntimeException ex ) {
                                ss= "" + dr + " fill";
                            }
                            sb.append(ss).append( "!c");
                        }
                        count++;
                        if ( count>10 ) {
                            break;
                        }
                    }
                    if ( ii.size()>count ) {
                        sb.append("(").append(ii.size()-count).append( " more items not shown)");
                    }
                    setLabel( sb.toString() );
                } else {
                    setLabel(null);
                }
            }
            return super.renderDrag( g, p1, p2 );
        }
        
    }
    
    private MouseModule mouseModule=null;
    private MouseModule getMouseModule() {
        if ( mouseModule==null ) {
            DasPlot parent= getParent();
            mouseModule= new MouseModule( parent, new EventLabelDragRenderer(parent), "Event Lookup" );
        }
        return mouseModule;
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
        RankZeroDataSet cad= org.das2.qds.DataSetUtil.guessCadenceNew( dep0, null );
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
     * @param vds events list in one of several supported forms
     * @return rank 2 N by 4 dataset.
     */
    private QDataSet makeCanonical( QDataSet vds ) {
        logger.entering( "EventsRenderer", "makeCanonical" );
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
                try {
                    xmins= DataSetOps.unbundle( vds,0 );
                    xmaxs= DataSetOps.unbundle( vds,1 );
                } catch ( IndexOutOfBoundsException ex ) {
                    if ( vds.length()==0 ) {
                        //TODO: unbundle should be able to handle this.
                        logger.exiting( "EventsRenderer", "makeCanonical", "null");
                        return null;
                    } else {
                        throw ex;
                    }
                }

                if ( useColor ) {
                    colors= Ops.replicate( getColor().getRGB(), xmins.length() );
                } else {
                    if ( vds.length(0)>3 ) {
                        colors= DataSetOps.unbundle( vds,2 );
                    } else {
                        colors= Ops.replicate( getColor().getRGB(), xmins.length() );
                    }
                }
                
            } else if ( dep0.rank()==2 ) {
                if ( SemanticOps.isBins(dep0) ) {
                    xmins= DataSetOps.slice1( dep0, 0 );
                    xmaxs= DataSetOps.slice1( dep0, 1 );
                    colors= Ops.replicate( getColor().getRGB(), xmins.length() );
                    Units u0= SemanticOps.getUnits(xmins );
                    Units u1= SemanticOps.getUnits(xmaxs );
                    if ( !u1.isConvertibleTo(u0) && u1.isConvertibleTo(u0.getOffsetUnits()) ) {
                        xmaxs= Ops.add( xmins, xmaxs );
                    }
                } else {
                    postMessage( "DEPEND_0 is rank 2 but not bins", DasPlot.WARNING, null, null );
                    logger.exiting( "EventsRenderer", "makeCanonical", "null" );
                    return null;
                }
                
            } else  if ( dep0.rank() == 1 ) {
                Datum width= SemanticOps.guessXTagWidth( dep0, null );
                if ( width!=null ) {
                    width= width.divide(2);
                } else {
                    QDataSet sort= Ops.sort(dep0);
                    QDataSet diffs= Ops.diff( DataSetOps.applyIndex(dep0,0,sort,false) );
                    QDataSet w= Ops.reduceMin( diffs,0 );
                    width= DataSetUtil.asDatum(w);
                }
                xmins= Ops.subtract(dep0, org.das2.qds.DataSetUtil.asDataSet(width) );
                xmaxs= Ops.add(dep0, org.das2.qds.DataSetUtil.asDataSet(width) );
                colors= Ops.replicate( getColor().getRGB(), xmins.length() );

            } else {
                postMessage( "rank 2 dataset must have dep0 of rank 1 or rank 2 bins", DasPlot.WARNING, null, null );
                logger.exiting( "EventsRenderer", "makeCanonical", "null");
                return null;
            }

            msgs= DataSetOps.unbundle( vds, vds.length(0)-1 );

        } else if ( vds.rank()==1 ) {
            QDataSet dep0= (QDataSet) vds.property(QDataSet.DEPEND_0);
            if ( dep0==null ) {
                if ( UnitsUtil.isNominalMeasurement(SemanticOps.getUnits(vds)) ) {
                    xmins= new TagGenDataSet(vds.length(),1.0,0.0);
                    xmaxs= new TagGenDataSet(vds.length(),1.0,1.0);
                    msgs= vds;
                } else {
                    xmins= vds;
                    xmaxs= vds;
                    msgs= vds;
                }
            } else if ( dep0.rank() == 2  ) {
                if ( SemanticOps.isBins(dep0) ) {
                    xmins= DataSetOps.slice1( dep0, 0 );
                    xmaxs= DataSetOps.slice1( dep0, 1 );
                    Units u0= SemanticOps.getUnits(xmins );
                    Units u1= SemanticOps.getUnits(xmaxs );
                    if ( !u1.isConvertibleTo(u0) && u1.isConvertibleTo(u0.getOffsetUnits()) ) {
                        xmaxs= Ops.add( xmins, xmaxs );
                    }
                    msgs= vds;
                } else {
                    postMessage( "DEPEND_0 is rank 2 but not bins", DasPlot.WARNING, null, null );
                    logger.exiting( "EventsRenderer", "makeCanonical", "null");
                    return null;
                }
            } else if ( dep0.rank() == 1 ) {
                Datum width= SemanticOps.guessXTagWidth( dep0, null );
                if ( width!=null ) {
                    width= width.divide(2);
                } else {
                    Units dep0units= SemanticOps.getUnits(dep0);
                    if ( UnitsUtil.isNominalMeasurement(dep0units) ) {
                        throw new IllegalArgumentException("dep0units are norminal units");
                    } else {
                        QDataSet sort= Ops.sort(dep0);
                        QDataSet diffs= Ops.diff( DataSetOps.applyIndex(dep0,0,sort,false) );
                        QDataSet w= Ops.reduceMin( diffs,0 );
                        width= DataSetUtil.asDatum(w);
                    }
                }
                xmins= Ops.subtract(dep0, org.das2.qds.DataSetUtil.asDataSet(width) );
                xmaxs= Ops.add(dep0, org.das2.qds.DataSetUtil.asDataSet(width) );                
                msgs= vds;
            } else {
                postMessage( "dataset is not correct form", DasPlot.WARNING, null, null );
                logger.exiting( "EventsRenderer", "makeCanonical", "null");
                return null;
            }
            Color c0= getColor();
            int alpha= c0.getAlpha()==255 ? 
                    ( opaque ? 255 : 128 ) :
                    c0.getAlpha();
            Color c1= new Color( c0.getRed(), c0.getGreen(), c0.getBlue(), alpha );
            int irgb= c1.getRGB();
            
            colors= Ops.replicate( irgb, xmins.length() );
            
        } else if ( vds.rank()==0 ) {
            xmins= Ops.replicate(vds,1); // increase rank from 0 to 1.
            xmaxs= xmins;
            Color c0= getColor();
            int alpha= c0.getAlpha()==255 ? 
                    ( opaque ? 255 : 128 ) :
                    c0.getAlpha();
            Color c1= new Color( c0.getRed(), c0.getGreen(), c0.getBlue(), alpha );
            int irgb= c1.getRGB();
            colors= Ops.replicate( irgb, xmins.length() );
            msgs= Ops.replicate(vds,1);
        } else {            
            postMessage( "dataset must be rank 0, 1 or 2", DasPlot.WARNING, null, null );
            logger.exiting( "EventsRenderer", "makeCanonical", "null");
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

        if ( u1.isConvertibleTo( u0.getOffsetUnits() ) && !u1.isConvertibleTo(u0) ) { // maxes are dt instead of stopt.
            xmaxs= Ops.add( xmins, xmaxs );
        }

        QDataSet lds= Ops.bundle( xmins, xmaxs, colors, msgs );
        logger.exiting( "EventsRenderer", "makeCanonical", "lds");
        
        return lds;

    }


    @Override
    public void render(java.awt.Graphics2D g1, DasAxis xAxis, DasAxis yAxis ) {
        
        GeneralPath sa= new GeneralPath();

        QDataSet vds= (QDataSet)getDataSet();
        if (vds == null ) {
            DasLogger.getLogger(DasLogger.GRAPHICS_LOG).fine("null data set");
            return;
        }
        if ( ( vds.rank()>0 && vds.length() == 0) ) {
            DasLogger.getLogger(DasLogger.GRAPHICS_LOG).fine("empty data set");
            return;
        }
        
        Graphics2D g= ( Graphics2D ) g1;
        
        g.setColor(color);

        if ( cds==null ) {
            // a message should be posted by makeCanonical
            return;
        }

        String mode= this.mode.intern();

        QDataSet cds1= cds;
        
        if ( mode.equals("gantt2") ) {
            QDataSet xmins= DataSetOps.unbundle( cds,0 );
            QDataSet xmaxs= DataSetOps.unbundle( cds,1 );

            QDataSet allBefore= Ops.lt( xmaxs, xAxis.getDatumRange().min() );
            QDataSet allAfter= Ops.gt( xmins, xAxis.getDatumRange().max() );
            QDataSet r= Ops.where( Ops.not( Ops.or( allBefore, allAfter ) ) );

            cds1= Ops.applyIndex( cds, r );                

            ganttMode= true;

        }
            
        QDataSet xmins= DataSetOps.unbundle( cds1,0 );
        QDataSet xmaxs= DataSetOps.unbundle( cds1,1 );
        QDataSet msgs= DataSetOps.unbundle( cds1,3 );
        Units eu= SemanticOps.getUnits( msgs );
        QDataSet lcolor= DataSetOps.unbundle( cds1,2 );
        
        long t0= System.currentTimeMillis();

        DasPlot parent= getParent();
        
        Rectangle current= null;
        
        if ( lastException!=null ) {
            renderException( g, xAxis, yAxis, lastException );
            
        } else {
            
            DasColumn column= xAxis.getColumn();
            DasRow row= parent.getRow();
            
            eventMap= new int[column.getWidth()];
            for ( int k=0; k<eventMap.length; k++ ) eventMap[k]= -1;
            
            QDataSet wxmins= SemanticOps.weightsDataSet(xmins);
            
            QDataSet xds= xmins;
            Units xunits= SemanticOps.getUnits(xds);
            if ( !xunits.isConvertibleTo( xAxis.getUnits() ) ) {
                if ( UnitsUtil.isRatioMeasurement(xunits) ) {
                    parent.postMessage( this, "x axis units changed from \""+xunits + "\" to \"" + xAxis.getUnits() + "\"", DasPlot.INFO, null, null );
                    xunits= xAxis.getUnits();
                }
            }        

            if ( cds1.length()>0 ) {
                
                int ivds0= 0;
                int ivds1= xmins.length();

                Font f= getParent().getFont();
                if ( getFontSize()!=null && getFontSize().length()>0 && !getFontSize().equals("1em") ) {
                    try {
                        double[] size= DasDevicePosition.parseLayoutStr(getFontSize());
                        double s= f.getSize2D() * size[0]/100 + f.getSize2D() * size[1] + size[2];
                        f= f.deriveFont((float)s);
                    } catch ( ParseException ex ) {
                        logger.log( Level.WARNING, ex.getMessage(), ex );
                    }
                }
                g1.setFont(f);

                GrannyTextRenderer gtr= new GrannyTextRenderer();

                int gymax,gymin;
                QDataSet u;
                Map<Integer,Integer> map= new HashMap<>();
                Map<Integer,Integer> pam= new HashMap<>();
                try {
                    QDataSet s= Ops.sort(msgs);
                    u= Ops.uniq(msgs,s);
                    gymax= u.length();
                    gymin= 0;
                    for ( int i=0; i<u.length(); i++ ) {
                        map.put( (int)msgs.value((int)u.value(i)), i );
                        pam.put( i, (int)msgs.value((int)u.value(i)) );
                    }
                    
                } catch ( IndexOutOfBoundsException ex ) {
                    ex.printStackTrace();  // shouldn't happen, put breakpoint here
                    return;
                }
                        
                int lastMessageTailX= -10000; // avoid overlaps by keeping track of last right side.

                gtr.setString( g1,"xxx" );
                int textHeight= (int)gtr.getHeight();
                
                int imin= xAxis.getColumn().getDMinimum();
                int imax= xAxis.getColumn().getDMaximum();
                
                boolean drawLineThick= lineThick.trim().length()>0;
                if ( drawLineThick ) {
                    double t= DasDevicePosition.parseLayoutStr( this.lineThick, f.getSize2D(), getParent().getWidth(), 1.0 );
                    g.setStroke( lineStyle.getStroke( (float)t ) );
                }
                for ( int i=ivds0; i<ivds1; i++ ) {

                    long dt= System.currentTimeMillis()-t0;
                    
                    if ( i%10==0 && dt > renderTimeLimitMs ) {
                        parent.postMessage( this, "renderer ran out of time, dataset truncated", DasPlot.WARNING, null, null);
                        break;
                    }
                    
                    if ( wxmins.value(i)==0.0 ) { // allow for fill in events dataset.
                        continue;
                    }
                    
                    int ixmin= (int)xAxis.transform( xmins.value(i),xunits);
                    int ixmax= (int)xAxis.transform( xmaxs.value(i),xunits);

                    if ( ixmax<=-10000 ) {
                        continue;
                    }
                    if ( ixmin>=10000 ) {
                        continue;
                    }

                    int ixmin0= ixmin;
                    
                    ixmin= Math.max( ixmin, imin );
                    ixmax= Math.min( ixmax, imax );
                                            
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
                            g.setColor( new Color( rr, gg, bb, 128 ) );
                        }
                    }

                    if ( column.getDMinimum() < ixmax && ixmin0 < column.getDMaximum() ) { // if any part is visible
                        if ( iwidth==0 ) iwidth=1;
                        Rectangle r1;
                        if ( this.orbitMode ) {
                            r1= new Rectangle( ixmin, row.getDMaximum()-textHeight, iwidth-1, textHeight );
                            g.fill( r1 );
                        } else if ( this.ganttMode ) {
                            int ord=(int)msgs.value(i);
                            Integer iy= map.get(ord);
                            if ( iy!=null ) {
                                int iymin= row.getDMinimum() + row.getHeight() * (iy) / ( gymax - gymin ) + 1;
                                int iymax= row.getDMinimum() + row.getHeight() * (1+iy) / ( gymax - gymin ) - 1;
                                //int iymin= row.getDMinimum() + row.getHeight() * ((int)msgs.value(i)-gymin) / ( gymax - gymin + 1 ) + 1;
                                //int iymax= row.getDMinimum() + row.getHeight() * (1+(int)msgs.value(i)-gymin) / ( gymax - gymin + 1 ) - 1;
                                r1= new Rectangle( ixmin, iymin, iwidth, Math.max( iymax-iymin, 2 ) );
                                g.fill( r1 );
                            } else {
                                continue;
                            }
                        } else {
                            if ( iwidth<=1 && drawLineThick ) {
                                r1= new Rectangle( ixmin, row.getDMinimum(), iwidth, row.getHeight() );
                                Line2D.Double l1= new Line2D.Double( ixmin, row.getDMinimum(), ixmin, row.getDMaximum() );
                                g.draw( l1 );
                            } else {
                                r1= new Rectangle( ixmin, row.getDMinimum(), iwidth, row.getHeight() );
                                g.fill( r1 );
                                if ( drawLineThick ) {
                                    g.draw( r1 );
                                }
                            }
                        }
                        r1.x= r1.x-2;
                        r1.y= r1.y-2;
                        r1.width= r1.width+4;
                        r1.height= r1.height+4;
                        if ( current==null ) {
                            current= r1; 
                        } else if ( current.intersects(r1) ) {
                            current= current.union(r1);
                        } else {
                            sa.append( current, false );
                            current= r1;
                        }
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
                        if ( this.orbitMode ) {
                            if ( rotateLabel==0 ) {
                                try {
                                    String text= eu.createDatum( msgs.value(i) ).toString();
                                    gtr.setString(g1,text);
                                    Color c0= g1.getColor();
                                    g1.setColor( getParent().getBackground() );
                                    gtr.draw( g1, ixmin+2 -1, row.getDMaximum()-textHeight+(int)gtr.getAscent() );
                                    gtr.draw( g1, ixmin+2, row.getDMaximum()-textHeight+(int)gtr.getAscent() +1 );
                                    gtr.draw( g1, ixmin+2 +1, row.getDMaximum()-textHeight+(int)gtr.getAscent() );
                                    gtr.draw( g1, ixmin+2, row.getDMaximum()-textHeight+(int)gtr.getAscent() -1 );
                                    g1.setColor( c0 );
                                    gtr.draw( g1, ixmin+2, row.getDMaximum()-textHeight+(int)gtr.getAscent() );
                                    lastMessageTailX= ixmin+2 + (int)gtr.getWidth();
                                } catch ( RuntimeException e ) {
                                    // don't show where there was a bad value.
                                }
                            } else if ( rotateLabel==90 ) {
                                try {
                                    String text= eu.createDatum( msgs.value(i) ).toString();
                                    gtr.setString(g1,text);
                                    //Graphics2D g2= (Graphics2D)g1.create();
                                    //g2.rotate(Math.PI/2);
                                    Graphics2D g2= (Graphics2D)g1.create();

                                    g2.translate((int)(ixmin0+2+gtr.getAscent()), row.getDMaximum()-textHeight+(int)gtr.getAscent());
                                    g2.setColor( color );
                                    g2.rotate(-Math.PI/2);
                                    gtr.draw( g2, textHeight, (int)(gtr.getAscent()-gtr.getHeight()) );
    //                                Rectangle b= gtr.getBounds();
    //                                for ( int k=0; k<5; k++ ) {
    //                                    g2.draw(b);
    //                                    b= GraphUtil.shrinkRectangle( b, 110 );
    //                                }
    //                                b= gtr.getBounds();
    //                                for ( int k=0; k<5; k++ ) {
    //                                    g2.draw(b);
    //                                    b= GraphUtil.shrinkRectangle( b, 90 );
    //                                }
                                    lastMessageTailX= ixmin+2 + (int)gtr.getWidth();
                                } catch ( RuntimeException e ) {
                                    // don't show where there was a bad value.
                                }
                            }
                        }
                    }
                }
                if ( current!=null ) sa.append( current, false );

                if ( ganttMode ) {
                    g1.setColor(color);
                    int di= Math.max( 1, ( gymax-gymin-1)  / ( row.getHeight() / textHeight ) );
                    for ( int i=gymin; i<gymax; i=i+di ) {
                        gtr.setString( g1, eu.createDatum(pam.get(i)).toString() );
                        int iymin= row.getDMinimum() + row.getHeight() * (i-gymin) / ( gymax - gymin );
                        Color c0= g1.getColor();
                        g1.setColor( Color.white );
                        gtr.draw( g1, column.getDMinimum() + textHeight/3 -1, iymin + textHeight ); 
                        gtr.draw( g1, column.getDMinimum() + textHeight/3, iymin + textHeight +1 );
                        gtr.draw( g1, column.getDMinimum() + textHeight/3 +1, iymin + textHeight );
                        gtr.draw( g1, column.getDMinimum() + textHeight/3, iymin + textHeight -1 );
                        g1.setColor( c0 );
                        gtr.draw( g1, column.getDMinimum() + textHeight/3, iymin + textHeight );
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
    
    private int renderTimeLimitMs = 3000;

    @Override
    public void setDataSet(QDataSet ds) {
        cds= makeCanonical(ds);
        super.setDataSet(ds);
    }


    @Override
    public Icon getListIcon() {
        return new ImageIcon(SpectrogramRenderer.class.getResource("/images/icons/eventsBar.png"));
    }

    @Override
    public void setControl(String s) {
        if ( this.control.equals(s) ) {
            return;
        }
        super.setControl(s);
        this.setShowLabels( getBooleanControl( "showLabels", false ) );
        this.setOrbitMode( getBooleanControl( "orbitMode", false ));
        this.setFontSize( getControl( Renderer.CONTROL_KEY_FONT_SIZE, "1em" ));
        this.setGanttMode( getBooleanControl( "ganttMode", false ));
        this.setLineStyle( Renderer.decodePlotSymbolConnectorControl( getControl( PROP_LINESTYLE, lineStyle.toString() ), lineStyle ) );
        this.setLineThick( getControl( Renderer.CONTROL_KEY_LINE_THICK, "" ) );
        this.setOpaque( getBooleanControl( "opaque", false ) );
        if ( hasControl("color") ) {
            this.setColor( getColorControl( Renderer.CONTROL_KEY_COLOR,color) );
            this.useColor= true;
        } else {
            this.setColor( new Color(100,100,100) );
        }
    }

    @Override
    public String getControl() {
        Map<String,String> controls= new LinkedHashMap();
        controls.put( "showLabels", encodeBooleanControl( isShowLabels() ) );
        controls.put( "orbitMode", encodeBooleanControl( isOrbitMode() ) );
        controls.put( Renderer.CONTROL_KEY_FONT_SIZE, getFontSize() );
        controls.put( "ganttMode", encodeBooleanControl( isGanttMode() ) );
        controls.put( Renderer.CONTROL_KEY_LINE_STYLE, getLineStyle().toString() );
        controls.put("lineThick", getLineThick() );
        controls.put("opaque", encodeBooleanControl( isOpaque() ) );
        if ( this.useColor ) {
            controls.put( Renderer.CONTROL_KEY_COLOR, encodeColorControl(color) );
        }
        return Renderer.formatControl(controls);
    }

    private boolean useColor= false;
    
    private Color color= new Color(100,100,100); 
    
    public Color getColor() {
        return color;
    }
    
    private PsymConnector lineStyle = PsymConnector.SOLID;

    public static final String PROP_LINESTYLE = "lineStyle";

    public PsymConnector getLineStyle() {
        return lineStyle;
    }

    public void setLineStyle(PsymConnector lineStyle) {
        PsymConnector oldLineStyle = this.lineStyle;
        this.lineStyle = lineStyle;
        if ( !oldLineStyle.equals(lineStyle) ) {
            super.invalidateParentCacheImage();
        }
        propertyChangeSupport.firePropertyChange(PROP_LINESTYLE, oldLineStyle, lineStyle);
    }

    private String lineThick = "";

    public static final String PROP_LINETHICK = "lineThick";

    /**
     * the line thickness, examples include "5pt" and "0.1em"
     * @return 
     */
    public String getLineThick() {
        return lineThick;
    }

    public void setLineThick(String lineThick) {
        String oldLineThick = this.lineThick;
        this.lineThick = lineThick;
        if ( !oldLineThick.equals(lineThick) ) {
            super.invalidateParentCacheImage();
        }
        propertyChangeSupport.firePropertyChange(PROP_LINETHICK, oldLineThick, lineThick);
    }
    
    private boolean opaque = false;

    public static final String PROP_OPAQUE = "opaque";

    public boolean isOpaque() {
        return opaque;
    }

    public void setOpaque(boolean opaque) {
        boolean oldOpaque = this.opaque;
        this.opaque = opaque;
        if ( oldOpaque!=opaque ) {
            cds= makeCanonical(ds);
            super.invalidateParentCacheImage();
        }
        propertyChangeSupport.firePropertyChange( PROP_OPAQUE, oldOpaque, opaque );
    }
    
    /**
     * set the color to use when the data doesn't specify a color.  If an alpha channel is specified, then
     * this alpha value is used, otherwise 80% is used.
     * @param color
     */
    public void setColor( Color color ) {
        Color old= this.color;
        this.color= color;
        if ( !old.equals(color) ) {
            cds= makeCanonical(ds);
            super.invalidateParentCacheImage();
        }
        propertyChangeSupport.firePropertyChange( PROP_COLOR, old, color);
    }

    public int getRenderTimeLimitMs() {
        return renderTimeLimitMs;
    }

    public void setRenderTimeLimitMs(int renderTimeLimitMs) {
        this.renderTimeLimitMs = renderTimeLimitMs;
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
        DasPlot parent= getParent();
        if ( parent!=null ) {
            parent.invalidateCacheImage();
            parent.repaint();
        }
        propertyChangeSupport.firePropertyChange(PROP_SHOWLABELS, oldShowLabels, showLabels);
    }

    private String mode = "";

    public static final String PROP_MODE = "mode";

    public String getMode() {
        return mode;
    }

    /**
     * if non-empty, then use this named mode.  Named modes include:<ul>
     * <li>gantt2 - only show the items which are visible, normally all values are visible
     * </ul>
     * @param mode 
     */
    public void setMode(String mode) {
        String oldMode = this.mode;
        this.mode = mode;
        propertyChangeSupport.firePropertyChange(PROP_MODE, oldMode, mode);
    }

    /**
     * orbitMode true means don't show times, draw bars with 1-pixel breaks
     */
    protected boolean orbitMode = false;
    public static final String PROP_ORBITMODE = "orbitMode";

    public boolean isOrbitMode() {
        return orbitMode;
    }

    public void setOrbitMode(boolean orbitMode) {
        boolean oldOrbitMode = this.orbitMode;
        this.orbitMode = orbitMode;
        propertyChangeSupport.firePropertyChange(PROP_ORBITMODE, oldOrbitMode, orbitMode);
    }
    
    /**
     * gantt mode true means the event types are laid out vertically.  The user
     * has very little control over the position, but at least you can see
     * common messages.
     */
    private boolean ganttMode = false;
    
    public static final String PROP_GANTTMODE = "ganttMode";

    public boolean isGanttMode() {
        return ganttMode;
    }

    public void setGanttMode(boolean ganttMode) {
        boolean oldGanttMode = this.ganttMode;
        this.ganttMode = ganttMode;
        propertyChangeSupport.firePropertyChange(PROP_GANTTMODE, oldGanttMode, ganttMode);
    }

    private int rotateLabel = 0;

    /**
     * rotate the label counter clockwise to make more room in orbitMode.
     */
    public static final String PROP_ROTATELABEL = "rotateLabel";

    public int getRotateLabel() {
        return rotateLabel;
    }

    public void setRotateLabel(int rotateLabel) {
        int oldRotateLabel = this.rotateLabel;
        this.rotateLabel = rotateLabel;
        propertyChangeSupport.firePropertyChange(PROP_ROTATELABEL, oldRotateLabel, rotateLabel);
    }

    /**
     * fontSize allows the font to be rescaled.  1em is the default size.  2em is twice the size.  12pt is 12 pixels.
     */
    protected String fontSize = "1em";
    public static final String PROP_FONTSIZE = "fontSize";

    public String getFontSize() {
        return fontSize;
    }

    public void setFontSize(String fontSize) {
        String oldFontSize = this.fontSize;
        this.fontSize = fontSize;
        propertyChangeSupport.firePropertyChange(PROP_FONTSIZE, oldFontSize, fontSize);
    }

    public static final String PROP_COLOR_SPECIFIER= "colorSpecifier";

    private ColorSpecifier colorSpecifier=null;

    /**
     * set this to be an object implementing ColorSpecifier interface, if more than
     * one color is to be used when drawing the bars.  Setting this to null will
     * restore the initial behavior of drawing all bars in one color (or with rank 2 bundle containing color).
     * @param spec the color specifier.
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
