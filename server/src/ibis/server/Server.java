package ibis.server;

import ibis.util.ClassLister;
import ibis.util.TypedProperties;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import ibis.server.remote.RemoteHandler;
import ibis.smartsockets.SmartSocketsProperties;
import ibis.smartsockets.direct.DirectSocketAddress;
import ibis.smartsockets.hub.Hub;
import ibis.smartsockets.hub.servicelink.ServiceLink;
import ibis.smartsockets.virtual.VirtualSocketFactory;

/**
 * Main Ibis Server class.
 */
public final class Server {

    private static final Logger logger = Logger.getLogger(Server.class);

    private final VirtualSocketFactory virtualSocketFactory;

    private final Hub hub;

    private final DirectSocketAddress address;

    private final Map<String, Service> services;

    private final boolean hubOnly;

    private final boolean remote;

    /**
     * Create a server with the given server properties
     */
    @SuppressWarnings("unchecked")
    public Server(Properties properties) throws Exception {
        services = new HashMap<String, Service>();

        // load properties from config files and such
        TypedProperties typedProperties = ServerProperties
                .getHardcodedProperties();

        typedProperties.addProperties(properties);

        if (logger.isDebugEnabled()) {
            TypedProperties serverProperties = typedProperties
                    .filter("ibis.server");
            logger.debug("Settings for server:\n" + serverProperties);
        }

        // create the virtual socket factory
        ibis.smartsockets.util.TypedProperties smartProperties = new ibis.smartsockets.util.TypedProperties();

        String hubs = typedProperties
                .getProperty(ServerProperties.HUB_ADDRESSES);
        if (hubs != null) {
            smartProperties.put(SmartSocketsProperties.HUB_ADDRESSES, hubs);
        }

        String hubAddressFile = typedProperties
                .getProperty(ServerProperties.HUB_ADDRESS_FILE);
        if (hubAddressFile != null) {
            smartProperties.put(SmartSocketsProperties.HUB_ADDRESS_FILE,
                    hubAddressFile);
        }

        hubOnly = typedProperties.getBooleanProperty(ServerProperties.HUB_ONLY);

        remote = typedProperties.getBooleanProperty(ServerProperties.REMOTE);

        if (hubOnly) {
            virtualSocketFactory = null;

            smartProperties.put(SmartSocketsProperties.HUB_PORT,
                    typedProperties.getProperty(ServerProperties.PORT));

            hub = new Hub(smartProperties);
            address = hub.getHubAddress();

        } else {
            hub = null;

            smartProperties.put(SmartSocketsProperties.PORT_RANGE,
                    typedProperties.getProperty(ServerProperties.PORT));

            if (typedProperties.getBooleanProperty(ServerProperties.START_HUB)) {
                smartProperties.put(SmartSocketsProperties.START_HUB, "true");
                smartProperties
                        .put(SmartSocketsProperties.HUB_DELEGATE, "true");
            }

            virtualSocketFactory = VirtualSocketFactory.createSocketFactory(
                    smartProperties, true);
            address = virtualSocketFactory.getLocalHost();

            try {
                ServiceLink sl = virtualSocketFactory.getServiceLink();
                if (sl != null) {
                    sl.registerProperty("smartsockets.viz", "S^Ibis server:,"
                            + address.toString());
                    // sl.registerProperty("ibis", id.toString());
                }
            } catch (Throwable e) {
                // ignored
            }

            ClassLister classLister = ClassLister.getClassLister(null);
            Class[] serviceClassList = classLister.getClassList("Ibis-Service",
                    Service.class).toArray(new Class[0]);

            for (int i = 0; i < serviceClassList.length; i++) {
                try {
                    Service service = (Service) serviceClassList[i]
                            .getConstructor(
                                    new Class[] { TypedProperties.class,
                                            VirtualSocketFactory.class })
                            .newInstance(
                                    new Object[] { typedProperties,
                                            virtualSocketFactory });
                    services.put(service.getServiceName(), service);
                } catch (InvocationTargetException e) {
                    if (e.getCause() == null) {
                        logger.warn("Could not create service "
                                + serviceClassList[i] + ":", e);
                    } else {
                        logger.warn("Could not create service "
                                + serviceClassList[i] + ":", e.getCause());
                    }
                } catch (Throwable e) {
                    logger.warn("Could not create service "
                            + serviceClassList[i] + ":", e);
                }
            }
        }
    }

    /**
     * Returns the local address of this server as a string
     */
    public String getLocalAddress() {
        return address.toString();
    }

    /**
     * Returns the names of all services currently in this server
     */
    public String[] getServiceNames() {
        return services.keySet().toArray(new String[0]);
    }

    /**
     * Function to retrieve statistics for a given service
     * 
     * @param serviceName
     *            Name of service to get statistics of
     * 
     * @return statistics for given service, or null if service exist.
     */
    public Map<String, String> getStats(String serviceName) {
        Service service = services.get(serviceName);

        if (service == null) {
            return null;
        }

        return service.getStats();
    }

    /**
     * Returns the addresses of all hubs known to this server
     */
    public String[] getHubs() {
        DirectSocketAddress[] hubs;
        if (hubOnly) {
            hubs = hub.knownHubs();
        } else {
            hubs = virtualSocketFactory.getKnownHubs();
        }

        ArrayList<String> result = new ArrayList<String>();
        for (DirectSocketAddress hub : hubs) {
            result.add(hub.toString());
        }

        return result.toArray(new String[0]);
    }

    /**
     * Tell the server about some hubs
     */
    public void addHubs(DirectSocketAddress... hubAddresses) {
        if (hubOnly) {
            hub.addHubs(hubAddresses);
        } else {
            virtualSocketFactory.addHubs(hubAddresses);
        }
    }

    /**
     * Tell the server about some hubs
     */
    public void addHubs(String... hubAddresses) {
        if (hubOnly) {
            hub.addHubs(hubAddresses);
        } else {
            virtualSocketFactory.addHubs(hubAddresses);
        }
    }

    public String toString() {
        if (hubOnly) {
            return "Hub running on " + getLocalAddress();
        }

        String message = "Ibis server running on " + getLocalAddress()
                + "\nList of Services:";

        for (Service service : services.values()) {
            message += "\n    " + service.toString();
        }

        return message;

    }

    /**
     * Stops all services. May wait until the services are idle.
     * 
     * @param timeout
     *            timeout for ending all services in Milliseconds. 0 == wait
     *            forever, -1 == no not wait.
     */
    public void end(long timeout) {
        long deadline = System.currentTimeMillis() + timeout;

        if (timeout == 0) {
            deadline = Long.MAX_VALUE;
        } else if (timeout == -1) {
            deadline = 0;
        }

        for (Service service : services.values()) {
            service.end(deadline);
        }
        if (hubOnly) {
            hub.end();
        } else {
            virtualSocketFactory.end();
        }
    }

    private boolean hasRemote() {
        return remote;
    }

    private static void printUsage(PrintStream out) {
        out.println("Start a server for Ibis.");
        out.println();
        out.println("USAGE: ibis-server [OPTIONS]");
        out.println();
        out.println("--no-hub\t\t\tDo not start a hub.");
        out
                .println("--hub-only\t\t\tOnly start a hub, not the rest of the server.");
        out
                .println("--hub-addresses HUB[,HUB]\tAdditional hubs to connect to.");
        out
                .println("--hub-address-file [FILE_NAME]\tWrite the addresses of the hub to the given");
        out.println("\t\t\t\tfile. The file is deleted on exit.");
        out.println("--port PORT\t\t\tPort used for the server.");
        out
                .println("--remote \t\t\t\tListen to commands for this server on stdin.");
        out.println();
        out
                .println("PROPERTY=VALUE\t\t\tSet a property, as if it was set in a");
        out.println("\t\t\t\tconfiguration file or as a System property.");
        out.println("Output Options:");
        out.println("--events\t\t\tPrint events.");
        out
                .println("--errors\t\t\tPrint details of errors (such as stacktraces).");
        out.println("--stats\t\t\t\tPrint statistics once in a while.");
        out.println("--help | -h | /?\t\tThis message.");
    }

    private static class Shutdown extends Thread {
        private final Server server;

        Shutdown(Server server) {
            this.server = server;
        }

        public void run() {
            server.end(-1);
        }
    }

    /**
     * Run the ibis server
     */
    public static void main(String[] args) {
        Properties properties = new Properties();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("--no-hub")) {
                properties.setProperty(ServerProperties.START_HUB, "false");
            } else if (args[i].equalsIgnoreCase("--hub-only")) {
                properties.setProperty(ServerProperties.HUB_ONLY, "true");
            } else if (args[i].equalsIgnoreCase("--hub-addresses")) {
                i++;
                properties.setProperty(ServerProperties.HUB_ADDRESSES, args[i]);
            } else if (args[i].equalsIgnoreCase("--hub-address-file")) {
                i++;
                properties.setProperty(ServerProperties.HUB_ADDRESS_FILE,
                        args[i]);
            } else if (args[i].equalsIgnoreCase("--port")) {
                i++;
                properties.put(ServerProperties.PORT, args[i]);
            } else if (args[i].equalsIgnoreCase("--events")) {
                properties.setProperty(ServerProperties.PRINT_EVENTS, "true");
            } else if (args[i].equalsIgnoreCase("--errors")) {
                properties.setProperty(ServerProperties.PRINT_ERRORS, "true");
            } else if (args[i].equalsIgnoreCase("--stats")) {
                properties.setProperty(ServerProperties.PRINT_STATS, "true");
            } else if (args[i].equalsIgnoreCase("--remote")) {
                properties.setProperty(ServerProperties.REMOTE, "true");
            } else if (args[i].equalsIgnoreCase("--help")
                    || args[i].equalsIgnoreCase("-help")
                    || args[i].equalsIgnoreCase("-h")
                    || args[i].equalsIgnoreCase("/?")) {
                printUsage(System.err);
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

        Server server = null;
        try {
            server = new Server(properties);
        } catch (Throwable t) {
            System.err.println("Could not start Server: " + t);
            System.exit(1);
        }

        // register shutdown hook
        try {
            Runtime.getRuntime().addShutdownHook(new Shutdown(server));
        } catch (Exception e) {
            System.err.println("warning: could not registry shutdown hook");
        }

        if (server.hasRemote()) {
            new RemoteHandler(server).run();
        } else {
            System.err.println(server.toString());
            String knownHubs = null;
            while (true) {
                String[] hubs = server.getHubs();
                // FIXME: remove if smartsockets promises to not return null ;)
                if (hubs == null) {
                    hubs = new String[0];
                }

                if (hubs.length != 0) {
                    String newKnownHubs = hubs[0].toString();
                    for (int i = 1; i < hubs.length; i++) {
                        newKnownHubs += "," + hubs[i].toString();
                    }

                    if (!newKnownHubs.equals(knownHubs)) {
                        knownHubs = newKnownHubs;
                        System.err.println("Known hubs now: " + knownHubs);
                    }
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }

}
