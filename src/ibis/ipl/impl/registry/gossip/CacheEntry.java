/**
 * 
 */
package ibis.ipl.impl.registry.gossip;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import ibis.smartsockets.virtual.VirtualSocketAddress;

class CacheEntry {
    private final VirtualSocketAddress address;

    private final boolean arrgOnly;

    CacheEntry(VirtualSocketAddress address, boolean arrgOnly) {
        this.address = address;
        this.arrgOnly = arrgOnly;
    }

    CacheEntry(DataInputStream in) throws IOException {
        address = new VirtualSocketAddress(in);
        arrgOnly = in.readBoolean();
    }

    void writeTo(DataOutputStream out) throws IOException {
        address.write(out);
        out.writeBoolean(arrgOnly);
    }

    public String toString() {
        return "address: " + address + ", arrg only: " + arrgOnly;

    }

    /**
     * @return the address
     */
    public VirtualSocketAddress getAddress() {
        return address;
    }

    /**
     * @return true if this peer only runs the ARRG algorithm, not the registry
     *         service
     */
    public boolean isArrgOnly() {
        return arrgOnly;
    }

    public boolean sameAddressAs(CacheEntry entry) {
        return address.equals(entry.address);
    }

}