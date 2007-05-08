package ibis.ipl.impl.registry;

import ibis.util.Log;
import ibis.util.PoolInfoServer;
import ibis.util.TypedProperties;

import java.io.PrintStream;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import ibis.smartsockets.hub.Hub;

public abstract class Server extends Thread {

    // NOT the name of this class but the entire registry package instead
    private static final Logger logger = Logger
            .getLogger("ibis.ipl.impl.registry");

    private static void printUsage(PrintStream stream) {
        stream.println("Start a server for Ibis.");
        stream.println();
        stream.println("USAGE: ibis-server [OPTIONS]");
        stream.println();

        stream.println("--port PORT\t\tBase port used for the server.");
        stream
                .println("\t\t\tAdditional services will use PORT+1, PORT+2, etc.");
        stream.println();
        stream.println("--hub\t\t\tAlso start a SmartSockets hub on PORT+1.");
        stream
                .println("--hub-only\t\tONLY start a SmartSockets hub on PORT+1.");
        stream.println("--hub-addresses ADDRESS,[ADDRESS]*");
        stream.println("\t\t\tAddresses of additional hubs to connect to.");
        stream.println("\t\t\tOnly works with --hub or --hub-only options.");
        stream.println();
        stream.println("--pool-info\t\tAlso start a PoolInfo Server on PORT+2");
        stream.println();

        stream
                .println("PROPERTY=VALUE\t\tSet a property, as if it was set in a configuration");
        stream.println("\t\t\tfile or as a System property.");
        stream.println();
        stream
                .println("--events\t\t\tPrint events such as new pools/joins/leaves/etc");
        stream.println("--stats\t\t\tPrint statistics once in a while");
        stream.println("--warn\t\t\tOnly print warnings and errors, "
                + "no status messages or events or statistics");
        stream.println("--debug\t\t\tPrint debug output.");
        stream.println("--help | -h | /?\tThis message.");
    }

    public static Server createServer(Properties properties) throws Throwable {
        String implName = properties
                .getProperty(RegistryProperties.SERVER_IMPL);

        if (implName == null) {
            throw new Exception("cannot create server, no implementation set");
        }

        Class<?> c = Class.forName(implName);

        try {
            return (Server) c.getConstructor(new Class[] { Properties.class })
                    .newInstance(new Object[] { properties });
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw e.getCause();
        }
    }

    /**
     * Run a registry server
     */
    public static void main(String[] args) {
        boolean startHub = false;
        boolean hubOnly = false;
        String[] hubAddresses = null;
        boolean startPoolInfo = false;
        Level logLevel = Level.INFO;

        // add an appender to this package if needed
        Log.initLog4J(logger);

        TypedProperties properties = new TypedProperties();
        properties.addProperties(RegistryProperties.getHardcodedProperties());
        properties.addProperties(TypedProperties.getDefaultConfigProperties());

        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("--port")) {
                i++;
                properties.put(RegistryProperties.SERVER_PORT, args[i]);
            } else if (args[i].equalsIgnoreCase("--hub")) {
                startHub = true;
            } else if (args[i].equalsIgnoreCase("--hub-only")) {
                startHub = true;
                hubOnly = true;
            } else if (args[i].equalsIgnoreCase("--hub-addresses")) {
                i++;
                hubAddresses = args[i].split(",");
            } else if (args[i].equalsIgnoreCase("--pool-info")) {
                startPoolInfo = true;
            } else if (args[i].equalsIgnoreCase("--events")) {
                properties.setProperty(RegistryProperties.CENTRAL_LOG_EVENTS,
                        "true");
            } else if (args[i].equalsIgnoreCase("--stats")) {
                properties.setProperty(
                        RegistryProperties.CENTRAL_LOG_STATISTICS, "true");
            } else if (args[i].equalsIgnoreCase("--warn")) {
                logLevel = Level.WARN;
            } else if (args[i].equalsIgnoreCase("--debug")) {
                logLevel = Level.DEBUG;
            } else if (args[i].equalsIgnoreCase("--help")
                    || args[i].equalsIgnoreCase("-h")
                    || args[i].equalsIgnoreCase("/?")) {
                printUsage(System.out);
                System.exit(0);
            } else if (args[i].contains("=")) {
                String[] parts = args[i].split("=", 2);
                properties.setProperty(parts[0], parts[1]);
            } else {
                System.err.println("Unknown argument: " + args[i]);
                printUsage(System.err);
                System.exit(1);
            }
        }

        logger.setLevel(logLevel);

        int basePort = properties
                .getIntProperty(RegistryProperties.SERVER_PORT);

        Hub hub = null;
        if (startHub || hubOnly) {
            // FIXME: use new/improved hub constructor when/if it arrives
            try {
                int port = basePort + 1;
                
                ibis.smartsockets.util.TypedProperties smartProperties = 
                    new ibis.smartsockets.util.TypedProperties();
                
                smartProperties.setProperty(
                        ibis.smartsockets.SmartSocketsProperties.HUB_PORT,
                        Integer.toString(port));
                
                hub = new Hub(smartProperties);
                hub.addHubs(hubAddresses);

                // tell the server there is a hub
                properties.setProperty(RegistryProperties.SERVER_HUB_ADDRESS,
                        hub.getHubAddress().toString());

                logger.info("Hub running on " + hub.getHubAddress());
            } catch (Throwable t) {
                logger.warn("Could not start Hub", t);
            }
        }

        if (startPoolInfo) {
            try {
                int port = basePort + 2;
                new PoolInfoServer(port, false).start();
                logger.info("PoolInfoServer running on port " + port);
            } catch (Throwable t) {
                logger.warn("Could not start Pool info: ", t);
            }
        }

        // properties.printProperties(System.out, "ibis");
        // properties.printProperties(System.out, "smart");

        Server server = null;
        if (!hubOnly) {
            try {
                server = createServer(properties);
                logger.info("Started " + server.toString());
            } catch (Throwable t) {
                logger.error("Could not start Server", t);
                System.exit(1);
            }
            // run server until completion, then exit
            server.run();
        } else {
            // wait for hub to finish...
            try {
                hub.join();
            } catch (InterruptedException e) {
                // IGNORE
            }
        }

    }

    /**
     * Runs a server (blocks)
     */
    public abstract void run();

    /**
     * Returns the local address of this server as a string
     */
    public abstract String getLocalAddress();
}
