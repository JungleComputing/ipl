package ibis.ipl.impl.tcp;

import ibis.ipl.ReadMessage;
import ibis.ipl.IbisException;

abstract class ConnectionHandler implements Runnable {
	public abstract ReadMessage poll() throws IbisException;
}
