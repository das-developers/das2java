/*
 * QDataSet.java
 *
 * Created on January 25, 2007, 9:12 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.das2.qds;

/**
 * <p>QDataSets are the data model used within Das2 and Autoplot.  It was preceded 
 * by a more specific data model, used to developed to deliver spectrogram time series data sets
 * where the dataset structure would change over time, and the interface is highly
 * optimized for that environment.  It's difficult to express many datasets in these
 * terms, so the simpler "quick" QDataSet was introduced.  </p>
 *
 * <p>The QDataSet can be thought of as a fast Java array that has name-value metadata
 * attached to it.  These arrays of data can have an arbitrary number of indexes, or rank, 
 * although currently the interface limits rank to 0, 1, 2, 3, and 4.  Each 
 * index's length can vary, like Java arrays, and datasets 
 * where the dimensions do not vary in length are colloquially called "Qubes."</p>
 *
 * <p>QDataSets can have other QDataSets as property values, for example the property
 * QDataSet.DEPEND_0 indicates that the values are dependent parameters of the "tags"
 * QDataSet found there.  This how how we get to the same abstraction level of 
 * the legacy Das2 dataset.  </p>
 * 
 * <p>This is inspired by the CDF data model and PaPCo's dataset model.</p>
 *
 * @see <a href="http://autoplot.org/QDataSet">http://autoplot.org/QDataSet</a>
 * @author jbf
 */
public interface QDataSet {
    
    /**
     * type QDataSet, this dataset is a dependent parameter of the independent parameter represented in this DataSet.
     * The tags for the DataSet's 0th index are identified by this tags dataset.
     */
    public final static String DEPEND_0="DEPEND_0";
    
    /**
     * type QDataSet, this dataset is a dependent parameter of the independent parameter represented in this DataSet.
     * The tags for the DataSet's 1st index are identified by this tags dataset.  When DEPEND_1 is rank 2,
     * then its first dimension goes with DEPEND_0 and its second are the tags for the second dimension.
     */
    public final static String DEPEND_1="DEPEND_1";
    
    /**
     * type QDataSet, this dataset is a dependent parameter of the independent parameter represented in this DataSet.
     * The tags for the DataSet's 2nd index are identified by this tags dataset.  When DEPEND_2 is rank 2,
     * then it's first dimension goes with DEPEND_0 and it's second are the tags for the second dimension.
     */
    public final static String DEPEND_2="DEPEND_2";
    
    /**
     * type QDataSet, this dataset is a dependent parameter of the independent parameter represented in this DataSet.
     * The tags for the DataSet's 3nd index are identified by this tags dataset.  When DEPEND_3 is rank 2,
     * then it's first dimension goes with DEPEND_0 and it's second are the tags for the second dimension.
     */
    public final static String DEPEND_3="DEPEND_3";

    /**
     * type QDataSet describing each of the bundled datasets (Bundle Descriptor).  This dataset describes 
     * how the columns should be split up
     * into separate parameters.  This rank 2 dataset has a length that is equal to the number
     * of bundled datasets.  The values(i,*) are the qube dimensions of the dataset,
     * except for the first dimension.  When all the bundled datasets are rank 1, then
     * length(*) will be equal to zero.  property(*,UNITS) will yield the unit for each
     * dataset.  Bundle dimensions generally add one physical dimension for each
     * bundled dataset.  property(*,DEPEND_0) is special, because it will return a string
     * rather than a QDataSet.  This string should refer to one of the bundled datasets by
     * its NAME property.  (Any property that returns a QDataSet should return a
     * string referring to another dataset in the bundle.)  Also the dataset is necessarily
     * a QUBE.
     */
    public final static String BUNDLE_1="BUNDLE_1";

    /**
     * type QDataSet describing each position of the rank 1 dataset (Bundle Descriptor).  This dataset describes how the columns should be split up
     * into separate parameters.  See BUNDLE_1.  Note slicing a dataset on the zeroth
     * dimension will move BUNDLE_1 to BUNDLE_0.  
     * Properties defined in this dataset will be overwritten by the BUNDLE dataset's properties.
     * For example, if the dataset has property( UNITS, 0 ) defined as "Hz" but the
     * bundle has property( UNITS,0 ) as "Hertz" then "Hertz" is used.
     */
    public final static String BUNDLE_0="BUNDLE_0";

    /**
     * type QDataSet Bundle Descriptor.  When multiple BUNDLES are present, they must be simple bundles, bundling just
     * rank 1 datasets.
     */
    public final static String BUNDLE_2="BUNDLE_2";
    
    /**
     * type QDataSet Bundle Descriptor.  When multiple BUNDLES are present, they must be simple bundles, bundling just
     * rank 1 datasets.
     */
    public final static String BUNDLE_3="BUNDLE_3";

    /**
     * type Integer, only found in a bundle descriptor (BUNDLE_0 or BUNDLE_1), this returns the integer
     * index of the start of the current dataset.  If this is null, then the index used to access
     * the value may be used.  (E.g. a bundle of Rank 1 datasets.)
     */
    public final static String START_INDEX="START_INDEX";

    /**
     * type String which is a comma-delimited list of keywords that describe the boundary
     * type for each column.  For example, "min,max" "min,maxInclusive" or "c95min,mean,c95max".
     * A bins dimension doesn't add a physical dimension.  Autoplot uses just "min,max" and "min,maxInclusive"
     */
    public final static String BINS_1="BINS_1";

    /**
     * type String which is a comma-delimited list of keywords that describe the boundary
     * type for each column.  This comma-delimited list of keywords that describe the boundary
     * type for each column.  For example, "min,max" "min,maxInclusive" or "c95min,mean,c95max".
     * A bins dimension doesn't add a physical dimension.   Autoplot uses just "min,max" and "min,maxInclusive"
     */
    public final static String BINS_0="BINS_0";

    /**
     * type String, non-null string identifies that elements in this dimension are
     * instances of data with the same dimensions.  ds[2,20] where JOIN_0="DEPEND_1" should
     * be equivalent to ds[40].  It's not clear if the text should indicate anything, but
     * for now let's just indicate the next dimension.
     */
    public final static String JOIN_0="JOIN_0";

    /**
     * type QDataSet, a correlated plane of data.  An additional dependent DataSet that is correlated by the first index.  
     * Note "0" is just a count, and does not refer to the 0th index.  All correlated datasets must be 
     * correlated by the first index.  TODO: what about two rank 2 datasets?  
     * Note that if PLANE_i==null then PLANE_(i+1) must also be null.
     */
    public final static String PLANE_0= "PLANE_0";

    /**
     * type QDataSet, that stores the position of a slice or range in
     * a collapsed dimension.  In "Flux(Energy) @ Time=2009-03-16T11:19 UT", the Time=... comes from
     * a context property.  Note "0" is just a count, and does not refer to the 0th index.
     * A dataset can have any number of contexts:
     * Temperature @ ( Time, Long, Lat ): 37 deg F @ ( 2009-03-16T11:19 UT, 91.5331 deg West, 41.6579 deg North )
     * Typically this will be a rank 0 dataset, but may also be a rank 1 dataset with a bins dimension.
     */
    public final static String CONTEXT_0= "CONTEXT_0";
    
    /**
     * type QDataSet, that stores the position of a slice or range in
     * a collapsed dimension.  In "Flux(Energy) @ Time=2009-03-16T11:19 UT", the Time=... comes from
     * a context property.  Note "1" is just a count, and does not refer to the 1th index.
     * A dataset can have any number of contexts:
     * Temperature @ ( Time, Long, Lat ): 37 deg F @ ( 2009-03-16T11:19 UT, 91.5331 deg West, 41.6579 deg North )
     * Typically this will be a rank 0 dataset, but may also be a rank 1 dataset with a bins dimension.
     */
    public final static String CONTEXT_1= "CONTEXT_1";
    
    /**
     * the maximum number of allowed planes.  This should be used to enumerate all the planes.
     */
    public final static int MAX_PLANE_COUNT=50;

    /**
     * maximum number of same-unit bundled dimensions (e.g. B_GSM[time,Bundle]).  This was introduced when CDF dataset
     * fa_k0_tms_20040224_v01.cdf?O+_en had 48 energy channels, was marked as time_series but wouldn't render because
     * view code limited to 12.
     */
    public final static int MAX_UNIT_BUNDLE_COUNT=96;

    /**
     * the highest rank supported by the library.   Arbitrary high rank datasets are supported through
     * RankNDataSet, but must be sliced to be accessed.
     */
    public static int MAX_RANK=4;
    
    /**
     * the highest rank supported by the library, without direct access 
     * to datums.  Some codes may choose to use this when supporting high rank
     * data is trivial.
     */
    public static int MAX_HIGH_RANK=8;
    
    /**
     * type Units indicating the units of the dataset in the enumeration of
     * org.das2.datum.Units, as in org.das2.datum.Units.km.  New unit types
     * can be introduced with Units.lookup.  For example,
     * <blockquote><pre>
     *from org.das2.datum import Units
     *u= Units.lookupUnits('seconds since 2015-001T00:00')
     *ds= findgen(3600)
     *ds= putProperty( ds, QDataSet.UNITS, u )
     *plot( ds )  # plots line from 00:00 to 01:00.
     *</pre></blockquote>
     * @see org.das2.datum.Units
     */
    public final static String UNITS="UNITS";

    /**
     * type String, Java/C format string for formatting the values.  This
     * should imply precision, and codes that serialize data can use this
     * to correctly format the data.  Examples include:<ul>
     * <li>%d integers</li>
     * <li>%5.1f floats with one decimal place</li>
     * <li>%.3e exponential notation</li>
     * <li>%x hex of the integer</li>
     * </ul>
     */
    public final static String FORMAT="FORMAT";

    /**
     * type Number, value to be considered fill (invalid) data.  Note because
     * all data is accessed as doubles, noise may be inadvertently affect numbers.
     */
    public final static String FILL_VALUE="FILL_VALUE";
        
    /**
     * type Number, minimum bounding measurements to be considered valid.  Lower
     * and Upper bounds are inclusive.  FILL_VALUE should be used to make the 
     * lower bound or upper bound exclusive.  Note DatumRange contains logic is
     * exclusive on the upper bound.
     */
    public final static String VALID_MIN="VALID_MIN";
    
    /**
     * type Number, maximum bounding measurements to be considered valid.  Lower
     * and Upper bounds are inclusive.  FILL_VALUE should be used to make the 
     * lower bound or upper bound exclusive.  Note DatumRange contains logic is
     * exclusive on the upper bound.
     */
    public final static String VALID_MAX="VALID_MAX";
        
    /**
     * type Number that is min used to discover datasets.  This should be a reasonable representation
     * of the expected dynamic range of the dataset.
     */    
    public final static String TYPICAL_MIN="TYPICAL_MIN";

    /**
     * type Number that is the max used to discover datasets.  This should be a reasonable representation
     * of the expected dynamic range of the dataset.
     */    
    public final static String TYPICAL_MAX="TYPICAL_MAX";
    
    /**
     * String, "linear" or "log", hinting at the preference for linear or a log
     * axis.
     */
    public final static String SCALE_TYPE="SCALE_TYPE";
    
    /**
     * String, Indicates how numbers should be combined in this space.  Possible values are
     * linear, geometric, mod24, mod360, and none.  The value "none" indicates that
     * no averaging is allowed (for example with nominal data) and only nearest neighbor
     * averaging can be done.  Note this is similar to SCALE_TYPE, where often 
     * geometric AVERAGE_TYPE will have a log SCALE_TYPE.  When AVERAGE_TYPE is missing, 
     * linear should be assumed.  See also https://spdf.gsfc.nasa.gov/istp_guide/vattributes.html#AVG_TYPE
     * and https://sourceforge.net/p/autoplot/feature-requests/593/.
     */
    public final static String AVERAGE_TYPE="AVERAGE_TYPE";
    
    /**
     * String, Concise Human-consumable label suitable for a plot label (~10 chars).
     */
    public final static String LABEL="LABEL";
    
    /**
     * String, Human-consumable string suitable for a plot title (~100 chars).
     */
    public final static String TITLE="TITLE";
                
    /**
     * String, Human-consumable string suitable for describing the data more fully.  
     * This should be html text, or just a link to other documentation (one URL, 
     * or two sentences to one page of text).
     */
    public final static String DESCRIPTION="DESCRIPTION";
    
    /**
     * QDataSet, dataset of same geometry that indicates the weights for each point.  Often weights are computed
     * in processing, and this is where they should be stored for other routines.  When the weights plane is 
     * present, routines can safely ignore the FILL_VALUE, VALID_MIN, and VALID_MAX properties, and use non-zero weight to 
     * indicate valid data.  Further, averages of averages will compute accurately.
     */
    public final static String WEIGHTS="WEIGHTS";

    /**
     * Boolean, Boolean.TRUE if dataset is monotonically increasing, and the data is rank 1.
     * Data may only contain invalid values at the beginning or end, and may contain repeated 
     * values.  Generally this will be used with tags datasets. 
     */
    public final static String MONOTONIC="MONOTONIC";
    
    /**
     * QDataSet of rank0, which is the expected distance between successive 
     * measurements where it is valid to make inferences about the data.
     * For example, interpolation is disallowed for points 1.5*CADENCE apart.  
     * This property only makes sense with a tags dataset.  Note this may be
     * a "ratiometric" datum, like 110 percentIncrease, for logarithmically 
     * spaced data.  Cadence must be positive.
     */
    public final static String CADENCE="CADENCE";
    
    /**
     * QDataSet of rank 0, or correlated QDataSet that limits accuracy.  This should
     * be interpreted as the one standard deviation confidence level, and must
     * be positive. 
     * @see #BIN_PLUS for measurement intervals.  
     */
    public final static String DELTA_PLUS="DELTA_PLUS";
    
    /**
     * QDataSet of rank 0, or correlated QDataSet that limits accuracy.  This should
     * be interpreted as the one standard deviation confidence level, and must
     * be positive.
     * @see #BIN_MINUS for measurement intervals.
     */
    public final static String DELTA_MINUS="DELTA_MINUS";

    /**
     * QDataSet of rank 0 or correlated QDataSet identifies boundary. This is added to the
     * measurements and should be interpreted as the upper limit of 100% confidence 
     * interval where a measurement was collected.   
     * Note if both DELTA_PLUS and BIN_PLUS are found,
     * then BIN_PLUS must be greater or equal to DELTA_PLUS.  This would be used where
     * a rank 0 dataset could be used, and where it varies, BIN_MAX and BIN_MINUS
     * are preferred.
     * @see #DELTA_PLUS for one-standard deviation confidence interval.  
     * @see #BIN_MAX
     */
    public final static String BIN_PLUS="BIN_PLUS";

    /**
     * QDataSet of rank 0 or correlated QDataSet identifies boundary. This is subtracted from the
     * measurements and should be interpreted as the lower limit of the 100% confidence interval 
     * where a measurement was collected.
     * @see #DELTA_MINUS for one-standard deviation confidence interval.
     */
    public final static String BIN_MINUS="BIN_MINUS";

    /**
     * QDataSet of rank 1 identifies boundary in the same units as the dataset. 
     * This should be interpreted as the upper limit of 100% confidence 
     * interval where a measurement was collected.  When this is found, BIN_PLUS
     * and BIN_MINUS should be ignored.
     * @see #BIN_PLUS which is the offset. 
     */
    public final static String BIN_MAX="BIN_MAX";

    /**
     * QDataSet of rank 1 identifies boundary in the same units as the dataset.
     * This should be interpreted as the lower limit of the 100% confidence 
     * interval where a measurement was collected.  When this is found, BIN_PLUS
     * and BIN_MINUS should be ignored.
     * @see #BIN_MINUS which is the offset. 
     */
    public final static String BIN_MIN="BIN_MIN";
    
    /**
     * name of the dataset in a bundle to be connected to BIN_MIN.
     */
    public final static String BIN_MIN_NAME="BIN_MIN_NAME";
    
    /**
     * name of the dataset in a bundle to be connected to BIN_MAX.
     */
    public final static String BIN_MAX_NAME="BIN_MAX_NAME";

    /**
     * name of the dataset in a bundle to be connected to BIN_MINUS.
     */
    public final static String BIN_MINUS_NAME="BIN_MINUS_NAME";
    
    /**
     * name of the dataset in a bundle to be connected to BIN_PLUS.
     */
    public final static String BIN_PLUS_NAME="BIN_PLUS_NAME";

    /**
     * name of the dataset in a bundle to be connected to DELTA_PLUS.
     */
    public final static String DELTA_PLUS_NAME="DELTA_PLUS_NAME";
    
    /**
     * name of the dataset in a bundle to be connected to DELTA_MINUS.
     */
    public final static String DELTA_MINUS_NAME="DELTA_MINUS_NAME";
    
    /**
     * CacheTag, indicating the coverage and resolution of a dimension.  This is 
     * an object that represents
     * the coverage and resolution of the interval covered.  For example, in Autoplot
     * the TimeSeriesBrowse uses this to keep track of what's already been read.
     */
    public final static String CACHE_TAG="CACHE_TAG";
    
    /**
     * String, hint as to preferred rendering method.  Examples include "spectrogram", "time_series", and "stack_plot", 
     * "nnSpectrogram", "hugeScatter", "series", "scatter", "colorScatter", "stairSteps", "fillToZero"
     * "digital", "image", "pitchAngleDistribution", "eventsBar".  Note these are just suggestions and are not
     * interpreted in the library.
     */
    public final static String RENDER_TYPE="RENDER_TYPE";
    
    /**
     * combining numbers is not allowed, and often nearest neighbor is a suitable result.
     */
    public static final String VALUE_AVERAGE_TYPE_NONE="none";
    
    /**
     * typical averages sum(ds)/len(ds)
     */
    public static final String VALUE_AVERAGE_TYPE_LINEAR="linear";
    
    /**
     * geometric mean where result is exp(sum(log(ds))/len(ds))
     */
    public static final String VALUE_AVERAGE_TYPE_GEOMETRIC="geometric";
    
    /**
     * mod24 mean where avg([23,1]) is 0.
     */
    public static final String VALUE_AVERAGE_TYPE_MOD24="mod24";
    
    /**
     * mod360 mean where avg([359,1]) is 0.
     */
    public static final String VALUE_AVERAGE_TYPE_MOD360="mod360";
    
    /**
     * modpi mean where avg( [5*PI/6,7*PI/6] ) is 0.
     */
    public static final String VALUE_AVERAGE_TYPE_MODPI="modpi";
    
    /**
     * modpi mean where avg( [5*TAU/6,7*TAU/6] ) is 0.
     */
    public static final String VALUE_AVERAGE_TYPE_MODTAU="modtau";
    
    /**
     * full-fidelity rendering of buckshot and connect-a-dot plots
     */
    public final static String VALUE_RENDER_TYPE_SERIES="series";
    
    /**
     * use blocks to draw each point, so data extents can be seen.
     */
    public final static String VALUE_RENDER_TYPE_NNSPECTROGRAM="nnSpectrogram";
    
    /**
     * draw events bars
     */
    public final static String VALUE_RENDER_TYPE_EVENTS_BAR="eventsBar";
    
    /**
     * values are drawn.
     */
    public final static String VALUE_RENDER_TYPE_DIGITAL="digital";

    /**
     * values are an RGB image, a rank 3 dataset [w,h,3] or [w,h,4].  The
     * "3" should be R,G, and B channels, and when "4" is used, ARGB is the
     * default.  There can be a DEPEND_2 that is a QDataSet with ordinal data,
     * specifying the channels like so Ops.labelsDataset(['a','b','g','r']) or
     * Ops.labelsDataset(['a','b','g','r']).  Only bgr or rgb models are supported
     * in the RGBImageRenderer, but future versions could support other color
     * models.
     */
    public final static String VALUE_RENDER_TYPE_COMPOSITE_IMAGE="image";

    /**
     * String, a java identifier that should can be used when an identifier is needed. This is
     * originally introduced for debugging purposes, so datasets can have a concise, meaningful name 
     * that is decoupled from the label. When NAMEs are used, properties with the same 
     * name should only refer to the named dataset.
     */
    public final static String NAME="NAME";
    
    /**
     * Boolean.TRUE indicates that the dataset is a "qube," meaning 
     * that all dimensions have fixed length and certain optimizations and 
     * operators are allowed.  Note that when DEPEND_1 is a rank 1 dataset,
     * this implies QUBE.  Likewise BUNDLE_1 is a qube.
     * Note the result of any slice must be a qube.
     */
    public final static String QUBE="QUBE";
    
    /**
     * String, representing the coordinate frame of the vector index.  The units
     * of a dataset should be EnumerationUnits which convert the data in this 
     * dimension to dimension labels that are understood in the coordinate frame
     * label context.  (E.g. X,Y,Z in GSM.)
     * (Note this is before BUNDLE dimensions were formalized and is not used.)
     */
    public final static String COORDINATE_FRAME="COORDINATE_FRAME";
    
    /**
     * Map&lt;String,Object&gt; representing additional properties used by client codes.  No
     * interpretation is done of these properties, but they are passed around as much
     * as possible.  Note Object can be String, Double, or Map&lt;String,Object&gt;.  METADATA_MODEL
     * is a string identifying the type of metadata,
     * a scheme for the metadata tree, such as ISTP-CDF or SPASE.
     */
    public final static String METADATA="METADATA";

    /**
     * String, a scheme for the metadata tree, such as ISTP or SPASE.  This should identify
     * a node's type when the node is present, but should not require that the node
     * be present.  When a required node is missing, this should be treated as if
     * none of the metadata is available.  This logic is to support aggregating
     * metadata.
     */
    public final static String METADATA_MODEL="METADATA_MODEL";

    /**
     * the metadata is ISTP-CDF metadata
     */
    public final static String VALUE_METADATA_MODEL_ISTP="ISTP-CDF";
    
    /**
     * the metadata is SPASE (Space Physics Archive Search Extract)
     */
    public final static String VALUE_METADATA_MODEL_SPASE="SPASE";

    /**
     * the value is a complex number, having two elements, the first is real second is imaginary.
     */
    public final static String VALUE_COORDINATE_FRAME_COMPLEX_NUMBER="ComplexNumber";
    
    /**
     * String, human consumable identifying data version.  Presently this 
     * is intended for human consumption, but eventually we may make them usable 
     * by software as well.  Note if multiple versions go into making a product 
     * (e.g. aggregation), the version string should contain space-delimited 
     * version ids, so note versions must not contain spaces for other purposes.  
     * Also two version strings containing the same value can be coalesced.  If this
     * is prefixed with "&lt;scheme&gt;:", then this is to be interpreted as such:<ul>
     *  <li>sep: period-delimited list of numeric sorted: 2.2.0 &lt; 2.15.2 &lt; 10.2.0
     *  <li>alpha: alpha-numeric sorted: 20030202B&gt;20030202A
     * </ul>
     * otherwise it should be numerically sorted.
     * (see org.das2.fsm.FileStorageModel)
     */
    public final static String VERSION="VERSION";

    /**
     * String, Human-consumable string identifying the source of a dataset, such as the file or URI from
     * which it was read.  Clearly this is easily lost as processes are applied to the
     * data, but when no other source is involved in a process (excluding library code
     * itself), then the source should be preserved.
     */
    public final static String SOURCE="SOURCE";

    /**
     * QDataSet of events scheme, containing a list of messages encountered during processing that annotate the data.  
     * For example, the AggregatingDataSource in Autoplot would add an event to the dataset when a file could not be used.
     * This is a rank 2 dataset with BUNDLE_1=startTime,stopTime,message for now, but may soon allow for bounding qubes:
     * BUNDLE_1=startTime,stopTime,startEn,stopEn,message, and this should be visualized via the EventsRenderer.  
     */
    public final static String NOTES="NOTES";

    /** Bundle Descriptor properties */

    /**
     * String, the name of another dataset in the bundle descriptor.  Before this was introduced,
     * a BundleDescriptor could have DEPEND_0 be a string.
     */
    public final static String DEPENDNAME_0="DEPENDNAME_0";

    /**
     * String, the name of another dataset in the bundle descriptor.  Before this was introduced,
     * a BundleDescriptor could have DEPEND_1 be a string.  Note this should only be used if
     * DEPEND_1 is rank 2, otherwise the dataset should be a property of DEPEND_1.  
     */
    public final static String DEPENDNAME_1="DEPENDNAME_1";

    /**
     * String, the name of the rank 2 or more dataset in a bundle descriptor.
     */
    public final static String ELEMENT_NAME="ELEMENT_NAME";

    /**
     * String, the label of the rank 2 or more dataset in a bundle descriptor.
     */
    public final static String ELEMENT_LABEL="ELEMENT_LABEL";

    /**
     * int array, the dimensions of the element.  A rank 0 is implicitly [], 
     * a rank 1, n by 1, would be [1].  This is similar to the size command, for
     * one record of the data.
     * @see org.das2.qds.ops.Ops#size(org.das2.qds.QDataSet) 
     */
    public final static String ELEMENT_DIMENSIONS="ELEMENT_DIMENSIONS";
    
    /**
     * Map&lt;String,Object&gt; representing additional properties used by client codes.  No
     * interpretation is done of these properties, but they are passed around as much
     * as possible.  The object values should be but don't have to be limited to: double, double array,
     * datum, QDataSet, String, String array.
     */
    public final static String USER_PROPERTIES="USER_PROPERTIES";

    /**
     * typical bin is min,max with min inclusive and max exclusive.
     */
    public static String VALUE_BINS_MIN_MAX="min,max";

    /**
     * scale type to suggest log axes and bins.
     */
    public static String VALUE_SCALE_TYPE_LOG="log";

    /**
     * scale type to suggest linear axes and bins.
     */
    public static String VALUE_SCALE_TYPE_LINEAR="linear";
    
    /**
     * the minimum length of each of the waveform packets in a rank 2 waveform dataset.
     */
    public static int MIN_WAVEFORM_LENGTH=32;
    
    /**
     * the fill value often used in codes.
     */
    public static double DEFAULT_FILL_VALUE= -1e31;

    /**
     * returns the rank of the dataset, which is the number of indeces used to access data.  Only rank 0, 1, 2, 3, and 4 datasets
     * are supported in the interface.   When a dataset's rank is 5 or greater, it should implement the HighRankDataSet interface
     * which affords a slice operation to reduce rank. 
     * @return the rank, or number of indeces used to access data.
     */
    int rank();
    
    /**
     * rank 0 accessor.
     * @throws IllegalArgumentException if the dataset is not rank 0.
     * @return the value, see the property UNITS to interpret this.
     */
    double value();

    /**
     * rank 0 accessor which provides the string value
     * @throws IllegalArgumentException if the dataset is not rank 0.
     * @return the value, see the property UNITS to interpret this.
     */
    String svalue();
    
    /**
     * rank 1 accessor.  
     * @param i the index
     * @return the value, see the property UNITS to interpret this.
     * @throws IllegalArgumentException if the dataset is not rank 1.
     */
    double value( int i );
    
    /**
     * rank 2 accessor.
     * @param i0 the index
     * @param i1 the index
     * @return the value, see the property UNITS to interpret this.
     * @throws IllegalArgumentException if the dataset is not rank 2.
     */
    double value( int i0, int  i1 );
    
    /**
     * rank 3 accessor.
     * @param i0 the index
     * @param i1 the index
     * @param i2 the index
     * @return the value, see the property UNITS to interpret this.     
     * @throws IllegalArgumentException if the dataset is not rank 3.
     */
    double value( int i0, int  i1, int i2 );

    /**
     * rank 4 accessor.
     * @param i0 the index
     * @param i1 the index
     * @param i2 the index
     * @param i3 the index
     * @return the value, see the property UNITS to interpret this.          
     * @throws IllegalArgumentException if the dataset is not rank 4.
     */
    double value( int i0, int i1, int i2, int i3);
    
    /**
     * accessor for properties attached to the dataset.  See final static members
     * for example properties.
     * @param name property name, such as "DEPEND_0" or "UNITS"
     * @return the value
     * @see #DEPEND_0
     * @see #UNITS
     */
    Object property( String name );
    
    /**
     * accessor for properties attached to the dataset's first index.  These properties
     * override (or shadow) properties attached to the dataset, and often implementations
     * will simply return the result of the no-index accessor.
     *
     * Note: properties of higher dimension are accessible only by slicing.
     * @param name property name, such as "DEPEND_0" or "UNITS"
     * @param i the index
     * @return the value
     * @see #DEPEND_0
     * @see #UNITS
     */    
    Object property( String name, int i );
    
    
    /**
     * return the length of the first dimension
     * @return the length of the first dimension
     */
    int length( );
    
    /**
     * return the length of the second dimension, for the ith element of the first dimension.  Note
     * if there are zero elements in the first dimension, but this is a QUBE, then this should not
     * throw an IndexOutOfBoundsException.
     * @param i the index
     * @return the length of the second dimension at index i.
     */
    int length( int i );

    /**
     * return the length of the third dimension, for the ith element of the first dimension and jth element of the second dimension.
     * @param i the index
     * @param j the index
     * @return the length of the third dimension at index i.
     */
    int length( int i, int j );

    /**
     * return the length of the fourth dimension for the ith, jth and kth elements of the first three dimensions.
     * @param i the index
     * @param j the index
     * @param k the index
     * @return the length of the fourth dimension for the ith, jth and kth elements of the first three dimensions.
     */
    int length( int i, int j, int k);

    /**
     * return a dataset that is a slice of this dataset, slicing on the zeroth 
     * dimension.
     * A slice will be the elements at this index, for example if this dataset
     * is a rank 2 dataset flux(Time,Energy) then the slice of this will be
     * a rank 1 dataset flux(Energy).
     * The result of any slice will be a qube.  Note the index must be positive.  
     * Negative indices, referenced from the end of the dataset, are not supported.
     * @throws IllegalArgumentException when dataset rank is zero.
     * @param i the index to slice at
     * @return the rank n-1 QDataSet at index i.
     */
    QDataSet slice( int i );

    /**
     * return a dataset that is a subset of this dataset.
     * For example:
     * <pre>
     * {@code
     *    ds= DDataSet.createRank1(100);
     *    QDataSet trim= ds.trim(50,60);
     *    assert( trim.length()==10 );
     * }
     * </pre> 
     * Note start and end must be positive.  Negative indices,
     * referenced from the end of the dataset, are not supported here.
     * @param start  the first index to be included in the new dataset.
     * @param end the exclusive index indicating the last index.
     * @return a QDataSet with the same rank but fewer elements.
     */
    QDataSet trim( int start, int end );

    /**
     * return null or an object implementing the capability for the given interface
     * For example:
     *<blockquote><pre>
     *ds= DDataSet.createRank1(100);
     *WriteCapability write= ds.capability( WriteCapability.class );
     *write.putValue( 99, -1e31 );
     *</pre></blockquote>
     * This allows operations to be performed efficiently.  Note there is no WriteCapability class, this is 
     * just an example.
     * 
     * @param <T> a capability
     * @param clazz the class, such as WriteCapability.class
     * @return the implementing class, or null (Jython None) if the capability is not provided.
     */
    <T> T capability( Class<T> clazz );
        
}