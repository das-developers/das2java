package edu.uiowa.physics.pw.das.graph;

import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.datum.format.*;
import edu.uiowa.physics.pw.das.util.*;
import java.text.ParseException;
import java.util.*;

/** A TickVDescriptor describes the position that ticks
 * should be drawn, so that a fairly generic tick drawing routine
 * can be used for multiple types of axes.
 *
 */
public class TickVDescriptor {
    
    DatumVector tickV;
    DatumVector minorTickV;
    
    Units units = null;
    DatumFormatter datumFormatter;
    
    /** This constructor is to support the use when tickVDescriptor was
     * internal to DasAxis.
     */
    protected TickVDescriptor() {
    }
    
    public TickVDescriptor( double[] minorTicks, double[] ticks, Units units ) {
        this.tickV= DatumVector.newDatumVector(ticks, units);
        this.minorTickV= DatumVector.newDatumVector(minorTicks, units);
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
        return tickV;
    }
    
    public DatumVector getMinorTicks() {
        return minorTickV;
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
        
        res.tickV= DatumVector.newDatumVector(result, res.units);
        
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
        
        res.minorTickV= DatumVector.newDatumVector(minorTickV, res.units);
        
        return res;
        
    }
    
    private static final DatumFormatter DEFAULT_LOG_FORMATTER;
    static {
        try {
            DatumFormatterFactory factory
                    =DefaultDatumFormatterFactory.getInstance();
            DEFAULT_LOG_FORMATTER= factory.newFormatter("0E0");
        }
        catch (ParseException pe) {
            throw new RuntimeException(pe);
        }
    }
    
    public static TickVDescriptor bestTickVLogNew( Datum minD, Datum maxD, int nTicksMin, int nTicksMax ) {
        
        TickVDescriptor ticks= new TickVDescriptor();
        ticks.units= minD.getUnits();
        double min= minD.doubleValue(ticks.units);
        double max= maxD.doubleValue(ticks.units);
        
        if ( ( max / min ) < 10.5 ) {
            return bestTickVLinear( minD, maxD, nTicksMin, nTicksMax );
        }
        
        if ( DasMath.log10( max / min ) > nTicksMax) {
            Units units = minD.getUnits();
            Datum logMinD= units.createDatum(DasMath.log10(min));
            Datum logMaxD= units.createDatum(DasMath.log10(max));
            TickVDescriptor linTicks= bestTickVLinear( logMinD, logMaxD, nTicksMin, nTicksMax );
            double[] tickV = linTicks.tickV.toDoubleArray(linTicks.units);

            // copy over the ticks into the linear space, but cull the fractional ones
            int i2=0;
            for ( int i=0; i<tickV.length; i++ ) {
                if ( tickV[i] % 1. == 0. ) {
                    tickV[i2++]= DasMath.exp10( tickV[i] );
                }
            }
            double[] t= tickV;
            tickV= new double[i2];
            for ( int i=0; i<i2; i++ ) { tickV[i]=t[i]; }
                    
            // now fill in the minor ticks, if there's room
            int idx=0;
            double[] minorTickV;
            if ( ( tickV[1]/tickV[0] ) <= 10.00001 ) {
                minorTickV= new double[(tickV.length+1)*9];
                for ( int j=2; j<10; j++ ) {
                    minorTickV[idx++]= j*(tickV[0]/10);
                }
                for ( int i=0; i<tickV.length; i++ ) {
                    for ( int j=2; j<10; j++ ) {
                        minorTickV[idx++]= j*tickV[i];
                    }
                }
            } else {
                minorTickV = linTicks.minorTickV.toDoubleArray(linTicks.units);
                for ( int i=0; i<minorTickV.length; i++ ) {
                    minorTickV[i]= DasMath.exp10( minorTickV[i] );
                }
            }
            
            linTicks.tickV = DatumVector.newDatumVector(tickV, linTicks.units);
            linTicks.minorTickV= DatumVector.newDatumVector(minorTickV, linTicks.units);
            linTicks.datumFormatter= DEFAULT_LOG_FORMATTER;
            return linTicks;
        }
        
        double dMinTick= DasMath.roundNFractionalDigits(DasMath.log10(min),4);
        int minTick= (int)Math.ceil(dMinTick);
        double dMaxTick= DasMath.roundNFractionalDigits(DasMath.log10(max),4);
        int maxTick= (int)Math.floor(dMaxTick);
        
        int nTicks= ( maxTick - minTick ) + 1;
        
        double[] major;  // major ticks labels
        double[] minors; // minor ticks to label -- {}, or { 2,3,4,5,6,7,8,9 }! !!
        
        major= new double[nTicks];
        for (int i=0; i<nTicks; i++) {
            major[i]= i+minTick;
        }
        minors= new double[] { 2,3,4,5,6,7,8,9 };
        
        ticks.datumFormatter= DEFAULT_LOG_FORMATTER;
        
        int firstMinorTickCycle= (int)Math.floor(DasMath.log10(min));
        int lastMinorTickCycle= (int)Math.floor(DasMath.log10(max));
        
        double[] minorTickV=null;
        int idx=0;
        minorTickV= new double[(lastMinorTickCycle-firstMinorTickCycle+1)*minors.length];
        for ( int i=firstMinorTickCycle; i<=lastMinorTickCycle; i++ ) {
            for ( int j=0; j<minors.length; j++ ) {
                minorTickV[idx++]= DasMath.exp10(i) * minors[j];
            }
        }
        ticks.minorTickV = DatumVector.newDatumVector(minorTickV, ticks.units);

        for ( int i=0; i<major.length; i++ ) {
            major[i]= DasMath.exp10( major[i] );
        }
        ticks.tickV= DatumVector.newDatumVector(major, ticks.units);
        
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
        
        if ( maxD.subtract(minD).gt( Datum.create(10*365,Units.days)) ) {
            int yearMin= TimeUtil.toTimeStruct(minD).year;
            int yearMax= TimeUtil.toTimeStruct(maxD).year;
            TickVDescriptor yearTicks= bestTickVLinear( Units.dimensionless.createDatum(yearMin), 
                Units.dimensionless.createDatum(yearMax), nTicksMin, nTicksMax );
            yearTicks.units= minD.getUnits();
            double[] tickV = yearTicks.tickV.toDoubleArray(Units.dimensionless);
            for ( int i=0; i<tickV.length; i++ ) {
                int iyear= (int)tickV[i];                
                tickV[i]= TimeUtil.convert( iyear, 1, 1, 0, 0, 0, (TimeLocationUnits)yearTicks.units );
            }
            yearTicks.tickV = DatumVector.newDatumVector(tickV, yearTicks.units);
            double[] minorTickV = yearTicks.minorTickV.toDoubleArray(Units.dimensionless);
            for ( int i=0; i<minorTickV.length; i++ ) {
                int iyear= (int)minorTickV[i];                
                minorTickV[i]= TimeUtil.convert( iyear, 1, 1, 0, 0, 0, (TimeLocationUnits)yearTicks.units );                
            }
            yearTicks.minorTickV = DatumVector.newDatumVector(minorTickV, yearTicks.units);
            Datum t1= yearTicks.getMajorTicks().get(0);
            int nticks= yearTicks.getMajorTicks().getLength();
            Datum t2= yearTicks.getMajorTicks().get(nticks-1);
            yearTicks.datumFormatter= DatumUtil.bestTimeFormatter( t1, t2, nticks );
            return yearTicks;
        }
        double [] res_tickV;
        double [] res_minorTickV;
        Units res_units;
        DatumFormatter res_datumFormatter;
        
        double data_minimum = minD.doubleValue(Units.t2000);
        double data_maximum = maxD.doubleValue(Units.t2000);
        
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
            
            res_tickV= new double[nTicks];
            for (i=0; i<nTicks; i++)
                res_tickV[i]=firstTick+i*tickSize;
            
            minor= tickSize / nminor[ikeep];
            double firstMinor= minor * Math.ceil( ( data_minimum - axisLengthData ) / minor );
            double lastMinor= minor * Math.floor( ( data_maximum + axisLengthData ) / minor );
            int nMinor= ( int ) ( ( lastMinor - firstMinor ) / minor + 0.5 );
            double [] minorTickV= new double[ nMinor ];
            for ( int ii=0; ii<nMinor; ii++ ) minorTickV[ii]= firstMinor + ii*minor;
            
            res_minorTickV = minorTickV;
            
            Datum t1= Units.t2000.createDatum(firstTick);
            Datum t2= Units.t2000.createDatum(lastTick);
            res_datumFormatter = DatumUtil.bestFormatter( t1, t2, nTicks-1 );
            if ( res_datumFormatter == TimeDatumFormatter.HOURS ) {
                res_datumFormatter= TimeDatumFormatter.MINUTES;
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
            
            res_tickV = new double[ir];
            for (ir=0; ir<res_tickV.length; ir++) {
                res_tickV[ir]= result[ir];
            }
            
            current = firstTickDatum;
            while(lastTickDatum.ge(current)) {
                minorTickV.add( current );
                current= TimeUtil.next(minorStep,current);
                for (int ii=minorNStep; ii>1; ii--) current= TimeUtil.next(minorStep,current);
            }
            
            res_minorTickV= new double[minorTickV.size()];
            for ( int ii=0; ii<minorTickV.size(); ii++ )
                res_minorTickV[ii]= ((Datum)minorTickV.get(ii)).doubleValue(Units.t2000);
            
            res_datumFormatter = DatumUtil.bestFormatter( firstTickDatum, lastTickDatum, 6 );
            
        }
        
        res_units= minD.getUnits();
        UnitsConverter uc= Units.getConverter(Units.t2000, res_units);
        for (int ii=0; ii<res_tickV.length; ii++) {
            res_tickV[ii]= uc.convert(res_tickV[ii]);
        }
        for (int ii=0; ii<res_minorTickV.length; ii++) {
            res_minorTickV[ii]= uc.convert(res_minorTickV[ii]);
        }
        
        /*
        res.tickV = DatumVector.newDatumVector(res_tickV, res.units);
        res.minorTickV = DatumVector.newDatumVector(res_minorTickV, res.units);
         */
        
        TickVDescriptor res = new TickVDescriptor(res_minorTickV, res_tickV, res_units);
        res.setFormatter(res_datumFormatter);
        
        return res;
        
    }
    
}

