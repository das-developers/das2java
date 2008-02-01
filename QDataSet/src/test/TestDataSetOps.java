/*
 * TestDataSetOps.java
 *
 * Created on April 1, 2007, 5:22 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package test;

import org.virbo.dataset.DDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.IndexGenDataSet;
import org.virbo.dataset.SortDataSet;
import org.virbo.dataset.WritableDataSet;

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
        
        QDataSet ds = new IndexGenDataSet(8) {
            double data[]= new double[] { 3,5,7,1,2,-1e31, 5,9 };
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
    
    public static void main( String[] args ) {
        new TestDataSetOps();
    }
    
}
