package ibis.gmi;

import ibis.ipl.ReadMessage;
import ibis.ipl.WriteMessage;

import java.io.IOException;

/**
 * The {@link ParameterVector} class provides a base class for generated
 * parameter vector classes, specific for a group method.
 */
public abstract class ParameterVector { 

    /** Set to true when the parameter vector is completely initialized. */
    public boolean done = false;

    /** Keeps track of the number of initialized vector elements. */
    protected int set_count = 0;

    /** Array of booleans, keeping track of which elements have been initialized. */
    protected boolean [] set;

    /**
     * Resets the parameter vector to uninitialized. Should be redefined by
     * generated parameter vector classes, to reset its additional fields.
     */
    protected void reset() {
	set_count = 0;
	done = false;
	if (set != null) {
	    for (int i = 0; i < set.length; i++) set[i] = false;
	}
    }

    /**
     * Reads a parameter vector from a message.
     *
     * @param r the message from which the parameters are to be read
     * @return The resulting parameter vector.
     */
    public abstract ParameterVector readParameters(ReadMessage r) throws IOException;

    /**
     * Writes this parameter vector to a message.
     *
     * @param w the message to which the parameters are to be appended
     */
    public abstract void writeParameters(WriteMessage w) throws IOException;

    /**
     * Creates a new parameter vector of the same type.
     *
     * @return the new parameter vector.
     */
    public ParameterVector getVector() {
	throw new RuntimeException("EEK: ParameterVector");
    }

    /**
     * Places a boolean value in parameter number "num".
     *
     * @param num the parameter number, ranging from 0 .. #parameters-1
     * @param value the value to be stored
     */
    public void write(int num, boolean value) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }

    /**
     * See {@link #write(int,boolean)}, but for a byte.
     */
    public void write(int num, byte value) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }

    /**
     * See {@link #write(int,boolean)}, but for a short.
     */
    public void write(int num, short value) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }

    /**
     * See {@link #write(int,boolean)}, but for a char.
     */
    public void write(int num, char value) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }

    /**
     * See {@link #write(int,boolean)}, but for an int.
     */
    public void write(int num, int value) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }

    /**
     * See {@link #write(int,boolean)}, but for a long.
     */
    public void write(int num, long value) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }

    /**
     * See {@link #write(int,boolean)}, but for a float.
     */
    public void write(int num, float value) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }

    /**
     * See {@link #write(int,boolean)}, but for a double.
     */
    public void write(int num, double value) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }

    /**
     * See {@link #write(int,boolean)}, but for an Object.
     */
    public void write(int num, Object value) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }

    /**
     * Places a notion in the parameter vector that parameter "num" is
     * a sub-array of array "value", with offset "offset" and length "size".
     * No copying takes place.
     *
     * @param num The parameter number, ranging from 0 .. #parameters-1.
     * @param offset Offset in the array where the sub-array begins.
     * @param size Length (number of elements) in the sub-array.
     * @param value The array of which a sub-array is the parameter.
     */
    public void writeSubArray(int num, int offset, int size, boolean[] value) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }

    /**
     * See {@link #writeSubArray(int,int,int,boolean[])}, but for byte arrays.
     */
    public void writeSubArray(int num, int offset, int size, byte[] value) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }

    /**
     * See {@link #writeSubArray(int,int,int,boolean[])}, but for short arrays.
     */
    public void writeSubArray(int num, int offset, int size, short[] value) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }

    /**
     * See {@link #writeSubArray(int,int,int,boolean[])}, but for char arrays.
     */
    public void writeSubArray(int num, int offset, int size, char[] value) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }

    /**
     * See {@link #writeSubArray(int,int,int,boolean[])}, but for int arrays.
     */
    public void writeSubArray(int num, int offset, int size, int[] value) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }

    /**
     * See {@link #writeSubArray(int,int,int,boolean[])}, but for long arrays.
     */
    public void writeSubArray(int num, int offset, int size, long[] value) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }

    /**
     * See {@link #writeSubArray(int,int,int,boolean[])}, but for float arrays.
     */
    public void writeSubArray(int num, int offset, int size, float[] value) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }

    /**
     * See {@link #writeSubArray(int,int,int,boolean[])}, but for double arrays.
     */
    public void writeSubArray(int num, int offset, int size, double[] value) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }

    /**
     * See {@link #writeSubArray(int,int,int,boolean[])}, but for Object arrays.
     */
    public void writeSubArray(int num, int offset, int size, Object[] value) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }

    /**
     * Get a boolean from position "num" in the parameter vector.
     *
     * @param num The position number in the parameter vector.
     * @return The boolean requested.
     */
    public boolean readBoolean(int num) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }

    /**
     * See {@link #readBoolean(int)}, but for a byte.
     */
    public byte readByte(int num) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }

    /**
     * See {@link #readBoolean(int)}, but for a short.
     */
    public short readShort(int num) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }

    /**
     * See {@link #readBoolean(int)}, but for a char.
     */
    public char readChar(int num) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }

    /**
     * See {@link #readBoolean(int)}, but for an int.
     */
    public int readInt(int num) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }

    /**
     * See {@link #readBoolean(int)}, but for a long.
     */
    public long readLong(int num) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }

    /**
     * See {@link #readBoolean(int)}, but for a float.
     */
    public float readFloat(int num) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }

    /**
     * See {@link #readBoolean(int)}, but for a double.
     */
    public double readDouble(int num) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }

    /**
     * See {@link #readBoolean(int)}, but for an Object.
     */
    public Object readObject(int num) { 
	throw new RuntimeException("EEK: ParameterVector"); 
    }
}
