/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.graph;

import java.util.HashSet;

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

    HashSet<DasAxis> axes= new HashSet<DasAxis>();

    private static TickMaster instance= new TickMaster();

    public static TickMaster getInstance() {
        return instance;
    }

    public void register( DasAxis h ) {
        this.axes.add(h);
    }

    public void unregister( DasAxis h ) {
        this.axes.remove(h);
    }

    public void offerTickV( DasAxis h, TickVDescriptor ticks ) {
        if ( h.isVisible() && h.isTickLabelsVisible() ) {
            for ( DasAxis a : this.axes ) {
                if ( ( a.getOrientation()==h.getOrientation() )
                        && ( Math.abs( a.getDLength()-h.getDLength() )<2 )
                        && ( a.isLog()==h.isLog() )
                        && ( a.getDatumRange().equals(h.getDatumRange() ) ) ) {
                    a.setTickV( ticks );
                }
            }
        }
    }


}
