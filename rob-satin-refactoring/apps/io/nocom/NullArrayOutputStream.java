/* $Id$ */


import java.io.IOException;

import ibis.io.DataOutputStream;

final class NullArrayOutputStream extends DataOutputStream {

    long len = 0;

    public final long getAndReset() {
        long temp = len;
        len = 0;
        return temp;
    }

    public final void writeArray(boolean[] a, int off, int len)
            throws IOException {
        this.len += len;
    }

    public final void write(int i) {
        this.len += 1;
    }

    public final void writeByte(byte i) {
        this.len += 1;
    }

    public final void writeBoolean(boolean i) {
        this.len += 1;
    }

    public final void writeChar(char i) {
        this.len += 2;
    }

    public final void writeShort(short i) {
        this.len += 2;
    }

    public final void writeInt(int i) {
        this.len += 4;
    }

    public final void writeFloat(float i) {
        this.len += 4;
    }

    public final void writeDouble(double i) {
        this.len += 8;
    }

    public final void writeLong(long i) {
        this.len += 8;
    }

    public final void write(byte[] a) {
        this.len += a.length;
    }

    public final void write(byte[] a, int off, int len) {
        this.len += len;
    }

    public final void writeArray(byte[] a, int off, int len) throws IOException {
        this.len += len;
    }

    public final void writeArray(short[] a, int off, int len)
            throws IOException {
        this.len += 2 * len;
    }

    public final void writeArray(char[] a, int off, int len) throws IOException {
        this.len += 2 * len;
    }

    public final void writeArray(int[] a, int off, int len) throws IOException {
        this.len += 4 * len;
    }

    public final void writeArray(long[] a, int off, int len) throws IOException {
        this.len += 8 * len;
    }

    public final void writeArray(float[] a, int off, int len)
            throws IOException {
        this.len += 4 * len;
    }

    public final void writeArray(double[] a, int off, int len)
            throws IOException {
        this.len += 8 * len;
    }

    public final void flush() throws IOException {
    }

    public final void finish() throws IOException {
    }

    public final boolean finished() {
        return true;
    }

    public final void close() throws IOException {
    }

    public final void resetBytesWritten() {
        len = 0;
    }

    public final long bytesWritten() {
        return len;
    }
}
