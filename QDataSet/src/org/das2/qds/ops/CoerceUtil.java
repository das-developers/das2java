/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.qds.ops;

import org.das2.datum.Units;
import org.das2.qds.DDataSet;
import org.das2.qds.DataSetOps;
import org.das2.qds.DataSetUtil;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.Slice0DataSet;
import org.das2.qds.TrimDataSet;
import org.das2.qds.WritableDataSet;
import org.das2.qds.WritableJoinDataSet;

/**
 * Utility class for reconciling the geometries of two datasets.  For example,
 * a rank 1 dataset's values can be repeated to make it rank 2.
 *
 * TODO: dataset geometry is increased by keeping a reference to a dataset with
 * the target geometry.  This might result in keeping data in memory that would
 * otherwise be released, so a future implementation of this should probably
 * use a non-qube dataset to store each index's length:
 *<blockquote><pre>
 * public int length() {
 *    return lengths.length()
 * }
 * public int length(int i0) {
 *    return lengths.value(i0);
 * }
 *</pre></blockquote>
 * 
 * @author jbf
 */
public class CoerceUtil {

    /**
     * returns true if two datasets have the same number of elements in each dimension.
     * @param ds1
     * @param ds2
     * @return returns true if two datasets have the same number of elements in each dimension.
     */
    public static boolean equalGeom(QDataSet ds1, QDataSet ds2) {
        if ( ds1.rank()!=ds2.rank() ) return false;
        int[] qube1 = DataSetUtil.qubeDims(ds1);
        int[] qube2 = DataSetUtil.qubeDims(ds2);
        if (qube1 == null || qube2 == null) { 
            if (ds1.rank() == 1 && ds2.rank() == 1) {
                return ds1.length() == ds2.length();
            } else {
                if (ds1.length() != ds2.length()) {
                    return false;
                } else {
                    for (int i0 = 0; i0 < ds1.length(); i0++ ) {
                        if ( !equalGeom( DataSetOps.slice0(ds1, i0),DataSetOps.slice0(ds2, i0) ) ) {
                            return false;
                        }
                    }
                    return true;
                }
            }
        } else {
            for (int i = 0; i < qube2.length; i++) {
                if (qube1[i] != qube2[i]) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * increase rank of datasets so that element-wise operations can be performed.  For example,
     * if a rank 1 and a rank 2 dataset are to be combined and both have equal dim 0 length, then the
     * rank 1 is promoted to rank 2 by repeating its values.  This implements the rule that dimensions
     * in QDataSet have nested context.  The second dimension elements are to be understood in the context of
     * the first dimension element. (Except for qubes, where the order is arbitrary.)
     * @param ds1 the first operand
     * @param ds2 the second operand
     * @param createResult if true, then a dataset is created where the result can be installed.
     * @param operands the array in which the promoted operands are inserted.
     * @return an empty dataset where the results can be inserted, or null if createResult is false.
     */
    public static WritableDataSet coerce(QDataSet ds1, QDataSet ds2, boolean createResult, QDataSet[] operands) {
        int ds1rank= ds1.rank();
        int ds2rank= ds2.rank();
        if (ds1rank == ds2rank && equalGeom(ds1, ds2)) {
        } else if (ds1rank < ds2rank) {
            if (ds1rank == 0) {
                ds1 = increaseRank0(ds1, ds2);
            } else if (ds1.rank() == 1) {
                ds1 = increaseRank1(ds1, ds2);
            } else if (ds1.rank() == 2) {
                ds1 = increaseRank2(ds1, ds2);
            } else {
                throw new IllegalArgumentException("rank limit");
            }

        } else {
            if (ds2rank == 0) {
                ds2 = increaseRank0(ds2, ds1);
            } else if (ds2rank == 1) {
                ds2 = increaseRank1(ds2, ds1);
            } else if (ds2rank == 2) {
                ds2 = increaseRank2(ds2, ds1);
            } else {
                throw new IllegalArgumentException("rank limit");
            }
        }
        operands[0] = ds1;
        operands[1] = ds2;
        DDataSet result = null;
        if (createResult) {
            int [] dims= DataSetUtil.qubeDims(ds1);
            if ( dims==null ) dims= DataSetUtil.qubeDims(ds2);
            if ( dims==null ) {
                if ( ds2rank==0 ) {
                    return Ops.copy(ds1);
                } else {
                    throw new RuntimeException( "either ds1 or ds2 needs to be a qube" );
                }
            } else {
                result = DDataSet.create(dims);
            }
        }
        return result;
    }

    /**
     * increase the rank of a rank zero dataset by adding join dimensions that repeat the lower rank elements.
     * @param ds a rank 0 dataset
     * @param ds2 dataset to provide rank and lengths.
     * @return a dataset with the same geometry as ds2.
     */
    public static QDataSet increaseRank0(final QDataSet ds, final QDataSet ds2) {
        return new QDataSet() {

            public int rank() {
                return ds2.rank();
            }

            public String svalue() {
                Units u= (Units) property(QDataSet.UNITS);
                if ( u==null ) {
                    return String.valueOf(value());
                } else {
                    return u.createDatum(value()).toString();
                }
            }
            
            public double value() {
                return ds.value();
            }

            public double value(int i) {
                return ds.value();
            }

            public double value(int i0, int i1) {
                return ds.value();
            }

            public double value(int i0, int i1, int i2) {
                return ds.value();
            }

            public double value(int i0, int i1, int i2, int i3) {
                return ds.value();
            }
            
            public Object property(String name) {
                if ( name.equals(QDataSet.QUBE) ) {
                    return ds2.property(name);
                } else {
                    return ds.property(name);
                }
            }

            public Object property(String name, int i) {
                return ds.property(name);
            }

            public int length() {
                return ds2.length();
            }

            public int length(int i) {
                return ds2.length(i);
            }

            public int length(int i, int j) {
                return ds2.length(i, j);
            }

            public int length(int i, int j, int k) {
                return ds2.length(i, j, k);
            }

            public <T> T capability(Class<T> clazz) {
                return null;
            }

            public QDataSet slice(int i) {
                return new Slice0DataSet(this, i);
            }

            public QDataSet trim(int start, int end) {
                return new TrimDataSet(this, start, end ); //TODO: I'm sure there is a better solution for this--like trim on ds2.
            }
            
            @Override
            public String toString() {
                return DataSetUtil.toString(this);
            }
        };
    }

    /**
     * increase the rank of a rank one dataset by adding join dimensions that repeat the lower rank elements.
     * @param ds a rank 1 dataset to provide values and properties
     * @param ds2 dataset to provide rank and lengths.
     * @return a dataset with the same geometry as ds2.
     */
    public static QDataSet increaseRank1(final QDataSet ds, final QDataSet ds2) {

        if ( ds.length()!=ds2.length() ) {
            throw new IllegalArgumentException("datasets must have the same length(): "+ ds + " "+ds2 );
        }
        
        return new QDataSet() {

            public int rank() {
                return ds2.rank();
            }

            public String svalue() {
                Units u= (Units) property(QDataSet.UNITS);
                if ( u==null ) {
                    return String.valueOf(value());
                } else {
                    return u.createDatum(value()).toString();
                }
            }            

            public double value() {
                throw new IllegalArgumentException("rank too low");
            }

            public double value(int i0) {
                return ds.value(i0);
            }

            public double value(int i0, int i1) {
                return ds.value(i0);
            }

            public double value(int i0, int i1, int i2) {
                return ds.value(i0);
            }

            public double value(int i0, int i1, int i2, int i3) {
                return ds.value(i0);
            }

            public Object property(String name) {
                if ( name.equals(QDataSet.QUBE) ) {
                    return ds2.property(name);
                } else {
                    return ds.property(name);
                }

            }

            public Object property(String name, int i) {
                return ds.property(name, i);
            }

            public int length() {
                return ds2.length();
            }

            public int length(int i) {
                return ds2.length(i);
            }

            public int length(int i, int j) {
                return ds2.length(i, j);
            }

            public int length(int i, int j, int k) {
                return ds2.length(i, j, k);
            }

            public <T> T capability(Class<T> clazz) {
                return null;
            }

            public QDataSet slice(int i) {
                return new Slice0DataSet(this, i);
            }

            public QDataSet trim(int start, int end) {
                return new TrimDataSet(this, start, end );
            }
            
            @Override
            public String toString() {
                return DataSetUtil.toString(this);
            }            
        };
    }

    /**
     * increase the rank of a rank two dataset by adding join dimensions that repeat the lower rank elements.
     * @param ds a rank 2 dataset to provide values and properties
     * @param ds2 dataset to provide rank and lengths.
     * @return a dataset with the same geometry as ds2.
     */
    public static QDataSet increaseRank2(final QDataSet ds, final QDataSet ds2) {

        if ( ds.length()!=ds2.length() ) {
            throw new IllegalArgumentException("datasets must have the same length(): "+ ds + " "+ds2 );
        }
        
        if ( ds.length(0)!=ds2.length(0) ) {
            throw new IllegalArgumentException("datasets must have the same length(0): "+ ds + " "+ds2 );
        }
        
        return new QDataSet() {

            public int rank() {
                return ds2.rank();
            }

            public String svalue() {
                Units u= (Units) property(QDataSet.UNITS);
                if ( u==null ) {
                    return String.valueOf(value());
                } else {
                    return u.createDatum(value()).toString();
                }
            }
            
            public double value() {
                throw new IllegalArgumentException("rank too low");
            }

            public double value(int i) {
                throw new IllegalArgumentException("rank too low");
            }

            public double value(int i0, int i1) {
                return ds.value(i0, i1);
            }

            public double value(int i0, int i1, int i2) {
                return ds.value(i0, i1);
            }

            public double value(int i0, int i1, int i2, int i3) {
                return ds.value(i0, i1);
            }

            public Object property(String name) {
                if ( name.equals(QDataSet.QUBE) ) {
                    return ds2.property(name);
                } else {
                    return ds.property(name);
                }

            }

            public Object property(String name, int i) {
                return ds.property(name, i);
            }

            public int length() {
                return ds2.length();
            }

            public int length(int i) {
                return ds2.length(i);
            }

            public int length(int i, int j) {
                return ds2.length(i, j);
            }

            public int length(int i, int j, int k) {
                return ds2.length(i, j, k);
            }

            public <T> T capability(Class<T> clazz) {
                return null;
            }

            public QDataSet slice(int i) {
                return new Slice0DataSet(this, i);
            }

            public QDataSet trim(int start, int end) {
                return new TrimDataSet(this, start, end );
            }
            
            @Override
            public String toString() {
                return DataSetUtil.toString(this);
            }
        };
    }
}
