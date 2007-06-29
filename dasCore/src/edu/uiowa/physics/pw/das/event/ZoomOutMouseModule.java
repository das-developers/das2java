/*
 * ZoomOutMouseModule.java
 *
 * Created on June 29, 2007, 3:50 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.uiowa.physics.pw.das.event;

import edu.uiowa.physics.pw.das.datum.DatumRange;
import edu.uiowa.physics.pw.das.datum.DatumRangeUtil;
import edu.uiowa.physics.pw.das.graph.DasAxis;

/**
 *
 * @author jbf
 */
public class ZoomOutMouseModule extends BoxSelectorMouseModule {
    
    DasAxis parent;
    
    BoxSelectionListener createBoxSelectionListener() {
        return new BoxSelectionListener() {
            public void BoxSelected( BoxSelectionEvent event ) {
                DatumRange outerRange= parent.getDatumRange();
                DatumRange range= parent.isHorizontal() ? event.getXRange() : event.getYRange();
                range= parent.getTickV().enclosingRange( range, true );
                DatumRange newRange;
                if ( parent.isLog() ) {
                    double nmin= DatumRangeUtil.normalizeLog( range, outerRange.min() );
                    double nmax= DatumRangeUtil.normalizeLog( range, outerRange.max() );
                    nmin= nmin < -3 ? -3 : nmin; 
                    nmax= nmax > 3 ? 3 : nmax;
                    newRange= DatumRangeUtil.rescaleLog( outerRange, nmin, nmax );
                } else {
                    double nmin= DatumRangeUtil.normalize( range, outerRange.min() );
                    double nmax= DatumRangeUtil.normalize( range, outerRange.max() );
                    nmin= nmin < -3 ? -3 : nmin; 
                    nmax= nmax > 3 ? 3 : nmax;
                    newRange= DatumRangeUtil.rescale( outerRange, nmin, nmax );
                }
                parent.setDatumRange( newRange );
            }
            
        };
    }
    
    /** Creates a new instance of ZoomOutMouseModule */
    public ZoomOutMouseModule( DasAxis axis ) {
        super( axis,
                axis.isHorizontal() ? axis : null,
                axis.isHorizontal() ? null : axis,
                null,
                new BoxRenderer(axis),
                "Zoom Out" );
        this.parent= axis;
        addBoxSelectionListener( createBoxSelectionListener() );
    }
    
}
