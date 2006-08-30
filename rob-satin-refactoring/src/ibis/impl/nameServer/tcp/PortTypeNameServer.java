/* $Id$ */

package ibis.impl.nameServer.tcp;

import ibis.ipl.IbisRuntimeException;
import ibis.ipl.StaticProperties;

import ibis.connect.IbisSocketFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Hashtable;

class PortTypeNameServer extends Thread implements Protocol {

    /**
     * The <code>Sequencer</code> class provides a global numbering.
     * This can be used, for instance, for global ordering of messages.
     * A sender must then first obtain a sequence number from the sequencer,
     * and tag the message with it. The receiver must then handle the messages
     * in the "tag" order.
     * <p>
     * A Sequencer associates a numbering scheme with a name, so the user can
     * associate different sequences with different names.
     */
    private static class Sequencer {
        private HashMap counters;

        private static class LongObject {
            long val;

            LongObject(long v) {
                val = v;
            }

            public String toString() {
                return "" + val;
            }
        }

        Sequencer() {
            counters = new HashMap();
        }

        /**
         * Returns the next sequence number associated with the specified name.
         * @param name the name of the sequence.
         * @return the next sequence number
         */
        public synchronized long getSeqno(String name) {
            LongObject i = (LongObject) counters.get(name);
            if (i == null) {
                i = new LongObject(ibis.ipl.ReadMessage.INITIAL_SEQNO);
                counters.put(name, i);
            }
            return i.val++;
        }

        public String toString() {
            return "" + counters;
        }
    }

    private Hashtable portTypes;

    private ServerSocket serverSocket;

    private DataInputStream in;

    private DataOutputStream out;

    private Sequencer seq;

    private boolean silent;

    PortTypeNameServer(boolean silent, IbisSocketFactory socketFactory)
            throws IOException {
        portTypes = new Hashtable();

        this.silent = silent;

        serverSocket = socketFactory.createServerSocket(0, null, true, null);
        setName("PortType Name Server");
        seq = new Sequencer();
        start();
    }

    int getPort() {
        return serverSocket.getLocalPort();
    }

    private void handlePortTypeNew() throws IOException {

        StaticProperties p = new StaticProperties();

        String name = in.readUTF();
        int size = in.readInt();

        for (int i = 0; i < size; i++) {
            String key = in.readUTF();
            String value = in.readUTF();

            p.add(key, value);
        }

        StaticProperties temp = (StaticProperties) portTypes.get(name);

        if (temp == null) {
            portTypes.put(name, p);
            out.writeByte(PORTTYPE_ACCEPTED);
        } else {
            if (temp.equals(p)) {
                out.writeByte(PORTTYPE_ACCEPTED);
            } else {
                out.writeByte(PORTTYPE_REFUSED);
            }
        }
    }

    private void handleSeqno() throws IOException {
        String name = in.readUTF();

        long l = seq.getSeqno(name);
        out.writeLong(l);
        out.flush();
    }

    public void run() {

        Socket s;
        int opcode;

        while (true) {

            try {
                s = serverSocket.accept();
            } catch (Exception e) {
                throw new IbisRuntimeException(
                        "PortTypeNameServer: got an error ", e);
            }

            out = null;
            in = null;

            try {
                in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
                out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));

                opcode = in.readByte();

                switch (opcode) {
                case (PORTTYPE_NEW):
                    handlePortTypeNew();
                    break;
                case (PORTTYPE_EXIT):
                    serverSocket.close();
                    return;
                case (SEQNO):
                    handleSeqno();
                    break;
                default:
                    if (! silent) {
                        System.err.println("PortTypeNameServer: got an illegal "
                                + "opcode " + opcode);
                    }
                }
            } catch (Exception e1) {
                if (! silent) {
                    System.err.println("Got an exception in PortTypeNameServer.run "
                                    + e1);
                    e1.printStackTrace();
                }
            } finally {
                NameServer.closeConnection(in, out, s);
            }
        }
    }

    public String toString() {
        return "portypeserver(porttypes=" + portTypes + ",seq=" + seq + ")";
    }
}
