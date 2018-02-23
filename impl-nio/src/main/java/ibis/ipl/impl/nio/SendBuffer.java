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

package ibis.ipl.impl.nio;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SendBuffer implements Config {

    // primitives are send in order of size, largest first
    private static final int HEADER = 0;

    private static final int LONGS = 1;

    private static final int DOUBLES = 2;

    private static final int INTS = 3;

    private static final int FLOATS = 4;

    private static final int SHORTS = 5;

    private static final int CHARS = 6;

    private static final int BYTES = 7;

    private static final int PADDING = 8;

    private static final int NR_OF_BUFFERS = 9;

    /**
     * The header contains 1 byte for the byte order, one byte indicating the
     * length of the padding at the end of the packet (in bytes), and 7 shorts
     * (14 bytes) for the number of each primitive send (in bytes!)
     */
    private static final int SIZEOF_HEADER = 16;

    private static final int SIZEOF_PADDING = 8;

    public static final int SIZEOF_BYTE = 1;

    public static final int SIZEOF_CHAR = 2;

    public static final int SIZEOF_SHORT = 2;

    public static final int SIZEOF_INT = 4;

    public static final int SIZEOF_LONG = 8;

    public static final int SIZEOF_FLOAT = 4;

    public static final int SIZEOF_DOUBLE = 8;

    static final int BUFFER_CACHE_SIZE = 128;

    static SendBuffer[] cache = new SendBuffer[BUFFER_CACHE_SIZE];

    static int cacheSize = 0;

    private static Logger logger = LoggerFactory.getLogger(SendBuffer.class);

    /**
     * Static method to get a sendbuffer out of the cache
     */
    synchronized static SendBuffer get() {
        if (cacheSize > 0) {
            if (logger.isDebugEnabled()) {
                logger.debug("SendBuffer: got empty buffer from cache");
            }
            cacheSize--;
            cache[cacheSize].clear();
            return cache[cacheSize];
        }
        if (logger.isDebugEnabled()) {
        	logger.debug("SendBuffer: got new empty buffer");
        }
        return new SendBuffer();
    }

    /**
     * static method to put a buffer in the cache
     */
    synchronized static void recycle(SendBuffer buffer) {
        if (buffer.parent == null) {
            if (buffer.copies != 0) {
                // throw new Error("tried to recycle buffer with children!");
                return;
            }
            if (cacheSize >= BUFFER_CACHE_SIZE) {
                if (logger.isDebugEnabled()) {
                    logger.debug("SendBuffer: cache full"
                            + " apon recycling buffer, throwing away");
                }
                return;
            }
            cache[cacheSize] = buffer;
            cacheSize++;
            if (logger.isDebugEnabled()) {
                logger.debug("SendBuffer: recycled buffer");
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("SendBuffer: recycling child buffer");
            }
            buffer.parent.copies--;
            if (buffer.parent.copies == 0) {
                if (cacheSize >= BUFFER_CACHE_SIZE) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("SendBuffer: cache full"
                                + " apon recycling parent of child buffer,"
                                + " throwing away");
                    }
                    return;
                }
                cache[cacheSize] = buffer.parent;
                cacheSize++;
                if (logger.isDebugEnabled()) {
                    logger.debug("SendBuffer: recycled parent buffer");
                }
            }
        }
    }

    /**
     * create copies of a buffer, records how may copies are made so far
     */
    synchronized static SendBuffer[] replicate(SendBuffer original, int copies) {
        SendBuffer[] result = new SendBuffer[copies];

        for (int i = 0; i < copies; i++) {
            result[i] = new SendBuffer(original);
        }
        original.copies += copies;

        return result;
    }

    // number of copies that exist of this buffer
    private int copies = 0;

    // original buffer this buffer is a copy of (if applicable)
    SendBuffer parent = null;

    private static long nextSequenceNr = 0;

    ShortBuffer header;

    LongBuffer longs;

    DoubleBuffer doubles;

    IntBuffer ints;

    FloatBuffer floats;

    ShortBuffer shorts;

    CharBuffer chars;

    ByteBuffer bytes;

    ByteBuffer[] byteBuffers;

    /**
     * Used to keep track of a buffer. Recycling a buffer will reset its
     * sequence number.
     */
    long sequenceNr;

    SendBuffer() {
        ByteOrder order = ByteOrder.nativeOrder();

        byteBuffers = new ByteBuffer[NR_OF_BUFFERS];
        byteBuffers[HEADER] = ByteBuffer.allocateDirect(SIZEOF_HEADER).order(
                order);
        byteBuffers[PADDING] = ByteBuffer.allocateDirect(SIZEOF_PADDING).order(
                order);

        // put the byte order in the first byte of the header
        if (order == ByteOrder.BIG_ENDIAN) {
            byteBuffers[HEADER].put(0, (byte) 1);
        } else {
            byteBuffers[HEADER].put(0, (byte) 0);
        }

        for (int i = 1; i < (NR_OF_BUFFERS - 1); i++) {
            byteBuffers[i] = ByteBuffer.allocateDirect(PRIMITIVE_BUFFER_SIZE)
                    .order(order);
        }

        header = byteBuffers[HEADER].asShortBuffer();
        longs = byteBuffers[LONGS].asLongBuffer();
        doubles = byteBuffers[DOUBLES].asDoubleBuffer();
        ints = byteBuffers[INTS].asIntBuffer();
        floats = byteBuffers[FLOATS].asFloatBuffer();
        shorts = byteBuffers[SHORTS].asShortBuffer();
        chars = byteBuffers[CHARS].asCharBuffer();
        bytes = byteBuffers[BYTES].duplicate();

        clear();
    }

    /**
     * Copy constructor. Acutally only copies byteBuffers;
     */
    SendBuffer(SendBuffer parent) {
        this.parent = parent;

        byteBuffers = new ByteBuffer[NR_OF_BUFFERS];
        for (int i = 0; i < NR_OF_BUFFERS; i++) {
            byteBuffers[i] = parent.byteBuffers[i].duplicate();
        }
    }

    /**
     * 
     * 
     * /** Resets a buffer as though it's a newly created buffer. Sets the
     * sequencenr to a new value
     */
    void clear() {
        // header.clear();
        longs.clear();
        doubles.clear();
        ints.clear();
        floats.clear();
        shorts.clear();
        chars.clear();
        bytes.clear();

        parent = null;
        copies = 0;

        // FIXME: this isn't thread safe
        sequenceNr = nextSequenceNr;
        nextSequenceNr++;
    }

    /**
     * Make a (partially) filled buffer ready for sending
     */
    void flip() {
        int paddingLength;

        // fill header with the size of the primitive arrays (in bytes)
        short[] headerArray = new short[NR_OF_BUFFERS - 1];
        headerArray[LONGS] = (short) (longs.position() * SIZEOF_LONG);
        headerArray[DOUBLES] = (short) (doubles.position() * SIZEOF_DOUBLE);
        headerArray[INTS] = (short) (ints.position() * SIZEOF_INT);
        headerArray[FLOATS] = (short) (floats.position() * SIZEOF_FLOAT);
        headerArray[SHORTS] = (short) (shorts.position() * SIZEOF_SHORT);
        headerArray[CHARS] = (short) (chars.position() * SIZEOF_CHAR);
        headerArray[BYTES] = (short) (bytes.position() * SIZEOF_BYTE);
        header.clear();
        header.put(headerArray);

        byteBuffers[HEADER].position(0);
        byteBuffers[HEADER].limit(SIZEOF_HEADER);

        byteBuffers[LONGS].position(0);
        byteBuffers[LONGS].limit(headerArray[LONGS]);

        byteBuffers[DOUBLES].position(0);
        byteBuffers[DOUBLES].limit(headerArray[DOUBLES]);

        byteBuffers[INTS].position(0);
        byteBuffers[INTS].limit(headerArray[INTS]);

        byteBuffers[FLOATS].position(0);
        byteBuffers[FLOATS].limit(headerArray[FLOATS]);

        byteBuffers[SHORTS].position(0);
        byteBuffers[SHORTS].limit(headerArray[SHORTS]);

        byteBuffers[CHARS].position(0);
        byteBuffers[CHARS].limit(headerArray[CHARS]);

        byteBuffers[BYTES].position(0);
        byteBuffers[BYTES].limit(headerArray[BYTES]);

        // add padding to make the total nr of bytes send a multiple of eight

        // find out length of padding we need
        int totalLength = SIZEOF_HEADER + headerArray[LONGS]
                + headerArray[DOUBLES] + headerArray[INTS]
                + headerArray[FLOATS] + headerArray[SHORTS]
                + headerArray[CHARS] + headerArray[BYTES];

        paddingLength = (8 - (totalLength % 8));
        byteBuffers[PADDING].position(0).limit(paddingLength);

        // put a byte in the header indicating the length of the paddding
        byteBuffers[HEADER].put(1, (byte) paddingLength);

        if (logger.isDebugEnabled()) {
            logger.debug("flipping buffer, sending: l[" + longs.position()
                    + "] d[" + doubles.position() + "] i[" + ints.position()
                    + "] f[" + floats.position() + "] s[" + shorts.position()
                    + "] c[" + chars.position() + "] b[" + bytes.position()
                    + "] total size: " + remaining() + " padding size: "
                    + paddingLength);
        }
    }

    /**
     * set a mark on all Byte Buffers
     */
    void mark() {
        for (int i = 0; i < byteBuffers.length; i++) {
            byteBuffers[i].mark();
        }
    }

    /**
     * reset all Byte Buffers
     */
    void reset() {
        for (int i = 0; i < byteBuffers.length; i++) {
            byteBuffers[i].reset();
        }
    }

    /**
     * returns the number of remaining bytes in the bytebuffers
     */
    long remaining() {
        long result = 0;
        for (int i = 0; i < byteBuffers.length; i++) {
            result += byteBuffers[i].remaining();
        }
        return result;
    }

    /**
     * returns if this buffer is empty (before flipping)
     */
    boolean isEmpty() {
        return ((longs.position() == 0) && (doubles.position() == 0)
                && (ints.position() == 0) && (floats.position() == 0)
                && (shorts.position() == 0) && (chars.position() == 0) && (bytes
                .position() == 0));
    }

    /**
     * Returns if this buffer has any data remaining in it. Only works _after_
     * it has been flipped!
     */
    boolean hasRemaining() {
        return byteBuffers[PADDING].hasRemaining();
    }

}
