package ibis.ipl.impl.net;

import ibis.ipl.StaticProperties;

import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.util.Hashtable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Provide a generic abstraction of an network {@link NetInput Input} or network {@link NetOutput Output}.
 */
public abstract class NetIO {

        /**
         * Optional (fine grained) logging object.
         *
         * This logging object should be used to display code-level information
         * like function calls, args and variable values.
         */
        protected NetLog           log                    = null;

        /**
         * Optional (coarse grained) logging object.
         *
         * This logging object should be used to display concept-level information
         * about high-level algorithmic steps (e.g. message send, new connection
         * initialization.
         */
        protected NetLog           trace                  = null;

        /**
         * Optional (general purpose) logging object.
         *
         * This logging object should only be used temporarily for debugging purpose.
         */
        protected NetLog           disp                   = null;

        /**
         * Optional statistic object.
         */
        protected NetMessageStat   stat                   = null;

	/**
	 * the maximum data length that can be transmitted atomically.
	 */
	protected int       	   mtu           	  = 	0;

	/**
	 * the offset at which this output's header starts for atomic packet.
	 */
	protected int       	   headerOffset  	  = 	0;

	/**
	 * the length of this output's header for atomic packet.
	 */
	protected int       	   headerLength  	  = 	0;

	/**
	 * the output's driver.
	 */
	protected NetDriver 	   driver        	  = null;

	/**
	 * the 'type' of the corresponding {@linkplain NetSendPort send port} or {@linkplain NetReceivePort receive port}.
	 */
        protected NetPortType      type                   = null;

        /**
         * the path to the driver in the driver tree.
         */
        protected String           context                = null;


	/**
	 * Current NetBufferFactory
	 */
	// protected NetBufferFactory factory = null;
	public NetBufferFactory factory                   = null;


	/**
	 * Constructor.
	 *
	 * @param type    the port type.
	 * @param driver  the driver.
         * @param context the context in which this object is created.
	 */
	protected NetIO(NetPortType      type,
			NetDriver 	 driver,
                        String           context) {
		this.type    = type;
		this.driver  = driver;

                if (context != null) {
                        this.context = context+"/"+getDriverName();
                } else {
                        this.context = "/"+getDriverName();
                }

                String s = "//"+type.name()+this.context;

                if (this instanceof NetOutput) {
                        s += ".output";
                } else if (this instanceof NetInput) {
                        s += ".input";
                }

                // Logging objects
                boolean logOn = type.getBooleanStringProperty(this.context, "Log", false);
                log = new NetLog(logOn, s, "LOG");

                boolean traceOn = type.getBooleanStringProperty(this.context, "Trace", false);
                trace = new NetLog(traceOn, s, "TRACE");

                boolean dispOn = type.getBooleanStringProperty(this.context, "Disp", true);
                disp = new NetLog(dispOn, s, "DISP");

                // Stat object
                boolean statOn = type.getBooleanStringProperty(this.context, "Stat", false);
                stat = newMessageStat(statOn, s);
// System.err.println(this + ": up...");
	}

        /**
         * Create a new object to compute message statistics.
         */
        protected NetMessageStat newMessageStat(boolean on, String moduleName) {
                return new NetMessageStat(on, moduleName);
        }

        /**
         * Returns the {@link #context} {@linkplain String string}.
         *
         * @return the {@link #context} {@linkplain String string}.
         */
        public final String context() {
                return context;
        }

	/**
	 * Returns the {@link #driver}.
	 *
	 * @return a reference to the {@link #driver}.
	 */
	public final NetDriver getDriver() {
		return driver;
	}

	/**
	 * Returns the {@link #driver}'s name.
	 *
	 * @return a {@linkplain String string} containing the {@link #driver}'s name.
	 */
	public final String getDriverName() {
		return driver.getName();
	}

        /**
         * Creates and returns a subcontext {@linkplain String string}.
         *
         * A subcontext string is a context string concatenated with a discriminant.
         *
         * @param contextValue the subcontext discriminant {@linkplain String string}.
         * @return a subcontext {@linkplain String string}.
         */
        private String subContext(String contextValue) {
                String sub = context;
                if (contextValue != null) {
                        sub += "#"+contextValue;
                }
                return sub;
        }

        /**
         * Creates and returns a new {@linkplain NetInput input} object.
         *
         * @param subDriver the new {@linkplain NetInput input}'s {@linkplain NetDriver driver}.
         * @param contextValue the {@link #subContext sub-context} discriminant.
         * @return the new {@linkplain NetInput input}.
         */
        public final NetInput newSubInput(NetDriver subDriver, String contextValue) throws IOException {
                return subDriver.newInput(type, subContext(contextValue));
        }

        /**
         * Creates and returns a new {@linkplain NetOutput output} object.
         *
         * @param subDriver the new {@linkplain NetOutput output}'s {@linkplain NetDriver driver}.
         * @param contextValue the {@link #subContext sub-context} discriminant.
         * @return the new {@linkplain NetOutput output}.
         */
        public final NetOutput newSubOutput(NetDriver subDriver, String contextValue) throws IOException {
                return subDriver.newOutput(type, subContext(contextValue));
        }

        /**
         * Creates and returns a new {@linkplain NetInput input} object with no subcontext discriminant.
         *
         * @param subDriver the new {@linkplain NetInput input}'s {@linkplain NetDriver driver}.
         * @return the new {@linkplain NetInput input}.
         */
        public final NetInput newSubInput(NetDriver subDriver) throws IOException {
                return newSubInput(subDriver, null);
        }

        /**
         * Creates and returns a new {@linkplain NetOutput output} object with no subcontext discriminant.
         *
         * @param subDriver the new {@linkplain NetOutput output}'s {@linkplain NetDriver driver}.
         * @return the new {@linkplain NetOutput output}.
         */
        public final NetOutput newSubOutput(NetDriver subDriver) throws IOException {
                return newSubOutput(subDriver, null);
        }

        /**
         * Returns a context sensitive property {@linkplain String string}.
         * Note: if the property is not found for that context, a default value
           is searched for, recursively removing subcontexts discriminants.
         *
         * @param contextValue the property context {@linkplain String string}.
         * @param name the property name {@linkplain String string}.
         * @return the property {@linkplain String string} value or <code>null</code> if not found.
         */
        public final String getProperty(String contextValue, String name) {
                return type.getStringProperty(subContext(contextValue), name);
        }

        /**
         * Returns a default property {@linkplain String string}.
         * @param name the property name {@linkplain String string}.
         * @return the property {@linkplain String string} value or <code>null</code> if not found.
         */
        public final String getProperty(String name) {
                return getProperty(null, name);
        }

        /**
         * Returns a context sensitive property {@linkplain String string}.
         * Note: if the property is not found for that context, a default value
           is searched for, recursively removing subcontexts discriminants.
         *
         * @param contextValue the property context {@linkplain String string}.
         * @param name the property name {@linkplain String string}.
         * @throws Error if the property is not found
         * @return the property {@linkplain String string} value.
         */
        public final String getMandatoryProperty(String contextValue, String name) {
                String s = getProperty(contextValue, name);
                if (s == null) {
                        throw new Error(name+" property not specified");
                }

                return s;
        }

        /**
         * Returns a context sensitive property {@linkplain String string}.
         * Note: if the property is not found for that context, a default value
           is searched for, recursively removing subcontexts discriminants.
         *
         * @param contextValue the property context {@linkplain String string}.
         * @param name the property name {@linkplain String string}.
         * @throws Error if the property is not found
         * @return the property {@linkplain String string} value.
         */
         public final String getMandatoryProperty(String name) {
                String s = getProperty(name);
                if (s == null) {
                        throw new Error(name+" property not specified");
                }

                return s;
        }


	/**
	 * Install a custom {@link NetBufferFactory} for atomic packet allocation.
	 *
	 * @param factory the {@link NetBuffer} factory
	 */
	public void setBufferFactory(NetBufferFactory factory) {
	    if (NetBufferFactory.DEBUG) {
		System.err.println(this + ": +++++++++++ set a new BufferFactory " + factory);
		Thread.dumpStack();
	    }
	    this.factory = factory;
	    if (NetBufferFactory.DEBUG) {
		dumpBufferFactoryInfo();
	    }
	}

        /**
         * Displays the current {@link NetBufferFactory}.
         */
	public void dumpBufferFactoryInfo() {
	    if (NetBufferFactory.DEBUG) {
		System.err.println(this + ": +++++++++++ current BufferFactory " + factory);
		Thread.dumpStack();
	    }
	}

	/**
	 * Create a {@link NetBuffer} using the installed factory.
	 *
	 * This is only valid for a {@link NetBufferFactory Factory} with MTU.
	 *
	 * @throws java.lang.IllegalArgumentException if no factory is installed or the factory has no default MTU.
         * @return the new {@link NetBuffer buffer}.
	 */
	public NetBuffer createBuffer() {
	    if (factory == null) {
		throw new IllegalArgumentException("Need a factory with MTU");
	    }
	    return factory.createBuffer();
	}

	/**
	 * Create a {@link NetBuffer} using the installed factory.
	 *
	 * @param length the length of the data to be stored in the buffer.
	 *        The buffer is a new byte array.
	 * @throws java.lang.IllegalArgumentException if the factory has no default MTU.
         * @return the new {@link NetBuffer buffer}.
	 */
	public NetBuffer createBuffer(int length) {
	    if (factory == null) {
		factory = new NetBufferFactory();
	    }
	    return factory.createBuffer(length);
	}

	/**
	 * Returns the maximum atomic packet transfert unit for this input.
	 *
	 * @return the maximum transfert unit.
	 */
	public final int getMaximumTransfertUnit() {
		return mtu;
	}

	/**
	 * Changes the offset of the atomic packet header start for this input.
	 *
	 * @param offset the new offset.
	 */
	public void setHeaderOffset(int offset) {
// System.err.println("Set header offset to " + offset);
// Thread.dumpStack();
		headerOffset = offset;
	}

	/**
	 * Returns the total atomic packet header's part length.
	 *
	 * @return the total header's part length.
	 */
	public int getHeadersLength() {
		return headerOffset + headerLength;
	}

	/**
	 * Return the atomic packet header length for this {@link NetIO}.
	 *
	 * @return the header length.
	 */
	public int getHeaderLength() {
		return headerLength;
	}

	/**
	 * Returns the maximum atomic payload size for this input.
	 *
	 * @return the maximum payload size.
	 */
	public int getMaximumPayloadUnit() {
		return mtu - (headerOffset + headerLength);
	}

	/**
	 * Actually establish a connection with a remote port.
	 *
	 * @param cnx the connection attributes.
	 * @exception IOException if the connection setup fails.
	 */
	public abstract void setupConnection(NetConnection cnx) throws IOException;


	/**
	 * Unconditionaly closes the I/O.
	 *
	 * Note: this method should not block and can be called at any time and several time for the same connection.
         * @param num the connection identifier
         * @exception IOException if this operation fails (that should not happen).
	 */
        public abstract void close(Integer num) throws IOException;

	/**
	 * Closes the I/O.
	 *
	 * Note: methods redefining this one should also call it, just in case
         *       we need to add something here
         * @exception IOException if this operation fails.
	 */
	public void free() throws IOException {
                //stat.report();
	}

	/**
	 * Finalizes this IO object.
	 *
	 * Note: methods redefining this one should also call the superclass version at the end.
         * @exception Throwable in case of problem.
	 */
	protected void finalize()
		throws Throwable {
		free();
		super.finalize();
	}
}
