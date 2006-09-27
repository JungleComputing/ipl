/* $Id$ */

package ibis.impl.net;

import ibis.impl.util.IbisIdentifierTable;
import ibis.ipl.IbisError;
import ibis.ipl.IbisIdentifier;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;

public final class NetIbisIdentifier extends IbisIdentifier implements
        java.io.Serializable {
    private static final boolean ID_CACHE = false;

    private static final long serialVersionUID = 9L;

    private InetAddress address;

    // ID_CACHE
    private static IbisIdentifierTable cache = new IbisIdentifierTable();

    private static HashMap inetAddrMap = new HashMap();

    private transient String toStringCache = null;

    public NetIbisIdentifier(String name, InetAddress address) {
        super(name);
        this.address = address;
    }

    public InetAddress address() {
        return address;
    }

    public String toString() {
        if (toStringCache == null) {
            String a = (address == null ? "<null>" : address.getHostName() + ", "
                    + address.getHostAddress());
            String n = (name == null ? "<null>" : name);
            toStringCache = "(NetId: " + n + " on [" + a + "])";
        }
        return toStringCache;
    }

    // no need to serialize super class fields, this is done automatically
    // We handle the address field special.
    // Do not do a writeObject on it (or a defaultWriteObject of the current
    // object), because InetAddress might not be rewritten as it is in the
    // classlibs --Rob
    private void writeObject(java.io.ObjectOutputStream out)
            throws IOException {
        int handle = -1;
        
        if (ID_CACHE) {
            handle = cache.getHandle(out, this);
        }
        out.writeInt(handle);
        if (handle < 0) { // First time, send it.
            out.writeUTF(address.getHostAddress());
        }
    }

    // no need to serialize super class fields, this is done automatically
    private void readObject(java.io.ObjectInputStream in) throws IOException,
            ClassNotFoundException {
        int handle = in.readInt();
        if (handle < 0) {
            String addr = in.readUTF();
            address = (InetAddress) inetAddrMap.get(addr);
            if (address == null) {
                try {
                    // this does not do a real lookup
                    address = InetAddress.getByName(addr);
                } catch (Exception e) {
                    throw new IbisError("EEK, could not create an inet address"
                            + "from a IP address. This shouldn't happen", e);
                }
                inetAddrMap.put(addr, address);
            }

            if (ID_CACHE) {
                cache.addIbis(in, -handle, this);
            }
        } else {
            if (! ID_CACHE) {
                throw new IbisError("This ibis cannot talk to ibisses or nameservers that do IbisIdentifier caching");
            }
            NetIbisIdentifier ident = (NetIbisIdentifier) cache.getIbis(in,
                    handle);
            address = ident.address;
            name = ident.name;
            cluster = ident.cluster;
        }
    }

    public void free() {
        if (ID_CACHE) {
            cache.removeIbis(this);
        }
    }
}
