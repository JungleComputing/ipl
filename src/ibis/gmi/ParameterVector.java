package ibis.group;

import ibis.ipl.WriteMessage;
import ibis.ipl.IbisException;

// This class is used as a wrapper for a marhalstruct. Needed for the implementation of a "personalized group invocation".

public class ParameterVector { 
       	
	private ParameterVector next;
	/*
	private static Class boolean_class;
	private static Class char_class;
	private static Class byte_class;
	private static Class short_class;
	private static Class int_class;
	private static Class long_class;
	private static Class float_class;
	private static Class double_class;
	*/
	private Class [] parameters;
	private int next_param;
	private WriteMessage message;
	
	static {
		/*
		boolean_class = getPrimitiveClass("boolean");
		char_class    = getPrimitiveClass("char");
		byte_class    = getPrimitiveClass("byte");
		short_class   = getPrimitiveClass("short");
		int_class     = getPrimitiveClass("int");
		long_class    = getPrimitiveClass("long");
		float_class   = getPrimitiveClass("float");
		double_class  = getPrimitiveClass("double");
		*/
	} 

	public ParameterVector(Class [] parameters) { 
		reInit(parameters);
	} 

	private void reInit(Class [] parameters) { 
		this.parameters = parameters; 
		next_param = parameters.length;
	} 

	public void reset(WriteMessage message) throws RuntimeException {

		if (next_param < parameters.length) throw new RuntimeException("ParamVector.reset(WriteMessage) paramvector not full!!");
		this.message = message;
		next_param = 0;
	}

	public void add(boolean value) throws RuntimeException { 
		if (next_param == parameters.length) throw new RuntimeException("ParamVector.add((char) " + value + ") : Too many parameters"); 
		if (parameters[next_param] != boolean.class) throw new RuntimeException("ParamVector.add((char) " + value + ") : Wrong parameter type");

		try {
			message.writeBoolean(value);
			next_param++;
		} catch (IbisException e) { 
			throw new RuntimeException("ParamVector.add((boolean) " + value + ") : Failed " + e.getMessage()); 
		} 
	} 

	public void add(char value) throws RuntimeException { 
		if (next_param == parameters.length) throw new RuntimeException("ParamVector.add((char) " + value + ") : Too many parameters"); 
		if (parameters[next_param] != char.class) throw new RuntimeException("ParamVector.add((char) " + value + ") : Wrong parameter type");

		try {
			message.writeChar(value);
			next_param++;
		} catch (IbisException e) { 
			throw new RuntimeException("ParamVector.add((char) " + value + ") : Failed " + e.getMessage()); 
		} 
	} 
	
	public void add(byte value) throws RuntimeException { 
		if (next_param == parameters.length) throw new RuntimeException("ParamVector.add((byte) " + value + ") : Too many parameters"); 
		if (parameters[next_param] != byte.class) throw new RuntimeException("ParamVector.add((byte) " + value + ") : Wrong parameter type");

		try {
			message.writeByte(value);
			next_param++;
		} catch (IbisException e) { 
			throw new RuntimeException("ParamVector.add((byte) " + value + ") : Failed " + e.getMessage()); 
		} 
	}  
	
	public void add(short value) throws RuntimeException {
		if (next_param == parameters.length) throw new RuntimeException("ParamVector.add((short) " + value + ") : Too many parameters"); 
		if (parameters[next_param] != short.class) throw new RuntimeException("ParamVector.add((short) " + value + ") : Wrong parameter type");

		try {
			message.writeShort(value);
			next_param++;
		} catch (IbisException e) { 
			throw new RuntimeException("ParamVector.add((short) " + value + ") : Failed " + e.getMessage()); 
		} 
} 
	
	public void add(int value) throws RuntimeException { 
		if (next_param == parameters.length) throw new RuntimeException("ParamVector.add((int) " + value + ") : Too many parameters"); 
		if (parameters[next_param] != int.class) throw new RuntimeException("ParamVector.add((int) " + value + ") : Wrong parameter type");
		
		try {
			message.writeInt(value);
			next_param++;
		} catch (IbisException e) { 
			throw new RuntimeException("ParamVector.add((int) " + value + ") : Failed " + e.getMessage()); 
		} 
	} 
	
	public void add(long value) throws RuntimeException {
		if (next_param == parameters.length) throw new RuntimeException("ParamVector.add((long) " + value + ") : Too many parameters"); 
		if (parameters[next_param] != long.class) throw new RuntimeException("ParamVector.add((long) " + value + ") : Wrong parameter type");

		try { 
			message.writeLong(value);
			next_param++;
		} catch (IbisException e) { 
			throw new RuntimeException("ParamVector.add((long) " + value + ") : Failed " + e.getMessage()); 
		} 
	} 
	
	public void add(float value) throws RuntimeException { 
		if (next_param == parameters.length) throw new RuntimeException("ParamVector.add((float) " + value + ") : Too many parameters"); 
		if (parameters[next_param] != float.class) throw new RuntimeException("ParamVector.add((float) " + value + ") : Wrong parameter type");

		try {
			message.writeFloat(value);
			next_param++;
		} catch (IbisException e) { 
			throw new RuntimeException("ParamVector.add((float) " + value + ") : Failed " + e.getMessage()); 
		} 
	} 
	
	public void add(double value) throws RuntimeException { 
		if (next_param == parameters.length) throw new RuntimeException("ParamVector.add((double) " + value + ") : Too many parameters"); 
		if (parameters[next_param] != double.class) throw new RuntimeException("ParamVector.add((double) " + value + ") : Wrong parameter type");
		
		try {
			message.writeDouble(value);
			next_param++;
		} catch (IbisException e) { 
			throw new RuntimeException("ParamVector.add((double) " + value + ") : Failed " + e.getMessage()); 
		} 
	} 
	
	public void add(Object value) throws RuntimeException {
		if (next_param == parameters.length) throw new RuntimeException("ParamVector.add((Object) " + value + ") : Too many parameters"); 
		if (!parameters[next_param].isAssignableFrom(value.getClass())) throw new RuntimeException("ParamVector.add((Object) " + value + ") : Wrong parameter type");

		try {
			message.writeObject(value);
			next_param++;
		} catch (IbisException e) { 
			throw new RuntimeException("ParamVector.add((Object) " + value + ") : Failed " + e.getMessage()); 
		} 
	}        

	/* -------------------- This is a little parametervector cache ------------------ */

	private static Object cache_lock = new Object();
	private static ParameterVector cache = null;
	private static int num_cached = 0;
	private static int CACHE_SIZE = 64;
	
	public static ParameterVector createParameterVector(Class [] parameter) { 

		ParameterVector temp;

		synchronized (cache_lock) {

			if (num_cached > 0) { 
				num_cached--;
				temp = cache;
				cache = temp.next;
				
				temp.reInit(parameter);
			} else { 
				temp = new ParameterVector(parameter);
			}
		}

		return temp;
	}

	public static void deleteParameterVector(ParameterVector v) { 

		synchronized (cache_lock) {

			if (num_cached < CACHE_SIZE) { 
				num_cached++;
				v.next = cache;
				cache = v;				
			}
		}
	}
	       		
	public static void resetParameterVector(ParameterVector v, WriteMessage m) { 
		v.reset(m);
	} 
}


