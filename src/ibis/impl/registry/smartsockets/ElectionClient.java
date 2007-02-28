/* $Id: ElectionClient.java 5108 2007-02-27 15:35:17Z ceriel $ */

package ibis.impl.registry.smartsockets;

import ibis.impl.IbisIdentifier;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import org.apache.log4j.Logger;

import smartsockets.virtual.*;

class ElectionClient implements Protocol {
    static Logger logger = Logger.getLogger(ElectionClient.class);

    VirtualSocketAddress server;

    NameServerClient cl;

    ElectionClient(VirtualSocketAddress server, NameServerClient cl) {
        this.server = server;
        this.cl = cl;
    }

    IbisIdentifier elect(String election, IbisIdentifier candidate)
            throws IOException {

        VirtualSocket s = null;
        DataOutputStream out = null;
        DataInputStream in = null;
        IbisIdentifier result = null;

        // Election: candidate==null means caller is not a candidate.
        // Note: at least one caller must have a candidate.
        logger.info("Election " + election + ": candidate = " + candidate);
        while (result == null) {
            try {
                s = cl.nsConnect(server, false, 10);
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
                VirtualSocketFactory.close(s, out, in);
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
