package ibis.ipl.impl.net;

import ibis.ipl.IbisIdentifier;

import ibis.io.MantaOutputStream;
import ibis.io.MantaInputStream;

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

	public NetIbisIdentifier(MantaInputStream stream) throws ibis.ipl.IbisIOException {
		stream.addObjectToCycleCheck(this);
		int handle = stream.readInt();

		if(handle < 0) {
			try {
				address = InetAddress.getByName(stream.readUTF()); // this does not do a real lookup
			} catch (Exception e) {
				System.err.println("could not create an inet address from a IP address");
				System.exit(1);
			}
			name = stream.readUTF();
			NetIbis.globalIbis.identTable.addIbis(stream, -handle, this);
		} else {
			NetIbisIdentifier ident = (NetIbisIdentifier)NetIbis.globalIbis.identTable.getIbis(stream, handle);
			address = ident.address;
			name    = ident.name;
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
		return "(NetId: "+name+" on ["+address.getHostName()+", "+address.getHostAddress()+"])";
	}

	/**
	 * {@inheritDoc}
	 */
	public int hashCode() {
		return name.hashCode();
	}

	public final void generated_WriteObject(MantaOutputStream stream) throws ibis.ipl.IbisIOException {
		int handle = NetIbis.globalIbis.identTable.getHandle(stream, this);
		stream.writeInt(handle);
		if (handle < 0) {
			stream.writeUTF(address.getHostAddress());
			stream.writeUTF(name);
		}
	}
}
