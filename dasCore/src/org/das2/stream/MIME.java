package org.das2.stream;

/** A listing of MIME types parseable by Das2 clients
 *
 * @author cwp
 */
public final class MIME{
	
	/** Das2 Text Stream mime-type */
	public static final String MIME_DAS2_TEXT = "text/vnd.das2.das2stream";
	
	/** Das2 Text Stream full content-type */
	public static final String CONTENT_DAS2_TEXT = "text/vnd.das2.das2stream; charset=utf-8";
	
	/** Autoplot QStream text version mime-type */
	public static final String MIME_QDS_TEXT = "text/vnd.das2.qstream";
	
	/** Autoplot QStream text version full content-type string */
	public static final String CONTENT_QDS_TEXT = "text/vnd.das2.qstream; charset=utf-8";
	
	/** Das2 Binary Stream data mime-type */
	public static final String MIME_DAS2_BIN = "application/vnd.das2.das2stream";
	
	/** Das2 Binary Stream full content-type (same as mime-type) */
	public static final String CONTENT_DAS2_BIN = "application/vnd.das2.das2stream";
	
	/** Autoplot QStream binary data mime-type */
	public static final String MIME_QDS_BIN = "application/vnd.das2.qstream";
	
	/** Autoplot QStream binary data mime-type (same as mime-type) */
	public static final String CONTENT_QDS_BIN = "application/vnd.das2.qstream";
	
	/** Autoplot VAP file mime-type recommended form */
	public static final String MIME_VAP = "applicaiton/vnd.autoplot.vap+xml";
	
	/** Autoplot VAP file mime-type deprecated form */
	public static final String MIME_VAP_ALT = "application/x-autoplot-vap+xml";
	
	// Is this a dataset mimetype
	public static boolean isDataStream(String sMime){
		return sMime.contains(MIME_DAS2_TEXT) || sMime.contains(MIME_DAS2_BIN) ||
			    sMime.contains(MIME_QDS_TEXT) || sMime.contains(MIME_QDS_BIN);
	}
	
	
	// Don't make one of these
	private MIME(){
	}
}
