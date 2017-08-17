package org.das2.qds;

import org.das2.datum.Datum;
import org.das2.datum.TimeUtil;
import org.das2.datum.Units;
import org.das2.datum.UnitsConverter;
import org.das2.datum.UnitsUtil;

/**
 * convert rank 2 waveform dataset into an equivalent rank 1 dataset.  Note that
 * data valid properties for the data's DEPEND_0 are ignored.
 * @author jbf
 */
public class FlattenWaveformDataSet extends AbstractDataSet {
    QDataSet ds;
    final int n;
    
    public FlattenWaveformDataSet( QDataSet ds ) {
        this.ds= ds;
        if ( !SemanticOps.isRank2Waveform(ds) ) {
            throw new IllegalArgumentException("dataset must be rank 2 waveform");
        }
        n= ds.length(0);
        setupDep0();
    }
    
    @Override
    public int rank() {
        return 1;
    }

    @Override
    public int length() {
        return n*ds.length();
    }


    @Override
    public double value(int i0) {
        return ds.value(i0/n,i0%n);
    }

    @Override
    public Object property(String name) {
        Object v= super.property(name);
        if ( v==null ) {
            if ( DataSetUtil.isInheritedProperty(name)) {
                return ds.property(name);
            } else {
                return null;
            }
        } else {
            return v;
        }
    }
    
    /**
     * create new depend0 so that the resolution is preserved (to within nanoseconds)
     * Note VALID_MIN, VALID_MAX, FILL_VALUE, TYPICAL_MIN, TYPICAL_MAX are all dropped.
     */
    private void setupDep0() {
        final QDataSet dsdep0 = (QDataSet) ds.property(QDataSet.DEPEND_0);
        final QDataSet dsdep1 = (QDataSet) ds.property(QDataSet.DEPEND_1);
        Units dep0units= SemanticOps.getUnits(dsdep0);
        Units newDep0Units= dep0units;
        if ( UnitsUtil.isTimeLocation( dep0units ) ) {
            Datum t0= DataSetUtil.asDatum(dsdep0.slice(0)); // pick a close month so that we don't create too many types of units.
            int [] timeArray= TimeUtil.fromDatum(t0);
            timeArray[2]= 1;
            timeArray[3]= 0;
            timeArray[4]= 0;
            timeArray[5]= 0;
            timeArray[6]= 0;
            t0= TimeUtil.toDatum(timeArray);
            newDep0Units= Units.lookupTimeUnits( t0, (Units)dsdep1.property(QDataSet.UNITS) );
        }
        final UnitsConverter ucbase= UnitsConverter.getConverter( dep0units, newDep0Units );
        final Units fnewDep0Units= newDep0Units;
        final UnitsConverter uc= UnitsConverter.getConverter( SemanticOps.getUnits(dsdep1), fnewDep0Units.getOffsetUnits() );
        MutablePropertyDataSet dep0= new AbstractDataSet() {
            @Override
            public int rank() {
                return 1;
            }
            @Override
            public int length() {
                return n*ds.length();
            }
            @Override
            public double value(int i0) {
                return ucbase.convert(dsdep0.value(i0/n))+uc.convert(dsdep1.value(i0%n));
            }

            @Override
            public Object property(String name) {
                switch (name) {
                    case QDataSet.CADENCE:
                        return dsdep1.property(QDataSet.CADENCE);
                    case QDataSet.UNITS:
                        return fnewDep0Units;
                    case QDataSet.VALID_MIN:
                        return null;
                    case QDataSet.VALID_MAX:
                        return null;
                    case QDataSet.TYPICAL_MIN:
                        return null;
                    case QDataSet.TYPICAL_MAX:
                        return null;
                    case QDataSet.FILL_VALUE:
                        return null;
                    default:
                        if ( DataSetUtil.isInheritedProperty(name) ) {
                            return dsdep0.property(name);
                        } else {
                            return null;
                        }
                }
            }


        };
        this.putProperty( QDataSet.DEPEND_0, dep0 );
    }
}
