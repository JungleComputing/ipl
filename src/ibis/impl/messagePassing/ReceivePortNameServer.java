package ibis.ipl.impl.messagePassing;

import java.io.IOException;

import java.util.Hashtable;

public abstract class ReceivePortNameServer implements
    ReceivePortNameServerProtocol {

    private Hashtable ports;

    protected ReceivePortNameServer() throws IOException {
	ports = new Hashtable();
    }

    protected abstract void bind_reply(int ret, int tag, int client);

    /* Called from native */
    private void bind(ReceivePortIdentifier ri, int tag, int client)
	    throws ClassNotFoundException {
	// already taken: synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis)
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockOwned();
// System.err.println(Thread.currentThread() + "" + this + ": bind receive port " + ri + " client " + client);

	ReceivePortIdentifier storedId;

	/* Check wheter the name is in use.*/
	storedId = (ReceivePortIdentifier)ports.get(ri.name);

	if (storedId != null) {
// System.err.println(Thread.currentThread() + "Don't bind existing port name \"" + ri.name + "\"");
	    bind_reply(PORT_REFUSED, tag, client);
	} else {
// System.err.println(Thread.currentThread() + "Bound new port name \"" + ri.name + "\"");
	    bind_reply(PORT_ACCEPTED, tag, client);
	    ports.put(ri.name, ri);
	}
    }

    protected abstract void lookup_reply(int ret, int tag, int client, String name, String type, int cpu, int port);

    /* Called from native */
    private void lookup(String name, int tag, int client) throws ClassNotFoundException {
	// already taken: synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis)
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockOwned();

	ReceivePortIdentifier storedId;

	storedId = (ReceivePortIdentifier)ports.get(name);

	if (storedId != null) {
// System.err.println(Thread.currentThread() + "Give this client his ReceivePort \"" + name + "\"; cpu " + storedId.cpu + " port " + storedId.port);
	    lookup_reply(PORT_KNOWN, tag, client, storedId.name, storedId.type, storedId.cpu, storedId.port);
	} else {
// System.err.println(Thread.currentThread() + "Cannot give this client his ReceivePort \"" + name + "\"");
	    lookup_reply(PORT_UNKNOWN, tag, client, null, null, -1, -1);
	}
    }

    /* Called from native */
    private void unbind(String name) throws ClassNotFoundException {
	// already taken: synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis) {
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockOwned();
	ports.remove(name);
    }

}
