package ibis.impl.net.gen;

import ibis.impl.net.NetDriver;
import ibis.impl.net.NetIbis;
import ibis.impl.net.NetInput;
import ibis.impl.net.NetInputUpcall;
import ibis.impl.net.NetOutput;
import ibis.impl.net.NetPortType;

import java.io.IOException;

/**
 * The generic splitter/poller virtual driver.
 */
public final class Driver extends NetDriver {

	/**
	 * The driver name.
	 */
	private final String name = "gen";
	
	/**
	 * Constructor.
	 *
	 * @param ibis the {@link ibis.impl.net.NetIbis} instance.
	 */
	public Driver(NetIbis ibis) {
		super(ibis);
	}

	public String getName() {
		return name;
	}

	public NetInput newInput(NetPortType pt, String context, NetInputUpcall inputUpcall) throws IOException {
	    if (inputUpcall == null && pt.inputSingletonOnly()) {
// System.err.println(this + ": PortType " + pt + ": SingletonPoller, inputUpcall " + inputUpcall);
		return new SingletonPoller(pt, this, context, inputUpcall);
	    } else {
// System.err.println(this + ": PortType " + pt + ": no SingletonPoller, inputUpcall " + inputUpcall);
		return new GenPoller(pt, this, context, inputUpcall);
	    }
	}

	public NetOutput newOutput(NetPortType pt, String context) throws IOException {
	    return new GenSplitter(pt, this, context);
	}
}
