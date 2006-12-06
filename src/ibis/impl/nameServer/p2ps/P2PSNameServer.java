/* $Id: NameServerClient.java 4275 2006-09-08 12:51:50 +0200 (Fri, 08 Sep 2006) ceriel $ */

package ibis.impl.nameServer.p2ps;

import ibis.ipl.Ibis;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.StaticProperties;
import ibis.ipl.ConnectionTimedOutException;

import java.io.IOException;

import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import p2ps.imp.peer.ConfigFactory;
import p2ps.peer.Config;
import p2ps.peer.Peer;
import p2ps.peer.PeerFactory;
import p2ps.discovery.DiscoveryListener;
import p2ps.discovery.DiscoveryEvent;
import p2ps.discovery.Advertisement;

import org.apache.commons.collections.map.LRUMap;

public class P2PSNameServer extends ibis.impl.nameServer.NameServer implements DiscoveryListener {

    /** The Peer for this name server in p2ps */
    private Peer peer;
    
    /** The hashtable of ads this name server has published */
    private Map<String,PortAdvertisement> myPortAds;
    
    /** The map of pending queries. */
    private Map<String,String> queryMap;
    
    /** The map of ads we have received. */
    private Map<String,PortAdvertisement> portAds;
    
    /** The Ibis this name server is part of */
    private Ibis ibisImpl;

    /** true if the user of this name server needs upcalls */
    private boolean needsUpcalls;
    
    private volatile boolean stop = false;

    private boolean left = false;

    private static Logger logger
            = ibis.util.GetLogger.getLogger(P2PSNameServer.class.getName());

    /** Constructs but does not initialize the instance. Users must call init. */
    public P2PSNameServer() {
        if (logger.isDebugEnabled()) {
            logger.debug("Constructing new P2PSNameServer.");
        }
        else {
            System.out.println("Not logging start of P2PS Name Server");
        }
        /* do nothing */
    }

    /** Initializes this name server. */
    protected void init(Ibis ibis, boolean ndsUpcalls)
            throws IOException, IbisConfigurationException {
        
        // Remember the good stuff
        this.ibisImpl = ibis;
        this.needsUpcalls = ndsUpcalls;
        
        // Construct the advert cache
        Properties p = System.getProperties();
        int maxCache = -1;
        try {
            String maxCacheString = p.getProperty("ibis.impl.nameServer.p2ps.maxCache");
            if (null != maxCacheString) {
                maxCache = Integer.parseInt(maxCacheString);
            }
        }
        catch (NumberFormatException e) {
            // Ignored
        }
        
        myPortAds = Collections.checkedMap(new HashMap<String,PortAdvertisement>(), 
                    String.class, PortAdvertisement.class);
        queryMap = Collections.checkedMap(new HashMap<String,String>(),
                    String.class, String.class);
        
        if (maxCache > 0) {
            portAds = Collections.checkedMap(new LRUMap(maxCache),
                    String.class, PortAdvertisement.class);
        }
        else {
            portAds = Collections.checkedMap(new LRUMap(),
                    String.class, PortAdvertisement.class);
        }
        
        // Setup P2PS properly
        try {
            // Get configuration for our system.
            Config config = ConfigFactory.getConfig(ConfigFactory.CONFIG_WINDOW_ALWAYS);
            
            // Construct a peer for this name server and intitialize
            peer = PeerFactory.newPeer(config, ConfigFactory.getPassword(config));
            peer.init();
            
            // Register our advert instantiators
            peer.getAdvertisementFactory().register(new PortAdvertisementInstantiator());
            peer.getAdvertisementFactory().register(new PortQueryInstantiator());
            
            // listen for discovered advertisements
            peer.getDiscoveryService().addDiscoveryListener(this);
        }
        catch (Exception e) {
            throw new IOException("Error initializing Peer: " + e);
        }
        
        if (logger.isDebugEnabled())
            logger.debug("Initialized P2PSNameServer");
    }

    /** Called when an advertisement is received */
    public void advertDiscovered(DiscoveryEvent event) {
    	System.out.println("Discovery Event");
    	
        if (logger.isDebugEnabled())
            logger.debug("Discovery Event: " + event);
        try {
            Advertisement advert = event.getAdvertisement();
            logger.setLevel(Level.DEBUG);
            if (logger.isDebugEnabled())
                logger.debug("Got Advert: " + advert);
            
            // handle discovered server pipe advertisement
            if (advert instanceof PortAdvertisement) {
                if (logger.isDebugEnabled())
                    logger.debug("Got Port Advertisement");
                PortAdvertisement portAd = (PortAdvertisement)advert;
                if (logger.isDebugEnabled())
                    logger.debug("Port Name: " + portAd.getPortName());
                
                // Cache the portID. This will cache our own ports as well.
                if (null == portAds.get(portAd.getPortName())) {
                    portAds.put(portAd.getPortName(), portAd);
                    
                    // Notify anybody waiting for this port
                    Object name = queryMap.get(portAd.getPortName());
                    if (null != name) {
                        synchronized (name) {
                            name.notifyAll();
                        }
                    }
                }
            }
            // handle a request for republish
            else if (advert instanceof PortQuery) {
                if (logger.isDebugEnabled())
                    logger.debug("Got Port Query.");
                PortQuery portQuery = (PortQuery)advert;
                if (logger.isDebugEnabled())
                    logger.debug("Port Name: " + portQuery.getQueryPortName());
                /*
                PortAdvertisement portAd = (PortAdvertisement)myPortAds.get(portQuery.getQueryPortName());
                if (null != portAd) {
                    if (logger.isDebugEnabled())
                        logger.debug("Republishing my ad.");
                    peer.getDiscoveryService().publish(portAd);
                    if (logger.isDebugEnabled())
                        logger.debug("Advertisement republished.");
                }
                */
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void maybeDead(IbisIdentifier ibisId) throws IOException {

    }

    public void dead(IbisIdentifier corpse) throws IOException {

    }

    public void mustLeave(IbisIdentifier[] ibisses) throws IOException {

    }

    public boolean newPortType(String name, StaticProperties p)
            throws IOException {
        return false;
    }

    public long getSeqno(String name) throws IOException {
        return 0;
    }

    public void leave() {
        if (! needsUpcalls) {
            synchronized (this) {
                stop = true;
            }
        } else {
            synchronized(this) {
                while (! left) {
                    try {
                        wait();
                    } catch(Exception e) {
                        // Ignored
                    }
                }
            }
        }
        if (logger.isInfoEnabled())
            logger.info("NS client: leave DONE");
    }

    public void run() {
        if (logger.isInfoEnabled())
            logger.info("P2PSNameServer: thread started");

        while (! stop) {
            try {
                wait();
            } catch(Exception e) {
                    //Ignored
            }
        }
        if (logger.isInfoEnabled())
            logger.info("P2PSNameServer: thread stopped");
    }

    public ReceivePortIdentifier lookupReceivePort(String name)
            throws IOException {
        return lookupReceivePort(name, 0);
    }

    public ReceivePortIdentifier lookupReceivePort(String name, long timeout)
            throws IOException {
    	logger.setLevel(Level.DEBUG);
    	
        logger.debug("Looking up receive port.");
        PortQuery query = (PortQuery)peer.getAdvertisementFactory().newAdvertisement(PortQuery.TYPE);
        query.setQueryPortName(name);
        ReceivePortIdentifier rpi = null;

        PortAdvertisement pa = portAds.get(name);
        if (null != pa) {
            rpi = pa.getPortId();
            if (logger.isDebugEnabled())
                logger.debug("Returning cached Recieve Port: " + rpi);
        }
        else {
            if (logger.isDebugEnabled())
                logger.debug("Attempting to query for port: " + name);
                
            queryMap.put(name, name);

            synchronized (name) {
                try {
                    if (logger.isDebugEnabled())
                        logger.debug("Publishing query!");
                    peer.getDiscoveryService().publish(query);
                    if (logger.isDebugEnabled())
                        logger.debug("Query Published!");
                    System.out.println("Waiting.");
                    // TODO: This should be waiting on a condition instead and should 
                    // throw an exception when the timeout expires.
                    name.wait(timeout);
                }
                catch (Exception e) {
                    if (logger.isDebugEnabled())
                        logger.debug("lookupReceivePort caught exception: " + e);
                    // Ignored.
                }
                finally {
                	// Cleanup the name we are waiting on.
                    queryMap.remove(name);
                    pa = portAds.get(name);
                    if (null != pa) {
                        rpi = pa.getPortId();
                    }
                
                }
            }
        }
        
        if (logger.isDebugEnabled())
            logger.debug("lookupReceivePort returning: " + rpi);
        
        return rpi;
    }

    public ReceivePortIdentifier[] lookupReceivePorts(String[] names)
            throws IOException {
        return lookupReceivePorts(names, 0, false);
    }

    public ReceivePortIdentifier[] lookupReceivePorts(String[] names, 
            long timeout, boolean allowPartialResult)
            throws IOException {
        /* 
         * Do this the stupid way for now. A smarter way might be to send out all the queries and
         * then walk the list checking the cache until timeout occurs.
         */
    	long start = 0;
    	long remaining = timeout;
    	
    	// Save the call to System.currentTimeMillis if we will never use it.
    	if (timeout > 0) {
            start = System.currentTimeMillis();
    	}
        
    	// Alllocate the returned list.
    	ArrayList<ReceivePortIdentifier> list = new ArrayList<ReceivePortIdentifier>(names.length);
    	
    	for (int i = 0; i < names.length; i++) {
    		ReceivePortIdentifier port = lookupReceivePort(names[i], remaining);
    		if (null == port && !allowPartialResult) {
                throw new ConnectionTimedOutException("Ran out of time while resolving: " + names[i]);
    		}
    		
    		list.add(port);
    		
    		if (timeout > 0) {
                remaining = timeout - (System.currentTimeMillis() - start);
    		
                if (remaining <= 0) {
                    if (allowPartialResult)
                        break;
                    else
                        throw new ConnectionTimedOutException("Ran out of time while resolving: " + names[i]);
                }
    		}
    	}
    	
    	return list.toArray(new ReceivePortIdentifier[list.size()]); 
    }

    public IbisIdentifier lookupIbis(String name) {
        return lookupIbis(name, 0);
    }

    public IbisIdentifier lookupIbis(String name, long timeout) {
        /* not implemented yet */
        return null;
    }

    public ReceivePortIdentifier[] listReceivePorts(IbisIdentifier ident) {
        /* not implemented yet */
        return new ReceivePortIdentifier[0];
    }

    public IbisIdentifier elect(String election) throws IOException,
            ClassNotFoundException {
        return null;
    }

    public IbisIdentifier getElectionResult(String election)
            throws IOException, ClassNotFoundException {
        return null;
    }

    public void bind(String name, ReceivePortIdentifier rpi)
            throws IOException {
        // Create Advertisement
        if (logger.isDebugEnabled())
            logger.debug("Creating Port Advertisement");
        PortAdvertisement portAd = (PortAdvertisement)peer.getAdvertisementFactory().newAdvertisement(PortAdvertisement.TYPE);
        portAd.setPortName(name);
        portAd.setPortId(rpi);

        // Save ad to be republished.
        myPortAds.put(name, portAd);
        
        // publish advertisement
        if (logger.isDebugEnabled())
            logger.debug("Publishing Advertisement");
        peer.getDiscoveryService().publish(portAd);
        if (logger.isDebugEnabled())
            logger.debug("Advertisement Published");
    }
    
    public void rebind(String name, ReceivePortIdentifier rpi)
            throws IOException {
        unbind(name);
        bind(name, rpi);
    }

    public void unbind(String name) throws IOException {

    }

    public String[] listNames(String pattern) throws IOException {
        return new String[0];
    }
}
