/* $Id: IbisSerializationInputStream.java 6556 2007-10-12 14:19:08Z ceriel $ */

package ibis.io.jme;

import java.io.EOFException;
import java.io.IOException;
import java.util.Hashtable;

/**
 * This is the <code>SerializationInputStream</code> version that is used
 * for Ibis serialization.
 */
public class ObjectInputStream extends DataSerializationInputStream {
    /** If <code>false</code>, makes all timer calls disappear. */
    private static final boolean TIME_IBIS_SERIALIZATION = false;

    /**
     * Record how many objects of any class are sent the expensive way:
     * via the uninitialized native creator.
     */
    private static final boolean STATS_NONREWRITTEN
            = properties.getBooleanProperty(s_stats_nonrewritten);

    // if STATS_NONREWRITTEN
    static Hashtable nonRewritten = null;

    static {
        if (STATS_NONREWRITTEN) {
            nonRewritten = new Hashtable();
            System.out.println("ObjectInputStream.STATS_NONREWRITTEN"
                    + " enabled");
            /* TODO: Add shutdown hook system
            Runtime.getRuntime().addShutdownHook(
                    new Thread("ObjectInputStream ShutdownHook") {
                        public void run() {
                            System.out.print("Serializable objects created "
                                    + "nonrewritten: ");
                            System.out.println(nonRewritten);
                        }
                    });
            */
        }
    }

    /** List of objects, for cycle checking. */
    private IbisVector objects;

    /** First free object index. */
    private int next_handle;

    /** Handle to invalidate. */
    private int unshared_handle = 0;

    /** First free type index. */
    private int next_type = 1;

    /** List of types seen sofar. */
    private IbisVector types;

    /**
     * There is a notion of a "current" object. This is needed when a
     * user-defined <code>readObject</code> refers to
     * <code>defaultReadObject</code> or to
     * <code>getFields</code>.
     */
    Object current_object;

    /**
     * There also is a notion of a "current" level.
     * The "level" of a serializable class is computed as follows:<ul>
     * <li> if its superclass is serializable: the level of the superclass + 1.
     * <li> if its superclass is not serializable: 1.
     * </ul>
     * This level implies a level at which an object can be seen. The "current"
     * level is the level at which <code>current_object</code> is being
     * processed.
     */
    int current_level;

    /**
     * The <code>current_object</code> and <code>current_level</code>
     * are maintained in
     * stacks, so that they can be managed by IOGenerator-generated code.
     */
    private Object[] object_stack;

    private int[] level_stack;

    private int max_stack_size = 0;

    private int stack_size = 0;

    /** <code>AlternativeTypeInfo</code> for <code>boolean</code> arrays. */
    private static AlternativeTypeInfo booleanArrayInfo
            = AlternativeTypeInfo.getAlternativeTypeInfo(classBooleanArray);

    /** <code>AlternativeTypeInfo</code> for <code>byte</code> arrays. */
    private static AlternativeTypeInfo byteArrayInfo
            = AlternativeTypeInfo.getAlternativeTypeInfo(classByteArray);

    /** <code>AlternativeTypeInfo</code> for <code>char</code> arrays. */
    private static AlternativeTypeInfo charArrayInfo
            = AlternativeTypeInfo.getAlternativeTypeInfo(classCharArray);

    /** <code>AlternativeTypeInfo</code> for <code>short</code> arrays. */
    private static AlternativeTypeInfo shortArrayInfo
            = AlternativeTypeInfo.getAlternativeTypeInfo(classShortArray);

    /** <code>AlternativeTypeInfo</code> for <code>int</code> arrays. */
    private static AlternativeTypeInfo intArrayInfo
            = AlternativeTypeInfo.getAlternativeTypeInfo(classIntArray);

    /** <code>AlternativeTypeInfo</code> for <code>long</code> arrays. */
    private static AlternativeTypeInfo longArrayInfo
            = AlternativeTypeInfo.getAlternativeTypeInfo(classLongArray);

    /** <code>AlternativeTypeInfo</code> for <code>float</code> arrays. */
    private static AlternativeTypeInfo floatArrayInfo
            = AlternativeTypeInfo.getAlternativeTypeInfo(classFloatArray);

    /** <code>AlternativeTypeInfo</code> for <code>double</code> arrays. */
    private static AlternativeTypeInfo doubleArrayInfo
            = AlternativeTypeInfo.getAlternativeTypeInfo(classDoubleArray);

    /**
     * Constructor with a <code>DataInputStream</code>.
     * @param in		the underlying <code>DataInputStream</code>
     * @exception IOException	gets thrown when an IO error occurs.
     */
    public ObjectInputStream(DataInputStream in) throws IOException {
        super(in);
        objects = new IbisVector(1024);
        init(true);
    }

    /**
     * Constructor, may be used when this class is sub-classed.
     */
    protected ObjectInputStream() throws IOException {
        super();
        objects = new IbisVector(1024);
        init(true);
    }

    public boolean reInitOnNewConnection() {
        return true;
    }

    /*
     * If you at some point want to override ObjectOutputStream,
     * you probably need to override the methods from here on up until
     * comment tells you otherwise.
     */

    public String serializationImplName() {
        return "ibis";
    }

    public void close() throws IOException {
        objects.clear();
        objects = null;
        types = null;
        current_object = null;
        object_stack = null;
        level_stack = null;
        super.close();
    }

    /*
     * If you are overriding ObjectInputStream,
     * you can stop now :-) 
     * The rest is built on top of these.
     */

    public void readArray(boolean[] ref, int off, int len) throws IOException {
        if (TIME_IBIS_SERIALIZATION) {
            startTimer();
        }
        try {
            readArrayHeader(classBooleanArray, len);
        } catch (ClassNotFoundException e) {
            if (DEBUG && logger.isDebugEnabled() && logger.isDebugEnabled()) {
                logger.debug("Caught exception, rethrow as SerializationError",
                        e);
            }
            throw new SerializationError("require boolean[]", e);
        }
        readBooleanArray(ref, off, len);
        if (TIME_IBIS_SERIALIZATION) {
            stopTimer();
        }
    }

    public void readArray(byte[] ref, int off, int len) throws IOException {
        if (TIME_IBIS_SERIALIZATION) {
            startTimer();
        }
        try {
            readArrayHeader(classByteArray, len);
        } catch (ClassNotFoundException e) {
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("Caught exception, rethrow as SerializationError",
                        e);
            }
            throw new SerializationError("require byte[]", e);
        }
        readByteArray(ref, off, len);
        if (TIME_IBIS_SERIALIZATION) {
            stopTimer();
        }
    }

    public void readArray(char[] ref, int off, int len) throws IOException {
        if (TIME_IBIS_SERIALIZATION) {
            startTimer();
        }
        try {
            readArrayHeader(classCharArray, len);
        } catch (ClassNotFoundException e) {
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("Caught exception, rethrow as SerializationError",
                        e);
            }
            throw new SerializationError("require char[]", e);
        }
        readCharArray(ref, off, len);
        if (TIME_IBIS_SERIALIZATION) {
            stopTimer();
        }
    }

    public void readArray(short[] ref, int off, int len) throws IOException {
        if (TIME_IBIS_SERIALIZATION) {
            startTimer();
        }
        try {
            readArrayHeader(classShortArray, len);
        } catch (ClassNotFoundException e) {
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("Caught exception, rethrow as SerializationError",
                        e);
            }
            throw new SerializationError("require short[]", e);
        }
        readShortArray(ref, off, len);
        if (TIME_IBIS_SERIALIZATION) {
            stopTimer();
        }
    }

    public void readArray(int[] ref, int off, int len) throws IOException {
        if (TIME_IBIS_SERIALIZATION) {
            startTimer();
        }
        try {
            readArrayHeader(classIntArray, len);
        } catch (ClassNotFoundException e) {
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("Caught exception, rethrow as SerializationError",
                        e);
            }
            throw new SerializationError("require int[]", e);
        }
        readIntArray(ref, off, len);
        if (TIME_IBIS_SERIALIZATION) {
            stopTimer();
        }
    }

    public void readArray(long[] ref, int off, int len) throws IOException {
        if (TIME_IBIS_SERIALIZATION) {
            startTimer();
        }
        try {
            readArrayHeader(classLongArray, len);
        } catch (ClassNotFoundException e) {
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("Caught exception, rethrow as SerializationError",
                        e);
            }
            throw new SerializationError("require long[]", e);
        }
        readLongArray(ref, off, len);
        if (TIME_IBIS_SERIALIZATION) {
            stopTimer();
        }
    }

    public void readArray(float[] ref, int off, int len) throws IOException {
        if (TIME_IBIS_SERIALIZATION) {
            startTimer();
        }
        try {
            readArrayHeader(classFloatArray, len);
        } catch (ClassNotFoundException e) {
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("Caught exception, rethrow as SerializationError",
                        e);
            }
            throw new SerializationError("require float[]", e);
        }
        readFloatArray(ref, off, len);
        if (TIME_IBIS_SERIALIZATION) {
            stopTimer();
        }
    }

    public void readArray(double[] ref, int off, int len) throws IOException {
        if (TIME_IBIS_SERIALIZATION) {
            startTimer();
        }
        try {
            readArrayHeader(classDoubleArray, len);
        } catch (ClassNotFoundException e) {
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("Caught exception, rethrow as SerializationError",
                        e);
            }
            throw new SerializationError("require double[]", e);
        }
        readDoubleArray(ref, off, len);
        if (TIME_IBIS_SERIALIZATION) {
            stopTimer();
        }
    }

    public void readArray(Object[] ref, int off, int len) throws IOException,
            ClassNotFoundException {
        if (TIME_IBIS_SERIALIZATION) {
            startTimer();
        }
        readArrayHeader(ref.getClass(), len);
        for (int i = off; i < off + len; i++) {
            ref[i] = doReadObject(false);
        }
        if (TIME_IBIS_SERIALIZATION) {
            stopTimer();
        }
    }

    /**
     * Allocates and reads an array of bytes from the input stream.
     * This method is used by IOGenerator-generated code.
     * @return the array read.
     * @exception IOException in case of error.
     */
    public byte[] readArrayByte() throws IOException {
        if (TIME_IBIS_SERIALIZATION) {
            startTimer();
        }
        int len = readInt();
        byte[] b = new byte[len];
        addObjectToCycleCheck(b);
        readByteArray(b, 0, len);
        if (TIME_IBIS_SERIALIZATION) {
            stopTimer();
        }
        return b;
    }

    /**
     * See {@link #readArrayByte()}, this one is for an array of boolans.
     */
    public boolean[] readArrayBoolean() throws IOException {
        if (TIME_IBIS_SERIALIZATION) {
            startTimer();
        }
        int len = readInt();
        boolean[] b = new boolean[len];
        addObjectToCycleCheck(b);
        readBooleanArray(b, 0, len);
        if (TIME_IBIS_SERIALIZATION) {
            stopTimer();
        }
        return b;
    }

    /**
     * See {@link #readArrayByte()}, this one is for an array of chars.
     */
    public char[] readArrayChar() throws IOException {
        if (TIME_IBIS_SERIALIZATION) {
            startTimer();
        }
        int len = readInt();
        char[] b = new char[len];
        addObjectToCycleCheck(b);
        readCharArray(b, 0, len);
        if (TIME_IBIS_SERIALIZATION) {
            stopTimer();
        }
        return b;
    }

    /**
     * See {@link #readArrayByte()}, this one is for an array of shorts.
     */
    public short[] readArrayShort() throws IOException {
        if (TIME_IBIS_SERIALIZATION) {
            startTimer();
        }
        int len = readInt();
        short[] b = new short[len];
        addObjectToCycleCheck(b);
        readShortArray(b, 0, len);
        if (TIME_IBIS_SERIALIZATION) {
            stopTimer();
        }
        return b;
    }

    /**
     * See {@link #readArrayByte()}, this one is for an array of ints.
     */
    public int[] readArrayInt() throws IOException {
        if (TIME_IBIS_SERIALIZATION) {
            startTimer();
        }
        int len = readInt();
        int[] b = new int[len];
        addObjectToCycleCheck(b);
        readIntArray(b, 0, len);
        if (TIME_IBIS_SERIALIZATION) {
            stopTimer();
        }
        return b;
    }

    /**
     * See {@link #readArrayByte()}, this one is for an array of longs.
     */
    public long[] readArrayLong() throws IOException {
        if (TIME_IBIS_SERIALIZATION) {
            startTimer();
        }
        int len = readInt();
        long[] b = new long[len];
        addObjectToCycleCheck(b);
        readLongArray(b, 0, len);
        if (TIME_IBIS_SERIALIZATION) {
            stopTimer();
        }
        return b;
    }

    /**
     * See {@link #readArrayByte()}, this one is for an array of floats.
     */
    public float[] readArrayFloat() throws IOException {
        if (TIME_IBIS_SERIALIZATION) {
            startTimer();
        }
        int len = readInt();
        float[] b = new float[len];
        addObjectToCycleCheck(b);
        readFloatArray(b, 0, len);
        if (TIME_IBIS_SERIALIZATION) {
            stopTimer();
        }
        return b;
    }

    /**
     * See {@link #readArrayByte()}, this one is for an array of doubles.
     */
    public double[] readArrayDouble() throws IOException {
        if (TIME_IBIS_SERIALIZATION) {
            startTimer();
        }
        int len = readInt();
        double[] b = new double[len];
        addObjectToCycleCheck(b);
        readDoubleArray(b, 0, len);
        if (TIME_IBIS_SERIALIZATION) {
            stopTimer();
        }
        return b;
    }

    /**
     * Initializes the <code>objects</code> and <code>types</code> fields,
     * including their indices.
     *
     * @param do_types	set when the type table must be initialized as well
     *  (this is not needed after a reset).
     */
    private void init(boolean do_types) {
        if (do_types) {
            types = new IbisVector();
            types.add(0, null); // Vector requires this
            types.add(TYPE_BOOLEAN, booleanArrayInfo);
            types.add(TYPE_BYTE, byteArrayInfo);
            types.add(TYPE_CHAR, charArrayInfo);
            types.add(TYPE_SHORT, shortArrayInfo);
            types.add(TYPE_INT, intArrayInfo);
            types.add(TYPE_LONG, longArrayInfo);
            types.add(TYPE_FLOAT, floatArrayInfo);
            types.add(TYPE_DOUBLE, doubleArrayInfo);

            next_type = PRIMITIVE_TYPES;
        }

        objects.clear();
        next_handle = CONTROL_HANDLES;
    }

    /**
     * resets the stream, by clearing the object and type table.
     */
    private void do_reset(boolean cleartypes) {
        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("received reset: next handle = " + next_handle + ".");
        }
        init(cleartypes);
    }

    public void clear() {
        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("explicit clear: next handle = " + next_handle + ".");
        }
        init(false);
    }

    public void statistics() {
        System.err.println("ObjectInputStream: "
                + "statistics() not yet implemented");
    }

    /* This is the data output / object output part */

    /**
     * Reads a handle, which is just an int representing an index
     * in the object table.
     * @exception IOException	gets thrown when an IO error occurs.
     * @return 			the handle read.
     */
    private final int readHandle() throws IOException {
        int handle = readInt();

        /* this replaces the checks for the reset handle
         everywhere else. --N */
        for (;;) {
            if (handle == RESET_HANDLE) {
                if (DEBUG && logger.isDebugEnabled()) {
                    logger.debug("received a RESET");
                }
                do_reset(false);
                handle = readInt();
            } else if (handle == CLEAR_HANDLE) {
                if (DEBUG && logger.isDebugEnabled()) {
                    logger.debug("received a CLEAR");
                }
                do_reset(true);
                handle = readInt();
            } else {
                break;
            }
        }

        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("read handle " + handle);
        }

        return handle;
    }

    /**
     * Reads a <code>Class</code> object from the stream and tries to load it.
     * @exception IOException when an IO error occurs.
     * @exception ClassNotFoundException when the class could not be loaded.
     * @return the <code>Class</code> object read.
     */
    public Class readClass() throws IOException, ClassNotFoundException {
        if (TIME_IBIS_SERIALIZATION) {
            startTimer();
        }
        int handle = readHandle();

        if (handle == NUL_HANDLE) {
            if (TIME_IBIS_SERIALIZATION) {
                stopTimer();
            }
            return null;
        }

        if ((handle & TYPE_BIT) == 0) {
            /* Ah, it's a handle. Look it up, return the stored ptr */
            Class o = (Class) objects.get(handle);

            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("readobj: handle = " + (handle - CONTROL_HANDLES)
                        + " obj = " + o);
            }
            return o;
        }

        readType(handle & TYPE_MASK);

        String s = readUTF();
        Class c = getClassFromName(s);

        addObjectToCycleCheck(c);
        if (TIME_IBIS_SERIALIZATION) {
            stopTimer();
        }
        return c;
    }

    /**
     * Reads the header of an array.
     * This header consists of a handle, a type, and an integer representing
     * the length.
     * Note that the data read is mostly redundant.
     *
     * @exception IOException when an IO error occurs.
     * @exception ClassNotFoundException when the array class could
     *  not be loaded.
     */
    void readArrayHeader(Class clazz, int len) throws IOException,
            ClassNotFoundException {

        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("readArrayHeader: class = " + clazz.getName() + " len = "
                    + len);
        }
        int type = readHandle();

        if ((type & TYPE_BIT) == 0) {
            throw new StreamCorruptedException(
                    "Array slice header but I receive a HANDLE!");
        }

        Class in_clazz = readType(type & TYPE_MASK).clazz;
        int in_len = readInt();

        if (ASSERTS && !clazz.isAssignableFrom(in_clazz)) {
            throw new ClassCastException("Cannot assign class " + clazz
                    + " from read class " + in_clazz);
        }
        if (ASSERTS && in_len != len) {
            throw new ArrayIndexOutOfBoundsException("Cannot read " + in_len
                    + " into " + len + " elements");
        }
    }

    /**
     * Adds an object <code>o</code> to the object table, for cycle checking.
     * This method is public because it gets called from IOGenerator-generated
     * code.
     * @param o		the object to be added
     */
    public void addObjectToCycleCheck(Object o) {
        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("addObjectToCycleCheck: handle = " + next_handle);
        }
        if (unshared_handle == next_handle) {
            objects.add(next_handle, null);
            unshared_handle = 0;
        } else {
            objects.add(next_handle, o);
        }
        next_handle++;
    }

    /**
     * Looks up an object in the object table.
     * This method is public because it gets called from IOGenerator-generated
     * code.
     * @param handle	the handle of the object to be looked up
     * @return		the corresponding object.
     */
    public Object getObjectFromCycleCheck(int handle) {
        Object o = objects.get(handle); // - CONTROL_HANDLES);

        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("getObjectFromCycleCheck: handle = " + handle);
        }

        return o;
    }

    /**
     * Method used by IOGenerator-generated code to read a handle, and
     * determine if it has to read a new object or get one from the object
     * table.
     *
     * @exception IOException		when an IO error occurs.
     * @return	0 for a null object, -1 for a new object, and the handle for an
     * 		object already in the object table.
     */
    public final int readKnownTypeHeader() throws IOException,
            ClassNotFoundException {
        int handle_or_type = readHandle();

        if ((handle_or_type & TYPE_BIT) == 0) {
            // Includes NUL_HANDLE.
            if (DEBUG && logger.isDebugEnabled()) {
                if (handle_or_type == NUL_HANDLE) {
                    logger.debug("readKnownTypeHeader -> read NUL_HANDLE");
                } else {
                    logger.debug("readKnownTypeHeader -> read OLD HANDLE "
                            + handle_or_type);
                }
            }
            return handle_or_type;
        }

        handle_or_type &= TYPE_MASK;
        if (handle_or_type >= next_type) {
            readType(handle_or_type);
        }
        if (DEBUG && logger.isDebugEnabled()) {
            AlternativeTypeInfo t = (AlternativeTypeInfo) types.get(handle_or_type);
            logger.debug("readKnownTypeHeader -> reading NEW object, class = "
                    + t.clazz.getName());
        }
        return -1;
    }

    /**
     * Reads an array from the stream.
     * The handle and type have already been read.
     *
     * @param clazz		the type of the array to be read
     * @param type		an index in the types table, but
     * 				also an indication of the base type of
     * 				the array
     *
     * @exception IOException			when an IO error occurs.
     * @exception ClassNotFoundException	when element type is Object and
     * 						readObject throws it.
     *
     * @return the array read.
     */
    Object readArray(Class clazz, int type) throws IOException,
            ClassNotFoundException {

        if (DEBUG && logger.isDebugEnabled()) {
            if (clazz != null) {
                logger.debug("readArray " + clazz.getName() + " type " + type);
            }
        }

        switch (type) {
        case TYPE_BOOLEAN:
            return readArrayBoolean();
        case TYPE_BYTE:
            return readArrayByte();
        case TYPE_SHORT:
            return readArrayShort();
        case TYPE_CHAR:
            return readArrayChar();
        case TYPE_INT:
            return readArrayInt();
        case TYPE_LONG:
            return readArrayLong();
        case TYPE_FLOAT:
            return readArrayFloat();
        case TYPE_DOUBLE:
            return readArrayDouble();
        default:
            int len = readInt();
        	int dimension = 1;
        	Object ref = null;
        	// We have to find the inner type
        	String className = clazz.getName();
        	System.err.println(className);

        	// Determine the dimension of the array
        	while (className.charAt(dimension) != 'L') {
        		dimension++;
        	}
        	// We have to extract the inner type and use the generator
        	String typeName = className.substring(dimension + 1,className.length()-1);
        	System.err.println(typeName);
        	System.err.println(dimension);
        	ref = getSpecialCaseArray(typeName, len, dimension);
        	if (null == ref) {
        		AlternativeTypeInfo ati = AlternativeTypeInfo.getAlternativeTypeInfo(typeName);
        		Generator g = ati.gen;
        		if (null != g) {
        			System.err.println(g);
        			ref = ati.gen.new_array(len, dimension);
        		}
        		else {
        			throw new NotSerializableException("Array type not supported: " + typeName);
        		}
        	}

        	addObjectToCycleCheck(ref);

        	for (int i = 0; i < len; i++) {
        		Object o = doReadObject(false);
        		((Object[]) ref)[i] = o;
        	}
        	return ref;
        }
    }

	Object getSpecialCaseArray(String typeName, int len, int dimension) {
		Object ref = null;
    	if (typeName.equals("java.lang.Byte")) {
    		switch(dimension) {
    		case 1:
    			ref = new Byte[len];
    			break;
    		case 2:
    			ref = new Byte[len][];
    			break;
    		case 3:
    			ref = new Byte[len][][];
    			break;
    		case 4:
    			ref = new Byte[len][][][];
    			break;
    		case 5:
    			ref = new Byte[len][][][][];
    			break;
    		}
    	}
    	else if (typeName.equals("java.lang.Boolean")) {
    		switch(dimension) {
    		case 1:
    			ref = new Boolean[len];
    			break;
    		case 2:
    			ref = new Boolean[len][];
    			break;
    		case 3:
    			ref = new Boolean[len][][];
    			break;
    		case 4:
    			ref = new Boolean[len][][][];
    			break;
    		case 5:
    			ref = new Boolean[len][][][][];
    			break;
    		}
    	}
    	else if (typeName.equals("java.lang.Character")) {
    		switch(dimension) {
    		case 1:
    			ref = new Character[len];
    			break;
    		case 2:
    			ref = new Character[len][];
    			break;
    		case 3:
    			ref = new Character[len][][];
    			break;
    		case 4:
    			ref = new Character[len][][][];
    			break;
    		case 5:
    			ref = new Character[len][][][][];
    			break;
    		}
    	}
    	else if (typeName.equals("java.lang.Short")) {
    		switch(dimension) {
    		case 1:
    			ref = new Short[len];
    			break;
    		case 2:
    			ref = new Short[len][];
    			break;
    		case 3:
    			ref = new Short[len][][];
    			break;
    		case 4:
    			ref = new Short[len][][][];
    			break;
    		case 5:
    			ref = new Short[len][][][][];
    			break;
    		}
    	}
    	else if (typeName.equals("java.lang.Integer")) {
    		switch(dimension) {
    		case 1:
    			ref = new Integer[len];
    			break;
    		case 2:
    			ref = new Integer[len][];
    			break;
    		case 3:
    			ref = new Integer[len][][];
    			break;
    		case 4:
    			ref = new Integer[len][][][];
    			break;
    		case 5:
    			ref = new Integer[len][][][][];
    			break;
    		}
    	}
    	else if (typeName.equals("java.lang.Double")) {
    		switch(dimension) {
    		case 1:
    			ref = new Double[len];
    			break;
    		case 2:
    			ref = new Double[len][];
    			break;
    		case 3:
    			ref = new Double[len][][];
    			break;
    		case 4:
    			ref = new Double[len][][][];
    			break;
    		case 5:
    			ref = new Double[len][][][][];
    			break;
    		}
    	}
    	else if (typeName.equals("java.lang.Long")) {
    		switch(dimension) {
    		case 1:
    			ref = new Long[len];
    			break;
    		case 2:
    			ref = new Long[len][];
    			break;
    		case 3:
    			ref = new Long[len][][];
    			break;
    		case 4:
    			ref = new Long[len][][][];
    			break;
    		case 5:
    			ref = new Long[len][][][][];
    			break;
    		}
    	}
    	else if (typeName.equals("java.lang.Float")) {
    		switch(dimension) {
    		case 1:
    			ref = new Float[len];
    			break;
    		case 2:
    			ref = new Float[len][];
    			break;
    		case 3:
    			ref = new Float[len][][];
    			break;
    		case 4:
    			ref = new Float[len][][][];
    			break;
    		case 5:
    			ref = new Float[len][][][][];
    			break;
    		}
    	}
    	else if (typeName.equals("java.lang.String")) {
    		switch(dimension) {
    		case 1:
    			ref = new String[len];
    			break;
    		case 2:
    			ref = new String[len][];
    			break;
    		case 3:
    			ref = new String[len][][];
    			break;
    		case 4:
    			ref = new String[len][][][];
    			break;
    		case 5:
    			ref = new String[len][][][][];
    			break;
    		}
    	}
    	return ref;
	}

    /**
     * This method tries to load a class given its name. It tries the
     * default classloader, and the one from the thread context. Also,
     * apparently some classloaders do not understand array classes, and
     * from the Java documentation, it is not clear that they should.
     * Therefore, if the typeName indicates an array type, and the
     * obvious attempts to load the class fail, this method also tries
     * to load the base type of the array.
     *
     * @param typeName	the name of the type to be loaded
     * @exception ClassNotFoundException is thrown when the class could
     * not be loaded.
     * @return the loaded class
     */
    Class getClassFromName(String typeName)
            throws ClassNotFoundException {
    	try {
    		return Class.forName(typeName);
    	} catch (ClassNotFoundException e) {
    		int dim = 0;

    		/* Some classloaders are not able to load array classes.
    		 * Therefore, if the name
    		 * describes an array, try again with the base type.
    		 */
    		if (typeName.length() > 0 && typeName.charAt(0) == '[') {
    			char[] s = typeName.toCharArray();
    			while (dim < s.length && s[dim] == '[') {
    				dim++;
    			}
    			int begin = dim;
    			int end = s.length;
    			if (dim < s.length && s[dim] == 'L') {
    				begin++;
    			}
    			if (s[end - 1] == ';') {
    				end--;
    			}
    			typeName = typeName.substring(begin, end);

    			int dims[] = new int[dim];
    			for (int i = 0; i < dim; i++)
    				dims[i] = 0;

    			/* Now try to load the base class, create an array
    			 * from it and then return its class.
    			 */
    			/* TODO: We need to use a generator method here.
    			 * because we do not have the reflect Throw for now.
                    return java.lang.reflect.Array.newInstance(
                            getClassFromName(typeName), dims).getClass();
    			 */
    			throw e;
    		}
    		throw new ClassNotFoundException(typeName);
    	}
    }

    /**
     * Returns the <code>AlternativeTypeInfo</code> corresponding to the type
     * number given as parameter.
     * If the parameter indicates a type not yet read, its name is read
     * (as an UTF), and the class is loaded.
     *
     * @param type the type number
     * @exception ClassNotFoundException is thrown when the class could
     *  not be loaded.
     * @exception IOException is thrown when an IO error occurs
     * @return the <code>AlternativeTypeInfo</code> for <code>type</code>.
     */
    private AlternativeTypeInfo readType(int type) throws IOException,
            ClassNotFoundException {
        if (type < next_type) {
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("read type number 0x" + Integer.toHexString(type));
            }
            return (AlternativeTypeInfo) types.get(type);
        }

        if (next_type != type) {
            throw new SerializationError("Internal error: next_type = "
                    + next_type + ", type = " + type);
        }

        String typeName = readUTF();

        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("read NEW type number 0x" + Integer.toHexString(type)
                    + " type " + typeName);
        }

        Class clazz = getClassFromName(typeName);

        AlternativeTypeInfo t = AlternativeTypeInfo.getAlternativeTypeInfo(clazz);

        types.add(next_type, t);
        next_type++;

        return t;
    }

    /**
     * This method reads a value from the stream and assigns it to a
     * final field.
     * IOGenerator uses this method when assigning final fields of an
     * object that is rewritten, but super is not, and super is serializable.
     * The problem with this situation is that IOGenerator cannot create
     * a proper constructor for this object, so cannot assign
     * to final fields without falling back to native code.
     *
     * @param ref		object with a final field
     * @param fieldname		name of the field
     * @param classname         the name of the class
     * @exception IOException	is thrown when an IO error occurs.
     */
    public void readFieldDouble(Object ref, String fieldname, String classname)
            throws IOException {
        double d = readDouble();

        throw new IOException("No unsafe");
    }

    /**
     * See {@link #readFieldDouble(Object, String, String)} for a description.
     */
    public void readFieldLong(Object ref, String fieldname, String classname)
            throws IOException {
        long d = readLong();

        throw new IOException("No unsafe");
    }

    /**
     * See {@link #readFieldDouble(Object, String, String)} for a description.
     */
    public void readFieldFloat(Object ref, String fieldname, String classname)
            throws IOException {
        float d = readFloat();

        throw new IOException("No unsafe");
    }

    /**
     * See {@link #readFieldDouble(Object, String, String)} for a description.
     */
    public void readFieldInt(Object ref, String fieldname, String classname)
            throws IOException {
        int d = readInt();

        throw new IOException("No unsafe");
    }

    /**
     * See {@link #readFieldDouble(Object, String, String)} for a description.
     */
    public void readFieldShort(Object ref, String fieldname, String classname)
            throws IOException {
        short d = readShort();

        throw new IOException("No unsafe");
    }

    /**
     * See {@link #readFieldDouble(Object, String, String)} for a description.
     */
    public void readFieldChar(Object ref, String fieldname, String classname)
            throws IOException {
        char d = readChar();

        throw new IOException("No unsafe");
    }

    /**
     * See {@link #readFieldDouble(Object, String, String)} for a description.
     */
    public void readFieldByte(Object ref, String fieldname, String classname)
            throws IOException {
        byte d = readByte();

        throw new IOException("No unsafe");
    }

    /**
     * See {@link #readFieldDouble(Object, String, String)} for a description.
     */
    public void readFieldBoolean(Object ref, String fieldname, String classname)
            throws IOException {
        boolean d = readBoolean();

        throw new IOException("No unsafe");
    }

    /**
     * See {@link #readFieldDouble(Object, String, String)} for a description.
     */
    public void readFieldString(Object ref, String fieldname, String classname)
            throws IOException {
        String d = readString();

        throw new IOException("No unsafe");
    }

    /**
     * See {@link #readFieldDouble(Object, String, String)} for a description.
     * @exception ClassNotFoundException when the class could not be loaded.
     */
    public void readFieldClass(Object ref, String fieldname, String classname)
            throws IOException, ClassNotFoundException {
        Class d = readClass();

        throw new IOException("No unsafe");
    }

    /**
     * See {@link #readFieldDouble(Object, String, String)} for a description.
     * @param fieldsig	signature of the field
     * @exception ClassNotFoundException when readObject throws it.
     */
    public void readFieldObject(Object ref, String fieldname, String classname,
            String fieldsig) throws IOException, ClassNotFoundException {
        Object d = doReadObject(false);

        throw new IOException("No unsafe");
    }

    /**
     * Creates an uninitialized object of the type indicated by
     * <code>classname</code>.
     * The corresponding constructor called is the parameter-less
     * constructor of the "highest" superclass that is not serializable.
     *
     * @param classname		name of the class
     * @exception ClassNotFoundException when class <code>classname</code>
     *  cannot be loaded.
     */
    public Object create_uninitialized_object(String classname)
            throws ClassNotFoundException, IOException {
        Class clazz = getClassFromName(classname);
        return create_uninitialized_object(clazz);
    }

    Object create_uninitialized_object(Class clazz) throws IOException {
        AlternativeTypeInfo t
                = AlternativeTypeInfo.getAlternativeTypeInfo(clazz);

        if (STATS_NONREWRITTEN) {
            Integer n = (Integer)nonRewritten.get(clazz);
            if (n == null) {
                n = new Integer(1);
            } else {
                n = new Integer(n.intValue() + 1);
            }
            nonRewritten.put(clazz, n);
        }

        Object o = t.newInstance();

        if (o != null) {
            addObjectToCycleCheck(o);
            return o;
        }

        throw new IOException("newInstance failed");
    }

    /**
     * Push the notions of <code>current_object</code> and
     * <code>current_level</code> on their stacks, and set new ones.
     * @param ref	the new <code>current_object</code> notion
     * @param level	the new <code>current_level</code> notion
     */
    public void push_current_object(Object ref, int level) {
        if (stack_size >= max_stack_size) {
            max_stack_size = 2 * max_stack_size + 10;
            Object[] new_o_stack = new Object[max_stack_size];
            int[] new_l_stack = new int[max_stack_size];
            for (int i = 0; i < stack_size; i++) {
                new_o_stack[i] = object_stack[i];
                new_l_stack[i] = level_stack[i];
            }
            object_stack = new_o_stack;
            level_stack = new_l_stack;
        }
        object_stack[stack_size] = current_object;
        level_stack[stack_size] = current_level;
        stack_size++;
        current_object = ref;
        current_level = level;
    }

    /**
     * Pop the notions of <code>current_object</code> and
     * <code>current_level</code> from their stacks.
     */
    public void pop_current_object() {
        stack_size--;
        current_object = object_stack[stack_size];
        current_level = level_stack[stack_size];
        // Don't keep references around ...
        object_stack[stack_size] = null;
    }

    /**
     * Reads and returns a <code>String</code> object. This is a special case,
     * because strings are written as an UTF.
     *
     * @exception IOException   gets thrown on IO error
     * @return the string read.
     */
    public String readString() throws IOException {
        if (TIME_IBIS_SERIALIZATION) {
            startTimer();
        }
        int handle = readHandle();

        if (handle == NUL_HANDLE) {
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("readString: --> null");
            }
            if (TIME_IBIS_SERIALIZATION) {
                stopTimer();
            }
            return null;
        }

        if ((handle & TYPE_BIT) == 0) {
            /* Ah, it's a handle. Look it up, return the stored ptr */
            String o = (String) objects.get(handle);

            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("readString: duplicate handle = " + handle
                        + " string = " + o);
            }
            if (TIME_IBIS_SERIALIZATION) {
                stopTimer();
            }
            return o;
        }

        try {
            readType(handle & TYPE_MASK);
        } catch (ClassNotFoundException e) {
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("Caught exception, rethrow as SerializationError", e);
            }
            throw new SerializationError("Cannot find java.lang.String?", e);
        }

        String s = readUTF();
        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("readString returns " + s);
        }
        addObjectToCycleCheck(s);
        if (TIME_IBIS_SERIALIZATION) {
            stopTimer();
        }
        return s;
    }

    public Object readObject() throws IOException, ClassNotFoundException {
        return doReadObject(false);
    }
    
    public Object readObject(Class clazz) throws IOException, ClassNotFoundException {
    	return doReadObject(false, clazz);
    }

    final Object doReadObject(boolean unshared) throws IOException, ClassNotFoundException {
    	return doReadObject(unshared, null);
    }
    
    final Object doReadObject(boolean unshared, Class clazz) throws IOException,
            ClassNotFoundException {
        /*
         * ref < 0:    type
         * ref = 0:    null ptr
         * ref > 0:    handle
         */

        if (TIME_IBIS_SERIALIZATION) {
            startTimer();
        }
        int handle_or_type = readHandle();

        if (handle_or_type == NUL_HANDLE) {
            if (TIME_IBIS_SERIALIZATION) {
                stopTimer();
            }
            return null;
        }

        if ((handle_or_type & TYPE_BIT) == 0) {
            // Ah, it's a handle. Look it up, return the stored ptr,
            // unless it should be unshared.
            if (unshared) {
                if (TIME_IBIS_SERIALIZATION) {
                    stopTimer();
                }
                throw new InvalidObjectException(
                        "readUnshared got a handle instead of an object");
            }
            Object o = objects.get(handle_or_type);

            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("readObject: duplicate handle " + handle_or_type
                        + " class = " + o.getClass());
            }
            if (TIME_IBIS_SERIALIZATION) {
                stopTimer();
            }

            if (o == null) {
                throw new InvalidObjectException(
                        "readObject got handle " + handle_or_type + " to unshared object");
            }
            return o;
        }

        if (unshared) {
            unshared_handle = next_handle;
        }

        int type = handle_or_type & TYPE_MASK;
        AlternativeTypeInfo t;
        if (clazz == null) {
        	t = readType(type);
        }
        else {
        	t = AlternativeTypeInfo.getAlternativeTypeInfo(clazz);
        }

        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("start readObject of class " + t.clazz.getName()
                    + " handle = " + next_handle);
        }

        Object obj = t.readerWriter.readObject(this, t, type);

        if (TIME_IBIS_SERIALIZATION) {
            stopTimer();
        }

        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("finished readObject of class " + t.clazz.getName());
        }

        return obj;
    }

    private JavaObjectInputStream objectStream = null;

    public ObjectInput getJavaObjectInputStream()
            throws IOException {
        if (objectStream == null) {
            objectStream = new JavaObjectInputStream(this);
        }
        return objectStream;
    }

    private class JavaObjectInputStream extends java.io.InputStream implements ObjectInput {

        ObjectInputStream ibisStream;

        JavaObjectInputStream(ObjectInputStream s)
                throws IOException {
            super();
            ibisStream = s;
        }

        public int available() throws IOException {
            return ibisStream.available();
        }

        public void close() throws IOException {
            ibisStream.close();
        }

        public int read() throws IOException {
            int b;
            try {
                b = ibisStream.readByte();
                return b & 0377;
            } catch(EOFException e) {
                return -1;
            }
        }

        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        public int read(byte[] b, int off, int len) throws IOException {
            ibisStream.readArray(b, off, len);
            return len;
        }

        public Object readObjectOverride()
                throws IOException, ClassNotFoundException {
            return ibisStream.doReadObject(false);
        }

        /**
         * Ignored for Ibis serialization.
         */
        protected void readStreamHeader() {
            // ignored
        }

        protected ObjectStreamClass readClassDescriptor()
                throws IOException, ClassNotFoundException {
            Class cl = ibisStream.readClass();
            return new ObjectStreamClass(cl);
        }

        public void readFully(byte[] b) throws IOException {
            ibisStream.readArray(b);
        }

        public void readFully(byte[] b, int off, int len) throws IOException {
            ibisStream.readArray(b, off, len);
        }

        public String readLine() throws IOException {
            // Now really deprecated :-)
            return null;
        }

        public Object readUnshared()
                throws IOException, ClassNotFoundException {
            return doReadObject(true);
        }

        public Class resolveClass(ObjectStreamClass desc)
                  throws IOException, ClassNotFoundException {
                return desc.forClass();
        }

        public int skipBytes(int len) throws IOException {
            throw new SerializationError("skipBytes not implemented");
        }

        public long skip(long len) throws IOException {
            throw new SerializationError("skip not implemented");
        }

        public boolean markSupported() {
            return false;
        }

        public void mark(int readLimit) {
            // nothing
        }

        public void reset() throws IOException {
            throw new IOException("mark/reset not supported");
        }
        
        public String readUTF() throws IOException {
            return ibisStream.readUTF();
        }

        public byte readByte() throws IOException {
            return ibisStream.readByte();
        }

        public int readUnsignedByte() throws IOException {
            return ibisStream.readUnsignedByte();
        }

        public boolean readBoolean() throws IOException {
            return ibisStream.readBoolean();
        }

        public short readShort() throws IOException {
            return ibisStream.readShort();
        }

        public int readUnsignedShort() throws IOException {
            return ibisStream.readUnsignedShort();
        }

        public char readChar() throws IOException {
            return ibisStream.readChar();
        }

        public int readInt() throws IOException {
            return ibisStream.readInt();
        }

        public long readLong() throws IOException {
            return ibisStream.readLong();
        }

        public float readFloat() throws IOException {
            return ibisStream.readFloat();
        }

        public double readDouble() throws IOException {
            return ibisStream.readDouble();
        }

        public void defaultReadObject()
                throws ClassNotFoundException, IOException, NotActiveException {
            if (current_object == null) {
                throw new NotActiveException(
                        "defaultReadObject without a current object");
            }
            Object ref = current_object;
            Class type = ref.getClass();
            AlternativeTypeInfo t
                    = AlternativeTypeInfo.getAlternativeTypeInfo(type);

            if (t.isJMESerializable) {
                if (DEBUG && logger.isDebugEnabled()) {
                    logger.debug("generated_DefaultReadObject, class = " + type
                            + ", level = " + current_level);
                }
                ((JMESerializable) ref).generated_JME_DefaultReadObject(ibisStream,
                        current_level);
            } else {
                throw new NotSerializableException("Not Serializable : "
                        + type.toString());
            }
        }

		public void clear() {
			// TODO Auto-generated method stub
			
		}

		public boolean reInitOnNewConnection() {
			// TODO Auto-generated method stub
			return false;
		}

		public void readArray(Object[] ref) throws IOException, ClassNotFoundException {
			// TODO Auto-generated method stub
			
		}

		public void readArray(Object[] ref, int off, int len) throws IOException, ClassNotFoundException {
			// TODO Auto-generated method stub
			
		}

		public Object readObject() throws IOException, ClassNotFoundException {
			// TODO Auto-generated method stub
			return null;
		}

		public String readString() throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		public void realClose() throws IOException {
			// TODO Auto-generated method stub
			
		}

		public String serializationImplName() {
			// TODO Auto-generated method stub
			return null;
		}

		public void statistics() {
			// TODO Auto-generated method stub
			
		}

		public void readArray(boolean[] destination, int offset, int length) throws IOException {
			// TODO Auto-generated method stub
			
		}

		public void readArray(byte[] destination, int offset, int length) throws IOException {
			// TODO Auto-generated method stub
			
		}

		public void readArray(char[] destination, int offset, int length) throws IOException {
			// TODO Auto-generated method stub
			
		}

		public void readArray(short[] destination, int offset, int length) throws IOException {
			// TODO Auto-generated method stub
			
		}

		public void readArray(int[] destination, int offset, int length) throws IOException {
			// TODO Auto-generated method stub
			
		}

		public void readArray(long[] destination, int offset, int length) throws IOException {
			// TODO Auto-generated method stub
			
		}

		public void readArray(float[] destination, int offset, int length) throws IOException {
			// TODO Auto-generated method stub
			
		}

		public void readArray(double[] destination, int offset, int length) throws IOException {
			// TODO Auto-generated method stub
			
		}

		public void readArray(boolean[] source) throws IOException {
			// TODO Auto-generated method stub
			
		}

		public void readArray(byte[] source) throws IOException {
			// TODO Auto-generated method stub
			
		}

		public void readArray(char[] source) throws IOException {
			// TODO Auto-generated method stub
			
		}

		public void readArray(short[] source) throws IOException {
			// TODO Auto-generated method stub
			
		}

		public void readArray(int[] source) throws IOException {
			// TODO Auto-generated method stub
			
		}

		public void readArray(long[] source) throws IOException {
			// TODO Auto-generated method stub
			
		}

		public void readArray(float[] source) throws IOException {
			// TODO Auto-generated method stub
			
		}

		public void readArray(double[] source) throws IOException {
			// TODO Auto-generated method stub
			
		}
    }
}
