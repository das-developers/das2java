package org.das2.util.catalog;

/** Exceptions associated with getting and parsing das2 federated catalog nodes.  
 * 
 * These should not be created if data sources listed in the catalog fail to function.
 * That is a different sort of error that is already handled by existing classes.
 * 
 * @author C. Piker, 2019-11-02
 */
public class CatalogException extends Exception {
	String sUrl;
	
	// The following constructors are provided by the base class and not repeated here:
	//
	// Exception()
	// Exception(String msg)
	// Exception(Throwable ex)
	// Exception(String msg, Throwable ex)
	//
	
	public CatalogException(String msg, String url){
		super(msg);
		sUrl = url;
	}
	
	public CatalogException(String msg, Throwable ex, String url){
		super(msg, ex);
		sUrl = url;
	}
}
