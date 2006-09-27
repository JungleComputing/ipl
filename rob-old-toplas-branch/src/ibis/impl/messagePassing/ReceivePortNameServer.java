/* $Id$ */

package ibis.impl.messagePassing;

import ibis.io.Conversion;

import java.io.IOException;
import java.util.Hashtable;

/**
 * messagePassing implementation of NameServer: the ReceivePort naming server
 */
final class ReceivePortNameServer implements ReceivePortNameServerProtocol {

    private Hashtable ports;

    protected ReceivePortNameServer() throws IOException {
        ports = new Hashtable();
    }

    private native void bind_reply(int ret, int tag, int client);

    /* Called from native */
    void bind(String name, byte[] serialForm, int tag, int client)
            throws IOException {
        Ibis.myIbis.checkLockOwned();
        ReceivePortIdentifier ri = null;
        try {
            ri = (ReceivePortIdentifier) Conversion.byte2object(serialForm);
        } catch (ClassNotFoundException e) {
            System.err.println("Cannot deserialize ReceivePortId to be bound");
            bind_reply(PORT_REFUSED, tag, client);
            return;
        }

        if (ReceivePortNameServerProtocol.DEBUG) {
            System.err.println(Thread.currentThread() + "" + this
                    + ": bind receive port " + ri + " client " + client);
        }

        ReceivePortIdentifier storedId;

        /* Check whether the name is in use.*/
        storedId = (ReceivePortIdentifier) ports.get(name);

        if (storedId != null) {
            if (ReceivePortNameServerProtocol.DEBUG) {
                System.err.println(Thread.currentThread()
                        + "Don't bind existing port name \"" + ri.name()
                        + "\", currently bound to \"" + storedId.name() + "\"");
            }
            bind_reply(PORT_REFUSED, tag, client);
        } else {
            if (ReceivePortNameServerProtocol.DEBUG) {
                System.err.println(Thread.currentThread()
                        + "Bound new port name \"" + ri.name() + "\"" + "=\""
                        + ri + "\" ibis " + ri.ibis());
            }
            bind_reply(PORT_ACCEPTED, tag, client);
            ports.put(name, ri);
        }
    }

    private native void lookup_reply(int ret, int tag, int client, int seqno,
            byte[] rcvePortId);

    /* Called from native */
    void lookup(String name, int tag, int seqno, int client) {
        Ibis.myIbis.checkLockOwned();

        ReceivePortIdentifier storedId;

        storedId = (ReceivePortIdentifier) ports.get(name);

        if (storedId != null) {
            if (ReceivePortNameServerProtocol.DEBUG) {
                System.err.println(Thread.currentThread()
                        + "Give client " + client
                        + ", tag " + tag 
                        + ", seqno " + seqno
                        + " his ReceivePort \"" + name + "\"; id " + storedId);
            }
            byte[] sf = storedId.getSerialForm();
            lookup_reply(PORT_KNOWN, tag, client, seqno, sf);
        } else {
            if (ReceivePortNameServerProtocol.DEBUG) {
                System.err.println(Thread.currentThread()
                        + "Cannot give client " + client
                        + ", tag " + tag 
                        + ", seqno " + seqno
                        + " his ReceivePort \"" + name + "\"");
            }
            lookup_reply(PORT_UNKNOWN, tag, client, seqno, null);
        }
    }

    /* Called from native */
    void unbind(String name) throws ClassNotFoundException {
        Ibis.myIbis.checkLockOwned();

        if (ReceivePortNameServerProtocol.DEBUG) {
            System.err.println(Thread.currentThread() + "" + this
                    + ": unbind receive port \"" + name + "\"");
        }
        ports.remove(name);
    }

}
