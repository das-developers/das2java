package org.das2.datum;

import java.text.ParseException;
import java.util.logging.Logger;
import org.das2.datum.format.DatumFormatter;
import org.das2.datum.format.DatumFormatterFactory;
import org.das2.datum.format.DefaultDatumFormatter;

/**
 *
 * @author Ed Jackson
 */
public final class DomainDividerUtil {

    private static final Logger logger= LoggerManager.getLogger("das2.datum.ddiv");
    
    // Still need to decide what all should be in here.

    public static DomainDivider getDomainDivider(Datum min, Datum max, boolean log) {
        // Look at supplied min/max units and make a guess at the appropriate divider
        if (UnitsUtil.isTimeLocation(min.getUnits())) {
            return new OrdinalTimeDomainDivider();
        } else if ( log ) {
            return new LogDomainDivider();
        } else {
            return new LinearDomainDivider();
        }
    }

    public static DomainDivider getDomainDivider(Datum min, Datum max) {
        return getDomainDivider(min, max, false);
    }

    private static String exp(int power) {
        StringBuilder buffer = new StringBuilder(power + 4);
        for (int i = 0; i < power - 1; i++) {
            buffer.append('#');
        }
        buffer.append("0.#E0");
        return buffer.toString();
    }
    private static final String zeros100 = "0.00000000000000000000" + "0000000000000000000000000000000000000000" + "0000000000000000000000000000000000000000";

    private static String zeros(int count) {
        if (count < 0) return "0";
        else if (count <= 100) return zeros100.substring(0, count + 2);
        else {
            StringBuffer buff = new StringBuffer(count + 2).append("0.");
            for (int index = 0; index < count; index++) {
                buff.append('0');
            }
            return buff.toString();
        }
    }

    /**
     * encapsulate the kludges that allow us to identify a good formatter for the divider and
     * a given range.
     * @param div the divider
     * @param range the range
     * @return a good formatter
     */
    public static DatumFormatter getDatumFormatter(DomainDivider div, DatumRange range) {
        if (div instanceof OrdinalTimeDomainDivider) {
            OrdinalTimeDomainDivider otdiv = (OrdinalTimeDomainDivider) div;
            return otdiv.getFormatter(range);
        } else if (div instanceof LogLinDomainDivider) {
            LogLinDomainDivider lldiv = (LogLinDomainDivider) div;
            int nFraction = lldiv.sigFigs();
            String format = exp(nFraction);
            DatumFormatterFactory factory = range.getUnits().getDatumFormatterFactory();
            try {
                return factory.newFormatter(format);
            } catch (ParseException ex) {
                throw new RuntimeException(ex); // sorry, user, please report this.
            }
        } else if (div instanceof LogDomainDivider) {
            // Labels should just be 10^x
            String format = "0.#E0";
            DatumFormatterFactory factory = range.getUnits().getDatumFormatterFactory();
            try {
                return factory.newFormatter(format);
            } catch (ParseException ex) {
                throw new RuntimeException(ex); // sorry, user, please report this.
            }
        } else {
            LinearDomainDivider ldiv = (LinearDomainDivider) div;
            DatumVector boundaries = ldiv.boundaries(range.min(), range.max());

            // There is kludginess here because of shortcomings in implementaiton of boundaries
            if (boundaries.getLength() <= 1) {
                try{
                    logger.info( "fall back to here, no formatting" );
                    return new DefaultDatumFormatter("0");
                } catch (ParseException ex) {
                    //This will never happen if the string literal above is okay
                    throw new RuntimeException(ex);
                }
            }
            double stepSize = boundaries.get(1).subtract(boundaries.get(0)).doubleValue();
            int nsteps = (int)Math.round( boundaries.get(boundaries.getLength()-1).subtract(boundaries.get(0)).divide(stepSize).doubleValue() );
            //System.err.printf("min: %f, max: %f, stepsize: %.20f, nsteps: %f%n", boundaries.get(0).doubleValue(), boundaries.get(boundaries.getLength()-1).doubleValue(), stepSize, nsteps);
            //System.err.println( boundaries.get(nsteps-1).subtract(boundaries.get(0)).divide(nsteps) );
            //System.err.printf("%f %f %d%n", boundaries.get(0).doubleValue(), boundaries.get(nsteps - 1).doubleValue(), nsteps);
            DatumFormatter result= DatumUtil.bestFormatter(boundaries.get(0), boundaries.get(boundaries.getLength() - 1), nsteps );
            return result;

//            String format;
//            if (ldiv.getExponent() < 0) {
//                format = zeros(-1 * ldiv.getExponent());
//            } else {
//                format = "0";
//            }
//
//            DatumFormatterFactory factory = range.getUnits().getDatumFormatterFactory();
//            try {
//                return factory.newFormatter(format);
//            } catch (ParseException ex) {
//                throw new RuntimeException(ex); // sorry, user, please report this.
//            }
        }
    }

    public static void main(String[] args) throws ParseException {
        if (true) {
            DomainDivider ldd = new LinearDomainDivider();
            for (int i = 0; i < 12; i++) {
                ldd = ldd.coarserDivider(false);
            }
            DatumRange range = new DatumRange(1e8, 2e8, Units.dimensionless);
            DatumFormatter df = getDatumFormatter(ldd, range);
            DatumVector dv = ldd.boundaries(range.min(), range.max());
            for (int i = 0; i < dv.getLength(); i++) {
                System.err.println(df.format(dv.get(i))); // logger okay
            }
        }
        if (false) {
            DomainDivider ldd = new LinearDomainDivider();
            //for (int i = 0; i < 10; i++) {
            //    ldd = ldd.coursDivider(false);
            //}
            for (int i = 0; i < 15; i++) {
                ldd = ldd.coarserDivider(false);
            }
            DatumRange range = new DatumRange(-1000000, 1000000, Units.dimensionless);
            DatumFormatter df = getDatumFormatter(ldd, range);
            DatumVector dv = ldd.boundaries(range.min(), range.max());
            for (int i = 0; i < dv.getLength(); i++) {
                System.err.println(df.format(dv.get(i))); // logger okay
            }
        }
        if (false) {
            System.err.println(""+TimeUtil.isLeapYear(2000)); // initialize class to define time ordinals // logger okay
            DomainDivider ldd = new OrdinalTimeDomainDivider();
            for (int i = 0; i < 13; i++) {
                ldd = ldd.finerDivider(false);
            }
            ldd.toString();
            DatumRange range = DatumRangeUtil.parseTimeRange("2009-001T00:00 to 00:01");

            DatumFormatter df = getDatumFormatter(ldd, range);
            DatumVector dv = ldd.boundaries(range.min(), range.max());
            for (int i = 0; i < dv.getLength(); i++) {
                System.err.println(df.format(dv.get(i))); // logger okay
            }
        }
    }
}
