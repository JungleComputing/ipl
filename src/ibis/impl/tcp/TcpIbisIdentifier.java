// handcrafted by Rob, do *NOT* overwrite!
// We cache these identifiers for efficiency, Satin sends them frequently.
package ibis.ipl.impl.tcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.EOFException;
import java.net.InetAddress;
import ibis.ipl.IbisIdentifier;
import ibis.io.Serializable;
import ibis.io.IbisSerializationOutputStream;
import ibis.io.IbisSerializationInputStream;

// the implements should be unnecessary, but the IOGenerator does not 
// see that the super class implents it, and rewrites the bytecode.
public final class TcpIbisIdentifier extends IbisIdentifier
	implements java.io.Serializable /*java.io.Externalizable*/,
			   ibis.io.Serializable {
	private static final long serialVersionUID = 3L;

	public TcpIbisIdentifier() {
		System.err.println("EEK, TcpIbisIdentifier default ctor!");
	}

	public TcpIbisIdentifier(String name, InetAddress address) {
		super(name, address);
	}

	public TcpIbisIdentifier(IbisSerializationInputStream stream)
		throws java.io.IOException {

		stream.addObjectToCycleCheck(this);
		generated_DefaultReadObject(stream, 0);
	}

	public final void generated_DefaultReadObject
		(IbisSerializationInputStream stream, int lvl)
		throws java.io.IOException {

		int handle = stream.readInt();
		if(handle < 0) {
			try {
				// this does not do a real lookup
				address = InetAddress.getByName(stream.readUTF());
			} catch (Exception e) {
				System.err.println("EEK, could not create an inet address" +
								   "from a IP address. This shouldn't happen");
				System.exit(1);
			}
			name = stream.readUTF();
			cluster = stream.readUTF();
			TcpIbis.globalIbis.identTable.addIbis(stream, -handle, this);
		} else {
			TcpIbisIdentifier ident = (TcpIbisIdentifier)
				TcpIbis.globalIbis.identTable.getIbis(stream, handle);
			address = ident.address;
			name = ident.name;
			cluster = ident.cluster;
		}
	}

	public final void generated_WriteObject
		(IbisSerializationOutputStream stream) throws java.io.IOException {

		int handle = TcpIbis.globalIbis.identTable.getHandle(stream, this);
		stream.writeInt(handle);
		if(handle < 0) { // First time, send it.
			stream.writeUTF(address.getHostAddress());
			stream.writeUTF(name);
			stream.writeUTF(cluster);
		}
	}

	public final void generated_DefaultWriteObject
		(IbisSerializationOutputStream stream, int lvl)
		throws java.io.IOException {

		generated_WriteObject(stream);
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
		return /*address.equals(other.address) &&*/ name.equals(other.name);
	}

	public String toString() {
		String a = (address == null ? "<null>" : address.getHostName() + ", " + address.getHostAddress());
		String n = (name == null ? "<null>" : name);
		return ("(TcpId: " + n + " on [" + a + "])");
	}

	public int hashCode() {
		return name.hashCode();
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
