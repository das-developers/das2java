/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.graph;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.das2.util.LoggerManager;

/**
 * Class for managing ticks.  When a set of plots shares a common column and
 * axis setting, they should all use the same ticks.  The other problem this
 * should fix is when a stack of plots all have heights within a couple of pixels
 * of one another, we should try to use the same ticks for them.
 *
 * DasAxis clients should offer their ticks to this master, which will decide
 * which set of ticks to use.
 *
 * @author jbf
 */
public class TickMaster {

    private static final Logger logger= LoggerManager.getLogger("das2.graphics.axis.tickmaster");;

    private final HashSet<WeakReference<DasAxis>> axes= new HashSet();
    private final HashSet<WeakReference<DasAxis>> pendingAxes= new HashSet();

    private static final TickMaster instance= new TickMaster();

    public static TickMaster getInstance() {
        return instance;
    }

    public synchronized void register( DasAxis h ) {
        this.pendingAxes.add( new WeakReference(h) );
    }

    /**
     * kludgy solution to problems where an axis didn't get the update from
     * its parent.  This is only called as a last-ditch measure.
     * @param h
     * @return 
     */
    public synchronized TickVDescriptor requestTickV( DasAxis h ) {
        DasAxis p= findParent(h);
        if ( p!=null ) {
            return p.getTickV();
        } else {
            return null;
        }
    }
    
    private DasAxis findParent(DasAxis h) {
        DasAxis parent= null;
        for ( WeakReference<DasAxis> da : this.axes ) {
            DasAxis a= da.get();

            if ( a==null ) {
                //rm.add(da);
            } else {
                if ( a.getCanvas()==null ) {
                    //rm.add(da);
                } else if ( ( a.getOrientation()==h.getOrientation() )
                        && ( Math.abs( a.getDLength()-h.getDLength() )<2 )
                        && ( a.isLog()==h.isLog() )
                        && ( a.getDatumRange().equals(h.getDatumRange() ) ) ) {
                    if ( a.isVisible() && a.isTickLabelsVisible() ) {
                        parent= a;
                        break;
                    }
                }
            }
        }
        return parent;
    }
    
    /**
     * offer a set of ticks.  This can be called from off or on the event thread,
     * but a new thread is started so the task is performed off the event thread.
     * @param h the axis
     * @param ticks null or the new ticks.
     */
    public synchronized void offerTickV( final DasAxis h, final TickVDescriptor ticks ) {
        
        logger.finer("Enter offerTickV");

        HashSet<WeakReference<DasAxis>> rm= new HashSet();
        for ( WeakReference<DasAxis> da : this.pendingAxes ) {
            DasAxis a= da.get();
            if ( a==null || a.getCanvas()!=null ) {
                rm.add(da);
            }
        }
        pendingAxes.removeAll(rm);
        this.axes.addAll(rm);

        List<String> axesUsing= new ArrayList();
        
        if ( h.isVisible() && h.isTickLabelsVisible() ) {
            if ( ticks==null ) {
                logger.log( Level.FINE, "axes {0} offers ticks: null", new Object[] { h.getDasName() } );
            } else {
                logger.log( Level.FINE, "axes {0} offers ticks: {1}", new Object[] { h.getDasName(), ticks.toString() } );
            }

            int count=0;
            int mecount=0; // how many times to do I see myself in the list?
            rm= new HashSet();
            for ( WeakReference<DasAxis> da : this.axes ) {
                DasAxis a= da.get();
                if ( a==h ) mecount++;
                if ( a==null ) {
                    rm.add(da);
                } else {
                    if ( a.getCanvas()==null ) {
                        rm.add(da);
                    } else if ( ( a.getOrientation()==h.getOrientation() )
                            && ( Math.abs( a.getDLength()-h.getDLength() )<2 )
                            && ( a.isLog()==h.isLog() )
                            && ( a.getDatumRange().equals(h.getDatumRange() ) ) ) {
                        a.resetTickV( ticks );
                        count++;
                        axesUsing.add(a.getDasName());
                    }
                }
            }
            if ( mecount==0 ) {
                h.resetTickV( ticks );
            }
            if ( rm.size()>0 ) {
                logger.log( Level.FINE, "remove old axes: {0}", rm );
                this.axes.removeAll(rm);
            }
            logger.log( Level.FINE, "axes using these ticks: {0} {1}", new Object[] { count, axesUsing } );
        }
        logger.finer("Exit offerTickV");
    }


}
