package ibis.ipl.impl.tcp;

import ibis.smartsockets.virtual.VirtualSocketAddress;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.SocketAddress;

/**
 * Either a SocketAddress or a VirtualSocketAddress.
 */
class IbisSocketAddress {
    SocketAddress address;
    VirtualSocketAddress virtualAddress;

    IbisSocketAddress(SocketAddress address) {
        this.address = address;
        virtualAddress = null;
    }

    IbisSocketAddress(VirtualSocketAddress virtualAddress) {
        address = null;
        this.virtualAddress = virtualAddress;
    }

    IbisSocketAddress(byte[] buf) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(buf);
        int b = in.read();
        if (b == 1) {
            virtualAddress = null;
            ObjectInputStream is = new ObjectInputStream(in);
            try {
                address = (SocketAddress) is.readObject();
            } catch(ClassNotFoundException e) {
                throw new IOException("Could not read address" + e);
            }
            is.close();
        } else {
            address = null;
            DataInputStream id = new DataInputStream(in);
            virtualAddress = new VirtualSocketAddress(id);
            id.close();
        }
    }

    byte[] toBytes() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (address != null) {
            out.write(1);
            ObjectOutputStream os = new ObjectOutputStream(out);
            os.writeObject(address);
            os.close();
        } else {
            out.write(0);
            DataOutputStream od = new DataOutputStream(out);
            virtualAddress.write(od);
            od.close();
        }
        return out.toByteArray();
    }

    public String toString() {
        if (address != null) {
            return address.toString();
        }
        return virtualAddress.toString();
    }
}
