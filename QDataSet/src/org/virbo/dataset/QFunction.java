
package org.virbo.dataset;

/**
 * QFunctions try to recycle as much of the QDataSet interface as possible to
 * define functions.  Functions take N parameters as input and result in M parameter
 * output.  The N parameters are passed into value as a rank 1 bundle QDataSet.
 * The M parameter output is returned in a rank 1 bundle dataset.  The method
 * exampleInput returns an example input that allows for discovery of the function.
 *
 * Implementations will generally extend AbstractQFunction, which implements
 * values() and exampleOutput().
 *
 *
 * @author jbf
 */
public interface QFunction {

    /**
     * A rank 1 dataset of N parameters is passed in, and a
     * rank 1 dataset of M parameters is returned.  It's presumed that this
     * is calculated in interactive time (1/30sec).
     * Goals:
     *   * support extra tick labels of axis.
     *   * allow discovery of function, so I can pick it up and use it
     *   * allow tabulation and plotting of a function
     *   * non-linear function optimization
     * @param parm rank 1 bundle of N elements, or rank 2 array of such.
     * @return rank 1 bundle of M elements, or rank 2 array of such.
     */
    QDataSet value( QDataSet parm );


    /**
     * A rank 2 dataset of CxN parameters is passed in, and a
     * rank 1 dataset of CxM parameters is returned, where C is the number
     * of repeated value operations.  This is useful for when it's expensive
     * to look up the first value.
     * @param parm rank 2 of C bundles of N elements.
     * @return rank 2 of C bundles of M elements, or rank 2 array of such.
     */
    QDataSet values( QDataSet parm );

    /**
     * discover an example input.  Result is a rank 1 bundle QDataSet.
     *   QFunction ff= TestFunction();
     *   ff.exampleInput().length();  // how many parameters the function takes
     *   QDataSet bds= ff.exampleInput().property( QDataSet.BUNDLE_0 );
     *   ; to discover properties of the first (0th) parameter:
     *   bds.slice(0).property( QDataSet.UNITS )       // function should handle convertible units (e.g. TimeAxes Ephemeris).
     *   bds.slice(0).property( QDataSet.VALID_MIN )   // absolute limits of domain of the function
     *   bds.slice(0).property( QDataSet.VALID_MAX )
     *   bds.slice(0).property( QDataSet.TYPICAL_MIN ) // domain of the function parameter
     *   bds.slice(0).property( QDataSet.TYPICAL_MAX )
     *   bds.slice(0).property( QDataSet.CADENCE ) // granularity of the function parameter
     *   bds.slice(0).property( QDataSet.LABEL )   // label for the parameter
     * slice(0) is the first argument, slice(1) would be the second, etc.
     * This would be a bundle.
     * @return rank 1 bundle of N elements.
     */
    QDataSet exampleInput();

    /**
     * discover an example of output.  Result is a rank 1 bundle QDataSet.  This
     * was introduced to support QFunctions where it would be expensive to calculate
     * an input that would result in a meaningful output.  It's assumed that many
     * implementations will simply be:
     *   value( exampleInput() );
     * 
     * @return rank 1 bundle of M elements.
     */
    QDataSet exampleOutput();
}
