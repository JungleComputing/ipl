/* $Id$ */

package ibis.io.jme;

public class SerializationError extends Error {
    /** 
     * Generated
     */
    private static final long serialVersionUID = 8658882919059316317L;
    Throwable cause = null;

    public SerializationError() {
        super();
    }

    public SerializationError(String message) {
        super(message);
    }

    public SerializationError(String message, Throwable cause) {
        super(message);
        this.cause = cause;
    }

    public SerializationError(Throwable cause) {
        super();
        this.cause = cause;
    }

    public Throwable initCause(Throwable t) {
        return cause = t;
    }

    public Throwable getCause() {
        return cause;
    }

    public String getMessage() {
        String res = super.getMessage();
        if (cause != null) {
            res += ": " + cause.getMessage();
        }

        return res;
    }

    public void printStackTrace() {
        if (cause != null) {
            cause.printStackTrace();
        }

        super.printStackTrace();
    }
}
