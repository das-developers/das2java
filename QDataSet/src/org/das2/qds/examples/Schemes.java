
package org.das2.qds.examples;

import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.datum.EnumerationUnits;
import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;
import org.das2.qds.ArrayDataSet;
import org.das2.qds.DDataSet;
import org.das2.qds.DataSetUtil;
import org.das2.qds.JoinDataSet;
import org.das2.qds.MutablePropertyDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.SparseDataSetBuilder;
import org.das2.qds.WritableDataSet;
import org.das2.qds.ops.Ops;
import static org.das2.qds.ops.Ops.PI;
import static org.das2.qds.ops.Ops.linspace;
import static org.das2.qds.ops.Ops.ripples;
import org.das2.util.ColorUtil;

/**
 * For the various QDataSet schemes, show examples and documentation for
 * each.  This was motivated when trying to describe the output of 
 * org.das2.graph.ContoursRenderer.doAutorange()
 * 
 * Note all QDataSets are "duck-typed," meaning if they happen to meet the 
 * requirements of an interface then they are an instance of the interface.
 * 
 * All schemes can be executed using reflection, looking for methods starting
 * with "is".  This Jython code scans through the class looking for all the 
 * methods which make a dataset:
 * <pre>{@code
 * mm= dir( Schemes )
 * i=0
 * for m in mm:
 * if ( m[0:2]=='is' ):
 *     continue
 * else:
 *     i=i+1
 * }</pre>
 * 
 * @see https://github.com/autoplot/dev/blob/master/bugs/2022/20220125/showAllRanks.md
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
     *from org.das2.qds.examples import Schemes
     *ds= Schemes.boundingBox()
     *print asDatumRange(ds.slice(0))
     *</pre></blockquote>
     * 
     * @return a bounding box for the data.
     * @see org.das2.qds.DataSetUtil#asDatumRange(org.das2.qds.QDataSet) 
     * @see #arrayOfBoundingBox() 
     */
    public static QDataSet boundingBox( ) {
        try {
            QDataSet xx= Ops.timegen( "2015-02-20T00:30", "60 s", 1440 );
            QDataSet yy= Ops.putProperty( Ops.linspace( 14., 16., 1440 ), QDataSet.UNITS, Units.nT );
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
     * return true if the data is a boundingBox.  A bounding box is
     * rank 2 and has length 2.  ds[0,:] is typically the horizontal
     * direction, and ds[1,:] is the vertical.
     * 
     * @param ds a dataset
     * @return true if the dataset is a bounding box.
     */
    public static boolean isBoundingBox( QDataSet ds ) {
        return ds.rank()==2 && ds.length(0)==2 && ds.length()==2;
    }
    
    /**
     * array of bounding boxes, joined by the zeroth dimension.
     * @return array of bounding boxes
     * @see #boundingBox() 
     */
    public static QDataSet arrayOfBoundingBox( ) {
        Ops.randomSeed(0);
        QDataSet xx = Ops.floor( Ops.add( Ops.multiply( Ops.randn(100), 10 ), 100) );
        xx= Ops.putProperty( xx, QDataSet.UNITS, Units.MeV );
        QDataSet yy = (MutablePropertyDataSet) Ops.randn(100);

        QDataSet bx = Ops.extent(xx);
        QDataSet by = Ops.extent(yy);

        QDataSet xx2 = Ops.floor( Ops.add( Ops.multiply( Ops.randn(100), 12), 170 ) );
        xx2= Ops.putProperty( xx2, QDataSet.UNITS, Units.MeV );
        QDataSet yy2 = Ops.add( Ops.randn(100), 2 );

        QDataSet bx2 = Ops.extent(xx2);
        QDataSet by2 = Ops.extent(yy2);

        QDataSet xx3 = Ops.floor( Ops.add( Ops.multiply( Ops.randn(100),12), 140) );
        xx3= Ops.putProperty( xx3, QDataSet.UNITS, Units.MeV );
        QDataSet yy3 = Ops.subtract( Ops.randn(100), 1);

        QDataSet bx3 = Ops.extent(xx3);
        QDataSet by3 = Ops.extent(yy3);

        JoinDataSet bounds = (JoinDataSet)Ops.join(Ops.join(bx,by),Ops.join(bx2,by2));
        bounds.join( Ops.join(bx3,by3) );
        return bounds;
        
    }
    
    /**
     * return true if the data is a rank 3 array of bounding boxes.  Presently only
     * 2-D bounding boxes are allowed.  The zeroth dimension is the number of boxes,
     * and each slice is a bounding box.
     * @param ds
     * @return true if the data is a rank 3 array of bounding boxes.
     */
    public static boolean isArrayOfBoundingBox( QDataSet ds ) {
        int[] dims= DataSetUtil.qubeDims(ds);
        return ds.rank()==3 && dims!=null && dims[1]==2 && dims[2]==2 ;
    }
    
    /**
     * return a rank 2 waveform, where the waveform is stored in packets.
     * DEPEND_0 is the time for each packet, and DEPEND_1 is the difference in
     * time for each measurement to the packet time.  Note the additional requirement
     * that the offsets be uniform, e.g.:
     *<blockquote><pre>
     *from org.das2.qds.examples import Schemes
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
        Units u0= SemanticOps.getUnits(dep0);
        if ( u0==Units.dimensionless ) {
            return false;
        } else {
            QDataSet dep1= (QDataSet)ds.property(QDataSet.DEPEND_1);
            if ( dep1==null ) return false;
            if ( dep1.length()<QDataSet.MIN_WAVEFORM_LENGTH ) return false;
            return u0.getOffsetUnits().isConvertibleTo(SemanticOps.getUnits(dep1));
        }
    }
    
    /**
     * return a join of rank 2 waveforms, also called a rank 3 waveform.  
     * @return rank 3 waveform
     */
    public static QDataSet rank3Waveform( ) {
        QDataSet w1= Ops.ripplesWaveformTimeSeries(20);
        
        WritableDataSet w2= Ops.maybeCopy(Ops.ripplesWaveformTimeSeries(14));
        WritableDataSet t2= Ops.maybeCopy( (QDataSet)w2.property(QDataSet.DEPEND_0) );
        QDataSet et= Ops.extent((QDataSet)w1.property(QDataSet.DEPEND_0));
        double dt= et.value(1)-et.value(0);
        for ( int i=0; i<t2.length(); i++ ) t2.putValue(i,t2.value(i)+dt);
        w2.putProperty( QDataSet.DEPEND_0, t2);
        w2.putProperty( QDataSet.DEPEND_1, Ops.multiply(w2.property(QDataSet.DEPEND_1), 0.8 ) );
        
        WritableDataSet w3= Ops.maybeCopy(Ops.ripplesWaveformTimeSeries(3));
        WritableDataSet t3= Ops.maybeCopy( (QDataSet)w3.property(QDataSet.DEPEND_0) );
        Units tu= (Units)t3.property(QDataSet.UNITS);
                
        et= Ops.extent((QDataSet)w2.property(QDataSet.DEPEND_0));
        double dt2= et.value(1)-et.value(0);
        for ( int i=0; i<t3.length(); i++ ) t3.putValue(i,t3.value(i)+dt + dt2 + Units.seconds.convertDoubleTo( tu.getOffsetUnits(), 1) );
        w3.putProperty( QDataSet.DEPEND_0, t3);

        return Ops.join( Ops.join( w1, w2 ), w3 );
        
    }
    
    /**
     * return true if the data is a rank 3 join of rank 2 waveforms.
     * @param ds a dataset
     * @return true if the data is a rank 3 waveform.
     */
    public static boolean isRank3Waveform( QDataSet ds ) {
        if ( ds.rank()!=3 ) return false;
        boolean isWaveform= true;
        for ( int i=0; i<ds.length() && isWaveform; i++ ) {
            if ( !isRank2Waveform(ds.slice(i) ) ) isWaveform=false;
        }
        return isWaveform;
    }
    
    /**
     * return a rank 2 waveform, but DEPEND_1 which contains the offsets is also  
     * rank 2.  This was introduced to support study where short waveform-like
     * structures were identified.
     * @return a rank 2 waveform, but with rank 2 time-varying DEPEND_1 offsets.
     */
    public static QDataSet rank2WaveformRank2Offsets() {
        QDataSet ds= Ops.ripplesWaveformTimeSeries(20);
        QDataSet offs= (QDataSet) ds.property(QDataSet.DEPEND_1);
        offs= Ops.append( Ops.replicate(offs,10), Ops.replicate(Ops.divide(offs,3.0),10) );
        ds= Ops.putProperty( ds, QDataSet.DEPEND_1, offs );
        return ds;
    }
    
    /**
     * return true if the data is a rank 2 waveform with rank 2 offsets.
     * @param ds a dataset
     * @return  true if the data is a rank 2 waveform.
     */    
    public static boolean isRank2WaveformRank2Offsets( QDataSet ds ) {
        return isRank2Waveform(ds) && ((QDataSet)ds.property(QDataSet.DEPEND_1)).rank()==2;
    }
    
    /**
     * return a rank 2 vectorTimeSeries, which is a bundle
     * of m rank 1 measurements.  This tacitly asserts orthogonality,
     * but the bundled data should at least all be rank 1 and in the same units.
     *<blockquote><pre>
     *from org.das2.qds.examples import Schemes
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
     * return true if the data is a simple spectrogram, which is 
     * rank 2, and not a small bundle.
     * @param ds a dataset
     * @return  true if the data is a simple spectrogram.
     */
    public static boolean isSimpleSpectrogram( QDataSet ds ) {
        if ( ds.rank()==2 ) {
            return !(ds.length(0)<4 && ( Ops.isBundle(ds) || Ops.isLegacyBundle(ds) ));
        } else {
            return false;
        }
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
     * return true if the data is a simple time series of scalars.
     * @param ds a dataset
     * @return  true if the data is a simple spectrogram.
     */
    public static boolean isScalarTimeSeries( QDataSet ds ) {
        return ds.rank()==1 && isTimeSeries(ds);
    }
    
    /**
     * return a rank 1 scalar series with errors.
     * @return  a rank 1 scalar series with errors.
     */
    public static QDataSet scalarSeriesWithErrors() {
        QDataSet x= Ops.add( 1, Ops.divide( Ops.findgen(41),2 ) );
        QDataSet y= Ops.exp( Ops.multiply( -1, Ops.pow( Ops.subtract(x,10), 2 ) ) );
        MutablePropertyDataSet result= Ops.maybeCopy( Ops.link( x, y ) );
        result.putProperty( QDataSet.DELTA_PLUS, Ops.replicate(0.04,41) );
        result.putProperty( QDataSet.DELTA_MINUS, Ops.replicate(0.04,41) );
        return result;
    }
    
    /**
     * return true is the data is a simple series of scalars with errors.
     * @param ds dataset
     * @return true is the data is a simple series of scalars with errors.
     */
    public static boolean isScalarSeriesWithErrors( QDataSet ds ) {
        return ds.rank()==1
                && ds.property(QDataSet.DELTA_PLUS)!=null 
                && ds.property(QDataSet.DELTA_MINUS)!=null;
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
     * @see SemanticOps#isTimeSeries(org.das2.qds.QDataSet) 
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
        return Math.abs( ( manyDv - dv ) / dv ) < 0.001;
    }

    /**
     * uniform ratiometric cadence is when the tags are uniform in log space.
     * @return dataset with uniform ratiometric cadence.
     */
    public static QDataSet uniformRatiometricCadence() {
        return Ops.pow( 10, Ops.linspace( 0., 4., 100 ) );
    }   

    /**
     * return true of the data has a uniform cadence in log space.  Note that 
     * the CADENCE property is ignored.
     * @param ds a rank 1 dataset
     * @return true if the data has uniform cadence.
     */
    public static boolean isUniformRatiometricCadence( QDataSet ds ) {
        if ( ds.rank()!=1 ) return false;
        if ( !UnitsUtil.isRatioMeasurement( SemanticOps.getUnits(ds) ) ) return false;
        double dv= Math.log( ds.value(1)/ds.value(0) );
        double manyDv= Math.log( ds.value(ds.length()-1) / ds.value(0) ) / ( ds.length()-1 );
        return Math.abs( ( manyDv - dv ) / dv ) < 0.001;
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
     * start time, end time, RGB color, and ordinal data for the message.
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
            QDataSet result= Ops.bundle( xx, Ops.add(xx,dxx), color, msgs );
            return result;
        } catch (ParseException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
    
    /**
     * return a canonical event
     * @return a canonical event
     */
    public static QDataSet canonicalEvent() {
        QDataSet ds= Ops.bundle( Ops.dataset("2025-05-06T14:29"), 
                Ops.dataset("2025-05-06T14:29"), 
                Ops.dataset(Units.rgbColor.createDatum(ColorUtil.SKY_BLUE.getRGB())),
                Ops.dataset(Units.nominal().createDatum("Sunny and Warm")) );
        
        DataSetUtil.toString(ds);
        return ds;
    }
    
    /**
     * returns true if the dataset is a rank 1 canonical event, having 
     * three or four elements, the first two are times or the same units and
     * the last is a nominal datum describing the event.
     * @param ds dataset
     * @return true if is a canonical event.
     */
    public static boolean isCanonicalEvent( QDataSet ds ) {
        if ( ds.rank()!=1 ) return false;
        QDataSet bundle0= (QDataSet) ds.property(QDataSet.BUNDLE_0);
        if ( bundle0!=null ) {
            if ( bundle0.length()==3 || bundle0.length()==4 || bundle0.length()==5 ) {
                Units u0= (Units) bundle0.property(QDataSet.UNITS,0);
                if ( u0==null ) u0= Units.dimensionless;
                Units u1= (Units) bundle0.property(QDataSet.UNITS,1);
                if ( u1==null ) u1= Units.dimensionless;
                Units u3= (Units) bundle0.property(QDataSet.UNITS,bundle0.length()-1);
                boolean t1t2= UnitsUtil.isTimeLocation(u0) && UnitsUtil.isTimeLocation(u1);
                if ( t1t2 || ( u3!=null && UnitsUtil.isOrdinalMeasurement(u3) && u0.getOffsetUnits().isConvertibleTo(u1) ) ) {
                    if ( u0.isConvertibleTo(u1) ) {
                        QDataSet isge= Ops.ge( Ops.slice0( ds, 1 ), Ops.slice0( ds, 0 ) );
                        return Ops.total(isge) == Ops.total( Ops.valid( Ops.slice0(ds,0) ) );
                    } else {
                        QDataSet isge= Ops.ge( Ops.abs( Ops.slice0( ds, 1 ) ), 0. );
                        return Ops.total(isge) == Ops.total( Ops.valid( Ops.slice0(ds,0) ) );
                    }
                } else if ( u3!=null  && UnitsUtil.isOrdinalMeasurement(u3) && u0.isConvertibleTo(u1) ) {
                    QDataSet isge= Ops.ge( Ops.slice0( ds, 1 ), Ops.slice0( ds, 0 ) );
                    return Ops.total(isge) == Ops.total( Ops.valid( Ops.slice0(ds,0) ) );
                }
            } else {
                Units u3= (Units) bundle0.property(QDataSet.UNITS,bundle0.length()-1);
                if ( u3!=null && UnitsUtil.isOrdinalMeasurement(u3) ) {
                    return true;
                }
            }
        } 
        return false; //TODO: this means a slice of some events lists are not events!
        
    }
    /**
     * return true if the data is an events list.
     * @param ds a dataset
     * @return true if the data is an events list.
     */
    public static boolean isEventsList( QDataSet ds ) {
        QDataSet bundle1= (QDataSet) ds.property(QDataSet.BUNDLE_1);
        if ( bundle1!=null ) {
            if ( bundle1.length()==3 || bundle1.length()==4 || bundle1.length()==5 ) {
                Units u0= (Units) bundle1.property(QDataSet.UNITS,0);
                if ( u0==null ) u0= Units.dimensionless;
                Units u1= (Units) bundle1.property(QDataSet.UNITS,1);
                if ( u1==null ) u1= Units.dimensionless;
                Units u3= (Units) bundle1.property(QDataSet.UNITS,bundle1.length()-1);
                boolean t1t2= UnitsUtil.isTimeLocation(u0) && UnitsUtil.isTimeLocation(u1);
                if ( t1t2 || ( u3!=null && UnitsUtil.isOrdinalMeasurement(u3) && u0.getOffsetUnits().isConvertibleTo(u1) ) ) {
                    if ( u0.isConvertibleTo(u1) ) {
                        QDataSet isge= Ops.ge( Ops.slice1( ds, 1 ), Ops.slice1( ds, 0 ) );
                        return Ops.total(isge) == Ops.total( Ops.valid( Ops.slice1(ds,0) ) );
                    } else {
                        QDataSet isge= Ops.ge( Ops.abs( Ops.slice1( ds, 1 ) ), 0. );
                        return Ops.total(isge) == Ops.total( Ops.valid( Ops.slice1(ds,0) ) );
                    }
                } else if ( u3!=null  && UnitsUtil.isOrdinalMeasurement(u3) && u0.isConvertibleTo(u1) ) {
                    QDataSet isge= Ops.ge( Ops.slice1( ds, 1 ), Ops.slice1( ds, 0 ) );
                    return Ops.total(isge) == Ops.total( Ops.valid( Ops.slice1(ds,0) ) );
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
     * A complexBundleDataSet is a set of datasets which have different rank.  The
     * rank 2 datasets take multiple columns of the dataset, and rank 3 and 4 datasets
     * are unrolled to make them rank 2 and the property ELEMENT_DIMENSIONS is used
     * to re-form them.  This returns an example bundle dataset that bundles timetags, density, velocity, and flux.
     * Yes, this was coded this twice, not realizing it was already done.  This code
     * is probably easier to read, so it is left in.
     * 
     * @return an example bundle dataset 
     * @see #complexBundleDataSet() 
     */
    public static QDataSet complexBundleDataSet2() {
        DDataSet data= DDataSet.createRank2( 12, 9 );
        QDataSet ttags= Ops.linspace( "2019-03-07T00:30Z", "2019-03-07T11:30Z", 12 );
        QDataSet vel= vectorTimeSeries();
        QDataSet dens= scalarTimeSeries();
        QDataSet flux= simpleSpectrogram();
        for ( int i=0; i<data.length(); i++ ) {
            data.putValue( i, 0, ttags.value(i) );
            data.putValue( i, 1, dens.value(i) );
            data.putValue( i, 2, vel.value(i,0) );
            data.putValue( i, 3, vel.value(i,1) );
            data.putValue( i, 4, vel.value(i,2) );
            data.putValue( i, 5, flux.value(i,0) );
            data.putValue( i, 6, flux.value(i,1) );
            data.putValue( i, 7, flux.value(i,2) );
            data.putValue( i, 8, flux.value(i,3) );
        }
        
        
        SparseDataSetBuilder sb= new SparseDataSetBuilder(2);
        sb.setLength(9);
        
        sb.putProperty( QDataSet.NAME, 0, "ttags");
        sb.putProperty( QDataSet.UNITS, 0, ttags.property(QDataSet.UNITS) );
        sb.putProperty( QDataSet.NAME, 1, "density");
        sb.putProperty( QDataSet.UNITS, 1, Units.pcm3 );
        
        Units vunits= (Units)((QDataSet)vel.property(QDataSet.BUNDLE_1)).property(QDataSet.UNITS,0);
        sb.putProperty( QDataSet.NAME, 2, "vx" );
        sb.putProperty( QDataSet.UNITS, 2, vunits );
        sb.putProperty( QDataSet.NAME, 3, "vy" );
        sb.putProperty( QDataSet.UNITS, 3, vunits );
        sb.putProperty( QDataSet.NAME, 4, "vz" );
        sb.putProperty( QDataSet.UNITS, 4, vunits );

        // a rank 2 dataset "V" can be unbundled to get all three components at once.
        sb.putProperty( QDataSet.ELEMENT_DIMENSIONS, new int[] { 3 } );
        sb.putProperty( QDataSet.ELEMENT_NAME, 2, "V" );
        sb.putProperty( QDataSet.ELEMENT_LABEL, 2, "S/C Velocity" );
        sb.putProperty( QDataSet.START_INDEX, 2, 2);
        sb.putProperty( QDataSet.START_INDEX, 3, 2);
        sb.putProperty( QDataSet.START_INDEX, 4, 2);

        Units funits= Units.lookupUnits( "kg m^2 s^-2 A^-1" );
        sb.putProperty( QDataSet.NAME, 5, "fl0" );
        sb.putProperty( QDataSet.UNITS, 5, funits );
        sb.putProperty( QDataSet.NAME, 6, "fl1" );        
        sb.putProperty( QDataSet.UNITS, 6, funits );
        sb.putProperty( QDataSet.NAME, 7, "fl2" );        
        sb.putProperty( QDataSet.UNITS, 7, funits );
        sb.putProperty( QDataSet.NAME, 8, "fl3" );        
        sb.putProperty( QDataSet.UNITS, 8, funits );

        // a rank 2 dataset "flux" can be unbundled to get the spectrogram.
        sb.putProperty( QDataSet.ELEMENT_DIMENSIONS, 5, new int[] { 4 } );
        sb.putProperty( QDataSet.ELEMENT_NAME, 5, "Flux" );
        sb.putProperty( QDataSet.DEPEND_1, 5, Ops.pow( 10, linspace(1.,4.,4) ) );
        sb.putProperty( QDataSet.START_INDEX, 5, 5);
        sb.putProperty( QDataSet.START_INDEX, 6, 5);
        sb.putProperty( QDataSet.START_INDEX, 7, 5);
        sb.putProperty( QDataSet.START_INDEX, 8, 5);
        
        data.putProperty( QDataSet.BUNDLE_1, sb.getDataSet() );
        
        return data;
    }
    
    /**
     * return an example bundle dataset that bundles timetags, a rank 1 dataset
     * and a rank 1 dataset.  
     * 
     * @return an example bundle dataset 
     * @see #complexBundleDataSet()  which has differing rank.
     */
    public static QDataSet bundleDataSet() {
        try {
            QDataSet tt= Ops.timegen( "2015-01-01", "60s", 1440 );
            QDataSet r1= Ops.ripplesTimeSeries(1440);
            QDataSet r2= Ops.unbundle( Ops.ripplesVectorTimeSeries(1440),0 );
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
     * return true if the data describes the columns of another dataset.
     * @param bds
     * @return 
     */
    public static boolean isBundleDescriptor( QDataSet bds ) {
        if ( bds.rank()!=2 ) {
            return false;
        } else {
            if ( bds.length(0)==0 ) {
                return true;
            } else {
                return false;
            }
        }
    }
    
    /**
     * return data that describes the columns of another dataset.  Note these
     * are typically not found in APIs.  The bundle descriptor will be <ul>
     * <li> rank 2, 
     * <li> length() gives the number of columns.
     * <li> length(i) gives the number of indices of the ith column, or the ith column's rank.
     * <li> property(i,NAME) gives the name of the column
     * <li> property(i,START_INDEX) gives the column which is the first column of the rank 2 or higher bundled data set.
     * <li> property(i,xxx) gives property xxx of the column
     * <li> property(i,DEPEND0_NAME) gives the name of the column containing the depend 0 values.
     * </ul>
     * @return data that describes the columns of another dataset.
     */
    public static QDataSet bundleDescriptor() {
        QDataSet bundle= bundleDataSet();
        return (QDataSet)bundle.property(QDataSet.BUNDLE_1);
    }
    
    /**
     * return a complex rank 2 dataset, N by 2, which can be thought of as a 1-D array of N complex numbers
     * @return a complex rank 2 dataset ds[N,2]
     * @see #isComplexNumbers(org.das2.qds.QDataSet) 
     */
    public static QDataSet rank2ComplexNumbers() {
        QDataSet w= rank2Waveform();
        w= w.slice(0);
        return Ops.fft(w);
    }
    
    private static final QDataSet COMPLEX_COORDINATE_SYSTEM_DEPEND;
    static {
        EnumerationUnits u1 = EnumerationUnits.create("complexCoordinates");
        DDataSet dep1 = DDataSet.createRank1(2);
        dep1.putValue(0, u1.createDatum("real").doubleValue(u1));
        dep1.putValue(1, u1.createDatum("imag").doubleValue(u1));
        dep1.putProperty(QDataSet.COORDINATE_FRAME, QDataSet.VALUE_COORDINATE_FRAME_COMPLEX_NUMBER);
        dep1.putProperty(QDataSet.UNITS, u1);
        COMPLEX_COORDINATE_SYSTEM_DEPEND= dep1;
    }
    
    /**
     * returns the QDataSet used to identify the columns of a complex coordinate frame.
     * @return the QDataSet used to identify the columns of a complex coordinate frame.
     */
    public static QDataSet complexCoordinateSystemDepend() {
        return COMPLEX_COORDINATE_SYSTEM_DEPEND;
    }
    
    /**
     * return true if the data is length 2, rank 1, and has "ComplexNumber" as the COORDINATE_FRAME. 
     * @param dep
     * @return 
     */
    public static boolean isComplexCoordinateSystemDepend( QDataSet dep ) {
        return dep.length()==2 && QDataSet.VALUE_COORDINATE_FRAME_COMPLEX_NUMBER.equals(dep.property(QDataSet.COORDINATE_FRAME));
    }
    
    /**
     * return true if the data represents an array of complex numbers, containing the property COORDINATE_FRAME
     * on the last DEPEND, which is equal to "ComplexNumber"
     * @param ds1 a dataset
     * @return true if the data represents an array of complex numbers.
     * @see Ops#checkComplexArgument(org.das2.qds.QDataSet) 
     */
    public static boolean isComplexNumbers( QDataSet ds1 ) {
        int r= ds1.rank();
        QDataSet dep;
        switch (r) {
            case 0:
                return false;
            case 1:
                if ( ds1.length()!=2 ) return false;
                dep= (QDataSet) ds1.property(QDataSet.DEPEND_0);
                break;
            case 2:
                if ( ds1.length(0)!=2 ) return false;
                dep= (QDataSet) ds1.property(QDataSet.DEPEND_1);
                break;
            case 3:
                if ( ds1.length(0,0)!=2 ) return false;
                dep= (QDataSet) ds1.property(QDataSet.DEPEND_2);
                break;
            case 4:
                // note complexMultiply, etc don't support rank 4 datasets.
                if ( ds1.length(0,0,0)!=2 ) return false;
                dep= (QDataSet) ds1.property(QDataSet.DEPEND_3);
                break;
            default:
                return false;
        }
        if ( dep==null ) return false;
        return isComplexCoordinateSystemDepend(dep);
    }
    
    /**
     * return bundle with Time, Density, Speed, and Flux, to demonstrate
     * a bundle of datasets with differing rank.
     * @return bundle with Time, Density, Speed, and Flux
     * @see #complexBundleDataSet2() 
     */
    public static QDataSet complexBundleDataSet() {
        try {
            QDataSet tt= Ops.timegen( "2016-12-21T00:00", "60s", 1440 );
            tt= Ops.putProperty( tt, QDataSet.NAME, "time" );
            Ops.randomSeed(5334);
            QDataSet density= Ops.pow( 10, Ops.add( Ops.divide( Ops.randn(1440),10 ), 1 ) );  // 10**(1+randn/10)
            density= Ops.putProperty( density, QDataSet.UNITS, Units.pcm3 );
            density= Ops.putProperty( density, QDataSet.NAME, "density" );
            density= Ops.putProperty( density, QDataSet.DEPENDNAME_0, "time" );
            QDataSet vv= Ops.transpose( Ops.reform( Ops.accum( Ops.randn(1440*3) ), new int[] { 3, 1440 } ) );
            vv= Ops.putProperty( vv, QDataSet.UNITS, Units.cmps );
            vv= Ops.putProperty( vv, QDataSet.NAME, "speed" );
            vv= Ops.putProperty( vv, QDataSet.DEPENDNAME_0, "time" );
            DDataSet ff= DDataSet.createRank2(1440,4);
            ff.putProperty( QDataSet.UNITS, Units.lookupUnits("s!E-1!Ncm!E-2!Nster!E-1!NkeV!E-1!N") );
            ff.putProperty( QDataSet.NAME, "flux" );
            for ( int i=0; i<1440; i++ ) {
                ff.putValue( i, 0, 23.0 + vv.value(i,0) );
                ff.putValue( i, 1, 45.0 + vv.value(i,0) );
                ff.putValue( i, 2, 31.0 + vv.value(i,0) );
                ff.putValue( i, 3, 11.0 + vv.value(i,0) );
            }
            ff.putProperty( QDataSet.DEPEND_1, Ops.pow( 10, linspace(1.,4.,4) ) );
            QDataSet result= Ops.bundle( tt, density );
            for ( int j=0; j<vv.length(0); j++ ) {
                MutablePropertyDataSet mpds= (MutablePropertyDataSet) Ops.slice1(vv,j);
                mpds.putProperty( QDataSet.NAME, "speed_"+(char)('x'+j) );
                mpds.putProperty( QDataSet.DEPENDNAME_0, "time" );
                result= Ops.bundle( result, mpds ); // presently the bundle operator works only on rank 1 datasets.
            }
            for ( int j=0; j<ff.length(0); j++ ) {
                MutablePropertyDataSet mpds= (MutablePropertyDataSet) Ops.slice1(ff,j);
                mpds.putProperty( QDataSet.NAME, "flux_"+j );
                mpds.putProperty( QDataSet.DEPENDNAME_0, "time" );
                result= Ops.bundle( result, mpds );
            }
            MutablePropertyDataSet bds= (MutablePropertyDataSet)result.property(QDataSet.BUNDLE_1);
            for ( int j=0; j<vv.length(0); j++ ) {
                bds.putProperty( QDataSet.START_INDEX, 2+j, 2 );
                bds.putProperty( QDataSet.ELEMENT_DIMENSIONS, 2+j, new int[] { 3 } );
                bds.putProperty( QDataSet.ELEMENT_NAME, 2+j, "speed" );
            }
            for ( int j=0; j<ff.length(0); j++ ) {
                bds.putProperty( QDataSet.START_INDEX, 5+j, 5 );
                bds.putProperty( QDataSet.ELEMENT_DIMENSIONS, 5+j, new int[] { 4 } );
                bds.putProperty( QDataSet.ELEMENT_NAME, 5+j, "flux" );
                bds.putProperty( QDataSet.DEPEND_1, ff.property(QDataSet.DEPEND_1) );
            }
            return result;
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }
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
     * return a rank 1 dataset that depends on a trajectory through a space,
     * which is not supported currently.
     * 
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

    /**
     * "scatter" is meant to indicate there is no connection between 
     * successive points, and that there is no dependence indicates no 
     * clean relation between the bundled datasets.
     * @return 
     */
    public static QDataSet xyScatter() {
        QDataSet xx= Ops.randomn(5334,30);
        QDataSet yy= Ops.randomn(5335,30);
        return Ops.bundle(xx,yy);
    }
    
    /**
     * is a xyScatter data set.
     * @param ds
     * @return true if is a xyScatter data set.
     */
    public static boolean isXYScatter( QDataSet ds ) {
        return ds.rank()==2 && ds.length(0)==2 && Schemes.isBundleDataSet(ds);
    }
    
    /**
     * Here there is a Z that is a function of X and Y of a xyScatter.
     * @return rank 1 dataset that has bundle for DEPEND_0.
     * @see #xyzScatter() 
     */    
    public static QDataSet rank1AtXYScatter() {
        QDataSet xx= Ops.randomn(5334,30);
        QDataSet yy= Ops.randomn(5335,30);
        QDataSet zz= Ops.sqrt( Ops.add( Ops.pow(xx,2),Ops.pow(yy,2) ) );
        return Ops.link(Ops.bundle(xx,yy),zz);
    }

    /**
     * is a Z that is a function of X and Y of a xyScatter.
     * @param ds
     * @return true if is a Z that is a function of X and Y of a xyScatter.
     */
    public static boolean isRank1AtXYScatter( QDataSet ds ) {
        QDataSet xy= (QDataSet) ds.property(QDataSet.DEPEND_0);
        return ( xy!=null && isXYScatter(xy) && ds.rank()==1 );
    }
    
    /**
     * return true if the data is a rank 2 list of M bins.  The data
     * will have rank=2 and the property BINS_1='min,max'
     * @param dep
     * @return true if the data is a rank 2 list of M bins
     */
    public static boolean isRank2Bins(QDataSet dep) {
        return dep.rank()==2 
                && ( QDataSet.VALUE_BINS_MIN_MAX.equals( dep.property(QDataSet.BINS_1) )
                        || QDataSet.VALUE_BINS_MIN_MAX_INCLUSIVE.equals( dep.property(QDataSet.BINS_1) ) );
    }
    
    /**
     * return a rank 2 dataset that is a list of bins.
     * @return 
     */
    public static QDataSet rank2Bins() {
        MutablePropertyDataSet result= Ops.maybeCopy( Ops.findgen( 20,2 ) );
        result.putProperty( QDataSet.BINS_1, QDataSet.VALUE_BINS_MIN_MAX );
        return result;
    }

    /**
     * Many of Autoplot's codes use the "legacyXYZScatter" QDataSet scheme,
     * where the X tags are in DEPEND_0, the Y tags are the QDataSet, and
     * PLANE_0 contains the Z values.
     * @param zds
     * @return 
     */
    public static boolean isLegacyXYZScatter(QDataSet zds) {
        return zds.rank()==1 && zds.property(QDataSet.PLANE_0)!=null;
    }
    
    /**
     * Many code use this form of data to represent Z(X,Y).  This is not preferred,
     * and ds[n;x,y,z] should be used instead.  This is available for testing.
     * @return rank 1 dataset with DEPEND_0 and PLANE_0 properties.
     * @see #rank1AtXYScatter
     */
    public static QDataSet legacyXYZScatter() {
        QDataSet xx= Ops.outerProduct( Ops.linspace( "2015-03-01T00:00", "2015-03-01T10:00", 150 ), Ops.ones(30) );
        QDataSet yy= Ops.outerProduct( Ops.ones(150), Ops.linspace( 10., 40., 30 ) );
        QDataSet zz= Ops.ripples( 150, 30 );
        xx= Ops.reform( xx, new int[] {4500} );
        yy= Ops.reform( yy, new int[] {4500} );
        yy= Ops.putProperty( yy, QDataSet.UNITS, Units.hertz );
        zz= Ops.reform( zz, new int[] {4500} );
        yy= Ops.putProperty( yy, QDataSet.DEPEND_0, xx );
        yy= Ops.putProperty( yy, QDataSet.PLANE_0, zz );
        return yy;
    }
    
    /**
     * @see #xyzScatter() 
     * @param zds
     * @return true is it is an xyzScatter scheme.
     */
    public static boolean isXYZScatter(QDataSet zds) {
        return zds.rank()==2 && isBundleDataSet(zds) && zds.length(0)==3;
    }
    
    /**
     * This will be the preferred way to represent X,Y &rarr; Z.  This shows
     * a problem, however, where there is no way to indicate the dependencies
     * for the columns.  The Z column can have DEPENDNAME_0, but how does one
     * declare the dependence on Y as well?  
     * In https://sourceforge.net/p/autoplot/bugs/1710/, I propose 
     * CONTEXT_0=field0,field2, which seems like it would work nicely.
     * @return  
     * @see #rank1AlongTrajectory() 
     * @see #rank1AtXYScatter() 
     */
    public static QDataSet xyzScatter() {
        //QDataSet xx= Ops.outerProduct( Ops.linspace( "2015-03-01T00:00", "2015-03-01T10:00", 150 ), Ops.ones(30) );
        //QDataSet yy= Ops.outerProduct( Ops.ones(150), Ops.linspace( 10., 40., 30 ) );
        //QDataSet zz= Ops.ripples( 150, 30 );
        QDataSet xx= Ops.append(Ops.randn(300),Ops.add(2,Ops.randn(300)));
        QDataSet yy= Ops.append(Ops.randn(300),Ops.add(-3,Ops.randn(300)));
        QDataSet zz= Ops.sin( Ops.add( xx, yy ) );
        QDataSet result= Ops.bundle( xx,yy,zz );
        return result;
    }

    /**
     * return true if the data is a join of datasets of different cadences or lengths.
     * @param ds
     * @return return true if the data is a join of datasets of different cadences or lengths.
     */
    public static boolean isIrregularJoin(QDataSet ds) {
        return !DataSetUtil.isQube(ds);
    }
    
    /**
     * return a rank 3 irregular join of three datasets, 
     * the first is 13 records of 27 energies, 
     * the second is 13 records of 20 energies, and
     * the third is 14 records of 24 energies.
     * @return a rank 3 irregular join.
     */
    public static QDataSet irregularJoin() {
        return Ops.ripplesJoinSpectrogramTimeSeries(40);
    }
    
    /**
     * set of 2-d or 3-d points and the triangles connecting them.  Note that 
     * Ops.triangulate does not return this schema.  This is introduced to 
     * experiment with an oddly-structured dataset.  For example, this can
     * be a DEPEND_0 of a rank 1 rgb color dataset.
     * @return a triangular mesh.
     */
    public static QDataSet polyMesh() {
        QDataSet xy= xyScatter();
        QDataSet tri= Ops.triangulate( Ops.slice1(xy,0), Ops.slice1(xy,1) );
        QDataSet result= Ops.join(xy,tri);
        result= Ops.putProperty( result, QDataSet.RENDER_TYPE, QDataSet.VALUE_RENDER_TYPE_TRIANGLE_MESH );
        return result;
    }
    
    /**
     * return true if the data can be used as a triangulation or tessalation
     * of 4-point rectangles.
     * @param ds
     * @return 
     * @see Ops#polyCenters(org.das2.qds.QDataSet) 
     * @see https://sourceforge.net/p/autoplot/feature-requests/819/
     */
    public static boolean isPolyMesh( QDataSet ds ) {
        if ( ds.rank()==3 && ds.length()==2 ) {
            int ndim= ds.slice(0).length(0); // xy or xyz points
            int ncorners= ds.slice(1).length(0); // are they triangles
            if ( ndim==2 || ndim==3 ) {
                if ( ncorners==3 ) { 
                    return true; 
                } else if ( ncorners==4 ) {
                    return true; // note the triangulation code we have supports four-point tetrahedra.
                } else if ( ncorners>4 ) {
                    return true; // support general polygons
                } else {
                    return false;  
                }
            }
        }
        return false;
    }
}
