package ibis.ipl;

public class IbisConfigurationException extends RuntimeException {

    public IbisConfigurationException() {
	super();
    }

    public IbisConfigurationException(String message) {
	super(message);
    }

    public IbisConfigurationException(String message, Throwable cause) {
	super(message, cause);
    }

    public IbisConfigurationException(Throwable cause) {
	super(cause);
    }

}
