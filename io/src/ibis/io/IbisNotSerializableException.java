package ibis.io;

import java.io.NotSerializableException;

class IbisNotSerializableException extends NotSerializableException {

    private static final long serialVersionUID = 1L;

    public IbisNotSerializableException() {
    }

    public IbisNotSerializableException(String message) {
        super(message);
    }

    public IbisNotSerializableException(Throwable cause) {
        super();
        initCause(cause);
    }

    public IbisNotSerializableException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
}
