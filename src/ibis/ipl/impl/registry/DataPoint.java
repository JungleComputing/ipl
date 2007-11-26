/**
 * 
 */
package ibis.ipl.impl.registry;

class DataPoint implements Comparable<DataPoint> {
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

    public int compareTo(DataPoint other) {
        return (int) (time - other.time);
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