package ibis.ipl.impl.net;

import ibis.ipl.IbisIdentifier;

import ibis.io.IbisSerializationOutputStream;
import ibis.io.IbisSerializationInputStream;
import java.io.IOException;

import java.net.InetAddress;

/**
 * Provides a identifier for {@link NetIbis} instances.
 */
// the implements should be unnecessary, but the IOGenerator does not 
// see that the super class implents it, and rewrites the bytecode.
public final class NetIbisIdentifier
	extends IbisIdentifier
	implements java.io.Serializable, ibis.io.Serializable {

	/**
	 * Serialization version ID
	 */
	public static final int serialversionID = 1;

	/**
	 * Default constructor (should not be called).
	 *
	 * Calling this constructor will result in the program being aborted.
	 */
	public NetIbisIdentifier() {
		__.abort__("NetIbisIdentifier default constructor called");
	}

	public NetIbisIdentifier(IbisSerializationInputStream stream) throws IOException {
		stream.addObjectToCycleCheck(this);
		generated_DefaultReadObject(stream, 0);
	}

	public final void generated_DefaultReadObject(IbisSerializationInputStream stream, int lvl) throws java.io.IOException {
		int handle = stream.readInt();

		if(handle < 0) {
			try {
				address = InetAddress.getByName(stream.readUTF()); // this does not do a real lookup
			} catch (Exception e) {
				System.err.println("could not create an inet address from a IP address");
				System.exit(1);
			}
			name = stream.readUTF();
			cluster = stream.readUTF();
			NetIbis.globalIbis.identTable.addIbis(stream, -handle, this);
		} else {
			NetIbisIdentifier ident = (NetIbisIdentifier)NetIbis.globalIbis.identTable.getIbis(stream, handle);
			address = ident.address;
			name    = ident.name;
			cluster = ident.cluster;
		}
	}

	/**
	 * Constructor.
	 *
	 * @param name the name of the instance.
	 * @param address the {@linkplain InetAddress IP address} of the instance.
	 */
	public NetIbisIdentifier(String      name,
				 InetAddress address) {
		super(name, address);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		
		if (o instanceof NetIbisIdentifier) {
			NetIbisIdentifier other = (NetIbisIdentifier) o;

			return equals(other);
		}
		return false;
	}

	/**
	 * Specialized version of {link Object#equals}.
	 */
	public boolean equals(NetIbisIdentifier other) {
		if (other == this) {
			return true;
		}
		
		return address.equals(other.address) && name.equals(other.name);
	}

	/**
	 * {@inheritDoc}
	 */
	public String toString() {
		String a = address == null ? "<null>" : (address.getHostName()+", "+address.getHostAddress());
		String n = name == null ? "<null>" : name;

		return "(NetId: "+ n +" on ["+ a +"])";
	}

	/**
	 * {@inheritDoc}
	 */
	public int hashCode() {
		return name.hashCode();
	}

	public final void generated_WriteObject(IbisSerializationOutputStream stream) throws IOException {
		int handle = NetIbis.globalIbis.identTable.getHandle(stream, this);
		stream.writeInt(handle);
		if (handle < 0) {
			stream.writeUTF(address.getHostAddress());
			stream.writeUTF(name);
			stream.writeUTF(cluster);
		}
	}

	public final void generated_DefaultWriteObject(IbisSerializationOutputStream stream, int lvl) throws IOException {
		generated_WriteObject(stream);
	}
}
