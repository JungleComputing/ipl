package ibis.ipl.impl.registry.gossip;

import java.util.ArrayList;

import ibis.smartsockets.virtual.VirtualSocketAddress;
import ibis.smartsockets.virtual.VirtualSocketFactory;

/**
 * Implementation of the ARRG algorithm
 * 
 * @author ndrost
 * 
 */
public class ARRG {

    private static class CacheEntry {
        final VirtualSocketAddress address;

        final boolean arrgOnly;

        CacheEntry(VirtualSocketAddress address, boolean arrgOnly) {
            this.address = address;
            this.arrgOnly = arrgOnly;
        }
    }

    private final CacheEntry self;
    
    private final ArrayList<CacheEntry> cache;
    private final ArrayList<CacheEntry> fallbackCache;
    
    
    ARRG(VirtualSocketAddress address, boolean arrgOnly, VirtualSocketAddress[] bootstrap) {
        self = new CacheEntry(address, arrgOnly);
        
        cache = new ArrayList<CacheEntry>();
        fallbackCache = new ArrayList<CacheEntry>();
        
    }

}
