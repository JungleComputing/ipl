package ibis.util;

import java.net.InetAddress;

public class IPUtils {

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
	
}
