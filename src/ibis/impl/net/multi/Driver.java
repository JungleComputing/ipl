package ibis.ipl.impl.net.multi;

import ibis.ipl.impl.net.*;

import java.util.Hashtable;

import java.io.IOException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIOException;

/**
 * The multieric splitter/poller virtual driver.
 */
public final class Driver extends NetDriver {

	/**
	 * The driver name.
	 */
	private final String name = "multi";

        private Hashtable    pluginTable = new Hashtable();

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

	/**
	 * {@inheritDoc}
	 */
	public NetInput newInput(NetPortType pt, String context) throws IOException {
		return new MultiPoller(pt, this, context);
	}

	/**
	 * {@inheritDoc}
	 */
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
                                Constructor cons     = cls.getConstructor(null);

                                plugin = (MultiPlugin)cons.newInstance(null);
			} catch (Exception e) {
				throw new IbisIOException(e);
			}

			pluginTable.put(name, plugin);

                        //System.err.println("Loading multi-protocol plugin ["+name+"] done");
                }

                return plugin;
        }
}
