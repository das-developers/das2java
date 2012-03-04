/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.qstream.filter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import org.virbo.dataset.QDataSet;
import org.virbo.qstream.PacketDescriptor;
import org.virbo.qstream.PlaneDescriptor;
import org.virbo.qstream.QDataSetStreamHandler;
import org.virbo.qstream.StreamDescriptor;
import org.virbo.qstream.StreamException;
import org.virbo.qstream.StreamHandler;
import org.virbo.qstream.StreamTool;


/**
 * Trivial filter just passes stream on to the next guy.  This is
 * useful for starting new filters or debugging.
 * 
 * @author jbf
 */
public class NullFilter implements StreamHandler {

    StreamHandler sink;

    double length=60;

    public void streamDescriptor(StreamDescriptor sd) throws StreamException {
        System.err.println(sd);
        sink.streamDescriptor(sd);
    }

    public void packetDescriptor(PacketDescriptor pd) throws StreamException {
        System.err.println(pd);
        sink.packetDescriptor(pd);
    }

    public void streamClosed(StreamDescriptor sd) throws StreamException {
        System.err.println(sd);
        sink.streamClosed(sd);
    }

    public void streamException(StreamException se) throws StreamException {
        System.err.println(se);
        sink.streamException(se);
    }

    public void packet(PacketDescriptor pd, ByteBuffer data) throws StreamException {
        //System.err.println(pd);

        data.mark();
        for ( PlaneDescriptor planeDescriptor : pd.getPlanes() ) {
            System.err.printf( "%s ", planeDescriptor.getType().read(data) );
        }
        System.err.println();
        data.reset();

        sink.packet(pd, data);
    }

    public static void main( String[] args ) throws StreamException, FileNotFoundException {
        File f = new File( "/home/jbf/ct/hudson/data/qds/proton_density.qds" );

        InputStream in = new FileInputStream(f);
        QDataSetStreamHandler handler = new QDataSetStreamHandler();

        //ReduceMeanFilter filter= new ReduceMeanFilter();
        NullFilter filter= new NullFilter();

        filter.sink= handler;

        StreamTool.readStream( Channels.newChannel(in), filter );
        QDataSet qds = handler.getDataSet();

        System.err.println( "result= "+qds );
    }
}
