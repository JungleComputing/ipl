package ibis.ipl;

import java.io.IOException;

/**
 * A PortType can be created using the Ibis.createPortType method. 
 **/

public interface PortType {

	/**
	 * Returns the name given to this PortType upon creation. 
	 **/
	public String name();

	/**
	 * Returns the properties given to this PortType upon creation. 
	 **/
	public StaticProperties properties();

	/**
	 * Create a named SendPort of this PortType. The name does not have to be unique 
	 * @exception java.io.IOException is thrown from the Port implementation
	 **/
	public SendPort createSendPort(String name) throws IOException;

	/** Create a named SendPort of this PortType. The name does not have to be unique
	    When a connection is lost, a ConnectUpcall is performed.
	 **/
	public SendPort createSendPort(String name, SendPortConnectUpcall cU) throws IOException;

	/**
	 * Create a anonymous SendPort of this PortType. 
	 * @exception java.io.IOException is thrown from the Port implementation
	 **/
	public SendPort createSendPort() throws IOException;

	/**
	 * Create a anonymous SendPort of this PortType, with a replacer. 
	 * @exception java.io.IOException is thrown from the Port implementation
	 **/
	public SendPort createSendPort(ibis.io.Replacer r) throws IOException;

	/**
	 * Create a SendPort of this PortType, with a replacer. 
	 * @exception java.io.IOException is thrown from the Port implementation
	 **/
	public SendPort createSendPort(String name, ibis.io.Replacer r) throws IOException;

	/** Create a anonymous SendPort of this PortType, with a replacer.
	    When a connection is lost, a ConnectUpcall is performed.
	 * @exception java.io.IOException is thrown from the Port implementation
	 **/
	public SendPort createSendPort(ibis.io.Replacer r, SendPortConnectUpcall cU) throws IOException;

	/** Create a SendPort of this PortType, with a replacer.
	    When a connection is lost, a ConnectUpcall is performed.
	 * @exception java.io.IOException is thrown from the Port implementation
	 **/
	public SendPort createSendPort(String name, ibis.io.Replacer r, SendPortConnectUpcall cU) throws IOException;

	/** Create a named ReceivePort of this PortType, with explicit receipt communication.
	    New connections will not be accepted until ReceivePort.enableConnections() is invoked.
	    This is done to avoid upcalls during initilization.
	 * @exception java.io.IOException is thrown from the Port implementation
	 **/
	public ReceivePort createReceivePort(String name) throws IOException;

	/** Create a named ReceivePort of this PortType, with upcall based communication.
	    New connections will not be accepted until ReceivePort.enableConnections() is invoked.
	    This is done to avoid upcalls during initilization.
	 * @exception java.io.IOException is thrown from the Port implementation
	 **/
	public ReceivePort createReceivePort(String name, Upcall u) throws IOException;

	/** Create a named ReceivePort of this PortType, with explicit receipt communication.
	    New connections will not be accepted until ReceivePort.enableConnections() is invoked.
	    This is done to avoid upcalls during initilization.
	    When a new connection request arrives, or when a connection is lost, a ConnectUpcall is performed.
	 * @exception java.io.IOException is thrown from the Port implementation
	 **/
	public ReceivePort createReceivePort(String name, ReceivePortConnectUpcall cU) throws IOException;

	/** Create a named ReceivePort of this PortType, with upcall based communication.
	    New connections will not be accepted until ReceivePort.enableConnections() is invoked.
	    This is done to avoid upcalls during initilization.
	    When a new connection request arrives, or when a connection is lost, a ConnectUpcall is performed.
	 * @exception java.io.IOException is thrown from the Port implementation
	 **/
	public ReceivePort createReceivePort(String name, Upcall u, ReceivePortConnectUpcall cU) throws IOException;

	/** The hashCode method is mentioned here just as a reminder that an implementation
	    must probably redefine it, because two objects representing the same porttype
	    must result in the same hashcode (and compare equal). To explicitly specify
	    it in the interface does not help, because java.lang.Object already implements it,
	    but, anyway, here it is:
	**/
	public int hashCode();

	/** The equals method is mentioned here just as a reminder that an implementation
	    must probably redefine it, because two objects representing the same porttype
	    must compare equal (and result in the same hashcode). To explicitly specify
	    it in the interface does not help, because java.lang.Object already implements it,
	    but, anyway, here it is:
	**/
	public boolean equals(Object other);
}
