package org.das2.graph;

import org.das2.datum.format.DefaultDatumFormatterFactory;
import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.das2.datum.format.DatumFormatter;
import org.das2.datum.DatumVector;
import org.das2.datum.format.TimeDatumFormatter;
import org.das2.datum.Datum;
import org.das2.datum.DatumUtil;
import org.das2.datum.DatumRangeUtil;
import org.das2.datum.format.DatumFormatterFactory;
import org.das2.datum.TimeLocationUnits;
import org.das2.datum.TimeUtil;
import org.das2.util.DasMath;
import java.text.ParseException;
import java.util.*;
import org.das2.datum.DomainDivider;
import org.das2.datum.DomainDividerUtil;
import org.das2.datum.EnumerationUnits;
import org.das2.datum.UnitsUtil;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.ops.Ops;

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

    private static boolean dayOfYear= false;

    public static boolean isDayOfYear() {
        return dayOfYear;
    }

    public static void setDayOfYear(boolean dayOfYear) {
        TickVDescriptor.dayOfYear = dayOfYear;
    }

    /** This constructor is to support the use when tickVDescriptor was
     * internal to DasAxis.
     */
    protected TickVDescriptor() {
    }

    public TickVDescriptor(double[] minorTicks, double[] ticks, Units units) {
        this.tickV = DatumVector.newDatumVector(ticks, units);
        this.minorTickV = DatumVector.newDatumVector(minorTicks, units);
        this.units = units;
        this.datumFormatter = DefaultDatumFormatterFactory.getInstance().defaultFormatter();
    }

    /**
     * create the tickVDescriptor for a bunch of given ticks.  The first two ticks are used
     * to derive minor ticks, using the DomainDivider code.
     * @param ticks set of major ticks.
     */
    public TickVDescriptor( QDataSet ticks ) {
        Units u= SemanticOps.getUnits(ticks);
        this.units= u;
        double [] major= new double[ticks.length()];
        for ( int i=0; i<ticks.length(); i++ ) {
            major[i]= ticks.value(i);
        }
        this.tickV = DatumVector.newDatumVector(major,u);
        if ( ticks.length()>1 ) {
            Datum min= Ops.datum(ticks.slice(0));
            Datum max= Ops.datum(ticks.slice(1));
            if ( min.ge(max) ) throw new IllegalArgumentException("ticks must be monotonically increasing");
            DomainDivider dd= DomainDividerUtil.getDomainDivider( min, max );
            while ( dd.boundaryCount(min, max)<2 ) {
                dd= dd.finerDivider(false);
            }
            while ( dd.boundaryCount(min, max)>10 ) {
                dd= dd.coarserDivider(false);
            }
            ArrayList<Double> minTicks= new ArrayList<>();
            for ( int i=0; i<ticks.length()-1; i++ ) {
                min= Ops.datum(ticks.slice(i));
                max= Ops.datum(ticks.slice(i+1));
                DatumVector dv= dd.boundaries(min, max);
                for ( int j=0; j<dv.getLength(); j++ ) {
                    minTicks.add(dv.doubleValue(j, u));
                }
            }
            double[] minordd= new double[minTicks.size()];
            for ( int i=0; i<minordd.length; i++ ) {
                minordd[i]= minTicks.get(i);
            }
            this.minorTickV = DatumVector.newDatumVector(minordd, u);
        } else {
            this.minorTickV= this.tickV;
        }
        
        this.datumFormatter= DatumUtil.bestFormatter( this.tickV );
        
    }

    public static TickVDescriptor newTickVDescriptor(DatumVector majorTicks, DatumVector minorTicks) {
        Units units = majorTicks.getUnits();
        double[] minor = minorTicks.toDoubleArray(units);
        double[] major = majorTicks.toDoubleArray(units);
        return new TickVDescriptor(minor, major, units);
    }

    /**
     * creates descriptor with two Lists containing Datums.
     * java 1.5: {@code List<Datum>}
     * @param majorTicks 
     * @param minorTicks
     * @return 
     */
    public static TickVDescriptor newTickVDescriptor(List majorTicks, List minorTicks) {
        if (majorTicks.isEmpty() && minorTicks.isEmpty()) {
            throw new IllegalArgumentException("need at least one major or minor tick");
        }
        Datum d = (majorTicks.size() > 0) ? (Datum) majorTicks.get(0) : (Datum) minorTicks.get(0);
        Units u = d.getUnits();
        double[] major = new double[majorTicks.size()];
        for (int i = 0; i < major.length; i++) {
            major[i] = ((Datum) majorTicks.get(i)).doubleValue(u);
        }
        double[] minor = new double[minorTicks.size()];
        for (int i = 0; i < minor.length; i++) {
            minor[i] = ((Datum) minorTicks.get(i)).doubleValue(u);
        }
        return new TickVDescriptor(minor, major, u);
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

    /**
     * Locates the next or previous tick starting at xDatum.
     *
     * @param xDatum  find the tick closest to this.
     * @param direction  -1 previous, 1 next, 0 closest
     * @param minor  find closest minor tick, major if false.
     * @return the closest tick.  If there is no tick in the given direction, then
     *   the behavior is undefined.
     */
    public Datum findTick(Datum xDatum, double direction, boolean minor) {
        int majorLen;
        int minorLen;
        double[] ticks;

        // direction<0 nearest left, direction>0 nearest right, direction=0 nearest.
        if (tickV == null) {
            return xDatum;
        }
        majorLen = tickV.getLength();
        minorLen = minorTickV.getLength();
        ticks = new double[majorLen + minorLen];
        for (int i = 0; i < majorLen; i++) {
            ticks[i] = tickV.doubleValue(i, units);
        }
        for (int i = 0; i < minorLen; i++) {
            ticks[i + majorLen] = minorTickV.doubleValue(i, units);
        }

        int iclose = 0;
        double close = Double.MAX_VALUE;

        double x = xDatum.doubleValue(units);

        for (int i = 0; i < ticks.length; i++) {
            if (direction < 0 && ticks[i] < x && x - ticks[i] < close) {
                iclose = i;
                close = x - ticks[i];
            } else if (direction > 0 && x < ticks[i] && ticks[i] - x < close) {
                iclose = i;
                close = ticks[i] - x;
            }
            if (direction == 0 && Math.abs(ticks[i] - x) < close) {
                iclose = i;
                close = Math.abs(ticks[i] - x);
            }
        }

        return Datum.create(ticks[iclose], units);

    }

    /**
     * Defining method for getting the range close to the given range,
     * but containing at least one minor(or major) tick interval.
     *
     * @param dr
     * @param minor  find the range from the minor ticks.
     * @return 
     * @see DomainDivider
     */
    public DatumRange enclosingRange(DatumRange dr, boolean minor) {
        Datum s1 = findTick(dr.min(), 0, minor);
        Datum s2 = findTick(dr.max(), 0, minor);
        if (s1.equals(s2)) {
            s1 = findTick(dr.min(), -1, true);
            s2 = findTick(dr.max(), 1, true);
        }
        return new DatumRange(s1, s2);
    }

    public void setFormatter(DatumFormatter datumFormatter) {
        this.datumFormatter = datumFormatter;
    }

    /** Returns a String representation of the TickVDescriptor.
     * @return a String representation of the TickVDescriptor.
     *
     */
    @Override
    public String toString() {
        String s = "tickV=" + getMajorTicks();
        s += ",minor=" + getMinorTicks();
        return s;
    }

    /**
     * return a set of linear ticks, within the given constraints.
     * @param min the minimum
     * @param max the maximum
     * @param nTicksMin the minimum number of ticks.
     * @param nTicksMax the maximum number of ticks.
     * @param fin final, useful when debugging.
     * @return 
     */
    public static TickVDescriptor bestTickVLinear(Datum min, Datum max, int nTicksMin, int nTicksMax, boolean fin) {
        
        if ( min.ge(max) ) {
            throw new IllegalArgumentException("min ge max");
        }
        
        TickVDescriptor res = new TickVDescriptor();

        res.units = min.getUnits();
        double minimum = min.doubleValue(res.units);
        double maximum = max.doubleValue(res.units);

        int targetTicks = Math.max(Math.min(6, nTicksMax), nTicksMin);

        double maj;
        double absissa=0;
        double mag=0;
        int nTicks= 0;

        while ( nTicks<2 ) {
            maj = ( maximum - minimum) / (targetTicks - 1);
            mag = Math.pow(10,Math.floor(Math.log10(maj)));

            absissa = maj / mag;

            if (absissa < 1.666) {
                absissa = 1.0;
                //double abmag= absissa*mag;
                //maj = ( Math.floor(maximum/abmag)*abmag -  Math.ceil(minimum/abmag)*abmag ) / (targetTicks - 1);
            } else if (absissa < 3.333) {
                absissa = 2.0;
                //double abmag= absissa*mag;
                //maj = ( Math.floor(maximum/abmag)*abmag -  Math.ceil(minimum/abmag)*abmag ) / (targetTicks - 1);
            } else if (absissa < 9.0) {
                absissa = 5.0;
                //double abmag= absissa*mag;
                //maj = ( Math.floor(maximum/abmag)*abmag -  Math.ceil(minimum/abmag)*abmag ) / (targetTicks - 1);
            } else {
                absissa = 1.;
                mag *= 10;
                //double abmag= mag;
                //maj = ( Math.floor(maximum/abmag)*abmag -  Math.ceil(minimum/abmag)*abmag ) / (targetTicks - 1);
            }

            double majorTickSize = absissa * mag;
            double firstTick = majorTickSize * Math.ceil( minimum / majorTickSize );
            double lastTick =  majorTickSize * Math.floor( maximum / majorTickSize );
            nTicks= 1 + (int) Math.round((lastTick - firstTick) / majorTickSize);
            if ( nTicks<2 ) {
                targetTicks= targetTicks+1;
            }
        }

        if ( UnitsUtil.isNominalMeasurement(res.units) || UnitsUtil.isOrdinalMeasurement(res.units) ) {
            if ( mag<1 ) {
                mag= 1;
                absissa= 1;
            }
        }

        double axisLengthData = maximum - minimum;

        int minorPerMajor;

        if (absissa == 5.) {
            minorPerMajor = 5;
        } else if (absissa == 2.) {
            minorPerMajor = 2;
        } else {
            minorPerMajor = 10;
        }

        double minorTickSize = absissa * mag / minorPerMajor;
        double majorTickSize = minorTickSize * minorPerMajor;
        double firstTick = majorTickSize * Math.ceil((minimum - axisLengthData) / majorTickSize - 0.01);
        double lastTick = majorTickSize * Math.floor((maximum + axisLengthData) / majorTickSize + 0.01);

        if ( UnitsUtil.isNominalMeasurement(res.units) || UnitsUtil.isOrdinalMeasurement(res.units) ) {
            if ( minorTickSize<1 ) {
                minorTickSize= 1;
            }
        }
        nTicks = 1 + (int) Math.round((lastTick - firstTick) / majorTickSize);

        double[] result = new double[nTicks];
        for (int i = 0; i < nTicks; i++) {
            result[i] = firstTick + (i * minorPerMajor * minorTickSize);
        }
        res.tickV = DatumVector.newDatumVector(result, res.units);

        int ifirst = nTicks / 3;
        int ilast = 2 * nTicks / 3;

        if ( UnitsUtil.isNominalMeasurement(res.units) || UnitsUtil.isOrdinalMeasurement(res.units) ) {
            EnumerationUnits eu= (EnumerationUnits)res.units;
            Map<Integer,Datum> ords= eu.getValues();
            int imax= eu.getHighestOrdinal();
            for ( int i=0; i<result.length; i++ ) {
                while ( result[i]<=imax && !ords.containsKey((int)result[i]) ) result[i]= result[i]+1;
                if ( result[i]>imax ) result[i]=imax;
            }
            //System.err.println("here245");
            //new Exception("here245 indicates ordinal data").printStackTrace();

        }

        res.datumFormatter = DatumUtil.bestFormatter(res.units.createDatum(result[ifirst]),
                res.units.createDatum(result[ilast]), ilast - ifirst);

        double firstMinor = firstTick;
        double lastMinor = lastTick;
        int nMinor = (int) ((lastMinor - firstMinor) / minorTickSize + 0.5);
        double[] minorTickV = new double[nMinor];
        for (int i = 0; i < nMinor; i++) {
            minorTickV[i] = firstMinor + i * minorTickSize;
        }

        if ( UnitsUtil.isNominalMeasurement(res.units) || UnitsUtil.isOrdinalMeasurement(res.units) ) {
            EnumerationUnits eu= (EnumerationUnits)res.units;
            int imax= eu.getHighestOrdinal();
            Map<Integer,Datum> ords= eu.getValues();
            for ( int i=0; i<minorTickV.length; i++ ) {
                while ( minorTickV[i]<=imax && !ords.containsKey((int)minorTickV[i]) ) minorTickV[i]= minorTickV[i]+1;
                if ( minorTickV[i]>imax ) minorTickV[i]=imax;
            }
        }
        res.minorTickV = DatumVector.newDatumVector(minorTickV, res.units);

        return res;

    }
    private static final DatumFormatter DEFAULT_LOG_FORMATTER;
    

    static {
        try {
            DatumFormatterFactory factory = DefaultDatumFormatterFactory.getInstance();
            DEFAULT_LOG_FORMATTER = factory.newFormatter("0E0");
        } catch (ParseException pe) {
            throw new RuntimeException(pe);
        }
    }

    /**
     * return a set of log ticks, within the given constraints.
     * @param minD the minimum
     * @param maxD the maximum
     * @param nTicksMin the minimum number of ticks.
     * @param nTicksMax the maximum number of ticks.
     * @param fin final, useful when debugging.
     * @return
     */
    public static TickVDescriptor bestTickVLogNew(Datum minD, Datum maxD, int nTicksMin, int nTicksMax, boolean fin) {

        TickVDescriptor ticks = new TickVDescriptor();
        ticks.units = minD.getUnits();
        double min = minD.doubleValue(ticks.units);
        double max = maxD.doubleValue(ticks.units);

        if (max <= 0) {
            max = 100.;
        }
        if (min <= 0) {
            min = max / 1000.;
        }
        double logMin = Math.log10(min);
        double logMax = Math.log10(max);
        int ntick0 = (int) (Math.floor(logMax * 0.999) - Math.ceil(logMin * 1.001) + 1);

        if (ntick0 < 2) {
            TickVDescriptor result = bestTickVLinear(minD, maxD, nTicksMin, nTicksMax, fin);
            int ii = 0;

            DatumVector majortics = result.getMajorTicks();
            Units u = majortics.getUnits();

            while (ii < majortics.getLength() && majortics.get(ii).doubleValue(u) <= 0) {
                ii++;
            }
            majortics = majortics.getSubVector(ii, majortics.getLength());

            DatumVector minortics = result.getMinorTicks();
            while (ii < minortics.getLength() && minortics.get(ii).doubleValue(u) <= 0) {
                ii++;
            }
            minortics = minortics.getSubVector(ii, minortics.getLength());

            DatumFormatter df = result.datumFormatter;
            result = TickVDescriptor.newTickVDescriptor(majortics, minortics);
            result.datumFormatter = df;

            return result;

        }

        if (ntick0 > nTicksMax) {
            Units units = minD.getUnits();
            Datum logMinD = units.createDatum(Math.log10(min));
            Datum logMaxD = units.createDatum(Math.log10(max));
            TickVDescriptor linTicks = bestTickVLinear(logMinD, logMaxD, nTicksMin, nTicksMax, fin);
            double[] tickV = linTicks.tickV.toDoubleArray(linTicks.units);

            // copy over the ticks into the linear space, but cull the fractional ones
            int i2 = 0;
            for (int i = 0; i < tickV.length; i++) {
                if (tickV[i] % 1. == 0.) {
                    tickV[i2++] = Math.pow(10,tickV[i]);
                }
            }
            double[] t = tickV;
            tickV = new double[i2];
            System.arraycopy(t, 0, tickV, 0, i2);

            if ( tickV.length<2 ) {
                System.err.println("Unable to calculate linear ticks, less than 2 found.  Brace for crash.");
            }
            
            // now fill in the minor ticks, if there's room
            int idx = 0;
            double[] minorTickV;
            if ((tickV[1] / tickV[0]) <= 10.00001) {
                minorTickV = new double[(tickV.length + 1) * 9];
                for (int j = 2; j < 10; j++) {
                    minorTickV[idx++] = j * (tickV[0] / 10);
                }
                for (int i = 0; i < tickV.length; i++) {
                    for (int j = 2; j < 10; j++) {
                        minorTickV[idx++] = j * tickV[i];
                    }
                }
            } else {
                minorTickV = linTicks.minorTickV.toDoubleArray(linTicks.units);
                for (int i = 0; i < minorTickV.length; i++) {
                    minorTickV[i] = Math.pow(10,minorTickV[i]);
                }
            }

            linTicks.tickV = DatumVector.newDatumVector(tickV, linTicks.units);
            linTicks.minorTickV = DatumVector.newDatumVector(minorTickV, linTicks.units);
            linTicks.datumFormatter = DEFAULT_LOG_FORMATTER;
            return linTicks;
        }

        double min3 = min / (max / min);
        double max3 = max * (max / min);

        double dMinTick = DasMath.roundNFractionalDigits(Math.log10(min3), 4);
        int minTick = (int) Math.ceil(dMinTick);
        double dMaxTick = DasMath.roundNFractionalDigits(Math.log10(max3), 4);
        int maxTick = (int) Math.floor(dMaxTick);

        int nTicks = (maxTick - minTick) + 1;

        double[] major;  // major ticks labels
        double[] minors; // minor ticks to label -- {}, or { 2,3,4,5,6,7,8,9 }! !!

        major = new double[nTicks];
        for (int i = 0; i < nTicks; i++) {
            major[i] = i + minTick;
        }
        minors = new double[]{2, 3, 4, 5, 6, 7, 8, 9};

        ticks.datumFormatter = DEFAULT_LOG_FORMATTER;

        int firstMinorTickCycle = (int) Math.floor(Math.log10(min3));
        int lastMinorTickCycle = (int) Math.floor(Math.log10(max3));

        double[] minorTickV;
        int idx = 0;
        minorTickV = new double[(lastMinorTickCycle - firstMinorTickCycle + 1) * minors.length];
        for (int i = firstMinorTickCycle; i <= lastMinorTickCycle; i++) {
            for (int j = 0; j < minors.length; j++) {
                minorTickV[idx++] = Math.pow(10,i) * minors[j];
            }
        }
        ticks.minorTickV = DatumVector.newDatumVector(minorTickV, ticks.units);

        for (int i = 0; i < major.length; i++) {
            major[i] = Math.pow(10,major[i]);
        }
        ticks.tickV = DatumVector.newDatumVector(major, ticks.units);

        return ticks;

    }

    /**
     * find a divider that gives the biggest divisions for unitsPerDecade.  For example, we want to
     * divide a pizza evenly.  If the pizza has 12 pieces, then we can return 1,2,3,4 or 6.
     * If the pizza has 10 pieces, then we can return 1,2,or 5.
     * A minute has 60 seconds, so we can return 1,2,5,10,20,30.  (why not 6 or 12?  exclude param introduced)
     * A day had 24 hours, so we can return 1,2,4,6,or 12.
     * A circle had 360 degrees, so we can return 1,2,5,10,15,30,60,45,90,
     *
     * Find the biggest of tt that divides into unitsPerDecade and is less than sizeLimit.
     *
     * @param sizeLimit max number of pieces
     * @param factors number of divisions allowed.
     * @param exclude don't allow this one.
     */
    private static int getMantissa(int sizeLimit, int unitsPerDecade, int exclude) {
        int[] tt = new int[]{1, 2, 3, 5, 6, 10, 12, 15, 20, 25, 30, 45, 60, 90, 100, 200, 500, 1000, 2000, 5000, 10000};
        int biggest = 1;
        for (int i = 0; i < tt.length && tt[i] <= sizeLimit; i++) {
            if (unitsPerDecade % tt[i] == 0 && (exclude == 0 || tt[i] % exclude != 0)) {
                biggest = tt[i];
            }
        }
        return biggest;
    }

    /**
     * return list of mantissas as described in getMantissa.  The goal here is to 
     * return a list of mantissas that divide the pie into integer number of pieces
     * and in a number of divisions humans like.  THIS IS THE GENERAL CODE!!!
     *
     * If the pizza has 10 pieces, then we can return 1,2,or 5.
     * A minute has 60 seconds, so we can return 1,2,5,10,20,30.  (why not 6 or 12?  exclude param introduced)
     * A day had 24 hours, so we can return 1,2,4,6,or 12.
     * A circle had 360 degrees, so we can return 1,2,5,10,15,30,60,45,90,
     */
    private static List/*<Integer>*/ getMantissas(int divisionsPerDecade, int exclude, int include) {
        int[] tt = new int[]{1, 2, 3, 5, 6, 10, 12, 15, 20, 25, 30, 45, 60, 90, 100, 200, 500, 1000, 2000, 5000, 10000};
        //int biggest = 1;
        ArrayList result = new ArrayList();
        for (int i = 0; i < tt.length && tt[i] < divisionsPerDecade; i++) {
            boolean incl = include != 0 && tt[i] % include == 0;
            boolean excl = exclude != 0 && tt[i] % exclude == 0;
            if (excl && !incl) {
                continue;
            }
            if (divisionsPerDecade % tt[i] == 0) {
                result.add(tt[i]);
            }
        }
        return result;
    }

    /**
     * 
     * @param minD 
     * @param maxD 
     * @param units 
     * @param biggerUnits 
     * @param biggerUnitsCount kludge for when units==YEAR and biggerUnits==YEAR, so we can get "10 years"
     * @param unitLengthNanos 
     * @param mantissa 
     * @param fin true when this is called for the last time, for debugging.
     * @return 
     */
    private static TickVDescriptor countOffTicks2(Datum minD, Datum maxD,
            TimeUtil.TimeDigit units, TimeUtil.TimeDigit biggerUnits, int biggerUnitsCount,
            long unitLengthNanos, int mantissa, boolean fin) {
        DatumRange range = new DatumRange(minD, maxD);

        Datum majorTickLength = Units.nanoseconds.createDatum(unitLengthNanos * mantissa);

        Datum first;

        /**
         * next is the next major tick
         */
        Datum next;

        if (units == TimeUtil.TD_YEAR) {
            int iyear = TimeUtil.fromDatum(minD)[0];
            iyear = (iyear / biggerUnitsCount) * biggerUnitsCount;  // round to mantissa
            first = TimeUtil.createTimeDatum(iyear, 1, 1, 0, 0, 0, 0);
        } else {
            first = TimeUtil.prev(units.getOrdinal() - 1, minD);
        }


        next = TimeUtil.next(biggerUnits.getOrdinal(), first);
        for (int i = 1; i < biggerUnitsCount; i++) {
            next = TimeUtil.next(biggerUnits.getOrdinal(), next);
        }
        next = next.subtract(majorTickLength.divide(2)); // don't bump right up to it.

        ArrayList majorTicks = new ArrayList();
        ArrayList minorTicks = new ArrayList();
        Datum d = first;

        TimeUtil.TimeDigit minorUnits = units;
        int minorMantissa = 1;

        TimeUtil.TimeDigit majorUnits = units;
        int majorMantissa = mantissa;

        if (majorMantissa == 1) {
            minorUnits = TimeUtil.TimeDigit.fromOrdinal(majorUnits.getOrdinal() + 1);
            minorMantissa = majorUnits==TimeUtil.TD_MONTH ? 10 : majorUnits.divisions() / 4;
        }

        Datum nextMajorTick= TimeUtil.next( majorUnits, majorMantissa, d );
        
        if ( minorMantissa==0 ) {
            throw new RuntimeException("minorMantissa==0");
        }
        while (d.le(maxD)) {
            
            while (d.le(next)) {
                if (DatumRangeUtil.sloppyContains(range, d)) {
                    majorTicks.add(d);
                }
                nextMajorTick= TimeUtil.next(majorUnits, majorMantissa, d);
                while ( d.lt(nextMajorTick) ) {
                    if (DatumRangeUtil.sloppyContains(range, d)) {
                        minorTicks.add(d);
                    }
                    d = TimeUtil.next(minorUnits, minorMantissa, d);
                }
            }
            next = next.add(majorTickLength.divide(2));  // this is all to avoid March-30, April-1. 
            while (d.le(next)) {
                while ( d.lt(nextMajorTick) ) {
                    if (DatumRangeUtil.sloppyContains(range, d)) {
                        if (d.lt(next)) {
                            minorTicks.add(d); // so it doesn't get added twice
                        }
                    }
                    d = TimeUtil.next(minorUnits, minorMantissa, d);
                }
                nextMajorTick= TimeUtil.next(majorUnits, majorMantissa, d);
            }
            d = next;
            next = TimeUtil.next(majorUnits, majorMantissa, next);
            next = next.subtract(majorTickLength.divide(2));
        }

        return TickVDescriptor.newTickVDescriptor(majorTicks, minorTicks);

    }
    

    /**
     * return a set of ticks counting off ordinal time ranges, such as months, years, days, etc.
     * @param minD the minimum
     * @param maxD the maximum
     * @param nTicksMin the minimum number of ticks.
     * @param nTicksMax the maximum number of ticks.
     * @param fin final, useful when debugging.
     * @return 
     */
    public static TickVDescriptor bestTickVTimeOrdinal(Datum minD, Datum maxD, int nTicksMin, int nTicksMax, boolean fin) {

        //System.err.println( "bestTimeOrdinal: "+ new DatumRange(minD,maxD) + " "+nTicksMin+" "+nTicksMax );
        Datum lengthMin = maxD.subtract(minD).divide(nTicksMax + 1);  // this is the approximation--you can't simply divide
        Datum lengthMax = maxD.subtract(minD).divide(Math.max(1, nTicksMin - 1));

        long lengthNanosMax = (long) lengthMax.doubleValue(Units.nanoseconds);
        long lengthNanosMin = (long) lengthMin.doubleValue(Units.nanoseconds);

        TimeUtil.TimeDigit[] units = new TimeUtil.TimeDigit[]{TimeUtil.TD_NANO, TimeUtil.TD_SECOND,
            TimeUtil.TD_MINUTE, TimeUtil.TD_HOUR, TimeUtil.TD_DAY, TimeUtil.TD_MONTH, TimeUtil.TD_YEAR
        };
        long[] lengths = new long[]{1, (long) 1e9, (long) 60e9, (long) 3600e9, (long) 86400e9, 30 * (long) 86400e9, 365 * (long) 86400e9};
        int[] excludeFactors = new int[]{0, 6, 6, 0, 3, 0, 0};
        int[] includeFactors = new int[]{0, 30, 30, 0, 15, 0, 0};

        // find the range of units to try
        int biggestUnitIndex, smallestUnitIndex;
        int lessThanIndex = 0;
        while (lessThanIndex < units.length && lengths[lessThanIndex] < lengthNanosMax) {
            lessThanIndex++;
        }
        lessThanIndex--;
        biggestUnitIndex = lessThanIndex;

        lessThanIndex = 0;
        while (lessThanIndex < units.length && lengths[lessThanIndex] < lengthNanosMin) {
            lessThanIndex++;
        }
        lessThanIndex--;

        smallestUnitIndex = lessThanIndex;

        TickVDescriptor bestTickV = null;
        TickVDescriptor secondBestTickV = null; // fallback
        TimeUtil.TimeDigit bestUnit = null;
        TimeUtil.TimeDigit secondBestUnit = null;

        // loop over units and mantissas for each unit
        for (int iunit = smallestUnitIndex; bestTickV == null && iunit <= biggestUnitIndex; iunit++) {

            int mantissa;

            TimeUtil.TimeDigit biggerUnits;
            if (units[lessThanIndex] == TimeUtil.TD_YEAR) {
                biggerUnits = TimeUtil.TD_YEAR;
            } else {
                biggerUnits = units[lessThanIndex + 1];
            }

            List/*<Integer>*/ mantissas;

            if (units[iunit] != TimeUtil.TD_YEAR) {
                int factors;
                factors = (int) (lengths[iunit + 1] / lengths[iunit]);
                mantissas = getMantissas(factors, excludeFactors[lessThanIndex], includeFactors[lessThanIndex]);
            } else {
                int factors = 10;
                mantissas = getMantissas(factors, excludeFactors[lessThanIndex], includeFactors[lessThanIndex]);
            }

            TickVDescriptor test;

            for (int imant = 0; imant < mantissas.size(); imant++) {
                mantissa = ((Integer) mantissas.get(imant));

                int biggerUnitsCount = units[iunit] == biggerUnits ? mantissa : 1;

                DatumRange visibleRange= new DatumRange( minD, maxD );
                DatumRange ticksRange= fin ? DatumRangeUtil.rescale( visibleRange, -1.0, 2.0 ) : visibleRange;

                test = countOffTicks2( ticksRange.min(), ticksRange.max(), units[iunit], biggerUnits, biggerUnitsCount, lengths[lessThanIndex], mantissa, fin);
                //    // for debugging
                //if (fin && test.tickV.getLength() <= nTicksMax) {
                //    test = countOffTicks2(ticksRange.min(), ticksRange.max(), units[iunit], biggerUnits, biggerUnitsCount, lengths[lessThanIndex], mantissa, fin);
                //}
                //if (!checkMono(test.getMinorTicks())) {
                //    test = countOffTicks2(minD, maxD, units[iunit], biggerUnits, biggerUnitsCount, lengths[lessThanIndex], mantissa, fin);
                //}
                int nticks= fin ? test.tickV.getLength() / 3 : test.tickV.getLength();
                if ( nticks <= nTicksMax && nticks >= nTicksMin) {
                    bestTickV = test;
                    bestUnit = units[iunit];
                    break;
                }

                if ( nticks >= nTicksMin) {  // this is our back up.
                    secondBestTickV = test;
                    secondBestUnit = units[iunit];
                }

            }

        }

        if (bestTickV == null) {
            bestUnit = secondBestUnit;
            bestTickV = secondBestTickV;
            if ( bestTickV==null ) throw new NullPointerException("unable to find ticks");
        }

        TickVDescriptor ticks = bestTickV;

        if (bestUnit == null) {
            throw new IllegalArgumentException("failed to find best unit");
        }
        ticks.datumFormatter = TimeDatumFormatter.formatterForScale( bestUnit.getOrdinal(), new DatumRange(minD, maxD), dayOfYear );

        return ticks;

    }

    public static TickVDescriptor bestTickVTime(Datum minD, Datum maxD, int nTicksMin, int nTicksMax, boolean fin) {

        Datum minute = Datum.create(60.0, Units.seconds);
        if (maxD.subtract(minD).lt(minute)) {
            Datum base = TimeUtil.prevMidnight(minD);

            Units offUnits = Units.seconds;
            Datum offMin = minD.subtract(base).convertTo(offUnits);
            Datum offMax = maxD.subtract(base).convertTo(offUnits);
            TickVDescriptor offTicks = bestTickVLinear(offMin, offMax, nTicksMin, nTicksMax, fin);

            DatumVector minorTicks = offTicks.getMinorTicks().add(base);
            DatumVector majorTicks = offTicks.getMajorTicks().add(base);

            TickVDescriptor result = TickVDescriptor.newTickVDescriptor(majorTicks, minorTicks);
            result.datumFormatter = DatumUtil.bestFormatter(majorTicks);
            return result;
        }

        if (maxD.subtract(minD).gt(Datum.create(10 * 365, Units.days))) {
            int yearMin = TimeUtil.toTimeStruct(minD).year;
            int yearMax = TimeUtil.toTimeStruct(maxD).year;
            TickVDescriptor yearTicks = bestTickVLinear(Units.dimensionless.createDatum(yearMin),
                    Units.dimensionless.createDatum(yearMax), nTicksMin, nTicksMax, fin);
            yearTicks.units = minD.getUnits();
            double[] tickV = yearTicks.tickV.toDoubleArray(Units.dimensionless);
            for ( int i=0; i<tickV.length; i++ ) if ( tickV[i]<=1582 ) tickV[i]=1583;
            for (int i = 0; i < tickV.length; i++) {
                int iyear = (int) tickV[i];
                tickV[i] = TimeUtil.convert(iyear, 1, 1, 0, 0, 0, (TimeLocationUnits) yearTicks.units);
            }
            yearTicks.tickV = DatumVector.newDatumVector(tickV, yearTicks.units);
            double[] minorTickV = yearTicks.minorTickV.toDoubleArray(Units.dimensionless);
            for ( int i=0; i<minorTickV.length; i++ ) if ( minorTickV[i]<=1582 ) minorTickV[i]=1583;
            for (int i = 0; i < minorTickV.length; i++) {
                int iyear = (int) minorTickV[i];
                minorTickV[i] = TimeUtil.convert(iyear, 1, 1, 0, 0, 0, (TimeLocationUnits) yearTicks.units);
            }
            yearTicks.minorTickV = DatumVector.newDatumVector(minorTickV, yearTicks.units);
            Datum t1 = yearTicks.getMajorTicks().get(0);
            int nticks = yearTicks.getMajorTicks().getLength();
            Datum t2 = yearTicks.getMajorTicks().get(nticks - 1);
            yearTicks.datumFormatter = DatumUtil.bestTimeFormatter(t1, t2, nticks);
            return yearTicks;
        } else {
            return bestTickVTimeOrdinal(minD, maxD, nTicksMin, nTicksMax, fin);

        }
    }

}

