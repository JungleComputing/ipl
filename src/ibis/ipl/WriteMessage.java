package ibis.ipl;

import java.io.IOException;

/** 
 * The Ibis abstraction for data to be written.
 * A <code>WriteMessage</code> is obtained from a {@link SendPort} through
 * the {@link SendPort#newMessage SendPort.newMessage} method.
 * At most one <code>WriteMessage</code> is alive at one time for a given
 * </code>SendPort</code>.
 * When a message is alive and a new message is requested, the requester
 * is blocked until the live message is finished.
 * For all write methods in this class, the invariant is that the reads must
 * match the writes one by one. The only exception to this rule is that an
 * array written with any of the <code>writeArray</code> methods can be
 * read by {@link ReadMessage#readObject ReadMessage.readObject}.
 * <strong>
 * In particular, an array written with {@link #writeObject writeObject}
 * cannot be read with <code>readArray</code>, because
 * {@link #writeObject writeObject} does duplicate detection, and may
 * have written only a handle.
 * </strong>
 **/

public interface WriteMessage { 

    /**
     * Starts sending the message to all {@link ibis.ipl.ReceivePort
     * ReceivePorts} its {@link ibis.ipl.SendPort} is connected to.
     * Data may be streamed, so the user is not allowed to change the data
     * pushed into this message, as the send is NON-blocking.
     * It is only safe to touch the data after it has actually been
     * sent, which can be ensured by either calling {@link #finish} or
     * {@link #reset}, or a {@link #sync} corresponding to this send.
     * The <code>send</code> method returns a ticket, which can be used
     * as a parameter to the {@link #sync} method, which will block until
     * the data corresponding to this ticket can be used again.
     *
     * @return a ticket.
     * @exception java.io.IOException	an error occurred 
     **/
    public int send() throws IOException; 

    /**
     * Blocks until the data of the <code>send</code> which returned
     * the <code>ticket</code> parameter may be used again.
     * It also synchronizes with respect to all sends before that.
     * If <code>ticket</code> does not correspond to any <code>send</code>,
     * it blocks until all outstanding sends have been processed.
     *
     * @param ticket the ticket number.
     * @exception java.io.IOException	an error occurred 
     */
    public void sync(int ticket) throws IOException;

    /**
     * Resets the state of any objects already written to the stream.
     * It is reset to be the same as for a new WriteMessage.
     *
     * @exception java.io.IOException	an error occurred 
     */
    public void reset() throws IOException;

    /**
     * If needed, send, and then block until the entire message has been sent
     * and clear the message.
     *
     * @return the number of bytes written in this message.
     * @exception java.io.IOException	an error occurred 
     **/
    public long finish() throws IOException;

    /**
     * This method can be used to inform Ibis that one of the
     * <code>WriteMessage</code> methods has thrown an IOException.
     * It implies a {@link #finish}.
     * @param e the exception that was thrown.
     */
    public void finish(IOException e);

    /**
     * Returns the {@link SendPort} of this <code>WriteMessage</code>.
     *
     * @return the {@link SendPort} of this <code>WriteMessage</code>.
     */
    public SendPort localPort();

    /**
     * Writes a boolean value to the message.
     * @param     val             	the boolean value to write.
     *
     * @exception java.io.IOException	an error occurred 
     */
    public void writeBoolean(boolean val) throws IOException;

    /**
     * Writes a byte value to the message.
     * @param     val			the byte value to write.
     *
     * @exception java.io.IOException	an error occurred 
     */
    public void writeByte(byte val) throws IOException;

    /**
     * Writes a char value to the message.
     * @param     val			the char value to write.
     *
     * @exception java.io.IOException	an error occurred 
     */
    public void writeChar(char val) throws IOException;

    /**
     * Writes a short value to the message.
     * @param     val			the short value to write.
     *
     * @exception java.io.IOException	an error occurred 
     */
    public void writeShort(short val) throws IOException;

    /**
     * Writes a int value to the message.
     * @param     val			the int value to write.
     *
     * @exception java.io.IOException	an error occurred 
     */
    public void writeInt(int val) throws IOException;

    /**
     * Writes a long value to the message.
     * @param     val			the long value to write.
     *
     * @exception java.io.IOException	an error occurred 
     */
    public void writeLong(long val) throws IOException;

    /**
     * Writes a float value to the message.
     * @param     val			the float value to write.
     *
     * @exception java.io.IOException	an error occurred 
     */
    public void writeFloat(float val) throws IOException;

    /**
     * Writes a double value to the message.
     * @param     val			the double value to write.
     *
     * @exception java.io.IOException	an error occurred 
     */
    public void writeDouble(double val) throws IOException;

    /**
     * Writes a <code>String</code> to the message.
     * A duplicate check for this <code>String</code> object
     * is performed: if the object was already written to this
     * message, a handle for this object is written instead of
     * the object itself.
     *
     * @param     val			the string to write.
     *
     * @exception java.io.IOException	an error occurred 
     */
    public void writeString(String val) throws IOException;

    /**
     * Writes a <code>Serializable</code> object to the message.
     * A duplicate check for this <code>String</code> object
     * is performed: if the object was already written to this
     * message, a handle for this object is written instead of
     * the object itself.
     *
     * @param     val			the object value to write.
     *
     * @exception java.io.IOException	an error occurred 
     */
    public void writeObject(Object val) throws IOException;

    /**
     * Writes an array of booleans to the message.
     * No duplicate check is performed for this array!
     * This method is just a shortcut for doing:
     * <code>writeArray(val, 0, val.length);</code>
     * (See {@link #writeArray(boolean[], int, int)}).
     *
     * @param val			the array to be written.
     *
     * @exception java.io.IOException	an error occurred 
     **/
    public void writeArray(boolean[] val) throws IOException;

    /**
     * Writes an array of bytes to the message.
     * See {@link #writeArray(boolean[])} for a description.
     */
    public void writeArray(byte []val) throws IOException;


    /**
     * Writes an array of chars to the message.
     * See {@link #writeArray(boolean[])} for a description.
     */
    public void writeArray(char[] val) throws IOException;

    /**
     * Writes an array of shorts to the message.
     * See {@link #writeArray(boolean[])} for a description.
     */
    public void writeArray(short[] val) throws IOException;

    /**
     * Writes an array of ints to the message.
     * See {@link #writeArray(boolean[])} for a description.
     */
    public void writeArray(int[] val) throws IOException;

    /**
     * Writes an array of longs to the message.
     * See {@link #writeArray(boolean[])} for a description.
     */
    public void writeArray(long[] val) throws IOException;

    /**
     * Writes an array of floats to the message.
     * See {@link #writeArray(boolean[])} for a description.
     */
    public void writeArray(float[] val) throws IOException;

    /**
     * Writes an array of doubles to the message.
     * See {@link #writeArray(boolean[])} for a description.
     */
    public void writeArray(double[] val) throws IOException;

    /**
     * Writes an array of objects to the message.
     * See {@link #writeArray(boolean[])} for a description.
     */
    public void writeArray(Object[] val) throws IOException;

    /**
     * Writes a slice of an array of booleans. The slice starts
     * at offset <code>off
     * No duplicate check is performed for this array!
     *
     * @param val			the array to be written
     * @param off			offset in the array
     * @param len			the number of elements to be written
     *
     * @exception java.io.IOException	an error occurred 
     */
    public void writeArray(boolean[] val, int off, int len) throws IOException;

    /**
     * Write a slice of an array of bytes.
     * See {@link #writeArray(boolean[], int, int)} for a description.
     */
    public void writeArray(byte[] val, int off, int len) throws IOException;

    /**
     * Write a slice of an array of chars.
     * See {@link #writeArray(boolean[], int, int)} for a description.
     */
    public void writeArray(char[] val, int off, int len) throws IOException;

    /**
     * Write a slice of an array of shorts.
     * See {@link #writeArray(boolean[], int, int)} for a description.
     */
    public void writeArray(short[] val, int off, int len) throws IOException;

    /**
     * Write a slice of an array of ints.
     * See {@link #writeArray(boolean[], int, int)} for a description.
     */
    public void writeArray(int[] val, int off, int len) throws IOException;

    /**
     * Write a slice of an array of longs.
     * See {@link #writeArray(boolean[], int, int)} for a description.
     */
    public void writeArray(long[] val, int off, int len) throws IOException;

    /**
     * Write a slice of an array of floats.
     * See {@link #writeArray(boolean[], int, int)} for a description.
     */
    public void writeArray(float[] val, int off, int len) throws IOException;

    /**
     * Write a slice of an array of doubles.
     * See {@link #writeArray(boolean[], int, int)} for a description.
     */
    public void writeArray(double[] val, int off, int len) throws IOException;

    /**
     * Write a slice of an array of objects.
     * See {@link #writeArray(boolean[], int, int)} for a description.
     */
    public void writeArray(Object[] val, int off, int len) throws IOException;
} 
