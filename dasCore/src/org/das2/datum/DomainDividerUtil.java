package org.das2.datum;

/**
 *
 * @author Ed Jackson
 */
public final class DomainDividerUtil {
    // Still need to decide what all should be in here.
    
    public static DomainDivider getDomainDivider(Datum min, Datum max, boolean log) {
        // Look at supplied min/max units and make a guess at the appropriate divider

        //placehoder:
        return new LinearDomainDivider();
    }

    public static DomainDivider getDomainDivider(Datum min, Datum max) {
        return getDomainDivider(min, max, false);
    }
}
