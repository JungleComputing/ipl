package ibis.impl.net.s_data;

import ibis.impl.net.NetDriver;
import ibis.impl.net.NetIbis;
import ibis.impl.net.NetInput;
import ibis.impl.net.NetInputUpcall;
import ibis.impl.net.NetOutput;
import ibis.impl.net.NetPortType;

import java.io.IOException;

public final class Driver extends NetDriver {

	/**
	 * The driver name.
	 */
	private final String name = "s_data";


	/**
	 * Constructor.
	 *
	 * @param ibis the {@link NetIbis} instance.
	 */
	public Driver(NetIbis ibis) {
		super(ibis);
	}	

	public String getName() {
		return name;
	}

	public NetInput newInput(NetPortType pt, String context, NetInputUpcall inputUpcall) throws IOException {
		return new SDataInput(pt, this, context, inputUpcall);
	}

	public NetOutput newOutput(NetPortType pt, String context) throws IOException {
		return new SDataOutput(pt, this, context);
	}
}
