/*
 * DataSetAdapter.java
 *
 * Created on April 2, 2007, 8:49 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.das2.dataset;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumRangeUtil;
import org.das2.datum.Units;
import org.das2.system.DasLogger;
import org.virbo.dataset.AbstractDataSet;
import org.virbo.dataset.DDataSet;
import org.virbo.dataset.DRank0DataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;
import org.virbo.dsops.Ops;

/**
 * Presents legacy das2 datasets as QDataSets. See also TableDataSetAdapter,VectorDataSetAdapter
 *
 * @author jbf
 */
public class DataSetAdapter {

    private static final Logger logger = DasLogger.getLogger(DasLogger.DATA_TRANSFER_LOG);
    
    public static final String PROPERTY_SOURCE = "adapterSource";

	 ///////////////////////////////////////////////////////////////////////////////////
    // Helper for conversion such as %{xCacheRange} -> %{USER_PROPERTIES.xCacheRange}
    protected static Map<String, Object> adaptSubstitutions(Map<String, Object> das2props) {
        // Defines a pattern with three subgroups
        Pattern ptrn = Pattern.compile("(%\\{)(.+?)(\\})");

        for (Map.Entry<String, Object> e : das2props.entrySet()) {
            Object o = e.getValue();
            if (!(o instanceof String)) {
                continue;
            }
            String s = (String) o;
            Matcher m = ptrn.matcher(s);
            while (m.find()) {
                // Group indices are not as expected, 0 = entire match, 1 = 1st group, etc.
                if (!m.group(2).contains("USER_PROPERTIES")) {
                    s = String.format("%sUSER_PROPERTIES.%s%s", s.substring(0, m.end(1)),
                            s.substring(m.start(2), m.end(2)),
                            s.substring(m.start(3), s.length()));
                    m = ptrn.matcher(s);
                }
            }
            e.setValue(s);
        }
        return das2props;
    }
	 
    /**
     * Created a new QDataSet given a Das2 DataSet
     *
     * This function and createLegacyDataSet() are inverses, though a round trip conversion 
	  * is not guaranteed to preserve all properties
     *
     * @param ds A Das2 Dataset
     * @return A new QDataSet
     */
    public static AbstractDataSet create(DataSet ds) {
        if (ds == null) {
            throw new NullPointerException("dataset is null");
        }

        // X-Y Datasets
        if (ds instanceof VectorDataSet) {

            if (ds.getPlaneIds().length <= 1) {
                //Handle x single y as a simple vector
                Vector v = new Vector((VectorDataSet) ds);
                String sname= (String)ds.getProperty("name");
                if ( sname==null ) sname= "y";
                v.putProperty(QDataSet.NAME, sname );
                return v;
                
            } else {
                //Handle x multi y as a bundle
                VectorDataSet vds = (VectorDataSet) ds;
                Vector v = new Vector(vds);
                String sname= (String)ds.getProperty("name");
                if ( sname==null ) sname= "y";
                v.putProperty(QDataSet.NAME, sname );
                AbstractDataSet bds = (AbstractDataSet) Ops.bundle(null,v);
                String[] planes = ds.getPlaneIds();
                Units unitsY = null;
                boolean bCommonYUnits = false;
                for (int i = 1; i < planes.length; i++) {
					     // Arg, everything we want to get at is hidden behind 7 levels of
                    // interfaces.  As a bonus, class names repeat in different packages from
                    // the same dev group.
                    org.das2.dataset.AbstractDataSet.ViewDataSet view
                            = (org.das2.dataset.AbstractDataSet.ViewDataSet) vds.getPlanarView(planes[i]);
                    if (unitsY == null) {
                        unitsY = view.getYUnits();
                    } else {
                        bCommonYUnits = (unitsY == view.getYUnits());
                    }

                    v = new Vector((VectorDataSet) vds.getPlanarView(planes[i]), planes[i]);
                    v.putProperty(QDataSet.NAME, planes[i]);
                    Ops.bundle(bds, v);
                }

                // Convert Das2 property substitutions to USER_PROPERTIES substitutions
                Map<String, Object> dasProps = adaptSubstitutions(vds.getProperties());
                bds.putProperty(QDataSet.USER_PROPERTIES, dasProps);

                bds.putProperty(QDataSet.DEPEND_0, new XTagsDataSet(vds));
                bds.putProperty(QDataSet.TITLE, dasProps.get(DataSet.PROPERTY_TITLE));

				    // If all Y elements of the bundle have the same units, put those units
                // on the Y axis, that way something identifies Y.
                if (bCommonYUnits) {
                    bds.putProperty(QDataSet.UNITS, unitsY);
                    bds.putProperty(QDataSet.LABEL, unitsY.toString());
                }
					 
					 // Copy more properties into the overall bundle dataset, wow this really
					 // needs to be refactored.
					 bds.putProperty(QDataSet.SCALE_TYPE, vds.getProperty(DataSet.PROPERTY_Y_SCALETYPE));
					 DatumRange yRng = (DatumRange) vds.getProperty(DataSet.PROPERTY_Y_RANGE);
					 if (yRng != null) {
						 bds.putProperty(QDataSet.TYPICAL_MIN, yRng.min().value());
						 bds.putProperty(QDataSet.TYPICAL_MAX, yRng.max().value());
					 }

                return DDataSet.copy(bds);
            }
        }

        // X-YScan Datasets
        if (ds instanceof TableDataSet) {
            TableDataSet tds = (TableDataSet) ds;
            if (tds.tableCount() <= 1) {
					// Handling table datasets that may come with statistics planes.  These are
					// denoted by having a "source" and "operations" string properties.
					return createSimpleTableDS(tds);
            } else {
                if (tds instanceof DefaultTableDataSet && tds.tableCount() > tds.getXLength() / 2) {
                    return ((DefaultTableDataSet) tds).toQDataSet();
                } else {
                    return new MultipleTable(tds);
                }
            }
        }

        throw new IllegalArgumentException("unsupported dataset type: " + ds.getClass().getName());
    }

    /**
     * Created a new Das2 DataSet given a QDataSet
     *
     * This function and create() are inverses, though a round trip conversion is not guaranteed to preserve
     * all properties. Note that not all QDataSets can be represented as Das2 DataSets. If the given QDataSet
     * has no Das2 analog, an IllegalArgumentException is thrown.
     *
     * @param ds A QDataSet
     * @return A new Das2 DataSet
     */
    public static DataSet createLegacyDataSet(org.virbo.dataset.QDataSet ds) {
        if (ds.rank() == 1) {
            return VectorDataSetAdapter.create(ds);
        } else if (SemanticOps.isBundle(ds)) {
            return VectorDataSetAdapter.createFromBundle(ds);
        } else if (ds.rank() == 2) {
            return TableDataSetAdapter.create(ds);
        } else if (ds.rank() == 3) {
            return TableDataSetAdapter.create(ds);
        } else {
            throw new IllegalArgumentException("unsupported rank: " + ds.rank());
        }
    }

	 ///////////////////////////////////////////////////////////////////////////////////
    // Helper dataset holds DEPEND_0 for MultipleTable QDataSets
    static class MultiTableXTagsDataSet extends AbstractDataSet {

        DataSet source;
        int offset;
        int length;

        MultiTableXTagsDataSet(DataSet source, int offset, int length) {
            this.source = source;
            this.offset = offset;
            this.length = length;
            properties.put(QDataSet.UNITS, source.getXUnits());
            properties.put(QDataSet.LABEL, source.getProperty(DataSet.PROPERTY_X_LABEL));
            Object o = source.getProperty(DataSet.PROPERTY_X_MONOTONIC);
            if (o != null) {
                properties.put(QDataSet.MONOTONIC, o);
            }
            Datum xTagWidth = (Datum) source.getProperty(DataSet.PROPERTY_X_TAG_WIDTH);
            if (xTagWidth != null) {
                properties.put(QDataSet.CADENCE, org.virbo.dataset.DataSetUtil.asDataSet(xTagWidth));
            }
        }

        @Override
        public int rank() {
            return 1;
        }

        @Override
        public double value(int i) {
            return source.getXTagDouble(i + offset, source.getXUnits());
        }

        @Override
        public int length() {
            return length;
        }
    }

	 ///////////////////////////////////////////////////////////////////////////////////
    // Helper dataset, holds DEPEND_0 for Vector & SimpleTable QDataSets
    static class XTagsDataSet extends AbstractDataSet {

        org.das2.dataset.DataSet source;

        XTagsDataSet(org.das2.dataset.DataSet source) {
            this.source = source;
            properties.put(QDataSet.UNITS, source.getXUnits());
            properties.put(QDataSet.LABEL, source.getProperty(DataSet.PROPERTY_X_LABEL));

            // QDataSet Cadences are a rank 0 dataset
            Datum d = (Datum) source.getProperty(DataSet.PROPERTY_X_TAG_WIDTH);
            if (d != null) {
                properties.put(QDataSet.CADENCE, DRank0DataSet.create(d));
            }

            Object o = source.getProperty(org.das2.dataset.DataSet.PROPERTY_X_MONOTONIC);
            if (o != null) {
                properties.put(QDataSet.MONOTONIC, o);
            }
        }

        @Override
        public int rank() {
            return 1;
        }

        @Override
        public double value(int i) {
            return source.getXTagDouble(i, source.getXUnits());
        }

        @Override
        public int length() {
            return source.getXLength();
        }

    }

	 ///////////////////////////////////////////////////////////////////////////////////
    // Top Level QDataSet for X vs Y data
    static class Vector extends AbstractDataSet {

        VectorDataSet source;

		  // Time for more ugly hacks:  TODO: Convert straight to QDataSet and get
        //                                  rid of stupid crap like this.
        Vector(VectorDataSet source) {
            this(source, null);
        }

        private static Object hack(Map<String, Object> m, String k, String id) {
            if (id == null) {
                return m.get(k);
            } else {
                return m.get(id + "." + k);
            }
        }

		  // This constructor takes a plane ID so that property values can be gathered.
        // It's a dumb hack to get around:
        //   1. Das2 DataSet objects are immutable
        //   2. We are not converting straght to QDataSet
        Vector(VectorDataSet source, String sPlaneID) {
            super();
            this.source = source;

            //Throw everything including the well-known stuff into user properties
            Map<String, Object> dasProps = adaptSubstitutions(source.getProperties());
            properties.put(QDataSet.USER_PROPERTIES, dasProps);

            properties.put(QDataSet.TITLE, hack(dasProps, DataSet.PROPERTY_TITLE, sPlaneID));
            properties.put(QDataSet.UNITS, source.getYUnits());
            properties.put(QDataSet.LABEL, hack(dasProps, DataSet.PROPERTY_Y_LABEL, sPlaneID));
				properties.put(QDataSet.FORMAT, hack(dasProps, DataSet.PROPERTY_Y_FORMAT, sPlaneID));
            AbstractDataSet xds= new XTagsDataSet(source);
            xds.putProperty( QDataSet.CACHE_TAG, hack( dasProps, DataSet.PROPERTY_CACHE_TAG, sPlaneID ) );
            properties.put(QDataSet.DEPEND_0, xds);
            properties.put(PROPERTY_SOURCE, source);

            // http://www.sarahandjeremy.net/~jbf/1wire/data/2007/0B000800408DD710.20071201.d2s uses property "valid_range"
            String syValid= (String)dasProps.get("valid_range");
            if ( syValid!=null ) {
                try {
                    DatumRange yValid= DatumRangeUtil.parseDatumRange( syValid, source.getYUnits() );
                    double val = yValid.min().doubleValue(source.getYUnits());
                    properties.put( QDataSet.VALID_MIN, val );
                    val = yValid.max().doubleValue(source.getYUnits());
                    properties.put( QDataSet.VALID_MAX, val );
                } catch (ParseException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
            
            //New properties after 2014-05-28 Das2 Dev meeting
            Datum d = (Datum) hack(dasProps, DataSet.PROPERTY_Y_VALID_MIN, sPlaneID);
            if (d != null) {
                double val = d.doubleValue(source.getYUnits());
                properties.put(QDataSet.VALID_MIN, val);
            }
            d = (Datum) hack(dasProps, DataSet.PROPERTY_Y_VALID_MAX, sPlaneID);
            if (d != null) {
                double val = d.doubleValue(source.getYUnits());
                properties.put(QDataSet.VALID_MAX, val);
            }

            properties.put(QDataSet.FILL_VALUE, hack(dasProps, DataSet.PROPERTY_Y_FILL, sPlaneID));
            properties.put(QDataSet.SCALE_TYPE, hack(dasProps, DataSet.PROPERTY_Y_SCALETYPE, sPlaneID));
            properties.put(QDataSet.MONOTONIC, hack(dasProps, DataSet.PROPERTY_Y_MONOTONIC, sPlaneID));

            //Add this in after next autoplot update
            properties.put(QDataSet.DESCRIPTION, hack(dasProps, DataSet.PROPERTY_Y_SUMMARY, sPlaneID));

            //Let Das2 Streams set a Y-Axis range
            DatumRange yRng = (DatumRange) hack(dasProps, DataSet.PROPERTY_Y_RANGE, sPlaneID);
            if (yRng != null) {
                properties.put(QDataSet.TYPICAL_MIN, yRng.min().value());
                properties.put(QDataSet.TYPICAL_MAX, yRng.max().value());
            }

            d = (Datum) hack(dasProps, DataSet.PROPERTY_Y_TAG_WIDTH, sPlaneID);
            if (d != null) {
                properties.put(QDataSet.CADENCE, DRank0DataSet.create(d));
            }
        }

        @Override
        public int rank() {
            return 1;
        }

        @Override
        public double value(int i) {
            return source.getDouble(i, source.getYUnits());
        }

        @Override
        public int length() {
            return source.getXLength();
        }

    }

	 ///////////////////////////////////////////////////////////////////////////////////
    // Helper Dataset, holds DEPEND_1 for SimpleTable QDataSets
    static class YTagsDataSet extends AbstractDataSet {

        TableDataSet source;
        int table;

        YTagsDataSet(TableDataSet source, int table) {
            this.source = source;
            this.table = table;
            properties.put(QDataSet.UNITS, source.getYUnits());
            properties.put(QDataSet.LABEL, source.getProperty(DataSet.PROPERTY_Y_LABEL));
            properties.put(QDataSet.SCALE_TYPE, source.getProperty(DataSet.PROPERTY_Y_SCALETYPE));

            Datum d = (Datum) source.getProperty(DataSet.PROPERTY_Y_TAG_WIDTH);
            if (d != null) {
                properties.put(QDataSet.CADENCE, DRank0DataSet.create(d));
            }

            DatumRange yRng = (DatumRange) source.getProperty(DataSet.PROPERTY_Y_RANGE);
            if (yRng != null) {
                properties.put(QDataSet.TYPICAL_MIN, yRng.min().value());
                properties.put(QDataSet.TYPICAL_MAX, yRng.max().value());
            }

        }

        @Override
        public int rank() {
            return 1;
        }

        @Override
        public double value(int i) {
            return source.getYTagDouble(table, i, source.getYUnits());
        }

        @Override
        public int length() {
            return source.tableCount() > 0 ? source.getYLength(table) : 99;
        }
    }
	 
	///////////////////////////////////////////////////////////////////////////////////
	// Helper for setting up Table QDataSet's that have statistics
	 
	private static AbstractDataSet createSimpleTableDS(TableDataSet tds){
		
		// Find the top level plane, expect there to be only one since this isn't a bundle
		// dataset handler.
		String sTopDs = null;
		String sTopSrc = null;
		String[] lPlanes = tds.getPlaneIds();
		if(lPlanes.length == 1){ 
			sTopDs = lPlanes[0];
		}
		else{
			// Take the first plane that meets the criteria as the top-level dataset, the
			// criteria is, (1) Don't have a source property or (2) be the average
			for(String sPlane: lPlanes){
				DataSet ds = tds.getPlanarView(sPlane);
				sTopSrc = (String)ds.getProperty(DataSet.PROPERTY_SOURCE);
				if(sTopSrc == null){ 
					sTopDs = sPlane;
					break;
				}
				else{
					String sOp = (String)ds.getProperty(DataSet.PROPERTY_OPERATION);
					if((sOp != null)&&(sOp.equals("BIN_AVG"))){
						sTopDs = sPlane;
						break;
					}
				}
			}
		}
		
		// TODO: Use some sort of conversion exception instead of this
		if(sTopDs == null) throw new IllegalArgumentException(
			"Couldn't locate the top-level <yscan> in the set of <yscans>.  "+
			"HINT: If this is simple bundle then you've hit a missing feature in Autoplot."+
			" If this is a peak and averages dataset, use the 'source' and 'operation'"+
			" properties to clairify the <yscan> relationships. "
		);
		
		AbstractDataSet qds = new SimpleTable((TableDataSet)tds.getPlanarView(sTopDs));
		
		// Create every other dataset and bind it to the top level one based on it's 
		// operation property.  If that's not specified OR it's source disagrees ignore it
		for(String sPlane: lPlanes){
			if(sPlane.equals(sTopDs)) continue;
			TableDataSet dsPlane = (TableDataSet) tds.getPlanarView(sPlane);
			String sPlaneSrc = (String)dsPlane.getProperty(DataSet.PROPERTY_SOURCE);
			if( ((sPlaneSrc == null)&&(sTopSrc == null)) || sTopSrc.equals(sPlaneSrc)){
				
				// Okay, they have the same source see if we understand the operation
				String sOp = (String)dsPlane.getProperty(DataSet.PROPERTY_OPERATION);
				
				if(sOp == null) continue; // TODO: Complain to logger here
				
				QDataSet qdsAncillary = new SimpleTable(dsPlane);
				
				if(sOp.equals("BIN_MAX")) qds.putProperty(QDataSet.BIN_MAX, qdsAncillary);
				if(sOp.equals("BIN_MIN")) qds.putProperty(QDataSet.BIN_MIN, qdsAncillary);
				
				if(sOp.equals(QDataSet.DELTA_PLUS)) qds.putProperty(sOp, qdsAncillary);
				if(sOp.equals(QDataSet.DELTA_MINUS)) qds.putProperty(sOp, qdsAncillary);
				
				// TODO: Complain to logger here
			}
			
		}
		return qds;
	}
	 
	 ///////////////////////////////////////////////////////////////////////////////////
    // Toplevel QDataSet for X,Y,Z "grid" data
    static class SimpleTable extends AbstractDataSet {

        TableDataSet source;

        SimpleTable(TableDataSet source) {
            super();
            if (source.tableCount() > 1) {
                throw new IllegalArgumentException("only simple tables are supported");
            }

            this.source = source;

            Map<String, Object> dasProps = adaptSubstitutions(source.getProperties());

            // Save properterties with value substitution strings in Autoplot Stlye
            properties.put(QDataSet.USER_PROPERTIES, dasProps);

            properties.put(QDataSet.UNITS, source.getZUnits());
            properties.put(QDataSet.LABEL, dasProps.get(DataSet.PROPERTY_Z_LABEL));
            properties.put(QDataSet.TITLE, dasProps.get(DataSet.PROPERTY_TITLE));
            QDataSet xtags = new XTagsDataSet(source);
            properties.put(QDataSet.DEPEND_0, xtags);
            QDataSet ytags = new YTagsDataSet(source, 0);
            properties.put(QDataSet.DEPEND_1, ytags);
            properties.put(QDataSet.QUBE, Boolean.TRUE);
            properties.put(PROPERTY_SOURCE, source);

            //Let Das2 Streams set a Z-Axis range
            DatumRange zRng = (DatumRange) dasProps.get(DataSet.PROPERTY_Z_RANGE);
            if (zRng != null) {
                properties.put(QDataSet.TYPICAL_MIN, zRng.min().value());
                properties.put(QDataSet.TYPICAL_MAX, zRng.max().value());
            }
            properties.put(QDataSet.RENDER_TYPE, dasProps.get(DataSet.PROPERTY_RENDERER));
            properties.put(QDataSet.MONOTONIC, dasProps.get(DataSet.PROPERTY_X_MONOTONIC));
            properties.put(QDataSet.FILL_VALUE, dasProps.get(DataSet.PROPERTY_Z_FILL));

            properties.put(QDataSet.VALID_MIN, dasProps.get(DataSet.PROPERTY_Z_VALID_MIN));
            properties.put(QDataSet.VALID_MAX, dasProps.get(DataSet.PROPERTY_Z_VALID_MAX));
				properties.put(QDataSet.SCALE_TYPE, dasProps.get(DataSet.PROPERTY_Z_SCALETYPE));
				properties.put(QDataSet.LABEL, dasProps.get(DataSet.PROPERTY_Z_LABEL));
        }

        @Override
        public int rank() {
            return 2;
        }

        @Override
        public int length(int i) {
            return source.getYLength(0);
        }

        @Override
        public double value(int i, int j) {
            return source.getDouble(i, j, source.getZUnits());
        }

        @Override
        public int length() {
            return source.getXLength();
        }

    }

	 ///////////////////////////////////////////////////////////////////////////////////
    // Toplevel QDataSet for multiple sets of Z data on an X,Y "grid"
    static class MultipleTable extends AbstractDataSet {

        TableDataSet source;

        MultipleTable(TableDataSet source) {
            super();

            this.source = source;

            Map<String, Object> dasProps = adaptSubstitutions(source.getProperties());

            // Save properterties with value substitution strings in Autoplot Stlye
            properties.put(QDataSet.USER_PROPERTIES, dasProps);

            properties.put(QDataSet.JOIN_0, DDataSet.create(new int[0]));
            properties.put(QDataSet.UNITS, source.getZUnits());
            properties.put(PROPERTY_SOURCE, source);
            properties.put(QDataSet.TITLE, dasProps.get(DataSet.PROPERTY_TITLE));
				
				//Let Das2 Streams set Z-Axis properties
            DatumRange zRng = (DatumRange) dasProps.get(DataSet.PROPERTY_Z_RANGE);
            if (zRng != null) {
                properties.put(QDataSet.TYPICAL_MIN, zRng.min().value());
                properties.put(QDataSet.TYPICAL_MAX, zRng.max().value());
            }
            properties.put(QDataSet.RENDER_TYPE, dasProps.get(DataSet.PROPERTY_RENDERER));
            properties.put(QDataSet.MONOTONIC, dasProps.get(DataSet.PROPERTY_X_MONOTONIC));
            properties.put(QDataSet.FILL_VALUE, dasProps.get(DataSet.PROPERTY_Z_FILL));

            properties.put(QDataSet.VALID_MIN, dasProps.get(DataSet.PROPERTY_Z_VALID_MIN));
            properties.put(QDataSet.VALID_MAX, dasProps.get(DataSet.PROPERTY_Z_VALID_MAX));
				properties.put(QDataSet.SCALE_TYPE, dasProps.get(DataSet.PROPERTY_Z_SCALETYPE));
				properties.put(QDataSet.LABEL, dasProps.get(DataSet.PROPERTY_Z_LABEL));
        }

        @Override
        public int rank() {
            return 3;
        }

        @Override
        public int length() {
            return source.tableCount();
        }

        @Override
        public int length(int i) {
            return source.tableEnd(i) - source.tableStart(i);
        }

        @Override
        public int length(int i, int j) {
            try {
                return source.getYLength(i);
            } catch (IndexOutOfBoundsException ex) {
                throw ex;
            }
        }

        @Override
        public double value(int i, int j, int k) {
            int ts = source.tableStart(i);
            try {
                return source.getDouble(ts + j, k, source.getZUnits());
            } catch (IndexOutOfBoundsException ex) {
                throw ex;
            }
        }

        @Override
        public Object property(String name, int i) {
            if (name.equals(QDataSet.DEPEND_0)) {
                return new MultiTableXTagsDataSet(source, source.tableStart(i), source.tableEnd(i) - source.tableStart(i));
            } else if (name.equals(QDataSet.DEPEND_1)) {
                return new YTagsDataSet(source, i);
            } else {
                return super.property(name, i);
            }
        }
    }
}
