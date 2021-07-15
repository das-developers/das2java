/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package test;

import java.util.Random;
import org.das2.datum.Units;
import org.das2.qds.AbstractDataSet;
import org.das2.qds.DDataSet;
import org.das2.qds.DataSetOps;
import org.das2.qds.QDataSet;
import org.das2.qds.ops.Ops;

/**
 * Demonstrations of the somewhat nasty BundleDataSet, which allows many data types to be encoded into a rank 2 table.
 * @author jbf
 */
public class BundleBinsDemo {
    /**
     * create a bundle of [ rank 2 time bins, rank 2 B-gsm, rank 1 cmps ]
     * @return a rank 2 bundle.
     */
     public static QDataSet demo1() {
         final DDataSet result= DDataSet.createRank2(10,6);
         Random r= new Random(12345);
         for ( int i=0; i<result.length(); i++ ) {
             result.putValue(i,0,i*3600e6);
             result.putValue(i,1,(i+1)*3600e6);
             result.putValue(i,2,r.nextDouble());
             result.putValue(i,3,r.nextDouble());
             result.putValue(i,4,r.nextDouble());
             result.putValue(i,5,r.nextDouble());
         }
         QDataSet bundleDescriptor= new AbstractDataSet() {
            @Override
            public int rank() {
                return 2;
            }

            @Override
            public int length() {
                return 6;
            }

            @Override
            public int length(int i) {
                switch (i) {
                    case 0: return 1;
                    case 1: return 1;
                    case 2: return 1;
                    case 3: return 1;
                    case 4: return 1;
                    case 5: return 0;
                    default: throw new IllegalArgumentException("bad index");
                }
            }

            @Override
            public Object property(String name, int i) {
                if ( QDataSet.NAME.equals(name) ) {
                    switch (i) {
                        case 0: return "TimeMin";
                        case 1: return "TimeMax";
                        case 2: return "BGSM_X";
                        case 3: return "BGSM_Y";
                        case 4: return "BGSM_Z";
                        case 5: return "Speed";
                    }
                }
                if ( i==0 || i==1 ) {
                    if ( QDataSet.BINS_1.equals(name) ) return QDataSet.VALUE_BINS_MIN_MAX;
                    if ( QDataSet.UNITS.equals(name) ) return Units.us2000;
                    //if ( QDataSet.NAME.equals(name) ) return "TimeBins";
                    if ( QDataSet.ELEMENT_NAME.equals(name) ) return "TimeBins";
                    if ( QDataSet.START_INDEX.equals(name) ) return 0;

                } else if ( i==2 || i==3 || i==4 ) {
                    if ( QDataSet.DEPEND_1.equals(name) ) return Ops.labelsDataset( new String[] { "B-GSM-X","B-GSM-Y","B-GSM-Z" } );
                    if ( QDataSet.DEPENDNAME_0.equals(name) ) return "TimeBins";
                    if ( QDataSet.UNITS.equals(name) ) return Units.meters;
                    //if ( QDataSet.NAME.equals(name) ) return "BGSM";
                    if ( QDataSet.ELEMENT_NAME.equals(name) ) return "BGSM";
                    if ( QDataSet.START_INDEX.equals(name) ) return 2;

                } else if ( i==5 ) {
                    if ( QDataSet.DEPENDNAME_0.equals(name) ) return "TimeBins";
                    if ( QDataSet.UNITS.equals(name) ) return Units.cmps;
                    //if ( QDataSet.NAME.equals(name) ) return "Speed";

                } else {
                    throw new IllegalArgumentException("bad index");
                }
                return null;
            }

            @Override
            public double value(int i, int i1) {
                if ( i==0 || i==1) {
                    return 2;
                } else if ( i==2 || i==3 || i==4 ) {
                    return 3;
                } else if ( i==5 ) {
                    throw new IndexOutOfBoundsException("bad index");
                } else {
                    throw new IndexOutOfBoundsException("bad index");
                }
            }

        };
        result.putProperty( QDataSet.BUNDLE_1, bundleDescriptor );

        return result;
     }

     public static void main( String[] args ) {
         QDataSet ds= demo1();
         System.err.println( DataSetOps.unbundle( ds, "TimeBins" ) );
         System.err.println( DataSetOps.unbundle( ds, "BGSM" ) );
         System.err.println( DataSetOps.unbundle( ds, "Speed" ) );

     }
}
