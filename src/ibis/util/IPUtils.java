package ibis.util;

import java.net.InetAddress;
import java.net.Inet4Address;
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

    private static final String[] sysprops = {
	dbg,
	addr,
	alt_addr
    };

    static {
	TypedProperties.checkProperties(prefix, sysprops, null);
    }

    private static final boolean DEBUG = TypedProperties.booleanProperty(dbg);

    private static InetAddress localaddress = null;
    private static InetAddress alt_localaddress = null;

    private IPUtils() {
    	/* do nothing */
    }

    /**
     * Returns true if the specified address is an external address.
     * External means not a site local, link local, or loopback address.
     * @param addr the specified address.
     * @return <code>true</code> if <code>addr</code> is an external address.
     */
    public static boolean isExternalAddress(InetAddress addr) {
	if(addr.isLoopbackAddress()) return false;
	if(addr.isLinkLocalAddress()) return false;
	if(addr.isSiteLocalAddress()) return false;

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
	InetAddress[] all = null;

	Properties p = System.getProperties();

	if (alt) {
	    String myIp = p.getProperty(alt_addr);
	    if (myIp != null) {
		try {
		    external = InetAddress.getByName(myIp);
System.err.println("Specified alt ip addr " + external);
		    return external;
		} catch (java.net.UnknownHostException e) {
		    System.err.println("IP addres property specified, but could not resolve it");
		}
	    }
	}
	else {
	    String myIp = p.getProperty(addr);
	    if (myIp != null) {
		try {
		    external = InetAddress.getByName(myIp);
		    if (DEBUG) {
			System.err.println("Found specified address " + external);
		    }
		    return external;
		} catch (java.net.UnknownHostException e) {
		    System.err.println("IP addres property specified, but could not resolve it");
		}
	    }

	    // backwards compatibility
	    myIp = p.getProperty("ibis.ip.address");
	    if (myIp != null) {
		try {
		    external = InetAddress.getByName(myIp);
		    return external;
		} catch (java.net.UnknownHostException e) {
		    System.err.println("IP addres property specified, but could not resolve it");
		}
	    }

	    // backwards compatibility
	    myIp = p.getProperty("ip_address");
	    if (myIp != null) {
		try {
		    external = InetAddress.getByName(myIp);
		    return external;
		} catch (java.net.UnknownHostException e) {
		    System.err.println("IP addres property specified, but could not resolve it");
		}
	    }
	}

	Enumeration e = null;
	try {
	    e = NetworkInterface.getNetworkInterfaces();
	} catch(SocketException ex) {
	    System.err.println("Could not get network interfaces. Trying local.");
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

		for (Enumeration e2 = nw.getInetAddresses(); e2.hasMoreElements();) {
		    InetAddress addr = (InetAddress) e2.nextElement();
		    if(DEBUG) {
			System.err.println("trying address: " + addr +
				(isExternalAddress(addr) ? " EXTERNAL" : " LOCAL"));
		    }
		    if(isExternalAddress(addr)) {
			if(external == null) {
			    external = addr;
			} else if (! (external instanceof Inet4Address) &&
				   addr instanceof Inet4Address) {
			    // Preference for IPv4
			    external = addr;
			} else {
			    if (first) {
				first = false;
				System.err.println("WARNING, this machine has more than one external " +
				    "IP address, using " +
				    external);
				System.err.println("  but found " + addr + " as well");
			    } else {
				System.err.println("  ... and found " + addr + " as well");
			    }
			}
		    } else if (internal == null) {
			internal = addr;
		    } else if (addr.isSiteLocalAddress()) {
			if (! internal.isSiteLocalAddress() || ! (internal instanceof Inet4Address)) {
			    internal = addr;
			}
		    } else {
			if (! internal.isSiteLocalAddress() && ! (internal instanceof Inet4Address)) {
			    internal = addr;
			}
		    }
		}
	    }
	}

	if(external == null) {
	    external = internal;
	    if (external == null) {
		try {
		    InetAddress a = InetAddress.getLocalHost();
		    if(a == null) {
			System.err.println("Could not find local IP address, you should specify the -Dibis.util.ip.address=A.B.C.D option");
			return null;
		    }
		    String name = a.getHostName();
		    external = InetAddress.getByName(InetAddress.getByName(name).getHostAddress());
		} catch (java.net.UnknownHostException ex) {
		    System.err.println("Could not find local IP address, you should specify the -Dibis.util.ip.address=A.B.C.D option");
		    return null;
		}
	    }
	}

	return external;
    }
}
