package ibis.impl.messagePassing;

import java.io.IOException;

public class InputStream extends java.io.InputStream {

    ByteInputStream in;

    InputStream(ByteInputStream in) {
	this.in = in;
    }

    public int read() throws IOException {
	return in.read();
    }

    public int read(byte[] b) throws IOException {
	return in.read(b);
    }

    public int read(byte[] b, int off, int len) throws IOException {
	return in.read(b, off, len);
    }

    public int available() throws IOException {
	return in.available();
    }

}
