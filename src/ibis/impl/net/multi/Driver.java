package ibis.impl.net.multi;

import ibis.impl.net.NetDriver;
import ibis.impl.net.NetIbis;
import ibis.impl.net.NetInput;
import ibis.impl.net.NetInputUpcall;
import ibis.impl.net.NetOutput;
import ibis.impl.net.NetPortType;
import ibis.ipl.IbisIOException;

import java.io.IOException;
import java.util.Hashtable;

/**
 * The multieric splitter/poller virtual driver.
 */
public final class Driver extends NetDriver {

	/**
	 * The driver name.
	 */
	private final String name = "multi";

        private Hashtable    pluginTable = new Hashtable();

	static {
	    System.err.println("**** net.multi probably broken by all optimizations");
	}

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
	    if (inputUpcall == null && pt.inputSingletonOnly()) {
		return new SingletonPoller(pt, this, context, inputUpcall);
	    } else {
		return new MultiPoller(pt, this, context, inputUpcall);
	    }
	}

	public NetOutput newOutput(NetPortType pt, String context) throws IOException {
	    return new MultiSplitter(pt, this, context);
	}

        protected MultiPlugin loadPlugin(String name) throws IOException {
		MultiPlugin plugin = (MultiPlugin)pluginTable.get(name);

                if (plugin == null) {
                        //System.err.println("Loading multi-protocol plugin ["+name+"]...");

			try {
                                String      clsName  =
                                        getClass().getPackage().getName()
                                        + ".plugins"
                                        + "."
                                        + name;

                                Class       cls      = Class.forName(clsName);
                                plugin = (MultiPlugin)cls.newInstance();
			} catch (Exception e) {
				throw new IbisIOException(e);
			}

			pluginTable.put(name, plugin);

                        //System.err.println("Loading multi-protocol plugin ["+name+"] done");
                }

                return plugin;
        }
}
