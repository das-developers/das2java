/*
 * ExceptionHandler.java
 *
 * Created on November 16, 2006, 12:31 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.das2.util;

/**
 *
 * @author jbf
 */
public interface ExceptionHandler {
    void handle(Throwable t);
    void handleUncaught(Throwable t);
}
