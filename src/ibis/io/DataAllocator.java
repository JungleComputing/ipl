package ibis.io;

import ibis.util.TypedProperties;

/**
 * Allocator for arrays of primitives that is tailored towards the
 * IbisSerializationOutputStream.
 */
public class DataAllocator implements IbisStreamFlags {

    /**
     * Number of buffers per type that is cached at maximum.
     */
    private final static int DEFAULT_CACHE_MAX = 512; // 64; // 256;
    private final static int CACHE_MAX;

    static {
	CACHE_MAX = TypedProperties.intProperty(IOProps.s_cache_max, DEFAULT_CACHE_MAX);
    }

    /**
     * The number of currently cached buffers per type.
     */
    private int[] cached = new int[PRIMITIVE_TYPES + 1];

    /**
     * The cache per type
     */
    private short[][]	index_cache  = new short[CACHE_MAX][];
    private byte[][]	byte_cache   = new byte[CACHE_MAX][];
    private char[][]	char_cache   = new char[CACHE_MAX][];
    private short[][]	short_cache  = new short[CACHE_MAX][];
    private int[][]	int_cache    = new int[CACHE_MAX][];
    private long[][]	long_cache   = new long[CACHE_MAX][];
    private float[][]	float_cache  = new float[CACHE_MAX][];
    private double[][]	double_cache = new double[CACHE_MAX][];

    private IbisHash	indexHash    = new IbisHash(2 * CACHE_MAX, true);
    private IbisHash	byteHash     = new IbisHash(2 * CACHE_MAX, true);
    private IbisHash	charHash     = new IbisHash(2 * CACHE_MAX, true);
    private IbisHash	shortHash    = new IbisHash(2 * CACHE_MAX, true);
    private IbisHash	intHash      = new IbisHash(2 * CACHE_MAX, true);
    private IbisHash	longHash     = new IbisHash(2 * CACHE_MAX, true);
    private IbisHash	floatHash    = new IbisHash(2 * CACHE_MAX, true);
    private IbisHash	doubleHash   = new IbisHash(2 * CACHE_MAX, true);

    private final static boolean STATISTICS = TypedProperties.booleanProperty(IOProps.s_cache_stats);

    // if (STATISTICS)
    private int alloc_index;
    private int alloc_byte;
    private int alloc_short;
    private int alloc_char;
    private int alloc_int;
    private int alloc_long;
    private int alloc_float;
    private int alloc_double;
    private int gc_index;
    private int gc_byte;
    private int gc_short;
    private int gc_char;
    private int gc_int;
    private int gc_long;
    private int gc_float;
    private int gc_double;
    private int cache_index;
    private int cache_byte;
    private int cache_short;
    private int cache_char;
    private int cache_int;
    private int cache_long;
    private int cache_float;
    private int cache_double;

    {
	if (STATISTICS) {
	    Runtime.getRuntime().addShutdownHook(new Thread("DataAllocator ShutdownHook") {
		public void run() {
		    System.out.println("Alloc" + " X " + alloc_index
			+ " B " + alloc_byte + " S " + alloc_short
			+ " C " + alloc_char + " I " + alloc_int
			+ " L " + alloc_long + " F " + alloc_float
			+ " D " + alloc_double);
		    System.out.println("GC   " + " X " + gc_index
			+ " B " + gc_byte + " S " + gc_short
			+ " C " + gc_char + " I " + gc_int
			+ " L " + gc_long + " F " + gc_float
			+ " D " + gc_double);
		    System.out.println("Cache" + " X " + cache_index
			+ " B " + cache_byte + " S " + cache_short
			+ " C " + cache_char + " I " + cache_int
			+ " L " + cache_long + " F " + cache_float
			+ " D " + cache_double);
		    }
		});
	}
    }

    /**
     * Obtain an array of shorts fit to store an IbisSerializationOutputStream
     * index descriptor. Get it from the cache if possible.
     *
     * @return an index descriptor buffer
     */
    public synchronized short[] getIndexArray() {
	if (cached[PRIMITIVE_TYPES] == 0) {
	    if (STATISTICS) alloc_index++;
	    short[] buffer = new short[PRIMITIVE_TYPES];
	    indexHash.put(buffer, 1);
	    return buffer;
	} else {
	    return index_cache[--cached[PRIMITIVE_TYPES]];
	}
    }

    /**
     * Obtain an array of bytes for an IbisSerializationOutputStream byte
     * buffer. The size is determined in IbisStreamFlags.BYTE_BUFFER_SIZE.
     * Get it from the cache if possible.
     *
     * @return a byte buffer
     */
    public synchronized byte[] getByteArray() {
	if (cached[TYPE_BYTE] == 0) {
	    if (STATISTICS) alloc_byte++;
	    byte[] buffer = new byte[BYTE_BUFFER_SIZE];
	    byteHash.put(buffer, 1);
	    return buffer;
	} else {
	    return byte_cache[--cached[TYPE_BYTE]];
	}
    }

    /**
     * Obtain an array of chars for an IbisSerializationOutputStream char
     * buffer. The size is determined in IbisStreamFlags.CHAR_BUFFER_SIZE.
     * Get it from the cache if possible.
     *
     * @return a char buffer
     */
    public synchronized char[] getCharArray() {
	if (cached[TYPE_CHAR] == 0) {
	    if (STATISTICS) alloc_char++;
	    char[] buffer = new char[CHAR_BUFFER_SIZE];
	    charHash.put(buffer, 1);
	    return buffer;
	} else {
	    return char_cache[--cached[TYPE_CHAR]];
	}
    }

    /**
     * Obtain an array of shorts for an IbisSerializationOutputStream short
     * buffer. The size is determined in IbisStreamFlags.SHORT_BUFFER_SIZE.
     * Get it from the cache if possible.
     *
     * @return a short buffer
     */
    public synchronized short[] getShortArray() {
	if (cached[TYPE_SHORT] == 0) {
	    if (STATISTICS) alloc_short++;
	    short[] buffer = new short[SHORT_BUFFER_SIZE];
	    shortHash.put(buffer, 1);
	    return buffer;
	} else {
	    return short_cache[--cached[TYPE_SHORT]];
	}
    }

    /**
     * Obtain an array of ints for an IbisSerializationOutputStream int
     * buffer. The size is determined in IbisStreamFlags.INT_BUFFER_SIZE.
     * Get it from the cache if possible.
     *
     * @return a int buffer
     */
    public synchronized int[] getIntArray() {
	if (cached[TYPE_INT] == 0) {
	    if (STATISTICS) alloc_int++;
	    int[] buffer = new int[INT_BUFFER_SIZE];
	    intHash.put(buffer, 1);
	    return buffer;
	} else {
	    if (ASSERTS) {
		if (int_cache[cached[TYPE_INT] - 1].length != INT_BUFFER_SIZE) {
		    throw new RuntimeException("Cache contains int[] length "
			    + int_cache[cached[TYPE_INT] - 1].length);
		}
	    }
	    return int_cache[--cached[TYPE_INT]];
	}
    }

    /**
     * Obtain an array of longs for an IbisSerializationOutputStream long
     * buffer. The size is determined in IbisStreamFlags.LONG_BUFFER_SIZE.
     * Get it from the cache if possible.
     *
     * @return a long buffer
     */
    public synchronized long[] getLongArray() {
	if (cached[TYPE_LONG] == 0) {
	    if (STATISTICS) alloc_long++;
	    long[] buffer = new long[LONG_BUFFER_SIZE];
	    longHash.put(buffer, 1);
	    return buffer;
	} else {
	    return long_cache[--cached[TYPE_LONG]];
	}
    }

    /**
     * Obtain an array of floats for an IbisSerializationOutputStream float
     * buffer. The size is determined in IbisStreamFlags.FLOAT_BUFFER_SIZE.
     * Get it from the cache if possible.
     *
     * @return a float buffer
     */
    public synchronized float[] getFloatArray() {
	if (cached[TYPE_FLOAT] == 0) {
	    if (STATISTICS) alloc_float++;
	    float[] buffer = new float[FLOAT_BUFFER_SIZE];
	    floatHash.put(buffer, 1);
	    return buffer;
	} else {
	    return float_cache[--cached[TYPE_FLOAT]];
	}
    }

    /**
     * Obtain an array of doubles for an IbisSerializationOutputStream double
     * buffer. The size is determined in IbisStreamFlags.DOUBLE_BUFFER_SIZE.
     * Get it from the cache if possible.
     *
     * @return a double buffer
     */
    public synchronized double[] getDoubleArray() {
	if (cached[TYPE_DOUBLE] == 0) {
	    if (STATISTICS) alloc_double++;
	    double[] buffer = new double[DOUBLE_BUFFER_SIZE];
	    doubleHash.put(buffer, 1);
	    return buffer;
	} else {
	    return double_cache[--cached[TYPE_DOUBLE]];
	}
    }

    /**
     * Return an array of shorts fit to store an IbisSerializationOutputStream
     * index descriptor. Put it in the cache if possible.
     *
     * @param buffer the index buffer
     */
    public synchronized void putIndexArray(short[] buffer) {
	int hc = indexHash.getHashCode(buffer);
	if (indexHash.find(buffer, hc) == 0) {
	    // It is not ours. Stay away from it.
	    return;
	}
	if (ASSERTS) {
	    if (buffer.length != PRIMITIVE_TYPES) {
		throw new RuntimeException("Get index array to cache of size "
			+ buffer.length);
	    }
	}
	if (cached[PRIMITIVE_TYPES] == CACHE_MAX) {
	    // Let the GC cope with it
	    if (STATISTICS) gc_index++;
	    indexHash.delete(buffer, hc);
	    return;
	}
	index_cache[cached[PRIMITIVE_TYPES]++] = buffer;
	if (STATISTICS) cache_index++;
    }

    /**
     * Return an array of bytes for an IbisSerializationOutputStream byte
     * buffer descriptor. Put it in the cache if possible.
     *
     * @param buffer the byte buffer
     */
    public synchronized void putByteArray(byte[] buffer) {
	int hc = byteHash.getHashCode(buffer);
	if (byteHash.find(buffer, hc) == 0) {
	    // It is not ours. Stay away from it.
	    return;
	}
	if (ASSERTS) {
	    if (buffer.length != BYTE_BUFFER_SIZE) {
		throw new RuntimeException("Get byte array to cache of size "
			+ buffer.length);
	    }
	}
	if (cached[TYPE_BYTE] == CACHE_MAX) {
	    // Let the GC cope with it
	    if (STATISTICS) gc_byte++;
	    byteHash.delete(buffer, hc);
	    return;
	}
	byte_cache[cached[TYPE_BYTE]++] = buffer;
	if (STATISTICS) cache_byte++;
    }

    /**
     * Return an array of chars for an IbisSerializationOutputStream char
     * buffer descriptor. Put it in the cache if possible.
     *
     * @param buffer the char buffer
     */
    public synchronized void putCharArray(char[] buffer) {
	int hc = charHash.getHashCode(buffer);
	if (charHash.find(buffer, hc) == 0) {
	    // It is not ours. Stay away from it.
	    return;
	}
	if (ASSERTS) {
	    if (buffer.length != CHAR_BUFFER_SIZE) {
		throw new RuntimeException("Get char array to cache of size "
			+ buffer.length);
	    }
	}
	if (cached[TYPE_CHAR] == CACHE_MAX) {
	    // Let the GC cope with it
	    if (STATISTICS) gc_char++;
	    charHash.delete(buffer, hc);
	    return;
	}
	char_cache[cached[TYPE_CHAR]++] = buffer;
	if (STATISTICS) cache_char++;
    }

    /**
     * Return an array of shorts for an IbisSerializationOutputStream short
     * buffer descriptor. Put it in the cache if possible.
     *
     * @param buffer the short buffer
     */
    public synchronized void putShortArray(short[] buffer) {
	if (buffer.length == PRIMITIVE_TYPES) {
	    putIndexArray(buffer);
	    return;
	}
	int hc = shortHash.getHashCode(buffer);
	if (shortHash.find(buffer, hc) == 0) {
	    // It is not ours. Stay away from it.
	    return;
	}
	if (ASSERTS) {
	    if (buffer.length != SHORT_BUFFER_SIZE) {
		throw new RuntimeException("Get short array to cache of size "
			+ buffer.length);
	    }
	}
	if (cached[TYPE_SHORT] == CACHE_MAX) {
	    // Let the GC cope with it
	    if (STATISTICS) gc_short++;
	    shortHash.delete(buffer, hc);
	    return;
	}
	short_cache[cached[TYPE_SHORT]++] = buffer;
	if (STATISTICS) cache_short++;
    }

    /**
     * Return an array of ints for an IbisSerializationOutputStream int
     * buffer descriptor. Put it in the cache if possible.
     *
     * @param buffer the int buffer
     */
    public synchronized void putIntArray(int[] buffer) {
	int hc = intHash.getHashCode(buffer);
	if (intHash.find(buffer, hc) == 0) {
	    // It is not ours. Stay away from it.
	    return;
	}
	if (ASSERTS) {
	    if (buffer.length != INT_BUFFER_SIZE) {
		throw new RuntimeException("Get int array to cache of size "
			+ buffer.length + " find()->"
			+ intHash.find(buffer, hc));
	    }
	}
	if (cached[TYPE_INT] == CACHE_MAX) {
	    // Let the GC cope with it
	    if (STATISTICS) gc_int++;
	    intHash.delete(buffer, hc);
	    return;
	}
	int_cache[cached[TYPE_INT]++] = buffer;
	if (STATISTICS) cache_int++;
    }

    /**
     * Return an array of longs for an IbisSerializationOutputStream long
     * buffer descriptor. Put it in the cache if possible.
     *
     * @param buffer the long buffer
     */
    public synchronized void putLongArray(long[] buffer) {
	int hc = longHash.getHashCode(buffer);
	if (longHash.find(buffer, hc) == 0) {
	    // It is not ours. Stay away from it.
	    return;
	}
	if (ASSERTS) {
	    if (buffer.length != LONG_BUFFER_SIZE) {
		throw new RuntimeException("Get long array to cache of size "
			+ buffer.length);
	    }
	}
	if (cached[TYPE_LONG] == CACHE_MAX) {
	    // Let the GC cope with it
	    if (STATISTICS) gc_long++;
	    longHash.delete(buffer, hc);
	    return;
	}
	long_cache[cached[TYPE_LONG]++] = buffer;
	if (STATISTICS) cache_long++;
    }

    /**
     * Return an array of floats for an IbisSerializationOutputStream float
     * buffer descriptor. Put it in the cache if possible.
     *
     * @param buffer the float buffer
     */
    public synchronized void putFloatArray(float[] buffer) {
	int hc = floatHash.getHashCode(buffer);
	if (floatHash.find(buffer, hc) == 0) {
	    // It is not ours. Stay away from it.
	    return;
	}
	if (ASSERTS) {
	    if (buffer.length != FLOAT_BUFFER_SIZE) {
		throw new RuntimeException("Get float array to cache of size "
			+ buffer.length);
	    }
	}
	if (cached[TYPE_FLOAT] == CACHE_MAX) {
	    // Let the GC cope with it
	    if (STATISTICS) gc_float++;
	    floatHash.delete(buffer, hc);
	    return;
	}
	float_cache[cached[TYPE_FLOAT]++] = buffer;
	if (STATISTICS) cache_float++;
    }

    /**
     * Return an array of doubles for an IbisSerializationOutputStream double
     * buffer descriptor. Put it in the cache if possible.
     *
     * @param buffer the double buffer
     */
    public synchronized void putDoubleArray(double[] buffer) {
	int hc = doubleHash.getHashCode(buffer);
	if (doubleHash.find(buffer, hc) == 0) {
	    // It is not ours. Stay away from it.
	    return;
	}
	if (ASSERTS) {
	    if (buffer.length != DOUBLE_BUFFER_SIZE) {
		throw new RuntimeException("Get double array to cache of size "
			+ buffer.length);
	    }
	}
	if (cached[TYPE_DOUBLE] == CACHE_MAX) {
	    // Let the GC cope with it
	    if (STATISTICS) gc_double++;
	    doubleHash.delete(buffer, hc);
	    return;
	}
	double_cache[cached[TYPE_DOUBLE]++] = buffer;
	if (STATISTICS) cache_double++;
    }

}
