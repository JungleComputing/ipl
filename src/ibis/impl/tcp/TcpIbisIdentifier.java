// handcrafted by Rob, do *NOT* overwrite!
package ibis.ipl.impl.tcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.EOFException;
import java.net.InetAddress;
import ibis.ipl.IbisIdentifier;
import ibis.io.Serializable;
import ibis.io.MantaOutputStream;
import ibis.io.MantaInputStream;

// the implements should be unnecessary, but the IOGenerator does not 
// see that the super class implents it, and rewrites the bytecode.
public final class TcpIbisIdentifier extends IbisIdentifier implements java.io.Serializable /*java.io.Externalizable*/, ibis.io.Serializable {
	private static final long serialVersionUID = 3L;

	public TcpIbisIdentifier() {
		System.err.println("EEK, TcpIbisIdentifier default ctor!");
	}

	public TcpIbisIdentifier(String name, InetAddress address) {
		super(name, address);
	}

	public TcpIbisIdentifier(MantaInputStream stream) throws ibis.ipl.IbisIOException {
		stream.addObjectToCycleCheck(this);
		int handle = stream.readInt();
		if(handle < 0) {
			try {
				address = InetAddress.getByName(stream.readUTF()); // this does not do a real lookup
			} catch (Exception e) {
				System.err.println("EEK, could not create an inet address from a IP address. This should not happen");
				System.exit(1);
			}
			name = stream.readUTF();
			TcpIbis.globalIbis.identTable.addIbis(stream, -handle, this);
		} else {
			TcpIbisIdentifier ident = (TcpIbisIdentifier) TcpIbis.globalIbis.identTable.getIbis(stream, handle);
			address = ident.address;
			name = ident.name;
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
		return ("(TcpId: " + name + " on [" + address.getHostName() + ", " + address.getHostAddress() + "])");
	}

	public int hashCode() {
		return name.hashCode();
	}

	public final void generated_WriteObject(MantaOutputStream stream) throws ibis.ipl.IbisIOException {
		int handle = TcpIbis.globalIbis.identTable.getHandle(stream, this);
		stream.writeInt(handle);
		if(handle < 0) { // First time, send it.
			stream.writeUTF(address.getHostAddress());
			stream.writeUTF(name);
		}
	}
/*
	public void writeExternal(java.io.ObjectOutput out) throws IOException {
		out.writeObject(address);
		out.writeObject(name);
	}

	public void readExternal(java.io.ObjectInput in) throws IOException, ClassNotFoundException {
		address = (java.net.InetAddress) in.readObject();
		name = (String) in.readObject();
	}
*/
}
