package ibis.ipl.impl.net.rel;

import ibis.ipl.impl.net.*;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.IbisIOException;


/**
 * The NetIbis 'reliability' driver.
 */
public final class Driver extends NetDriver implements RelConstants {

	static final boolean STATISTICS = true;

	/**
	 * The driver name.
	 */
	private final String name = "rel";

	Sweeper sweeper;

	/**
	 * Constructor.
	 *
	 * @param ibis the {@link NetIbis} instance.
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
	}	

	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return name;
	}

	/**
	 * {@inheritDoc}
	 */
	public NetInput newInput(NetPortType pt, NetIO up, String context) throws IbisIOException {
		RelInput input = new RelInput(pt, this, up, context);
		return input;
	}

	/**
	 * {@inheritDoc}
	 */
	public NetOutput newOutput(NetPortType pt, NetIO up, String context) throws IbisIOException {
		return new RelOutput(pt, this, up, context);
	}


	class InputElement {
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


	class Vector {
	    private final static int INCREMENT_DATABASE = 16;

	    private int		n;
	    private int		alloc;
	    private Object[]	data;

	    int add(Object x) {
		if (n == alloc) {
		    alloc += INCREMENT_DATABASE;
		    Object[] new_data = new Object[alloc];
		    if (n != 0) {
			System.arraycopy(data, 0, new_data, 0, n);
		    }
		    data = new_data;
		}
		data[n] = x;
		return n++;
	    }

	    Object get(int i) {
		if (i < 0 || i >= n) {
		    return null;
		}
		return data[i];
	    }

	    int size() {
		return n;
	    }
	}


	Vector input = new Vector();
	Vector output = new Vector();

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
System.err.println("Register Output " + output + " at index " + index);
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
System.err.println("Register InputConnection id " + id + " for RelInput " + input);
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
System.err.println("Register OutputConnection id " + id + " for index " + index);
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


    class Sweeper extends Thread {

	private Vector sweep = new Vector();
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
		    s.invoke();
		}
	    }
	}

    }
    

}
