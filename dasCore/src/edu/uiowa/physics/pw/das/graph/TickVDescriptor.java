package edu.uiowa.physics.pw.das.graph;

import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.datum.format.*;
import edu.uiowa.physics.pw.das.util.*;
import java.util.*;

/** A TickVDescriptor describes the position that ticks
 * should be drawn, so that a fairly generic tick drawing routine
 * can be used for multiple types of axes.
 *
 */
public class TickVDescriptor {
    
    double[] tickV = null;
    double[] minorTickV = null;
    Units units = null;
    DatumFormatter datumFormatter;
    
    /** This constructor is to support the use when tickVDescriptor was
     * internal to DasAxis.
     */
    protected TickVDescriptor() {
    }
    
    public TickVDescriptor( double[] minorTicks, double[] ticks, Units units ) {
        this.tickV= ticks;
        this.minorTickV= minorTicks;
        this.units= units;
        this.datumFormatter= DefaultDatumFormatterFactory.getInstance().defaultFormatter();
    }
    
    public DatumVector getMajorTicks() {
        return DatumVector.newDatumVector( tickV, units );
    }
    
    public DatumVector getMinorTicks() {
        return DatumVector.newDatumVector( minorTickV, units );
    }
    
    public DatumFormatter getFormatter() {
        return this.datumFormatter;
    }
    
    /** Returns a String representation of the TickVDescriptor.
     * @return a String representation of the TickVDescriptor.
     *
     */
    public String toString() {
        String s="tickV=[";
        for (int i=0; i<tickV.length; i++) s+=datumFormatter.format(units.createDatum(tickV[i]))+", ";
        s+="],minor=";
        for (int i=0; i<minorTickV.length; i++) s+=minorTickV[i]+", ";
        s+="]";
        return s;
    }
    
    public static TickVDescriptor bestTickVLinear( Datum min, Datum max, int nTicksMax ) {
        
        TickVDescriptor res= new TickVDescriptor();
        
        res.units= min.getUnits();
        double minimum= min.doubleValue(res.units);
        double maximum= max.doubleValue(res.units);
        
        double maj= (maximum-minimum)/nTicksMax;
        double mag= DasMath.exp10(Math.floor(DasMath.log10(maj)));
        double absissa= maj/mag;
        
        if (absissa<1.666) absissa=1.0;
        else if (absissa<3.333) absissa=2.0;
        else absissa=5.0;
        
        double axisLengthData= maximum-minimum;
        
        double tickSize= absissa * mag;
        
        double firstTick= tickSize*Math.ceil( ( minimum - axisLengthData ) / tickSize - 0.01 );
        double lastTick= tickSize*Math.floor( ( maximum + axisLengthData ) / tickSize + 0.01 );
        
        int nTicks= 1+(int)Math.round((lastTick-firstTick)/tickSize);
        
        double [] result= new double[nTicks];
        for (int i=0; i<nTicks; i++) result[i]=firstTick+i*tickSize;
        
        res.tickV= result;
        
        double minor;
        if (absissa==5.) {
            minor= tickSize/5;
        } else if ( absissa==2. ) {
            minor= tickSize/2;
        } else {
            minor= tickSize/4;
        }
        
        int ifirst= nTicks/3;
        int ilast= 2*nTicks/3;
        
        res.datumFormatter = DatumUtil.bestFormatter( res.units.createDatum(result[ifirst]),
        res.units.createDatum(result[ilast]), ilast-ifirst );
        
        double firstMinor= minor * Math.ceil( ( minimum - axisLengthData ) / minor );
        double lastMinor= minor * Math.floor( ( maximum + axisLengthData ) / minor );
        int nMinor= ( int ) ( ( lastMinor - firstMinor ) / minor + 0.5 );
        double [] minorTickV= new double[ nMinor ];
        for ( int i=0; i<nMinor; i++ ) minorTickV[i]= firstMinor + i*minor;
        
        res.minorTickV= minorTickV;
        
        return res;
        
    }
    
    public static TickVDescriptor bestTickVLog( Datum minD, Datum maxD, int nTicksMax ) {
        
        TickVDescriptor ticks= new TickVDescriptor();
        ticks.units= minD.getUnits();
        double min= minD.doubleValue(ticks.units);
        double max= maxD.doubleValue(ticks.units);
        
        double [] result;
        double dMinTick= DasMath.roundNFractionalDigits(DasMath.log10(min),4);
        int minTick= (int)Math.ceil(dMinTick);
        double dMaxTick= DasMath.roundNFractionalDigits(DasMath.log10(max),4);
        int maxTick= (int)Math.floor(dMaxTick);
        
        int stepSize= 1;
        
        int nTicks= ( maxTick - minTick ) / stepSize + 1;
        
        double[] major;
        double[] mantissas;
        DatumFormatter formatter;
        DatumFormatterFactory factory = ticks.units.getDatumFormatterFactory();
        
        try {
            if ( nTicks<2 ) {
                if ( min >= DasMath.exp10(maxTick) ||  DasMath.exp10(maxTick) >= max ) {
                    major= new double[] { min, (min+max)/2, max };
                } else {
                    major= new double[] { min, DasMath.exp10(maxTick), max };
                }
                formatter= DatumUtil.bestFormatter( DatumVector.newDatumVector( major, ticks.units ) );
                mantissas= new double[] { 2,3,4,5,6,7,8,9 };
            } else  if ( nTicks<3 ) {                
                double[] mant;
                if ( nTicksMax>5 ) {
                    mant= new double[] { 2,3,4,6,8 };
                    mantissas= new double[] { 5,7,9 };
                } else if ( nTicksMax>3 ) {
                    mant= new double[] { 2, 5 };
                    mantissas= new double[] { 3,4,6,7,8,9 };
                } else {
                    mant= new double[] { 3 };
                    mantissas= new double[] { 2,4,5,6,7,8,9 };
                }
                minTick= minTick-1;
                nTicks++;
                major= new double[nTicks*(1+mant.length)];
                int idx= 0;
                for (int i=0; i<nTicks; i++) {
                    major[idx++]= DasMath.exp10(i*stepSize+minTick);
                    for ( int j=0; j<mant.length; j++ ) {
                        major[idx++]= mant[j]*DasMath.exp10(i*stepSize+minTick);
                    }
                }                
                formatter= factory.newFormatter("0E0");
            } else if ( nTicks>nTicksMax ) {
                stepSize= (int)Math.ceil( nTicks / (double)nTicksMax );
                minTick= (int)( Math.floor( minTick / (double) stepSize ) * stepSize );
                maxTick= (int)( Math.ceil(maxTick / (double)stepSize ) * stepSize );
                nTicks= ( maxTick-minTick ) / stepSize + 1 ;
                major= new double[ nTicks ] ;
                for ( int i=0; i<nTicks; i++ ) {
                    major[i]= DasMath.exp10( i*stepSize + minTick );
                }
                mantissas= new double[0];
                formatter= factory.newFormatter("0E0");
            } else {
                major= new double[nTicks];
                for (int i=0; i<nTicks; i++) {
                    major[i]= DasMath.exp10(i*stepSize+minTick);
                }
                mantissas= new double[] { 2,3,4,5,6,7,8,9 };
                formatter= factory.newFormatter("0E0");
            }
            
        } catch ( java.text.ParseException e ) {
            throw new RuntimeException(e);
        }
        ticks.tickV= major;
        ticks.datumFormatter= formatter;
        
        int firstMinorTickCycle= (int)Math.floor(DasMath.log10(min));
        int lastMinorTickCycle= (int)Math.floor(DasMath.log10(max));
        
        int idx=0;
        double [] minorTickV= new double[(lastMinorTickCycle-firstMinorTickCycle+1)*mantissas.length];
        for ( int i=firstMinorTickCycle; i<=lastMinorTickCycle; i++ ) {
            for ( int j=0; j<mantissas.length; j++ ) {
                minorTickV[idx++]= DasMath.exp10(i) * mantissas[j];
            }
        }
        ticks.minorTickV= minorTickV;
        
        if ( minTick>=maxTick ) {
            double[] majorTicks= new double[ ticks.tickV.length + ticks.minorTickV.length ];
            for ( int i=0; i<ticks.tickV.length; i++ ) majorTicks[i]= ticks.tickV[i];
            for ( int i=0; i<ticks.minorTickV.length; i++ ) majorTicks[i+ticks.tickV.length]= ticks.minorTickV[i];
        }
        
        return ticks;
        
    }
    
    public static TickVDescriptor bestTickVTime( Datum minD, Datum maxD, int nTicksMax ) {
        
        Datum minute = Datum.create(60.0, Units.seconds);
        if (maxD.subtract(minD).lt(minute)) {
            return bestTickVLinear( minD, maxD, nTicksMax );
        }
        
        TickVDescriptor res= new TickVDescriptor();
        
        double data_minimum = minD.doubleValue(Units.t2000);
        double data_maximum = maxD.doubleValue(Units.t2000);
        
        double [] tickV;
        
        double[] mags12= {
            0.001, 0.002, 0.005,
            0.01, 0.02, 0.05,
            0.1, 0.2, 0.5,
            1, 2, 5, 10, 30,
            60, 120, 300, 600, 1200,
            3600, 7200, 10800, 14400, 21600, 28800, 43200, //1hr, 2hr, 3hr, 4hr, 6hr, 8hr, 12hr
            86400, 172800, 86400*5, 86400*10
        };
        
        int[] nminor= {
            4, 4, 5,
            4, 4, 5,
            4, 4, 5,
            4, 4, 5, 5, 3,
            6, 4, 5, 5, 4,
            4, 4, 3, 4, 3, 4, 6,
            4, 2, 5, 10
        };
        
        double mag_keep=-1;
        double absissa;
        double mag;
        
        double tickSize, firstTick, lastTick;
        int nTicks;
        double minor;
        
        int i=0;
        int ikeep=-1;
        
        if ((data_maximum-data_minimum)>86400) i=4; // get it past the very small ticks to avoid rollover error
        while (i<mags12.length && ikeep==-1) {
            
            mag= mags12[i];
            
            tickSize= mag;
            
            firstTick= tickSize*Math.ceil((data_minimum)/tickSize);
            lastTick= tickSize*Math.floor((data_maximum)/tickSize);
            
            if ( (lastTick-firstTick)/tickSize > 1000 ) {
                nTicks= 1000; // avoid intger roll-over
            } else {
                nTicks= 1+(int)((lastTick-firstTick)/tickSize);
            }
            
            if (nTicks<nTicksMax) {
                ikeep= i;
            }
            i++;
        }
        
        if (ikeep!=-1) {
            mag_keep= mags12[ikeep];
            absissa= 1.0;
            
            tickSize= absissa * mag_keep;
            
            double axisLengthData= ( data_maximum - data_minimum );
            
            firstTick= tickSize*Math.ceil((data_minimum - axisLengthData)/tickSize);
            lastTick= tickSize*Math.floor((data_maximum + axisLengthData)/tickSize);
            
            nTicks= 1+(int)Math.round((lastTick-firstTick)/tickSize);
            if (nTicks<2) {
                edu.uiowa.physics.pw.das.util.DasDie.println("Only able to find one major tick--sorry! ");
                edu.uiowa.physics.pw.das.util.DasDie.println("please let us know how you entered this condition");
                nTicks=2;
            }
            
            tickV= new double[nTicks];
            for (i=0; i<nTicks; i++)
                tickV[i]=firstTick+i*tickSize;
            
            res.tickV= tickV;
            
            minor= tickSize / nminor[ikeep];
            double firstMinor= minor * Math.ceil( ( data_minimum - axisLengthData ) / minor );
            double lastMinor= minor * Math.floor( ( data_maximum + axisLengthData ) / minor );
            int nMinor= ( int ) ( ( lastMinor - firstMinor ) / minor + 0.5 );
            double [] minorTickV= new double[ nMinor ];
            for ( int ii=0; ii<nMinor; ii++ ) minorTickV[ii]= firstMinor + ii*minor;
            
            res.minorTickV= minorTickV;
            
            Datum t1= Units.t2000.createDatum(firstTick);
            Datum t2= Units.t2000.createDatum(lastTick);
            res.datumFormatter = DatumUtil.bestFormatter( t1, t2, nTicks-1 );
            if ( res.datumFormatter == TimeDatumFormatter.HOURS ) {
                res.datumFormatter= TimeDatumFormatter.MINUTES;
            }
            
        } else  { // pick off month boundaries
            double [] result= new double[30];
            ArrayList minorTickV= new ArrayList();
            int ir=0;
            Datum current;
            Datum min= Datum.create( data_minimum,Units.t2000 );
            Datum max= Datum.create( data_maximum,Units.t2000 );
            int step;
            int nstep=1;
            int minorStep;  // step size for minor ticks
            int minorNStep=1;    // multiplier for step size
            if ((data_maximum-data_minimum)<86400*30*6) {  // months
                step= TimeUtil.MONTH;
                minorStep= TimeUtil.DAY;
                minorNStep=1;
            } else if ((data_maximum-data_minimum)<86400*30*25) { // seasons
                step= TimeUtil.QUARTER;
                minorStep= TimeUtil.MONTH;
            } else if ((data_maximum-data_minimum)<86400*365*6) { // years
                step= TimeUtil.YEAR;
                minorStep= TimeUtil.MONTH;
            } else {
                // TODO fall back to decimal years
                step= TimeUtil.YEAR;
                minorStep= TimeUtil.YEAR;
                nstep= 2;
            }
            
            Datum firstTickDatum= TimeUtil.prev(step,TimeUtil.prev(step,min));
            Datum lastTickDatum= TimeUtil.next(step,TimeUtil.next(step,max));
            
            current= firstTickDatum;
            
            while(lastTickDatum.ge(current)) {
                result[ir++]= current.doubleValue(Units.t2000);
                current= TimeUtil.next(step,current);
                for (int ii=nstep; ii>1; ii--) current= TimeUtil.next(step,current);
            }
            
            res.tickV= new double[ir];
            for (ir=0; ir<res.tickV.length; ir++) res.tickV[ir]= result[ir];
            
            current = firstTickDatum;
            while(lastTickDatum.ge(current)) {
                minorTickV.add( current );
                current= TimeUtil.next(minorStep,current);
                for (int ii=minorNStep; ii>1; ii--) current= TimeUtil.next(minorStep,current);
            }
            
            res.minorTickV= new double[minorTickV.size()];
            for ( int ii=0; ii<minorTickV.size(); ii++ )
                res.minorTickV[ii]= ((Datum)minorTickV.get(ii)).doubleValue(Units.t2000);
            
            res.datumFormatter = DatumUtil.bestFormatter( firstTickDatum, lastTickDatum, 6 );
            
        }
        
        res.units= minD.getUnits();
        UnitsConverter uc= Units.getConverter(Units.t2000, res.units);
        for (int ii=0; ii<res.tickV.length; ii++) {
            res.tickV[ii]= uc.convert(res.tickV[ii]);
        }
        for (int ii=0; ii<res.minorTickV.length; ii++) {
            res.minorTickV[ii]= uc.convert(res.minorTickV[ii]);
        }
        
        return res;
        
    }
    
}

