package ibis.ipl.impl.tcp;

import ibis.ipl.SendPortIdentifier;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.IbisIdentifier;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;

public final class TcpSendPortIdentifier implements SendPortIdentifier, java.io.Serializable { 
	private static final long serialVersionUID = 5L;

	String type;
	String name;
	TcpIbisIdentifier ibis;

	TcpSendPortIdentifier(String name, String type, TcpIbisIdentifier ibis) {
		this.name = name;
		this.type = type;
		this.ibis = ibis;
	}

	public boolean equals(TcpSendPortIdentifier other) {
		
		if (other == null) { 
			return false;
		} else { 
			return (type.equals(other.type) && ibis.equals(other.ibis) && name.equals(other.name));
		}
	}

	public boolean equals(Object other) { 
		if (other == null) return false;
		if (other instanceof TcpSendPortIdentifier) {			
			return equals((TcpSendPortIdentifier) other);
		} else { 
			return false;
		}
	} 

	public String name() {
		if (name != null) {
			return name;
		}

		return "anonymous";
	}

	public String type() {
		return type;
	}

	public IbisIdentifier ibis() {
		return ibis;
	}

	public String toString() {
		return ("(TcpSendPortIdent: name = " + (name != null ? name : "anonymous") + ", type = " + type + ", ibis = " + ibis + ")");
	}
}
