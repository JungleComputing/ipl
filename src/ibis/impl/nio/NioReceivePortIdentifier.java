/* $Id$ */

package ibis.impl.nio;

import ibis.io.Accumulator;
import ibis.io.Dissipator;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReceivePortIdentifier;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public final class NioReceivePortIdentifier implements ReceivePortIdentifier,
        java.io.Serializable {
    private static final long serialVersionUID = 4L;

    String name;

    String type;

    NioIbisIdentifier ibis;

    InetSocketAddress address;

    NioReceivePortIdentifier(String name, String type, NioIbisIdentifier ibis,
            InetSocketAddress address) {
        this.name = name;
        this.type = type;
        this.ibis = ibis;
        this.address = address;
    }

    NioReceivePortIdentifier(Dissipator in) throws IOException {
        int nameLength;
        int typeLength;
        int addressLength;
        byte[] nameBytes;
        byte[] typeBytes;
        byte[] addressBytes;
        InetAddress inetAddress;
        int port;

        nameLength = in.readInt();
        nameBytes = new byte[nameLength];
        in.readArray(nameBytes, 0, nameLength);
        name = new String(nameBytes, "UTF-8");

        typeLength = in.readInt();
        typeBytes = new byte[typeLength];
        in.readArray(typeBytes, 0, typeLength);
        type = new String(typeBytes, "UTF-8");

        addressLength = in.readInt();
        addressBytes = new byte[addressLength];
        in.readArray(addressBytes, 0, addressLength);
        inetAddress = InetAddress.getByAddress(addressBytes);

        port = in.readInt();

        address = new InetSocketAddress(inetAddress, port);

        ibis = new NioIbisIdentifier(in);
    }

    public boolean equals(NioReceivePortIdentifier other) {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }
        return (type().equals(other.type()) && ibis.equals(other.ibis)
                && name().equals(other.name()) && address.equals(other.address));
    }

    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other instanceof NioReceivePortIdentifier) {
            return equals((NioReceivePortIdentifier) other);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return name().hashCode() + address.hashCode();
    }

    public String name() {
        return name;
    }

    public String type() {
        if (type != null) {
            return type;
        }
        return "__notype__";
    }

    public IbisIdentifier ibis() {
        return ibis;
    }

    public String toString() {
        return name + "@" + address.toString();
    }

    /**
     * Serializes this object. No not trust default writeObject because
     * InetAddress is in the classlibs, and may not be rewritten
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        byte[] rawIP = address.getAddress().getAddress();
        int port = address.getPort();

        out.writeUTF(name);
        out.writeUTF(type);
        out.writeObject(ibis);
        out.writeByte(rawIP.length);
        out.write(rawIP);
        out.writeInt(port);

    }

    /**
     * De-serializes this object. No not trust default readObject because
     * InetAddress is in the classlibs, and may not be rewritten
     */
    private void readObject(java.io.ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        byte[] rawIP;
        int ipLength; // size of an ip address (IPv4 = 4, IPv6 = 16)
        int port;
        InetAddress inetAddress;

        name = in.readUTF();
        type = in.readUTF();
        ibis = (NioIbisIdentifier) in.readObject();

        ipLength = in.readByte();
        rawIP = new byte[ipLength];
        in.readFully(rawIP);
        inetAddress = InetAddress.getByAddress(rawIP);

        port = in.readInt();

        address = new InetSocketAddress(inetAddress, port);

    }

    public void writeTo(Accumulator out) throws IOException {
        byte[] nameBytes;
        byte[] typeBytes;
        byte[] addressBytes;

        nameBytes = name.getBytes("UTF-8");
        out.writeInt(nameBytes.length);
        out.writeArray(nameBytes, 0, nameBytes.length);

        typeBytes = type.getBytes("UTF-8");
        out.writeInt(typeBytes.length);
        out.writeArray(typeBytes, 0, typeBytes.length);

        addressBytes = address.getAddress().getAddress();
        out.writeInt(addressBytes.length);
        out.writeArray(addressBytes, 0, addressBytes.length);

        out.writeInt(address.getPort());

        ibis.writeTo(out);
    }
}
