package ibis.ipl.impl.registry.gossip;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;

class Cache {
    
    private static final Logger logger = Logger.getLogger(Cache.class);

    private final int cacheSize;

    private final Random random;

    private final ArrayList<CacheEntry> cache;

    public Cache(int cacheSize) {
        this.cacheSize = cacheSize;

        cache = new ArrayList<CacheEntry>();

        random = new Random();
    }

    public synchronized CacheEntry[] getRandomEntries(int n) {
        List<CacheEntry> result = new ArrayList<CacheEntry>();
        BitSet selected = new BitSet();

        if (n > cache.size()) {
            n = cache.size();
        }

        for (int i = 0; i < n; i++) {
            int next;
            do {
                next = random.nextInt(cache.size());
            } while (selected.get(next));

            selected.set(next);
            result.add(cache.get(next));
        }

        return result.toArray(new CacheEntry[0]);
    }

    public synchronized CacheEntry getRandomEntry() {
        if (cache.size() == 0) {
            return null;
        }
        return cache.get(random.nextInt(cache.size()));
    }

    public synchronized void add(CacheEntry... entries) {
        //add entries
        for (CacheEntry entry : entries) {
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
        
        //remove random entries if cache outgrew maximum size
        while(cache.size() > cacheSize) {
            cache.remove(random.nextInt(cache.size()));
        }
        
        logger.debug("cache size now: " + cache.size());
    }
    
    

}
