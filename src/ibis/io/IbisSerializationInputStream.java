package ibis.io;

import java.io.ObjectInput;
import java.io.ObjectStreamClass;
import java.io.IOException;
import java.io.NotActiveException;
import java.io.Serializable;
import java.io.Externalizable;
import ibis.ipl.IbisIOException;

public final class IbisSerializationInputStream extends SerializationInputStream implements IbisStreamFlags {

    IbisVector objects = new IbisVector();
    int next_object;

    public IbisDissipator in;

    /* Type id management */
    private int next_type = 1;
    private IbisVector types;

    /* Notion of a current object, needed for defaultWriteObject. */
    private Object current_object;
    private int current_level;
    private ImplGetField current_getfield;

    private Object[] object_stack;
    private int[] level_stack;
    private ImplGetField[] getfield_stack;
    private int max_stack_size = 0;
    private int stack_size = 0;

    private static IbisTypeInfo booleanArrayInfo = IbisTypeInfo.getIbisTypeInfo(classBooleanArray);
    private static IbisTypeInfo byteArrayInfo = IbisTypeInfo.getIbisTypeInfo(classByteArray);
    private static IbisTypeInfo charArrayInfo = IbisTypeInfo.getIbisTypeInfo(classCharArray);
    private static IbisTypeInfo shortArrayInfo = IbisTypeInfo.getIbisTypeInfo(classShortArray);
    private static IbisTypeInfo intArrayInfo = IbisTypeInfo.getIbisTypeInfo(classIntArray);
    private static IbisTypeInfo longArrayInfo = IbisTypeInfo.getIbisTypeInfo(classLongArray);
    private static IbisTypeInfo floatArrayInfo = IbisTypeInfo.getIbisTypeInfo(classFloatArray);
    private static IbisTypeInfo doubleArrayInfo = IbisTypeInfo.getIbisTypeInfo(classDoubleArray);
   
    /**
      * Deprecated constructor only included for backwards compatibility.
      */
    public IbisSerializationInputStream(ArrayInputStream in)
							 throws IOException {
	super();
	init(new IbisArrayInputStreamDissipator(in));
    }

    public IbisSerializationInputStream(IbisDissipator in) throws IOException {
	super();
	init(in);
    }

    public void init(IbisDissipator in) {
	types = new IbisVector();
	types.add(0, null);	// Vector requires this
	types.add(TYPE_BOOLEAN,	booleanArrayInfo);
	types.add(TYPE_BYTE,	byteArrayInfo);
	types.add(TYPE_CHAR,	charArrayInfo);
	types.add(TYPE_SHORT,	shortArrayInfo);
	types.add(TYPE_INT,	intArrayInfo);
	types.add(TYPE_LONG,	longArrayInfo);
	types.add(TYPE_FLOAT,	floatArrayInfo);
	types.add(TYPE_DOUBLE,	doubleArrayInfo);

	next_type = PRIMITIVE_TYPES;

	this.in = in;
	objects.clear();
	next_object = CONTROL_HANDLES;
    }

    public String serializationImplName() {
	return "ibis";
    }

    public void reset() {
	if (DEBUG) {
	    System.err.println("IN(" + this + ") reset: next handle = " + 
							next_object + "."); 
	}
	init(in);
    }


    public void statistics() {
	System.err.println("IbisSerializationInputStream: statistics() not yet implemented");
    }

    public int bytesRead() {
                return in.bytesRead();
    }
                                                                                
    public void resetBytesRead() {
                in.resetBytesRead();
    }


    /* This is the data output / object output part */

    public int read() throws IOException {
	return in.readByte();
    }

    public int read(byte[] b) throws IOException {
	return read(b, 0, b.length);
    }

    public int read(byte[] b, int off, int len) throws IOException {
	readArray(b, off, len);
	return len;
    }

    public long skip(long n) throws IOException {
	throw new IOException("skip not meaningful in a typed input stream");
    }

    public int skipBytes(int n) throws IOException {
	throw new IOException("skipBytes not meaningful in a typed input stream");
    }

    public int available() throws IOException {
	/* @@@ NOTE: this is not right. There are also some buffered arrays..*/

        /* @@@ NOTE(2): now it is ;) --N */
                                                                                
        return in.available();

    }

    public void readFully(byte[] b) throws IOException {
	readFully(b, 0, b.length);
    }

    public void readFully(byte[] b, int off, int len) throws IOException {
	read(b, off, len);
    }

    public boolean readBoolean() throws IOException {
	return in.readBoolean();
    }

    public byte readByte() throws IOException {
	return in.readByte();
    }

    public int readUnsignedByte() throws IOException {
	int i = in.readByte();
	if (i < 0) {
	    i += 256;
	}
	return i;
    }

    public short readShort() throws IOException {
	return in.readShort();
    }

    public int readUnsignedShort() throws IOException {
	int i = in.readShort();
	if (i < 0) {
	    i += 65536;
	}
	return i;
    }

    public char readChar() throws IOException {
	return in.readChar();
    }

    public int readInt() throws IOException {
	return in.readInt();
    }

    public int readHandle() throws IOException {
	int handle = in.readInt();

       /* this replaces the checks for the reset handle
	   everywhere else. --N */
	while(handle == RESET_HANDLE) {
		if (DEBUG) {
			System.err.println("received a RESET");
		}
		reset();
		handle = in.readInt();
	}
	
	if (DEBUG) {
	    System.err.println("read handle " + handle);
	}

	return handle;
    }

    public long readLong() throws IOException {
	return in.readLong();
    }


    public float readFloat() throws IOException {
	return in.readFloat();
    }

    public double readDouble() throws IOException {
	return in.readDouble();
    }

    public String readUTF() throws IOException {
	int bn = readInt();

	if (DEBUG) {
	    System.err.println("readUTF: len = " + bn);
	}

	if (bn == -1) {
	    return null;
	}

	byte[] b = new byte[bn];
	readArray(b, 0, bn);

	int len = 0;
	char[] c = new char[bn];

	for (int i = 0; i < bn; i++) {
	    if ((b[i] & ~0x7f) == 0) {
		c[len++] = (char)(b[i] & 0x7f);
	    } else if ((b[i] & ~0x1f) == 0xc0) {
		if (i + 1 >= bn || (b[i + 1] & ~0x3f) != 0x80) {
		    throw new IOException("UTF Data Format Exception");
		}
		c[len++] = (char)(((b[i] & 0x1f) << 6) | (b[i] & 0x3f));
		i++;
	    } else if ((b[i] & ~0x0f) == 0xe0) {
		if (i + 2 >= bn ||
			(b[i + 1] & ~0x3f) != 0x80 ||
			(b[i + 2] & ~0x3f) != 0x80) {
		    throw new IOException("UTF Data Format Exception");
			}
		c[len++] = (char)(((b[i] & 0x0f) << 12) | ((b[i+1] & 0x3f) << 6) | b[i+2] & 0x3f);
	    } else {
		throw new IOException("UTF Data Format Exception");
	    }
	}

	String s = new String(c, 0, len);
	// System.out.println("readUTF: " + s);

	if (DEBUG) {
	    System.err.println("read string "  + s);
	}
	return s;
    }

    public Class readClass() throws IOException, ClassNotFoundException {
	int handle = readHandle();

	if (handle == NUL_HANDLE) {
	    return null;
	}

	if ((handle & TYPE_BIT) == 0) {
	    /* Ah, it's a handle. Look it up, return the stored ptr */
	    Class o = (Class) objects.get(handle);

	    if (DEBUG) {
		System.err.println("readobj: handle = " + (handle - CONTROL_HANDLES) + " obj = " + o);
	    }
	    return o;
	}

	IbisTypeInfo t = readType(handle & TYPE_MASK);

	String s = readUTF();
	Class c = null;
	
	try {
	    c = Class.forName(s);
	} catch(ClassNotFoundException e) {
	    c = Thread.currentThread().getContextClassLoader().loadClass(s);
	}
	addObjectToCycleCheck(c);
	return c;
    }

    private void readArrayHeader(Class clazz, int len)
	throws IOException {

	if (DEBUG) {
	    System.err.println("readArrayHeader: class = " + clazz + " len = " + len);
	}
	int type = readHandle();

	if (ASSERTS && ((type & TYPE_BIT) == 0)) {
	    throw new IOException("Array slice header but I receive a HANDLE!");
	}

	Class in_clazz = readType(type & TYPE_MASK).clazz;
	int in_len = readInt();

	if (ASSERTS && !clazz.isAssignableFrom(in_clazz)) {
	    throw new ClassCastException("Cannot assign class " + clazz +
		    " from read class " + in_clazz);
	}
	if (ASSERTS && in_len != len) {
	    throw new ArrayIndexOutOfBoundsException("Cannot read " + in_len +
		    " into " + len +
		    " elements");
	}
    }


    public String readBytes() throws IOException {
	int len = readInt();
	byte[] bytes = new byte[len];
	for (int i = 0; i < len; i++) {
	    bytes[i] = readByte();
	}
	return new String(bytes);
    }

    public String readChars() throws IOException {
	int len = readInt();
	char[] chars = new char[len];
	for (int i = 0; i < len; i++) {
	    chars[i] = readChar();
	}
	return new String(chars);
    }

    public void readArray(boolean[] ref, int off, int len)
	throws IOException {
	readArrayHeader(classBooleanArray, len);
	in.readArray(ref, off, len);
    }

    public void readArray(byte[] ref, int off, int len)
	throws IOException {
	readArrayHeader(classByteArray, len);
	in.readArray(ref, off, len);
    }

    public void readArray(char[] ref, int off, int len)
	throws IOException {
	readArrayHeader(classCharArray, len);
	in.readArray(ref, off, len);
    }

    public void readArray(short[] ref, int off, int len)
	throws IOException {
	readArrayHeader(classShortArray, len);
	in.readArray(ref, off, len);
    }

    public void readArray(int[] ref, int off, int len)
	throws IOException {
	readArrayHeader(classIntArray, len);
	in.readArray(ref, off, len);
    }

    public void readArray(long[] ref, int off, int len)
	throws IOException {
	readArrayHeader(classLongArray, len);
	in.readArray(ref, off, len);
    }

    public void readArray(float[] ref, int off, int len)
	throws IOException {
	readArrayHeader(classFloatArray, len);
	in.readArray(ref, off, len);
    }

    public void readArray(double[] ref, int off, int len)
	throws IOException {
	readArrayHeader(classDoubleArray, len);
	in.readArray(ref, off, len);
    }

    public void readArray(Object[] ref, int off, int len)
	throws IOException, ClassNotFoundException {
	readArrayHeader(ref.getClass(), len);
	for (int i = off; i < off + len; i++) {
	    ref[i] = readObject();
	}
    }

    public void addObjectToCycleCheck(Object o) {
	objects.add(next_object, o);
/* No print here. The object may not have been initialized yet, so a toString may fail.
	if (DEBUG) {
	    System.out.println("objects[" + next_object + "] = " + (o == null ? "null" : o));
	}
*/
	next_object++;
    }

    public Object getObjectFromCycleCheck(int handle) {
	Object o = objects.get(handle); // - CONTROL_HANDLES);

	if (DEBUG) {
	    System.err.println("getfromcycle: handle = " + (handle - CONTROL_HANDLES) + " obj = " + o);
	}

	return o;
    }

    public int readKnownTypeHeader() throws IOException {
	int handle_or_type = readHandle();

	if (handle_or_type == NUL_HANDLE) {
	    if (DEBUG) {
		System.err.println("readKnownTypeHeader -> read NUL_HANDLE");
	    }
	    return 0;
	}

	if ((handle_or_type & TYPE_BIT) == 0) {
	    if (DEBUG) {
		System.err.println("readKnownTypeHeader -> read OLD HANDLE " + 
			(handle_or_type - CONTROL_HANDLES));
	    }
	    return handle_or_type;
	}

	if (DEBUG) {
	    System.err.println("readKnownTypeHeader -> read NEW HANDLE " + 
		    ((handle_or_type & TYPE_MASK) - CONTROL_HANDLES));
	}
	return -1;
    }

    Object readArray(Class arrayClass, int type) throws IOException, ClassNotFoundException {
	int len = readInt();

	if (DEBUG) {
	    System.err.println("Read array " + arrayClass + " length " + len);
	}

	//		if (len < 0) len = -len;

	switch (type) {
	case TYPE_BOOLEAN:
	    boolean [] temp1 = new boolean[len];	
	    in.readArray(temp1, 0, len);
	    addObjectToCycleCheck(temp1);
	    return temp1;
	case TYPE_BYTE:
	    byte [] temp2 = new byte[len];
	    in.readArray(temp2, 0, len);
	    addObjectToCycleCheck(temp2);
	    return temp2;
	case TYPE_SHORT:
	    short [] temp3 = new short[len];
	    in.readArray(temp3, 0, len);
	    addObjectToCycleCheck(temp3);
	    return temp3;
	case TYPE_CHAR:
	    char [] temp4 = new char[len];
	    in.readArray(temp4, 0, len);
	    addObjectToCycleCheck(temp4);
	    return temp4;
	case TYPE_INT:
	    int [] temp5 = new int[len];
	    in.readArray(temp5, 0, len);
	    addObjectToCycleCheck(temp5);
	    return temp5;
	case TYPE_LONG:
	    long [] temp6 = new long[len];
	    in.readArray(temp6, 0, len);
	    addObjectToCycleCheck(temp6);
	    return temp6;
	case TYPE_FLOAT:
	    float [] temp7 = new float[len];
	    in.readArray(temp7, 0, len);
	    addObjectToCycleCheck(temp7);
	    return temp7;
	case TYPE_DOUBLE:
	    double [] temp8 = new double[len];
	    in.readArray(temp8, 0, len);
	    addObjectToCycleCheck(temp8);
	    return temp8;
	default:
	    if (DEBUG) {
		System.err.println("Read an array " + arrayClass + " of len " + len);
	    }
	    Object ref = java.lang.reflect.Array.newInstance(arrayClass.getComponentType(), len);
	    addObjectToCycleCheck(ref);

	    for (int i = 0; i < len; i++) {
		Object o = readObject();
		if (DEBUG) {
		    System.err.println("Read array[" + i + "] = " + (o == null ? "<null>" : o.getClass().getName()));
		}
		((Object[])ref)[i] = o;
	    }		

	    return ref;
	}
    }

    public IbisTypeInfo readType(int type) throws IOException {
	if (DEBUG) {
	    System.err.println("Read type_number " + Integer.toHexString(type) + ", next = " + Integer.toHexString(next_type));
	}
	if (type < next_type) {
	    return (IbisTypeInfo) types.get(type);
	} else {        
	    if (next_type != type) {
		System.err.println("type = " + type + ", next_type = " + next_type);
		System.err.println("EEK: readType: next_type != type");
		System.exit(1);
	    }

	    if (DEBUG) {
		System.err.println("NEW TYPE: reading utf");
	    }
	    String typeName = readUTF();
	    if (DEBUG) {
		System.err.println("New type " + typeName);
	    }


	    Class clazz = null;
	    try {
		clazz = Class.forName(typeName);
	    } catch (ClassNotFoundException e) {
		try {
		    if (DEBUG) {
			System.out.println("Could not load class " + typeName + " using Class.forName(), trying Thread.currentThread().getContextClassLoader().loadClass()");
			System.out.println("Default class loader is " + this.getClass().getClassLoader());
			System.out.println("now trying " + Thread.currentThread().getContextClassLoader());
		    }
		    int dim = 0;

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
			if (s[end-1] == ';') {
			    end--;
			}
			typeName = typeName.substring(begin, end);
		    }
		    clazz = Thread.currentThread().getContextClassLoader().loadClass(typeName);
		    if (dim > 0) {
			int dims[] = new int[dim];
			for (int i = 0; i < dim; i++) dims[i] = 0;
			Object o = java.lang.reflect.Array.newInstance(clazz, dims);
			clazz = o.getClass();
		    }

		} catch (ClassNotFoundException e2) {
		    throw new IOException("class " + typeName + " not found");
		}
	    }

	    IbisTypeInfo t = IbisTypeInfo.getIbisTypeInfo(clazz);

	    types.add(next_type, t);
	    next_type++;

	    return t;
	}
    }

    private native void setFieldDouble(Object ref, String fieldname, double d);
    private native void setFieldLong(Object ref, String fieldname, long l);
    private native void setFieldFloat(Object ref, String fieldname, float f);
    private native void setFieldInt(Object ref, String fieldname, int i);
    private native void setFieldShort(Object ref, String fieldname, short s);
    private native void setFieldChar(Object ref, String fieldname, char c);
    private native void setFieldByte(Object ref, String fieldname, byte b);
    private native void setFieldBoolean(Object ref, String fieldname, boolean b);
    private native void setFieldObject(Object ref, String fieldname, String osig, Object o);

    /** For IOGenerator: needed when assigning final fields of an object that is rewritten,
      but super is not, and super is serializable.
      */
    public void readFieldDouble(Object ref, String fieldname) throws IOException {
	setFieldDouble(ref, fieldname, readDouble());
    }

    public void readFieldLong(Object ref, String fieldname) throws IOException {
	setFieldLong(ref, fieldname, readLong());
    }

    public void readFieldFloat(Object ref, String fieldname) throws IOException {
	setFieldFloat(ref, fieldname, readFloat());
    }

    public void readFieldInt(Object ref, String fieldname) throws IOException {
	setFieldInt(ref, fieldname, readInt());
    }

    public void readFieldShort(Object ref, String fieldname) throws IOException {
	setFieldShort(ref, fieldname, readShort());
    }

    public void readFieldChar(Object ref, String fieldname) throws IOException {
	setFieldChar(ref, fieldname, readChar());
    }

    public void readFieldByte(Object ref, String fieldname) throws IOException {
	setFieldByte(ref, fieldname, readByte());
    }

    public void readFieldBoolean(Object ref, String fieldname) throws IOException {
	setFieldBoolean(ref, fieldname, readBoolean());
    }

    public void readFieldString(Object ref, String fieldname) throws IOException {
	setFieldObject(ref, fieldname, "Ljava/lang/String;", readString());
    }

    public void readFieldClass(Object ref, String fieldname) throws IOException, ClassNotFoundException {
	setFieldObject(ref, fieldname, "Ljava/lang/Class;", readClass());
    }

    public void readFieldObject(Object ref, String fieldname, String fieldsig) throws IOException, ClassNotFoundException {
	setFieldObject(ref, fieldname, fieldsig, readObject());
    }

    private void alternativeDefaultReadObject(AlternativeTypeInfo t, Object ref) throws IOException {
	int temp = 0;
	try {
	    for (int i=0;i<t.double_count;i++) {
		if (t.fields_final[temp]) {
		    setFieldDouble(ref, t.serializable_fields[temp].getName(), readDouble());
		}
		else {
		    t.serializable_fields[temp].setDouble(ref, readDouble());
		}
		temp++;
	    }
	    for (int i=0;i<t.long_count;i++) {
		if (t.fields_final[temp]) {
		    setFieldLong(ref, t.serializable_fields[temp].getName(), readLong());
		}
		else {
		    t.serializable_fields[temp].setLong(ref, readLong());
		}
		temp++;
	    }
	    for (int i=0;i<t.float_count;i++) {
		if (t.fields_final[temp]) {
		    setFieldFloat(ref, t.serializable_fields[temp].getName(), readFloat());
		}
		else {
		    t.serializable_fields[temp].setFloat(ref, readFloat());
		}
		temp++;
	    }
	    for (int i=0;i<t.int_count;i++) {
		if (t.fields_final[temp]) {
		    setFieldInt(ref, t.serializable_fields[temp].getName(), readInt());
		}
		else {
		    t.serializable_fields[temp].setInt(ref, readInt());
		}
		temp++;
	    }
	    for (int i=0;i<t.short_count;i++) {
		if (t.fields_final[temp]) {
		    setFieldShort(ref, t.serializable_fields[temp].getName(), readShort());
		}
		else {
		    t.serializable_fields[temp].setShort(ref, readShort());
		}
		temp++;
	    }
	    for (int i=0;i<t.char_count;i++) {
		if (t.fields_final[temp]) {
		    setFieldChar(ref, t.serializable_fields[temp].getName(), readChar());
		}
		else {
		    t.serializable_fields[temp].setChar(ref, readChar());
		}
		temp++;
	    }
	    for (int i=0;i<t.byte_count;i++) {
		if (t.fields_final[temp]) {
		    setFieldByte(ref, t.serializable_fields[temp].getName(), readByte());
		}
		else {
		    t.serializable_fields[temp].setByte(ref, readByte());
		}
		temp++;
	    }
	    for (int i=0;i<t.boolean_count;i++) {
		if (t.fields_final[temp]) {
		    setFieldBoolean(ref, t.serializable_fields[temp].getName(), readBoolean());
		}
		else {
		    t.serializable_fields[temp].setBoolean(ref, readBoolean());
		}
		temp++;
	    }
	    for (int i=0;i<t.reference_count;i++) {
		if (t.fields_final[temp]) {
		    String fieldname = t.serializable_fields[temp].getName();
		    String fieldtype = t.serializable_fields[temp].getType().getName();

		    if (fieldtype.startsWith("[")) {
		    } else {
			fieldtype = "L" + fieldtype.replace('.', '/') + ";";
		    }

		    // System.out.println("fieldname = " + fieldname);
		    // System.out.println("signature = " + fieldtype);

		    setFieldObject(ref, fieldname, fieldtype, readObject());
		}
		else {
		    Object o = readObject();
		    if (DEBUG) {
			if (o == null) {
			    System.out.println("Assigning null to field " +
				    t.serializable_fields[temp].getName());
			}
			else {
			    System.out.println("Assigning an object of type " +
				    o.getClass().getName() + " to field " +
				    t.serializable_fields[temp].getName());
			}
		    }
		    t.serializable_fields[temp].set(ref, o);
		}
		temp++;
	    }
	} catch(ClassNotFoundException e) {
	    throw new IbisIOException("class not found exception", e);
	} catch(IllegalAccessException e2) {
	    throw new IbisIOException("illegal access exception", e2);
	}
    }

    private void alternativeReadObject(AlternativeTypeInfo t, Object ref) throws IOException {

	if (DEBUG) {
	    System.err.println("alternativeReadObject " + t);
	}
	if (t.superSerializable) { 
	    alternativeReadObject(t.alternativeSuperInfo, ref);
	} 

	if (t.hasReadObject) {
	    current_level = t.level;
	    t.invokeReadObject(ref, this);
	    return;
	}

	if (DEBUG) {
	    System.err.println("Using alternative readObject for " + ref.getClass().getName());
	}

	alternativeDefaultReadObject(t, ref);
    } 


    public void readSerializableObject(Object ref, String classname) throws IOException {
	AlternativeTypeInfo t = AlternativeTypeInfo.getAlternativeTypeInfo(classname);
	push_current_object(ref, 0);
	alternativeReadObject(t, ref);
	pop_current_object();
    }

    public void defaultReadSerializableObject(Object ref, int depth) throws IOException {
	Class type = ref.getClass();
	AlternativeTypeInfo t = AlternativeTypeInfo.getAlternativeTypeInfo(type);

	/*  Find the type info corresponding to the current invocation.
	    See the invokeReadObject invocation in alternativeReadObject.
	    */
	while (t.level > depth) {
	    t = t.alternativeSuperInfo;
	}
	alternativeDefaultReadObject(t, ref);
    }

    private native Object createUninitializedObject(Class type, Class non_serializable_super);

    public Object create_uninitialized_object(String classname) throws IOException {
	Class clazz = null;
	try {
	    clazz = Class.forName(classname);
	} catch(Exception e) {
	    try {
		clazz = Thread.currentThread().getContextClassLoader().loadClass(classname);
	    } catch (ClassNotFoundException e2) {
		throw new IOException("class " + classname + " not found");
	    }
	}

	Class t2 = clazz;
	while (Serializable.class.isAssignableFrom(t2)) {
	    /* Find first non-serializable super-class. */
	    t2 = t2.getSuperclass();
	}
	// Calls constructor for non-serializable superclass.
	Object obj = createUninitializedObject(clazz, t2);

	addObjectToCycleCheck(obj);

	return obj;
    }

    public void push_current_object(Object ref, int level) {
	if (stack_size >= max_stack_size) {
	    max_stack_size = 2 * max_stack_size + 10;
	    Object[] new_o_stack = new Object[max_stack_size];
	    int[] new_l_stack = new int[max_stack_size];
	    ImplGetField[] new_g_stack = new ImplGetField[max_stack_size];
	    for (int i = 0; i < stack_size; i++) {
		new_o_stack[i] = object_stack[i];
		new_l_stack[i] = level_stack[i];
		new_g_stack[i] = getfield_stack[i];
	    }
	    object_stack = new_o_stack;
	    level_stack = new_l_stack;
	    getfield_stack = new_g_stack;
	}
	object_stack[stack_size] = current_object;
	level_stack[stack_size] = current_level;
	getfield_stack[stack_size] = current_getfield;
	stack_size++;
	current_object = ref;
	current_level = level;
    }

    public void pop_current_object() {
	stack_size--;
	current_object = object_stack[stack_size];
	current_level = level_stack[stack_size];
	current_getfield = getfield_stack[stack_size];
    }

    public String readString() throws IOException {
	int handle = readHandle();

	while (handle == RESET_HANDLE) {
	    reset();
	    handle = readHandle();
	}

	if (handle == NUL_HANDLE) {
	    if (DEBUG) {
		System.out.println("readString: --> null");
	    }
	    return null;
	}

	if ((handle & TYPE_BIT) == 0) {
	    /* Ah, it's a handle. Look it up, return the stored ptr */
	    String o = (String) objects.get(handle);

	    if (DEBUG) {
		System.err.println("readString: handle = " + (handle - CONTROL_HANDLES) + " obj = " + o);
	    }
	    return o;
	}

	IbisTypeInfo t = readType(handle & TYPE_MASK);

	String s = readUTF();
	if (DEBUG) {
	    System.out.println("readString returns " + s);
	}
	addObjectToCycleCheck(s);
	return s;
    }

    public Object doReadObject() throws IOException, ClassNotFoundException {

	/*
	 * ref < 0:    type
	 * ref = 0:    null ptr
	 * ref > 0:    handle
	 */

	int handle_or_type = readHandle();

	if (handle_or_type == NUL_HANDLE) {
	    return null;
	}

	if ((handle_or_type & TYPE_BIT) == 0) {
	    /* Ah, it's a handle. Look it up, return the stored ptr */
	    Object o = objects.get(handle_or_type);

	    if (DEBUG) {
		try {
		    System.err.println("readobj: handle = " + (handle_or_type - CONTROL_HANDLES) + " obj = " + o);
		} catch (Exception e) {
		    System.out.println("Object print got an exception:" + e);
		    System.out.println("Stacktrace: ------------");
		    e.printStackTrace();
		    System.out.println("------------------------");
		}
	    }
	    return o;
	}

	int type = handle_or_type & TYPE_MASK;
	IbisTypeInfo t = readType(type);

	if (DEBUG) {
	    System.err.println("read type " + t.clazz + " isarray " + t.isArray);
	}

	Object obj;

	if (DEBUG) {
	    System.err.println("t = "  + t);
	}

	if (t.isArray) {
	    obj = readArray(t.clazz, type);
	} else if (t.isString) {
	    obj = readUTF();
	    addObjectToCycleCheck(obj);
	} else if (t.isClass) {
	    String name = readUTF();
	    try {
		obj = Class.forName(name);
	    } catch(ClassNotFoundException e) {
		obj = Thread.currentThread().getContextClassLoader().loadClass(name);
	    }
	    addObjectToCycleCheck(obj);
	} else if (t.gen != null) {
	    obj = t.gen.generated_newInstance(this);
	} else if (Externalizable.class.isAssignableFrom(t.clazz)) {
	    try {
		// TODO: is this correct? I guess it is, when accessibility
		// is fixed.
		obj = t.clazz.newInstance();
	    } catch(Exception e) {
		throw new RuntimeException("Could not instantiate" + e);
	    }
	    addObjectToCycleCheck(obj);
	    push_current_object(obj, 0);
	    ((java.io.Externalizable) obj).readExternal(this);
	    pop_current_object();
	} else {
	    // this is for java.io.Serializable
	    try {
		// obj = t.clazz.newInstance(); Not correct: calls wrong constructor.
		Class t2 = t.clazz;
		while (Serializable.class.isAssignableFrom(t2)) {
		    /* Find first non-serializable super-class. */
		    t2 = t2.getSuperclass();
		}
		// Calls constructor for non-serializable superclass.
		obj = createUninitializedObject(t.clazz, t2);
		addObjectToCycleCheck(obj);
		push_current_object(obj, 0);
		alternativeReadObject(t.altInfo, obj);
		pop_current_object();
	    } catch (Exception e) {
		e.printStackTrace();
		throw new RuntimeException("Couldn't deserialize or create object " + e);
	    }
	}
	return obj;
    }

    public void close() throws IOException {
	in.close();
    }

    protected void readStreamHeader() {
	/* ignored */
    }

    public GetField readFields() throws IOException, ClassNotFoundException {
	if (current_object == null) {
	    throw new NotActiveException("not in readObject");
	}
	Class type = current_object.getClass();
	AlternativeTypeInfo t = AlternativeTypeInfo.getAlternativeTypeInfo(type);
	current_getfield = new ImplGetField(t);
	current_getfield.readFields();
	return current_getfield;
    }

    private class ImplGetField extends GetField {
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
	    for (int i = 0; i < t.double_count; i++) doubles[i] = readDouble();
	    for (int i = 0; i < t.float_count; i++) floats[i] = readFloat();
	    for (int i = 0; i < t.long_count; i++) longs[i] = readLong();
	    for (int i = 0; i < t.int_count; i++) ints[i] = readInt();
	    for (int i = 0; i < t.short_count; i++) shorts[i] = readShort();
	    for (int i = 0; i < t.char_count; i++) chars[i] = readChar();
	    for (int i = 0; i < t.byte_count; i++) bytes[i] = readByte();
	    for (int i = 0; i < t.boolean_count; i++)
		booleans[i] = readBoolean();
	    for (int i = 0; i < t.reference_count; i++)
		references[i] = readObject();
	}
    }

    public static boolean isIbisSerializable(Class clazz) {
	Class[] intfs = clazz.getInterfaces();

	for (int i = 0; i < intfs.length; i++) {
	    if (intfs[i].equals(ibis.io.Serializable.class)) return true;
	}
	return false;
    }

    public void defaultReadObject() throws IOException, NotActiveException {
	if (current_object == null) {
	    throw new NotActiveException("defaultReadObject without a current object");
	}
	Object ref = current_object;
	Class type = ref.getClass();

	if (isIbisSerializable(type)) {
	    ((ibis.io.Serializable)ref).generated_DefaultReadObject(this, current_level);
	} else if (ref instanceof java.io.Serializable) {
	    AlternativeTypeInfo t = AlternativeTypeInfo.getAlternativeTypeInfo(type);

	    /*  Find the type info corresponding to the current invocation.
	     *  See the invokeReadObject invocation in alternativeReadObject.
	     */
	    while (t.level > current_level) {
		t = t.alternativeSuperInfo;
	    }
	    alternativeDefaultReadObject(t, ref);
	} else {
	    throw new RuntimeException("Not Serializable : " + type.toString());
	}
    }

    static {
	try {
	    /*  Need conversion for allocation of uninitialized objects. */
	    System.loadLibrary("conversion");
	} catch(Throwable t) {
	    System.err.println("Could not load libconversion");
	}
    }
}
