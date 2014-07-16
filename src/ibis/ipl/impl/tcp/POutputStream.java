package ibis.ipl.impl.tcp;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class POutputStream extends OutputStream {

    private OutputStream[] streams;
    private int length;
    private int currentStream;
    private int currentOffset;
    private byte[] tmp = new byte[1];

    public POutputStream(Socket[] sockets) throws IOException {
        streams = new OutputStream[sockets.length];
        for (int i = 0; i < sockets.length; i++) {
            streams[i] = sockets[i].getOutputStream();
        }
        length = 8192; // Make configurable later.
    }

    @Override
    public void write(int v) throws IOException {
        tmp[0] = (byte) v;
        this.write(tmp);
    }

    @Override
    public void write(byte[] b) throws IOException {
        this.write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        // System.out.println("Write: " + len + " bytes, currentStream = " + currentStream + ", currentOffset = " + currentOffset);
        while (len > 0) {
            int l = Math.min(len, length - currentOffset);
            streams[currentStream].write(b, off, l);
            off += l;
            len -= l;
            currentOffset = (currentOffset + l) % length;
            if (currentOffset == 0) {
                currentStream = (currentStream + 1) % streams.length;
            }
        }
    }

    @Override
    public void flush() {
        // no flush needed
    }

    @Override
    public void close() throws IOException {
        if (streams != null) {
            try {
                for (OutputStream o : streams) {
                    o.close();
                }
            } finally {
                streams = null;
            }
        }
    }
}
