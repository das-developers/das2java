/*
 * ConsoleExceptionHandler.java
 *
 * Created on November 16, 2006, 12:34 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.das2.util;

/**
 * ExceptionHandler that prints stack traces out to the stderr.
 * @author jbf
 */
public class ConsoleExceptionHandler implements ExceptionHandler {
    
    /** Creates a new instance of ConsoleExceptionHandler */
    public ConsoleExceptionHandler() {
    }
    
    public void handle(Throwable t) {
        t.printStackTrace();
    }
    
    public void handleUncaught(Throwable t) {
        t.printStackTrace();
    }
    
}
