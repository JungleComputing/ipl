package ibis.io;

import java.io.ObjectInput;
import java.io.ObjectStreamClass;
import java.io.IOException;
import java.io.NotActiveException;
import java.io.Serializable;
import java.io.Externalizable;
import java.io.UTFDataFormatException;
import java.io.StreamCorruptedException;
import java.io.NotSerializableException;

/**
 * This is the <code>SerializationInputStream</code> version that is used for Ibis serialization.
 * An effort has been made to make it look like and extend <code>java.io.ObjectInputStream</code>.
 * However, versioning is not supported, like it is in Sun serialization.
 */
public final class IbisSerializationInputStream extends SerializationInputStream implements IbisStreamFlags {

    /**
     * List of objects, for cycle checking.
     */
    IbisVector objects = new IbisVector();

    /**
     * First free object index.
     */
    int next_object;

    /**
     * The underlying <code>IbisDissipator</code>.
     * Must be public so that IOGenerator-generated code can access it.
     */
    public final IbisDissipator in;

    /**
     * First free type index.
     */
    private int next_type = 1;

    /**
     * List of types seen sofar.
     */
    private IbisVector types;

    /**
     * There is a notion of a "current" object. This is needed when a user-defined
     * <code>readObject</code> refers to <code>defaultReadObject</code> or to
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
     * level is the level at which <code>current_object</code> is being processed.
     */
    private int current_level;

    /**
     * The <code>current_object</code> and <code>current_level</code> are maintained in
     * stacks, so that they can be managed by IOGenerator-generated code.
     */
    private Object[] object_stack;
    private int[] level_stack;
    private int max_stack_size = 0;
    private int stack_size = 0;

    /**
     * <code>IbisTypeInfo</code> for <code>boolean</code> arrays.
     */
    private static IbisTypeInfo booleanArrayInfo = IbisTypeInfo.getIbisTypeInfo(classBooleanArray);

    /**
     * <code>IbisTypeInfo</code> for <code>byte</code> arrays.
     */
    private static IbisTypeInfo byteArrayInfo = IbisTypeInfo.getIbisTypeInfo(classByteArray);

    /**
     * <code>IbisTypeInfo</code> for <code>char</code> arrays.
     */
    private static IbisTypeInfo charArrayInfo = IbisTypeInfo.getIbisTypeInfo(classCharArray);

    /**
     * <code>IbisTypeInfo</code> for <code>short</code> arrays.
     */
    private static IbisTypeInfo shortArrayInfo = IbisTypeInfo.getIbisTypeInfo(classShortArray);

    /**
     * <code>IbisTypeInfo</code> for <code>int</code> arrays.
     */
    private static IbisTypeInfo intArrayInfo = IbisTypeInfo.getIbisTypeInfo(classIntArray);

    /**
     * <code>IbisTypeInfo</code> for <code>long</code> arrays.
     */
    private static IbisTypeInfo longArrayInfo = IbisTypeInfo.getIbisTypeInfo(classLongArray);

    /**
     * <code>IbisTypeInfo</code> for <code>float</code> arrays.
     */
    private static IbisTypeInfo floatArrayInfo = IbisTypeInfo.getIbisTypeInfo(classFloatArray);

    /**
     * <code>IbisTypeInfo</code> for <code>double</code> arrays.
     */
    private static IbisTypeInfo doubleArrayInfo = IbisTypeInfo.getIbisTypeInfo(classDoubleArray);

    /**
     * Constructor with an <code>IbisDissipator</code>.
     * @param in		the underlying <code>IbisDissipator</code>
     * @exception IOException	gets thrown when an IO error occurs.
     */
    public IbisSerializationInputStream(IbisDissipator in) throws IOException {
	super();
	init(true);
	this.in = in;
    }

    /**
     * Initializes the <code>objects</code> and <code>types</code> fields, including
     * their indices.
     *
     * @param do_types	set when the type table must be initialized as well (this
     * is not needed after a reset).
     */
    private void init(boolean do_types) {
	if (do_types) {
	    types = new IbisVector();
	    types.add(0, null);	// Vector requires this
	    types.add(TYPE_BOOLEAN,	booleanArrayInfo);
	    types.add(TYPE_BYTE,	byteArrayInfo);
	    types.add(TYPE_CHAR,	charArrayInfo);
	    types.add(TYPE_SHORT,	shortArrayInfo);
	    types.add(TYPE_INT,		intArrayInfo);
	    types.add(TYPE_LONG,	longArrayInfo);
	    types.add(TYPE_FLOAT,	floatArrayInfo);
	    types.add(TYPE_DOUBLE,	doubleArrayInfo);

	    next_type = PRIMITIVE_TYPES;
	}

	objects.clear();
	next_object = CONTROL_HANDLES;
    }

    /**
     * @inheritDoc
     */
    public String serializationImplName() {
	return "ibis";
    }

    /**
     * resets the stream, by clearing the object and type table.
     */
    private void do_reset() {
	if (DEBUG) {
	    System.err.println("IN(" + this + ") do_reset: next handle = " +
							next_object + ".");
	}
	init(false);
    }

    /**
     * @inheritDoc
     */
    public void clear() {
	objects.clear();
	next_object = CONTROL_HANDLES;
    }

    /**
     * @inheritDoc
     */
    public void statistics() {
	System.err.println("IbisSerializationInputStream: statistics() not yet implemented");
    }

    /**
     * @inheritDoc
     */
    public int bytesRead() {
	return in.bytesRead();
    }

    /**
     * @inheritDoc
     */
    public void resetBytesRead() {
	in.resetBytesRead();
    }

    /* This is the data output / object output part */

    /**
     * @inheritDoc
     */
    public int read() throws IOException {
	return in.readByte();
    }

    /**
     * @inheritDoc
     */
    public int read(byte[] b) throws IOException {
	return read(b, 0, b.length);
    }

    /**
     * @inheritDoc
     */
    public int read(byte[] b, int off, int len) throws IOException {
	readArray(b, off, len);
	return len;
    }

    /**
     * @inheritDoc
     */
    public long skip(long n) throws IOException {
	throw new IOException("skip not meaningful in a typed input stream");
    }

    /**
     * @inheritDoc
     */
    public int skipBytes(int n) throws IOException {
	throw new IOException("skipBytes not meaningful in a typed input stream");
    }

    /**
     * @inheritDoc
     */
    public int available() throws IOException {
	/* @@@ NOTE: this is not right. There are also some buffered arrays..*/

        /* @@@ NOTE(2): now it is ;) --N */

        return in.available();

    }

    /**
     * @inheritDoc
     */
    public void readFully(byte[] b) throws IOException {
	readFully(b, 0, b.length);
    }

    /**
     * @inheritDoc
     */
    public void readFully(byte[] b, int off, int len) throws IOException {
	read(b, off, len);
    }

    /**
     * @inheritDoc
     */
    public boolean readBoolean() throws IOException {
	return in.readBoolean();
    }

    /**
     * @inheritDoc
     */
    public byte readByte() throws IOException {
	return in.readByte();
    }

    /**
     * @inheritDoc
     */
    public int readUnsignedByte() throws IOException {
	int i = in.readByte();
	if (i < 0) {
	    i += 256;
	}
	return i;
    }

    /**
     * @inheritDoc
     */
    public short readShort() throws IOException {
	return in.readShort();
    }

    /**
     * @inheritDoc
     */
    public int readUnsignedShort() throws IOException {
	int i = in.readShort();
	if (i < 0) {
	    i += 65536;
	}
	return i;
    }

    /**
     * @inheritDoc
     */
    public char readChar() throws IOException {
	return in.readChar();
    }

    /**
     * @inheritDoc
     */
    public int readInt() throws IOException {
	return in.readInt();
    }

    /**
     * Reads a handle, which is just an int representing an index
     * in the object table.
     * @exception IOException	gets thrown when an IO error occurs.
     * @return 			the handle read.
     */
    private int readHandle() throws IOException {
	int handle = in.readInt();

       /* this replaces the checks for the reset handle
	   everywhere else. --N */
	while(handle == RESET_HANDLE) {
		if (DEBUG) {
			System.err.println("received a RESET");
		}
		do_reset();
		handle = in.readInt();
	}

	if (DEBUG) {
	    System.err.println("read handle " + handle);
	}

	return handle;
    }

    /**
     * @inheritDoc
     */
    public long readLong() throws IOException {
	return in.readLong();
    }

    /**
     * @inheritDoc
     */
    public float readFloat() throws IOException {
	return in.readFloat();
    }

    /**
     * @inheritDoc
     */
    public double readDouble() throws IOException {
	return in.readDouble();
    }

    /**
     * @inheritDoc
     */
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
		    throw new UTFDataFormatException("UTF Data Format Exception");
		}
		c[len++] = (char)(((b[i] & 0x1f) << 6) | (b[i] & 0x3f));
		i++;
	    } else if ((b[i] & ~0x0f) == 0xe0) {
		if (i + 2 >= bn ||
			(b[i + 1] & ~0x3f) != 0x80 ||
			(b[i + 2] & ~0x3f) != 0x80) {
		    throw new UTFDataFormatException("UTF Data Format Exception");
		}
		c[len++] = (char)(((b[i] & 0x0f) << 12) | ((b[i+1] & 0x3f) << 6) | b[i+2] & 0x3f);
	    } else {
		throw new UTFDataFormatException("UTF Data Format Exception");
	    }
	}

	String s = new String(c, 0, len);
	// System.out.println("readUTF: " + s);

	if (DEBUG) {
	    System.err.println("read string "  + s);
	}
	return s;
    }

    /**
     * Reads a <code>Class</code> object from the stream and tries to load it.
     * @exception IOException			when an IO error occurs.
     * @exception ClassNotFoundException	when the class could not be loaded.
     * @return the <code>Class</code> object read.
     */
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
	Class c = getClassFromName(s);

	addObjectToCycleCheck(c);
	return c;
    }

    /**
     * Reads the header of an array.
     * This header consists of a handle, a type, and an integer representing the length.
     * Note that the data read is mostly redundant. TODO: optimize?
     *
     * @exception IOException			when an IO error occurs.
     * @exception ClassNotFoundException	when the array class could not be loaded.
     */
    private void readArrayHeader(Class clazz, int len)
	    throws IOException, ClassNotFoundException {

	if (DEBUG) {
	    System.err.println("readArrayHeader: class = " + clazz + " len = " + len);
	}
	int type = readHandle();

	if ((type & TYPE_BIT) == 0) {
	    throw new StreamCorruptedException("Array slice header but I receive a HANDLE!");
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


    /**
     * @inheritDoc
     */
    public String readBytes() throws IOException {
	int len = readInt();
	byte[] bytes = new byte[len];
	for (int i = 0; i < len; i++) {
	    bytes[i] = readByte();
	}
	return new String(bytes);
    }

    /**
     * @inheritDoc
     */
    public String readChars() throws IOException {
	int len = readInt();
	char[] chars = new char[len];
	for (int i = 0; i < len; i++) {
	    chars[i] = readChar();
	}
	return new String(chars);
    }

    /**
     * @inheritDoc
     */
    public void readArray(boolean[] ref, int off, int len) throws IOException {
	try {
	    readArrayHeader(classBooleanArray, len);
	} catch (ClassNotFoundException e) {
	    throw new Error("require boolean[]", e);
	}
	in.readArray(ref, off, len);
    }

    /**
     * @inheritDoc
     */
    public void readArray(byte[] ref, int off, int len) throws IOException {
	try {
	    readArrayHeader(classByteArray, len);
	} catch (ClassNotFoundException e) {
	    throw new Error("require byte[]", e);
	}
	in.readArray(ref, off, len);
    }

    /**
     * @inheritDoc
     */
    public void readArray(char[] ref, int off, int len) throws IOException {
	try {
	    readArrayHeader(classCharArray, len);
	} catch (ClassNotFoundException e) {
	    throw new Error("require char[]", e);
	}
	in.readArray(ref, off, len);
    }

    /**
     * @inheritDoc
     */
    public void readArray(short[] ref, int off, int len) throws IOException {
	try {
	    readArrayHeader(classShortArray, len);
	} catch (ClassNotFoundException e) {
	    throw new Error("require short[]", e);
	}
	in.readArray(ref, off, len);
    }

    /**
     * @inheritDoc
     */
    public void readArray(int[] ref, int off, int len) throws IOException {
	try {
	    readArrayHeader(classIntArray, len);
	} catch (ClassNotFoundException e) {
	    throw new Error("require int[]", e);
	}
	in.readArray(ref, off, len);
    }

    /**
     * @inheritDoc
     */
    public void readArray(long[] ref, int off, int len) throws IOException {
	try {
	    readArrayHeader(classLongArray, len);
	} catch (ClassNotFoundException e) {
	    throw new Error("require long[]", e);
	}
	in.readArray(ref, off, len);
    }

    /**
     * @inheritDoc
     */
    public void readArray(float[] ref, int off, int len) throws IOException {
	try {
	    readArrayHeader(classFloatArray, len);
	} catch (ClassNotFoundException e) {
	    throw new Error("require float[]", e);
	}
	in.readArray(ref, off, len);
    }

    /**
     * @inheritDoc
     */
    public void readArray(double[] ref, int off, int len) throws IOException {
	try {
	    readArrayHeader(classDoubleArray, len);
	} catch (ClassNotFoundException e) {
	    throw new Error("require double[]", e);
	}
	in.readArray(ref, off, len);
    }

    /**
     * @inheritDoc
     */
    public void readArray(Object[] ref, int off, int len)
	    throws IOException, ClassNotFoundException {
	readArrayHeader(ref.getClass(), len);
	for (int i = off; i < off + len; i++) {
	    ref[i] = readObject();
	}
    }

    /**
     * Adds an object <code>o</code> to the object table, for cycle checking.
     * This method is public because it gets called from IOGenerator-generated code.
     * @param o		the object to be added
     */
    public void addObjectToCycleCheck(Object o) {
	objects.add(next_object, o);
/* No print here. The object may not have been completely initialized yet, so a toString may fail.
	if (DEBUG) {
	    System.out.println("objects[" + next_object + "] = " + (o == null ? "null" : o));
	}
*/
	next_object++;
    }

    /**
     * Looks up an object in the object table.
     * This method is public because it gets called from IOGenerator-generated code.
     * @param handle	the handle of the object to be looked up
     * @return		the corresponding object.
     */
    public Object getObjectFromCycleCheck(int handle) {
	Object o = objects.get(handle); // - CONTROL_HANDLES);

/* No print here. The object may not have been completely initialized yet, so a toString may fail.
	if (DEBUG) {
	    System.err.println("getfromcycle: handle = " + (handle - CONTROL_HANDLES) + " obj = " + o);
	}
*/

	return o;
    }

    /**
     * Method used by IOGenerator-generated code to read a handle, and determine
     * if it has to read a new object or get one from the object table.
     *
     * @exception IOException		when an IO error occurs.
     * @return	0 for a null object, -1 for a new object, and the handle for an
     * 		object already in the object table.
     */
    public int readKnownTypeHeader() throws IOException {
	int handle_or_type = readHandle();

	if ((handle_or_type & TYPE_BIT) == 0) {
	    // Includes NUL_HANDLE.
	    if (DEBUG) {
		if (handle_or_type == NUL_HANDLE) {
		    System.err.println("readKnownTypeHeader -> read NUL_HANDLE");
		}
		else {
		    System.err.println("readKnownTypeHeader -> read OLD HANDLE " +
			(handle_or_type - CONTROL_HANDLES));
		}
	    }
	    return handle_or_type;
	}

	if (DEBUG) {
	    System.err.println("readKnownTypeHeader -> read NEW HANDLE " +
		    ((handle_or_type & TYPE_MASK) - CONTROL_HANDLES));
	}
	return -1;
    }

    /**
     * Reads an array from the stream.
     * The handle and type have already been read.
     *
     * @param arrayClass	the type of the array to be read
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
    private Object readArray(Class arrayClass, int type)
	    throws IOException, ClassNotFoundException {
	int len = readInt();

	if (DEBUG) {
	    System.err.println("Read array " + arrayClass + " length " + len);
	}

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

    /**
     * This method tries to load a class given its name. It tries the default classloader,
     * and the one from the thread context. Also, apparently some classloaders do not understand
     * array classes, and from the Java documentation, it is not clear that they should.
     * Therefore, if the typeName indicates an array type, and the obvious attempts to load
     * the class fail, this method also tries to load the base type of the array.
     *
     * @param typeName	the name of the type to be loaded
     * @exception ClassNotFoundException is thrown when the class could not be loaded.
     * @return the loaded class
     */
    private Class getClassFromName(String typeName) throws ClassNotFoundException {
	try {
	    return Class.forName(typeName);
	} catch (ClassNotFoundException e) {
	    try {
		if (DEBUG) {
		    System.out.println("Could not load class " + typeName + " using Class.forName(), trying Thread.currentThread().getContextClassLoader().loadClass()");
		    System.out.println("Default class loader is " + this.getClass().getClassLoader());
		    System.out.println("now trying " + Thread.currentThread().getContextClassLoader());
		}
		return Thread.currentThread().getContextClassLoader().loadClass(typeName);
	    } catch (ClassNotFoundException e2) {
		int dim = 0;

		/* Some classloaders are not able to load array classes. Therefore, if the name
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
		    if (s[end-1] == ';') {
			end--;
		    }
		    typeName = typeName.substring(begin, end);

		    int dims[] = new int[dim];
		    for (int i = 0; i < dim; i++) dims[i] = 0;

		    /* Now try to load the base class, create an array from it and
		     * then return its class.
		     */
		    return java.lang.reflect.Array.newInstance(getClassFromName(typeName), dims).getClass();
		}
		throw e;
	    }
	}
    }

    /**
     * Returns the <code>IbisTypeInfo</code> corresponding to the type number given as parameter.
     * If the parameter indicates a type not yet read, its name is read (as an UTF), and the
     * class is loaded.
     *
     * @param type the type number
     * @exception ClassNotFoundException is thrown when the class could not be loaded.
     * @exception IOException is thrown when an IO error occurs
     * @return the <code>IbisTypeInfo</code> for <code>type</code>.
     */
    private IbisTypeInfo readType(int type) throws IOException, ClassNotFoundException {
	if (DEBUG) {
	    System.err.println("Read type_number " + Integer.toHexString(type) + ", next = " + Integer.toHexString(next_type));
	}
	if (type < next_type) {
	    return (IbisTypeInfo) types.get(type);
	}

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
    private native void setFieldBoolean(Object ref, String fieldname, boolean b);
    private native void setFieldObject(Object ref, String fieldname, String osig, Object o);

    /**
     * This method reads a value from the stream and assigns it to a final field.
     * IOGenerator uses this method when assigning final fields of an object that is rewritten, 
     * but super is not, and super is serializable. The problem with this situation is that
     * IOGenerator cannot create a proper constructor for this object, so cannot assign
     * to native fields without falling back to native code.
     *
     * @param ref		object with a final field
     * @param fieldName		name of the field
     * @exception IOException	is thrown when an IO error occurs.
     */
    public void readFieldDouble(Object ref, String fieldname) throws IOException {
	setFieldDouble(ref, fieldname, readDouble());
    }

    /**
     * See {@link #readFieldDouble(Object, String)} for a description.
     */
    public void readFieldLong(Object ref, String fieldname) throws IOException {
	setFieldLong(ref, fieldname, readLong());
    }

    /**
     * See {@link #readFieldDouble(Object, String)} for a description.
     */
    public void readFieldFloat(Object ref, String fieldname) throws IOException {
	setFieldFloat(ref, fieldname, readFloat());
    }

    /**
     * See {@link #readFieldDouble(Object, String)} for a description.
     */
    public void readFieldInt(Object ref, String fieldname) throws IOException {
	setFieldInt(ref, fieldname, readInt());
    }

    /**
     * See {@link #readFieldDouble(Object, String)} for a description.
     */
    public void readFieldShort(Object ref, String fieldname) throws IOException {
	setFieldShort(ref, fieldname, readShort());
    }

    /**
     * See {@link #readFieldDouble(Object, String)} for a description.
     */
    public void readFieldChar(Object ref, String fieldname) throws IOException {
	setFieldChar(ref, fieldname, readChar());
    }

    /**
     * See {@link #readFieldDouble(Object, String)} for a description.
     */
    public void readFieldByte(Object ref, String fieldname) throws IOException {
	setFieldByte(ref, fieldname, readByte());
    }

    /**
     * See {@link #readFieldDouble(Object, String)} for a description.
     */
    public void readFieldBoolean(Object ref, String fieldname) throws IOException {
	setFieldBoolean(ref, fieldname, readBoolean());
    }

    /**
     * See {@link #readFieldDouble(Object, String)} for a description.
     */
    public void readFieldString(Object ref, String fieldname) throws IOException {
	setFieldObject(ref, fieldname, "Ljava/lang/String;", readString());
    }

    /**
     * See {@link #readFieldDouble(Object, String)} for a description.
     * @exception ClassNotFoundException when the class could not be loaded.
     */
    public void readFieldClass(Object ref, String fieldname) throws IOException, ClassNotFoundException {
	setFieldObject(ref, fieldname, "Ljava/lang/Class;", readClass());
    }

    /**
     * See {@link #readFieldDouble(Object, String)} for a description.
     * @param fieldsig	signature of the field
     * @exception ClassNotFoundException when readObject throws it.
     */
    public void readFieldObject(Object ref, String fieldname, String fieldsig) throws IOException, ClassNotFoundException {
	setFieldObject(ref, fieldname, fieldsig, readObject());
    }

    /**
     * Reads the serializable fields of an object <code>ref</code> using the type
     * information <code>t</code>.
     *
     * @param t		the type info for object <code>ref</code>
     * @param ref	the object of which the fields are to be read
     *
     * @exception IOException		 when an IO error occurs
     * @exception IllegalAccessException when access to a field is denied.
     * @exception ClassNotFoundException when readObject throws it.
     */
    private void alternativeDefaultReadObject(AlternativeTypeInfo t, Object ref) throws ClassNotFoundException, IllegalAccessException, IOException {
	int temp = 0;
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
    }

    /**
     * De-serializes an object <code>ref</code> using the type information <code>t</code>.
     *
     * @param t		the type info for object <code>ref</code>
     * @param ref	the object of which the fields are to be read
     *
     * @exception IOException		 when an IO error occurs
     * @exception IllegalAccessException when access to a field or <code>readObject</code>
     * method is denied.
     * @exception ClassNotFoundException when readObject throws it.
     */
    private void alternativeReadObject(AlternativeTypeInfo t, Object ref)
	    throws ClassNotFoundException, IllegalAccessException, IOException {

	if (DEBUG) {
	    System.err.println("alternativeReadObject " + t);
	}
	if (t.superSerializable) {
	    alternativeReadObject(t.alternativeSuperInfo, ref);
	}

	if (t.hasReadObject) {
	    current_level = t.level;
	    try {
		t.invokeReadObject(ref, this);
	    } catch (java.lang.reflect.InvocationTargetException e) {
		throw new IllegalAccessException("readObject method: " + e);
	    }
	    return;
	}

	if (DEBUG) {
	    System.err.println("Using alternative readObject for " + ref.getClass().getName());
	}

	alternativeDefaultReadObject(t, ref);
    }

    /**
     * This method takes care of reading the serializable fields of the parent object.
     * and also those of its parent objects.
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
	AlternativeTypeInfo t = AlternativeTypeInfo.getAlternativeTypeInfo(classname);
	push_current_object(ref, 0);
	try {
	    alternativeReadObject(t, ref);
	} catch (IllegalAccessException e) {
	    throw new NotSerializableException(classname + " " + e);
	}
	pop_current_object();
    }

    /**
     * This method reads the serializable fields of object <code>ref</code> at the level indicated
     * by <code>depth</code>
     * (see the explanation at the declaration of the <code>current_level</code> field.
     * It gets called from IOGenerator-generated code, when a parent object
     * is serializable but not Ibis serializable.
     *
     * @param ref		the object of which serializable fields must be written
     * @param depth		an indication of the current "view" of the object
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
	    throw new NotSerializableException(type + " " + e);
	}
    }

    /**
     * Native method for creating an uninitialized object.
     * We need such a method to call the right constructor for it, which is the parameter-less
     * constructor of the "highest" superclass that is not serializable.
     * @param type			the type of the object to be created
     * @param non_serializable_super	the "highest" superclass of <code>type</code> that
     * 					is not serializable
     * @return the object created
     */
    private native Object createUninitializedObject(Class type, Class non_serializable_super);

    /**
     * Creates an uninitialized object of the type indicated by <code>classname</code>.
     * The corresponding constructor called is the parameter-less constructor of the
     * "highest" superclass that is not serializable.
     *
     * @param classname		name of the class
     * @exception IOException	gets thrown when an IO error occurs.
     * @exception ClassNotFoundException when class <code>classname</code> cannot be
     * loaded.
     */
    public Object create_uninitialized_object(String classname)
	    throws ClassNotFoundException, IOException {
	Class clazz = getClassFromName(classname);

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

    /**
     * Push the notions of <code>current_object</code> and <code>current_level</code>
     * on their stacks, and set new ones.
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
     * Pop the notions of <code>current_object</code> and <code>current_level</code>
     * from their stacks.
     */
    public void pop_current_object() {
	stack_size--;
	current_object = object_stack[stack_size];
	current_level = level_stack[stack_size];
    }

    /**
     * Reads and returns a <code>String</code> object. This is a special case, because strings
     * are written as an UTF.
     *
     * @exception IOException   gets thrown on IO error
     * @return the string read.
     */
    public String readString() throws IOException {
	int handle = readHandle();

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

	IbisTypeInfo t;
	try {
	    t = readType(handle & TYPE_MASK);
	} catch (ClassNotFoundException e) {
	    throw new Error("Cannot find class java.lang.String?", e);
	}

	String s = readUTF();
	if (DEBUG) {
	    System.out.println("readString returns " + s);
	}
	addObjectToCycleCheck(s);
	return s;
    }

    /**
     * @inheritDoc
     */
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
	    obj = getClassFromName(name);
	    addObjectToCycleCheck(obj);
	} else if (t.gen != null) {
	    obj = t.gen.generated_newInstance(this);
	} else if (Externalizable.class.isAssignableFrom(t.clazz)) {
	    try {
		// TODO: is this correct? I guess it is, when accessibility
		// is fixed.
		obj = t.clazz.newInstance();
	    } catch(Exception e) {
		throw new ClassNotFoundException("Could not instantiate" + e);
	    }
	    addObjectToCycleCheck(obj);
	    push_current_object(obj, 0);
	    ((java.io.Externalizable) obj).readExternal(this);
	    pop_current_object();
	} else {
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
	    try {
		alternativeReadObject(t.altInfo, obj);
	    } catch (IllegalAccessException e) {
		throw new NotSerializableException(type + " " + e);
	    }
	    pop_current_object();
	}
	return obj;
    }

    /**
     * @inheritDoc
     */
    public void close() throws IOException {
	types = null;
	objects.clear();
	in.close();
    }

    /**
     * Ignored for Ibis serialization.
     */
    protected void readStreamHeader() {
    }

    /**
     * @inheritDoc
     */
    public GetField readFields() throws IOException, ClassNotFoundException {
	if (current_object == null) {
	    throw new NotActiveException("not in readObject");
	}
	Class type = current_object.getClass();
	AlternativeTypeInfo t = AlternativeTypeInfo.getAlternativeTypeInfo(type);
	ImplGetField current_getfield = new ImplGetField(t);
	current_getfield.readFields();
	return current_getfield;
    }

    /**
     * The Ibis serialization implementation of <code>GetField</code>.
     */
    private class ImplGetField extends GetField {
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
	    for (int i = 0; i < t.boolean_count; i++) booleans[i] = readBoolean();
	    for (int i = 0; i < t.reference_count; i++) references[i] = readObject();
	}
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
    public static boolean isIbisSerializable(Class clazz) {
	Class[] intfs = clazz.getInterfaces();

	for (int i = 0; i < intfs.length; i++) {
	    if (intfs[i].equals(ibis.io.Serializable.class)) return true;
	}
	return false;
    }

    /**
     * @inheritDoc
     */
    public void defaultReadObject()
	    throws ClassNotFoundException, IOException, NotActiveException {
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
	    try {
		alternativeDefaultReadObject(t, ref);
	    } catch (IllegalAccessException e) {
		throw new NotSerializableException(type + " " + e);
	    }
	} else {
	    throw new NotSerializableException("Not Serializable : " + type.toString());
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
