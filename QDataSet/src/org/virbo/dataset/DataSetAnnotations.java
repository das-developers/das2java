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
    
    private static final DataSetAnnotations instance= new DataSetAnnotations();
    
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

    /**
     * number of entries containing the value zero.  If zero, then the
     * data is all non-zero or fill.  This is used with ANNOTATION_INVALID_COUNT
     * in the where function.
     */
    public static final String ANNOTATION_ZERO_COUNT= "zeroCount";
    
    /**
     * the bounding cube of the dataset, see SemanticOps.getBounds.
     */
    public static final String ANNOTATION_BOUNDS= "bounds";
    
    /**
     * return either null or the value for the annotation.  Note some
     * Java compilers (Java6?) will not allow code such as:<tt>
     * 0==DataSetAnnotations.getInstance().getAnnotation(...)
     * </tt>
     * so instead use
     * <tt>
     * Integer.valueOf(0)==DataSetAnnotations.getInstance().getAnnotation(...)
     * </tt>
     * 
     * @param ds the dataset.
     * @param annotation the annotation name, such as ANNOTATION_INVALID_COUNT.
     * @return null or the annotation value.
     */
    public synchronized Object getAnnotation( QDataSet ds, String annotation ) {
        Map<String,Object> anno= annotations.get(ds);
        if ( anno==null ) {
            return null;
        } else {
            return anno.get(annotation);
        }
    }
    
    /**
     * add the annotation.
     * @param ds the dataset to annotate
     * @param annotation the annotation name, such as ANNOTATION_INVALID_COUNT.
     * @param value the value.
     */
    public synchronized void putAnnotation( QDataSet ds, String annotation, Object value ) {
        Map<String,Object> anno= annotations.get(ds);
        if ( anno==null ) {
            anno= new HashMap();
        }
        anno.put(annotation, value);
        annotations.put( ds, anno );
    }
    
    
}
