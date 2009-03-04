/* $Id$ */

package ibis.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;

/**
 * Some utilities that deal with IP addresses. Shamelessly copied from
 * smartsockets.direct.IPAddressSet et al.
 */
public class IPUtils {

    /**
     * Inner class capable of sorting addresses
     */
    private static class AddressSorter implements Comparator<InetAddress>, java.io.Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * Orders two addresses based on their length. If both addresses have
         * the same length, the addresses are ordered based on comparing their
         * raw bytes values.
         * 
         * @param i1
         *            an InetAddress
         * @param i2
         *            an InetAddress
         * @return int value indicating the order of the addresses
         */
        private int compareAddress(InetAddress i1, InetAddress i2) {

            // We have a preference for the shortest address (i.e., IPv4)
            byte[] tmp1 = i1.getAddress();
            byte[] tmp2 = i2.getAddress();

            if (tmp1.length != tmp2.length) {
                return tmp2.length - tmp1.length;
            }

            for (int i = 0; i < tmp1.length; i++) {
                if (tmp1[i] != tmp2[i]) {

                    int t1 = tmp1[i] & 0xFF;
                    int t2 = tmp2[i] & 0xFF;
                    return t2 - t1;
                }
            }

            return 0;
        }

        /**
         * Gives the address a score based on the class it belongs to. The more
         * general the class, the lower the score.
         * 
         * @param ina
         *            the address to score
         * @return score given to the address
         */
        private int score(InetAddress ina) {

            if (ina.isLoopbackAddress()) {
                return 8;
            }

            if (ina.isLinkLocalAddress()) {
                return 6;
            }

            if (ina.isSiteLocalAddress()) {
                return 4;
            }

            // It's a 'normal' global IP
            return 2;
        }

        /**
         * This compares two InetAddresses.
         * 
         * Both parameters should either be an InetAddress or a
         * InetSocketAddress. It starts by putting the addresses in one of the
         * following classes:
         * 
         * 1. Global 2. Site Local 3. Link Local 4. Loopback
         * 
         * When the addresses end up in the same class, they are sorted by
         * length. (shortest first, so IPv4 is preferred over IPv6). If their
         * length is the same, the individual bytes are compared. The address
         * with the lowest byte values comes first.
         */
        public int compare(InetAddress i1, InetAddress i2) {

            int score1 = score(i1);
            int score2 = score(i2);

            int result = 0;

            if (score1 == score2) {
                result = compareAddress(i1, i2);
            } else {
                result = score1 - score2;
            }
            return result;
        }
    }// end of inner AddressSorter class

    // list of all addresses of this machine.
    private static InetAddress[] addresses;

    /**
     * Returns a sorted list of all local addresses of this machine. The list is
     * sorted as follows:
     * <ul>
     * <li>Global Addresses</li>
     * <li>Site local Addresses</li>
     * <li>Link local Addresses</li>
     * <li>Loopback addresses</li>
     * </ul>
     * 
     * If a global, site or link local address is present the loopback address
     * is ommited from the result.
     * 
     * @return a sorted list of all local addresses of this machine.
     * @throws UnknownHostException
     *             in case no address could be found.
     */
    public static synchronized InetAddress[] getLocalHostAddresses()
            throws UnknownHostException {

        if (addresses == null) {
            // Get all the local addresses, including IPv6 ones, but excluding
            // loopback addresses, and sort them.
            // TODO: removed ipv6 for now!!
            addresses = getAllHostAddresses(true, true);

            if (addresses == null || addresses.length == 0) {
                // Oh dear, we don't have a network... Let's see if there is
                // a loopback available...
                // TODO: removed ipv6 for now!!
                addresses = getAllHostAddresses(false, true);
            }

            if (addresses == null || addresses.length == 0) {
                throw new UnknownHostException(
                        "could not determine addresseses for localhost");
            }

            Arrays.sort(addresses, new AddressSorter());
        }

        return addresses.clone();
    }

    /**
     * Returns the "most public" InetAddress of this machine.
     * 
     * @return the "most public" InetAddress of this machine.
     * @throws UnknownHostException
     *             in case no address could be found.
     */
    public static InetAddress getLocalHostAddress() throws UnknownHostException {
        // Trigger allocation of addresses if null, throws UnknownHostException
        // if unsuccesful...
        InetAddress[] addresses = getLocalHostAddresses();

        return addresses[0];
    }

    /**
     * Returns all IP addresses that could be found on this machine. If desired,
     * loopback and/or IPv6 addresses can be ignored.
     * 
     * @param ignoreLoopback
     *            ignore loopback addresses
     * @param ignoreIP6
     *            ignore IPv6 addresses
     * 
     * @return all IP addresses found that adhere to the restrictions.
     */
    private static InetAddress[] getAllHostAddresses(boolean ignoreLoopback,
            boolean ignoreIP6) {

        ArrayList<InetAddress> list = new ArrayList<InetAddress>();

        try {
            Enumeration<NetworkInterface> interfaces =
                    NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                getAllHostAddresses(networkInterface, list, ignoreLoopback,
                        ignoreIP6);
            }
        } catch (SocketException e) {
            // IGNORE
        }

        return list.toArray(new InetAddress[list.size()]);
    }

    /**
     * Adds all IP addresses that are bound to a specific network interface to a
     * list. If desired, loopback and/or IPv6 addresses can be ignored.
     * 
     * @param nw
     *            the network interface for which the addresses are determined
     * @param target
     *            the list used to store the addresses
     * @param ignoreLoopback
     *            ignore loopback addresses
     * @param ignoreIP6
     *            ignore IPv6 addresses
     */
    private static void getAllHostAddresses(NetworkInterface nw,
            List<InetAddress> target, boolean ignoreLoopback, boolean ignoreIP6) {

        Enumeration<InetAddress> e2 = nw.getInetAddresses();

        while (e2.hasMoreElements()) {

            InetAddress tmp = e2.nextElement();

            boolean t1 = !ignoreLoopback || !tmp.isLoopbackAddress();
            boolean t2 = !ignoreIP6 || (tmp instanceof Inet4Address);

            if (t1 && t2) {
                target.add(tmp);
            }
        }
    }

}
