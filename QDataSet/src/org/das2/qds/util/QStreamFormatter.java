
package org.das2.qds.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import org.das2.datum.Datum;
import org.das2.datum.TimeParser;
import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;
import org.das2.qds.DataSetUtil;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.ops.Ops;

/**
 * Formatter for QStreams, which doesn't require the full QStream parse/format 
 * library, useful for debugging.  
 * 
 * @see org.das2.qstream.SimpleStreamFormatter for a complete implementation.
 * 
 * @author jbf
 */
public class QStreamFormatter {
    public void formatToFile( File f, QDataSet ... dss ) throws IOException {
        Charset charset= Charset.forName("UTF-8");
        String[] encoding= new String[dss.length];
        try ( FileOutputStream fout= new FileOutputStream(f) ) {
            QDataSet ds1= dss[dss.length-1];
            String name1= Ops.guessName( ds1, "ds_"+(dss.length-1) );
            String s;
            s= String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?><stream dataset_id=\"%s\"/>\n", name1 );
            fout.write( String.format("[00]%06d", s.getBytes(charset).length ).getBytes(charset) );
            fout.write( s.getBytes(charset) );
            StringBuilder sb= new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            sb.append("<packet>\n");
            int i=0;
            for ( QDataSet ds : dss ) {
                String namei= Ops.guessName( ds, "ds_"+i );
                sb.append( String.format( "  <qdataset id=\"%s\" rank=\"%d\">\n", namei, ds.rank() ) );
                Units u= SemanticOps.getUnits(ds);
                encoding[i]= UnitsUtil.isTimeLocation(u) ? "time24" : "ascii10";
                sb.append( String.format( "       <values encoding=\"%s\" length=\"\"/>\n",encoding[i]) );
                sb.append( String.format( "       <properties>\n" ) );
                sb.append( String.format( "            <property name=\"UNITS\" type=\"units\" value=\"%s\"/>\n", u.toString() ) );
                sb.append( String.format( "       </properties>\n" ) );
                sb.append( String.format( "  </qdataset>\n") ); 
                i++;
            }
            sb.append( "</packet>\n");
            s= sb.toString();
            fout.write( String.format("[01]%06d", s.getBytes(charset).length ).getBytes(charset) );
            fout.write( s.getBytes(charset) );
            if ( dss.length==0 ) throw new IllegalArgumentException("at least one dataset, please");
            int n= dss[0].length();
            TimeParser tp= TimeParser.create("$Y-$m-$dT$H:$M:$S.$(subsec,places=3)");
            int nds= dss.length;
            for ( int irec=0; irec<n; irec++ ) {
                i=0;
                fout.write( ":01:".getBytes() );
                for ( QDataSet ds : dss ) {
                    if ( ds.rank()==2 ) {
                        for ( int j=0; j<ds.length(irec); j++ ) {
                            if ( encoding[i].equals("time24") ) {
                                fout.write( tp.format( DataSetUtil.asDatum( ds.slice(irec).slice(j) ) ).getBytes(charset) );
                            } else if ( encoding[i].equals("ascii10") ) {
                                fout.write( String.format( "%9.3e", ds.value(irec,j) ).getBytes(charset) );
                            }
                            fout.write( i==(nds-1) && j==(ds.length(irec)-1) ? '\n' : ' ' );
                        }
                    } else if ( ds.rank()==1 ) {
                        Datum d= DataSetUtil.asDatum(ds.slice(irec));
                        if ( encoding[i].equals("time24") ) {
                            fout.write( tp.format( DataSetUtil.asDatum( ds.slice(irec) ) ).getBytes(charset) );
                        } else if ( encoding[i].equals("ascii10") ) {
                            fout.write( String.format( "%9.3e", ds.value(irec) ).getBytes(charset) );
                        }
                        fout.write( i==(nds-1) ? '\n' : ' ' );
                    }
                    i++;
                }
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
