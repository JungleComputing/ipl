package ibis.ipl;

public class PortMismatchException extends java.io.IOException {

    /**
     * Thrown when ports of different types are connected.
     */

    public PortMismatchException() {
	super();
    }

    public PortMismatchException(String name) {
	super(name);
    }

    public PortMismatchException(String name, Throwable cause) {
	super(name + cause);
//	initCause(cause);
    }

    public PortMismatchException(Throwable cause) {
	super("" + cause);
//	initCause(cause);
    }

}
