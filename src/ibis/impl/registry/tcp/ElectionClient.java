/* $Id$ */

package ibis.impl.registry.tcp;

import ibis.ipl.IbisIdentifier;

import ibis.io.Conversion;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetAddress;
import java.net.Socket;

import org.apache.log4j.Logger;

class ElectionClient implements Protocol {
    static Logger logger
    = ibis.util.GetLogger.getLogger(ElectionClient.class.getName());

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
                    out.writeUTF(candidate.toString());
                    byte[] b = Conversion.object2byte(candidate);
                    out.writeInt(b.length);
                    out.write(b);
                }
                out.flush();

                in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
                int len = in.readInt();
                if(len >= 0) {
                	byte[] buf = new byte[len];
                	in.readFully(buf, 0, len);
                	result = (IbisIdentifier) Conversion.byte2object(buf);
                }
            } catch (ClassNotFoundException e) {
            	logger.warn("election client got exception: " + e, e);
                throw new IOException("Got wrong class in elect");
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
