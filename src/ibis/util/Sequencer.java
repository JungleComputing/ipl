package ibis.util;

import ibis.ipl.Ibis;
import ibis.ipl.IbisException;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.StaticProperties;
import ibis.ipl.WriteMessage;
import ibis.ipl.Upcall;

import java.io.IOException;

import java.util.HashMap;
import java.util.Collection;
import java.util.Iterator;

/**
 * The <code>Sequencer</code> class provides a global numbering.
 * This can be used, for instance, for global ordering of messages.
 * A sender must then first obtain a sequence number from the sequencer,
 * and tag the message with it. The receiver must then handle the messages
 * in the "tag" order.
 */
public class Sequencer {
    /** The first sequence number that gets given out. */
    public static final int START_SEQNO = 1;

    private int seqno = START_SEQNO;
    private IbisIdentifier ident;
    private boolean master;
    private ReceivePort rcv;
    private PortType tp;
    private SendPort snd;	// Only for client
    private HashMap counters;

    private static HashMap sequencers = new HashMap();

    private static class UpcallHandler implements Upcall {

	private HashMap sendports;
	private Sequencer seq;

	UpcallHandler(Sequencer s) {
	    seq = s;
	    sendports = new HashMap();
	}

	public void upcall(ReadMessage m) throws IOException {
	    ReceivePortIdentifier rid = null;
	    String name = null;
	    try {
		rid = (ReceivePortIdentifier) m.readObject();
		name = (String) m.readObject();

	    } catch(ClassNotFoundException e) {
		throw new IOException("Got ClassNotFoundException " + e);
	    }
	
	    m.finish();
	    SendPort s = (SendPort) sendports.get(rid);
	    if (s == null) {
		s = seq.tp.createSendPort();
		s.connect(rid);
		sendports.put(rid, s);
	    }
	    WriteMessage w = s.newMessage();
	    w.writeInt(seq.getNo(name));
	    w.send();
	    w.finish();
	}

	protected void finalize() {
	    Collection v = sendports.values();
	    Iterator i = v.iterator();
	    while (i.hasNext()) {
		SendPort s = (SendPort) (i.next());
		try {
		    s.close();
		} catch(IOException e) {
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
	try {
	    IbisIdentifier boss = (IbisIdentifier)
			    ibis.registry().elect("sequencer", ident);
	    master = boss.equals(ident);
	} catch(ClassNotFoundException e) {
	    throw new IOException("Got ClassNotFoundException " + e);
	}

	StaticProperties p = new StaticProperties();
	p.add("serialization", "object");
	p.add("communication", "OneToOne, AutoUpcalls, ExplicitReceipt, Reliable");
	try {
	    tp = ibis.createPortType("sequencer", p);
	} catch(IbisException e) {
	    throw new IOException("Got IbisException " + e);
	}

	if (master) {
	    rcv = tp.createReceivePort("seq recvr", new UpcallHandler(this));
	    rcv.enableConnections();
	    rcv.enableUpcalls();
	    counters = new HashMap();
	}
	else {
	    snd = tp.createSendPort();
	    rcv = tp.createReceivePort("sequencer port on " + ibis.identifier().name());
	    rcv.enableConnections();
	    ReceivePortIdentifier master_id = ibis.registry().lookup("seq recvr");
	    snd.connect(master_id);
	}
    }

    protected void finalize() {
	if (snd != null) {
	    try {
		snd.close();
	    } catch(IOException e) {
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

    private synchronized int getNo(String name) {
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
	ReceivePortIdentifier rid = rcv.identifier();
	WriteMessage w = snd.newMessage();
	w.writeObject(rid);
	w.writeObject(name);
	w.send();
	w.finish();
	ReadMessage r = rcv.receive();
	int retval = r.readInt();
	r.finish();
	return retval;
    }

}
