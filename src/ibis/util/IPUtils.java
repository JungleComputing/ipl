package ibis.util;

import java.net.InetAddress;
import java.util.Properties;
import java.net.UnknownHostException;
import java.net.Socket;

public class IPUtils {

	static final boolean DEBUG = false;
	
	public static boolean isLoopbackAddress(InetAddress addr) {
		byte[] a = addr.getAddress();

		if(a.length != 4) {
			System.err.println("WARNING: IPUtils: this only works for IP v 4 addresses");
			return false;
		}

		return a[0] == 127;
	}

	public static boolean isLinkLocalAddress(InetAddress addr) {
		byte[] a = addr.getAddress();

		if(a.length != 4) {
			System.err.println("WARNING: IPUtils: this only works for IP v 4 addresses");
			return false;
		}

		long address = a[0] << 24 | a[1] << 16 | 
			a[2] << 8 | a[0];

		return (address >>> 24 & 0xff) == 169 && 
			(address >>> 16 & 0xff) == 254;
	}

	public static boolean isSiteLocalAddress(InetAddress addr) {
		byte[] a = addr.getAddress();

		if(a.length != 4) {
			System.err.println("WARNING: IPUtils: this only works for IP v 4 addresses");
			return false;
		}

		long address = a[0] << 24 | a[1] << 16 | 
			a[2] << 8 | a[0];

		return (address >>> 24 & 0xff) == 10 || 
			(address >>> 24 & 0xff) == 172 && (address >>> 16 & 0xf0) == 16 || 
			(address >>> 24 & 0xff) == 192 && (address >>> 16 & 0xff) == 168;
	}

	public static boolean isExternalAddress(InetAddress addr) {
		if(isLoopbackAddress(addr)) return false;
		if(isSiteLocalAddress(addr)) return false;
		if(isLinkLocalAddress(addr)) return false;

		return true;
	}
	
	public static InetAddress getLocalHostAddress() {
		InetAddress external = null;
		InetAddress[] all = null;

		Properties p = System.getProperties();

		String myIp = p.getProperty("ibis.ip.address");
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

		try {
			String hostname = InetAddress.getLocalHost().getHostName();
			all = InetAddress.getAllByName(hostname);
		} catch (java.net.UnknownHostException e) {
			System.err.println("IP addres property specified, but could not resolve it");
		}

		if(all != null) {
			for(int i=0; i<all.length; i++) {
				if(DEBUG) {
					System.err.println("trying address: " + all[i] +
							   (isExternalAddress(all[i]) ? " EXTERNAL" : " LOCAL"));
				}
				if(isExternalAddress(all[i])) {
					if(external == null) {
						external = all[i];
					} else {
						System.err.println("WARNING, this machine has more than one external " +
								   "IP address, using " +
								   external);
						return external;
					}
				}
			}
		}

		if(external == null) {
			try {
				InetAddress a = InetAddress.getLocalHost();
				if(a == null) {
					System.err.println("Could not find local IP address, you should specify the -Dibis.ip.address=A.B.C.D option");
					return null;
				}
				String name = a.getHostName();
				external = InetAddress.getByName(InetAddress.getByName(name).getHostAddress());
			} catch (java.net.UnknownHostException e) {
				System.err.println("Could not find local IP address, you should specify the -Dibis.ip.address=A.B.C.D option");
				return null;
			}
		}

		return external;
	}
}
