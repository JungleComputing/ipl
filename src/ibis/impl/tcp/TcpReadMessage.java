package ibis.impl.tcp;

import ibis.io.SerializationInputStream;
import ibis.ipl.IbisError;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPortIdentifier;

import java.io.IOException;

final class TcpReadMessage implements ReadMessage { 
    private SerializationInputStream in;
    private long sequenceNr = -1;
    private TcpReceivePort port;
    private TcpSendPortIdentifier origin;
    private ConnectionHandler handler;
    boolean isFinished = false;
    long before;

    TcpReadMessage(
	    TcpReceivePort port,
	    SerializationInputStream in, 
	    TcpSendPortIdentifier origin,
	    ConnectionHandler handler) {
	this.port = port;
	this.in = in;
	this.origin = origin;
	this.handler = handler;
	before = handler.dummy.getCount();
    }

    TcpReadMessage(TcpReadMessage o) {
	this.port = o.port;
	this.in = o.in;
	this.origin = o.origin;
	this.handler = o.handler;
	this.isFinished = false;
	this.sequenceNr = o.sequenceNr;
	before = handler.dummy.getCount();
    }

    ConnectionHandler getHandler() {
	return handler;
    }

    public ReceivePort localPort() {
	if(isFinished) {
	    throw new IbisError("Reading data from a message that was already finished");
	}
	return port;
    }

    protected int available() throws IOException {
	if(isFinished) {
	    throw new IbisError("Reading data from a message that was already finished");
	}
	return in.available();
    }

    public long finish() throws IOException {
	long retval = 0;

	if(isFinished) {
	    throw new IbisError("Reading data from a message that was already finished");
	}

	if (Config.STATS) {
	    long after = handler.dummy.getCount();
	    retval = after - before;
	    before = after;
	    port.count += retval;
	}

	port.finishMessage();
	in.clear();

	return retval;
    }

    public void finish(IOException e) {
	if(isFinished) {
	    throw new IbisError("Reading data from a message that was already finished");
	}
	port.finishMessage(e);
    }

    public SendPortIdentifier origin() {
	if(isFinished) {
	    throw new IbisError("Reading data from a message that was already finished");
	}
	return origin;
    }

    void setSequenceNumber(long s) {
	sequenceNr = s;
    }

    public long sequenceNumber() { 
	if(isFinished) {
	    throw new IbisError("Reading data from a message that was already finished");
	}
	return sequenceNr;
    }

    public boolean readBoolean() throws IOException { 
	if(isFinished) {
	    throw new IbisError("Reading data from a message that was already finished");
	}
	return in.readBoolean();
    } 

    public byte readByte() throws IOException { 
	if(isFinished) {
	    throw new IbisError("Reading data from a message that was already finished");
	}
	return (byte) in.read();
    } 

    public char readChar() throws IOException {
	if(isFinished) {
	    throw new IbisError("Reading data from a message that was already finished");
	}
	return in.readChar();
    }

    public short readShort() throws IOException { 
	if(isFinished) {
	    throw new IbisError("Reading data from a message that was already finished");
	}
	return in.readShort();
    } 

    public int readInt() throws IOException { 
	if(isFinished) {
	    throw new IbisError("Reading data from a message that was already finished");
	}
	return in.readInt();
    }

    public long readLong() throws IOException { 
	if(isFinished) {
	    throw new IbisError("Reading data from a message that was already finished");
	}
	return in.readLong();
    } 

    public float readFloat() throws IOException {
	if(isFinished) {
	    throw new IbisError("Reading data from a message that was already finished");
	}
	return in.readFloat();
    } 

    public double readDouble() throws IOException { 
	if(isFinished) {
	    throw new IbisError("Reading data from a message that was already finished");
	}
	return in.readDouble();
    }

    public String readString() throws IOException { 
	if(isFinished) {
	    throw new IbisError("Reading data from a message that was already finished");
	}
	return in.readUTF();
    } 

    public Object readObject() throws IOException, ClassNotFoundException {
	if(isFinished) {
	    throw new IbisError("Reading data from a message that was already finished");
	}
	return in.readObject();
    } 

    public void readArray(boolean [] destination) throws IOException {
	if(isFinished) {
	    throw new IbisError("Reading data from a message that was already finished");
	}
	in.readArray(destination, 0, destination.length); 
    }

    public void readArray(byte [] destination) throws IOException { 
	if(isFinished) {
	    throw new IbisError("Reading data from a message that was already finished");
	}
	in.readArray(destination, 0, destination.length); 
    }

    public void readArray(char [] destination) throws IOException { 
	if(isFinished) {
	    throw new IbisError("Reading data from a message that was already finished");
	}
	in.readArray(destination, 0, destination.length); 
    }

    public void readArray(short [] destination) throws IOException { 
	if(isFinished) {
	    throw new IbisError("Reading data from a message that was already finished");
	}
	in.readArray(destination, 0, destination.length); 
    }

    public void readArray(int [] destination) throws IOException {  
	if(isFinished) {
	    throw new IbisError("Reading data from a message that was already finished");
	}
	in.readArray(destination, 0, destination.length); 
    }

    public void readArray(long [] destination) throws IOException {
	if(isFinished) {
	    throw new IbisError("Reading data from a message that was already finished");
	}
	in.readArray(destination, 0, destination.length); 
    }

    public void readArray(float [] destination) throws IOException { 
	if(isFinished) {
	    throw new IbisError("Reading data from a message that was already finished");
	}
	in.readArray(destination, 0, destination.length); 
    }

    public void readArray(double [] destination) throws IOException {
	if(isFinished) {
	    throw new IbisError("Reading data from a message that was already finished");
	}
	in.readArray(destination, 0, destination.length); 
    }

    public void readArray(Object [] destination) throws IOException, ClassNotFoundException {
	if(isFinished) {
	    throw new IbisError("Reading data from a message that was already finished");
	}
	in.readArray(destination, 0, destination.length); 
    }

    public void readArray(boolean [] destination, int offset, int size) throws IOException {
	if(isFinished) {
	    throw new IbisError("Reading data from a message that was already finished");
	}
	in.readArray(destination, offset, size); 
    }

    public void readArray(byte [] destination, int offset, int size) throws IOException {
	if(isFinished) {
	    throw new IbisError("Reading data from a message that was already finished");
	}
	in.readArray(destination, offset, size); 
    }

    public void readArray(char [] destination, int offset, int size) throws IOException {
	if(isFinished) {
	    throw new IbisError("Reading data from a message that was already finished");
	}
	in.readArray(destination, offset, size); 
    }


    public void readArray(short [] destination, int offset, int size) throws IOException {
	if(isFinished) {
	    throw new IbisError("Reading data from a message that was already finished");
	}
	in.readArray(destination, offset, size); 
    }

    public void readArray(int [] destination, int offset, int size) throws IOException {
	if(isFinished) {
	    throw new IbisError("Reading data from a message that was already finished");
	}
	in.readArray(destination, offset, size); 
    }

    public void readArray(long [] destination, int offset, int size) throws IOException {
	if(isFinished) {
	    throw new IbisError("Reading data from a message that was already finished");
	}
	in.readArray(destination, offset, size); 
    }

    public void readArray(float [] destination, int offset, int size) throws IOException {
	if(isFinished) {
	    throw new IbisError("Reading data from a message that was already finished");
	}
	in.readArray(destination, offset, size); 
    }

    public void readArray(double [] destination, int offset, int size) throws IOException {
	if(isFinished) {
	    throw new IbisError("Reading data from a message that was already finished");
	}
	in.readArray(destination, offset, size); 
    }	

    public void readArray(Object [] destination, int offset, int size)
	    throws IOException, ClassNotFoundException {
	if(isFinished) {
	    throw new IbisError("Reading data from a message that was already finished");
	}
	in.readArray(destination, offset, size); 
    }	
}  
