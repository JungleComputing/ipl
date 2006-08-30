/* $Id$ */

package ibis.impl.nio;

import ibis.io.DataInputStream;
import ibis.io.DataOutputStream;
import ibis.ipl.IbisIdentifier;

import java.io.IOException;

public final class NioIbisIdentifier extends IbisIdentifier implements
        java.io.Serializable {

    private static final long serialVersionUID = 3L;

    public NioIbisIdentifier(String name) {
        super(name);
    }

    public NioIbisIdentifier(DataInputStream in) throws IOException {
        // bogus name
        super("will override this");

        int nameLength;
        int clusterLength;
        byte[] nameBytes;
        byte[] clusterBytes;

        nameLength = in.readInt();
        nameBytes = new byte[nameLength];
        in.readArray(nameBytes, 0, nameLength);
        name = new String(nameBytes, "UTF-8");

        clusterLength = in.readInt();
        clusterBytes = new byte[clusterLength];
        in.readArray(clusterBytes, 0, clusterLength);
        this.cluster = new String(clusterBytes, "UTF-8");
    }

    /**
     * Writes out this identifier to an accumulator
     */
    public void writeTo(DataOutputStream out) throws IOException {
        byte[] nameBytes;
        byte[] clusterBytes;

        nameBytes = name.getBytes("UTF-8");
        out.writeInt(nameBytes.length);
        out.writeArray(nameBytes, 0, nameBytes.length);

        clusterBytes = cluster.getBytes("UTF-8");
        out.writeInt(clusterBytes.length);
        out.writeArray(clusterBytes, 0, clusterBytes.length);
    }
}
