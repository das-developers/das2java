
package org.das2.util.monitor;

/**
 * Experiment with unchecked vs checked exceptions.  This allows
 * calling codes to check for this specific exception.  Note that this
 * is a subclass of RuntimeException, not CancelledOperationException.
 * @author faden@cottagesystems.com
 */
public class UncheckedCancelledOperationException extends RuntimeException {
    public UncheckedCancelledOperationException() {
        super();
    }

    public UncheckedCancelledOperationException(String message) {
    }

    public UncheckedCancelledOperationException(Throwable cause) {
        super(cause);
    }

    public UncheckedCancelledOperationException(String message, Throwable cause) {
        super(message,cause);
    }
}
