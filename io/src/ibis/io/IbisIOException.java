package ibis.io;

import java.io.IOException;

public class IbisIOException extends IOException {

    private static final long serialVersionUID = 1L;

    public IbisIOException() {
    }

    public IbisIOException(String message) {
        super(message);
    }

    public IbisIOException(Throwable cause) {
        super();
        initCause(cause);
    }

    public IbisIOException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
}
