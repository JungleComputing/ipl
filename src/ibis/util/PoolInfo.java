package ibis.util;

import ibis.ipl.IbisException;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Properties;
import java.util.StringTokenizer;

public class PoolInfo {

	int total_hosts;
	int host_number;
	String [] host_names;
	InetAddress [] hosts;

	public PoolInfo() throws IbisException {
		this(false);
	}

	public PoolInfo(boolean forceSequential) throws IbisException {
		if (forceSequential) {
			sequentialPool();
		} else {
			propertiesPool();
		}
	}


	private void sequentialPool() {
		String temp;
		
		total_hosts = 1;
		host_number = 0;
		
		host_names = new String[total_hosts];
		hosts      = new InetAddress[total_hosts];
				
		try {
			InetAddress adres = InetAddress.getLocalHost();
			adres             = InetAddress.getByName(adres.getHostAddress());
			host_names[host_number] = adres.getHostName();
			hosts[host_number]      = adres;
			
		} catch (Exception e) {
			throw new Error("Could not find my host name");
		}		       			
	}


	private void propertiesPool() throws IbisException {
		String ibisHostNames;
		
		Properties p = System.getProperties();
		
		total_hosts = getIntProperty(p, "ibis.pool.total_hosts");
		try {
		    host_number = getIntProperty(p, "ibis.pool.host_number");
		} catch (IbisException e) {
		    host_number = -1;
		}
		
		ibisHostNames = p.getProperty("ibis.pool.host_names");
		if(ibisHostNames == null) {
			throw new IbisException("Property ibis.pool.host_names not set!");
		}

		host_names = new String[total_hosts];
		hosts      = new InetAddress[total_hosts];
		
		StringTokenizer tok = new StringTokenizer(ibisHostNames, " ", false);
		
		String my_hostname;
		try {
		    my_hostname = InetAddress.getLocalHost().getHostName();
		} catch (java.net.UnknownHostException e) {
		    my_hostname = null;
		}
		int match = 0;
		int my_host = -1;
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
					System.err.println("This is probably M$ Windows. Restored lower case in host name " + t);
					host_names[i] = t;
				}
				hosts[i]          = adres;

				if (host_number == -1) {
				    if (host_names[i].equals(my_hostname)) {
					match++;
					my_host = i;
				    }
				}
				
			} catch (IOException e) {
				throw new IbisException("Could not find host name " + t);
			}		       			
		}

		if (host_number == -1 && match == 1) {
		    host_number = my_host;
System.err.println("Phew... found a host number " + my_host + " for " + my_hostname);
		}

		if (host_number >= total_hosts || host_number < 0 || total_hosts < 1) {
			throw new IbisException("Sanity check on host numbers failed!");
		}
	}

	public int size() {
		return total_hosts;
	}

	public int rank() {
		return host_number;
	}

	public String hostName() {
		return host_names[host_number];
	}

	public String hostName(int rank) {
		return host_names[rank];
	}

	public InetAddress hostAddress(int rank) {
		return hosts[rank];
	}

	public InetAddress hostAddress() {
		return hosts[host_number];
	}

	public InetAddress[] hostAddresses() {
		return (InetAddress[]) hosts.clone();
	}

	public String[] hostNames() {
		return (String[]) host_names.clone();
	}

	private static int getIntProperty(Properties p, String name) throws IbisException {

		String temp = p.getProperty(name);
		
		if (temp == null) { 
			throw new IbisException("Property " + name + " not found !");
		}
		
		return Integer.parseInt(temp);
	}	

	public void printTime(String id, long time) {
		System.out.println("Application: " + id + "; Ncpus: " + total_hosts +
				   "; time: " + time/1000.0 + " seconds\n");
	}
}
