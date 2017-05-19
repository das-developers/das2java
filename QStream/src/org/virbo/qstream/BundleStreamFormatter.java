
package org.virbo.qstream;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.das2.datum.LoggerManager;
import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;
import org.virbo.dsops.Ops;
import test.BundleBinsDemo;

/**
 * Like SimpleStreamFormatter, but this correctly handles bundles.
 * This also shows a brute-force method for formatting streams.
 * @author jbf
 */
public class BundleStreamFormatter {
    
    private static final Logger logger= LoggerManager.getLogger("qstream");
    
    /**
     * format the properties.
     * @param build the StringBuilder, having just added "      <properties>" tag
     * @param bds the bundle dataset
     * @param i the index of the dataset within.
     */
    private void formatProperties( StringBuilder build, QDataSet bds, int i ) {
        String s;
        Units u;
        Number n;
        s= (String) bds.property(QDataSet.DEPENDNAME_0,i);
        if ( s!=null ) {
            build.append( String.format( "        <property name=\"DEPENDNAME_0\" type=\"String\" value=\"%s\"/>\n", s ) );
        } else {
            Object o= bds.property(QDataSet.DEPEND_0,i); // TODO: this is really sloppy, because DEPEND_0 is always supposed to be a dataset...
            logger.fine("DEPEND_0 found that is carrying a name of a dataset instead of the reference to the dataset.");
            if ( o!=null && o instanceof String ) {
                build.append( String.format( "        <property name=\"DEPENDNAME_0\" type=\"String\" value=\"%s\"/>\n", (String)o ) );
            }
        }
        u= (Units) bds.property(QDataSet.UNITS,i);
        if ( u!=null ) {
            build.append( String.format( "        <property name=\"UNITS\" type=\"units\" value=\"%s\"/>\n", u.getId() ) );
        }
        n= (Number) bds.property(QDataSet.FILL_VALUE,i);
        if ( n!=null ) {
            build.append( String.format( "        <property name=\"FILL_VALUE\" type=\"Number\" value=\"%s\"/>\n", n ) );
        }
        n= (Number) bds.property(QDataSet.VALID_MIN,i);
        if ( n!=null ) {
            build.append( String.format( "        <property name=\"VALID_MIN\" type=\"Number\" value=\"%s\"/>\n", n ) );
        }
        n= (Number) bds.property(QDataSet.VALID_MAX,i);
        if ( n!=null ) {
            build.append( String.format( "        <property name=\"VALID_MAX\" type=\"Number\" value=\"%s\"/>\n", n ) );
        }
        n= (Number) bds.property(QDataSet.TYPICAL_MIN,i);
        if ( n!=null ) {
            build.append( String.format( "        <property name=\"TYPICAL_MIN\" type=\"Number\" value=\"%s\"/>\n", n ) );
        }
        n= (Number) bds.property(QDataSet.TYPICAL_MAX,i);
        if ( n!=null ) {
            build.append( String.format( "        <property name=\"TYPICAL_MAX\" type=\"Number\" value=\"%s\"/>\n", n ) );
        }
        s= (String) bds.property(QDataSet.NAME,i);
        if ( s!=null ) {
            build.append( String.format( "        <property name=\"NAME\" type=\"String\" value=\"%s\"/>\n", s ) );
        }
        s= (String) bds.property(QDataSet.LABEL,i);
        if ( s!=null ) {
            build.append( String.format( "        <property name=\"LABEL\" type=\"String\" value=\"%s\"/>\n", s ) );
        }
        s= (String) bds.property(QDataSet.TITLE,i);
        if ( s!=null ) {
            build.append( String.format( "        <property name=\"TITLE\" type=\"String\" value=\"%s\"/>\n", s ) );
        }        
    }
    
    /** allocate a name
     * 
     * @param bds
     * @param j
     * @return 
     */
    private String nameFor( QDataSet bds, int j ) {
        String name= (String) bds.property( QDataSet.NAME, j );
        if ( name==null ) {
            name= (String) bds.property( QDataSet.LABEL, j );
            if ( name!=null ) {
                name= Ops.safeName(name);
            } else {
                String base= "data_";
                Units u= (Units) bds.property(QDataSet.UNITS,j);
                if ( u!=null && UnitsUtil.isTimeLocation(u) ) {
                    base= "time_";
                }
                name= base + j;
            }
        }
        return name;
    }
    
    /**
     * guess an ASCII transfer type which can accurately and efficiently 
     * represent the data in the dataset.  If the format property
     * is found, then a TransferType based on the format is used.
     * @param ds the dataset
     * @return the transfer type.
     */
    public static TransferType guessAsciiTransferType( QDataSet ds ) {
        Units u= SemanticOps.getUnits(ds);
        String format= (String) ds.property( QDataSet.FORMAT );
        if ( format!=null ) {
            Pattern p= Pattern.compile(FORMAT_PATTERN);
            Matcher m= p.matcher(format);
            if ( m.matches() ) {
                char ch= format.charAt(format.length()-1);
                int len= Integer.parseInt(m.group(1));
                String sdec= m.group(2);
                int dec= ( sdec!=null ) ? Integer.parseInt(sdec) : 2 ;
                TransferType result;
                switch ( ch ) {
                    case 'f': 
                        result= new AsciiTransferType( len, false, dec );
                        break;
                    case 'e':
                        result= new AsciiTransferType( len, true, dec );
                        break;
                    case 'd':
                        result= new AsciiIntegerTransferType(len);
                        break;
                    default:
                        result= new AsciiTransferType( 10,true );
                }
                return result;
            } else {
                logger.warning("format string must match "+FORMAT_PATTERN);
                return new AsciiTransferType( 10,true );
            }
        } else {
        
            if ( UnitsUtil.isRatioMeasurement(u) ) {
                QDataSet gcd= DataSetUtil.gcd( Ops.diff(ds), Ops.dataset( u.getOffsetUnits().createDatum(0.0001) ) );
                int fracDigits= (int)Math.ceil( -1 * Math.log10(gcd.value()) );
                QDataSet extent= Ops.extent(ds);
                int intDigits= -1 * (int)Math.log10( Math.abs( extent.value(0) ) );
                intDigits= Math.max( intDigits, (int)Math.log10( Math.abs( extent.value(1) ) ) );
                return new AsciiTransferType( intDigits+1+fracDigits, false, fracDigits );
            } else {
                return new AsciiTransferType( 10, true );
            }
        }
        
    }
    public static final String FORMAT_PATTERN = "(\\%)?(\\d*)(\\.\\d*)?([f|e|d])";
    
    /**
     * format the rank 2 bundle.
     * @param ds rank 2 bundle dataset.
     * @param osout
     * @param asciiTypes true if ascii types should be used.
     * @throws StreamException
     * @throws IOException 
     */
    public void format( QDataSet ds, OutputStream osout, boolean asciiTypes ) throws StreamException, IOException {
        
        if ( ds.property(QDataSet.BUNDLE_1)==null ) throw new IllegalArgumentException("only rank 2 bundles");

        // if there is a depend0 then bundle it as well.
        QDataSet dep0= (QDataSet) ds.property(QDataSet.DEPEND_0);        

        if ( dep0!=null ) {
            QDataSet newBundle= Ops.bundle( dep0, Ops.unbundle( ds, 0 ) );
            for ( int j=1; j<ds.length(0); j++ ) {
                newBundle= Ops.bundle( newBundle, Ops.unbundle( ds, j ) );
            }
            ds= newBundle;
        }
                
        /**
         * bds describes each field of the dataset.
         */
        QDataSet bds= (QDataSet) ds.property(QDataSet.BUNDLE_1);

        /**
         * number of fields.
         */
        int nf= bds.length(); 
        
        /**
         * TransferType array specifies how each field is converted to the stream.
         */
        TransferType[] tt= new TransferType[bds.length()];

        /**
         * record length in bytes.
         */
        int recordLength= 0;
        
        if ( dep0!=null ) {

        }
        
        // calculate the transfer types and total record length.
        for ( int j=0; j<bds.length(); j++ ) {
            if ( asciiTypes ) {
                String format=  (String)bds.property( QDataSet.FORMAT, j );
                Units u= (Units) bds.property( QDataSet.UNITS, j );
                if ( u==null ) u= Units.dimensionless;
                boolean useGuess= false;
                if ( useGuess && format!=null && !UnitsUtil.isTimeLocation(u) ) {
                    tt[j]= guessAsciiTransferType( Ops.slice1(ds,j) );
                } else {
                    if ( UnitsUtil.isTimeLocation(u) ) {
                        tt[j]= new AsciiTimeTransferType( 24, u );
                    } else if ( UnitsUtil.isNominalMeasurement(u) ) {
                        tt[j]= new AsciiIntegerTransferType( 10 );
                    } else if ( format!=null ) {
                        tt[j]= new AsciiTransferType(10,true); //TODO: it would be nice to use the formatter.
                    } else {
                        tt[j]= new AsciiTransferType(10,true);
                    }
                }
            } else {
                Units u= (Units) bds.property( QDataSet.UNITS, j );
                if ( u!=null && UnitsUtil.isTimeLocation(u) ) {
                    tt[j]= new DoubleTransferType();
                } else if ( u!=null && UnitsUtil.isNominalMeasurement(u) ) {
                    tt[j]= new IntegerTransferType( );
                } else {
                    tt[j]= new FloatTransferType();
                }
            }                
            recordLength+= tt[j].sizeBytes();
        }
        
        // pick a default column, since the stream must have a default.
        int defaultColumn= bds.length()<3 ? bds.length()-1 : 1;
        Units u= (Units) bds.property( QDataSet.UNITS, defaultColumn );
        if ( u!=null && UnitsUtil.isTimeLocation(u) && bds.length()>2 ) defaultColumn++;  // startTime, stopTime bins.
        String defaultName= nameFor( bds, defaultColumn );
        
        String rec;
        byte[] bytes;
        
        // stream header
        rec= String.format( "<stream dataset_id=\"%s\"/>\n", defaultName );
        bytes= rec.getBytes( "UTF-8" );
        osout.write( String.format( "[00]%06d", bytes.length ).getBytes( "UTF-8" ) );
        osout.write( bytes );
        
        // packet descriptor
        StringBuilder build= new StringBuilder();
        build.append("<packet>\n");
        for ( int j=0; j<bds.length(); j++ ) {
            String name= nameFor( bds, j ); 
            build.append( String.format( "  <qdataset id=\"%s\" rank=\"1\">\n", name ) );  // TODO: support rank 2 bundled datasets.
            build.append( String.format( "     <properties>\n") );
            formatProperties( build, bds, j );
            build.append( String.format( "     </properties>\n") );
            build.append( String.format( "     <values encoding=\"%s\" length=\"%d\"/>\n", tt[j].name(), 1 ) );  // danger length is not the length in bytes, it's the number of elements.
            build.append( String.format( "  </qdataset>\n" ) );
        }
        build.append("</packet>\n");
        bytes= build.toString().getBytes( "UTF-8" );
        osout.write( String.format( "[01]%06d", bytes.length ).getBytes( "UTF-8" ) );
        osout.write( bytes );

        // format the packets.
        byte[] packet= String.format( ":01:" ).getBytes( "UTF-8" );
        ByteBuffer buf= ByteBuffer.allocate(recordLength);
        for ( int i=0; i<ds.length(); i++ ) {
            osout.write( packet );
            for ( int j=0; j<nf; j++ ) {
                tt[j].write( ds.value(i,j), buf );
            }
            byte[] array= buf.array();
            if ( tt[nf-1].isAscii() && array[recordLength-1]==32 ) {
                array[recordLength-1]= '\n';
            }
            osout.write( array );
            buf.flip();
        }
        
        osout.close();
    }
    
    public static void main( String[] args ) throws StreamException, IOException {
        QDataSet ds= BundleBinsDemo.demo1();
        new BundleStreamFormatter().format( ds, new FileOutputStream("/tmp/jbf/foo.qds"), true );
    }

}
