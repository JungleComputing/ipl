/* $Id$ */

package ibis.impl.messagePassing;

import ibis.ipl.IbisException;
import ibis.ipl.StaticProperties;

import java.io.IOException;

/**
 * messagePassing Ibis implementation of Ibis election: the client side
 */
class ElectionClient implements ElectionProtocol {

    private ibis.ipl.ReceivePortIdentifier server;

    private ibis.ipl.SendPort sport;

    private ibis.ipl.ReceivePort rport;

    ElectionClient() throws IbisException, IOException {
        if (!ElectionProtocol.NEED_ELECTION) {
            return;
        }

        StaticProperties p = new StaticProperties();
        p.add("Communication",
                "OneToOne, Reliable, AutoUpcalls, ExplicitReceipt");
        p.add("Serialization", "sun");
        ibis.ipl.PortType type = Ibis.myIbis.newPortType(
                "++++ElectionPort++++", p);

        rport = type.createReceivePort("++++ElectionClient-"
                + Ibis.myIbis.myCpu + "++++");
        rport.enableConnections();

        server = Ibis.myIbis.registry().lookupReceivePort(
                "++++ElectionServer-" + Ibis.myIbis.myCpu + "++++");
        sport = type.createSendPort("election_client");
        sport.connect(server);

        // Read initial message from server. This makes sure that the
        // server is initialized with respect to this client, so that
        // this client cannot disappear when the server has not even set
        // up a connection yet (which gives deadlocks in termination).
        ibis.ipl.ReadMessage r = rport.receive();
        r.readInt();
        r.finish();
    }

    void end() throws IOException {
        if (!ElectionProtocol.NEED_ELECTION) {
            return;
        }

        sport.close();
        if (ElectionServer.DEBUG) {
            System.err.println("ElectionClient frees receive port " + rport);
        }
        rport.close();
        if (ElectionServer.DEBUG) {
            System.err.println("ElectionClient has freed receive port "
                    + rport);
        }
    }

    Object elect(String election, Object candidate) throws IOException {
        if (!ElectionProtocol.NEED_ELECTION) {
            System.err.println("elections are turned OFF!");
            System.exit(1);
        }

        Object winner = null;

        while (winner == null) {
            ibis.ipl.WriteMessage m = sport.newMessage();
            m.writeInt(Ibis.myIbis.myCpu);
            m.writeObject(election);
            m.writeObject(candidate);
            m.finish();

            if (ElectionServer.DEBUG) {
                System.err.println(Thread.currentThread()
                        + "ElectionClient: elect(): send done, now start rcve");
            }

            ibis.ipl.ReadMessage r = rport.receive();
            try {
                winner = r.readObject();
            } catch (ClassNotFoundException e) {
                throw new IOException("election object class not found " + e);
            }
            r.finish();
            if (ElectionServer.DEBUG) {
                System.err.println(Thread.currentThread()
                        + "ElectionClient: elect() finished, winner " + winner
                        + " my stake " + candidate);
            }
            if (winner == null) {
                try {
                    Thread.sleep(100);
                } catch (Exception ee) {
                    // Give up
                }
            }
        }

        return winner;
    }
}
