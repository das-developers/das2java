
package org.das2.qstream.filter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.text.ParseException;
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
import org.das2.datum.DatumRange;
import org.das2.datum.DatumRangeUtil;
import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;
import org.das2.qstream.AsciiTimeTransferType;
import org.das2.qstream.AsciiTransferType;
import org.das2.qstream.FormatStreamHandler;
import org.das2.qstream.PacketDescriptor;
import org.das2.qstream.PlaneDescriptor;
import org.das2.qstream.StreamComment;
import org.das2.qstream.StreamDescriptor;
import org.das2.qstream.StreamException;
import org.das2.qstream.StreamHandler;
import org.das2.qstream.StreamTool;
import org.das2.qstream.TransferType;
import org.w3c.dom.Element;

/**
 * Converts all the transfer types to Ascii TransferTypes, but also trims the
 * data.
 * @author jbf
 */
public class TrimStreamHandler implements StreamHandler {

    private static final Logger logger= Logger.getLogger("qstream");

    public TrimStreamHandler( OutputStream out, DatumRange dr ) {
        format= new FormatStreamHandler();
        format.setOutputStream( out );
        pdouts= new LinkedHashMap<>();
        newEncodings= new LinkedHashMap<>();
        trim= dr;
        newEncodings.put( "double",new AsciiTransferType(20,true) );
        newEncodings.put( "float",new AsciiTransferType(10,true) );
        newEncodings.put( "int8",new AsciiTransferType(20,false) );
        newEncodings.put( "int4",new AsciiTransferType(10,false) );
        newEncodings.put( "int2",new AsciiTransferType(7,false) ); // 7 is the shortest supported.
    }
    
    FormatStreamHandler format;
    
    DatumRange trim;
    
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
                        u= Units.lookupUnits(sunit);
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
        
        int newPacketSizeLocal=0;
        int inPacketSizeLocal=0;
        PacketDescriptor pdout= pdouts.get(pd.getPacketId());
        int np1= pdout.getPlanes().size();
        
        int hasTime= -1;
        for ( int ip=0; ip<np1; ip++ ) {
            PlaneDescriptor p= pdout.getPlanes().get(ip);
            if ( UnitsUtil.isTimeLocation( p.getUnits() ) ) {
                hasTime= ip;
            }
            newPacketSizeLocal+= p.getType().sizeBytes() * p.getElements();
            PlaneDescriptor plin= pd.getPlanes().get(ip);
            inPacketSizeLocal+= plin.getType().sizeBytes() * plin.getElements();
            //System.err.println("plane "+ip+ ": "+ p.getType().sizeBytes() +"*"+ p.getElements() + "=" + ( p.getType().sizeBytes() * p.getElements() ) );
        }
        
//        if ( newPacketSizeLocal!=newPacketSize ) {
//            System.err.println("wow, it does happen...");
//        }
                 
        if ( hasTime==0 ) {
            PlaneDescriptor pin= pd.getPlanes().get(hasTime);
            double d= pin.getType().read(data);
            if ( !trim.contains( pd.getPlanes().get(hasTime).getUnits().createDatum(d) ) ) {
                return;
            }
            data.position(0);
        }

        ByteBuffer dataOut= ByteBuffer.allocate(newPacketSizeLocal);
        data.flip();
        pdout= pdouts.get(pd.getPacketId());
        int np= pd.getPlanes().size();
        for ( int ip=0; ip<np; ip++ ) {
            PlaneDescriptor pout= pdout.getPlanes().get(ip);
            PlaneDescriptor pin= pd.getPlanes().get(ip);
            data.limit( data.limit() + pin.getElements() * pin.getType().sizeBytes() );
            //System.err.println(" -> "+ data.position() );
            for ( int i=0; i<pin.getElements(); i++ ) {
                double d= pin.getType().read(data);
                if ( ip==np-1 && i==pin.getElements()-1 ) {
                    pout.getType().write( d, dataOut );
                    if ( Character.isWhitespace( dataOut.get(dataOut.position()-1) ) ) dataOut.put( dataOut.position()-1, (byte)'\n' );
                } else {
                    pout.getType().write( d, dataOut );
                }
                //System.err.println("  -> "+ data.position() + " of " + data.limit() + "  into  "+ dataOut.position() + " of " + dataOut.limit() );
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
            System.err.println("java -jar autoplot.jar org.qstream.filter.ToAsciiStreamHandler <timeRange> [urlin] [fileout]");
            System.exit(-1);
        }
        
        InputStream in= System.in;
        OutputStream out= System.out;
        
        DatumRange trim;
                
        try {
            trim= DatumRangeUtil.parseTimeRange(args[0]);
        } catch (ParseException ex) {
            System.err.println("unable to parse as time range: "+args[0]);
            System.exit(-1);
            return;
        }
        
        if ( args.length>1 ) {
            in= new FileInputStream(args[1]);
            System.err.println("reading "+args[1] );
        }
        
        if ( args.length>2 ) {
            out= new FileOutputStream(args[2]);
            System.err.println("writing "+args[2] );
        }

        StreamHandler sink= new TrimStreamHandler(out,trim);
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
