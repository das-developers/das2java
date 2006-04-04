/*
 * PlasmaModelDataSetDescriptor.java
 *
 * Created on December 11, 2005, 9:54 AM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package edu.uiowa.physics.pw.das.dataset.test;
import edu.uiowa.physics.pw.das.dataset.DataSet;
import edu.uiowa.physics.pw.das.dataset.DataSetDescriptor;
import edu.uiowa.physics.pw.das.dataset.TableDataSetBuilder;
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.datum.DatumVector;
import edu.uiowa.physics.pw.das.datum.Units;
import edu.uiowa.physics.pw.das.util.DasMath;
import edu.uiowa.physics.pw.das.util.DasProgressMonitor;
import java.util.Random;
import testNew.dataset.PlasmaModel;

/**
 *
 * @author Jeremy
 */
public class PlasmaModelDataSetDescriptor extends DataSetDescriptor {
    
    /** Creates a new instance of PlasmaModelDataSetDescriptor */
    public PlasmaModelDataSetDescriptor( ) {
    }
    
    public Units getXUnits() {
        return Units.t2000;
    }
    
    public DataSet getDataSetImpl( Datum start, Datum end, Datum resolution, DasProgressMonitor mon ) {
        PlasmaModel model= new PlasmaModel();
        Random random= new Random(5330);
        
        Units xunits= getXUnits();
        
        double xTagWidth= 13.8;
        double x=start.doubleValue( xunits );
        
        TableDataSetBuilder builder= new TableDataSetBuilder( xunits, Units.dimensionless, Units.dimensionless );
        int i=0;
        
        boolean ylog=false;
        boolean zlog=false;
        
        DatumVector[] yTags= new DatumVector[5];
        
        Random s= new java.util.Random(234567); // repeatable random sequence
                      
        while ( x < end.doubleValue(xunits) ) {        
            int whichYTags= s.nextInt(yTags.length);
            
            int nj;
            if ( yTags[whichYTags]==null ) {
                nj= whichYTags*10+20;
                double[] yy= new double[nj];
                
                for ( int j=0;j<nj; j++ ) {
                    if ( ylog ) {
                        yy[j]= (nj/300)+j*0.05;
                        yy[j]=DasMath.exp10(yy[j]);
                    } else {
                        yy[j]= (nj/3) + j*1.2;
                    }
                }
                
                yTags[whichYTags]= DatumVector.newDatumVector( yy, Units.dimensionless );
            } else {
                nj= yTags[whichYTags].getLength();
            }
            
            
            double[] zz= new double[nj];
            
            int ncol= s.nextInt(4)+1;
            DatumVector ydv= yTags[whichYTags];
            for ( int icol=0; icol<ncol; icol++ ) {
                for ( int j=0;j<nj; j++ ) {
                    zz[j]= model.counts( ydv.get(j).doubleValue(Units.dimensionless), Units.eV, random );
                }
                builder.insertYScan( xunits.createDatum( x ), yTags[whichYTags], DatumVector.newDatumVector(zz,Units.dimensionless) );
                x+= xTagWidth;
            }
        }
        
        return builder.toTableDataSet();
    }
    
}
