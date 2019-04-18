package org.das2.qds.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.das2.datum.Datum;
import org.das2.qds.DDataSet;
import org.das2.qds.DataSetUtil;
import org.das2.qds.JoinDataSet;
import org.das2.qds.QDataSet;

/**
 * It would be useful to have a method for exporting data when working with
 * QDataSet, so this is introduced.  This is very limited in its functionality,
 * and should be extended as needed.
 * @author jbf
 */
public class AsciiFormatter {
    
    /**
     * format to the given file
     * @param f the file
     * @param dss the datasets
     * @throws IOException 
     */
    public void formatToFile( File f, QDataSet ... dss ) throws IOException {
        try ( FileOutputStream fout= new FileOutputStream(f) ) {
            if ( dss.length==0 ) throw new IllegalArgumentException("at least one dataset, please");
            int n= dss[0].length();
            for ( int i=0; i<n; i++ ) {
                int ifield= 0;
                for ( QDataSet ds : dss ) {
                    if ( ds.rank()==2 ) {
                        for ( int j=0; j<ds.length(i); j++ ) {
                            if ( ifield>0 ) fout.write(',');
                            fout.write(String.valueOf(ds.value(i,j)).getBytes());   
                            ifield++;
                        }
                    } else if ( ds.rank()==1 ) {
                        if ( ifield>0 ) fout.write(',');
                        Datum d= DataSetUtil.asDatum(ds.slice(i));
                        String s= d.getUnits().getDatumFormatterFactory().defaultFormatter().format( d, d.getUnits() );
                        fout.write( s.getBytes());   
                        ifield++;
                    }
                }
                fout.write(10);
            }
        }
    }
    
    /**
     * format to the given file name.
     * @param f the file name
     * @param dss the datasets
     * @throws IOException 
     */
    public void formatToFile( String f, QDataSet ... dss ) throws IOException {
        formatToFile( new File(f), dss );
    }

    /**
     * format to the given file.
     * @param f the file
     * @param ds the datasets
     * @throws IOException 
     */
    public void formatToFile( File f, QDataSet ds ) throws IOException {
        formatToFile( f, new QDataSet[] { ds } );
    }
    
    /**
     * format to the given file
     * @param f the file name
     * @param ds the dataset
     * @throws IOException 
     */
    public void formatToFile( String f, QDataSet ds ) throws IOException {
        formatToFile( new File(f), new QDataSet[] { ds } );
    }
    
    /**
     * format to the given file
     * @param f the file name
     * @param dd the rank 2 data.
     * @throws IOException 
     */
    public void formatToFile( String f, double[][] dd ) throws IOException {
        JoinDataSet jds= new JoinDataSet(2);
        for (double[] dd1 : dd) {
            QDataSet ds = DDataSet.wrap(dd1);
            jds.join(ds);
        }
        formatToFile( new File(f), jds );
    }

}
