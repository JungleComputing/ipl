package ibis.impl.net.tcp_blk;

import java.net.Socket;

import java.util.Hashtable;
import java.util.Vector;

import ibis.ipl.IbisIdentifier;

import ibis.util.TypedProperties;


/**
 * A cache for TCP sockets to attempt to reuse the back link for connections
 * the other way around. This saves explicit TCP acknowledgement traffic and
 * therefore performance.
 */
class TcpConnectionCache {

    final static boolean VERBOSE =
	TypedProperties.booleanProperty(Driver.tcpblk_cache_v, false);
    private final static boolean DISABLED =
	! TypedProperties.booleanProperty(Driver.tcpblk_cache_enable, true);

    private Hashtable	inputCache = new Hashtable();
    private Hashtable	outputCache = new Hashtable();


    /**
     * Store unused input link in the cache.
     * @param ibis the partner's {@link ibis.ipl.IbisIdentifier}
     * @param socket the socket to which the link belongs
     * @return whether the link has been added to the cache. If the output
     * 		link is also present in the cache, both are removed and the
     * 		socket should be closed.
     */
    synchronized
    boolean cacheInput(IbisIdentifier ibis, Socket socket) {
	if (DISABLED) {
	    return false;
	}

	Vector oCache = (Vector)outputCache.get(ibis);
	if (oCache != null && oCache.remove(socket)) {
	    if (VERBOSE) {
		System.err.println("Remove/i " + socket
			+ " from caches because no used connections");
	    }
	    return false;
	}

	Hashtable iCache = (Hashtable)inputCache.get(ibis);

	if (iCache == null) {
	    iCache = new Hashtable();
	    inputCache.put(ibis, iCache);
	}

	iCache.put(new Integer(socket.getPort()), socket);
	if (VERBOSE) {
	    System.err.println("Added input " + socket
		    + " to cache " + iCache);
	}

	return true;
    }


    /**
     * Store unused output link in the cache.
     * @param ibis the partner's {@link ibis.ipl.IbisIdentifier}
     * @param socket the socket to which the link belongs
     * @return whether the link has been added to the cache. If the input link
     * 		is also present in the cache, both are removed and the socket
     * 		should be closed.
     */
    synchronized
    boolean cacheOutput(IbisIdentifier ibis, Socket socket) {
	if (DISABLED) {
	    return false;
	}

	Hashtable iCache = (Hashtable)inputCache.get(ibis);
	if (iCache != null && iCache.contains(socket)) {
	    int port = socket.getPort();
	    iCache.remove(new Integer(port));
	    if (VERBOSE) {
		System.err.println("Remove/o " + socket
			+ " from caches because no used connections");
	    }
	    return false;
	}

	Vector oCache = (Vector)outputCache.get(ibis);

	if (oCache == null) {
	    oCache = new Vector();
	    outputCache.put(ibis, oCache);
	}

	oCache.add(socket);
	if (VERBOSE) {
	    System.err.println("Added output " + socket
		    + " to cache " + oCache);
	}

	return true;
    }


    /**
     * Get the unused input link with a specified remote port from the cache.
     * @param ibis the partner's {@link ibis.ipl.IbisIdentifier}
     * @param port remote port; the output side has found this link in its
     * 		output cache and knows that the input link that belongs to it
     * 		is in our cache
     * @return the socket with remote port <code>port</code> whose input link
     * 		was cached.
     */
    synchronized
    Socket getCachedInput(IbisIdentifier ibis, int port) {
	if (DISABLED) {
	    return null;
	}

	Hashtable cache = (Hashtable)inputCache.get(ibis);

	if (cache == null) {
	    throw new Error("Must locate (" + ibis + "," + port
			    + ") but there are no cached connections there");
	}

	Integer key = new Integer(port);
	Socket socket = (Socket)cache.remove(key);

	if (socket == null) {
	    throw new Error("Must locate (" + ibis + "," + port
			    + ") but it is not there");
	}

	if (VERBOSE) {
	    System.err.println("Return output " + socket + " from cache " + cache);
	}
	return socket;
    }


    /**
     * Try to get an unused output link from the cache.
     * @param ibis the partner's {@link ibis.ipl.IbisIdentifier}
     * @return a socket whose output link was cached, or <code>null</code> if
     * 		there are no matching links in the cache
     */
    synchronized
    Socket getCachedOutput(IbisIdentifier ibis) {
	if (DISABLED) {
	    return null;
	}

	Vector cache = (Vector)outputCache.get(ibis);

	if (cache == null || cache.size() == 0) {
	    return null;
	}

	if (VERBOSE) {
	    System.err.println("Return output "
		    + cache.elementAt(cache.size() - 1) + " from cache "
		    + cache);
	}

	return (Socket)cache.remove(cache.size() - 1);
    }

}
