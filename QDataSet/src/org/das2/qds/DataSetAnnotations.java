/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.qds;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;

/**
 * Place to experiment with runtime notes about datasets in this 
 * single-instance lookup table.  This uses weak references so that 
 * datasets will be garbage collected.
 * @author jbf
 */
public class DataSetAnnotations {
    
    private static final DataSetAnnotations instance= new DataSetAnnotations();
    
    /**
     * the value 0 to avoid the if statement in valueOf
     */
    public static final Integer VALUE_0= 0;
    
    /**
     * the value 1 to avoid the if statement in valueOf
     */
    public static final Integer VALUE_1= 1;
    
    private final WeakHashMap<QDataSet,Map<String,Object>> annotations= new WeakHashMap<>();
    
    /**
     * set this to true to keep track of hits.
     */
    private final boolean hitTracking= true;
    
    /**
     * keep track of how often an annotation is used.
     */
    private final WeakHashMap<QDataSet,Map<String,Integer>> hits= new WeakHashMap<>();
     
    /**
     * access the single instance
     * @return the single instance
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
     * the cadence for the dataset.
     */
    public static final String ANNOTATION_CADENCE= "cadence";

    /**
     * qube for the dataset.
     */
    public static final String ANNOTATION_QUBE= "qube";
    
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
            if ( hitTracking ) {
                Map<String,Integer> hit= hits.get(ds);
                if ( hit==null ) {
                    hit= new HashMap();
                    hits.put(ds,hit);
                }
                Integer h= hit.get(annotation);
                if ( h==null ) {
                    hit.put(annotation,1);
                } else {
                    hit.put(annotation,1+h);
                }
            }
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
    
    /**
     * print the map of annotations to stderr.
     */
    public synchronized void peek() {
        for ( Entry<QDataSet,Map<String,Object>> ent: annotations.entrySet() ) {
            System.err.println( "\n---" + ent.getKey() + "---" );
            Map<String,Integer> hits1=null;
            if ( hitTracking ) {
                hits1= hits.get(ent.getKey());
            }
            for ( Entry<String,Object> ent1: ent.getValue().entrySet() ) {
                String ss= "";
                if ( hitTracking ) {
                    if ( hits1!=null ) {
                        Integer h= hits1.get(ent1.getKey());
                        if ( h!=null ) ss= h.toString(); else ss="0";
                    } else {
                        ss= "??"; // weak ref was cleaned in one map but not another?
                    }
                }
                System.err.println( ""+ ent1.getKey()+ "->" + ent1.getValue() + "  " + ss );
            }
        }
    }
    
    /**
     * reset the internal state.
     */
    public synchronized void reset() {
        annotations.clear();
        hits.clear();
    }
    
}
