package ibis.impl.tcp;

import ibis.io.SerializationOutputStream;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;
import ibis.util.SplitterException;

import java.io.IOException;

final class TcpWriteMessage implements WriteMessage {
	private TcpSendPort sport;
	private SerializationOutputStream out;
	private boolean connectionAdministration;
	private long before;

	TcpWriteMessage(TcpSendPort p, SerializationOutputStream out,
					boolean connectionAdministration) throws IOException {
		this.connectionAdministration = connectionAdministration;
		sport = p;
		this.out = out;
		before = sport.dummy.getCount();
	}

	// if we keep connectionAdministration, forward exception to upcalls / downcalls.
	// otherwise, rethrow the exception to the user.
	private void forwardLosses(SplitterException e) throws IOException {
		System.err.println("connection lost!");

		// Inform the port
		for(int i=0; i<e.count(); i++) {
			sport.lostConnection(e.getStream(i), e.getException(i), connectionAdministration);
		}

		if(!connectionAdministration) { // otherwise an upcall /downcall was/will be done
			throw e;
		}
	}

	public SendPort localPort() {
		return sport;
	}

	public int send() throws IOException {
		return 0;
	}

	public long finish() throws IOException {
		try {
			out.reset();
		} catch (SplitterException e) {
			forwardLosses(e);
		}
		try {
			out.flush();
		} catch (SplitterException e) {
			forwardLosses(e);
		}
		sport.finishMessage();
		long after = sport.dummy.getCount();
		long retval = after - before;
		sport.count += retval;
		before = after;
		return retval;
	}

	public void reset() throws IOException {
		try {
			out.reset();
		} catch (SplitterException e) {
			forwardLosses(e);
		}
		try {
			out.flush();
		} catch (SplitterException e) {
			forwardLosses(e);
		}
		long after = sport.dummy.getCount();
		sport.count += after - before;
		before = after;
	}

	public void sync(int ticket) throws IOException {
		try {
			out.flush();
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeBoolean(boolean value) throws IOException {
		try {
			out.writeBoolean(value);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeByte(byte value) throws IOException {
		try {
			out.writeByte(value);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeChar(char value) throws IOException {
		try {
			out.writeChar(value);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeShort(short value) throws IOException {
		try {
			out.writeShort(value);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeInt(int value) throws IOException {
		try {
			out.writeInt(value);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeLong(long value) throws IOException {
		try {
			out.writeLong(value);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeFloat(float value) throws IOException {
		try {
			out.writeFloat(value);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeDouble(double value) throws IOException {
		try {
			out.writeDouble(value);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeString(String value) throws IOException {
		try {
			out.writeUTF(value);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeObject(Object value) throws IOException {
		try {
			out.writeObject(value);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeArray(boolean [] value) throws IOException {
		try {
			out.writeArray(value);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}
	
	public void writeArray(byte [] value) throws IOException { 
		try {
			out.writeArray(value);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeArray(char [] value) throws IOException { 
		try {
			out.writeArray(value);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeArray(short [] value) throws IOException { 
		try {
			out.writeArray(value);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeArray(int [] value) throws IOException { 
		try {
			out.writeArray(value);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeArray(long [] value) throws IOException { 
		try {
			out.writeArray(value);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeArray(float [] value) throws IOException { 
		try {
			out.writeArray(value);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeArray(double [] value) throws IOException { 
		try {
			out.writeArray(value);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeArray(Object [] value) throws IOException { 
		try {
			out.writeArray(value);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeArray(boolean [] value, int offset, int size) throws IOException { 
		try {
			out.writeArray(value, offset, size);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeArray(byte [] value, int offset, int size) throws IOException { 
		try {
			out.writeArray(value, offset, size);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeArray(char [] value, int offset, int size) throws IOException { 
		try {
			out.writeArray(value, offset, size);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeArray(short [] value, int offset, int size) throws IOException { 
		try {
			out.writeArray(value, offset, size);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeArray(int [] value, int offset, int size) throws IOException { 
		try {
			out.writeArray(value, offset, size);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeArray(long [] value, int offset, int size) throws IOException { 
		try {
			out.writeArray(value, offset, size);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeArray(float [] value, int offset, int size) throws IOException { 
		try {
			out.writeArray(value, offset, size);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeArray(double [] value, int offset, int size) throws IOException { 
		try {
			out.writeArray(value, offset, size);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeArray(Object [] value, int offset, int size) throws IOException { 
		try {
			out.writeArray(value, offset, size);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}
}
