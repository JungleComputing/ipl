package ibis.io.jme;

import java.io.IOException;

public class NotActiveException extends IOException {

	    private static final long serialVersionUID = 1L;
	    private Throwable cause;
	    
	    public Throwable initCause(Throwable cause) {
	    	if (cause == this) {
	    		throw new IllegalArgumentException("Can not cause self.");
	    	}
	    	if (null != cause) {
	    		throw new IllegalStateException("Cause already set.");
	    	}
	    	this.cause = cause;
	    	return cause;
	    }
	    
	    public Throwable getCause() {
	    	return cause;
	    }
	    
	    public NotActiveException() {
	    }

	    public NotActiveException(String message) {
	        super(message);
	    }

	    public NotActiveException(Throwable cause) {
	        super();
	        initCause(cause);
	    }

	    public NotActiveException(String message, Throwable cause) {
	        super(message);
	        initCause(cause);
	    }
}
