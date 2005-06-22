/*
 * Created on Feb 14, 2005
 */
package ibis.connect.NIOParallelStreams;

import java.io.IOException;
import java.io.InputStream;

class NIOParallelStreamsInputStream extends InputStream {
    private final NIOParallelStreamsSocket socket;

    byte[] tmp = new byte[1];

    public NIOParallelStreamsInputStream(NIOParallelStreamsSocket socket) {
        super();
        this.socket = socket;
    }

    public int read(byte[] b) throws IOException {
        return this.read(b, 0, b.length);
    }

    public int read(byte[] b, int off, int len) throws IOException {
        return this.socket.recv(b, off, len);
    }

    public int read() throws IOException {
        int rc = 0;
        while (rc == 0) {
            rc = this.read(tmp);
        }
        if (rc == -1) {
            return -1;
        }
        return tmp[0] & 255;
    }

    public int available() throws IOException {
        return this.socket.available();
    }

    public void close() throws IOException {
        this.socket.close();
    }
}