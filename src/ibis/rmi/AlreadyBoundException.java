package ibis.rmi;

public class AlreadyBoundException extends Exception
{
    public AlreadyBoundException() {
	super();
    }

    public AlreadyBoundException(String s) {
	super(s);
    }

    public AlreadyBoundException(String s, Throwable e) {
	super(s, e);
    }
}
