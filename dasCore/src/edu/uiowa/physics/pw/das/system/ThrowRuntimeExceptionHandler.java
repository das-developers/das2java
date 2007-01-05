/*
 * NullExceptionHandler.java
 *
 * Created on November 16, 2006, 12:59 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.uiowa.physics.pw.das.system;

/**
 *
 * @author jbf
 */
public class ThrowRuntimeExceptionHandler implements ExceptionHandler {
    
    /** Creates a new instance of NullExceptionHandler */
    public ThrowRuntimeExceptionHandler() {
    }

    public void handle(Throwable t) {
        throw new RuntimeException(t);
    }

    public void handleUncaught(Throwable t) {
        throw new RuntimeException(t);
    }
    
}
