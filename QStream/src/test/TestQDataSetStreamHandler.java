/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import java.nio.channels.Channels;
import javax.xml.parsers.ParserConfigurationException;
import org.virbo.dataset.QDataSet;
import org.virbo.qstream.QDataSetStreamHandler;
import org.virbo.qstream.SimpleStreamFormatter;
import org.virbo.qstream.StreamException;
import org.virbo.qstream.StreamTool;

/**
 *
 * @author jbf
 */
public class TestQDataSetStreamHandler {
    public static void main( String[] args ) throws StreamException, IOException, org.das2.stream.StreamException, ParserConfigurationException {
        URL url= TestQDataSetStreamHandler.class.getResource("test3.binary.qds");
        QDataSetStreamHandler handler= new QDataSetStreamHandler();
        StreamTool.readStream( Channels.newChannel(url.openStream()), handler );
        QDataSet qds= handler.getDataSet();
        
        SimpleStreamFormatter fmt2 = new SimpleStreamFormatter();

        fmt2.format( qds, new FileOutputStream("test3.reformat.qds"), true );
        
    }
        
}
