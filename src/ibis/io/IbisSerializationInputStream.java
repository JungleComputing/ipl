package ibis.io;

import java.io.ObjectInput;
import java.io.IOException;
import java.io.NotActiveException;
import java.io.Serializable;
import java.io.Externalizable;

public final class IbisSerializationInputStream extends SerializationInputStream
	implements IbisStreamFlags {

    private short[]	indices        = new short[PRIMITIVE_TYPES + 1];
    public byte[]	byte_buffer    = new byte[BYTE_BUFFER_SIZE];
    public char[]	char_buffer    = new char[CHAR_BUFFER_SIZE];
    public short[]	short_buffer   = new short[SHORT_BUFFER_SIZE];
    public int[]	int_buffer     = new int[INT_BUFFER_SIZE];
    public long[]	long_buffer    = new long[LONG_BUFFER_SIZE];
    public float[]	float_buffer   = new float[FLOAT_BUFFER_SIZE];
    public double[]	double_buffer  = new double[DOUBLE_BUFFER_SIZE];

    public int[]	handle_buffer  = new int[HANDLE_BUFFER_SIZE];

    public int		byte_index;
    public int		char_index;
    public int		short_index;
    public int		int_index;
    public int		long_index;
    public int		float_index;
    public int		double_index;
    public int		handle_index;

    public int		max_byte_index;
    public int		max_char_index;
    public int		max_short_index;
    public int		max_int_index;
    public int		max_long_index;
    public int		max_float_index;
    public int		max_double_index;
    public int		max_handle_index;

    class TypeInfo { 
	Class clazz;		
	boolean isArray;
	boolean isString;

	// for ibis.io.Serializable    
	Generator gen;

	// for java.io.Serializable
	AlternativeTypeInfo altInfo;

	TypeInfo(Class clzz, boolean isArray, boolean isString, Generator gen) {
	    this.clazz = clzz;
	    this.isArray = isArray;
	    this.isString = isString;
	    this.gen = gen;

	    if (gen == null) { 
		altInfo = new AlternativeTypeInfo(clzz);
	    }	   
	} 
    } 

    IbisVector objects = new IbisVector();
    int next_object;

    ArrayInputStream in;

    /* Type id management */
    private int next_type = 1;
    private IbisVector types;

    private Class stringClass;

    /* Notion of a current object, needed for defaultWriteObject. */
    private Object current_object;
    private int current_depth;

    public IbisSerializationInputStream(ArrayInputStream in) throws IOException {
	super();
	types = new IbisVector();
	types.add(0, null);	// Vector requires this
	types.add(TYPE_BOOLEAN, new TypeInfo(classBooleanArray, true, false, null));
	types.add(TYPE_BYTE,    new TypeInfo(classByteArray, true, false, null));
	types.add(TYPE_CHAR,    new TypeInfo(classCharArray, true, false, null));
	types.add(TYPE_SHORT,   new TypeInfo(classShortArray, true, false, null));
	types.add(TYPE_INT,     new TypeInfo(classIntArray, true, false, null));
	types.add(TYPE_LONG,    new TypeInfo(classLongArray, true, false, null));
	types.add(TYPE_FLOAT,   new TypeInfo(classFloatArray, true, false, null));
	types.add(TYPE_DOUBLE,  new TypeInfo(classDoubleArray, true, false, null));
	next_type = PRIMITIVE_TYPES;

	try { 
	    stringClass = Class.forName("java.lang.String");
	} catch (Exception e) { 
	    System.err.println("Failed to find java.lang.String " + e);
	    System.exit(1);
	}
	this.in = in;
	objects.clear();
	next_object = CONTROL_HANDLES;
    }

    public String serializationImplName() {
	return "ibis";
    }

    public void reset() {
	if(DEBUG) {
	    System.err.println("IN(" + this + ") reset: next handle = " + next_object + "."); 
	}
	objects.clear();
	next_object = CONTROL_HANDLES;
    }


    public void statistics() {
	System.err.println("IbisSerializationInputStream: statistics() not yet implemented");
    }


    public int bytesRead() {
	System.err.println("IbisSerializationInputStream: bytesRead() not yet implemented");
	return 0;
    }

    public void resetBytesRead() {
	System.err.println("IbisSerializationInputStream: resetBytesRead() not yet implemented");
    }

    /* This is the data output / object output part */

    public int read() throws IOException {
	if (byte_index == max_byte_index) {
	    receive();
	}
	return byte_buffer[byte_index++];
    }

    public int read(byte[] b) throws IOException {
	return read(b, 0, b.length);
    }

    public int read(byte[] b, int off, int len) throws IOException {
	readArraySliceByte(b, off, len);
	return len;
    }

    public long skip(long n) throws IOException {
	throw new IOException("skip not meaningful in a typed input stream");
    }

    public int skipBytes(int n) throws IOException {
	throw new IOException("skipBytes not meaningful in a typed input stream");
    }


    public int available() throws IOException {
	/* @@@ NOTE: this is not right. There are also some buffered arrays.. */

	return (max_byte_index - byte_index) +
		(max_char_index - char_index) +
		(max_short_index - short_index) +
		(max_int_index - int_index) +
		(max_long_index - long_index) +
		(max_float_index - float_index) +
		(max_double_index - double_index) +
		in.available();
    }

    public void readFully(byte[] b) throws IOException {
	readFully(b, 0, b.length);
    }


    public void readFully(byte[] b, int off, int len) throws IOException {
	read(b, off, len);
    }


    public boolean readBoolean() throws IOException {
	if (byte_index == max_byte_index) {
	    receive();
	}
	return (byte_buffer[byte_index++] == 1);
    }


    public byte readByte() throws IOException {
	if (byte_index == max_byte_index) {
	    receive();
	}
	return byte_buffer[byte_index++];
    }


    public int readUnsignedByte() throws IOException {
	if (byte_index == max_byte_index) {
	    receive();
	}
	int i = byte_buffer[byte_index++];
	if (i < 0) {
	    i += 256;
	}
	return i;
    }


    public short readShort() throws IOException {
	if (short_index == max_short_index) {
	    receive();
	}
	return short_buffer[short_index++];
    }


    public int readUnsignedShort() throws IOException {
	if (short_index == max_short_index) {
	    receive();
	}
	int i = short_buffer[short_index++];
	if (i < 0) {
	    i += 65536;
	}
	return i;
    }


    public char readChar() throws IOException {
	if (char_index == max_char_index) {
	    receive();
	}
	return char_buffer[char_index++];
    }


    public int readInt() throws IOException {
	if (int_index == max_int_index) {
	    receive();
	}
	return int_buffer[int_index++];
    }

    public int readHandle() throws IOException {
	if (handle_index == max_handle_index) {
	    receive();
	}
	if(DEBUG) {
	    System.err.println("read handle [" + handle_index + "] = " + Integer.toHexString(handle_buffer[handle_index]));
	}

	return handle_buffer[handle_index++];
    }

    public long readLong() throws IOException {
	if (long_index == max_long_index) {
	    receive();
	}
	return long_buffer[long_index++];
    }


    public float readFloat() throws IOException {
	if (float_index == max_float_index) {
	    receive();
	}
	return float_buffer[float_index++];
    }


    public double readDouble() throws IOException {
	if (double_index == max_double_index) {
	    receive();
	}
	return double_buffer[double_index++];
    }


    public String readUTF() throws IOException {
	int bn = readInt();

	if(DEBUG) {
	    System.err.println("readUTF: len = " + bn);
	}

	if(bn == -1) {
	    return null;
	}

/*
	    char[] data=new char[bn];
	    readArray(data, 0, bn);
	    String s = new String(data);
	    if(DEBUG) {
		System.err.println("returning string " + s);
	    }
	    return s;
*/

	byte[] b = new byte[bn];
	readArraySliceByte(b, 0, bn);

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

	if(DEBUG) {
	    System.err.println("read string "  + s);
	}
	return s;
    }


    private void readArraySliceHeader(Class clazz, int len) throws IOException {
	if(DEBUG) {
	    System.err.println("readArraySliceHeader: class = " + clazz + " len = " + len);
	}
	int type;
	while (true) {
	    type = readHandle();
	    if (type != RESET_HANDLE) {
		break;
	    }
	    reset();
	}

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


    public void readArraySliceBoolean(boolean[] ref, int off, int len)
	    throws IOException {
	readArraySliceHeader(classBooleanArray, len);
	in.readArray(ref, off, len);
    }


    public void readArraySliceByte(byte[] ref, int off, int len)
	    throws IOException {
	readArraySliceHeader(classByteArray, len);
	in.readArray(ref, off, len);
    }


    public void readArraySliceChar(char[] ref, int off, int len)
	    throws IOException {
	readArraySliceHeader(classCharArray, len);
	in.readArray(ref, off, len);
    }


    public void readArraySliceShort(short[] ref, int off, int len)
	    throws IOException {
	readArraySliceHeader(classShortArray, len);
	in.readArray(ref, off, len);
    }


    public void readArraySliceInt(int[] ref, int off, int len)
	    throws IOException {
	readArraySliceHeader(classIntArray, len);
	in.readArray(ref, off, len);
    }


    public void readArraySliceLong(long[] ref, int off, int len)
	    throws IOException {
	readArraySliceHeader(classLongArray, len);
	in.readArray(ref, off, len);
    }


    public void readArraySliceFloat(float[] ref, int off, int len)
	    throws IOException {
	readArraySliceHeader(classFloatArray, len);
	in.readArray(ref, off, len);
    }


    public void readArraySliceDouble(double[] ref, int off, int len)
	    throws IOException {
	readArraySliceHeader(classDoubleArray, len);
	in.readArray(ref, off, len);
    }


    public void readArraySliceObject(Object[] ref, int off, int len)
	    throws IOException, ClassNotFoundException {
	readArraySliceHeader(ref.getClass(), len);
	for (int i = off; i < off + len; i++) {
	    ref[i] = readObject();
	}
    }


    public void addObjectToCycleCheck(Object o) {
	objects.add(next_object++, o);
    }


    public Object getObjectFromCycleCheck(int handle) {
	Object o = objects.get(handle); // - CONTROL_HANDLES);
	
	if(DEBUG) {
	    System.err.println("getfromcycle: handle = " + (handle - CONTROL_HANDLES) + " obj = " + o);
	}

	return o;
    }


    public int readKnownTypeHeader() throws IOException {
	int handle_or_type = readHandle();

	if (handle_or_type == NUL_HANDLE) {
	    if(DEBUG) {
		System.err.println("readKnownTypeHeader -> read NUL_HANDLE");
	    }
	    return 0;
	}

	if ((handle_or_type & TYPE_BIT) == 0) {
	    if(DEBUG) {
		System.err.println("readKnownTypeHeader -> read OLD HANDLE " + 
				   (handle_or_type - CONTROL_HANDLES));
	    }
	    return handle_or_type;
	}

	if(DEBUG) {
	    System.err.println("readKnownTypeHeader -> read NEW HANDLE " + 
			       ((handle_or_type & TYPE_MASK) - CONTROL_HANDLES));
	}
	return -1;
    }


    Object readArray(Class arrayClass, int type) throws IOException, ClassNotFoundException {
	int len = readInt();

	if(DEBUG) {
	    System.err.println("Read array " + arrayClass + " length " + len);
	}

//		if(len < 0) len = -len;

	switch (type) {
	case TYPE_BOOLEAN:
	    boolean [] temp1 = new boolean[len];	
	    in.readArray(temp1, 0, len);
	    objects.add(next_object++, temp1);
	    return temp1;
	case TYPE_BYTE:
	    byte [] temp2 = new byte[len];
	    in.readArray(temp2, 0, len);
	    objects.add(next_object++, temp2);
	    return temp2;
	case TYPE_SHORT:
	    short [] temp3 = new short[len];
	    in.readArray(temp3, 0, len);
	    objects.add(next_object++, temp3);
	    return temp3;
	case TYPE_CHAR:
	    char [] temp4 = new char[len];
	    in.readArray(temp4, 0, len);
	    objects.add(next_object++, temp4);
	    return temp4;
	case TYPE_INT:
	    int [] temp5 = new int[len];
	    in.readArray(temp5, 0, len);
	    objects.add(next_object++, temp5);
	    return temp5;
	case TYPE_LONG:
	    long [] temp6 = new long[len];
	    in.readArray(temp6, 0, len);
	    objects.add(next_object++, temp6);
	    return temp6;
	case TYPE_FLOAT:
	    float [] temp7 = new float[len];
	    in.readArray(temp7, 0, len);
	    objects.add(next_object++, temp7);
	    return temp7;
	case TYPE_DOUBLE:
	    double [] temp8 = new double[len];
	    in.readArray(temp8, 0, len);
	    objects.add(next_object++, temp8);
	    return temp8;
	default:
	    if(DEBUG) {
		System.err.println("Read an array " + arrayClass + " of len " + len);
	    }
	    Object ref = java.lang.reflect.Array.newInstance(arrayClass.getComponentType(), len);
	    objects.add(next_object++, ref);

	    for (int i = 0; i < len; i++) {
		((Object[])ref)[i] = readObject();
		if(DEBUG) {
		    System.err.println("Read array[" + i + "] = " + ((Object[])ref)[i].getClass().getName());  
		}
	    }		
	
	    return ref;
	}
    }


    public TypeInfo readType(int type) throws IOException {
	if(DEBUG) {
	    System.err.println("Read type_number " + Integer.toHexString(type) + ", next = " + Integer.toHexString(next_type));
	}
	if (type < next_type) {
	    return (TypeInfo) types.get(type);
	} else {        
	    if (next_type != type) {
		System.err.println("EEK: readType: next_type != type");
		System.exit(1);
	    }

	    if(DEBUG) {
		System.err.println("NEW TYPE: reading utf");
	    }
	    String typeName = readUTF();
	    if(DEBUG) {
		System.err.println("New type " + typeName);
	    }
	    Class clazz = null;
	    try {
		clazz = Class.forName(typeName);        
	    } catch (ClassNotFoundException e) {
		throw new IOException("class " + typeName + " not found");
	    }

	    Generator g = null;           
	    TypeInfo t = null;

	    if (clazz.isArray()) { 
		t  = new TypeInfo(clazz, true, false, g);
	    } else if (clazz == stringClass) { 
		t = new TypeInfo(clazz, false, true, g);
	    } else { 
		try { 
		    Class gen_class = Class.forName(typeName + "_ibis_io_Generator");
		    g = (Generator) gen_class.newInstance();
		} catch (Exception e) { 
		    System.err.println("WARNING: Failed to find generator for " + clazz.getName());
// + " error: " + e);
// failed to get generator class -> use null
		}
		t = new TypeInfo(clazz, false, false, g);
	    } 
	
	    types.add(next_type, t);
	    next_type++;
	
	    return t;
	}
    }
    
    private void alternativeDefaultReadObject(AlternativeTypeInfo t, Object ref) throws IOException, IllegalAccessException {
	int temp = 0;
	for (int i=0;i<t.double_count;i++)    t.serializable_fields[temp++].setDouble(ref, readDouble());
	for (int i=0;i<t.long_count;i++)      t.serializable_fields[temp++].setLong(ref, readLong());
	for (int i=0;i<t.float_count;i++)     t.serializable_fields[temp++].setFloat(ref, readFloat());
	for (int i=0;i<t.int_count;i++)       t.serializable_fields[temp++].setInt(ref, readInt());
	for (int i=0;i<t.short_count;i++)     t.serializable_fields[temp++].setShort(ref, readShort());
	for (int i=0;i<t.char_count;i++)      t.serializable_fields[temp++].setChar(ref, readChar());
	for (int i=0;i<t.boolean_count;i++)   t.serializable_fields[temp++].setBoolean(ref, readBoolean());
	try {
	    for (int i=0;i<t.reference_count;i++) t.serializable_fields[temp++].set(ref, readObject());
	} catch(ClassNotFoundException e2) {
	    throw new IOException("class not found exception" + e2);
	}
    }

    private void alternativeReadObject(AlternativeTypeInfo t, Object ref) throws IOException, IllegalAccessException {
	    
	if (t.superSerializable) { 
	    alternativeReadObject(t.alternativeSuperInfo, ref);
	} 

	if (t.hasReadObject) {
	    current_depth = t.level;
	    t.invokeReadObject(ref, this);
	    return;
	}

	if (DEBUG) {
	    System.err.println("Using alternative readObject for " + ref.getClass().getName());
	}

	alternativeDefaultReadObject(t, ref);
    } 


    private native Object createUninitializedObject(Class type);

    public Object set_current_object(Object ref, int depth) {
        Object sav_current_object = current_object;
	current_object = ref;
	current_depth = depth;
	return sav_current_object;
    }

    public Object doReadObject() throws IOException, ClassNotFoundException {

	/*
	 * ref < 0:    type
	 * ref = 0:    null ptr
	 * ref > 0:    handle
	 */

	int handle_or_type = readHandle();

	while (handle_or_type == RESET_HANDLE) {
	    reset();
	    handle_or_type = readHandle();
	}

	if (handle_or_type == NUL_HANDLE) {
	    return null;
	}

	if ((handle_or_type & TYPE_BIT) == 0) {
	    /* Ah, it's a handle. Look it up, return the stored ptr */
	    Object o = objects.get(handle_or_type);

	    if(DEBUG) {
		System.err.println("readobj: handle = " + (handle_or_type - CONTROL_HANDLES) + " obj = " + o);
	    }
	    return o;
	}

	int type = handle_or_type & TYPE_MASK;
	TypeInfo t = readType(type);

	if(DEBUG) {
	    System.err.println("read type " + t.clazz + " isarray " + t.isArray);
	}

	Object obj;

	if(DEBUG) {
	    System.err.println("t = "  + t);
	}

	if (t.isArray) {
	    obj = readArray(t.clazz, type);
	} else if (t.isString) {
	    obj = readUTF();
	    addObjectToCycleCheck(obj);
	} else if (t.gen != null) {
	    obj = t.gen.generated_newInstance(this);
	} else if (Externalizable.class.isAssignableFrom(t.clazz)) {
	    try {
		obj = t.clazz.newInstance();
	    } catch(Exception e) {
		throw new RuntimeException("Could not instantiate" + e);
	    }
	    Object sav_current_object = set_current_object(obj, 0);
	    ((java.io.Externalizable) obj).readExternal(this);
	    current_object = sav_current_object;
	} else {
	    // this is for java.io.Serializable
	    try {
		// obj = t.clazz.newInstance(); // this is not correct --> need native call ??? !!!
		obj = createUninitializedObject(t.clazz);
		addObjectToCycleCheck(obj);
		Object sav_current_object = set_current_object(obj, 0);
		alternativeReadObject(t.altInfo, obj);
		current_object = sav_current_object;
	    } catch (Exception e) {
		throw new RuntimeException("Couldn't deserialize or create object " + e);
	    }
	}

	return obj;
    }

    private void receive() throws IOException {
	int leftover = (max_handle_index - handle_index);

	if (leftover == 1 && handle_buffer[handle_index] == RESET_HANDLE) { 
	    // there is a 'reset' leftover
	    reset();
	    handle_index++;
	}
	if(ASSERTS) {
	    int sum = (max_byte_index - byte_index) + 
		    (max_char_index - char_index) + 
		    (max_short_index - short_index) + 
		    (max_int_index - int_index) + 
		    (max_long_index - long_index) + 
		    (max_float_index - float_index) + 
		    (max_double_index - double_index) +
		    (max_handle_index - handle_index);
	    if (sum != 0) { 
		System.err.println("EEEEK : receiving while there is data in buffer !!!");
		System.err.println("byte_index "   + (max_byte_index - byte_index));
		System.err.println("char_index "   + (max_char_index - char_index));
		System.err.println("short_index "  + (max_short_index -short_index));
		System.err.println("int_index "    + (max_int_index - int_index));
		System.err.println("long_index "   + (max_long_index -long_index));
		System.err.println("double_index " + (max_double_index -double_index));
		System.err.println("float_index "  + (max_float_index - float_index));
		System.err.println("handle_index " + (max_handle_index -handle_index));
		new Exception().printStackTrace();
		
		if ((max_handle_index -handle_index) > 0) { 
		    for (int i=handle_index;i<max_handle_index;i++) { 
			System.err.println("Handle(" + i + ") = " + handle_buffer[i]);
		    }
		}
		
		System.exit(1);
	    }
	}
	in.readArray(indices, 0, PRIMITIVE_TYPES);

	byte_index    = 0;
	char_index    = 0;
	short_index   = 0;
	int_index     = 0;
	long_index    = 0;
	float_index   = 0;
	double_index  = 0;
	handle_index  = 0;

	max_byte_index    = indices[TYPE_BYTE];
	max_char_index    = indices[TYPE_CHAR];
	max_short_index   = indices[TYPE_SHORT];
	max_int_index     = indices[TYPE_INT];
	max_long_index    = indices[TYPE_LONG];
	max_float_index   = indices[TYPE_FLOAT];
	max_double_index  = indices[TYPE_DOUBLE];
	max_handle_index  = indices[TYPE_HANDLE];

	if(DEBUG) {
	    System.err.println("reading bytes " + max_byte_index);
	    System.err.println("reading char " + max_char_index);
	    System.err.println("reading short " + max_short_index);
	    System.err.println("reading int " + max_int_index);
	    System.err.println("reading long " + max_long_index);
	    System.err.println("reading float " + max_float_index);
	    System.err.println("reading double " + max_double_index);
	    System.err.println("reading handle " + max_handle_index);
	}
//        eof               = indices[PRIMITIVE_TYPES] == 1;

	if (max_byte_index > 0) {
	    in.readArray(byte_buffer, 0, max_byte_index);
	}
	if (max_char_index > 0) {
	    in.readArray(char_buffer, 0, max_char_index);
	}
	if (max_short_index > 0) {
	    in.readArray(short_buffer, 0, max_short_index);
	}
	if (max_int_index > 0) {
	    in.readArray(int_buffer, 0, max_int_index);
	}
	if (max_long_index > 0) {
	    in.readArray(long_buffer, 0, max_long_index);
	}
	if (max_float_index > 0) {
	    in.readArray(float_buffer, 0, max_float_index);
	}
	if (max_double_index > 0) {
	    in.readArray(double_buffer, 0, max_double_index);
	}
	if (max_handle_index > 0) {
	    in.readArray(handle_buffer, 0, max_handle_index);
	}
    }

    public void close() throws IOException {
    }

    protected void readStreamHeader() {
	/* ignored */
    }

    public GetField readFields() throws IOException {
	/* TODO: TO BE WRITTEN! */
	return null;
    }

    public void defaultReadObject() throws IOException, NotActiveException {
	if (current_object == null) {
	    throw new NotActiveException("defaultReadObject without a current object");
	}
	Object ref = current_object;

	if (ref instanceof ibis.io.Serializable) {
	    ((ibis.io.Serializable)ref).generated_DefaultReadObject(this, current_depth);
	} else if (ref instanceof java.io.Serializable) {
	    Class type = ref.getClass();
	    try {
		AlternativeTypeInfo t = AlternativeTypeInfo.getAlternativeTypeInfo(type);

		/*  Find the type info corresponding to the current invocation.
		    See the invokeReadObject invocation in alternativeReadObject.
		*/
		while (t.level > current_depth) {
		    t = t.alternativeSuperInfo;
		}
		alternativeDefaultReadObject(t, ref);
	    } catch (IllegalAccessException e) {
		throw new RuntimeException("Serializable failed for : " + type.toString());
	    }
	} else {
	    Class type = ref.getClass();
	    throw new RuntimeException("Not Serializable : " + type.toString());
	}
    }
}
