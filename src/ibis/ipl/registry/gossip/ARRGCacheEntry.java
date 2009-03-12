/**
 * 
 */
package ibis.ipl.registry.gossip;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import ibis.smartsockets.virtual.VirtualSocketAddress;

class ARRGCacheEntry {
    private final VirtualSocketAddress address;

    private final boolean arrgOnly;

    ARRGCacheEntry(VirtualSocketAddress address, boolean arrgOnly) {
        this.address = address;
        this.arrgOnly = arrgOnly;
    }

    ARRGCacheEntry(DataInputStream in) throws IOException {
        try {
            address = new VirtualSocketAddress(in);
            arrgOnly = in.readBoolean();
        } catch (IOException e) {
            throw e;
        } catch (Throwable e) {
            IOException exception = new IOException("could not read entry"); 
            exception.initCause(e);
            throw exception;
        }
    }

    void writeTo(DataOutputStream out) throws IOException {
        address.write(out);
        out.writeBoolean(arrgOnly);
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

    public boolean sameAddressAs(ARRGCacheEntry entry) {
        return address.equals(entry.address);
    }

    public String toString() {
        return "address: " + address + ", arrg only: " + arrgOnly;
    }

}