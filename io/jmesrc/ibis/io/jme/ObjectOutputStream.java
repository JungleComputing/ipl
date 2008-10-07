/* $Id$ */

package ibis.io.jme;

import ibis.io.Replacer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Hashtable;

/**
 * This is the <code>SerializationOutputStream</code> version that is used
 * for Ibis serialization.
 */
public class ObjectOutputStream
        extends DataSerializationOutputStream {
    /** If <code>false</code>, makes all timer calls disappear. */
    private static final boolean TIME_IBIS_SERIALIZATION = false;

    /** Record how many objects of any class are sent. */
    private static final boolean STATS_OBJECTS
            = properties.getBooleanProperty(s_stats_written);

    // if STATS_OBJECTS
    static Hashtable statSendObjects;

    static final int[] statArrayCount;

    static int statObjectHandle;

    static final int[] statArrayHandle;

    static final long[] statArrayLength;

    static {
        if (STATS_OBJECTS) {
        	/* TODO: Setup a shutdown hook system
            Runtime.getRuntime().addShutdownHook(
                    new Thread("IbisSerializationOutputStream ShutdownHook") {
                        public void run() {
                            System.out.print("Serializable objects sent: ");
                            System.out.println(statSendObjects);
                            System.out.println("Non-array handles sent "
                                    + statObjectHandle);
                            for (int i = BEGIN_TYPES; i < PRIMITIVE_TYPES;
                                    i++) {
                                if (statArrayCount[i]
                                        + statArrayHandle[i] > 0) {
                                    System.out.println("       "
                                            + primitiveName(i)
                                            + " arrays "
                                            + statArrayCount[i]
                                            + " total bytes "
                                            + (statArrayLength[i]
                                                    * primitiveBytes(i))
                                            + " handles "
                                            + statArrayHandle[i]);
                                }
                            }
                        }
                    });
                    */
            System.out.println("IbisSerializationOutputStream.STATS_OBJECTS "
                    + "enabled");
            statSendObjects = new Hashtable();
            statArrayCount = new int[PRIMITIVE_TYPES];
            statArrayHandle = new int[PRIMITIVE_TYPES];
            statArrayLength = new long[PRIMITIVE_TYPES];
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

    // private IbisHash references  = new IbisHash(2048);

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
     * There also is the notion of a "current" <code>PutField</code>, needed for
     * the <code>writeFields</code> method.
     */
    Object current_putfield;

    /**
     * The <code>current_object</code>, <code>current_level</code>,
     * and <code>current_putfield</code> are maintained in stacks, so that
     * they can be managed by IOGenerator-generated code.
     */
    private Object[] object_stack;

    private int[] level_stack;

    private Object[] putfield_stack;

    private int max_stack_size = 0;

    private int stack_size = 0;

    private Class lastClass;
    private int   lastTypeno;

    /**
     * Constructor with an <code>DataOutputStream</code>.
     * @param out		the underlying <code>DataOutputStream</code>
     * @exception IOException	gets thrown when an IO error occurs.
     */
    public ObjectOutputStream(DataOutputStream out)
            throws IOException {
        super(out);

        types_clear();

        next_type = PRIMITIVE_TYPES;
        next_handle = CONTROL_HANDLES;
    }

    /**
     * Constructor, may be used when this class is sub-classed.
     */
    protected ObjectOutputStream() throws IOException {
        super();
        types_clear();

        next_type = PRIMITIVE_TYPES;
        next_handle = CONTROL_HANDLES;
    }

    static String primitiveName(int i) {
        switch (i) {
        case TYPE_BOOLEAN:
            return "boolean";
        case TYPE_BYTE:
            return "byte";
        case TYPE_CHAR:
            return "char";
        case TYPE_SHORT:
            return "short";
        case TYPE_INT:
            return "int";
        case TYPE_LONG:
            return "long";
        case TYPE_FLOAT:
            return "float";
        case TYPE_DOUBLE:
            return "double";
        }

        return null;
    }

    static int primitiveBytes(int i) {
        switch (i) {
        case TYPE_BOOLEAN:
            return SIZEOF_BOOLEAN;
        case TYPE_BYTE:
            return SIZEOF_BYTE;
        case TYPE_CHAR:
            return SIZEOF_CHAR;
        case TYPE_SHORT:
            return SIZEOF_SHORT;
        case TYPE_INT:
            return SIZEOF_INT;
        case TYPE_LONG:
            return SIZEOF_LONG;
        case TYPE_FLOAT:
            return SIZEOF_FLOAT;
        case TYPE_DOUBLE:
            return SIZEOF_DOUBLE;
        }

        return 0;
    }
    
    private static int arrayClassType(Class arrayClass) {
        if (false) {
            // nothing
        } else if (arrayClass == classByteArray) {
            return TYPE_BYTE;
        } else if (arrayClass == classIntArray) {
            return TYPE_INT;
        } else if (arrayClass == classBooleanArray) {
            return TYPE_BOOLEAN;
        } else if (arrayClass == classDoubleArray) {
            return TYPE_DOUBLE;
        } else if (arrayClass == classCharArray) {
            return TYPE_CHAR;
        } else if (arrayClass == classShortArray) {
            return TYPE_SHORT;
        } else if (arrayClass == classLongArray) {
            return TYPE_LONG;
        } else if (arrayClass == classFloatArray) {
            return TYPE_FLOAT;
        }
        return -1;
    }

    public boolean reInitOnNewConnection() {
        return true;
    }

    /**
     * Set a replacer. The replacement mechanism can be used to replace
     * an object with another object during serialization. This is used
     * in RMI, for instance, to replace a remote object with a stub. 
     * 
     * @param replacer the replacer object to be associated with this
     *  output stream
     */
    public void setReplacer(Replacer replacer) throws IOException {
        this.replacer = replacer;
    }

    public String serializationImplName() {
        return "ibis";
    }

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
        types.put(classBooleanArray, TYPE_BOOLEAN | TYPE_BIT);
        types.put(classByteArray, TYPE_BYTE | TYPE_BIT);
        types.put(classCharArray, TYPE_CHAR | TYPE_BIT);
        types.put(classShortArray, TYPE_SHORT | TYPE_BIT);
        types.put(classIntArray, TYPE_INT | TYPE_BIT);
        types.put(classLongArray, TYPE_LONG | TYPE_BIT);
        types.put(classFloatArray, TYPE_FLOAT | TYPE_BIT);
        types.put(classDoubleArray, TYPE_DOUBLE | TYPE_BIT);
        next_type = PRIMITIVE_TYPES;
    }

    public void reset() {
        reset(false);
    }

    public void reset(boolean cleartypes) {
        if (cleartypes || next_handle > CONTROL_HANDLES) {
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("reset: next handle = " + next_handle + ".");
            }
            references.clear();
            /* We cannot send out the reset immediately, because the
             * reader side only accepts a reset when it is expecting a
             * handle. So, instead, we remember that we need to send
             * out a reset, and send before sending the next handle.
             */
            if (cleartypes) {
                clearPending = true;
            } else {
                resetPending = true;
            }
            next_handle = CONTROL_HANDLES;
        }
        if (cleartypes) {
            types_clear();
        }
    }

    /* This is the data output / object output part */

    /**
     * Called by IOGenerator-generated code to write a Class object to this
     * stream. For a Class object, only its name is written.
     * @param ref		the <code>Class</code> to be written
     * @exception IOException	gets thrown when an IO error occurs.
     */
    public void writeClass(Class ref) throws IOException {
        if (TIME_IBIS_SERIALIZATION) {
            startTimer();
        }
        if (ref == null) {
            writeHandle(NUL_HANDLE);
            if (TIME_IBIS_SERIALIZATION) {
                stopTimer();
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
            stopTimer();
        }
    }

    /**
     * Sends out handles as normal int's. Also checks if we
     * need to send out a reset first.
     * @param v		the handle to be written
     * @exception IOException	gets thrown when an IO error occurs.
     */
    void writeHandle(int v) throws IOException {
        if (clearPending) {
            writeInt(CLEAR_HANDLE);
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("wrote a CLEAR");
            }
            resetPending = false;
            clearPending = false;
        } else if (resetPending) {
            writeInt(RESET_HANDLE);
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


    public void writeArray(boolean[] ref, int off, int len) throws IOException {
        if (TIME_IBIS_SERIALIZATION) {
            startTimer();
        }
        if (writeArrayHeader(ref, classBooleanArray, len, false)) {
            writeArrayBoolean(ref, off, len);
        }
        if (TIME_IBIS_SERIALIZATION) {
            stopTimer();
        }
    }

    public void writeArray(byte[] ref, int off, int len) throws IOException {
        if (TIME_IBIS_SERIALIZATION) {
            startTimer();
        }
        if (writeArrayHeader(ref, classByteArray, len, false)) {
            writeArrayByte(ref, off, len);
        }
        if (TIME_IBIS_SERIALIZATION) {
            stopTimer();
        }
    }

    public void writeArray(short[] ref, int off, int len) throws IOException {
        if (TIME_IBIS_SERIALIZATION) {
            startTimer();
        }
        if (writeArrayHeader(ref, classShortArray, len, false)) {
            writeArrayShort(ref, off, len);
        }
        if (TIME_IBIS_SERIALIZATION) {
            stopTimer();
        }
    }

    public void writeArray(char[] ref, int off, int len) throws IOException {
        if (TIME_IBIS_SERIALIZATION) {
            startTimer();
        }
        if (writeArrayHeader(ref, classCharArray, len, false)) {
            writeArrayChar(ref, off, len);
        }
        if (TIME_IBIS_SERIALIZATION) {
            stopTimer();
        }
    }

    public void writeArray(int[] ref, int off, int len) throws IOException {
        if (TIME_IBIS_SERIALIZATION) {
            startTimer();
        }
        if (writeArrayHeader(ref, classIntArray, len, false)) {
            writeArrayInt(ref, off, len);
        }
        if (TIME_IBIS_SERIALIZATION) {
            stopTimer();
        }
    }

    public void writeArray(long[] ref, int off, int len) throws IOException {
        if (TIME_IBIS_SERIALIZATION) {
            startTimer();
        }
        if (writeArrayHeader(ref, classLongArray, len, false)) {
            writeArrayLong(ref, off, len);
        }
        if (TIME_IBIS_SERIALIZATION) {
            stopTimer();
        }
    }

    public void writeArray(float[] ref, int off, int len) throws IOException {
        if (TIME_IBIS_SERIALIZATION) {
            startTimer();
        }
        if (writeArrayHeader(ref, classFloatArray, len, false)) {
            writeArrayFloat(ref, off, len);
        }
        if (TIME_IBIS_SERIALIZATION) {
            stopTimer();
        }
    }

    public void writeArray(double[] ref, int off, int len) throws IOException {
        if (TIME_IBIS_SERIALIZATION) {
            startTimer();
        }
        if (writeArrayHeader(ref, classDoubleArray, len, false)) {
            writeArrayDouble(ref, off, len);
        }
        if (TIME_IBIS_SERIALIZATION) {
            stopTimer();
        }
    }

    public void writeArray(Object[] ref, int off, int len) throws IOException {
        if (TIME_IBIS_SERIALIZATION) {
            startTimer();
        }
        Class clazz = ref.getClass();
        if (writeArrayHeader(ref, clazz, len, false)) {
            for (int i = off; i < off + len; i++) {
                doWriteObject(ref[i]);
            }
        }
        if (TIME_IBIS_SERIALIZATION) {
            stopTimer();
        }
    }

    /**
     * Writes a type or a handle.
     * If <code>ref</code> has been written before, this method writes its
     * handle and returns <code>true</code>. If not, its type is written,
     * a new handle is associated with it, and <code>false</code> is returned.
     *
     * @param ref	the object that is going to be put on the stream
     * @param clazz	the <code>Class</code> representing the type
     * 			of <code>ref</code>
     * @exception IOException	gets thrown when an IO error occurs.
     */
    private boolean writeTypeHandle(Object ref, Class clazz)
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
     * Writes a handle or an array header, depending on wether a cycle
     * should be and was detected. If a cycle was detected, it returns
     * <code>false</code>, otherwise <code>true</code>.
     * The array header consists of a type and a length.
     * @param ref	    the array to be written
     * @param clazz	    the <code>Class</code> representing the array type
     * @param len	    the number of elements to be written
     * @param doCycleCheck  set when cycles should be detected
     * @exception IOException	gets thrown when an IO error occurs.
     * @return <code>true</code> if no cycle was or should be detected
     *  (so that the array should be written).
     */
    private boolean writeArrayHeader(Object ref, Class clazz, int len,
            boolean doCycleCheck) throws IOException {
        if (ref == null) {
            writeHandle(NUL_HANDLE);
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
            logger.debug("writeArrayHeader " + clazz.getName() + " length = " + len);
        }
        return true;
    }

    /**
     * Writes an array, but possibly only a handle.
     * @param ref	  the array to be written
     * @param arrayClass  the <code>Class</code> representing the array type
     * @param unshared	  set when no cycle detection check shoud be done
     * @exception IOException	gets thrown when an IO error occurs.
     */
    void writeArray(Object ref, Class arrayClass, boolean unshared)
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
     * Adds the type represented by <code>clazz</code> to the type
     * table and returns its number.
     * @param clazz	represents the type to be added
     * @return		the type number.
     */
    private int newType(Class clazz) {
        int type_number = next_type++;

        type_number = (type_number | TYPE_BIT);
        types.put(clazz, type_number);

        return type_number;
    }

    void writeType(Class clazz) throws IOException {
    	writeType(clazz, false);
    }
    
    /**
     * Writes a type number, and, when new, a type name to the output stream.
     * @param clazz		the clazz to be written.
     * @exception IOException	gets thrown when an IO error occurs.
     */
    void writeType(Class clazz, boolean expected) throws IOException {
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
        if (!expected) {
        	writeUTF(clazz.getName());
        }
    }

    /**
     * Writes a (new or old) handle for object <code>ref</code> to the output
     * stream. Returns 1 if the object is new, -1 if not.
     * @param ref		the object whose handle is to be written
     * @exception IOException	gets thrown when an IO error occurs.
     * @return			1 if it is a new object, -1 if it is not.
     */
    public int writeKnownObjectHeader(Object ref) throws IOException {

        if (ref == null) {
            writeHandle(NUL_HANDLE);
            return 0;
        }

        int handle = references.lazyPut(ref, next_handle);
        if (handle == next_handle) {
            // System.err.write("+");
            Class clazz = ref.getClass();
            next_handle++;
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("writeKnownObjectHeader -> writing NEW object, class = "
                        + clazz.getName());
            }
            writeType(clazz);
            return 1;
        }

        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("writeKnownObjectHeader -> writing OLD HANDLE " + handle);
        }
        writeHandle(handle);

        return -1;
    }

    /**
     * Writes a (new or old) handle for array of primitives <code>ref</code>
     * to the output stream. Returns 1 if the object is new, -1 if not.
     * @param ref		the object whose handle is to be written
     * @param typehandle	the type number
     * @exception IOException	gets thrown when an IO error occurs.
     * @return			1 if it is a new object, -1 if it is not.
     */
    public int writeKnownArrayHeader(Object ref, int typehandle)
            throws IOException {
        if (ref == null) {
            writeHandle(NUL_HANDLE);
            return 0;
        }

        int handle = references.lazyPut(ref, next_handle);
        if (handle == next_handle) {
            // System.err.write("+");
            next_handle++;
            writeInt(typehandle | TYPE_BIT);
            return 1;
        }

        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("writeKnownObjectHeader -> writing OLD HANDLE " + handle);
        }
        writeHandle(handle);

        return -1;
    }

    /**
     * Push the notions of <code>current_object</code>,
     * <code>current_level</code>, and <code>current_putfield</code> on
     * their stacks, and set new ones.
     *
     * @param ref	the new <code>current_object</code> notion
     * @param level	the new <code>current_level</code> notion
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
     * <code>current_level</code>, and <code>current_putfield</code> from
     * their stacks.
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
     * Writes a <code>String</code> object.
     * This is a special case, because strings are written as an UTF.
     *
     * @param ref		the string to be written
     * @exception IOException	gets thrown on IO error
     */
    public void writeString(String ref) throws IOException {
        if (TIME_IBIS_SERIALIZATION) {
            startTimer();
        }
        if (ref == null) {
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("writeString: --> null");
            }
            writeHandle(NUL_HANDLE);
            if (TIME_IBIS_SERIALIZATION) {
                stopTimer();
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
            stopTimer();
        }
    }

    static void addStatSendObject(Object ref) {
        if (STATS_OBJECTS) {
            Class clazz = ref.getClass();
            Integer n = (Integer)statSendObjects.get(clazz);
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

    void addStatSendArray(Object ref, int type, int len) {
        if (STATS_OBJECTS) {
            addStatSendObject(ref);
            statArrayCount[type]++;
            statArrayLength[type] += len;
        }
    }

    private static void addStatSendArrayHandle(Object ref, int len) {
        if (STATS_OBJECTS) {
            Class arrayClass = ref.getClass();
            int type = arrayClassType(arrayClass);
            if (type == -1) {
                statObjectHandle++;
            } else {
                statArrayHandle[type]++;
            }
        }
    }

    /**
     * Write objects and arrays.
     * Duplicates are deteced when this call is used.
     * The replacement mechanism is implemented here as well.
     *
     * @param ref the object to be written
     * @exception java.io.IOException is thrown when an IO error occurs.
     */
    public void writeObject(Object ref) throws IOException {
        doWriteObject(ref);
    }
    
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
         * ref < 0:	type
         * ref = 0:	null ptr
         * ref > 0:	handle
         */

        if (TIME_IBIS_SERIALIZATION) {
            startTimer();
        }
        if (ref == null) {
            writeHandle(NUL_HANDLE);
            if (TIME_IBIS_SERIALIZATION) {
                stopTimer();
            }
            return;
        }
        /* TODO: deal with writeReplace! This should be done before
         looking up the handle. If we don't want to do runtime
         inspection, this should probably be handled somehow in
         IOGenerator.
         Note that the needed info is available in AlternativeTypeInfo,
         but we don't want to use that when we have ibis.io.Serializable.
         */

        if (replacer != null) {
            ref = replacer.replace(ref);
        }

        int hashCode = HandleHash.getHashCode(ref);
        int handle = references.find(ref, hashCode);

        if (handle == 0) {
        	Class clazz = ref.getClass();
            AlternativeTypeInfo t
                        = AlternativeTypeInfo.getAlternativeTypeInfo(clazz);
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("start writeObject of class " + clazz.getName()
                        + " handle = " + next_handle);
            }
            t.readerWriter.writeObject(this, ref, t, hashCode, false);
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("finished writeObject of class " + clazz.getName());
            }
        } else {
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("writeObject: duplicate handle " + handle + " class = "
                        + ref.getClass());
            }
            writeHandle(handle);

            addStatSendObjectHandle(ref);
        }
        if (TIME_IBIS_SERIALIZATION) {
            stopTimer();
        }
    }

    private JavaObjectOutputStream objectStream = null;

    public ObjectOutput getJavaObjectOutputStream()
            throws IOException {
        if (objectStream == null) {
            objectStream = new JavaObjectOutputStream(this);
        }
        return objectStream;
    }

    private class JavaObjectOutputStream extends OutputStream implements ObjectOutput {

        ObjectOutputStream ibisStream;

        JavaObjectOutputStream(ObjectOutputStream s)
                throws IOException {
            super();
            ibisStream = s;
        }

        public void writeObjectOverride(Object ref) throws IOException {
            ibisStream.doWriteObject(ref);
        }

        public void defaultWriteObject() throws IOException, NotActiveException {
            if (current_object == null) {
                throw new NotActiveException("defaultWriteObject: no object");
            }

            Object ref = current_object;
            Class clazz = ref.getClass();
            AlternativeTypeInfo t
                    = AlternativeTypeInfo.getAlternativeTypeInfo(clazz);

            if (t.isJMESerializable) {
                /* Note that this will take the generated_DefaultWriteObject of the
                 dynamic type of ref. The current_level variable actually
                 indicates which instance of generated_DefaultWriteObject 
                 should do some work.
                */
                if (DEBUG && logger.isDebugEnabled()) {
                    logger.debug("generated_DefaultWriteObject, class = "
                            + clazz.getName() + ", level = " + current_level);
                }
                ((JMESerializable) ref).generated_JME_DefaultWriteObject(ibisStream,
                        current_level);
            } else {
                throw new NotSerializableException("Not Serializable : "
                        + clazz.getName());
            }
        }

        public void writeUnshared(Object ref) throws IOException {
            if (ref == null) {
                ibisStream.writeHandle(NUL_HANDLE);
                return;
            }
            /* TODO: deal with writeReplace! This should be done before
             looking up the handle. If we don't want to do runtime
             inspection, this should probably be handled somehow in
             IOGenerator.
             Note that the needed info is available in AlternativeTypeInfo,
             but we don't want to use that when we have ibis.io.Serializable.
             */
            Class clazz = ref.getClass();
            AlternativeTypeInfo t
                    = AlternativeTypeInfo.getAlternativeTypeInfo(clazz);

            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("start writeUnshared of class " + clazz.getName()
                        + " handle = " + next_handle);
            }

            t.readerWriter.writeObject(ibisStream, ref, t, 0, true);

            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("finished writeUnshared of class " + clazz.getName()
                        + " handle = " + next_handle);
            }
        }

        public void useProtocolVersion(int version) {
            /* ignored. */
        }

        protected void writeStreamHeader() {
            /* ignored. */
        }

        /* annotateClass does not have to be redefined: it is empty in the
         ObjectOutputStream implementation.
         */

        public void writeFields() throws IOException {
            throw new NotActiveException("no PutField object");
        }

        public void close() throws IOException {
            ibisStream.close();
        }

        public void reset() throws IOException {
            ibisStream.reset();
        }

        public void flush() throws IOException {
            ibisStream.flush();
        }

        protected void drain() throws IOException {
        	// empty implementation
        }

        public void write(byte[] buf, int off, int len) throws IOException {
            ibisStream.writeArray(buf, off, len);
        }

        public void write(byte[] buf) throws IOException {
            ibisStream.writeArray(buf);
        }

        public void write(int val) throws IOException {
            ibisStream.writeByte((byte) val);
        }

        public void writeBoolean(boolean val) throws IOException {
            ibisStream.writeBoolean(val);
        }

        public void writeByte(int val) throws IOException {
            ibisStream.writeByte((byte) val);
        }

        public void writeShort(int val) throws IOException {
            ibisStream.writeShort((short) val);
        }

        public void writeChar(int val) throws IOException {
            ibisStream.writeChar((char) val);
        }

        public void writeInt(int val) throws IOException {
            ibisStream.writeInt(val);
        }

        public void writeLong(long val) throws IOException {
            ibisStream.writeLong(val);
        }

        public void writeFloat(float val) throws IOException {
            ibisStream.writeFloat(val);
        }

        public void writeDouble(double val) throws IOException {
            ibisStream.writeDouble(val);
        }

        public void writeUTF(String val) throws IOException {
            ibisStream.writeUTF(val);
        }

		public void writeObject(Object obj) throws IOException {
			// TODO Auto-generated method stub
			
		}

		public void writeBytes(String s) throws IOException {
			ibisStream.writeArray(s.getBytes());
		}

		public void writeChars(String s) throws IOException {
			char[] chars = new char[s.length()];
			s.getChars(0, s.length()-1, chars, 0);
			ibisStream.writeArray(chars);
		}

		public boolean reInitOnNewConnection() {
			// TODO Auto-generated method stub
			return false;
		}

		public void realClose() throws IOException {
			// TODO Auto-generated method stub
			
		}

		public void reset(boolean cleartypes) throws IOException {
			// TODO Auto-generated method stub
			
		}

		public String serializationImplName() {
			// TODO Auto-generated method stub
			return null;
		}

		public void setReplacer(Replacer replacer) throws IOException {
			// TODO Auto-generated method stub
			
		}

		public void statistics() {
			// TODO Auto-generated method stub
			
		}

		public void writeArray(Object[] val) throws IOException {
			// TODO Auto-generated method stub
			
		}

		public void writeArray(Object[] val, int off, int len) throws IOException {
			// TODO Auto-generated method stub
			
		}

		public void writeString(String val) throws IOException {
			// TODO Auto-generated method stub
			
		}

		public void writeArray(boolean[] source, int offset, int length) throws IOException {
			// TODO Auto-generated method stub
			
		}

		public void writeArray(byte[] source, int offset, int length) throws IOException {
			// TODO Auto-generated method stub
			
		}

		public void writeArray(char[] source, int offset, int length) throws IOException {
			// TODO Auto-generated method stub
			
		}

		public void writeArray(short[] source, int offset, int length) throws IOException {
			// TODO Auto-generated method stub
			
		}

		public void writeArray(int[] source, int offset, int length) throws IOException {
			// TODO Auto-generated method stub
			
		}

		public void writeArray(long[] source, int offset, int length) throws IOException {
			// TODO Auto-generated method stub
			
		}

		public void writeArray(float[] source, int offset, int length) throws IOException {
			// TODO Auto-generated method stub
			
		}

		public void writeArray(double[] source, int offset, int length) throws IOException {
			// TODO Auto-generated method stub
			
		}

		public void writeArray(boolean[] source) throws IOException {
			// TODO Auto-generated method stub
			
		}

		public void writeArray(byte[] source) throws IOException {
			// TODO Auto-generated method stub
			
		}

		public void writeArray(char[] source) throws IOException {
			// TODO Auto-generated method stub
			
		}

		public void writeArray(short[] source) throws IOException {
			// TODO Auto-generated method stub
			
		}

		public void writeArray(int[] source) throws IOException {
			// TODO Auto-generated method stub
			
		}

		public void writeArray(long[] source) throws IOException {
			// TODO Auto-generated method stub
			
		}

		public void writeArray(float[] source) throws IOException {
			// TODO Auto-generated method stub
			
		}

		public void writeArray(double[] source) throws IOException {
			// TODO Auto-generated method stub
			
		}

		public void writeByte(byte value) throws IOException {
			// TODO Auto-generated method stub
			
		}

		public void writeChar(char value) throws IOException {
			// TODO Auto-generated method stub
			
		}

		public void writeShort(short value) throws IOException {
			// TODO Auto-generated method stub
			
		}

    }
}
