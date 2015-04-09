/* $Id$ */

package ibis.io;

import java.io.IOException;
import java.io.NotActiveException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.nio.ByteBuffer;
import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the <code>SerializationOutputStream</code> version that is used for
 * Ibis serialization.
 */
public class IbisSerializationOutputStream extends
        DataSerializationOutputStream {
    private static final Logger logger = LoggerFactory
            .getLogger(IbisSerializationOutputStream.class);

    private static final boolean DEBUG = IOProperties.DEBUG;

    /** If <code>false</code>, makes all timer calls disappear. */
    private static final boolean TIME_IBIS_SERIALIZATION = IOProperties.properties
            .getBooleanProperty(IOProperties.s_timer_ibis);

    /** Record how many objects of any class are sent. */
    private static final boolean STATS_OBJECTS = IOProperties.properties
            .getBooleanProperty(IOProperties.s_stats_written);

    // if STATS_OBJECTS
    static Hashtable<Class<?>, Integer> statSendObjects;

    static final int[] statArrayCount;

    static int statObjectHandle;

    static final int[] statArrayHandle;

    static final long[] statArrayLength;

    static {
        if (STATS_OBJECTS) {
            Runtime.getRuntime().addShutdownHook(
                    new Thread("IbisSerializationOutputStream ShutdownHook") {
                        @Override
                        public void run() {
                            System.out.print("Serializable objects sent: ");
                            System.out.println(statSendObjects);
                            System.out.println("Non-array handles sent "
                                    + statObjectHandle);
                            for (int i = Constants.BEGIN_TYPES; i < Constants.PRIMITIVE_TYPES; i++) {
                                if (statArrayCount[i] + statArrayHandle[i] > 0) {
                                    System.out
                                            .println("       "
                                                    + primitiveName(i)
                                                    + " arrays "
                                                    + statArrayCount[i]
                                                    + " total bytes "
                                                    + (statArrayLength[i] * primitiveBytes(i))
                                                    + " handles "
                                                    + statArrayHandle[i]);
                                }
                            }
                        }
                    });
            System.out.println("IbisSerializationOutputStream.STATS_OBJECTS "
                    + "enabled");
            statSendObjects = new Hashtable<Class<?>, Integer>();
            statArrayCount = new int[Constants.PRIMITIVE_TYPES];
            statArrayHandle = new int[Constants.PRIMITIVE_TYPES];
            statArrayLength = new long[Constants.PRIMITIVE_TYPES];
        } else {
            statSendObjects = null;
            statArrayCount = null;
            statArrayLength = null;
            statArrayHandle = null;
        }
    }

    /** Accessed from IOGenerator-generated code. */
    public Replacer replacer;

    /** The first free object handle. */
    int next_handle;

    /** Hash table for keeping references to objects already written. */
    private HandleHash references = new HandleHash(2048);

    // private IbisHash references = new IbisHash(2048);

    /** Remember when a reset must be sent out. */
    private boolean resetPending = false;

    /** Remember when a clear must be sent out. */
    private boolean clearPending = false;

    /** The first free type index. */
    private int next_type;

    /** Hashtable for types already put on the stream. */
    private IbisHash types = new IbisHash();

    /**
     * There is a notion of a "current" object. This is needed when a
     * user-defined <code>writeObject</code> refers to
     * <code>defaultWriteObject</code> or to <code>putFields</code>.
     */
    Object current_object;

    /**
     * There also is a notion of a "current" level. The "level" of a
     * serializable class is computed as follows:
     * <ul>
     * <li>if its superclass is serializable: the level of the superclass + 1.
     * <li>if its superclass is not serializable: 1.
     * </ul>
     * This level implies a level at which an object can be seen. The "current"
     * level is the level at which <code>current_object</code> is being
     * processed.
     */
    int current_level;

    /**
     * There also is the notion of a "current" <code>PutField</code>, needed for
     * the <code>writeFields</code> method.
     */
    Object current_putfield;

    /**
     * The <code>current_object</code>, <code>current_level</code>, and
     * <code>current_putfield</code> are maintained in stacks, so that they can
     * be managed by IOGenerator-generated code.
     */
    private Object[] object_stack;

    private int[] level_stack;

    private Object[] putfield_stack;

    private int max_stack_size = 0;

    private int stack_size = 0;

    private Class<?> lastClass;
    private int lastTypeno;

    /**
     * Constructor with an <code>DataOutputStream</code>.
     * 
     * @param out
     *            the underlying <code>DataOutputStream</code>
     * @exception IOException
     *                gets thrown when an IO error occurs.
     */
    public IbisSerializationOutputStream(DataOutputStream out)
            throws IOException {
        super(out);

        types_clear();

        next_type = Constants.PRIMITIVE_TYPES;
        next_handle = Constants.CONTROL_HANDLES;
    }

    /**
     * Constructor, may be used when this class is sub-classed.
     */
    protected IbisSerializationOutputStream() throws IOException {
        super();
        types_clear();

        next_type = Constants.PRIMITIVE_TYPES;
        next_handle = Constants.CONTROL_HANDLES;
    }

    static String primitiveName(int i) {
        switch (i) {
        case Constants.TYPE_BOOLEAN:
            return "boolean";
        case Constants.TYPE_BYTE:
            return "byte";
        case Constants.TYPE_CHAR:
            return "char";
        case Constants.TYPE_SHORT:
            return "short";
        case Constants.TYPE_INT:
            return "int";
        case Constants.TYPE_LONG:
            return "long";
        case Constants.TYPE_FLOAT:
            return "float";
        case Constants.TYPE_DOUBLE:
            return "double";
        }

        return null;
    }

    static int primitiveBytes(int i) {
        switch (i) {
        case Constants.TYPE_BOOLEAN:
            return Constants.SIZEOF_BOOLEAN;
        case Constants.TYPE_BYTE:
            return Constants.SIZEOF_BYTE;
        case Constants.TYPE_CHAR:
            return Constants.SIZEOF_CHAR;
        case Constants.TYPE_SHORT:
            return Constants.SIZEOF_SHORT;
        case Constants.TYPE_INT:
            return Constants.SIZEOF_INT;
        case Constants.TYPE_LONG:
            return Constants.SIZEOF_LONG;
        case Constants.TYPE_FLOAT:
            return Constants.SIZEOF_FLOAT;
        case Constants.TYPE_DOUBLE:
            return Constants.SIZEOF_DOUBLE;
        }

        return 0;
    }

    private static int arrayClassType(Class<?> arrayClass) {
        if (false) {
            // nothing
        } else if (arrayClass == Constants.classByteArray) {
            return Constants.TYPE_BYTE;
        } else if (arrayClass == Constants.classIntArray) {
            return Constants.TYPE_INT;
        } else if (arrayClass == Constants.classBooleanArray) {
            return Constants.TYPE_BOOLEAN;
        } else if (arrayClass == Constants.classDoubleArray) {
            return Constants.TYPE_DOUBLE;
        } else if (arrayClass == Constants.classCharArray) {
            return Constants.TYPE_CHAR;
        } else if (arrayClass == Constants.classShortArray) {
            return Constants.TYPE_SHORT;
        } else if (arrayClass == Constants.classLongArray) {
            return Constants.TYPE_LONG;
        } else if (arrayClass == Constants.classFloatArray) {
            return Constants.TYPE_FLOAT;
        }
        return -1;
    }

    @Override
    public boolean reInitOnNewConnection() {
        return true;
    }

    /**
     * Set a replacer. The replacement mechanism can be used to replace an
     * object with another object during serialization. This is used in RMI, for
     * instance, to replace a remote object with a stub.
     * 
     * @param replacer
     *            the replacer object to be associated with this output stream
     */
    @Override
    public void setReplacer(Replacer replacer) throws IOException {
        this.replacer = replacer;
    }

    @Override
    public String serializationImplName() {
        return "ibis";
    }

    @Override
    public void statistics() {
        if (false) {
            System.err.print("IbisOutput: references -> ");
            references.statistics();
            System.err.print("IbisOutput: types      -> ");
            types.statistics();
        }
    }

    /**
     * Initializes the type hash by adding arrays of primitive types.
     */
    private void types_clear() {
        lastClass = null;
        types.clear();
        types.put(Constants.classBooleanArray, Constants.TYPE_BOOLEAN
                | Constants.TYPE_BIT);
        types.put(Constants.classByteArray, Constants.TYPE_BYTE
                | Constants.TYPE_BIT);
        types.put(Constants.classCharArray, Constants.TYPE_CHAR
                | Constants.TYPE_BIT);
        types.put(Constants.classShortArray, Constants.TYPE_SHORT
                | Constants.TYPE_BIT);
        types.put(Constants.classIntArray, Constants.TYPE_INT
                | Constants.TYPE_BIT);
        types.put(Constants.classLongArray, Constants.TYPE_LONG
                | Constants.TYPE_BIT);
        types.put(Constants.classFloatArray, Constants.TYPE_FLOAT
                | Constants.TYPE_BIT);
        types.put(Constants.classDoubleArray, Constants.TYPE_DOUBLE
                | Constants.TYPE_BIT);
        next_type = Constants.PRIMITIVE_TYPES;
    }

    @Override
    public void reset() {
        reset(false);
    }

    @Override
    public void reset(boolean cleartypes) {
        if (cleartypes || next_handle > Constants.CONTROL_HANDLES) {
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("reset: next handle = " + next_handle + ".");
            }
            references.clear();
            /*
             * We cannot send out the reset immediately, because the reader side
             * only accepts a reset when it is expecting a handle. So, instead,
             * we remember that we need to send out a reset, and send before
             * sending the next handle.
             */
            if (cleartypes) {
                clearPending = true;
            } else {
                resetPending = true;
            }
            next_handle = Constants.CONTROL_HANDLES;
        }
        if (cleartypes) {
            types_clear();
        }
    }

    /* This is the data output / object output part */

    /**
     * Called by IOGenerator-generated code to write a Class object to this
     * stream. For a Class object, only its name is written.
     * 
     * @param ref
     *            the <code>Class</code> to be written
     * @exception IOException
     *                gets thrown when an IO error occurs.
     */
    public void writeClass(Class<?> ref) throws IOException {
        if (TIME_IBIS_SERIALIZATION) {
            timer.start();
        }
        if (ref == null) {
            writeHandle(Constants.NUL_HANDLE);
            if (TIME_IBIS_SERIALIZATION) {
                timer.stop();
            }
            return;
        }
        int hashCode = HandleHash.getHashCode(ref);
        int handle = references.find(ref, hashCode);
        if (handle == 0) {
            assignHandle(ref, hashCode);
            writeType(java.lang.Class.class);
            writeUTF(ref.getName());
        } else {
            writeHandle(handle);
        }
        if (TIME_IBIS_SERIALIZATION) {
            timer.stop();
        }
    }

    /**
     * Sends out handles as normal int's. Also checks if we need to send out a
     * reset first.
     * 
     * @param v
     *            the handle to be written
     * @exception IOException
     *                gets thrown when an IO error occurs.
     */
    void writeHandle(int v) throws IOException {
        if (clearPending) {
            writeInt(Constants.CLEAR_HANDLE);
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("wrote a CLEAR");
            }
            resetPending = false;
            clearPending = false;
        } else if (resetPending) {
            writeInt(Constants.RESET_HANDLE);
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("wrote a RESET");
            }
            resetPending = false;
        }

        // treating handles as normal int's --N
        writeInt(v);
        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("wrote handle " + v);
        }
    }

    @Override
    public void writeArray(boolean[] ref, int off, int len) throws IOException {
        if (TIME_IBIS_SERIALIZATION) {
            timer.start();
        }
        if (writeArrayHeader(ref, Constants.classBooleanArray, len, false)) {
            writeArrayBoolean(ref, off, len);
        }
        if (TIME_IBIS_SERIALIZATION) {
            timer.stop();
        }
    }

    @Override
    public void writeArray(byte[] ref, int off, int len) throws IOException {
        if (TIME_IBIS_SERIALIZATION) {
            timer.start();
        }
        if (writeArrayHeader(ref, Constants.classByteArray, len, false)) {
            writeArrayByte(ref, off, len);
        }
        if (TIME_IBIS_SERIALIZATION) {
            timer.stop();
        }
    }

    @Override
    public void writeByteBuffer(ByteBuffer ref) throws IOException {
        if (TIME_IBIS_SERIALIZATION) {
            timer.start();
        }
        if (ref == null) {
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("writeByteBuffer: --> null");
            }
            writeHandle(Constants.NUL_HANDLE);
            if (TIME_IBIS_SERIALIZATION) {
                timer.stop();
            }
            return;
        }

        int handle = references.lazyPut(ref, next_handle);
        if (handle == next_handle) {
            next_handle++;
            writeType(java.nio.ByteBuffer.class);
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("writeString: " + ref);
            }
            writeInt(ref.capacity());
            writeBoolean(ref.isDirect());
            internalWriteByteBuffer(ref);
        } else {
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("writeByteBuffer: duplicate handle " + handle);
            }
            writeHandle(handle);

        }
        if (TIME_IBIS_SERIALIZATION) {
            timer.stop();
        }
    }

    @Override
    public void writeArray(short[] ref, int off, int len) throws IOException {
        if (TIME_IBIS_SERIALIZATION) {
            timer.start();
        }
        if (writeArrayHeader(ref, Constants.classShortArray, len, false)) {
            writeArrayShort(ref, off, len);
        }
        if (TIME_IBIS_SERIALIZATION) {
            timer.stop();
        }
    }

    @Override
    public void writeArray(char[] ref, int off, int len) throws IOException {
        if (TIME_IBIS_SERIALIZATION) {
            timer.start();
        }
        if (writeArrayHeader(ref, Constants.classCharArray, len, false)) {
            writeArrayChar(ref, off, len);
        }
        if (TIME_IBIS_SERIALIZATION) {
            timer.stop();
        }
    }

    @Override
    public void writeArray(int[] ref, int off, int len) throws IOException {
        if (TIME_IBIS_SERIALIZATION) {
            timer.start();
        }
        if (writeArrayHeader(ref, Constants.classIntArray, len, false)) {
            writeArrayInt(ref, off, len);
        }
        if (TIME_IBIS_SERIALIZATION) {
            timer.stop();
        }
    }

    @Override
    public void writeArray(long[] ref, int off, int len) throws IOException {
        if (TIME_IBIS_SERIALIZATION) {
            timer.start();
        }
        if (writeArrayHeader(ref, Constants.classLongArray, len, false)) {
            writeArrayLong(ref, off, len);
        }
        if (TIME_IBIS_SERIALIZATION) {
            timer.stop();
        }
    }

    @Override
    public void writeArray(float[] ref, int off, int len) throws IOException {
        if (TIME_IBIS_SERIALIZATION) {
            timer.start();
        }
        if (writeArrayHeader(ref, Constants.classFloatArray, len, false)) {
            writeArrayFloat(ref, off, len);
        }
        if (TIME_IBIS_SERIALIZATION) {
            timer.stop();
        }
    }

    @Override
    public void writeArray(double[] ref, int off, int len) throws IOException {
        if (TIME_IBIS_SERIALIZATION) {
            timer.start();
        }
        if (writeArrayHeader(ref, Constants.classDoubleArray, len, false)) {
            writeArrayDouble(ref, off, len);
        }
        if (TIME_IBIS_SERIALIZATION) {
            timer.stop();
        }
    }

    @Override
    public void writeArray(Object[] ref, int off, int len) throws IOException {
        if (TIME_IBIS_SERIALIZATION) {
            timer.start();
        }
        Class<?> clazz = ref.getClass();
        if (writeArrayHeader(ref, clazz, len, false)) {
            for (int i = off; i < off + len; i++) {
                doWriteObject(ref[i]);
            }
        }
        if (TIME_IBIS_SERIALIZATION) {
            timer.stop();
        }
    }

    /**
     * Writes a type or a handle. If <code>ref</code> has been written before,
     * this method writes its handle and returns <code>true</code>. If not, its
     * type is written, a new handle is associated with it, and
     * <code>false</code> is returned.
     * 
     * @param ref
     *            the object that is going to be put on the stream
     * @param clazz
     *            the <code>Class</code> representing the type of
     *            <code>ref</code>
     * @exception IOException
     *                gets thrown when an IO error occurs.
     */
    private boolean writeTypeHandle(Object ref, Class<?> clazz)
            throws IOException {
        int handle = references.lazyPut(ref, next_handle);

        if (handle != next_handle) {
            writeHandle(handle);
            return true;
        }

        writeType(clazz);
        next_handle++;

        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("writeTypeHandle: references[" + handle + "] = "
                    + (ref == null ? "null" : ref));
        }

        return false;
    }

    /**
     * Writes a handle or an array header, depending on wether a cycle should be
     * and was detected. If a cycle was detected, it returns <code>false</code>,
     * otherwise <code>true</code>. The array header consists of a type and a
     * length.
     * 
     * @param ref
     *            the array to be written
     * @param clazz
     *            the <code>Class</code> representing the array type
     * @param len
     *            the number of elements to be written
     * @param doCycleCheck
     *            set when cycles should be detected
     * @exception IOException
     *                gets thrown when an IO error occurs.
     * @return <code>true</code> if no cycle was or should be detected (so that
     *         the array should be written).
     */
    private boolean writeArrayHeader(Object ref, Class<?> clazz, int len,
            boolean doCycleCheck) throws IOException {
        if (ref == null) {
            writeHandle(Constants.NUL_HANDLE);
            return false;
        }

        if (doCycleCheck) {
            /* A complete array. Do cycle/duplicate detection */
            if (writeTypeHandle(ref, clazz)) {
                return false;
            }
        } else {
            writeType(clazz);
        }

        writeInt(len);

        addStatSendArrayHandle(ref, len);

        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("writeArrayHeader " + clazz.getName() + " length = "
                    + len);
        }
        return true;
    }

    /**
     * Writes an array, but possibly only a handle.
     * 
     * @param ref
     *            the array to be written
     * @param arrayClass
     *            the <code>Class</code> representing the array type
     * @param unshared
     *            set when no cycle detection check should be done
     * @exception IOException
     *                gets thrown when an IO error occurs.
     */
    void writeArray(Object ref, Class<?> arrayClass, boolean unshared)
            throws IOException {
        String s = arrayClass.getName();
        switch (s.charAt(1)) {
        case 'B': {
            byte[] a = (byte[]) ref;
            int len = a.length;
            if (writeArrayHeader(a, arrayClass, len, !unshared)) {
                writeArrayByte(a, 0, len);
            }
            break;
        }
        case 'I': {
            int[] a = (int[]) ref;
            int len = a.length;
            if (writeArrayHeader(a, arrayClass, len, !unshared)) {
                writeArrayInt(a, 0, len);
            }
            break;
        }
        case 'Z': {
            boolean[] a = (boolean[]) ref;
            int len = a.length;
            if (writeArrayHeader(a, arrayClass, len, !unshared)) {
                writeArrayBoolean(a, 0, len);
            }
            break;
        }
        case 'D': {
            double[] a = (double[]) ref;
            int len = a.length;
            if (writeArrayHeader(a, arrayClass, len, !unshared)) {
                writeArrayDouble(a, 0, len);
            }
            break;
        }
        case 'C': {
            char[] a = (char[]) ref;
            int len = a.length;
            if (writeArrayHeader(a, arrayClass, len, !unshared)) {
                writeArrayChar(a, 0, len);
            }
            break;
        }
        case 'S': {
            short[] a = (short[]) ref;
            int len = a.length;
            if (writeArrayHeader(a, arrayClass, len, !unshared)) {
                writeArrayShort(a, 0, len);
            }
            break;
        }
        case 'J': {
            long[] a = (long[]) ref;
            int len = a.length;
            if (writeArrayHeader(a, arrayClass, len, !unshared)) {
                writeArrayLong(a, 0, len);
            }
            break;
        }
        case 'F': {
            float[] a = (float[]) ref;
            int len = a.length;
            if (writeArrayHeader(a, arrayClass, len, !unshared)) {
                writeArrayFloat(a, 0, len);
            }
            break;
        }
        default: {
            Object[] a = (Object[]) ref;
            int len = a.length;
            if (writeArrayHeader(a, arrayClass, len, !unshared)) {
                for (int i = 0; i < len; i++) {
                    doWriteObject(a[i]);
                }
            }
        }
        }
    }

    /**
     * Adds the type represented by <code>clazz</code> to the type table and
     * returns its number.
     * 
     * @param clazz
     *            represents the type to be added
     * @return the type number.
     */
    private int newType(Class<?> clazz) {
        int type_number = next_type++;

        type_number = (type_number | Constants.TYPE_BIT);
        types.put(clazz, type_number);

        return type_number;
    }

    /**
     * Writes a type number, and, when new, a type name to the output stream.
     * 
     * @param clazz
     *            the clazz to be written.
     * @exception IOException
     *                gets thrown when an IO error occurs.
     */
    void writeType(Class<?> clazz) throws IOException {
        int type_number;

        if (clazz == lastClass) {
            type_number = lastTypeno;
        } else {
            type_number = types.find(clazz);
            lastClass = clazz;
            lastTypeno = type_number;
        }

        if (type_number != 0) {
            writeHandle(type_number); // TYPE_BIT is set, receiver sees it

            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("wrote type number 0x"
                        + Integer.toHexString(type_number));
            }
            return;
        }

        type_number = newType(clazz);
        lastTypeno = type_number;
        lastClass = clazz;
        writeHandle(type_number); // TYPE_BIT is set, receiver sees it
        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("wrote NEW type number 0x"
                    + Integer.toHexString(type_number) + " type "
                    + clazz.getName());
        }
        writeUTF(clazz.getName());
    }

    /**
     * Writes a (new or old) handle for object <code>ref</code> to the output
     * stream. Returns 1 if the object is new, -1 if not.
     * 
     * @param ref
     *            the object whose handle is to be written
     * @exception IOException
     *                gets thrown when an IO error occurs.
     * @return 1 if it is a new object, -1 if it is not.
     */
    public int writeKnownObjectHeader(Object ref) throws IOException {

        if (ref == null) {
            writeHandle(Constants.NUL_HANDLE);
            return 0;
        }

        int handle = references.lazyPut(ref, next_handle);
        if (handle == next_handle) {
            // System.err.write("+");
            Class<?> clazz = ref.getClass();
            next_handle++;
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("writeKnownObjectHeader -> writing NEW object, class = "
                        + clazz.getName());
            }
            writeType(clazz);
            return 1;
        }

        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("writeKnownObjectHeader -> writing OLD HANDLE "
                    + handle);
        }
        writeHandle(handle);

        return -1;
    }

    /**
     * Writes a (new or old) handle for array of primitives <code>ref</code> to
     * the output stream. Returns 1 if the object is new, -1 if not.
     * 
     * @param ref
     *            the object whose handle is to be written
     * @param typehandle
     *            the type number
     * @exception IOException
     *                gets thrown when an IO error occurs.
     * @return 1 if it is a new object, -1 if it is not.
     */
    public int writeKnownArrayHeader(Object ref, int typehandle)
            throws IOException {
        if (ref == null) {
            writeHandle(Constants.NUL_HANDLE);
            return 0;
        }

        int handle = references.lazyPut(ref, next_handle);
        if (handle == next_handle) {
            // System.err.write("+");
            next_handle++;
            writeInt(typehandle | Constants.TYPE_BIT);
            return 1;
        }

        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("writeKnownObjectHeader -> writing OLD HANDLE "
                    + handle);
        }
        writeHandle(handle);

        return -1;
    }

    /**
     * Writes the serializable fields of an object <code>ref</code> using the
     * type information <code>t</code>.
     * 
     * @param t
     *            the type info for object <code>ref</code>
     * @param ref
     *            the object of which the fields are to be written
     * 
     * @exception IOException
     *                when an IO error occurs
     * @exception IllegalAccessException
     *                when access to a field is denied.
     */
    void alternativeDefaultWriteObject(AlternativeTypeInfo t, Object ref)
            throws IOException, IllegalAccessException {
        int temp = 0;
        int i;

        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("alternativeDefaultWriteObject, class = "
                    + t.clazz.getName());
        }
        for (i = 0; i < t.double_count; i++) {
            writeDouble(t.serializable_fields[temp++].getDouble(ref));
        }
        for (i = 0; i < t.long_count; i++) {
            writeLong(t.serializable_fields[temp++].getLong(ref));
        }
        for (i = 0; i < t.float_count; i++) {
            writeFloat(t.serializable_fields[temp++].getFloat(ref));
        }
        for (i = 0; i < t.int_count; i++) {
            writeInt(t.serializable_fields[temp++].getInt(ref));
        }
        for (i = 0; i < t.short_count; i++) {
            writeShort(t.serializable_fields[temp++].getShort(ref));
        }
        for (i = 0; i < t.char_count; i++) {
            writeChar(t.serializable_fields[temp++].getChar(ref));
        }
        for (i = 0; i < t.byte_count; i++) {
            writeByte(t.serializable_fields[temp++].getByte(ref));
        }
        for (i = 0; i < t.boolean_count; i++) {
            writeBoolean(t.serializable_fields[temp++].getBoolean(ref));
        }
        for (i = 0; i < t.reference_count; i++) {
            doWriteObject(t.serializable_fields[temp++].get(ref));
        }
    }

    /**
     * Serializes an object <code>ref</code> using the type information
     * <code>t</code>.
     * 
     * @param t
     *            the type info for object <code>ref</code>
     * @param ref
     *            the object of which the fields are to be written
     * 
     * @exception IOException
     *                when an IO error occurs
     * @exception IllegalAccessException
     *                when access to a field or <code>writeObject</code> method
     *                is denied.
     */
    void alternativeWriteObject(AlternativeTypeInfo t, Object ref)
            throws IOException, IllegalAccessException {
        if (t.superSerializable) {
            alternativeWriteObject(t.alternativeSuperInfo, ref);
        }

        if (t.hasWriteObject) {
            current_level = t.level;
            try {
                if (DEBUG && logger.isDebugEnabled()) {
                    logger.debug("invoking writeObject() of class "
                            + t.clazz.getName());
                }
                t.invokeWriteObject(ref, getJavaObjectOutputStream());
                if (DEBUG && logger.isDebugEnabled()) {
                    logger.debug("done with writeObject() of class "
                            + t.clazz.getName());
                }
            } catch (java.lang.reflect.InvocationTargetException e) {
                if (DEBUG && logger.isDebugEnabled()) {
                    logger.debug("Caught exception", e);
                }
                Throwable cause = e.getTargetException();
                if (cause instanceof Error) {
                    throw (Error) cause;
                }
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                }
                if (cause instanceof IOException) {
                    throw (IOException) cause;
                }
                if (DEBUG && logger.isDebugEnabled()) {
                    logger.debug("now rethrow as IllegalAccessException ...");
                }
                throw new IbisIllegalAccessException("writeObject method", e);
            }
            return;
        }

        alternativeDefaultWriteObject(t, ref);
    }

    /**
     * Push the notions of <code>current_object</code>,
     * <code>current_level</code>, and <code>current_putfield</code> on their
     * stacks, and set new ones.
     * 
     * @param ref
     *            the new <code>current_object</code> notion
     * @param level
     *            the new <code>current_level</code> notion
     */
    public void push_current_object(Object ref, int level) {
        if (stack_size >= max_stack_size) {
            max_stack_size = 2 * max_stack_size + 10;
            Object[] new_o_stack = new Object[max_stack_size];
            int[] new_l_stack = new int[max_stack_size];
            Object[] new_p_stack = new Object[max_stack_size];
            for (int i = 0; i < stack_size; i++) {
                new_o_stack[i] = object_stack[i];
                new_l_stack[i] = level_stack[i];
                new_p_stack[i] = putfield_stack[i];
            }
            object_stack = new_o_stack;
            level_stack = new_l_stack;
            putfield_stack = new_p_stack;
        }
        object_stack[stack_size] = current_object;
        level_stack[stack_size] = current_level;
        putfield_stack[stack_size] = current_putfield;
        stack_size++;
        current_object = ref;
        current_level = level;
        current_putfield = null;
    }

    /**
     * Pop the notions of <code>current_object</code>,
     * <code>current_level</code>, and <code>current_putfield</code> from their
     * stacks.
     */
    public void pop_current_object() {
        stack_size--;
        current_object = object_stack[stack_size];
        current_level = level_stack[stack_size];
        current_putfield = putfield_stack[stack_size];
        // Don't keep references around ...
        object_stack[stack_size] = null;
        putfield_stack[stack_size] = null;
    }

    /**
     * This method takes care of writing the serializable fields of the parent
     * object, and also those of its parent objects. It gets called by
     * IOGenerator-generated code when an object has a superclass that is
     * serializable but not Ibis serializable.
     * 
     * @param ref
     *            the object with a non-Ibis-serializable parent object
     * @param classname
     *            the name of the superclass
     * @exception IOException
     *                gets thrown on IO error
     */
    public void writeSerializableObject(Object ref, String classname)
            throws IOException {
        AlternativeTypeInfo t;
        try {
            t = AlternativeTypeInfo.getAlternativeTypeInfo(classname);
        } catch (ClassNotFoundException e) {
            throw new SerializationError("Internal error", e);
        }
        try {
            push_current_object(ref, 0);
            alternativeWriteObject(t, ref);
            pop_current_object();
        } catch (IllegalAccessException e) {
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug(
                        "Caught exception, rethrow as NotSerializableException",
                        e);
            }
            throw new IbisNotSerializableException("Serializable failed for : "
                    + classname, e);
        }
    }

    /**
     * Writes a <code>String</code> object. This is a special case, because
     * strings are written as an UTF.
     * 
     * @param ref
     *            the string to be written
     * @exception IOException
     *                gets thrown on IO error
     */
    @Override
    public void writeString(String ref) throws IOException {
        if (TIME_IBIS_SERIALIZATION) {
            timer.start();
        }
        if (ref == null) {
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("writeString: --> null");
            }
            writeHandle(Constants.NUL_HANDLE);
            if (TIME_IBIS_SERIALIZATION) {
                timer.stop();
            }
            return;
        }

        int handle = references.lazyPut(ref, next_handle);
        if (handle == next_handle) {
            next_handle++;
            writeType(java.lang.String.class);
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("writeString: " + ref);
            }
            writeUTF(ref);
        } else {
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("writeString: duplicate handle " + handle
                        + " string = " + ref);
            }
            writeHandle(handle);
        }
        if (TIME_IBIS_SERIALIZATION) {
            timer.stop();
        }
    }

    static void addStatSendObject(Object ref) {
        if (STATS_OBJECTS) {
            Class<?> clazz = ref.getClass();
            Integer n = statSendObjects.get(clazz);
            if (n == null) {
                n = new Integer(1);
            } else {
                n = new Integer(n.intValue() + 1);
            }
            statSendObjects.put(clazz, n);
        }
    }

    private static void addStatSendObjectHandle(Object ref) {
        if (STATS_OBJECTS) {
            statObjectHandle++;
        }
    }

    @Override
    void addStatSendArray(Object ref, int type, int len) {
        if (STATS_OBJECTS) {
            addStatSendObject(ref);
            statArrayCount[type]++;
            statArrayLength[type] += len;
        }
    }

    private static void addStatSendArrayHandle(Object ref, int len) {
        if (STATS_OBJECTS) {
            Class<?> arrayClass = ref.getClass();
            int type = arrayClassType(arrayClass);
            if (type == -1) {
                statObjectHandle++;
            } else {
                statArrayHandle[type]++;
            }
        }
    }

    /**
     * Write objects and arrays. Duplicates are deteced when this call is used.
     * The replacement mechanism is implemented here as well.
     * 
     * @param ref
     *            the object to be written
     * @exception java.io.IOException
     *                is thrown when an IO error occurs.
     */
    @Override
    public void writeObject(Object ref) throws IOException {
        doWriteObject(ref);
    }

    @Override
    public void close() throws IOException {
        super.close();
        replacer = null;
        references = null;
        types = null;
        current_object = null;
        current_putfield = null;
        object_stack = null;
        level_stack = null;
        putfield_stack = null;
        lastClass = null;
    }

    void assignHandle(Object ref, int hashCode) {
        int handle = next_handle++;
        references.put(ref, handle, hashCode);
        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("assignHandle: references[" + handle + "] = " + ref);
        }
    }

    void doWriteObject(Object ref) throws IOException {
        /*
         * ref < 0: type ref = 0: null ptr ref > 0: handle
         */

        if (TIME_IBIS_SERIALIZATION) {
            timer.start();
        }
        if (ref == null) {
            writeHandle(Constants.NUL_HANDLE);
            if (TIME_IBIS_SERIALIZATION) {
                timer.stop();
            }
            return;
        }
        /*
         * TODO: deal with writeReplace! This should be done before looking up
         * the handle. If we don't want to do runtime inspection, this should
         * probably be handled somehow in IOGenerator. Note that the needed info
         * is available in AlternativeTypeInfo, but we don't want to use that
         * when we have ibis.io.Serializable.
         */

        if (replacer != null) {
            ref = replacer.replace(ref);
        }

        int hashCode = HandleHash.getHashCode(ref);
        int handle = references.find(ref, hashCode);

        if (handle == 0) {
            Class<?> clazz = ref.getClass();
            AlternativeTypeInfo t = AlternativeTypeInfo
                    .getAlternativeTypeInfo(clazz);
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("start writeObject of class " + clazz.getName()
                        + " handle = " + next_handle);
            }
            t.writer.writeObject(this, ref, t, hashCode, false);
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("finished writeObject of class " + clazz.getName());
            }
        } else {
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("writeObject: duplicate handle " + handle
                        + " class = " + ref.getClass());
            }
            writeHandle(handle);

            addStatSendObjectHandle(ref);
        }
        if (TIME_IBIS_SERIALIZATION) {
            timer.stop();
        }
    }

    /**
     * This method writes the serializable fields of object <code>ref</code> at
     * the level indicated by <code>depth</code>. (see the explanation at the
     * declaration of the <code>current_level</code> field). It gets called from
     * IOGenerator-generated code, when a parent object is serializable but not
     * Ibis serializable.
     * 
     * @param ref
     *            the object of which serializable fields must be written
     * @param depth
     *            an indication of the current "view" of the object
     * @exception IOException
     *                gets thrown when an IO error occurs.
     */
    public void defaultWriteSerializableObject(Object ref, int depth)
            throws IOException {
        Class<?> clazz = ref.getClass();
        AlternativeTypeInfo t = AlternativeTypeInfo
                .getAlternativeTypeInfo(clazz);

        /*
         * Find the type info corresponding to the current invocation. See the
         * invokeWriteObject invocation in alternativeWriteObject.
         */
        while (t.level > depth) {
            t = t.alternativeSuperInfo;
        }
        try {
            alternativeDefaultWriteObject(t, ref);
        } catch (IllegalAccessException e) {
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug(
                        "Caught exception, rethrow as NotSerializableException",
                        e);
            }
            throw new IbisNotSerializableException("illegal access", e);
        }
    }

    private JavaObjectOutputStream objectStream = null;

    public ObjectOutputStream getJavaObjectOutputStream() throws IOException {
        if (objectStream == null) {
            objectStream = new JavaObjectOutputStream(this);
        }
        return objectStream;
    }

    private class JavaObjectOutputStream extends ObjectOutputStream {

        IbisSerializationOutputStream ibisStream;

        JavaObjectOutputStream(IbisSerializationOutputStream s)
                throws IOException {
            super();
            ibisStream = s;
        }

        @Override
        public void writeObjectOverride(Object ref) throws IOException {
            ibisStream.doWriteObject(ref);
        }

        @Override
        public void defaultWriteObject() throws IOException, NotActiveException {
            if (current_object == null) {
                throw new NotActiveException("defaultWriteObject: no object");
            }

            Object ref = current_object;
            Class<?> clazz = ref.getClass();
            AlternativeTypeInfo t = AlternativeTypeInfo
                    .getAlternativeTypeInfo(clazz);

            if (t.isIbisSerializable) {
                /*
                 * Note that this will take the generated_DefaultWriteObject of
                 * the dynamic type of ref. The current_level variable actually
                 * indicates which instance of generated_DefaultWriteObject
                 * should do some work.
                 */
                if (DEBUG && logger.isDebugEnabled()) {
                    logger.debug("generated_DefaultWriteObject, class = "
                            + clazz.getName() + ", level = " + current_level);
                }
                ((Serializable) ref).generated_DefaultWriteObject(ibisStream,
                        current_level);
            } else if (ref instanceof java.io.Serializable) {
                /*
                 * Find the type info corresponding to the current invocation.
                 * See the invokeWriteObject invocation in
                 * alternativeWriteObject.
                 */
                while (t.level > current_level) {
                    t = t.alternativeSuperInfo;
                }
                try {
                    alternativeDefaultWriteObject(t, ref);
                } catch (IllegalAccessException e) {
                    if (DEBUG && logger.isDebugEnabled()) {
                        logger.debug(
                                "Caught exception, rethrow as NotSerializableException",
                                e);
                    }
                    throw new IbisNotSerializableException("illegal access", e);
                }
            } else {
                throw new IbisNotSerializableException("Not Serializable : "
                        + clazz.getName());
            }
        }

        @Override
        public void writeUnshared(Object ref) throws IOException {
            if (ref == null) {
                ibisStream.writeHandle(Constants.NUL_HANDLE);
                return;
            }
            /*
             * TODO: deal with writeReplace! This should be done before looking
             * up the handle. If we don't want to do runtime inspection, this
             * should probably be handled somehow in IOGenerator. Note that the
             * needed info is available in AlternativeTypeInfo, but we don't
             * want to use that when we have ibis.io.Serializable.
             */
            Class<?> clazz = ref.getClass();
            AlternativeTypeInfo t = AlternativeTypeInfo
                    .getAlternativeTypeInfo(clazz);

            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("start writeUnshared of class " + clazz.getName()
                        + " handle = " + next_handle);
            }

            t.writer.writeObject(ibisStream, ref, t, 0, true);

            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("finished writeUnshared of class "
                        + clazz.getName() + " handle = " + next_handle);
            }
        }

        @Override
        public void useProtocolVersion(int version) {
            /* ignored. */
        }

        @Override
        protected void writeStreamHeader() {
            /* ignored. */
        }

        @Override
        protected void writeClassDescriptor(ObjectStreamClass desc)
                throws IOException {
            Class<?> cl = desc.forClass();
            if (cl == null) {
                ibisStream.writeHandle(Constants.NUL_HANDLE);
                return;
            }
            ibisStream.writeClass(cl);
        }

        /*
         * annotateClass does not have to be redefined: it is empty in the
         * ObjectOutputStream implementation.
         */

        @Override
        public void writeFields() throws IOException {
            if (current_putfield == null) {
                throw new NotActiveException("no PutField object");
            }
            ((ImplPutField) current_putfield).writeFields();
        }

        @Override
        public PutField putFields() throws IOException {
            if (current_putfield == null) {
                if (current_object == null) {
                    throw new NotActiveException("not in writeObject");
                }
                Class<?> clazz = current_object.getClass();
                AlternativeTypeInfo t = AlternativeTypeInfo
                        .getAlternativeTypeInfo(clazz);
                while (t.level > current_level) {
                    t = t.alternativeSuperInfo;
                }
                current_putfield = new ImplPutField(t);
            }
            return (ImplPutField) current_putfield;
        }

        /**
         * The Ibis serialization implementation of <code>PutField</code>.
         */
        private class ImplPutField extends PutField {
            private double[] doubles;

            private long[] longs;

            private int[] ints;

            private float[] floats;

            private short[] shorts;

            private char[] chars;

            private byte[] bytes;

            private boolean[] booleans;

            private Object[] refs;

            private AlternativeTypeInfo t;

            ImplPutField(AlternativeTypeInfo t) {
                doubles = new double[t.double_count];
                longs = new long[t.long_count];
                ints = new int[t.int_count];
                shorts = new short[t.short_count];
                floats = new float[t.float_count];
                chars = new char[t.char_count];
                bytes = new byte[t.byte_count];
                booleans = new boolean[t.boolean_count];
                refs = new Object[t.reference_count];
                this.t = t;
            }

            @Override
            public void put(String name, boolean value)
                    throws IllegalArgumentException {
                booleans[t.getOffset(name, Boolean.TYPE)] = value;
            }

            @Override
            public void put(String name, char value)
                    throws IllegalArgumentException {
                chars[t.getOffset(name, Character.TYPE)] = value;
            }

            @Override
            public void put(String name, byte value)
                    throws IllegalArgumentException {
                bytes[t.getOffset(name, Byte.TYPE)] = value;
            }

            @Override
            public void put(String name, short value)
                    throws IllegalArgumentException {
                shorts[t.getOffset(name, Short.TYPE)] = value;
            }

            @Override
            public void put(String name, int value)
                    throws IllegalArgumentException {
                ints[t.getOffset(name, Integer.TYPE)] = value;
            }

            @Override
            public void put(String name, long value)
                    throws IllegalArgumentException {
                longs[t.getOffset(name, Long.TYPE)] = value;
            }

            @Override
            public void put(String name, float value)
                    throws IllegalArgumentException {
                floats[t.getOffset(name, Float.TYPE)] = value;
            }

            @Override
            public void put(String name, double value)
                    throws IllegalArgumentException {
                doubles[t.getOffset(name, Double.TYPE)] = value;
            }

            @Override
            public void put(String name, Object value) {
                refs[t.getOffset(name, Object.class)] = value;
            }

            @Override
            public void write(ObjectOutput o) throws IOException {
                for (int i = 0; i < t.double_count; i++) {
                    o.writeDouble(doubles[i]);
                }
                for (int i = 0; i < t.float_count; i++) {
                    o.writeFloat(floats[i]);
                }
                for (int i = 0; i < t.long_count; i++) {
                    o.writeLong(longs[i]);
                }
                for (int i = 0; i < t.int_count; i++) {
                    o.writeInt(ints[i]);
                }
                for (int i = 0; i < t.short_count; i++) {
                    o.writeShort(shorts[i]);
                }
                for (int i = 0; i < t.char_count; i++) {
                    o.writeChar(chars[i]);
                }
                for (int i = 0; i < t.byte_count; i++) {
                    o.writeByte(bytes[i]);
                }
                for (int i = 0; i < t.boolean_count; i++) {
                    o.writeBoolean(booleans[i]);
                }
                for (int i = 0; i < t.reference_count; i++) {
                    o.writeObject(refs[i]);
                }
            }

            void writeFields() throws IOException {
                for (int i = 0; i < t.double_count; i++) {
                    ibisStream.writeDouble(doubles[i]);
                }
                for (int i = 0; i < t.float_count; i++) {
                    ibisStream.writeFloat(floats[i]);
                }
                for (int i = 0; i < t.long_count; i++) {
                    ibisStream.writeLong(longs[i]);
                }
                for (int i = 0; i < t.int_count; i++) {
                    ibisStream.writeInt(ints[i]);
                }
                for (int i = 0; i < t.short_count; i++) {
                    ibisStream.writeShort(shorts[i]);
                }
                for (int i = 0; i < t.char_count; i++) {
                    ibisStream.writeChar(chars[i]);
                }
                for (int i = 0; i < t.byte_count; i++) {
                    ibisStream.writeByte(bytes[i]);
                }
                for (int i = 0; i < t.boolean_count; i++) {
                    ibisStream.writeBoolean(booleans[i]);
                }
                for (int i = 0; i < t.reference_count; i++) {
                    ibisStream.writeObject(refs[i]);
                }
            }
        }

        @Override
        public void writeBytes(String s) throws IOException {

            if (TIME_IBIS_SERIALIZATION) {
                timer.start();
            }
            if (s != null) {
                byte[] bytes = s.getBytes();
                int len = bytes.length;
                ibisStream.writeInt(len);
                for (int i = 0; i < len; i++) {
                    ibisStream.writeByte(bytes[i]);
                }
            }
            if (TIME_IBIS_SERIALIZATION) {
                timer.stop();
            }
        }

        @Override
        public void writeChars(String s) throws IOException {

            if (TIME_IBIS_SERIALIZATION) {
                timer.start();
            }
            if (s != null) {
                int len = s.length();
                ibisStream.writeInt(len);
                for (int i = 0; i < len; i++) {
                    ibisStream.writeChar(s.charAt(i));
                }
            }
            if (TIME_IBIS_SERIALIZATION) {
                timer.stop();
            }
        }

        @Override
        public void close() throws IOException {
            ibisStream.close();
        }

        @Override
        public void reset() throws IOException {
            ibisStream.reset();
        }

        @Override
        public void flush() throws IOException {
            ibisStream.flush();
        }

        @Override
        protected void drain() throws IOException {
            // empty implementation
        }

        @Override
        public void write(byte[] buf, int off, int len) throws IOException {
            ibisStream.writeArray(buf, off, len);
        }

        @Override
        public void write(byte[] buf) throws IOException {
            ibisStream.writeArray(buf);
        }

        @Override
        public void write(int val) throws IOException {
            ibisStream.writeByte((byte) val);
        }

        @Override
        public void writeBoolean(boolean val) throws IOException {
            ibisStream.writeBoolean(val);
        }

        @Override
        public void writeByte(int val) throws IOException {
            ibisStream.writeByte((byte) val);
        }

        @Override
        public void writeShort(int val) throws IOException {
            ibisStream.writeShort((short) val);
        }

        @Override
        public void writeChar(int val) throws IOException {
            ibisStream.writeChar((char) val);
        }

        @Override
        public void writeInt(int val) throws IOException {
            ibisStream.writeInt(val);
        }

        @Override
        public void writeLong(long val) throws IOException {
            ibisStream.writeLong(val);
        }

        @Override
        public void writeFloat(float val) throws IOException {
            ibisStream.writeFloat(val);
        }

        @Override
        public void writeDouble(double val) throws IOException {
            ibisStream.writeDouble(val);
        }

        @Override
        public void writeUTF(String val) throws IOException {
            ibisStream.writeUTF(val);
        }

    }
}
