package ibis.ipl.impl.net;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.SendPortIdentifier;

import java.io.Serializable;

public class NetSendPortIdentifier
	implements SendPortIdentifier, Serializable {
	private String 	    	  type = null;
	private String 	    	  name = null;
	private NetIbisIdentifier ibis = null;


	public NetSendPortIdentifier(String	       name,
				     String	       type,
				     NetIbisIdentifier ibis) {
		this.name = name;
		this.type = type;
		this.ibis = ibis;
	}

	
	public boolean equals(NetSendPortIdentifier other) {
		
		if (other == null) { 
			return false;
		} else { 
			return (type.equals(other.type)
				&&
				ibis.equals(other.ibis)
				&&
				name.equals(other.name));
		}
	}

	public boolean equals(SendPortIdentifier other) {
		if (other instanceof NetSendPortIdentifier) {			
			return equals((NetSendPortIdentifier) other);
		} else { 
			return false;
		}
	}
	
	public String type() {
		return type;
	}
	
	public String name() {
		if (name != null) {
			return name;
		}

		return "anonymous";
	}
	
	public IbisIdentifier ibis() {
		return ibis;
	}	
} 
