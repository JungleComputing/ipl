/* $Id$ */

package ibis.impl.messagePassing;

import ibis.ipl.IbisException;
import ibis.ipl.StaticProperties;
import ibis.util.TypedProperties;

import java.io.IOException;
import java.util.Hashtable;

/**
 * messagePassing Ibis implementation of Ibis election: the server side
 */
class ElectionServer implements Runnable, ibis.ipl.Upcall {

    static private Hashtable elections;

    private boolean started = false;

    private boolean finished = false;

    private boolean halted = false;

    final static boolean DEBUG = TypedProperties.booleanProperty(
            MPProps.s_elect_debug);

    public void upcall(ibis.ipl.ReadMessage m) {
        Ibis.myIbis.checkLockNotOwned();
        try {
            int sender = m.readInt();
            String name;
            Object o;
            try {
                name = (String) m.readObject();
                o = m.readObject();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("class not found " + e);
            }
            m.finish();

            if (ElectionServer.DEBUG) {
                System.err.println(Thread.currentThread()
                        + "ElectionServer receives election " + name
                        + " contender " + (o == null ? "null" : o.toString()));
            }

            Object e;
            synchronized (elections) {
                while (!started) {
                    try {
                        elections.wait();
                    } catch (InterruptedException ie) {
                        // ignore
                    }
                }
                e = elections.get(name);
                if (e == null) {
                    if (o != null) {
                        elections.put(name, o);
                    }
                    e = o;
                }
            }

            if (ElectionServer.DEBUG && e != null) {
                System.err.println(Thread.currentThread()
                        + "ElectionServer pronounces election " + name
                        + " winner " + e.toString());
            }
            ibis.ipl.WriteMessage r = client_port[sender].newMessage();
            r.writeObject(e);
            r.finish();
            if (ElectionServer.DEBUG) {
                System.err.println(Thread.currentThread()
                        + "ElectionServer election " + name + " done");
            }
        } catch (IOException e) {
            System.err.println(Thread.currentThread()
                    + ": ElectionServer upcall exception " + e);
            e.printStackTrace();
        }
    }

    private ibis.ipl.ReceivePort[] server_port;

    private ibis.ipl.SendPort[] client_port;

    ElectionServer() throws IbisException {
        if (!ElectionProtocol.NEED_ELECTION) {
            halted = true;
            return;
        }

        if (elections != null) {
            throw new IbisException("Can have only one ElectionServer");
        }
        elections = new Hashtable();

        Thread thr = new Thread(this, "Election Server");
        thr.setDaemon(true);
        thr.start();
    }

    void end() {
        synchronized (this) {
            notifyAll();
            finished = true;
        }
    }

    void awaitShutdown() {
        synchronized (this) {
            try {
                while (!halted) {
                    wait();
                }
            } catch (InterruptedException e) {
                // OK, bail out
            }
        }
    }

    public void run() {
        int n = Ibis.myIbis.nrCpus;

        try {
            server_port = new ibis.ipl.ReceivePort[Ibis.myIbis.nrCpus];
            client_port = new ibis.ipl.SendPort[Ibis.myIbis.nrCpus];
            StaticProperties p = new StaticProperties();
            p.add("Communication",
                    "OneToOne, Reliable, AutoUpcalls, ExplicitReceipt");
            p.add("Serialization", "sun");

            ibis.ipl.PortType type = Ibis.myIbis.newPortType(
                    "++++ElectionPort++++", p);

            for (int i = 0; i < n; i++) {
                server_port[i] = type.createReceivePort("++++ElectionServer-"
                        + i + "++++", this);
                server_port[i].enableConnections();
                server_port[i].enableUpcalls();
                client_port[i] = type.createSendPort("election_server");
            }

            for (int i = 0; i < n; i++) {
                ibis.ipl.ReceivePortIdentifier rid
                    = Ibis.myIbis.registry().lookupReceivePort(
                            "++++ElectionClient-" + i + "++++");
                client_port[i].connect(rid);
            }

            for (int i = 0; i < n; i++) {
                // Send a message so that client can wait until server
                // is initialized.
                ibis.ipl.WriteMessage r = client_port[i].newMessage();
                r.writeInt(0);
                r.finish();
            }

            synchronized (elections) {
                started = true;
                elections.notifyAll();
            }

        } catch (IOException e) {
            System.err.println("ElectionServer meets exception " + e);
        } catch (IbisException e2) {
            System.err.println("ElectionServer meets exception " + e2);
        }

        /* Await command to shut down */
        synchronized (this) {
            while (!finished) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
        }

        for (int i = 0; i < n; i++) {
            try {
                client_port[i].close();
            } catch (IOException e) {
                // Ignore
            }
        }
        for (int i = 0; i < n; i++) {
            if (DEBUG) {
                System.err.println("ElectionServer frees server port[" + i
                        + "] (of " + n + ") = " + server_port[i]);
            }
            try {
                server_port[i].close();
            } catch (IOException e) {
                // Ignore
            }
        }
        if (DEBUG) {
            System.err.println("ElectionServer has freed all server ports");
        }

        synchronized (this) {
            halted = true;
            notifyAll();
        }
    }

}
