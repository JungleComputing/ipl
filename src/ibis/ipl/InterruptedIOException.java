package ibis.ipl;

public class InterruptedIOException extends IbisIOException {
    public InterruptedIOException() {
	super();
    }

    public InterruptedIOException(String name) {
	super(name);
    }

    public InterruptedIOException(String name, Throwable cause) {
	super(name, cause);
	initCause(cause);
    }

    public InterruptedIOException(Throwable cause) {
	super("", cause);
	initCause(cause);
    }
}
