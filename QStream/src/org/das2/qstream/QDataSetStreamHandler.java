
package org.das2.qstream;

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFactoryConfigurationException;
import org.das2.datum.Datum;
import org.das2.datum.EnumerationUnits;
import org.das2.qds.ArrayDataSet;
import org.das2.qds.BundleDataSet;
import org.das2.qds.DDataSet;
import org.das2.qds.DataSetUtil;
import org.das2.qds.JoinDataSet;
import org.das2.qds.MutablePropertyDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.ops.Ops;
import org.das2.qds.util.DataSetBuilder;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * reads a stream and produces QDataSets representing the data found on the stream. The stream is read in, and
 * then getDataSet or getDataSet(name) is called to retrieve datasets.
 *
 * @author jbf
 */
public class QDataSetStreamHandler implements StreamHandler {

    private static final Logger logger = Logger.getLogger("qstream");

    public static final String BUILDER_JOIN_CHILDREN = "join";

    Map<String, DataSetBuilder> builders;
    Map<String, JoinDataSet> joinDataSets;
    Map<String, String[]> bundleDataSets;
    Map<String, Integer> ranks;

    XPathFactory factory = getXPathFactory();
    XPath xpath = factory.newXPath();
    String dsname;
    boolean readPackets = true;
    
    // each enumeration units need to be parsed within the context of the stream, not the entire session.
    EnumerationUnitsSerializeDelegate enumerationUnitsSerializeDelegate= new EnumerationUnitsSerializeDelegate();

    public QDataSetStreamHandler() {
        builders = new HashMap<>();
        joinDataSets = new HashMap<>();
        bundleDataSets = new HashMap<>();
        ranks= new HashMap<>();
    }

    /**
     * Matlab uses net.sf.saxon.xpath.XPathEvaluator by default, so we explicitly look for the Java 6 one.
     *
     * @return com.sun.org.apache.xpath.internal.jaxp.XPathFactoryImpl, probably.
     *
     * This is a copy of DataSourceUtil.getXPathFactory.
     */
    private static XPathFactory getXPathFactory() {
        XPathFactory xpf;
        try {
            xpf = XPathFactory.newInstance(XPathFactory.DEFAULT_OBJECT_MODEL_URI, "com.sun.org.apache.xpath.internal.jaxp.XPathFactoryImpl", null);
        } catch (XPathFactoryConfigurationException ex) {
            xpf = XPathFactory.newInstance();
            logger.log(Level.INFO, "using default xpath implementation: {0}", xpf.getClass());
        }
        return xpf;
    }

    /**
     * return a list of available datasets
     *
     * @return
     */
    public List<String> getDataSetNames() {
        return new ArrayList<>(builders.keySet());
    }

    /**
     * return a list of available datasets and their label (or name if not available).
     *
     * @return
     */
    public Map<String, String> getDataSetNamesAndDescriptions() {
        Map<String, String> result = new LinkedHashMap();
        for (Entry<String, DataSetBuilder> e : builders.entrySet()) {
            DataSetBuilder b = e.getValue();
            String name = null;
            String n;
            n = (String) b.getProperties().get(QDataSet.NAME);
            if (n != null) {
                name = n;
            }
            n = (String) b.getProperties().get(QDataSet.LABEL);
            if (n != null) {
                name = n;
            }
            result.put(e.getKey(), name);
        }
        return result;
    }

    /**
     * create a builder, figuring out if there's an implicit streaming dimension.
     *
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
                case 4:
                    result = new DataSetBuilder(rank, qube[0], qube[1], qube[2], qube[3] );
                    break;
                default:
                    throw new IllegalArgumentException("rank limit");
            }
        } else if (rank == qube.length + 1) {
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
                case 4:
                    result = new DataSetBuilder(rank, 100, qube[0], qube[1], qube[2] );
                    break;
                    
                default:
                    throw new IllegalArgumentException("rank limit");
            }
        } else if (rank == qube.length + 2) {
            switch (rank) {
                case 2:
                    result = new DataSetBuilder(rank - 1, 100);
                    break;
                case 3:
                    result = new DataSetBuilder(rank - 1, 100, qube[0]);
                    break;
                case 4:
                    result = new DataSetBuilder(rank - 1, 100, qube[0], qube[1] );
                    break;
                    
                default:
                    throw new IllegalArgumentException("rank limit");
            }
        } else {
            throw new IllegalArgumentException("rank and qube don't reconcile");
        }

        return result;
    }

    @Override
    public void streamDescriptor(StreamDescriptor sd) throws StreamException {
        try {
            Element e = sd.getDomElement();
            dsname = xpath.evaluate("//stream/@dataset_id", e);
            if (dsname.length() == 0) {
                Node stream = (Node) xpath.evaluate("//stream", e, XPathConstants.NODE);
                if (stream != null) {
                    throw new StreamException("dataset_id attribute expected in the stream descriptor.  Is this a qstream?");
                } else {
                    throw new StreamException("dataset_id attribute expected in the stream descriptor.  Expected to find stream.");
                }
            }
            logger.log(Level.FINE, "got streamDescriptor with default dataset {0}", dsname);
        } catch (XPathExpressionException ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    /**
     * parse the data found in the node. TODO: we don't have the Units yet, but we could and should use it to
     * parse.
     *
     * @param vn
     * @return
     * @throws XPathExpressionException
     */
    private DDataSet doInLine(Element vn) throws XPathExpressionException {

        String svals = xpath.evaluate("@values", vn).trim();
        if ( svals.equals("") ) {
            svals= xpath.evaluate("@inline", vn).trim();
        }
        String sdims = xpath.evaluate("@length", vn);
        int[] dims;
        String[] ss = svals.split(",");
        if (sdims == null || sdims.length() == 0) {
            if (svals.length() == 0) {
                dims = new int[0];
            } else {
                dims = new int[]{ss.length};
            }
        } else {
            dims = Util.decodeArray(sdims);
        }
        int total = dims[0];
        for (int i = 1; i < dims.length; i++) {
            total *= dims[i];
        }
        double[] data;
        if (!svals.trim().equals("")) { //TODO: length 0?
            data = new double[total];
            if (total != ss.length) {
                throw new IllegalArgumentException("number of elements inline doesn't match length");
            }
            for (int j = 0; j < total; j++) {
                data[j] = Double.parseDouble(ss[j]);
            }
        } else {
            if (total > 0) {
                throw new IllegalArgumentException("nonzero length but no inline elements");
            }
            data = new double[0];
        }
        DDataSet result = DDataSet.wrap(data, dims);
        return result;
    }

    @Override
    public void packetDescriptor(PacketDescriptor pd) throws StreamException {
        try {
            Element e = pd.getDomElement();
            XPathExpression expr = xpath.compile("/packet/qdataset");
            Object o = expr.evaluate(e, XPathConstants.NODESET);
            NodeList nodes = (NodeList) o;

            List<PlaneDescriptor> planes = pd.getPlanes();
            if (planes.size() != nodes.getLength()) {
                logger.log(Level.WARNING, "planes.size and nodes.getLength should be the same length, QDataSetStreamHandler line 254");
            }
            for (int i = 0; i < nodes.getLength(); i++) {
                Element n = (Element) nodes.item(i);
                String name = n.getAttribute("id");
                logger.log(Level.FINER, "got packetDescriptor for {0}", name);
                int rank = Integer.parseInt(n.getAttribute("rank"));
                ranks.put( name, rank );
                DataSetBuilder builder = null;
                String sdims;
                int[] dims;
                String joinChildren; //  join will be specified.
                String joinParent;
                boolean isInline = false;
                joinParent = n.getAttribute("joinId");

                NodeList values = (NodeList) xpath.evaluate("values", n, XPathConstants.NODESET);
                NodeList bundles = null;
                if (values.getLength() == 0) {
                    bundles = (NodeList) xpath.evaluate("bundle", n, XPathConstants.NODESET);
                    if (bundles.getLength() == 0) {
                        throw new IllegalArgumentException("no values node in " + n.getNodeName() + " " + n.getAttribute("id"));
                    } else {
                        logger.log(Level.FINER, "newBundle");
                    }
                }

                if (bundles != null) {
                    //nothing to do yet...
                    builder = new DataSetBuilder(0, 0);
                    builders.put(name, builder); // this is to hold properties.

                    String[] ss = planes.get(i).getBundles();
                    bundleDataSets.put(name, ss);

                } else {
                    for (int iv = 0; iv < values.getLength(); iv++) {
                        Element vn = (Element) values.item(iv);
                        DDataSet inlineDs = null;
                        int index=-1;
                        if (vn.hasAttribute("values")) {  // TODO: consider "inline"
                            inlineDs = doInLine(vn);
                            inlineDs.putProperty( QDataSet.UNITS, planes.get(i).getUnits() );
                            isInline = true;
                            if ( vn.hasAttribute("index") ) {
                                index= Integer.parseInt( vn.getAttribute("index") );
                            }
                        } else if ( vn.hasAttribute("inline")) {  // TODO: consider "inline"
                            inlineDs = doInLine(vn);
                            isInline = true;
                            if ( vn.hasAttribute("index") ) {
                                index= Integer.parseInt( vn.getAttribute("index") );
                            }
                        } else if ( vn.hasAttribute("bundle") ) {
                            builder = new DataSetBuilder(1, 0);
                            builders.put(name, builder); // this is to hold properties.

                            String[] ss = planes.get(i).getBundles();
                            bundleDataSets.put(name, ss);
                        }
                        
                        //index stuff--Ed W. thinks index should be implicit.
                        sdims = xpath.evaluate("@length", vn);
                        joinChildren = xpath.evaluate("@join", vn);

                        if (sdims == null) {
                            dims = new int[0];
                        } else {
                            dims = Util.decodeArray(sdims);
                        }

                        if (isInline && inlineDs != null && inlineDs.rank() < rank) { // I believe assert inlineDs!=null
                            if ( iv==0 && joinDataSets.containsKey(name) ) {
                                logger.log(Level.FINE, "resetting join dataset for name {0}", name);
                                joinDataSets.remove(name);
                            }
                            JoinDataSet join = getJoinDataSet(name,rank);
                            if ( index>=0 ) {
                                join.join(index,inlineDs);                                
                            } else {
                                join.join(inlineDs);
                            }
                            builder = new DataSetBuilder(0);
                            builders.put(name, builder); // rank 0 means the values were in line.
                        } else if (isInline && inlineDs != null && inlineDs.rank() == rank) {
                            builder = new DataSetBuilder(rank, inlineDs.length());
                            for (int j = 0; j < inlineDs.length(); j++) {
                                DDataSet slice = (DDataSet) inlineDs.slice(j);
                                builder.putValues(-1, slice, DataSetUtil.totalLength(slice));
                                builder.nextRecord();
                            }
                            builders.put(name, builder); // rank 0 means the values were in line.
                        } else if (joinChildren.length() > 0) {
                            getJoinDataSet(name,rank); // make sure it is allocated.
                            builder = new DataSetBuilder(1, 10);
                            builder.putProperty(BUILDER_JOIN_CHILDREN, joinChildren);
                            builders.put(name, builder); //
                        } else {
                            builder = builders.get(name);
                            if (builder == null) {
                                builder = createBuilder(rank, dims);
                                builders.put(name, builder);
                                if (!joinParent.equals("")) {
                                    JoinDataSet parent = joinDataSets.get(joinParent);
                                    String children = (String) parent.property(BUILDER_JOIN_CHILDREN);
                                    if (children == null || children.length() == 0) {
                                        children = name;
                                    } else {
                                        children = children + "," + name;
                                    }
                                    parent.putProperty(BUILDER_JOIN_CHILDREN, children);
                                }
                            } else {
                                JoinDataSet join;
                                if (joinParent.equals("")) {
                                    join = joinDataSets.get(name); // old scheme
                                } else {
                                    join = joinDataSets.get(joinParent);
                                }

                                if (join == null) { // /home/jbf/ct/hudson/data.backup/qds/aggregation.qds
                                    logger.log(Level.FINE, "repeat of packet type for {0}, increasing rank.", name);
                                    join = new JoinDataSet(rank + 1);
                                    joinDataSets.put(name, join);
                                }
                                if ( !bundleDataSets.containsKey(name) ) {
                                    builder.setDataSetResolver( getResolver() );
                                    MutablePropertyDataSet mds = resolveProps(name, builder.getDataSet());
                                    join.join(mds);

                                    builder = createBuilder(rank, dims);
                                    builders.put(name, builder);
                                }
                            }
                        }
                    }
                }
                
                NodeList odims = (NodeList) xpath.evaluate("properties[not(@index)]/property", n, XPathConstants.NODESET);
                doProps(odims, builder);
                
                if ( planes.get(i).getUnits() instanceof EnumerationUnits && builder!=null ) {
                    EnumerationUnits eu= (EnumerationUnits) planes.get(i).getUnits();
                    EnumerationUnits eun= (EnumerationUnits) builder.getProperties().get(QDataSet.UNITS);
                    if ( eu!=null && eun!=null ) {
                        for ( Entry<Integer,Datum> es: eu.getValues().entrySet() ) {
                            Datum d= es.getValue();
                            eun.createDatum( (int)d.doubleValue(eu), d.toString(), eu.getColor(d) );
                        }
                    }
                }
                
                odims = (NodeList) xpath.evaluate("properties[@index]/property", n, XPathConstants.NODESET);
                doPropsIndex(odims, joinDataSets.get(name));

                PlaneDescriptor planeDescriptor = planes.get(i);
                planeDescriptor.setBuilder(builder);
                // we no longer do the addPlane stuff here, since it is done at the source.
                // TODO: a lot of work is done twice here, but this takes a trivial amount of time.
            }
        } catch (XPathExpressionException ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    /**
     * resolve properties that can be resolved, namely DEPENDNAME_0 which is a string linking two datasets.
     *
     * @param name the name of this dataset in the file.
     * @param result the dataset we are resolving.
     */
    private MutablePropertyDataSet resolveProps(String name, MutablePropertyDataSet result) {
        // read datasets that need to be resolved.
        for (int i = 0; i < QDataSet.MAX_RANK; i++) {
            String s = (String) result.property("DEPENDNAME_" + i);
            if (s != null) {
                if (s.equals(name)) {
                    //result.putProperty( QDataSet.DEPEND_0, result );  // sometimes it's nice to look at Epoch[Epoch]
                    continue;
                }
                QDataSet dep0ds = getDataSetInternal(s);
                if ( dep0ds==null ) {
                    logger.log(Level.WARNING, "unable to resolve property DEPENDNAME_{0}=\"{1}\"", new Object[]{i, s});
                } else if (dep0ds.rank() == 1) {
                    result.putProperty("DEPEND_" + i, dep0ds);
                } else if (dep0ds.rank()==2 && SemanticOps.isBins(dep0ds) ){
                    result.putProperty("DEPEND_" + i, dep0ds);
                } else if (i > 0 && dep0ds.rank() == 2) {
                    result.putProperty("DEPEND_" + i, dep0ds);
                } else {
                   //we're building DEPEND_0 as well, so this is resolved at the end.
                    //System.err.println("dropping DEPEND_0 for now");
                }
            }
        }

        // check for legacy behavior where string was used to refer to dataset.
        for (int i = 0; i < QDataSet.MAX_RANK; i++) {
            Object o = result.property("DEPEND_" + i);
            if (o != null && o instanceof String) {
                String s = (String) o;
                logger.log(Level.WARNING, "QDataSetStreamHandler: still strings in DEPEND_{0}", i);
                result.putProperty("DEPEND_" + i, getDataSetInternal(s));
            }
        }
        for (int i = 0; i < QDataSet.MAX_RANK; i++) {
            Object o = result.property("BUNDLE_" + i);
            if (o instanceof String) { //TODO: still need to clean up messages here, maybe...
                String s = (String) o;
                result.putProperty("BUNDLE_" + i, getDataSetInternal(s));
            }
        }
        for (int i = 0; i < QDataSet.MAX_PLANE_COUNT; i++) {
            String propertyName= "PLANE_" + i;
            Object o= result.property(propertyName);
            if ( o instanceof String ) {
                String s = (String) o;
                result.putProperty( propertyName, getDataSetInternal(s) );
            } else if ( o==null ) {
                break;
            }
        }
        
        String[] propertyNames= new String[] { "DELTA_MINUS", "DELTA_PLUS", 
            "BIN_MINUS", "BIN_PLUS",
            "BIN_MIN", "BIN_MAX", "WEIGHTS"
        };
        for (String propertyName : propertyNames) {
            Object o= result.property(propertyName);
            if ( o instanceof String ) {
                String s = (String) o;
                result.putProperty( propertyName, getDataSetInternal(s) );
            }
        }
        return result;
    }

    @Override
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
                        if (planeDescriptor.getRank() > 1) {
                            throw new IllegalArgumentException("non-streaming and rank>1 not supported");
                        }
                        for (int i = 0; i < rec.length(); i++) {
                            builder.putValue(-1, rec.value(i));
                            builder.nextRecord();
                        }
                    } else {
                        builder.putValues(-1, rec, planeDescriptor.getElements()); // aliasing okay
                        builder.nextRecord();
                    }
                } else {
                    TransferType tt = planeDescriptor.getType();
                    if (tt == null) {
                        logger.severe("here planeDescriptor.getType() is null");
                    }
                    builder.putValue(-1, planeDescriptor.getType().read(data));
                    builder.nextRecord();
                }

            }
        }
    }

    @Override
    public void streamClosed(StreamDescriptor sd) throws StreamException {
    }

    @Override
    public void streamComment(StreamComment se) throws StreamException {
        System.err.println("here comment");
    }

    @Override
    public void streamException(StreamException se) throws StreamException {
    }

    private DataSetBuilder.DataSetResolver getResolver() {
        return new DataSetBuilder.DataSetResolver() {
            @Override
            public QDataSet resolve( String name ) {
                QDataSet result= joinDataSets.get(name);
                if ( result==null ) {
                    DataSetBuilder builder= builders.get(name);
                    if ( builder!=null ) {
                        result= builder.getDataSet();
                    }
                }
                return result;
            }
        };
    }
    
    /**
     * get the JoinDataSet into which we will store datasets.  Start
     * a new one if the dataset does not exist, or return the one we are working
     * on.
     * @param name name of the join
     * @param rank the requested or expected rank.
     */
    private JoinDataSet getJoinDataSet( String name, int rank ) {
        JoinDataSet join = joinDataSets.get(name);
        if (join == null) {
            join = new JoinDataSet(rank);
            joinDataSets.put(name, join);
        } else {
            if ( join.rank()!=rank ) {
                throw new IllegalArgumentException("rank mismatch");
            }
        }
        return join;
    }

    /**
     * return the dataset from the stream.
     *
     * @param name the name of the dataset to retrieve.
     * @return the dataset
     */
    public QDataSet getDataSet(String name) {
        QDataSet result= getDataSetInternal(name);
        if ( result==null ) {
            throw new IllegalArgumentException("No such dataset \"" + name + "\"");
        }
        Integer rank= ranks.get(name);
        if ( rank!=result.rank() && isFlattenableJoin(result) ) {
            logger.log(Level.FINE, "flattening join for {0}: {1}", new Object[]{name, result});
            result= flattenJoin(result);
        }
        return result;
    }    
    
    /**
     * return the default dataset for the stream.  The default dataset is
     * identified in the stream packet descriptor dataset_id attribute.
     *
     * @return the default dataset
     */
    public QDataSet getDataSet() {
        return getDataSet(dsname);
    }
    
    /**
     * return the dataset from the stream
     *
     * @param name the name of the dataset to retrieve.
     * @return the dataset or null if no such dataset exists.
     */
    private QDataSet getDataSetInternal(String name) {
        logger.log(Level.FINE, "getDataSet({0})", name);
        DataSetBuilder builder = builders.get(name);
        String[] sbds = bundleDataSets.get(name);
        JoinDataSet join = joinDataSets.get(name);

        if (builder == null && sbds == null) {
            logger.log(Level.INFO, "no such dataset: {0}", name);
            return null;
        }

        MutablePropertyDataSet result;
        MutablePropertyDataSet sliceDs = null;

        if (join != null && sbds==null ) {
            String joinChild = (String) join.property(BUILDER_JOIN_CHILDREN);
            join = JoinDataSet.copy(join);
            if (builder != null && builder.rank() > 0) {
                builder.setDataSetResolver( getResolver() );
                sliceDs = builder.getDataSet();
                List<QDataSet> childDataSets = null;
                if ( sliceDs.property(BUILDER_JOIN_CHILDREN) != null) {
                    if (joinChild == null) {
                        joinChild = (String) sliceDs.property(BUILDER_JOIN_CHILDREN);
                    }
                    String[] children = joinChild.split(",");
                    childDataSets = new ArrayList();
                    for (String children1 : children) {
                        DataSetBuilder childBuilder = builders.get(children1);
                        if (childBuilder != null) {
                            childBuilder.setDataSetResolver( getResolver() );
                            MutablePropertyDataSet sliceDs1 = childBuilder.getDataSet();
                            
                            resolveProps(null, sliceDs1);
                            childDataSets.add(sliceDs1);
                            logger.log(Level.FINER, "child: {0}", sliceDs1.toString());
                        } else {
                            logger.log(Level.WARNING, "missing child: {0}", children1);
                        }
                    }
                }
                if (childDataSets != null) {
                    for (QDataSet child : childDataSets) {
                        join.join(child);
                    }
                }
                DataSetUtil.putProperties(builder.getProperties(), join);
                if ( sliceDs.length() > 0) {
                    resolveProps(null, sliceDs);
                    logger.fine("aggregation has one last dataset to append");
                    join.join(sliceDs);
                }
            } else {
                assert builder != null;
                DataSetUtil.putProperties(builder.getProperties(), join);
                sliceDs = (MutablePropertyDataSet) join.slice(join.length() - 1); //wha??  when do we use this?
            }
            result = join;

        } else if (sbds != null) {
            BundleDataSet bds = new BundleDataSet();
            for (String sbd : sbds) {
                bds.bundle(getDataSet(sbd));
            }
            result = bds;

        } else {
            assert builder != null;
            builder.setDataSetResolver( getResolver() );
            result = builder.getDataSet();
        }

        if (join != null) {
            if (sliceDs != null) {
                resolveProps(name, sliceDs); //TODO: this technically breaks things, because we cannot call getDataSet again
            }
        } else {
            resolveProps(name, result);
        }
        
        return result;
    }

    /**
     * set this is false if you just want to look at the empty dataset metadata.
     *
     * @param val
     */
    public void setReadPackets(boolean val) {
        this.readPackets = val;
    }

    /**
     * if true, then packets are interpreted.
     *
     * @return
     */
    public boolean getReadPackets() {
        return this.readPackets;
    }

    private void doProps(NodeList odims, DataSetBuilder builder) {
        for (int j = 0; j < odims.getLength(); j++) {
            Element n2 = (Element) odims.item(j);
            String pname = n2.getAttribute("name");
            String svalue;
            if (n2.hasAttribute("value")) {
                svalue = n2.getAttribute("value");
            } else {
                svalue = n2.getTextContent();
            }
            Element evalue = null;

            String stype;
            if (n2.hasAttribute("type")) {
                stype = n2.getAttribute("type");
            } else {
                evalue = Util.singletonChildElement(n2);
                stype = evalue.getTagName();
            }
            if (stype.equals("qdataset")) {
                if (pname.equals(QDataSet.DELTA_MINUS) || pname.equals(QDataSet.DELTA_PLUS)) {
                    logger.warning("skipping DELTA_MINUS and DELTA_PLUS because bug");
                    builder.putProperty(pname, svalue);
                    builder.putUnresolvedProperty(DataSetBuilder.UNRESOLVED_PROP_QDATASET,pname,svalue);
                }
                if (pname.matches("DEPEND_\\d+")) {
                    String si = pname.substring(7);
                    builder.putProperty("DEPENDNAME_" + si, svalue);
                } else {
                    builder.putProperty(pname, svalue);
                    builder.putUnresolvedProperty(DataSetBuilder.UNRESOLVED_PROP_QDATASET,pname,svalue);
                }
            } else {
                SerializeDelegate delegate = SerializeRegistry.getByName(stype);
                if (delegate == null) {
                    logger.log(Level.SEVERE, "!!! No delegate found for \"{0}\"", stype); // chris and I didn't see this invisible message
                    continue;
                }
                Object oval;
                try {
                    if (evalue != null && delegate instanceof XMLSerializeDelegate) {
                        oval = ((XMLSerializeDelegate) delegate).xmlParse(evalue);
                    } else {
                        if ( stype.equals("enumerationUnit") ) {
                            oval= enumerationUnitsSerializeDelegate.parse( stype, svalue );
                        } else {
                            oval = delegate.parse(stype, svalue);
                        }
                    }
                    builder.putProperty(pname, oval);
                } catch (ParseException ex) {
                    logger.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
        }
    }

    private static void doPropsIndex(NodeList odims, JoinDataSet join) {
        for (int j = 0; j < odims.getLength(); j++) {

            Element n2 = (Element) odims.item(j);
            String sidx = ((Element) n2.getParentNode()).getAttribute("index");
            int index = Integer.parseInt(sidx);
            String pname = n2.getAttribute("name");
            String svalue;
            if (n2.hasAttribute("value")) {
                svalue = n2.getAttribute("value");
            } else {
                svalue = n2.getTextContent();
            }
            Element evalue = null;

            String stype;
            if (n2.hasAttribute("type")) {
                stype = n2.getAttribute("type");
            } else {
                evalue = Util.singletonChildElement(n2);
                stype = evalue.getTagName();
            }
            if (stype.equals("qdataset")) {
                join.putProperty(pname, index, svalue);
            } else {
                SerializeDelegate delegate = SerializeRegistry.getByName(stype);
                if (delegate == null) {
                    logger.log(Level.SEVERE, "no delegate found for \"{0}\"", stype);
                    continue;
                }
                Object oval;
                try {
                    if (evalue != null && delegate instanceof XMLSerializeDelegate) {
                        oval = ((XMLSerializeDelegate) delegate).xmlParse(evalue);
                    } else {
                        oval = delegate.parse(stype, svalue);
                    }
                    join.putProperty(pname, index, oval);
                } catch (ParseException ex) {
                    logger.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
        }
    }

    /**
     * If the dataset is a join of appendable datasets, then we can append them to reduce the rank by 1 and
     * make one long time series. These datasets should be equivalent, however most of the system doesn't
     * implement this (and probably never will). So this is a bit of a kludge, where I don't want to flatten a
     * dataset automatically, but we probably want to.
     *
     * @param ds a join dataset of rank 2 or rank 3.
     * @return true if the data can be joined.
     */
    public static boolean isFlattenableJoin(QDataSet ds) {
        if ( ds.rank() == 2 && ds.property(QDataSet.DEPEND_0)==null && ds.property(QDataSet.BUNDLE_1)==null && ds.property(QDataSet.BINS_1)==null ) {
            return true;
        } else if (ds.rank() == 3 && ds.length() > 0 && ds.property(QDataSet.DEPEND_0) == null && ds.property(QDataSet.DEPENDNAME_0) != null) {
            QDataSet dep1 = (QDataSet) ds.slice(0).property(QDataSet.DEPEND_1);
            for (int i = 1; i < ds.length(); i++) {
                if (dep1 == null) {
                    if (ds.rank() == 3) { // join of vector datasets.
                        if (ds.length(0, 0) != ds.length(i, 0)) {
                            return false;
                        }
                    } else { // ds.rank()==2, old code.
                        if (ds.length(0) != ds.length(i)) {
                            return false;
                        }
                    }
                } else {
                    QDataSet dep1t = (QDataSet) ds.slice(i).property(QDataSet.DEPEND_1);
                    if (!Ops.equivalent(dep1, dep1t)) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    /**
     * since an appended series of rank 1 datasets will return as a rank 2 join, this utility provides a
     * standard place to flatten it. This will also flatten DEPENDNAME_0.
     *
     * @param ds rank 2 or 3 join dataset.
     * @return rank 1 or 2 dataset.
     */
    public MutablePropertyDataSet flattenJoin(QDataSet ds) {
        int len = 0;
        if ( ds.rank()<2 ) throw new IllegalArgumentException("rank should be > 2" );
        for (int i = 0; i < ds.length(); i++) {
            len += ds.length(i);
        }
        ArrayDataSet result = ArrayDataSet.maybeCopy(ds.slice(0));
        if ( result.isImmutable() ) result= ArrayDataSet.copy(result);
        result.grow(len);
        for (int i = 1; i < ds.length(); i++) {
            result.append(ArrayDataSet.maybeCopy(ds.slice(i)));
        }
        String s = (String) ds.property(QDataSet.DEPENDNAME_0);
        if (s != null) {
            MutablePropertyDataSet dep0 = (MutablePropertyDataSet) getDataSet(s);
            //dep0 = (MutablePropertyDataSet) flattenJoin(dep0); //getDataSet has already flattened it.
            dep0.putProperty(QDataSet.TYPICAL_MIN, null); // remove so we don't just use MIN and MAX from one dataset.
            dep0.putProperty(QDataSet.TYPICAL_MAX, null);
            ((MutablePropertyDataSet) result).putProperty(QDataSet.DEPEND_0, dep0);
            ((MutablePropertyDataSet) result).putProperty(QDataSet.DEPENDNAME_0, null);
        }
        return result;

    }
}
