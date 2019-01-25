
package org.das2.client;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.datum.CacheTag;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumRangeUtil;
import org.das2.datum.DatumUtil;
import org.das2.datum.DatumVector;
import org.das2.qds.DataSetUtil;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.ops.Ops;
import org.das2.qds.util.DataSetBuilder;
import org.das2.stream.PacketDescriptor;
import org.das2.stream.SkeletonDescriptor;
import org.das2.stream.StreamComment;
import org.das2.stream.StreamDescriptor;
import org.das2.stream.StreamException;
import org.das2.stream.StreamHandler;
import org.das2.stream.StreamMultiYDescriptor;
import org.das2.stream.StreamYScanDescriptor;
import org.das2.util.LoggerManager;

/**
 * Write out QDataSet instead of legacy Das2 DataSet.
 * @author jbf
 */
public class QDataSetStreamHandler implements StreamHandler {

    private static final Logger logger= LoggerManager.getLogger("das2.dataTransfer");
    
    Map<PacketDescriptor,DataSetBuilder> xbuilders;
    Map<PacketDescriptor,DataSetBuilder[]> builders;
    PacketDescriptor currentPd;
    DataSetBuilder[] currentBuilders;
    DataSetBuilder currentXBuilder;
    
    String streamTitle;
    Map streamProperties;
        
    @Override
    public void streamDescriptor(StreamDescriptor sd) throws StreamException {
        builders= new HashMap<>();
        xbuilders= new HashMap<>();
        streamTitle= String.valueOf( sd.getProperty("title") );
        streamProperties= sd.getProperties();
    }

    private void putProperty( DataSetBuilder builder, String name, Object value ) {
        if ( value instanceof Datum ) {
            logger.warning("kludge to fix Datum property values");
            value= ((Datum)value).doubleValue(((Datum)value).getUnits());
        }
        if ( SemanticOps.checkPropertyType( name, value, false ) ) {
            builder.putProperty( name, value );
        } else {
            logger.log(Level.WARNING, "property \"{0}\" should be type \"{1}\"", new Object[]{name, SemanticOps.getPropertyType(name)});
        }
    }
    
    @Override
    public void packetDescriptor(PacketDescriptor pd) throws StreamException {
        DataSetBuilder[] lbuilders= new DataSetBuilder[pd.getYCount()];
        
        for ( int i=0; i<pd.getYCount(); i++ ) {
            SkeletonDescriptor sd= pd.getYDescriptor(i);
            logger.log(Level.FINER, "got packet: {0}", sd);
            DataSetBuilder builder;
            if ( sd instanceof StreamYScanDescriptor ) {
                StreamYScanDescriptor yscan= (StreamYScanDescriptor)sd;
                builder= new DataSetBuilder(2,1000,yscan.getNItems());
                putProperty( builder, QDataSet.UNITS, yscan.getZUnits() );
                putProperty( builder, QDataSet.NAME, yscan.getName() );
                
            } else if ( sd instanceof StreamMultiYDescriptor ) {
                StreamMultiYDescriptor multiy= (StreamMultiYDescriptor)sd;
                builder= new DataSetBuilder(1,1000);
                putProperty( builder, QDataSet.UNITS, multiy.getUnits() );
                putProperty( builder, QDataSet.NAME, multiy.getName() );
                putProperty( builder, QDataSet.LABEL, multiy.getProperty("yLabel") ); // any of the following may return null.
                putProperty( builder, QDataSet.FORMAT, multiy.getProperty("yFormat") );
                putProperty( builder, QDataSet.VALID_MIN, multiy.getProperty("yValidMin") );
                putProperty( builder, QDataSet.VALID_MAX, multiy.getProperty("yValidMax") );
                putProperty( builder, QDataSet.FILL_VALUE, multiy.getProperty("yFill") );
            } else {
                throw new IllegalArgumentException("not supported: "+sd);
            }
            lbuilders[i]= builder;
        }
        
        DataSetBuilder xbuilder= new DataSetBuilder(1,1000);
        xbuilder.putProperty( QDataSet.UNITS, pd.getXDescriptor().getUnits() );
        xbuilder.putProperty( QDataSet.LABEL, streamProperties.get("xLabel") );
            
        xbuilders.put( pd, xbuilder );
        this.builders.put( pd, lbuilders );
    }

    @Override
    public void packet(PacketDescriptor pd, Datum xTag, DatumVector[] vectors) throws StreamException {
        if ( pd!=currentPd ) {
            currentXBuilder= xbuilders.get(pd);
            currentBuilders= builders.get(pd);
            currentPd= pd;
        }
        
        currentXBuilder.nextRecord(xTag);
        
        for ( int i=0; i<pd.getYCount(); i++ ) {
            if (currentBuilders[i].rank()==1 ) {
                currentBuilders[i].nextRecord(vectors[i].get(0));
            } else {
                currentBuilders[i].nextRecord(DataSetUtil.asDataSet(vectors[i]));
            }
        }
        
    }

    @Override
    public void streamClosed(StreamDescriptor sd) throws StreamException {
        System.err.println("streamClosed " + currentXBuilder );
    }

    @Override
    public void streamException(StreamException se) throws StreamException {
        System.err.println("streamException ");
    }

    @Override
    public void streamComment(StreamComment sc) throws StreamException {
        System.err.println("streamComment ");
    }
    
    public QDataSet getDataSet() {
        QDataSet ds;
        if ( xbuilders.size()==1 ) {
            QDataSet xds1= currentXBuilder.getDataSet();
            QDataSet ds1;
            if ( currentBuilders.length==1 ) {
                ds1= currentBuilders[0].getDataSet();
            } else {
                ds1= null;
                for (DataSetBuilder currentBuilder : currentBuilders) {
                    ds1 = Ops.bundle(ds1, currentBuilder.getDataSet());
                }
            }
            ds= Ops.link( xds1, ds1 );
        } else {
            throw new UnsupportedOperationException("not implemented");
        }
        ds= Ops.putProperty( ds, QDataSet.TITLE, streamTitle );
        Object oxCacheRange= streamProperties.get( "xCacheRange" );
        if ( oxCacheRange!=null ) {
            Object oxCacheResolution= streamProperties.get("xCacheResolution");
            try {
                CacheTag ct;
                if ( oxCacheResolution!=null ) {
                    ct= new CacheTag( (DatumRange)oxCacheRange, (Datum)oxCacheResolution );
                } else {
                    ct= new CacheTag( (DatumRange)oxCacheRange, null );
                }
                ds= Ops.putProperty( ds, QDataSet.CACHE_TAG, ct );
            } catch ( RuntimeException ex) {
                logger.log(Level.SEVERE, "unable to use properties for cacheTag", ex);
            }
        }
        ds= Ops.putProperty( ds, QDataSet.USER_PROPERTIES, streamProperties );
        
        return ds;
    }
    
}
