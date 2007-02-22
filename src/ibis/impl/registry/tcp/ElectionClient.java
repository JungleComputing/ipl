/* $Id$ */

package ibis.impl.registry.tcp;

import ibis.impl.IbisIdentifier;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetAddress;
import java.net.Socket;

import org.apache.log4j.Logger;

class ElectionClient implements Protocol {
    static Logger logger = Logger.getLogger(ElectionClient.class);

    InetAddress server;

    int port;

    InetAddress localAddress = null;


    ElectionClient(InetAddress localAddress, InetAddress server, int port) {
        this.server = server;
        this.port = port;
        this.localAddress = localAddress;
    }

    IbisIdentifier elect(String election, IbisIdentifier candidate) throws IOException {

        Socket s = null;
        DataOutputStream out = null;
        DataInputStream in = null;
        IbisIdentifier result = null;

        // Election: candidate==null means caller is not a candidate.
        // Note: at least one caller must have a candidate.
        logger.info("Election " + election + ": candidate = " + candidate);
        while (result == null) {
            try {
                s = NameServerClient.nsConnect(server, port, localAddress,
                        false, 10);
                out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));

                out.writeByte(ELECTION);
                out.writeUTF(election);
                if (candidate == null) {
                    out.writeInt(0);
                } else {
                    out.writeInt(1);
                    candidate.writeTo(out);
                }
                out.flush();

                in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
                int retval = in.readInt();
                if (retval == 0) {
                    result = new IbisIdentifier(in);
                }
            } finally {
                NameServer.closeConnection(in, out, s);
            }
            if (result == null) {
                try {
                    Thread.sleep(3000);
                } catch (Exception ee) { /* ignore */
                }
            }
        }

        logger.info("Election result " + election + ": " + result);

        return result;
    }
}
