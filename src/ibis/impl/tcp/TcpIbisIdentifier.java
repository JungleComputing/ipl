package ibis.ipl.impl.tcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.EOFException;
import java.net.InetAddress;
import ibis.ipl.IbisIdentifier;

public final class TcpIbisIdentifier implements IbisIdentifier, java.io.Serializable {

	public static final int serialversionID = 1;
	InetAddress address;
	String name;

	public TcpIbisIdentifier() { 
	} 

	public boolean equals(Object o) {
		if (o instanceof TcpIbisIdentifier) {
			TcpIbisIdentifier other = (TcpIbisIdentifier) o;
			return equals(other);
		}
		return false;
	}

	public boolean equals(TcpIbisIdentifier other) {
		return address.equals(other.address) && name.equals(other.name);
	}

	public String toString() {
		return ("(TcpId: " + name + " on [" + address.getHostName() + ", " + address.getHostAddress() + "])");
	}

	public String name() {
		return name;
	}

	public int hashCode() {
		return name.hashCode();
	}
}
