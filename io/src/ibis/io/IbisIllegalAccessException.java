package ibis.io;

class IbisIllegalAccessException extends IllegalAccessException {

    private static final long serialVersionUID = 1L;

    public IbisIllegalAccessException() {
    }

    public IbisIllegalAccessException(String message) {
        super(message);
    }

    public IbisIllegalAccessException(Throwable cause) {
        super();
        initCause(cause);
    }

    public IbisIllegalAccessException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
}
