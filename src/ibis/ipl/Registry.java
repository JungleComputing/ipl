package ibis.ipl;

import java.io.IOException;

public interface Registry {	

	/**
	 * Locate the ReceivePortIdentifier that has been bound with name name.
	 * The registry is polled regularly until a ReceivePortIdentifier with
	 * name name is returned.
	 * @exception If a ReceivePortIdentifier is returned whose class is
	 * 	locally unknown, a java.lang.ClassNotFoundException is thrown
	 * @exception The implementation may throw a java.io.IOException
	 */
	public ReceivePortIdentifier lookup(String name)
		throws IOException;

	/**
	 * Locate the ReceivePortIdentifier that has been bound with name name.
	 * The registry is polled regularly until a ReceivePortIdentifier with
	 * name name is returned or until timeout milliseconds have passed.
	 * If timeout is 0, lookup does not time out.
	 * @exception If the ReceivePortIdentifier has not been found within
	 * 	timeout milliseconds, an ibis.ipl.ConnectionTimedOutException
	 * 	is thrown.
	 * @exception If an ReceivePortIdentifier is returned whose class is
	 * 	locally unknown, a java.lang.ClassNotFoundException is thrown
	 * @exception The implementation may throw a java.io.IOException
	 */
	public ReceivePortIdentifier lookup(String name, long timeout)
		throws IOException;

	/**
	 * The registry is polled regularly until an Ibis with name name
	 * is returned.
	 * @exception If an IbisIdentifier is returned whose class is
	 * 	locally unknown, a java.lang.ClassNotFoundException is thrown
	 * @exception The implementation may throw a java.io.IOException
	 */
	public IbisIdentifier locate(String name)
		throws IOException, ClassNotFoundException;

	/**
	 * Locate the IbisIdentifier that has been registered with name name.
	 * The registry is polled regularly until an IbisIdentifier with name name
	 * is returned or until timeout milliseconds have passed.
	 * If timeout is 0, locate does not time out.
	 * @exception If the IbisIdentifier has not been found within
	 * 	timeout milliseconds, an Ibis.ipl.ConnectionTimedOutException
	 * 	is thrown.
	 * @exception If an IbisIdentifier is returned whose class is
	 * 	locally unknown, a java.lang.ClassNotFoundException is thrown
	 * @exception The implementation may throw a java.io.IOException
	 */
	public IbisIdentifier locate(String name, long timeout)
		throws IOException, ClassNotFoundException;

	/**
	 * @exception If any ReceivePortIdentifier is returned whose class is
	 * 	locally unknown, a java.lang.ClassNotFoundException is thrown
	 * @exception The implementation may throw a java.io.IOException
	 */
	public ReceivePortIdentifier[] query(IbisIdentifier ident)
		throws IOException, ClassNotFoundException;

	/**
	 * @exception If an Object is returned whose class is locally
	 * 	unknown, a java.lang.ClassNotFoundException is thrown
	 * @exception The implementation may throw a java.io.IOException
	 */
	public Object elect(String election, Object candidate)
		throws IOException, ClassNotFoundException;
	
	/**
	 * @exception The implementation may throw a java.io.IOException
	 */
	public void bind(String name, ReceivePortIdentifier port)
		throws IOException;
	
	/**
	 * @exception The implementation may throw a java.io.IOException
	 */
	public void rebind(String name, ReceivePortIdentifier port)
		throws IOException;
	
	/**
	 * @exception The implementation may throw a java.io.IOException
	 */
	public void unbind(String name)
		throws IOException;
	
	/**
	 * @exception The implementation may throw a java.io.IOException
	 */
	public String[] list(String pattern)
		throws IOException;

}
