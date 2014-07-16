package ibis.ipl.impl.tcp;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class PInputStream extends InputStream {

    private InputStream[] streams;
    private int length;
    private int currentStream;
    private int currentOffset;
    private byte[] tmp = new byte[1];

    public PInputStream(Socket[] sockets) throws IOException {
        streams = new InputStream[sockets.length];
        for (int i = 0; i < sockets.length; i++) {
            streams[i] = sockets[i].getInputStream();
        }
        length = 8192; // Make configurable later.
    }

    @Override
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

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int nextAvail = 0;
        int done = 0;
        do {
            int nextRead = Math.min(len, length - currentOffset);
            int rc = 0;
            rc = streams[currentStream].read(b, off, nextRead);
            if (rc < 0) {
                if (done == 0) {
                    return -1;
                }
                break;
            }
            done += rc;
            off += rc;
            len -= rc;
            currentOffset = (currentOffset + rc) % length;
            if (currentOffset == 0) {
                currentStream = (currentStream + 1) % streams.length;
            }
            nextAvail = streams[currentStream].available();
            if (rc < nextRead || nextAvail == 0) {
                break;
            }
            // } while(nextAvail > 0 && len > 0);
        } while (len > 0);
        return done;
    }

    @Override
    public int available() throws IOException {
        int rc = 0;
        for (InputStream in : streams) {
            rc += in.available();
        }
        return rc;
    }

    @Override
    public void close() throws IOException {
        if (streams != null) {
            try {
                for (InputStream i : streams) {
                    i.close();
                }
            } finally {
                streams = null;
            }
        }
    }
}
