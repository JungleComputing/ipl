package ibis.impl.nio;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.nio.channels.Channel;

class NioConnectionCache implements Config {

    private static class Peer implements Config {

	private static class Connection implements Config {
	    Channel channel;
	    boolean inFree;
	    boolean outFree;

	    Connection(Channel channel, boolean inFree, boolean outFree) {
		this.channel = channel;
		this.inFree = inFree;
		this.outFree = outFree;
	    }

	    void kill() {
		try {
		    channel.close();
		} catch (Exception e) {
		    if(DEBUG_LEVEL >= ERROR_DEBUG_LEVEL) {
			System.err.println("got exception on closing free channel" + e);
			e.printStackTrace();
		    }
		    //IGNORE
		}
	    }

	    public String toString() {
		return "Connection(channel = " + channel + " inFree = " + inFree
		    + " outFree = " + outFree + ")";
	    }

	} // end of inner class Connection

	private ArrayList connections;

	Peer() {
	    connections = new ArrayList();
	}

	/**
	 * Signal the cache that the output of the given channel is not used
	 */
	synchronized void outputIsFree(Channel channel) {
	    for(int i = 0;i < connections.size(); i++) {
		Connection temp = (Connection) connections.get(i);
		if(temp.channel == channel) {
		    temp.outFree = true;
		    if(temp.inFree) { // channel not used anymore
			temp.kill();
			connections.remove(i);
		    }
		    return;
		}
	    }
	    connections.add(new Connection(channel, false, true));
	}

	/**
	 * Signal the cache that the input of the given channel is not used
	 */
	synchronized void inputIsFree(Channel channel) {
	    for (int i = 0; i < connections.size(); i++) {
		Connection temp = (Connection) connections.get(i);
		if (temp.channel == channel) {
		    temp.inFree = true;
		    if(temp.outFree) { // channel not used anymore
			temp.kill();
			connections.remove(i);
		    }
		    return;
		}
	    }
	    connections.add(new Connection(channel, true, false));
	}

	/**
	 * Looks in the cache for a free output to this peer, returns null
	 * if not found
	 */
	synchronized Channel getFreeOutput() {
	    for(int i = 0;i < connections.size(); i++) {
		Connection temp = (Connection) connections.get(i);
		if(temp.outFree) {
		    temp.outFree = false;
		    if(!temp.inFree) { // channel now fully used
			connections.remove(i);
		    }
		    return temp.channel;
		}
	    }
	    return null;
	}

	/**
	 * Looks in the cache for a free input to this peer, returns null
	 * if not found
	 */
	synchronized Channel getFreeInput() {
	    for(int i = 0;i < connections.size(); i++) {
		Connection temp = (Connection) connections.get(i);
		if(temp.inFree) {
		    temp.inFree = false;
		    if(!temp.outFree) { // channel now fully used
			connections.remove(i);
		    }
		    return temp.channel;
		}
	    }
	    return null;
	}

    } // end of inner class Peer

    private HashMap peers = new HashMap();

    private synchronized Peer getPeer(NioIbisIdentifier ibis) {
	Peer p = (Peer) peers.get(ibis);

	if(p == null) {
	    p = new Peer();
	    peers.put(ibis, p);
	}

	return p;
    }

    /**
     * Tries to find a channel to "ibis" with a free output
     */
    Channel getFreeOutput(NioIbisIdentifier ibis) {
	if(DEBUG_LEVEL >= RIDICULOUSLY_HIGH_DEBUG_LEVEL) {
	    System.err.println("ConCache: getFreeOutput(" + ibis + ")" );
	}
	Peer p = getPeer(ibis);
	return p.getFreeOutput();
    }

    /**
     * Tries to find a channel to "ibis" with a free input
     */
    Channel getFreeInput(NioIbisIdentifier ibis) {
	if(DEBUG_LEVEL >= RIDICULOUSLY_HIGH_DEBUG_LEVEL) {
	    System.err.println("ConCache: getFreeInput(" + ibis + ")" );
	}
	Peer p = getPeer(ibis);
	return p.getFreeInput();
    }

    /**
     * Signals the cache the given channel had a free input
     */
    void inputIsFree(NioIbisIdentifier ibis, Channel channel) {
	if(DEBUG_LEVEL >= RIDICULOUSLY_HIGH_DEBUG_LEVEL) {
	    System.err.println("ConCache: inputIsFree(" + ibis + ", "
		    + channel + ")");
	}
	Peer p = getPeer(ibis);
	p.inputIsFree(channel);
    }

    /**
     * Signals the cache the given channel had a free input
     */
    void outputIsFree(NioIbisIdentifier ibis, Channel channel) {
	if(DEBUG_LEVEL >= RIDICULOUSLY_HIGH_DEBUG_LEVEL) {
	    System.err.println("ConCache: outputIsFree(" + ibis + ", "
		    + channel + ")");
	}
	Peer p = getPeer(ibis);
	p.outputIsFree(channel);
    }
}


