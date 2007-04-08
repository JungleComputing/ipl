/* $Id$ */

package ibis.mpj;
import ibis.util.io.DataOutputStream;

import java.io.IOException;

final class StoreArrayOutputStream extends DataOutputStream {

    StoreBuffer buf;

    public StoreArrayOutputStream(StoreBuffer buf) {
        this.buf = buf;
    }

    public int bufferSize() {
        // Let serialization stream decide/
        return -1;
    }       
        
    public void writeByte(byte b) {
        buf.writeByte(b);
    }

    public void writeBoolean(boolean b) {
        buf.writeBoolean(b);
    }

    public void writeChar(char b) {
        buf.writeChar(b);
    }

    public void writeShort(short b) {
        buf.writeShort(b);
    }

    public void writeInt(int b) {
        buf.writeInt(b);
    }

    public void writeFloat(float b) {
        buf.writeFloat(b);
    }

    public void writeLong(long b) {
        buf.writeLong(b);
    }

    public void writeDouble(double b) {
        buf.writeDouble(b);
    }

    public void write(int b) {
        buf.write(b);
    }

    public void write(byte[] b) {
        buf.write(b);
    }

    public void write(byte[] b, int off, int len) {
        buf.write(b, off, len);
    }

    public void writeArray(boolean[] a, int off, int len) throws IOException {
        buf.writeArray(a, off, len);
    }

    public void writeArray(byte[] a, int off, int len) throws IOException {
        buf.writeArray(a, off, len);
    }

    public void writeArray(short[] a, int off, int len) throws IOException {
        buf.writeArray(a, off, len);
    }

    public void writeArray(char[] a, int off, int len) throws IOException {
        buf.writeArray(a, off, len);
    }

    public void writeArray(int[] a, int off, int len) throws IOException {
        buf.writeArray(a, off, len);
    }

    public void writeArray(long[] a, int off, int len) throws IOException {
        buf.writeArray(a, off, len);
    }

    public void writeArray(float[] a, int off, int len) throws IOException {
        buf.writeArray(a, off, len);
    }

    public void writeArray(double[] a, int off, int len) throws IOException {
        buf.writeArray(a, off, len);
    }

    public void flush() throws IOException {
    	// nothing here
    }

    public boolean finished() {
        return true;
    }

    public void finish() throws IOException {
        flush();
    }

    public void close() throws IOException {
        flush();
    }

    public long bytesWritten() {
        return buf.bytesWritten();
    }

    public void resetBytesWritten() {
        buf.resetBytesWritten();
    }
}
