/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.dsops;

import org.virbo.dataset.DDataSet;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.RankZeroDataSet;
import org.virbo.dataset.WritableDataSet;

/**
 * Utility class for reconciling the geometries of two datasets.  For example,
 * a rank 1 dataset's values can be repeated to make it rank 2.
 *
 * TODO: dataset geometry is increased by keeping a reference to a dataset with
 * the target geometry.  This might result in keeping data in memory that would
 * otherwise be released, so a future implementation of this should probably
 * use a non-qube dataset to store each index's length:
 *
 * public int length() {
 *    return lengths.length()
 * }
 * public int length(int i0) {
 *    return lengths.value(i0);
 * }
 * 
 * @author jbf
 */
public class CoerceUtil {

    /**
     * returns true if two datasets have the same number of elements in each dimension.
     * @param ds1
     * @param ds2
     * @return
     */
    static boolean equalGeom(QDataSet ds1, QDataSet ds2) {
        int[] qube1 = DataSetUtil.qubeDims(ds1);
        int[] qube2 = DataSetUtil.qubeDims(ds2);
        if (qube1 == null && qube2 == null) {
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
     * the first dimension element. (Except for qubes the order is arbitrary.)
     * @param ds1 the first operand
     * @param ds2 the second operand
     * @param createResult if true, then a dataset is created where the result can be installed.
     * @param result an empty dataset where the results can be inserted, or null if createResult is false.
     * @return
     */
    public static WritableDataSet coerce(QDataSet ds1, QDataSet ds2, boolean createResult, QDataSet[] operands) {
        if (ds1.rank() == ds2.rank() && equalGeom(ds1, ds2)) {
        } else if (ds1.rank() < ds2.rank()) {
            if (ds1.rank() == 0) {
                ds1 = increaseRank0((RankZeroDataSet) ds1, ds2);
            } else if (ds1.rank() == 1) {
                ds1 = increaseRank1(ds1, ds2);
            } else if (ds1.rank() == 2) {
                ds1 = increaseRank2(ds1, ds2);
            } else {
                throw new IllegalArgumentException("rank limit");
            }

        } else {
            if (ds2.rank() == 0) {
                ds2 = increaseRank0((RankZeroDataSet) ds2, ds1);
            } else if (ds2.rank() == 1) {
                ds2 = increaseRank1(ds2, ds1);
            } else if (ds2.rank() == 2) {
                ds2 = increaseRank2(ds2, ds1);
            } else {
                throw new IllegalArgumentException("rank limit");
            }
        }
        operands[0] = ds1;
        operands[1] = ds2;
        DDataSet result = null;
        if (createResult) {
            result = DDataSet.create(DataSetUtil.qubeDims(ds1));
        }
        return result;
    }

    /**
     * increase the rank of a rank zero dataset by adding join dimensions that repeat the lower rank elements.
     * @param ds a rank 0 dataset
     * @param ds2
     * @return a dataset with the same geometry as ds2.
     */
    static QDataSet increaseRank0(final RankZeroDataSet ds, final QDataSet ds2) {
        return new QDataSet() {

            public int rank() {
                return ds2.rank();
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

            public Object property(String name) {
                return ds.property(name);
            }

            public Object property(String name, int i) {
                return ds.property(name);
            }

            public Object property(String name, int i0, int i1) {
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
        };
    }

    /**
     * increase the rank of a rank one dataset by adding join dimensions that repeat the lower rank elements.
     * @param ds a rank 1 dataset
     * @param ds2
     * @return a dataset with the same geometry as ds2.
     */
    static QDataSet increaseRank1(final QDataSet ds, final QDataSet ds2) {


        return new QDataSet() {

            public int rank() {
                return ds2.rank();
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

            public Object property(String name) {
                return ds.property(name);
            }

            public Object property(String name, int i) {
                return ds.property(name, i);
            }

            public Object property(String name, int i0, int i1) {
                return ds.property(name, i0);
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
        };
    }

    /**
     * increase the rank of a rank two dataset by adding join dimensions that repeat the lower rank elements.
     * @param ds a rank 2 dataset
     * @param ds2
     * @return a dataset with the same geometry as ds2.
     */
    static QDataSet increaseRank2(final QDataSet ds, final QDataSet ds2) {

        return new QDataSet() {

            public int rank() {
                return ds2.rank();
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

            public Object property(String name) {
                return ds.property(name);
            }

            public Object property(String name, int i) {
                return ds.property(name, i);
            }

            public Object property(String name, int i0, int i1) {
                return ds.property(name, i0, i1);
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
        };
    }
}
