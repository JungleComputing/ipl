/* $Id$ */


import java.io.*;

class CountingOutputStream extends OutputStream {

    private long bytes_written;

    private OutputStream out;

    public CountingOutputStream(OutputStream out) {
        this.out = out;
        bytes_written = 0;
    }

    public final void close() throws IOException {
        out.close();
    }

    public final void flush() throws IOException {
        out.flush();
    }

    public final void write(byte[] b) throws IOException {
        bytes_written += b.length;
        out.write(b);
    }

    public final void write(byte[] b, int off, int len) throws IOException {
        bytes_written += len;
        out.write(b, off, len);
    }

    public final void write(int b) throws IOException {
        bytes_written++;
        out.write(b);
    }

    public final long bytesWritten() {
        return bytes_written;
    }
}

