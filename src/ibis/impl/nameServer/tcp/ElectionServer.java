/* $Id$ */

package ibis.impl.nameServer.tcp;

import ibis.ipl.IbisRuntimeException;

import ibis.connect.IbisSocketFactory;

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

    static Logger logger
            = ibis.util.GetLogger.getLogger(ElectionServer.class.getName());

    private HashMap elections;
    private HashMap buffers;

    private ServerSocket serverSocket;

    private DataInputStream in;

    private DataOutputStream out;

    boolean silent;

    ElectionServer(boolean silent, IbisSocketFactory socketFactory)
            throws IOException {
        elections = new HashMap();
        buffers = new HashMap();

        this.silent = silent;

        serverSocket = socketFactory.createServerSocket(0, null, 256,
                true /* retry */, null);
        setName("NameServer ElectionServer");
        start();
    }

    int getPort() {
        return serverSocket.getLocalPort();
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
        Socket s;
        int opcode;
        boolean stop = false;

        while (!stop) {

            try {
                s = serverSocket.accept();
            } catch (Exception e) {
                throw new IbisRuntimeException("ElectionServer: got an error",
                        e);
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
            } catch (Exception e1) {
                if (! silent) {
                    logger.error("Got an exception in ElectionServer.run " + e1,
                            e1);
                }
            } finally {
                NameServer.closeConnection(in, out, s);
            }
        }
    }
}
