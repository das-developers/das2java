/* File: CachedDataSetReader.java
 * Copyright (C) 2002-2003 University of Iowa
 * Created by: Jeremy Faden <jbf@space.physics.uiowa.edu>
 *             Jessica Swanner <jessica@space.physics.uiowa.edu>
 *             Edward E. West <eew@space.physics.uiowa.edu>
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

package edu.uiowa.physics.pw.das.dataset;

import edu.uiowa.physics.pw.das.server.DataSetReader;
import edu.uiowa.physics.pw.das.util.DasDate;

import java.io.*;
import java.util.*;

/**
 * This class implements a data set reader that keeps a
 * cache of previous data sets that have been loaded by this reader
 * and attempts to use the cache to avoid querying the server
 *
 * @author Edward West
 */
public class CachedDataSetReader implements DataSetReader {





    /** The underlying DataSetReader this object queries for data */
    protected DataSetReader reader;


    /**
     * An ordered set containing <code>DataSetDescriptor</code>s
     * that are ordered by startTime.
     */
    private SortedSet startTimeSet;


    /**
     * An ordered set containing <code>DataSetDescriptor</code>s
     * that are ordered by endTim
     */
    private SortedSet endTimeSet;







    /**
     * Creates a new instance of <code>CachedDataSetReader</code> that
     * wraps the specifed <code>DataSetReader</code>
     *
     * @param reader the specified <code>DataSetReader</code>
     */
    public CachedDataSetReader(DataSetReader reader) {
	this.reader = reader;
	this.startTimeSet = new TreeSet(new StartTimeComparator());
	this.endTimeSet = new TreeSet(new EndTimeComparator());
    }







    /**
     * Returns a <code>DataSet</code> object
     *
     * This method first checks to see if a data set exists in the cache
     * that can be used to derive the result.  If not, this method will query
     * the underlying reader for an appropriate dataset
     *
     * @param dsdfPath the path to the data set description file relative to datasetroot
     * @param startTime a <code>DasDate</code> object specifying the start time for the interval requested
     * @param endTime a <code>DasDate</code> object specifiying the end time for the interval requested
     * @param param a place for extra parameters
     */
    public DataSet getDataSet(String dsdfPath, Object param, DasDate startTime, DasDate endTime) throws IOException {
	System.out.println("HERE");
	DataSet ds = restore(dsdfPath, param, startTime, endTime);
	if (ds == null) {
	    ds = reader.getDataSet(dsdfPath, param, startTime, endTime);
	    DataSetDescriptor d = new DataSetDescriptor();
	    d.dsdfPath = dsdfPath;
	    d.param = (param==null ? "" : param.toString());
	    d.startTime = startTime;
	    d.endTime = endTime;
	    File tempFile = File.createTempFile("das2.",".obj");
	    System.out.print("Temp file created: ");
	    System.out.println(tempFile.getCanonicalPath());
	    ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(tempFile));
	    out.writeObject(ds);
	    out.flush();
	    out.close();
	    d.filename = tempFile.getCanonicalPath();
	    startTimeSet.add(d);
	    endTimeSet.add(d);
	}
	System.out.print("Contents of startTimeSet: ");
	System.out.println(startTimeSet);
	System.out.print("Contents of endTimeSet: ");
	System.out.println(endTimeSet);
	return ds;
    }









    /**
     * Restores a data set if a copy of that data set is in the cache.
     *
     * @param dsdfPath the path to the data set description file relative to datasetroot
     * @param startTime a <code>DasDate</code> object specifying the start time for the interval requested
     * @param endTime a <code>DasDate</code> object specifiying the end time for the interval requested
     * @param param a place for extra parameters
     */
    public DataSet restore(String dsdfPath, Object param, DasDate startTime, DasDate endTime) throws IOException {
	DataSetDescriptor d = new DataSetDescriptor();
	d.dsdfPath = dsdfPath;
	d.param = (param==null ? "" : param.toString());
	d.startTime = startTime;
	d.endTime = endTime;
	Set s = new HashSet(startTimeSet.tailSet(d));
	s.retainAll(endTimeSet.tailSet(d));
	if (s.isEmpty()) return null;
	DataSetDescriptor d2 = (DataSetDescriptor)s.iterator().next();
	ObjectInputStream in = new ObjectInputStream(new FileInputStream(d2.filename));
	DataSet ds = null;
	try {
	    ds = (DataSet)in.readObject();
	}
	catch (ClassNotFoundException cnfe) {
	    cnfe.printStackTrace();
	}
	in.close();
	//jbf ds.setTimeBase(startTime);
	return ds;
    }









    /**
     * Tests to see if a data set is in the cache.
     *
     * @param dsdfPath the path to the data set description file relative to datasetroot
     * @param startTime a <code>DasDate</code> object specifying the start time for the interval requested
     * @param endTime a <code>DasDate</code> object specifiying the end time for the interval requested
     * @param param a place for extra parameters
     */
    public boolean isSaved(String dsdfPath, Object param, DasDate startTime, DasDate endTime) {
	DataSetDescriptor d = new DataSetDescriptor();
	d.dsdfPath = dsdfPath;
	d.param = param.toString();
	d.startTime = startTime;
	d.endTime = endTime;
	if (startTimeSet.contains(d)) return true;
	Set s = new HashSet(startTimeSet.tailSet(d));
	s.retainAll(endTimeSet.tailSet(d));
	return !s.isEmpty();
    }

    public void addDasReaderListener(edu.uiowa.physics.pw.das.event.DasReaderListener listener) {
        reader.addDasReaderListener(listener);
    }

    public void removeDasReaderListener(edu.uiowa.physics.pw.das.event.DasReaderListener listener) {
        reader.removeDasReaderListener(listener);
    }







    /**
     * This inner class describes a particular data set and also contains the 
     * name of the file that particular data set is stored in.
     */
    protected class DataSetDescriptor {
	public String dsdfPath;
	public String param;
	public DasDate startTime;
	public DasDate endTime;
	public String filename;
    }









    /**
     * This inner class defines a <code>Comparator</code> for
     * <code>DataSetDescriptor</code> instances that orders them
     * by the <code>startTime</code> property.
     */
    protected class StartTimeComparator implements Comparator {
	
	/**
	 * Compares two arguments for order.
	 *
	 * The order of the arguements is determined by the order of
	 * the <code>startTime</code> property of each argument.
	 * this method should only be called with <code>DataSetDescriptor</code>
	 * arguments.
	 *
	 * @param o1 the first object to be compared
	 * @param o2 the second object to be compard
	 * @return a negative integer, zero, or a positive integer as the first
	 *    argument is less than, equal to, or greater than the second.
	 * @throws java.lang.ClassCastException if either (o1 instanceof DataSetDescriptor) or
	 *    (o2 instanceof DataSetDescriptor) evaluate to false
	 * @see edu.uiowa.physics.pw.das.util.DasDate#compareTo(java.lang.Object)
	 */
	public int compare(Object o1, Object o2) {
	    DataSetDescriptor d1 = (DataSetDescriptor)o1;
	    DataSetDescriptor d2 = (DataSetDescriptor)o2;
	    if (d1.dsdfPath.equals(d2.dsdfPath)) {
		return d1.startTime.compareTo(d2.startTime)*(-1);
	    }
	    return d1.dsdfPath.compareTo(d2.dsdfPath)*(-1);
	}

    }









    /**
     * This inner class defines a <code>Comparator</code> for
     * <code>DataSetDescriptor</code> instances that orders them
     * by the <code>endTime</code> property.
     */
    protected class EndTimeComparator implements Comparator {
	
	/**
	 * Compares two arguments for order.
	 *
	 * The order of the arguements is determined by the order of
	 * the <code>endTime</code> property of each argument.
	 * this method should only be called with <code>DataSetDescriptor</code>
	 * arguments.
	 *
	 * @param o1 the first object to be compared
	 * @param o2 the second object to be compard
	 * @return a negative integer, zero, or a positive integer as the first
	 *    argument is less than, equal to, or greater than the second.
	 * @throws java.lang.ClassCastException if either <code>(o1 instanceof DataSetDescriptor)</code>
	 *     or <code>(o2 instanceof DasDate)</code> evaluate to false
	 * @see edu.uiowa.physics.pw.das.util.DasDate#compareTo(java.lang.Object)
	 */
	public int compare(Object o1, Object o2) {
	    DataSetDescriptor d1 = (DataSetDescriptor)o1;
	    DataSetDescriptor d2 = (DataSetDescriptor)o2;
	    if (d1.dsdfPath.equals(d2.dsdfPath)) {
		return d1.endTime.compareTo(d2.endTime);
	    }
	    return d1.dsdfPath.compareTo(d2.dsdfPath);
	}

    }









}
