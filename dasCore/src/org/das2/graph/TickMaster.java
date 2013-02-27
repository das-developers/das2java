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

    private HashSet<WeakReference<DasAxis>> axes= new HashSet();
    private HashSet<WeakReference<DasAxis>> pendingAxes= new HashSet();

    private static TickMaster instance= new TickMaster();

    public static TickMaster getInstance() {
        return instance;
    }

    public synchronized void register( DasAxis h ) {
        this.pendingAxes.add( new WeakReference(h) );
    }

    public synchronized void offerTickV( DasAxis h, TickVDescriptor ticks ) {

        HashSet<WeakReference<DasAxis>> rm= new HashSet();
        for ( WeakReference<DasAxis> da : this.pendingAxes ) {
            if ( da.get().getCanvas()!=null ) {
                rm.add(da);
            }
        }
        pendingAxes.removeAll(rm);
        this.axes.addAll(rm);

        List<String> axesUsing= new ArrayList();

        if ( h.isVisible() && h.isTickLabelsVisible() ) {
            logger.log( Level.FINE, "axes {0} offers ticks: {1}", new Object[] { h.getDasName(), ticks.toString() } );

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
    }


}
