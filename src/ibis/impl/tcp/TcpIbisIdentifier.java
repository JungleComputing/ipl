package ibis.ipl.impl.tcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.EOFException;
import java.net.InetAddress;
import ibis.ipl.IbisIdentifier;
import ibis.io.Serializable;
import ibis.ipl.IbisIOException;
import ibis.io.MantaOutputStream;
import ibis.io.MantaInputStream;


public final class TcpIbisIdentifier implements IbisIdentifier, java.io.Serializable, ibis.io.Serializable {
	public static final int serialversionID = 1;
	InetAddress address; // these are transferred as strings in the form WWW.XXX.YYY.ZZZ
	                     // to avoid (SUN) serialization problems, *NO* lookups are needed.
	String name;

	public TcpIbisIdentifier() { 
	}

	public TcpIbisIdentifier(MantaInputStream stream) throws IbisIOException, ClassNotFoundException {
		stream.addObjectToCycleCheck(this);
		int handle = stream.readInt();
		if(handle < 0) {
			try {
				address = InetAddress.getByName(stream.readUTF());
			} catch (Exception e) {
				throw new IbisIOException(e);
			}
			name = stream.readUTF();
			TcpIbis.globalIbis.identTable.addIbis(stream, -handle, this);
		} else {
			TcpIbisIdentifier ident = (TcpIbisIdentifier) TcpIbis.globalIbis.identTable.getIbis(stream, handle);
			address = ident.address;
			name = ident.name;
		}
	}

	public final void generated_WriteObject(MantaOutputStream stream) throws IbisIOException {
		int handle = TcpIbis.globalIbis.identTable.getHandle(stream, this);
		stream.writeInt(handle);
		if(handle < 0) { // First time, send it.
			stream.writeUTF(address.getHostAddress());
			stream.writeUTF(name);
		}
	}

	public boolean equals(Object o) {
		if(o == this) return true;
		if (o instanceof TcpIbisIdentifier) {
			TcpIbisIdentifier other = (TcpIbisIdentifier) o;
			return equals(other);
		}
		return false;
	}

	public boolean equals(TcpIbisIdentifier other) {
		if(other == this) return true;
		return address.equals(other.address) && name.equals(other.name);
	}

	public String toString() {
		return ("(TcpId: " + name + " on [" + 
			address.getHostName() + ", " + 
			address.getHostAddress() + "])");
	}

	public String name() {
		return name;
	}

	public int hashCode() {
		return name.hashCode();
	}
}
