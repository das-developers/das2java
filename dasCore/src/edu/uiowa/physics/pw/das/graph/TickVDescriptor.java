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
    
    public static TickVDescriptor newTickVDescriptor( DatumVector majorTicks, DatumVector minorTicks ) {
        Units units= majorTicks.getUnits();
        double[] minor= minorTicks.toDoubleArray(units);
        double[] major= majorTicks.toDoubleArray(units);
        return new TickVDescriptor( minor, major, units );
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
    
    public void setFormatter(DatumFormatter datumFormatter) {
        this.datumFormatter = datumFormatter;
    }
    
    /** Returns a String representation of the TickVDescriptor.
     * @return a String representation of the TickVDescriptor.
     *
     */
    public String toString() {        
        String s="tickV=" + getMajorTicks();        
        s+=",minor=" + getMinorTicks();                
        return s;
    }
    
    public static TickVDescriptor bestTickVLinear( Datum min, Datum max, int nTicksMin, int nTicksMax ) {
        
        TickVDescriptor res= new TickVDescriptor();
        
        res.units= min.getUnits();
        double minimum= min.doubleValue(res.units);
        double maximum= max.doubleValue(res.units);
        
        int targetTicks= Math.max( Math.min( 6, nTicksMax ), nTicksMin );
        
        double maj= (maximum-minimum)/targetTicks;
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
    
    
    public static TickVDescriptor bestTickVLogNew( Datum minD, Datum maxD, int nTicksMin, int nTicksMax ) {
        
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
        
        double[] major;  // major ticks labels
        double[] minors; // minor ticks to label -- {}, or { 2,3,4,5,6,7,8,9 }! !!
        DatumFormatter formatter;
        DatumFormatterFactory factory = ticks.units.getDatumFormatterFactory();
        
        
        try {
            if ( ( max / min ) < 10.5 ) {
                return bestTickVLinear( minD, maxD, nTicksMin, nTicksMax );
            } else if ( nTicksMin <= nTicks && nTicks <= nTicksMax ) {
                major= new double[nTicks];
                for (int i=0; i<nTicks; i++) {
                    major[i]= i*stepSize+minTick;
                }
                minors= new double[] { 2,3,4,5,6,7,8,9 };
                formatter= factory.newFormatter("0E0");
            } else if ( nTicksMin > nTicks ) {
                stepSize= (int) Math.floor( nTicks / (double)nTicksMin );
                stepSize= stepSize < 1 ? 1 : stepSize;
                minTick= (int)( Math.floor( minTick / (double) stepSize ) * stepSize );
                maxTick= (int)( Math.ceil(maxTick / (double)stepSize ) * stepSize );
                nTicks= ( maxTick-minTick ) / stepSize + 1 ;
                major= new double[ nTicks ] ;
                for ( int i=0; i<nTicks; i++ ) {
                    major[i]= i*stepSize + minTick;
                }
                minors= new double[] { 2,3,4,5,6,7,8,9 };
                formatter= factory.newFormatter("0E0");
            } else {
                Units units = minD.getUnits();
                Datum logMinD= units.createDatum(DasMath.log10(min));
                Datum logMaxD= units.createDatum(DasMath.log10(max));
                TickVDescriptor linTicks= bestTickVLinear( logMinD, logMaxD, nTicksMin, nTicksMax );
                for ( int i=0; i<linTicks.tickV.length; i++ ) linTicks.tickV[i]= DasMath.exp10( linTicks.tickV[i] );
                int idx=0;
                if ( ( linTicks.tickV[1]/linTicks.tickV[0] ) <= 10.00001 ) {
                    double [] newMinorTicks;
                    newMinorTicks= new double[(linTicks.tickV.length+1)*9];
                    for ( int j=2; j<10; j++ ) {
                        newMinorTicks[idx++]= j*(linTicks.tickV[0]/10);
                    }
                    for ( int i=0; i<linTicks.tickV.length; i++ ) {
                        for ( int j=2; j<10; j++ ) {
                            newMinorTicks[idx++]= j*linTicks.tickV[i];
                        }
                    }
                    linTicks.minorTickV= newMinorTicks;         
                } else {
                    for ( int i=0; i<linTicks.minorTickV.length; i++ ) linTicks.minorTickV[i]= DasMath.exp10( linTicks.minorTickV[i] );
                }
                linTicks.datumFormatter= factory.newFormatter("0E0");
                return linTicks;
            }
        } catch ( java.text.ParseException e ) {
            throw new RuntimeException(e);
        }
        
        ticks.datumFormatter= formatter;
        
        int firstMinorTickCycle= (int)Math.floor(DasMath.log10(min));
        int lastMinorTickCycle= (int)Math.floor(DasMath.log10(max));
        
        double[] minorTickV=null;
        if ( minors.length>0 ) {
            int idx=0;
            minorTickV= new double[(lastMinorTickCycle-firstMinorTickCycle+1)*minors.length];
            for ( int i=firstMinorTickCycle; i<=lastMinorTickCycle; i++ ) {
                for ( int j=0; j<minors.length; j++ ) {
                    minorTickV[idx++]= DasMath.exp10(i) * minors[j];
                }
            }
        } else {
            if ( stepSize>1 ) { // put in minor ticks at cycle boundaries
                minorTickV= new double[ (lastMinorTickCycle-firstMinorTickCycle+1) - major.length + 1 ];
                int imajor=0;
                int idx=0;
                int i= firstMinorTickCycle;
                while ( idx<minorTickV.length ) {
                    if ( major[imajor]==i ) {
                        imajor++;
                    } else {
                        minorTickV[idx]= DasMath.exp10(i);
                        idx++;
                    }
                    i++;
                }
            }
        }
        ticks.minorTickV= minorTickV;
        for ( int i=0; i<major.length; i++ ) {
            major[i]= DasMath.exp10( major[i] );
        }
        ticks.tickV= major;
        
        if ( minTick>=maxTick ) {
            double[] majorTicks= new double[ ticks.tickV.length + ticks.minorTickV.length ];
            for ( int i=0; i<ticks.tickV.length; i++ ) majorTicks[i]= ticks.tickV[i];
            for ( int i=0; i<ticks.minorTickV.length; i++ ) majorTicks[i+ticks.tickV.length]= ticks.minorTickV[i];
        }
        
        return ticks;
        
    }
    
    public static TickVDescriptor bestTickVTime( Datum minD, Datum maxD, int nTicksMin, int nTicksMax ) {
        
        Datum length= maxD.subtract(minD);
        
        Datum minute = Datum.create(60.0, Units.seconds);
        if (maxD.subtract(minD).lt(minute)) {
            Datum base= TimeUtil.prevMidnight( minD );
            
            Units offUnits= Units.seconds;
            Datum offMin= minD.subtract(base).convertTo(offUnits);
            Datum offMax= maxD.subtract(base).convertTo(offUnits);
            TickVDescriptor offTicks= bestTickVLinear( offMin, offMax, nTicksMin, nTicksMax );
            
            DatumVector minorTicks= offTicks.getMinorTicks().add(base);
            DatumVector majorTicks= offTicks.getMajorTicks().add(base);
            
            TickVDescriptor result= TickVDescriptor.newTickVDescriptor( majorTicks, minorTicks );
            result.datumFormatter= DatumUtil.bestFormatter( majorTicks );
            return result;
        } 
        
        
        if ( length.gt( Datum.create( 1, Units.days ) ) && length.lt( Datum.create( 10, Units.days ) ) ) {
            
            Datum base= TimeUtil.prevMidnight( minD );
            
            Units offUnits= Units.days;
            Datum offMin= minD.subtract(base).convertTo(offUnits);
            Datum offMax= maxD.subtract(base).convertTo(offUnits);
            TickVDescriptor offTicks= bestTickVLinear( offMin, offMax, nTicksMin, nTicksMax );
            
            DatumVector minorTicks= offTicks.getMinorTicks().add(base);
            DatumVector majorTicks= offTicks.getMajorTicks().add(base);
            
            TickVDescriptor dayTicks= TickVDescriptor.newTickVDescriptor( majorTicks, minorTicks );
            dayTicks.datumFormatter= DatumUtil.bestFormatter( majorTicks );
            return dayTicks;
        }
        
        if ( maxD.subtract(minD).gt( Datum.create(10*365,Units.days)) ) {
            int yearMin= TimeUtil.toTimeStruct(minD).year;
            int yearMax= TimeUtil.toTimeStruct(maxD).year;
            TickVDescriptor yearTicks= bestTickVLinear( Units.dimensionless.createDatum(yearMin), 
                Units.dimensionless.createDatum(yearMax), nTicksMin, nTicksMax );
            yearTicks.units= minD.getUnits();
            for ( int i=0; i<yearTicks.tickV.length; i++ ) {
                int iyear= (int)yearTicks.tickV[i];                
                yearTicks.tickV[i]= TimeUtil.convert( iyear, 1, 1, 0, 0, 0, (TimeLocationUnits)yearTicks.units );
            }
            for ( int i=0; i<yearTicks.minorTickV.length; i++ ) {
                int iyear= (int)yearTicks.minorTickV[i];                
                yearTicks.minorTickV[i]= TimeUtil.convert( iyear, 1, 1, 0, 0, 0, (TimeLocationUnits)yearTicks.units );                
            }
            Datum t1= yearTicks.getMajorTicks().get(0);
            int nticks= yearTicks.getMajorTicks().getLength();
            Datum t2= yearTicks.getMajorTicks().get(nticks-1);
            yearTicks.datumFormatter= DatumUtil.bestTimeFormatter( t1, t2, nticks );
            return yearTicks;
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

