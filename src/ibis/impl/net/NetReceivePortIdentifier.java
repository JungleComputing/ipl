package ibis.ipl.impl.net;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReceivePortIdentifier;

import java.util.Hashtable;

/**
 * Provides an identifier for a {@link NetReceivePort}.
 */
public class NetReceivePortIdentifier
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
	Hashtable         connectionInfo = null;


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
		this.connectionInfo = connectionInfo;
	}

	
	/**
	 * Specific equality test.
	 *
	 * @return The equality result.
	 */
	public boolean equals(NetReceivePortIdentifier other) { 		
		if (other == null) { 
			return false;
		} else { 			
			return (type.equals(other.type)
				&&
				ibis.equals(other.ibis)
				&&
				name.equals(other.name)
				&&
				connectionInfo == other.connectionInfo);
		}		
	}

	/**
	 * Generic equality test.
	 *
	 * @return The equality result.
	 */
	public boolean equals(ReceivePortIdentifier other) { 

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
		return name;
	}

	/**
	 * Returns the ReceivePort type name.
	 *
	 * @return The type name.
	 */
	public String type() {
		return type;
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
		return connectionInfo;
	}

	/*
	 * Returns a string representation of the identifier.
	 *
	 * @return A string representing the ReceivePort identifier
	 */
	public String toString() {
		return ("(" +
			"NetRecPortIdent: name = " + name +
			", " +
			"type = " + type +
			", " +
			"ibis = " + ibis +
			", " +
			"info = " + connectionInfo +
			")");
	}

}
