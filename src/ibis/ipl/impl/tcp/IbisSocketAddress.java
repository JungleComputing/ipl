package ibis.ipl.impl.tcp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.SocketAddress;

class IbisSocketAddress {
    SocketAddress address;

    IbisSocketAddress(SocketAddress address) {
        this.address = address;
    }

    IbisSocketAddress(byte[] buf) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(buf);
        ObjectInputStream is = new ObjectInputStream(in);
        try {
            address = (SocketAddress) is.readObject();
        } catch(ClassNotFoundException e) {
            throw new IOException("Could not read address" + e);
        }
        is.close();
    }

    byte[] toBytes() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(address);
        os.close();
        return out.toByteArray();
    }

    public String toString() {
        return address.toString();
    }
}
