/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.util.monitor;

/**
 * FileSystem cancel exception.  Note this is used where we once used
 * org.das2.CancelledOperationException, and we separated this to decouple
 * org.das2.util.
 *
 * @author jbf
 */
public class CancelledOperationException extends Exception {
    /** Creates a new instance of CancelledOperationException */
    public CancelledOperationException() {
        super();
    }

    public CancelledOperationException(String message) {
    }

    public CancelledOperationException(Throwable cause) {
        super(cause);
    }

    public CancelledOperationException(String message, Throwable cause) {
        super(message,cause);
    }
}
