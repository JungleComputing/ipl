package ibis.ipl.support;

import java.io.IOException;

public class RemoteException extends IOException {

    private static final long serialVersionUID = 1L;

    RemoteException(String message) {
        super(message);
    }
    
}
