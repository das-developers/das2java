/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.qstream.filter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;
import org.virbo.dataset.SemanticOps;
import org.virbo.qstream.AsciiTimeTransferType;
import org.virbo.qstream.AsciiTransferType;
import org.virbo.qstream.FormatStreamHandler;
import org.virbo.qstream.PacketDescriptor;
import org.virbo.qstream.PlaneDescriptor;
import org.virbo.qstream.StreamComment;
import org.virbo.qstream.StreamDescriptor;
import org.virbo.qstream.StreamException;
import org.virbo.qstream.StreamHandler;
import org.virbo.qstream.StreamTool;
import org.virbo.qstream.TransferType;
import org.w3c.dom.Element;

/**
 * Converts all the transfer types to Ascii TransferTypes.
 * @author jbf
 */
public class ToAsciiStreamHandler implements StreamHandler {

    private static final Logger logger= Logger.getLogger("qstream");

    public ToAsciiStreamHandler( OutputStream out ) {
        format= new FormatStreamHandler();
        format.setOutputStream( out );
        pdouts= new LinkedHashMap<Integer, PacketDescriptor>();
        newEncodings= new LinkedHashMap<String,TransferType>();
        newEncodings.put( "double",new AsciiTransferType(20,false) );
        newEncodings.put( "float",new AsciiTransferType(10,false) );
        newEncodings.put( "int8",new AsciiTransferType(20,false) );
        newEncodings.put( "int4",new AsciiTransferType(10,false) );
        newEncodings.put( "int2",new AsciiTransferType(7,false) ); // 7 is the shortest supported.
    }
    
    FormatStreamHandler format;
    
    int newPacketSize;
    StreamDescriptor sdout;
    Map<Integer,PacketDescriptor> pdouts;
    Map<String,TransferType> newEncodings;
    
    @Override
    public void streamDescriptor(StreamDescriptor sd) throws StreamException {
        sdout= new StreamDescriptor(DocumentBuilderFactory.newInstance());
        format.streamDescriptor(sd);
    }

    @Override
    public void packetDescriptor(PacketDescriptor pd) throws StreamException {
        newPacketSize= 0;
        try {
            PacketDescriptor pdout= (PacketDescriptor) pd.clone();

            Element ele= pd.getDomElement();
            Element eleOut= (Element) ele; // .cloneNode(true);

            for ( PlaneDescriptor p: pdout.getPlanes() ) {
                try { // Just recycle the document and hack away.
                    boolean isTime;
                    Units u;
                    XPath xpath= XPathFactory.newInstance().newXPath();
                    XPathExpression expr;
                    expr= xpath.compile("string(/packet/qdataset[@id='"+p.getName()+"']/properties/property[@name='UNITS']/@value)");
                    Object o1= expr.evaluate(eleOut,XPathConstants.STRING);
                    if ( o1==null ) {
                        isTime= false;
                        u= Units.dimensionless;  // we're not going to use this.
                    } else {
                        String sunit= String.valueOf(o1);
                        u= SemanticOps.lookupUnits(sunit);
                        isTime= UnitsUtil.isTimeLocation(u);
                    }
                    
                    TransferType newTT;
                    if ( p.getType().name().startsWith("ascii") ) {
                        newTT= p.getType(); // just leave it alone.
                    } else {
                        if ( isTime ) {
                            newTT= new AsciiTimeTransferType(27,u);
                        } else {
                            newTT= newEncodings.get( p.getType().name() );
                        }
                    }
                    
                    p.setType( newTT );
                    newPacketSize+= newTT.sizeBytes() * p.getElements();
                    
                    expr = xpath.compile("/packet/qdataset[@id='"+p.getName()+"']/values");
                    Object o = expr.evaluate(eleOut, XPathConstants.NODE);
                    if (o==null) throw new IllegalArgumentException("unable to find node named "+p.getName());
                    Element node = (Element) o;
                    if ( node.hasAttribute("encoding") ) {
                        node.setAttribute("encoding",newTT.name());
                    }
                } catch ( XPathExpressionException ex ) {
                    logger.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
            sdout.addDescriptor(pdout);
            
            pdout.setDomElement( eleOut );
            
            format.packetDescriptor(pdout);
            this.pdouts.put( pd.getPacketId(), pdout);
        } catch (CloneNotSupportedException ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    @Override
    public void packet(PacketDescriptor pd, ByteBuffer data) throws StreamException {
        ByteBuffer dataOut= ByteBuffer.allocate(newPacketSize);
        data.flip();
        PacketDescriptor pdout= pdouts.get(pd.getPacketId());
        int np= pd.getPlanes().size();
        for ( int ip=0; ip<np; ip++ ) {
            PlaneDescriptor pout= pdout.getPlanes().get(ip);
            PlaneDescriptor pin= pd.getPlanes().get(ip);
            data.limit( data.limit() + pin.getElements() * pin.getType().sizeBytes() );
            for ( int i=0; i<pin.getElements(); i++ ) {
                double d= pin.getType().read(data);
                if ( ip==np-1 && i==pin.getElements()-1 ) {
                    pout.getType().write( d, dataOut );
                    if ( Character.isWhitespace( dataOut.get(dataOut.position()-1) ) ) dataOut.put( dataOut.position()-1, (byte)'\n' );
                } else {
                    pout.getType().write( d, dataOut );
                }
            }            
        }
        dataOut.flip();
        format.packet(pdout,dataOut);
    }

    @Override
    public void streamClosed(StreamDescriptor sd) throws StreamException {
        format.streamClosed(sd);
    }

    @Override
    public void streamException(StreamException se) throws StreamException {
        format.streamException(se);
    }

    @Override
    public void streamComment(StreamComment sd) throws StreamException {
        format.streamComment(sd);
    }
    
    public static void main( String[] args ) throws FileNotFoundException, StreamException {
        
        if ( args.length==1 && args[0].trim().equals("--help" ) ) {
            System.err.println("java -jar autoplot.jar org.qstream.filter.ToAsciiStreamHandler [urlin] [fileout]");
            System.exit(-1);
        }
        
        InputStream in= System.in;
        OutputStream out= System.out;

        if ( args.length>0 ) {
            in= new FileInputStream(args[0]);
            System.err.println("reading "+args[0] );
        }
        
        if ( args.length>1 ) {
            out= new FileOutputStream(args[1]);
            System.err.println("writing "+args[1] );
        }

        StreamHandler sink= new ToAsciiStreamHandler(out);
        StreamTool.readStream( Channels.newChannel(in), sink );
                        
        if ( in!=System.in ) {
            try {
                in.close();
            } catch ( IOException ex ) {
                logger.log(Level.SEVERE, ex.getMessage(), ex);
                System.exit(-2);
            }
        }
        if ( out!=System.out )  {
            try {
                out.close();
            } catch ( IOException ex ) {
                logger.log(Level.SEVERE, ex.getMessage(), ex);
                System.exit(-3);                
            }
        }

    }    
}
