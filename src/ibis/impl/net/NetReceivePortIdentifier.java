package ibis.impl.net;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReceivePortIdentifier;

import java.util.Hashtable;

/**
 * Provides an identifier for a {@link NetReceivePort}.
 */
public final class NetReceivePortIdentifier
	implements ReceivePortIdentifier, java.io.Serializable {

	/**
	 * ReceivePort type name.
	 */
	String	     	  name           = null;

	/**
	 * ReceivePort name.
	 */
	String	     	  type           = null;

	/**
	 * Ibis instance identifier.
	 */
	NetIbisIdentifier ibis           = null;

	/**
	 * Set of connection data.
	 */
        byte []           infoBytes      = null;

	/**
	 * Constructor.
	 *
	 * @param name the name of the ReceivePort.
	 * @param type the type name.
	 * @param ibis the Ibis instance identifier.
	 * @param connectionInfo the set of connection data
	 */
	NetReceivePortIdentifier(String	  	   name,
				 String	  	   type,
				 NetIbisIdentifier ibis,
				 Hashtable         connectionInfo) {
		this.name	    = name;
		this.type	    = type;
		this.ibis	    = ibis;
                try {
                        this.infoBytes = NetConvert.object2bytes(connectionInfo);
                } catch (Exception e) {
                        throw new Error(e.getMessage());
                }
	}

	
	/**
	 * Specific equality test.
	 *
	 * @return The equality result.
	 */
	public boolean equals(NetReceivePortIdentifier other) {
		if (other == null) { 
			return false;
		}

		Object o1 = this.connectionInfo();
		Object o2 = other.connectionInfo();

		return (type().equals(other.type())
			&&
			ibis.equals(other.ibis)
			&&
			name().equals(other.name())
			&&
			o1 == o2);
	}

	/**
	 * @inheritDoc
	 */
	public int hashCode() {
		return name().hashCode() + ibis.hashCode();
	}

	/**
	 * Generic equality test.
	 *
	 * @return The equality result.
	 */
	public boolean equals(Object other) { 

		if (other instanceof NetReceivePortIdentifier) { 
			return equals((NetReceivePortIdentifier) other);
		} else { 
			return false;		
		}
	} 

	/**
	 * Returns the ReceivePort name.
	 *
	 * @return The name.
	 */
	public String name() {
		if (name != null) {
			return name;
		}
		return "__anonymous__";
	}

	/**
	 * Returns the ReceivePort type name.
	 *
	 * @return The type name.
	 */
	public String type() {
		if (type != null) {
			return type;
		}
		return "__notype__";
	}
	
	/**
	 * Returns the Ibis instance identifier.
	 *
	 * @return The Ibis identifier.
	 */
	public IbisIdentifier ibis() {
		return ibis;
	}

	/**
	 * Returns the table of connection data.
	 *
	 * @return A reference the {@link #connectionInfo} table.
	 */
	public Hashtable connectionInfo() {
                try {
                        return (Hashtable)NetConvert.bytes2object(infoBytes);
                } catch (Exception e) {
                        throw new Error(e.getMessage());
                }
	}

	/*
	 * Returns a string representation of the identifier.
	 *
	 * @return A string representing the ReceivePort identifier
	 */
	public String toString() {
		return ("(" +
			"NetRecPortIdent: name = " + (name == null ? "<null>" : name) +
			", " +
			"type = " + (type == null ? "<null>" : type) +
			", " +
			"ibis = " + (ibis == null ? "<null>" : ibis.toString()) +
			", " +
			"info = " + (infoBytes == null ? "<null>" : connectionInfo().toString()) +
			")");
	}

}
