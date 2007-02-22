/* $Id$ */

package ibis.impl.registry.tcp;

import ibis.impl.IbisIdentifier;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;

class ElectionServer extends Thread implements Protocol {

    static Logger logger = Logger.getLogger(ElectionServer.class);

    private HashMap<String, IbisIdentifier> elections;

    private ServerSocket serverSocket;

    private DataInputStream in;

    private DataOutputStream out;

    boolean silent;

    ElectionServer(boolean silent) throws IOException {
        elections = new HashMap<String, IbisIdentifier>();

        this.silent = silent;

        serverSocket = new ServerSocket();
        serverSocket.bind(null, 256);
        setName("NameServer ElectionServer");
        start();
    }

    int getPort() {
        return serverSocket.getLocalPort();
    }

    private void handleElection() throws IOException {
        String election = in.readUTF();
        IbisIdentifier id = null;
        if (in.readInt() != 0) {
            id = new IbisIdentifier(in);
        }

        IbisIdentifier temp = elections.get(election);

        if (temp == null) {
            if (id != null) {
                elections.put(election, id);
            } else {
                out.writeInt(-1);
                out.flush();
                return;
            }
        } else {
            id = temp;
        }
        out.writeInt(0);
        id.writeTo(out);
        out.flush();
    }

    private void handleKill() throws IOException {

        int ns = in.readInt();
        IbisIdentifier ids[] = new IbisIdentifier[ns];
        for (int i = 0; i < ns; i++) {
            ids[i] = new IbisIdentifier(in);
        }

        for (Iterator key = elections.keySet().iterator(); key.hasNext();) {
            String election = (String) key.next();
            IbisIdentifier o = elections.get(election);
            for (int i = 0; i < ids.length; i++) {
                if (o.equals(ids[i])) {
                    // result of election is dead. Make new election
                    // possible.
                    key.remove();
                    break;
                }
            }
        }
    }

    public void run() {
        Socket s;
        int opcode;
        boolean stop = false;

        while (!stop) {

            try {
                s = serverSocket.accept();
            } catch (Throwable e) {
            	logger.error("Got an exception in ElectionServer accept: " + e, e);
            	
            	try {
            		Thread.sleep(1000);
            	} catch (Exception x) {
            		// ignore
            	}
            	continue;
            }

            try {
                in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
                out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));

                opcode = in.readByte();

                switch (opcode) {
                case (ELECTION):
                    handleElection();
                    break;
                case (ELECTION_KILL):
                    handleKill();
                    break;
                case (ELECTION_EXIT):
                    serverSocket.close();
                    return;
                default:
                    if (! silent) {
                        logger.error("ElectionServer: got an illegal opcode "
                                + opcode);
                    }
                }
            } catch (Throwable e) {
                if (! silent) {
                    logger.error("Got an exception in ElectionServer.run " + e,
                            e);
                }
            } finally {
                NameServer.closeConnection(in, out, s);
            }
        }
    }
}
