package ibis.impl.tcp;

import ibis.ipl.ReadMessage;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.ReceivePort;

import java.io.InputStream;
import java.io.IOException;
import ibis.io.SerializationInputStream;
import java.io.DataInputStream;

final class TcpReadMessage implements ReadMessage { 
	private SerializationInputStream in;
	private long sequenceNr = -1;
	private TcpReceivePort port;
	private TcpSendPortIdentifier origin;
	private Connectionhandler handler;
	boolean isFinished = false;

	TcpReadMessage(TcpReceivePort port, SerializationInputStream in, 
				       TcpSendPortIdentifier origin, Connectionhandler handler) {
		this.port = port;
		this.in = in;
		this.origin = origin;
		this.handler = handler;
	}

	TcpReadMessage(TcpReadMessage o) {
		this.port = o.port;
		this.in = o.in;
		this.origin = o.origin;
		this.handler = o.handler;
		this.isFinished = false;
		this.sequenceNr = o.sequenceNr;
	}

	Connectionhandler getHandler() {
		return handler;
	}

	public ReceivePort localPort() {
		return port;
	}

	protected int available() throws IOException {
		return in.available();
	}

      	public void finish() throws IOException {
		port.finishMessage();
		in.clear();
	}

	public SendPortIdentifier origin() {
		return origin;
	}

	void setSequenceNumber(long s) {
		sequenceNr = s;
	}

	public long sequenceNumber() { 
		return sequenceNr;
	}

	public boolean readBoolean() throws IOException { 
		return in.readBoolean();
	} 

	public byte readByte() throws IOException { 
		return (byte) in.read();
	} 

	public char readChar() throws IOException {
		return in.readChar();
	}

	public short readShort() throws IOException { 
		return in.readShort();
	} 

	public int readInt() throws IOException { 
		return in.readInt();
	}

	public long readLong() throws IOException { 
		return in.readLong();
	} 

	public float readFloat() throws IOException {
		return in.readFloat();
	} 

	public double readDouble() throws IOException { 
		return in.readDouble();
	}

	public String readString() throws IOException { 
		return (String) in.readUTF();
	} 

	public Object readObject() throws IOException, ClassNotFoundException {
		return in.readObject();
	} 
	
	public void readArray(boolean [] destination) throws IOException {
		in.readArray(destination, 0, destination.length); 
	}

	public void readArray(byte [] destination) throws IOException { 
		in.readArray(destination, 0, destination.length); 
	}

	public void readArray(char [] destination) throws IOException { 
		in.readArray(destination, 0, destination.length); 
	}

	public void readArray(short [] destination) throws IOException { 
		in.readArray(destination, 0, destination.length); 
	}

	public void readArray(int [] destination) throws IOException {  
		in.readArray(destination, 0, destination.length); 
	}

	public void readArray(long [] destination) throws IOException {
		in.readArray(destination, 0, destination.length); 
	}

	public void readArray(float [] destination) throws IOException { 
		in.readArray(destination, 0, destination.length); 
	}

	public void readArray(double [] destination) throws IOException {
		in.readArray(destination, 0, destination.length); 
	}

	public void readArray(Object [] destination) throws IOException, ClassNotFoundException {
		in.readArray(destination, 0, destination.length); 
	}

	public void readArray(boolean [] destination, int offset, int size) throws IOException {
		in.readArray(destination, offset, size); 
	}

	public void readArray(byte [] destination, int offset, int size) throws IOException {
		in.readArray(destination, offset, size); 
	}

	public void readArray(char [] destination, int offset, int size) throws IOException {
		in.readArray(destination, offset, size); 
	}


	public void readArray(short [] destination, int offset, int size) throws IOException {
		in.readArray(destination, offset, size); 
	}

	public void readArray(int [] destination, int offset, int size) throws IOException {
		in.readArray(destination, offset, size); 
	}

	public void readArray(long [] destination, int offset, int size) throws IOException {
		in.readArray(destination, offset, size); 
	}

	public void readArray(float [] destination, int offset, int size) throws IOException {
		in.readArray(destination, offset, size); 
	}

	public void readArray(double [] destination, int offset, int size) throws IOException {
		in.readArray(destination, offset, size); 
	}	

	public void readArray(Object [] destination, int offset, int size) throws IOException, ClassNotFoundException {
		in.readArray(destination, offset, size); 
	}	
}  
