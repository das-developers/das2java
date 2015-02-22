/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.dataset.examples;

import java.text.ParseException;
import org.virbo.dataset.JoinDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;
import org.virbo.dsops.Ops;

/**
 * For the various QDataSet schemes, show examples and documentation for
 * each.  This was motivated when trying to describe the output of 
 * org.das2.graph.ContoursRenderer.doAutorange()
 * 
 * Note all QDataSets are "duck-typed," meaning if they happen to meet the 
 * requirements of an interface then they are an instance of the interface.
 * 
 * @author jbf 
 */
public class Schemes {
    
    /**
     * return a bounding box for the data.  This is a rank 2 dataset where
     * ds[0,:] shows the bounds for the first dimension and ds[1,:] shows the
     * bounds for the second dimension.  Therefor ds[0,0] is the minumum extent
     * of the first dimension, and ds[0,1] is the maximum.
     * Note this can be extended to any number
     * of dimensions (cube or hypercube).
     * 
     * Note,
     *<blockquote><pre><small>{@code
     *from org.virbo.dataset.examples import Schemes
     *ds= Schemes.boundingBox()
     *print asDatumRange(ds.slice(0))
     *}</small></pre></blockquote>
     * 
     * @return a bounding box for the data.
     * @see org.virbo.dataset.DataSetUtil#asDatumRange(org.virbo.dataset.QDataSet) 
     */
    public static QDataSet boundingBox( ) {
        try {
            QDataSet xx= Ops.timegen( "2015-02-20T00:30", "60 seconds", 1440 );
            QDataSet yy= Ops.linspace( 14., 16., 1440 );
            JoinDataSet bds= new JoinDataSet(2);
            bds.join( Ops.extent( xx ) );
            bds.join( Ops.extent( yy ) );
            bds.makeImmutable();
            return bds;
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * return true if the data is a boundingBox.
     * @param ds a dataset
     * @return true if the dataset is a bounding box.
     */
    public static boolean isBoundingBox( QDataSet ds ) {
        return ds.rank()==2 && ds.length(0)==2 && ds.length()>0;
    }
    
    /**
     * return a rank 2 waveform, where the waveform is stored in packets.
     * DEPEND_0 is the time for each packet, and DEPEND_1 is the difference in
     * time for each measurement to the packet time.  Note the additional requirement
     * that the offsets be uniform, e.g.:
     *<blockquote><pre><small>{@code
     *from org.virbo.dataset.examples import Schemes
     *ds= Schemes.rank2Waveform()
     *deltaT= ds.property( QDataSet.DEPEND_1 )
     *ddeltaT= diffs(dep1)
     *print ddeltaT[0], ddeltT[-1] # should be the same
     *}</small></pre></blockquote>
     * 
     * @return rank 2 waveform.
     */
    public static QDataSet rank2Waveform( ) {
        return Ops.ripplesWaveformTimeSeries(20);
    }
    
    /**
     * return true if the data is a rank 2 waveform.
     * @param ds a dataset
     * @return  true if the data is a rank 2 waveform.
     */
    public static boolean isRank2Waveform( QDataSet ds ) {
        if ( ds.rank()!=2 ) return false;
        QDataSet dep0= (QDataSet)ds.property(QDataSet.DEPEND_0);
        if ( dep0==null ) return false;
        QDataSet dep1= (QDataSet)ds.property(QDataSet.DEPEND_1);
        if ( dep1==null ) return false;
        if ( SemanticOps.getUnits(dep0).getOffsetUnits().isConvertableTo(SemanticOps.getUnits(dep1)) ) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * return a rank 2 vectorTimeSeries, which is a bundle
     * of m rank 1 measurements.  This tacitly asserts orthogonality,
     * but the bundled data should at least all be rank 1 and in the same units.
     *<blockquote><pre><small>{@code
     *from org.virbo.dataset.examples import Schemes
     *ds= Schemes.vectorTimeSeries()
     *plot( magnitude( ds ) )
     *plot( unbundle( ds, 0 ) )
     *}</small></pre></blockquote>
     * @return rank 2 vector time series.
     *dataset>rank2bundle>vectorTimeSeries.
     */
    public static QDataSet vectorTimeSeries( ) {
        return Ops.ripplesVectorTimeSeries(1440);
    }
    
    /**
     * return true if the data is a vector time series.
     * @param ds a dataset
     * @return  true if the data is a vector time series.
     */
    public static boolean isVectorTimeSeries( QDataSet ds ) {
        return ds.rank()==2 && ( Ops.isLegacyBundle(ds) || Ops.isBundle(ds) ) && isTimeSeries(ds);
    }
    
    /**
     * return a rank 2 simple spectrogram, which has two indeces.
     * @return rank 2 simple spectrogram
     */
    public static QDataSet simpleSpectrogram() {
        return Ops.ripples(40,30);
    }
        
    /**
     * return true if the data is a simple spectrogram.
     * @param ds a dataset
     * @return  true if the data is a simple spectrogram.
     */
    public static boolean isSimpleSpectrogram( QDataSet ds ) {
        return ds.rank()==2;
    }

    /**
     * return a rank 2 simple spectrogram, which has two indeces
     * and is a TimeSeries.
     * @return simple spectrogram time series
     */
    public static QDataSet simpleSpectrogramTimeSeries() {
        return Ops.ripplesSpectrogramTimeSeries(1440);
    }
        
    /**
     * return true if the data is a simple spectrogram.
     * @param ds a dataset
     * @return  true if the data is a simple spectrogram.
     */
    public static boolean isSimpleSpectrogramTimeSeries( QDataSet ds ) {
        return isSimpleSpectrogram(ds) && isTimeSeries(ds);
    }
    
    /**
     * returns true if the dataset is a time series.  This is either something 
     * that has DEPEND_0 as a dataset with time location units, or a join of 
     * other datasets that are time series.
     * @param ds a dataset
     * @return true if the dataset is a time series.
     * @see SemanticOps#isTimeSeries(org.virbo.dataset.QDataSet) 
     */
    public static boolean isTimeSeries( QDataSet ds ) {
        return SemanticOps.isTimeSeries(ds);
    }
}
