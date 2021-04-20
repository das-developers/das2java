
package org.das2.qds;

/**
 * QFunctions try to recycle as much of the QDataSet interface as possible to
 * define functions.  Functions take N parameters as input and result in M parameter
 * output.  The N parameters are passed into value as a rank 1 bundle QDataSet, or rank 0 
 * dataset when there is just one input.
 * The M parameter output is returned in a rank 1 bundle dataset.  The method
 * exampleInput returns an example input that allows for discovery of the function.
 *
 * Implementations will generally extend AbstractQFunction, which implements
 * values() and exampleOutput().
 * 
 * Goals:
 * <ul>
 * <li> support extra tick labels of axis, which are often the result of SPICE kernel evaluations.
 * <li> allow discovery of function, so the system can pick it up and use it.
 * <li> allow tabulation and plotting of a function.
 * <li> non-linear function optimization.
 * </ul>
 * 
 * @see <a href="http://jfaden.net/hudson/job/autoplot-test037/lastSuccessfulBuild/artifact/tcaQFunctionTest.jy"tcaQFunctionTest.jy</a>
 * @author jbf
 */
public interface QFunction {

    /**
     * Evaluate the function at the location.
     * A rank 1 dataset of N parameters is passed in, and a
     * rank 1 dataset of M parameters is returned.  It's presumed that this
     * is calculated in interactive time (1/30sec) for GUI applications like
     * attaching ephemeris ticks to an axis (note no monitor parameter to indicate feedback).
     * 
     * @param parm rank 1 bundle of N elements, or rank 2 array of such.
     * @return rank 1 bundle of M elements, or rank 2 array of such.
     */
    QDataSet value( QDataSet parm );

    /**
     * Evaluate the function at the locations in parm.
     * A rank 2 dataset of CxN parameters is passed in, and a
     * rank 1 dataset of CxM parameters is returned, where C is the number
     * of repeated value operations.  This is useful for when it's expensive
     * to look up the first value.
     * @param parm rank 2 of C bundles of N elements.  rank 1 parm is acceptable if the exampleInput result is rank 0.
     * @return rank 2 of C bundles of M elements, or rank 2 array of such.
     */
    QDataSet values( QDataSet parm );

    /**
     * Discover an example input.  Result is a rank 1 bundle QDataSet.
     *<blockquote><pre>
     *QFunction ff= TestFunction();
     *ff.exampleInput().length();  // how many parameters the function takes
     *QDataSet bds= ff.exampleInput().property( QDataSet.BUNDLE_0 );
     *bds.slice(0).property( QDataSet.UNITS )       // function should handle convertible units (e.g. TimeAxes Ephemeris).
     *bds.slice(0).property( QDataSet.VALID_MIN )   // absolute limits of domain of the function
     *bds.slice(0).property( QDataSet.VALID_MAX )
     *bds.slice(0).property( QDataSet.TYPICAL_MIN ) // domain of the function parameter
     *bds.slice(0).property( QDataSet.TYPICAL_MAX )
     *bds.slice(0).property( QDataSet.CADENCE ) // granularity of the function parameter
     *bds.slice(0).property( QDataSet.LABEL )   // label for the parameter
     *</pre></blockquote>
     * slice(0) is the first argument, slice(1) would be the second, etc.
     * This would be a bundle.
     * 
     * Note, for functions that have only one argument, like F(T)&rarr;[R,MLT,MLAT], this
     * may return a rank 0 dataset.  Clients should pass a dataset to the value method a
     * dataset with the same geometry.
     * @return rank 1 bundle of N elements, or rank 0 for functions when the function has just one parameter.
     */
    QDataSet exampleInput();

    /**
     * Discover an example of output.  Result is a rank 1 bundle QDataSet.  This
     * was introduced to support QFunctions where it would be expensive to calculate
     * an input that would result in a meaningful output.  It's assumed that many
     * implementations will simply be:
     *<blockquote><pre>
     *value( exampleInput() );
     *</pre></blockquote>
     * @return rank 1 bundle of M elements.
     */
    QDataSet exampleOutput();
}
