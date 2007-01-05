/*
 * DefaultExceptionHandler.java
 *
 * Created on November 16, 2006, 12:32 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.uiowa.physics.pw.das.system;

import edu.uiowa.physics.pw.das.util.DasExceptionHandler;

/**
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
