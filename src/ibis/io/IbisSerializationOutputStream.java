package ibis.io;

import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.NotActiveException;
import java.io.IOException;
import java.io.ObjectOutput;

final class ArrayDescriptor {
	int		type;
	Object	        array;
	int		offset;
	int		len;
}


public final class IbisSerializationOutputStream extends SerializationOutputStream implements IbisStreamFlags {
    private ArrayOutputStream out;

    /* Handles for duplicate objects within one stream */
    private int next_handle;
    private IbisHash references  = new IbisHash();

    private ArrayDescriptor[] array    = new ArrayDescriptor[ARRAY_BUFFER_SIZE];

    private int		array_index;

    private Class stringClass;

    /* Type id management */
    private int next_type = 1;
    private IbisHash types;

    /* Notion of a "current" object.
       Needed for defaultWriteObject, and maybe others.
    */
    private Object current_object;
    private int current_level;
    private ImplPutField current_putfield;

    private Object[] object_stack;
    private int[] level_stack;
    private ImplPutField[] putfield_stack;
    private int max_stack_size = 0;
    private int stack_size = 0;

    public IbisSerializationOutputStream(ArrayOutputStream out) throws IOException {
	super();
	types = new IbisHash();
	types.put(classBooleanArray, TYPE_BOOLEAN | TYPE_BIT);
	types.put(classByteArray,    TYPE_BYTE | TYPE_BIT);
	types.put(classCharArray,    TYPE_CHAR | TYPE_BIT);
	types.put(classShortArray,   TYPE_SHORT | TYPE_BIT);
	types.put(classIntArray,     TYPE_INT | TYPE_BIT);
	types.put(classLongArray,    TYPE_LONG | TYPE_BIT);
	types.put(classFloatArray,   TYPE_FLOAT | TYPE_BIT);
	types.put(classDoubleArray,  TYPE_DOUBLE | TYPE_BIT);

	next_type = PRIMITIVE_TYPES;
	this.out    = out;
	for (int i = 0; i < ARRAY_BUFFER_SIZE; i++) {
	    array[i] = new ArrayDescriptor();
	}
	out.reset_indices();
	array_index = 0;
	references.clear();
	next_handle = CONTROL_HANDLES;
	try {
	    stringClass = Class.forName("java.lang.String");
	} catch (Exception e) {
	    System.err.println("Failed to find java.lang.String " + e);
	    System.exit(1);
	}
    }

    public String serializationImplName() {
	return "ibis";
    }

    public void reset() throws IOException {
	if (next_handle > CONTROL_HANDLES) { 
	    if(DEBUG) {
		System.err.println("OUT(" + this + ") reset: next handle = " + next_handle + ".");
	    }
	    references.clear();
	    writeHandle(RESET_HANDLE);
	    next_handle = CONTROL_HANDLES;
	}
    }

    public void statistics() {
	System.err.println("IbisOutput:");
	references.statistics();
    }

    public void print() {
	System.err.println("IbisTypedOutputStream.print() not implemented");
    }

    public int bytesWritten() {    
	return out.bytesWritten();
    }

    public void resetBytesWritten() {
	out.resetBytesWritten();
    }

    /* This is the data output / object output part */

    public void write(int v) throws IOException {
	if (out.byte_index + 1 == BYTE_BUFFER_SIZE) {
	    partial_flush();
	}
	out.byte_buffer[out.byte_index++] = (byte)(0xff & v);
    }

    public void write(byte[] b) throws IOException {
	write(b, 0, b.length);
    }

    public void write(byte[] b, int off, int len) throws IOException {
	writeArray(b, off, len, classByteArray, TYPE_BYTE, false);
    }

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
	writeArraySliceByte(b, 0, bn);
    }

    public void writeBoolean(boolean v) throws IOException {
	if (out.byte_index + 1 == BYTE_BUFFER_SIZE) {
	    partial_flush();
	}
	out.byte_buffer[out.byte_index++] = (byte) (v ? 1 : 0);
    }

    public void writeByte(int v) throws IOException {
	if (out.byte_index + 1 == BYTE_BUFFER_SIZE) {
	    partial_flush();
	}
	out.byte_buffer[out.byte_index++] = (byte)(0xff & v);
    }

    public void writeShort(int v) throws IOException {
	if (out.short_index + 1 == SHORT_BUFFER_SIZE) {
	    partial_flush();
	}
	out.short_buffer[out.short_index++] = (short)(0xffff & v);
    }

    public void writeChar(int v) throws IOException {
	if (out.char_index + 1 == CHAR_BUFFER_SIZE) {
	    partial_flush();
	}
	out.char_buffer[out.char_index++] = (char)(0xffff & v);
    }

    public void writeInt(int v) throws IOException {
	if (out.int_index + 1 == INT_BUFFER_SIZE) {
	    partial_flush();
	}
	out.int_buffer[out.int_index++] = v;
    }

    private void writeHandle(int v) throws IOException {
	if (out.handle_index + 1 == HANDLE_BUFFER_SIZE) {
	    partial_flush();
	}
	out.handle_buffer[out.handle_index++] = v;
    }

    public void writeLong(long v) throws IOException {
	if (out.long_index + 1 == LONG_BUFFER_SIZE) {
	    partial_flush();
	}
	out.long_buffer[out.long_index++] = v;
    }

    public void writeFloat(float f) throws IOException {
	if (out.float_index + 1 == FLOAT_BUFFER_SIZE) {
	    partial_flush();
	}
	out.float_buffer[out.float_index++] = f;
    }

    public void writeDouble(double d) throws IOException {
	if (out.double_index + 1 == DOUBLE_BUFFER_SIZE) {
	    partial_flush();
	}
	out.double_buffer[out.double_index++] = d;
    }

    public void writeBytes(String s) throws IOException {

	if (s == null) return;

	byte[] bytes = s.getBytes();
	int len = bytes.length;
	writeInt(len);
	for (int i = 0; i < len; i++) {
	    writeByte(bytes[i]);
	}
    }

    public void writeChars(String s) throws IOException {

	if (s == null) return;

	int len = s.length();
	writeInt(len);
	for (int i = 0; i < len; i++) {
	    writeChar(s.charAt(i));
	}
    }

    /* Often, the type of the array to be written is known (in an Ibis message
       for instance). Therefore, provide these methods for efficiency reasons.
    */
    public void writeArraySliceBoolean(boolean[] ref, int off, int len) throws IOException {
	writeArray(ref, off, len, arrayClasses[TYPE_BOOLEAN], TYPE_BOOLEAN, false);
    }

    public void writeArraySliceByte(byte[] ref, int off, int len) throws IOException {
	writeArray(ref, off, len, arrayClasses[TYPE_BYTE], TYPE_BYTE, false);
    }

    public void writeArraySliceShort(short[] ref, int off, int len) throws IOException {
	writeArray(ref, off, len, arrayClasses[TYPE_SHORT], TYPE_SHORT, false);
    }

    public void writeArraySliceChar(char[] ref, int off, int len) throws IOException {
	writeArray(ref, off, len, arrayClasses[TYPE_CHAR], TYPE_CHAR, false);
    }

    public void writeArraySliceInt(int[] ref, int off, int len) throws IOException {
	writeArray(ref, off, len, arrayClasses[TYPE_INT], TYPE_INT, false);
    }

    public void writeArraySliceLong(long[] ref, int off, int len) throws IOException {
	writeArray(ref, off, len, arrayClasses[TYPE_LONG], TYPE_LONG, false);
    }

    public void writeArraySliceFloat(float[] ref, int off, int len) throws IOException {
	writeArray(ref, off, len, arrayClasses[TYPE_FLOAT], TYPE_FLOAT, false);
    }

    public void writeArraySliceDouble(double[] ref, int off, int len) throws IOException {
	writeArray(ref, off, len, arrayClasses[TYPE_DOUBLE], TYPE_DOUBLE, false);
    }

    public void writeArraySliceObject(Object[] ref, int off, int len) throws IOException {
	Class clazz = ref.getClass();
	if (writeArrayHeader(ref, clazz, len, false)) {
	    for (int i = off; i < off + len; i++) {
		writeObject(ref[i]);
	    }
	}
    }

    private boolean writeTypeHandle(Object ref, Class type) throws IOException {
	int handle = references.find(ref);

	if (handle != 0) {
	    writeHandle(handle);
	    return true;
	}

	writeType(type);

	handle = next_handle++;
	references.put(ref, handle);

	return false;
    }

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

    private void writeArray(Object ref, Class arrayClass) throws IOException {
	if (false) {
	} else if (arrayClass == classByteArray) {
	    byte[] a = (byte[])ref;
	    int len = a.length;
	    writeArray(ref, 0, len, arrayClass, TYPE_BYTE, true);
	} else if (arrayClass == classIntArray) {
	    int[] a = (int[])ref;
	    int len = a.length;
	    writeArray(a, 0, len, arrayClass, TYPE_INT, true);
	} else if (arrayClass == classBooleanArray) {
	    boolean[] a = (boolean[])ref;
	    int len = a.length;
	    writeArray(a, 0, len, arrayClass, TYPE_BOOLEAN, true);
	} else if (arrayClass == classDoubleArray) {
	    double[] a = (double[])ref;
	    int len = a.length;
	    writeArray(a, 0, len, arrayClass, TYPE_DOUBLE, true);
	} else if (arrayClass == classCharArray) {
	    char[] a = (char[])ref;
	    int len = a.length;
	    writeArray(a, 0, len, arrayClass, TYPE_CHAR, true);
	} else if (arrayClass == classShortArray) {
	    short[] a = (short[])ref;
	    int len = a.length;
	    writeArray(a, 0, len, arrayClass, TYPE_SHORT, true);
	} else if (arrayClass == classLongArray) {
	    long[] a = (long[])ref;
	    int len = a.length;
	    writeArray(a, 0, len, arrayClass, TYPE_LONG, true);
	} else if (arrayClass == classFloatArray) {
	    float[] a = (float[])ref;
	    int len = a.length;
	    writeArray(a, 0, len, arrayClass, TYPE_FLOAT, true);
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
	    if(writeArrayHeader(a, arrayClass, len, true)) {
		for (int i = 0; i < len; i++) {
		    writeObject(a[i]);
		}
	    }
	}
    }

    private int newType(Class type) {
	int type_number = next_type++;

	type_number = (type_number | TYPE_BIT);
	types.put(type, type_number);                    

	return type_number;
    }

    private void writeType(Class type) throws IOException {
	int type_number = types.find(type);

	if (type_number != 0) {
	    writeHandle(type_number);	// TYPE_BIT is set, receiver sees it

	    if(DEBUG) {
		System.err.println("Write type number " + Integer.toHexString(type_number));
	    }
	    return;
	}

	type_number = newType(type);
	writeHandle(type_number);		// TYPE_BIT is set, receiver sees it
	if(DEBUG) {
	    System.err.println("Write NEW type " + type.getName() + " number " + Integer.toHexString(type_number));
	}
	writeUTF(type.getName());
    }

    private void writeArray(Object ref, int off, int len, Class clazz, int type, boolean doCycleCheck)
	    throws IOException {
	if(!writeArrayHeader(ref, clazz, len, doCycleCheck)) return;

	if (array_index + 1 == ARRAY_BUFFER_SIZE) {
	    partial_flush();
	}
	array[array_index].type   = type;
	array[array_index].offset = off;
	array[array_index].len    = len;
	array[array_index].array  = ref;
	array_index++;
    }

    /* This must be public, it is called by generated code which is in
       another package. --Rob
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
		System.err.println("writeKnownObjectHeader -> writing NEW HANDLE" + handle);
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

    private void alternativeDefaultWriteObject(AlternativeTypeInfo t, Object ref) throws IOException {
	int temp = 0;
	int i;

	try {
	    for (i=0;i<t.double_count;i++)    writeDouble(t.serializable_fields[temp++].getDouble(ref));
	    for (i=0;i<t.long_count;i++)      writeLong(t.serializable_fields[temp++].getLong(ref));
	    for (i=0;i<t.float_count;i++)     writeFloat(t.serializable_fields[temp++].getFloat(ref));
	    for (i=0;i<t.int_count;i++)       writeInt(t.serializable_fields[temp++].getInt(ref));
	    for (i=0;i<t.short_count;i++)     writeShort(t.serializable_fields[temp++].getShort(ref));
	    for (i=0;i<t.char_count;i++)      writeChar(t.serializable_fields[temp++].getChar(ref));
	    for (i=0;i<t.byte_count;i++)      writeByte(t.serializable_fields[temp++].getByte(ref));
	    for (i=0;i<t.boolean_count;i++)   writeBoolean(t.serializable_fields[temp++].getBoolean(ref));
	    for (i=0;i<t.reference_count;i++) writeObject(t.serializable_fields[temp++].get(ref));
	} catch(IllegalAccessException e) {
	    throw new IOException("illegal access" + e);
	}
    }


    private void alternativeWriteObject(AlternativeTypeInfo t, Object ref) throws IOException, IllegalAccessException {		
	if (t.superSerializable) { 
	    alternativeWriteObject(t.alternativeSuperInfo, ref);
	} 

	if (t.hasWriteObject) {
	    current_level = t.level;
	    t.invokeWriteObject(ref, this);
	    return;
	}

	if(DEBUG) {
	    System.err.println("Using alternative writeObject for " + ref.getClass().getName());
	}

	alternativeDefaultWriteObject(t, ref);
    } 

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

    public void pop_current_object() {
	stack_size--;
	current_object = object_stack[stack_size];
	current_level = level_stack[stack_size];
	current_putfield = putfield_stack[stack_size];
    }

    public void writeSerializableObject(Object ref, String classname) throws IOException {
	try {
	    AlternativeTypeInfo t = AlternativeTypeInfo.getAlternativeTypeInfo(classname);
	    push_current_object(ref, 0);
	    alternativeWriteObject(t, ref);
	    pop_current_object();
	} catch (IllegalAccessException e) {
	    throw new RuntimeException("Serializable failed for : " + classname);
	}
    }

    private void writeSerializableObject(Object ref, Class clazz) throws IOException {
	try {
	    AlternativeTypeInfo t = AlternativeTypeInfo.getAlternativeTypeInfo(clazz);
	    push_current_object(ref, 0);
	    alternativeWriteObject(t, ref);
	    pop_current_object();
	} catch (IllegalAccessException e) {
	    throw new RuntimeException("Serializable failed for : " + clazz.getName());
	}
    }

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
		writeArray(ref, type);
	    } else if (type == stringClass) {
		/* EEK this is not nice !! */
		handle = next_handle++;
		references.put(ref, handle);
		writeType(type);
		writeUTF((String)ref);
	    } else {
		handle = next_handle++;
		references.put(ref, handle);
		writeType(type);

		if (ref instanceof ibis.io.Serializable) { 
		    ((ibis.io.Serializable)ref).generated_WriteObject(this);
		} else if (ref instanceof java.io.Externalizable) {
		    push_current_object(ref, 0);
		    ((java.io.Externalizable) ref).writeExternal(this);
		    pop_current_object();
		} else if (ref instanceof java.io.Serializable) {
		    writeSerializableObject(ref, type);
		} else { 
		    throw new RuntimeException("Not Serializable : " + type.toString());
		}
	    }
	} else {
	    if(DEBUG) {
		System.err.println("Write duplicate handle " + handle + " class = " + ref.getClass());
	    }
	    writeHandle(handle);
	}
    }

    private void partial_flush() throws IOException {
	out.flushBuffers();

	/* Retain the order in which the arrays were pushed. This costs a
	 * cast at send/receive.
	 */
	for (int i = 0; i < array_index; i++) {
	    int len = array[i].len;
	    if (len < 0) {
		len = -len;
	    }
	
	    switch (array[i].type) {
	    case TYPE_BOOLEAN:
		out.writeArray((boolean[])array[i].array, array[i].offset, len);
		break;
	    case TYPE_BYTE:
		out.writeArray((byte[])array[i].array, array[i].offset, len);
		break;
	    case TYPE_CHAR:
		out.writeArray((char[])array[i].array, array[i].offset, len);
		break;
	    case TYPE_SHORT:
		out.writeArray((short[])array[i].array, array[i].offset, len);
		break;
	    case TYPE_INT:
		out.writeArray((int[])array[i].array, array[i].offset, len);
		break;
	    case TYPE_LONG:
		out.writeArray((long[])array[i].array, array[i].offset, len);
		break;
	    case TYPE_FLOAT:
		out.writeArray((float[])array[i].array, array[i].offset, len);
		break;
	    case TYPE_DOUBLE:
		out.writeArray((double[])array[i].array, array[i].offset, len);
		break;
	    }
	}

	out.flush();
	array_index = 0;
    }

    public void flush() throws IOException { 
	partial_flush();
	out.flush();
    } 

    public void close() throws IOException {
	flush();
	out.close();
    }

    public void useProtocolVersion(int version) {
	/* ignored. */
    }

    protected void writeStreamHeader() {
	/* ignored. */
    }

    protected void writeClassDescriptor(ObjectStreamClass desc) {
	/* ignored */
    }

    /* annotateClass does not have to be redefined: it is empty in the
       ObjectOutputStream implementation.
    */

    public void writeFields() throws IOException {
	if (current_putfield == null) {
	    throw new NotActiveException("no PutField object");
	}
	current_putfield.writeFields();
    }

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

    private class ImplPutField extends PutField {
	double[]  doubles;
	long[]	  longs;
	int[]	  ints;
	float[]   floats;
	short[]   shorts;
	char[]    chars;
	byte[]	  bytes;
	boolean[] booleans;
	Object[]  references;
	AlternativeTypeInfo t;

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
	    // throws IllegalArgumentException {
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

    public void defaultWriteSerializableObject(Object ref, int depth) throws IOException {
	Class type = ref.getClass();
	AlternativeTypeInfo t = AlternativeTypeInfo.getAlternativeTypeInfo(type);

	/*  Find the type info corresponding to the current invocation.
	    See the invokeWriteObject invocation in alternativeWriteObject.
	*/
	while (t.level > depth) {
	    t = t.alternativeSuperInfo;
	}
	alternativeDefaultWriteObject(t, ref);
    }

    public void defaultWriteObject() throws IOException, NotActiveException {
	if (current_object == null) {
	    throw new NotActiveException("defaultWriteObject without a current object");
	}

	Object ref = current_object;

	if(DEBUG) {
	    System.err.println("Default write object " + ref);
	}

	if (ref instanceof ibis.io.Serializable) { 
	    /* Note that this will take the generated_DefaultWriteObject of the
	       dynamic type of ref. The current_level variable actually indicates
	       which instance of generated_DefaultWriteObject should do some work.
	    */
	    ((ibis.io.Serializable)ref).generated_DefaultWriteObject(this, current_level);
	} else if (ref instanceof java.io.Serializable) {
	    Class type = ref.getClass();
	    AlternativeTypeInfo t = AlternativeTypeInfo.getAlternativeTypeInfo(type);

	    /*	Find the type info corresponding to the current invocation.
		See the invokeWriteObject invocation in alternativeWriteObject.
	    */
	    while (t.level > current_level) {
		t = t.alternativeSuperInfo;
	    }
	    alternativeDefaultWriteObject(t, ref);
	} else { 
	    Class type = ref.getClass();
	    throw new RuntimeException("Not Serializable : " + type.toString());
	}
    }
}
