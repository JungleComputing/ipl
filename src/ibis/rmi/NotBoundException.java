package ibis.rmi;

public class NotBoundException extends Exception
{
    public NotBoundException() {
	super();
    }

    public NotBoundException(String s) {
	super(s);
    }
}
