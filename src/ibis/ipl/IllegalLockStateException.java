package ibis.ipl;

public class IllegalLockStateException extends java.lang.RuntimeException {

    /**
     * Thrown to indicate that a thread has attempted to lock, unlcok, wait,
     * or notify an nternal Ibis lock that it does not own, or that has
     * been cleaned up.
     */

    public IllegalLockStateException() {
	super();
    }

    public IllegalLockStateException(String name) {
	super(name);
    }

    public IllegalLockStateException(String name, Throwable cause) {
	super(name);
	initCause(cause);
    }

    public IllegalLockStateException(Throwable cause) {
	super();
	initCause(cause);
    }

}
