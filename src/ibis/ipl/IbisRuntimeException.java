package ibis.ipl;

public class IbisRuntimeException extends RuntimeException {
	Throwable cause = null;

	public IbisRuntimeException() {
		super();
	}

	public IbisRuntimeException(String name) {
		super(name);
	}

	public IbisRuntimeException(String name, Throwable cause) {
		super(name);
		initCause(cause);
	}

	public IbisRuntimeException(Throwable cause) {
		super();
		initCause(cause);
	}

	public Throwable initCause(Throwable t) {
		return cause = t;
	}

	public Throwable getCause() {
		return cause;
	}

	public String getMessage() {
		String res = super.getMessage();
		if(cause != null) {
			res += ": " + cause.getMessage();
		}

		return res;
	}

	public void printStackTrace() {
		if(cause != null) {
			cause.printStackTrace();
		}

		super.printStackTrace();
	}
}
