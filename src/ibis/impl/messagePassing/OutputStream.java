package ibis.impl.messagePassing;

import java.io.IOException;

public class OutputStream extends java.io.OutputStream {

    private ByteOutputStream out;

    OutputStream(ByteOutputStream out) {
	this.out = out;
    }

    public void write(int b) throws IOException {
	out.write(b);
    }

    public void write(byte[] b) throws IOException {
	out.write(b);
    }

    public void write(byte[] b, int off, int len) throws IOException {
	out.write(b, off, len);
    }

    public void flush() throws IOException {
	out.flush();
    }

}
