/* $Id$ */

package ibis.ipl;

import java.io.IOException;
import java.nio.ByteBuffer;

/** 
 * A message used to write data from a {@link SendPort} to one or more
 * {@link ReceivePort}s.
 * <p>
 * A <code>WriteMessage</code> is obtained from a {@link SendPort} through
 * the {@link SendPort#newMessage SendPort.newMessage} method.
 * At most one <code>WriteMessage</code> is alive at one time for a given
 * </code>SendPort</code>.
 * When a message is alive and a new message is requested, the requester
 * is blocked until the live message is finished.
 * <p>
 * For all read methods in this class, the invariant is that the reads must
 * match the writes one by one. An exception to this rule is that an
 * array written with any of the <code>writeArray</code> methods can be
 * read by {@link ReadMessage#readObject}. Likewise, an
 * array written with the {@link WriteMessage#writeByteBuffer} method can be read
 * by {@link ReadMessage#readObject} (resulting in a byte array), and also by the
 * {@link ReadMessage#readArray(byte[])} method.
 * <strong>
 * In contrast, an array written with
 * {@link #writeObject writeObject}
 * cannot be read with <code>readArray</code>, because
 * {@link #writeObject writeObject} does duplicate detection,
 * and may have written only a handle.
 * </strong>
 * However, an array written with {@link #writeArray(byte[])} can be
 * read with {@link ReadMessage#readByteBuffer(ByteBuffer)}. 
 * <p>
 * The {@link #writeObject(Object)} and {@link #writeString(String)} methods
 * do duplicate checks if the underlying serialization stream is an object
 * serialization stream: if the object was already written to this
 * message, a handle for this object is written instead of the object itself.
 *<p>
 * When the port type has the {@link PortType#CONNECTION_ONE_TO_MANY}
 * or {@link PortType#CONNECTION_MANY_TO_MANY} capability set,
 * no exceptions are thrown by write methods in the write message.
 * Instead, the exception may be passed on to the <code>lostConnection</code>
 * upcall, in case a {@link SendPortDisconnectUpcall} is registered.
 * This allows a multicast to continue to the other destinations.
 * Ultimately, the {@link WriteMessage#finish() finish} method will
 * throw an exception.
 * <p>
 * Data may be streamed, so the user is not allowed to change the data
 * pushed into this message.
 * It is only safe to touch the data after it has actually been
 * sent, which can be ensured by either calling {@link #flush},
 * {@link  WriteMessage#finish()} or {@link #reset},
 * or a {@link #send} followed by a corresponding {@link #sync}.
 */

public interface WriteMessage {

    /**
     * Starts sending the message to all {@link ibis.ipl.ReceivePort
     * ReceivePorts} its {@link ibis.ipl.SendPort} is connected to.
     * Data may be streamed, so the user is not allowed to change the data
     * pushed into this message, as the send is NON-blocking.
     * It is only safe to touch the data after it has actually been
     * sent, which can be ensured by either calling {@link #finish()} or
     * {@link #reset}, or a {@link #sync} corresponding to this send.
     * The <code>send</code> method returns a ticket, which can be used
     * as a parameter to the {@link #sync} method, which will block until
     * the data corresponding to this ticket can be used again.
     * @return
     *          a ticket.
     * @exception IOException
     *          an error occurred 
     **/
    public int send() throws IOException;

    /**
     * Blocks until the data of the <code>send</code> which returned
     * the <code>ticket</code> parameter may be used again.
     * It also synchronizes with respect to all sends before that.
     * If <code>ticket</code> does not correspond to any <code>send</code>,
     * it blocks until all outstanding sends have been processed.
     * @param ticket 
     *          the ticket number.
     * @exception IOException
     *          an error occurred 
     */
    public void sync(int ticket) throws IOException;
    
    /**
     * Blocks until all objects written to this message can be
     * used again.
     * @exception IOException
     *          an error occurred 
     */
    public void flush() throws IOException;

    /**
     * Resets the state of any objects already written to the stream.
     * This means that the WriteMessage "forgets" which objects already
     * have been written with it.
     * @exception IOException
     *          an error occurred 
     */
    public void reset() throws IOException;

    /**
     * If needed, send, and then block until the entire message has been sent
     * and clear the message. The number of bytes written in this message
     * is returned.
     * After the finish, no more bytes can be written to the message,
     * and other information obtained from the message, such as
     * {@link #bytesWritten()} is no longer available. Implementations
     * are explicitly allowed to reuse the message object.
     * <strong> 
     * Even for multicast messages, the size only counts once.
     * </strong>
     * @return
     *          the number of bytes written in this message.
     * @exception IOException
     *          an error occurred 
     **/
    public long finish() throws IOException;

    /**
     * This method can be used to inform Ibis that one of the
     * <code>WriteMessage</code> methods has thrown an IOException.
     * It implies a {@link #finish()}.
     * @param exception
     *          the exception that was thrown.
     */
    public void finish(IOException exception);

    /**
     * Returns the number of bytes read from this message. 
     * Note that for streaming implementations (i.e., messages with an unlimited
     * capacity) this number may not be exact because of intermediate buffering.  
     * 
     * <strong> 
     * Even for multicast messages, the size only counts once.
     * </strong>
     * @return
     *          the number of bytes written to this message.
     * @exception IOException
     *          an error occurred.
     */
    public long bytesWritten() throws IOException;

    /**
     * Returns the maximum number of bytes that will fit into this message.  
     * 
     * @return
     *          the maximum number of bytes that will fit into this message or
     *          -1 when the message size is unlimited 
     * @exception IOException
     *          an error occurred.
     */
    public int capacity() throws IOException;

    /**
     * Returns the remaining number of bytes that can be written into this 
     * message.  
     * 
     * @return
     *          the remaining number of bytes that can be written into this 
     *          message or -1 when the message size is unlimited 
     * @exception IOException
     *          an error occurred.
     */
    public int remaining() throws IOException;
    
    /**
     * Returns the {@link SendPort} of this <code>WriteMessage</code>.
     * @return
     *          the {@link SendPort} of this <code>WriteMessage</code>.
     */
    public SendPort localPort();

    /**
     * Writes a boolean value to the message.
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     * @param value             
     *          the boolean value to write.
     * @exception IOException
     *          an error occurred 
     */
    public void writeBoolean(boolean value) throws IOException;

    /**
     * Writes a byte value to the message.
     * @param value
     *          the byte value to write.
     * @exception IOException
     *          an error occurred 
     */
    public void writeByte(byte value) throws IOException;

    /**
     * Writes a char value to the message.
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     * @param value
     *          the char value to write.
     * @exception IOException
     *          an error occurred 
     */
    public void writeChar(char value) throws IOException;

    /**
     * Writes a short value to the message.
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     * @param value
     *          the short value to write.
     * @exception IOException
     *          an error occurred 
     */
    public void writeShort(short value) throws IOException;

    /**
     * Writes a int value to the message.
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     * @param value
     *          the int value to write.
     * @exception IOException
     *          an error occurred 
     */
    public void writeInt(int value) throws IOException;

    /**
     * Writes a long value to the message.
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     * @param value
     *          the long value to write.
     * @exception IOException
     *          an error occurred 
     */
    public void writeLong(long value) throws IOException;

    /**
     * Writes a float value to the message.
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     * @param value
     *          the float value to write.
     * @exception IOException
     *          an error occurred 
     */
    public void writeFloat(float value) throws IOException;

    /**
     * Writes a double value to the message.
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     * @param value
     *          the double value to write.
     * @exception IOException
     *          an error occurred 
     */
    public void writeDouble(double value) throws IOException;

    /**
     * Writes a <code>String</code> to the message.
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     * If the underlying serialization stream is an object serialization
     * stream, a duplicate check for this <code>String</code> object
     * is performed: if the object was already written to this
     * message, a handle for this object is written instead of
     * the object itself.
     * @param value
     *          the string to write.
     * @exception IOException
     *          an error occurred 
     */
    public void writeString(String value) throws IOException;

    /**
     * Writes a <code>Serializable</code> object to the message.
     * This method throws an IOException if the underlying serialization
     * stream cannot do object serialization.
     * (See {@link PortType#SERIALIZATION_OBJECT}).
     * A duplicate check for this <code>String</code> object
     * is performed: if the object was already written to this
     * message, a handle for this object is written instead of
     * the object itself.
     * @param value
     *          the object value to write.
     * @exception IOException
     *          an error occurred 
     */
    public void writeObject(Object value) throws IOException;

    /**
     * Writes an array to the message.
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     * No duplicate check is performed for this array!
     * This method is just a shortcut for doing:
     * <code>writeArray(val, 0, val.length);</code>
     * @param value
     *          the array to be written.
     * @exception IOException
     *          an error occurred 
     **/
    public void writeArray(boolean[] value) throws IOException;

    /**
     * Writes an array to the message.
     * No duplicate check is performed for this array!
     * This method is just a shortcut for doing:
     * <code>writeArray(val, 0, val.length);</code>
     * @param value
     *          the array to be written.
     * @exception IOException
     *          an error occurred 
     */
    public void writeArray(byte[] value) throws IOException;

    /**
     * Writes an array to the message.
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     * No duplicate check is performed for this array!
     * This method is just a shortcut for doing:
     * <code>writeArray(val, 0, val.length);</code>
     * @param value
     *          the array to be written.
     * @exception IOException
     *          an error occurred 
     */
    public void writeArray(char[] value) throws IOException;

    /**
     * Writes an array to the message.
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     * No duplicate check is performed for this array!
     * This method is just a shortcut for doing:
     * <code>writeArray(val, 0, val.length);</code>
     * @param value
     *          the array to be written.
     * @exception IOException
     *          an error occurred 
     */
    public void writeArray(short[] value) throws IOException;

    /**
     * Writes an array to the message.
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     * No duplicate check is performed for this array!
     * This method is just a shortcut for doing:
     * <code>writeArray(val, 0, val.length);</code>
     * @param value
     *          the array to be written.
     * @exception IOException
     *          an error occurred 
     */
    public void writeArray(int[] value) throws IOException;

    /**
     * Writes an array to the message.
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     * No duplicate check is performed for this array!
     * This method is just a shortcut for doing:
     * <code>writeArray(val, 0, val.length);</code>
     * @param value
     *          the array to be written.
     * @exception IOException
     *          an error occurred 
     */
    public void writeArray(long[] value) throws IOException;

    /**
     * Writes an array to the message.
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     * No duplicate check is performed for this array!
     * This method is just a shortcut for doing:
     * <code>writeArray(val, 0, val.length);</code>
     * @param value
     *          the array to be written.
     * @exception IOException
     *          an error occurred 
     */
    public void writeArray(float[] value) throws IOException;

    /**
     * Writes an array to the message.
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     * No duplicate check is performed for this array!
     * This method is just a shortcut for doing:
     * <code>writeArray(val, 0, val.length);</code>
     * @param value
     *          the array to be written.
     * @exception IOException
     *          an error occurred 
     */
    public void writeArray(double[] value) throws IOException;

    /**
     * Writes an array to the message.
     * This method throws an IOException if the underlying serialization
     * stream cannot do object serialization.
     * (See {@link PortType#SERIALIZATION_OBJECT}).
     * No duplicate check is performed for this array!
     * This method is just a shortcut for doing:
     * <code>writeArray(val, 0, val.length);</code>
     * @param value
     *          the array to be written.
     * @exception IOException
     *          an error occurred 
     */
    public void writeArray(Object[] value) throws IOException;

    /**
     * Writes a slice of an array. The slice starts at offset <code>off</code>.
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     * No duplicate check is performed for this array!
     * @param value
     *          the array to be written
     * @param offset
     *          offset in the array
     * @param length
     *          the number of elements to be written
     * @exception IOException
     *          an error occurred 
     */
    public void writeArray(boolean[] value, int offset, int length)
            throws IOException;

    /**
     * Writes a slice of an array. The slice starts at offset <code>off</code>.
     * No duplicate check is performed for this array!
     * @param value
     *          the array to be written
     * @param offset
     *          offset in the array
     * @param length
     *          the number of elements to be written
     * @exception IOException
     *          an error occurred 
     */
    public void writeArray(byte[] value, int offset, int length)
            throws IOException;

    /**
     * Writes a slice of an array. The slice starts at offset <code>off</code>.
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     * No duplicate check is performed for this array!
     * @param value
     *          the array to be written
     * @param offset
     *          offset in the array
     * @param length
     *          the number of elements to be written
     * @exception IOException
     *          an error occurred 
     */
    public void writeArray(char[] value, int offset, int length)
            throws IOException;

    /**
     * Writes a slice of an array. The slice starts at offset <code>off</code>.
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     * No duplicate check is performed for this array!
     * @param value
     *          the array to be written
     * @param offset
     *          offset in the array
     * @param length
     *          the number of elements to be written
     * @exception IOException
     *          an error occurred 
     */
    public void writeArray(short[] value, int offset, int length)
            throws IOException;

    /**
     * Writes a slice of an array. The slice starts at offset <code>off</code>.
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     * No duplicate check is performed for this array!
     * @param value
     *          the array to be written
     * @param offset
     *          offset in the array
     * @param length
     *          the number of elements to be written
     * @exception IOException
     *          an error occurred 
     */
    public void writeArray(int[] value, int offset, int length)
            throws IOException;

    /**
     * Writes a slice of an array. The slice starts at offset <code>off</code>.
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     * No duplicate check is performed for this array!
     * @param value
     *          the array to be written
     * @param offset
     *          offset in the array
     * @param length
     *          the number of elements to be written
     * @exception IOException
     *          an error occurred 
     */
    public void writeArray(long[] value, int offset, int length)
            throws IOException;

    /**
     * Writes a slice of an array. The slice starts at offset <code>off</code>.
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     * No duplicate check is performed for this array!
     * @param value
     *          the array to be written
     * @param offset
     *          offset in the array
     * @param length
     *          the number of elements to be written
     * @exception IOException
     *          an error occurred 
     */
    public void writeArray(float[] value, int offset, int length)
            throws IOException;

    /**
     * Writes a slice of an array. The slice starts at offset <code>off</code>.
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     * No duplicate check is performed for this array!
     * @param value
     *          the array to be written
     * @param offset
     *          offset in the array
     * @param length
     *          the number of elements to be written
     * @exception IOException
     *          an error occurred 
     */
    public void writeArray(double[] value, int offset, int length)
            throws IOException;

    /**
     * Writes a slice of an array. The slice starts at offset <code>off</code>.
     * This method throws an IOException if the underlying serialization
     * stream cannot do object serialization.
     * (See {@link PortType#SERIALIZATION_OBJECT}).
     * No duplicate check is performed for this array!
     * @param value
     *          the array to be written
     * @param offset
     *          offset in the array
     * @param length
     *          the number of elements to be written
     * @exception IOException
     *          an error occurred 
     */
    public void writeArray(Object[] value, int offset, int length)
            throws IOException;
    
    /**
     * Writes the contents of the byte buffer (between its current position and its
     * limit). This method is allowed for all serialization types, even
     * {@link PortType#SERIALIZATION_BYTE}.
     * @param value
     * 		the byte buffer from which data is to be written
     * @exception IOException
     *          an error occurred
     */
    public void writeByteBuffer(ByteBuffer value)
    		throws IOException;
}
