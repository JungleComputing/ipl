package ibis.io;

import java.io.ObjectOutput;
import ibis.ipl.IbisIOException;
import java.io.IOException;


final class ArrayDescriptor {
	int		type;
	Object	        array;
	int		offset;
	int		len;
}


public final class IbisSerializationOutputStream extends SerializationOutputStream implements IbisStreamFlags {
	private ArrayOutputStream out;

	/* Handles for duplicate objects within one stream */
	int next_handle;
	IbisHash references  = new IbisHash();

	private ArrayDescriptor[] array    = new ArrayDescriptor[ARRAY_BUFFER_SIZE];

	private int		array_index;

	private Class stringClass;

	/* Type id management */
	private int next_type = 1;
	private IbisHash types;

	public IbisSerializationOutputStream(ArrayOutputStream out) {
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

	public void reset() throws IbisIOException {
		if (next_handle > CONTROL_HANDLES) { 
			if(DEBUG) {
				System.err.println("OUT(" + this + ") reset: next handle = " + next_handle + ".");
			}
			references.clear();
			next_handle = CONTROL_HANDLES;
			writeHandle(RESET_HANDLE);
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

	public void write(int v) throws IbisIOException {
		if (out.byte_index + 1 == BYTE_BUFFER_SIZE) {
			partial_flush();
		}
		out.byte_buffer[out.byte_index++] = (byte)(0xff & v);
	}

	public void write(byte[] b) throws IbisIOException {
		write(b, 0, b.length);
	}

	public void write(byte[] b, int off, int len) throws IbisIOException {
		writeArray(b, off, len, classByteArray, TYPE_BYTE, false);
	}

	public void writeUTF(String str) throws IbisIOException {
		if(str == null) {
			writeInt(-1);
			return;
		}

		if(DEBUG) {
			System.err.println("write string " + str);
		}
		int len = str.length();

//	    writeInt(len);
//	    writeArray(str.toCharArray(), 0, len);

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

	public void writeBoolean(boolean v) throws IbisIOException {
		if (out.byte_index + 1 == BYTE_BUFFER_SIZE) {
			partial_flush();
		}
		out.byte_buffer[out.byte_index++] = (byte) (v ? 1 : 0);
	}

	public void writeByte(int v) throws IbisIOException {
		if (out.byte_index + 1 == BYTE_BUFFER_SIZE) {
			partial_flush();
		}
		out.byte_buffer[out.byte_index++] = (byte)(0xff & v);
	}

	public void writeShort(int v) throws IbisIOException {
		if (out.short_index + 1 == SHORT_BUFFER_SIZE) {
			partial_flush();
		}
		out.short_buffer[out.short_index++] = (short)(0xffff & v);
	}

	public void writeChar(int v) throws IbisIOException {
		if (out.char_index + 1 == CHAR_BUFFER_SIZE) {
			partial_flush();
		}
		out.char_buffer[out.char_index++] = (char)(0xffff & v);
	}

	public void writeInt(int v) throws IbisIOException {
		if (out.int_index + 1 == INT_BUFFER_SIZE) {
			partial_flush();
		}
		out.int_buffer[out.int_index++] = v;
	}

	private void writeHandle(int v) throws IbisIOException {
		if (out.handle_index + 1 == HANDLE_BUFFER_SIZE) {
			partial_flush();
		}
		out.handle_buffer[out.handle_index++] = v;
	}

	public void writeLong(long v) throws IbisIOException {
		if (out.long_index + 1 == LONG_BUFFER_SIZE) {
			partial_flush();
		}
		out.long_buffer[out.long_index++] = v;
	}

	public void writeFloat(float f) throws IbisIOException {
		if (out.float_index + 1 == FLOAT_BUFFER_SIZE) {
			partial_flush();
		}
		out.float_buffer[out.float_index++] = f;
	}

	public void writeDouble(double d) throws IbisIOException {
		if (out.double_index + 1 == DOUBLE_BUFFER_SIZE) {
			partial_flush();
		}
		out.double_buffer[out.double_index++] = d;
	}

	public void writeBytes(String s) throws IbisIOException {
		throw new RuntimeException("IbisOut.writeBytes not implemented");
	}

	public void writeChars(String s) throws IbisIOException {
		throw new RuntimeException("IbisOut.writeChars not implemented");
	}

	/* Often, the type of the array to be written is known (in an Ibis message for instance).
	   Therefore, provide these methods for efficiency reasons. */
	public void writeArrayBoolean(boolean[] ref) throws IbisIOException {
		writeArray(ref, 0, ref.length, arrayClasses[TYPE_BOOLEAN], TYPE_BOOLEAN, false);
	}

	public void writeArrayByte(byte[] ref) throws IbisIOException {
		writeArray(ref, 0, ref.length, arrayClasses[TYPE_BYTE], TYPE_BYTE, false);
	}

	public void writeArrayShort(short[] ref) throws IbisIOException {
		writeArray(ref, 0, ref.length, arrayClasses[TYPE_SHORT], TYPE_SHORT, false);
	}

	public void writeArrayChar(char[] ref) throws IbisIOException {
		writeArray(ref, 0, ref.length, arrayClasses[TYPE_CHAR], TYPE_CHAR, false);
	}

	public void writeArrayInt(int[] ref) throws IbisIOException {
		writeArray(ref, 0, ref.length, arrayClasses[TYPE_INT], TYPE_INT, false);
	}

	public void writeArrayLong(long[] ref) throws IbisIOException {
		writeArray(ref, 0, ref.length, arrayClasses[TYPE_LONG], TYPE_LONG, false);
	}

	public void writeArrayFloat(float[] ref) throws IbisIOException {
		writeArray(ref, 0, ref.length, arrayClasses[TYPE_FLOAT], TYPE_FLOAT, false);
	}

	public void writeArrayDouble(double[] ref) throws IbisIOException {
		writeArray(ref, 0, ref.length, arrayClasses[TYPE_DOUBLE], TYPE_DOUBLE, false);
	}

	public void writeArrayObject(Object[] ref) throws IbisIOException {
		Class clazz = ref.getClass();
		if (writeArrayHeader(ref, clazz, ref.length, false)) {
			for (int i = 0; i < ref.length; i++) {
				writeObject(ref[i]);
			}
		}
	}

	/* Often, the type of the array to be written is known (in an Ibis message for instance).
	   Therefore, provide these methods for efficiency reasons. */
	public void writeArraySliceBoolean(boolean[] ref, int off, int len) throws IbisIOException {
		writeArray(ref, off, len, arrayClasses[TYPE_BOOLEAN], TYPE_BOOLEAN, false);
	}

	public void writeArraySliceByte(byte[] ref, int off, int len) throws IbisIOException {
		writeArray(ref, off, len, arrayClasses[TYPE_BYTE], TYPE_BYTE, false);
	}

	public void writeArraySliceShort(short[] ref, int off, int len) throws IbisIOException {
		writeArray(ref, off, len, arrayClasses[TYPE_SHORT], TYPE_SHORT, false);
	}

	public void writeArraySliceChar(char[] ref, int off, int len) throws IbisIOException {
		writeArray(ref, off, len, arrayClasses[TYPE_CHAR], TYPE_CHAR, false);
	}

	public void writeArraySliceInt(int[] ref, int off, int len) throws IbisIOException {
		writeArray(ref, off, len, arrayClasses[TYPE_INT], TYPE_INT, false);
	}

	public void writeArraySliceLong(long[] ref, int off, int len) throws IbisIOException {
		writeArray(ref, off, len, arrayClasses[TYPE_LONG], TYPE_LONG, false);
	}

	public void writeArraySliceFloat(float[] ref, int off, int len) throws IbisIOException {
		writeArray(ref, off, len, arrayClasses[TYPE_FLOAT], TYPE_FLOAT, false);
	}

	public void writeArraySliceDouble(double[] ref, int off, int len) throws IbisIOException {
		writeArray(ref, off, len, arrayClasses[TYPE_DOUBLE], TYPE_DOUBLE, false);
	}

	public void writeArraySliceObject(Object[] ref, int off, int len) throws IbisIOException {
		Class clazz = ref.getClass();
		if (writeArrayHeader(ref, clazz, len, false)) {
			for (int i = off; i < off + len; i++) {
				writeObject(ref[i]);
			}
		}
	}

	private boolean writeTypeHandle(Object ref, Class type) throws IbisIOException {
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
		throws IbisIOException {

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

	private void writeArray(Object ref, Class arrayClass) throws IbisIOException {
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

	private void writeType(Class type) throws IbisIOException {
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
		throws IbisIOException {
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

	private int writeKnownObjectHeader(Object ref) throws IbisIOException {

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

	private void alternativeWriteObject(AlternativeTypeInfo t, Object ref) throws IOException, IllegalAccessException {		
		if (t.superSerializable) { 
			alternativeWriteObject(t.alternativeSuperInfo, ref);
		} 

		if(DEBUG) {
			System.err.println("Using alternative writeObject for " + ref.getClass().getName());
		}

		int temp = 0;
		int i;
		for (i=0;i<t.double_count;i++)    writeDouble(t.serializable_fields[temp++].getDouble(ref));
		for (i=0;i<t.long_count;i++)      writeLong(t.serializable_fields[temp++].getLong(ref));
		for (i=0;i<t.float_count;i++)     writeFloat(t.serializable_fields[temp++].getFloat(ref));
		for (i=0;i<t.int_count;i++)       writeInt(t.serializable_fields[temp++].getInt(ref));
		for (i=0;i<t.short_count;i++)     writeShort(t.serializable_fields[temp++].getShort(ref));
		for (i=0;i<t.char_count;i++)      writeChar(t.serializable_fields[temp++].getChar(ref));
		for (i=0;i<t.boolean_count;i++)   writeBoolean(t.serializable_fields[temp++].getBoolean(ref));
		for (i=0;i<t.reference_count;i++) writeObject(t.serializable_fields[temp++].get(ref));
	} 

	public void doWriteObject(Object ref) throws IbisIOException {

		/*
		 * ref < 0:	type
		 * ref = 0:	null ptr
		 * ref > 0:	handle
		 */

		if (ref == null) {
			writeHandle(NUL_HANDLE);
		} else {
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
					} else if (ref instanceof java.io.Serializable) {
						try { 
							AlternativeTypeInfo t = AlternativeTypeInfo.getAlternativeTypeInfo(type);
							alternativeWriteObject(t, ref);
						} catch (IllegalAccessException e) { 
							throw new RuntimeException("Serializable failed for : " + type.toString());
						} catch (IOException e2) { 
							throw new IbisIOException("Serializable failed for : " + type.toString(), e2);
						}
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
	}

	private void partial_flush() throws IbisIOException {
		out.flushBuffers();

		//    Retain the order in which the arrays were pushed. This costs a
		//    cast at send/receive.
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

	public void flush() throws IbisIOException { 
		partial_flush();
		out.flush();
	} 

	public void close() throws IbisIOException {
		flush();
		out.close();
	}
}
