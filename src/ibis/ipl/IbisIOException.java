package ibis.ipl;

import java.io.IOException;

public class IbisIOException extends IOException {
	Throwable cause = null;

	public IbisIOException() {
		super();
	}

	public IbisIOException(String name) {
		super(name);
	}

	public IbisIOException(String name, Throwable cause) {
		super(name);
		initCause(cause);
	}

	public IbisIOException(Throwable cause) {
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
