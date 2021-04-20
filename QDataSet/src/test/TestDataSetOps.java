/*
 * TestDataSetOps.java
 *
 * Created on April 1, 2007, 5:22 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package test;

import java.util.HashMap;
import java.util.Map;
import org.das2.datum.Units;
import org.das2.qds.AbstractRank1DataSet;
import org.das2.qds.DDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.DataSetOps;
import org.das2.qds.DataSetUtil;
import org.das2.qds.IndexGenDataSet;
import org.das2.qds.SortDataSet;
import org.das2.qds.WritableDataSet;
import org.das2.qds.ops.Ops;

/**
 *
 * @author jbf
 */
public class TestDataSetOps {
    
    /** Creates a new instance of TestDataSetOps */
    public TestDataSetOps() {
        test1();
        
        test2();
        test3();
        test4();
    }

    private void test4() {
        QDataSet tags= DataSetUtil.tagGenDataSet( 10 , 0 , 1 );
        System.err.println( DataSetUtil.closest( tags, 5.5, -1 ) );
    }
        
        
    private void test3() {
        System.err.println("DDataSet.version="+DDataSet.version );
        
    }
    
    private void test2() {
        QDataSet tags= DDataSet.wrap(         new double[] {  4,  8,  9,10, 5, 6,   7,  1,  2, 3 } );
        WritableDataSet data= DDataSet.wrap( new double[] { 24,28,29,30,25,26,27,28,29,30 } );
        data.putProperty( QDataSet.DEPEND_0, tags );
        QDataSet sort= DataSetOps.sort(tags);
        
       QDataSet sortData= new SortDataSet( data, sort );
        
        QDataSet depend0= (QDataSet) sortData.property( QDataSet.DEPEND_0 );
        for ( int i=0; i<data.length(); i++ ){
            System.err.println( "" +depend0.value(i) + "  " + sortData.value(i) );
        }
    }
    
    
    private void test1() {

        System.out.println("sort");
        
        QDataSet ds = new AbstractRank1DataSet(8) {
            double data[]= new double[] { 3,5,7,1,2,-1e31, 5,9 };
            @Override
            public double value(int i) {
                return data[i];
            }
        };
        
        QDataSet result = DataSetOps.sort(ds);
        
        for ( int i=0; i<result.length(); i++ ) {
            System.err.println("  "+ds.value((int)result.value(i)) );
        }
        
        assert( result.value(0)==3 && result.value(2)==4 );
    }

    public static void testSliceIndexProps() {
        Map m= new HashMap<String,Object>();
        m.put( "UNITS__0", Units.hertz );
        m.put( "UNITS__1", Units.eV );
        m.put( "UNITS__1_100", Units.eV );
        m.put( "UNITS__2", Units.seconds );
        System.err.println( DataSetOps.sliceProperties0(1, m) );
    }

    public static void testUnbundleRank1() {
        QDataSet bundle;
        bundle= Ops.bundle( null, DataSetUtil.asDataSet(0,Units.dB) );
        bundle= Ops.bundle( bundle, DataSetUtil.asDataSet(1,Units.dB) );
        bundle= Ops.bundle( bundle, DataSetUtil.asDataSet(2,Units.dB) );
        bundle= Ops.bundle( bundle, DataSetUtil.asDataSet(3,Units.bytesPerSecond) );
        bundle= Ops.bundle( bundle, DataSetUtil.asDataSet(4,Units.dB) );

        System.err.println( DataSetOps.unbundle(bundle,2) );
    }

    public static void testArrayOfArrayToRank2DataSet() {
        double[][] a= new double[4][];
        for ( int i=0; i<4; i++ ) {
            a[i]= new double[5];
            for ( int j=i; j<i+5; j++ ) {
                a[i][j-i]= j;
            }
        }
        QDataSet ds= Ops.dataset(a);
        System.err.println( ds.value(1,1) );
    }
    
    public static void main( String[] args ) {
        testArrayOfArrayToRank2DataSet();
        //new TestDataSetOps();
        //testSliceIndexProps();
        //testUnbundleRank1();
    }
    
}
