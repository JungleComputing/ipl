package ibis.ipl.impl.registry;

import java.util.SortedSet;
import java.util.TreeSet;

public class PoolStatistics {

    private static class Item implements Comparable<Item> {
        long time;
        long value;

        Item(long value) {
            time = System.currentTimeMillis();
            this.value = value;
        }

        public int compareTo(Item other) {
            return (int) (time - other.time);
        }
    }

    SortedSet<Item> poolSizeHistory;
    
    SortedSet<Item> electionEventHistory;

    int currentPoolSize;

    public PoolStatistics() {
        poolSizeHistory = new TreeSet<Item>();
        electionEventHistory = new TreeSet<Item>();

        currentPoolSize = 0;
    }

    public void ibisJoined() {
        currentPoolSize++;
        
        poolSizeHistory.add(new Item(currentPoolSize));
    }

    public void ibisLeft() {
        currentPoolSize--;

        poolSizeHistory.add(new Item(currentPoolSize));
    }

    public void ibisDied() {
        currentPoolSize--;

        poolSizeHistory.add(new Item(currentPoolSize));
    }

    public void unElect() {
        electionEventHistory.add(new Item(electionEventHistory.size() + 1));
    }

    public void newElection() {
        electionEventHistory.add(new Item(electionEventHistory.size() + 1));
    }

}
