/* $Id$ */

package ibis.impl.nameServer.tcp;

import ibis.ipl.IbisIdentifier;

import ibis.io.DummyInputStream;
import ibis.io.DummyOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

class ElectionClient implements Protocol {

    InetAddress server;

    int port;

    InetAddress localAddress = null;

    ElectionClient(InetAddress localAddress, InetAddress server, int port) {
        this.server = server;
        this.port = port;
        this.localAddress = localAddress;
    }

    IbisIdentifier elect(String election, IbisIdentifier candidate) throws IOException,
            ClassNotFoundException {

        Socket s = null;
        ObjectOutputStream out;
        ObjectInputStream in;
        IbisIdentifier result = null;

        // Election: candidate==null means caller is not a candidate.
        // Note: at least one caller must have a candidate.
        while (result == null) {
            s = NameServerClient.nsConnect(server, port, localAddress, false,
                    10);
            DummyOutputStream dos = new DummyOutputStream(s.getOutputStream());
            out = new ObjectOutputStream(new BufferedOutputStream(dos));

            out.writeByte(ELECTION);
            out.writeUTF(election);
            if (candidate == null) {
                out.writeObject(null);
            } else {
                out.writeObject(candidate.name());
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(candidate);
                oos.close();
                byte buf[] = baos.toByteArray();
    
                out.writeObject(buf);
            }
            out.flush();

            DummyInputStream di = new DummyInputStream(s.getInputStream());
            in = new ObjectInputStream(new BufferedInputStream(di));

            byte[] buf = (byte[]) in.readObject();

	    ByteArrayInputStream bais = new ByteArrayInputStream(buf);
	    ObjectInputStream ois = new ObjectInputStream(bais);
	    result = (IbisIdentifier) ois.readObject();
	    ois.close();
            NameServer.closeConnection(in, out, s);
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
