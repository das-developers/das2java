/**
 * 
 */
package org.das2.system;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Support class for encapsulating implementation of pendingChanges and mutator locks.
 * PendingChanges are a way of notifying the bean and other clients using the bean that changes are coming to
 * the bean.
 * mutatorLock() is a way for a client to get exclusive, read-only access to a bean.  This also sets the valueAdjusting
 * property.
 *
 * See http://das2.org/wiki/index.php/Pending_changes (Wiki was lost, but may be recoverable.)
 * @author jbf
 */
public class ChangesSupport {

    Map<Object, Object> changesPending;
    
    // Number of said changes, typically 1.
    Map<Object, Integer> changeCount;
    
    // threads, for debugging.
    Map<Object,String> threads;
    
    WeakReference<Object> parent;
    private static final Logger logger = Logger.getLogger("das2.system");

    /**
     * if the propertyChangeSupport is provided, then change messages will be sent to
     * it directly.  If null, then one is created with the parent as the source.
     * @param pcs the PropertyChangeSupport 
     * @param parent  the object this is supporting, for debugging purposes.
     */
    public ChangesSupport(PropertyChangeSupport pcs, Object parent) {
        this.parent = new WeakReference<>(parent);
        this.changesPending = new HashMap<>(); // client->lock
        this.changeCount= new HashMap<>();
        this.threads= new HashMap<>(); // lockObject -> thread ID.
        if ( pcs==null ) {
            pcs= new PropertyChangeSupport(parent);
        }
        this.propertyChangeSupport = pcs;
    }
    
    /**
     * returns the clients who have registered the change.  Note this
     * implementation only allows for one client for each lock object.
     * @param lockObject object identifying the change.
     * @return clients who have registered the change.
     */
    synchronized List<Object> whoIsChanging( Object lockObject ) {
        String msg= "whoIsChanging "+lockObject;
        logger.fine( msg );
        Object client= changesPending.get(lockObject);
        if ( client==null ) {
            return Collections.emptyList();
        } else {
            return Collections.singletonList(client);
        }
    }    

    /**
     * the client knows a change will be coming, and the canvas' clients should
     * know that its current state will change soon.  Example pending changes
     * would be:<ul>
     *   <li>layout because tick labels are changing
     *   <li>data is loading
     * </ul>
     * Note, it is okay to call this multiple times for the same client and lock object.
     * @param client the object that will perform the change.  This allows the
     *   canvas (and developers) identify who has registered the change.
     * @param lockObject object identifying the change.
     */
    public synchronized void registerPendingChange(Object client, Object lockObject) {
        String msg = "registerPendingChange " + lockObject + " by " + client + "  in " + parent.get();
        logger.fine(msg);
        Object existingClient = changesPending.get(lockObject);
        if (existingClient != null) {
            if (existingClient != client) {
                throw new IllegalStateException("lock object in use: " + lockObject + ", by " + changesPending.get(lockObject));
            } else if ( existingClient==client ) {
                logger.log( Level.FINE, "bug 1075: second change registered but the first was not done."); // this is somewhat harmless.  Don't bug clients with this message.
            } else {
                //Note, it is okay to call this multiple times for the same client and lock object.
                return;
            }
        }
        boolean oldVal = this.isPendingChanges();
        changesPending.put(lockObject, client);
        threads.put( lockObject, Thread.currentThread().toString() );
        propertyChangeSupport.firePropertyChange(PROP_PENDINGCHANGES, oldVal, true);
    }

    /**
     * return a list of all the pending changes.  These are returned in a
     * Map that goes from pending change to change manager.  
     *
     * @param changes a Map to which the changes will be added.
     */
    public synchronized void pendingChanges( Map<Object,Object> changes ) {
        changes.putAll( changesPending );
    }

    /**
     * performingChange tells that the change is about to be performed.  This
     * is a place holder in case we use a mutator lock, but currently does
     * nothing.  If the change has not been registered, it will be registered implicitly.
     * This will increment the internal count of how many times the change
     * ought to occur.
     * @param client the object that is mutating the bean.
     * @param lockObject an object identifying the change.  
     */
    public synchronized void performingChange(Object client, Object lockObject) {
        Object ownerClient= changesPending.get(lockObject);
        if ( ownerClient==null || ownerClient!=client ) {
            if ( ownerClient!=null && ownerClient!=client ) {
                logger.log(Level.INFO, "performingChange by client object is not owner {0}", client );
            }
            registerPendingChange( client, lockObject );
        }
        Integer count= changeCount.get(lockObject);
        if ( count==null ) {
            changeCount.put( lockObject, 1 );
        } else {
            changeCount.put( lockObject, count+1 );
        }

        logger.log( Level.FINE, "performingChange {0} by {1}  in {2}", new Object[]{lockObject, client, parent});
    }

    /**
     * the change is complete, and as far as the client is concerned, 
     * the canvas is valid.
     * @param client the object that is mutating the bean.
     * @param lockObject an object identifying the change.  
     */
    public synchronized void changePerformed(Object client, Object lockObject) {
        logger.log(Level.FINE, "clearPendingChange {0} by {1}  in {2}", new Object[]{lockObject, client, parent});
        Integer count= changeCount.get(lockObject);
        Object ownerClient= changesPending.get(lockObject);        
        if ( ownerClient == null) {
            // throw new IllegalStateException( "no such lock object: "+lockObject );  //TODO: handle multiple registrations by the same client
            logger.log(Level.INFO, "no lock object found for {0}", lockObject);
        } else if ( ownerClient!=client ) {
            logger.log(Level.INFO, "change performed client object is not owner {0}", ownerClient );
        }
        boolean oldVal = this.isPendingChanges();
        if ( count==null ) {
            logger.log(Level.INFO, "expect value for changeCount {0}, was performingChange called?", lockObject);
            count= 0;
        } else {
            count= count-1;
        }
        
        if ( count==0 ) {
            changesPending.remove(lockObject);
            threads.remove( lockObject );
            changeCount.remove(lockObject);
        } else if ( count>0) {
            changeCount.put(lockObject,count);
        } else {
            throw new IllegalStateException("what happened here--changeCount<0!");
        }
        
        propertyChangeSupport.firePropertyChange(PROP_PENDINGCHANGES, oldVal, true);
    }
    public static final String PROP_PENDINGCHANGES = "pendingChanges";

    /**
     * true if someone has registered a pending change.
     * @return true if someone has registered a pending change.
     */
    public boolean isPendingChanges() {
        return changesPending.size() > 0;
    }
    
    /**
     * allow check for particular change.
     * @param lockObject object identifying the change.
     * @return true if that particular change is pending.
     */
    public boolean isPendingChanges( Object lockObject ) {
        return changesPending.containsKey(lockObject);
    }
    
    /**
     * return a map listing the pending changes.  This is a thread-safe
     * read-only copy.
     * @return a map listing the pending changes.
     */
    public synchronized Map getChangesPending() {
        if ( changesPending.isEmpty() ) {
            return Collections.emptyMap();            
        } else {
            return new HashMap(changesPending);
        }
    }
    
    public static final String PROP_VALUEADJUSTING = "valueAdjusting";

    /**
     * true when the bean state is rapidly changing.
     * @return true when the bean state is rapidly changing.
     */
    public boolean isValueAdjusting() {
        return valueIsAdjusting;
    }
    private boolean valueIsAdjusting = false;

    private final Lock mutatorLock = new ReentrantLock() {
        @Override
        public void lock() {
            super.lock();
            if (valueIsAdjusting) {
                //System.err.println("lock is already set!");
            } else {
                propertyChangeSupport.firePropertyChange( PROP_VALUEADJUSTING, false, true );
                valueIsAdjusting = true;
            }
        }

        @Override
        public void unlock() {
            super.unlock();
            if ( !super.isLocked() ) {
                valueIsAdjusting = false;
                propertyChangeSupport.firePropertyChange( PROP_VALUEADJUSTING, true, false );
            } else {
                //System.err.println("lock is still set, neat!");
            }
        }
    };

    /**
     * one client will have write access to the bean, and when unlock
     * is called, a "valueAdjusting" property change event is fired.
     * In the future, this
     * will return null if the lock is already out, but for now,
     * clients should check the valueIsAdjusting property.
     * @return the lock or null.
     */
    public synchronized Lock mutatorLock() {
        return mutatorLock;
    }

    private PropertyChangeSupport propertyChangeSupport;

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }
}
