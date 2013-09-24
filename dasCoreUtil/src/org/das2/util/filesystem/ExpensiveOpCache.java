/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.util.filesystem;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.util.LoggerManager;

/**
 * This simply reduces invocations of an expensive operation by
 * caching the result for a given number of milliseconds.  This was
 * introduced to remove the number of HEAD requests to the server for
 * a given file.
 * @author jbf
 */
public class ExpensiveOpCache {

    private static final Logger logger= LoggerManager.getLogger("das2.filesystem.opcache");

    public static interface Op {
        Object doOp( String key ) throws Exception;
    }

    /**
     * Cache the result of op for no more than limitMs milliseconds.
     * @param op
     * @param limitMs
     */
    public ExpensiveOpCache( Op op, int limitMs ) {
        this.limitMs= limitMs;
        this.op= op;
    }

    Op op;
    int limitMs;
    Map<String,Object> results= new HashMap();
    Map<String,Long> times= new HashMap();

    /**
     * Do the operation if it has not been done, or if it has not been
     * done recently.
     * @param key
     * @throws Exception, see the given op.
     * @return the result of the operation
     */
    public Object doOp( String key ) throws Exception {
        Long t;
        Object result;
        synchronized (this ) {
            t = times.get(key);
            result= results.get(key);
        }
        long t0= System.currentTimeMillis();
        long dt= t!=null ? t0 - t.longValue() : 99999;
        if ( t==null ) {
            logger.log(Level.FINE, "no cache entry for: {0}", new Object[]{ key });
            result= op.doOp(key);
            synchronized ( this ) {
                times.put( key, t0 );
                results.put( key, result );
            }
        } else if ( dt > limitMs ) {
            logger.log(Level.FINE, "stale ({0}s) cache entry {1}: {2}", new Object[]{ dt/1000, key, result.toString()});
            result= op.doOp(key);
            synchronized ( this ) {
                times.put( key, t0 );
                results.put( key, result );
            }
        } else {
            logger.log(Level.FINE, "using cached value for {0}: {1}", new Object[]{key, result.toString()});
        }
        return result;
    }

    public synchronized void reset() {
        times.clear();
        results.clear();
    }

}
