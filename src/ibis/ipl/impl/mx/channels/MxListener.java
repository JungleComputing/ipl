package ibis.ipl.impl.mx.channels;

public interface MxListener {
	
	/**
	 * When a new connect request arrives at the MxSocket, the endpoint will call this method of its Listener. 
	 * The Listener must give an appropriate reply to the request. The Listener should not block in this 
	 * function or hijack the thread.
	 * 
	 * @param The connect request
	 */
	void newConnection(ConnectionRequest request);
	
}
