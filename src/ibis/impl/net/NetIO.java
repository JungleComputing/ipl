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
 * Provide a generic abstraction of an network Input or network Output.
 */
public abstract class NetIO {
	/**
	 * the maximum data length that can be sent atomically.
	 */
	protected int       	   mtu           	  = 	0;

	/**
	 * the offset at which this output's header starts.
	 */
	protected int       	   headerOffset  	  = 	0;

	/**
	 * the length of this output's header.
	 */
	protected int       	   headerLength  	  = 	0;

	/**
	 * the output's driver.
	 */
	protected NetDriver 	   driver        	  = null;

	/**
	 * the type of the corresponing {@linkplain NetSendPort send port}.
	 */
	protected NetPortType      type                   = null;

	/**
	 * the controling I/O if there is one.
	 */
	protected NetIO            up                     = null;

        protected String           context                = null;


	/**
	 * Current NetBufferFactory
	 */
	protected NetBufferFactory factory;


	/**
	 * Constructor.
	 *
	 * @param staticProperties the properties of the port.
	 * @param driver           the driver.
	 * @param up the controlling I/O or <code>null</code> if this I/O is a root I/O.
	 */
	protected NetIO(NetPortType      type,
			NetDriver 	 driver,
			NetIO            up,
                        String           context) {
		this.type    = type;
		this.driver  = driver;
		this.up      = up;
                if (context != null) {
                        this.context = context+"/"+getDriverName();
                } else {
                        this.context = "/"+getDriverName();
                }
	}

	/**
	 * Returns the controlling I/O object.
	 *
	 * @return the controlling I/O object reference or <code>null</code> if this I/O is a root I/O.
	 */
	protected NetIO getUp() {
		return up;
	}

	/**
	 * Returns the output's driver.
	 *
	 * @return a reference to the output's driver.
	 */
	public final NetDriver getDriver() {
		return driver;
	}

	/**
	 * Returns the input driver's name.
	 *
	 * @return a reference to the input driver's name.
	 */
	public final String getDriverName() {
		return driver.getName();
	}

        private String subContext(String contextValue) {
                String sub = context;
                if (contextValue != null) {
                        sub += "#"+contextValue;
                }
                return sub;
        }

        public final NetInput newSubInput(NetDriver subDriver, String contextValue) throws NetIbisException {
                return subDriver.newInput(type, this, subContext(contextValue));
        }

        public final NetOutput newSubOutput(NetDriver subDriver, String contextValue) throws NetIbisException {
                return subDriver.newOutput(type, this, subContext(contextValue));
        }

        public final NetInput newSubInput(NetDriver subDriver) throws NetIbisException {
                return newSubInput(subDriver, null);
        }

        public final NetOutput newSubOutput(NetDriver subDriver) throws NetIbisException {
                return newSubOutput(subDriver, null);
        }

        public final String getProperty(String contextValue, String name) {
                return type.getStringProperty(subContext(contextValue), name);
        }

        public final String getProperty(String name) {
                return getProperty(null, name);
        }

        public final String getMandatoryProperty(String contextValue, String name) {
                String s = getProperty(contextValue, name);
                if (s == null) {
                        throw new Error(name+" property not specified");
                }

                return s;
        }

        public final String getMandatoryProperty(String name) {
                String s = getProperty(name);
                if (s == null) {
                        throw new Error(name+" property not specified");
                }

                return s;
        }


	/**
	 * Install a custom {@link NetBufferFactory}.
	 *
	 * @param factory the {@link NetBuffer} factory
	 */
	public void setBufferFactory(NetBufferFactory factory) {
	    this.factory = factory;
	}

	/**
	 * Create a {@link NetBuffer} using the installed factory.
	 *
	 * This is only valid for a Factory with MTU.
	 *
	 * @throws an {@link NetIbisException} if the factory has no default MTU
	 */
	public NetBuffer createBuffer() throws NetIbisException {
	    if (factory == null) {
		throw new NetIbisException("Need a factory with MTU");
	    }
	    return factory.createBuffer();
	}

	/**
	 * Create a {@link NetBuffer} using the installed factory.
	 *
	 * @param length the length of the data to be stored in the buffer.
	 *        The buffer is a new byte array
	 */
	public NetBuffer createBuffer(int length) throws NetIbisException {
	    if (factory == null) {
		factory = new NetBufferFactory();
	    }
	    return factory.createBuffer(length);
	}

	/**
	 * Returns the maximum transfert unit for this input.
	 *
	 * @return The maximum transfert unit.
	 */
	public final int getMaximumTransfertUnit() {
		return mtu;
	}

	/**
	 * Changes the offset of the header start for this input.
	 *
	 * @param offset the new offset.
	 */
	public void setHeaderOffset(int offset) {
		headerOffset = offset;
	}

	/**
	 * Returns the total header's part length.
	 *
	 * @return The total header's part length.
	 */
	public int getHeadersLength() {
		return headerOffset + headerLength;
	}

	/**
	 * Return this input's header length.
	 *
	 * @return This input's header length.
	 */
	public int getHeaderLength() {
		return headerLength;
	}

	/**
	 * Returns the maximum atomic payload size for this input.
	 *
	 * @return The maximum payload size.
	 */
	public int getMaximumPayloadUnit() {
		return mtu - (headerOffset + headerLength);
	}

	/**
	 * Actually establish a connection with a remote port.
	 *
	 * @param cnx the connection data.
	 * @exception NetIbisException if the connection setup fails.
	 */
	public abstract void setupConnection(NetConnection cnx) throws NetIbisException;


        public abstract void close(Integer num) throws NetIbisException;

	/**
	 * Closes the I/O.
	 *
	 * Note: methods redefining this one should also call it, just in case
         *       we need to add something here
	 */
	public void free() throws NetIbisException {
                // nothing
	}

	/**
	 * Finalizes this IO object.
	 *
	 * Note: methods redefining this one should call it at the end.
	 */
	protected void finalize()
		throws Throwable {
		free();
		super.finalize();
	}
}
