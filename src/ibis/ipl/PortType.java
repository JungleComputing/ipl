package ibis.ipl;

/** A PortType can be created using the Ibis.createPortType method. **/

public interface PortType {

	/** Returns the name given to this PortType upon creation. **/
	public String name();

	/** Returns the properties given to this PortType upon creation. **/
	public StaticProperties properties();

	/** Create a named SendPort of this PortType. The name does not have to be unique **/
	public SendPort createSendPort(String name) throws IbisIOException;

	/** Create a anonymous SendPort of this PortType. **/
	public SendPort createSendPort() throws IbisIOException;

	/** Create a anonymous SendPort of this PortType, with a replacer. **/
	public SendPort createSendPort(Replacer r) throws IbisIOException;

	/** Create a named ReceivePort of this PortType, with explicit receipt communication.
	    New connections will not be accepted until ReceivePort.enableConnections() is invoked.
	    This is done to avoid upcalls during initilization.
	**/
	public ReceivePort createReceivePort(String name) throws IbisIOException;

	/** Create a named ReceivePort of this PortType, with upcall based communication.
	    New connections will not be accepted until ReceivePort.enableConnections() is invoked.
	    This is done to avoid upcalls during initilization.
	**/
	public ReceivePort createReceivePort(String name, Upcall u) throws IbisIOException;

	/** Create a named ReceivePort of this PortType, with explicit receipt communication.
	    New connections will not be accepted until ReceivePort.enableConnections() is invoked.
	    This is done to avoid upcalls during initilization.
	    When a new connection request arrives, a ConnectUpcall is performed.
	**/
	public ReceivePort createReceivePort(String name, ConnectUpcall cU) throws IbisIOException;

	/** Create a named ReceivePort of this PortType, with upcall based communication.
	    New connections will not be accepted until ReceivePort.enableConnections() is invoked.
	    This is done to avoid upcalls during initilization.
	    When a new connection request arrives, a ConnectUpcall is performed.
	**/
	public ReceivePort createReceivePort(String name, Upcall u, ConnectUpcall cU) throws IbisIOException;

	/** Compare two PortTypes. **/
	public boolean equals(PortType other);
}
