/*
 * Copyright 2010 Vrije Universiteit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* $Id$ */

package ibis.io;

import java.io.IOException;

/** 
 * The <code>SerializationOutput</code> interface specifies which methods
 * must be implemented by any serialization output stream type. Some
 * of these methods may just throw an exception, depending on the
 * serialization type. For instance, the <code>writeInt</code> method
 * of a "byte" serialization output stream will just throw an exception.
 * <p>
 * For all write methods in this interface, the invariant is that the reads
 * must match the writes one by one. The only exception to this rule is that an
 * array written with any of the <code>writeArray</code> methods can be
 * read by
 * {@link SerializationInput#readObject SerializationInput.readObject}.
 * <strong>
 * In particular, an array written with {@link #writeObject writeObject}
 * cannot be read with <code>readArray</code>, because
 * {@link #writeObject writeObject} does duplicate detection, and may
 * have written only a handle.
 * </strong>
 **/

public interface SerializationOutput extends DataOutput {

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
