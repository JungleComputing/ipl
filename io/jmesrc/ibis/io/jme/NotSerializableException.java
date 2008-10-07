package ibis.io.jme;

import java.io.IOException;

public class NotSerializableException extends IOException {

    private static final long serialVersionUID = 1L;

    public NotSerializableException() {
    }

    public NotSerializableException(String message) {
        super(message);
    }
}
