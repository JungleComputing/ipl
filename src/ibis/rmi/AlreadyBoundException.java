package ibis.rmi;

public class AlreadyBoundException extends ibis.ipl.IbisException
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
