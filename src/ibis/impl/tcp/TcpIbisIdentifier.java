/* $Id$ */

package ibis.impl.tcp;

import ibis.ipl.IbisIdentifier;

import java.io.IOException;
import java.util.HashMap;
import java.net.InetAddress;

public final class TcpIbisIdentifier extends IbisIdentifier implements
        java.io.Serializable {

    private static final long serialVersionUID = 3L;

    private InetAddress address;

    // Added for implementation of connect(IbisIdentifier, String).
    // (Ceriel)
    int port;

    private static HashMap inetAddrMap = new HashMap();

    private transient String toStringCache = null;

    public TcpIbisIdentifier(String name, InetAddress address) {
        super(name);
        this.address = address;
    }

    InetAddress address() {
        return address;
    }

    public String toString() {
        if (toStringCache == null) {
            String a = address == null ? "<null>" : address.getHostName();
            String n = (name == null ? "<null>" : name);
/* This is annoying, how can we debug if you can't see which ibis said what? --Rob
            if (n.length() > 8) {
                n = n.substring(0,8) + "...";
            }
*/
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
        out.writeUTF(address.getHostAddress());
        out.writeInt(port);
    }

    // no need to serialize super class fields, this is done automatically
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        String addr = in.readUTF();
        port = in.readInt();
        address = (InetAddress) inetAddrMap.get(addr);
        if (address == null) {
            try {
                address = InetAddress.getByName(addr);
            } catch(Exception e) {
                throw new Error("EEK, could not create an inet address"
                        + "from a IP address. This shouldn't happen", e);
            }
            inetAddrMap.put(addr, address);
        }
    }

    public void free() {
    }
}
