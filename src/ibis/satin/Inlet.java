package ibis.satin;

/**
 * Optional class that application Exceptions (inlets) can extend. The advantage
 * of this is that the generation of stack traces (an expensive operation) is
 * inhibited.
 */
public class Inlet extends Throwable {

	public Inlet() {
	}

	public Throwable fillInStackTrace() {
		return this;
	}
}
