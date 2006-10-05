/* $Id$ */

package ibis.impl.nameServer.tcp;


import ibis.io.Conversion;
import ibis.ipl.IbisIdentifier;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import smartsockets.virtual.*;

class ElectionClient implements Protocol {

    VirtualSocketAddress server;

    String ibisName;

    ElectionClient(VirtualSocketAddress server, String name) {
        this.server = server;
        ibisName = name;
    }

    IbisIdentifier elect(String election, IbisIdentifier candidate) throws IOException,
            ClassNotFoundException {

        VirtualSocket s = null;
        DataOutputStream out = null;
        DataInputStream in = null;
        IbisIdentifier result = null;

        // Election: candidate==null means caller is not a candidate.
        // Note: at least one caller must have a candidate.
        while (result == null) {
            try {
                s = NameServerClient.nsConnect(server, false, 10);
                out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));

                out.writeByte(ELECTION);
                out.writeUTF(election);
                if (candidate == null) {
                    out.writeInt(0);
                } else {
                    out.writeInt(1);
                    out.writeUTF(candidate.name());
                    byte[] b = Conversion.object2byte(candidate);
                    out.writeInt(b.length);
                    out.write(b);
                }
                out.flush();

                in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
                int len = in.readInt();
                byte[] buf = new byte[len];
                in.readFully(buf, 0, len);
                result = (IbisIdentifier) Conversion.byte2object(buf);
            } finally {
                NameServer.closeConnection(in, out, s);
            }
            if (result == null) {
                try {
                    Thread.sleep(1000);
                } catch (Exception ee) { /* ignore */
                }
            }
        }

        return result;
    }
}
