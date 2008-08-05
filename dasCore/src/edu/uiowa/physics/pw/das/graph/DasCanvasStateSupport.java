/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.uiowa.physics.pw.das.graph;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * DasCanvasStateSupport is a registery where objects can tell the canvas they
 * intend to mutate the canvas, so canvas clients should know that its result 
 * may be incomplete.  This is first introduced to support server-side 
 * processing, where in Autoplot the Autolayout feature knows then layout is 
 * going to be adjusted, but doesn't have enough information to perform the 
 * function.  
 * 
 * Also, this may be used to address bugs like 
 * https://bugs-pw.physics.uiowa.edu/mantis/view.php?id=303, 
 * "strange intermediate states in transitions," since canvas painting can
 * be halted while the change is being performed.
 * 
 * @author jbf
 */
public class DasCanvasStateSupport {
    DasCanvas canvas;
    Map<Object,Object> changesPending;
    private static final Logger logger= Logger.getLogger( "das2.graphics" );
    
    DasCanvasStateSupport( DasCanvas canvas ) {
        this.canvas= canvas;
        this.changesPending= new HashMap<Object,Object>(); // client->lock
    }
    
    /**
     * the client knows a change will be coming, and the canvas' clients should
     * know that its current state will change soon.  Example pending changes
     * would be:
     *   layout because tick labels are changing
     *   data is loading
     *   
     * @param client the object that will perform the change.  This allows the
     *   canvas (and developers) identify who has registered the change.
     * @param lockObject object identifying the change.
     */
    void registerPendingChange( Object client, Object lockObject ) {
        logger.fine( "registerPendingChange "+lockObject+" by "+client);
        Object existingClient= changesPending.get(lockObject);
        if ( existingClient!=null ) {
            if ( existingClient!=client ) {
                throw new IllegalStateException( "lock object in use: "+lockObject + ", by "+changesPending.get(lockObject) );
            } else {
                return;
            }
        }
        boolean oldVal= this.isPendingChanges();
        changesPending.put( lockObject, client );
        canvas.firePropertyChange( PROP_PENDINGCHANGES, oldVal, true );
    }
     
    /**
     * performingChange tells that the change is about to be performed.  This
     * is a place holder in case we use a mutator lock, but currently does 
     * nothing.
     * @param lockObject
     */
    void performingChange( Object client, Object lockObject ) {
        
    }
    
    /**
     * the change is complete, and as far as the client is concerned, the canvas
     * is valid.
     * @param lockObject
     */
    void changePerformed( Object client, Object lockObject ) {
        logger.fine( "clearPendingChange "+lockObject+" by "+client);
        if ( changesPending.get(lockObject)==null ) {
            throw new IllegalStateException( "no such lock object: "+lockObject );
        }
        boolean oldVal= this.isPendingChanges();
        changesPending.remove(lockObject);
        canvas.firePropertyChange( PROP_PENDINGCHANGES, oldVal, true );
    }
    
    // --- properties 
    
    /**
     * someone has registered a pending change.
     */
    public static final String PROP_PENDINGCHANGES = "pendingChanges";

    public boolean isPendingChanges() {
        return changesPending.size() > 0;
    }

}

