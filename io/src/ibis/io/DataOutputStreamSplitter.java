/* $Id$ */

package ibis.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Contract: write to multiple outputstreams.
 * when an exception occurs, store it and continue.
 * when the data is written to all streams, throw one large exception
 * that contains all previous exceptions.
 * This way, even when one of the streams dies, the rest will receive the data.
 **/
public final class DataOutputStreamSplitter extends DataOutputStream {

    private boolean removeOnException = false;

    ArrayList<DataOutputStream> out = new ArrayList<DataOutputStream>();

    int bytesWritten = 0;
    
    public DataOutputStreamSplitter() {
        // empty constructor
    }

    public DataOutputStreamSplitter(boolean removeOnException) {
        this();
        this.removeOnException = removeOnException;
    }

    public void add(DataOutputStream s) {
        out.add(s);
    }

    public void remove(DataOutputStream s) {
        int i = out.indexOf(s);
        if (i == -1) {
            throw new Error("Removing unknown stream from splitter.");
        }

        out.remove(i);
    }

    private SplitterException handleException(SplitterException e,
            IOException newException, int pos) {
        if (e == null) {
            e = new SplitterException();
        }
        e.add(out.get(pos), newException);
        if (removeOnException) {
            out.remove(pos);
        }
        return e;
    }

    public void write(int b) throws IOException {
        SplitterException e = null;
        
        bytesWritten++;
        
        for (int i = 0; i < out.size(); i++) {
            try {
                out.get(i).write(b);
            } catch (IOException e2) {
                e = handleException(e, e2, i);
                if (removeOnException)
                    i--;
            }
        }

        if (e != null)
            throw e;
    }

    public void write(byte[] b) throws IOException {
        SplitterException e = null;
        bytesWritten += b.length;
        for (int i = 0; i < out.size(); i++) {
            try {
                out.get(i).write(b);
            } catch (IOException e2) {
                e = handleException(e, e2, i);
                if (removeOnException) {
                    i--;
                }
            }
        }

        if (e != null) {
            throw e;
        }
    }

    public void write(byte[] b, int off, int len) throws IOException {
        SplitterException e = null;
        bytesWritten += len;
        for (int i = 0; i < out.size(); i++) {
            try {
                out.get(i).write(b, off, len);
            } catch (IOException e2) {
                e = handleException(e, e2, i);
                if (removeOnException) {
                    i--;
                }
            }
        }

        if (e != null) {
            throw e;
        }
    }

    public void flush() throws IOException {
        SplitterException e = null;

        for (int i = 0; i < out.size(); i++) {
            try {
                out.get(i).flush();
            } catch (IOException e2) {
                e = handleException(e, e2, i);
                if (removeOnException) {
                    i--;
                }
            }
        }

        if (e != null) {
            throw e;
        }
    }

    public void finish() throws IOException {
        SplitterException e = null;

        for (int i = 0; i < out.size(); i++) {
            try {
                out.get(i).finish();
            } catch (IOException e2) {
                e = handleException(e, e2, i);
                if (removeOnException) {
                    i--;
                }
            }
        }

        if (e != null) {
            throw e;
        }
    }

    public void close() throws IOException {
        SplitterException e = null;

        for (int i = 0; i < out.size(); i++) {
            try {
                out.get(i).close();
            } catch (IOException e2) {
                e = handleException(e, e2, i);
                if (removeOnException) {
                    i--;
                }
            }
        }

        if (e != null) {
            throw e;
        }
    }

    public boolean finished() throws IOException {
        SplitterException e = null;

        for (int i = 0; i < out.size(); i++) {
            try {
                if (! out.get(i).finished()) {
                    return false;
                }
            } catch (IOException e2) {
                e = handleException(e, e2, i);
                if (removeOnException) {
                    i--;
                }
            }
        }

        if (e != null) {
            throw e;
        }

        return true;
    }

    public long bytesWritten() {
        return bytesWritten;
    }

    public void resetBytesWritten() {
        bytesWritten = 0;
    }

    public void writeArray(boolean[] source, int offset, int length) throws IOException {
        SplitterException e = null;
        
        bytesWritten += length;
        
        for (int i = 0; i < out.size(); i++) {
            try {
                out.get(i).writeArray(source, offset, length);
            } catch (IOException e2) {
                e = handleException(e, e2, i);
                if (removeOnException)
                    i--;
            }
        }

        if (e != null)
            throw e;
    }
    
    public void writeByteBuffer(ByteBuffer b) throws IOException {
        SplitterException e = null;
        
        bytesWritten += b.limit() - b.position();
        for (int i = 0; i < out.size(); i++) {
            try {
                out.get(i).writeByteBuffer(b);
            } catch (IOException e2) {
                e = handleException(e, e2, i);
                if (removeOnException)
                    i--;
            }
        }

        if (e != null)
            throw e;
    }

    public void writeArray(byte[] source, int offset, int length) throws IOException {
        write(source, offset, length);
    }

    public void writeArray(char[] source, int offset, int length) throws IOException {
        SplitterException e = null;
        
        bytesWritten += length*2;
        
        for (int i = 0; i < out.size(); i++) {
            try {
                out.get(i).writeArray(source, offset, length);
            } catch (IOException e2) {
                e = handleException(e, e2, i);
                if (removeOnException)
                    i--;
            }
        }

        if (e != null)
            throw e;
    }

    public void writeArray(double[] source, int offset, int length) throws IOException {
        SplitterException e = null;
        
        bytesWritten += length*8;
        
        for (int i = 0; i < out.size(); i++) {
            try {
                out.get(i).writeArray(source, offset, length);
            } catch (IOException e2) {
                e = handleException(e, e2, i);
                if (removeOnException)
                    i--;
            }
        }

        if (e != null)
            throw e;
    }

    public void writeArray(float[] source, int offset, int length) throws IOException {
        SplitterException e = null;
        
        bytesWritten += length*4;
        
        for (int i = 0; i < out.size(); i++) {
            try {
                out.get(i).writeArray(source, offset, length);
            } catch (IOException e2) {
                e = handleException(e, e2, i);
                if (removeOnException)
                    i--;
            }
        }

        if (e != null)
            throw e;
    }

    public void writeArray(int[] source, int offset, int length) throws IOException {
        SplitterException e = null;
        
        bytesWritten += length*4;
        
        for (int i = 0; i < out.size(); i++) {
            try {
                out.get(i).writeArray(source, offset, length);
            } catch (IOException e2) {
                e = handleException(e, e2, i);
                if (removeOnException)
                    i--;
            }
        }

        if (e != null)
            throw e;
    }

    public void writeArray(long[] source, int offset, int length) throws IOException {
        SplitterException e = null;
        
        bytesWritten += length*8;
        
        for (int i = 0; i < out.size(); i++) {
            try {
                out.get(i).writeArray(source, offset, length);
            } catch (IOException e2) {
                e = handleException(e, e2, i);
                if (removeOnException)
                    i--;
            }
        }

        if (e != null)
            throw e;
    }

    public void writeArray(short[] source, int offset, int length) throws IOException {
        SplitterException e = null;
        
        bytesWritten += length*2;
        
        for (int i = 0; i < out.size(); i++) {
            try {
                out.get(i).writeArray(source, offset, length);
            } catch (IOException e2) {
                e = handleException(e, e2, i);
                if (removeOnException)
                    i--;
            }
        }

        if (e != null)
            throw e;
    }

    public void writeBoolean(boolean value) throws IOException {
        SplitterException e = null;
        
        bytesWritten += 1;
        
        for (int i = 0; i < out.size(); i++) {
            try {
                out.get(i).writeBoolean(value);
            } catch (IOException e2) {
                e = handleException(e, e2, i);
                if (removeOnException)
                    i--;
            }
        }

        if (e != null)
            throw e;
    }

    public void writeByte(byte value) throws IOException {
        SplitterException e = null;
        
        bytesWritten += 1;
        
        for (int i = 0; i < out.size(); i++) {
            try {
                out.get(i).writeByte(value);
            } catch (IOException e2) {
                e = handleException(e, e2, i);
                if (removeOnException)
                    i--;
            }
        }

        if (e != null)
            throw e;
    }

    public void writeChar(char value) throws IOException {
        SplitterException e = null;
        
        bytesWritten += 2;
        
        for (int i = 0; i < out.size(); i++) {
            try {
                out.get(i).writeChar(value);
            } catch (IOException e2) {
                e = handleException(e, e2, i);
                if (removeOnException)
                    i--;
            }
        }

        if (e != null)
            throw e;
    }

    public void writeDouble(double value) throws IOException {
        SplitterException e = null;
        
        bytesWritten += 8;
        
        for (int i = 0; i < out.size(); i++) {
            try {
                out.get(i).writeDouble(value);
            } catch (IOException e2) {
                e = handleException(e, e2, i);
                if (removeOnException)
                    i--;
            }
        }

        if (e != null)
            throw e;
    }

    public void writeFloat(float value) throws IOException {
        SplitterException e = null;
        
        bytesWritten += 4;
        
        for (int i = 0; i < out.size(); i++) {
            try {
                out.get(i).writeFloat(value);
            } catch (IOException e2) {
                e = handleException(e, e2, i);
                if (removeOnException)
                    i--;
            }
        }

        if (e != null)
            throw e;
    }

    public void writeInt(int value) throws IOException {
        SplitterException e = null;
        
        bytesWritten += 4;
        
        for (int i = 0; i < out.size(); i++) {
            try {
                out.get(i).writeInt(value);
            } catch (IOException e2) {
                e = handleException(e, e2, i);
                if (removeOnException)
                    i--;
            }
        }

        if (e != null)
            throw e;
    }

    public void writeLong(long value) throws IOException {
        SplitterException e = null;
        
        bytesWritten += 8;
        
        for (int i = 0; i < out.size(); i++) {
            try {
                out.get(i).writeLong(value);
            } catch (IOException e2) {
                e = handleException(e, e2, i);
                if (removeOnException)
                    i--;
            }
        }

        if (e != null)
            throw e;
    }

    public void writeShort(short value) throws IOException {
        SplitterException e = null;
        
        bytesWritten += 2;
        
        for (int i = 0; i < out.size(); i++) {
            try {
                out.get(i).writeShort(value);
            } catch (IOException e2) {
                e = handleException(e, e2, i);
                if (removeOnException)
                    i--;
            }
        }

        if (e != null)
            throw e;
    }
    
    public int bufferSize() {
        int min = Integer.MAX_VALUE;
        for (DataOutputStream o : out) {
            int sz = o.bufferSize();
            if (sz < min) {
                min = sz;
            }
        }
        return min;
    }
}
