package ibis.io.jme;

import java.io.IOException;

public class IbisIOException extends IOException {

    private static final long serialVersionUID = 1L;

    public IbisIOException() {
    }

    public IbisIOException(String message) {
        super(message);
    }
}
