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
import java.nio.ByteBuffer;

/**
 * The <code>DataOutput</code> interface provides methods to write data.
 */
public interface DataOutput {

    /**
     * Writes a boolean value.
     * 
     * @param value
     *            the boolean value to write
     * @exception IOException
     *                on an IO error
     */
    public void writeBoolean(boolean value) throws IOException;

    /**
     * Writes a byte value.
     * 
     * @param value
     *            the byte value to write
     * @exception IOException
     *                on an IO error
     */
    public void writeByte(byte value) throws IOException;

    /**
     * Writes a char value.
     * 
     * @param value
     *            the char value to write
     * @exception IOException
     *                on an IO error
     */
    public void writeChar(char value) throws IOException;

    /**
     * Writes a short value.
     * 
     * @param value
     *            the short value to write
     * @exception IOException
     *                on an IO error
     */
    public void writeShort(short value) throws IOException;

    /**
     * Writes a int value.
     * 
     * @param value
     *            the int value to write
     * @exception IOException
     *                on an IO error
     */
    public void writeInt(int value) throws IOException;

    /**
     * Writes a long value.
     * 
     * @param value
     *            the long value to write
     * @exception IOException
     *                on an IO error
     */
    public void writeLong(long value) throws IOException;

    /**
     * Writes a float value.
     * 
     * @param value
     *            the float value to write
     * @exception IOException
     *                on an IO error
     */
    public void writeFloat(float value) throws IOException;

    /**
     * Writes a double value.
     * 
     * @param value
     *            the double value to write
     * @exception IOException
     *                on an IO error
     */
    public void writeDouble(double value) throws IOException;

    /**
     * Writes (a slice of) an array of booleans.
     * 
     * @param source
     *            the array to write
     * @param offset
     *            the offset at which to start
     * @param length
     *            the number of elements to be copied
     * @exception IOException
     *                on an IO error
     */
    public void writeArray(boolean[] source, int offset, int length)
            throws IOException;

    /**
     * Writes (a slice of) an array of bytes.
     * 
     * @param source
     *            the array to write
     * @param offset
     *            the offset at which to start
     * @param length
     *            the number of elements to be copied
     * @exception IOException
     *                on an IO error
     */
    public void writeArray(byte[] source, int offset, int length)
            throws IOException;

    /**
     * Writes (a slice of) an array of characters.
     * 
     * @param source
     *            the array to write
     * @param offset
     *            the offset at which to start
     * @param length
     *            the number of elements to be copied
     * @exception IOException
     *                on an IO error
     */
    public void writeArray(char[] source, int offset, int length)
            throws IOException;

    /**
     * Writes (a slice of) an array of shorts.
     * 
     * @param source
     *            the array to write
     * @param offset
     *            the offset at which to start
     * @param length
     *            the number of elements to be copied
     * @exception IOException
     *                on an IO error
     */
    public void writeArray(short[] source, int offset, int length)
            throws IOException;

    /**
     * Writes (a slice of) an array of integers.
     * 
     * @param source
     *            the array to write
     * @param offset
     *            the offset at which to start
     * @param length
     *            the number of elements to be copied
     * @exception IOException
     *                on an IO error
     */
    public void writeArray(int[] source, int offset, int length)
            throws IOException;

    /**
     * Writes (a slice of) an array of longs.
     * 
     * @param source
     *            the array to write
     * @param offset
     *            the offset at which to start
     * @param length
     *            the number of elements to be copied
     * @exception IOException
     *                on an IO error
     */
    public void writeArray(long[] source, int offset, int length)
            throws IOException;

    /**
     * Writes (a slice of) an array of floats.
     * 
     * @param source
     *            the array to write
     * @param offset
     *            the offset at which to start
     * @param length
     *            the number of elements to be copied
     * @exception IOException
     *                on an IO error
     */
    public void writeArray(float[] source, int offset, int length)
            throws IOException;

    /**
     * Writes (a slice of) an array of doubles.
     * 
     * @param source
     *            the array to write
     * @param offset
     *            the offset at which to start
     * @param length
     *            the number of elements to be copied
     * @exception IOException
     *                on an IO error
     */
    public void writeArray(double[] source, int offset, int length)
            throws IOException;

    /**
     * Writes an array of booleans.
     * 
     * @param source
     *            the array to write
     * @exception IOException
     *                on an IO error
     */
    public void writeArray(boolean[] source) throws IOException;

    /**
     * Writes an array of bytes.
     * 
     * @param source
     *            the array to write
     * @exception IOException
     *                on an IO error
     */
    public void writeArray(byte[] source) throws IOException;

    /**
     * Writes an array of characters.
     * 
     * @param source
     *            the array to write
     * @exception IOException
     *                on an IO error
     */
    public void writeArray(char[] source) throws IOException;

    /**
     * Writes an array of shorts.
     * 
     * @param source
     *            the array to write
     * @exception IOException
     *                on an IO error
     */
    public void writeArray(short[] source) throws IOException;

    /**
     * Writes an array of integers.
     * 
     * @param source
     *            the array to write
     * @exception IOException
     *                on an IO error
     */
    public void writeArray(int[] source) throws IOException;

    /**
     * Writes an array of longs.
     * 
     * @param source
     *            the array to write
     * @exception IOException
     *                on an IO error
     */
    public void writeArray(long[] source) throws IOException;

    /**
     * Writes an array of floats.
     * 
     * @param source
     *            the array to write
     * @exception IOException
     *                on an IO error
     */
    public void writeArray(float[] source) throws IOException;

    /**
     * Writes an array of doubles.
     * 
     * @param source
     *            the array to write
     * @exception IOException
     *                on an IO error
     */
    public void writeArray(double[] source) throws IOException;

    /**
     * Writes the contents of the byte buffer (between its current position and
     * its limit).
     * 
     * @param value
     *            the byte buffer from which data is to be written
     * @exception IOException
     *                an error occurred
     */
    public void writeByteBuffer(ByteBuffer value) throws IOException;
}
