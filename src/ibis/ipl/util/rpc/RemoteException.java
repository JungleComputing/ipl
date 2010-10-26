package ibis.ipl.util.rpc;

import java.io.IOException;

public class RemoteException extends IOException {

    private static final long serialVersionUID = 1L;

    RemoteException(Throwable cause) {
    	super(cause);
    }
    
    RemoteException(String message, Throwable cause) {
    	super(message, cause);
    }
    
    RemoteException(String message) {
        super(message);
    }

}
