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
import org.das2.datum.CacheTag;
import org.virbo.dataset.ArrayDataSet;
import org.virbo.dataset.BundleDataSet;
import org.virbo.dataset.DDataSet;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.JoinDataSet;
import org.virbo.dataset.MutablePropertyDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dsops.Ops;
import org.virbo.dsutil.DataSetBuilder;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * reads a stream and produces QDataSets representing the data found on the
 * stream.  The stream is read in, and then getDataSet or getDataSet(name) is
 * called to retrieve datasets.
 *
 * @author jbf
 */
public class QDataSetStreamHandler implements StreamHandler {

    private static final Logger logger= Logger.getLogger("qstream");

    public static final String BUILDER_JOIN_CHILDREN = "join";

    Map<String, DataSetBuilder> builders;
    Map<String, JoinDataSet> joinDataSets;
    Map<String, String[]> bundleDataSets;
    
    XPathFactory factory = XPathFactory.newInstance();
    XPath xpath = factory.newXPath();
    String dsname;
    boolean readPackets = true;

    public QDataSetStreamHandler() {
        builders = new HashMap<String, DataSetBuilder>();
        joinDataSets = new HashMap<String, JoinDataSet>();
        bundleDataSets = new HashMap<String, String[]>();
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
                default:
                    throw new IllegalArgumentException("rank limit");
            }
        } else {
            throw new IllegalArgumentException("rank and qube don't reconcile");
        }

        return result;
    }

    public void streamDescriptor(StreamDescriptor sd) throws StreamException {
        try {
            Element e = sd.getDomElement();
            dsname = xpath.evaluate("//stream/@dataset_id", e);
            if ( dsname.length()==0 ) {
                Node stream = (Node)xpath.evaluate("//stream", e, XPathConstants.NODE );
                if ( stream!=null ) {
                    throw new StreamException("dataset_id attribute expected in the stream descriptor.  Is this a qstream?");
                } else {
                    throw new StreamException("dataset_id attribute expected in the stream descriptor.  Expected to find stream.");
                }
            }
            logger.log( Level.FINE, "got streamDescriptor with default dataset {0}", dsname );
        } catch (XPathExpressionException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    /**
     * parse the data found in the node.
     * TODO: we don't have the Units yet, but we could and should use it to parse.
     * @param vn
     * @return
     * @throws XPathExpressionException
     */
    private DDataSet doInLine(Element vn) throws XPathExpressionException {

        String svals = xpath.evaluate("@values", vn ).trim();
        String sdims = xpath.evaluate("@length", vn);
        int[] dims;
        String[] ss= svals.split(",");
        if (sdims == null || sdims.length()==0 ) {
           if ( svals.length()==0 ) {
               dims = new int[0];
           } else {
               dims = new int[] { ss.length };
           }
        } else {
           dims = Util.decodeArray(sdims);
        }
        int total= dims[0];
        for ( int i=1; i<dims.length; i++ ) {
            total*= dims[i];
        }
        double [] data;
        if ( svals!=null && !svals.trim().equals("") ) { //TODO: length 0?
            data= new double[total];
            if ( total!=ss.length ) throw new IllegalArgumentException("number of elements inline doesn't match length");
            for ( int j=0; j<total; j++ ) {
                data[j]= Double.parseDouble(ss[j]);
            }
        } else {
            if ( total>0 ) throw new IllegalArgumentException("nonzero length but no inline elements");
            data= new double[0];
        }
        DDataSet result= DDataSet.wrap(data, dims);
        return result;
    }

    public void packetDescriptor(PacketDescriptor pd) throws StreamException {
        try {
            Element e = pd.getDomElement();
            XPathExpression expr = xpath.compile("/packet/qdataset");
            Object o = expr.evaluate(e, XPathConstants.NODESET);
            NodeList nodes = (NodeList) o;

            List<PlaneDescriptor> planes= pd.getPlanes();
            if ( planes.size()!=nodes.getLength() ) {
                logger.log( Level.WARNING, "these should be the same length, QDataSetStreamHandler line 189" );
            }
            for (int i = 0; i < nodes.getLength(); i++) {
                Element n = (Element) nodes.item(i);
                String name = n.getAttribute("id");
                logger.log( Level.FINER, "got packetDescriptor for {0}", name);
                int rank = Integer.parseInt(n.getAttribute("rank"));
                DataSetBuilder builder=null;
                String sdims=null;
                int[] dims= null;
                String joinChildren= null; //  join will be specified.
                String joinParent= null;
                boolean isInline= false;
                joinParent= n.getAttribute("joinId");

                NodeList values= (NodeList) xpath.evaluate("values", n, XPathConstants.NODESET );
                NodeList bundles= null;
                if ( values.getLength()==0 ) {
                    bundles= (NodeList) xpath.evaluate("bundle", n, XPathConstants.NODESET );
                    if ( bundles.getLength()==0 ) {
                        throw new IllegalArgumentException("no values node in "+n.getNodeName() + " " +n.getAttribute("id") );
                    } else {
                        logger.log( Level.FINER, "newBundle");
                    }
                }

                if ( bundles!=null ) {
                    //nothing to do yet...
                    builder= new DataSetBuilder(0,0);
                    builders.put(name,builder ); // this is to hold properties.

                    String[] ss= planes.get(i).getBundles();
                    bundleDataSets.put( name, ss );

                } else {
                    for ( int iv= 0; iv<values.getLength(); iv++ ) {
                        Element vn= (Element)values.item(iv);
                        DDataSet inlineDs= null;
                        if ( vn.hasAttribute("values") ) {  // TODO: consider "inline"
                            inlineDs= doInLine( vn );
                            isInline= true;
                        }
                        //index stuff--Ed W. thinks index should be implicit.
                        sdims = xpath.evaluate("@length", vn);
                        joinChildren = xpath.evaluate("@join", vn);

                        if (sdims == null) {
                            dims = new int[0];
                        } else {
                            dims = Util.decodeArray(sdims);
                        }

                        if ( isInline && inlineDs.rank()<rank ) {
                           JoinDataSet join = joinDataSets.get(name);
                           if (join == null) {
                               join = new JoinDataSet(rank);
                               joinDataSets.put(name, join);
                           }
                           join.join(inlineDs);
                           builder= new DataSetBuilder(0,0);
                           builders.put(name,builder ); // rank 0 means the values were in line.
                        } else if ( isInline && inlineDs.rank()==rank ) {
                           builder= new DataSetBuilder(rank,inlineDs.length());
                           for ( int j=0; j<inlineDs.length(); j++ ) {
                               DDataSet slice= (DDataSet)inlineDs.slice(j);
                               builder.putValues( -1, slice, DataSetUtil.totalLength(slice) );
                               builder.nextRecord();
                           }
                           builders.put(name,builder ); // rank 0 means the values were in line.
                        } else if ( joinChildren.length()>0 ) {
                            JoinDataSet join= joinDataSets.get(name);
                            if (join == null) { // typically we will only declare once.
                               join = new JoinDataSet(rank);
                               joinDataSets.put(name, join);
                           }
                            builder= new DataSetBuilder(1,10);
                            builder.putProperty(BUILDER_JOIN_CHILDREN, joinChildren);
                            builders.put(name,builder ); //
                        } else {
                            builder = builders.get(name);
                            if (builder == null) {
                                builder = createBuilder(rank, dims);
                                builders.put(name, builder);
                                if ( !joinParent.equals("") ) {
                                    JoinDataSet parent= joinDataSets.get( joinParent );
                                    String children= (String) parent.property(BUILDER_JOIN_CHILDREN);
                                    if ( children==null || children.length()==0 ) {
                                        children= name;
                                    } else {
                                        children= children + "," + name ;
                                    }
                                    parent.putProperty( BUILDER_JOIN_CHILDREN, children );
                                }
                            } else {
                                JoinDataSet join;
                                if ( joinParent.equals("") ) {
                                    join= joinDataSets.get(name); // old scheme
                                } else {
                                    join= joinDataSets.get(joinParent);
                                }

                                if (join == null) { // /home/jbf/ct/hudson/data.backup/qds/aggregation.qds
                                    logger.log( Level.FINE, "repeat of packet type for {0}, increasing rank.", name );
                                    join = new JoinDataSet(rank+1);
                                    joinDataSets.put(name, join);
                                }
                                MutablePropertyDataSet mds= resolveProps( builder.getDataSet() );
                                join.join(mds);

                                builder = createBuilder(rank, dims);
                                builders.put(name, builder);
                            }
                        }
                    }
                }

                NodeList odims = (NodeList) xpath.evaluate("properties[not(@index)]/property", n, XPathConstants.NODESET);
                doProps( odims, builder );

                odims = (NodeList) xpath.evaluate("properties[@index]/property", n, XPathConstants.NODESET);
                doPropsIndex( odims, joinDataSets.get(name) );

                PlaneDescriptor planeDescriptor = planes.get(i);
                planeDescriptor.setBuilder(builder);
                // we no longer do the addPlane stuff here, since it is done at the source.
                // TODO: a lot of work is done twice here, but this takes a trivial amount of time.
            }
        } catch (XPathExpressionException ex) {
            logger.log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }

    private MutablePropertyDataSet resolveProps( MutablePropertyDataSet result ) {
       for (int i = 0; i < QDataSet.MAX_RANK; i++) {
            String s = (String) result.property("DEPENDNAME_" + i);
            if (s != null) {
                QDataSet dep0ds= getDataSet(s);
                if ( dep0ds.rank()==1 ) {
                    result.putProperty("DEPEND_" + i, dep0ds );
                } else {
                    //we're building DEPEND_0 as well, so this is resolved at the end.
                    //System.err.println("dropping DEPEND_0 for now");
                }
            }
        }
       for (int i = 0; i < QDataSet.MAX_RANK; i++) {
            Object o = result.property("DEPEND_" + i);
            if (o != null && o instanceof String ) {
                logger.log(Level.WARNING, "QDataSetStreamHandler: still strings in DEPEND_{0}", i);
                result.putProperty("DEPEND_" + i, getDataSet((String)o));
            }
        }
       for (int i = 0; i < QDataSet.MAX_RANK; i++) {
            Object o= result.property("BUNDLE_" + i);
            if ( o instanceof String ) { //TODO: still need to clean up messages here, maybe...
		String s = (String) o;
		if (s != null) {
		    result.putProperty("BUNDLE_" + i, getDataSet(s));
		}
	    }
        }
        for (int i = 0; i < QDataSet.MAX_PLANE_COUNT; i++) {
            String s = (String) result.property("PLANE_" + i);
            if (s != null) {
                result.putProperty("PLANE_" + i, getDataSet(s));
            } else {
                break;
            }
        }
       for ( int i=0; i<2; i++ ) {
           String pname= i==0 ? "DELTA_MINUS" : "DELTA_PLUS";
           String s = (String) result.property( pname );
           if (s != null) {
                result.putProperty( pname, getDataSet(s) );
            } else {
                continue;
            }
       }
       return result;
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
                    TransferType tt= planeDescriptor.getType();
                    if ( tt==null ) {
                        logger.severe("here planeDescriptor.getType() is null");
                    }
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

    /**
     * return the dataset from the stream
     * @param name the name of the dataset to retrieve.
     * @return the dataset
     */
    public QDataSet getDataSet(String name) {
        logger.log(Level.FINE, "getDataSet({0})", name);
        DataSetBuilder builder = builders.get(name);
        String[] sbds= bundleDataSets.get(name);
        JoinDataSet join = joinDataSets.get(name);

        if (builder == null && sbds==null ) throw new IllegalArgumentException("No such dataset \"" + name + "\"");
        
        MutablePropertyDataSet result;
        MutablePropertyDataSet sliceDs= null;
            
        if (join != null) {
            String joinChild= (String)join.property(BUILDER_JOIN_CHILDREN);
            join= JoinDataSet.copy(join);
            if ( builder.rank()>0 ) {
                sliceDs= builder.getDataSet();
                List<QDataSet> childDataSets=null;
                if ( sliceDs!=null && sliceDs.property(BUILDER_JOIN_CHILDREN)!=null ) {
                    if ( joinChild==null ) joinChild= (String)sliceDs.property(BUILDER_JOIN_CHILDREN);
                    String[] children= joinChild.split(",");
                    childDataSets= new ArrayList();
                    for ( int i=0; i<children.length; i++ ) {
                        DataSetBuilder childBuilder= builders.get(children[i]);
                        if ( childBuilder!=null ) {
                            MutablePropertyDataSet sliceDs1= childBuilder.getDataSet();
                            resolveProps(sliceDs1);
                            childDataSets.add( sliceDs1 );
                            logger.log(Level.FINER, "child: {0}", sliceDs1.toString());
                        } else {
                            logger.log(Level.WARNING, "missing child: {0}", children[i]);
                        }
                    }
                }
                if ( childDataSets!=null ) {
                    for ( QDataSet child: childDataSets ) {
                        join.join(child);
                    }
                }
                DataSetUtil.putProperties( builder.getProperties(), join );
                //if ( sliceDs!=null ) join.join(sliceDs);
                if ( sliceDs!=null && sliceDs.length()>0 ) {
                    logger.fine("aggregation has one last dataset to append");
                    join.join(sliceDs);
                }
            } else {
                DataSetUtil.putProperties( builder.getProperties(), join );
                sliceDs= (MutablePropertyDataSet) join.slice(join.length()-1); //wha??  when do we use this?
            }
            result = join;

        } else if ( sbds!=null ) {
            BundleDataSet bds= new BundleDataSet();
            for ( int i=0; i<sbds.length; i++ ) {
                bds.bundle( getDataSet(sbds[i]) );
            }
            result= bds;
            
        } else {
            result = builder.getDataSet();
        }

        if (join != null) {
            if ( sliceDs!=null ) resolveProps(sliceDs); //TODO: this technically breaks things, because we cannot call getDataSet again
        } else {
            resolveProps(result);
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
     * if true, then packets are interpreted.
     * @return
     */
    public boolean getReadPackets() {
        return this.readPackets;
    }

    private static void doProps(NodeList odims, DataSetBuilder builder) {
         for (int j = 0; j < odims.getLength(); j++) {
                    Element n2 = (Element) odims.item(j);
                    String pname = n2.getAttribute("name");
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
                        if (pname.equals(QDataSet.DELTA_MINUS) || pname.equals(QDataSet.DELTA_PLUS) ) {
                            logger.warning("skipping DELTA_MINUS and DELTA_PLUS because bug");
                            builder.putProperty( pname, svalue );
                        }
                        if ( pname.matches( "DEPEND_\\d+") ) {
                            String si= pname.substring(7);
                            builder.putProperty( "DEPENDNAME_"+si, svalue );
                        } else {
                            builder.putProperty(pname, svalue);
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
                            builder.putProperty(pname, oval);
                        } catch (ParseException ex) {
                            logger.log(Level.SEVERE, null, ex);
                        }
                    }
         }
    }
    
    private static void doPropsIndex( NodeList odims, JoinDataSet join ) {
        for (int j = 0; j < odims.getLength(); j++) {

            Element n2 = (Element) odims.item(j);
            String sidx= ((Element)n2.getParentNode()).getAttribute("index");
            int index= Integer.parseInt(sidx);
            String pname = n2.getAttribute("name");
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
                join.putProperty(pname, index, svalue);
            } else {
                SerializeDelegate delegate = SerializeRegistry.getByName(stype);
                if (delegate == null) {
                    logger.log(Level.SEVERE, "no delegate found for \"{0}\"", stype);
                    continue;
                }
                Object oval;
                try {
                    if ( evalue!=null && delegate instanceof XMLSerializeDelegate ) {
                        oval= ((XMLSerializeDelegate)delegate).xmlParse(evalue);
                    } else {
                        oval= delegate.parse(stype, svalue);
                    }
                    join.putProperty(pname, index, oval);
                } catch (ParseException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
         }
    }

    /**
     * this is a bit of a kludge, where I don't want to flatten a dataset automatically, but we probably want to.
     *
     * @param ds
     * @return
     */
    public static boolean isFlattenableJoin( QDataSet ds ) {
        if ( ds.rank()==2 && ds.property(QDataSet.DEPEND_0)==null && ds.property(QDataSet.DEPENDNAME_0)!=null ) {
            return true;
        } else if ( ds.rank()==3 && ds.length()>0 && ds.property(QDataSet.DEPEND_0)==null && ds.property(QDataSet.DEPENDNAME_0)!=null ) {
            QDataSet dep1= (QDataSet) ds.slice(0).property(QDataSet.DEPEND_1);
            for ( int i=1; i<ds.length(); i++ ) {
                if ( dep1==null ) {
                    if ( ds.length(0)!=ds.length(i) ) {
                        return false;
                    }
                } else {
                    QDataSet dep1t= (QDataSet) ds.slice(i).property(QDataSet.DEPEND_1);
                    if ( !Ops.equivalent( dep1, dep1t ) ){
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    /**
     * since an appended series of rank 1 datasets will return as a rank 2 join, this utility provides
     * a standard place to flatten it.
     * This will also flatten DEPENDNAME_0.
     * @param ds
     * @return
     */
    public QDataSet flattenJoin( QDataSet ds ) {
        int len= 0;
        for ( int i=0; i<ds.length(); i++ ) {
            len+= ds.length(i);
        }
        ArrayDataSet result= ArrayDataSet.maybeCopy( ds.slice(0) );
        result.grow(len);
        for ( int i=1; i<ds.length(); i++ ) {
            result.append( ArrayDataSet.maybeCopy( ds.slice(i)) );
        }
        String s= (String)ds.property(QDataSet.DEPENDNAME_0);
        if ( s!=null ) {
            MutablePropertyDataSet dep0= (MutablePropertyDataSet) getDataSet(s);
            dep0= (MutablePropertyDataSet)flattenJoin(dep0);
            ((MutablePropertyDataSet)result).putProperty( QDataSet.DEPEND_0, dep0 );
            ((MutablePropertyDataSet)result).putProperty( QDataSet.DEPENDNAME_0, null );
        }
        return result;

    }
}
