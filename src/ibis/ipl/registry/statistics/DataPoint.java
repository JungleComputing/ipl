/**
 * 
 */
package ibis.ipl.registry.statistics;

class DataPoint {
    private long time;

    private long value;

    DataPoint(long time, long value) {
        this.time = time;
        this.value = value;
    }

    DataPoint(long value) {
        time = System.currentTimeMillis();
        this.value = value;
    }

    public String toString() {
        return time + " " + value;
    }
    
    public long getTime() {
        return time;
    }
    
    public long getValue() {
        return value;
    }
}