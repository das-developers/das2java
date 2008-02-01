/*
 * DataSetUtilTest.java
 * JUnit based test
 *
 * Created on April 1, 2007, 5:15 PM
 */

package org.virbo.dataset;

import junit.framework.*;

/**
 *
 * @author jbf
 */
public class DataSetUtilTest extends TestCase {
    
    public DataSetUtilTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    /**
     * Test of indexGenDataSet method, of class org.virbo.dataset.DataSetUtil.
     */
    public void testIndexGenDataSet() {
        System.out.println("indexGenDataSet");
        
        int n = 0;
        
        QDataSet expResult = null;
        QDataSet result = DataSetUtil.indexGenDataSet(n);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of tagGenDataSet method, of class org.virbo.dataset.DataSetUtil.
     */
    public void testTagGenDataSet() {
        System.out.println("tagGenDataSet");
        
        int n = 0;
        double start = 0.0;
        double cadence = 0.0;
        
        QDataSet expResult = null;
        QDataSet result = DataSetUtil.tagGenDataSet(n, start, cadence);
        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
