package ibis.ipl.impl.net;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReceivePortIdentifier;

import java.io.Serializable;

import java.util.Hashtable;

public class NetReceivePortIdentifier
	implements ReceivePortIdentifier, Serializable {
	String	     	  name           = null;
	String	     	  type           = null;
	NetIbisIdentifier ibis           = null;
	Hashtable         connectionInfo = null;

	NetReceivePortIdentifier(String	  	   name,
				 String	  	   type,
				 NetIbisIdentifier ibis,
				 Hashtable         connectionInfo) {
		this.name	    = name;
		this.type	    = type;
		this.ibis	    = ibis;
		this.connectionInfo = connectionInfo;
	}

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

	public boolean equals(ReceivePortIdentifier other) { 

		if (other instanceof NetReceivePortIdentifier) { 
			return equals((NetReceivePortIdentifier) other);
		} else { 
			return false;		
		}
	} 

	public String name() {
		return name;
	}

	public String type() {
		return type;
	}

	public IbisIdentifier ibis() {
		return ibis;
	}

	public Hashtable connectionInfo() {
		return connectionInfo;
	}

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
