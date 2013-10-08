/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.qstream.filter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.text.ParseException;
import java.util.List;
import org.das2.datum.Datum;
import org.das2.datum.Units;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.MutablePropertyDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dsops.Ops;
import org.virbo.qstream.AsciiTimeTransferType;
import org.virbo.qstream.PacketDescriptor;
import org.virbo.qstream.PlaneDescriptor;
import org.virbo.qstream.SerialStreamFormatter;
import org.virbo.qstream.StreamException;
import org.virbo.qstream.StreamTool;

/**
 * Second attempt at figuring this out.  This uses SerialStreamFormatter
 * to format the new stream packet by packet.
 * 
 * @author jbf
 */
public class MinMaxReduceFilter extends QDataSetsFilter {
    
    SerialStreamFormatter form= new SerialStreamFormatter();
    Datum cadence;
    int icadence;
    QDataSet start= null;
    QDataSet end= null;
    QDataSet offsets;
    
    private static final String NAME= "MinMax";
    
    void init( OutputStream out ) throws IOException, StreamException {
        form.setAsciiTypes(true);
        form.setUnitTransferType( Units.us2000, new AsciiTimeTransferType(17,Units.us2000 ) );
        
        String name= NAME;
        form.init( name, java.nio.channels.Channels.newChannel( out ) );
    }
    
    QDataSetSink mySink= new QDataSetSink() {
        QDataSet ttag;
        @Override
        public void packetData(PacketDescriptor pd, PlaneDescriptor pld, QDataSet ds) {
            List<PlaneDescriptor> planes= pd.getPlanes();
            if ( planes.size()>1 ) {
                if ( pld==planes.get(0) ) {
                    ttag= ds;
                } else if ( pld==planes.get(planes.size()-1) ) {
                    int i=0;
                    while ( (i+icadence)<offsets.length() ) {                        
                        MutablePropertyDataSet mds= DataSetOps.makePropertiesMutable( Ops.extent(ds.trim(i,i+icadence) ) );
                        mds.putProperty( QDataSet.NAME, NAME );
                        mds.putProperty( QDataSet.CONTEXT_0, Ops.add( ttag, offsets.slice(i+icadence/2) ) );
                        try {
                            form.format( "", mds, false );
                        } catch ( IOException ex ) {
                        } catch ( StreamException ex ) {
                        }
                        i+= icadence;
                    }
                }
            } else if ( planes.size()==1 ) {
                offsets= ds;
                icadence= 4;
                while ( icadence<offsets.length()/2 && cadence.gt( DataSetUtil.asDatum(offsets.slice(icadence)).subtract( DataSetUtil.asDatum( offsets.slice(0)) ) ) ) {
                    icadence= icadence*2;
                }
                icadence= icadence/2;                
                if ( icadence<2 ) throw new IllegalArgumentException("should not happen");
            }
        }
        
        
    };
    
    public static void main( String[] args ) throws StreamException, MalformedURLException, IOException, ParseException {
       
        InputStream in= System.in;
        OutputStream out= System.out;
        
        if ( args.length>1 ) {
            in= new URL(args[1]).openStream();
        }
        if ( args.length>2 ) {
            out= new FileOutputStream(args[2]);
        }
        
        if ( args.length==0 || args[0].trim().length()==0 ) {
            System.err.println("java -jar autoplot.jar org.qstream.filter.MinMaxFilter <seconds> [Urlin] [FileOut]");
            System.exit(-1);
        }

        MinMaxReduceFilter me= new MinMaxReduceFilter();
        me.cadence= Units.seconds.parse(args[0]);
        me.init(out);
        me.setSink( me.mySink );
        
        StreamTool.readStream( Channels.newChannel(in), me );
        
        if ( args.length>1 ) {
            in.close();
        }
        if ( args.length>2 ) {
            out.close();
        }
    }
}
