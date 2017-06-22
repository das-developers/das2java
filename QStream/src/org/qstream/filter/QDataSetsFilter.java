/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.qstream.filter;

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.das2.qds.DDataSet;
import org.das2.qds.DataSetUtil;
import org.das2.qds.MutablePropertyDataSet;
import org.das2.qds.QDataSet;
import org.virbo.qstream.PacketDescriptor;
import org.virbo.qstream.PlaneDescriptor;
import org.virbo.qstream.SerializeDelegate;
import org.virbo.qstream.SerializeRegistry;
import org.virbo.qstream.StreamComment;
import org.virbo.qstream.StreamDescriptor;
import org.virbo.qstream.StreamException;
import org.virbo.qstream.StreamHandler;
import org.virbo.qstream.TransferType;
import org.virbo.qstream.Util;
import org.virbo.qstream.XMLSerializeDelegate;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Use this to promote the abstraction of the stream to QDataSets.  This was
 * introduced when it became clear that to introduce an FFT filter would be
 * quite difficult with the StreamHandler interface.  For example, take a simple
 * rank 2 spectrogram.  The DEPEND_1 tags can be encoded inline, or as a single
 * packet.  It would be burdensome to have to handle both cases.
 *
 * @author jbf
 */
public class QDataSetsFilter implements StreamHandler {

    protected static final Logger logger= Logger.getLogger("qstream");

    /**
     * implement this to receive QDataSets packet-by-packet.
     */
    public static class QDataSetSink implements StreamHandler {

        @Override
        public void streamDescriptor(StreamDescriptor sd) throws StreamException {
        }

        @Override
        public void packetDescriptor(PacketDescriptor pd) throws StreamException {
        }

        @Override
        public void packet(PacketDescriptor pd, ByteBuffer data) throws StreamException {
        }

        @Override
        public void streamClosed(StreamDescriptor sd) throws StreamException {
        }

        @Override
        public void streamException(StreamException se) throws StreamException {
        }

        @Override
        public void streamComment(StreamComment se) throws StreamException {
        }
        
        /**
         * QDataSets or parts of datasets as they come in.
         * @param sd
         * @param ds
         */
        public void packetData( PacketDescriptor pd, PlaneDescriptor pld, QDataSet ds ) {
        }

    }

    /**
     * send packets on to here
     */
    QDataSetSink sink;

    Map<String,Map<String,Object>> propsn= new HashMap();

    /**
     *
     * @param n node containing properties
     * @param props map to insert properties, or null.
     */
    private static Map<String,Object> doProps( Node n, Map<String,Object> props ) {

        if ( props==null ) {
            props= new LinkedHashMap();
        }

        if ( n==null ) return props;

        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();

        try {
            NodeList odims = (NodeList) xpath.evaluate("property", n, XPathConstants.NODESET );

            for (int j = 0; j < odims.getLength(); j++) {
                Element n2 = (Element) odims.item(j);
                String pname = n2.getAttribute("name");
                if ( pname.equals(QDataSet.USER_PROPERTIES) ) {
                    //System.err.println("ehre");
                }
                String svalue;
                if ( n2.hasAttribute("value") ){
                    svalue= n2.getAttribute("value");
                } else {
                    svalue= n2.getTextContent();
                }
                Element evalue=null;

                String stype;
                if ( n2.hasAttribute("type") ) {
                    stype = n2.getAttribute("type");
                } else {
                    evalue= Util.singletonChildElement(n2);
                    stype= evalue.getTagName();
                }
                if (stype.equals("qdataset")) {
                    //The QStream has these as Strings, which need to be converted to
                    //if (pname.equals(QDataSet.DELTA_MINUS) || pname.equals(QDataSet.DELTA_PLUS) ) {
                    //    System.err.println("skipping DELTA_MINUS and DELTA_PLUS because bug");
                    //    continue;
                    //}
                    //props.put(pname, svalue);
                    if ( pname.equals(QDataSet.DEPEND_0) ) {
                        props.put( QDataSet.DEPENDNAME_0, svalue );
                    }

                } else {
                    SerializeDelegate delegate = SerializeRegistry.getByName(stype);
                    if (delegate == null) {
                        logger.log(Level.SEVERE, "!!! No delegate found for \"{0}\"", stype); // chris and I didn't see this invisible message
                        continue;
                    }
                    Object oval;
                    try {
                        if ( evalue!=null && delegate instanceof XMLSerializeDelegate ) {
                            oval= ((XMLSerializeDelegate)delegate).xmlParse(evalue);
                        } else {
                            oval= delegate.parse(stype, svalue);
                        }
                        props.put(pname, oval);
                    } catch (ParseException ex) {
                        logger.log(Level.SEVERE, ex.getMessage(), ex);
                    }
                }
             }
        } catch ( XPathExpressionException ex ) {
            logger.log( Level.SEVERE, ex.getMessage(), ex );
        }

        return props;
    }

    @Override
    public void streamDescriptor(StreamDescriptor sd) throws StreamException {
        sink.streamDescriptor(sd);
    }

    @Override
    public void packetDescriptor(PacketDescriptor pd) throws StreamException {
        sink.packetDescriptor(pd);

            Element ele= pd.getDomElement();

            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();

            XPathExpression exprv;
            XPathExpression exprp;

            try {
                NodeList odims = (NodeList) xpath.evaluate("/packet/qdataset", ele, XPathConstants.NODESET );
                List<PlaneDescriptor> pds= pd.getPlanes();

                for (int j = 0; j < odims.getLength(); j++) {
                    Element ds= (Element)odims.item(j);

                    exprp=  xpath.compile( "properties" );
                    //exprp=  xpath.compile( "properties" );
                    Node nprops= (Node) exprp.evaluate( ds, XPathConstants.NODE );

                    Map<String,Object> props= doProps( nprops, null );

                    String id= ds.getAttribute("id");

                    propsn.put( id, props );

                    exprv=  xpath.compile("values/@values");
                    Node values= (Node) exprv.evaluate( ds, XPathConstants.NODE );

                    if ( values!=null ) {
                        PlaneDescriptor planed= pds.get(j);
                        //exprv=  xpath.compile("values/@length");
                        String svalues= values.getTextContent();
                        String[] ss= svalues.split(",");
                        double[] dd= new double[ss.length];
                        for ( int i=0; i<ss.length; i++ ) {
                            dd[i]= Double.parseDouble(ss[i]);
                        }
                        MutablePropertyDataSet mds;
                        if ( planed.getQube().length==0 ) {
                            mds= DDataSet.wrap( dd );
                        } else {
                            mds= DDataSet.wrap( dd, planed.getQube() );
                        }

                        DataSetUtil.putProperties( props, mds );
                        sink.packetData( pd, planed, mds );

                    }
                }


            } catch (XPathExpressionException ex) {
                throw new RuntimeException(ex);
            }

    }

    @Override
    public void packet( PacketDescriptor pd, ByteBuffer data ) throws StreamException {
        //TODO: form QDataSet when the values are not in-line and only one packet exists.  Fire off a QDataSet packet.
        sink.packet(pd, data);

       //int j=0;

        for ( PlaneDescriptor planed : pd.getPlanes() ) {

            TransferType tt= planed.getType();
            // int pos= data.position();
            int len= pd.sizeBytes();

            double[] dd= new double[len];

            for ( int i=0; i<planed.getElements(); i++ ) {
                double bb= tt.read(data);
                dd[i]= bb;
            }

            MutablePropertyDataSet ds= DDataSet.wrap( dd, planed.getQube() );

           // try {
            //    XPathFactory factory = XPathFactory.newInstance();
            //    XPath xpath = factory.newXPath();

                //XPathExpression expr= xpath.compile("/packet/qdataset["+j+"]/properties");
                Map<String,Object> props= propsn.get( planed.getName() );
                if ( props!=null ) DataSetUtil.putProperties( props, ds );

                //j++;

            //} catch ( XPathExpressionException ex ) {
            //    logger.log( Level.SEVERE, "packet", ex );
            //}

            sink.packetData( pd, planed, ds );

            

        }

    }
    
    @Override
    public void streamClosed(StreamDescriptor sd) throws StreamException {
        sink.streamClosed(sd);
    }

    @Override
    public void streamException(StreamException se) throws StreamException {
        sink.streamException(se);
    }

    @Override
    public void streamComment(StreamComment se) throws StreamException {
        sink.streamComment(se);
    }

    public void setSink( QDataSetSink sink ) {
        this.sink= sink;
    }
    
//    public static void main( String[] args ) throws IOException, StreamException, Exception {
//        //File f = new File( "/home/jbf/data.nobackup/qds/waveformTable.qds" );
//        File f = new File( "/home/jbf/data.nobackup/qds/waveformTable2.qds" );
//
//        InputStream in = new FileInputStream(f);
//
//        QDataSetsFilter filter= new QDataSetsFilter();
//
//        filter.sink= new QDataSetsFilter.QDataSetSink() {
//
//            @Override
//            public void packetData(PacketDescriptor pd, PlaneDescriptor pld, QDataSet ds) {
//                System.err.println( "From "+pld.getName() + ": " + ds );
//            }
//
//        };
//
//        StreamTool.readStream( Channels.newChannel(in), filter );
//        //StreamTool.readStream( Channels.newChannel(in), handler ); // test without filter.
//
//    }

}
