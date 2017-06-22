/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.qstream.filter;

import java.nio.ByteBuffer;
import org.das2.qstream.PacketDescriptor;
import org.das2.qstream.PlaneDescriptor;
import org.das2.qstream.StreamComment;
import org.das2.qstream.StreamDescriptor;
import org.das2.qstream.StreamException;
import org.das2.qstream.StreamHandler;


/**
 * Trivial filter just passes stream on to the next guy.  This is
 * useful for starting new filters or debugging.
 * 
 * @author jbf
 */
public class NullFilter implements StreamHandler {

    StreamHandler sink;

    public void setSink( StreamHandler sink ) {
        this.sink= sink;
    }
    
    @Override
    public void streamDescriptor(StreamDescriptor sd) throws StreamException {
        System.err.println(sd);
        sink.streamDescriptor(sd);
    }

    @Override
    public void packetDescriptor(PacketDescriptor pd) throws StreamException {
        System.err.println(pd);
        sink.packetDescriptor(pd);
    }

    @Override
    public void streamClosed(StreamDescriptor sd) throws StreamException {
        System.err.println(sd);
        sink.streamClosed(sd);
    }

    @Override
    public void streamException(StreamException se) throws StreamException {
        System.err.println(se);
        sink.streamException(se);
    }

    @Override
    public void streamComment(StreamComment se) throws StreamException {
        System.err.println(se);
        sink.streamComment(se);
    }

    @Override
    public void packet(PacketDescriptor pd, ByteBuffer data) throws StreamException {

        data.mark();
        for ( PlaneDescriptor planeDescriptor : pd.getPlanes() ) {
            System.err.printf( "%s ", planeDescriptor.getType().read(data) );
        }
        System.err.println();
        data.reset(); // reset the buffer for the sink

        sink.packet(pd, data);
    }

//    public static void main( String[] args ) throws StreamException, FileNotFoundException, IOException {
//        File f = new File( "/home/jbf/ct/hudson/data/qds/proton_density.qds" );
//
//        InputStream in = new FileInputStream(f);
//        QDataSetStreamHandler handler = new QDataSetStreamHandler();
//
//        //ReduceMeanFilter filter= new ReduceMeanFilter();
//        NullFilter filter= new NullFilter();
//
//        filter.setSink( handler );
//
//        StreamTool.readStream( Channels.newChannel(in), filter );
//        QDataSet qds = handler.getDataSet();
//
//        System.err.println( "result= "+qds );
//
//        SimpleStreamFormatter format= new SimpleStreamFormatter();
//        format.format( qds, new FileOutputStream("/tmp/proton_density.new.qds"), true );
//        format.format( qds, new FileOutputStream("/tmp/proton_density.new.binary.qds"), false );
//        
//    }
}
