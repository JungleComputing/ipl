package ibis.impl.tcp;

import ibis.ipl.IbisError;
import ibis.ipl.IbisIdentifier;
import ibis.util.IbisIdentifierTable;

import java.io.IOException;
import java.net.InetAddress;

public final class TcpIbisIdentifier extends IbisIdentifier implements java.io.Serializable {

	private static final long serialVersionUID = 3L;
	private InetAddress address;
	private static IbisIdentifierTable cache = new IbisIdentifierTable();

	public TcpIbisIdentifier(String name, InetAddress address) {
		super(name);
		this.address = address;
	}

	public InetAddress address() {
		return address;
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

	// no need to serialize super class fields, this is done automatically
	// We handle the address field special.
	// Do not do a writeObject on it (or a defaultWriteObject of the current object),
	// because InetAddress might not be rewritten as it is in the classlibs --Rob
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
                int handle = cache.getHandle(out, this);
		out.writeInt(handle);
		if(handle < 0) { // First time, send it.
                        out.writeUTF(address.getHostAddress());
		}
	}

	// no need to serialize super class fields, this is done automatically
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		int handle = in.readInt();
		if(handle < 0) {
			String addr = in.readUTF();
			try {
                                // this does not do a real lookup
                                address = InetAddress.getByName(addr);
                        } catch (Exception e) {
                                throw new IbisError("EEK, could not create an inet address" +
                                                                   "from a IP address. This shouldn't happen", e);
                        }

                        cache.addIbis(in, -handle, this);
		} else {
                        TcpIbisIdentifier ident = (TcpIbisIdentifier)
                                cache.getIbis(in, handle);
			address = ident.address;
			name = ident.name;
			cluster = ident.cluster;
		}
	}

	public void free() {
		cache.removeIbis(this);
	}
}
