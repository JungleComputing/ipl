package ibis.util;

import java.util.Properties;
import java.util.StringTokenizer;
import java.net.InetAddress;

public class PoolInfo {

	private static int total_hosts;
	private  static int host_number;
	private  static String [] host_names;
	private  static InetAddress [] hosts;

	static {
		String temp;
		
		Properties p = System.getProperties();
		
		total_hosts = getIntProperty(p, "ibis.pool.total_hosts");
		host_number = getIntProperty(p, "ibis.pool.host_number");
		
		temp = p.getProperty("ibis.pool.host_names");
		if(temp == null) {
			throw new RuntimeException("Property ibis.pool.host_names not set!");
		}
		
		if (host_number >= total_hosts || host_number < 0 || total_hosts < 1) {
			throw new RuntimeException("Sanity check on host numbers failed!");
		}
		
		host_names = new String[total_hosts];
		hosts      = new InetAddress[total_hosts];
		
		StringTokenizer tok = new StringTokenizer(temp, " ", false);
		
		for (int i=0;i<total_hosts;i++) {
			
			String t = tok.nextToken();       
			
			try {
				/*
				  This looks weird, but is required to get the entire hostname
				  ie. 'java.sun.com' instead of just 'java'.
				*/
				
				InetAddress adres = InetAddress.getByName(t);
				adres             = InetAddress.getByName(adres.getHostAddress());
				host_names[i]     = adres.getHostName();
				if (! host_names[i].equals(t) &&
				    host_names[i].toUpperCase().equals(t.toUpperCase())) {
				    System.err.println("This is probably M$ Windows. Restore lower case in host name " + t);
				    host_names[i] = t;
				}
				hosts[i]          = adres;
				
			} catch (Exception e) {
				throw new RuntimeException("Could not find host name " + t);
			}		       			
		}
	}

	public static int size() {
		return total_hosts;
	}

	public static int rank() {
		return host_number;
	}

	public static String hostName() {
		return host_names[host_number];
	}

	public static String hostName(int rank) {
		return host_names[rank];
	}

	public static InetAddress hostAddress(int rank) {
		return hosts[rank];
	}

	public static InetAddress hostAddress() {
		return hosts[host_number];
	}

	public static InetAddress[] hostAddresses() {
		return hosts;
	}

	public static String[] hostNames() {
		return host_names;
	}

	private static int getIntProperty(Properties p, String name) throws RuntimeException {

		String temp = p.getProperty(name);
		
		if (temp == null) { 
			throw new RuntimeException("Property " + name + " not found !");
		}
		
		return Integer.parseInt(temp);
	}	


    public static void printTime(String id, long time) {
	System.out.println("Application: " + id + "; Ncpus: " + total_hosts +
			   "; time: " + time/1000.0 + " seconds\n");
    }

}
