/* $Id: PoolInfo.java 5075 2007-02-22 16:43:45Z ceriel $ */

package ibis.poolInfo;

import ibis.server.Client;
import ibis.smartsockets.direct.IPAddressSet;
import ibis.smartsockets.virtual.VirtualSocket;
import ibis.smartsockets.virtual.VirtualSocketAddress;
import ibis.smartsockets.virtual.VirtualSocketFactory;
import ibis.util.TypedProperties;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * The <code>PoolInfo</code> class provides a utility for finding out
 * information about the nodes involved in a closed-world run.
 * 
 * The <code>PoolInfo</code> class depends on the ibis.pool.size property,
 * which must contain the total number of hosts involved in the run. This
 * property can be set either as a system property, or in the ibis.properties
 * config file.
 */
public class PoolInfo {

    private static final Logger logger = Logger.getLogger(PoolInfo.class);

    public static final int CONNECTION_TIMEOUT = 120000;

    private final String poolName;

    private final int size;

    private final int rank;

    private final String[] clusters;

    private final IPAddressSet[] addresses;

    public PoolInfo(Properties properties, boolean addSystemProperties)
            throws Exception {
        TypedProperties typedProperties = PoolInfoProperties
                .getHardcodedProperties();

        if (addSystemProperties) {
            typedProperties.addProperties(System.getProperties());
        }

        typedProperties.addProperties(properties);

        try {
            size = typedProperties.getIntProperty(PoolInfoProperties.POOL_SIZE);
        } catch (NumberFormatException e) {
            throw new Exception("Cannot create PoolInfo, required property "
                    + PoolInfoProperties.POOL_SIZE + " not set or invalid");
        }

        poolName = typedProperties.getProperty(PoolInfoProperties.POOL_NAME);
        if (poolName == null) {
            throw new Exception("Cannot create PoolInfo, required property "
                    + PoolInfoProperties.POOL_NAME + " not set");
        }

        String cluster = typedProperties
                .getProperty(PoolInfoProperties.CLUSTER);
        
        IPAddressSet localAddress = IPAddressSet.getLocalHost();
        
        if (localAddress == null || localAddress.getAddresses().length == 0) {
            throw new UnknownHostException("could not determine local host address");
        }
        
        logger.debug("Creating PoolInfo for " + localAddress + "@" + cluster);

        VirtualSocketFactory factory = Client.getFactory(typedProperties);
        VirtualSocketAddress serviceAddress = Client.getServiceAddress(
                Service.VIRTUAL_PORT, typedProperties);

        VirtualSocket socket = factory.createClientSocket(serviceAddress, 
                CONNECTION_TIMEOUT, true, null);
        
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(
                socket.getOutputStream()));
        DataInputStream in = new DataInputStream(new BufferedInputStream(socket
                .getInputStream()));

        out.writeUTF(poolName);
        out.writeUTF(localAddress.toString());
        out.writeUTF(cluster);
        out.writeInt(size);

        out.flush();

        rank = in.readInt();

        if (rank == Service.RESULT_INVALID_SIZE) {
            throw new Exception("Server: invalid size: " + size);
        } else if (rank == Service.RESULT_POOL_CLOSED) {
            throw new Exception("Server: cannot join pool " + poolName
                    + " : pool already closed");
        } else if (rank == Service.RESULT_UNEQUAL_SIZE) {
            throw new Exception("Server: cannot join pool " + poolName
                    + " : pool exists with different size");
        } else if (rank < 0) {
            throw new Exception("Unknown result: " + rank);
        }

        addresses = new IPAddressSet[size];
        for (int i = 0; i < size; i++) {
            String addressString = in.readUTF(); 
            addresses[i] = IPAddressSet.getFromString(addressString);
        }
        clusters = new String[size];
        for (int i = 0; i < size; i++) {
            clusters[i] = in.readUTF();
        }
      
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            // IGNORE
        }
        
        logger.debug("PoolInfo created for " + localAddress + "@" + cluster + ", rank = " + rank);


    }

    /**
     * Name of the pool.
     */
    public String getPoolName() {
        return poolName;
    }

    /**
     * Returns the number of nodes in the pool.
     * 
     * @return the total number of nodes.
     */
    public int size() {
        return size;
    }

    /**
     * Returns the rank number in the pool of the current host.
     * 
     * @return the rank number.
     */
    public int rank() {
        return rank;
    }

    /**
     * Returns an IP address of the local machine.
     * 
     * @return an IP address of the local machine.
     */
    public InetAddress getInetAddress() {
        return addresses[rank].getAddresses()[0];
    }

    /**
     * Returns the IP address of the machine with the given rank.
     * 
     * @param rank the
     *            rank of the machine
     * @return the IP address of the machine.
     */
    public InetAddress getInetAddress(int rank) {
        return addresses[rank].getAddresses()[0];
    }

    
    /**
     * Returns all IP addresses of the local machine, as an IPAddressSet
     * 
     * @return all IP addresses of the local machine.
     */
    public IPAddressSet getIPAddressSet() {
        return addresses[rank];
    }

    /**
     * Returns all IP addresses of the machine with the given rank, as an
     * IPAddressSet.
     * 
     * @param rank the
     *            rank of the machine
     * @return all IP addresses of the machine.
     */
    public IPAddressSet getIPAddressSet(int rank) {
        return addresses[rank];
    }

    /**
     * Returns the cluster for the current host.
     * 
     * @return the cluster.
     */
    public String getCluster() {
        return clusters[rank];
    }

    /**
     * Returns the cluster for the host specified by the rank number.
     * 
     * @param rank
     *            the rank number.
     * @return the cluster name.
     */
    public String getCluster(int rank) {
        return clusters[rank];
    }

    /**
     * Returns a string representation of the information in this
     * <code>PoolInfo</code>.
     * 
     * @return a string representation.
     */
    public String toString() {
        String result =
                "pool info: size = " + size + "; my rank is " + rank
                        + "; host list:\n";
        for (int i = 0; i < addresses.length; i++) {
            result +=
                    i + ": address = " + addresses[i] + " cluster = "
                            + clusters[i] + "\n";
        }
        return result;
    }

    public static void main(String[] args) {
        PoolInfo info;
        try {
            info = new PoolInfo(null, true);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return;
        }

        System.err.println(info.toString());
    }
}
