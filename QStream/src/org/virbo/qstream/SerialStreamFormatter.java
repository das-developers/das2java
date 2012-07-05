/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.qstream;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.das2.datum.EnumerationUnits;
import org.das2.datum.TimeLocationUnits;
import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;
import org.virbo.dataset.DDataSet;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.QubeDataSetIterator;
import org.virbo.dataset.SemanticOps;
import org.virbo.dsops.Ops;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * We need a class that can format a stream serially.  SimpleStreamFormatter needs the whole dataset, which really
 * misses the point with QStreams, that you can process and create data streams serially on the server side.  The
 * das2stream codes were first coded with this use case, but QStreams were not and because of this a good serial
 * generator was never created.
 *
 * See the main method for an example of how this is used.
 *
 * Note this class is not thread-safe and assumes that only one thread will be working on the stream.  This may change.
 * 
 * This is not DONE!!!!
 * 
 * @author jbf
 */
public class SerialStreamFormatter {
    public static final int DEFAULT_TIME_DIGITS = 27;

    StreamDescriptor sd;

    Map<String,PlaneDescriptor> planes;
    Map<String,PacketDescriptor> pds;
    Map<String,TransferType> transferTypes;

    /**
     * keeps track of names assigned to each dataset.  Presently these are automatically assigned, but there
     * may also be a method for explicitly naming packets.
     */
    Map<QDataSet,String> names;

    WritableByteChannel channel;

    private static final char CHAR_NEWLINE = '\n';
    private static final Logger logger= Logger.getLogger("autoplot.qstream");

    protected boolean asciiTypes = true;

    public boolean isAsciiTypes() {
        return asciiTypes;
    }

    public void setAsciiTypes(boolean asciiTypes) {
        this.asciiTypes = asciiTypes;
    }

    protected boolean bigEndian = false;

    public boolean isBigEndian() {
        return bigEndian;
    }

    public void setBigEndian(boolean bigEndian) {
        this.bigEndian = bigEndian;
    }

    public SerialStreamFormatter() {
        names= new LinkedHashMap();
        transferTypes= new LinkedHashMap();
    }

    /**
     * send out the streamDescriptor, which contains just the name of the stream's default dataset.
     * @param name
     */
    protected StreamDescriptor doStreamDescriptor( String name ) {
        try {
            StreamDescriptor sd = new StreamDescriptor( DocumentBuilderFactory.newInstance() );
            Document document = sd.newDocument(sd);

            Element streamElement = document.createElement("stream");

            streamElement.setAttribute("dataset_id", name);
            if ( asciiTypes == false) {
                streamElement.setAttribute("byte_order", isBigEndian() ? "big_endian" : "little_endian");
            }

            sd.setDomElement(streamElement);
            sd.addDescriptor(sd); // allocate [00] for itself.

            return sd;
        } catch ( ParserConfigurationException ex ) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * the name of the default dataset.
     * @param name
     */
    public void init( String name, WritableByteChannel out ) throws IOException, StreamException {

        pds= new HashMap<String, PacketDescriptor>();

        sd= doStreamDescriptor(name);
        sd.send( sd, out );

        this.channel= out;

    }

    /**
     * return a name for the thing that describes ds1.  This will be used in
     * the descriptor, so if the descriptor doesn't contain the values, then
     * the name can be reused.  Note no names are reused at this point.
     * @param ds1
     * @return
     */
    private synchronized String nameFor(QDataSet ds1) {
        String name = names.get(ds1);
        boolean assignName= name==null;

        if (name == null) {
            name = (String) ds1.property(QDataSet.NAME);
        }
        if (name == null) {
            name = "ds_" + names.size();
        }

        if ( assignName ) {
            names.put( ds1, name );
        }

        return name;
    }

    private TransferType getTransferType( String name, QDataSet ds1 ) {
        TransferType tt= transferTypes.get(name);
        if ( tt==null ) {
            if ( asciiTypes ) {
                Units u= SemanticOps.getUnits(ds1);
                if ( UnitsUtil.isTimeLocation(u) ) {
                    tt= new AsciiTimeTransferType(  DEFAULT_TIME_DIGITS , u);
                } else {
                    tt= new AsciiTransferType( 10, true );
                }
            } else {
                tt= new DoubleTransferType();
            }
        }
        return tt;
    }

    /**
     * explicitly set the transfer type.
     * @param name
     * @param tt
     */
    public void setTransferType( String name, TransferType tt ) {
        if ( asciiTypes && !tt.isAscii() ) {
            throw new IllegalArgumentException("stream is declared as ascii stream, but non-ascii transfer type specified for "+name+": "+tt );
        }
        transferTypes.put( name, tt );
    }

    private Element doProperties(Document document, QDataSet ds) {
        Element properties = document.createElement("properties");

        Map<String, Object> props = DataSetUtil.getProperties(ds);
        Element prop;
        for (String name : props.keySet()) {
            prop= null;
            Object value = props.get(name);

            if (value instanceof QDataSet) {
                QDataSet qds = (QDataSet) value;

                prop = document.createElement("property");
                prop.setAttribute("name", name);
                if (qds.rank() == 0) {
                    SerializeDelegate r0d= (SerializeDelegate) SerializeRegistry.getByName("rank0dataset");
                    prop.setAttribute("type", "rank0dataset");
                    Units u= (Units) qds.property( QDataSet.UNITS );
                    if ( u!=null && u instanceof EnumerationUnits ) {
                        continue; // TODO: this should do something!
                    } else {
                        prop.setAttribute("value", r0d.format(value) );
                    }
                } else {
                    prop.setAttribute("type", "qdataset");
                    prop.setAttribute("value", nameFor((QDataSet) value));
                }

            } else {
                SerializeDelegate sd = SerializeRegistry.getDelegate(value.getClass());
                if (sd == null) {
                    System.err.println("dropping "+name+" because unsupported type: "+value.getClass());
                } else {
                    prop = document.createElement("property");
                    prop.setAttribute("name", name);
                    if ( sd instanceof XMLSerializeDelegate ) {
                        prop.appendChild( ((XMLSerializeDelegate)sd).xmlFormat(document,value) );
                    } else {
                        prop.setAttribute("type", sd.typeId(value.getClass()));
                        prop.setAttribute("value", sd.format(value));
                    }
                }
            }
            if ( prop!=null ) properties.appendChild(prop);
        }

        return properties;
    }

    private Element doValues( Document document, PacketDescriptor packetDescriptor, PlaneDescriptor planeDescriptor, QDataSet ds1 ) {
        Element values = document.createElement("values");
        int[] qubeDims=null;
        if ( !packetDescriptor.isValuesInDescriptor() ) {
            values.setAttribute("encoding", planeDescriptor.getType().name());
            qubeDims = DataSetUtil.qubeDims(ds1);
        }

        if (packetDescriptor.isStream()) {
            if (ds1.rank() > 0) {
                values.setAttribute("length", Util.encodeArray(qubeDims, 1, qubeDims.length - 1)); // must be a qube for stream.
            } else {
                values.setAttribute("length", "");
            }
        } else {
            if ( qubeDims!=null ) {
                values.setAttribute("length", Util.encodeArray(qubeDims, 0, qubeDims.length));
            }
            if (packetDescriptor.isValuesInDescriptor()) {
                StringBuilder s = new StringBuilder("");
                for (int i = 0; i < ds1.length(); i++) {
                    s.append( "," ).append( ds1.value(i) );
                }
                values.setAttribute("values", ds1.length() == 0 ? "" : s.substring(1));
                if ( ds1.length()==0 ) {
                    values.setAttribute("length", "0" );
                }
            }
        }
        return values;
    }

    private void doValuesElement(QDataSet ds, PacketDescriptor pd, PlaneDescriptor planeDescriptor, Document document, Element qdatasetElement) throws DOMException {

        Object sunits= ds.property(QDataSet.UNITS);
        if ( sunits!=null && !(sunits instanceof Units) ) {
            throw new IllegalArgumentException( "UNITS property doesn't contain type units, it's type "+sunits.getClass()+": "+sunits );
        }

        Units u = (Units) sunits;
        if ( u==null && SemanticOps.isRank1Bundle(ds) && ds.rank()==1 ) {
            QDataSet bundleDescriptor= (QDataSet) ds.property(QDataSet.BUNDLE_0);
            if ( bundleDescriptor==null ) {
                //TODO: check this schema, demoed by BundleBinsDemo.demo1();
            } else {
                // uh-oh.  just use a high-resolution format for now.
                for ( int i=0; i<bundleDescriptor.length(); i++ ) {
                    Units u1= (Units)bundleDescriptor.property( QDataSet.UNITS, i );
                    if ( u1!=null && UnitsUtil.isTimeLocation(u1) ) {
                        System.err.println("using high res kludge to format bundle dataset that contains "+u1 );
                    }
                }
            }
        }

        String name= nameFor(ds);
        TransferType tt= getTransferType( name, ds );

        if ( !pd.isValuesInDescriptor() ) {
            planeDescriptor.setType(tt);
        }

        if (pd.isValuesInDescriptor() && ds.rank() == 2) {
            for (int i = 0; i < ds.length(); i++) {
                Element values = doValues( document, pd, planeDescriptor, ds.slice(i) );
                values.setAttribute("index", String.valueOf(i));
                qdatasetElement.appendChild(values);
            }
        } else {
            Element values = doValues( document, pd, planeDescriptor, ds );
            qdatasetElement.appendChild(values);
        }
    }

    /**
     *
     * @param document
     * @param pd
     * @param ds
     * @param slice
     * @return
     */
    private PlaneDescriptor doPlaneDescriptor( Document document, PacketDescriptor pd,
            QDataSet ds, boolean slice ) {
        Element qdatasetElement = document.createElement("qdataset");
        qdatasetElement.setAttribute("id", nameFor(ds));

        logger.log( Level.FINE, "writing qdataset {0}", nameFor(ds));

        qdatasetElement.setAttribute("rank", String.valueOf( ds.rank() + ( slice ? 1 : 0 ) ) );

        if ( SemanticOps.isRank1Bundle(ds) ) {
            QDataSet bds;
            bds= (QDataSet) ds.property("BUNDLE_0");

            // if we haven't already serialized a bundleDescriptor, then do legacy.
            if ( bds==null ) {
                for ( int i=0; i<ds.length(); i++ ) {
                    QDataSet ds1= DataSetOps.unbundle( ds, i );
                    Element props = doProperties(document, ds1);
                    props.setAttribute("index", String.valueOf(i) );
                    qdatasetElement.appendChild(props);
                }
            }
        }

        Element props = doProperties(document, ds);
        qdatasetElement.appendChild(props);

        PlaneDescriptor planeDescriptor = new PlaneDescriptor();

        planeDescriptor.setRank(ds.rank());

        if ( true ) {
            int[] qube = DataSetUtil.qubeDims(ds);
            if ( !pd.valuesInDescriptor ) {
                if (pd.isStream()) {
                    planeDescriptor.setQube(qube);
                } else {
                    planeDescriptor.setQube(qube);
                }
            } else {
                if ( ds.length()==0 ) {
                    System.err.println("here");
                }
            }
        }
        planeDescriptor.setDs(ds);
        planeDescriptor.setName(nameFor(ds));

        doValuesElement( ds, pd, planeDescriptor, document, qdatasetElement );

        planeDescriptor.setDomElement(qdatasetElement);

        return planeDescriptor;
    }


    /**
     * create the packetDescriptor object.
     * @param sd the handle for the stream
     * @param ds a slice of dataset when stream is set, or the dataset that will be serialized to this packet descriptor.
     * @param stream the data will be streamed in packets.
     * @param valuesInDescriptor the data will be contained in the packet.
     * @param streamRank the rank of the stream.
     * @return
     * @throws ParserConfigurationException
     */
    private PacketDescriptor doPacketDescriptor( StreamDescriptor sd,
            QDataSet ds, boolean stream,
            boolean valuesInDescriptor,
            int streamRank, String joinId) {

        try {
            if ( !valuesInDescriptor && DataSetUtil.isQube(ds) == false ) {
                throw new IllegalArgumentException("must be qube!");
            }

            PacketDescriptor packetDescriptor = new PacketDescriptor();
            packetDescriptor.setStream(stream);
            packetDescriptor.setStreamRank(streamRank);
            if (valuesInDescriptor) {
                packetDescriptor.setValuesInDescriptor(true);
            }

            Document document = sd.newDocument(packetDescriptor);

            Element packetElement = document.createElement("packet");

            if ( stream ) {  // ds.rank()==streamRank-1
                QDataSet dep0 = (QDataSet) ds.property( QDataSet.CONTEXT_0 );
                if (dep0 != null) {
                    PlaneDescriptor planeDescriptor = doPlaneDescriptor(document, packetDescriptor, dep0, stream );
                    packetDescriptor.addPlane(planeDescriptor);
                    packetElement.appendChild(planeDescriptor.getDomElement());
                }
            }

            for (int i = 0; i < QDataSet.MAX_PLANE_COUNT; i++) {
                QDataSet plane0 = (QDataSet) ds.property("PLANE_" + i);
                if (plane0 != null) {
                    PlaneDescriptor planeDescriptor = doPlaneDescriptor(document, packetDescriptor, plane0, stream );
                    packetDescriptor.addPlane(planeDescriptor);
                    packetElement.appendChild(planeDescriptor.getDomElement());
                } else {
                    break;
                }
            }

            if ( SemanticOps.isBundle(ds) ) {
                for ( int i=0; i<ds.length(0); i++ ) {
                    QDataSet ds1= DataSetOps.unbundle(ds,i);
                    PlaneDescriptor planeDescriptor = doPlaneDescriptor(document, packetDescriptor, ds1, stream );
                    packetDescriptor.addPlane(planeDescriptor);
                    Element dselement= planeDescriptor.getDomElement();
                    if (joinId!=null ) dselement.setAttribute("joinId", joinId );
                    packetElement.appendChild(dselement);
                }

            } else {
                PlaneDescriptor planeDescriptor = doPlaneDescriptor(document, packetDescriptor, ds, stream );
                packetDescriptor.addPlane(planeDescriptor);
                Element dselement= planeDescriptor.getDomElement();
                if (joinId!=null ) dselement.setAttribute("joinId", joinId );
                packetElement.appendChild(dselement);
            }

            packetDescriptor.setDomElement(packetElement);

            return packetDescriptor;
        } catch ( ParserConfigurationException ex ) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * format the dataset, maybe sending a descriptor if it hasn't been sent already.
     * ds1 may be null in this function, in which case nothing needs to be done.
     * @param name the name for the dataset of which ds1 is all or a slice of, or null.
     * @param ds1
     * @param inline
     * @throws IOException
     * @throws StreamException
     */
    public void maybeFormat( String name, QDataSet ds1, boolean inline ) throws IOException, StreamException  {
        if ( ds1!=null ) {
            format( name, ds1, inline );
        }
    }

    /**
     * format the dataset, maybe sending a descriptor if it hasn't been sent already.
     * @param name the name for the dataset of which ds1 is all or a slice of, or null.
     * @param ds1
     * @param inline if true, then the entire dataset will be present.
     * @throws IOException
     * @throws StreamException
     */
    public void format( String name, QDataSet ds1, boolean inline ) throws IOException, StreamException  {

        if ( name==null ) {
            if ( inline ) {
                name= nameFor(ds1);
            } else {
                throw new IllegalArgumentException("anonymous dataset must be inline");
            }
        }

        //get the packetDescriptor being used to describe these slices.
        PacketDescriptor pd= pds.get(name);

        if ( pd==null ) {
            if ( inline ) {
                pd= doPacketDescriptor(sd, ds1, false, true, ds1.rank(), null );
            } else {
                pd= doPacketDescriptor(sd, ds1, true, false, ds1.rank()+1, null );
            }
            pds.put( name, pd );
            sd.addDescriptor(pd);
            sd.send(pd, channel);

            if ( inline ) return;
        }

        int bufferSize = 4 + pd.sizeBytes(); // :01:

        byte[] bbuf = new byte[bufferSize];
        ByteBuffer buffer = ByteBuffer.wrap(bbuf); //ByteBuffer.allocateDirect(bufferSize);
        if (isBigEndian()) {
            buffer.order(ByteOrder.BIG_ENDIAN);
        } else {
            buffer.order(ByteOrder.LITTLE_ENDIAN);
        }

        String packetTag = String.format(":%02d:", sd.descriptorId(pd));
        buffer.put(packetTag.getBytes());  // we only need to write it once.

        if (pd.isStream()) {

            int planeCount = pd.planes.size();

            if ( planeCount>2 ) throw new IllegalArgumentException("more than two planes found!");

            for (int iplane = 0; iplane < planeCount; iplane++) {

                PlaneDescriptor plane = pd.planes.get(iplane);
                TransferType tt = plane.getType();
                QDataSet planeds;
                if ( iplane==planeCount-1 ) {
                    planeds= ds1;
                } else {
                    planeds= (QDataSet) ds1.property(QDataSet.CONTEXT_0);
                }

                boolean lastPlane = iplane == planeCount - 1;

                if (planeds.rank() == 0) {                       // format just the one number
                    tt.write(planeds.value(), buffer);
                } else if (planeds.rank() == 1) {                // format the 1-D array of numbers
                    for (int j = 0; j < planeds.length(); j++) {
                        tt.write(planeds.value(j), buffer);
                    }
                } else {                                         // format the flattened n-D array
                    QDataSet slice = planeds;
                    QubeDataSetIterator it = new QubeDataSetIterator(slice);
                    while (it.hasNext()) {
                        it.next();
                        tt.write(it.getValue(slice), buffer);
                    }
                }
                if (lastPlane && tt.isAscii() && Character.isWhitespace(buffer.get(bufferSize - 1))) {
                    buffer.put(bufferSize - 1, (byte) CHAR_NEWLINE);
                }
            }

            buffer.flip();
            channel.write(buffer);
            buffer.position(4);

            buffer.flip();
        } else {
            int planeCount = pd.planes.size();

            for (int iplane = 0; iplane < planeCount; iplane++) {
                PlaneDescriptor plane = pd.planes.get(iplane);
                TransferType tt = plane.getType();
                QDataSet planeds = plane.getDs();

                QubeDataSetIterator it = new QubeDataSetIterator(planeds);
                while (it.hasNext()) {
                    it.next();
                    tt.write(it.getValue(planeds), buffer);
                }
                boolean lastPlane = iplane == planeCount - 1;
                if (lastPlane && tt.isAscii() && Character.isWhitespace(buffer.get(bufferSize - 1))) {
                    buffer.put(bufferSize - 1, (byte) CHAR_NEWLINE);
                }

            }

            buffer.flip();
            channel.write(buffer);
            buffer.flip();
        }

        List<String> probs= new ArrayList();
        if ( !DataSetUtil.validate(ds1, probs ) ) {
             throw new IllegalArgumentException("DataSet is not valid: "+probs.get(0));
        }

    }

    /**
     * allow the stream to recycle the name.  new packets with this name will issue a new packetDescriptor.
     *
     * @param name
     */
    public void retire( String name ) {
        planes.remove(name);
        PacketDescriptor pd= pds.get(name);
        sd.retireDescriptor(pd);
    }

    public static void main( String[] args ) throws FileNotFoundException, IOException, StreamException {

        boolean join;
        //QDataSet ds= Ops.ripplesTimeSeries(24);
        //QDataSet ds= Ops.ripplesVectorTimeSeries(24);
        //QDataSet ds= Ops.ripplesSpectrogramTimeSeries(24);
        QDataSet ds= Ops.ripplesJoinSpectrogramTimeSeries(24);
        join= SemanticOps.isJoin(ds);

        SerialStreamFormatter form= new SerialStreamFormatter();

        String name= "vector";
        String dep0name= "time";

        //File f = new File( name + ".qds" );
        //FileOutputStream out = new FileOutputStream(f);


        // configure the tool
        form.setAsciiTypes(true);
        form.setTransferType( name, new AsciiTransferType(12,true) );
        form.setTransferType( dep0name, new AsciiTimeTransferType(18,Units.us2000 ) );

        form.init( name, Channels.newChannel( System.out ) ); //WritableByteChannelSystem.out(WritableByteChannel)out.getChannel() );
        form.maybeFormat( null, (QDataSet) ds.property(QDataSet.DEPEND_1), true );

        if ( join ) {
            for ( int j=0; j<ds.length(); j++ ) {
                QDataSet jds1= ds.slice(j);
                for ( int i=0; i<jds1.length(); i++ ) {
                    form.format( name, DDataSet.copy( jds1.slice(i) ), false ); //TODO: remove DDataSet.copy used for debugging.
                }            
            }
        } else {
            for ( int i=0; i<ds.length(); i++ ) {
                form.format( name, DDataSet.copy( ds.slice(i) ), false ); //TODO: remove DDataSet.copy used for debugging.
            }
        }

    }

}
