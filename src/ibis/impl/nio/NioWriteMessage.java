package ibis.impl.nio;

import ibis.io.SerializationOutputStream;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

import java.nio.channels.GatheringByteChannel;

import java.io.IOException;

final class NioWriteMessage implements WriteMessage, Config {
    private NioSendPort port;
    private SerializationOutputStream out;

    NioWriteMessage(NioSendPort port, 
		    SerializationOutputStream out) throws IOException {
	this.port = port;
	this.out = out;
    }

    /**
     * Sends all pending output to the network
     */
    public int send() throws IOException {
	try {
	    out.flush();
	} catch (NioSplitterException e) {
	    port.lostConnections(e);
	}
	return 0; // fake ticket
    }

    /**
     * Syncs data up to point of ticket. Actually just sends out everything
     */
    public void sync(int ticket) throws IOException {
	try {
	    out.flush();
	} catch (NioSplitterException e) {
	    port.lostConnections(e);
	}
    }

    public void reset() throws IOException {
	try {
	    out.reset();
	} catch (NioSplitterException e) {
	    port.lostConnections(e);
	}
    }

    public long finish() throws IOException {
	try {
	    out.flush();
	} catch (NioSplitterException e) {
	    port.lostConnections(e);
	}
	try {
	    out.reset();
	} catch (NioSplitterException e) {
	    port.lostConnections(e);
	}
	return port.finish();
    }

    public void finish(IOException e) {
	// What to do here? Niels?
	try {
	    finish();
	} catch(IOException e2) {
	}
    }

    public SendPort localPort() {
	return port;
    }

    public void writeBoolean(boolean value) throws IOException {
	try {
	    out.writeBoolean(value);
	} catch (NioSplitterException e) {
	    port.lostConnections(e);
	}
    }

    public void writeByte(byte value) throws IOException {
	try {
	    out.writeByte(value);
	} catch (NioSplitterException e) {
	    port.lostConnections(e);
	}
    }

    public void writeChar(char value) throws IOException {
	try {
	    out.writeChar(value);
	} catch (NioSplitterException e) {
	    port.lostConnections(e);
	}
    }

    public void writeShort(short value) throws IOException {
	try {
	    out.writeShort(value);
	} catch (NioSplitterException e) {
	    port.lostConnections(e);
	}
    }

    public void writeInt(int value) throws IOException {
	if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
	    System.err.println("NioWriteMessage: writing int");
	}
	try {
	    out.writeInt(value);
	} catch (NioSplitterException e) {
	    port.lostConnections(e);
	}
    }

    public void writeLong(long value) throws IOException {
	try {
	    out.writeLong(value);
	} catch (NioSplitterException e) {
	    port.lostConnections(e);
	}
    }

    public void writeFloat(float value) throws IOException {
	try {
	    out.writeFloat(value);
	} catch (NioSplitterException e) {
	    port.lostConnections(e);
	}
    }

    public void writeDouble(double value) throws IOException {
	try {
	    out.writeDouble(value);
	} catch (NioSplitterException e) {
	    port.lostConnections(e);
	}
    }

    public void writeString(String value) throws IOException {
	try {
	    out.writeUTF(value);
	} catch (NioSplitterException e) {
	    port.lostConnections(e);
	}
    }

    public void writeObject(Object value) throws IOException {
	try {
	    out.writeObject(value);
	} catch (NioSplitterException e) {
	    port.lostConnections(e);
	}
    }

    public void writeArray(boolean [] value) throws IOException {
	try {
	    out.writeArray(value);
	} catch (NioSplitterException e) {
	    port.lostConnections(e);
	}
    }

    public void writeArray(byte [] value) throws IOException { 
	try {
	    out.writeArray(value);
	} catch (NioSplitterException e) {
	    port.lostConnections(e);
	}
    }

    public void writeArray(char [] value) throws IOException { 
	try {
	    out.writeArray(value);
	} catch (NioSplitterException e) {
	    port.lostConnections(e);
	}
    }

    public void writeArray(short [] value) throws IOException { 
	try {
	    out.writeArray(value);
	} catch (NioSplitterException e) {
	    port.lostConnections(e);
	}
    }

    public void writeArray(int [] value) throws IOException { 
	try {
	    out.writeArray(value);
	} catch (NioSplitterException e) {
	    port.lostConnections(e);
	}
    }

    public void writeArray(long [] value) throws IOException { 
	try {
	    out.writeArray(value);
	} catch (NioSplitterException e) {
	    port.lostConnections(e);
	}
    }

    public void writeArray(float [] value) throws IOException { 
	try {
	    out.writeArray(value);
	} catch (NioSplitterException e) {
	    port.lostConnections(e);
	}
    }

    public void writeArray(double [] value) throws IOException { 
	try {
	    out.writeArray(value);
	} catch (NioSplitterException e) {
	    port.lostConnections(e);
	}
    }

    public void writeArray(Object [] value) throws IOException { 
	try {
	    out.writeArray(value);
	} catch (NioSplitterException e) {
	    port.lostConnections(e);
	}
    }

    public void writeArray(boolean [] value, int offset, int size) throws IOException { 
	try {
	    out.writeArray(value, offset, size);
	} catch (NioSplitterException e) {
	    port.lostConnections(e);
	}
    }

    public void writeArray(byte [] value, int offset, int size) throws IOException { 
	try {
	    out.writeArray(value, offset, size);
	} catch (NioSplitterException e) {
	    port.lostConnections(e);
	}
    }

    public void writeArray(char [] value, int offset, int size) throws IOException { 
	try {
	    out.writeArray(value, offset, size);
	} catch (NioSplitterException e) {
	    port.lostConnections(e);
	}
    }

    public void writeArray(short [] value, int offset, int size) throws IOException { 
	try {
	    out.writeArray(value, offset, size);
	} catch (NioSplitterException e) {
	    port.lostConnections(e);
	}
    }

    public void writeArray(int [] value, int offset, int size) throws IOException { 
	try {
	    out.writeArray(value, offset, size);
	} catch (NioSplitterException e) {
	    port.lostConnections(e);
	}
    }

    public void writeArray(long [] value, int offset, int size) throws IOException { 
	try {
	    out.writeArray(value, offset, size);
	} catch (NioSplitterException e) {
	    port.lostConnections(e);
	}
    }

    public void writeArray(float [] value, int offset, int size) throws IOException { 
	try {
	    out.writeArray(value, offset, size);
	} catch (NioSplitterException e) {
	    port.lostConnections(e);
	}
    }

    public void writeArray(double [] value, int offset, int size) throws IOException { 
	try {
	    out.writeArray(value, offset, size);
	} catch (NioSplitterException e) {
	    port.lostConnections(e);
	}
    }

    public void writeArray(Object [] value, int offset, int size) throws IOException { 
	try {
	    out.writeArray(value, offset, size);
	} catch (NioSplitterException e) {
	    port.lostConnections(e);
	}
    }
}
