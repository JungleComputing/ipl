/* $Id$ */

package ibis.impl.nameServer.tcp;

import ibis.ipl.StaticProperties;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Iterator;
import java.util.Set;

class PortTypeNameServerClient implements Protocol {

    InetAddress server;

    int port;

    InetAddress localAddress;

    String ibisName;

    PortTypeNameServerClient(InetAddress localAddress, InetAddress server,
            int port, String name) {
        this.server = server;
        this.port = port;
        this.localAddress = localAddress;
        ibisName = name;
    }

    public boolean newPortType(String name, StaticProperties p)
            throws IOException {

        Socket s = null;
        DataOutputStream out = null;
        DataInputStream in = null;
        int result;

        try {
            s = NameServerClient.nsConnect(server, port, localAddress, false,
                    10);
            out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream(), 4096));

            out.writeByte(PORTTYPE_NEW);
            out.writeUTF(name);

            Set e = p.propertyNames();
            Iterator i = e.iterator();

            out.writeInt(p.size());

            // Send all properties.
            while (i.hasNext()) {
                String key = (String) i.next();
                out.writeUTF(key);

                String value = p.find(key);
                out.writeUTF(value);
            }

            out.flush();

            in = new DataInputStream(s.getInputStream());
            result = in.readByte();
        } finally {
            NameServer.closeConnection(in, out, s);
        }

        switch (result) {
        case PORTTYPE_ACCEPTED:
            return true;
        case PORTTYPE_REFUSED:
            return false;
        default:
            throw new StreamCorruptedException(
                    "PortTypeNameServer: got illegal opcode");
        }
    }

    public long getSeqno(String name) throws IOException {
        Socket s = null;
        DataOutputStream out = null;
        DataInputStream in = null;
        try {
            s = NameServerClient.nsConnect(server, port, localAddress,
                    false, 10);

            out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));

            out.writeByte(SEQNO);
            out.writeUTF(name);
            out.flush();

            in = new DataInputStream(new BufferedInputStream(s.getInputStream()));

            return in.readLong();

        } finally {
            NameServer.closeConnection(in, out, s);
        }
    }
}
