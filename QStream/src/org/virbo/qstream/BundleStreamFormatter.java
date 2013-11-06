/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.qstream;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;
import org.virbo.dataset.QDataSet;
import org.virbo.dsops.Ops;
import test.BundleBinsDemo;

/**
 * Like SimpleStreamFormatter, but this correctly handles bundles.
 * This also shows a brute-force method for formatting streams.
 * @author jbf
 */
public class BundleStreamFormatter {
    
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
     * format the rank 2 bundle.
     * @param ds  rank 2 bundle dataset.
     * @param osout
     * @param asciiTypes
     * @throws StreamException
     * @throws IOException 
     */
    public void format( QDataSet ds, OutputStream osout, boolean asciiTypes ) throws StreamException, IOException {
        
        if ( ds.property(QDataSet.BUNDLE_1)==null ) throw new IllegalArgumentException("only rank 2 bundles");
        
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
        
        // calculate the transfer types and total record length.
        for ( int j=0; j<bds.length(); j++ ) {
            if ( asciiTypes ) {
                Units u= (Units) bds.property( QDataSet.UNITS, j );
                if ( u!=null && UnitsUtil.isTimeLocation(u) ) {
                    tt[j]= new AsciiTimeTransferType( 24, u );
                } else {
                    tt[j]= new AsciiTransferType(10,true);
                }
            } else {
                Units u= (Units) bds.property( QDataSet.UNITS, j );
                if ( u!=null && UnitsUtil.isTimeLocation(u) ) {
                    tt[j]= new DoubleTransferType();
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
