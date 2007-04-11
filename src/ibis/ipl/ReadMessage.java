/* $Id$ */

package ibis.ipl;

import java.io.IOException;

/** 
 * The Ibis abstraction for data to be read.
 * A <code>ReadMessage</code> is obtained from a
 * {@link ibis.ipl.ReceivePort receiveport}, either
 * by means of an upcall, or by means of an explicit receive, which is
 * accomplished by call to {@link ibis.ipl.ReceivePort#receive() receive}.
 * A {@link ibis.ipl.ReceivePort receiveport} can be configured to generate
 * upcalls or to support blocking receive, but NOT both!
 * At most one <code>ReadMessage</code> is alive at one time for a given
 * </code>ReceivePort</code>.
 * For all read methods in this class, the invariant is that the reads must
 * match the writes one by one. The only exception to this rule is that an
 * array written with any of the <code>writeArray</code> methods of
 * {@link WriteMessage} can be read by {@link #readObject}.
 * <strong>
 * In particular, an array written with
 * {@link WriteMessage#writeObject writeObject}
 * cannot be read with <code>readArray</code>, because
 * {@link WriteMessage#writeObject writeObject} does duplicate detection,
 * and may have written only a handle.
 * </strong>
 **/

public interface ReadMessage {

    /** The first sequence number when communication is numbered. */
    public static final long INITIAL_SEQNO = 1;

    /**
     * The <code>finish</code> operation is used to indicate that the
     * reader is done with the message.
     * After the finish, no more bytes can be read from the message.
     * The thread reading the message (can be an upcall) is NOT
     * allowed to block when a message is alive but not finished.
     * Only after the finish is called can the thread that holds the
     * message block.
     * The finish operation must always be called when blocking receive
     * is used. When upcalls are used, the finish can be avoided when the
     * user is sure that the upcall never blocks or waits for a message
     * to arrive. In that case, the message is automatically finished when
     * the upcall terminates. This is much more efficient, because in this way,
     * the runtime system can reuse the upcall thread!
     *
     * @return the number of bytes read from this message.
     * @exception java.io.IOException	an error occurred.
     **/
    public long finish() throws IOException;

    /**
     * This method can be used to inform Ibis that one of the
     * <code>ReadMessage</code> methods has thrown an IOException.
     * It implies a {@link #finish()}.
     * In a message upcall, the alternative way to do this is to have
     * the upcall throw the exception.
     * @param exception the exception that was thrown. 
     */
    public void finish(IOException exception);

    /**
     * Returns the number of bytes read from this message. This number
     * is not exact, because of buffering in underlying input streams.
     * @return the number of bytes read sofar from this message.
     * @exception java.io.IOException	an error occurred.
     */
    public long bytesRead() throws IOException;

    /**
     * Returns the {@link ibis.ipl.ReceivePort receiveport} of this
     * <code>ReadMessage</code>.
     *
     * @return the {@link ibis.ipl.ReceivePort receiveport} of this
     * <code>ReadMessage</code>.
     */
    public ReceivePort localPort();

    /**
     * Returns the sequence number of this message.
     * An Ibis implementation may choose to just return -1, in case it does not
     * promise a specific ordering of the messages.
     * @return a sequence number, or -1.
     */
    public long sequenceNumber();

    /**
     * Returns the {@link ibis.ipl.SendPortIdentifier sendport identifier}
     * of the sender of this message.
     *
     * @return the id of the sender of this message.
     */
    public SendPortIdentifier origin();

    /**
     * Reads a boolean value from the message.
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     *
     * @return the value read.
     * @exception java.io.IOException	an error occurred 
     */
    public boolean readBoolean() throws IOException;

    /**
     * Reads a byte value from the message.
     *
     * @return the value read.
     * @exception java.io.IOException	an error occurred 
     */
    public byte readByte() throws IOException;

    /**
     * Reads a char value from the message.
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     *
     * @return the value read.
     * @exception java.io.IOException	an error occurred 
     */
    public char readChar() throws IOException;

    /**
     * Reads a short value from the message.
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     *
     * @return the value read.
     * @exception java.io.IOException	an error occurred 
     */
    public short readShort() throws IOException;

    /**
     * Reads an int value from the message.
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     *
     * @return the value read.
     * @exception java.io.IOException	an error occurred 
     */
    public int readInt() throws IOException;

    /**
     * Reads a long value from the message.
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     *
     * @return the value read.
     * @exception java.io.IOException	an error occurred 
     */
    public long readLong() throws IOException;

    /**
     * Reads a float value from the message.
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     *
     * @return the value read.
     * @exception java.io.IOException	an error occurred 
     */
    public float readFloat() throws IOException;

    /**
     * Reads a double value from the message.
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     *
     * @return the value read.
     * @exception java.io.IOException	an error occurred 
     */
    public double readDouble() throws IOException;

    /**
     * Reads a <code>String</code> value from the message.
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     *
     * @return the value read.
     * @exception java.io.IOException	an error occurred 
     */
    public String readString() throws IOException;

    /**
     * Reads an <code>Object</code> value from the message.
     * Note: implementations should take care that an array, written with one
     * of the writeArray variants, can be read with <code>readObject</code>.
     * This method throws an IOException if the underlying serialization
     * stream cannot do object serialization.
     * (See {@link PortType#SERIALIZATION_OBJECT}).
     *
     * @return the value read.
     * @exception java.io.IOException	an error occurred 
     * @exception ClassNotFoundException is thrown when an object arrives
     *	of a class that cannot be loaded locally.
     */
    public Object readObject() throws IOException, ClassNotFoundException;

    /**
     * Receives an array in place. This method is a shortcut for
     * <code>readArray(destination, 0, destination.length);</code>
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     *
     * @param destination where the received array is stored.
     * @exception IOException is thrown when an IO error occurs.
     */
    public void readArray(boolean[] destination) throws IOException;

    /**
     * Receives an array in place. This method is a shortcut for
     * <code>readArray(destination, 0, destination.length);</code>
     *
     * @param destination where the received array is stored.
     * @exception IOException is thrown when an IO error occurs.
     */
    public void readArray(byte[] destination) throws IOException;

    /**
     * Receives an array in place. This method is a shortcut for
     * <code>readArray(destination, 0, destination.length);</code>
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     *
     * @param destination where the received array is stored.
     * @exception IOException is thrown when an IO error occurs.
     */
    public void readArray(char[] destination) throws IOException;

    /**
     * Receives an array in place. This method is a shortcut for
     * <code>readArray(destination, 0, destination.length);</code>
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     *
     * @param destination where the received array is stored.
     * @exception IOException is thrown when an IO error occurs.
     */
    public void readArray(short[] destination) throws IOException;

    /**
     * Receives an array in place. This method is a shortcut for
     * <code>readArray(destination, 0, destination.length);</code>
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     *
     * @param destination where the received array is stored.
     * @exception IOException is thrown when an IO error occurs.
     */
    public void readArray(int[] destination) throws IOException;

    /**
     * Receives an array in place. This method is a shortcut for
     * <code>readArray(destination, 0, destination.length);</code>
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     *
     * @param destination where the received array is stored.
     * @exception IOException is thrown when an IO error occurs.
     */
    public void readArray(long[] destination) throws IOException;

    /**
     * Receives an array in place. This method is a shortcut for
     * <code>readArray(destination, 0, destination.length);</code>
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     *
     * @param destination where the received array is stored.
     * @exception IOException is thrown when an IO error occurs.
     */
    public void readArray(float[] destination) throws IOException;

    /**
     * Receives an array in place. This method is a shortcut for
     * <code>readArray(destination, 0, destination.length);</code>
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     *
     * @param destination where the received array is stored.
     * @exception IOException is thrown when an IO error occurs.
     */

    public void readArray(double[] destination) throws IOException;

    /**
     * Receives an array in place. This method is a shortcut for
     * <code>readArray(destination, 0, destination.length);</code>
     * This method throws an IOException if the underlying serialization
     * stream cannot do object serialization.
     * (See {@link PortType#SERIALIZATION_OBJECT}).
     *
     * @param destination where the received array is stored.
     * @exception IOException is thrown when an IO error occurs.
     * @exception ClassNotFoundException when an object arrives
     * of a class that cannot be loaded locally.
     */
    public void readArray(Object[] destination) throws IOException,
            ClassNotFoundException;

    /**
     * Reads a slice of an array in place.
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     *
     * @param destination array in which the slice is stored
     * @param offset      offset where the slice starts
     * @param size        length of the slice (the number of elements)
     * @exception IOException is thrown on an IO error.
     */
    public void readArray(boolean[] destination, int offset, int size)
            throws IOException;

    /**
     * Reads a slice of an array in place.
     *
     * @param destination array in which the slice is stored
     * @param offset      offset where the slice starts
     * @param size        length of the slice (the number of elements)
     * @exception IOException is thrown on an IO error.
     */
    public void readArray(byte[] destination, int offset, int size)
            throws IOException;

    /**
     * Reads a slice of an array in place.
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     *
     * @param destination array in which the slice is stored
     * @param offset      offset where the slice starts
     * @param size        length of the slice (the number of elements)
     * @exception IOException is thrown on an IO error.
     */
    public void readArray(char[] destination, int offset, int size)
            throws IOException;

    /**
     * Reads a slice of an array in place.
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     *
     * @param destination array in which the slice is stored
     * @param offset      offset where the slice starts
     * @param size        length of the slice (the number of elements)
     * @exception IOException is thrown on an IO error.
     */
    public void readArray(short[] destination, int offset, int size)
            throws IOException;

    /**
     * Reads a slice of an array in place.
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     *
     * @param destination array in which the slice is stored
     * @param offset      offset where the slice starts
     * @param size        length of the slice (the number of elements)
     * @exception IOException is thrown on an IO error.
     */
    public void readArray(int[] destination, int offset, int size)
            throws IOException;

    /**
     * Reads a slice of an array in place.
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     *
     * @param destination array in which the slice is stored
     * @param offset      offset where the slice starts
     * @param size        length of the slice (the number of elements)
     * @exception IOException is thrown on an IO error.
     */
    public void readArray(long[] destination, int offset, int size)
            throws IOException;

    /**
     * Reads a slice of an array in place.
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     *
     * @param destination array in which the slice is stored
     * @param offset      offset where the slice starts
     * @param size        length of the slice (the number of elements)
     * @exception IOException is thrown on an IO error.
     */
    public void readArray(float[] destination, int offset, int size)
            throws IOException;

    /**
     * Reads a slice of an array in place.
     * This method throws an IOException if the underlying serialization
     * stream can only do byte serialization.
     * (See {@link PortType#SERIALIZATION_BYTE}).
     *
     * @param destination array in which the slice is stored
     * @param offset      offset where the slice starts
     * @param size        length of the slice (the number of elements)
     * @exception IOException is thrown on an IO error.
     */
    public void readArray(double[] destination, int offset, int size)
            throws IOException;

    /**
     * Reads a slice of an array in place.
     * This method throws an IOException if the underlying serialization
     * stream cannot do object serialization.
     * (See {@link PortType#SERIALIZATION_OBJECT}).
     *
     * @param destination array in which the slice is stored
     * @param offset      offset where the slice starts
     * @param size        length of the slice (the number of elements)
     * @exception IOException is thrown on an IO error.
     * @exception ClassNotFoundException when an object arrives
     * of a class that cannot be loaded locally.
     */
    public void readArray(Object[] destination, int offset, int size)
            throws IOException, ClassNotFoundException;
}
