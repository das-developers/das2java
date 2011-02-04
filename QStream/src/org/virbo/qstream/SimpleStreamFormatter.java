/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.qstream;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.das2.datum.EnumerationUnits;
import org.das2.datum.TimeLocationUnits;
import org.das2.datum.TimeUtil;
import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.QubeDataSetIterator;
import org.virbo.dataset.RankZeroDataSet;
import org.virbo.dataset.SemanticOps;
import org.virbo.dsops.Ops;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * 
 * @author jbf
 */
public class SimpleStreamFormatter {

    Map<PlaneDescriptor, QDataSet> planeToDataSet;
    boolean asciiTypes = true;
    boolean isBigEndian = true; //ByteOrder.nativeOrder() .equals( ByteOrder.BIG_ENDIAN );

    Document getNewDocument() {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            return builder.newDocument();
        } catch (ParserConfigurationException pce) {
            throw new RuntimeException(pce);
        }
    }

    Element getPacketElement(Document document) {
        Element packet = document.createElement("packet");
        return packet;
    }
    Map<QDataSet, String> names = new HashMap<QDataSet, String>();
    Map<QDataSet, String> joinDataSets= new HashMap<QDataSet, String>();

    private void doValuesElement(QDataSet ds, PacketDescriptor pd, PlaneDescriptor planeDescriptor, Document document, Element qdatasetElement) throws DOMException {
        Object sunits= ds.property(QDataSet.UNITS);
        if ( sunits!=null && !(sunits instanceof Units) ) {
            throw new IllegalArgumentException( "UNITS property doesn't contain type units, it's type "+sunits.getClass()+": "+sunits );
        }
        boolean highResKludge= false;
        Units u = (Units) sunits;
        if ( u==null && isBundle(ds) ) {
            // uh-oh.  just use a high-resolution format for now.
            highResKludge= true;
        }
        if (!pd.isValuesInDescriptor()) {
            if (asciiTypes) {
                double min = Double.POSITIVE_INFINITY;
                double max = Double.NEGATIVE_INFINITY;
                double absMin = Double.MAX_VALUE; // smallest non-zero number.
                double maxFp = 0.; // biggest fractional part.
                double minFp = 1.; // smallest non-zero fractional part.
                QubeDataSetIterator it = new QubeDataSetIterator(ds);
                while (it.hasNext()) {
                    it.next();
                    double d = it.getValue(ds);
                    if (d < min) {
                        min = d;
                    }
                    final double dd = Math.abs(d);
                    final double fp = dd - (long) dd;
                    if (fp > maxFp) {
                        maxFp = fp; // check for all ints, for now. //TODO: check for highest precision.
                    }
                    if (fp > 0 && fp < minFp) {
                        minFp = fp;
                    }
                    if (dd > 0 && dd < absMin) {
                        absMin = dd;
                    }
                    if (d > max) {
                        max = d;
                    }
                }
                if (u instanceof EnumerationUnits) {
                    planeDescriptor.setType(new AsciiIntegerTransferType(10));
                } else if (u instanceof TimeLocationUnits) {
                    AsciiTimeTransferType att = getTT(ds);
                    planeDescriptor.setType(att);
                } else {
                    //QDataSet diffs= Ops.subtract( ds, DataSetOps.slice0(ds,0) );
                    //QDataSet gcd= DataSetUtil.gcd( diffs, DataSetUtil.asDataSet(absMin/100) );
                    if ( highResKludge ) {
                        // we don't have separate formatters for each bundled dataset, so just use a high resolution format for now.
                        planeDescriptor.setType( new AsciiTransferType(18,true) );
                    } else if (maxFp == 0 && min > -1e8 && max < 1e8) {
                        planeDescriptor.setType(new AsciiIntegerTransferType(10));
                    } else if (min > -10000 && max < 10000 && absMin > 0.0001) {
                        planeDescriptor.setType(new AsciiTransferType(10, false));
                    } else if (min > -100000 && max < 100000 && minFp == 0.5 && maxFp == 0.5) {
                        planeDescriptor.setType(new AsciiTransferType(10, false));
                    } else {
                        if (absMin > 1e-100 && max < 1e100) {
                            planeDescriptor.setType(new AsciiTransferType(10, true));
                        } else {
                            planeDescriptor.setType(new AsciiTransferType(12, true));
                        }
                    }
                }
            } else {
                if ( highResKludge ) {
                    planeDescriptor.setType(new DoubleTransferType());
                } else if (u instanceof EnumerationUnits) {
                    planeDescriptor.setType(new IntegerTransferType());
                } else if (u instanceof TimeLocationUnits) {
                    planeDescriptor.setType(new DoubleTransferType());
                } else {
                    planeDescriptor.setType(new FloatTransferType());
                }
            }
        }
        if (pd.isValuesInDescriptor() && ds.rank() == 2) {
            for (int i = 0; i < ds.length(); i++) {
                Element values = doValues(document, pd, planeDescriptor, DataSetOps.slice0(ds, i));
                values.setAttribute("index", String.valueOf(i));
                qdatasetElement.appendChild(values);
            }
        } else {
            Element values = doValues(document, pd, planeDescriptor, ds);
            qdatasetElement.appendChild(values);
        }
    }

    /**
     * 
     * @param ds rank 0 dataset of time.
     * @return the number of characters needed to represent: 17,20,24,27,30.
     */
    private int timeDigits( QDataSet ds ) {
        Units u= SemanticOps.getUnits(ds);
        double micros;
        if ( UnitsUtil.isTimeLocation(u) ) {
            micros= TimeUtil.getMicroSecondsSinceMidnight( DataSetUtil.asDatum((RankZeroDataSet)ds) );
        } else {
            micros= DataSetUtil.value( (RankZeroDataSet)ds, Units.microseconds );
        }
        
        if ( micros==0 || ( micros>=60e6 && micros%60e6 <0.1 ) ) {
            return 17; // HH:MM
        } else if ( micros>=1e6 && micros%1e6 <0.001 ) {
            return 20; // HH:MM:SS
        } else if ( micros>=1000 && micros%1000 < 0.1 ) {
            return 24; // HH:MM:SS.mmm
        } else if ( micros>=1 && micros%1 < 0.001 ) {
            return 27; // HH:MM:SS.mmmmmm
        } else {
            return 30; // HH:MM:SS.mmmmmmmmm
        }

    }

    /**
     * pick a time formatter so that resolution is not reduced noticeably
     * in formatting.
     * @param ds
     * @return
     */
    private AsciiTimeTransferType getTT( QDataSet ds ) {
        
        Units u= SemanticOps.getUnits(ds);

        int timeDigits= timeDigits( DataSetOps.slice0(ds,0) );

        //TODO: same thing but with gcd added, use max of the two.
        QDataSet gcd= null;
        try {
            QDataSet diffs= Ops.subtract( ds, DataSetOps.slice0(ds,0) );
            gcd= DataSetUtil.gcd( diffs, DataSetUtil.asDataSet( 1, Units.picoseconds ) );
        } catch ( IllegalArgumentException ex ) {
            gcd= null;
        }

        if ( gcd==null ) {
            return new AsciiTimeTransferType(timeDigits, u); // millis
        } else {
            int timeDigitsGcd= timeDigits( gcd );
            return new AsciiTimeTransferType( Math.max( timeDigits, timeDigitsGcd ), u );
        }

    }

    /**
     * 
     * @param document
     * @param pd
     * @param ds
     * @param streamRank
     * @return
     */
    private PlaneDescriptor doPlaneDescriptor(Document document, PacketDescriptor pd,
            QDataSet ds, int streamRank) {
        Element qdatasetElement = document.createElement("qdataset");
        qdatasetElement.setAttribute("id", nameFor(ds));

        if ( isJoin(ds) ){
            qdatasetElement.setAttribute("rank", "" + ds.rank() );
        } else {
            qdatasetElement.setAttribute("rank", "" + (ds.rank() + (streamRank - 1)));
        }

        if ( isBundle(ds) ) {
            QDataSet bds;
            if ( ds.rank()==1 ) {
                bds= (QDataSet) ds.property("BUNDLE_0");
            } else {
                bds= (QDataSet) ds.property("BUNDLE_1");
            }
            // if we haven't already serialized a bundleDescriptor, then do legacy.
            if ( bds==null ) {
                for ( int i=0; i<ds.length(); i++ ) {
                    QDataSet ds1= DataSetOps.slice0(ds, i);
                    Element props = doProperties(document, ds1);
                    props.setAttribute("index", String.valueOf(i) );
                    qdatasetElement.appendChild(props);
                }
            }
        } else if ( isJoin(ds) ) {
            Element values=  document.createElement("values");
            String sliceName= nameFor( ds.slice(0) );  // the slices ought to have a different name
            for ( int i=0; i<ds.length(); i++ ) {
                setNameFor(ds.slice(i),sliceName);
            }
            values.setAttribute("join", sliceName ); //TODO: multiple property names
            qdatasetElement.appendChild( values );
        }

        Element props = doProperties(document, ds);
        qdatasetElement.appendChild(props);

        PlaneDescriptor planeDescriptor = new PlaneDescriptor();

        planeDescriptor.setRank(ds.rank());

        if ( !isJoin(ds) ) {
            int[] qube = DataSetUtil.qubeDims(ds);
            if ( !pd.valuesInDescriptor ) {
                if (pd.isStream()) {
                    planeDescriptor.setQube(Util.subArray(qube, 1, qube.length - 1));
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

        if ( !isJoin(ds) ) {
            doValuesElement(ds, pd, planeDescriptor, document, qdatasetElement);
        }

        planeDescriptor.setDomElement(qdatasetElement);

        return planeDescriptor;
    }

    private boolean isBundle( QDataSet ds ) {
        if ( ds.property(QDataSet.BUNDLE_0) !=null ) return true;
        if ( ds.property(QDataSet.BUNDLE_1) !=null ) return true;
        if ( ds.property(QDataSet.NAME,0)!=null &&
                ds.property(QDataSet.NAME,0)!=ds.property(QDataSet.NAME) ) {
            //TODO: this is a little bit of a kludge.  See demo 5, export the Z component.
            return true;
        } else {
            return false;
        }
    }

    private boolean isJoin( QDataSet ds ) {
        return ds.property(QDataSet.JOIN_0) !=null;
    }

    private Element doProperties(Document document, QDataSet ds) {
        Element properties = document.createElement("properties");

        Map<String, Object> props = DataSetUtil.getProperties(ds);

        for (String name : props.keySet()) {
            Object value = props.get(name);
            Element prop = document.createElement("property");
            if (value instanceof QDataSet) {
                QDataSet qds = (QDataSet) value;
                prop.setAttribute("name", name);
                if (qds.rank() == 0) {
                    SerializeDelegate r0d= (SerializeDelegate) SerializeRegistry.getByName("rank0dataset");
                    prop.setAttribute("type", "rank0dataset");
                    Units u= (Units) qds.property( QDataSet.UNITS );
                    if ( u!=null && u instanceof EnumerationUnits ) {
                        //prop.setAttribute("value", r0d.format(value) );
                        //prop.setAttribute("value", String.valueOf(value) );
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
                prop.setAttribute("name", name);
                if (sd == null) {
                    System.err.println("dropping "+name+" because unsupported type: "+value.getClass());
                } else {
                    if ( sd instanceof XMLSerializeDelegate ) {
                        prop.appendChild( ((XMLSerializeDelegate)sd).xmlFormat(document,value) );
                    } else {
                        prop.setAttribute("type", sd.typeId(value.getClass()));
                        prop.setAttribute("value", sd.format(value));
                    }
                }
            }
            properties.appendChild(prop);
        }

        return properties;
    }

    private StreamDescriptor doStreamDescriptor(QDataSet ds) throws ParserConfigurationException {
        StreamDescriptor sd = new StreamDescriptor(DocumentBuilderFactory.newInstance());
        Document document = sd.newDocument(sd);

        Element streamElement = document.createElement("stream");

        String name = nameFor(ds);
        streamElement.setAttribute("dataset_id", name);
        if (asciiTypes == false) {
            streamElement.setAttribute("byte_order", isBigEndian ? "big_endian" : "little_endian");
        }

        sd.setDomElement(streamElement);
        sd.addDescriptor(sd); // allocate [00] for itself.

        return sd;

    }

    private Element doValues( Document document, PacketDescriptor packetDescriptor, PlaneDescriptor planeDescriptor, QDataSet ds ) {
        Element values = document.createElement("values");
        int[] qubeDims=null;
        if ( !packetDescriptor.isValuesInDescriptor() ) {
            values.setAttribute("encoding", planeDescriptor.getType().name());
            qubeDims = DataSetUtil.qubeDims(ds);
        }
        
        if (packetDescriptor.isStream()) {
            if (ds.rank() > 1) {
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
                for (int i = 0; i < ds.length(); i++) {
                    s.append( "," ).append( ds.value(i) );
                }
                values.setAttribute("values", ds.length() == 0 ? "" : s.substring(1));
                if ( ds.length()==0 ) {
                    values.setAttribute("length", "0" );
                }
            }
        }
        return values;
    }

    private void formatPackets(WritableByteChannel channel, StreamDescriptor sd, PacketDescriptor pd) throws IOException {

        int bufferSize = 4 + pd.sizeBytes();

        byte[] bbuf = new byte[bufferSize];
        ByteBuffer buffer = ByteBuffer.wrap(bbuf); //ByteBuffer.allocateDirect(bufferSize);
        if (isBigEndian) {
            buffer.order(ByteOrder.BIG_ENDIAN);
        } else {
            buffer.order(ByteOrder.LITTLE_ENDIAN);
        }

        String packetTag = String.format(":%02d:", sd.descriptorId(pd));
        buffer.put(packetTag.getBytes());  // we only need to write it once.

        if (pd.isStream()) {
            int packetCount = pd.planes.get(0).getDs().length();

            int planeCount = pd.planes.size();

            for (int i = 0; i < packetCount; i++) {

                for (int iplane = 0; iplane < planeCount; iplane++) {

                    PlaneDescriptor plane = pd.planes.get(iplane);
                    TransferType tt = plane.getType();
                    QDataSet planeds = plane.getDs();

                    boolean lastPlane = iplane == planeCount - 1;

                    if (planeds.rank() == 1) {
                        tt.write(planeds.value(i), buffer);
                    } else if (planeds.rank() == 2) {
                        for (int j = 0; j < planeds.length(i); j++) {
                            tt.write(planeds.value(i, j), buffer);
                        }
                    } else {
                        QDataSet slice = DataSetOps.slice0(planeds, i);
                        QubeDataSetIterator it = new QubeDataSetIterator(slice);
                        //QubeDataSetIterator it = QubeDataSetIterator.sliceIterator(planeds,i);
                        while (it.hasNext()) {
                            it.next();
                            tt.write(it.getValue(slice), buffer);
                        }
                    }
                    if (lastPlane && tt.isAscii() && Character.isWhitespace(buffer.get(bufferSize - 1))) {
                        buffer.put(bufferSize - 1, (byte) '\n');
                    }
                }

                buffer.flip();
                channel.write(buffer);
                buffer.position(4);

            }

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
                    buffer.put(bufferSize - 1, (byte) '\n');
                }

            }

            buffer.flip();
            channel.write(buffer);
            buffer.flip();
        }


    }

    /**
     * set the names for all the joined datasets.
     * @param slice
     * @param name
     */
    private synchronized void setNameFor( QDataSet slice, String name ) {
        names.put(slice, name);
    }

    /**
     * return a name for the thing that describes dep0.  This will be used in
     * the descriptor, so if the descriptor doesn't contain the values, then
     * the name can be reused.  Note no names are reused at this point.
     * @param dep0
     * @return
     */
    private synchronized String nameFor(QDataSet dep0) {
        String name = names.get(dep0);

        if (name == null) {
            name = (String) dep0.property(QDataSet.NAME);
        }
        if (name == null) {
            name = "ds_" + names.size();
        }

        names.put(dep0, name);

        return name;
    }

    PacketDescriptor doPacketDescriptorNonQube( StreamDescriptor sd, QDataSet ds,
            boolean stream, boolean valuesInDescriptor, int streamRank )
            throws ParserConfigurationException {

        PacketDescriptor packetDescriptor = new PacketDescriptor();
        packetDescriptor.setStream(stream);
        packetDescriptor.setStreamRank(streamRank);
        if (valuesInDescriptor) {
            packetDescriptor.setValuesInDescriptor(true);
        }

        Document document = sd.newDocument(packetDescriptor);

        Element packetElement = getPacketElement(document);

        QDataSet dep0 = (QDataSet) ds.property(QDataSet.DEPEND_0);
        if (dep0 != null) {
            PlaneDescriptor planeDescriptor = doPlaneDescriptor(document, packetDescriptor, dep0, streamRank);
            packetDescriptor.addPlane(planeDescriptor);
            packetElement.appendChild(planeDescriptor.getDomElement());
        }

        for (int i = 0; i < QDataSet.MAX_PLANE_COUNT; i++) {
            QDataSet plane0 = (QDataSet) ds.property("PLANE_" + i);
            if (plane0 != null) {
                PlaneDescriptor planeDescriptor = doPlaneDescriptor(document, packetDescriptor, plane0, streamRank);
                packetDescriptor.addPlane(planeDescriptor);
                packetElement.appendChild(planeDescriptor.getDomElement());
            } else {
                break;
            }
        }

        PlaneDescriptor planeDescriptor = doPlaneDescriptor(document, packetDescriptor, ds, streamRank);
        packetDescriptor.addPlane(planeDescriptor);

        packetElement.appendChild(planeDescriptor.getDomElement());

        packetDescriptor.setDomElement(packetElement);

        return packetDescriptor;

    }

    PacketDescriptor doPacketDescriptorJoin( StreamDescriptor sd, QDataSet ds,
            boolean stream, boolean valuesInDescriptor, int streamRank )
            throws ParserConfigurationException {

        PacketDescriptor packetDescriptor = new PacketDescriptor();
        packetDescriptor.setStream(stream);
        packetDescriptor.setStreamRank(streamRank);
        if (valuesInDescriptor) {
            packetDescriptor.setValuesInDescriptor(true);
        }

        Document document = sd.newDocument(packetDescriptor);

        Element packetElement = getPacketElement(document);

        Object join = ds.property(QDataSet.JOIN_0);
        if (join == null) {
            throw new IllegalArgumentException("expected join");
        }
            
        PlaneDescriptor planeDescriptor = doPlaneDescriptor(document, packetDescriptor, ds, streamRank);
        packetDescriptor.addPlane(planeDescriptor);
        packetElement.appendChild(planeDescriptor.getDomElement());

        packetDescriptor.setDomElement(packetElement);

        return packetDescriptor;

    }
    /**
     * create the packetDescriptor object.  
     * @param sd the handle for the stream
     * @param ds the dataset that will be serialized to this packet descriptor.
     * @param stream the data will be streamed in packets.
     * @param valuesInDescriptor the data will be contained in the packet.
     * @param streamRank the rank of the stream.  
     * @return
     * @throws ParserConfigurationException
     */
    PacketDescriptor doPacketDescriptor(StreamDescriptor sd, QDataSet ds, boolean stream, boolean valuesInDescriptor,
            int streamRank, String joinId) throws ParserConfigurationException {

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

        Element packetElement = getPacketElement(document);

        QDataSet dep0 = (QDataSet) ds.property(QDataSet.DEPEND_0);
        if (dep0 != null) {
            PlaneDescriptor planeDescriptor = doPlaneDescriptor(document, packetDescriptor, dep0, streamRank);
            packetDescriptor.addPlane(planeDescriptor);
            packetElement.appendChild(planeDescriptor.getDomElement());
        }

        for (int i = 0; i < QDataSet.MAX_PLANE_COUNT; i++) {
            QDataSet plane0 = (QDataSet) ds.property("PLANE_" + i);
            if (plane0 != null) {
                PlaneDescriptor planeDescriptor = doPlaneDescriptor(document, packetDescriptor, plane0, streamRank);
                packetDescriptor.addPlane(planeDescriptor);
                packetElement.appendChild(planeDescriptor.getDomElement());
            } else {
                break;
            }
        }

        PlaneDescriptor planeDescriptor = doPlaneDescriptor(document, packetDescriptor, ds, streamRank);
        packetDescriptor.addPlane(planeDescriptor);

        Element dselement= planeDescriptor.getDomElement();
        if (joinId!=null ) dselement.setAttribute("joinId", joinId );

        packetElement.appendChild(dselement);

        packetDescriptor.setDomElement(packetElement);

        return packetDescriptor;
    }

    /**
     * serialize the dataset to the output stream.  Presently most datasets can
     * be serialized:
     *   * rank 0
     *   * rank 1
     *   * rank 2 qubes
     *   * array of rank 2 qubes
     *   * rank 3 qubes
     *   * bundle datasets.
     * The design goal is that all datasets can be serialized using this formatter,
     * however some schemas (e.g. high-rank non-qubes) will be inefficient.
     *
     * @param ds the dataset to serialize.
     * @param osout
     * @param asciiTypes use ascii format types so that the stream is completely ascii.
     * @throws StreamException
     * @throws IOException
     */
    public void format( QDataSet ds, OutputStream osout, boolean asciiTypes ) throws StreamException, IOException {

        try {

            this.asciiTypes = asciiTypes;

            WritableByteChannel out = Channels.newChannel(osout);

            StreamDescriptor sd = doStreamDescriptor(ds);

            List<String> probs= new ArrayList();
            if ( !DataSetUtil.validate(ds, probs ) ) {
                throw new IllegalArgumentException("DataSet is not valid: "+probs.get(0)); 
            }

            List<PacketDescriptor> depPackets = new ArrayList<PacketDescriptor>();

            sd.send(sd, out);

            int packetDescriptorCount = 1;
            int streamRank; //TODO: describe this
            String dep0Name = null;

            if ( isBundle(ds) && asciiTypes ) {
                // we need to do more with this.  right now there's just one planeDescriptor for the bunch.
                System.err.println("suboptimal implementation doesn't use different formatters for each bundled dataset");
            }

            if (DataSetUtil.isQube(ds)) {
                packetDescriptorCount = 1;
                streamRank = 1;
            } else if ( isJoin(ds) ) {
                packetDescriptorCount = ds.length();
                streamRank = 1;
            } else {
                packetDescriptorCount = ds.length();
                streamRank = 2;
            }

            QDataSet dep0 = (QDataSet) ds.property(QDataSet.DEPEND_0);
            if (dep0 != null) {
                dep0Name = nameFor(dep0);
            }

            String joinDataSet=null;
            boolean isjoin= ds.property(QDataSet.JOIN_0) != null;
            if ( isjoin ) {
                PacketDescriptor join= doPacketDescriptorJoin(sd, ds, false, false, streamRank);
                sd.addDescriptor(join);
                sd.send(join, out);
                joinDataSet= nameFor(ds);
            }

            for (int ipacket = 0; ipacket < packetDescriptorCount; ipacket++) {
                PacketDescriptor mainPd;
                QDataSet packetDs;

                List<PacketDescriptor> retire= new ArrayList();
                
                if (streamRank == 1 && !isjoin ) {
                    packetDs = ds;
                } else {
                    packetDs = ds.slice(ipacket);
                    names.put(packetDs, nameFor(packetDs));
                    if (dep0Name != null) {
                        names.put((QDataSet) packetDs.property(QDataSet.DEPEND_0), dep0Name);
                    //TODO: Planes are still a problem
                    }
                }

                if ( isJoin(packetDs) ) {
                    throw new IllegalArgumentException("join of join not supported");
                }
                mainPd = doPacketDescriptor(sd, packetDs, true, false, streamRank, joinDataSet );

                sd.addDescriptor(mainPd);

                // check for DEPEND_1 and DEPEND_2 datasets that need to be sent out first.
                for (int i = 1; i < QDataSet.MAX_RANK; i++) {
                    QDataSet depi = (QDataSet) packetDs.property("DEPEND_" + i);
                    if (depi != null) {
                        if ( depi==dep0 ) { // kludge: if DEPEND_0==DEPEND_1, an invalid stream was created
                            throw new RuntimeException("bug in QStream prevents DEPEND_0==DEPEND_1");
                            /*(System.err.println("DEPEND_0==DEPEND_1, copy kludge");
                            depi= DDataSet.copy(dep0);
                            String name= (String) dep0.property(QDataSet.NAME);
                            if ( name!=null ) {
                                ((MutablePropertyDataSet)depi).putProperty( QDataSet.NAME, name + "_1" );
                            }*/
                        }
                        PacketDescriptor pd;

                        boolean valuesInDescriptor = true; // because it's a non-qube
                        pd = doPacketDescriptor(sd, depi, false, valuesInDescriptor, 1, null);

                        sd.addDescriptor(pd);

                        depPackets.add(pd);
                        sd.send(pd, out);

                        retire.add(pd);
                    }
                }

                // check for BUNDLE_1 datasets that need to be sent out first.
                for (int i = 0; i < 2; i++) {
                    QDataSet depi = (QDataSet) packetDs.property("BUNDLE_" + i);
                    if (depi != null) {
                        PacketDescriptor pd;

                        for ( int j=0; j<depi.length(); j++ ) {
                            QDataSet dep1= (QDataSet) depi.property(QDataSet.DEPEND_1, j );
                            if ( dep1!=null ) {
                                pd = doPacketDescriptor(sd, dep1, false, true, 1, null);
                                sd.addDescriptor(pd);
                                depPackets.add(pd);
                                sd.send(pd, out);
                            }
                        }
                        
                        boolean valuesInDescriptor = true;
                        pd = doPacketDescriptor(sd, depi, false, valuesInDescriptor, 1, null );

                        sd.addDescriptor(pd);

                        depPackets.add(pd);
                        sd.send(pd, out);

                        retire.add(pd);
                    }
                }

                sd.send(mainPd, out);

                for (PacketDescriptor deppd : depPackets) {
                    if (!deppd.isValuesInDescriptor()) {
                        formatPackets(out, sd, deppd);
                    }
                }

                formatPackets(out, sd, mainPd);

                for ( PacketDescriptor r: retire ) {
                    sd.retireDescriptor(r);
                }
                sd.retireDescriptor(mainPd);
            }

        } catch (ParserConfigurationException ex) {
            throw new RuntimeException(ex);
        }
    }
}
