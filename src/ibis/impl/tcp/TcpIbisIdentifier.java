/* $Id$ */

package ibis.impl.tcp;

import ibis.connect.direct.SocketAddressSet;
import ibis.impl.util.IbisIdentifierTable;
import ibis.ipl.IbisError;
import ibis.ipl.IbisIdentifier;

import java.io.IOException;
import java.util.HashMap;

public final class TcpIbisIdentifier extends IbisIdentifier implements
        java.io.Serializable {

    private static final long serialVersionUID = 3L;

    private SocketAddressSet address;

    private static IbisIdentifierTable cache = new IbisIdentifierTable();

    private static HashMap inetAddrMap = new HashMap();

    private transient String toStringCache = null;

    public TcpIbisIdentifier(String name, SocketAddressSet address) {
        super(name);
        this.address = address;
    }

    public SocketAddressSet address() {
        return address;
    }

    public String toString() {
        if (toStringCache == null) {
            String a = (address == null ? "<null>" : address.toString());
            String n = (name == null ? "<null>" : name);
            if (n.length() > 8) {
                n = n.substring(0,8) + "...";
            }
            toStringCache = "(TcpId: " + n + " on [" + a + "])";
        }
        return toStringCache;
    }

    // no need to serialize super class fields, this is done automatically
    // We handle the address field special.
    // Do not do a writeObject on it (or a defaultWriteObject of the current
    // object), because InetAddress might not be rewritten as it is in the
    // classlibs --Rob
    // Is this still a problem? I don't think so --Ceriel
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        int handle = -1;
        if (Config.ID_CACHE) {
            handle = cache.getHandle(out, this);
        }
        out.writeInt(handle);
        if (handle < 0) { // First time, send it.
            out.writeUTF(address.toString());
        }
    }

    // no need to serialize super class fields, this is done automatically
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        int handle = in.readInt();
        if (handle < 0) {
            String addr = in.readUTF();
            address = (SocketAddressSet) inetAddrMap.get(addr);
            if (address == null) {
                try {
                    address = new SocketAddressSet(addr);
                } catch(Exception e) {
                    throw new IbisError("EEK, could not create an inet address"
                            + "from a IP address. This shouldn't happen", e);
                }
                inetAddrMap.put(addr, address);
            }
            if (Config.ID_CACHE) {
                cache.addIbis(in, -handle, this);
            }
        } else {
            if (! Config.ID_CACHE) {
                throw new IbisError("This ibis cannot talk to ibisses or nameservers that do IbisIdentifier caching");
            }
            TcpIbisIdentifier ident = (TcpIbisIdentifier) cache.getIbis(in,
                    handle);
            address = ident.address;
            name = ident.name;
            cluster = ident.cluster;
        }
    }

    public void free() {
        if (Config.ID_CACHE) {
            cache.removeIbis(this);
        }
    }
}
