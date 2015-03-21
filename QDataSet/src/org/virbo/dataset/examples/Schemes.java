
package org.virbo.dataset.examples;

import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.datum.EnumerationUnits;
import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;
import org.virbo.dataset.ArrayDataSet;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.JoinDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;
import org.virbo.dataset.WritableDataSet;
import org.virbo.dsops.Ops;
import static org.virbo.dsops.Ops.PI;
import static org.virbo.dsops.Ops.linspace;
import static org.virbo.dsops.Ops.ripples;

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
    
    private static Logger logger= Logger.getLogger("qdataset.schemes");
    
    /**
     * return a bounding box for the data.  This is a rank 2 dataset where
     * ds[0,:] shows the bounds for the first dimension and ds[1,:] shows the
     * bounds for the second dimension.  Therefor ds[0,0] is the minumum extent
     * of the first dimension, and ds[0,1] is the maximum.
     * Note this can be extended to any number
     * of dimensions (cube or hypercube).
     * 
     * Note,
     *<blockquote><pre>
     *from org.virbo.dataset.examples import Schemes
     *ds= Schemes.boundingBox()
     *print asDatumRange(ds.slice(0))
     *</pre></blockquote>
     * 
     * @return a bounding box for the data.
     * @see org.virbo.dataset.DataSetUtil#asDatumRange(org.virbo.dataset.QDataSet) 
     */
    public static QDataSet boundingBox( ) {
        try {
            QDataSet xx= Ops.timegen( "2015-02-20T00:30", "60 s", 1440 );
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
     *<blockquote><pre>
     *from org.virbo.dataset.examples import Schemes
     *ds= Schemes.rank2Waveform()
     *deltaT= ds.property( QDataSet.DEPEND_1 )
     *ddeltaT= diffs(dep1)
     *print ddeltaT[0], ddeltT[-1] # should be the same
     *</pre></blockquote>
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
        return SemanticOps.getUnits(dep0).getOffsetUnits().isConvertibleTo(SemanticOps.getUnits(dep1));
    }
    
    /**
     * return a rank 2 vectorTimeSeries, which is a bundle
     * of m rank 1 measurements.  This tacitly asserts orthogonality,
     * but the bundled data should at least all be rank 1 and in the same units.
     *<blockquote><pre>
     *from org.virbo.dataset.examples import Schemes
     *ds= Schemes.vectorTimeSeries()
     *plot( magnitude( ds ) )
     *plot( unbundle( ds, 0 ) )
     *</pre></blockquote>
     * dataset&rarr;rank2bundle&rarr;vectorTimeSeries.
     * @return rank 2 vector time series.
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
     * return a rank 1 scalar time series.
     * @return a rank 1 scalar time series.
     */
    public static QDataSet scalarTimeSeries() {
        try {
            QDataSet density= Ops.add( Ops.ripples(20), Ops.randomn(0,20) );
            density= Ops.putProperty( density, QDataSet.UNITS, Units.pcm3 );
            QDataSet t = Ops.timegen("2011-10-24", "20 sec", 20 );        
            return Ops.link( t, density );
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * return true if the data is a simple spectrogram.
     * @param ds a dataset
     * @return  true if the data is a simple spectrogram.
     */
    public static boolean isScalarTimeSeries( QDataSet ds ) {
        return ds.rank()==1 && isTimeSeries(ds);
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
    
    /**
     * uniform cadence is when each tag is the same distance apart, within a reasonable threshold.
     * @return dataset with uniform cadence
     */
    public static QDataSet uniformCadence() {
        return Ops.linspace( 0., 4., 100 );
    }
    
    /**
     * return true of the data has a uniform cadence.  Note that 
     * the CADENCE property is ignored.
     * @param ds a rank 1 dataset
     * @return true if the data has uniform cadence.
     */
    public static boolean isUniformCadence( QDataSet ds ) {
        if ( ds.rank()!=1 ) return false;
        double dv= ds.value(1)-ds.value(0);
        double manyDv= ( ds.value(ds.length()-1)-ds.value(0) ) / ( ds.length()-1)  ;
        return ( ( manyDv - dv ) / dv ) < 0.001;
    }

    /**
     * uniform ratiometric cadence is when the tags are uniform in log space.
     * @return dataset with uniform ratiometric cadence.
     */
    public static QDataSet uniformRatiometricCadence() {
        return Ops.pow( 10, Ops.linspace( 0., 4., 100 ) );
    }   

    /**
     * return true of the data has a uniform cadence.  Note that 
     * the CADENCE property is ignored.
     * @param ds a rank 1 dataset
     * @return true if the data has uniform cadence.
     */
    public static boolean isUniformRatiometricCadence( QDataSet ds ) {
        if ( ds.rank()!=1 ) return false;
        double dv= Math.log( ds.value(1)/ds.value(0) );
        double manyDv= Math.log( ds.value(ds.length()-1) / ds.value(0) ) / ( ds.length()-1 );
        return ( ( manyDv - dv ) / dv ) < 0.001;
    }
    
    /**
     * return an example of a compositeImage.
     * @return image[320,240,3]
     */    
    public static QDataSet compositeImage() {
        WritableDataSet rgb= Ops.zeros( 320, 240, 3 );
        for ( int i=0; i<320; i++ ) {
            for ( int j=0; j<240; j++ ) {
               if ( ( Math.pow((i-160),2) +  Math.pow((j-120),2) ) <2500 ) rgb.putValue( i,j,0, 255 );
               if ( i<160 ) rgb.putValue( i,j,1, 255 );
               if ( j<120 ) rgb.putValue( i,j,2, 255 );
            }
        }
        rgb.putProperty( QDataSet.DEPEND_0, Ops.linspace(0,4,rgb.length() ) );
        rgb.putProperty( QDataSet.DEPEND_1, Ops.pow( 10, Ops.linspace(0,4,rgb.length(0) ) ) );
        rgb.putProperty( QDataSet.RENDER_TYPE, QDataSet.VALUE_RENDER_TYPE_COMPOSITE_IMAGE );
                
        return rgb;
    }
    
    /**
     * return true if the dataset is a composite image, and is plottable
     * with the RGBImageRenderer
     * @param ds a dataset
     * @return true if the dataset is a composite image.
     */
    public static boolean isCompositeImage( QDataSet ds ) {
        QDataSet dep0= (QDataSet) ds.property(QDataSet.DEPEND_0);
        QDataSet dep1= (QDataSet) ds.property(QDataSet.DEPEND_1);
        return ds.rank()==3 && DataSetUtil.checkQube(ds) 
                && ( dep0==null || isUniformCadence(dep0) || isUniformRatiometricCadence(dep0) ) 
                && ( dep1==null || isUniformCadence(dep1) || isUniformRatiometricCadence(dep1) );
    }
    
    /**
     * return example events list.  This is a four-column rank 2 dataset with
     * start time, duration, RGB color, and ordinal data for the message.
     * @return example events list.
     */
    public static QDataSet eventsList( ) {
        try {
            QDataSet xx= Ops.timegen( "2015-01-01", "60s", 1440 );
            QDataSet dxx= Ops.putProperty( Ops.replicate( 45, 1440 ), QDataSet.UNITS, Units.seconds );
            QDataSet color= Ops.replicate( 0xFFAAAA, 1440 );
            for ( int i=100; i<200; i++ ) ((WritableDataSet)color).putValue( i, 0xFFAAFF );
            EnumerationUnits eu= EnumerationUnits.create("default");
            QDataSet msgs= Ops.putProperty( Ops.replicate( eu.createDatum("on1").doubleValue(eu), 1440 ), QDataSet.UNITS, eu );
            QDataSet result= Ops.bundle( xx, dxx, color, msgs );
            return result;
        } catch (ParseException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
    
    /**
     * return true if the data is an events list.
     * @param ds a dataset
     * @return true if the data is an events list.
     */
    public static boolean isEventsList( QDataSet ds ) {
        QDataSet bundle1= (QDataSet) ds.property(QDataSet.BUNDLE_1);
        if ( bundle1!=null ) {
            if ( bundle1.length()==3 || bundle1.length()==4 ) {
                Units u0= (Units) bundle1.property(QDataSet.UNITS,0);
                if ( u0==null ) u0= Units.dimensionless;
                Units u1= (Units) bundle1.property(QDataSet.UNITS,1);
                if ( u1==null ) u1= Units.dimensionless;
                Units u3= (Units) bundle1.property(QDataSet.UNITS,bundle1.length()-1);
                if ( u3!=null && UnitsUtil.isOrdinalMeasurement(u3) && u0.getOffsetUnits().isConvertibleTo(u1) ) {
                    return true;
                }
            } else {
                Units u3= (Units) bundle1.property(QDataSet.UNITS,bundle1.length()-1);
                if ( u3!=null && UnitsUtil.isOrdinalMeasurement(u3) ) {
                    return true;
                }            
            }
        } else {
            if ( SemanticOps.getUnits(ds) instanceof EnumerationUnits ) {
                QDataSet dep0= (QDataSet) ds.property(QDataSet.DEPEND_0);
                if ( dep0!=null ) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * return example angle distribution.
     * @return example angle distribution.
     */
    public static QDataSet angleDistribution( ) {
        ArrayDataSet rip= ArrayDataSet.maybeCopy( ripples( 30, 15 ) );
        QDataSet angle= linspace( PI/30/2, PI-PI/30/2, 30 );
        angle= Ops.putProperty( angle, QDataSet.UNITS, Units.radians );
        QDataSet rad= linspace( 1, 5, 15 );
        rip.putProperty( QDataSet.DEPEND_0, angle );
        rip.putProperty( QDataSet.DEPEND_1, rad );
        rip.putProperty( QDataSet.RENDER_TYPE, "pitchAngleDistribution" );
        return rip;
    }
    
    /**
     * return example angle distribution.
     * @param i the example number.  0..n-1.
     * @return example angle distribution.
     */
    public static QDataSet angleDistribution( int i ) {
        if ( i==0 ) {
            ArrayDataSet rip= ArrayDataSet.maybeCopy( Ops.randn( 24, 15 ) );
            for ( int j= 0; j<15; j++ ) {
                rip.putValue( 0, j, 20. );
                rip.putValue( 4, j, 20. );
            }
            
            QDataSet angle= Ops.multiply( linspace( 0.5, 23.5, 24 ), 15 );
            angle= Ops.putProperty( angle, QDataSet.UNITS, Units.degrees );
            QDataSet rad= linspace( 1, 5, 15 );
            rip.putProperty( QDataSet.DEPEND_0, angle );
            rip.putProperty( QDataSet.DEPEND_1, rad );
            rip.putProperty( QDataSet.RENDER_TYPE, "pitchAngleDistribution" );
            return rip;
            
        } else {
            return null;
        }
    }
    
    /**
     * return true if the data is an angle distribution.
     * @param ds a dataset
     * @return true if the data is an angle distribution.
     */
    public static boolean isAngleDistribution( QDataSet ds ) {
        if ( ds.rank()!=2 ) return false;
        QDataSet ads= (QDataSet)ds.property(QDataSet.DEPEND_0);
        //QDataSet rds= (QDataSet)ds.property(QDataSet.DEPEND_1);
        Units au= (Units) ads.property(QDataSet.UNITS);
        if ( au!=null && !( au==Units.dimensionless || au.isConvertibleTo(Units.degrees) ) ) {
            return false;
        }
        return true;
    }
    
    /**
     * return an example bundle dataset that bundles timetags, a rank 1 dataset
     * and a rank 2 dataset.
     * @return an example bundle dataset 
     */
    public static QDataSet bundleDataSet() {
        try {
            QDataSet tt= Ops.timegen( "2015-01-01", "60s", 1440 );
            QDataSet r1= Ops.ripplesTimeSeries(1440);
            QDataSet r2= Ops.ripplesVectorTimeSeries(1440);
            return Ops.bundle( tt, r1, r2 );
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * return true if the data is a bundle dataset
     * @param ds a dataset
     * @return true if the data is a bundle dataset
     */
    public static boolean isBundleDataSet( QDataSet ds ) {
        return Ops.isBundle(ds);
    }
    
    /**
     * return a trajectory through a space
     * @return rank 2 dataset
     */
    public static QDataSet trajectory( ) {
        QDataSet tt= Ops.multiply("1hr",Ops.linspace(0,24,1440));
        QDataSet xy= Ops.bundle( Ops.cos(tt), Ops.sin(tt) );
        return Ops.link( tt, xy );
    }
    
    /**
     * return true is the data is a trajectory
     * @param ds a dataset
     * @return true if the data is a trajectory
     */
    public static boolean isTrajectory( QDataSet ds ) {
        return isBundleDataSet(ds) && ds.rank()==2;
    }
    
    /**
     * return a rank 1 dataset that depends on a trajectory through a space.
     * @return rank 1 dataset with DEPEND_0 a trajectory.
     */
    public static QDataSet rank1AlongTrajectory( ) {
        QDataSet trajectory= trajectory();
        QDataSet zz= Ops.sin( Ops.linspace(0,Ops.PI,trajectory.length()));
        return Ops.link( trajectory, zz );
    }
    
    /**
     * return true if the data is rank 1 along a trajectory
     * @param ds a dataset
     * @return true if the data is rank 1 along a trajectory
     */
    public static boolean isRank1AlongTrajectory( QDataSet ds ) {
        return ds.rank()==1 && isTrajectory( (QDataSet) ds.property(QDataSet.DEPEND_0));
    }
}
