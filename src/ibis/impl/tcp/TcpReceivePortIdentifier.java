package ibis.ipl.impl.tcp;

import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.IbisIdentifier;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.EOFException;

public final class TcpReceivePortIdentifier implements ReceivePortIdentifier, java.io.Serializable { 

	String name;
	String type;
	TcpIbisIdentifier ibis;
	int port;

	TcpReceivePortIdentifier(String name, String type, TcpIbisIdentifier ibis, int port) {
		this.name = name;
		this.type = type;
		this.ibis = ibis;
		this.port = port;
	}

	public boolean equals(TcpReceivePortIdentifier other) { 		
		if (other == null) { 
			return false;
		} else { 			
			return (type.equals(other.type) && ibis.equals(other.ibis) && name.equals(other.name) && port == other.port);
		}		
	}

	public boolean equals(ReceivePortIdentifier other) { 

		if (other instanceof TcpReceivePortIdentifier) { 
			return equals((TcpReceivePortIdentifier) other);
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

	public String toString() {
		return ("(TcpRecPortIdent: name = " + name + ", type = " + type + ", ibis = " + ibis + "port = " + port + ")");
	}
}  

