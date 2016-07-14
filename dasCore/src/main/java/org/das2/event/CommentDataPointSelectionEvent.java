/*
 * DataTupleSelectionEvent.java
 *
 * Created on October 13, 2003, 11:27 AM
 */

package org.das2.event;

import org.das2.datum.Datum;
import org.das2.event.DataPointSelectionEvent;

/**
 * @deprecated  use DataPointSelectionEvent planes hashmap "comment" key
 * @author  jbf
 */
public class CommentDataPointSelectionEvent extends DataPointSelectionEvent {
          
    String comment;
    
    public static CommentDataPointSelectionEvent create( DataPointSelectionEvent e, String comment ) {
        CommentDataPointSelectionEvent ce =  new CommentDataPointSelectionEvent( e.getSource(), e.getX(), e.getY(), comment );
        ce.setDataSet(e.getDataSet());
        return ce;
    }
    
    /** Creates a new instance of DataTupleSelectionEvent */
    private CommentDataPointSelectionEvent( Object source, Datum x, Datum y, String comment ) {        
        super( source, x, y );
        this.comment= comment;
    }
        
    public String getComment() {
        return comment;
    }
    
}
