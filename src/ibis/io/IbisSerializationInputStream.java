package ibis.io;

import java.io.ObjectInput;
import java.io.ObjectStreamClass;
import java.io.IOException;
import java.io.NotActiveException;
import java.io.Serializable;
import java.io.Externalizable;
import ibis.ipl.IbisIOException;

public final class IbisSerializationInputStream
	extends SerializationInputStream implements IbisStreamFlags {

    class TypeInfo { 
		Class clazz;		
		boolean isArray;
		boolean isString;

		// for ibis.io.Serializable    
		Generator gen;

		// for java.io.Serializable
		AlternativeTypeInfo altInfo;

		TypeInfo(Class clzz, boolean isArray, boolean isString, Generator gen){
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

    public ArrayInputStream in;

    /* Type id management */
    private int next_type = 1;
    private IbisVector types;

    private static Class stringClass;

    static {
		try { 
			stringClass = Class.forName("java.lang.String");
		} catch (Exception e) { 
			System.err.println("Failed to find java.lang.String " + e);
			System.exit(1);
		}
    }

    /* Notion of a current object, needed for defaultWriteObject. */
    private Object current_object;
    private int current_level;
    private ImplGetField current_getfield;

    private Object[] object_stack;
    private int[] level_stack;
    private ImplGetField[] getfield_stack;
    private int max_stack_size = 0;
    private int stack_size = 0;

    public IbisSerializationInputStream(ArrayInputStream in)
		throws IOException {
		super();
		init(in);
    }

    public void init(ArrayInputStream in) throws IOException {
		types = new IbisVector();
		types.add(0, null);	// Vector requires this
		types.add(TYPE_BOOLEAN,
				  new TypeInfo(classBooleanArray, true, false, null));
		types.add(TYPE_BYTE,
				  new TypeInfo(classByteArray, true, false, null));
		types.add(TYPE_CHAR,
				  new TypeInfo(classCharArray, true, false, null));
		types.add(TYPE_SHORT,
				  new TypeInfo(classShortArray, true, false, null));
		types.add(TYPE_INT,
				  new TypeInfo(classIntArray, true, false, null));
		types.add(TYPE_LONG,
				  new TypeInfo(classLongArray, true, false, null));
		types.add(TYPE_FLOAT,
				  new TypeInfo(classFloatArray, true, false, null));
		types.add(TYPE_DOUBLE,
				  new TypeInfo(classDoubleArray, true, false, null));
		next_type = PRIMITIVE_TYPES;

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

    private void receive() throws IOException {
		int leftover = in.max_handle_index - in.handle_index;

		if (leftover == 1 &&
			in.handle_buffer[in.handle_index] == RESET_HANDLE) {
			reset();
			in.handle_index++;
		}
		in.receive();
    }

    /* This is the data output / object output part */

    public int read() throws IOException {
		while (in.byte_index == in.max_byte_index) {
			receive();
		}
		return in.byte_buffer[in.byte_index++];
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
		/* @@@ NOTE: this is not right. There are also some buffered arrays..*/

		return in.available();
    }

    public void readFully(byte[] b) throws IOException {
		readFully(b, 0, b.length);
    }

    public void readFully(byte[] b, int off, int len) throws IOException {
		read(b, off, len);
    }

    public boolean readBoolean() throws IOException {
		while (in.byte_index == in.max_byte_index) {
			receive();
		}
		return (in.byte_buffer[in.byte_index++] == 1);
    }

    public byte readByte() throws IOException {
		while (in.byte_index == in.max_byte_index) {
			receive();
		}
		return in.byte_buffer[in.byte_index++];
    }

    public int readUnsignedByte() throws IOException {
		while (in.byte_index == in.max_byte_index) {
			receive();
		}
		int i = in.byte_buffer[in.byte_index++];
		if (i < 0) {
			i += 256;
		}
		return i;
    }

    public short readShort() throws IOException {
		while (in.short_index == in.max_short_index) {
			receive();
		}
		return in.short_buffer[in.short_index++];
    }

    public int readUnsignedShort() throws IOException {
		while (in.short_index == in.max_short_index) {
			receive();
		}
		int i = in.short_buffer[in.short_index++];
		if (i < 0) {
			i += 65536;
		}
		return i;
    }

    public char readChar() throws IOException {
		while (in.char_index == in.max_char_index) {
			receive();
		}
		return in.char_buffer[in.char_index++];
    }

    public int readInt() throws IOException {
		while (in.int_index == in.max_int_index) {
			receive();
		}
		return in.int_buffer[in.int_index++];
    }

    public int readHandle() throws IOException {
		while (in.handle_index == in.max_handle_index) {
			receive();
		}
		if(DEBUG) {
			System.err.println("read handle [" + in.handle_index + "] = " + Integer.toHexString(in.handle_buffer[in.handle_index]));
		}

		return in.handle_buffer[in.handle_index++];
    }

    public long readLong() throws IOException {
		while (in.long_index == in.max_long_index) {
			receive();
		}
		return in.long_buffer[in.long_index++];
    }


    public float readFloat() throws IOException {
		while (in.float_index == in.max_float_index) {
			receive();
		}
		return in.float_buffer[in.float_index++];
    }

    public double readDouble() throws IOException {
		while (in.double_index == in.max_double_index) {
			receive();
		}
		return in.double_buffer[in.double_index++];
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
		// System.out.println("readUTF: " + s);

		if(DEBUG) {
			System.err.println("read string "  + s);
		}
		return s;
    }

    private void readArraySliceHeader(Class clazz, int len)
		throws IOException {

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
		objects.add(next_object, o);
		next_object++;
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

    private native void setFieldDouble(Object ref, String fieldname, double d);
    private native void setFieldLong(Object ref, String fieldname, long l);
    private native void setFieldFloat(Object ref, String fieldname, float f);
    private native void setFieldInt(Object ref, String fieldname, int i);
    private native void setFieldShort(Object ref, String fieldname, short s);
    private native void setFieldChar(Object ref, String fieldname, char c);
    private native void setFieldByte(Object ref, String fieldname, byte b);
    private native void setFieldBoolean(Object ref, String fieldname, boolean b);
    private native void setFieldObject(Object ref, String fieldname, String osig, Object o);
    
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
					setFieldObject(ref, t.serializable_fields[temp].getName(), t.serializable_fields[temp].getClass().getName(), readObject());
				}
				else {
					t.serializable_fields[temp].set(ref, readObject());
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
				// TODO: is this correct? I guess it is, when accessibility
				// is fixed.
				obj = t.clazz.newInstance();
			} catch(Exception e) {
				throw new RuntimeException("Could not instantiate" + e);
			}
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

	public void defaultReadObject() throws IOException, NotActiveException {
		if (current_object == null) {
			throw new NotActiveException("defaultReadObject without a current object");
		}
		Object ref = current_object;
		
		if (ref instanceof ibis.io.Serializable) {
			((ibis.io.Serializable)ref).generated_DefaultReadObject(this, current_level);
		} else if (ref instanceof java.io.Serializable) {
			Class type = ref.getClass();
			AlternativeTypeInfo t = AlternativeTypeInfo.getAlternativeTypeInfo(type);
			
			/*  Find the type info corresponding to the current invocation.
				See the invokeReadObject invocation in alternativeReadObject.
			*/
			while (t.level > current_level) {
				t = t.alternativeSuperInfo;
			}
			alternativeDefaultReadObject(t, ref);
		} else {
			Class type = ref.getClass();
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
