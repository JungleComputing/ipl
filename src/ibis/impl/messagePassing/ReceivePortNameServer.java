package ibis.ipl.impl.messagePassing;

import ibis.ipl.IbisIOException;

import java.util.Hashtable;

final class ReceivePortNameServer implements
    ReceivePortNameServerProtocol {

    private Hashtable ports;

    protected ReceivePortNameServer() throws IbisIOException {
	ports = new Hashtable();
    }

    native void bind_reply(int ret, int tag, int client);

    /* Called from native */
    private void bind(ReceivePortIdentifier ri, int tag, int client)
	    throws ClassNotFoundException {
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockOwned();
	if (ReceivePortNameServerProtocol.DEBUG) {
	    System.err.println(Thread.currentThread() + "" + this + ": bind receive port " + ri + " client " + client);
	}

	ReceivePortIdentifier storedId;

	/* Check wheter the name is in use.*/
	storedId = (ReceivePortIdentifier)ports.get(ri.name);

	if (storedId != null) {
	    if (ReceivePortNameServerProtocol.DEBUG) {
		System.err.println(Thread.currentThread() + "Don't bind existing port name \"" + ri.name + "\"");
	    }
	    bind_reply(PORT_REFUSED, tag, client);
	} else {
	    if (ReceivePortNameServerProtocol.DEBUG) {
		System.err.println(Thread.currentThread() + "Bound new port name \"" + ri.name + "\"" + " ibis " + ri.ibis().name());
	    }
	    bind_reply(PORT_ACCEPTED, tag, client);
	    ports.put(ri.name, ri);
	}
    }

    native void lookup_reply(int ret, int tag, int client, String name, String type, String ibis_name, int cpu, byte[] inetAddr, int port);

    /* Called from native */
    private void lookup(String name, int tag, int client) throws ClassNotFoundException {
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockOwned();

	ReceivePortIdentifier storedId;

	storedId = (ReceivePortIdentifier)ports.get(name);

	if (storedId != null) {
	    if (ReceivePortNameServerProtocol.DEBUG) {
		System.err.println(Thread.currentThread() + "Give this client his ReceivePort \"" + name + "\"; cpu " + storedId.cpu + " port " + storedId.port);
	    }
	    IbisIdentifier ibisId = (IbisIdentifier)storedId.ibis();
	    lookup_reply(PORT_KNOWN, tag, client, storedId.name, storedId.type, ibisId.name(), storedId.cpu, ibisId.getSerialForm(), storedId.port);
	} else {
	    if (ReceivePortNameServerProtocol.DEBUG) {
		System.err.println(Thread.currentThread() + "Cannot give this client his ReceivePort \"" + name + "\"");
	    }
	    lookup_reply(PORT_UNKNOWN, tag, client, null, null, null, -1, null, -1);
	}
    }

    /* Called from native */
    private void unbind(String name) throws ClassNotFoundException {
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockOwned();
	ports.remove(name);
    }

}
