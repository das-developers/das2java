/* File: DataSetStreamHandler.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on February 11, 2004, 4:26 PM
 *      by Edward E. West <eew@space.physics.uiowa.edu>
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

package org.das2.client;

import org.das2.stream.StreamYScanDescriptor;
import org.das2.stream.StreamComment;
import org.das2.stream.StreamScalarDescriptor;
import org.das2.stream.StreamDescriptor;
import org.das2.stream.StreamException;
import org.das2.stream.StreamHandler;
import org.das2.stream.SkeletonDescriptor;
import org.das2.stream.PacketDescriptor;
import org.das2.util.monitor.NullProgressMonitor;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.datum.CacheTag;
import org.das2.dataset.DataSet;
import org.das2.dataset.TableDataSetBuilder;
import org.das2.dataset.VectorDataSetBuilder;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumRangeUtil;
import org.das2.datum.DatumUtil;
import org.das2.datum.DatumVector;
import org.das2.datum.TimeUtil;
import org.das2.datum.Units;
import org.das2.system.DasLogger;
import java.text.ParseException;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Edward E. West
 */
public class DataSetStreamHandler implements StreamHandler {
    
    StreamHandlerDelegate delegate;
    StreamDescriptor sd;
    Map extraProperties;
    ProgressMonitor monitor;
    int totalPacketCount= -1;
    int taskSize= -1;
    int packetCount= 0;
    Datum xTagMax= null;
	 boolean bReadPkts = true;
    
    private static final Logger logger= DasLogger.getLogger(DasLogger.DATA_TRANSFER_LOG);
    private boolean monotonic= true; // generally streams are monotonic, so check for monotonicity.
    
    /**
     * create a DataSetStreamHandler.
     * @param extraProperties
     * @param monitor atypical use of the monitor, where its started and setTaskProgress methods are called, but not its finished.
     */
    public DataSetStreamHandler( Map extraProperties, ProgressMonitor monitor ) {
        this.extraProperties = new HashMap(extraProperties);
        this.monitor= monitor==null ? new NullProgressMonitor() : monitor;
    }
    
	 @Override
    public void streamDescriptor(StreamDescriptor sd) throws StreamException {
        logger.finest("got stream descriptor");
        this.sd = sd;
        Object o;
        if ( ( o= sd.getProperty("taskSize") )!=null ) {
            this.taskSize= ((Integer)o);
            monitor.setTaskSize( taskSize );
            monitor.started();
        } else if ( ( o= sd.getProperty("packetCount" ) )!=null ) {
            this.totalPacketCount= ((Integer)o);
            monitor.setTaskSize( totalPacketCount );
            monitor.started();
        }
        if ( ( o= sd.getProperty("cacheTagString" ) ) !=null ) {
            // kludge to xmit cacheTags.  "start,resolution,end"
            try {
                String[] ss= ((String)o).split(",");
                Datum min= TimeUtil.create(ss[0]);
                Datum max= TimeUtil.create(ss[2]);
                Datum res= DatumUtil.parse(ss[1]);
                if ( res.doubleValue( res.getUnits() ) == 0 ) res= null; // intrinsic resolution
                extraProperties.put( DataSet.PROPERTY_CACHE_TAG, new CacheTag( min, max, res ) );
            } catch ( ParseException e ) {
                e.printStackTrace();
            }
        }
		  else{
			  // Get the cacheTag via the new parameters, *CacheRange *CacheResolution. 
			  // Don't use X_RANGE and X_TAG_WIDTH because these are ment for display 
			  // purposes and don't represent the coverage period and resolution directly.
			  Object oRng;
			  Object oRes;
			  if((oRng = sd.getProperty(DataSet.PROPERTY_X_CACHE_RNG)) != null){
				  try{
					  DatumRange rng = null;
					  if(!(oRng instanceof DatumRange))
				         rng = DatumRangeUtil.parseDatumRange((String)oRng);
					  else
						   rng = (DatumRange)oRng;
					  
						Datum res = null; // intrinsic resolution
				      if((oRes = sd.getProperty(DataSet.PROPERTY_X_CACHE_RES)) != null){
							if(!(oRes instanceof Datum))
				          res = DatumUtil.parse((String) oRes);
							else
							 res = (Datum)oRes;
						}

					    extraProperties.put(DataSet.PROPERTY_CACHE_TAG,
					                         new CacheTag(rng.min(), rng.max(), res));
				  }
				  catch(ParseException e){
					  e.printStackTrace();
				  }
			  }
		  }
        if ( ( o=sd.getProperty( DataSet.PROPERTY_X_MONOTONIC ) )!=null ) {
            extraProperties.put( DataSet.PROPERTY_X_MONOTONIC, Boolean.valueOf((String)o) );
            
        }
        if ( ( o=sd.getProperty("pid") )!=null ) {
            logger.log(Level.FINE, "stream pid={0}", o);
        }
    }
    
    @Override
    public void packetDescriptor(PacketDescriptor pd) throws StreamException {
        logger.finest("got packet descriptor");
        if (delegate == null) {
            SkeletonDescriptor descriptor = pd.getYDescriptor(0);
            if (descriptor instanceof StreamScalarDescriptor) {
                logger.fine("using VectorDS delegate");
                delegate = new VectorDataSetStreamHandler(pd);
            } else if (descriptor instanceof StreamYScanDescriptor) {
                logger.fine("using TableDS delegate");
                delegate = new TableDataSetStreamHandler(pd);
            }
        } else {
            delegate.packetDescriptor(pd);
        }
    }

	 public void setReadPackets(boolean b){
		 bReadPkts = b;
	 }

	 public boolean getReadPackets(){
		 return bReadPkts;
	 }
    
    @Override
    public void packet(PacketDescriptor pd, Datum xTag, DatumVector[] vectors) throws StreamException {
		 if(!bReadPkts) return;
		 
        logger.finest("got packet");
        ensureNotNullDelegate();
        if ( xTagMax==null || xTag.ge( this.xTagMax ) ) {
            xTagMax= xTag;
        } else {
            monotonic= false;
        }
        delegate.packet(pd, xTag, vectors);
        packetCount++;
        if ( totalPacketCount!=-1 ) {
            monitor.setTaskProgress(packetCount);
        }
    }
    
    @Override
    public void streamClosed(StreamDescriptor sd) throws StreamException {
        logger.finest("got streamClosed");
        if (delegate != null) {
            delegate.streamClosed(sd);
        }
    }
    
    @Override
    public void streamException(StreamException se) throws StreamException {
        logger.finest("got stream exception");
    }
    
    @Override
    public void streamComment(StreamComment sc) throws StreamException {
		logger.log(Level.FINEST, "got stream comment: {0}", sc);
		  		  
		if (sc.getType().equals(sc.TYPE_TASK_SIZE)){
			if(! monitor.isCancelled()){
				taskSize = Integer.parseInt(sc.getValue());
                                monitor.setTaskSize(taskSize);
                                monitor.started();
			}
			return;
		}
		  
		if ( sc.getType().equals(sc.TYPE_TASK_PROGRESS) ) {  
         if ( taskSize != -1 && !monitor.isCancelled() ) 
				monitor.setTaskProgress( Long.parseLong(sc.getValue() ) );
			return;
		}
		  
		  if ( sc.getType().matches(sc.TYPE_LOG) ) {
            String level= sc.getType().substring(4);
            Level l= Level.parse(level.toUpperCase());
            if ( l.intValue()>Level.FINE.intValue() ) {
                logger.log(Level.FINE,sc.getValue());                
            } else {
                logger.log(l,sc.getValue());
            }
            monitor.setProgressMessage(sc.getValue());
        }
    }
    
    /**
     * return the dataset collected or null if no packets were received.
     * @return the dataset collected or null if no packets were received.
     */
    public DataSet getDataSet() {
        if (delegate == null) {
            System.err.println("never established delegate, which might mean the stream contains no packets.");
            return null;
        } else {
            return delegate.getDataSet();
        }
    }
    
    private void ensureNotNullDelegate() {
        if (delegate == null) {
            throw new IllegalStateException("Null delegate");
        }
    }
    
    private static double getXWithBase(Datum base, Datum x) {
        if (base == null) {
            return x.doubleValue(x.getUnits());
        } else {
            return base.doubleValue(base.getUnits()) + x.doubleValue(base.getUnits().getOffsetUnits());
        }
    }
    
    private static interface StreamHandlerDelegate extends StreamHandler {
        DataSet getDataSet();
    }
    
    private class VectorDataSetStreamHandler implements StreamHandlerDelegate {
        
        private VectorDataSetBuilder builder;
        
        private DatumRange validRange=null;

        private VectorDataSetStreamHandler(PacketDescriptor pd) throws StreamException {
            StreamScalarDescriptor y = (StreamScalarDescriptor)pd.getYDescriptor(0);
            Datum base = pd.getXDescriptor().getBase();
            Units xUnits = base == null ? pd.getXDescriptor().getUnits() : base.getUnits();
            Units yUnits = y.getUnits();
            builder = new VectorDataSetBuilder(xUnits,yUnits);
            builder.addProperties( Collections.singletonMap( DataSet.PROPERTY_Y_LABEL, y.getProperty("name") ));
            for ( int i=1; i<pd.getYCount(); i++ ) {
                StreamScalarDescriptor smyd= (StreamScalarDescriptor)pd.getYDescriptor(i);
                builder.addProperties( Collections.singletonMap( smyd.getName()+ "." + DataSet.PROPERTY_Y_LABEL, smyd.getName()));
            }
            String srange= (String)y.getProperty("valid_range");
            if ( srange!=null ) {
                try {
                    validRange= DatumRangeUtil.parseDatumRange( srange, yUnits );
                } catch ( ParseException ex ) {
                    throw new StreamException("Unable to parse valid_range:"+srange);
                }
            }
            this.packetDescriptor(pd);
        }
        
        @Override
        public void packet(PacketDescriptor pd, Datum xTag, DatumVector[] vectors) throws StreamException {
            Datum base = pd.getXDescriptor().getBase();
            double x = getXWithBase(base, xTag);
            
            for (int i = 0; i < pd.getYCount(); i++) {
                if (pd.getYDescriptor(i) instanceof StreamScalarDescriptor) {
                    StreamScalarDescriptor my = (StreamScalarDescriptor)pd.getYDescriptor(i);
                    double y = vectors[i].doubleValue(0, my.getUnits());
                    if ( validRange!=null ) {
                        if ( !validRange.contains( Datum.create( y, my.getUnits() ) ) ) {
                            y= my.getUnits().getFillDouble();
                        }
                    }
                    if (i != 0) {
                        builder.insertY(x, y, my.getName());
                    } else {
                        builder.insertY(x, y);
                    }
                    Map<String,Object> props= my.getProperties();
                    for ( Entry<String,Object> p: props.entrySet() ) {
                        if ( i==0 ) {
                            builder.setProperty( p.getKey(), p.getValue() );                            
                        } else {
                            builder.setProperty( my.getName()+"."+p.getKey(), p.getValue() );
                        }
                    }
                } else {
                    throw new StreamException("Mixed data sets are not currently supported");
                }
            }
            //TODO: see TableDataSet below, where PacketDescriptor can have properties.
            
        }
        
        @Override
        public void packetDescriptor(PacketDescriptor pd) throws StreamException {
            logger.log(Level.FINE, "got packet descriptor: {0}", pd);
            for (int i = 1; i < pd.getYCount(); i++) {
                StreamScalarDescriptor y = (StreamScalarDescriptor)pd.getYDescriptor(i);
                builder.addPlane(y.getName(),y.getUnits());
            }
        }
        
        @Override
        public void streamClosed(StreamDescriptor sd) throws StreamException {}
        
        @Override
        public void streamDescriptor(StreamDescriptor sd) throws StreamException {}
        
        @Override
        public void streamException(StreamException se) throws StreamException {}
        
        @Override
        public void streamComment(StreamComment sc) throws StreamException {}
        
        @Override
        public DataSet getDataSet() {
            builder.addProperties(sd.getProperties());
            builder.addProperties(extraProperties);
            if ( monotonic&& builder.getProperty(DataSet.PROPERTY_X_MONOTONIC)==null ) {
                builder.setProperty( DataSet.PROPERTY_X_MONOTONIC, Boolean.TRUE );
            }
            return builder.toVectorDataSet();
        }
        
    }
    
    private class TableDataSetStreamHandler implements StreamHandlerDelegate {
        
        private TableDataSetBuilder builder;
        
        private TableDataSetStreamHandler(PacketDescriptor pd) throws StreamException {
            StreamYScanDescriptor y = (StreamYScanDescriptor)pd.getYDescriptor(0);
            Datum base = pd.getXDescriptor().getBase();
            Units xUnits = base != null ? base.getUnits() : pd.getXDescriptor().getUnits();
            Units yUnits = y.getYUnits();
            Units zUnits = y.getZUnits();
            builder = new TableDataSetBuilder(xUnits, yUnits, zUnits, y.getName());
            this.packetDescriptor(pd);
        }
        
        @Override
        public void packet(PacketDescriptor pd, Datum xTag, DatumVector[] vectors) throws StreamException {
            StreamYScanDescriptor yscan = (StreamYScanDescriptor)pd.getYDescriptor(0);
            Datum base = pd.getXDescriptor().getBase();
            Datum x = base == null ? xTag : base.add(xTag);
            DatumVector y = DatumVector.newDatumVector(yscan.getYTags(), yscan.getYUnits());
            String[] planeIDs = new String[pd.getYCount()];
            for (int i = 0; i < pd.getYCount(); i++) {
                planeIDs[i] = ((StreamYScanDescriptor)pd.getYDescriptor(i)).getName();
            }
            builder.insertYScan(x, y, vectors, planeIDs);
        }
        
        String[] streamPlaneIDs= null;
                
        @Override
        public void packetDescriptor(PacketDescriptor pd) throws StreamException {
           
			// Set properties for the default plane that is always preset
			StreamYScanDescriptor y = (StreamYScanDescriptor)pd.getYDescriptor(0);
			builder.setPlaneProperties(0, y.getProperties());
				
			// Add more planes (and thier props) as needed
			for (int i = 1; i < pd.getYCount(); i++) {
				y = (StreamYScanDescriptor)pd.getYDescriptor(i);
				builder.addPlane(y.getName(), y.getZUnits(), y.getProperties()); 
			}
            String[] planeIDs = new String[pd.getYCount()];
            for (int i = 0; i < pd.getYCount(); i++) {
                planeIDs[i] = ((StreamYScanDescriptor)pd.getYDescriptor(i)).getName();
            }
            if ( streamPlaneIDs==null ) {
                streamPlaneIDs= planeIDs;
            } else {
                for ( int i=0; i<streamPlaneIDs.length; i++ ) {
						 // Why is this true?  Seems oddly restrictive, there must be a reason -cwp
                    if ( !streamPlaneIDs[i].equals(planeIDs[i]) ) {
                        throw new StreamException( "only one name set allowed in stream: "+
									planeIDs[i] + " != "+ streamPlaneIDs[i] );
                    }
                }
            }
				
				// Handle properties at the Packet level, not sure this is done correctly
            Map<String,String> p= pd.getProperties();
            for ( Entry e: p.entrySet() ) {
                String key= (String)e.getKey();
                Object p0= builder.getProperty(key);
                if ( p0==null ) {
                    builder.setProperty( key, e.getValue() );
                } else {
                    if ( ! p0.equals( e.getValue() ) ) {
                        int i2;
                        for ( i2=1; builder.getProperty(""+key+"."+i2)!=null; i2++ ) { /* nothing */ }
                        builder.setProperty( ""+key+"."+i2, e.getValue() );
                    }
                }
            }
        }
        
        @Override
        public void streamClosed(StreamDescriptor sd) throws StreamException {}
        
        @Override
        public void streamDescriptor(StreamDescriptor sd) throws StreamException {}
        
        @Override
        public void streamException(StreamException se) throws StreamException {}
        
        @Override
        public void streamComment(StreamComment sc) throws StreamException {}
        
        @Override
        public DataSet getDataSet() {
            builder.addProperties(sd.getProperties());
            builder.addProperties(extraProperties);
            if ( monotonic&& builder.getProperty(DataSet.PROPERTY_X_MONOTONIC)==null ) {
                builder.setProperty( DataSet.PROPERTY_X_MONOTONIC, Boolean.TRUE );
            }
            return builder.toTableDataSet();
        }
        
    }
    
}
