// handcrafted by Rob, do *NOT* overwrite!
// We cache these identifiers for efficiency, Satin sends them frequently.
package ibis.ipl.impl.messagePassing;

import ibis.io.IbisSerializationOutputStream;
import ibis.io.IbisSerializationInputStream;
import java.net.InetAddress;
import ibis.ipl.impl.generic.IbisIdentifierTable;

import ibis.ipl.IbisIOException;

// Make this final, make inlining possible
final class IbisIdentifier extends ibis.ipl.IbisIdentifier implements java.io.Serializable, ibis.io.Serializable {

	private int cpu;
	private transient byte[] serialForm;

	IbisIdentifier(String name, int cpu) throws IbisIOException {
	    this(name, cpu, null);
	}

	IbisIdentifier(String name, int cpu, byte[] inetAddr)
		throws IbisIOException {
	    if (inetAddr == null) {
		try {
			this.address = java.net.InetAddress.getLocalHost();
		} catch (java.net.UnknownHostException e) {
			// this should not happen.
			System.err.println("EEk: could not get my own IP address: " + e);
			System.exit(1);
		}

	    } else {
		SerializeInputBuffer addrBuffer = null;
		java.net.InetAddress addr = null;
		try {
		    addrBuffer = new SerializeInputBuffer(inetAddr);
		    addr = (java.net.InetAddress)addrBuffer.objIn.readObject();
		    addrBuffer.objIn.close();
		} catch (Exception e) {
System.err.println("And the serialization throws " + e);
		    throw new IbisIOException(e);
		}
		this.address = addr;
	    }
	    this.name = new String(name);
	    this.cpu  = cpu;
	    makeSerialForm();
	}

	public IbisIdentifier(IbisSerializationInputStream stream) throws ibis.ipl.IbisIOException {
		stream.addObjectToCycleCheck(this);
		int handle = stream.readInt();
		if(handle < 0) {
			try {
				address = InetAddress.getByName(stream.readUTF()); // this does not do a real lookup
			} catch (Exception e) {
				System.err.println("EEK, could not create an inet address from a IP address. This should not happen:" + e);
				System.exit(1);
			}
			cpu = stream.readInt();
			name = stream.readUTF();
			ibis.ipl.impl.messagePassing.Ibis.myIbis.identTable.addIbis(stream, -handle, this);
		} else {
			IbisIdentifier ident = (IbisIdentifier) Ibis.myIbis.identTable.getIbis(stream, handle);
			address = ident.address;
			name = ident.name;
			cpu = ident.cpu;
		}
	}

	public final void generated_WriteObject(IbisSerializationOutputStream stream) throws ibis.ipl.IbisIOException {
		int handle = Ibis.myIbis.identTable.getHandle(stream, this);
		stream.writeInt(handle);
		if(handle < 0) { // First time, send it.
			stream.writeUTF(address.getHostAddress());
			stream.writeInt(cpu);
			stream.writeUTF(name);
		}
	}

	private void makeSerialForm() throws IbisIOException {
	    SerializeOutputBuffer addrBuffer = null;
	    try {
		addrBuffer = new SerializeOutputBuffer();
		addrBuffer.objOut.writeObject(address);
		addrBuffer.objOut.flush();
		addrBuffer.objOut.close();
	    } catch (java.io.IOException e) {
		throw new IbisIOException(e);
	    }
	    serialForm = addrBuffer.getData();
	}

	byte[] getSerialForm() {
	    return serialForm;
	}

	// Compare ranks here, much faster. This is method critical for Satin. --Rob
	public boolean equals(ibis.ipl.impl.messagePassing.IbisIdentifier other) {
		return cpu == other.cpu;
	}

	public boolean equals(Object o) {
		if(o == this) return true;

		if (o instanceof ibis.ipl.impl.messagePassing.IbisIdentifier) {
			ibis.ipl.impl.messagePassing.IbisIdentifier other = (ibis.ipl.impl.messagePassing.IbisIdentifier)o;
			// there is only one messagePassing.Ibis per cpu, so this should be ok
			return cpu == other.cpu;
		}
		return false;
	}

	public String toString() {
		return ("(IbisIdent: name = \"" + name + "\")");
	}

	public String name() {
		return name;
	}

	public int hashCode() {
		return name.hashCode();
	}
}
