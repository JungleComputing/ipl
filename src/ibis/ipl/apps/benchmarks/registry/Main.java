package ibis.ipl.apps.benchmarks.registry;


import ibis.util.Log;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;


public final class Main {
 
    private static final Logger logger = Logger.getLogger(Main.class);
    
    private final Application[] apps;
    
    Main(int threads, boolean sync, int step) {
        
        apps = new Application[threads];
        for (int i = 0; i < threads ; i++) {
            logger.debug("starting thread " + i + " of " + threads);
            try {
                apps[i] = new Application(i == (threads - 1), step);
                if (sync) {
                    //wait for the ibis to be initialized
                    apps[i].run();
                } else {
                    //fork of a thread for the application to run in
                    apps[i].start();
                }
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
    }
    
    public static void main(String[] args) {
        int threads = 1;
        int step = 1;
        boolean sync = false;

	Log.initLog4J("ibis.ipl.apps", Level.INFO);
        
        for (int i = 0; i < args.length;i++) {
            if (args[i].equalsIgnoreCase("--threads")) {
                i++;
                threads = new Integer(args[i]);
            } else if (args[i].equalsIgnoreCase("--sync")) {
                sync = true;
            } else if (args[i].equalsIgnoreCase("--step")) {
                i++;
                step = new Integer(args[i]);
            } else {
                System.err.println("unknown option: " + args[i]);
                System.exit(1);
            }
        }
            
        new Main(threads, sync, step);

      
        
        while(true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                //IGNORE
            }
        }
    }
    
   


}
