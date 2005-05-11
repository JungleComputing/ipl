/* $Id$ */

package ibis.io;

import ibis.util.TypedProperties;

import java.io.EOFException;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.NotActiveException;
import java.io.NotSerializableException;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.lang.reflect.Field;

import sun.misc.Unsafe;

/**
 * This is the <code>SerializationInputStream</code> version that is used
 * for Ibis serialization.
 */
public class IbisSerializationInputStream extends DataSerializationInputStream
        implements IbisStreamFlags {
    /** If <code>false</code>, makes all timer calls disappear. */
    private static final boolean TIME_IBIS_SERIALIZATION = true;

    /**
     * Record how many objects of any class are sent the expensive way:
     * via the uninitialized native creator.
     */
    private static final boolean STATS_NONREWRITTEN
            = TypedProperties.booleanProperty(IOProps.s_stats_nonrewritten);

    // if STATS_NONREWRITTEN
    private static java.util.Hashtable nonRewritten = new java.util.Hashtable();

    // Only works as of Java 1.4, earlier versions of Java don't have Unsafe.
    private static Unsafe unsafe = null;

    static {
        try {
            // unsafe = Unsafe.getUnsafe();
            // does not work when a classloader is present, so we get it
            // from ObjectStreamClass.
            Class cl
                = Class.forName("java.io.ObjectStreamClass$FieldReflector");
            Field uf = cl.getDeclaredField("unsafe");
            uf.setAccessible(true);
            unsafe = (Unsafe) uf.get(null);
        } catch (Exception e) {
            System.out.println("Got exception while getting unsafe: " + e);
            unsafe = null;
        }
        if (STATS_NONREWRITTEN) {
            System.out.println("IbisSerializationInputStream.STATS_NONREWRITTEN"
                    + " enabled");
            Runtime.getRuntime().addShutdownHook(
                    new Thread("IbisSerializationInputStream ShutdownHook") {
                        public void run() {
                            System.out.print("Serializable objects created "
                                    + "nonrewritten: ");
                            System.out.println(nonRewritten);
                        }
                    });
        }
    }

    private static ClassLoader customClassLoader;

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
    private Object current_object;

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
    private int current_level;

    /**
     * The <code>current_object</code> and <code>current_level</code>
     * are maintained in
     * stacks, so that they can be managed by IOGenerator-generated code.
     */
    private Object[] object_stack;

    private int[] level_stack;

    private int max_stack_size = 0;

    private int stack_size = 0;

    /** <code>IbisTypeInfo</code> for <code>boolean</code> arrays. */
    private static IbisTypeInfo booleanArrayInfo
            = IbisTypeInfo.getIbisTypeInfo(classBooleanArray);

    /** <code>IbisTypeInfo</code> for <code>byte</code> arrays. */
    private static IbisTypeInfo byteArrayInfo
            = IbisTypeInfo.getIbisTypeInfo(classByteArray);

    /** <code>IbisTypeInfo</code> for <code>char</code> arrays. */
    private static IbisTypeInfo charArrayInfo
            = IbisTypeInfo.getIbisTypeInfo(classCharArray);

    /** <code>IbisTypeInfo</code> for <code>short</code> arrays. */
    private static IbisTypeInfo shortArrayInfo
            = IbisTypeInfo.getIbisTypeInfo(classShortArray);

    /** <code>IbisTypeInfo</code> for <code>int</code> arrays. */
    private static IbisTypeInfo intArrayInfo
            = IbisTypeInfo.getIbisTypeInfo(classIntArray);

    /** <code>IbisTypeInfo</code> for <code>long</code> arrays. */
    private static IbisTypeInfo longArrayInfo
            = IbisTypeInfo.getIbisTypeInfo(classLongArray);

    /** <code>IbisTypeInfo</code> for <code>float</code> arrays. */
    private static IbisTypeInfo floatArrayInfo
            = IbisTypeInfo.getIbisTypeInfo(classFloatArray);

    /** <code>IbisTypeInfo</code> for <code>double</code> arrays. */
    private static IbisTypeInfo doubleArrayInfo
            = IbisTypeInfo.getIbisTypeInfo(classDoubleArray);

    static {
        String clName = System.getProperty(IOProps.s_classloader);
        if (clName != null) {
            //we try to instanciate it
            try {
                Class classDefinition = Class.forName(clName);
                customClassLoader = (ClassLoader) classDefinition.newInstance();
            } catch (Exception e) {
                System.err.println("Warning: could not find or load custom "
                        + "classloader " + clName);
                if (DEBUG) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Constructor with a <code>DataInputStream</code>.
     * @param in		the underlying <code>DataInputStream</code>
     * @exception IOException	gets thrown when an IO error occurs.
     */
    public IbisSerializationInputStream(DataInputStream in) throws IOException {
        super(in);
        objects = new IbisVector(1024);
        init(true);
    }

    /**
     * Constructor, may be used when this class is sub-classed.
     */
    protected IbisSerializationInputStream() throws IOException {
        super();
        objects = new IbisVector(1024);
        init(true);
    }

    public boolean reInitOnNewConnection() {
        return true;
    }

    /*
     * If you at some point want to override IbisSerializationOutputStream,
     * you probably need to override the methods from here on up until
     * comment tells you otherwise.
     */

    public String serializationImplName() {
        return "ibis";
    }

    public void close() throws IOException {
        super.close();
        types = null;
        objects.clear();
    }

    /*
     * If you are overriding IbisSerializationInputStream,
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
            if (DEBUG) {
                dbPrint("Caught exception: " + e);
                e.printStackTrace();
                dbPrint("now rethrow as SerializationError ...");
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
            if (DEBUG) {
                dbPrint("Caught exception: " + e);
                e.printStackTrace();
                dbPrint("now rethrow as SerializationError ...");
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
            if (DEBUG) {
                dbPrint("Caught exception: " + e);
                e.printStackTrace();
                dbPrint("now rethrow as SerializationError ...");
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
            if (DEBUG) {
                dbPrint("Caught exception: " + e);
                e.printStackTrace();
                dbPrint("now rethrow as SerializationError ...");
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
            if (DEBUG) {
                dbPrint("Caught exception: " + e);
                e.printStackTrace();
                dbPrint("now rethrow as SerializationError ...");
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
            if (DEBUG) {
                dbPrint("Caught exception: " + e);
                e.printStackTrace();
                dbPrint("now rethrow as SerializationError ...");
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
            if (DEBUG) {
                dbPrint("Caught exception: " + e);
                e.printStackTrace();
                dbPrint("now rethrow as SerializationError ...");
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
            if (DEBUG) {
                dbPrint("Caught exception: " + e);
                e.printStackTrace();
                dbPrint("now rethrow as SerializationError ...");
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
    private void do_reset() {
        if (DEBUG) {
            dbPrint("received reset: next handle = " + next_handle + ".");
        }
        init(false);
    }

    public void clear() {
        if (DEBUG) {
            dbPrint("explicit clear: next handle = " + next_handle + ".");
        }
        init(false);
    }

    public void statistics() {
        System.err.println("IbisSerializationInputStream: "
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
        while (handle == RESET_HANDLE) {
            if (DEBUG) {
                dbPrint("received a RESET");
            }
            do_reset();
            handle = readInt();
        }

        if (DEBUG) {
            dbPrint("read handle " + handle);
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

            if (DEBUG) {
                dbPrint("readobj: handle = " + (handle - CONTROL_HANDLES)
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
    private void readArrayHeader(Class clazz, int len) throws IOException,
            ClassNotFoundException {

        if (DEBUG) {
            dbPrint("readArrayHeader: class = " + clazz.getName() + " len = "
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
        if (DEBUG) {
            dbPrint("addObjectToCycleCheck: handle = " + next_handle);
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

        if (DEBUG) {
            dbPrint("getObjectFromCycleCheck: handle = " + handle);
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
            if (DEBUG) {
                if (handle_or_type == NUL_HANDLE) {
                    dbPrint("readKnownTypeHeader -> read NUL_HANDLE");
                } else {
                    dbPrint("readKnownTypeHeader -> read OLD HANDLE "
                            + handle_or_type);
                }
            }
            return handle_or_type;
        }

        handle_or_type &= TYPE_MASK;
        if (handle_or_type >= next_type) {
            readType(handle_or_type);
        }
        if (DEBUG) {
            IbisTypeInfo t = (IbisTypeInfo) types.get(handle_or_type);
            dbPrint("readKnownTypeHeader -> reading NEW object, class = "
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
    private Object readArray(Class clazz, int type) throws IOException,
            ClassNotFoundException {

        if (DEBUG) {
            if (clazz != null) {
                dbPrint("readArray " + clazz.getName() + " type " + type);
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
            Object ref = java.lang.reflect.Array.newInstance(
                    clazz.getComponentType(), len);
            addObjectToCycleCheck(ref);

            for (int i = 0; i < len; i++) {
                Object o = doReadObject(false);
                ((Object[]) ref)[i] = o;
            }

            return ref;
        }
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
    private Class getClassFromName(String typeName)
            throws ClassNotFoundException {
        try {
            return Class.forName(typeName);
        } catch (ClassNotFoundException e) {
            try {
                if (DEBUG) {
                    dbPrint("Could not load class " + typeName
                            + " using Class.forName(), trying "
                            + "Thread.currentThread()."
                            + "getContextClassLoader().loadClass()");
                    dbPrint("Default class loader is "
                            + this.getClass().getClassLoader());
                    dbPrint("now trying "
                            + Thread.currentThread().getContextClassLoader());
                }
                return Thread.currentThread().getContextClassLoader()
                        .loadClass(typeName);
            } catch (ClassNotFoundException e2) {
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
                    return java.lang.reflect.Array.newInstance(
                            getClassFromName(typeName), dims).getClass();
                }
                return loadClassFromCustomCL(typeName);
            }
        }
    }

    private Class loadClassFromCustomCL(String className)
            throws ClassNotFoundException {
        if (DEBUG) {
            System.out.println("loadClassTest " + className);
        }
        if (customClassLoader == null) {
            throw new ClassNotFoundException(className);
        }
        if (DEBUG) {
            System.out.println("******* Calling custom classloader");
        }
        return customClassLoader.loadClass(className);
    }

    /**
     * Returns the <code>IbisTypeInfo</code> corresponding to the type
     * number given as parameter.
     * If the parameter indicates a type not yet read, its name is read
     * (as an UTF), and the class is loaded.
     *
     * @param type the type number
     * @exception ClassNotFoundException is thrown when the class could
     *  not be loaded.
     * @exception IOException is thrown when an IO error occurs
     * @return the <code>IbisTypeInfo</code> for <code>type</code>.
     */
    private IbisTypeInfo readType(int type) throws IOException,
            ClassNotFoundException {
        if (type < next_type) {
            if (DEBUG) {
                dbPrint("read type number 0x" + Integer.toHexString(type));
            }
            return (IbisTypeInfo) types.get(type);
        }

        if (next_type != type) {
            throw new SerializationError("Internal error: next_type != type");
        }

        String typeName = readUTF();

        if (DEBUG) {
            dbPrint("read NEW type number 0x" + Integer.toHexString(type)
                    + " type " + typeName);
        }

        Class clazz = getClassFromName(typeName);

        IbisTypeInfo t = IbisTypeInfo.getIbisTypeInfo(clazz);

        types.add(next_type, t);
        next_type++;

        return t;
    }

    /**
     * Native methods needed for assigning to final fields of objects that are
     * not rewritten.
     */
    private native void setFieldDouble(Object ref, String fieldname, double d);

    private native void setFieldLong(Object ref, String fieldname, long l);

    private native void setFieldFloat(Object ref, String fieldname, float f);

    private native void setFieldInt(Object ref, String fieldname, int i);

    private native void setFieldShort(Object ref, String fieldname, short s);

    private native void setFieldChar(Object ref, String fieldname, char c);

    private native void setFieldByte(Object ref, String fieldname, byte b);

    private native void setFieldBoolean(Object ref, String fieldname,
            boolean b);

    private native void setFieldObject(Object ref, String fieldname,
            String osig, Object o);

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
     * @exception IOException	is thrown when an IO error occurs.
     */
    public void readFieldDouble(Object ref, String fieldname)
            throws IOException {
        double d = readDouble();
        if (unsafe != null) {
            Class cl = ref.getClass();
            try {
                Field f = cl.getDeclaredField(fieldname);
                int key = unsafe.fieldOffset(f);
                unsafe.putDouble(ref, key, d);
                return;
            } catch (Exception e) {
                // throw new InternalError("No such field " + fieldname
                //         + " in " cl.getName());
            }
        }
        setFieldDouble(ref, fieldname, d);
    }

    /**
     * See {@link #readFieldDouble(Object, String)} for a description.
     */
    public void readFieldLong(Object ref, String fieldname) throws IOException {
        long d = readLong();
        if (unsafe != null) {
            Class cl = ref.getClass();
            try {
                Field f = cl.getDeclaredField(fieldname);
                int key = unsafe.fieldOffset(f);
                unsafe.putLong(ref, key, d);
                return;
            } catch (Exception e) {
                // throw new InternalError("No such field " + fieldname
                //         + " in " cl.getName());
            }
        }
        setFieldLong(ref, fieldname, d);
    }

    /**
     * See {@link #readFieldDouble(Object, String)} for a description.
     */
    public void readFieldFloat(Object ref, String fieldname)
            throws IOException {
        float d = readFloat();
        if (unsafe != null) {
            Class cl = ref.getClass();
            try {
                Field f = cl.getDeclaredField(fieldname);
                int key = unsafe.fieldOffset(f);
                unsafe.putFloat(ref, key, d);
                return;
            } catch (Exception e) {
                // throw new InternalError("No such field " + fieldname
                //         + " in " cl.getName());
            }
        }
        setFieldFloat(ref, fieldname, d);
    }

    /**
     * See {@link #readFieldDouble(Object, String)} for a description.
     */
    public void readFieldInt(Object ref, String fieldname) throws IOException {
        int d = readInt();
        if (unsafe != null) {
            Class cl = ref.getClass();
            try {
                Field f = cl.getDeclaredField(fieldname);
                int key = unsafe.fieldOffset(f);
                unsafe.putInt(ref, key, d);
                return;
            } catch (Exception e) {
                // throw new InternalError("No such field " + fieldname
                //         + " in " cl.getName());
            }
        }
        setFieldInt(ref, fieldname, d);
    }

    /**
     * See {@link #readFieldDouble(Object, String)} for a description.
     */
    public void readFieldShort(Object ref, String fieldname)
            throws IOException {
        short d = readShort();
        if (unsafe != null) {
            Class cl = ref.getClass();
            try {
                Field f = cl.getDeclaredField(fieldname);
                int key = unsafe.fieldOffset(f);
                unsafe.putShort(ref, key, d);
                return;
            } catch (Exception e) {
                // throw new InternalError("No such field " + fieldname
                //         + " in " cl.getName());
            }
        }
        setFieldShort(ref, fieldname, d);
    }

    /**
     * See {@link #readFieldDouble(Object, String)} for a description.
     */
    public void readFieldChar(Object ref, String fieldname) throws IOException {
        char d = readChar();
        if (unsafe != null) {
            Class cl = ref.getClass();
            try {
                Field f = cl.getDeclaredField(fieldname);
                int key = unsafe.fieldOffset(f);
                unsafe.putChar(ref, key, d);
                return;
            } catch (Exception e) {
                // throw new InternalError("No such field " + fieldname
                //         + " in " cl.getName());
            }
        }
        setFieldChar(ref, fieldname, d);
    }

    /**
     * See {@link #readFieldDouble(Object, String)} for a description.
     */
    public void readFieldByte(Object ref, String fieldname) throws IOException {
        byte d = readByte();
        if (unsafe != null) {
            Class cl = ref.getClass();
            try {
                Field f = cl.getDeclaredField(fieldname);
                int key = unsafe.fieldOffset(f);
                unsafe.putByte(ref, key, d);
                return;
            } catch (Exception e) {
                // throw new InternalError("No such field " + fieldname
                //         + " in " cl.getName());
            }
        }
        setFieldByte(ref, fieldname, d);
    }

    /**
     * See {@link #readFieldDouble(Object, String)} for a description.
     */
    public void readFieldBoolean(Object ref, String fieldname)
            throws IOException {
        boolean d = readBoolean();
        if (unsafe != null) {
            Class cl = ref.getClass();
            try {
                Field f = cl.getDeclaredField(fieldname);
                int key = unsafe.fieldOffset(f);
                unsafe.putBoolean(ref, key, d);
                return;
            } catch (Exception e) {
                // throw new InternalError("No such field " + fieldname
                //         + " in " cl.getName());
            }
        }
        setFieldBoolean(ref, fieldname, d);
    }

    /**
     * See {@link #readFieldDouble(Object, String)} for a description.
     */
    public void readFieldString(Object ref, String fieldname)
            throws IOException {
        String d = readString();
        if (unsafe != null) {
            Class cl = ref.getClass();
            try {
                Field f = cl.getDeclaredField(fieldname);
                int key = unsafe.fieldOffset(f);
                unsafe.putObject(ref, key, d);
                return;
            } catch (Exception e) {
                // throw new InternalError("No such field " + fieldname
                //         + " in " cl.getName());
            }
        }
        setFieldObject(ref, fieldname, "Ljava/lang/String;", d);
    }

    /**
     * See {@link #readFieldDouble(Object, String)} for a description.
     * @exception ClassNotFoundException when the class could not be loaded.
     */
    public void readFieldClass(Object ref, String fieldname)
            throws IOException, ClassNotFoundException {
        Class d = readClass();
        if (unsafe != null) {
            Class cl = ref.getClass();
            try {
                Field f = cl.getDeclaredField(fieldname);
                int key = unsafe.fieldOffset(f);
                unsafe.putObject(ref, key, d);
                return;
            } catch (Exception e) {
                // throw new InternalError("No such field " + fieldname
                //         + " in " cl.getName());
            }
        }
        setFieldObject(ref, fieldname, "Ljava/lang/Class;", d);
    }

    /**
     * See {@link #readFieldDouble(Object, String)} for a description.
     * @param fieldsig	signature of the field
     * @exception ClassNotFoundException when readObject throws it.
     */
    public void readFieldObject(Object ref, String fieldname, String fieldsig)
            throws IOException, ClassNotFoundException {
        Object d = doReadObject(false);
        if (unsafe != null) {
            Class cl = ref.getClass();
            try {
                Field f = cl.getDeclaredField(fieldname);
                if (d != null && !f.getType().isInstance(d)) {
                    throw new ClassCastException("wrong field type");
                }
                int key = unsafe.fieldOffset(f);
                unsafe.putObject(ref, key, d);
                return;
            } catch (ClassCastException e) {
                throw e;
            } catch (Exception e) {
                // throw new InternalError("No such field " + fieldname
                //         + " in " cl.getName());
            }
        }
        setFieldObject(ref, fieldname, fieldsig, d);
    }

    /**
     * Reads the serializable fields of an object <code>ref</code> using the
     * type information <code>t</code>.
     *
     * @param t		the type info for object <code>ref</code>
     * @param ref	the object of which the fields are to be read
     *
     * @exception IOException		 when an IO error occurs
     * @exception IllegalAccessException when access to a field is denied.
     * @exception ClassNotFoundException when readObject throws it.
     */
    private void alternativeDefaultReadObject(AlternativeTypeInfo t, Object ref)
            throws ClassNotFoundException, IllegalAccessException, IOException {
        int temp = 0;
        if (DEBUG) {
            dbPrint("alternativeDefaultReadObject, class = "
                    + t.clazz.getName());
        }
        for (int i = 0; i < t.double_count; i++) {
            if (t.fields_final[temp]) {
                setFieldDouble(ref, t.serializable_fields[temp].getName(),
                        readDouble());
            } else {
                t.serializable_fields[temp].setDouble(ref, readDouble());
            }
            temp++;
        }
        for (int i = 0; i < t.long_count; i++) {
            if (t.fields_final[temp]) {
                setFieldLong(ref, t.serializable_fields[temp].getName(),
                        readLong());
            } else {
                t.serializable_fields[temp].setLong(ref, readLong());
            }
            temp++;
        }
        for (int i = 0; i < t.float_count; i++) {
            if (t.fields_final[temp]) {
                setFieldFloat(ref, t.serializable_fields[temp].getName(),
                        readFloat());
            } else {
                t.serializable_fields[temp].setFloat(ref, readFloat());
            }
            temp++;
        }
        for (int i = 0; i < t.int_count; i++) {
            if (t.fields_final[temp]) {
                setFieldInt(ref, t.serializable_fields[temp].getName(),
                        readInt());
            } else {
                t.serializable_fields[temp].setInt(ref, readInt());
            }
            temp++;
        }
        for (int i = 0; i < t.short_count; i++) {
            if (t.fields_final[temp]) {
                setFieldShort(ref, t.serializable_fields[temp].getName(),
                        readShort());
            } else {
                t.serializable_fields[temp].setShort(ref, readShort());
            }
            temp++;
        }
        for (int i = 0; i < t.char_count; i++) {
            if (t.fields_final[temp]) {
                setFieldChar(ref, t.serializable_fields[temp].getName(),
                        readChar());
            } else {
                t.serializable_fields[temp].setChar(ref, readChar());
            }
            temp++;
        }
        for (int i = 0; i < t.byte_count; i++) {
            if (t.fields_final[temp]) {
                setFieldByte(ref, t.serializable_fields[temp].getName(),
                        readByte());
            } else {
                t.serializable_fields[temp].setByte(ref, readByte());
            }
            temp++;
        }
        for (int i = 0; i < t.boolean_count; i++) {
            if (t.fields_final[temp]) {
                setFieldBoolean(ref, t.serializable_fields[temp].getName(),
                        readBoolean());
            } else {
                t.serializable_fields[temp].setBoolean(ref, readBoolean());
            }
            temp++;
        }
        for (int i = 0; i < t.reference_count; i++) {
            if (t.fields_final[temp]) {
                String fieldname = t.serializable_fields[temp].getName();
                String fieldtype
                        = t.serializable_fields[temp].getType().getName();

                if (fieldtype.startsWith("[")) {
                    // do nothing
                } else {
                    fieldtype = "L" + fieldtype.replace('.', '/') + ";";
                }

                // dbPrint("fieldname = " + fieldname);
                // dbPrint("signature = " + fieldtype);

                setFieldObject(ref, fieldname, fieldtype, doReadObject(false));
            } else {
                Object o = doReadObject(false);
                if (DEBUG) {
                    if (o == null) {
                        dbPrint("Assigning null to field "
                                + t.serializable_fields[temp].getName());
                    } else {
                        dbPrint("Assigning an object of type "
                                + o.getClass().getName() + " to field "
                                + t.serializable_fields[temp].getName());
                    }
                }
                t.serializable_fields[temp].set(ref, o);
            }
            temp++;
        }
    }

    /**
     * De-serializes an object <code>ref</code> using the type information
     * <code>t</code>.
     *
     * @param t the type info for object <code>ref</code>
     * @param ref the object of which the fields are to be read
     *
     * @exception IOException when an IO error occurs
     * @exception IllegalAccessException when access to a field or
     *   <code>readObject</code> method is
     *    denied.
     * @exception ClassNotFoundException when readObject throws it.
     */
    private void alternativeReadObject(AlternativeTypeInfo t, Object ref)
            throws ClassNotFoundException, IllegalAccessException, IOException {

        if (t.superSerializable) {
            alternativeReadObject(t.alternativeSuperInfo, ref);
        }

        if (t.hasReadObject) {
            current_level = t.level;
            try {
                if (DEBUG) {
                    dbPrint("invoking readObject() of class "
                            + t.clazz.getName());
                }
                t.invokeReadObject(ref, getJavaObjectInputStream());
                if (DEBUG) {
                    dbPrint("done with readObject() of class "
                            + t.clazz.getName());
                }
            } catch (java.lang.reflect.InvocationTargetException e) {
                if (DEBUG) {
                    dbPrint("Caught exception: " + e);
                    e.printStackTrace();
                }

                Throwable cause = e.getTargetException();
                if (cause instanceof Error) {
                    throw (Error) cause;
                }
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                }

                if (DEBUG) {
                    dbPrint("now rethrow as IllegalAccessException ...");
                }
                throw new IllegalAccessException("readObject method: " + e);
            }
            return;
        }
        alternativeDefaultReadObject(t, ref);
    }

    /**
     * This method takes care of reading the serializable fields of the
     * parent object, and also those of its parent objects.
     * Its gets called by IOGenerator-generated code when an object
     * has a superclass that is serializable but not Ibis serializable.
     *
     * @param ref	the object with a non-Ibis-serializable parent object
     * @param classname	the name of the superclass
     * @exception IOException	gets thrown on IO error
     * @exception ClassNotFoundException when readObject throws it.
     */
    public void readSerializableObject(Object ref, String classname)
            throws ClassNotFoundException, IOException {
        AlternativeTypeInfo t
                = AlternativeTypeInfo.getAlternativeTypeInfo(classname);
        push_current_object(ref, 0);
        try {
            alternativeReadObject(t, ref);
        } catch (IllegalAccessException e) {
            if (DEBUG) {
                dbPrint("Caught exception: " + e);
                e.printStackTrace();
                dbPrint("now rethrow as NotSerializableException ...");
            }
            throw new NotSerializableException(classname + " " + e);
        }
        pop_current_object();
    }

    /**
     * This method reads the serializable fields of object <code>ref</code>
     * at the level indicated by <code>depth</code> (see the explanation at
     * the declaration of the <code>current_level</code> field.
     * It gets called from IOGenerator-generated code, when a parent object
     * is serializable but not Ibis serializable.
     *
     * @param ref	the object of which serializable fields must be written
     * @param depth	an indication of the current "view" of the object
     * @exception IOException	gets thrown when an IO error occurs.
     * @exception ClassNotFoundException when readObject throws it.
     */
    public void defaultReadSerializableObject(Object ref, int depth)
            throws ClassNotFoundException, IOException {
        Class type = ref.getClass();
        AlternativeTypeInfo t = AlternativeTypeInfo.getAlternativeTypeInfo(type);

        /*  Find the type info corresponding to the current invocation.
         See the invokeReadObject invocation in alternativeReadObject.
         */
        while (t.level > depth) {
            t = t.alternativeSuperInfo;
        }
        try {
            alternativeDefaultReadObject(t, ref);
        } catch (IllegalAccessException e) {
            if (DEBUG) {
                dbPrint("Caught exception: " + e);
                e.printStackTrace();
                dbPrint("now rethrow as NotSerializableException ...");
            }
            throw new NotSerializableException(type + " " + e);
        }
    }

    /**
     * Native method for creating an uninitialized object.
     * We need such a method to call the right constructor for it,
     * which is the parameter-less constructor of the "highest" superclass
     * that is not serializable.
     * @param type the type of the object to be created
     * @param non_serializable_super the "highest" superclass of
     * <code>type</code> that is not serializable
     * @return the object created
     */
    private native Object createUninitializedObject(Class type,
            Class non_serializable_super);

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
            throws ClassNotFoundException {
        Class clazz = getClassFromName(classname);
        return create_uninitialized_object(clazz);
    }

    private Object create_uninitialized_object(Class clazz) {
        AlternativeTypeInfo t
                = AlternativeTypeInfo.getAlternativeTypeInfo(clazz);

        if (STATS_NONREWRITTEN) {
            Integer n = (Integer) nonRewritten.get(clazz);
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

        Class t2 = clazz;
        while (Serializable.class.isAssignableFrom(t2)) {
            /* Find first non-serializable super-class. */
            t2 = t2.getSuperclass();
        }
        // Calls constructor for non-serializable superclass.
        try {
            Object obj = createUninitializedObject(clazz, t2);
            addObjectToCycleCheck(obj);
            return obj;

        } catch (Throwable thro) {
            thro.printStackTrace();
            System.err.println("class: " + clazz.getName() + ",superclass: "
                    + t2.getName());
            throw new RuntimeException();
        }

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
            if (DEBUG) {
                dbPrint("readString: --> null");
            }
            if (TIME_IBIS_SERIALIZATION) {
                stopTimer();
            }
            return null;
        }

        if ((handle & TYPE_BIT) == 0) {
            /* Ah, it's a handle. Look it up, return the stored ptr */
            String o = (String) objects.get(handle);

            if (DEBUG) {
                dbPrint("readString: duplicate handle = " + handle
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
            if (DEBUG) {
                dbPrint("Caught exception: " + e);
                e.printStackTrace();
                dbPrint("now rethrow as SerializationError ...");
            }
            throw new SerializationError("Cannot find java.lang.String?", e);
        }

        String s = readUTF();
        if (DEBUG) {
            dbPrint("readString returns " + s);
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

    private final Object doReadObject(boolean unshared) throws IOException,
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

            if (DEBUG) {
                dbPrint("readObject: duplicate handle " + handle_or_type
                        + " class = " + o.getClass());
            }
            if (TIME_IBIS_SERIALIZATION) {
                stopTimer();
            }

            if (o == null) {
                throw new InvalidObjectException(
                        "readObject got handle to unshared object");
            }
            return o;
        }

        if (unshared) {
            unshared_handle = next_handle;
        }

        int type = handle_or_type & TYPE_MASK;
        IbisTypeInfo t = readType(type);

        if (DEBUG) {
            dbPrint("start readObject of class " + t.clazz.getName()
                    + " handle = " + next_handle);
        }

        Object obj;

        if (t.isArray) {
            obj = readArray(t.clazz, type);
        } else if (t.isString) {
            obj = readUTF();
            addObjectToCycleCheck(obj);
        } else if (t.isClass) {
            String name = readUTF();
            obj = getClassFromName(name);
            addObjectToCycleCheck(obj);
        } else if (t.gen != null) {
            obj = t.gen.generated_newInstance(this);
        } else if (Externalizable.class.isAssignableFrom(t.clazz)) {
            try {
                // Also calls parameter-less constructor
                obj = t.clazz.newInstance();
            } catch (Exception e) {
                if (DEBUG) {
                    dbPrint("Caught exception: " + e);
                    e.printStackTrace();
                    dbPrint("now rethrow as ClassNotFound ...");
                }
                throw new ClassNotFoundException("Could not instantiate" + e);
            }
            addObjectToCycleCheck(obj);
            push_current_object(obj, 0);
            ((java.io.Externalizable) obj).readExternal(
                    getJavaObjectInputStream());
            pop_current_object();
        } else {
            // obj = t.clazz.newInstance(); Not correct:
            // calls wrong constructor.
            obj = create_uninitialized_object(t.clazz);
            push_current_object(obj, 0);
            try {
                alternativeReadObject(t.altInfo, obj);
            } catch (IllegalAccessException e) {
                if (DEBUG) {
                    dbPrint("Caught exception: " + e);
                    e.printStackTrace();
                    dbPrint("now rethrow as NotSerializableException ...");
                }
                throw new NotSerializableException(type + " " + e);
            }
            pop_current_object();
        }
        if (TIME_IBIS_SERIALIZATION) {
            stopTimer();
        }

        if (DEBUG) {
            dbPrint("finished readObject of class " + t.clazz.getName());
        }

        return obj;
    }

    private JavaObjectInputStream objectStream = null;

    public java.io.ObjectInputStream getJavaObjectInputStream()
            throws IOException {
        if (objectStream == null) {
            objectStream = new JavaObjectInputStream(this);
        }
        return objectStream;
    }

    /**
     * Determines whether a class is Ibis-serializable.
     * We cannot use "instanceof ibis.io.Serializable", because that would
     * also return true if a parent class implements ibis.io.Serializable,
     * which is not good enough.
     *
     * @param clazz	the class to be tested
     * @return whether the class is ibis-serializable.
     */
    protected static boolean isIbisSerializable(Class clazz) {
        Class[] intfs = clazz.getInterfaces();

        for (int i = 0; i < intfs.length; i++) {
            if (intfs[i].equals(ibis.io.Serializable.class)) {
                return true;
            }
        }
        return false;
    }

    private class JavaObjectInputStream extends java.io.ObjectInputStream {

        IbisSerializationInputStream ibisStream;

        JavaObjectInputStream(IbisSerializationInputStream s)
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
            if (cl == null) {
                return null;
            }
            return ObjectStreamClass.lookup(cl);
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

        public void registerValidation(java.io.ObjectInputValidation obj,
                int prio) throws NotActiveException, InvalidObjectException {
            if (current_object != obj) {
                throw new NotActiveException("not in readObject");
            }
            throw new SerializationError("registerValidation not implemented");
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

        public GetField readFields()
                throws IOException, ClassNotFoundException {
            if (current_object == null) {
                throw new NotActiveException("not in readObject");
            }
            Class type = current_object.getClass();
            AlternativeTypeInfo t
                    = AlternativeTypeInfo.getAlternativeTypeInfo(type);
            ImplGetField current_getfield = new ImplGetField(t);
            current_getfield.readFields();
            return current_getfield;
        }

        /**
         * The Ibis serialization implementation of <code>GetField</code>.
         */
        private class ImplGetField extends GetField {
            private double[] doubles;

            private long[] longs;

            private int[] ints;

            private float[] floats;

            private short[] shorts;

            private char[] chars;

            private byte[] bytes;

            private boolean[] booleans;

            private Object[] references;

            private AlternativeTypeInfo t;

            ImplGetField(AlternativeTypeInfo t) {
                doubles = new double[t.double_count];
                longs = new long[t.long_count];
                ints = new int[t.int_count];
                shorts = new short[t.short_count];
                floats = new float[t.float_count];
                chars = new char[t.char_count];
                bytes = new byte[t.byte_count];
                booleans = new boolean[t.boolean_count];
                references = new Object[t.reference_count];
                this.t = t;
            }

            public ObjectStreamClass getObjectStreamClass() {
                /*  I don't know how it could be used here, but ... */
                return ObjectStreamClass.lookup(t.clazz);
            }

            public boolean defaulted(String name) {
                return false;
            }

            public boolean get(String name, boolean dflt) {
                return booleans[t.getOffset(name, Boolean.TYPE)];
            }

            public char get(String name, char dflt) {
                return chars[t.getOffset(name, Character.TYPE)];
            }

            public byte get(String name, byte dflt) {
                return bytes[t.getOffset(name, Byte.TYPE)];
            }

            public short get(String name, short dflt) {
                return shorts[t.getOffset(name, Short.TYPE)];
            }

            public int get(String name, int dflt) {
                return ints[t.getOffset(name, Integer.TYPE)];
            }

            public long get(String name, long dflt) {
                return longs[t.getOffset(name, Long.TYPE)];
            }

            public float get(String name, float dflt) {
                return floats[t.getOffset(name, Float.TYPE)];
            }

            public double get(String name, double dflt) {
                return doubles[t.getOffset(name, Double.TYPE)];
            }

            public Object get(String name, Object dflt) {
                return references[t.getOffset(name, Object.class)];
            }

            void readFields() throws IOException, ClassNotFoundException {
                for (int i = 0; i < t.double_count; i++) {
                    doubles[i] = ibisStream.readDouble();
                }
                for (int i = 0; i < t.float_count; i++) {
                    floats[i] = ibisStream.readFloat();
                }
                for (int i = 0; i < t.long_count; i++) {
                    longs[i] = ibisStream.readLong();
                }
                for (int i = 0; i < t.int_count; i++) {
                    ints[i] = ibisStream.readInt();
                }
                for (int i = 0; i < t.short_count; i++) {
                    shorts[i] = ibisStream.readShort();
                }
                for (int i = 0; i < t.char_count; i++) {
                    chars[i] = ibisStream.readChar();
                }
                for (int i = 0; i < t.byte_count; i++) {
                    bytes[i] = ibisStream.readByte();
                }
                for (int i = 0; i < t.boolean_count; i++) {
                    booleans[i] = ibisStream.readBoolean();
                }
                for (int i = 0; i < t.reference_count; i++) {
                    references[i] = ibisStream.doReadObject(false);
                }
            }
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

            if (ref instanceof ibis.io.Serializable) {
                // if (isIbisSerializable(type))
                if (DEBUG) {
                    dbPrint("generated_DefaultReadObject, class = " + type
                            + ", level = " + current_level);
                }
                ((ibis.io.Serializable) ref).generated_DefaultReadObject(ibisStream,
                        current_level);
            } else if (ref instanceof java.io.Serializable) {
                AlternativeTypeInfo t
                        = AlternativeTypeInfo.getAlternativeTypeInfo(type);

                /*  Find the type info corresponding to the current invocation.
                 *  See the invokeReadObject invocation in alternativeReadObject.
                 */
                while (t.level > current_level) {
                    t = t.alternativeSuperInfo;
                }
                try {
                    ibisStream.alternativeDefaultReadObject(t, ref);
                } catch (IllegalAccessException e) {
                    if (DEBUG) {
                        dbPrint("Caught exception: " + e);
                        e.printStackTrace();
                        dbPrint("now rethrow as NotSerializableException ...");
                    }
                    throw new NotSerializableException(type + " " + e);
                }
            } else {
                throw new NotSerializableException("Not Serializable : "
                        + type.toString());
            }
        }
    }
}
