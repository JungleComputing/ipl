package ibis.ipl;

public class IbisError extends Error {
	Throwable cause = null;

	public IbisError() {
		super();
	}

	public IbisError(String name) {
		super(name);
	}

	public IbisError(String name, Throwable cause) {
		super(name);
		initCause(cause);
	}

	public IbisError(Throwable cause) {
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
