package ibis.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Some utilities that deal with IP addresses.
 */
public class IPUtils {
    private static final String prefix = "ibis.util.ip.";

    private static final String dbg = prefix + "debug";

    private static final String addr = prefix + "address";

    private static final String alt_addr = prefix + "alt-address";

    private static final String[] sysprops = { dbg, addr, alt_addr };

    static {
        TypedProperties.checkProperties(prefix, sysprops, null);
    }

    private static final boolean DEBUG = TypedProperties.booleanProperty(dbg);

    private static InetAddress localaddress = null;

    private static InetAddress alt_localaddress = null;

    private static InetAddress detected = null;

    private IPUtils() {
        /* do nothing */
    }

    /**
     * Returns true if the specified address is an external address.
     * External means not a site local, link local, or loopback address.
     * @param address the specified address.
     * @return <code>true</code> if <code>addr</code> is an external address.
     */
    public static boolean isExternalAddress(InetAddress address) {
        if (address.isLoopbackAddress()) {
            return false;
        }
        if (address.isLinkLocalAddress()) {
            return false;
        }
        if (address.isSiteLocalAddress()) {
            return false;
        }

        return true;
    }

    /**
     * Returns the {@link java.net.InetAddress} associated with the
     * local host. If the ibis.util.ip.address property is specified
     * and set to a specific IP address, that address is used.
     */
    public static InetAddress getLocalHostAddress() {
        if (localaddress == null) {
            localaddress = doWorkGetLocalHostAddress(false);

            // To make sure that a hostname is filled in:
            localaddress.getHostName();

            if (DEBUG) {
                System.err.println("Found address: " + localaddress);
            }
        }
        return localaddress;
    }

    /**
     * Returns the {@link java.net.InetAddress} associated with the
     * local host. If the ibis.util.ip.alt_address property is specified
     * and set to a specific IP address, that address is used.
     */
    public static InetAddress getAlternateLocalHostAddress() {
        if (alt_localaddress == null) {
            alt_localaddress = doWorkGetLocalHostAddress(true);

            // To make sure that a hostname is filled in:
            alt_localaddress.getHostName();

            if (DEBUG) {
                System.err.println("Found alt address: " + alt_localaddress);
            }
        }
        return alt_localaddress;
    }

    private static InetAddress doWorkGetLocalHostAddress(boolean alt) {
        InetAddress external = null;
        InetAddress internal = null;
        Properties p = System.getProperties();

        if (alt) {
            String myIp = p.getProperty(alt_addr);
            if (myIp != null) {
                try {
                    external = InetAddress.getByName(myIp);
                    System.err.println("Specified alt ip addr " + external);
                    return external;
                } catch (java.net.UnknownHostException e) {
                    System.err.println("IP addres property specified, "
                            + "but could not resolve it");
                }
            }
        } else {
            String myIp = p.getProperty(addr);
            if (myIp != null) {
                try {
                    external = InetAddress.getByName(myIp);
                    if (DEBUG) {
                        System.err.println("Found specified address "
                                + external);
                    }
                    return external;
                } catch (java.net.UnknownHostException e) {
                    System.err.println("IP addres property specified, "
                            + "but could not resolve it");
                }
            }

            // backwards compatibility
            myIp = p.getProperty("ibis.ip.address");
            if (myIp != null) {
                try {
                    external = InetAddress.getByName(myIp);
                    return external;
                } catch (java.net.UnknownHostException e) {
                    System.err.println("IP addres property specified, "
                            + "but could not resolve it");
                }
            }

            // backwards compatibility
            myIp = p.getProperty("ip_address");
            if (myIp != null) {
                try {
                    external = InetAddress.getByName(myIp);
                    return external;
                } catch (java.net.UnknownHostException e) {
                    System.err.println("IP addres property specified, "
                            + "but could not resolve it");
                }
            }
        }

        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            InetAddress[] all = InetAddress.getAllByName(hostname);
            if (all != null) {
                for (int i = 0; i < all.length; i++) {
                    if (isExternalAddress(all[i])) {
                        external = all[i];
                        if (DEBUG) {
                            System.err.println("trying address: " + external
                                    + " EXTERNAL");
                        }
                        break;
                    }
                }
            }
        } catch (java.net.UnknownHostException e) {
            if (DEBUG) {
                System.err.println("InetAddress.getLocalHost().getHostName() "
                        + "failed");
            }
        }

        if (detected != null) {
            return detected;
        }

        Enumeration e = null;
        try {
            e = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException ex) {
            if (DEBUG) {
                System.err.println("Could not get network interfaces. "
                        + "Trying local.");
            }
        }
        boolean first = true;
        /*
         * Preference order:
         * 1. external and IPv4.
         * 2. external
         * 3. sitelocal and IPv4
         * 4. sitelocal
         * 5. Ipv4
         * 6. other
         */
        if (e != null) {
            for (; e.hasMoreElements();) {
                NetworkInterface nw = (NetworkInterface) e.nextElement();

                for (Enumeration e2 = nw.getInetAddresses();
                        e2.hasMoreElements();) {
                    InetAddress address = (InetAddress) e2.nextElement();
                    if (DEBUG) {
                        System.err.println("trying address: "
                                + address
                                + (isExternalAddress(address) ? " EXTERNAL"
                                        : " LOCAL"));
                    }
                    if (isExternalAddress(address)) {
                        if (external == null) {
                            external = address;
                        } else if (external.equals(address)) {
                            // OK
                        } else if (!(external instanceof Inet4Address)
                                && address instanceof Inet4Address) {
                            // Preference for IPv4
                            external = address;
                        } else if (DEBUG || (address instanceof Inet4Address)) {
                            if (first) {
                                first = false;
                                System.err.println("WARNING, this machine has "
                                        + "more than one external "
                                        + "IP address, using "
                                        + external);
                                System.err.println("  but found " + address
                                        + " as well");
                            } else {
                                System.err.println("  ... and found " + address
                                        + " as well");
                            }
                        }
                    } else if (internal == null) {
                        internal = address;
                    } else if (address.isSiteLocalAddress()) {
                        if (!internal.isSiteLocalAddress()
                                || !(internal instanceof Inet4Address)) {
                            internal = address;
                        }
                    } else {
                        if (!internal.isSiteLocalAddress()
                                && !(internal instanceof Inet4Address)) {
                            internal = address;
                        }
                    }
                }
            }
        }

        if (external == null) {
            external = internal;
            if (external == null) {
                try {
                    InetAddress a = InetAddress.getLocalHost();
                    if (a == null) {
                        System.err.println("Could not find local IP address, "
                                + "you should specify the "
                                + "-Dibis.util.ip.address=A.B.C.D option");
                        return null;
                    }
                    String name = a.getHostName();
                    external = InetAddress.getByName(
                            InetAddress.getByName(name).getHostAddress());
                } catch (java.net.UnknownHostException ex) {
                    System.err.println("Could not find local IP address, you "
                            + "should specify the "
                            + "-Dibis.util.ip.address=A.B.C.D option");
                    return null;
                }
            }
        }

        detected = external;

        return external;
    }
}
