package ibis.impl.net.rel;

import ibis.impl.net.NetDriver;
import ibis.impl.net.NetIbis;
import ibis.impl.net.NetInput;
import ibis.impl.net.NetInputUpcall;
import ibis.impl.net.NetOutput;
import ibis.impl.net.NetPortType;
import ibis.impl.net.NetVector;
import ibis.ipl.IbisIdentifier;

import ibis.util.TypedProperties;

import java.io.IOException;


/**
 * The NetIbis 'reliability' driver.
 */
public final class Driver extends NetDriver implements RelConstants {

	private static final String prefix = "ibis.net.rel.";

	static final String rel_window = prefix + "window";

	private static final String[] properties = {
		rel_window
	};

	static {
		TypedProperties.checkProperties(prefix, properties, null);
	}

	/**
	 * The driver name.
	 */
	private final String name = "rel";

	Sweeper sweeper;

	/**
	 * Constructor.
	 *
	 * @param ibis the {@link ibis.impl.net.NetIbis} instance.
	 */
	public Driver(NetIbis ibis) {
		super(ibis);

		sweeper = new Sweeper(sweepInterval);

		sweeper.setDaemon(true);
		sweeper.setName("net.rel.Driver sweeper");
		if (false) {
		    System.err.println("*************** Don't use sweeper (yet)");
		} else {
		    sweeper.start();
		}

		if (false) {
		    System.err.println("Start a time slice snooper");
		    Snooper snooper = new Snooper();
		    snooper.setDaemon(true);
		    snooper.setName("net.rel.Driver time slice snooper");
		    snooper.start();
		}

	}

	public String getName() {
		return name;
	}

	public NetInput newInput(NetPortType pt,
				 String context,
				 NetInputUpcall inputUpcall)
		       	throws IOException {
		RelInput input = new RelInput(pt, this, context, inputUpcall);
		return input;
	}

	public NetOutput newOutput(NetPortType pt, String context) throws IOException {
		return new RelOutput(pt, this, context);
	}


	private final static class InputElement {
	    RelInput	input;
	    boolean	matched;
	    IbisIdentifier	remote_out_id;

	    InputElement(RelInput input, IbisIdentifier id) {
		this.input = input;
		this.remote_out_id = id;
	    }
	}


	class OutputElement {
	    RelOutput		output;
	    IbisIdentifier	remote_in_id;

	    OutputElement(RelOutput output) {
		this.output = output;
	    }
	}


	NetVector input = new NetVector();
	NetVector output = new NetVector();

	/**
	 * Database of incoming and outgoing connections. Use this to
	 * do piggybacking of acks on data messages.
	 *
	 * @param output the {@link RelOutput} that is registered
	 * @return the index of the connection in the database
	 */
	synchronized
	int registerOutput(RelOutput output) {
	    int index = this.output.add(new OutputElement(output));
	    if (DEBUG) {
		System.err.println("Register Output " + output + " at index " + index);
	    }
	    return index;
	}


	/**
	 * Database of incoming and outgoing connections. Use this to
	 * do piggybacking of acks on data messages.
	 *
	 * @param input the {@link RelInput} that owns the connection
	 * @param id the {@link IbisIdentifier} of the remote {@link RelOutput}
	 */
	synchronized
	void registerInputConnection(RelInput input, IbisIdentifier id) {
	    if (DEBUG) {
		System.err.println("Register InputConnection id " + id + " for RelInput " + input);
	    }
	    this.input.add(new InputElement(input, id));
	}


	/**
	 * Database of incoming and outgoing connections. Use this to
	 * do piggybacking of acks on data messages.
	 *
	 * @param index the index in our Output database of the
	 *        {@link RelOutput} that owns the connection
	 * @param id the {@link IbisIdentifier} of the remote {@link RelInput}
	 */
	synchronized
	void registerOutputConnection(int index, IbisIdentifier id) {
	    if (DEBUG) {
		System.err.println("Register OutputConnection id " + id + " for index " + index);
	    }
	    OutputElement e = (OutputElement)output.get(index);
	    e.remote_in_id = id;
	}


	/**
	 * Database of incoming and outgoing connections. Use this to
	 * do piggybacking of acks on data messages.
	 * Query by a {@link RelOutput} to find a suitable {@link RelInput}
	 * whose acks can be piggybacked.
	 *
	 * @param output the {@link RelOutput} that wants to be matched
	 * @param id the {@link IbisIdentifier} of the Ibis that
	 *        <CODE>output</CODE> is connected to
	 * @return a suitable {@link RelInput} or <CODE>null</CODE> if no
	 *        suitable {@link RelInput} is found.
	 */
	synchronized
	RelInput requestPiggyPartner(RelOutput output, IbisIdentifier id) {
	    InputElement e = null;
	    int		i = 0;

	    for (i = 0; i < input.size(); i++) {
		e = (InputElement)input.get(i);
		if (id.equals(e.remote_out_id) && ! e.matched) {
		    break;
		}
	    }
	    if (i == input.size()) {
		return null;
	    }
	    e.matched = true;
	    return e.input;
	}


	/**
	 * Database of incoming and outgoing connections. Use this to
	 * do piggybacking of acks on data messages.
	 * Query by a {@link RelInput} to find the {@link RelOutput}
	 * whose acks are piggybacked.
	 *
	 * @param index the index of the {@link RelOutput}
	 * @return the {@link RelOutput} that was registered with index
	 *        <CODE>index</CODE>
	 */
	synchronized
	RelOutput lookupPiggyPartner(int index) {
	    OutputElement e = (OutputElement)output.get(index);
	    if (e == null) {
		return null;
	    }
	    return e.output;
	}


    void registerSweep(RelSweep sweep) {
	sweeper.registerSweep(sweep);
    }

    void unRegisterSweep(RelSweep sweep) {
	sweeper.unRegisterSweep(sweep);
    }


    private final static class Sweeper extends Thread {

	private NetVector sweep = new NetVector();
	private long sweepInterval = 30;

	Sweeper(long sweepInterval) {
	    this.sweepInterval = sweepInterval;
	}

	/**
	 * Having a sweeper thread per input or output seems a bit much.
	 * Driver offers a sweeper service.
	 */
	synchronized void registerSweep(RelSweep sweep) {
	    this.sweep.add(sweep);
	}

	synchronized void unRegisterSweep(RelSweep sweep) {
	    this.sweep.delete(sweep);
	}


	public void run() {
	    while (true) {
		synchronized (this) {
		    try {
			wait(sweepInterval);
		    } catch (InterruptedException e) {
			// Ignore
		    }
		}
		for (int i = 0; i < sweep.size(); i++) {
		    RelSweep s = (RelSweep)sweep.get(i);
		    if (s != null) {
			s.invoke();
		    }
		}
	    }
	}

    }


    private final static class Snooper extends Thread {

	public void run() {
	    while (true) {
		synchronized (this) {
		    try {
			wait(30);
		    } catch (InterruptedException e) {
			// Ignore
		    }
		}
		System.err.print("-");
	    }
	}

    }

}
