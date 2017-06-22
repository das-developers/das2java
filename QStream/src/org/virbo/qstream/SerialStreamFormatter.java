/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.qstream;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.das2.datum.EnumerationUnits;
import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;
import org.das2.qds.DataSetOps;
import org.das2.qds.DataSetUtil;
import org.das2.qds.QDataSet;
import org.das2.qds.QubeDataSetIterator;
import org.das2.qds.SemanticOps;
import org.das2.qds.ops.Ops;
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
 * @author jbf
 */
public class SerialStreamFormatter {
    
    public static final int DEFAULT_TIME_DIGITS = 27;
    
    public static final String INOUTFORM_INLINE = "inline";         // put the values in the descriptor
    public static final String INOUTFORM_ONE_RECORD = "oneRecord";  // put the values in one record
    public static final String INOUTFORM_STREAMING = "streaming";   // put the values in records, one slice at a time.
    
    StreamDescriptor sd;

    StreamHandler sh;

    //Map<String,PlaneDescriptor> planes;
    Map<String,PacketDescriptor> pds;
    Map<String,TransferType> transferTypes;
    Map<Units,TransferType> unitsTransferTypes;

    /**
     * keeps track of names assigned to each dataset.  Presently these are automatically assigned, but there
     * may also be a method for explicitly naming packets.
     */
    Map<QDataSet,String> names;
    Map<String,String> namesRev;


    private static final char CHAR_NEWLINE = '\n';
    private static final Logger logger= Logger.getLogger("qstream");

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
        namesRev= new LinkedHashMap();
        transferTypes= new LinkedHashMap();
        unitsTransferTypes= new LinkedHashMap();
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

        FormatStreamHandler sh= new FormatStreamHandler();
        sh.setWritableByteChannel(out);

        init( name, sh );
        
    }

    /**
     * initialize, sending data directly via the StreamHandler interface.  This avoids parsing and formatting the XML.
     * @param name the name of the default dataset.
     */
    public void init( String name, StreamHandler sh ) throws IOException, StreamException {

        pds= new HashMap<String, PacketDescriptor>();

        this.sd= doStreamDescriptor(name);

        this.sh= sh;

        sh.streamDescriptor(sd);

    }
    /**
     * return a name for the thing that describes ds1.  This will be used in
     * the descriptor, so if the descriptor doesn't contain the values, then
     * the name can be reused.  Note no names are reused at this point.
     * @param ds1
     * @return
     */
    private synchronized String newNameFor(QDataSet ds1) {
        String name = names.get(ds1);

        if (name == null) {
            name = (String) ds1.property(QDataSet.NAME);
        }

        if (name == null) {
            name = "ds_" + names.size();
        }

        if ( namesRev.containsKey(name) ) {
            String name0= name;
            int i=1;
            name= name0 + String.valueOf(i);
            while (  namesRev.containsKey(name) ) {
                i=i+1;
                name = name0 + String.valueOf(i);
            }
        }

        names.put( ds1, name );
        namesRev.put( name, ds1.toString() );

        return name;
    }

    /**
     * return a name for the thing that describes ds1.  This will be used in
     * the descriptor, so if the descriptor doesn't contain the values, then
     * the name can be reused.  Note no names are reused at this point.
     * @param ds1
     * @return
     */
    private synchronized String nameFor(QDataSet ds1) {
        System.err.println(""+ ds1 + " " + ds1.hashCode() );
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
            namesRev.put( name,ds1.toString() );
        }

        return name;
    }

    /**
     * return the transfer type for this unit.
     * @param ds
     * @return
     */
    private TransferType getUnitTransferType( QDataSet ds ) {
        Units u= SemanticOps.getUnits(ds);
        return unitsTransferTypes.get(u);
    }

    private TransferType getTransferType( String name, QDataSet ds1 ) {
        TransferType tt= transferTypes.get(name);
        if ( tt==null ) {
            tt= getUnitTransferType(ds1);
            if ( tt!=null ) {
                return tt;
            }
            if ( asciiTypes ) {
                Units u= SemanticOps.getUnits(ds1);
                if ( UnitsUtil.isTimeLocation(u) ) {
                    tt= new AsciiTimeTransferType(  DEFAULT_TIME_DIGITS , u);
                } else {
                    tt= new AsciiTransferType( 10, true );
                }
            } else {
                if ( ds1.length()>125000 ) {
                    logger.fine("using floats because we'll certainly run out of room otherwise");
                    tt= new FloatTransferType();
                } else {
                    tt= new DoubleTransferType();
                }
            }
        }
        return tt;
    }

    /**
     * explicitly set the transfer type used to transfer data that is convertible to this
     * unit.
     * @param name
     * @param tt
     */
    public void setUnitTransferType( Units u, TransferType tt ) {
        if ( asciiTypes && !tt.isAscii() ) {
            throw new IllegalArgumentException("stream is declared as ascii stream, but non-ascii transfer type specified for times: "+tt );
        }
        unitsTransferTypes.put( u, tt );
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

    private Element doProperties(Document document, Map<String, Object> props, boolean slice) {
        Element properties = document.createElement("properties");

        Element prop;
        for ( Entry<String,Object> e: props.entrySet() ) {
            String name= e.getKey();
            prop= null;

            Object value = e.getValue();
            String name1= name; // name of the unsliced dataset
            boolean allowRank0 = true;
            if ( slice ) {
                if ( name.startsWith("DEPEND_") ) {
                    name1= "DEPEND_" + ( 1 + Integer.parseInt( name.substring(7) ) );
                } else if ( name.equals("CONTEXT_0") ) {
                    name1= "DEPEND_0";
                    allowRank0= false;
                } else if ( name.startsWith("BINS_") ) {
                    name1= "BINS_" + ( 1 + Integer.parseInt( name.substring(5) ) );
                } else if ( name.startsWith("BUNDLE_") ) {
                    name1= "BUNDLE_" + ( 1 + Integer.parseInt( name.substring(7) ) );
                }
            }
            
            
            if ( value==null ) {
                continue; // slice
            }

            if (value instanceof QDataSet) {
                QDataSet qds = (QDataSet) value;

                prop = document.createElement("property");
                prop.setAttribute("name", name1);
                if ( qds.rank() == 0 && allowRank0 ) {
                    SerializeDelegate r0d= (SerializeDelegate) SerializeRegistry.getByName("rank0dataset");
                    prop.setAttribute("type", "rank0dataset");
                    Units u= (Units) qds.property( QDataSet.UNITS );
                    if ( u!=null && u instanceof EnumerationUnits ) {
                        continue; // TODO: this should do something!
                    } else {
                        prop.setAttribute("value", r0d.format(value) );
                    }
                } else {
                    if ( !names.containsKey((QDataSet)value) ) {
                        System.err.println("Unidentified "+name1 +"!!");
                    }
                    prop.setAttribute("type", "qdataset");
                    prop.setAttribute("value", nameFor((QDataSet) value));
                }

            } else {
                SerializeDelegate sd = SerializeRegistry.getDelegate(value.getClass());
                if (sd == null) {
                    System.err.println("dropping "+name1+" because unsupported type: "+value.getClass());
                } else {
                    prop = document.createElement("property");
                    prop.setAttribute("name", name1);
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
                values.setAttribute("length", Util.encodeArray(qubeDims, 0, qubeDims.length) ); // must be a qube for stream.
            } else {
                values.setAttribute("length", "");
            }
        } else {
            if ( qubeDims!=null ) {
                values.setAttribute("length", Util.encodeArray(qubeDims, 0, qubeDims.length) );
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

        if (pd.isValuesInDescriptor() && ds.rank() == 2) { //TODO: check this
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
     * @param slice the dataset is a slice of the thing we need to describe.
     * @return
     */
    private PlaneDescriptor doPlaneDescriptor( Document document, PacketDescriptor pd,
            QDataSet ds, boolean slice ) {
        Element qdatasetElement = document.createElement("qdataset");
        qdatasetElement.setAttribute("id", newNameFor(ds));

        logger.log( Level.FINE, "writing qdataset {0}", nameFor(ds));

        qdatasetElement.setAttribute("rank", String.valueOf( ds.rank() + ( slice ? 1 : 0 ) ) );

        if ( SemanticOps.isRank1Bundle(ds) ) {
            QDataSet bds;
            bds= (QDataSet) ds.property("BUNDLE_0");

            // if we haven't already serialized a bundleDescriptor, then do legacy.
            if ( bds==null ) {
                for ( int i=0; i<ds.length(); i++ ) {
                    QDataSet ds1= DataSetOps.unbundle( ds, i );
                    Element props = doProperties(document, DataSetUtil.getProperties(ds1), slice);
                    props.setAttribute("index", String.valueOf(i) );
                    qdatasetElement.appendChild(props);
                }
            }
        }

        Element props = doProperties(document, DataSetUtil.getProperties(ds), slice);
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
                    logger.severe("here ds.length()==0");
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
    public void maybeFormat( String name, QDataSet ds1, String inoutForm ) throws IOException, StreamException  {
        if ( ds1!=null ) {
            format( name, ds1, inoutForm );
        }
    }

    /**
     * format the dataset, maybe sending a descriptor if it hasn't been sent already.
     * @param joinName the name to which we join the dataset.
     * @param name the name for the dataset of which ds1 is all or a slice of, or null.
     * @param ds1
     * @param inoutForm one of "inline", "oneRecord", or "streaming"
     * @throws IOException
     * @throws StreamException
     */
    public void format( String joinName, String name, QDataSet ds1, String inoutForm ) throws IOException, StreamException  {

        if ( name==null || name.trim().length()==0 ) {
            if ( inoutForm.equals(INOUTFORM_INLINE) ) {
                name= nameFor(ds1);
            } else {
                throw new IllegalArgumentException("anonymous dataset must be inline");
            }
        }

        //get the packetDescriptor being used to describe these slices.
        PacketDescriptor pd= pds.get(name);

        if ( pd==null || inoutForm.equals(INOUTFORM_INLINE) ) {
            if ( inoutForm.equals(INOUTFORM_INLINE) ) {
                pd= doPacketDescriptor(sd, ds1, false, true, ds1.rank(), joinName );
            } else if ( inoutForm.equals("oneRecord") ) {
                pd= doPacketDescriptor(sd, ds1, false, false, ds1.rank(), joinName );
            } else {
                pd= doPacketDescriptor(sd, ds1, true, false, ds1.rank()+1, joinName );
            }

            pds.put( name, pd );
            
            sh.packetDescriptor(pd);

            if ( inoutForm.equals(INOUTFORM_INLINE) ) return;
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
            buffer.position(4);
            sh.packet( pd, buffer.slice() ); 
            
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
            buffer.position(4);
            sh.packet( pd, buffer.slice() );
            buffer.flip();
        }

        List<String> probs= new ArrayList();
        if ( !DataSetUtil.validate(ds1, probs ) ) {
             throw new IllegalArgumentException("DataSet is not valid: "+probs.get(0));
        }

    }

    /**
     * format the dataset which is part of a join dataset.
     * @param name the name for the dataset of which ds1 is all or a slice of, or null.
     * @param ds1
     * @param inline if true, then the entire dataset will be present.
     * @throws IOException
     * @throws StreamException
     */
    public void format( String name, QDataSet ds1, String inoutForm ) throws IOException, StreamException  {
        format( null, name, ds1, inoutForm );
    }


    public void join( String name, int rank, Map<String,Object> props ) throws StreamException, IOException {

        PacketDescriptor packetDescriptor = new PacketDescriptor();
        packetDescriptor.setStream(true);
        packetDescriptor.setStreamRank(rank);

        try {
           Document document = sd.newDocument(packetDescriptor);

           Element packetElement = document.createElement("packet");

           Element qdataset= document.createElement("qdataset");
           packetElement.appendChild( qdataset );

           qdataset.setAttribute( "id", name );

           qdataset.setAttribute("rank", String.valueOf(rank) );

           Element propsElement= doProperties( document, props, false );

           qdataset.appendChild(propsElement);

           Element valuesElement= document.createElement("values");
           valuesElement.setAttribute( "join", "ignore" ); // this property value is ignored, but must have length>0

           qdataset.appendChild(valuesElement);

           packetDescriptor.setDomElement(packetElement);

           sh.packetDescriptor( packetDescriptor );

        } catch ( ParserConfigurationException ex ) {
            throw new RuntimeException(ex);
        }
            //TODO: finish me!

    }

    /**
     * allow the stream to recycle the name.  new packets with this name will issue a new packetDescriptor.
     *
     * @param name
     */
    public void retire( String name ) {
        //planes.remove(name);
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

        String name= "Flux";

        //File f = new File( name + ".qds" );
        //FileOutputStream out = new FileOutputStream(f);


        // configure the tool
        form.setAsciiTypes(true);
        form.setTransferType( name, new AsciiTransferType(10,false) );
        form.setUnitTransferType( Units.us2000, new AsciiTimeTransferType(17,Units.us2000 ) );
        
        //FormatStreamHandler sh= new FormatStreamHandler();
        //sh.setOutputStream( new FileOutputStream("/tmp/foo.serialStreamFormatter.via.formatStreamHandler.qds") );
        //form.init( name, sh );
        
        form.init( name, java.nio.channels.Channels.newChannel( new FileOutputStream("/tmp/foo.serialStreamFormatter.toStream.qds") ) );
        //form.init( name, Channels.newChannel( System.out ) ); //


        if ( join ) {
            form.join( name, 3, new HashMap<String, Object>() );
            for ( int j=0; j<ds.length(); j++ ) {
                QDataSet jds1= ds.slice(j);
                form.maybeFormat( null, (QDataSet) jds1.property(QDataSet.DEPEND_1), INOUTFORM_INLINE);
                String channelName= name + String.valueOf(j);
                for ( int i=0; i<jds1.length(); i++ ) {
                    form.format( name, channelName, jds1.slice(i), INOUTFORM_STREAMING); //TODO: remove DDataSet.copy used for debugging.
                }            
            }
        } else {
            form.maybeFormat( null, (QDataSet) ds.property(QDataSet.DEPEND_1), INOUTFORM_INLINE );
            for ( int i=0; i<ds.length(); i++ ) {
                //TODO: we should just send all the data explicitly rather than requiring that a 
                // dataset be formed for each packet: form.format( name, tt, ds.slice(i) ), false );
                form.format( name, ds.slice(i), INOUTFORM_STREAMING ); //TODO: remove DDataSet.copy used for debugging.
            }
        }

    }

}
