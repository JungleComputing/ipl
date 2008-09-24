/* $Id$ */

package ibis.io.jme;

import ibis.io.Replacer;
import ibis.io.SerializationOutput;

import java.io.IOException;

/** 
 * The <code>ObjectInput</code> is a rename of the SerializationInput
 * interface in order to support replacement of classes using Sun
 * serialization with just a change in packages since Sun serialization
 * has an <code>ObjectInput</code> interface on it's 
 * <code>ObjectStream*</code> classes.
 **/

public interface ObjectOutput extends SerializationOutput {

    /**
     * Resets the state of any objects already written to this output.
     *
     * @exception java.io.IOException	an error occurred 
     */
    public void reset() throws IOException;

    /**
     * Resets the state of any objects already written to this output, and
     * optionally also clears the type table cache.
     *
     * @param cleartypes when set, the type table cache is also cleared.
     *
     * @exception java.io.IOException	an error occurred 
     */
    public void reset(boolean cleartypes) throws IOException;

    /**
     * Returns true when the stream must be re-initialized when a
     * connection is added.
     * @return true when the stream must be re-initialized when a connection
     * is added.
     */
    public boolean reInitOnNewConnection();

    /**
     * Writes a <code>String</code> to the output.
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
     * Writes a <code>Serializable</code> object to the output.
     * A duplicate check for this <code>String</code> object
     * is performed: if the object was already written to this
     * output, a handle for this object is written instead of
     * the object itself.
     *
     * @param     val			the object value to write.
     *
     * @exception java.io.IOException	an error occurred 
     */
    public void writeObject(Object val) throws IOException;

    /**
     * Writes an array of objects to the output.
     *
     * @param val                       the array to write.
     *
     * @exception java.io.IOException   an error occurred
     */
    public void writeArray(Object[] val) throws IOException;

    /**
     * Writes a slice of an array of objects.
     *
     * @param val                       the array to write.
     * @param off                       the offset where the slice starts
     * @param len                       the length of the slice.
     *
     * @exception java.io.IOException   an error occurred
     */
    public void writeArray(Object[] val, int off, int len) throws IOException;

    /**
     * Returns the actual implementation used by this output.
     *
     * @return the name of the actual serialization implementation used
     */
    public String serializationImplName();

    /**
     * Print some statistics. 
     */
    public void statistics();

    /**
     * Set a replacer. The replacement mechanism can be used to replace
     * an object with another object during serialization. This is used
     * in RMI, for instance, to replace a remote object with a stub. 
     * 
     * @param replacer the replacer object to be associated with this
     *  output stream
     *
     * @exception java.io.IOException is thrown when the implementation
     *  refuses to set the replacer, for instance when it is not an
     *  object serialization.
     */
    public void setReplacer(Replacer replacer) throws IOException;

    /**
     * Flushes the stream and its underlying streams.
     * @exception java.io.IOException	an error occurred 
     */
    public void flush() throws IOException;

    /**
     * Flushes and closes this stream, and flushes the underlying streams.
     * @exception java.io.IOException	an error occurred 
     */
    public void close() throws IOException;

    /**
     * Flushes and closes this stream and the underlying streams.
     * @exception java.io.IOException	an error occurred 
     */
    public void realClose() throws IOException;
}
