package ibis.ipl.impl.tcp;

import ibis.ipl.ReadMessage;
import ibis.ipl.IbisIOException;

abstract class ConnectionHandler implements Runnable {
	public abstract ReadMessage poll() throws IbisIOException;
}
