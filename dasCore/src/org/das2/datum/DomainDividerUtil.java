package org.das2.datum;

import org.das2.datum.format.DatumFormatter;
import org.das2.datum.format.DefaultDatumFormatter;
import org.das2.datum.format.TimeDatumFormatter;

/**
 *
 * @author Ed Jackson
 */
public final class DomainDividerUtil {
    // Still need to decide what all should be in here.
    
    public static DomainDivider getDomainDivider(Datum min, Datum max, boolean log) {
        // Look at supplied min/max units and make a guess at the appropriate divider
        if ( UnitsUtil.isTimeLocation( min.getUnits() ) ) {
            return new OrdinalTimeDomainDivider();
        } else {
            return new LinearDomainDivider();
        }
    }

    public static DomainDivider getDomainDivider(Datum min, Datum max) {
        return getDomainDivider(min, max, false);
    }

    /**
     * encapsulate the kludges that allow us to identify a good formatter for the divider and
     * a given range.
     * @param div
     * @param min
     * @param max
     * @return
     */
    public static DatumFormatter getDatumFormatter( DomainDivider div, DatumRange range ) {
        if ( div instanceof OrdinalTimeDomainDivider ) {
            return DatumUtil.bestTimeFormatter( range.min(), range.max(),
                    (int)div.boundaryCount( range.min(), range.max() ) - 1 );
        } else if ( div instanceof LogDomainDivider ) {
            return DatumUtil.limitLogResolutionFormatter( range.min(), range.max(),
                    (int)div.boundaryCount( range.min(), range.max() ) - 1 );
        } else {
            return DatumUtil.bestFormatter( range.min(), range.max(),
                    (int)div.boundaryCount( range.min(), range.max() ) - 1 );
        }
    }
}
