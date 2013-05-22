/*
 * NullExceptionHandler.java
 *
 * Created on November 16, 2006, 12:59 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.das2.util;

/**
 * ExceptionHandler that throws a RuntimeException caused by the Exception.
 * This is useful for server-side applications that need to handle the 
 * exception externally.
 *
 * @author jbf
 */
public class ThrowRuntimeExceptionHandler implements ExceptionHandler {
    
    /** Creates a new instance of NullExceptionHandler */
    public ThrowRuntimeExceptionHandler() {
    }

    public void handle(Throwable t) {
        t.printStackTrace();
        throw new RuntimeException(t);
    }

    public void handleUncaught(Throwable t) {
        t.printStackTrace();
        throw new RuntimeException(t);
    }
    
}
