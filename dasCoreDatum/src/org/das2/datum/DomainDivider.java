package org.das2.datum;

/**
 * Divides a given range of input values into useful intervals.  This can be
 * used, for example, to determine the locations of tick marks on an axis.
 * Implementations will vary their behavior based on a number of factors including
 * the type of data in question.
 * <p>
 * Note that the intervals need not be uniformly sized.  For example, this would
 * be the case when dividing a time interval into months, or into log spaced
 * intervals.
 * <p>
 * Implementations should have protected constructors so factory methods in
 * <code>DomainDividerUtil</code> can access them.
 *
 * @author Ed Jackson
 */
public interface DomainDivider {

    /**
     * Return a new divider with larger intervals.  If the <code>superset</code>
     * parameter is true, the interval boundaries of the new divider are
     * guaranteed to align with (some) interval boundaries of the existing one.
     * In other words, the existing divider will subdivide the new one.
     * @param superset true to force boundary alignment with the calling <code>DomainDivider</code>.
     *   For a given range, the boundaries will be aligned.
     * @return the new DomainDivider object
     */
    public DomainDivider coarserDivider(boolean superset);

    /**
     * Return a new divider with smaller intervals. If the <code>superset</code>
     * parameter is true, the interval boundaries of the existing divider are
     * guaranteed to align with (some) intervals of the new one.  In other
     * words, the new divider will subdivide the existing one.
     * @param superset true to force boundary alignment with the calling <code>DomainDivider</code>
     *     For a given range, the boundaries will be aligned.
     * @return the new DomainDivider object
     */
    public DomainDivider finerDivider(boolean superset);

    public final int MAX_BOUNDARIES=1000000;
    
    /**
     * Returns the boundaries between intervals on the given data range. When
     * min or max lay on a boundary, it should be returned.  Note this is
     * inconsistent with DatumRange logic, where the max is exclusive.
     * If min>=max, then an empty list should be returned.
     * @param min
     * @param max
     * @return a <code>DatumVector</code> containing the boundary values
     */
    public DatumVector boundaries(Datum min, Datum max);

    /**
     * Compute the number of intervals produced by this <code>DomainDivider</code>
     * on the given range.  This allows the DomainDivider to indicate the number
     * of intervals (e.g. a zillion) without having to enumerate them.  When
     * min or max lay on a boundary, it should be counted.  If min>=max, then 
     * zero should be returned.
     * @param min
     * @param max
     * @return
     */
    public long boundaryCount(Datum min, Datum max);

    /**
     * Return a <code>DatumRange</code> representing the interval containing
     * the given value.
     * @param v
     * @return
     */
    public DatumRange rangeContaining(Datum v);

}
