/*
 * NullDataSetCache.java
 *
 * Created on November 13, 2006, 11:45 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.uiowa.physics.pw.das.dataset;

/**
 * DataSetCache that does no caching at all.  This is useful for batch
 * mode or when dataset caching is undesirable.
 *
 * @author jbf
 */
public class NullDataSetCache implements DataSetCache {
    
    public NullDataSetCache() {
    }

    public void store(DataSetDescriptor dsd, CacheTag cacheTag, DataSet data) {
    }

    public boolean haveStored(DataSetDescriptor dsd, CacheTag cacheTag) {
        return false;
    }

    public DataSet retrieve(DataSetDescriptor dsd, CacheTag cacheTag) {
            throw new IllegalArgumentException("not found in cache");
    }

    public void reset() {
    }
    
}
