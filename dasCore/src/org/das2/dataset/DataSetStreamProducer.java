/*
 * DataSetStreamProducer.java
 *
 * Created on October 12, 2007, 10:11 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.das2.dataset;

import org.das2.datum.Datum;
import org.das2.datum.DatumVector;
import org.das2.datum.UnitsUtil;
import org.das2.stream.DataTransferType;
import org.das2.stream.PacketDescriptor;
import org.das2.stream.StreamDescriptor;
import org.das2.stream.StreamException;
import org.das2.stream.StreamScalarDescriptor;
import org.das2.stream.StreamProducer;
import org.das2.stream.StreamXDescriptor;
import org.das2.stream.StreamYScanDescriptor;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Configurable class for serializing a DataSet into a das2Stream.  This class
 * handles both VectorDataSets and TableDataSets, and uses java beans properties
 * to control how the stream is produced.  This code subsumes the functionality
 * of TableUtil.dumpToDas2Stream and VectorUtil.dumpToDas2Stream.
 *
 * @author jbf
 */
public class DataSetStreamProducer {
    
    /** Creates a new instance of DataSetStreamProducer */
    public DataSetStreamProducer() {
    }
    
    /**
     * convenient method for writing to an OutputStream.  Simply
     * uses Channels.newChannel to create a WritableByteChannel.
     */
    public void writeStream( OutputStream out ) {
        this.writeStream( Channels.newChannel(out) );
    }
    
    /**
     * writes the stream to the Channel.
     */
    public void writeStream( WritableByteChannel out ) {
        if ( dataSet instanceof VectorDataSet ) {
            writeVectorDataSetStream( out );
        } else {
            writeTableDataSetStream( out );
        }
    }
    
    private void writeTableDataSetStream( WritableByteChannel out ) {
        TableDataSet tds= (TableDataSet)dataSet;
        
        if (tds.getXLength() == 0) {
            try {
                out.close();
            } catch (IOException ioe) {
                //Do nothing.
            }
            return;
        }
        try {
            StreamProducer producer = new StreamProducer(out);
            StreamDescriptor sd = new StreamDescriptor();
            
            Map<String,Object> properties= tds.getProperties();
            if ( properties!=null) {
                for ( Entry<String,Object> e: properties.entrySet() ) {
                    String key= e.getKey();
                    sd.setProperty(key, e.getValue() );
                }
            }
            
            if ( compressed ) {
                sd.setCompression( "deflate" );
            }
            
            producer.streamDescriptor(sd);
            
            DataTransferType xTransferType;
            DataTransferType yTransferType;
            
            if ( asciiTransferTypes ) {
                if ( UnitsUtil.isTimeLocation(tds.getXUnits()) ) {
                    xTransferType= DataTransferType.getByName("time24");
                } else {
                    xTransferType= DataTransferType.getByName("ascii10");
                }
                yTransferType= DataTransferType.getByName("ascii10");
            } else {
                xTransferType= DataTransferType.getByName("sun_real8");
                yTransferType= DataTransferType.getByName("sun_real4");
            }
            
            PacketDescriptor pd = new PacketDescriptor();
            
            StreamXDescriptor xDescriptor = new StreamXDescriptor();
            xDescriptor.setUnits(tds.getXUnits());
            xDescriptor.setDataTransferType(xTransferType);
            
            pd.setXDescriptor(xDescriptor);
            
            String[] planeIds= DataSetUtil.getAllPlaneIds(tds);
            
            DatumVector[] yValues = new DatumVector[planeIds.length];
            
            for ( int j=0; j<tds.tableCount(); j++ ) {
                for ( int i=0; i<planeIds.length; i++ ) {
                    TableDataSet plane= (TableDataSet)tds.getPlanarView(planeIds[i]);
                    StreamYScanDescriptor yDescriptor = new StreamYScanDescriptor();
                    yDescriptor.setName(planeIds[i]);
                    yDescriptor.setDataTransferType(yTransferType);
                    yDescriptor.setZUnits(plane.getZUnits());
                    yDescriptor.setYCoordinates( plane.getYTags(j) );
                    pd.addYDescriptor(yDescriptor);
                }
                
                producer.packetDescriptor(pd);
                
                for (int i = tds.tableStart(j); i < tds.tableEnd(j); i++) {
                    Datum xTag = tds.getXTagDatum(i);
                    for ( int k=0; k<planeIds.length; k++ ) {
                        yValues[k] = tds.getScan(k);
                    }
                    producer.packet(pd, xTag, yValues);
                }
            }
            
            producer.streamClosed(sd);
            
        } catch (StreamException se) {
            throw new RuntimeException(se);
        }
    }
    
    private static DatumVector toDatumVector(Datum d) {
        double[] array = { d.doubleValue(d.getUnits()) };
        return DatumVector.newDatumVector(array, d.getUnits());
    }
    
    private void writeVectorDataSetStream( WritableByteChannel out ) {
        VectorDataSet vds= (VectorDataSet)dataSet;
        
        if (vds.getXLength() == 0) {
            try {
                out.close();
            } catch (IOException ioe) {
                //Do nothing.
            }
            return;
        }
        try {
            StreamProducer producer = new StreamProducer(out);
            StreamDescriptor sd = new StreamDescriptor();
            
            Map<String,Object> properties= vds.getProperties();
            if ( properties!=null) {
                for ( Entry<String,Object> e: properties.entrySet() ) {
                    String key= e.getKey();
                    sd.setProperty(key, e.getValue() );
                }
            }
            
            if ( compressed ) {
                sd.setCompression( "deflate" );
            }
            
            producer.streamDescriptor(sd);
            
            DataTransferType xTransferType;
            DataTransferType yTransferType;
            
            if ( asciiTransferTypes ) {
                if ( UnitsUtil.isTimeLocation(vds.getXUnits()) ) {
                    xTransferType= DataTransferType.getByName("time24");
                } else {
                    xTransferType= DataTransferType.getByName("ascii10");
                }
                yTransferType= DataTransferType.getByName("ascii10");
            } else {
                xTransferType= DataTransferType.getByName("sun_real8");
                yTransferType= DataTransferType.getByName("sun_real4");
            }
            
            StreamXDescriptor xDescriptor = new StreamXDescriptor();
            xDescriptor.setUnits(vds.getXUnits());
            xDescriptor.setDataTransferType(xTransferType);
            
            PacketDescriptor pd = new PacketDescriptor();
            pd.setXDescriptor(xDescriptor);
            
            String[] planeIds= DataSetUtil.getAllPlaneIds(vds);
            
            DatumVector[] yValues = new DatumVector[planeIds.length];
            
            for ( int i=0; i<planeIds.length; i++ ) {
                StreamScalarDescriptor yDescriptor = new StreamScalarDescriptor();
                yDescriptor.setName(planeIds[i]);
                yDescriptor.setDataTransferType(yTransferType);
                yDescriptor.setUnits(((VectorDataSet)vds.getPlanarView(planeIds[i])).getYUnits());
                pd.addYDescriptor(yDescriptor);
            }
            
            producer.packetDescriptor(pd);
            for (int i = 0; i < vds.getXLength(); i++) {
                Datum xTag = vds.getXTagDatum(i);
                for ( int j=0; j<planeIds.length; j++ ) {
                    yValues[j] = toDatumVector(((VectorDataSet)vds.getPlanarView(planeIds[j])).getDatum(i));
                }
                producer.packet(pd, xTag, yValues);
            }
            producer.streamClosed(sd);
        } catch (StreamException se) {
            throw new RuntimeException(se);
        }
        
    }
    
    /**
     * Holds value of property asciiTransferTypes.
     */
    private boolean asciiTransferTypes= true;
    
    /**
     * Getter for property asciiTransferTypes.
     * @return Value of property asciiTransferTypes.
     */
    public boolean isAsciiTransferTypes() {
        return this.asciiTransferTypes;
    }
    
    /**
     * If true, use ascii-type transfer types when creating the stream, so the 
     * stream is more easily read by humans and stream-naive parsers.
     * @param asciiTransferTypes New value of property asciiTransferTypes.
     */
    public void setAsciiTransferTypes(boolean asciiTransferTypes) {
        this.asciiTransferTypes = asciiTransferTypes;
    }
    
    /**
     * Holds value of property compressed.
     */
    private boolean compressed= false;
    
    /**
     * Getter for property compressed.
     * @return Value of property compressed.
     */
    public boolean isCompressed() {
        return this.compressed;
    }
    
    /**
     * If true, create a compressed stream.
     * @param compressed New value of property compressed.
     */
    public void setCompressed(boolean compressed) {
        this.compressed = compressed;
    }
    
    /**
     * Holds value of property dataSet.
     */
    private DataSet dataSet;
    
    /**
     * Getter for property dataSet.
     * @return Value of property dataSet.
     */
    public DataSet getDataSet() {
        return this.dataSet;
    }
    
    /**
     * Setter for property dataSet.
     * @param dataSet New value of property dataSet.
     */
    public void setDataSet(DataSet dataSet) {
        this.dataSet = dataSet;
    }
}
