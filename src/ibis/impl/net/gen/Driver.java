package ibis.impl.net.gen;

import ibis.impl.net.NetDriver;
import ibis.impl.net.NetIbis;
import ibis.impl.net.NetInput;
import ibis.impl.net.NetInputUpcall;
import ibis.impl.net.NetOutput;
import ibis.impl.net.NetPortType;
import ibis.ipl.StaticProperties;
import ibis.util.TypedProperties;

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
	 * @param ibis the {@link NetIbis} instance.
	 */
	public Driver(NetIbis ibis) {
		super(ibis);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return name;
	}

	private boolean inputSingletonOnly(NetPortType type,
					   NetInputUpcall upcallFunc) {
	    StaticProperties prop = type.properties();
	    boolean singletonOnly = upcallFunc == null
				&& TypedProperties.booleanProperty("ibis.net.poller.singleton", true)
				&& ! prop.isProp("communication", "ManyToOne")
				&& ! prop.isProp("communication", "Poll")
				&& ! prop.isProp("communication", "ReceiveTimeout");
	    if (false && singletonOnly) {
		System.err.println(this + ": set Poller.singletonOnly to " + singletonOnly);
		System.err.println(this + ": upcallFunc " + upcallFunc);
		System.err.println(this + ": property ManyToOne " + prop.isProp("communication", "ManyToOne"));
	    }
	    return singletonOnly;
	}

	/**
	 * {@inheritDoc}
	 */
	public NetInput newInput(NetPortType pt, String context, NetInputUpcall inputUpcall) throws IOException {
	    if (inputSingletonOnly(pt, inputUpcall)) {
		return new SingletonPoller(pt, this, context, inputUpcall);
	    } else {
		return new GenPoller(pt, this, context, inputUpcall);
	    }
	}

	private boolean outputSingletonOnly(NetPortType type) {
	    StaticProperties prop = type.properties();
	    if (false) {
		System.err.println(this + ": property OneToMany " + prop.isProp("communication", "OneToMany"));
	    }
	    return ! prop.isProp("communication", "OneToMany")
		    && TypedProperties.booleanProperty("ibis.net.poller.singleton", true);
	}

	/**
	 * {@inheritDoc}
	 */
	public NetOutput newOutput(NetPortType pt, String context) throws IOException {
	    if (outputSingletonOnly(pt)) {
		return new SingletonSplitter(pt, this, context);
	    } else {
		return new GenSplitter(pt, this, context);
	    }
	}
}
