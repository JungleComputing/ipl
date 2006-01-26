/* $Id$ */

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

    /**
     * Array of booleans, keeping track of which elements have been
     * initialized. 
     */
    protected boolean[] set;

    /** Disable construction from outside. */
    protected ParameterVector() {
    }

    /**
     * Resets the parameter vector to uninitialized. Should be redefined by
     * generated parameter vector classes, to reset its additional fields.
     */
    protected void reset() {
        set_count = 0;
        done = false;
        if (set != null) {
            for (int i = 0; i < set.length; i++)
                set[i] = false;
        }
    }

    /**
     * Reads a parameter vector from a message.
     *
     * @param r the message from which the parameters are to be read
     * @return The resulting parameter vector.
     * @throws IOException some network error occurred
     */
    public abstract ParameterVector readParameters(ReadMessage r)
            throws IOException;

    /**
     * Writes this parameter vector to a message.
     *
     * @param w the message to which the parameters are to be appended
     * @throws IOException some network error occurred
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
     * Places a byte value in parameter number "num".
     *
     * @param num the parameter number, ranging from 0 .. #parameters-1
     * @param value the value to be stored
     */
    public void write(int num, byte value) {
        throw new RuntimeException("EEK: ParameterVector");
    }

    /**
     * Places a short value in parameter number "num".
     *
     * @param num the parameter number, ranging from 0 .. #parameters-1
     * @param value the value to be stored
     */
    public void write(int num, short value) {
        throw new RuntimeException("EEK: ParameterVector");
    }

    /**
     * Places a char value in parameter number "num".
     *
     * @param num the parameter number, ranging from 0 .. #parameters-1
     * @param value the value to be stored
     */
    public void write(int num, char value) {
        throw new RuntimeException("EEK: ParameterVector");
    }

    /**
     * Places an int value in parameter number "num".
     *
     * @param num the parameter number, ranging from 0 .. #parameters-1
     * @param value the value to be stored
     */
    public void write(int num, int value) {
        throw new RuntimeException("EEK: ParameterVector");
    }

    /**
     * Places a long value in parameter number "num".
     *
     * @param num the parameter number, ranging from 0 .. #parameters-1
     * @param value the value to be stored
     */
    public void write(int num, long value) {
        throw new RuntimeException("EEK: ParameterVector");
    }

    /**
     * Places a float value in parameter number "num".
     *
     * @param num the parameter number, ranging from 0 .. #parameters-1
     * @param value the value to be stored
     */
    public void write(int num, float value) {
        throw new RuntimeException("EEK: ParameterVector");
    }

    /**
     * Places a double value in parameter number "num".
     *
     * @param num the parameter number, ranging from 0 .. #parameters-1
     * @param value the value to be stored
     */
    public void write(int num, double value) {
        throw new RuntimeException("EEK: ParameterVector");
    }

    /**
     * Places an Object value in parameter number "num".
     *
     * @param num the parameter number, ranging from 0 .. #parameters-1
     * @param value the value to be stored
     */
    public void write(int num, Object value) {
        throw new RuntimeException("EEK: ParameterVector");
    }

    /**
     * Records in the parameter vector that parameter "num" is a sub-array 
     * of boolean array "value", with offset "offset" and length "size".
     * Copying does not necessarily take place.
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
     * Records in the parameter vector that parameter "num" is a sub-array 
     * of byte array "value", with offset "offset" and length "size".
     * Copying does not necessarily take place.
     *
     * @param num The parameter number, ranging from 0 .. #parameters-1.
     * @param offset Offset in the array where the sub-array begins.
     * @param size Length (number of elements) in the sub-array.
     * @param value The array of which a sub-array is the parameter.
     */
    public void writeSubArray(int num, int offset, int size, byte[] value) {
        throw new RuntimeException("EEK: ParameterVector");
    }

    /**
     * Records in the parameter vector that parameter "num" is a sub-array 
     * of short array "value", with offset "offset" and length "size".
     * Copying does not necessarily take place.
     *
     * @param num The parameter number, ranging from 0 .. #parameters-1.
     * @param offset Offset in the array where the sub-array begins.
     * @param size Length (number of elements) in the sub-array.
     * @param value The array of which a sub-array is the parameter.
     */
    public void writeSubArray(int num, int offset, int size, short[] value) {
        throw new RuntimeException("EEK: ParameterVector");
    }

    /**
     * Records in the parameter vector that parameter "num" is a sub-array 
     * of char array "value", with offset "offset" and length "size".
     * Copying does not necessarily take place.
     *
     * @param num The parameter number, ranging from 0 .. #parameters-1.
     * @param offset Offset in the array where the sub-array begins.
     * @param size Length (number of elements) in the sub-array.
     * @param value The array of which a sub-array is the parameter.
     */
    public void writeSubArray(int num, int offset, int size, char[] value) {
        throw new RuntimeException("EEK: ParameterVector");
    }

    /**
     * Records in the parameter vector that parameter "num" is a sub-array 
     * of int array "value", with offset "offset" and length "size".
     * Copying does not necessarily take place.
     *
     * @param num The parameter number, ranging from 0 .. #parameters-1.
     * @param offset Offset in the array where the sub-array begins.
     * @param size Length (number of elements) in the sub-array.
     * @param value The array of which a sub-array is the parameter.
     */
    public void writeSubArray(int num, int offset, int size, int[] value) {
        throw new RuntimeException("EEK: ParameterVector");
    }

    /**
     * Records in the parameter vector that parameter "num" is a sub-array 
     * of long array "value", with offset "offset" and length "size".
     * Copying does not necessarily take place.
     *
     * @param num The parameter number, ranging from 0 .. #parameters-1.
     * @param offset Offset in the array where the sub-array begins.
     * @param size Length (number of elements) in the sub-array.
     * @param value The array of which a sub-array is the parameter.
     */
    public void writeSubArray(int num, int offset, int size, long[] value) {
        throw new RuntimeException("EEK: ParameterVector");
    }

    /**
     * Records in the parameter vector that parameter "num" is a sub-array 
     * of float array "value", with offset "offset" and length "size".
     * Copying does not necessarily take place.
     *
     * @param num The parameter number, ranging from 0 .. #parameters-1.
     * @param offset Offset in the array where the sub-array begins.
     * @param size Length (number of elements) in the sub-array.
     * @param value The array of which a sub-array is the parameter.
     */
    public void writeSubArray(int num, int offset, int size, float[] value) {
        throw new RuntimeException("EEK: ParameterVector");
    }

    /**
     * Records in the parameter vector that parameter "num" is a sub-array 
     * of double array "value", with offset "offset" and length "size".
     * Copying does not necessarily take place.
     *
     * @param num The parameter number, ranging from 0 .. #parameters-1.
     * @param offset Offset in the array where the sub-array begins.
     * @param size Length (number of elements) in the sub-array.
     * @param value The array of which a sub-array is the parameter.
     */
    public void writeSubArray(int num, int offset, int size, double[] value) {
        throw new RuntimeException("EEK: ParameterVector");
    }

    /**
     * Records in the parameter vector that parameter "num" is a sub-array 
     * of Object array "value", with offset "offset" and length "size".
     * Copying does not necessarily take place.
     *
     * @param num The parameter number, ranging from 0 .. #parameters-1.
     * @param offset Offset in the array where the sub-array begins.
     * @param size Length (number of elements) in the sub-array.
     * @param value The array of which a sub-array is the parameter.
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
     * Get a byte from position "num" in the parameter vector.
     *
     * @param num The position number in the parameter vector.
     * @return The boolean requested.
     */
    public byte readByte(int num) {
        throw new RuntimeException("EEK: ParameterVector");
    }

    /**
     * Get a short from position "num" in the parameter vector.
     *
     * @param num The position number in the parameter vector.
     * @return The boolean requested.
     */
    public short readShort(int num) {
        throw new RuntimeException("EEK: ParameterVector");
    }

    /**
     * Get a char from position "num" in the parameter vector.
     *
     * @param num The position number in the parameter vector.
     * @return The boolean requested.
     */
    public char readChar(int num) {
        throw new RuntimeException("EEK: ParameterVector");
    }

    /**
     * Get a int from position "num" in the parameter vector.
     *
     * @param num The position number in the parameter vector.
     * @return The boolean requested.
     */
    public int readInt(int num) {
        throw new RuntimeException("EEK: ParameterVector");
    }

    /**
     * Get a long from position "num" in the parameter vector.
     *
     * @param num The position number in the parameter vector.
     * @return The boolean requested.
     */
    public long readLong(int num) {
        throw new RuntimeException("EEK: ParameterVector");
    }

    /**
     * Get a float from position "num" in the parameter vector.
     *
     * @param num The position number in the parameter vector.
     * @return The boolean requested.
     */
    public float readFloat(int num) {
        throw new RuntimeException("EEK: ParameterVector");
    }

    /**
     * Get a double from position "num" in the parameter vector.
     *
     * @param num The position number in the parameter vector.
     * @return The boolean requested.
     */
    public double readDouble(int num) {
        throw new RuntimeException("EEK: ParameterVector");
    }

    /**
     * Get a Object from position "num" in the parameter vector.
     *
     * @param num The position number in the parameter vector.
     * @return The boolean requested.
     */
    public Object readObject(int num) {
        throw new RuntimeException("EEK: ParameterVector");
    }
}
