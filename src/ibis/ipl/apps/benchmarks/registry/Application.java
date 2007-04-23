package ibis.ipl.apps.benchmarks.registry;

import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.RegistryEventHandler;
import ibis.util.ThreadPool;

import org.apache.log4j.Logger;

import sun.misc.Signal;
import sun.misc.SignalHandler;

final class Application implements Runnable, RegistryEventHandler {

    private static final Logger logger = Logger.getLogger(Application.class);

    private final long start;

    private final int step;

    private int seen;

    private final boolean printStats;

    private Ibis ibis;

    Application(boolean printStats, int step) {
        this.printStats = printStats;
        this.step = step;

        start = System.currentTimeMillis();
        seen = 0;
        
        // register shutdown hook
        try {
            Runtime.getRuntime().addShutdownHook(new Shutdown(this));
        } catch (Exception e) {
            // IGNORE
        }
        
        try {
            Signal.handle(new Signal("USR2"), new Terminator(this));
        } catch (Exception e) {
            logger.warn("could not install handler for USR2 signal");
        }

    }

    void start() {
        ThreadPool.createNew(this, "application");
    }

    public void run() {
        try {
            IbisCapabilities s = new IbisCapabilities(IbisCapabilities.WORLDMODEL_OPEN);
            PortType p = new PortType(PortType.CONNECTION_ONE_TO_ONE, PortType.SERIALIZATION_DATA);

            logger.debug("creating ibis");
            Ibis ibis = IbisFactory.createIbis(s, null, this, p);
            logger.debug("ibis created, enabling upcalls");

            ibis.registry().enableEvents();
            logger.debug("upcalls enabled");

            synchronized (this) {
                this.ibis = ibis;
            }
        } catch (Exception e) {
            logger.error("could not initialize application", e);
        }
    }

    synchronized void end() {
        if (ibis != null) {
            try {
                ibis.end();
                logger.debug("ended ibis");
            } catch (Exception e) {
                logger.error("could not end ibis", e);
            }
        }
    }

    public synchronized void joined(IbisIdentifier ident) {
        seen++;
        if (printStats && seen % step == 0) {
            double time = (System.currentTimeMillis() - start) / 1000.0;
            logger.info(time + ": seen " + seen + " joins so far");
/*	    try {
		    ibis.registry().signal("I see you", ident);
	    } catch (IOException e) {
	    	System.err.println("could not send signal");
		e.printStackTrace(System.err);
	    }
*/
        }
    }

    public void left(IbisIdentifier ident) {
        // IGNORE
    }

    public void died(IbisIdentifier corpse) {
        // IGNORE
    }

    public void gotSignal(String signal) {
    	System.err.println("got signal: " + signal);
    }
    
    private static class Shutdown extends Thread {
        private final Application app;

        Shutdown(Application app) {
            this.app = app;
        }

        public void run() {
            System.err.println("shutdown hook triggered");

            app.end();
        }
    }
    
    private static class Terminator implements SignalHandler {
        private final Application app;

        Terminator(Application app) {
            this.app = app;
        }

        public void handle(Signal signal) {
            logger.debug("SIGUSR2 catched, shutting down");

            app.end();
            System.exit(0);
        }
    }
}
