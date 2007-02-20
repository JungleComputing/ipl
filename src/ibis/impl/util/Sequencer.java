/* $Id$ */

package ibis.impl.util;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.CapabilitySet;
import ibis.ipl.WriteMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * The <code>Sequencer</code> class provides a global numbering.
 * This can be used, for instance, for global ordering of messages.
 * A sender must then first obtain a sequence number from the sequencer,
 * and tag the message with it. The receiver must then handle the messages
 * in the "tag" order.
 * <p>
 * A Sequencer associates a numbering scheme with a name, so the user can
 * associate different sequences with different names.
 * <p>
 * A Sequencer is obtained by means of the {@link #getSequencer} method.
 * A sequence number is obtained from a sequencer <code>seq</code> by
 * the {@link #getSeqno seq.getSeqno(name)} call.
 */
public final class Sequencer implements ibis.ipl.PredefinedCapabilities {
    /** The first sequence number that gets given out. */
    public static final int START_SEQNO = 1;

    private IbisIdentifier ident;

    private boolean master;

    private ReceivePort rcv;

    PortType tp;

    private SendPort snd; // Only for client

    private int idno;

    private HashMap counters;

    private static HashMap sequencers = new HashMap();

    private static class ServerThread extends Thread {

        private ArrayList sendports;

        private Sequencer seq;

        private ArrayList clients;

        private ReceivePort rcv;

        ServerThread(ReceivePort r, Sequencer s) {
            seq = s;
            rcv = r;
            sendports = new ArrayList();
            clients = new ArrayList();
        }

        public void handleMessage(ReadMessage m) throws IOException {
            ReceivePortIdentifier rid = null;
            String name = null;
            SendPort s;
            WriteMessage w = null;
            int index = m.readInt();

            if (index == -1) {
                try {
                    rid = (ReceivePortIdentifier) m.readObject();
                } catch (ClassNotFoundException e) {
                    System.err.println("Got ClassNotFoundException!");
                    e.printStackTrace();
                }
                m.finish();
                clients.add(rid);
                s = seq.tp.createSendPort();
                s.connect(rid);
                sendports.add(s);
                w = s.newMessage();
                w.writeInt(clients.size() - 1);
            } else {
                rid = (ReceivePortIdentifier) clients.get(index);
                name = m.readString();
                m.finish();
                s = (SendPort) sendports.get(index);
                w = s.newMessage();
                w.writeInt(seq.getNo(name));
            }
            w.finish();
        }

        public void run() {

            while (true) {
                try {
                    ReadMessage m = rcv.receive();
                    handleMessage(m);
                } catch (IOException e) {
                    System.err.println("Got IOException!");
                    e.printStackTrace();
                    break;
                }
            }
        }

        protected void finalize() {
            for (int i = 0; i < sendports.size(); i++) {
                SendPort s = (SendPort) sendports.get(i);
                try {
                    s.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    private static class IntObject {
        int val;

        IntObject(int v) {
            val = v;
        }
    }

    private Sequencer(Ibis ibis) throws IOException {
        ident = ibis.identifier();
        idno = -1;
        CapabilitySet p = new CapabilitySet(
            SER_OBJECT, CONN_MANYTOONE, COMM_RELIABLE, RECV_EXPLICIT);
        try {
            tp = ibis.createPortType(p);
        } catch (Exception e) {
            throw new IOException("Got Exception " + e);
        }

        rcv = tp.createReceivePort("seq recvr");
        rcv.enableConnections();

        IbisIdentifier boss;
        boss = ibis.registry().elect("sequencer");
        master = boss.equals(ident);

        if (master) {
            counters = new HashMap();
            ServerThread server = new ServerThread(rcv, this);
            server.setDaemon(true);
            server.start();
        } else {
            snd = tp.createSendPort();
            snd.connect(boss, "seq recvr");
            ReceivePortIdentifier rid = rcv.identifier();
            WriteMessage w = snd.newMessage();
            w.writeInt(-1);
            w.writeObject(rid);
            w.finish();
            ReadMessage r = rcv.receive();
            idno = r.readInt();
            r.finish();
        }
    }

    protected void finalize() {
        if (snd != null) {
            try {
                snd.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    /**
     * Returns a sequencer for the specified Ibis instance.
     * This sequencer can then be used to obtain sequence numbers from.
     * @param ibis the Ibis instance for which a sequencer must be obtained
     * @return the sequencer
     * @exception IOException gets thrown in case of trouble
     */
    public static synchronized Sequencer getSequencer(Ibis ibis)
            throws IOException {
        IbisIdentifier ident = ibis.identifier();
        Sequencer seq = (Sequencer) sequencers.get(ident);
        if (seq != null) {
            return seq;
        }
        seq = new Sequencer(ibis);
        sequencers.put(ident, seq);
        return seq;
    }

    synchronized int getNo(String name) {
        IntObject i = (IntObject) counters.get(name);
        if (i == null) {
            i = new IntObject(START_SEQNO);
            counters.put(name, i);
        }
        return i.val++;
    }

    /**
     * Returns the next sequence number associated with the specified name.
     * @param name the name of the sequence.
     * @return the next sequence number
     * @exception IOException gets thrown in case of trouble
     */
    public int getSeqno(String name) throws IOException {
        if (master) {
            return getNo(name);
        }
        WriteMessage w = snd.newMessage();
        w.writeInt(idno);
        w.writeString(name);
        w.finish();
        ReadMessage r = rcv.receive();
        int retval = r.readInt();
        r.finish();
        return retval;
    }

}
