/*
 * Created on 23.01.2005
 */
package ibis.mpj;

import java.io.*;


/**
 * Object which will be sent and received by the basic point-to-point modes.
 */
public class MPJObject implements Serializable {
	private final boolean DEBUG = false;
	private static final int HEADER_SIZE = 5;
	
	protected Object buffer;
	protected int[] desc = new int[HEADER_SIZE]; 
	
	
	MPJObject() {
		
	}
	
	MPJObject(int tag, int contextId, boolean buffered, int baseType, int count) {
		
		desc[0] = tag;
		desc[1] = contextId;
		if (!buffered) {
			desc[2] = 0;
		}
		else {
			desc[2] = 1;
		}
		
		desc[3] = baseType;
		desc[4] = count;
	}

	protected int[] getDesc() {
		return this.desc;
	}
	
	protected int getTag () {
		
		return this.desc[0];
	}

	
	protected void setTag(int tag) {
		this.desc[0] = tag;
	}

	
	protected int getContextId() {
		return (this.desc[1]);
	}

	protected void setContextId(int contextId) {
		this.desc[1] = contextId;
	}


	protected boolean isBuffered() {
		if (this.desc[2] == 1) return(true);
		else return(false);
	}
	
	protected void setBuffered(boolean buffered) {
		if (buffered == true) this.desc[2] = 1;
		else this.desc[2] = 0;
	}

	
	protected void setBaseDatatype(int baseType) {
		this.desc[3] = baseType;
	}
	
	protected int getBaseDatatype() {
		return(this.desc[3]);
	}
	
	
	protected void setNumberOfElements(int count) {
		this.desc[4] =  count;
	}
	
	protected int getNumberOfElements() {
		return(this.desc[4]);
	}
	
	
	protected Object getObjectData() {
		return buffer;
	}
	
	protected void setObjectData(Object buf) {
		this.buffer = buf;
	}
	
	
	protected void initBuffer() {
		int type = this.getBaseDatatype();
		int count = this.getNumberOfElements();
		
		if (type == Datatype.BASE_TYPE_BYTE) {
			this.buffer = new byte[count];
		}
		else if (type == Datatype.BASE_TYPE_CHAR) {
			this.buffer = new char[count];
		}
		else if (type == Datatype.BASE_TYPE_SHORT) {
			this.buffer = new short[count];
		}
		else if (type == Datatype.BASE_TYPE_BOOLEAN) {
			this.buffer = new boolean[count];
		}
		else if (type == Datatype.BASE_TYPE_INT) {
			this.buffer = new int[count];
		}
		else if (type == Datatype.BASE_TYPE_LONG ) {
			this.buffer = new long[count];
		}
		else if (type == Datatype.BASE_TYPE_FLOAT) {
			this.buffer = new float[count];
		}
		else if (type == Datatype.BASE_TYPE_DOUBLE) {
			this.buffer = new double[count];
		}
		else {
			this.buffer = new Object[count];
		}
		
	}
	
	
	
	protected int cast2ByteArray(Object destBuf, int offset, int count) {
		
		if ((this.buffer == null) || (destBuf == null) || (count <= 0) || (offset < 0) || 
			(!(destBuf instanceof byte[])) || (!(this.buffer instanceof byte[]))) {
			return(0);
		}
		else {
			if ((offset + count) > ((byte[])destBuf).length) {
				count = ((byte[])destBuf).length - offset;
			}
			if (count > ((byte[])this.buffer).length) {
				count = ((byte[])this.buffer).length - offset;
			}
			System.arraycopy(this.buffer, 0, destBuf, offset, count);
			return(count);
		}
		
	}
	
	protected int cast2CharArray(Object destBuf, int offset, int count) {
		
		if ((this.buffer == null) || (destBuf == null) || (count <= 0) || (offset < 0) || 
			(!(destBuf instanceof char[])) || (!(this.buffer instanceof char[]))) {
			return(0);
		}
		else {
			if ((offset + count) > ((char[])destBuf).length) {
				count = ((char[])destBuf).length - offset;
			}
			if (count > ((char[])this.buffer).length) {
				count = ((char[])this.buffer).length - offset;
			}
			System.arraycopy(this.buffer, 0, destBuf, offset, count);
			return(count);
		}
	}
	
	protected int cast2ShortArray(Object destBuf, int offset, int count) {
		if ((this.buffer == null) || (destBuf == null) || (count <= 0) || (offset < 0) || 
			(!(destBuf instanceof short[])) || (!(this.buffer instanceof short[]))) {
			return(0);
		}
		else {
			if ((offset + count) > ((short[])destBuf).length) {
				count = ((short[])destBuf).length - offset;
			}
			if (count > ((short[])this.buffer).length) {
				count = ((short[])this.buffer).length - offset;
			}
			System.arraycopy(this.buffer, 0, destBuf, offset, count);
			return(count);
		}
	}
	
	protected int cast2BooleanArray(Object destBuf, int offset, int count) {
		if ((this.buffer == null) || (destBuf == null) || (count <= 0) || (offset < 0) || 
			(!(destBuf instanceof boolean[])) || (!(this.buffer instanceof boolean[]))) {
				return(0);
		}
		else {
			if ((offset + count) > ((boolean[])destBuf).length) {
				count = ((boolean[])destBuf).length - offset;
			}
			if (count > ((boolean[])this.buffer).length) {
				count = ((boolean[])this.buffer).length - offset;
			}
			
			System.arraycopy(this.buffer, 0, destBuf, offset, count);
			return(count);
		}
	}
	
	
	
	protected int cast2IntArray(Object destBuf, int offset, int count) {
		
		if ((this.buffer == null) || (destBuf == null) || (count <= 0) || (offset < 0) || 
			(!(destBuf instanceof int[])) || (!(this.buffer instanceof int[]))) {
			return(0);
		}
		else {
			if ((offset + count) > ((int[])destBuf).length) {
				count = ((int[])destBuf).length - offset;
			}
			if (count > ((int[])this.buffer).length) {
				count = ((int[])this.buffer).length - offset;
			}
			if (DEBUG) {
				System.out.println("offset: " + offset);
				System.out.println("count: " + count);
				System.out.println("this.buffer.length: " + ((int[])this.buffer).length);
				System.out.println("destBuf.length: " + ((int[])destBuf).length);
			}
			System.arraycopy(this.buffer, 0, destBuf, offset, count);
			return(count);
		}
		
	}

	
	protected int cast2LongArray(Object destBuf, int offset, int count) {
		if ((this.buffer == null) || (destBuf == null) || (count <= 0) || (offset < 0) || 
			(!(destBuf instanceof long[])) || (!(this.buffer instanceof long[]))) {
			return(0);
		}
		else {
			if ((offset + count) > ((long[])destBuf).length) {
				count = ((long[])destBuf).length - offset;
			}
			if (count > ((long[])this.buffer).length) {
				count = ((long[])this.buffer).length - offset;
			}
			System.arraycopy(this.buffer, 0, destBuf, offset, count);
			return(count);
		}
		
		
	}
	
	protected int cast2FloatArray(Object destBuf, int offset, int count) {
		if ((this.buffer == null) || (destBuf == null) || (count <= 0) || (offset < 0) || 
			(!(destBuf instanceof float[])) || (!(this.buffer instanceof float[]))) {
			return(0);
		}
		else {
			if ((offset + count) > ((float[])destBuf).length) {
				count = ((float[])destBuf).length - offset;
			}
			if ((offset + count) > ((float[])this.buffer).length) {
				count = ((float[])this.buffer).length - offset;
			}
			System.arraycopy(this.buffer, 0, destBuf, offset, count);
			return(count);
		}
	
	}

		
	protected int cast2DoubleArray(Object destBuf, int offset, int count) {
		if ((this.buffer == null) || (destBuf == null) || (count <= 0) || (offset < 0) || 
			(!(destBuf instanceof double[])) || (!(this.buffer instanceof double[]))) {
			
			return(0);
		}
		else {
			System.out.println("c: " + count);
			System.out.println("o: " + offset);
			if ((offset + count) > ((double[])destBuf).length) {
				count = ((double[])destBuf).length - offset;
			}
			
			if ((offset + count) > ((double[])this.buffer).length) {
				count = ((double[])this.buffer).length - offset;
			}
			System.out.println("c: " + count);
			System.out.println("o: " + offset);
			System.arraycopy(this.buffer, 0, destBuf, offset, count);
			
			return(count);
		}
	
	}
	
	protected int cast2ObjectArray(Object destBuf, int offset, int count) {
		if ((this.buffer == null) || (destBuf == null) || (count <= 0) || (offset < 0) || 
			(!(destBuf instanceof Object[])) || (!(this.buffer instanceof byte[]))) {
			return(0);
		}
		else {
			try {
				ByteArrayInputStream byteInStream = new ByteArrayInputStream((byte [])this.buffer);
				ObjectInputStream objectInStream = new ObjectInputStream(byteInStream);

				for (int i = offset; i < count+offset; i++) {
					try {
						((Object[])destBuf)[i] = (Object)objectInStream.readObject();
					}
					catch(EOFException e) {
						count--;
						if (((Object[])destBuf)[i] == null) {
							
							Class cl = ((Object[])destBuf)[offset].getClass();
							((Object[])destBuf)[i] = cl.newInstance();
						}
					}
				}

				return(count);	
			} 
			catch (Exception e) {
				System.err.println(e.getMessage());
				e.printStackTrace();
				return(0);
			}
			
		}
	}
	
	
	
	
	
}
