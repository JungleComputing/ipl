package ibis.ipl.impl.net.gm;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;
import ibis.ipl.StaticProperties;

import ibis.ipl.impl.net.NetDriver;
import ibis.ipl.impl.net.NetIbis;
import ibis.ipl.impl.net.NetInput;
import ibis.ipl.impl.net.NetMutex;
import ibis.ipl.impl.net.NetOutput;

import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.net.DatagramSocket;

/**
 * The NetIbis GM driver with pipelined block transmission.
 */
public class Driver extends NetDriver {

	// Native functions
	//private static native void gm_init();
	//private static native void gm_exit();

        static NetMutex gmLock = new NetMutex();

	/**
	 * The driver name.
	 */
	private final String name = "gm";

	static native long nInitDevice(int deviceNum) throws IbisIOException;
	static native void nCloseDevice(long deviceHandler) throws IbisIOException;
        static native void nGmThread();

	static {
		System.loadLibrary("net_ibis_gm");
                gmLock = new NetMutex();
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
	public NetInput newInput(StaticProperties sp,
				 NetInput	  input)
		throws IbisIOException {
                
		return new GmInput(sp, this, input);
	}

	/**
	 * Creates a new GM output.
	 *
	 * @param sp the properties of the output's 
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param output the controlling output.
	 * @return The new GM output.
	 */
	public NetOutput newOutput(StaticProperties sp,
				   NetOutput	    output)
		throws IbisIOException {
		return new GmOutput(sp, this, output);
	}
}
