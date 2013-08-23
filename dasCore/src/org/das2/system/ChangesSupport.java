/**
 * 
 */
package org.das2.system;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
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
 * See http://das2.org/wiki/index.php/Pending_changes
 * @author jbf
 */
public class ChangesSupport {

    Map<Object, Object> changesPending;
    Object parent;
    private static final Logger logger = Logger.getLogger("das2.system");

    /**
     * if the propertyChangeSupport is provided, then change messages will be sent to
     * it directly.  If null, then one is created with the parent as the source.
     * @param pcs
     * @param parent  the object this is supporting, for debugging purposes.
     */
    public ChangesSupport(PropertyChangeSupport pcs, Object parent) {
        this.parent = parent;
        this.changesPending = new HashMap<Object, Object>(); // client->lock
        if (pcs == null) {
            pcs = new PropertyChangeSupport(parent);
        }
        this.propertyChangeSupport = pcs;
    }

    /**
     * the client knows a change will be coming, and the canvas' clients should
     * know that its current state will change soon.  Example pending changes
     * would be:
     *   layout because tick labels are changing
     *   data is loading
     * Note, it is okay to call this multiple times for the same client and lock object.
     * @param client the object that will perform the change.  This allows the
     *   canvas (and developers) identify who has registered the change.
     * @param lockObject object identifying the change.
     */
    public synchronized void registerPendingChange(Object client, Object lockObject) {
        String msg = "registerPendingChange " + lockObject + " by " + client + "  in " + parent;
        logger.fine(msg);
        Object existingClient = changesPending.get(lockObject);
        if (existingClient != null) {
            if (existingClient != client) {
                throw new IllegalStateException("lock object in use: " + lockObject + ", by " + changesPending.get(lockObject));
            } else {
                //Note, it is okay to call this multiple times for the same client and lock object.
                return;
            }
        }
        boolean oldVal = this.isPendingChanges();
        changesPending.put(lockObject, client);
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
     * @param client the object that is mutating the bean.
     * @param lockObject an object identifying the change.  
     */
    public synchronized void performingChange(Object client, Object lockObject) {
        Object c = changesPending.get(lockObject);
        if (c == null || c != client) {
            logger.log(Level.INFO, "change was not registered, automatically registering change lockObject={0}", lockObject);
            registerPendingChange(client, lockObject);
        }
        logger.log(Level.FINE, "performingChange {0} by {1}  in {2}", new Object[]{lockObject, client, parent});
    }

    /**
     * the change is complete, and as far as the client is concerned, the canvas
     * is valid.
     * @param lockObject
     */
    public synchronized void changePerformed(Object client, Object lockObject) {
        logger.log(Level.FINE, "clearPendingChange {0} by {1}  in {2}", new Object[]{lockObject, client, parent});
        if (changesPending.get(lockObject) == null) {
            // throw new IllegalStateException( "no such lock object: "+lockObject );  //TODO: handle multiple registrations by the same client
        }
        boolean oldVal = this.isPendingChanges();
        changesPending.remove(lockObject);
        propertyChangeSupport.firePropertyChange(PROP_PENDINGCHANGES, oldVal, true);
    }
    public static final String PROP_PENDINGCHANGES = "pendingChanges";

    /**
     * someone has registered a pending change.
     */
    public boolean isPendingChanges() {
        return changesPending.size() > 0;
    }
    public static final String PROP_VALUEADJUSTING = "valueAdjusting";

    /**
     * the bean state is rapidly changing.
     * @return
     */
    public boolean isValueAdjusting() {
        return valueIsAdjusting;
    }
    private boolean valueIsAdjusting = false;

    private Lock mutatorLock = new ReentrantLock() {
            public void lock() {
                super.lock();
                if (valueIsAdjusting) {
                    //System.err.println("lock is already set!");
                } else {
                    propertyChangeSupport.firePropertyChange( PROP_VALUEADJUSTING, false, true );
                    valueIsAdjusting = true;
                }
            }

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
     * @return
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
