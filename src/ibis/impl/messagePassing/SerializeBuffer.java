package ibis.ipl.impl.messagePassing;

import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

    class InputBuffer extends java.io.InputStream {

	private byte[] buffer;
	private int offset;

	InputBuffer(byte[] buffer) {
	    this.buffer = buffer;
	    offset = 0;
	}

	public int read(byte[] b) throws IOException {
	    return read(b, 0, b.length);
	}

	public int read(byte[] b, int off, int len) {
	    System.arraycopy(buffer, offset, b, off, len);
	    offset += len;

	    return len;
	}

	public int read() throws IOException {
	    byte x = buffer[offset];
	    offset++;

	    return (int)x;
	}
    }


    class SerializeInputBuffer {

	InputBuffer in;
	ObjectInputStream objIn;

	SerializeInputBuffer(byte[] data) throws IOException {
	    in = new InputBuffer(data);
	    objIn = new ObjectInputStream(in);
	}
    }


    class OutputBuffer extends java.io.OutputStream {

	private byte[] buffer;
	private int size;
	private int alloc;

	private final static int ALLOC = 1024;

	void grow(int needed) {
	    if (size + needed <= alloc) {
		return;
	    }

	    alloc = size + needed + ALLOC;
	    byte[] newBuffer = new byte[alloc];
	    if (size > 0) {
		System.arraycopy(buffer, 0, newBuffer, 0, size);
	    }

	    buffer = newBuffer;
	}

	public void write(byte[] b) {
	    write(b, 0, b.length);
	}

	public void write(byte[] b, int off, int len) {
	    grow(len);
	    System.arraycopy(b, off, buffer, size, len);
	    size += len;
	}

	public void write(int b) throws IOException {
	    grow(1);
	    buffer[size] = (byte)b;
	    size++;
	}

	byte[] getData() {
	    byte[] data = new byte[size];
	    System.arraycopy(buffer, 0, data, 0, size);
	    return data;
	}
    }


    class SerializeOutputBuffer {

	OutputBuffer out;
	ObjectOutputStream objOut;

	SerializeOutputBuffer() throws IOException {
	    out = new OutputBuffer();
	    objOut = new ObjectOutputStream(out);
	}

	byte[] getData() {
	    return out.getData();
	}
    }


class SerializeBuffer {

    static byte[] writeObject(Serializable obj) throws IOException {
	SerializeOutputBuffer addrBuffer = new SerializeOutputBuffer();
	addrBuffer.objOut.writeObject(obj);
	addrBuffer.objOut.flush();
	addrBuffer.objOut.close();

	return addrBuffer.getData();
    }


    static Serializable readObject(byte[] serialForm)
	    throws IOException, ClassNotFoundException {
	SerializeInputBuffer addrBuffer = new SerializeInputBuffer(serialForm);
	Serializable obj = (Serializable)addrBuffer.objIn.readObject();
	addrBuffer.objIn.close();

	return obj;
    }

}
