package ibis.util;

import ibis.ipl.IbisException;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.NoSuchElementException;

/**
 * The <code>PoolInfo</code> class provides a utility for finding out
 * information about the nodes involved in a closed-world run.
 * This class can be used when the names of the nodes involved in the
 * run are known in advance, and can be given in the
 * <code>ibis.pool.host_names</code> property.
 * If these names are not known in advance, a {@link ibis.util.PoolInfoClient
 * PoolInfoClient} can be used instead. The
 * {@link ibis.util.PoolInfo#createPoolInfo createPoolInfo} static method
 * can be used to take care of this automatically.
 * <br>
 * The <code>PoolInfo</code> class depends on the following system properties:
 * <br>
 * <pre>ibis.pool.total_hosts</pre>
 * must contain the total number of hosts involved in the run.
 * <br>
 * <pre>ibis.pool.host_names</pre>
 * must contain a space-separated list of hostnames.
 * The number of hostnames in the list must at least be equal to
 * the number of hosts involved in the run as given by the
 * <code>ibis.pool.total_hosts</code> property. Any additional host names
 * are ignored.
 * <br>
 * <pre>ibis.pool.host_number</pre>
 * optional, gives the index of the current host in the list of host names.
 * Should be between 0 and <code>ibis.pool.total_hosts</code> (inclusive).
 * If not supplied, it is determined by looking up the current host in
 * the list of host names.
 */
public class PoolInfo {

	int total_hosts;
	int host_number;
	String [] host_names;
	InetAddress [] hosts;
	private String clusterName;

	/**
	 * Constructs a <code>PoolInfo</code> object.
	 * @exception IbisException is thrown when something is wrong.
	 */
	public PoolInfo() throws IbisException {
		this(false);
	}

	/**
	 * Constructs a <code>PoolInfo</code> object.
	 * @param forceSequential when set to <code>true</code>,
	 * a sequential pool is created, with only the current node as
	 * member. The system properties are ignored.
	 * @exception IbisException is thrown when something is wrong.
	 */
	public PoolInfo(boolean forceSequential) throws IbisException {
		clusterName = TypedProperties.stringPropertyValue("ibis.pool.cluster");
		if (clusterName == null) {
		    clusterName = "unknown";
		}
		if (forceSequential) {
			sequentialPool();
		} else {
			propertiesPool();
		}
	}

	/**
	 * Constructor for subclasses.
	 */
	protected PoolInfo(int dummy) {
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
		// System.err.println(my_hostname + ": I see rank \"" + p.getProperty("ibis.pool.rank") + "\"");
		// System.err.println(my_hostname + ": I see host_names \"" + ibisHostNames+ "\"");
		int match = 0;
		int my_host = -1;
		for (int i=0;i<total_hosts;i++) {
			
			String t;
			try {
			    t = tok.nextToken();       
			} catch (NoSuchElementException e) {
			    throw new IbisException("Not enough hostnames in ibis.pool.host_names!");
			}
			
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

	/**
	 * Returns the number of nodes in the pool.
	 * @return the total number of nodes.
	 */
	public int size() {
		return total_hosts;
	}

	/**
	 * Returns the rank number in the pool of the current host.
	 * @return the rank number.
	 */
	public int rank() {
		return host_number;
	}

	/**
	 * Returns the name of the current host.
	 * @return the name of the current host.
	 */
	public String hostName() {
		return host_names[host_number];
	}
    
	/**
	 * Returns the cluster name for the current host.
	 * @return the cluster name.
	 */
	public String clusterName() {
		return clusterName;
	}

	/**
	 * Returns the cluster name for the host specified by the rank number.
	 * @param rank the rank number.
	 * @return the cluster name.
	 */
	public String clusterName(int rank) {
		return clusterName;
	}

	/**
	 * Returns an array of cluster names, one for each host involved in
	 * the run.
	 * @return the cluster names
	 */
	public String[] clusterNames() {
		String[] r = new String[total_hosts];
		for (int i = 0; i < total_hosts; i++) {
			r[i] = clusterName;
		}
		return r;
	}

	/**
	 * Returns the name of the host with the given rank.
	 * @param rank the rank number.
	 * @return the name of the host with the given rank.
	 */
	public String hostName(int rank) {
		return host_names[rank];
	}

	/**
	 * Returns the <code>InetAddress</code> of the host with the given
	 * rank.
	 * @param rank the rank number.
	 * @return the <code>InetAddress</code> of the host with the given
	 * rank.
	 */
	public InetAddress hostAddress(int rank) {
		return hosts[rank];
	}

	/**
	 * Returns the <code>InetAddress</code> of the current host.
	 * @return the <code>InetAddress</code> of the current host.
	 */
	public InetAddress hostAddress() {
		return hosts[host_number];
	}

	/**
	 * Returns an array of <code>InetAddress</code>es of the hosts.
	 * @return an array of <code>InetAddress</code>es of the hosts.
	 */
	public InetAddress[] hostAddresses() {
		return (InetAddress[]) hosts.clone();
	}

	/**
	 * Returns an array of hostnames of the hosts.
	 * @return an array of hostnames of the hosts.
	 */
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

	/**
	 * Utility method to print the time used in a uniform format.
	 * @param id name of the application
	 * @param time the time used, in milliseconds.
	 */
	public void printTime(String id, long time) {
		System.out.println("Application: " + id + "; Ncpus: " + total_hosts +
				   "; time: " + time/1000.0 + " seconds\n");
	}

	/**
	 * Creates and returns a <code>PoolInfo</code>.
	 * The parameter indicates wether a pool for a sequential run
	 * must be created. If not, if the system property
	 * <code>ibis.pool.host_names</code> is set, a <code>PoolInfo</code>
	 * is created. If not, a {@link ibis.util.PoolInfoClient PoolInfoClient}
	 * is created.
	 * @param forceSeq indicates wether a pool for a sequential run must
	 * be created.
	 * @return the resulting <code>PoolInfo</code> object.
	 * @exception IbisException is thrown when something is wrong.
	 */
	public static PoolInfo createPoolInfo(boolean forceSeq)
			throws IbisException
	{
		if (forceSeq) {
			return new PoolInfo(true);
		}
		if (TypedProperties.stringPropertyValue("ibis.pool.host_names") != null) {
			return new PoolInfo();
		}
		try {
			return PoolInfoClient.create();
		} catch(Throwable e) {
			throw new IbisException("Got exception", e);
		}
	}

	/**
	 * Creates and returns a <code>PoolInfo</code>.
	 * If the system property <code>ibis.pool.host_names</code> is set,
	 * a <code>PoolInfo</code> is created.
	 * If not, a {@link ibis.util.PoolInfoClient PoolInfoClient}
	 * is created.
	 * @return the resulting <code>PoolInfo</code> object.
	 * @exception IbisException is thrown when something is wrong.
	 */
	public static PoolInfo createPoolInfo() throws IbisException {
		return createPoolInfo(false);
	}

	/**
	 * Returns a string representation of the information in this
	 * <code>PoolInfo</code>.
	 * @return a string representation.
	 */
	public String toString() {
	    String result = "pool info: size = " + total_hosts +
		"; my rank is " + host_number + "; host list:\n";
	    for (int i = 0; i < total_hosts; i++) {
		result += i + ": address= " + hosts[i] + 
		    " cluster=" + clusterName + "\n";
	    }
	    return result;
	}
}
