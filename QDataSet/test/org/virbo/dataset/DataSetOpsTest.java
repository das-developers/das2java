/*
 * DataSetOpsTest.java
 * JUnit based test
 *
 * Created on April 1, 2007, 5:16 PM
 */

package org.virbo.dataset;

import junit.framework.*;
import java.util.Arrays;
import java.util.Comparator;

/**
 *
 * @author jbf
 */
public class DataSetOpsTest extends TestCase {
    
    public DataSetOpsTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    /**
     * Test of slice0 method, of class org.virbo.dataset.DataSetOps.
     */
    public void testSlice0() {
        System.out.println("slice0");
        
        QDataSet ds = null;
        int index = 0;
        
        QDataSet expResult = null;
        QDataSet result = DataSetOps.slice0(ds, index);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of slice1 method, of class org.virbo.dataset.DataSetOps.
     */
    public void testSlice1() {
        System.out.println("slice1");
        
        QDataSet ds = null;
        int index = 0;
        
        QDataSet expResult = null;
        QDataSet result = DataSetOps.slice1(ds, index);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of toString method, of class org.virbo.dataset.DataSetOps.
     */
    public void testToString() {
        System.out.println("toString");
        
        QDataSet ds = null;
        
        String expResult = "";
        String result = DataSetUtil.toString(ds);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of sort method, of class org.virbo.dataset.DataSetOps.
     */
    public void testSort() {
        System.out.println("sort");
        
        QDataSet ds = new IndexGenDataSet(7) {
            int data[]= new int[] { 3,5,7,1,2,5,9 };
            public double value(int i) {
                return data[i];
            }
        };
        
        QDataSet result = DataSetOps.sort(ds);
        
        assert( result.value(0)==3 && result.value(2)==4 );
        

    }

    /**
     * Test of histogram method, of class org.virbo.dataset.DataSetOps.
     */
    public void testHistogram() {
        System.out.println("histogram");
        
        QDataSet ds = null;
        double min = 0.0;
        double max = 0.0;
        double binsize = 0.0;
        
        QDataSet expResult = null;
        QDataSet result = DataSetOps.histogram(ds, min, max, binsize);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
