
package org.das2.client;

import java.util.HashMap;
import java.util.Map;
import org.das2.datum.Datum;
import org.das2.datum.DatumVector;
import org.das2.datum.Units;
import org.das2.qds.DataSetUtil;
import org.das2.qds.QDataSet;
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

/**
 * Write out QDataSet instead of legacy Das2 DataSet.
 * @author jbf
 */
public class QDataSetStreamHandler implements StreamHandler {

    Map<PacketDescriptor,DataSetBuilder> xbuilders;
    Map<PacketDescriptor,DataSetBuilder[]> builders;
    PacketDescriptor currentPd;
    DataSetBuilder[] currentBuilders;
    DataSetBuilder currentXBuilder;
        
    @Override
    public void streamDescriptor(StreamDescriptor sd) throws StreamException {
        builders= new HashMap<>();
        xbuilders= new HashMap<>();
    }

    @Override
    public void packetDescriptor(PacketDescriptor pd) throws StreamException {
        DataSetBuilder[] builders= new DataSetBuilder[pd.getYCount()];
        
        for ( int i=0; i<pd.getYCount(); i++ ) {
            SkeletonDescriptor sd= pd.getYDescriptor(i);
            DataSetBuilder builder;
            if ( sd instanceof StreamYScanDescriptor ) {
                StreamYScanDescriptor yscan= (StreamYScanDescriptor)sd;
                builder= new DataSetBuilder(2,1000,yscan.getNItems());
            } else if ( sd instanceof StreamMultiYDescriptor ) {
                builder= new DataSetBuilder(1,1000);
            } else {
                throw new IllegalArgumentException("not supported: "+sd);
            }
            builder.putProperty( QDataSet.UNITS, Units.lookupUnits( (String)sd.getProperty( "units") ) );
            builders[i]= builder;
        }
        
        DataSetBuilder xbuilder= new DataSetBuilder(1,1000);
        xbuilder.putProperty( QDataSet.UNITS, pd.getXDescriptor().getUnits() );
        xbuilders.put( pd, xbuilder );
        this.builders.put( pd, builders );
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
                for ( int i=0; i<currentBuilders.length; i++ ) {
                    ds1= Ops.bundle( ds1, currentBuilders[i].getDataSet() );
                }
            }
            ds= Ops.link( xds1, ds1 );
        } else {
            throw new UnsupportedOperationException("not implemented");
        }
        return ds;
    }
    
}
