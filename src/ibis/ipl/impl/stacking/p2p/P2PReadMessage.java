package ibis.ipl.impl.stacking.p2p;

import java.io.IOException;

import ibis.io.SerializationFactory;
import ibis.io.SerializationInput;
import ibis.io.SingleBufferArrayInputStream;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.impl.smartsockets.SmartSocketsUltraLightReceivePort;

public class P2PReadMessage implements ReadMessage {

	private final SerializationInput in;

	private final SingleBufferArrayInputStream bin;

	private boolean isFinished = false;

	private boolean inUpcall = false;

	private boolean finishCalledFromUpcall = false;

	private final SendPortIdentifier origin;

	private final P2PReceivePort port;

	public P2PReadMessage(P2PReceivePort port, 
            SendPortIdentifier origin, byte [] data) throws IOException {
		this.origin = origin;
		this.port = port;

		PortType type = port.getPortType();

		String serialization = null;

		if (type.hasCapability(PortType.SERIALIZATION_DATA)) {
			serialization = "data";
		} else if (type.hasCapability(PortType.SERIALIZATION_OBJECT_SUN)) {
			serialization = "sun";
		} else if (type.hasCapability(PortType.SERIALIZATION_OBJECT_IBIS)) {
			serialization = "ibis";
		} else if (type.hasCapability(PortType.SERIALIZATION_OBJECT)) {
			serialization = "object";
		} else {
			serialization = "byte";
		}

		bin = new SingleBufferArrayInputStream(data);
		in = SerializationFactory.createSerializationInput(serialization, bin);
	}

	@Override
	public long bytesRead() throws IOException {
		return bin.bytesRead();
	}

	public void setInUpcall(boolean val) {
        inUpcall = val;
    }

    /**
     * May be called by an implementation to allow for detection of finish()
     * calls within an upcall.
     */
    public boolean getInUpcall() {
        return inUpcall;
    }

    public boolean finishCalledInUpcall() {
        return finishCalledFromUpcall;
    }

    public boolean isFinished() { 
        return isFinished;
    }

    public long finish() throws IOException {

        if (isFinished) {
            throw new IOException(
                    "Operating on a message that was already finished");
        }

        isFinished = true;

        if (inUpcall) {
            finishCalledFromUpcall = true;
            port.newUpcallThread();
        }

        return bin.bytesRead();
    }

    public void finish(IOException e) {

        if (isFinished) {
            return;
        }

        isFinished = true;

        if (inUpcall) {
            finishCalledFromUpcall = true;
            port.newUpcallThread();
        }
    }

	@Override
	public ReceivePort localPort() {
		return port;
	}

	@Override
	public SendPortIdentifier origin() {
		return origin;
	}

	@Override
	public void readArray(boolean[] destination) throws IOException {
		in.readArray(destination);
	}

	@Override
	public void readArray(byte[] destination) throws IOException {
		in.readArray(destination);
	}

	@Override
	public void readArray(char[] destination) throws IOException {
		in.readArray(destination);
	}

	@Override
	public void readArray(short[] destination) throws IOException {
		in.readArray(destination);
	}

	@Override
	public void readArray(int[] destination) throws IOException {
		in.readArray(destination);
	}

	@Override
	public void readArray(long[] destination) throws IOException {
		in.readArray(destination);
	}

	@Override
	public void readArray(float[] destination) throws IOException {
		in.readArray(destination);
	}

	@Override
	public void readArray(double[] destination) throws IOException {
		in.readArray(destination);
	}

	@Override
	public void readArray(Object[] destination) throws IOException,
			ClassNotFoundException {
		in.readArray(destination);

	}

	@Override
	public void readArray(boolean[] destination, int offset, int size)
			throws IOException {
		in.readArray(destination, offset, size);
	}

	@Override
	public void readArray(byte[] destination, int offset, int size)
			throws IOException {
		in.readArray(destination, offset, size);
	}

	@Override
	public void readArray(char[] destination, int offset, int size)
			throws IOException {
		in.readArray(destination, offset, size);
	}

	@Override
	public void readArray(short[] destination, int offset, int size)
			throws IOException {
		in.readArray(destination, offset, size);
	}

	@Override
	public void readArray(int[] destination, int offset, int size)
			throws IOException {
		in.readArray(destination, offset, size);
	}

	@Override
	public void readArray(long[] destination, int offset, int size)
			throws IOException {
		in.readArray(destination, offset, size);
	}

	@Override
	public void readArray(float[] destination, int offset, int size)
			throws IOException {
		in.readArray(destination, offset, size);
	}

	@Override
	public void readArray(double[] destination, int offset, int size)
			throws IOException {
		in.readArray(destination, offset, size);
	}

	@Override
	public void readArray(Object[] destination, int offset, int size)
			throws IOException, ClassNotFoundException {
		in.readArray(destination, offset, size);
	}

	@Override
	public boolean readBoolean() throws IOException {
		return in.readBoolean();
	}

	@Override
	public byte readByte() throws IOException {
		return in.readByte();
	}

	@Override
	public char readChar() throws IOException {
		return in.readChar();
	}

	@Override
	public double readDouble() throws IOException {
		return in.readDouble();
	}

	@Override
	public float readFloat() throws IOException {
		return in.readFloat();
	}

	@Override
	public int readInt() throws IOException {
		return in.readInt();
	}

	@Override
	public long readLong() throws IOException {
		return in.readLong();
	}

	@Override
	public Object readObject() throws IOException, ClassNotFoundException {
		return in.readObject();
	}

	@Override
	public short readShort() throws IOException {
		return in.readShort();
	}

	@Override
	public String readString() throws IOException {
		return in.readString();
	}

	@Override
	public int remaining() throws IOException {
		return bin.available();
	}

	@Override
	public long sequenceNumber() {
		return 0;
	}

	@Override
	public int size() throws IOException {
		return bin.size();
	}

}
