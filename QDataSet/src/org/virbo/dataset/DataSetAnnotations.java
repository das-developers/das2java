/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.dataset;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Place to experiment with runtime notes about datasets.  This
 * uses weak references so that datasets will be garbage collected.
 * @author jbf
 */
public class DataSetAnnotations {
    
    private static DataSetAnnotations instance= new DataSetAnnotations();
    
    private WeakHashMap<QDataSet,Map> annotations= new WeakHashMap<QDataSet,Map>();
     
    /**
     * access the single instance
     * @return 
     */
    public static DataSetAnnotations getInstance() {
        return instance;
    }
    
    /**
     * number of invalid entries.  If zero, the data is all valid, within
     * valid min to valid max and not fill.
     */
    public static final String ANNOTATION_INVALID_COUNT= "invalidCount";
    
    public static final String ANNOTATION_BOUNDS= "bounds";
    
    public synchronized Object getAnnotation( QDataSet ds, String annotation ) {
        Map<String,Object> anno= annotations.get(ds);
        if ( anno==null ) {
            return null;
        } else {
            return anno.get(annotation);
        }
    }
    
    public synchronized void putAnnotation( QDataSet ds, String annotation, Object value ) {
        Map<String,Object> anno= annotations.get(ds);
        if ( anno==null ) {
            anno= new HashMap();
        }
        anno.put(annotation, value);
        annotations.put( ds, anno );
    }
    
    
}
