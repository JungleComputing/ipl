package ibis.ipl.benchmarks.registry;

import java.text.DateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private final IbisApplication[] apps;

    Main(int threads, boolean generateEvents, boolean fail) throws Exception {

        apps = new IbisApplication[threads];
        for (int i = 0; i < threads; i++) {
            logger.debug("starting thread " + i + " of " + threads);
            apps[i] = new IbisApplication(generateEvents, fail);
        }
    }

    void end() {
        for (IbisApplication app : apps) {
            app.end();
        }
    }

    void printStats() {
        int totalSeen = 0;
        for (int i = 0; i < apps.length; i++) {
            totalSeen += apps[i].nrOfIbisses();
        }
        double average = (double) totalSeen / (double) apps.length;

        String date =
            DateFormat.getTimeInstance().format(
                new Date(System.currentTimeMillis()));

        System.out.printf(date + " average seen members = %.2f\n", average,
            apps.length);
    }

    public static void main(String[] args) throws Exception {
        int threads = 1;
        boolean generateEvents = false;
        long start = System.currentTimeMillis();
        long runtime = Long.MAX_VALUE;
	long delay = 0;
        boolean fail = false;

        // int rank = new Integer(System.getProperty("rank", "0"));
        
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("--threads")) {
                i++;
                threads = new Integer(args[i]);
            } else if (args[i].equalsIgnoreCase("--events")) {
                generateEvents = true;
            } else if (args[i].equalsIgnoreCase("--fail")) {
                fail = true;
            } else if (args[i].equalsIgnoreCase("--runtime")) {
                i++;
                runtime = new Integer(args[i]) * 1000;
            } else if (args[i].equalsIgnoreCase("--delay")) {
                i++;
                delay = new Integer(args[i]) * 1000;
            } else {
                System.err.println("unknown option: " + args[i]);
                System.exit(1);
            }
        }

	    //delay specified time
            if (delay > 0) {
                Thread.sleep(delay);
            }

	    //create ibisses
            new Main(threads, generateEvents, fail);

	    //sleep for specified runtime
            long sleep = runtime - (System.currentTimeMillis() - start);
            System.err.println("Benchmark app sleeping : " + sleep);
            if (sleep > 0) {
                Thread.sleep(sleep);
            }

	    //app stopped automatically
    }

}
