package ibis.ipl.impl.net;

import ibis.ipl.IbisIdentifier;

import java.net.InetAddress;

/**
 * Provides a identifier for {@link NetIbis} instances.
 */
// the implements should be unnecessary, but the IOGenerator does not 
// see that the super class implents it, and rewrites the bytecode.
public final class NetIbisIdentifier
	extends IbisIdentifier
	implements java.io.Serializable {

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
		return "(NetId: " + name +
			" on [" + address.getHostName() + ", " +
			address.getHostAddress() + "])";
	}

	/**
	 * {@inheritDoc}
	 */
	public int hashCode() {
		return name.hashCode();
	}
}
