/* File: DataSet.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on October 24, 2003, 11:23 AM
 *      by Edward West <eew@space.physics.uiowa.edu>
 *
 * This file is part of the das2 library.
 *
 * das2 is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.das2.dataset;

import org.das2.datum.Units;
import org.das2.datum.Datum;
import java.util.*;

/** General interface for objects encapsulating a data set
 *
 * @author  Edward West
 */
public interface DataSet {
    
    /**
     * CacheTag object describing the start, end, and resolution of the dataset. 
     */
    final static String PROPERTY_CACHE_TAG= "cacheTag";
    
    /**
     * Long estimating the size of the dataset in memory.  For example, if a dataset is
     * backed by a local file, then zero for this indicates no penalty for storing this
     * dataset. 
     */
    final static String PROPERTY_SIZE_BYTES= "sizeBytes";
    
    /**
     * Datum which is the nominal distance between successive xTags.  This is used for example to prevent
     * interpolation between distant measurements 
     */
    final static String PROPERTY_X_TAG_WIDTH= "xTagWidth";
    
    /**
     * Datum, see xTagWidth 
     */
    final static String PROPERTY_Y_TAG_WIDTH= "yTagWidth";    
    
    /**
     * DatumRange useful for setting scales
     */
    final static String PROPERTY_X_RANGE="xRange";
	 
	 /** Double, used to indicate minimum valid X value */
	 final static String PROPERTY_X_VALID_MIN="xValidMin";
	 
	 /** Double, used to indicate maximum valid X value */
	 final static String PROPERTY_X_VALID_MAX="xValidMax";
    
    /** Datum, useful for setting scales  */
    final static String PROPERTY_Y_RANGE="yRange";
    
	 /** Double, used to indicate minimum valid X value */
	 final static String PROPERTY_Y_VALID_MIN="yValidMin";
	 
	 /** Double, used to indicate maximum valid X value */
	 final static String PROPERTY_Y_VALID_MAX="yValidMax";
	 
    /** DatumRange useful for setting scales */
    final static String PROPERTY_Z_RANGE="zRange";
	 
	 /** Double, used to indicate minimum valid X value */
	 final static String PROPERTY_Z_VALID_MIN="zValidMin";
	 
	 /** Double, used to indicate maximum valid X value */
	 final static String PROPERTY_Z_VALID_MAX="zValidMax";
	 
	 /** Double: Raw value used to indicate fill data. */
	 final static String PROPERTY_Y_FILL="yFill";
	 
	 /** Raw value used to indicate fill data.
	  * Since yscan's are rectangular it's handy to have a fill value to indicate
	  * gaps in the rectangle
	  */
	 final static String PROPERTY_Z_FILL="zFill";
    
    /**
     * suggest render method to use.  These are 
     * canonical:
     *    spectrogram
     *    symbolLine
     *    stackedHistogram
     */
    final static String PROPERTY_RENDERER="renderer";
    
    /**
     * String "log" or "linear" 
     */
    final static String PROPERTY_Y_SCALETYPE="yScaleType";
        
    /**
     * String "log" or "linear" 
     */
    final static String PROPERTY_Z_SCALETYPE="zScaleType";
        
    final static String PROPERTY_X_LABEL="xLabel";
    
    final static String PROPERTY_Y_LABEL="yLabel";
    
    final static String PROPERTY_Z_LABEL="zLabel";
	 
	 /** A brief description of the x direction values */
	 final static String PROPERTY_X_SUMMARY="xSummary";
	 
	 /** A brief description of the y direction values */
	 final static String PROPERTY_Y_SUMMARY="ySummary";
	 
	 /** A brief description of the z direction values */
	 final static String PROPERTY_Z_SUMMARY="zSummary";
	 
	 /** A brief description for the entire stream */
	 final static String PROPERTY_SUMMARY="summary";

    /**
     * finally, this data model is done with the addition of title.
     */
    final static String PROPERTY_TITLE="title";

    /** Boolean assuring that the dataset is monotonic in X.  This allows 
     * some optimizations to be made. */
    final static String PROPERTY_X_MONOTONIC="xMonotonic";
	 
	 /** Boolean assuring that the dataset is monotonic in Y.  This allows 
     * some optimizations to be made. */
    final static String PROPERTY_Y_MONOTONIC="yMonotonic";
	 
    /**
     * dataset containing the peaks when available
     */
    final static String PROPERTY_PLANE_PEAKS= "peaks";
    
    /**
     * dataset containing the weights when available
     */
    final static String PROPERTY_PLANE_WEIGHTS= "weights";
    
    /**
     * DatumFormatter for formatting data in the dataset.
     */
    public static String PROPERTY_FORMATTER= "formatter";
    
    /** Returns the property value associated with the string <code>name</code>
     * @param name the name of the property requested
     * @return the property value for <code>name</code> or null
     */
    Object getProperty(String name);
    
    /** Returns all dataset properties in a Map.
     * @return a Map of all properties.
     */
    Map getProperties();
    
    /** Returns the Units object representing the unit type of the x tags
     * for this data set.
     * @return the x units
     */
    Units getXUnits();
    
    /** Returns the Units object representing the unit type of the y tags
     * or y values for this data set.
     * @return the y units
     */
    Units getYUnits();
    
    /** Returns the value of the x tag at the given index i as a
     *      <code>Datum</code>.
     * @param i the index of the requested x tag
     * @return the value of the x tag at the given index i as a
     *      <code>Datum</code>.
     */
    Datum getXTagDatum(int i);
    
    /** Returns the value of the x tag at the given index i as a
     *      <code>double</code> in the given units.  XTags must be
     *      monotonically increasing with i.
     * @return the value of the x tag at the given index i as a
     *      <code>double</code>.
     * @param units the units of the returned value
     * @param i the index of the requested x tag
     */
    double getXTagDouble(int i, Units units);
    
    /** Returns the value of the x tag at the given index i as an
     *      <code>int</code> in the given units.  XTags must be
     *      monotonically increasing with i.
     * @return the value of the x tag at the given index i as an
     *      <code>int</code>.
     * @param units the units of the returned value.
     * @param i the index of the requested x tag
     */
    int getXTagInt(int i, Units units);

    /** Returns the number of x tags in this data set.  XTags must be
     *      monotonically increasing with i.
     * @return the number of x tags in this data set.
     */
    int getXLength();
    
    /** Returns a <code>DataSet</code> with the specified view as the primary
     * view.
     * @param planeID the <code>String</code> id of the requested plane.
     * @return the specified view, as a <code>DataSet</code>
     */
    //TODO: consider throwing IllegalArgumentException if the plane doesn't exist.
    //   we have methods to query for the plane names.
    DataSet getPlanarView(String planeID);
    
    /** 
     * Returns a list of auxiliary planes (e.g. weights, peaks) for the dataset.
     * Note that the default plane, "" may or may not be returned, based on
     * implementations.
     */
    public String[] getPlaneIds();
    
}
