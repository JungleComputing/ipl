package ibis.ipl.registry.gossip;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ARRGCache {
    
    private static final Logger logger = LoggerFactory.getLogger(ARRGCache.class);

    private final int cacheSize;

    private final Random random;

    private final ArrayList<ARRGCacheEntry> cache;

    public ARRGCache(int cacheSize) {
        this.cacheSize = cacheSize;

        cache = new ArrayList<ARRGCacheEntry>();

        random = new Random();
    }

    public synchronized ARRGCacheEntry[] getRandomEntries(int n, boolean includeArrgOnly) {
        List<ARRGCacheEntry> result = new ArrayList<ARRGCacheEntry>();
        BitSet selected = new BitSet();

        while (selected.cardinality() < n && selected.cardinality() < cache.size()) {
            int next = random.nextInt(cache.size());

            selected.set(next);
            ARRGCacheEntry entry = cache.get(next);
            
            if (includeArrgOnly || !entry.isArrgOnly()) {
                result.add(entry);
            }
        }

        return result.toArray(new ARRGCacheEntry[0]);
    }
    
    public synchronized ARRGCacheEntry getRandomEntry(boolean includeArrgOnly) {
        ARRGCacheEntry[] result = getRandomEntries(1, includeArrgOnly);
        
        if (result.length < 1) {
            return null;
        }
        
        return result[0];
    }

    public synchronized void add(ARRGCacheEntry... entries) {
        //add entries
        for (ARRGCacheEntry entry : entries) {
            if (entry != null) {
                cache.add(entry);
            }
        }
        
        //purge duplicates
        for (int i = 0; i < cache.size(); i++) {
            for (int j = i + 1; j < cache.size(); j++) {
                if (cache.get(i).sameAddressAs(cache.get(j))) {
                    cache.remove(j);
                    j--;
                }
            }
        }
        
        //remove random entries if cache outgrew maximum value
        while(cache.size() > cacheSize) {
            cache.remove(random.nextInt(cache.size()));
        }
        
        if (logger.isDebugEnabled()) {
            logger.debug("cache value now: " + cache.size());
        }
    }

    public synchronized ARRGCacheEntry[] getEntries(boolean includeArrgOnly) {
        if (includeArrgOnly) {
            return cache.toArray(new ARRGCacheEntry[0]);
        } else {
            ArrayList<ARRGCacheEntry> result = new ArrayList<ARRGCacheEntry>();
            
            for (ARRGCacheEntry entry: cache) {
                if (!entry.isArrgOnly()) {
                    result.add(entry);
                }
            }
            
            return result.toArray(new ARRGCacheEntry[0]);
        }
    }
}
