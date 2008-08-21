package ibis.ipl.impl.smartsockets;

import java.io.IOException;

import ibis.io.SerializationFactory;
import ibis.io.SerializationInput;
import ibis.io.SingleBufferArrayInputStream;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPortIdentifier;

public final class SmartSocketsUltraLightReadMessage implements ReadMessage {

	private final SerializationInput in;

	private final SingleBufferArrayInputStream bin;

	private boolean isFinished = false;

	private boolean inUpcall = false;

	private boolean finishCalledFromUpcall = false;
	    
	private final SendPortIdentifier origin;
	
	private final SmartSocketsUltraLightReceivePort port;
	
	SmartSocketsUltraLightReadMessage(SmartSocketsUltraLightReceivePort port, 
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
		
	public long bytesRead() throws IOException {
		return bin.bytesRead();
	}

	 /**
     * May be called by an implementation to allow for detection of finish()
     * calls within an upcall.
     * @param val the value to set.
     */
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


	public ReceivePort localPort() {
		return port;
	}

	public SendPortIdentifier origin() {
		return origin;
	}

	public void readArray(boolean[] destination) throws IOException {
		in.readArray(destination);
	}

	public void readArray(byte[] destination) throws IOException {
		in.readArray(destination);
	}

	public void readArray(char[] destination) throws IOException {
		in.readArray(destination);
	}

	public void readArray(short[] destination) throws IOException {
		in.readArray(destination);
	}

	public void readArray(int[] destination) throws IOException {
		in.readArray(destination);
	}

	public void readArray(long[] destination) throws IOException {
		in.readArray(destination);
	}

	public void readArray(float[] destination) throws IOException {
		in.readArray(destination);
	}

	public void readArray(double[] destination) throws IOException {
		in.readArray(destination);		
	}

	public void readArray(Object[] destination) throws IOException, ClassNotFoundException {
		in.readArray(destination);		
	}

	public void readArray(boolean[] destination, int offset, int size) throws IOException {
		in.readArray(destination, offset, size);		
	}

	public void readArray(byte[] destination, int offset, int size) throws IOException {
		in.readArray(destination, offset, size);		
	}

	public void readArray(char[] destination, int offset, int size) throws IOException {
		in.readArray(destination, offset, size);		
	}

	public void readArray(short[] destination, int offset, int size) throws IOException {
		in.readArray(destination, offset, size);		
	}

	public void readArray(int[] destination, int offset, int size) throws IOException {
		in.readArray(destination, offset, size);		
	}

	public void readArray(long[] destination, int offset, int size) throws IOException {
		in.readArray(destination, offset, size);		
	}

	public void readArray(float[] destination, int offset, int size) throws IOException {
		in.readArray(destination, offset, size);		
	}

	public void readArray(double[] destination, int offset, int size) throws IOException {
		in.readArray(destination, offset, size);		
	}

	public void readArray(Object[] destination, int offset, int size) throws IOException, ClassNotFoundException {
		in.readArray(destination, offset, size);		
	}

	public boolean readBoolean() throws IOException {
		return in.readBoolean();
	}

	public byte readByte() throws IOException {
		return in.readByte();
	}

	public char readChar() throws IOException {
		return in.readChar();
	}

	public double readDouble() throws IOException {
		return in.readDouble();
	}

	public float readFloat() throws IOException {
		return in.readFloat();
	}

	public int readInt() throws IOException {
		return in.readInt();
	}

	public long readLong() throws IOException {
		return in.readLong();
	}

	public Object readObject() throws IOException, ClassNotFoundException {
		return in.readObject();
	}

	public short readShort() throws IOException {
		return in.readShort();
	}

	public String readString() throws IOException {
		return in.readString();
	}

	public long sequenceNumber() {
		return 0;
	}
}
