// handcrafted by Rob, do *NOT* overwrite!
// We cache these identifiers for efficiency, Satin sends them frequently.
package ibis.ipl.impl.messagePassing;

import ibis.io.IbisSerializationOutputStream;
import ibis.io.IbisSerializationInputStream;
import java.net.InetAddress;
import ibis.ipl.impl.generic.IbisIdentifierTable;

import ibis.ipl.IbisIOException;

// Make this final, make inlining possible
final class IbisIdentifier
	extends ibis.ipl.IbisIdentifier
	implements java.io.Serializable, ibis.io.Serializable {

	private int cpu;
	private transient byte[] serialForm;


	IbisIdentifier(String name, int cpu) throws IbisIOException {
	    try {
		    this.address = java.net.InetAddress.getLocalHost();
	    } catch (java.net.UnknownHostException e) {
		    // this should not happen.
		    System.err.println("EEk: could not get my own IP address: " + e);
		    System.exit(1);
	    }

	    this.name = new String(name);
	    this.cpu  = cpu;
	    makeSerialForm();
	}


	public IbisIdentifier(IbisSerializationInputStream stream) throws java.io.IOException {
		stream.addObjectToCycleCheck(this);
		generated_DefaultReadObject(stream, 0);
	}

	public final void generated_DefaultReadObject(IbisSerializationInputStream stream, int lvl) throws java.io.IOException {
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
			Ibis.myIbis.identTable.addIbis(stream, -handle, this);
		} else {
			IbisIdentifier ident = (IbisIdentifier) Ibis.myIbis.identTable.getIbis(stream, handle);
			address = ident.address;
			name = ident.name;
			cpu = ident.cpu;
		}
	}


	static IbisIdentifier createIbisIdentifier(byte[] serialForm)
		throws IbisIOException {
	    IbisIdentifier id = (IbisIdentifier)SerializeBuffer.readObject(serialForm);
	    id.serialForm = serialForm;

	    return id;
	}


	private void makeSerialForm() throws IbisIOException {
	    serialForm = SerializeBuffer.writeObject(this);
	}


	byte[] getSerialForm() throws IbisIOException {
	    if (serialForm == null) {
		makeSerialForm();
	    }
	    return serialForm;
	}


	public final void generated_WriteObject(IbisSerializationOutputStream stream) throws java.io.IOException {
		int handle = Ibis.myIbis.identTable.getHandle(stream, this);
		stream.writeInt(handle);
		if(handle < 0) { // First time, send it.
			stream.writeUTF(address.getHostAddress());
			stream.writeInt(cpu);
			stream.writeUTF(name);
		}
	}

	public final void generated_DefaultWriteObject(IbisSerializationOutputStream stream, int lvl) throws java.io.IOException {
		generated_WriteObject(stream);
	}

	// Compare ranks here, much faster. This is method critical for Satin. --Rob
	public boolean equals(IbisIdentifier other) {
		return cpu == other.cpu;
	}

	public boolean equals(Object o) {
		if(o == this) return true;

		if (o instanceof IbisIdentifier) {
			IbisIdentifier other = (IbisIdentifier)o;
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

	int getCPU() {
	    return cpu;
	}

}
