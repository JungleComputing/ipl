package ibis.io;

import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.NotActiveException;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.NotSerializableException;


/**
 * This is the <code>SerializationOutputStream</code> version that is used for Ibis serialization.
 * An effort has been made to make it look like and extend <code>java.io.ObjectOutputStream</code>.
 * However, versioning is not supported, like it is in Sun serialization.
 */
public final class IbisSerializationOutputStream extends SerializationOutputStream implements IbisStreamFlags {
    /**
     * The underlying <code>IbisAccumulator</code>.
     * Must be public so that IOGenerator-generated code can access it.
     */
    public final IbisAccumulator out;

    /**
     * The first free object handle.
     */
    private int next_handle;

    /**
     * Hash table for keeping references to objects already written.
     */
    private IbisHash references  = new IbisHash();

    /**
     * Remember when a reset must be sent out.
     */
    private boolean resetPending = false;

    /**
     * The first free type index.
     */
    private int next_type;

    /**
     * Hashtable for types already put on the stream.
     */
    private IbisHash types = new IbisHash();

    /**
     * There is a notion of a "current" object. This is needed when a user-defined
     * <code>writeObject</code> refers to <code>defaultWriteObject</code> or to
     * <code>putFields</code>.
     */
    private Object current_object;

    /**
     * There also is a notion of a "current" level.
     * The "level" of a serializable class is computed as follows:<ul>
     * <li> if its superclass is serializable: the level of the superclass + 1.
     * <li> if its superclass is not serializable: 1.
     * </ul>
     * This level implies a level at which an object can be seen. The "current"
     * level is the level at which <code>current_object</code> is being processed.
     */
    private int current_level;

    /**
     * There also is the notion of a "current" <code>PutField</code>, needed for
     * the <code>writeFields</code> method.
     */
    private ImplPutField current_putfield;

    /**
     * The <code>current_object</code>, <code>current_level</code>, and <code>current_putfield</code>
     * are maintained in stacks, so that they can be managed by IOGenerator-generated
     * code.
     */
    private Object[] object_stack;
    private int[] level_stack;
    private ImplPutField[] putfield_stack;
    private int max_stack_size = 0;
    private int stack_size = 0;

    /**
     * Constructor with an <code>IbisAccumulator</code>.
     * @param out		the underlying <code>IbisAccumulator</code>
     * @exception IOException	gets thrown when an IO error occurs.
     */
    public IbisSerializationOutputStream(IbisAccumulator out)
							 throws IOException {
	super();
	types_clear();

	next_type = PRIMITIVE_TYPES;
	this.out    = out;
	references.clear();
	next_handle = CONTROL_HANDLES;
    }

    /**
     * Initializes the type hash by adding arrays of primitive types.
     */
    private void types_clear() {
	types.clear();
	types.put(classBooleanArray, TYPE_BOOLEAN | TYPE_BIT);
	types.put(classByteArray,    TYPE_BYTE | TYPE_BIT);
	types.put(classCharArray,    TYPE_CHAR | TYPE_BIT);
	types.put(classShortArray,   TYPE_SHORT | TYPE_BIT);
	types.put(classIntArray,     TYPE_INT | TYPE_BIT);
	types.put(classLongArray,    TYPE_LONG | TYPE_BIT);
	types.put(classFloatArray,   TYPE_FLOAT | TYPE_BIT);
	types.put(classDoubleArray,  TYPE_DOUBLE | TYPE_BIT);
	next_type = PRIMITIVE_TYPES;
    }

    /**
     * @inhetitDoc
     */
    public String serializationImplName() {
	return "ibis";
    }

    /**
     * @inheritDoc
     */
    public void reset() throws IOException {
	if (next_handle > CONTROL_HANDLES) {
	    if(DEBUG) {
		System.err.println("OUT(" + this + ") reset: next handle = " + next_handle + ".");
	    }
	    references.clear();
	    // out.writeInt(RESET_HANDLE);
	    // write out the reset handle immediately, so that the input side can cleanup.
	    // This is important, because otherwise stubs will be kept alive for too long,
	    // and thus, connections will be kept for too long.
	    // No, the other side is not expecting this! This must be solved in another way.
	    resetPending = true; /* remember we need to send out a reset */
	    next_handle = CONTROL_HANDLES;
	}
	// types_clear();
	// There is no need to clear the type table. It can be reused after a reset.
    }

    /**
     * @inheritDoc
     */
    public void statistics() {
	System.err.println("IbisOutput:");
	references.statistics();
    }

    /* This is the data output / object output part */

    /**
     * @inheritDoc
     */
    public void write(int v) throws IOException {
	out.writeByte((byte)(0xff & v));
    }

    /**
     * @inheritDoc
     */
    public void write(byte[] b) throws IOException {
	write(b, 0, b.length);
    }

    /**
     * @inheritDoc
     */
    public void write(byte[] b, int off, int len) throws IOException {
	writeArray(b, off, len);
    }

    /**
     * @inheritDoc
     */
    public void writeUTF(String str) throws IOException {
	// System.out.println("WriteUTF: " + str);
	if(str == null) {
	    writeInt(-1);
	    return;
	}

	if(DEBUG) {
	    System.err.println("write string " + str);
	}
	int len = str.length();

	//	writeInt(len);
	//	writeArray(str.toCharArray(), 0, len);

	byte[] b = new byte[3 * len];
	int bn = 0;

	for (int i = 0; i < len; i++) {
	    char c = str.charAt(i);
	    if (c > 0x0000 && c <= 0x007f) {
		b[bn++] = (byte)c;
	    } else if (c <= 0x07ff) {
		b[bn++] = (byte)(0xc0 | (0x1f & (c >> 6)));
		b[bn++] = (byte)(0x80 | (0x3f & c));
	    } else {
		b[bn++] = (byte)(0xe0 | (0x0f & (c >> 12)));
		b[bn++] = (byte)(0x80 | (0x3f & (c >>  6)));
		b[bn++] = (byte)(0x80 | (0x3f & c));
	    }
	}

	if(DEBUG) {
	    System.err.print("Write UTF[" + bn + "] \"");
	    for (int i = 0; i < bn; i++) {
		System.err.print((char)b[i]);
	    }
	    System.err.println("\"");
	    System.err.flush();
	}

	writeInt(bn);
	writeArray(b, 0, bn);
    }

    /**
     * Called by IOGenerator-generated code to write a Class object to this stream.
     * For a Class object, only its name is written.
     * @param ref		the <code>Class</code> to be written
     * @exception IOException	gets thrown when an IO error occurs.
     */
    public void writeClass(Class ref) throws IOException {
	if (ref == null) {
	    writeHandle(NUL_HANDLE);
	    return;
	}
	int handle = references.find(ref);
	if (handle == 0) {
	    handle = next_handle++;
	    references.put(ref, handle);
	    writeType(java.lang.Class.class);
	    writeUTF(ref.getName());
	} else {
	    writeHandle(handle);
	}
    }

    /**
     * @inheritDoc
     */
    public void writeBoolean(boolean v) throws IOException {
	if (DEBUG) {
	    System.out.println("writeBoolean: " + v);
	}
	out.writeBoolean(v);
    }

    /**
     * @inheritDoc
     */
    public void writeByte(int v) throws IOException {
	out.writeByte((byte)(0xff & v));
    }

    /**
     * @inheritDoc
     */
    public void writeShort(int v) throws IOException {
	out.writeShort((short)(0xffff & v));
    }

    /**
     * @inheritDoc
     */
    public void writeChar(int v) throws IOException {
	out.writeChar((char)(0xffff & v));
    }

    /**
     * @inheritDoc
     */
    public void writeInt(int v) throws IOException {
	out.writeInt(v);
    }

    /**
     * Sends out handles as normal int's. Also checks if we
     * need to send out a reset first.
     * @param v		the handle to be written
     * @exception IOException	gets thrown when an IO error occurs.
     */
    private void writeHandle(int v) throws IOException {
        if (resetPending) {
                out.writeInt(RESET_HANDLE);
                resetPending = false;
        }

        // treating handles as normal int's --N
        out.writeInt(v);
    }

    /**
     * @inheritDoc
     */
    public void writeLong(long v) throws IOException {
	out.writeLong(v);
    }

    /**
     * @inheritDoc
     */
    public void writeFloat(float f) throws IOException {
	out.writeFloat(f);
    }

    /**
     * @inheritDoc
     */
    public void writeDouble(double d) throws IOException {
	out.writeDouble(d);
    }

    /**
     * @inheritDoc
     */
    public void writeBytes(String s) throws IOException {

	if (s == null) return;

	byte[] bytes = s.getBytes();
	int len = bytes.length;
	writeInt(len);
	for (int i = 0; i < len; i++) {
	    writeByte(bytes[i]);
	}
    }

    /**
     * @inheritDoc
     */
    public void writeChars(String s) throws IOException {

	if (s == null) return;

	int len = s.length();
	writeInt(len);
	for (int i = 0; i < len; i++) {
	    writeChar(s.charAt(i));
	}
    }

    /**
     * @inheritDoc
     */
    public void writeArray(boolean[] ref, int off, int len) throws IOException {
	if(writeArrayHeader(ref, arrayClasses[TYPE_BOOLEAN], len, false)) {
	    out.writeArray(ref, off, len);
	}
    }

    /**
     * @inheritDoc
     */
    public void writeArray(byte[] ref, int off, int len) throws IOException {
	if(writeArrayHeader(ref, arrayClasses[TYPE_BYTE], len, false)) {
	    out.writeArray(ref, off, len);
	}
    }

    /**
     * @inheritDoc
     */
    public void writeArray(short[] ref, int off, int len) throws IOException {
	if(writeArrayHeader(ref, arrayClasses[TYPE_SHORT], len, false)) {
	    out.writeArray(ref, off, len);
	}
    }

    /**
     * @inheritDoc
     */
    public void writeArray(char[] ref, int off, int len) throws IOException {
	if(writeArrayHeader(ref, arrayClasses[TYPE_CHAR], len, false)) {
	    out.writeArray(ref, off, len);
	}
    }

    /**
     * @inheritDoc
     */
    public void writeArray(int[] ref, int off, int len) throws IOException {
	if(writeArrayHeader(ref, arrayClasses[TYPE_INT], len, false)) {
	    out.writeArray(ref, off, len);
	}
    }

    /**
     * @inheritDoc
     */
    public void writeArray(long[] ref, int off, int len) throws IOException {
	if(writeArrayHeader(ref, arrayClasses[TYPE_LONG], len, false)) {
	    out.writeArray(ref, off, len);
	}
    }

    /**
     * @inheritDoc
     */
    public void writeArray(float[] ref, int off, int len) throws IOException {
	if(writeArrayHeader(ref, arrayClasses[TYPE_FLOAT], len, false)) {
	    out.writeArray(ref, off, len);
	}
    }

    /**
     * @inheritDoc
     */
    public void writeArray(double[] ref, int off, int len) throws IOException {
	if(writeArrayHeader(ref, arrayClasses[TYPE_DOUBLE], len, false)) {
	    out.writeArray(ref, off, len);
	}
    }

    /**
     * @inheritDoc
     */
    public void writeArray(Object[] ref, int off, int len) throws IOException {
	Class clazz = ref.getClass();
	if (writeArrayHeader(ref, clazz, len, false)) {
	    for (int i = off; i < off + len; i++) {
		writeObject(ref[i]);
	    }
	}
    }

    /**
     * Writes a type or a handle.
     * If <code>ref</code> has been written before, this method writes its handle
     * and returns <code>true</code>. If not, its type is written, a new handle is
     * associated with it, and <code>false</code> is returned.
     *
     * @param ref		the object that is going to be put on the stream
     * @param type		the <code>Class</code> representing the type of <code>ref</code>
     * @exception IOException	gets thrown when an IO error occurs.
     */
    private boolean writeTypeHandle(Object ref, Class type) throws IOException {
	int handle = references.find(ref);

	if (handle != 0) {
	    writeHandle(handle);
	    return true;
	}

	writeType(type);

	handle = next_handle++;
	references.put(ref, handle);
	if (DEBUG) {
	    System.out.println("writeTypeHandle: references[" + handle + "] = " + (ref == null ? "null" : ref));
	}

	return false;
    }

    /**
     * Writes a handle or an array header, depending on wether a cycle should be and was
     * detected. If a cycle was detected, it returns <code>false</code>, otherwise <code>true</code>.
     * The array header consists of a type and a length.
     * @param ref		the array to be written
     * @param clazz		the <code>Class</code> representing the array type
     * @param len		the number of elements to be written
     * @param doCycleCheck	set when cycles should be detected
     * @exception IOException	gets thrown when an IO error occurs.
     * @return <code>true</code> if no cycle was or should be detected (so that the array
     * should be written).
     */
    private boolean writeArrayHeader(Object ref, Class clazz, int len, boolean doCycleCheck)
	throws IOException {

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
	return true;
    }

    /**
     * Writes an array, but possibly only a handle.
     * @param ref		the array to be written
     * @param arrayClazz	the <code>Class</code> representing the array type
     * @param unshared		set when no cycle detection check shoud be done
     * @exception IOException	gets thrown when an IO error occurs.
     */
    private void writeArray(Object ref, Class arrayClass, boolean unshared) throws IOException {
	if (false) {
	} else if (arrayClass == classByteArray) {
	    byte[] a = (byte[])ref;
	    int len = a.length;
	    if(writeArrayHeader(a, arrayClass, len, ! unshared)) {
		out.writeArray(a, 0, len);
	    }
	} else if (arrayClass == classIntArray) {
	    int[] a = (int[])ref;
	    int len = a.length;
	    if(writeArrayHeader(a, arrayClass, len, !unshared)) {
		out.writeArray(a, 0, len);
	    }
	} else if (arrayClass == classBooleanArray) {
	    boolean[] a = (boolean[])ref;
	    int len = a.length;
	    if(writeArrayHeader(a, arrayClass, len, ! unshared)) {
		out.writeArray(a, 0, len);
	    }
	} else if (arrayClass == classDoubleArray) {
	    double[] a = (double[])ref;
	    int len = a.length;
	    if(writeArrayHeader(a, arrayClass, len, ! unshared)) {
		out.writeArray(a, 0, len);
	    }
	} else if (arrayClass == classCharArray) {
	    char[] a = (char[])ref;
	    int len = a.length;
	    if(writeArrayHeader(a, arrayClass, len, ! unshared)) {
		out.writeArray(a, 0, len);
	    }
	} else if (arrayClass == classShortArray) {
	    short[] a = (short[])ref;
	    int len = a.length;
	    if(writeArrayHeader(a, arrayClass, len, ! unshared)) {
		out.writeArray(a, 0, len);
	    }
	} else if (arrayClass == classLongArray) {
	    long[] a = (long[])ref;
	    int len = a.length;
	    if(writeArrayHeader(a, arrayClass, len, ! unshared)) {
		out.writeArray(a, 0, len);
	    }
	} else if (arrayClass == classFloatArray) {
	    float[] a = (float[])ref;
	    int len = a.length;
	    if(writeArrayHeader(a, arrayClass, len, ! unshared)) {
		out.writeArray(a, 0, len);
	    }
	} else {
	    if(ASSERTS) {
		if (! (ref instanceof Object[])) {
		    System.err.println("What's up NOW!");
		}
	    }
	    if(DEBUG) {
		System.err.println("Writing array " + ref.getClass().getName());
	    }
	    Object[] a = (Object[])ref;
	    int len = a.length;
	    if(writeArrayHeader(a, arrayClass, len, ! unshared)) {
		for (int i = 0; i < len; i++) {
		    writeObject(a[i]);
		}
	    }
	}
    }

    /**
     * Adds the type represented by <code>type</code> to the type
     * table and returns its number.
     * @param type	represents the type to be added
     * @return		the type number.
     */
    private int newType(Class type) {
	int type_number = next_type++;

	type_number = (type_number | TYPE_BIT);
	types.put(type, type_number);

	return type_number;
    }

    /**
     * Writes a type number, and, when new, a type name to the output stream.
     * @param type		the type to be written.
     * @exception IOException	gets thrown when an IO error occurs.
     */
    private void writeType(Class type) throws IOException {
	int type_number = types.find(type);

	if (type_number != 0) {
	    writeHandle(type_number);	// TYPE_BIT is set, receiver sees it

	    if(DEBUG) {
		System.err.println("Write type number " +
					Integer.toHexString(type_number));
	    }
	    return;
	}

	type_number = newType(type);
	writeHandle(type_number);	// TYPE_BIT is set, receiver sees it
	if(DEBUG) {
	    System.err.println("Write NEW type " + type.getName()
			+ " number " + Integer.toHexString(type_number));
	}
	writeUTF(type.getName());
    }

    /**
     * Writes a (new or old) handle for object <code>ref</code> to the output stream.
     * Returns 1 if the object is new, -1 if not.
     * @param ref		the object whose handle is to be written
     * @exception IOException	gets thrown when an IO error occurs.
     * @return			1 if it is a new object, -1 if it is not.
     */
    public int writeKnownObjectHeader(Object ref) throws IOException {

	if (ref == null) {
	    writeHandle(NUL_HANDLE);
	    return 0;
	}

	int handle = references.find(ref);

	if (handle == 0) {
	    handle = next_handle++;
	    references.put(ref, handle);
	    if(DEBUG) {
		System.err.println("writeKnownObjectHeader -> references[" + handle + "] = " + (ref == null ? "null" : ref));
	    }
	    writeHandle(handle | TYPE_BIT);
	    return 1;
	}

	if(DEBUG) {
	    System.err.println("writeKnownObjectHeader -> writing OLD HANDLE " + handle);
	}
	writeHandle(handle);
	return -1;
    }

    /**
     * Writes the serializable fields of an object <code>ref</code> using the type
     * information <code>t</code>.
     *
     * @param t		the type info for object <code>ref</code>
     * @param ref	the object of which the fields are to be written
     *
     * @exception IOException		 when an IO error occurs
     * @exception IllegalAccessException when access to a field is denied.
     */
    private void alternativeDefaultWriteObject(AlternativeTypeInfo t, Object ref) throws IOException, IllegalAccessException {
	int temp = 0;
	int i;

	for (i=0;i<t.double_count;i++)    writeDouble(t.serializable_fields[temp++].getDouble(ref));
	for (i=0;i<t.long_count;i++)      writeLong(t.serializable_fields[temp++].getLong(ref));
	for (i=0;i<t.float_count;i++)     writeFloat(t.serializable_fields[temp++].getFloat(ref));
	for (i=0;i<t.int_count;i++)       writeInt(t.serializable_fields[temp++].getInt(ref));
	for (i=0;i<t.short_count;i++)     writeShort(t.serializable_fields[temp++].getShort(ref));
	for (i=0;i<t.char_count;i++)      writeChar(t.serializable_fields[temp++].getChar(ref));
	for (i=0;i<t.byte_count;i++)      writeByte(t.serializable_fields[temp++].getByte(ref));
	for (i=0;i<t.boolean_count;i++)   writeBoolean(t.serializable_fields[temp++].getBoolean(ref));
	for (i=0;i<t.reference_count;i++) writeObject(t.serializable_fields[temp++].get(ref));
    }


    /**
     * Serializes an object <code>ref</code> using the type information <code>t</code>.
     *
     * @param t		the type info for object <code>ref</code>
     * @param ref	the object of which the fields are to be written
     *
     * @exception IOException		 when an IO error occurs
     * @exception IllegalAccessException when access to a field or <code>writeObject</code>
     * method is denied.
     */
    private void alternativeWriteObject(AlternativeTypeInfo t, Object ref) throws IOException, IllegalAccessException {
	if (t.superSerializable) {
	    alternativeWriteObject(t.alternativeSuperInfo, ref);
	}

	if (t.hasWriteObject) {
	    current_level = t.level;
	    try {
		t.invokeWriteObject(ref, this);
	    } catch (java.lang.reflect.InvocationTargetException e) {
		throw new IllegalAccessException("writeObject method: " + e);
	    }
	    return;
	}

	if(DEBUG) {
	    System.err.println("Using alternative writeObject for " + ref.getClass().getName());
	}

	alternativeDefaultWriteObject(t, ref);
    }

    /**
     * Push the notions of <code>current_object</code>, <code>current_level</code>,
     * and <code>current_putfield</code> on their stacks, and set new ones.
     * @param ref	the new <code>current_object</code> notion
     * @param level	the new <code>current_level</code> notion
     */
    public void push_current_object(Object ref, int level) {
	if (stack_size >= max_stack_size) {
	    max_stack_size = 2 * max_stack_size + 10;
	    Object[] new_o_stack = new Object[max_stack_size];
	    int[] new_l_stack = new int[max_stack_size];
	    ImplPutField[] new_p_stack = new ImplPutField[max_stack_size];
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
     * Pop the notions of <code>current_object</code>, <code>current_level</code>,
     * and <code>current_putfield</code> from their stacks.
     */
    public void pop_current_object() {
	stack_size--;
	current_object = object_stack[stack_size];
	current_level = level_stack[stack_size];
	current_putfield = putfield_stack[stack_size];
    }

    /**
     * This method takes care of writing the serializable fields of the parent object.
     * and also those of its parent objects.
     * It gets called by IOGenerator-generated code when an object
     * has a superclass that is serializable but not Ibis serializable.
     *
     * @param ref	the object with a non-Ibis-serializable parent object
     * @param classname	the name of the superclass
     * @exception IOException	gets thrown on IO error
     */
    public void writeSerializableObject(Object ref, String classname) throws IOException {
	AlternativeTypeInfo t = AlternativeTypeInfo.getAlternativeTypeInfo(classname);
	try {
	    push_current_object(ref, 0);
	    alternativeWriteObject(t, ref);
	    pop_current_object();
	} catch (IllegalAccessException e) {
	    throw new java.io.NotSerializableException("Serializable failed for : " + classname);
	}
    }

    /**
     * This method takes care of writing the serializable fields of the parent object.
     * and also those of its parent objects.
     *
     * @param ref	the object with a non-Ibis-serializable parent object
     * @param clazz	the superclass
     * @exception IOException	gets thrown on IO error
     */
    private void writeSerializableObject(Object ref, Class clazz) throws IOException {
	AlternativeTypeInfo t = AlternativeTypeInfo.getAlternativeTypeInfo(clazz);
	try {
	    push_current_object(ref, 0);
	    alternativeWriteObject(t, ref);
	    pop_current_object();
	} catch (IllegalAccessException e) {
	    throw new java.io.NotSerializableException("Serializable failed for : " + clazz.getName());
	}
    }

    /**
     * Writes a <code>String</code> object. This is a special case, because strings
     * are written as an UTF.
     *
     * @param ref		the string to be written
     * @exception IOException	gets thrown on IO error
     */
    public void writeString(String ref) throws IOException {
	if (ref == null) {
	    if (DEBUG) {
		System.out.println("writeString: --> null");
	    }
	    writeHandle(NUL_HANDLE);
	    return;
	}

	int handle = references.find(ref);
	if (handle == 0) {
	    handle = next_handle++;
	    references.put(ref, handle);
	    writeType(java.lang.String.class);
	    if (DEBUG) {
		System.out.println("writeString: " + ref);
	    }
	    writeUTF(ref);
	} else {
	    if (DEBUG) {
		System.out.println("writeString: found handle " + handle);
	    }
	    writeHandle(handle);
	}
    }

    /**
     * @inheritDoc
     */
    public void doWriteObject(Object ref) throws IOException {
	/*
	 * ref < 0:	type
	 * ref = 0:	null ptr
	 * ref > 0:	handle
	 */

	if (ref == null) {
	    writeHandle(NUL_HANDLE);
	    return;
	}
	/* TODO: deal with writeReplace! This should be done before
	   looking up the handle. If we don't want to do runtime
	   inspection, this should probably be handled somehow in
	   IOGenerator.
	   Note that the needed info is available in AlternativeTypeInfo,
	   but we don't want to use that when we have ibis.io.Serializable.
	   */
	int handle = references.find(ref);

	if (handle == 0) {
	    Class type = ref.getClass();
	    if(DEBUG) {
		System.err.println("Write object " + ref + " of class " + type + " handle = " + next_handle);
	    }

	    if (type.isArray()) {
		writeArray(ref, type, false);
	    } else {
		handle = next_handle++;
		references.put(ref, handle);
		if (DEBUG) {
		    System.out.println("doWriteObject: references[" + handle + "] = " + (ref == null ? "null" : ref));
		}
		writeType(type);
		if (type == java.lang.String.class) {
		    /* EEK this is not nice !! */
		    writeUTF((String)ref);
		} else if (type == java.lang.Class.class) {
		    /* EEK this is not nice !! */
		    writeUTF(((Class)ref).getName());
		} else if (IbisSerializationInputStream.isIbisSerializable(type)) {
		    ((ibis.io.Serializable)ref).generated_WriteObject(this);
		} else if (ref instanceof java.io.Externalizable) {
		    push_current_object(ref, 0);
		    ((java.io.Externalizable) ref).writeExternal(this);
		    pop_current_object();
		} else if (ref instanceof java.io.Serializable) {
		    writeSerializableObject(ref, type);
		} else { 
		    throw new java.io.NotSerializableException("Not Serializable : " + type.toString());
		}
	    }
	} else {
	    if(DEBUG) {
		System.err.println("Write duplicate handle " + handle + " class = " + ref.getClass());
	    }
	    writeHandle(handle);
	}
    }

    /**
     * @inheritDoc
     */
    public void writeUnshared(Object ref) throws IOException {
	if (ref == null) {
	    writeHandle(NUL_HANDLE);
	    return;
	}
	/* TODO: deal with writeReplace! This should be done before
	   looking up the handle. If we don't want to do runtime
	   inspection, this should probably be handled somehow in
	   IOGenerator.
	   Note that the needed info is available in AlternativeTypeInfo,
	   but we don't want to use that when we have ibis.io.Serializable.
	*/
	Class type = ref.getClass();
	if(DEBUG) {
	    System.err.println("Write object " + ref + " of class " + type + " handle = " + next_handle);
	}

	if (type.isArray()) {
	    writeArray(ref, type, true);
	} else {
	    writeType(type);
	    if (type == java.lang.String.class) {
		/* EEK this is not nice !! */
		writeUTF((String)ref);
	    } else if (type == java.lang.Class.class) {
		/* EEK this is not nice !! */
		writeUTF(((Class)ref).getName());
	    } else if (ref instanceof java.io.Externalizable) {
		push_current_object(ref, 0);
		((java.io.Externalizable) ref).writeExternal(this);
		pop_current_object();
	    } else if (IbisSerializationInputStream.isIbisSerializable(type)) {
		((ibis.io.Serializable)ref).generated_WriteObject(this);
	    } else if (ref instanceof java.io.Serializable) {
		writeSerializableObject(ref, type);
	    } else {
		throw new RuntimeException("Not Serializable : " + type.toString());
	    }
	}
    }

    /**
     * @inheritDoc
     */
    public void flush() throws IOException {
	if (DEBUG) {
	    System.out.println("flushing ...");
	}
	out.flush();
    }

    /**
     * @inheritDoc
     */
    public void close() throws IOException {
	flush();
	out.close();
    }

    /**
     * @inheritDoc
     */
    public void useProtocolVersion(int version) {
	/* ignored. */
    }

    /**
     * @inheritDoc
     */
    protected void writeStreamHeader() {
	/* ignored. */
    }

    /**
     * @inheritDoc
     */
    protected void writeClassDescriptor(ObjectStreamClass desc) {
	/* ignored */
    }

    /* annotateClass does not have to be redefined: it is empty in the
       ObjectOutputStream implementation.
    */

    /**
     * @inheritDoc
     */
    public void writeFields() throws IOException {
	if (current_putfield == null) {
	    throw new NotActiveException("no PutField object");
	}
	current_putfield.writeFields();
    }

    /**
     * @inheritDoc
     */
    public PutField putFields() throws IOException {
	if (current_putfield == null) {
	    if (current_object == null) {
		throw new NotActiveException("not in writeObject");
	    }
	    Class type = current_object.getClass();
	    AlternativeTypeInfo t = AlternativeTypeInfo.getAlternativeTypeInfo(type);
	    current_putfield = new ImplPutField(t);
	}
	return current_putfield;
    }

    /**
     * The Ibis serialization implementation of <code>PutField</code>.
     */
    private class ImplPutField extends PutField {
	private double[]  doubles;
	private long[]	  longs;
	private int[]	  ints;
	private float[]   floats;
	private short[]   shorts;
	private char[]    chars;
	private byte[]	  bytes;
	private boolean[] booleans;
	private Object[]  references;
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
	    references = new Object[t.reference_count];
	    this.t = t;
	}

	public void put(String name, boolean value)
	    throws IllegalArgumentException {
	    booleans[t.getOffset(name, Boolean.TYPE)] = value;
	}

	public void put(String name, char value)
	    throws IllegalArgumentException {
	    chars[t.getOffset(name, Character.TYPE)] = value;
	}

	public void put(String name, byte value)
	    throws IllegalArgumentException {
	    bytes[t.getOffset(name, Byte.TYPE)] = value;
	}

	public void put(String name, short value)
	    throws IllegalArgumentException {
	    shorts[t.getOffset(name, Short.TYPE)] = value;
	}

	public void put(String name, int value)
	    throws IllegalArgumentException {
	    ints[t.getOffset(name, Integer.TYPE)] = value;
	}

	public void put(String name, long value)
	    throws IllegalArgumentException {
	    longs[t.getOffset(name, Long.TYPE)] = value;
	}

	public void put(String name, float value)
	    throws IllegalArgumentException {
	    floats[t.getOffset(name, Float.TYPE)] = value;
	}

	public void put(String name, double value)
	    throws IllegalArgumentException {
	    doubles[t.getOffset(name, Double.TYPE)] = value;
	}

	public void put(String name, Object value) {
	    references[t.getOffset(name, Object.class)] = value;
	}

	public void write(ObjectOutput out) throws IOException {
	    for (int i = 0; i < t.double_count; i++) out.writeDouble(doubles[i]);
	    for (int i = 0; i < t.float_count; i++) out.writeFloat(floats[i]);
	    for (int i = 0; i < t.long_count; i++) out.writeLong(longs[i]);
	    for (int i = 0; i < t.int_count; i++) out.writeInt(ints[i]);
	    for (int i = 0; i < t.short_count; i++) out.writeShort(shorts[i]);
	    for (int i = 0; i < t.char_count; i++) out.writeChar(chars[i]);
	    for (int i = 0; i < t.byte_count; i++) out.writeByte(bytes[i]);
	    for (int i = 0; i < t.boolean_count; i++) out.writeBoolean(booleans[i]);
	    for (int i = 0; i < t.reference_count; i++) out.writeObject(references[i]);
	}

	void writeFields() throws IOException {
	    for (int i = 0; i < t.double_count; i++) writeDouble(doubles[i]);
	    for (int i = 0; i < t.float_count; i++) writeFloat(floats[i]);
	    for (int i = 0; i < t.long_count; i++) writeLong(longs[i]);
	    for (int i = 0; i < t.int_count; i++) writeInt(ints[i]);
	    for (int i = 0; i < t.short_count; i++) writeShort(shorts[i]);
	    for (int i = 0; i < t.char_count; i++) writeChar(chars[i]);
	    for (int i = 0; i < t.byte_count; i++) writeByte(bytes[i]);
	    for (int i = 0; i < t.boolean_count; i++) writeBoolean(booleans[i]);
	    for (int i = 0; i < t.reference_count; i++) writeObject(references[i]);
	}
    }

    /**
     * This method writes the serializable fields of object <code>ref</code> at the
     * level indicated by <code>depth</code> * (see the explanation at the declaration
     * of the <code>current_level</code> field.
     * It gets called from IOGenerator-generated code, when a parent object
     * is serializable but not Ibis serializable.
     *
     * @param ref		the object of which serializable fields must be written
     * @param depth		an indication of the current "view" of the object
     * @exception IOException	gets thrown when an IO error occurs.
     */
    public void defaultWriteSerializableObject(Object ref, int depth) throws IOException {
	Class type = ref.getClass();
	AlternativeTypeInfo t = AlternativeTypeInfo.getAlternativeTypeInfo(type);

	/*  Find the type info corresponding to the current invocation.
	    See the invokeWriteObject invocation in alternativeWriteObject.
	    */
	while (t.level > depth) {
	    t = t.alternativeSuperInfo;
	}
	try {
	    alternativeDefaultWriteObject(t, ref);
	} catch(IllegalAccessException e) {
	    throw new NotSerializableException("illegal access" + e);
	}
    }

    /**
     * @inheritDoc
     */
    public void defaultWriteObject() throws IOException, NotActiveException {
	if (current_object == null) {
	    throw new NotActiveException("defaultWriteObject without a current object");
	}

	Object ref = current_object;
	Class type = ref.getClass();

	if(DEBUG) {
	    System.err.println("Default write object " + ref);
	}

	if (IbisSerializationInputStream.isIbisSerializable(type)) {
	    /* Note that this will take the generated_DefaultWriteObject of the
	       dynamic type of ref. The current_level variable actually indicates
	       which instance of generated_DefaultWriteObject should do some work.
	       */
	    ((ibis.io.Serializable)ref).generated_DefaultWriteObject(this, current_level);
	} else if (ref instanceof java.io.Serializable) {
	    AlternativeTypeInfo t = AlternativeTypeInfo.getAlternativeTypeInfo(type);

	    /*	Find the type info corresponding to the current invocation.
		See the invokeWriteObject invocation in alternativeWriteObject.
		*/
	    while (t.level > current_level) {
		t = t.alternativeSuperInfo;
	    }
	    try {
		alternativeDefaultWriteObject(t, ref);
	    } catch(IllegalAccessException e) {
		throw new NotSerializableException("illegal access" + e);
	    }
	} else { 
	    throw new java.io.NotSerializableException("Not Serializable : " + type.toString());
	}
    }
}
