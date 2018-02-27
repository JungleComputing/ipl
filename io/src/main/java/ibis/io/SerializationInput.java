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
 * The <code>SerializationInput</code> interface specifies which methods must be
 * implemented by any serialization input stream type. Some of these methods may
 * just throw an exception, depending on the serialization type. For instance,
 * the <code>readInt</code> method of a "byte" serialization input stream will
 * just throw an exception.
 * <p>
 * For all read methods in this interface, the invariant is that the reads must
 * match the writes one by one. The only exception to this rule is that an array
 * written with any of the <code>writeArray</code> methods can be read by
 * {@link #readObject}. <strong> In particular, an array written with
 * {@link SerializationOutput#writeObject writeObject} cannot be read with
 * <code>readArray</code>, because {@link SerializationOutput#writeObject
 * writeObject} does duplicate detection, and may have written only a handle.
 * </strong>
 **/

public interface SerializationInput extends DataInput {

    /**
     * Returns the actual implementation used by the stream.
     *
     * @return the name of the actual serialization implementation used
     */
    public String serializationImplName();

    /**
     * Returns true when the stream must be re-initialized when a connection is
     * added.
     * 
     * @return true when the stream must be re-initialized when a connection is
     *         added.
     */
    public boolean reInitOnNewConnection();

    /**
     * Prints some statistics.
     */
    public void statistics();

    /**
     * Ibis serialization profits from an explicit clear of the object table, so
     * that any stubs in it can be garbage-collected. This significantly reduces
     * the number of connections kept alive.
     */
    public void clear();

    /**
     * Returns the number of bytes that can be read without blocking.
     * 
     * @return number of bytes available without blocking.
     * @throws IOException
     *             on I/O error
     */
    public int available() throws IOException;

    /**
     * Reads a <code>String</code> from the input.
     *
     * @return the string read.
     *
     * @exception java.io.IOException
     *                an error occurred
     */
    public String readString() throws IOException;

    /**
     * Reads a <code>Serializable</code> object from the input.
     *
     * @return the object read.
     *
     * @exception java.io.IOException
     *                an error occurred
     * @exception ClassNotFoundException
     *                is thrown when the class of a serialized object is not
     *                found.
     */
    public Object readObject() throws IOException, ClassNotFoundException;

    /**
     * Reads an array of objects in place.
     *
     * @param ref
     *            array where the result is stored.
     * @exception IOException
     *                is thrown on an IO error.
     * @exception ClassNotFoundException
     *                is thrown when the class of a serialized object is not
     *                found.
     */
    public void readArray(Object[] ref)
            throws IOException, ClassNotFoundException;

    /**
     * Reads a slice of an array in place. No cycle checks are done.
     *
     * @param ref
     *            array in which the slice is stored
     * @param off
     *            offset where the slice starts
     * @param len
     *            length of the slice (the number of elements)
     * @exception IOException
     *                is thrown on an IO error.
     * @exception ClassNotFoundException
     *                is thrown when the class of a serialized object is not
     *                found.
     */
    public void readArray(Object[] ref, int off, int len)
            throws IOException, ClassNotFoundException;

    /**
     * Closes this stream, but not the underlying streams.
     * 
     * @exception java.io.IOException
     *                an error occurred
     */
    public void close() throws IOException;

    /**
     * Closes this stream and the underlying streams.
     * 
     * @exception java.io.IOException
     *                an error occurred
     */
    public void realClose() throws IOException;
}
