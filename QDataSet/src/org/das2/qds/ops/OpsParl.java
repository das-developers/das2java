/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.qds.ops;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.datum.LoggerManager;
import org.das2.datum.UnitsConverter;
import org.das2.qds.DataSetUtil;
import org.das2.qds.MutablePropertyDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.QubeDataSetIterator;
import org.das2.qds.SemanticOps;
import org.das2.qds.WritableDataSet;
import static org.das2.qds.ops.Ops.equalProperties;

/**
 *
 * @author jbf
 */
public class OpsParl {
    
    private static final Logger logger= LoggerManager.getLogger("qdataset.ops");
    private static final String CLASSNAME = OpsParl.class.getCanonicalName();

    /**
     * this class cannot be instantiated.
     */
    private OpsParl() {    
    }
    
    /**
     * BinaryOps are operations such as add, pow, atan2
     */
    public interface BinaryOp {
        double op(double d1, double d2);
    }
    
    /**
     * these are properties that are propagated through operations.
     */
    private static final String[] _dependProperties= new String[] {
                    QDataSet.DEPEND_0, QDataSet.DEPEND_1, QDataSet.DEPEND_2, QDataSet.DEPEND_3, 
                    QDataSet.BINS_0, QDataSet.BINS_1, 
    };
    
    /**
     * apply the binary operator element-for-element of the two datasets, minding
     * dataset geometry, fill values, etc.  The two datasets are coerced to
     * compatible geometry, if possible (e.g.Temperature[Time]+2deg), using 
     * CoerceUtil.coerce.  Structural metadata such as DEPEND_0 are preserved 
     * where this is reasonable, and dimensional metadata such as UNITS are
     * dropped.
     * 
     * @param ds1 the first argument
     * @param ds2 the second argument
     * @param op binary operation for each pair of elements
     * @return the result with the same geometry as the pair.
     */
    public static MutablePropertyDataSet applyBinaryOp( QDataSet ds1, QDataSet ds2, BinaryOp op ) {
        //TODO: handle JOIN from RPWS group, which is not a QUBE...
        if ( ds1.rank()==ds2.rank() && ds1.rank()>0 ) {
            if ( ds1.length()!=ds2.length() ) {
                throw new IllegalArgumentException("binary operation on datasets of different lengths: "+ ds1 + " " + ds2 );
            }
        }

        QDataSet[] operands= new QDataSet[2];

//        if ( checkComplexArgument(ds1) || checkComplexArgument(ds2) ) {
//            if ( ds1.rank()!=ds2.rank() ) {
//                throw new IllegalArgumentException("complex numbers argument requires that rank must be equal.");
//            }
//        }
//        
        WritableDataSet result = CoerceUtil.coerce( ds1, ds2, true, operands );

        QubeDataSetIterator it1 = new QubeDataSetIterator( operands[0] );
        QubeDataSetIterator it2 = new QubeDataSetIterator( operands[1] );

        QDataSet w1= DataSetUtil.weightsDataSet(operands[0]);
        QDataSet w2= DataSetUtil.weightsDataSet(operands[1]);

        double fill= -1e38;
        while (it1.hasNext()) {
            it1.next();
            it2.next();
            double d1 = it1.getValue(operands[0]);
            double d2 = it2.getValue(operands[1]);
            double w= it1.getValue(w1) * it2.getValue(w2);
            it1.putValue(result, w==0 ? fill : op.op(d1, d2));
        }

        Map<String, Object> m1 = DataSetUtil.getProperties( operands[0], _dependProperties, null );
        Map<String, Object> m2 = DataSetUtil.getProperties( operands[1], _dependProperties, null );

        boolean resultIsQube= Boolean.TRUE.equals( m1.get(QDataSet.QUBE) ) || Boolean.TRUE.equals( m2.get(QDataSet.QUBE) );
        if ( m1.size()==1 ) m1.remove( QDataSet.QUBE ); // kludge: CoerceUtil.coerce would copy over a QUBE property, so just remove this.
        if ( m2.size()==1 ) m2.remove( QDataSet.QUBE );
        if ( ( m2.isEmpty() && !m1.isEmpty() ) || ds2.rank()==0 ) {
            m2.put( QDataSet.DEPEND_0, m1.get(QDataSet.DEPEND_0 ) );
            m2.put( QDataSet.DEPEND_1, m1.get(QDataSet.DEPEND_1 ) );
            m2.put( QDataSet.DEPEND_2, m1.get(QDataSet.DEPEND_2 ) );
            m2.put( QDataSet.DEPEND_3, m1.get(QDataSet.DEPEND_3 ) );
            m2.put( QDataSet.CONTEXT_0, m1.get(QDataSet.CONTEXT_0) );
            m2.put( QDataSet.BINS_0,   m1.get(QDataSet.BINS_0 ) );
            m2.put( QDataSet.BINS_1,   m1.get(QDataSet.BINS_1 ) );
        }
        if ( ( m1.isEmpty() && !m2.isEmpty() ) || ds1.rank()==0 ) {
            m1.put( QDataSet.DEPEND_0, m2.get(QDataSet.DEPEND_0 ) );
            m1.put( QDataSet.DEPEND_1, m2.get(QDataSet.DEPEND_1 ) );
            m1.put( QDataSet.DEPEND_2, m2.get(QDataSet.DEPEND_2 ) );
            m1.put( QDataSet.DEPEND_3, m2.get(QDataSet.DEPEND_3 ) );
            m1.put( QDataSet.CONTEXT_0, m2.get(QDataSet.CONTEXT_0) );
            m1.put( QDataSet.BINS_0,   m2.get(QDataSet.BINS_0 ) );
            m1.put( QDataSet.BINS_1,   m2.get(QDataSet.BINS_1 ) );
        }
        Map<String, Object> m3 = equalProperties(m1, m2);
        if ( resultIsQube ) {
            m3.put( QDataSet.QUBE, Boolean.TRUE );
        }
        String[] ss= DataSetUtil.dimensionProperties();
        for ( String s: ss ) {
            m3.remove( s );
        }
        // because this contains FILL_VALUE, etc that are no longer correct.  
        // My guess is that anything with a BUNDLE_1 property shouldn't have 
        // been passed into this routine anyway.
        m3.remove( QDataSet.BUNDLE_1 ); 

        DataSetUtil.putProperties(m3, result);    
        result.putProperty( QDataSet.FILL_VALUE, fill );
        
        return result;
    }
    
    /**
     * element-wise equality test.  1.0 is returned where the two datasets are
     * equal.  Fill is returned where either measurement is invalid.
     * @param ds1 rank n dataset
     * @param ds2 rank m dataset with compatible geometry.
     * @return rank n or m dataset.
     */
    public static QDataSet eq(QDataSet ds1, QDataSet ds2) {
        final UnitsConverter uc= SemanticOps.getLooseUnitsConverter( ds1, ds2 );
        return applyBinaryOp(ds1, ds2, new BinaryOp() {
           @Override
           public double op(double d1, double d2) {
               return uc.convert(d1) == d2 ? 1.0 : 0.0;
           }
        });
    } 
    
    /**
     * apply the binary operator element-for-element of the two datasets, minding
     * dataset geometry, fill values, etc.  The two datasets are coerced to
     * compatible geometry, if possible (e.g.Temperature[Time]+2deg), using 
     * CoerceUtil.coerce.  Structural metadata such as DEPEND_0 are preserved 
     * where this is reasonable, and dimensional metadata such as UNITS are
     * dropped.
     * 
     * This implementation avoids the use of DataSetIterators, which have been
     * shown to be slow.  (But it's not known why.)
     * 
     * @param ds1 the first argument
     * @param ds2 the second argument
     * @param op binary operation for each pair of elements
     * @return the result with the same geometry as the pair.
     */
    public static MutablePropertyDataSet applyBinaryOpNoIter( QDataSet ds1, QDataSet ds2, BinaryOp op ) {
        //TODO: handle JOIN from RPWS group, which is not a QUBE...
        if ( ds1.rank()==ds2.rank() && ds1.rank()>0 ) {
            if ( ds1.length()!=ds2.length() ) {
                throw new IllegalArgumentException("binary operation on datasets of different lengths: "+ ds1 + " " + ds2 );
            }
        }

        QDataSet[] operands= new QDataSet[2];

//        if ( checkComplexArgument(ds1) || checkComplexArgument(ds2) ) {
//            if ( ds1.rank()!=ds2.rank() ) {
//                throw new IllegalArgumentException("complex numbers argument requires that rank must be equal.");
//            }
//        }
//        
        WritableDataSet result = CoerceUtil.coerce( ds1, ds2, true, operands );

        QDataSet w1= DataSetUtil.weightsDataSet(operands[0]);
        QDataSet w2= DataSetUtil.weightsDataSet(operands[1]);
        double fill= -1e38;
        
        if ( w1.rank()==1 ) {
            int n= w1.length();
            QDataSet op1= operands[0];
            QDataSet op2= operands[1];
            for ( int i=0; i<n; i++ ) {
                double d1 = op1.value(i);
                double d2 = op2.value(i);
                double w= w1.value(i) * w2.value(i);   
                result.putValue( i, w==0 ? fill : op.op(d1, d2) );
            }
            
        } else {
            QubeDataSetIterator it1 = new QubeDataSetIterator( operands[0] );
            QubeDataSetIterator it2 = new QubeDataSetIterator( operands[1] );
            
            while (it1.hasNext()) {
                it1.next();
                it2.next();
                double d1 = it1.getValue(operands[0]);
                double d2 = it2.getValue(operands[1]);
                double w= it1.getValue(w1) * it2.getValue(w2);
                it1.putValue(result, w==0 ? fill : op.op(d1, d2));
            }
        }

        Map<String, Object> m1 = DataSetUtil.getProperties( operands[0], _dependProperties, null );
        Map<String, Object> m2 = DataSetUtil.getProperties( operands[1], _dependProperties, null );

        boolean resultIsQube= Boolean.TRUE.equals( m1.get(QDataSet.QUBE) ) || Boolean.TRUE.equals( m2.get(QDataSet.QUBE) );
        if ( m1.size()==1 ) m1.remove( QDataSet.QUBE ); // kludge: CoerceUtil.coerce would copy over a QUBE property, so just remove this.
        if ( m2.size()==1 ) m2.remove( QDataSet.QUBE );
        if ( ( m2.isEmpty() && !m1.isEmpty() ) || ds2.rank()==0 ) {
            m2.put( QDataSet.DEPEND_0, m1.get(QDataSet.DEPEND_0 ) );
            m2.put( QDataSet.DEPEND_1, m1.get(QDataSet.DEPEND_1 ) );
            m2.put( QDataSet.DEPEND_2, m1.get(QDataSet.DEPEND_2 ) );
            m2.put( QDataSet.DEPEND_3, m1.get(QDataSet.DEPEND_3 ) );
            m2.put( QDataSet.CONTEXT_0, m1.get(QDataSet.CONTEXT_0) );
            m2.put( QDataSet.BINS_0,   m1.get(QDataSet.BINS_0 ) );
            m2.put( QDataSet.BINS_1,   m1.get(QDataSet.BINS_1 ) );
        }
        if ( ( m1.isEmpty() && !m2.isEmpty() ) || ds1.rank()==0 ) {
            m1.put( QDataSet.DEPEND_0, m2.get(QDataSet.DEPEND_0 ) );
            m1.put( QDataSet.DEPEND_1, m2.get(QDataSet.DEPEND_1 ) );
            m1.put( QDataSet.DEPEND_2, m2.get(QDataSet.DEPEND_2 ) );
            m1.put( QDataSet.DEPEND_3, m2.get(QDataSet.DEPEND_3 ) );
            m1.put( QDataSet.CONTEXT_0, m2.get(QDataSet.CONTEXT_0) );
            m1.put( QDataSet.BINS_0,   m2.get(QDataSet.BINS_0 ) );
            m1.put( QDataSet.BINS_1,   m2.get(QDataSet.BINS_1 ) );
        }
        Map<String, Object> m3 = equalProperties(m1, m2);
        if ( resultIsQube ) {
            m3.put( QDataSet.QUBE, Boolean.TRUE );
        }
        String[] ss= DataSetUtil.dimensionProperties();
        for ( String s: ss ) {
            m3.remove( s );
        }
        // because this contains FILL_VALUE, etc that are no longer correct.  
        // My guess is that anything with a BUNDLE_1 property shouldn't have 
        // been passed into this routine anyway.
        m3.remove( QDataSet.BUNDLE_1 ); 

        DataSetUtil.putProperties(m3, result);    
        result.putProperty( QDataSet.FILL_VALUE, fill );
        
        return result;
    }
    
    /**
     * element-wise equality test.  1.0 is returned where the two datasets are
     * equal.  Fill is returned where either measurement is invalid.
     * @param ds1 rank n dataset
     * @param ds2 rank m dataset with compatible geometry.
     * @return rank n or m dataset.
     */
    public static QDataSet eq_noiter(QDataSet ds1, QDataSet ds2) {
        final UnitsConverter uc= SemanticOps.getLooseUnitsConverter( ds1, ds2 );
        return applyBinaryOpNoIter(ds1, ds2, new BinaryOp() {
           @Override
           public double op(double d1, double d2) {
               return uc.convert(d1) == d2 ? 1.0 : 0.0;
           }
        });
    } 
    
    /**
     * apply the binary operator element-for-element of the two datasets, minding
     * dataset geometry, fill values, etc.  The two datasets are coerced to
     * compatible geometry, if possible (e.g.Temperature[Time]+2deg), using 
     * CoerceUtil.coerce.  Structural metadata such as DEPEND_0 are preserved 
     * where this is reasonable, and dimensional metadata such as UNITS are
     * dropped.
     * 
     * This implementation runs the trivially parallelizable task on separate 
     * threads.
     * 
     * @param ds1 the first argument
     * @param ds2 the second argument
     * @param op binary operation for each pair of elements
     * @return the result with the same geometry as the pair.
     */
    public static MutablePropertyDataSet applyBinaryOpParl( QDataSet ds1, QDataSet ds2, final BinaryOp op ) {
        //TODO: handle JOIN from RPWS group, which is not a QUBE...
        if ( ds1.rank()==ds2.rank() && ds1.rank()>0 ) {
            if ( ds1.length()!=ds2.length() ) {
                throw new IllegalArgumentException("binary operation on datasets of different lengths: "+ ds1 + " " + ds2 );
            }
        }

        QDataSet[] operands= new QDataSet[2];

//        if ( checkComplexArgument(ds1) || checkComplexArgument(ds2) ) {
//            if ( ds1.rank()!=ds2.rank() ) {
//                throw new IllegalArgumentException("complex numbers argument requires that rank must be equal.");
//            }
//        }
//        
        final WritableDataSet result = CoerceUtil.coerce( ds1, ds2, true, operands );

        final QDataSet w1= DataSetUtil.weightsDataSet(operands[0]);
        final QDataSet w2= DataSetUtil.weightsDataSet(operands[1]);
        final double fill= -1e38;
        
        if ( w1.rank()==1 ) {
            final int nthread= 4;
            final int n= w1.length();
            
            final QDataSet op1= operands[0];
            final QDataSet op2= operands[1];
            
            List<Callable<Object>> callables= new ArrayList<>(4);
            for ( int i=0; i<nthread; i++ ) {
                final int istart= i;
                Callable r= new Callable<Object>() {
                    @Override
                    public Object call() {
                        for ( int i=istart; i<n; i+=nthread ) {
                            double d1 = op1.value(i);
                            double d2 = op2.value(i);
                            double w= w1.value(i) * w2.value(i);   
                            result.putValue( i, w==0 ? fill : op.op(d1, d2) );
                        }
                        return null;
                    }
                };
                callables.add(i, r);
            }
            
            ExecutorService executor= Executors.newCachedThreadPool();
            List<Callable<Object>> tasks= callables;
            List<Future<Object>> futures;
            try {
                futures = executor.invokeAll(tasks);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        
            while ( true ) {
                boolean allDone= true;
                for ( Future f: futures ) {
                    if ( !f.isDone() ) {
                        allDone= false;
                        break;
                    }
                }
                if ( allDone ) break;
            }
            
        } else {
            QubeDataSetIterator it1 = new QubeDataSetIterator( operands[0] );
            QubeDataSetIterator it2 = new QubeDataSetIterator( operands[1] );
            
            while (it1.hasNext()) {
                it1.next();
                it2.next();
                double d1 = it1.getValue(operands[0]);
                double d2 = it2.getValue(operands[1]);
                double w= it1.getValue(w1) * it2.getValue(w2);
                it1.putValue(result, w==0 ? fill : op.op(d1, d2));
            }
        }

        Map<String, Object> m1 = DataSetUtil.getProperties( operands[0], _dependProperties, null );
        Map<String, Object> m2 = DataSetUtil.getProperties( operands[1], _dependProperties, null );

        boolean resultIsQube= Boolean.TRUE.equals( m1.get(QDataSet.QUBE) ) || Boolean.TRUE.equals( m2.get(QDataSet.QUBE) );
        if ( m1.size()==1 ) m1.remove( QDataSet.QUBE ); // kludge: CoerceUtil.coerce would copy over a QUBE property, so just remove this.
        if ( m2.size()==1 ) m2.remove( QDataSet.QUBE );
        if ( ( m2.isEmpty() && !m1.isEmpty() ) || ds2.rank()==0 ) {
            m2.put( QDataSet.DEPEND_0, m1.get(QDataSet.DEPEND_0 ) );
            m2.put( QDataSet.DEPEND_1, m1.get(QDataSet.DEPEND_1 ) );
            m2.put( QDataSet.DEPEND_2, m1.get(QDataSet.DEPEND_2 ) );
            m2.put( QDataSet.DEPEND_3, m1.get(QDataSet.DEPEND_3 ) );
            m2.put( QDataSet.CONTEXT_0, m1.get(QDataSet.CONTEXT_0) );
            m2.put( QDataSet.BINS_0,   m1.get(QDataSet.BINS_0 ) );
            m2.put( QDataSet.BINS_1,   m1.get(QDataSet.BINS_1 ) );
        }
        if ( ( m1.isEmpty() && !m2.isEmpty() ) || ds1.rank()==0 ) {
            m1.put( QDataSet.DEPEND_0, m2.get(QDataSet.DEPEND_0 ) );
            m1.put( QDataSet.DEPEND_1, m2.get(QDataSet.DEPEND_1 ) );
            m1.put( QDataSet.DEPEND_2, m2.get(QDataSet.DEPEND_2 ) );
            m1.put( QDataSet.DEPEND_3, m2.get(QDataSet.DEPEND_3 ) );
            m1.put( QDataSet.CONTEXT_0, m2.get(QDataSet.CONTEXT_0) );
            m1.put( QDataSet.BINS_0,   m2.get(QDataSet.BINS_0 ) );
            m1.put( QDataSet.BINS_1,   m2.get(QDataSet.BINS_1 ) );
        }
        Map<String, Object> m3 = equalProperties(m1, m2);
        if ( resultIsQube ) {
            m3.put( QDataSet.QUBE, Boolean.TRUE );
        }
        String[] ss= DataSetUtil.dimensionProperties();
        for ( String s: ss ) {
            m3.remove( s );
        }
        // because this contains FILL_VALUE, etc that are no longer correct.  
        // My guess is that anything with a BUNDLE_1 property shouldn't have 
        // been passed into this routine anyway.
        m3.remove( QDataSet.BUNDLE_1 ); 

        DataSetUtil.putProperties(m3, result);    
        result.putProperty( QDataSet.FILL_VALUE, fill );
        
        return result;
    }
    
    /**
     * element-wise equality test.  1.0 is returned where the two datasets are
     * equal.  Fill is returned where either measurement is invalid.
     * @param ds1 rank n dataset
     * @param ds2 rank m dataset with compatible geometry.
     * @return rank n or m dataset.
     */
    public static QDataSet eq_parl(QDataSet ds1, QDataSet ds2) {
        final UnitsConverter uc= SemanticOps.getLooseUnitsConverter( ds1, ds2 );
        return applyBinaryOpParl(ds1, ds2, new BinaryOp() {
           @Override
           public double op(double d1, double d2) {
               return uc.convert(d1) == d2 ? 1.0 : 0.0;
           }
        });
    }         
}
