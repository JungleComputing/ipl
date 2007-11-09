/* $Id: Hello.java 6430 2007-09-20 16:37:59Z ceriel $ */

package ibis.ipl.examples;

import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.RegistryEventHandler;

/**
 * This program shows how to handle events from the registry using upcalls. It
 * will run for 10 seconds, then stop. You can start as many instances of this
 * application as you like. 
 */

public class Registry implements RegistryEventHandler {

    IbisCapabilities ibisCapabilities =
        new IbisCapabilities(IbisCapabilities.MEMBERSHIP_TOTALLY_ORDERED);

    private void run() throws Exception {
        // Create an ibis instance, pass ourselves as the
        Ibis ibis = IbisFactory.createIbis(ibisCapabilities, this);
        ibis.registry().enableEvents();

        // sleep for 30 seconds
        Thread.sleep(30000);

        // End ibis.
        ibis.end();
    }

    public static void main(String args[]) {
        try {
            new Registry().run();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    // Methods of the registry event handler. We only implement the 
    // join/leave/died methods, as signals and elections are disabled
    
    public void joined(IbisIdentifier joinedIbis) {
        System.err.println("Got event from registry: " + joinedIbis
                + " joined pool");
    }

    public void died(IbisIdentifier corpse) {
        System.err.println("Got event from registry: " + corpse + " died!");
    }

    public void left(IbisIdentifier leftIbis) {
        System.err.println("Got event from registry: " + leftIbis + " left");
    }

    public void gotSignal(String signal) {
        // NOTHING
    }

    public void electionResult(String electionName, IbisIdentifier winner) {
        // NOTHING
    }
}
