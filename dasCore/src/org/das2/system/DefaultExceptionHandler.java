/*
 * DefaultExceptionHandler.java
 *
 * Created on November 16, 2006, 12:32 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.das2.system;

import org.das2.util.DasExceptionHandler;

/**
 * Wrapper for DasExceptionHandler, a dialog box for endusers with options
 * to get at the debugging information.
 *
 * @author jbf
 */
public class DefaultExceptionHandler implements ExceptionHandler {
    
    /** Creates a new instance of DefaultExceptionHandler */
    public DefaultExceptionHandler() {
    }

    public void handle(Throwable t) {
        DasExceptionHandler.handle(t);
    }

    public void handleUncaught(Throwable t) {
        DasExceptionHandler.handleUncaught(t);
    }
    
}
