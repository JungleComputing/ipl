// TODO: cycle check: schrijft array, leest als slice.
// Als het een duplicate was, maak dan een kopie en retourneer die...
package ibis.io;

import java.io.ObjectInput;
import ibis.ipl.IbisIOException;
import java.io.IOException;

public final class IbisSerializationInputStream extends SerializationInputStream
	implements ObjectInput, IbisStreamFlags {

	private short[]	indices        = new short[PRIMITIVE_TYPES + 1];
	/* private */ public byte[]	byte_buffer    = new byte[BYTE_BUFFER_SIZE];
	/* private */ public char[]	char_buffer    = new char[CHAR_BUFFER_SIZE];
	/* private */ public short[]	short_buffer   = new short[SHORT_BUFFER_SIZE];
	/* private */ public int[]	int_buffer     = new int[INT_BUFFER_SIZE];
	/* private */ public long[]	long_buffer    = new long[LONG_BUFFER_SIZE];
	/* private */ public float[]	float_buffer   = new float[FLOAT_BUFFER_SIZE];
	/* private */ public double[]	double_buffer  = new double[DOUBLE_BUFFER_SIZE];

	/* private */ public int[]	handle_buffer  = new int[HANDLE_BUFFER_SIZE];

	/* private */ public int		byte_index;
	/* private */ public int		char_index;
	/* private */ public int		short_index;
	/* private */ public int		int_index;
	/* private */ public int		long_index;
	/* private */ public int		float_index;
	/* private */ public int		double_index;
	/* private */ public int		handle_index;

	/* private */ public int		max_byte_index;
	/* private */ public int		max_char_index;
	/* private */ public int		max_short_index;
	/* private */ public int		max_int_index;
	/* private */ public int		max_long_index;
	/* private */ public int		max_float_index;
	/* private */ public int		max_double_index;
	/* private */ public int		max_handle_index;

	class TypeInfo { 
		Class clazz;		
		boolean isArray;
		boolean isString;

		// for ibis.io.Serializable    
		Generator gen;

		// for java.io.Serializable
		AlternativeTypeInfo altInfo;

		TypeInfo(Class clazz, boolean isArray, boolean isString, Generator gen) { 
			this.clazz = clazz;
			this.isArray = isArray;
			this.isString = isString;
			this.gen = gen;

			if (gen == null) { 
				altInfo = new AlternativeTypeInfo(clazz);
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


	public IbisSerializationInputStream(ArrayInputStream in) {
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

	private void reset() {
		if(DEBUG) {
			System.err.println("IN(" + this + ") reset: next handle = " + next_object + "."); 
		}
		objects.clear();
		next_object = CONTROL_HANDLES;
	}


	public void statistics() {
		System.err.println("IbisInput:");
		System.err.println("bytes read = ");
		// objects.statistics();
	}


	public void print() {
		System.err.println("Uh oh -- IbisSerializationInputStream.print unimplemented");
	}


	public int bytesRead() {
		System.err.println("bytesRead not (yet) implemented");
		return 0;
	}

	public void resetBytesRead() {
		System.err.println("resetBytesRead not (yet) implemented");
	}

	/* This is the data output / object output part */

	public int read() throws IbisIOException {
		if (byte_index == max_byte_index) {
			receive();
		}
		return byte_buffer[byte_index++];
	}

	public int read(byte[] b) throws IbisIOException {
		return read(b, 0, b.length);
	}

	public int read(byte[] b, int off, int len) throws IbisIOException {
		readArraySliceByte(b, off, len);
		return len;
	}

	public long skip(long n) throws IbisIOException {
		throw new IbisIOException("skip not meaningful in a typed input stream");
	}

	public int skipBytes(int n) throws IbisIOException {
		throw new IbisIOException("skipBytes not meaningful in a typed input stream");
	}


	public int available() throws IbisIOException {
//	throw new IbisIOException("available not meaningful in a typed input stream");

		// @@@ NOTE: this is not right. There are also some buffered arrays...

		return (max_byte_index - byte_index) +
			(max_char_index - char_index) +
			(max_short_index - short_index) +
			(max_int_index - int_index) +
			(max_long_index - long_index) +
			(max_float_index - float_index) +
			(max_double_index - double_index) +
			in.available();
	}

	public void readFully(byte[] b) throws IbisIOException {
		readFully(b, 0, b.length);
	}


	public void readFully(byte[] b, int off, int len) throws IbisIOException {
		read(b, off, len);
	}


	public boolean readBoolean() throws IbisIOException {
		if (byte_index == max_byte_index) {
			receive();
		}
		return (byte_buffer[byte_index++] == 1);
	}


	public byte readByte() throws IbisIOException {
		if (byte_index == max_byte_index) {
			receive();
		}
		return byte_buffer[byte_index++];
	}


	public int readUnsignedByte() throws IbisIOException {
		if (byte_index == max_byte_index) {
			receive();
		}
		int i = byte_buffer[byte_index++];
		if (i < 0) {
			i += 256;
		}
		return i;
	}


	public short readShort() throws IbisIOException {
		if (short_index == max_short_index) {
			receive();
		}
		return short_buffer[short_index++];
	}


	public int readUnsignedShort() throws IbisIOException {
		if (short_index == max_short_index) {
			receive();
		}
		int i = short_buffer[short_index++];
		if (i < 0) {
			i += 65536;
		}
		return i;
	}


	public char readChar() throws IbisIOException {
		if (char_index == max_char_index) {
			receive();
		}
		return char_buffer[char_index++];
	}


	public int readInt() throws IbisIOException {
		if (int_index == max_int_index) {
			receive();
		}
		return int_buffer[int_index++];
	}

	public int readHandle() throws IbisIOException {
		if (handle_index == max_handle_index) {
			receive();
		}
		if(DEBUG) {
			System.err.println("read handle [" + handle_index + "] = " + Integer.toHexString(handle_buffer[handle_index]));
		}

		return handle_buffer[handle_index++];
	}

	public long readLong() throws IbisIOException {
		if (long_index == max_long_index) {
			receive();
		}
		return long_buffer[long_index++];
	}


	public float readFloat() throws IbisIOException {
		if (float_index == max_float_index) {
			receive();
		}
		return float_buffer[float_index++];
	}


	public double readDouble() throws IbisIOException {
		if (double_index == max_double_index) {
			receive();
		}
		return double_buffer[double_index++];
	}


	public String readUTF() throws IbisIOException {

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
					throw new IbisIOException("UTF Data Format Exception"); // UTFDataFormatException();
				}
				c[len++] = (char)(((b[i] & 0x1f) << 6) | (b[i] & 0x3f));
				i++;
			} else if ((b[i] & ~0x0f) == 0xe0) {
				if (i + 2 >= bn ||
				    (b[i + 1] & ~0x3f) != 0x80 ||
				    (b[i + 2] & ~0x3f) != 0x80) {
					throw new IbisIOException("UTF Data Format Exception"); // UTFDataFormatException();
				}
				c[len++] = (char)(((b[i] & 0x0f) << 12) | ((b[i+1] & 0x3f) << 6) | b[i+2] & 0x3f);
			} else {
				throw new IbisIOException("UTF Data Format Exception"); // UTFDataFormatException();
			}
		}

		String s = new String(c, 0, len);

		if(DEBUG) {
			System.err.println("read string "  + s);
		}
		return s;
	}


	private void readArraySliceHeader(Class clazz, int len) throws IbisIOException {
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
			throw new IbisIOException("Array slice header but I receive a HANDLE!");
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


	public String readBytes() throws IbisIOException {
		throw new RuntimeException("IbisOut.readBytes not implemented");
	}


	public String readChars() throws IbisIOException {
		throw new RuntimeException("IbisOut.readChars not implemented");
	}


	public String readLine() throws IbisIOException {
		throw new IbisIOException("readLine not meaningful in typed stream");
	}


	public void readArraySliceBoolean(boolean[] ref, int off, int len)
		throws IbisIOException {
		readArraySliceHeader(classBooleanArray, len);
		in.readArray(ref, off, len);
	}


	public void readArraySliceByte(byte[] ref, int off, int len)
		throws IbisIOException {
		readArraySliceHeader(classByteArray, len);
		in.readArray(ref, off, len);
	}


	public void readArraySliceChar(char[] ref, int off, int len)
		throws IbisIOException {
		readArraySliceHeader(classCharArray, len);
		in.readArray(ref, off, len);
	}


	public void readArraySliceShort(short[] ref, int off, int len)
		throws IbisIOException {
		readArraySliceHeader(classShortArray, len);
		in.readArray(ref, off, len);
	}


	public void readArraySliceInt(int[] ref, int off, int len)
		throws IbisIOException {
		readArraySliceHeader(classIntArray, len);
		in.readArray(ref, off, len);
	}


	public void readArraySliceLong(long[] ref, int off, int len)
		throws IbisIOException {
		readArraySliceHeader(classLongArray, len);
		in.readArray(ref, off, len);
	}


	public void readArraySliceFloat(float[] ref, int off, int len)
		throws IbisIOException {
		readArraySliceHeader(classFloatArray, len);
		in.readArray(ref, off, len);
	}


	public void readArraySliceDouble(double[] ref, int off, int len)
		throws IbisIOException {
		readArraySliceHeader(classDoubleArray, len);
		in.readArray(ref, off, len);
	}


	public void readArraySliceObject(Object[] ref, int off, int len)
		throws IbisIOException {
		readArraySliceHeader(ref.getClass(), len);
		for (int i = off; i < off + len; i++) {
			ref[i] = readObject();
		}
	}

	public void readArrayBoolean(boolean[] destination) throws IbisIOException {
		readArraySliceBoolean(destination, 0, destination.length);
	}

	public void readArrayByte(byte[] destination) throws IbisIOException {
		readArraySliceByte(destination, 0, destination.length);
	}

	public void readArrayShort(short[] destination) throws IbisIOException {
		readArraySliceShort(destination, 0, destination.length);
	}

	public void readArrayChar(char[] destination) throws IbisIOException {
		readArraySliceChar(destination, 0, destination.length);
	}

	public void readArrayInt(int[] destination) throws IbisIOException {
		readArraySliceInt(destination, 0, destination.length);
	}

	public void readArrayLong(long[] destination) throws IbisIOException {
		readArraySliceLong(destination, 0, destination.length);
	}

	public void readArrayFloat(float[] destination) throws IbisIOException {
		readArraySliceFloat(destination, 0, destination.length);
	}

	public void readArrayDouble(double[] destination) throws IbisIOException {
		readArraySliceDouble(destination, 0, destination.length);
	}

	public void readArrayObject(Object[] destination) throws IbisIOException {
		readArraySliceObject(destination, 0, destination.length);
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


	public int readKnownTypeHeader() throws IbisIOException {
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


	Object readArray(Class arrayClass, int type) throws IbisIOException {
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

								 
	public TypeInfo readType(int type)
		throws IbisIOException {
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
			    throw new IbisIOException("class " + typeName + " not found");
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
	
	private void alternativeReadObject(AlternativeTypeInfo ati, Object ref) throws IOException, IllegalAccessException {
		
		if (ati.superSerializable) { 
			alternativeReadObject(ati.alternativeSuperInfo, ref);
		} 

		System.err.println("Using alternative readObject for " + ref.getClass().getName());

		int temp = 0;
		for (int i=0;i<ati.double_count;i++)    ati.serializable_fields[temp++].setDouble(ref, readDouble());
		for (int i=0;i<ati.long_count;i++)      ati.serializable_fields[temp++].setLong(ref, readLong());
		for (int i=0;i<ati.float_count;i++)     ati.serializable_fields[temp++].setFloat(ref, readFloat());
		for (int i=0;i<ati.int_count;i++)       ati.serializable_fields[temp++].setInt(ref, readInt());
		for (int i=0;i<ati.short_count;i++)     ati.serializable_fields[temp++].setShort(ref, readShort());
		for (int i=0;i<ati.char_count;i++)      ati.serializable_fields[temp++].setChar(ref, readChar());
		for (int i=0;i<ati.boolean_count;i++)   ati.serializable_fields[temp++].setBoolean(ref, readBoolean());
		for (int i=0;i<ati.reference_count;i++) ati.serializable_fields[temp++].set(ref, readObject());
	} 

/*
	private void alternativeReadObject(TypeInfo t, Object ref) throws IOException, IllegalAccessException {

		System.err.println("Using alternativeReadObject for " + t.clazz.toString());
		alternativeReadObject(t.altInfo, ref);
	} 
*/

	private native Object createUninitializedObject(Class type);

	public Object readObject()
		throws IbisIOException {

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
		} else {
			// this is for java.io.Serializable
			try {
				// obj = t.clazz.newInstance(); // this is not correct --> need native call ??? !!!
				obj = createUninitializedObject(t.clazz);
				addObjectToCycleCheck(obj);
				alternativeReadObject(t.altInfo, obj);
			} catch (Exception e) {
				throw new RuntimeException("Couldn't deserialize or create object " + e);
			}
		}

		return obj;
	}

	private void receive() throws IbisIOException {
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

	public void close() throws IbisIOException {
	}
}
