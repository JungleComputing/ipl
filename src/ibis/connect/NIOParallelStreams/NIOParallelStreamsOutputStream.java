/*
 * Created on Feb 14, 2005
 */
package ibis.connect.NIOParallelStreams;

import java.io.IOException;
import java.io.OutputStream;

class NIOParallelStreamsOutputStream extends OutputStream {
    private final NIOParallelStreamsSocket socket;

    byte[] tmp = new byte[1];

    public NIOParallelStreamsOutputStream(NIOParallelStreamsSocket socket) {
        super();
        this.socket = socket;
    }

    public void write(int v) throws IOException {
        tmp[0] = (byte) v;
        this.write(tmp);
    }

    public void write(byte[] b) throws IOException {
        this.write(b, 0, b.length);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        if (len == 0)
            return; // sometimes we get 0 writes from serialization
        this.socket.send(b, off, len);
    }

    public void flush() throws IOException {
        socket.flush();
    }

    public void close() throws IOException {
        this.socket.close();
    }
}