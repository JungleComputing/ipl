/* $Id$ */

package ibis.impl.nameServer.tcp;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;

import smartsockets.virtual.VirtualServerSocket;
import smartsockets.virtual.VirtualSocket;
import smartsockets.virtual.VirtualSocketAddress;
import smartsockets.virtual.VirtualSocketFactory;

class ElectionServer extends Thread implements Protocol {

    static Logger logger
            = ibis.util.GetLogger.getLogger(ElectionServer.class.getName());

    private HashMap elections;
    private HashMap buffers;

    private VirtualServerSocket serverSocket;

    private DataInputStream in;

    private DataOutputStream out;

    boolean silent;

    ElectionServer(boolean silent, VirtualSocketFactory socketFactory)
            throws IOException {
        elections = new HashMap();
        buffers = new HashMap();

        this.silent = silent;

        serverSocket = socketFactory.createServerSocket(0, 256, true, null);
        setName("NameServer ElectionServer");
        start();
    }

    VirtualSocketAddress getAddress() {
        return serverSocket.getLocalSocketAddress();
    }

    private void handleElection() throws IOException {
        String election = in.readUTF();
        int candidate = in.readInt();
        String name = null;
        byte[] buf = null;
        if (candidate != 0) {
            name = in.readUTF();
            int len = in.readInt();
            buf = new byte[len];
            in.readFully(buf, 0, len);
        }

        Object temp = elections.get(election);

        if (temp == null) {
            if (name != null) {
                elections.put(election, name);
                buffers.put(election, buf);
            } else {
                out.writeInt(-1);
                out.flush();
                return;
            }
        } else {
            buf = (byte[])  buffers.get(election);
        }
        out.writeInt(buf.length);
        out.write(buf);
        out.flush();
    }

    private void handleKill() throws IOException {

        int ns = in.readInt();
        String ids[] = new String[ns];
        for (int i = 0; i < ns; i++) {
            ids[i] = in.readUTF();
        }

        for (Iterator key = elections.keySet().iterator(); key.hasNext();) {
            String election = (String) key.next();
            Object o = elections.get(election);
            for (int i = 0; i < ids.length; i++) {
                if (o.equals(ids[i])) {
                    // result of election is dead. Make new election
                    // possible.
                    key.remove();
                    buffers.remove(election);
                    break;
                }
            }
        }
    }

    public void run() {
        VirtualSocket s;
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
                VirtualSocketFactory.close(s, out, in);
            }
        }
    }
}
