package ibis.ipl.server;

import ibis.ipl.registry.ControlPolicy;
import ibis.ipl.registry.central.server.CentralRegistryService;
import ibis.ipl.registry.gossip.BootstrapService;
import ibis.ipl.support.management.ManagementService;
import ibis.smartsockets.SmartSocketsProperties;
import ibis.smartsockets.direct.DirectSocketAddress;
import ibis.smartsockets.hub.Hub;
import ibis.smartsockets.hub.servicelink.ServiceLink;
import ibis.smartsockets.virtual.VirtualSocketFactory;
import ibis.util.ClassLister;
import ibis.util.TypedProperties;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main Ibis Server class.
 * @ibis.experimental
 */
public final class Server implements ServerInterface {

    public static final String ADDRESS_LINE_PREFIX = "IBIS SERVER RUNNING ON: ";

    public static final String ADDRESS_LINE_POSTFIX = "EOA";

    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    private final VirtualSocketFactory virtualSocketFactory;

    private final Hub hub;

    private final ServerConnectionHandler connectionHandler;

    private final DirectSocketAddress address;

    private final CentralRegistryService registryService;

    private final BootstrapService bootstrapService;

    private final ManagementService managementService;

    // services specified by user (either in jars or loaded over the network)
    private final Map<String, Service> services;

    private final boolean hubOnly;

    private final boolean remote;

    public Server(Properties properties) throws Exception {
        this(properties, null);
    }

    /**
     * Create a server with the given server properties
     */
    @SuppressWarnings("unchecked")
    public Server(Properties properties, ControlPolicy policy) throws Exception {
        services = new HashMap<String, Service>();

        // get default properties.
        TypedProperties typedProperties = ServerProperties
                .getHardcodedProperties();

        // add specified properties
        typedProperties.addProperties(properties);

        if (logger.isDebugEnabled()) {
            TypedProperties serverProperties = typedProperties
                    .filter("ibis.server");
            logger.debug("Settings for server:\n" + serverProperties);
        }

        // create the virtual socket factory
        ibis.smartsockets.util.TypedProperties smartProperties = new ibis.smartsockets.util.TypedProperties();
        smartProperties.putAll(SmartSocketsProperties.getDefaultProperties());

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

        if (typedProperties.getBooleanProperty(ServerProperties.PRINT_STATS)) {
            smartProperties.put(SmartSocketsProperties.HUB_STATISTICS, "true");
            smartProperties.put(SmartSocketsProperties.HUB_STATS_INTERVAL,
                    "60000");
        }

        hubOnly = typedProperties.getBooleanProperty(ServerProperties.HUB_ONLY);

        remote = typedProperties.getBooleanProperty(ServerProperties.REMOTE);

        String vizInfo = properties.getProperty(ServerProperties.VIZ_INFO);

        if (hubOnly) {
            if (vizInfo != null) {
                smartProperties.put(SmartSocketsProperties.HUB_VIZ_INFO,
                        vizInfo);
            }

            virtualSocketFactory = null;

            smartProperties.put(SmartSocketsProperties.HUB_PORT,
                    typedProperties.getProperty(ServerProperties.PORT));

            hub = new Hub(smartProperties);

            address = hub.getHubAddress();

            connectionHandler = null;
            registryService = null;
            bootstrapService = null;
            managementService = null;

        } else {
            if (vizInfo == null) {
                smartProperties.put(SmartSocketsProperties.HUB_VIZ_INFO,
                        "S^Ibis Server");
            } else {
                smartProperties.put(SmartSocketsProperties.HUB_VIZ_INFO,
                        vizInfo);

            }

            // create server socket

            hub = null;

            smartProperties.put(SmartSocketsProperties.PORT_RANGE,
                    typedProperties.getProperty(ServerProperties.PORT));

            if (typedProperties.getBooleanProperty(ServerProperties.START_HUB)) {
                smartProperties.put(SmartSocketsProperties.START_HUB, "true");
                smartProperties
                        .put(SmartSocketsProperties.HUB_DELEGATE, "true");
            }

            // create a factory, or get an existing one from SmartSockets
            VirtualSocketFactory factory = VirtualSocketFactory
                    .getSocketFactory("ibis");

            if (factory == null) {
                factory = VirtualSocketFactory.getOrCreateSocketFactory("ibis",
                        smartProperties, true);
            } else if (hubs != null) {
                factory.addHubs(hubs.split(","));
            }

            this.virtualSocketFactory = factory;

            address = virtualSocketFactory.getLocalHost();

            try {
                ServiceLink sl = virtualSocketFactory.getServiceLink();
                if (sl != null) {
                    sl.registerProperty("smartsockets.viz", "invisible");
                } else {
                    logger
                            .warn("could not set smartsockets viz property: could not get smartsockets service link");
                }
            } catch (Throwable e) {
                logger.warn("could not register smartsockets viz property", e);
            }

            // create default services

            registryService = new CentralRegistryService(typedProperties,
                    virtualSocketFactory, policy);
            services.put(registryService.getServiceName(), registryService);

            bootstrapService = new BootstrapService(typedProperties,
                    virtualSocketFactory);
            services.put(bootstrapService.getServiceName(), bootstrapService);

            managementService = new ManagementService(typedProperties,
                    virtualSocketFactory);
            services.put(managementService.getServiceName(), managementService);

            // create user specified services

            ClassLister classLister = ClassLister.getClassLister(null);
            Class[] serviceClassList;
            if (typedProperties.containsKey(ServerProperties.SERVICES)) {
                String[] services = typedProperties
                        .getStringList(ServerProperties.SERVICES);
                if (services == null) {
                    serviceClassList = classLister.getClassList("Ibis-Service",
                            Service.class).toArray(new Class[0]);
                } else {
                    serviceClassList = new Class[services.length];
                    for (int i = 0; i < services.length; i++) {
                        serviceClassList[i] = Class.forName(services[i]);
                    }
                }
            } else {
                serviceClassList = classLister.getClassList("Ibis-Service",
                        Service.class).toArray(new Class[0]);
            }

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

            // start handling remote requests
            connectionHandler = new ServerConnectionHandler(this, factory);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.server.ServerInterface#getRegistryService()
     */
    public CentralRegistryService getRegistryService() {
        return registryService;
    }

    BootstrapService getBootstrapService() {
        return bootstrapService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.server.ServerInterface#getManagementService()
     */
    public ManagementService getManagementService() {
        return managementService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.server.ServerInterface#getAddress()
     */
    public String getAddress() {
        return address.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.server.ServerInterface#getServiceNames()
     */
    public String[] getServiceNames() {
        return services.keySet().toArray(new String[0]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.server.ServerInterface#getHubs()
     */
    public String[] getHubs() {
        DirectSocketAddress[] hubs;
        if (hubOnly) {
            hubs = hub.knownHubs();
        } else {
            hubs = virtualSocketFactory.getKnownHubs();
        }

        ArrayList<String> result = new ArrayList<String>();
        if (hubs != null) {
            for (DirectSocketAddress hub : hubs) {
                result.add(hub.toString());
            }
        }

        return result.toArray(new String[0]);
    }

    /*
     * (non-Javadoc)
     * 
     * @seeibis.ipl.server.ServerInterface#addHubs(ibis.smartsockets.direct.
     * DirectSocketAddress)
     */
    public void addHubs(DirectSocketAddress... hubAddresses) {
        if (hubOnly) {
            hub.addHubs(hubAddresses);
        } else {
            virtualSocketFactory.addHubs(hubAddresses);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.server.ServerInterface#addHubs(java.lang.String)
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
            return "Hub running on " + getAddress();
        }

        String message = "Ibis server running on " + getAddress()
                + "\nList of Services:";

        for (Service service : services.values()) {
            message += "\n    " + service.toString();
        }

        return message;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.server.ServerInterface#end(long)
     */
    public void end(long timeout) {
        long deadline = System.currentTimeMillis() + timeout;

        if (timeout == 0) {
            deadline = Long.MAX_VALUE;
        } else if (timeout == -1) {
            deadline = 0;
        }

        connectionHandler.end();

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
                .println("--remote\t\t\tListen to commands for this server on stdin.");
        out.println();
        out.println("Output Options:");
        out.println("--events\t\t\tPrint events.");
        out
                .println("--errors\t\t\tPrint details of errors (such as stacktraces).");
        out.println("--stats\t\t\t\tPrint statistics once in a while.");
        out.println("--help | -h | /?\t\tThis message.");

    }

    private void waitUntilFinished() {
        try {
            int read = 0;

            while (read != -1) {
                read = System.in.read();
            }
        } catch (IOException e) {
            // IGNORE
        }
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
        TypedProperties properties = new TypedProperties();
        properties.putAll(System.getProperties());

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
            System.err.println("Could not start Server");
            t.printStackTrace();

            System.exit(1);
        }

        // register shutdown hook
        try {
            Runtime.getRuntime().addShutdownHook(new Shutdown(server));
        } catch (Exception e) {
            System.err.println("warning: could not registry shutdown hook");
        }

        if (server.hasRemote()) {
            System.out.println(ADDRESS_LINE_PREFIX + server.getAddress()
                    + ADDRESS_LINE_POSTFIX);
            System.out.flush();
            server.waitUntilFinished();
        } else {
            System.err.println(server.toString());
            String knownHubs = null;
            while (true) {
                String[] hubs = server.getHubs();
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
        server.end(-1);
    }

    public VirtualSocketFactory getSocketFactory() {
        return virtualSocketFactory;
    }

}
