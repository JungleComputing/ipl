package ibis.rmi.server;

public class ServerNotActiveException extends Exception
{
    public ServerNotActiveException() {
    }

    public ServerNotActiveException(String s) {
	super(s);
    }
}
