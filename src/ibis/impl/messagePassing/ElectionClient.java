package ibis.impl.messagePassing;

import java.util.Enumeration;

import java.io.IOException;

import ibis.ipl.IbisException;

class ElectionClient implements ElectionProtocol {

    ibis.ipl.ReceivePortIdentifier server;
    ibis.ipl.SendPort sport;
    ibis.ipl.ReceivePort rport;

    ElectionClient() throws IbisException, IOException {
	if (! ElectionProtocol.NEED_ELECTION) {
	    return;
	}

// System.err.println(Thread.currentThread() + "ElectionClient: enter...");
	ibis.ipl.PortType type = Ibis.myIbis.createPortType("++++ElectionPort++++", new ibis.ipl.StaticProperties());
// System.err.println(Thread.currentThread() + "ElectionClient: portTypes lives");

	rport = type.createReceivePort("++++ElectionClient-" +
				       Ibis.myIbis.myCpu + "++++");
	rport.enableConnections();
// System.err.println(Thread.currentThread() + "ElectionClient: receivePort lives");

	server = Ibis.myIbis.registry().lookup("++++ElectionServer-" +
						Ibis.myIbis.myCpu + "++++");
// System.err.println(Thread.currentThread() + "ElectionClient: server located");
	sport = type.createSendPort("election_client");
// System.err.println(Thread.currentThread() + "ElectionClient: sendPort lives");
	sport.connect(server);
// System.err.println(Thread.currentThread() + "ElectionClient: sendPort connected!");
    }

    void end() throws IOException {
	if (! ElectionProtocol.NEED_ELECTION) {
	    return;
	}

	sport.free();
	if (ElectionServer.DEBUG) {
	    System.err.println("ElectionClient frees receive port " + rport);
	}
	rport.free();
	if (ElectionServer.DEBUG) {
	    System.err.println("ElectionClient has freed receive port " + rport);
	}
    }

    Object elect(String election, Object candidate) throws IOException {
	if (! ElectionProtocol.NEED_ELECTION) {
		System.err.println("elections are turned OFF!");
		System.exit(1);
	}

// System.err.println(Thread.currentThread() + "ElectionClient: elect(): start send");

	ibis.ipl.WriteMessage m = sport.newMessage();
	m.writeInt(Ibis.myIbis.myCpu);
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
	    throw new IOException("election object class not found " + e);
	}
	r.finish();
	if (ElectionServer.DEBUG) {
	    System.err.println(Thread.currentThread() + "ElectionClient: elect() finished, winner " + winner + " my stake " + candidate);
	}

	return winner;
    }
}
