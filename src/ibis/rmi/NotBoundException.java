package ibis.rmi;

public class NotBoundException extends Exception
{
    public NotBoundException() {
	super();
    }

    public NotBoundException(String s) {
	super(s);
    }

    public NotBoundException(String s, Throwable e) {
	super(s, e);
    }
}
