package ibis.ipl.impl.tcp;

import java.io.IOException;

import ibis.ipl.ReadMessage;

abstract class ConnectionHandler implements Runnable {
	public abstract ReadMessage poll() throws IOException;
}
