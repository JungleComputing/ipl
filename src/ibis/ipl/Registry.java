package ibis.ipl;

public interface Registry {	

	/**
	 Locate the ReceivePortIdentifier that has been bound with name name.
	 The registry is polled regularly until a ReceivePortIdentifier with
	 name name is returned.
	 */
	public ReceivePortIdentifier lookup(String name) throws IbisIOException;

	/**
	 Locate the ReceivePortIdentifier that has been bound with name name.
	 The registry is polled regularly until a ReceivePortIdentifier with
	 name name is returned or until timeout milliseconds have passed.
	 If timeout is 0, lookup does not time out.
	 If the ReceivePortIdentifier has not been found within timeout
	 milliseconds, an IbisIOException with corresponding message is thrown.
	 */
	public ReceivePortIdentifier lookup(String name, long timeout) throws IbisIOException;

	/**
	 The registry is polled regularly until an Ibis with name name
	 is returned.
	 */
	public IbisIdentifier locate(String name) throws IbisIOException;

	/**
	 Locate the IbisIdentifier that has been registered with name name.
	 The registry is polled regularly until an IbisIdentifier with name name
	 is returned or until timeout milliseconds have passed.
	 If timeout is 0, locate does not time out.
	 If the IbisIdentifier has not been found within timeout milliseconds,
	 an IbisIOException with corresponding message is thrown.
	 */
	public IbisIdentifier locate(String name, long timeout) throws IbisIOException;

	public ReceivePortIdentifier[] query(IbisIdentifier ident)  throws IbisIOException;

	public Object elect(String election, Object candidate) throws IbisIOException;
	
	public void bind(String name, ReceivePortIdentifier port) throws IbisIOException;
	
	public void rebind(String name, ReceivePortIdentifier port) throws IbisIOException;
	
	public void unbind(String name) throws IbisIOException;
	
	public String[] list(String pattern) throws IbisIOException;
}
