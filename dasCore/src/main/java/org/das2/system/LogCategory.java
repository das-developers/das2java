/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.system;

/** This class takes the strings from DasLogger, and uses them as names for the 
 * standard Java Logging facility
 *
 * Original Constants by jbf, copied here by cwp
 */
public class LogCategory{
	/** messages having to do with the application-specific Das 2 Application */
	public static final String APPLICATION_LOG = "";
    
	/** system messages such as RequestProcessor activity*/
	public static final String SYSTEM_LOG = "das2.system";
    
	/** events, gestures, user feedback */
	public static final String GUI_LOG = "das2.gui";
    
	/** renders, drawing */
	public static final String GRAPHICS_LOG = "das2.graphics";
    
	/** renderer's logger */
	public static final String RENDERER_LOG = "das2.graphics";
    
	/** rebinning  and dataset operators */
	public static final String DATA_OPERATIONS_LOG = "das2.dataOperations";
    
	/** internet transactions, file I/O */
	public static final String DATA_TRANSFER_LOG = "das2.dataTransfer";
    
	/** virtual file system activities */
	public static final String FILESYSTEM_LOG = "das2.filesystem";
    
	/** das2 application description files */
	public static final String DASML_LOG = "das2.dasml";
}
