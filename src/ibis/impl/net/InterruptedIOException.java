package ibis.impl.net;

public class InterruptedIOException extends ibis.ipl.IbisIOException {
    public InterruptedIOException() {
        super();
    }

    public InterruptedIOException(String name) {
        super(name);
    }

    public InterruptedIOException(String name, Throwable cause) {
        super(name, cause);
    }

    public InterruptedIOException(Throwable cause) {
        super(cause);
    }
}