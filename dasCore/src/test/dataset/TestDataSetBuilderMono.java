/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package test.dataset;

import org.das2.dataset.TableDataSet;
import org.das2.dataset.TableDataSetBuilder;
import org.das2.datum.DatumVector;
import org.das2.datum.Units;

/**
 *
 * @author jbf
 */
public class TestDataSetBuilderMono {
    public static void main( String[] args ) {
        TableDataSetBuilder b= new TableDataSetBuilder( Units.dimensionless, Units.dimensionless, Units.dimensionless );
        DatumVector y= DatumVector.newDatumVector( new double[10], Units.dimensionless );
        DatumVector z= DatumVector.newDatumVector( new double[10], Units.dimensionless );
        b.insertYScan( Units.dimensionless.createDatum(0), y, z );
        b.insertYScan( Units.dimensionless.createDatum(1), y, z );
        b.insertYScan( Units.dimensionless.createDatum(2), y, z );
        b.insertYScan( Units.dimensionless.createDatum(3), y, z );
        TableDataSet tds= b.toTableDataSet();
    }
}
