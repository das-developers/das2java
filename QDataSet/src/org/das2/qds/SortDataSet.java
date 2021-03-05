/*
 * SortDataSet.java
 *
 * Created on April 2, 2007, 8:52 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.das2.qds;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.datum.LoggerManager;
import static org.das2.qds.QubeDataSetIterator.currentJythonLine;
import org.das2.qds.ops.Ops;

/**
 * wraps QDataSet, rearranging the elements of the first index as specified
 * by a rank 1 data set of indeces
 * 
 * @author jbf
 */
public class SortDataSet extends AbstractDataSet {
    
    private static final Logger logger= LoggerManager.getLogger("qdataset.sort");
    
    QDataSet source;
    QDataSet sort;
            
    /**
     * creates the SortDataSet
     * @param source rank N dataset.  Supports plane_0.  Supports rank 2 Depend_1.
     * @param sort the indeces of the sort. 
     */
    public SortDataSet( QDataSet source, QDataSet sort ) {
        this.source= source;
        this.sort= sort;

        if ( sort.length()==0 ) {
            logger.log(Level.FINE, "sort is zero-length for {0}", source);

        } else {
            QubeDataSetIterator.checkValidIndexList(sort,source.length());
            QDataSet range= Ops.extent(sort);

            if ( range.value(0)< 0 ) throw new IndexOutOfBoundsException("sort index contains out-of-bounds element: "+range.value(0) );
            if ( range.value(1)>= source.length() ) {
//                if ( false ) {
//                    logger.log(Level.WARNING, "sort index contains out-of-bounds element: {0}", range.value(1));
//                    logger.log(Level.WARNING, "  range: {0}", range);
//                    logger.log(Level.WARNING, "  source: {0}", source);
//                    logger.log(Level.WARNING, "  sort: {0}", sort);
//                    File f= new File("/tmp/jbfaden.org.virbo.dataset.sortDataSet.line47.txt" );
//                    if ( f.getParentFile().canWrite() && sort.rank()==1 ) {
//                        PrintWriter fw = null;
//                        try {
//                            logger.warning("  dumping data to /tmp/jbfaden.org.virbo.dataset.sortDataSet.line47.txt");
//                            fw = new PrintWriter( new FileWriter(f) );
//                            for ( Entry<String,Object> e: DataSetUtil.getProperties(sort).entrySet() ) {
//                                try {
//                                    fw.println( "# "+e.getKey()+": " + String.valueOf(e.getValue()) );
//                                } catch ( RuntimeException ex ) {
//                                    logger.log(Level.SEVERE, ex.getMessage(), ex);
//                                }
//                            }
//                            for ( int i=0; i<sort.length(); i++ ) {
//                                fw.printf( "%d %f\n", i, sort.value(i) );
//                            }
//                            fw.close();
//                        } catch (RuntimeException ex ) {
//                            logger.log(Level.SEVERE, ex.getMessage(), ex);
//                        } catch (IOException ex) {
//                            logger.log(Level.SEVERE, ex.getMessage(), ex);
//                        } finally {
//                            if ( fw!=null ) fw.close();
//                        }
//                    }
//                }
                throw new IndexOutOfBoundsException("sort index contains out-of-bounds element: "+range.value(1) );
            }
        }
        
        properties= new HashMap();
        QDataSet dep0= (QDataSet) source.property( QDataSet.DEPEND_0 );
        if ( dep0!=null ) properties.put( QDataSet.DEPEND_0, new SortDataSet( dep0, sort ) );
        QDataSet p0= (QDataSet) source.property( QDataSet.PLANE_0 );
        if ( p0!=null ) properties.put( QDataSet.PLANE_0, new SortDataSet( p0, sort ) );
        QDataSet dep1= (QDataSet) source.property( QDataSet.DEPEND_1 );
        if ( dep1!=null && dep1.rank()>1 ) {
            properties.put( QDataSet.DEPEND_1, new SortDataSet( dep1, sort ) );
        }
        QDataSet dep2= (QDataSet) source.property( QDataSet.DEPEND_2 );
        if ( dep2!=null && dep2.rank()>1 ) {
            properties.put( QDataSet.DEPEND_2, new SortDataSet( dep2, sort ) );
        }
        QDataSet dep3= (QDataSet) source.property( QDataSet.DEPEND_3 );
        if ( dep3!=null && dep3.rank()>1 ) {
            properties.put( QDataSet.DEPEND_3, new SortDataSet( dep3, sort ) );
        }
        
        QDataSet ds;
        ds= (QDataSet) source.property(QDataSet.BIN_PLUS);
        if ( ds!=null ) {
            if ( ds.rank()==0 ) ds= Ops.replicate(ds,source.length());
            properties.put( QDataSet.BIN_PLUS, new SortDataSet( ds, sort ) );
        }
        ds= (QDataSet) source.property(QDataSet.BIN_MINUS);
        if ( ds!=null ) {
            if ( ds.rank()==0 ) ds= Ops.replicate(ds,source.length());
            properties.put( QDataSet.BIN_MINUS, new SortDataSet( ds, sort ) );
        }
        ds= (QDataSet) source.property(QDataSet.DELTA_PLUS);
        if ( ds!=null ) {
            if ( ds.rank()==0 ) ds= Ops.replicate(ds,source.length());
            properties.put( QDataSet.DELTA_PLUS, new SortDataSet( ds, sort ) );
        }
        ds= (QDataSet) source.property(QDataSet.DELTA_MINUS);
        if ( ds!=null ) {
            if ( ds.rank()==0 ) ds= Ops.replicate(ds,source.length());
            properties.put( QDataSet.DELTA_MINUS, new SortDataSet( ds, sort ) );
        }
        ds= (QDataSet) source.property(QDataSet.BIN_MAX);
        if ( ds!=null ) {
            properties.put( QDataSet.BIN_MAX, new SortDataSet( ds, sort ) );
        }
        ds= (QDataSet) source.property(QDataSet.BIN_MIN);
        if ( ds!=null ) {
            properties.put( QDataSet.BIN_MIN, new SortDataSet( ds, sort ) );
        }        
        properties.put( QDataSet.CACHE_TAG, null ); // note this null needs to hide the CACHE_TAG in the source.
        
        DataSetUtil.putProperties( DataSetUtil.getDimensionProperties(source,null), this );
        properties.put( QDataSet.CADENCE, null );  // cadence is no longer correct, as with decimate.
    }

    public int rank() {
        return source.rank();
    }

    @Override
    public double value(int i) {
        return source.value( (int)sort.value(i) );
    }

    @Override
    public double value(int i0, int i1) {
        return source.value( (int)sort.value(i0), i1 );
    }

    @Override
    public double value(int i0, int i1, int i2) {
        return source.value( (int)sort.value(i0), i1, i2 );
    }

    @Override
    public double value(int i0, int i1, int i2, int i3 ) {
        return source.value( (int)sort.value(i0), i1, i2, i3 );
    }

    /**
     * TODO: this is dangerous code, because as new properties are added to QDataSet, 
     * they may not be handled properly here. (e.g. BIN_PLUS).
     * Note the properties are intended to mask the things that have changed!
     * @param name
     * @return 
     */
    @Override
    public Object property(String name) {
        if ( properties.containsKey(name) ) {
            return properties.get(name);
        } else {
            return source.property(name);
        }
    }

    @Override
    public Object property(String name, int i) {
        if ( properties.containsKey(name) ) {
            return properties.get(name);
        } else {
            return source.property(name,(int)sort.value(i));
        }
    }

    @Override
    public int length() {
        return sort.length();
    }

    @Override
    public int length(int i) {
        return source.length( (int)sort.value(i) );
    }

    @Override
    public int length(int i, int j) {
        return source.length( (int)sort.value(i), j );
    }

    @Override
    public int length(int i, int j, int k) {
        return source.length( (int)sort.value(i), j, k );
    }
    
}
