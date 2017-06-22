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
import org.das2.qds.QDataSet;
import org.das2.qds.QubeDataSetIterator;
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
        //QDataSet qds= test3_binary();
        QDataSet qds= test0_rank2();
        //QDataSet qds= test0_rank2_0();
        
        SimpleStreamFormatter fmt2 = new SimpleStreamFormatter();

        fmt2.format( qds, new FileOutputStream("test0_rank2_0.reformat.qds"), true );
        
    }

    private static QDataSet test0_rank2() throws IOException, StreamException {
        URL url = TestQDataSetStreamHandler.class.getResource("test2.qds");
        QDataSetStreamHandler handler = new QDataSetStreamHandler();
        StreamTool.readStream(Channels.newChannel(url.openStream()), handler);
        QDataSet qds = handler.getDataSet();
        QubeDataSetIterator it= new QubeDataSetIterator(qds);
        while ( it.hasNext() ) {
            it.next();
            System.err.println(" "+it+" "+it.getValue(qds) );
        }
        QDataSet tds= (QDataSet) qds.property(QDataSet.DEPEND_0);
        it= new QubeDataSetIterator(tds);
        while ( it.hasNext() ) {
            it.next();
            System.err.println(" "+it+" "+it.getValue(tds) );
        }
        return qds;
    }    

    private static QDataSet test0_rank2_0() throws IOException, StreamException {
        URL url = TestQDataSetStreamHandler.class.getResource("test0_rank2_1.qds");
        QDataSetStreamHandler handler = new QDataSetStreamHandler();
        StreamTool.readStream(Channels.newChannel(url.openStream()), handler);
        QDataSet qds = handler.getDataSet();
        QubeDataSetIterator it= new QubeDataSetIterator(qds);
        while ( it.hasNext() ) {
            it.next();
            System.err.println(" "+it+" "+it.getValue(qds) );
        }
        QDataSet tds= (QDataSet) qds.property(QDataSet.DEPEND_0);
        it= new QubeDataSetIterator(tds);
        while ( it.hasNext() ) {
            it.next();
            System.err.println(" "+it+" "+it.getValue(tds) );
        }
        return qds;
    }    
    private static QDataSet test3_binary() throws IOException, StreamException {
        URL url = TestQDataSetStreamHandler.class.getResource("test3.binary.qds");
        QDataSetStreamHandler handler = new QDataSetStreamHandler();
        StreamTool.readStream(Channels.newChannel(url.openStream()), handler);
        QDataSet qds = handler.getDataSet();

        return qds;
    }
        
}
