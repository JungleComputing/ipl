package ibis.ipl.impl.tcp;

import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.IbisIdentifier;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.EOFException;

public final class TcpReceivePortIdentifier implements ReceivePortIdentifier, java.io.Serializable { 
	private static final long serialVersionUID = 4L;

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
			return (type().equals(other.type()) && ibis.equals(other.ibis) && name().equals(other.name()) && port == other.port);
		}		
	}

	public boolean equals(Object other) { 

		if (other == null) return false;

		if (other instanceof TcpReceivePortIdentifier) { 
			return equals((TcpReceivePortIdentifier) other);
		} else { 
			return false;		
		}
	} 
	
	//gosia
	public int hashCode() {
		return name().hashCode() + type().hashCode() + ibis.hashCode() + port;
	}
	//end gosia

	public String name() {
		if (name != null) {
			return name;
		}
		return "__anonymous__";
	}

	public String type() {
		if (type != null) {
			return type;
		}
		return "__notype__";
	}

	public IbisIdentifier ibis() {
		return ibis;
	}

	public String toString() {
		return ("(TcpRecPortIdent: name = " + name + ", type = " + type + ", ibis = " + ibis + "port = " + port + ")");
	}
}  

