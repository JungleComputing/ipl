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
	 * the type of the corresponing {@linkplain NetSendPort send port}.
	 */
	protected NetPortType      type                   = null;

	/**
	 * the controling I/O if there is one.
	 */
	protected NetIO            up                     = null;

        protected String           context                = null;

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

        private String subContext(String contextValue) {
                String sub = context;
                if (contextValue != null) {
                        sub += "#"+contextValue;
                }
                return sub;
        }
        
        public final NetInput newSubInput(NetDriver subDriver, String contextValue) throws IbisIOException {
                return subDriver.newInput(type, this, subContext(contextValue));
        }
        
        public final NetOutput newSubOutput(NetDriver subDriver, String contextValue) throws IbisIOException {
                return subDriver.newOutput(type, this, subContext(contextValue));
        }

        public final NetInput newSubInput(NetDriver subDriver) throws IbisIOException {
                return newSubInput(subDriver, null);
        }
        
        public final NetOutput newSubOutput(NetDriver subDriver) throws IbisIOException {
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
	 * @param rpn the integer locally associated to the remote port.
	 * @param is  a TCP input from the peer node.
	 * @param os  a TCP output to the peer node.
	 * @exception IbisIOException if the connection setup fails.
	 */
	public abstract void setupConnection(Integer            rpn,
					     ObjectInputStream  is,
					     ObjectOutputStream os,
                                             NetServiceListener nsl)
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
                type                =  null;
                context             =  null;
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
