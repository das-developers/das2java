/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package test;

import java.util.Random;
import org.das2.datum.Units;
import org.virbo.dataset.AbstractDataSet;
import org.virbo.dataset.DDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dsops.Ops;

/**
 *
 * @author jbf
 */
public class BundleBinsDemo {
     public static QDataSet demo1() {
         final DDataSet result= DDataSet.createRank2(10,6);
         Random r= new Random();
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
                return 3;
            }

            @Override
            public int length(int i) {
                switch (i) {
                    case 0: return 1;
                    case 1: return 1;
                    case 2: return 0;
                    default: throw new IllegalArgumentException("bad index");
                }
            }

            @Override
            public Object property(String name, int i) {
                if ( i==0 ) {
                    if ( QDataSet.BINS_1.equals(name) ) return "min,max";
                    if ( QDataSet.UNITS.equals(name) ) return Units.us2000;
                    if ( QDataSet.NAME.equals(name) ) return "TimeBins";
                } else if ( i==1 ) {
                    if ( QDataSet.DEPEND_1.equals(name) ) return Ops.labels( new String[] { "B-GSM-X","B-GSM-Y","B-GSM-Z" } );
                    if ( QDataSet.DEPEND_0.equals(name) ) return "TimeBins";
                    if ( QDataSet.UNITS.equals(name) ) return Units.meters;
                } else if ( i==2 ) {
                    if ( QDataSet.DEPEND_0.equals(name) ) return "TimeBins";
                    if ( QDataSet.UNITS.equals(name) ) return Units.cmps;
                } else {
                    throw new IllegalArgumentException("bad index");
                }
                return null;
            }

            @Override
            public double value(int i, int i1) {
                if ( i==0 ) {
                    return 2;
                } else if ( i==1 ) {
                    return 3;
                } else if ( i==2 ) {
                    throw new IndexOutOfBoundsException("bad index");
                } else {
                    throw new IndexOutOfBoundsException("bad index");
                }
            }

        };
        result.putProperty( QDataSet.BUNDLE_1, bundleDescriptor );

        return result;
     }
}
