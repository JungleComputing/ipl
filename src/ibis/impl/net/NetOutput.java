package ibis.ipl.impl.net;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;
import ibis.ipl.StaticProperties;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.Socket;

import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.util.Hashtable;
import java.util.Iterator;


/**
 * Provides an abstraction of a network output.
 */
public abstract class NetOutput extends NetIO {
	/**
	 * Constructor.
	 *
	 * @param staticProperties the properties of the corresponing
	 *                         {@linkplain NetReceivePort receive port}.
	 * @param driver the output's driver.
	 * @param output the controlling output or <code>null</code>
	 *               if this output is a root output.
	 */
	protected NetOutput(StaticProperties staticProperties,
			    NetDriver 	     driver,
			    NetOutput 	     output) {
		super(staticProperties, driver, output);
	}

	/**
	 * Sends a buffer over the network.
	 *
	 * @param buffer the buffer to be sent.
	 * @exception IbisIOException if the transmission fails.
	 */
	public abstract void sendBuffer(NetSendBuffer buffer)
		throws IbisIOException;

}
