package ibis.ipl.impl.net;

import ibis.ipl.IbisIOException;
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
	 * the properties of the corresponing {@linkplain NetSendPort send port}.
	 */
	protected StaticProperties staticProperties       = null;

	/**
	 * the sorted property key prefixes for this I/O.
	 *
	 * The first matching key (prefix plus name) The order is:
	 * <OL>
	 * <LI> /up. ... .up.driver.name/...up.driver.name/this.driver.name
	 * <LI> up. ... .up.driver.name/...up.driver.name/this.driver.name
	 * <LI> ...
	 * <LI> this.driver.name
	 * </OL>
	 *
	 * Exemple:
	 * <OL>
	 * <LI> "/gen/udp"
	 * <LI> "gen/udp"
	 * <LI> "udp"
	 * </OL>
	 */
	protected LinkedList       propertyKeys           = null;

	/**
	 * the property cache for this I/O.
	 */
	protected HashMap          propertyCache          = null;

	/**
	 * the controling I/O if there is one.
	 */
	protected NetIO            up                     = null;

	/**
	 * Constructor.
	 *
	 * @param staticProperties the properties of the port.
	 * @param driver           the driver.
	 * @param up the controlling I/O or <code>null</code> if this I/O is a root I/O.
	 */
	protected NetIO(StaticProperties staticProperties,
			NetDriver 	 driver,
			NetIO            up) {
		this.staticProperties = staticProperties;
		this.driver    	      = driver;
		this.up               = up;
	}

	/**
	 * Initializes a LinkedList with the property key prefixes in the right order.
	 *
	 * @see #propertyKeys
	 */
	private LinkedList initializePropertyKeys() {
		LinkedList keys = new LinkedList();
		NetIO  	   _up  = up;
		String 	   key  = driver.getName();

		keys.addFirst(key);

		while (_up != null) {
			key = _up.getDriverName() + "/" + key;
			keys.addFirst(key);
			_up = _up.getUp();
		}

		keys.addFirst("/" + key);

		return keys;
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
	 * Lookup the properties for a property destinated to this input.
	 *
	 * Note: the lookup order is the following:
	 * <OL>
	 * <LI> "/up. ... .up.driver.name/...up.driver.name/this.driver.name:name"
	 * <LI> "up. ... .up.driver.name/...up.driver.name/this.driver.name:name"
	 * <LI> ...
	 * <LI> "this.driver.name:name"
	 * <LI> "name"
	 * </OL>
	 *
	 * Exemple:
	 * <OL>
	 * <LI> "/gen/udp:timeout"
	 * <LI> "gen/udp:timeout"
	 * <LI> "udp:timeout"
	 * <LI> "timeout"
	 * </OL>
	 *
	 * The first matching key selects the value to be returned. 
	 * Note: the key/value result of each new {@link #getProperty} call
	 *       is stored in a cache for improving subsequent access latency.
	 *       As a result, the {@link StaticProperties} object should not be
	 *       modified after the initialization of the {@linkplain NetSendPort sendPort}
	 *       or {@linkplain NetReceivePort receivePort}.
	 *
	 * @param name the name of the property.
	 * @return the property or <code>null</code> if not found.
	 */
	protected String getProperty(String name) {
		String result = null;
		
		if (propertyCache != null) {
			if (propertyCache.containsKey(name)) {
				return (String)propertyCache.get(name);
			}
		} else {
			propertyCache = new HashMap();
		}

		if (propertyKeys == null) {
			propertyKeys = initializePropertyKeys();
		}

		Iterator i = propertyKeys.iterator();
		while (i.hasNext()) {
			String key = (String)i.next();
			result = staticProperties.find(key + ":" + name);

			if (result != null) {
				break;
			}
		}

		if (result == null) {
			result = staticProperties.find(name);
		}
		
		propertyCache.put(name, result);

		return result;
	}

	/**
	 * Helper function for sending a set of connection data to the connection peer.
	 */
	protected final void sendInfoTable(ObjectOutputStream os,
					   Hashtable          t)
		throws IbisIOException {
		try {
			os.writeObject(t);
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}	
	
	/**
	 * Helper function for receiving a set of connection data to
	 * the connection peer.
	 */
	protected final Hashtable receiveInfoTable(ObjectInputStream is) 
		throws IbisIOException {
		Hashtable t = null;
		try {
			t = (Hashtable)is.readObject();		 
		} catch (Exception e) {
			throw new IbisIOException(e);
		}

		return t;
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
	 * Write an short in a byte buffer.
	 * The first byte is the least significant byte.
	 *
	 * @param b the byte buffer.
	 * @param offset the offset ar which to write the value.
	 * @param value the short value to write in the buffer.
	 */
	public final void writeShort(byte [] b, int offset, short value) {
		b[offset] = (byte)value;
		value >>= 8;
		b[offset + 1] = (byte)value;
		value >>= 8;
	}
	
	/**
	 * Read an integer from a byte buffer.
	 * The first byte is the least significant byte.
	 *
	 * @param b the byte buffer.
	 * @param offset the offset of the first byte of the integer.
	 * @return the short value.
	 */
	public final short readShort(byte [] b, int offset) {
		short value = 0;

		value |=  ((short)b[offset + 0])&0xFF;
		value |= (((short)b[offset + 1])&0xFF) << 8;

		return value;
	}


	/**
	 * Write an integer in a byte buffer.
	 * The first byte is the least significant byte.
	 *
	 * @param b the byte buffer.
	 * @param offset the offset ar which to write the value.
	 * @param value the integer value to write in the buffer.
	 */
	public final void writeInt(byte [] b, int offset, int value) {
		b[offset] = (byte)value;
		value >>= 8;
		b[offset + 1] = (byte)value;
		value >>= 8;
		b[offset + 2] = (byte)value;
		value >>= 8;
		b[offset + 3] = (byte)value;
	}
	
	/**
	 * Read an integer from a byte buffer.
	 * The first byte is the least significant byte.
	 *
	 * @param b the byte buffer.
	 * @param offset the offset of the first byte of the integer.
	 * @return the integer value.
	 */
	public final int readInt(byte [] b, int offset) {
		int value = 0;

		value |=  ((int)b[offset + 0])&0xFF;
		value |= (((int)b[offset + 1])&0xFF) << 8;
		value |= (((int)b[offset + 2])&0xFF) << 16;
		value |= (((int)b[offset + 3])&0xFF) << 24;

		return value;
	}
	

	/**
	 * Write an long in a byte buffer.
	 * The first byte is the least significant byte.
	 *
	 * @param b the byte buffer.
	 * @param offset the offset ar which to write the value.
	 * @param value the long value to write in the buffer.
	 */
	public final void writeLong(byte [] b, int offset, long value) {
		b[offset + 0] = (byte)value;
		value >>= 8;
		b[offset + 1] = (byte)value;
		value >>= 8;
		b[offset + 2] = (byte)value;
		value >>= 8;
		b[offset + 3] = (byte)value;
		value >>= 8;
		b[offset + 4] = (byte)value;
		value >>= 8;
		b[offset + 5] = (byte)value;
		value >>= 8;
		b[offset + 6] = (byte)value;
		value >>= 8;
		b[offset + 7] = (byte)value;
	}
	
	/**
	 * Read an long from a byte buffer.
	 * The first byte is the least significant byte.
	 *
	 * @param b the byte buffer.
	 * @param offset the offset ar which to write the value.
	 * @return the long value.
	 */
	public final long readLong(byte [] b, int offset) {
		long value = 0;

		value |=  ((long)b[offset + 0])&0xFF;
		value |= (((long)b[offset + 1])&0xFF) << 8;
		value |= (((long)b[offset + 2])&0xFF) << 16;
		value |= (((long)b[offset + 3])&0xFF) << 24;
		value |= (((long)b[offset + 4])&0xFF) << 32;
		value |= (((long)b[offset + 5])&0xFF) << 40;
		value |= (((long)b[offset + 6])&0xFF) << 48;
		value |= (((long)b[offset + 7])&0xFF) << 56;

		return value;
	}

	/**
	 * Actually establish a connection with a remote port.
	 *
	 * @param rpn the integer locally associated to the remote port.
	 * @param is  a TCP input from the peer node.
	 * @param os  a TCP output to the peer node.
	 * @exception IbisIOException if the connection setup fails.
	 */
	public abstract void setupConnection(Integer            rpn,
					     ObjectInputStream  is,
					     ObjectOutputStream os)
		throws IbisIOException;


	/**
	 * Closes the I/O.
	 *
	 * Note: methods redefining this one should also call it.
	 */
	public void free() 
		throws IbisIOException {
		up                  =  null;
		mtu                 =     0;
		headerOffset        =     0;
		headerLength        =     0;
		driver              =  null;
		staticProperties    =  null;
		propertyKeys        =  null;
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
