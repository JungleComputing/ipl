package ibis.ipl.impl.net.gm;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;

import ibis.ipl.impl.net.*;

import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.net.DatagramSocket;

/**
 * The NetIbis GM driver with pipelined block transmission.
 */
public final class Driver extends NetDriver {

	// Native functions
	//private static native void gm_init();
	//private static native void gm_exit();

        static NetMutex         gmReceiveLock = null;
        static NetPriorityMutex gmAccessLock  = null;
        static NetLockArray     gmLockArray   = null;


	/**
	 * The driver name.
	 */
	private final String name = "gm";

	static native long nInitDevice(int deviceNum) throws IbisIOException;
	static native void nCloseDevice(long deviceHandler) throws IbisIOException;
        static native void nGmThread();

	static {
		System.loadLibrary("net_ibis_gm");
                gmReceiveLock = new NetMutex(false);
                gmAccessLock = new NetPriorityMutex(false);
                gmLockArray = new NetLockArray();
                gmLockArray.initLock(0, false);
	}


	/**
	 * Constructor.
	 *
	 * @param ibis the {@link NetIbis} instance.
	 */
	public Driver(NetIbis ibis) {
		super(ibis);
	}	

	/**
	 * Returns the name of the driver.
	 *
	 * @return The driver name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Creates a new GM input.
	 *
	 * @param sp the properties of the input's 
	 * {@link ibis.ipl.impl.net.NetReceivePort NetReceivePort}.
	 * @param input the controlling input.
	 * @return The new GM input.
	 */
	public NetInput newInput(NetPortType pt, NetIO up, String context)
		throws IbisIOException {
                
		return new GmInput(pt, this, up, context);
	}

	/**
	 * Creates a new GM output.
	 *
	 * @param sp the properties of the output's 
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param output the controlling output.
	 * @return The new GM output.
	 */
	public NetOutput newOutput(NetPortType pt, NetIO up, String context)
		throws IbisIOException {
		return new GmOutput(pt, this, up, context);
	}
}
