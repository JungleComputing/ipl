package ibis.ipl.impl.stacking.lrmc.util;

import java.util.LinkedList;

public class ByteArrayCache {

    private static final int DEFAULT_MAX = 100;

    private LinkedList<byte[]> cache = new LinkedList<byte[]>();

    private final int maxSize;
    private final int arraySize;

    public ByteArrayCache(int arraySize) {
        this(arraySize, DEFAULT_MAX);
    }

    public ByteArrayCache(int arraySize, int maxArrays) {
        this.maxSize = maxArrays;
        this.arraySize = arraySize;
    }

    public synchronized void put(byte[] array) {
        if (cache.size() < maxSize && array.length == arraySize) {
            cache.addLast(array);
        }
    }

    public synchronized byte[] get(int len) {
        if (len <= arraySize && cache.size() > 0) {
            return cache.removeLast();
        }

        return new byte[len];
    }
}
