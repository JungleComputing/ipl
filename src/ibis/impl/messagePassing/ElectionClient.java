package ibis.ipl.impl.messagePassing;

import java.util.Enumeration;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;

class ElectionClient implements ElectionProtocol {

    ibis.ipl.ReceivePortIdentifier server;
    ibis.ipl.SendPort sport;
    ibis.ipl.ReceivePort rport;

    ElectionClient() throws IbisException, IbisIOException {
// System.err.println(Thread.currentThread() + "ElectionClient: enter...");
	ibis.ipl.PortType type = ibis.ipl.impl.messagePassing.Ibis.myIbis.createPortType("++++ElectionPort++++", new ibis.ipl.StaticProperties());
// System.err.println(Thread.currentThread() + "ElectionClient: portTypes lives");

	rport = type.createReceivePort("++++ElectionClient-" +
				       ibis.ipl.impl.messagePassing.Ibis.myIbis.myCpu + "++++");
	rport.enableConnections();
// System.err.println(Thread.currentThread() + "ElectionClient: receivePort lives");

	server = ibis.ipl.impl.messagePassing.Ibis.myIbis.registry().lookup("++++ElectionServer-" +
						ibis.ipl.impl.messagePassing.Ibis.myIbis.myCpu + "++++");
// System.err.println(Thread.currentThread() + "ElectionClient: server located");
	sport = type.createSendPort();
// System.err.println(Thread.currentThread() + "ElectionClient: sendPort lives");
	sport.connect(server);
// System.err.println(Thread.currentThread() + "ElectionClient: sendPort connected!");
    }

    void end() {
	try {
	    sport.free();
	} catch (IbisIOException e) {
	    // Ignore
	}
	if (ElectionServer.DEBUG) {
	    System.err.println("ElectionClient frees receive port " + rport);
	}
	rport.free();
	if (ElectionServer.DEBUG) {
	    System.err.println("ElectionClient has freed receive port " + rport);
	}
    }

    Object elect(String election, Object candidate) throws IbisIOException {

// System.err.println(Thread.currentThread() + "ElectionClient: elect(): start send");

	ibis.ipl.WriteMessage m = sport.newMessage();
	m.writeInt(ibis.ipl.impl.messagePassing.Ibis.myIbis.myCpu);
	m.writeObject(election);
	m.writeObject(candidate);
	m.send();
	m.finish();

	if (ElectionServer.DEBUG) {
	    System.err.println(Thread.currentThread() + "ElectionClient: elect(): send done, now start rcve");
	}

	ibis.ipl.ReadMessage r = rport.receive();
	Object winner = null;
	try {
	    winner = r.readObject();
	} catch (ClassNotFoundException e) {
	    throw new IbisIOException(e);
	}
	r.finish();
	if (ElectionServer.DEBUG) {
	    System.err.println(Thread.currentThread() + "ElectionClient: elect() finished, winner " + winner + " my stake " + candidate);
	}

	return winner;
    }
}
