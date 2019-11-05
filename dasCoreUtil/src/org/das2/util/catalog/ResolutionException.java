package org.das2.util.catalog;

/** Exception thrown if a path can not be resolved.
 * 
 * These are handled internally by the catalog package if possible, but some paths just
 * aren't resolvable no matter how many catalog branches are inspected.
 * 
 * @author C. Piker, 2019-11-02
 */
public class ResolutionException extends Exception {
	String path;
	
	// The following constructors are provided by the base class and not repeated here:
	//
	// Exception()
	// Exception(String msg)
	// Exception(Throwable ex)
	// Exception(String msg, Throwable ex)
	/** Construct a das2 catalog resolution exception
	 * @param msg  A general error message
	 * @param sPath The catalog path or sub-path that could not be resolved
	 */
	public ResolutionException(String msg, String sPath){
		super(msg);
		path = sPath;
	}
	
	/** Construct a das2 catalog resolution exception, and attache a cause.
	 * @param msg  A general error message
	 * @param ex   A throwable object that cause the resolution failure
	 * @param sPath The catalog path or sub-path that could not be resolved
	 */
	public ResolutionException(String msg, Throwable ex, String sPath){
		super(msg, ex);
		path = sPath;
	}
}
