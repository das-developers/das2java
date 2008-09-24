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
import org.das2.datum.Units;
import org.das2.stream.StreamException;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.QubeDataSetIterator;
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

    private PlaneDescriptor doPlaneDescriptor(Document document, PacketDescriptor pd, QDataSet ds) {
        Element qdatasetElement = document.createElement("qdataset");
        qdatasetElement.setAttribute("id", nameFor(ds));

        qdatasetElement.setAttribute("rank", "" + ds.rank());

        Element props = doProperties(document, ds);
        qdatasetElement.appendChild(props);

        PlaneDescriptor planeDescriptor = new PlaneDescriptor();

        planeDescriptor.setRank(ds.rank());
        int[] qube = DataSetUtil.qubeDims(ds);
        if (pd.isStream()) {
            planeDescriptor.setQube(Util.subArray(qube, 1, qube.length - 1));
        } else {
            planeDescriptor.setQube(qube);
        }
        planeDescriptor.setDs(ds);
        planeDescriptor.setName(nameFor(ds));

        Units u = (Units) ds.property(QDataSet.UNITS);

        if (asciiTypes) {
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            double absMin = Double.MAX_VALUE;
            QubeDataSetIterator it = new QubeDataSetIterator(ds);
            while (it.hasNext()) {
                it.next();
                double d = it.getValue(ds);
                if (d < min) {
                    min = d;
                }
                final double dd = Math.abs(d);
                if (dd < absMin) {
                    absMin = dd;
                }
                if (d > max) {
                    max = d;
                }
            }

            if (u instanceof EnumerationUnits) {
                planeDescriptor.setType(new AsciiIntegerTransferType(10));
            } else if (u instanceof TimeLocationUnits) {
                planeDescriptor.setType(new AsciiTimeTransferType(24, u));
            } else {
                if ( min > -10000 && max < 10000 && (absMin == 0 || absMin > 0.0001)) {
                    planeDescriptor.setType(new AsciiTransferType(10, false));
                } else {
                    planeDescriptor.setType(new AsciiTransferType(10, true));
                }
            }
        } else {
            if (u instanceof EnumerationUnits) {
                planeDescriptor.setType(new IntegerTransferType());
            } else if (u instanceof TimeLocationUnits) {
                planeDescriptor.setType(new DoubleTransferType());
            } else {
                planeDescriptor.setType(new FloatTransferType());
            }
        }

        Element values = doValues(document, pd, planeDescriptor, ds);
        qdatasetElement.appendChild(values);

        planeDescriptor.setDomElement(qdatasetElement);

        return planeDescriptor;
    }

    private Element doProperties(Document document, QDataSet ds) {
        Element properties = document.createElement("properties");

        Map<String, Object> props = DataSetUtil.getProperties(ds);

        for (String name : props.keySet()) {
            Object value = props.get(name);
            Element prop = document.createElement("property");
            if (value instanceof QDataSet) {
                prop.setAttribute("name", name);
                prop.setAttribute("type", "qdataset");
                prop.setAttribute("value", nameFor((QDataSet) value));

            } else {
                SerializeDelegate sd = SerializeRegistry.getDelegate(value.getClass());
                if (sd == null) {
                    throw new IllegalArgumentException("Unsupported data type: " + value.getClass());
                // TODO: just skip the property, or insert it as a string.
                }
                prop.setAttribute("name", name);
                prop.setAttribute("type", sd.typeId(value.getClass()));
                prop.setAttribute("value", sd.format(value));
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
            streamElement.setAttribute("byte_order", isBigEndian ? " big_endian" : "little_endian");
        }

        sd.setDomElement(streamElement);

        return sd;

    }

    private Element doValues(Document document, PacketDescriptor packetDescriptor, PlaneDescriptor planeDescriptor, QDataSet ds) {
        Element values = document.createElement("values");
        values.setAttribute("encoding", planeDescriptor.getType().name());
        int[] qubeDims = DataSetUtil.qubeDims(ds);
        if (packetDescriptor.isStream()) {
            if (ds.rank() > 1) {
                values.setAttribute("length", Util.encodeArray(qubeDims, 1, qubeDims.length - 1));
            } else {
                values.setAttribute("length", "");
            }
        } else {
            values.setAttribute("length", Util.encodeArray(qubeDims, 0, qubeDims.length));
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

    PacketDescriptor doPacketDescriptor(StreamDescriptor sd, QDataSet ds, boolean stream) throws ParserConfigurationException {

        if (DataSetUtil.isQube(ds) == false) {
            throw new IllegalArgumentException("must be qube!");
        }

        PacketDescriptor packetDescriptor = new PacketDescriptor();
        packetDescriptor.setStream(stream);

        Document document = sd.newDocument(packetDescriptor);

        Element packetElement = getPacketElement(document);

        QDataSet dep0 = (QDataSet) ds.property(QDataSet.DEPEND_0);
        if (dep0 != null) {
            PlaneDescriptor planeDescriptor = doPlaneDescriptor(document, packetDescriptor, dep0);
            packetDescriptor.addPlane(planeDescriptor);
            packetElement.appendChild(planeDescriptor.getDomElement());
        }

        for (int i = 0; i < QDataSet.MAX_PLANE_COUNT; i++) {
            QDataSet plane0 = (QDataSet) ds.property("PLANE_" + i);
            if (plane0 != null) {
                PlaneDescriptor planeDescriptor = doPlaneDescriptor(document, packetDescriptor, plane0);
                packetDescriptor.addPlane(planeDescriptor);
                packetElement.appendChild(planeDescriptor.getDomElement());
            }
        }

        PlaneDescriptor planeDescriptor = doPlaneDescriptor(document, packetDescriptor, ds);
        packetDescriptor.addPlane(planeDescriptor);

        packetElement.appendChild(planeDescriptor.getDomElement());

        packetDescriptor.setDomElement(packetElement);

        return packetDescriptor;
    }

    public void format(QDataSet ds, OutputStream osout, boolean asciiTypes) throws StreamException, IOException, ParserConfigurationException {

        this.asciiTypes = asciiTypes;

        WritableByteChannel out = Channels.newChannel(osout);

        StreamDescriptor sd = doStreamDescriptor(ds);

        List<PacketDescriptor> depPackets = new ArrayList<PacketDescriptor>();

        sd.send(sd, out);

        PacketDescriptor mainPd = doPacketDescriptor(sd, ds, true);

        sd.addDescriptor(mainPd);

        // check for DEPEND_1 and DEPEND_2 datasets that need to be sent out first.
        for (int i = 1; i < QDataSet.MAX_RANK; i++) {
            QDataSet depi = (QDataSet) ds.property("DEPEND_" + i);
            if (depi != null) {
                PacketDescriptor pd = doPacketDescriptor(sd, depi, false);

                sd.addDescriptor(pd);

                depPackets.add(pd);
                sd.send(pd, out);
            }
        }

        sd.send(mainPd, out);

        for (PacketDescriptor deppd : depPackets) {
            formatPackets(out, sd, deppd);
        }

        formatPackets(out, sd, mainPd);

    }
}
