package org.das2.qds.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.das2.datum.Datum;
import org.das2.qds.DataSetUtil;
import org.das2.qds.QDataSet;

/**
 * It would be useful to have a method for exporting data when working with
 * QDataSet, so this is introduced.  This is very limited in its functionality,
 * and should be extended as needed.
 * @author jbf
 */
public class AsciiFormatter {
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
    
    public void formatToFile( String f, QDataSet ... dss ) throws IOException {
        formatToFile( new File(f), dss );
    }

    public void formatToFile( File f, QDataSet ds ) throws IOException {
        formatToFile( f, new QDataSet[] { ds } );
    }
    
    public void formatToFile( String f, QDataSet ds ) throws IOException {
        formatToFile( new File(f), new QDataSet[] { ds } );
    }

}
