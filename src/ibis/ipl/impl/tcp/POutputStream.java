package ibis.ipl.impl.tcp;

import ibis.io.Conversion;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class POutputStream extends OutputStream {

    private Conversion conversion = Conversion.loadConversion(false);
    private OutputStream[] streams;
    private int currentStream;
    private byte[] tmp = new byte[1];
    private byte[] tmpInt = new byte[4];

    public POutputStream(Socket[] sockets) throws IOException {
	streams = new OutputStream[sockets.length];
	for (int i = 0; i < sockets.length; i++) {
	    streams[i] = sockets[i].getOutputStream();
	}
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
	conversion.int2byte(len, tmpInt, 0);

	streams[currentStream].write(tmpInt);
	streams[currentStream].write(b, off, len);

	currentStream = (currentStream + 1) % streams.length;
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
