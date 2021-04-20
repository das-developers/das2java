/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.dataset;

import org.das2.qds.DDataSet;
import org.das2.qds.DRank0DataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.DataSetUtil;
import org.das2.qds.WritableDataSet;
import org.das2.qds.RankZeroDataSet;
import org.das2.qds.MutablePropertyDataSet;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;
import org.das2.datum.Datum;
import org.das2.datum.TimeUtil;
import org.das2.datum.Units;

/**
 *
 * @author jbf
 */
public class DataSetUtilTest extends TestCase {
    
    private static final Logger logger= Logger.getLogger("test");
    public DataSetUtilTest(String testName) {
        super(testName);
    }

    /**
     * Test of indexGenDataSet method, of class DataSetUtil.
     */
    public void testIndexGenDataSet() {
        System.out.println("indexGenDataSet");
        int n = 0;
        MutablePropertyDataSet expResult = null;
        MutablePropertyDataSet result = DataSetUtil.indexGenDataSet(n);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of tagGenDataSet method, of class DataSetUtil.
     */
    public void testTagGenDataSet_3args() {
        System.out.println("tagGenDataSet");
        int n = 0;
        double start = 0.0;
        double cadence = 0.0;
        MutablePropertyDataSet expResult = null;
        MutablePropertyDataSet result = DataSetUtil.tagGenDataSet(n, start, cadence);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of tagGenDataSet method, of class DataSetUtil.
     */
    public void testTagGenDataSet_4args() {
        System.out.println("tagGenDataSet");
        int n = 0;
        double start = 0.0;
        double cadence = 0.0;
        Units units = null;
        MutablePropertyDataSet expResult = null;
        MutablePropertyDataSet result = DataSetUtil.tagGenDataSet(n, start, cadence, units);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of replicateDataSet method, of class DataSetUtil.
     */
    public void testReplicateDataSet() {
        System.out.println("replicateDataSet");
        int n = 0;
        double value = 0.0;
        MutablePropertyDataSet expResult = null;
        MutablePropertyDataSet result = DataSetUtil.replicateDataSet(n, value);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of isMonotonic method, of class DataSetUtil.
     */
    public void testIsMonotonic() {
        System.out.println("isMonotonic");
        QDataSet ds = null;
        boolean expResult = false;
        boolean result = DataSetUtil.isMonotonic(ds);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of binarySearch method, of class DataSetUtil.
     */
    public void testBinarySearch() {
        System.out.println("binarySearch");
        QDataSet ds = null;
        double key = 0.0;
        int low = 0;
        int high = 0;
        int expResult = 0;
        int result = DataSetUtil.binarySearch(ds, key, low, high);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of closest method, of class DataSetUtil.
     */
    public void testClosest() {
        System.out.println("closest");
        QDataSet ds = null;
        double d = 0.0;
        int guess = 0;
        int expResult = 0;
        int result = DataSetUtil.closest(ds, d, guess);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of propertyNames method, of class DataSetUtil.
     */
    public void testPropertyNames() {
        System.out.println("propertyNames");
        String[] expResult = null;
        String[] result = DataSetUtil.propertyNames();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getProperties method, of class DataSetUtil.
     */
    public void testGetProperties_QDataSet_Map() {
        System.out.println("getProperties");
        QDataSet ds = null;
        Map def = null;
        Map expResult = null;
        Map result = DataSetUtil.getProperties(ds, def);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getProperties method, of class DataSetUtil.
     */
    public void testGetProperties_QDataSet() {
        System.out.println("getProperties");
        QDataSet ds = null;
        Map expResult = null;
        Map result = DataSetUtil.getProperties(ds);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of putProperties method, of class DataSetUtil.
     */
    public void testPutProperties() {
        System.out.println("putProperties");
        Map<String, Object> properties = null;
        MutablePropertyDataSet ds = null;
        DataSetUtil.putProperties(properties, ds);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of toString method, of class DataSetUtil.
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
     * Test of firstValidPoint method, of class DataSetUtil.
     */
    public void testFirstValidPoint() {
        System.out.println("firstValidPoint");
        QDataSet ds = null;
        QDataSet expResult = null;
        QDataSet result = DataSetUtil.firstValidPoint(ds);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of validPoints method, of class DataSetUtil.
     */
    public void testValidPoints() {
        System.out.println("validPoints");
        QDataSet ds = null;
        QDataSet expResult = null;
        QDataSet result = DataSetUtil.validPoints(ds);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of gcd method, of class DataSetUtil.
     */
    public void testGcd_QDataSet_QDataSet() {
        System.out.println("gcd");
        QDataSet ds = null;
        QDataSet d = null;
        QDataSet expResult = null;
        QDataSet result = DataSetUtil.gcd(ds, d);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of gcd method, of class DataSetUtil.
     */
    public void testGcd_QDataSet() {
        System.out.println("gcd");
        QDataSet ds = DDataSet.wrap( new double[] { 4.60040, 6.90060, 16.1014, 52.9046, 96.6084, 105.8092, 2.30020, 128.8112 } );
        QDataSet expResult = DataSetUtil.asDataSet(2.3002);
        QDataSet limit= DataSetUtil.asDataSet(1e-7);
        QDataSet result = DataSetUtil.gcd(ds,limit);
        assertEquals( expResult.value(), result.value(), limit.value() );
        
    }

    /**
     * Test of guessCadenceNew method, of class DataSetUtil.
     */
    public void testGuessCadenceNew() {
        System.out.println("guessCadenceNew");
        QDataSet xds = null;
        QDataSet yds = null;
        RankZeroDataSet expResult = null;
        RankZeroDataSet result = DataSetUtil.guessCadenceNew(xds, yds);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of guessCadence method, of class DataSetUtil.
     */
    public void testGuessCadence_QDataSet_QDataSet() {
        System.out.println("guessCadence");
        QDataSet xds = null;
        QDataSet yds = null;
        QDataSet expResult = null;
        QDataSet result = DataSetUtil.guessCadenceNew(xds, yds);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of guessCadence method, of class DataSetUtil.
     */
    public void testGuessCadence_QDataSet() {
        System.out.println("guessCadence");
        QDataSet xds = null;
        QDataSet expResult = null;
        QDataSet result = DataSetUtil.guessCadence(xds,null);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of isQube method, of class DataSetUtil.
     */
    public void testIsQube() {
        System.out.println("isQube");
        QDataSet ds = null;
        boolean expResult = false;
        boolean result = DataSetUtil.isQube(ds);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of qubeDims method, of class DataSetUtil.
     */
    public void testQubeDims() {
        System.out.println("qubeDims");
        QDataSet ds = null;
        int[] expResult = null;
        int[] result = DataSetUtil.qubeDims(ds);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of addQube method, of class DataSetUtil.
     */
    public void testAddQube() {
        System.out.println("addQube");
        MutablePropertyDataSet ds = null;
        DataSetUtil.addQube(ds);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of format method, of class DataSetUtil.
     */
    public void testFormat() {
        System.out.println("format");
        QDataSet ds = null;
        String expResult = "";
        String result = DataSetUtil.format(ds);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of statsString method, of class DataSetUtil.
     */
    public void testStatsString() {
        System.out.println("statsString");
        QDataSet ds = null;
        String expResult = "";
        String result = DataSetUtil.statsString(ds);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of validate method, of class DataSetUtil.
     */
    public void testValidate() {
        System.out.println("validate");
        QDataSet ds = null;
        List<String> problems = null;
        boolean expResult = false;
        boolean result = DataSetUtil.validate(ds, problems);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of totalLength method, of class DataSetUtil.
     */
    public void testTotalLength() {
        System.out.println("totalLength");
        QDataSet ds = null;
        int expResult = 0;
        int result = DataSetUtil.totalLength(ds);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of makeValid method, of class DataSetUtil.
     */
    public void testMakeValid() {
        System.out.println("makeValid");
        MutablePropertyDataSet ds = null;
        DataSetUtil.makeValid(ds);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of weightsDataSet method, of class DataSetUtil.
     */
    public void testWeightsDataSet() {
        System.out.println("weightsDataSet");
        QDataSet ds = null;
        QDataSet expResult = null;
        QDataSet result = DataSetUtil.weightsDataSet(ds);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of canonizeFill method, of class DataSetUtil.
     */
    public void testCanonizeFill() {
        System.out.println("canonizeFill");
        QDataSet ds = null;
        WritableDataSet expResult = null;
        WritableDataSet result = DataSetUtil.canonizeFill(ds);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of convertTo method, of class DataSetUtil.
     */
    public void testConvertTo() {
        System.out.println("convertTo");
        QDataSet ds = null;
        Units u = null;
        QDataSet expResult = null;
        QDataSet result = DataSetUtil.convertTo(ds, u);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of value method, of class DataSetUtil.
     */
    public void testValue() {
        try {
            System.out.println("value");
            RankZeroDataSet ds = (RankZeroDataSet) DataSetUtil.asDataSet( TimeUtil.parseTime("2000-01-01T01:00" ) );
            Units tu = Units.us2000;
            double expResult = 3600e6;
            double result = DataSetUtil.value(ds, tu);
            assertEquals(expResult, result, 0.0);
        } catch (ParseException ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    /**
     * Test of asDatum method, of class DataSetUtil.
     */
    public void testAsDatum() {
        System.out.println("asDatum");
        RankZeroDataSet ds = null;
        Datum expResult = null;
        Datum result = DataSetUtil.asDatum(ds);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of asDataSet method, of class DataSetUtil.
     */
    public void testAsDataSet_double_Units() {
        System.out.println("asDataSet");
        double d = 0.0;
        Units u = null;
        DRank0DataSet expResult = null;
        DRank0DataSet result = DataSetUtil.asDataSet(d, u);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of asDataSet method, of class DataSetUtil.
     */
    public void testAsDataSet_double() {
        System.out.println("asDataSet");
        double d = 0.0;
        DRank0DataSet expResult = null;
        DRank0DataSet result = DataSetUtil.asDataSet(d);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of asDataSet method, of class DataSetUtil.
     */
    public void testAsDataSet_Datum() {
        System.out.println("asDataSet");
        Datum d = null;
        DRank0DataSet expResult = null;
        DRank0DataSet result = DataSetUtil.asDataSet(d);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of asDataSet method, of class DataSetUtil.
     */
    public void testAsDataSet_Object() {
        System.out.println("asDataSet");
        Object arr = null;
        QDataSet expResult = null;
        QDataSet result = DataSetUtil.asDataSet(arr);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of addContext method, of class DataSetUtil.
     */
    public void testAddContext() {
        System.out.println("addContext");
        MutablePropertyDataSet ds = null;
        QDataSet cds = null;
        DataSetUtil.addContext(ds, cds);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of contextAsString method, of class DataSetUtil.
     */
    public void testContextAsString() {
        System.out.println("contextAsString");
        QDataSet ds = null;
        String expResult = "";
        String result = DataSetUtil.contextAsString(ds);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

}
