package ibis.ipl.examples;

import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;

import java.io.IOException;

/**
 * This program is to be run as two or more instances. The first instance is a
 * server, others are clients. The Server waits a while, then terminates the
 * pool. The clients use this to detect there is nothing more to be done, and
 * leave.
 */

public class Termination {

    IbisCapabilities ibisCapabilities = new IbisCapabilities(
            IbisCapabilities.ELECTIONS_STRICT, IbisCapabilities.TERMINATION);

    private void server(Ibis myIbis) throws IOException {

        // wait 30 seconds
        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            // IGNORE
        }

        // terminate the pool
        System.out.println("Terminating pool");

        myIbis.registry().terminate();

        // wait for this termination to propagate through the system

        myIbis.registry().waitUntilTerminated();

    }

    private void client(Ibis myIbis, IbisIdentifier server) throws IOException {

        // poll once every second for the termination of the pool.
        while (!myIbis.registry().hasTerminated()) {
            System.out.println("Pool not terminated, waiting a second");

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // IGNORE
            }
        }

        // alternatively, we could also simply do:
        // myIbis.registry().waitUntilTerminated();

        System.out.println("Pool termintated!");
    }

    private void run() throws Exception {
        // Create an ibis instance.
        Ibis ibis = IbisFactory.createIbis(ibisCapabilities, null);

        System.out.println("I am " + ibis.identifier());

        // Elect a server
        IbisIdentifier server = ibis.registry().elect("Server");

        System.out.println("Server is " + server);

        // If I am the server, run server, else run client.
        if (server.equals(ibis.identifier())) {
            server(ibis);
        } else {
            client(ibis, server);
        }

        // End ibis.
        ibis.end();
    }

    public static void main(String args[]) {
        try {
            new Termination().run();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
}
