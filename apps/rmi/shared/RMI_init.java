
import java.rmi.Remote;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.server.RMISocketFactory;
import java.rmi.RemoteException;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import java.io.IOException;
import java.net.InetAddress;

import ibis.util.IPUtils;
import ibis.util.TypedProperties;

public class RMI_init {

    private final static boolean VERBOSE = TypedProperties.booleanProperty(
            "RMI_init.verbose", false);

    private final static boolean USE_IP_MAP_FACTORY = TypedProperties
            .booleanProperty("RMI_init.factory", true);

    private final static String FACTORY = TypedProperties.stringProperty(
            "RMI_init.factory.name", "IPMapSocketFactory");

    static Registry reg = null;

    static boolean isInitialized = false;

    private static RMISocketFactory socketFactory = null;

    static {
        if (USE_IP_MAP_FACTORY) {
            try {
                RMISocketFactory f = RMISocketFactory.getSocketFactory();
                if (f == null) {
                    Class c = Class.forName(FACTORY);
                    f = (RMISocketFactory) c.newInstance();
                    try {
                        RMISocketFactory.setSocketFactory(f);
                        if (VERBOSE) {
                            System.out.println("Installed RMISocketFactory "
                                    + f);
                        }
                        socketFactory = f;
                    } catch (IOException e) {
                        System.err.println("Cannot set RMISocketFactory " + f
                                + "; use default");
                    }
                } else {
                    System.err
                            .println("Cannot set RMISocketFactory: already in use");
                }
            } catch (ClassNotFoundException e) {
                System.err.println("Cannot instantiate class \"" + FACTORY
                        + "\"");
            } catch (InstantiationException e) {
                System.err.println("Cannot instantiate class \"" + FACTORY
                        + "\"");
            } catch (IllegalAccessException e) {
                System.err.println("Cannot instantiate class \"" + FACTORY
                        + "\"");
            } catch (IllegalArgumentException e) {
                // This must be Ibis RMI. Silently ignore.
                if (VERBOSE) {
                    System.out
                            .println("Ibis RMI does not support RMISocketFactory. Use the default Ibis SocketFactory.");
                }
            }
        }
    }

    public static Registry getRegistry(String registryOwner) throws IOException {

        // System.out.println("In RMI_init.getRegistry");

        if (isInitialized) {
            System.out.println("Registry already initialized");
            return reg;
        }
        isInitialized = true;

        String version = System.getProperty("java.version");
        if (version == null || version.startsWith("1.1")) {
            // Start a security manager.
            if (VERBOSE) {
                System.out.println("Start a security manager");
            }
            System.setSecurityManager(new RMISecurityManager());
        }

        int port = Registry.REGISTRY_PORT;

        InetAddress addr = IPUtils.getLocalHostAddress();
        InetAddress regAddr = InetAddress.getByName(registryOwner);

        if (addr.equals(regAddr)) {
            while (reg == null) {
                try {
                    if (socketFactory != null) {
                        reg = LocateRegistry.createRegistry(port,
                                socketFactory, socketFactory);
                    } else {
                        reg = LocateRegistry.createRegistry(port);
                    }
                    if (VERBOSE) {
                        System.out
                                .println("Use LocateRegistry to create a Registry, socketFactory "
                                        + socketFactory);
                    }
                    break;
                } catch (RemoteException e) {
                }
                try {
                    if (socketFactory != null) {
                        reg = LocateRegistry.getRegistry(registryOwner, port,
                                socketFactory);
                    } else {
                        reg = LocateRegistry.getRegistry(registryOwner, port);
                    }
                    if (VERBOSE) {
                        System.out
                                .println("Use LocateRegistry to get a Registry from owner "
                                        + regAddr
                                        + ", socketFactory "
                                        + socketFactory);
                    }
                    break;
                } catch (RemoteException e) {
                }
            }

        } else {
            if (VERBOSE) {
                System.out
                        .println("Use LocateRegistry to get a Registry from owner "
                                + regAddr + ", socketFactory " + socketFactory);
            }
            while (reg == null) {
                try {
                    if (socketFactory != null) {
                        reg = LocateRegistry.getRegistry(registryOwner, port,
                                socketFactory);
                    } else {
                        reg = LocateRegistry.getRegistry(registryOwner, port);
                    }
                } catch (RemoteException e) {
                    try {
                        System.out.println("Look up registry: sleep a while..");
                        Thread.sleep(100);
                    } catch (InterruptedException eI) {
                    }
                }
            }
        }

        // System.out.println("Registry is " + reg);

        return reg;
    }

    /**
     * A wrapper for the RMI nameserver lookup
     */
    public static Remote lookup(String id) throws IOException {
        int wait = 100;

        while (true) {
            if (wait < 6000) {
                wait *= 2;
            }
            try {
                return Naming.lookup(id);

            } catch (java.rmi.NotBoundException eR) {
                if (VERBOSE) {
                    System.out.println("Look up object " + id
                            + ": sleep a while.. " + eR);
                }
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException e2) {
                    // ignore
                }

            } catch (java.rmi.ConnectException eR) {
                if (VERBOSE) {
                    System.out.println("Look up object " + id
                            + ": sleep a while.. " + eR);
                }
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException e2) {
                    // ignore
                }
            }
        }
    }

    /**
     * A wrapper for the RMI nameserver bind
     */
    public static void bind(String name, Remote obj) throws IOException,
            java.rmi.AlreadyBoundException {
        Naming.bind(name, obj);
    }

    /**
     * A wrapper for the RMI nameserver unbind
     */
    public static void unbind(String name) throws IOException,
            java.rmi.NotBoundException {
        Naming.unbind(name);
    }

    /**
     * A wrapper for the RMI nameserver rebind
     */
    public static void rebind(String name, Remote obj) throws IOException {
        Naming.rebind(name, obj);
    }

    public static void main(String[] args) {
        System.out.println("Got registry:");
        try {
            System.out.println(getRegistry(args[0]));
        } catch (IOException e) {
            System.out.println("The bugger threw an exception: " + e);
        }
    }
}