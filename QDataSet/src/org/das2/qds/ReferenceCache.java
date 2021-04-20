/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.qds;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.util.LoggerManager;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.qds.ops.Ops;

/**
 * Provide a cache of datasets that are in memory, so that the same data is not loaded twice.  This first implementation
 * uses WeakReferences, so that this cache need not be emptied, but we will avoid the situation where the same data is loaded
 * twice.
 *
 * @author jbf
 */
public class ReferenceCache {

    private static final Logger logger= LoggerManager.getLogger("apdss.refcache");

    private static ReferenceCache instance;

    private final Map<String,ReferenceCacheEntry> uris= new LinkedHashMap();

    private final Map<String,ProgressMonitor> locks= new LinkedHashMap();

    // no one can directly instantiate, see getInstance.
    private ReferenceCache() {
        
    }
    
    /**
     * get the single instance of the ReferenceCache
     * @return
     */
    public synchronized static ReferenceCache getInstance() {
        if ( instance==null ) {
            instance= new ReferenceCache();
        }
        return instance;
    }

    public enum ReferenceCacheEntryStatus {
        LOADING, DONE
    }

    /**
     * Keep track of the status of a load.  This keeps track of the thread that is actually
     * loading the data and it's
     */
    public static class ReferenceCacheEntry {
        String uri= null;
        WeakReference<QDataSet> qds=null; // this is a weak reference to the data.
        Exception exception=null;
        ProgressMonitor monitor=null; // the progress monitor for the load.
        Thread loadThread=null; // the thread
        ReferenceCacheEntryStatus status=null;
        ReferenceCacheEntry( String uri, ProgressMonitor monitor ) {
            this.status= ReferenceCacheEntryStatus.LOADING;
            this.uri= uri;
            this.monitor= monitor;
        }

        /**
         * query this to see if the current thread should load the resource, or just park while
         * the loading thread loads the resource.
         * @param t the current thread (Thread.currentThread())
         * @return true if the data should be loaded.
         */
        public boolean shouldILoad( Thread t ) {
            logger.log( Level.FINE, "shouldILoad({0})= {1}", new Object[]{Thread.currentThread(), this.loadThread==t } );
            boolean result= ( this.loadThread==t && this.status!=ReferenceCacheEntryStatus.DONE && !wasGarbageCollected() );
            if ( wasGarbageCollected() ) {
                this.status= ReferenceCacheEntryStatus.LOADING;
            }
            return result;
        }

        /**
         * park this thread until the other guy has finished loading.
         * @param mon monitor that will monitor the load status.
         * @return the dataset
         * @throws java.lang.Exception when the data cannot be read.
         */
        public QDataSet park( ProgressMonitor mon ) throws Exception {
            logger.log( Level.FINE, "parking thread {0} {1}", new Object[]{Thread.currentThread(), uri} );
            getInstance().park( this, mon );
            if ( this.exception!=null ) {
                throw this.exception;
            } else {
                logger.log( Level.FINE, "park if {0} {1} resulted in {2}", new Object[]{Thread.currentThread(), uri, this.qds.get() } );
                QDataSet result= this.qds.get();
                return result;
            }
        }
        
        /**
         * hand the dataset resulting from the completed load off to the reference cache.
         * Threads that are parked will continue.  If the dataset is mutable, then a 
         * copy is made.
         * @param ds the dataset resolved from the URI.
         */
        public void finished( QDataSet ds ) {
            logger.log( Level.FINE, "finished {0} {1} {2}", new Object[]{Thread.currentThread(), ds, uri} );
            if ( ds instanceof MutablePropertyDataSet ) {
                MutablePropertyDataSet mpds= (MutablePropertyDataSet)ds;
                if ( mpds.isImmutable() ) {

                } else {
                    WritableDataSet wds= Ops.copy(ds); //TODO: This extaneous copy will be removed once mutability concerns lessen.
                    wds.makeImmutable();
                    ds= wds;
                }
            }
            this.qds= new WeakReference<QDataSet>(ds);
            this.status= ReferenceCacheEntryStatus.DONE;
        }
        
        /**
         * Notify the reference cache that the load resulted in an exception.
         * Threads that are parked will continue
         * @param ex the exception that occurred during the load.
         */
        public void exception( Exception ex ) {
            logger.log( Level.FINE, "finished {0} {1} {2}", new Object[]{Thread.currentThread(), ex, uri} );
            this.exception= ex;
            this.status= ReferenceCacheEntryStatus.DONE;
        }

        /**
         * returns true if the entry was loaded, but now has been garbage collected.
         * @return true if the reference was garbage collected and is no longer available.
         */
        public boolean wasGarbageCollected( ) {
            return ReferenceCacheEntryStatus.DONE==this.status && ( this.qds==null || this.qds.get()==null );
        }

        @Override
        public String toString( ) {
            QDataSet _qds= qds==null ? null : qds.get();
            return String.format( "loadThread=%s\tmonitor=%s\tstatus=%s\turi=%s\tqds=%s", loadThread.getName(), monitor, status, uri, String.valueOf(_qds) );
        }

    }

    /**
     * Query to see if the dataset exists in the cache.  Null is returned if it is not, or a QDataSet is returned if it is.
     * @param uri the URI that can be resolved into a dataset.
     * @return the dataset or null
     */
    public synchronized QDataSet getDataSet( String uri ) {
        ReferenceCacheEntry entry= uris.get(uri);
        if ( entry==null ) {
            return null;
        } else {
            if ( entry.qds==null ) {
                return null;
            } else {
                return entry.qds.get();
            }
        }
    }

    /**
     * Get a ReferenceCacheEntry for the URI, which will indicate the thread which has been designated as the load thread.
     **<blockquote><pre><small>{@code
     *rcent= ReferenceCache.getInstance().getDataSetOrLock( this.tsb.getURI(), mon);
     *if ( !rcent.shouldILoad( Thread.currentThread() ) ) { 
     *   QDataSet result= rcent.park( mon );
     *}
     *</small></pre></blockquote>
     *
     * Be sure to use try/finally when using this cache!
     *
     * @param uri the URI to load.
     * @param monitor to monitor the load.
     * @return the ReferenceCacheEntry
     */
    public ReferenceCacheEntry getDataSetOrLock( String uri, ProgressMonitor monitor ) {
        tidy();
        logger.log( Level.FINEST, "getDataSetOrLock on thread {0} {1}", new Object[]{Thread.currentThread(), uri});
        ReferenceCacheEntry result;
        synchronized (this) {
            result= uris.get(uri);
            if ( result!=null ) {
                if ( result.wasGarbageCollected() ) { // it was garbage collected.
                    result= new ReferenceCacheEntry(uri,monitor);
                    result.loadThread= Thread.currentThread();
                    uris.put( uri, result );
                    logger.log( Level.FINEST, "this thread must reload garbage-collected uri" );
                } else {
                    logger.log( Level.FINEST, "wait for another thread which is loading uri" );
                    if ( result.monitor.isFinished() && result.status!=ReferenceCacheEntryStatus.DONE) {
                        logger.log(Level.WARNING, "cache entry was never cleared: {0}", result.uri);
                    }
                }
            } else {
                result= new ReferenceCacheEntry(uri,monitor);
                result.status= ReferenceCacheEntryStatus.LOADING;
                result.loadThread= Thread.currentThread();
                uris.put( uri, result );
                logger.log( Level.FINEST, "this thread will load uri" );
            }
        }
        return result;
    }

    /**
     * park this thread until the other guy has finished loading.
     * @param ent
     * @param monitor the monitor of the load.
     */
    public void park( ReferenceCacheEntry ent, ProgressMonitor monitor ) {
        if ( ent.loadThread==Thread.currentThread() && ent.status!=ReferenceCacheEntryStatus.DONE ) {
            throw new IllegalStateException("This thread was supposed to load the data");
        }
        monitor.started();
        monitor.setProgressMessage("waiting for load");
        int warn1095Count=0;
        while ( true ) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                logger.log(Level.SEVERE, ex.getMessage(), ex);
            }
            if ( !( ent.monitor.isFinished() || ent.monitor.isCancelled() ) ) {
                monitor.setTaskSize( ent.monitor.getTaskSize());
                monitor.setTaskProgress( ent.monitor.getTaskProgress());
            }
            if ( ent.monitor.isFinished() && ent.status!=ReferenceCacheEntryStatus.DONE ) {
                if ( warn1095Count>1 ) {
                    logger.warning("bug 1095: there is a monitor that is finished, but the reference cache entry is not marked as done.");
                }
                warn1095Count++;
                //ent.status= ReferenceCacheEntryStatus.DONE;
            }
            if ( ent.status==ReferenceCacheEntryStatus.DONE ) break;
        }
        monitor.finished();
    }

    /**
     * put the dataset into the ReferenceCache.  If it is mutable, then a copy is
     * made.
     * @param uri
     * @param ds 
     */
    public synchronized void putDataSet( String uri, QDataSet ds ) {
        ReferenceCacheEntry result= uris.get(uri);
        logger.log( Level.FINEST, "putDataSet on thread {0} {1}", new Object[]{Thread.currentThread(), uri});
        if ( ds instanceof MutablePropertyDataSet ) {
            MutablePropertyDataSet mpds= (MutablePropertyDataSet)ds;
            if ( !mpds.isImmutable() ) {
                ds= Ops.copy(mpds);
                ((MutablePropertyDataSet)ds).makeImmutable();
            }
        }
        result.qds= new WeakReference<QDataSet>(ds);
        result.status= ReferenceCacheEntryStatus.DONE;
    }

    /**
     * explicitly remove entries from the cache.
     */
    public synchronized void reset() {
        logger.fine("reset");
        uris.clear();
        locks.clear();
    }

    /**
     * remove all the entries that have been garbage collected.
     */
    public synchronized void tidy() {
        List<String> rm= new ArrayList();
        for ( Entry<String,ReferenceCacheEntry> ent : instance.uris.entrySet() ) {
            ReferenceCacheEntry ent1= ent.getValue();
            if ( ent1.wasGarbageCollected() ) {
                rm.add(ent1.uri);
            }
        }
        for ( String uri: rm ) {
            instance.uris.remove(uri);
        }
    }

    /**
     * display the status of all the entries.
     */
    public synchronized void printStatus() {

        int i;

        System.err.println("== uris ==");
        i=0;
        for ( Entry<String,ReferenceCacheEntry> ent : instance.uris.entrySet() ) {
            System.err.printf( "%3d %s\n", ++i, String.valueOf(ent.getValue()) );
            ReferenceCacheEntry ent1= ent.getValue();
            QDataSet ds= ent1.qds==null ? null : ent1.qds.get();
            if ( ds!=null ) {
                Class dsclass= ds.getClass();
                Method m=null;
                try {
                    m= dsclass.getDeclaredMethod( "jvmMemory" );
                } catch ( NoSuchMethodException ex) {
                    try {
                        m= dsclass.getSuperclass().getDeclaredMethod( "jvmMemory" );
                    } catch (NoSuchMethodException | SecurityException ex1) {
                        logger.log(Level.SEVERE, null, ex1);
                    }
                }
                if ( m!=null ) {
                    try {
                        Object r= m.invoke( ds );
                        System.err.println("     jvmMemory (bytes): "+r + "  "+dsclass.getName() );                    
                    } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                        logger.log(Level.SEVERE, null, ex);
                    }
                }
            }
        }

        System.err.println("== locks ==");
        i=0;
        for ( Entry<String,ProgressMonitor> ent : instance.locks.entrySet() ) {
            System.err.printf( "%3d %s\n", ++i, String.valueOf(ent.getValue()) );
        }
    }

}
