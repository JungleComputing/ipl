package ibis.impl.registry;

import ibis.ipl.IbisProperties;
import ibis.util.Log;
import ibis.util.PoolInfoServer;
import ibis.util.TypedProperties;

import java.io.PrintStream;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import smartsockets.hub.Hub;

public abstract class Server extends Thread {

    // NOT the name of this class but the entire registry package instead
    private static final Logger logger = Logger.getLogger("ibis.impl.registry");

    private static void printUsage(PrintStream stream) {
        stream.println("Start a registry server for Ibis)");
        stream.println();
        stream.println("USAGE: ibis-registry-server [OPTIONS]");
        stream.println();
        stream.println("Basic options:");
        stream.println();
        stream.println("--port PORT\t\tBase port used for the server");
        stream
                .println("\t\t\tAdditional services will use PORT+1, PORT+2, etc");
        stream.println();
        stream.println("--hub\t\t\tAlso start a SmartSockets hub on PORT+1");
        stream.println("--hub_only\t\tONLY start a SmartSockets hub");
        stream.println("--hub_addresses ADDRESS,[ADDRESS]*");
        stream.println("\t\t\tAddresses of additional hubs to connect to");
        stream.println("\t\t\tOnly works with --hub or --hub_only options");
        stream.println();
        stream
                .println("--pool_info_server\tAlso start a PoolInfoServer hub on PORT+2");
        stream.println();

        stream.println("--warn\t\t\tOnly print warnings and errors, "
                + "no status messages");
        stream.println("--debug\t\t\tPrint debug output");
        stream.println("--help | -h | /?\tThis message");
        stream.println();

        stream.println("Advanced options:");
        stream.println();
        stream
                .println("--old_plain\t\tUse a version of the registry without smartsockets");

        stream
                .println("--old_smart\t\tUse a version of the registry with smartsockets");
        stream
                .println("--new_plain\t\tNew registry implementation, without smartsockets");
        stream
                .println("--new_smart\t\tUse the new registry implementation, with smartsockets");
        stream.println();
        stream.println("PROPERTY=VALUE\t\tset a property");
        stream.println();
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
        properties.addProperties(IbisProperties.getConfigProperties());

        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("--port")) {
                i++;
                properties.put(RegistryProperties.SERVER_PORT, args[i]);
            } else if (args[i].equalsIgnoreCase("--hub")) {
                startHub = true;
            } else if (args[i].equalsIgnoreCase("--hub_only")) {
                startHub = true;
                hubOnly = true;
            } else if (args[i].equalsIgnoreCase("--hub_addresses")) {
                i++;
                hubAddresses = args[i].split(",");
            } else if (args[i].equalsIgnoreCase("--pool_info")) {
                startPoolInfo = true;
            } else if (args[i].equalsIgnoreCase("--warn")) {
                logLevel = Level.WARN;
            } else if (args[i].equalsIgnoreCase("--debug")) {
                logLevel = Level.DEBUG;
            } else if (args[i].equalsIgnoreCase("--help")
                    || args[i].equalsIgnoreCase("-h")
                    || args[i].equalsIgnoreCase("/?")) {
                printUsage(System.out);
                System.exit(0);
            } else if (args[i].equalsIgnoreCase("--old_plain")) {
                properties.setProperty(RegistryProperties.SERVER_IMPL,
                        "ibis.impl.registry.tcp.NameServer");
                properties
                        .setProperty(RegistryProperties.SMARTSOCKETS, "false");
            } else if (args[i].equalsIgnoreCase("--old_smart")) {
                properties.setProperty(RegistryProperties.SERVER_IMPL,
                        "ibis.impl.registry.smartsockets.NameServer");
                properties.setProperty(RegistryProperties.SMARTSOCKETS, "true");
            } else if (args[i].equalsIgnoreCase("--new_plain")) {
                properties.setProperty(RegistryProperties.SERVER_IMPL,
                        "ibis.impl.registry.central.Server");
                properties
                        .setProperty(RegistryProperties.SMARTSOCKETS, "false");
            } else if (args[i].equalsIgnoreCase("--new_smart")) {
                properties.setProperty(RegistryProperties.SERVER_IMPL,
                        "ibis.impl.registry.central.Server");
                properties.setProperty(RegistryProperties.SMARTSOCKETS, "true");
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
                properties.setProperty(smartsockets.Properties.HUB_PORT,
                        Integer.toString(port));
                hub = new Hub(null, new smartsockets.util.TypedProperties(
                        properties));
                hub.addHubs(null, hubAddresses);
                properties.setProperty(smartsockets.Properties.HUB_ADDRESS, hub
                        .getHubAddress().toString());
                logger.info("Hub running on " + hub.getHubAddress());
            } catch (Throwable t) {
                logger.warn("Could not start Hub", t);
            }
        }

        if (startPoolInfo) {
            try {
                int port = basePort + 1;
                new PoolInfoServer(port, false);
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
                logger.info("Server running on " + server.getLocalAddress());
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
