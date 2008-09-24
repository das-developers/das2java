/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.qstream;

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.virbo.dataset.DDataSet;
import org.virbo.dataset.MutablePropertyDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dsutil.DataSetBuilder;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * reads a QDataSet from a stream 
 * @author jbf
 */
public class QDataSetStreamHandler implements StreamHandler {

    Map<String, DataSetBuilder> builders;
    XPathFactory factory = XPathFactory.newInstance();
    XPath xpath = factory.newXPath();
    String dsname;
    boolean readPackets = true;

    public QDataSetStreamHandler() {
        builders = new HashMap<String, DataSetBuilder>();
    }

    /**
     * return a list of available datasets
     * @return
     */
    public List<String> getDataSetNames() {
        return new ArrayList<String>(builders.keySet());
    }

    /**
     * create a builder, figuring out if there's an implicit streaming dimension.
     * @param rank
     * @param qube
     * @return
     */
    DataSetBuilder createBuilder(int rank, int[] qube) {
        DataSetBuilder result;
        if (rank == qube.length) {
            switch (rank) {
                case 1:
                    result = new DataSetBuilder(rank, qube[0]);
                    break;
                case 2:
                    result = new DataSetBuilder(rank, qube[0], qube[1]);
                    break;
                case 3:
                    result = new DataSetBuilder(rank, qube[0], qube[1], qube[2]);
                    break;
                default:
                    throw new IllegalArgumentException("rank limit");
            }
        } else {
            switch (rank) {
                case 1:
                    result = new DataSetBuilder(rank, 100);
                    break;
                case 2:
                    result = new DataSetBuilder(rank, 100, qube[0]);
                    break;
                case 3:
                    result = new DataSetBuilder(rank, 100, qube[0], qube[1]);
                    break;
                default:
                    throw new IllegalArgumentException("rank limit");
            }
        }
        return result;
    }

    public void streamDescriptor(StreamDescriptor sd) throws StreamException {
        try {
            Element e = sd.getDomElement();
            dsname = xpath.evaluate("//stream/@dataset_id", e);
        } catch (XPathExpressionException ex) {
            Logger.getLogger(QDataSetStreamHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void packetDescriptor(PacketDescriptor pd) throws StreamException {
        try {
            Element e = pd.getDomElement();
            XPathExpression expr = xpath.compile("/packet/qdataset");
            Object o = expr.evaluate(e, XPathConstants.NODESET);
            NodeList nodes = (NodeList) o;
            for (int i = 0; i < nodes.getLength(); i++) {
                Element n = (Element) nodes.item(i);
                String name = n.getAttribute("id");
                int rank = Integer.parseInt(n.getAttribute("rank"));
                String sdims = xpath.evaluate("values/@length", n);
                String ttype = xpath.evaluate("values/@encoding", n);
                int[] dims;
                if (sdims == null) {
                    dims = new int[0];
                } else {
                    dims = Util.decodeArray(sdims);
                }
                DataSetBuilder builder = builders.get(name);
                if (builder == null) {
                    builder = createBuilder(rank, dims);
                    builders.put(name, builder);
                }
                NodeList odims = (NodeList) xpath.evaluate("properties/property", n, XPathConstants.NODESET);

                for (int j = 0; j < odims.getLength(); j++) {
                    Element n2 = (Element) odims.item(j);
                    String pname = n2.getAttribute("name");
                    String svalue = n2.getAttribute("value");
                    String stype = n2.getAttribute("type");
                    if (stype.equals("qdataset")) {
                        builder.putProperty(pname, svalue);
                    } else {
                        SerializeDelegate delegate = SerializeRegistry.getByName(stype);
                        if ( delegate==null ) {
                            Logger.getLogger(QDataSetStreamHandler.class.getName()).log(Level.SEVERE, "no delegate found for \""+stype+"\"");
                            continue;
                        }
                        try {
                            builder.putProperty(pname, delegate.parse(stype, svalue));
                        } catch (ParseException ex) {
                            Logger.getLogger(QDataSetStreamHandler.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }

                PlaneDescriptor planeDescriptor = new PlaneDescriptor();
                planeDescriptor.setRank(rank);
                planeDescriptor.setQube(dims); // zero length is okay
                boolean isStream = rank > dims.length;
                pd.setStream(isStream);

                TransferType tt = TransferType.getForName(ttype, builder.getProperties());
                if (tt == null) throw new IllegalArgumentException("unrecognized transfer type: " + ttype);
                planeDescriptor.setType(tt);
                planeDescriptor.setName(name);
                planeDescriptor.setBuilder(builder);

                pd.addPlane(planeDescriptor);

            }
        } catch (XPathExpressionException ex) {
            Logger.getLogger(QDataSetStreamHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void packet(PacketDescriptor pd, ByteBuffer data) throws StreamException {
        if (readPackets) {
            for (PlaneDescriptor planeDescriptor : pd.planes) {
                DataSetBuilder builder = planeDescriptor.getBuilder();
                if (planeDescriptor.getElements() > 1) {
                    DDataSet rec = DDataSet.createRank1(planeDescriptor.getElements());
                    for (int ii = 0; ii < planeDescriptor.getElements(); ii++) {
                        rec.putValue(ii, planeDescriptor.getType().read(data));
                    }
                    if (pd.isStream() == false) {
                        if (planeDescriptor.getRank() > 1) throw new IllegalArgumentException("non-streaming and rank>1 not supported");
                        for (int i = 0; i < rec.length(); i++) {
                            builder.putValue(-1, rec.value(i));
                            builder.nextRecord();
                        }
                    } else {
                        builder.putValues(-1, rec, planeDescriptor.getElements()); // aliasing okay
                        builder.nextRecord();
                    }
                } else {
                    builder.putValue(-1, planeDescriptor.getType().read(data));
                    builder.nextRecord();
                }

            }
        }
    }

    public void streamClosed(StreamDescriptor sd) throws StreamException {
    }

    public void streamException(StreamException se) throws StreamException {
    }

    public QDataSet getDataSet(String name) {
        DataSetBuilder builder = builders.get(name);
        if (builder == null) throw new IllegalArgumentException("No such dataset");
        MutablePropertyDataSet result = builder.getDataSet();
        for (int i = 0; i < QDataSet.MAX_RANK; i++) {
            String s = (String) result.property("DEPEND_" + i);
            if (s != null) {
                result.putProperty("DEPEND_" + i, getDataSet(s));
            }
        }
        for (int i = 0; i < QDataSet.MAX_PLANE_COUNT; i++) {
            String s = (String) result.property("PLANE_" + i);
            if (s != null) {
                result.putProperty("PLANE_" + i, getDataSet(s));
            }
        }
        return result;
    }

    /**
     * return the default dataset for the stream.
     * @return
     */
    public QDataSet getDataSet() {
        return getDataSet(dsname);
    }

    /**
     * set this is false if you just want to look at the empty dataset metadata.
     * @param val
     */
    public void setReadPackets(boolean val) {
        this.readPackets = val;
    }

    /**
     * if true, then packets are interpretted.
     * @return
     */
    public boolean getReadPackets() {
        return this.readPackets;
    }
}
