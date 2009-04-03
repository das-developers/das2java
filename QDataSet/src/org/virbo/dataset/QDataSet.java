/*
 * QDataSet.java
 *
 * Created on January 25, 2007, 9:12 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.virbo.dataset;

/**
 * <p>QDataSets are less abstract and more flexible data model for das2.  das2's 
 * current data model was developed to deliver spectrogram time series data sets
 * where the dataset structure would change over time, and the interface is highly
 * optimized for that environment.  It's difficult to express many datasets in these
 * terms, so the simpler "quick" QDataSet was introduced.  </p>
 *
 * <p>The QDataSet can be thought of as a fast java array that has name-value metadata
 * attached to it.  These arrays of data can have arbitrary rank, although currently
 * the interface limits rank to 1,2, and 3.  (Rank 0 and Rank N are proposed but not
 * developed.)  Each dimension's length can vary, like java arrays, and datasets 
 * where the dimensions do not vary in length are colloquially called "Qubes."</p>
 *
 * <p>QDataSets can have other QDataSets as property values, for example the property
 * QDataSet.DEPEND_0 indicates that the values are dependend parameters of the "tags"
 * QDataSet found there.  This how how we get to the same abstraction level of 
 * the legacy das2 dataset.  </p>
 * 
 * <p>This is inspired by the CDF data model and PaPCo's dataset model.</p>
 *
 * <p>See http://www.das2.org/wiki/index.php/Das2_DataSet_Abstraction_3.0</p>
 *
 * @author jbf
 */
public interface QDataSet {
    
    /**
     * type QDataSet.  This dataset is a dependent parameter of the independent parameter represented in this DataSet.
     * The tags for the DataSet's 0th index are identified by this tags dataset.
     */
    public final static String DEPEND_0="DEPEND_0";
    
    /**
     * type QDataSet.  This dataset is a dependent parameter of the independent parameter represented in this DataSet.
     * The tags for the DataSet's 1st index are identified by this tags dataset.  Note this is not very mature code, and
     * it's probably safe to assume there will be problems with this for datasets where DEPEND_1 is dependent on the 0th index.
     * The legacy das2 DataSet API had the abstraction of tables, and this may be replaced by a bundling dimension or metadata
     * that indicates where DEPEND_1 changes.  For now, correct codes should should assume that this changes with each 0th index 
     * value, and a helper method should be introduced that finds the geometry changes.
     */
    public final static String DEPEND_1="DEPEND_1";
    
    /**
     * type QDataSet.  This dataset is a dependent parameter of the independent parameter represented in this DataSet.
     * The tags for the DataSet's 2nd index are identified by this tags dataset.
     */
    public final static String DEPEND_2="DEPEND_2";
    
    /**
     * type QDataSet. Correllated plane of data.  An additional dependent DataSet that is correllated by the first index.  
     * Note "0" is just a count, and does not refer to the 0th index.  All correllated datasets must be 
     * correllated by the first index.  TODO: what about two rank 2 datasets?
     */
    public final static String PLANE_0= "PLANE_0";

    /**
     * this is the maximum number of allowed planes.  This should be used to enumerates all the planes.
     */
    public final static int MAX_PLANE_COUNT=50;

    /**
     * this is the highest rank supported by the library.  Rank 0 is supported though Rank0DataSet.  High rank datasets are supported through
     * RankNDataSet.
     * 
     */
    public static int MAX_RANK=3;
    
    /**
     * type Units.  The dataset units, found in org.das2.units.Units.
     */
    public final static String UNITS="UNITS";

    /**
     * Double, Double value to be considered fill (invalid) data.
     */
    public final static String FILL_VALUE="FILL_VALUE";
        
    /**
     * type double.  Range bounding measurements to be considered valid.  Lower
     * and Upper bounds are inclusive.  FILL_VALUE should be used to make the 
     * lower bound or upper bound exclusive.  Note DatumRange contains logic is
     * exclusive on the upper bound.
     */
    public final static String VALID_MIN="VALID_MIN";
    
    /**
     * type double.  Range bounding measurements to be considered valid.  Lower
     * and Upper bounds are inclusive.  FILL_VALUE should be used to make the 
     * lower bound or upper bound exclusive.  Note DatumRange contains logic is
     * exclusive on the upper bound.
     */
    public final static String VALID_MAX="VALID_MAX";
        
    /**
     * type double.  Range used to discover datasets.  This should be a reasonable representation
     * of the expected dynamic range of the dataset.
     */    
    public final static String TYPICAL_MIN="TYPICAL_MIN";

    /**
     * type double.  Range used to discover datasets.  This should be a reasonable representation
     * of the expected dynamic range of the dataset.
     */    
    public final static String TYPICAL_MAX="TYPICAL_MAX";
    
    /**
     * String "linear" or "log"
     */
    public final static String SCALE_TYPE="SCALE_TYPE";
    
    /**
     * String, Concise Human-consumable label suitable for a plot label. (10 chars)
     */
    public final static String LABEL="LABEL";
    
    /**
     * String, Human-consumable string suitable for a plot title. (100 chars).
     */
    public final static String TITLE="TITLE";
    
    /**
     * Boolean, Boolean.TRUE if dataset is monotonically increasing.  Also, the data must not contain 
     * invalid values.  Generally this will be used with tags datasets.
     */
    public final static String MONOTONIC="MONOTONIC";
            
    /**
     * QDataSet, dataset of same geometry that indicates the weights for each point.  Often weights are computed
     * in processing, and this is where they should be stored for other routines.  When the weights plane is 
     * present, routines can safely ignore the FILL_VALUE, VALID_MIN, and VALID_MAX properties, and use non-zero weight to 
     * indicate valid data.  Further, averages of averages will compute accurately.
     */
    public final static String WEIGHTS_PLANE="WEIGHTS";
    
    /**
     * RankZeroDataSet, the expected distance between successive measurements where it is valid to make inferences about the data.
     * For example, interpolation is disallowed for points 1.5*CADENCE apart.
     * This property only makes sense with a tags dataset.
     */
    public final static String CADENCE="CADENCE";
    
    /**
     * QDataSet of rank 0 or correlated plane that limits accuracy.  Integration
     * intervals should be indicated here, as well as measurement uncertainty.
     * TODO: break this into BIN_PLUS and ERROR_PLUS to distinguish between
     * errors and integration intervals, or introduce "BINS" type dimension.
     */
    public final static String DELTA_PLUS="DELTA_PLUS";
    
    /**
     * QDataSet of rank 0 or correlated plane limits limits accuracy.  Integration
     * intervals should be indicated here, as well as measurement uncertainty.
     */
    public final static String DELTA_MINUS="DELTA_MINUS";
    
    /**
     * CacheTag, to be attached to tags datasets.
     */
    public final static String CACHE_TAG="CACHE_TAG";
    
    /**
     * Hint as to preferred rendering method.  Examples include "spectrogram", "time_series", and "stack_plot"
     */
    public final static String RENDER_TYPE="RENDER_TYPE";
    
    /**
     * String a java identifier that should can be used when an identifier is needed. This is 
     * originally introduced for debugging purposes, so datasets can have a concise, meaningful name 
     * that is decoupled from the label. When NAMEs are used, properties with the same 
     * name should only refer to the named dataset.
     */
    public final static String NAME="NAME";
    
    /**
     * Boolean.TRUE indicates that the dataset is a "qube," meaning 
     * that all dimensions have fixed length and certain optimizations and operators are allowed. 
     */
    public final static String QUBE="QUBE";
    
    /**
     * String representing the coordinate frame of the vector index.  The units 
     * of a dataset should be EnumerationUnits which convert the data in this 
     * dimension to dimension labels that are understood in the coordinate frame
     * label context.  (E.g. X,Y,Z in GSM.)
     */
    public final static String COORDINATE_FRAME="COORDINATE_FRAME";
    
    /**
     * Map<String,Object> representing additional properties used by client codes.  No
     * interpretation is done of these properties, but they are passed around as much
     * as possible.
     */
    public final static String USER_PROPERTIES="USER_PROPERTIES";
    
    /**
     * returns the rank of the dataset, which is the number of indeces used to access data.  Only rank 1, 2, and 3 datasets
     * are supported in the interface.   When a dataset's rank is 4 or greater, it should implement the HighRankDataSet interface
     * which affords a slice operation to reduce rank.  When a dataset's rank is 0, it should implement the RankZeroDataSet interface,
     * which has a no-argument accessor.  (TODO: Note that rank 0 and rank N have very limited use, so many routines aren't 
     * coded to handle them.)
     */
    int rank();
    
    /**
     * rank 0 accessor.
     * @throws IllegalArgumentException if the dataset is not rank 0.
     * @return
     */
    double value();

    /**
     * rank 1 accessor.  
     * @throws IllegalArgumentException if the dataset is not rank 1.
     */
    double value( int i );
    
    /**
     * rank 2 accessor.
     * @throws IllegalArgumentException if the dataset is not rank 2.
     */
    double value( int i0, int  i1 );
    
    /**
     * rank 3 accessor.
     * @throws IllegalArgumentException if the dataset is not rank 2.
     */
    double value( int i0, int  i1, int i2 );
    
    /**
     * accessor for properties attached to the dataset.  See final static members
     * for example properties.
     */
    Object property( String name );
    
    /**
     * accessor for properties attached to the dataset's first index.  These properties
     * override (or shadow) properties attached to the dataset, and often implementations
     * will simply return the result of the no-index accessor.
     */    
    Object property( String name, int i );
    
    /**
     * accessor for properties attached to the dataset's second index.  These properties
     * override (or shadow) properties attached to the dataset's first index, and 
     * often implementations will simply return the result of the one-index (or zero-index) 
     * accessor.
     */        
    Object property( String name, int i0, int i1 );
    
    /**
     * return the length of the first dimension
     */
    int length( );
    
    /**
     * return the length of the second dimension, for the ith element of the first dimension
     */
    int length( int i );

    /**
     *return the length of the third dimension, for the ith element of the first dimension and jth element of the second dimension.
     */
    int length( int i, int j );
        
}